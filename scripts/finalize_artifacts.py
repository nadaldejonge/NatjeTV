"""Historical phase-1 packager for 1.0.0; not the current release workflow."""
from pathlib import Path
import subprocess,hashlib,json,shutil,xml.etree.ElementTree as ET,zipfile,re,datetime
ROOT=Path(__file__).resolve().parents[1]
ART=ROOT/'artifacts'; LOG=ROOT/'logs'; DOC=ROOT/'docs'
TOOLS=Path.home()/'AppData/Local/Android/Sdk/build-tools/36.0.0'
assert 'BUILD SUCCESSFUL' in (LOG/'final-verification-build.log').read_text(encoding='utf-8-sig')
suites=[ET.parse(p).getroot() for p in (ROOT/'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml')]
assert suites and all(int(s.attrib['failures'])==0 and int(s.attrib['errors'])==0 for s in suites)
tests=sum(int(s.attrib['tests']) for s in suites)
lint=ET.parse(ROOT/'app/build/reports/lint-results-debug.xml').getroot()
assert not [i for i in lint.findall('issue') if i.get('severity') in ('Error','Fatal')]
warnings=len([i for i in lint.findall('issue') if i.get('severity')=='Warning'])
shutil.copy2(ROOT/'app/build/outputs/apk/debug/app-debug.apk',ART/'NatjeTV-1.0.0-debug.apk')
shutil.copy2(ROOT/'app/build/outputs/apk/release/app-release-unsigned.apk',ART/'NatjeTV-1.0.0-release-unsigned.apk')
entries=[]
for file in sorted(ART.glob('*.apk'))+sorted((ART/'companions').glob('*.apk')):
    result=subprocess.run([str(TOOLS/'aapt2.exe'),'dump','badging',str(file)],capture_output=True,text=True,encoding='utf-8',errors='replace')
    assert result.returncode==0, result.stderr
    (LOG/(file.stem+'-badging.log')).write_text(result.stdout+result.stderr,encoding='utf-8')
    header=next(line for line in result.stdout.splitlines() if line.startswith('package:'))
    def field(name): return re.search(name+"='([^']*)'",header).group(1)
    signed='unsigned' not in file.name
    if signed:
        result=subprocess.run([str(TOOLS/'apksigner.bat'),'verify','--print-certs',str(file)],capture_output=True,text=True,encoding='utf-8',errors='replace')
        assert result.returncode==0,result.stderr
        (LOG/(file.stem+'-signature.log')).write_text(result.stdout+result.stderr,encoding='utf-8')
    entries.append({'file':file.relative_to(ART).as_posix(),'bytes':file.stat().st_size,'sha256':hashlib.file_digest(file.open('rb'),'sha256').hexdigest(),'package':field('name'),'versionName':field('versionName'),'versionCode':int(field('versionCode')),'signed':signed})
assert all(e['package']=='nl.natje.tv' and e['versionCode']==10000 for e in entries if 'NatjeTV-' in e['file'])
assert any(e['package']=='nl.natje.natjetube' and e['versionCode']==2460 for e in entries)
assert any(e['package']=='nl.natje.iptvplayer' and e['versionCode']==11 for e in entries)
tree=subprocess.run([str(TOOLS/'aapt2.exe'),'dump','xmltree',str(ART/'NatjeTV-1.0.0-debug.apk'),'--file','AndroidManifest.xml'],capture_output=True,text=True,encoding='utf-8')
assert tree.returncode==0
assert all(x in tree.stdout for x in ['android.intent.category.HOME','android.intent.category.LEANBACK_LAUNCHER','nl.natje.tv','FOREGROUND_SERVICE_SPECIAL_USE'])
assert re.search(r'foregroundServiceType[^\n]*0x40000000',tree.stdout)
assert 'android.permission.INTERNET' not in tree.stdout
(LOG/'apk-manifest-tree.log').write_text(tree.stdout,encoding='utf-8')
(ART/'APK-MANIFEST.json').write_text(json.dumps(entries,indent=2),encoding='utf-8')
reports=ART/'reports'; reports.mkdir(exist_ok=True)
for p in (ROOT/'app/build/reports').glob('lint-results-debug.*'): shutil.copy2(p,reports/p.name)
shutil.copytree(ROOT/'app/build/reports/tests',reports/'tests',dirs_exist_ok=True)
shutil.copytree(ROOT/'app/build/test-results/testDebugUnitTest',reports/'junit',dirs_exist_ok=True)
source=ROOT.parent/'assets'
for directory in ['source','brand']:
    if (source/directory).exists(): shutil.copytree(source/directory,ROOT/'branding'/directory,dirs_exist_ok=True)
passed='GETEST EN GESLAAGD'; untested='NOG NIET GETEST'
rows=[
('Debug en unsigned release compilatie',passed,'Windows / Gradle; logs/final-verification-build.log'),
('APK-versie/package/HOME/Leanback/foregroundtype/geen internetpermissie',passed,'aapt2 op definitieve debug-APK; logs/apk-manifest-tree.log, *-badging.log'),
('Debug- en NatjeTube-signatuur + SHA256',passed,'apksigner en hashlib; logs/*-signature.log; artifacts/APK-MANIFEST.json'),
('Lint',passed,f'0 fouten, {warnings} waarschuwingen; artifacts/reports/lint-results-debug.html'),
('Unit-/Android-UI-tests',passed,f'{tests} tests, 0 failures/errors; artifacts/reports/junit en reports/tests'),
('Middernacht-/dagschema alle 1440 minuten, gelijke grenzen, handmatig aan/uit',passed,'CoreTest; JVM'),
('Warmte 0.60 i.p.v. 0.06 / dimbereik / daadwerkelijke canvaspixels',passed,'CoreTest + actualFilterDrawHasAmberAndDimming; Robolectric API34 native graphics'),
('Persistente instellingen/profielen/volgorde/status na nieuwe ConfigStore',passed,'persistentConfigurationSurvivesStoreRecreation; Robolectric, geen echte reboot'),
('Importgrootte/schema/ongeldige JSON/veilig uit blijven',passed,'importIsAtomicAndCannotEnableServices + malformedProfilesAndRangesAreHandled; Robolectric'),
('Dynamische TV- en sideload-appdetectie, deduplicatie, eigen app uitsluiten',passed,'installedTvAndSideloadedAppsAreDiscoveredAndDeduplicated; gesimuleerde PackageManager-inventaris'),
('Pakket/naam/herkenning, handmatige koppeling, ontbrekende app veilig',passed,'favoriteMatchingUsesInstalledAppsAndManualBinding + missingLaunchIntentDoesNotCrash; Robolectric'),
('Appwinkel: uitsluitend geïnstalleerde handler en juiste appnaam in zoekroute',passed,'marketOnlyOpensAnInstalledHandlerAndEncodesAppSearch; Robolectric met gesimuleerde Play-handler'),
('Home/settings/night-schermen renderen, focusbaar en langklik',passed,'homeAndSettingsRenderWithoutInstalledApps; artifacts/screenshots/*-robolectric.png'),
('Profiel maken via instellingsdialoog',passed,'profileCanBeCreatedFromRemoteAccessibleSettings; Robolectric'),
('Noodstop: 3 seconden hold en status persistent uit',passed,'remoteEmergencyHoldWorksWithoutConsumingKeys + emergencyPersistsOffWithoutPermission; Robolectric'),
('Geen overlay starten zonder expliciete toestemming',passed,'overlayRequiresExplicitPermission; Robolectric'),
('PowerShell-syntax en standaard dry-run zonder SDK/ADB',passed,'logs/script-verification.log; werkelijke lokale uitvoering uitsluitend dry-run'),
('NatjeTube nieuwste menu-/warmtepatch en releasebestand',passed,'GitHub HEAD/tag/release/diff; research/nadaldejonge__NatjeTube/ + APK-hash/signatuur'),
('Lokale IPTV-kandidaten, versiecode, broncode, signatuur en logo',passed,'research/iptv_inventory.json; docs/NATJETUBE_EN_IPTV_VERSIES.md; logs/iptv-apksigner.log'),
('Fysieke D-pad-navigatie, verplaatsen, favorieten, zoeken, echte apps starten',untested,'NOG NIET OP ECHTE STICK GETEST; lokale focus-/inventaristests bewijzen dit niet volledig'),
('Alle zes apps geïnstalleerd + echte Google Play Store open/installatie',untested,'Twee Natje-APK\'s volledig meegeleverd; overige vier streamingapps via winkelroute, geen binaries. Niets geïnstalleerd op Xiaomi'),
('Profielen hernoemen/wissen en documentpicker-import/export/artwork op firmware',untested,'Geïmplementeerd; geen volledige geautomatiseerde end-to-endtest'),
('Echte overlayvensters/foregroundservice boven andere processen',untested,'Geïmplementeerd; unitcanvas/toestemmingstests zijn geen WindowManager/DRM-hardwarebewijs'),
('5 seconden echte compositorovergang en rendement/batterij',untested,'ValueAnimator geïmplementeerd; meten in fase 2'),
('Night Mode na echte boot/procesdood/standby met Xiaomi-beleid',untested,'Bootreceiver + START_STICKY + opgeslagen toestand voorbereid; echte firmware niet benaderd'),
('Toegankelijkheidsbackend en echte firmware-combinaties',untested,'Service gedeclareerd, geen toestemming geactiveerd in fase 1'),
('Homescreen + NatjeTube + IPTV filter tijdens daadwerkelijke playback',untested,'NOG NIET OP ECHTE STICK GETEST'),
('Netflix-menu en daadwerkelijke DRM-playback',untested,'NOG NIET OP ECHTE STICK GETEST'),
('Videoland, Prime Video, NPO Start menu/playback',untested,'NOG NIET OP ECHTE STICK GETEST'),
('SDR, HDR10, Dolby Vision en dubbele filtering',untested,'NOG NIET OP ECHTE STICK GETEST'),
('HOME standaardrol, Home-knop, reboot/wake en herstel',untested,'Manifest en veilige scripts aanwezig; geen ADB/device-mutatie uitgevoerd'),
('Install-/update-/herstelscript op Android-firmware',untested,'Alleen syntax/dry-run getest. Uitvoering uitsluitend na fase-2-startsein'),
('Screensaver, rusttimer en boot-app op werkelijke firmware',untested,'Geïmplementeerd met beperkingen uit README; fysieke tv-uitstand niet aangeboden'),
('RAM/PSS, cold/warm starttijd en framejank op 2GB-stick',untested,'Geen cijfers verzonnen; meetprotocol in INSTALLATIE_FASE_2.md'),
('Philips Titan OS / PS5 HDMI',untested,'Uitsluitend niet-beschikbare provider/stub. Geen tv benaderd. Ambilight buiten scope'),
('Emulator/instrumentatie op echt Android-systeem',untested,'Niet gestart; uitsluitend JVM/Robolectric en native graphics. Geen ADB in fase 1')]
lines=['# Acceptatietests — fase 1','',f'8 november 2026. Definitieve lokale ronde: {tests} tests geslaagd, lint 0 fouten / {warnings} waarschuwingen. De drie PNG-schermen zijn werkelijke Android Views in Robolectric, geen ontwerp-mockups en geen emulator-/Xiaomi-bewijs. De PackageManager-inventaris in de tests is bewust gesimuleerd.','', '| Onderdeel | Status | Omgeving en concrete bewijsbron |','|---|---|---|']
lines += [f'| {name} | **{status}** | {evidence} |' for name,status,evidence in rows]
lines += ['', '## Eerdere iteraties','', 'Eerdere logs bevatten herstelde fouten: onjuiste volgorde van Robolectric-qualifiers, focuscontrole in touchmode, een nog niet afgehandeld OnShow-event in de profieltest, een API31-splashattribuut in de API26-basisstijl en een onjuist ge-escapet Windows-SDK-pad. De definitieve ronde hierboven is groen. Er is geen mislukte of ontbrekende hardwaretest als geslaagd aangemerkt.', '', '## Bekende grenzen','', 'Zes niet-blokkerende lintwaarschuwingen in deze toolchain (zie volledig rapport): gepinde oudere target/tool/testversies, synchroon opgeslagen bootmarker, vaste TV-landscape en toekomstige backupregels. Het doel is lokale sideload op API34; geen Play-publicatie. Android/Xiaomi kan overlays/achtergrondstart en inputrechten beperken. Het actieve filtervenster kan niet betrouwbaar vaststellen dat een andere app het boven secure playback verbergt. Touchscreenpassthrough kan door Android worden beperkt bij hoge gewone overlay-opacity; de TV-bediening is D-pad en vereist nog echte verificatie.']
(DOC/'ACCEPTATIETESTS.md').write_text('\n'.join(lines)+'\n',encoding='utf-8')
result=['# Resultaat fase 1','',f'Bronproject: `{ROOT}`. Android TV-launcher Natje TV 1.0.0, package `nl.natje.tv`, code 10000. {tests} lokale tests geslaagd, lint 0 fouten / {warnings} waarschuwingen. Geen ADB gestart, geen Xiaomi-/tv-toegang en geen HOME-/stock-launcherwijzigingen.','', '| APK | Package / versiecode | Bytes | SHA256 |','|---|---|---|---|']
for e in entries: result.append(f"| `artifacts/{e['file']}` | {e['package']} / {e['versionName']} / {e['versionCode']} | {e['bytes']} | `{e['sha256']}` |")
result += ['', 'Debug is lokaal ondertekend en klaar voor latere testinstallatie. Release is unsigned en moet vóór installatie worden ondertekend. Er zitten geen private sleutels in project of broncode-ZIP. De NatjeTube-companion is byte-geverifieerd tegen GitHub en bevat menucrashfix plus 60%-warmtepatch; hij is niet in de launcher ingebouwd.', '', 'Lokale IPTV: `C:/Users/nadal/Downloads/Player/artifacts/android-tv/Natje-IPTV-AndroidTV-0.1.11-recovery-debug.apk`, `nl.natje.iptvplayer`, code 11, SHA256 `24a9c2a7906cd43ba86f1828b1305dff4aff478f0bd355ecc3ad09dc5ee06398`. Volledige APK ook gekopieerd naar artifacts/companions. Geen IPTV-gegevens of playlists gelezen. Zie de volledige inhoudsvergelijking in de inventaris.', '', 'Alle zes apptegels plus Appwinkel zijn geïntegreerd. Twee Natje-apps volledig als losse APK meegeleverd; Netflix/Videoland/NPO Start/Prime Video krijgen een officiële winkelzoekroute en zijn niet als offline binaries gebundeld. Appwinkel opent de aanwezige Google Play-/market-handler. Zie docs/APPPAKKET.md.', '', 'Scripts: `scripts/build.ps1`, `verify.ps1`, `preflight.ps1`, `install-phase2.ps1`, `restore-original-launcher.ps1`, `verify-scripts.ps1`. Installatie/herstel zijn standaard dry-run en zijn niet tegen een apparaat uitgevoerd. `-IncludeNatjeApps` bereidt installatie van beide losse Natje-apps voor. Broncodebundel: `artifacts/NatjeTV-1.0.0-source.zip`. Compleet apppakket inclusief broncode-ZIP, beide Natje-apps en launcher: `artifacts/NatjeTV-1.0.0-app-pakket.zip`.', '', 'Screenshots: `artifacts/screenshots/home-robolectric.png`, `settings-robolectric.png`, `nightmode-robolectric.png`. Echte Android-rendering onder Robolectric; geen claims over decoder, DRM, HOME of emulator. Alle details en bewijs: `docs/ACCEPTATIETESTS.md` en `artifacts/reports/`.', '', 'Geen lokale bouwblocker. Openstaande bewijzen: fysieke remote en volledige appflows, werkelijke overlay/DRM/HDR/Dolby Vision, firmwarepersistentie, HOME/recovery en 2GB-performance. HDMI is een stub. Fase 2 begint pas met jouw expliciete startsein; dan model/API/fingerprint/serial vaststellen, recoverybaseline vastleggen, eerst alleen APK testen en daarna eventueel HOME toewijzen. Stock launcher blijft intact.']
(DOC/'RESULTAAT.md').write_text('\n'.join(result)+'\n',encoding='utf-8')
(ART/'screenshots/README.txt').write_text('Android Views onder Robolectric API34/native graphics; geen emulator- of Xiaomi-test. De lege testinventaris toont apps als niet beschikbaar. De klok gebruikt de virtuele JVM-testtijd.\n',encoding='utf-8')
bundle=ART/'NatjeTV-1.0.0-source.zip'
with zipfile.ZipFile(bundle,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for p in sorted(ROOT.rglob('*')):
        if not p.is_file(): continue
        rel=p.relative_to(ROOT)
        if rel.parts[0] in ['.git','.gradle','artifacts','NatjeTV'] or 'build' in rel.parts or rel.name=='local.properties' or p.suffix in ['.jks','.keystore']: continue
        if rel.parts[0]=='logs' and p.name not in ['final-verification-build.log','script-verification.log','runtime-dependencies.log','apk-manifest-tree.log','iptv-apksigner.log'] and not p.name.endswith(('-badging.log','-signature.log')): continue
        z.write(p,'NatjeTV/'+rel.as_posix())
    for p in sorted((ART/'screenshots').glob('*')):
        if p.is_file(): z.write(p,'NatjeTV/artifacts/screenshots/'+p.name)
    for p in sorted(reports.rglob('*')):
        if p.is_file() and 'binary' not in p.parts: z.write(p,'NatjeTV/artifacts/reports/'+p.relative_to(reports).as_posix())
app_bundle=ART/'NatjeTV-1.0.0-app-pakket.zip'
with zipfile.ZipFile(app_bundle,'w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for e in entries: z.write(ART/e['file'],'NatjeTV/artifacts/'+e['file'])
    z.write(ART/'APK-MANIFEST.json','NatjeTV/artifacts/APK-MANIFEST.json')
    z.write(bundle,'NatjeTV/artifacts/'+bundle.name)
    for name in ['preflight.ps1','install-phase2.ps1','restore-original-launcher.ps1']:
        z.write(ROOT/'scripts'/name,'NatjeTV/scripts/'+name)
    for name in ['INSTALLATIE_FASE_2.md','APPPAKKET.md','RESULTAAT.md','ACCEPTATIETESTS.md']:
        z.write(DOC/name,'NatjeTV/docs/'+name)
    z.write(ROOT/'README.md','NatjeTV/README.md')
    z.write(ROOT/'LICENSE','NatjeTV/LICENSE')
delivery={'apks':entries,'source':{'file':bundle.name,'bytes':bundle.stat().st_size,'sha256':hashlib.file_digest(bundle.open('rb'),'sha256').hexdigest()},'appBundle':{'file':app_bundle.name,'bytes':app_bundle.stat().st_size,'sha256':hashlib.file_digest(app_bundle.open('rb'),'sha256').hexdigest()},'tests':tests,'lintErrors':0,'lintWarnings':warnings}
(ART/'DELIVERY-MANIFEST.json').write_text(json.dumps(delivery,indent=2),encoding='utf-8')
print(json.dumps(delivery,indent=2))
