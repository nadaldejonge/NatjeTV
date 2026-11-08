[CmdletBinding()]
param([switch]$Execute,[string]$Serial,[switch]$SetHome,[switch]$IncludeNatjeApps,[string]$SdkPath=$env:ANDROID_HOME,[string]$ApkPath,[string]$StatePath)
$ErrorActionPreference='Stop'
$projectPath=Split-Path $PSScriptRoot -Parent
if(-not $ApkPath) { $ApkPath=Join-Path $projectPath 'artifacts\NatjeTV-1.0.0-debug.apk' }
if(-not $StatePath) { $StatePath=Join-Path $projectPath 'artifacts\phase2-recovery.json' }
if(-not $SdkPath) { $SdkPath=Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adbPath=Join-Path $SdkPath 'platform-tools\adb.exe'
Write-Output "Plan: APK $ApkPath installeren/updaten; HOME toewijzen: $SetHome. Stock launcher blijft intact."
Write-Output "Losse NatjeTube 2.0.1 en IPTV 0.1.11 meeleveren/installeren: $IncludeNatjeApps. Overige streamingapps via Google Play op het apparaat."
if(-not $Execute) { Write-Output 'DRY-RUN: geen ADB gestart, geen verbinding, geen mutatie. Fase 2 vereist -Execute, -Serial en bevestiging.'; return }
if(-not $Serial -or -not (Test-Path -LiteralPath $adbPath) -or -not (Test-Path -LiteralPath $ApkPath)) { throw 'Geef een bestaande APK, SDK en expliciet -Serial op.' }
$companionPaths=@()
if($IncludeNatjeApps) {
    $companionPaths=@((Join-Path $projectPath 'artifacts\companions\NatjeTube_2.0.1_universal.apk'),(Join-Path $projectPath 'artifacts\companions\Natje-IPTV-AndroidTV-0.1.11-recovery-debug.apk'))
    foreach($companionPath in $companionPaths) { if(-not (Test-Path -LiteralPath $companionPath)) { throw 'Een meegeleverde Natje-app ontbreekt in artifacts/companions.' } }
}
if((Read-Host 'Fase 2: typ INSTALLEER NATJE TV om op dit apparaat te wijzigen') -cne 'INSTALLEER NATJE TV') { throw 'Geannuleerd.' }
function Invoke-Device([string[]]$Arguments) {
    $result=& $adbPath -s $Serial @Arguments 2>&1
    if($LASTEXITCODE -ne 0) { throw "ADB-stap mislukt: $($Arguments[0])" }
    return ($result -join "`n").Trim()
}
$model=Invoke-Device @('shell','getprop','ro.product.model')
$api=Invoke-Device @('shell','getprop','ro.build.version.sdk')
$fingerprint=Invoke-Device @('shell','getprop','ro.build.fingerprint')
$homeResult=Invoke-Device @('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
$home=($homeResult -split "`n" | Where-Object { $_ -match '^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$' } | Select-Object -Last 1)
if(Test-Path -LiteralPath $StatePath) {
    $baseline=Get-Content -LiteralPath $StatePath -Raw | ConvertFrom-Json
    if($baseline.schema -ne 1 -or $baseline.serial -ne $Serial -or $baseline.fingerprint -ne $fingerprint -or $baseline.originalHome -notmatch '^[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+$' -or $baseline.originalHome -match '^nl\.natje\.tv/') { throw 'Bestaande recoverybaseline past niet bij dit apparaat/firmware.' }
    Write-Output 'Update: bestaande oorspronkelijke HOME/recoverybaseline blijft bewaard.'
} else {
    if(-not $home -or $home -match 'ResolverActivity|ChooserActivity|nl\.natje\.tv/') { throw 'Geen unieke oorspronkelijke HOME gevonden. Eerst oorspronkelijke HOME vaststellen.' }
    $accessibility=Invoke-Device @('shell','settings','get','secure','enabled_accessibility_services')
    $installed=Invoke-Device @('shell','pm','path','nl.natje.tv')
    $overlay=if($installed -match '^package:') { Invoke-Device @('shell','cmd','appops','get','nl.natje.tv','SYSTEM_ALERT_WINDOW') } else { 'Niet geïnstalleerd; oorspronkelijke modus: default' }
    $state=[ordered]@{ schema=1; serial=$Serial; model=$model; api=$api; fingerprint=$fingerprint; originalHome=$home; originalAccessibility=$accessibility; originalOverlay=$overlay; disabledByNatjeTv=@(); capturedAt=(Get-Date).ToString('o') }
    $state | ConvertTo-Json | Set-Content -LiteralPath $StatePath -Encoding utf8
}
Write-Output "Recovery opgeslagen: $StatePath; apparaat: $model, API $api"
Invoke-Device @('install','-r',$ApkPath)
foreach($companionPath in $companionPaths) {
    Write-Output "Losse app installeren/updaten: $([IO.Path]::GetFileName($companionPath))"
    # Android enforces package signatures; on mismatch stop. Never uninstall or clear data.
    Invoke-Device @('install','-r',$companionPath)
}
if($SetHome) {
    Invoke-Device @('shell','cmd','package','set-home-activity','--user','0','nl.natje.tv/.launcher.MainActivity')
    $resolved=Invoke-Device @('shell','cmd','package','resolve-activity','--brief','-a','android.intent.action.MAIN','-c','android.intent.category.HOME')
    if($resolved -notmatch 'nl\.natje\.tv/') { throw 'HOME-toewijzing niet bevestigd; gebruik recovery. Geen stock app uitgeschakeld.' }
    Write-Output 'HOME-resolutie bevestigd. Test nu zelf Home, Back, reboot en wake. Dit script herstart niets.'
}
Write-Output 'Overlay en toegankelijkheid alleen zelf in Android-instellingen activeren. Geen systeemlauncher gewijzigd.'
