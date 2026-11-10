import ctypes
from ctypes import wintypes
import json
import os
from pathlib import Path
import shutil
import socket
import subprocess
import time

root = Path(__file__).resolve().parents[1] / "build/sakura-native-validation"
game = root.parents[2]
java = Path("D:/software/Java/JDK-25/bin/java.exe")
config = json.loads((root / "launch.json").read_text(encoding="utf-8"))
run = root / ("run-" + str(time.time_ns()))
run.mkdir()
with socket.socket() as port_socket:
    port_socket.bind(("127.0.0.1", 0))
    port = port_socket.getsockname()[1]
children = {}


def quote(arg):
    return '"' + str(arg).replace("\\", "/").replace('"', '\\"') + '"'


def launch(name, environment, program, mods):
    instance = run / name
    spec = config[environment]
    vm = [arg for arg in spec["vm"] if not arg.startswith("@")]
    version = next(line.split("=",1)[1].strip() for line in (game / "GooseTools-publish/gradle.properties").read_text().splitlines() if line.startswith("mod_version="))
    release = root.parent / "libs" / ("goosetools-" + version + ".jar")
    mods = [root / "fixture.jar", release, *mods]
    development_outputs = {root.parent / "classes/java/main", root.parent / "resources/main"}
    classpath = os.pathsep.join(
        path for path in spec["classpath"].split(os.pathsep) if Path(path) not in development_outputs
    )
    argv = [
        *vm, "-Xmx1600m", "-XX:ActiveProcessorCount=4",
        "-Doshi.os.windows.hkeyperfdata=false", "-Doshi.os.windows.perfproc.disabled=true",
        "-Doshi.os.windows.perfos.disabled=true", "-Doshi.util.wmi.timeout=2000",
        f"-Dsakura.probe={run}", f"-Dsakura.port={port}", f"-Dsakura.role={name}",
        "-Dsakura.quick="+os.environ.get("SAKURA_QUICK","false"),
        "-Dfabric.addMods=" + os.pathsep.join(map(str, mods)),
        "-classpath", classpath, spec["main"], *spec["program"], *program,
    ]
    argfile = instance / "java.args"
    argfile.write_text("\n".join(map(quote, argv)), encoding="utf-8")
    log = (instance / "process.log").open("wb")
    try:
        process = subprocess.Popen([str(java), "@" + str(argfile)], cwd=instance, stdin=subprocess.DEVNULL,
                                   stdout=log, stderr=subprocess.STDOUT, creationflags=subprocess.CREATE_NO_WINDOW)
        children[name] = process
    finally:
        log.close()


def prepare_client(name):
    instance = run / name
    (instance / "resourcepacks").mkdir(parents=True)
    repo = game / "GooseGooseDuck" / "resourcepack"
    base = instance / "resourcepacks" / "test-selector"
    items = base / "assets" / "minecraft" / "items"
    items.mkdir(parents=True)
    shutil.copyfile(repo / "pack.mcmeta", base / "pack.mcmeta")
    definition = json.loads((repo / "assets" / "minecraft" / "items" / "carrot_on_a_stick.json").read_text(encoding="utf-8"))
    selector = definition["model"]
    selector["cases"] = [case for case in selector["cases"] if case["when"] in ("sakura_sword", "sakura_sword_charging")]
    assert len(selector["cases"]) == 2
    (items / "carrot_on_a_stick.json").write_text(json.dumps(definition), encoding="utf-8")
    overlay = root / "iaido-test.zip"
    import zipfile
    with zipfile.ZipFile(overlay,"w",zipfile.ZIP_DEFLATED) as z:
        z.write(repo / "pack.mcmeta","pack.mcmeta")
        for f in [* (repo / "assets/goosetools").rglob("*"),*(repo / "assets/minecraft/models/item").glob("sakura_sword*.json")]:
            if f.is_file() and ("sakura" in str(f) or "geometry.json" in str(f)):z.write(f,str(f.relative_to(repo)))
    shutil.copyfile(overlay, instance / "resourcepacks" / overlay.name)
    packs = ["vanilla", "file/test-selector", "file/" + overlay.name]
    if name == "B":
        moves_pack = game / "resourcepacks" / "Fluid & Foolish 1.0.1.zip"
        shutil.copyfile(moves_pack, instance / "resourcepacks" / moves_pack.name)
        packs.insert(1, "file/" + moves_pack.name)
    if name == "B" and os.environ.get("SAKURA_IRIS") == "true":
        (instance / "shaderpacks").mkdir(exist_ok=True)
        (instance / "config").mkdir(exist_ok=True)
        shader = "ComplementaryReimagined_r5.9.3.zip"
        shutil.copyfile(game / "shaderpacks" / shader, instance / "shaderpacks" / shader)
        (instance / "config/iris.properties").write_text("enableShaders=true\nshaderPack="+shader+"\n", encoding="utf-8")
    if name == "B" and os.environ.get("SAKURA_SKIN_FAST") == "false":
        (instance / "config").mkdir(exist_ok=True)
        (instance / "config/skinlayers.json").write_text('{"fastRender":false}', encoding="utf-8")
    (instance / "options.txt").write_text(
        "resourcePacks:" + json.dumps(packs) + "\n"
        "lang:en_us\npauseOnLostFocus:false\nrenderDistance:3\nsimulationDistance:5\n"
        "tutorialStep:none\nskipMultiplayerWarning:true\nfullscreen:false\nenableVsync:false\nmaxFps:35\n",
        encoding="utf-8",
    )


def hide_validation_windows():
    pids = {children[k].pid for k in ("A", "B") if k in children}
    user32 = ctypes.windll.user32
    callback_type = ctypes.WINFUNCTYPE(wintypes.BOOL, wintypes.HWND, wintypes.LPARAM)

    @callback_type
    def hide(hwnd, _):
        pid = wintypes.DWORD()
        user32.GetWindowThreadProcessId(hwnd, ctypes.byref(pid))
        if pid.value in pids and user32.IsWindowVisible(hwnd):
            user32.ShowWindowAsync(hwnd, 0)
        return True

    user32.EnumWindows(hide, 0)


try:
    server = run / "server"
    server.mkdir()
    (server / "eula.txt").write_text("eula=true\n")
    (server / "server.properties").write_text(
        f"online-mode=false\nserver-ip=127.0.0.1\nserver-port={port}\n"
        "level-type=minecraft:flat\nview-distance=4\nsimulation-distance=5\n"
        "generate-structures=false\nmax-players=2\nmax-tick-time=-1\nwhite-list=false\n"
    )
    launch("server", "Server", ["nogui"], [])
    for _ in range(160):
        if (run / "server-ready").is_file():
            break
        if children["server"].poll() is not None:
            raise RuntimeError("Dedicated server exited before start")
        time.sleep(.25)
    else:
        raise RuntimeError("Dedicated server did not start")
    print("Dedicated server reached SERVER_STARTED", flush=True)

    for name in ("A", "B"):
        prepare_client(name)
        extras = [game / "mods" / mod for mod in ("punchy-3.2-fabric-26.3.jar", "punchy-moves-1.0.8-fabric-26.3.jar")] if name == "B" else []
        if name == "B" and os.environ.get("SAKURA_SKINLAYERS", "true") == "true":
            extras.append(game / "mods/skinlayers3d-fabric-1.11.4-mc26.3.jar")
        if name == "B" and os.environ.get("SAKURA_IRIS") == "true":
            extras += [game / "mods" / name for name in ("iris-fabric-1.11.7+mc26.3.jar", "sodium-fabric-0.9.2+mc26.3.jar")]
        launch(name, "Client", ["--username", "Sakura" + name, "--width", "900", "--height", "560"], extras)
    deadline = time.monotonic() + 240
    reported = set()
    while time.monotonic() < deadline:
        hide_validation_windows()
        for key in ("A-result", "B-result", "server-result"):
            path = run / key
            if path.is_file() and key not in reported:
                print(key, path.read_text(encoding="utf-8"), flush=True)
                reported.add(key)
        if len(reported) == 3:
            if any(not (run / key).read_text(encoding="utf-8").startswith("PASS") for key in reported):
                raise RuntimeError("Regression assertion failed")
            break
        crashed = [k for k in ("A", "B") if children[k].poll() is not None and not (run / (k + "-result")).exists()]
        if crashed:
            raise RuntimeError(f"Client exited before writing a result: {crashed}")
        time.sleep(.25)
    else:
        raise RuntimeError("Timed out waiting for both client results")
    for process in children.values():
        process.wait(timeout=20)
except Exception:
    for k in ("server", "A", "B"):
        log_path = run / k / "process.log"
        if log_path.is_file():
            lines = log_path.read_text(encoding="utf-8", errors="replace").splitlines()
            print(f"--- {k} log tail ---")
            print("\n".join(lines[-32:]))
    raise
finally:
    for process in children.values():
        if process.poll() is None:
            process.terminate()
    print("Validation directory", run, flush=True)
