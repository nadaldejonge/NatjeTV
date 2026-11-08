from pathlib import Path
import json,zipfile,io,hashlib
ROOT=Path(__file__).resolve().parents[1]
output=ROOT/'app/src/main/assets/licenses/dependencies'
output.mkdir(parents=True,exist_ok=True)
dependencies=json.loads((ROOT/'docs/DEPENDENCIES.json').read_text())
def notices(archive,prefix):
    names=[]
    with zipfile.ZipFile(archive) as z:
        for name in z.namelist():
            filename=name.rsplit('/',1)[-1]
            if filename.upper().startswith(('LICENSE','NOTICE','COPYING')) and not name.endswith('/'):
                dest=output/(prefix+'__'+filename.replace(':','_'))
                dest.write_bytes(z.read(name)); names.append(dest.relative_to(ROOT).as_posix())
        if 'classes.jar' in z.namelist(): names+=notices(io.BytesIO(z.read('classes.jar')),prefix+'__classes')
    return names
for d in dependencies:
    p=Path(d['file']); d['sha256']=hashlib.file_digest(p.open('rb'),'sha256').hexdigest()
    d['license']='Apache-2.0'
    d['bundledNotices']=notices(p,d['group']+'__'+d['name'])
    # No developer-specific cache paths in public dependency manifest.
    d['file']=p.name
(ROOT/'docs/DEPENDENCIES.json').write_text(json.dumps(dependencies,indent=2),encoding='utf-8')
print('Dependency inventory and original notices:',len(dependencies))
