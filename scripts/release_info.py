"""Produce release notes directly from this version's Changelog entry."""
import pathlib
import re
import sys
import json
import zipfile

root = pathlib.Path(__file__).resolve().parents[1]
version = re.search(r"^mod_version=(.+)$", (root / "gradle.properties").read_text(), re.M).group(1).strip()
if len(sys.argv) > 1 and sys.argv[1] != "v" + version:
    raise SystemExit("Release tag must match gradle.properties")
changelog = (root / "CHANGELOG.md").read_text(encoding="utf-8")
entry = re.search(r"^## " + re.escape(version) + r" - [^\n]+\n(?:(?!^## ).)*", changelog, re.M | re.S)
if not entry:
    raise SystemExit("Missing Changelog entry for " + version)
(root / "build").mkdir(exist_ok=True)
(root / "build/release-notes.md").write_text(entry.group(0).strip() + "\n", encoding="utf-8")
jar = root / "build/libs" / ("goosetools-" + version + ".jar")
if not jar.is_file():
    raise SystemExit("Missing runtime JAR: " + jar.name)
with zipfile.ZipFile(jar) as archive:
    metadata = json.loads(archive.read("fabric.mod.json"))
protocol_source = (root / "src/main/java/com/goosethings/tools/GooseTools.java").read_text(encoding="utf-8")
protocol = int(re.search(r"PROTOCOL_VERSION\s*=\s*(\d+)", protocol_source).group(1))
if metadata["version"] != version or metadata.get("custom", {}).get("goosetools:protocol") != protocol:
    raise SystemExit("Runtime JAR version/protocol metadata does not match its source")
print("version=" + version)
print("jar=" + jar.relative_to(root).as_posix())
print("prerelease=" + str("alpha" in version.lower()).lower())
