from pathlib import Path
import json, urllib.request, hashlib, concurrent.futures
ROOT=Path(__file__).resolve().parents[1]
RESEARCH=ROOT/'research'
def get(url):
    return urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'NatjeTV-source-audit'}),timeout=60).read()
def fetch(job):
    directory,path=job
    a=json.loads((RESEARCH/directory/'audit.json').read_text())
    target=RESEARCH/directory/path
    data=get('https://raw.githubusercontent.com/'+a['repo']+'/'+a['commit']+'/'+path)
    target.parent.mkdir(parents=True,exist_ok=True); target.write_bytes(data)
    return [directory,path,len(data)]
jobs=[('MadeinTaly__midnightbeam',p) for p in ['app/src/dev/midnightbeam/DimService.java','app/src/dev/midnightbeam/ScheduleMath.java','build.sh']]
tree=json.loads((RESEARCH/'ismael6499__omnitv/tree.json').read_text())
services=[p for p in tree if p.endswith('.java') and any(k in p.lower() for k in ['service','filter'])]
print('OmniTV services:',services)
jobs += [('ismael6499__omnitv',p) for p in services if any(k in p.lower() for k in ['accessibility','screenfilter','overlayservice'])][:5]
with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
    for r in pool.map(fetch,jobs): print(r)
directory=RESEARCH/'nadaldejonge__NatjeTube'
patch=get('https://api.github.com/repos/nadaldejonge/NatjeTube/commits/a061876e2c8167a4873422754ab448a2a1fa65ea')
(directory/'night_menu_patch.json').write_bytes(patch)
releases=json.loads((directory/'releases.json').read_text())
latest=releases[0]
asset=next(a for a in latest['assets'] if a['name'].endswith('.apk'))
dest=ROOT/'artifacts/companions'/asset['name']; dest.parent.mkdir(parents=True,exist_ok=True)
data=get(asset['browser_download_url']); actual=hashlib.sha256(data).hexdigest()
assert 'sha256:'+actual==asset['digest']
dest.write_bytes(data)
(directory/'download_verification.json').write_text(json.dumps({'url':asset['browser_download_url'],'bytes':len(data),'sha256':actual,'verified_against_github_digest':True},indent=2))
print('Verified release APK',dest,len(data),actual)
