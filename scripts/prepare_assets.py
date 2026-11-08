"""Copy original supplied assets. No generated/repainted brand artwork."""
from pathlib import Path
import shutil
ROOT=Path(__file__).resolve().parents[1]
source=ROOT.parent/'assets'
res=ROOT/'app/src/main/res'
(res/'drawable-nodpi').mkdir(parents=True,exist_ok=True)
copies={'logos/NatjeTV_logo_horizontal_1024x273.png':'natje_logo.png','logos/NatjeTV_symbool_transparant_1024.png':'natje_symbol.png','banners/NatjeTV_banner_320x180.png':'tv_banner.png','backgrounds/NatjeTV_wallpaper_1_3840x2160.png':'wallpaper1.png','backgrounds/NatjeTV_wallpaper_2_3840x2160.png':'wallpaper2.png'}
for a,b in copies.items(): shutil.copy2(source/a,res/'drawable-nodpi'/b)
for p in (source/'icons/legacy').iterdir():
    if p.is_dir(): shutil.copytree(p,res/p.name,dirs_exist_ok=True)
iptv=Path('C:/Users/nadal/Downloads/Player/src/renderer/public/branding/natje-2026/natje-iptv-logo-400w.png')
shutil.copy2(iptv,res/'drawable-nodpi/iptv_logo.png')
(ROOT/'app/src/main/assets').mkdir(exist_ok=True)
(ROOT/'app/src/main/assets/NOTICE.txt').write_text('Natje TV 1.0\nEigen launcher- en servicecode.\nNatjeTube Night Mode parameters: NatjeNN / nadaldejonge, MIT.\nAndroidX RecyclerView en transitieve AndroidX libraries: Apache-2.0.\nKotlin standaardbibliotheek: Apache-2.0.\nVolledige licenties: assets/licenses/ en docs/BRONNEN_EN_LICENTIES.md.\nBranding: door gebruiker aangeleverde Natje TV en Natje IPTV assets.',encoding='utf-8')
print('Original assets copied')
