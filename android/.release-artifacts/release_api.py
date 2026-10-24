import os,subprocess,json,urllib.request,urllib.parse,http.client,hashlib,time,sys
from pathlib import Path
ROOT=Path(__file__).resolve().parent
REPO="HuyTec/CNPM-Multilingual-Audio-Guide-2026-2027"
os.environ["GIT_TERMINAL_PROMPT"]="0";os.environ["GCM_INTERACTIVE"]="never"
r=subprocess.run(["git","credential","fill"],input="protocol=https\nhost=github.com\n\n",capture_output=True,text=True,check=True)
credential=dict(line.split("=",1) for line in r.stdout.splitlines() if "=" in line)
HEADERS={"Authorization":"Bearer "+credential["password"],"Accept":"application/vnd.github+json","X-GitHub-Api-Version":"2026-03-10"}
state=json.loads((ROOT/"release-state.json").read_text())
def api(path,method="GET",body=None):
 request=urllib.request.Request("https://api.github.com/repos/"+REPO+path,data=None if body is None else json.dumps(body).encode(),headers=HEADERS,method=method)
 with urllib.request.urlopen(request,timeout=60) as response:
  return None if response.status==204 else json.load(response)
mode=sys.argv[1]
release=api("/releases/"+str(state["id"]))
if mode in ("upload","replace-draft-assets"):
 if not release["draft"]:raise SystemExit("Upload guard: release is not a draft.")
 if release["tag_name"]!="v1.0.0" or release["id"]!=420192459:raise SystemExit("Upload guard: unexpected release.")
 for name in sys.argv[2:]:
  path=ROOT/name;size=path.stat().st_size;digest="sha256:"+hashlib.sha256(path.read_bytes()).hexdigest()
  old=next((a for a in api("/releases/"+str(release["id"])+"/assets") if a["name"]==name),None)
  if old:
   if old.get("digest")==digest and old["size"]==size:print("Already verified:",name,flush=True);continue
   if mode!="replace-draft-assets":raise SystemExit("Existing asset differs; inspect before retrying: "+name)
   if name not in ("TravelQ1-v1.0.0.apk","SHA256SUMS.txt"):raise SystemExit("Replacement is limited to the rebuilt APK and its checksums.")
   api("/releases/assets/"+str(old["id"]),"DELETE")
   print("Removed outdated draft asset:",name,flush=True)
  url=urllib.parse.urlsplit(state["upload_url"]+"?name="+urllib.parse.quote(name))
  conn=http.client.HTTPSConnection(url.hostname,timeout=120)
  conn.putrequest("POST",url.path+"?"+url.query)
  for key,value in HEADERS.items():conn.putheader(key,value)
  conn.putheader("Content-Length",str(size));conn.putheader("Content-Type","application/vnd.android.package-archive" if name.endswith(".apk") else "application/octet-stream");conn.endheaders()
  sent=0;last=0;started=time.monotonic();print("Uploading",name,size,"bytes",flush=True)
  with path.open("rb") as file:
   while chunk:=file.read(128*1024):
    conn.send(chunk);sent+=len(chunk)
    if sent-last>=1024*1024 or sent==size:
     print(f"{name}: {sent}/{size} bytes ({time.monotonic()-started:.1f}s)",flush=True);last=sent
  response=conn.getresponse();raw=response.read();conn.close()
  if response.status!=201:raise SystemExit("Upload failed with HTTP "+str(response.status))
  asset=json.loads(raw)
  if asset.get("digest")!=digest or asset["size"]!=size:raise SystemExit("Uploaded digest/size mismatch")
  print("Verified upload:",name,digest,flush=True)
elif mode=="update-draft":
 if not release["draft"] or release["id"]!=420192459 or release["tag_name"] not in ("v1.0.0","untagged-5ea994776d0e83949349"):raise SystemExit("Draft update guard failed.")
 source=subprocess.run(["git","rev-parse","HEAD"],capture_output=True,text=True,check=True).stdout.strip()
 remote=api("/commits/HVP-22-travelq1-ux-preview")
 if remote["sha"]!=source:raise SystemExit("Draft source is not the verified remote branch head.")
 release=api("/releases/"+str(release["id"]),"PATCH",{"tag_name":"v1.0.0","target_commitish":source,"body":(ROOT/"RELEASE_NOTES.md").read_text(encoding="utf-8")})
 if not release["draft"] or release["target_commitish"]!=source or release["tag_name"]!="v1.0.0":raise SystemExit("Draft source update verification failed.")
 state["sourceCommit"]=source
 (ROOT/"release-state.json").write_text(json.dumps(state,indent=2),encoding="utf-8")
 print(json.dumps({"draft":release["draft"],"sourceCommit":source,"url":release["html_url"]}),flush=True)
elif mode=="publish":
 required=["TravelQ1-v1.0.0.apk","SHA256SUMS.txt","TravelQ1-v1.0.0-QUICKSTART.md","TravelQ1-release-certificate.txt"]
 assets=api("/releases/"+str(release["id"])+"/assets")
 for name in required:
  expected="sha256:"+hashlib.sha256((ROOT/name).read_bytes()).hexdigest()
  asset=next((a for a in assets if a["name"]==name),None)
  if not asset or asset.get("digest")!=expected or asset["state"]!="uploaded":raise SystemExit("Publish guard: asset missing or differs: "+name)
 if release["target_commitish"]!=state["sourceCommit"]:raise SystemExit("Publish guard: source commit differs.")
 release=api("/releases/"+str(release["id"]),"PATCH",{"tag_name":"v1.0.0","target_commitish":state["sourceCommit"],"body":(ROOT/"RELEASE_NOTES.md").read_text(encoding="utf-8"),"draft":False,"prerelease":False,"make_latest":"true"})
 print(json.dumps({"url":release["html_url"],"draft":release["draft"],"prerelease":release["prerelease"],"assets":[{"name":a["name"],"digest":a.get("digest"),"size":a["size"],"url":a["browser_download_url"]} for a in release["assets"]]},ensure_ascii=False),flush=True)
elif mode=="inspect":print(json.dumps({"url":release["html_url"],"draft":release["draft"],"prerelease":release["prerelease"],"assets":[{"name":a["name"],"digest":a.get("digest"),"size":a["size"]} for a in release["assets"]]},ensure_ascii=False))
else:raise SystemExit("Unknown mode")
