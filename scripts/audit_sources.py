"""Read-only public source audit; never reads account credentials."""
import concurrent.futures, json, pathlib, urllib.request, datetime
ROOT = pathlib.Path(__file__).resolve().parents[1]
OUT = ROOT / 'research'
OUT.mkdir(exist_ok=True)
REPOS = ['nadaldejonge/NatjeTube','nadaldejonge/NatjeTube-SharedModules','nadaldejonge/NatjeTube-MediaServiceCore','pengchujin/aerial-tv','kierandrewett/vista','mcssc25/LumaLauncher','AazainKhan/glass-tv-launcher','B0redDev/glass-launcher','leanbitlab-org/LtvLauncher','MadeinTaly/midnightbeam','ismael6499/omnitv','eslavnov/pylips']
def get(url):
    req=urllib.request.Request(url, headers={'User-Agent':'NatjeTV-source-audit','Accept':'application/vnd.github+json'})
    with urllib.request.urlopen(req, timeout=35) as r: return r.read()
def audit(repo):
    d=OUT / repo.replace('/','__'); d.mkdir(exist_ok=True)
    record={'repo':repo,'observed_at':datetime.datetime.now(datetime.timezone.utc).isoformat()}
    try:
        meta=json.loads(get('https://api.github.com/repos/'+repo))
        record.update(default_branch=meta['default_branch'],license=meta.get('license'),updated_at=meta['updated_at'])
        head=json.loads(get('https://api.github.com/repos/'+repo+'/commits/'+meta['default_branch']))
        record.update(commit=head['sha'],commit_date=head['commit']['committer']['date'],message=head['commit']['message'])
        tree=json.loads(get('https://api.github.com/repos/'+repo+'/git/trees/'+head['sha']+'?recursive=1'))
        paths=[x['path'] for x in tree.get('tree',[]) if x['type']=='blob']
        (d/'tree.json').write_text(json.dumps(paths,indent=2),encoding='utf-8')
        chosen=[p for p in paths if '/' not in p and any(k in p.lower() for k in ['license','licence','notice','readme','build.gradle'])]
        chosen += [p for p in paths if p.endswith(('app/build.gradle','app/build.gradle.kts','libs.versions.toml','app/src/main/AndroidManifest.xml','tools/install.sh'))]
        if 'NatjeTube' in repo and repo.endswith('NatjeTube'): chosen += [p for p in paths if 'NatjeNight' in p or p.endswith('natje_night_mode.xml') or p.endswith('smarttubetv/build.gradle')]
        else:
            candidates=[p for p in paths if p.endswith(('.kt','.java')) and any(k in p.lower() for k in ['overlay','night','apprepository','appsrepository','mainactivity','screenfilter','accessibility','schedule'])]
            chosen += candidates[:10]
        record['files']=[]
        for p in dict.fromkeys(chosen):
            try:
                data=get('https://raw.githubusercontent.com/'+repo+'/'+head['sha']+'/'+p)
                dest=d/p; dest.parent.mkdir(parents=True,exist_ok=True); dest.write_bytes(data); record['files'].append(p)
            except Exception as e: record.setdefault('file_errors',[]).append([p,str(e)])
        if repo=='nadaldejonge/NatjeTube':
            for endpoint in ['releases','tags','branches']:
                try: (d/(endpoint+'.json')).write_bytes(get('https://api.github.com/repos/'+repo+'/'+endpoint+'?per_page=20'))
                except Exception as e: record[endpoint+'_error']=str(e)
    except Exception as e: record['error']=str(e)
    (d/'audit.json').write_text(json.dumps(record,indent=2),encoding='utf-8')
    return {k:record.get(k) for k in ['repo','commit','license','error']}
if __name__=='__main__':
    with concurrent.futures.ThreadPoolExecutor(max_workers=4) as pool:
        results=list(pool.map(audit,REPOS))
    (OUT/'summary.json').write_text(json.dumps(results,indent=2),encoding='utf-8')
    print(json.dumps(results,indent=2))
