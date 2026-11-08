[CmdletBinding()]
param([switch]$Execute,[string]$Serial,[string]$SdkPath=$env:ANDROID_HOME)
$ErrorActionPreference='Stop'
$projectPath=Split-Path $PSScriptRoot -Parent
$state=Get-Content -LiteralPath (Join-Path $projectPath 'artifacts\phase2\npo-analysis\private-dns-before.json') -Raw | ConvertFrom-Json
if(-not $Execute) { Write-Output 'DRY-RUN: herstel uitsluitend de oorspronkelijke Android Private DNS-instellingen. Geen ADB gestart.'; return }
if(-not $Serial) { $Serial=$state.serial }
if(-not $SdkPath) { $SdkPath=Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adbPath=Join-Path $SdkPath 'platform-tools\adb.exe'
$fingerprint=(& $adbPath -s $Serial shell getprop ro.build.fingerprint) -join ''
if($LASTEXITCODE -ne 0 -or $fingerprint.Trim() -ne $state.fingerprint) { throw 'Apparaat niet bereikbaar of fingerprint wijkt af.' }
foreach($key in @('private_dns_mode','private_dns_specifier')) {
    $value=$state.$key
    if(-not $value -or $value -eq 'null') { & $adbPath -s $Serial shell settings delete global $key }
    else { & $adbPath -s $Serial shell settings put global $key $value }
    if($LASTEXITCODE -ne 0) { throw "Herstel mislukt voor $key" }
}
Write-Output 'Oorspronkelijke Private DNS-instellingen hersteld.'
