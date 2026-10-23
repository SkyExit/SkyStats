#!/usr/bin/env python3
"""Download verified official test servers and isolated Java runtimes."""
import argparse
import hashlib
import json
import pathlib
import shutil
import tarfile
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
TOOLS = ROOT / ".tools"
SERVERS = ROOT / ".test-servers"
AGENT = "SkyStats-development/2.0 (local compatibility testing)"


def fetch(url):
    with urllib.request.urlopen(urllib.request.Request(url, headers={"User-Agent": AGENT}), timeout=120) as response:
        return response.read()


def download(url, target, checksum=None):
    if target.exists() and (not checksum or hashlib.sha256(target.read_bytes()).hexdigest() == checksum):
        return
    print(f"Downloading {target.name}", flush=True)
    data = fetch(url)
    if checksum and hashlib.sha256(data).hexdigest() != checksum:
        raise RuntimeError(f"Checksum mismatch: {target}")
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes(data)


def java(version):
    target = TOOLS / f"java-{version}"
    if (target / "bin/java").exists():
        return target
    assets = json.loads(fetch(f"https://api.adoptium.net/v3/assets/latest/{version}/hotspot?architecture=x64&image_type=jdk&os=linux"))
    package = assets[0]["binary"]["package"]
    archive = TOOLS / f"java-{version}.tar.gz"
    download(package["link"], archive, package["checksum"])
    unpack = TOOLS / f"unpack-java-{version}"
    unpack.mkdir(exist_ok=True)
    with tarfile.open(archive) as tar:
        tar.extractall(unpack, filter="data")
    shutil.move(str(next(unpack.iterdir())), target)
    unpack.rmdir()
    return target


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--accept-eula", action="store_true", help="Accept https://www.minecraft.net/eula for local test instances")
    parser.add_argument("--include-paper-1.18", dest="include_paper_118", action="store_true", help="Also prepare the first supported 1.18 release")
    args = parser.parse_args()
    TOOLS.mkdir(exist_ok=True)
    instances = [("paper", "1.18.2", 17, 25568), ("paper", "26.3", 25, 25565), ("folia", "26.3", 25, 25566)]
    if args.include_paper_118:
        instances.append(("paper", "1.18", 17, 25569))
    for project, version, jdk, port in instances:
        runtime = java(jdk)
        path = SERVERS / f"{project}-{version}"
        path.mkdir(parents=True, exist_ok=True)
        builds = json.loads(fetch(f"https://fill.papermc.io/v3/projects/{project}/versions/{version}/builds"))
        build = builds[0]
        artifact = build["downloads"]["server:default"]
        download(artifact["url"], path / "server.jar", artifact["checksums"]["sha256"])
        (path / "download.json").write_text(json.dumps(build, indent=2) + "\n")
        (path / "plugins").mkdir(exist_ok=True)
        properties = path / "server.properties"
        if not properties.exists():
            properties.write_text(f"server-ip=127.0.0.1\nserver-port={port}\nonline-mode=false\nwhite-list=false\nenforce-secure-profile=false\nview-distance=3\nsimulation-distance=3\nmax-players=8\nspawn-protection=0\nlevel-type=minecraft:flat\ngenerator-settings={{\"layers\":[{{\"block\":\"minecraft:bedrock\",\"height\":1}},{{\"block\":\"minecraft:dirt\",\"height\":2}},{{\"block\":\"minecraft:grass_block\",\"height\":1}}],\"biome\":\"minecraft:plains\"}}\ngenerate-structures=false\nmax-tick-time=120000\nmotd=SkyStats local {project} {version}\n")
        eula = path / "eula.txt"
        if args.accept_eula or not eula.exists():
            eula.write_text("# https://www.minecraft.net/eula\neula=" + str(args.accept_eula).lower() + "\n")
        launcher = path / "start.sh"
        launcher.write_text(f'#!/usr/bin/env bash\nset -euo pipefail\ncd -- "$(dirname -- "${{BASH_SOURCE[0]}}")"\nexec "{runtime}/bin/java" -Dterminal.jline=false -Dterminal.ansi=false -Xms512M -Xmx2G -XX:ActiveProcessorCount=4 -jar server.jar --nogui "$@"\n')
        launcher.chmod(0o755)
        print(f"Ready: {path.relative_to(ROOT)} (localhost:{port})", flush=True)


if __name__ == "__main__":
    main()
