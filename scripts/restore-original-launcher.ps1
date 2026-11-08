[CmdletBinding()]
param([switch]$Execute,[string]$Serial,[string]$SdkPath=$env:ANDROID_HOME,[string]$StatePath)
$ErrorActionPreference='Stop'
if(-not $StatePath) { $StatePath=Join-Path (Split-Path $PSScriptRoot -Parent) 'artifacts\phase2-recovery.json' }
if(-not $Execute) { Write-Output "DRY-RUN: oorspronkelijke HOME herstellen uit $StatePath; Natje-overlay/toegankelijkheid terugzetten. Geen ADB gestart."; return }
if(-not $Serial) { throw '-Serial is verplicht.' }
if(-not $SdkPath) { $SdkPath=Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adbPath=Join-Path $SdkPath 'platform-tools\adb.exe'
$state=Get-Content -LiteralPath $StatePath -Raw | ConvertFrom-Json
if($state.schema -ne 1 -or $state.serial -ne $Serial -or $state.originalHome -notmatch '^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$') { throw 'Recoverybestand hoort niet bij dit apparaat of is ongeldig.' }
if((Read-Host 'Typ HERSTEL HOME om de oorspronkelijke launcher te herstellen') -cne 'HERSTEL HOME') { throw 'Geannuleerd.' }
function Invoke-Device([string[]]$Arguments) {
    $result=& $adbPath -s $Serial @Arguments 2>&1
    if($LASTEXITCODE -ne 0) { throw 'ADB-herstelstap mislukt.' }; return ($result -join "`n").Trim()
}
if((Invoke-Device @('shell','getprop','ro.build.fingerprint')) -ne $state.fingerprint) { throw 'Firmware is veranderd; controleer HOME handmatig voordat je herstelt.' }
Invoke-Device @('shell','am','force-stop','nl.natje.tv')
foreach($package in $state.disabledByNatjeTv) {
    if($package -notmatch '^[A-Za-z0-9_.]+$') { throw 'Ongeldige pakketnaam in recoverybestand.' }
    Invoke-Device @('shell','pm','enable','--user','0',$package)
}
Invoke-Device @('shell','cmd','package','set-home-activity','--user','0',$state.originalHome)
# Remove only our optional service if it was not enabled before; preserve every other service.
$ourService='nl.natje.tv/nl.natje.tv.nightmode.NightAccessibilityService'
$ourShort='nl.natje.tv/.nightmode.NightAccessibilityService'
if($state.originalAccessibility -notmatch 'nl\.natje\.tv/') {
    $current=Invoke-Device @('shell','settings','get','secure','enabled_accessibility_services')
    $remaining=@($current -split ':' | Where-Object { $_ -and $_ -ne 'null' -and $_ -ne $ourService -and $_ -ne $ourShort })
    if($remaining.Count) { Invoke-Device @('shell','settings','put','secure','enabled_accessibility_services',($remaining -join ':')) }
    else { Invoke-Device @('shell','settings','delete','secure','enabled_accessibility_services') }
}
$mode='default'
if($state.originalOverlay -match 'SYSTEM_ALERT_WINDOW:\s*(allow|ignore|deny|default)') { $mode=$Matches[1] }
Invoke-Device @('shell','cmd','appops','set','nl.natje.tv','SYSTEM_ALERT_WINDOW',$mode)
Invoke-Device @('shell','am','start','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
Write-Output 'Herstel voltooid; controleer zelf de Home-knop. Natje TV is niet verwijderd.'
