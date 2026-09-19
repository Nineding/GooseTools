package com.goosethings.tools.client.shader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/** Narrow Modrinth client restricted to the four approved project IDs and signed HTTPS downloads. */
public final class ModrinthClient {
    public static final String SODIUM = "AANobbMI";
    public static final String IRIS = "YL57xq9U";
    public static final String EUPHORIA = "4H6sumDB";
    public static final String COMPLEMENTARY = "HVnmMxH1";
    private static final String GAME_VERSION = "26.3";
    private static final long MAX_FILE_BYTES = 64L * 1024L * 1024L;
    private static final long MAX_TOTAL_BYTES = 256L * 1024L * 1024L;
    private static final int MAX_DOWNLOAD_REDIRECTS = 3;
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER).build();

    private ModrinthClient() {
    }

    public static Plan resolve() throws Exception {
        List<Artifact> artifacts = new ArrayList<>();
        Selection selection = select(loaded("iris"), loaded("sodium"),
                loaded("euphoria_patcher"), RecommendedShaderManager.findBasePack().isPresent());
        JsonObject irisVersion = null;
        if (selection.iris()) {
            irisVersion = latest(IRIS, "fabric");
            artifacts.add(artifact(irisVersion, "iris", "mods"));
        }
        if (selection.sodium()) {
            if (irisVersion == null) {
                irisVersion = latest(IRIS, "fabric");
            }
            String sodiumVersionId = requiredVersion(irisVersion, SODIUM);
            JsonObject sodiumVersion = sodiumVersionId == null
                    ? latest(SODIUM, "fabric") : version(sodiumVersionId);
            if (!SODIUM.equals(string(sodiumVersion, "project_id"))) {
                throw new SecurityException("Iris dependency did not resolve to the Sodium project");
            }
            validateStableExact(sodiumVersion, "fabric");
            artifacts.add(artifact(sodiumVersion, "sodium", "mods"));
        }
        if (selection.euphoria()) {
            JsonObject version = latest(EUPHORIA, "fabric");
            artifacts.add(artifact(version, "euphoria_patcher", "mods"));
        }
        if (selection.complementary()) {
            String requiredShader = selection.euphoria()
                    ? compatibleShaderVersion(artifacts.stream()
                            .filter(item -> "euphoria_patcher".equals(item.modId()))
                            .findFirst().orElseThrow().fileName())
                    : EuphoriaCompat.requiredComplementaryVersion().orElse(null);
            artifacts.add(artifact(latestComplementary(requiredShader),
                    "complementary", "shaderpacks"));
        }
        return new Plan(artifacts);
    }

    static Selection select(boolean irisInstalled, boolean sodiumInstalled,
                            boolean euphoriaInstalled, boolean basePackInstalled) {
        return new Selection(!irisInstalled, !sodiumInstalled,
                !euphoriaInstalled, !basePackInstalled);
    }

    public static Path download(Plan plan, RestartInstaller.Launch launch) throws Exception {
        Path gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        Path staging = gameDir.resolve("config").resolve("goosetools")
                .resolve("update-staging").resolve(UUID.randomUUID().toString());
        Files.createDirectories(staging);
        Properties manifest = new Properties();
        manifest.setProperty("game.dir", gameDir.toString());
        manifest.setProperty("staging.dir", staging.toString());
        manifest.setProperty("artifact.count", Integer.toString(plan.artifacts().size()));
        RestartInstaller.writeLaunch(manifest, launch);
        long total = 0L;
        for (int index = 0; index < plan.artifacts().size(); index++) {
            Artifact artifact = plan.artifacts().get(index);
            if (artifact.size() <= 0L || artifact.size() > MAX_FILE_BYTES
                    || (total += artifact.size()) > MAX_TOTAL_BYTES) {
                throw new IOException("Modrinth file exceeds the GooseTools download limit");
            }
            Path staged = staging.resolve(index + "-" + safeName(artifact.fileName())).normalize();
            if (!staged.startsWith(staging)) {
                throw new SecurityException("Unsafe Modrinth filename");
            }
            downloadOne(artifact, staged);
            validateArchive(staged, artifact.modId());
            Path targetParent = artifact.targetFolder().equals("mods")
                    ? gameDir.resolve("mods") : gameDir.resolve("shaderpacks");
            Path target = targetParent.resolve(safeName(artifact.fileName())).toAbsolutePath().normalize();
            manifest.setProperty("artifact." + index + ".source", staged.toString());
            manifest.setProperty("artifact." + index + ".target", target.toString());
            if (!"complementary".equals(artifact.modId())) {
                int artifactIndex = index;
                loadedOrigin(artifact.modId()).ifPresent(origin ->
                        manifest.setProperty("artifact." + artifactIndex + ".replace", origin.toString()));
            }
        }
        Path manifestPath = staging.resolve("pending-update.properties");
        try (var output = Files.newOutputStream(manifestPath)) {
            manifest.store(output, "GooseTools verified pending update");
        }
        return manifestPath;
    }

    /** Installs only a verified shader archive while Minecraft remains running. */
    public static Path installShaderPackNow(Plan plan) throws Exception {
        if (plan.requiresRestart()) {
            throw new IllegalArgumentException("A Fabric mod update cannot be installed while Minecraft is running");
        }
        Artifact shader = plan.artifacts().stream()
                .filter(artifact -> "complementary".equals(artifact.modId()))
                .findFirst().orElse(null);
        if (shader == null) {
            Path existing = RecommendedShaderManager.findBasePack()
                    .orElseThrow(() -> new IOException("Complementary Reimagined is missing"));
            validateArchive(existing, "complementary");
            return existing;
        }
        if (plan.artifacts().size() != 1) {
            throw new SecurityException("Immediate shader setup contained an unexpected artifact");
        }
        if (shader.size() <= 0L || shader.size() > MAX_FILE_BYTES) {
            throw new IOException("Modrinth shader file exceeds the GooseTools download limit");
        }

        Path gameDir = FabricLoader.getInstance().getGameDir().toAbsolutePath().normalize();
        Path staging = gameDir.resolve("config").resolve("goosetools")
                .resolve("update-staging").resolve(UUID.randomUUID().toString());
        Files.createDirectories(staging);
        Path staged = staging.resolve(safeName(shader.fileName())).normalize();
        try {
            downloadOne(shader, staged);
            validateArchive(staged, shader.modId());
            Path shaderpacks = gameDir.resolve("shaderpacks").toAbsolutePath().normalize();
            Files.createDirectories(shaderpacks);
            Path target = shaderpacks.resolve(safeName(shader.fileName())).normalize();
            if (!target.startsWith(shaderpacks)) {
                throw new SecurityException("Unsafe shaderpack target");
            }
            if (Files.exists(target)) {
                validateArchive(target, shader.modId());
                if (!sha512(target).equals(shader.sha512())) {
                    throw new IOException("The shaderpack target changed while downloading; retry setup");
                }
                Files.deleteIfExists(staged);
                return target;
            }
            try {
                return Files.move(staged, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
                return Files.move(staged, target);
            }
        } finally {
            Files.deleteIfExists(staged);
            Files.deleteIfExists(staging);
        }
    }

    private static JsonObject latest(String project, String loader) throws Exception {
        String loaders = URLEncoder.encode("[\"" + loader + "\"]", StandardCharsets.UTF_8);
        String games = URLEncoder.encode("[\"" + GAME_VERSION + "\"]", StandardCharsets.UTF_8);
        JsonArray versions = getJson("https://api.modrinth.com/v2/project/" + project
                + "/version?loaders=" + loaders + "&game_versions=" + games).getAsJsonArray();
        for (JsonElement element : versions) {
            JsonObject candidate = element.getAsJsonObject();
            if ("release".equals(candidate.get("version_type").getAsString())) {
                validateStableExact(candidate, loader);
                return candidate;
            }
        }
        throw new IOException("No stable " + GAME_VERSION + " release found for " + project);
    }

    private static JsonObject latestComplementary(String requiredShader) throws Exception {
        String loaders = URLEncoder.encode("[\"iris\"]", StandardCharsets.UTF_8);
        String games = URLEncoder.encode("[\"" + GAME_VERSION + "\"]", StandardCharsets.UTF_8);
        JsonArray versions = getJson("https://api.modrinth.com/v2/project/" + COMPLEMENTARY
                + "/version?loaders=" + loaders + "&game_versions=" + games).getAsJsonArray();
        for (JsonElement element : versions) {
            JsonObject candidate = element.getAsJsonObject();
            if (!"release".equals(string(candidate, "version_type"))) {
                continue;
            }
            validateStableExact(candidate, "iris");
            if (requiredShader == null || versionMatches(
                    artifact(candidate, "complementary", "shaderpacks").fileName(), requiredShader)) {
                return candidate;
            }
        }
        throw new IOException(requiredShader == null
                ? "No stable Complementary shader was found"
                : "No Complementary " + requiredShader + " release compatible with the loaded Euphoria Patcher was found");
    }

    static String compatibleShaderVersion(String euphoriaFileName) {
        Matcher matcher = Pattern.compile("(?i)(r\\d+(?:\\.\\d+)+)").matcher(euphoriaFileName);
        return matcher.find() ? matcher.group(1).toLowerCase(Locale.ROOT) : null;
    }

    static boolean versionMatches(String shaderFileName, String requiredShader) {
        if (requiredShader == null || requiredShader.isBlank()) return true;
        return Pattern.compile("(?i)" + Pattern.quote(requiredShader) + "(?!\\d|\\.\\d)")
                .matcher(shaderFileName).find();
    }

    private static JsonObject version(String id) throws Exception {
        return getJson("https://api.modrinth.com/v2/version/" + encodePath(id)).getAsJsonObject();
    }

    private static void validateStableExact(JsonObject version, String loader) throws IOException {
        if (!"release".equals(string(version, "version_type"))
                || !contains(version.getAsJsonArray("game_versions"), GAME_VERSION)
                || !contains(version.getAsJsonArray("loaders"), loader)) {
            throw new IOException("Modrinth returned a non-stable or incompatible file");
        }
    }

    private static String requiredVersion(JsonObject version, String projectId) {
        for (JsonElement element : version.getAsJsonArray("dependencies")) {
            JsonObject dependency = element.getAsJsonObject();
            if (projectId.equals(string(dependency, "project_id"))
                    && "required".equals(string(dependency, "dependency_type"))) {
                return string(dependency, "version_id");
            }
        }
        return null;
    }

    private static Artifact artifact(JsonObject version, String modId, String target) throws IOException {
        JsonObject selected = null;
        for (JsonElement element : version.getAsJsonArray("files")) {
            JsonObject file = element.getAsJsonObject();
            if (selected == null || (file.has("primary") && file.get("primary").getAsBoolean())) {
                selected = file;
            }
        }
        if (selected == null) {
            throw new IOException("Modrinth version has no downloadable file");
        }
        URI uri = URI.create(string(selected, "url"));
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"cdn.modrinth.com".equalsIgnoreCase(uri.getHost())) {
            throw new SecurityException("Untrusted Modrinth download host");
        }
        return new Artifact(string(version, "name"), string(version, "version_number"), modId,
                target, string(selected, "filename"), uri,
                selected.get("size").getAsLong(),
                string(selected.getAsJsonObject("hashes"), "sha512").toLowerCase(Locale.ROOT));
    }

    private static void downloadOne(Artifact artifact, Path target) throws Exception {
        URI current = artifact.url();
        HttpResponse<InputStream> response = null;
        for (int redirects = 0; redirects <= MAX_DOWNLOAD_REDIRECTS; redirects++) {
            validateDownloadUri(current);
            response = HTTP.send(request(current), HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() == 200) {
                break;
            }
            if (!isRedirect(response.statusCode())) {
                response.body().close();
                throw new IOException("Modrinth download returned HTTP " + response.statusCode());
            }
            String location = response.headers().firstValue("location")
                    .orElseThrow(() -> new IOException("Modrinth redirect omitted its location"));
            response.body().close();
            if (redirects == MAX_DOWNLOAD_REDIRECTS) {
                throw new IOException("Modrinth download exceeded the redirect limit");
            }
            current = preferRawCdn(current.resolve(location));
        }
        if (response == null || response.statusCode() != 200) {
            throw new IOException("Modrinth download did not return a file");
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        long count = 0;
        try (InputStream input = response.body(); var output = Files.newOutputStream(target)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                count += read;
                if (count > MAX_FILE_BYTES || count > artifact.size() + 1024L) {
                    throw new IOException("Modrinth response exceeded its declared size");
                }
                digest.update(buffer, 0, read);
                output.write(buffer, 0, read);
            }
        }
        String actual = HexFormat.of().formatHex(digest.digest());
        if (count != artifact.size() || !actual.equals(artifact.sha512())) {
            Files.deleteIfExists(target);
            throw new SecurityException("Modrinth SHA-512 verification failed");
        }
    }

    private static boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }

    static void validateDownloadUri(URI uri) {
        String host = uri.getHost();
        boolean trustedHost = "cdn.modrinth.com".equalsIgnoreCase(host)
                || "cdn-raw.modrinth.com".equalsIgnoreCase(host)
                || "cdn-alt.modrinth.com".equalsIgnoreCase(host);
        if (!"https".equalsIgnoreCase(uri.getScheme()) || !trustedHost
                || uri.getUserInfo() != null || uri.getPath() == null
                || !uri.getPath().startsWith("/data/")) {
            throw new SecurityException("Untrusted Modrinth download redirect");
        }
    }

    static URI preferRawCdn(URI uri) {
        validateDownloadUri(uri);
        if (!"cdn-alt.modrinth.com".equalsIgnoreCase(uri.getHost())) {
            return uri;
        }
        try {
            URI raw = new URI("https", null, "cdn-raw.modrinth.com", -1,
                    uri.getPath(), uri.getQuery(), null);
            validateDownloadUri(raw);
            return raw;
        } catch (java.net.URISyntaxException exception) {
            throw new SecurityException("Invalid Modrinth CDN redirect", exception);
        }
    }

    private static String sha512(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-512");
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void validateArchive(Path archive, String modId) throws Exception {
        try (ZipFile zip = new ZipFile(archive.toFile())) {
            if (zip.size() > 20_000) {
                throw new IOException("Archive contains too many entries");
            }
            long expandedBytes = 0L;
            var entries = zip.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                Path entryPath = Path.of(entry.getName()).normalize();
                if (entryPath.isAbsolute() || entryPath.startsWith("..")) {
                    throw new SecurityException("Archive contains an unsafe path");
                }
                if (entry.getSize() > 0L && (expandedBytes += entry.getSize()) > MAX_TOTAL_BYTES) {
                    throw new IOException("Archive expands beyond the GooseTools limit");
                }
            }
            if ("complementary".equals(modId)) {
                if (zip.getEntry("shaders/shaders.properties") == null) {
                    throw new IOException("Complementary shader archive is invalid");
                }
                return;
            }
            ZipEntry metadata = zip.getEntry("fabric.mod.json");
            if (metadata == null || metadata.getSize() > 1024 * 1024L) {
                throw new IOException("Fabric mod metadata is missing or too large");
            }
            try (InputStream input = zip.getInputStream(metadata)) {
                JsonObject json = JsonParser.parseReader(
                        new java.io.InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
                if (!modId.equals(string(json, "id"))) {
                    throw new SecurityException("Downloaded Fabric mod ID does not match request");
                }
            }
        }
    }

    private static JsonElement getJson(String url) throws Exception {
        URI uri = URI.create(url);
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !"api.modrinth.com".equalsIgnoreCase(uri.getHost())) {
            throw new SecurityException("Untrusted API host");
        }
        HttpResponse<String> response = HTTP.send(request(uri),
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() != 200 || response.body().length() > 8 * 1024 * 1024) {
            throw new IOException("Modrinth API returned HTTP " + response.statusCode());
        }
        return JsonParser.parseString(response.body());
    }

    private static HttpRequest request(URI uri) {
        return HttpRequest.newBuilder(uri).header("User-Agent", "GooseTools/1.11.0")
                .timeout(java.time.Duration.ofSeconds(30)).GET().build();
    }

    private static boolean loaded(String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }

    private static java.util.Optional<Path> loadedOrigin(String id) {
        return FabricLoader.getInstance().getModContainer(id)
                .flatMap(container -> container.getOrigin().getPaths().stream().findFirst())
                .map(path -> path.toAbsolutePath().normalize())
                .filter(Files::isRegularFile);
    }

    private static boolean contains(JsonArray values, String expected) {
        if (values == null) return false;
        for (JsonElement value : values) if (expected.equals(value.getAsString())) return true;
        return false;
    }

    private static String string(JsonObject object, String key) {
        return object != null && object.has(key) && !object.get(key).isJsonNull()
                ? object.get(key).getAsString() : "";
    }

    private static String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    static String safeName(String name) {
        String leaf = Path.of(name).getFileName().toString();
        if (!leaf.equals(name) || leaf.isBlank() || leaf.contains("..")
                || (!leaf.endsWith(".jar") && !leaf.endsWith(".zip"))) {
            throw new SecurityException("Unsafe Modrinth filename");
        }
        return leaf;
    }

    public record Artifact(String name, String version, String modId, String targetFolder,
                           String fileName, URI url, long size, String sha512) { }

    record Selection(boolean iris, boolean sodium, boolean euphoria, boolean complementary) { }

    public record Plan(List<Artifact> artifacts) {
        public Plan { artifacts = List.copyOf(artifacts); }

        public boolean requiresRestart() {
            return artifacts.stream().anyMatch(artifact -> "mods".equals(artifact.targetFolder()));
        }
    }
}
