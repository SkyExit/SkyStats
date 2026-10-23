#!/usr/bin/env python3
"""Run SkyStats against real Paper/Folia with disposable Vanilla-stat fixtures."""
import argparse
import hashlib
import json
import os
import pathlib
import shutil
import subprocess
import sys
import time
import uuid
import zipfile
import xml.etree.ElementTree as ET

ROOT = pathlib.Path(__file__).resolve().parents[1]
VERSION = ET.parse(ROOT / "pom.xml").getroot().find("{http://maven.apache.org/POM/4.0.0}version").text


def offline_uuid(name):
    data = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
    data[6] = (data[6] & 15) | 48
    data[8] = (data[8] & 63) | 128
    return str(uuid.UUID(bytes=bytes(data)))


def probe_jar():
    classes = ROOT / "target/probe-classes"
    classes.mkdir(parents=True, exist_ok=True)
    jars = [file for file in (ROOT / ".tools/m2").glob("**/*.jar")
            if not file.name.endswith(("-sources.jar", "-javadoc.jar"))] + [ROOT / "target/classes"]
    classpath = os.pathsep.join(map(str, jars))
    # An argument file also works with a populated cache beyond Windows' command limit.
    arguments = ROOT / "target/probe-javac.args"
    quote = lambda value: '"' + str(value).replace("\\", "/").replace('"', '\\"') + '"'
    arguments.write_text("--release 17\n-encoding UTF-8\n-cp " + quote(classpath)
                         + "\n-d " + quote(classes) + "\n" + quote(ROOT / "tests/integration/ProbePlugin.java"), encoding="utf-8")
    subprocess.run(["javac", "@" + str(arguments)], check=True)
    target = ROOT / "target/SkyStatsProbe.jar"
    with zipfile.ZipFile(target, "w") as jar:
        jar.writestr("plugin.yml", "name: SkyStatsProbe\nversion: '1'\nmain: de.skystats.probe.ProbePlugin\napi-version: '1.18'\ndepend: [SkyStats]\nfolia-supported: true\n")
        for file in classes.rglob("*.class"):
            jar.write(file, file.relative_to(classes))
    return target


def fixtures(server, version):
    directory = server / ("world/players/stats" if version == "26.3" else "world/stats")
    directory.mkdir(parents=True, exist_ok=True)
    cache = []
    players = {"OfflineA": 1000, "OfflineB": 1000, "OfflineC": 500, "StatsTester": 5}
    players.update({f"Medal{i:02}": 0 for i in range(55)})
    for index, (name, stone) in enumerate(players.items()):
        ident = offline_uuid(name)
        cache.append({"uuid": ident, "name": name, "expiresOn": "2030-01-01 00:00:00 +0000"})
        stats = {"minecraft:mined": {"minecraft:stone": stone}, "minecraft:custom": {f"minecraft:probe_{index}": 1}}
        if name == "OfflineA":
            stats["minecraft:mined"].update({f"minecraft:probe_block_{i}": 1 for i in range(50)})
        (directory / f"{ident}.json").write_text(json.dumps({"stats": stats, "DataVersion": 2975}))
    (directory / f"{uuid.uuid4()}.json").write_text('{"stats":')
    (server / "usercache.json").write_text(json.dumps(cache))
    whitelist = server / "whitelist.json"
    allowed = json.loads(whitelist.read_text()) if whitelist.exists() else []
    if not any(entry.get("uuid") == offline_uuid("StatsTester") for entry in allowed):
        allowed.append({"uuid": offline_uuid("StatsTester"), "name": "StatsTester"})
        whitelist.write_text(json.dumps(allowed))
    config = server / "plugins/SkyStats/config.yml"
    config.parent.mkdir(parents=True, exist_ok=True)
    config.write_text("stats-directory: ''\ncache-seconds: 1\nrefresh-cooldown-seconds: 1\nlive-reads-per-tick: 400\n", encoding="utf-8")


def run_server(name, probe, server_root, java):
    if name not in {"paper-1.18", "paper-1.18.2", "paper-26.3", "folia-26.3"}:
        raise ValueError("Use one of the disposable instances created by setup-test-servers.py")
    version = name.split("-", 1)[1]
    server = server_root / name
    if not server.exists() and version == "26.3":
        server = server_root / name.split("-", 1)[0]
    if (server / "eula.txt").read_text().splitlines()[-1] != "eula=true":
        raise RuntimeError("Minecraft EULA must be accepted explicitly before integration tests")
    fixtures(server, version)
    (server / "probe-result.txt").unlink(missing_ok=True)
    (server / "network-ready.txt").unlink(missing_ok=True)
    shutil.copy2(probe, server / "plugins/SkyStatsProbe.jar")
    shutil.copy2(ROOT / f"target/SkyStats-{VERSION}.jar", server / "plugins/SkyStats.jar")
    executable = "java.exe" if os.name == "nt" else "java"
    jdk = java or ROOT / ".tools" / ("java-17" if "1.18" in name else "java-25") / "bin" / executable
    log = (server / "integration-console.log").open("w", encoding="utf-8")
    process = subprocess.Popen([str(jdk), "-Dterminal.jline=false", "-Dterminal.ansi=false", "-Xms512M", "-Xmx2G", "-XX:ActiveProcessorCount=4", "-jar", "server.jar", "--nogui"], cwd=server, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT, text=True, encoding="utf-8")
    client = None
    try:
        deadline = time.monotonic() + 120
        while time.monotonic() < deadline:
            if process.poll() is not None:
                raise RuntimeError(f"{name} exited: {process.returncode}")
            if "Done (" in (server / "integration-console.log").read_text(errors="replace"):
                break
            time.sleep(0.5)
        else:
            raise RuntimeError(f"{name} did not start")
        properties = dict(line.split("=", 1) for line in (server / "server.properties").read_text().splitlines() if "=" in line and not line.startswith("#"))
        port = int(properties["server-port"])
        client_log = (server / "integration-client.log").open("w")
        command = [sys.executable, str(ROOT / "tests/integration/client.py"), str(port), version, str(server)]
        client = subprocess.Popen(command, cwd=ROOT, stdout=client_log, stderr=subprocess.STDOUT)
        deadline = time.monotonic() + 150
        result = server / "probe-result.txt"
        while not result.exists() and time.monotonic() < deadline:
            if client.poll() is not None:
                raise RuntimeError(f"{name}: client disconnected: " + (server / "integration-client.log").read_text()[-3000:])
            time.sleep(0.5)
        if not result.exists():
            raise RuntimeError(f"{name}: probe timed out; inspect integration-console.log")
        report = result.read_text()
        print(f"{name}: {report}", flush=True)
        if not report.splitlines() or report.splitlines()[0] != "PASS":
            raise RuntimeError(f"{name}: integration assertions failed")
        return report
    finally:
        if client is not None:
            client.terminate()
            client.wait(timeout=10)
        if process.poll() is None:
            process.stdin.write("stop\n")
            process.stdin.flush()
            try:
                process.wait(timeout=40)
            except subprocess.TimeoutExpired:
                process.terminate()
                process.wait(timeout=15)
        log.close()
        (server / "plugins/SkyStatsProbe.jar").unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("servers", nargs="*", default=["paper-1.18.2", "paper-26.3", "folia-26.3"])
    parser.add_argument("--server-root", type=pathlib.Path, default=ROOT / ".test-servers")
    parser.add_argument("--java", type=pathlib.Path, help="Use an installed Java runtime")
    args = parser.parse_args()
    probe = probe_jar()
    reports = [run_server(name, probe, args.server_root.resolve(), args.java) for name in args.servers]
    (ROOT / "target/integration-results.txt").write_text("\n".join(reports))


if __name__ == "__main__":
    main()
