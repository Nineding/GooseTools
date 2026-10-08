package com.goosethings.tools.client.update;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.metadata.version.VersionPredicate;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.zip.ZipFile;

/** Public Releases only; no user token, arbitrary repository, mirror or remote command support. */
public final class GitHubUpdateClient {
    private static final String API = "https://api.github.com/repos/Nineding/GooseTools/releases";
    private static final String DOWNLOAD = "/Nineding/GooseTools/releases/download/";
    private static final long MAX_JAR = 64L * 1024 * 1024;
    private static final HttpClient HTTP = httpClient();

    private static HttpClient httpClient() {
        var builder = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NEVER);
        var proxy = UpdateProxySelector.configured(System.getenv(), java.net.ProxySelector.getDefault());
        if (proxy != null) builder.proxy(proxy);
        return builder.build();
    }

    private GitHubUpdateClient() { }

    public static List<Release> releases(String current, boolean alpha, String required) throws Exception {
        return releases(current, alpha, required, new UpdateMonitor());
    }

    public static List<Release> releases(String current, boolean alpha, String required, UpdateMonitor monitor) throws Exception {
        monitor.update(UpdateMonitor.Phase.CHECKING, "", 0, 0);
        List<Release> result = new ArrayList<>();
        // An exact server version may be much older than the first page of Releases.
        String url = required == null ? API + "?per_page=100" : API + "/tags/v" + required.replace("+", "%2B");
        JsonElement json = JsonParser.parseString(new String(get(URI.create(url), 4L * 1024 * 1024, monitor, 0),
                StandardCharsets.UTF_8));
        JsonArray entries = new JsonArray();
        if (json.isJsonArray()) entries = json.getAsJsonArray();
        else entries.add(json);
        for (JsonElement element : entries) {
            try {
                release(element.getAsJsonObject(), current, alpha, required).ifPresent(result::add);
            } catch (IllegalArgumentException | IllegalStateException | NullPointerException invalid) {
                // Ignore unrelated releases and malformed assets, never install them.
            }
        }
        result.sort(Comparator.comparing((Release item) -> UpdateVersion.parse(item.version())).reversed());
        return result;
    }

    static Optional<Release> release(JsonObject json, String current, boolean alpha, String required) {
        if (json.get("draft").getAsBoolean()) return Optional.empty();
        String version = json.get("tag_name").getAsString().replaceFirst("^v", "");
        UpdateVersion parsed = UpdateVersion.parse(version);
        if ((!alpha && (json.get("prerelease").getAsBoolean() || parsed.alphaMajor() != null))
                || version.equals(current)
                || (required == null && parsed.compareTo(UpdateVersion.parse(current)) <= 0)
                || (required != null && !version.equals(required))) return Optional.empty();
        for (JsonElement element : json.getAsJsonArray("assets")) {
            JsonObject asset = element.getAsJsonObject();
            if (!("goosetools-" + version + ".jar").equals(asset.get("name").getAsString())) continue;
            String digest = asset.has("digest") && !asset.get("digest").isJsonNull()
                    ? asset.get("digest").getAsString() : "";
            long size = asset.get("size").getAsLong();
            URI uri = URI.create(asset.get("browser_download_url").getAsString());
            if (!digest.matches("sha256:[0-9a-fA-F]{64}") || size <= 0 || size > MAX_JAR
                    || !allowed(uri) || !"github.com".equals(uri.getHost())
                    || !uri.getPath().startsWith(DOWNLOAD)) continue;
            return Optional.of(new Release(version, asset.get("name").getAsString(), uri, size,
                    digest.substring(7).toLowerCase(java.util.Locale.ROOT)));
        }
        return Optional.empty();
    }

    public static Optional<Path> stage(Release release, Path gameDir, Path origin,
                                      int protocol, Map<String, String> installed) throws Exception {
        return stage(release, gameDir, origin, protocol, installed, new UpdateMonitor());
    }

    public static Optional<Path> stage(Release release, Path gameDir, Path origin,
                                      int protocol, Map<String, String> installed, UpdateMonitor monitor) throws Exception {
        monitor.update(UpdateMonitor.Phase.DOWNLOADING, release.version(), 0, release.size());
        Path root = gameDir.toAbsolutePath().normalize();
        Path mods = root.resolve("mods");
        // Do not replace a development class directory, nested dependency or launcher-managed path.
        if (!origin.toAbsolutePath().normalize().getParent().equals(mods)
                || !Files.isRegularFile(origin) || Files.isSymbolicLink(origin)) return Optional.empty();
        Path staging = root.resolve("config/goosetools/update-staging").resolve(UUID.randomUUID().toString());
        com.goosethings.tools.client.update.bootstrap.BootstrapFiles.safe(staging);
        com.goosethings.tools.client.update.bootstrap.BootstrapFiles.safe(origin);
        Files.createDirectories(staging);
        Path jar = staging.resolve(release.fileName());
        byte[] data = get(release.uri(), MAX_JAR, monitor, release.size());
        monitor.update(UpdateMonitor.Phase.VERIFYING, release.version(), data.length, release.size());
        if (data.length != release.size() || !HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(data)).equals(release.sha256())) {
            throw new IOException("GitHub update checksum or size mismatch");
        }
        Files.write(jar, data);
        if (!compatible(jar, release.version(), protocol, installed)) {
            Files.deleteIfExists(jar);
            Files.deleteIfExists(staging);
            return Optional.empty();
        }
        Properties manifest = new Properties();
        manifest.setProperty("game.dir", root.toString());
        manifest.setProperty("staging.dir", staging.toString());
        manifest.setProperty("artifact.count", "1");
        manifest.setProperty("artifact.0.source", jar.toString());
        manifest.setProperty("artifact.0.target", mods.resolve(release.fileName()).toString());
        manifest.setProperty("artifact.0.replace", origin.toAbsolutePath().normalize().toString());
        manifest.setProperty("artifact.0.sha256", release.sha256());
        manifest.setProperty("update.version", release.version());
        manifest.setProperty("launch.automatic", "false");
        Path file = staging.resolve("pending-update.properties");
        try (var output = Files.newOutputStream(file)) { manifest.store(output, "Verified GooseTools update"); }
        monitor.checkCancelled();
        return Optional.of(file);
    }

    static boolean compatible(Path jar, String expected, int protocol, Map<String, String> installed)
            throws Exception {
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            var entry = zip.getEntry("fabric.mod.json");
            if (entry == null || entry.getSize() > 128 * 1024) return false;
            JsonObject metadata;
            try (InputStream input = zip.getInputStream(entry)) {
                byte[] bytes = input.readNBytes(128 * 1024 + 1);
                if (bytes.length > 128 * 1024) return false;
                metadata = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            }
            if (!"goosetools".equals(metadata.get("id").getAsString())
                    || !expected.equals(metadata.get("version").getAsString())
                    || !metadata.has("custom")
                    || metadata.getAsJsonObject("custom").get("goosetools:protocol").getAsInt() <= 0
                    || (protocol >= 0 && metadata.getAsJsonObject("custom").get("goosetools:protocol").getAsInt() != protocol)) return false;
            for (var dependency : metadata.getAsJsonObject("depends").entrySet()) {
                String version = installed.get(dependency.getKey());
                if (version == null || !matches(dependency.getValue(), version)) return false;
            }
            if (metadata.has("breaks")) {
                for (var conflict : metadata.getAsJsonObject("breaks").entrySet()) {
                    String version = installed.get(conflict.getKey());
                    if (version != null && matches(conflict.getValue(), version)) return false;
                }
            }
            return true;
        }
    }

    private static boolean matches(JsonElement predicates, String version) throws Exception {
        if (!predicates.isJsonArray()) return VersionPredicate.parse(predicates.getAsString()).test(Version.parse(version));
        for (JsonElement predicate : predicates.getAsJsonArray()) {
            if (VersionPredicate.parse(predicate.getAsString()).test(Version.parse(version))) return true;
        }
        return false;
    }

    private static byte[] get(URI uri, long max, UpdateMonitor monitor, long downloadTotal) throws Exception {
        for (int redirects = 0; redirects <= 5; redirects++) {
            monitor.checkCancelled();
            if (!allowed(uri)) throw new IOException("Unapproved GitHub download address");
            HttpRequest request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(60))
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "GooseTools-Updater").GET().build();
            var future = HTTP.sendAsync(request, info -> new LimitedBody(max, monitor,
                    info.statusCode() == 200 ? downloadTotal : 0));
            HttpResponse<byte[]> response;
            long deadline = System.nanoTime() + Duration.ofSeconds(60).toNanos();
            try {
                while (true) {
                    monitor.checkCancelled();
                    try { response = future.get(200, java.util.concurrent.TimeUnit.MILLISECONDS); break; }
                    catch (java.util.concurrent.TimeoutException waiting) {
                        if (System.nanoTime() >= deadline) throw new IOException("GitHub transfer timed out");
                    }
                }
            } catch (Exception failure) { future.cancel(true); throw failure; }
            if (response.statusCode() >= 300 && response.statusCode() < 400) {
                uri = uri.resolve(response.headers().firstValue("location").orElseThrow());
                continue;
            }
            if (response.statusCode() != 200) throw new IOException("GitHub returned " + response.statusCode());
            return response.body();
        }
        throw new IOException("Too many GitHub download redirects");
    }

    static boolean allowed(URI uri) {
        return "https".equals(uri.getScheme()) && uri.getUserInfo() == null
                && (uri.getPort() == -1 || uri.getPort() == 443)
                && List.of("api.github.com", "github.com", "release-assets.githubusercontent.com",
                "objects.githubusercontent.com").contains(uri.getHost());
    }

    /** Cancel excessive bodies while receiving, with a total network deadline on the future. */
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final long max;
        private final UpdateMonitor monitor;
        private final long total;
        private final java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
        private final java.util.concurrent.CompletableFuture<byte[]> result = new java.util.concurrent.CompletableFuture<>();
        private java.util.concurrent.Flow.Subscription subscription;

        LimitedBody(long max, UpdateMonitor monitor, long total) { this.max = max; this.monitor = monitor; this.total = total; }
        @Override public java.util.concurrent.CompletionStage<byte[]> getBody() { return result; }
        @Override public void onSubscribe(java.util.concurrent.Flow.Subscription value) {
            subscription = value;
            subscription.request(1);
        }
        @Override public void onNext(List<java.nio.ByteBuffer> buffers) {
            try { monitor.checkCancelled(); }
            catch (java.util.concurrent.CancellationException cancelled) {
                subscription.cancel(); result.completeExceptionally(cancelled); return;
            }
            for (var buffer : buffers) {
                if ((long) bytes.size() + buffer.remaining() > max) {
                    subscription.cancel();
                    result.completeExceptionally(new IOException("GitHub response exceeds update limit"));
                    return;
                }
                byte[] chunk = new byte[buffer.remaining()];
                buffer.get(chunk);
                bytes.writeBytes(chunk);
            }
            if (total > 0) {
                try { monitor.update(UpdateMonitor.Phase.DOWNLOADING, monitor.snapshot().version(), bytes.size(), total); }
                catch (java.util.concurrent.CancellationException cancelled) {
                    subscription.cancel(); result.completeExceptionally(cancelled); return;
                }
            }
            subscription.request(1);
        }
        @Override public void onError(Throwable failure) { result.completeExceptionally(failure); }
        @Override public void onComplete() { result.complete(bytes.toByteArray()); }
    }

    public record Release(String version, String fileName, URI uri, long size, String sha256) { }
}
