"""Transfer the tested APK by delta; verify both APKs before touching the draft."""

import argparse
import gzip
import hashlib
import json
import os
from pathlib import Path
import subprocess
import tempfile
import urllib.parse
import urllib.request

REPO = "HuyTec/CNPM-Multilingual-Audio-Guide-2026-2027"
BASE_SHA = "8cce42859d7563872d64203922a4b3190176897503bd92c56524b9b8f893428b"
APK_SHA = "e477200e06771bb5544a4592503df05a2d7cde9bbbec25169bbc4eba9eaec04e"
SOURCE_SHA = "9731cbe2aebcef472255ccb1beb11936497072fa"
APK_SIZE = 59609491
ASSET_NAME = "TravelVoice-verr2-demo.apk"
PATCH = Path(__file__).with_name("verr2-apk.patch.gz").resolve()


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def reconstruct(base, work):
    """Apply the binary delta and prove the result is the locally tested APK."""
    if digest(base) != BASE_SHA:
        raise RuntimeError("Base APK checksum mismatch")
    apk = work / "TravelVoice.apk"
    if base != apk:
        apk.write_bytes(base.read_bytes())
    patch = work / "verr2-apk.patch"
    patch.write_bytes(gzip.decompress(PATCH.read_bytes()))
    # Keep apply independent of an enclosing checkout during local verification.
    git_env = {**os.environ, "GIT_CEILING_DIRECTORIES": str(work.parent)}
    subprocess.run(["git", "apply", "--check", str(patch)], cwd=work, env=git_env, check=True)
    subprocess.run(["git", "apply", str(patch)], cwd=work, env=git_env, check=True)
    if apk.stat().st_size != APK_SIZE or digest(apk) != APK_SHA:
        raise RuntimeError("Reconstructed APK checksum mismatch")
    print(f"Verified tested APK: {APK_SIZE} bytes; sha256:{APK_SHA}", flush=True)
    return apk


def upload(apk):
    """Upload only to the matching draft; never replace published assets."""
    headers = {
        "Authorization": "Bearer " + os.environ["GH_TOKEN"],
        "Accept": "application/vnd.github+json",
        "User-Agent": "TravelVoice-verr2-verified-transfer",
    }
    url = f"https://api.github.com/repos/{REPO}/releases/416914412"
    with urllib.request.urlopen(urllib.request.Request(url, headers=headers), timeout=30) as response:
        release = json.load(response)
    if release["tag_name"] != "verr2" or release["target_commitish"] != SOURCE_SHA:
        raise RuntimeError("Draft does not match the tested source")
    existing = next((item for item in release["assets"] if item["name"] == ASSET_NAME), None)
    if existing:
        if existing["state"] == "uploaded" and existing["size"] == APK_SIZE and existing.get("digest") == "sha256:" + APK_SHA:
            print("Matching APK already uploaded", flush=True)
            return
        raise RuntimeError("Existing APK differs; refusing to replace it")
    if not release["draft"]:
        raise RuntimeError("Release is already public; refusing to modify it")
    endpoint = release["upload_url"].split("{", 1)[0]
    if urllib.parse.urlparse(endpoint).hostname != "uploads.github.com":
        raise RuntimeError("Unexpected upload host")
    endpoint += "?name=" + urllib.parse.quote(ASSET_NAME)
    headers["Content-Type"] = "application/vnd.android.package-archive"
    request = urllib.request.Request(endpoint, data=apk.read_bytes(), headers=headers, method="POST")
    with urllib.request.urlopen(request, timeout=180) as response:
        asset = json.load(response)
    if asset["state"] != "uploaded" or asset["size"] != APK_SIZE or asset.get("digest") != "sha256:" + APK_SHA:
        raise RuntimeError("GitHub asset checksum mismatch")
    print(f"Uploaded verified APK to draft verr2: {asset['browser_download_url']}", flush=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-apk", type=Path)
    parser.add_argument("--verify-only", action="store_true")
    args = parser.parse_args()
    with tempfile.TemporaryDirectory(prefix="verr2-") as temporary:
        work = Path(temporary)
        base = args.base_apk.resolve() if args.base_apk else work / "base.apk"
        if not args.base_apk:
            source = f"https://github.com/{REPO}/releases/download/v0.1.0-demo/TravelVoice-demo-v0.1.0.apk"
            urllib.request.urlretrieve(source, base)
        apk = reconstruct(base, work)
        if not args.verify_only:
            upload(apk)


if __name__ == "__main__":
    main()
