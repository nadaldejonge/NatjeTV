"""Bounded local APK inventory. No media playlists, user settings or credentials are read."""
from pathlib import Path
import subprocess,hashlib,json,re,shutil
ROOT=Path(__file__).resolve().parents[1]
DOWNLOADS=ROOT.parent.parent
TOOLS=Path.home()/'AppData/Local/Android/Sdk/build-tools/36.0.0'
roots=[p for p in DOWNLOADS.iterdir() if p.is_dir() and (p.name=='Player' or p.name.startswith(('Player-backup-','Player-branding-backup-','Player-recovery-backup-')))]
records=[]
for root in roots:
    for directory in [root/'artifacts/android-tv',root/'android/app/build/outputs/apk/debug']:
        for apk in directory.glob('*.apk'):
            output=subprocess.check_output([str(TOOLS/'aapt.exe'),'dump','badging',str(apk)],text=True,encoding='utf-8',errors='replace')
            first=output.splitlines()[0]
            def field(name):
                m=re.search(name+"='([^']*)'",first)
                return m.group(1) if m else None
            records.append({'file':str(apk),'bytes':apk.stat().st_size,'modified':apk.stat().st_mtime,'package':field('name'),'versionCode':int(field('versionCode')),'versionName':field('versionName'),'sha256':hashlib.file_digest(apk.open('rb'),'sha256').hexdigest()})
records.sort(key=lambda x:(x['versionCode'],x['modified']),reverse=True)
(ROOT/'research/iptv_inventory.json').write_text(json.dumps({'searched':[str(p) for p in roots],'candidates':records},indent=2),encoding='utf-8')
latest=[r for r in records if r['package']=='nl.natje.iptvplayer' and r['versionCode']==max(x['versionCode'] for x in records)]
hashes=set(r['sha256'] for r in latest)
if len(hashes)!=1: raise RuntimeError('Multiple different APKs at newest versionCode: manual comparison required')
selected=next((r for r in latest if '0.1.11-recovery-debug.apk' in r['file']),latest[0])
print('Newest consistent candidate:',json.dumps(selected,indent=2))
signature=subprocess.run([str(TOOLS/'apksigner.bat'),'verify','--print-certs',selected['file']],capture_output=True,text=True,encoding='utf-8',errors='replace')
(ROOT/'logs/iptv-apksigner.log').write_text(signature.stdout+signature.stderr,encoding='utf-8')
assert signature.returncode==0
print('Signature verification passed; candidates:',len(records))
