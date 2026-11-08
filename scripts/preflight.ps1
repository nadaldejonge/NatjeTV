[CmdletBinding()]
param([switch]$InspectDevice,[string]$Serial,[string]$SdkPath=$env:ANDROID_HOME)
$ErrorActionPreference='Stop'
if (-not $SdkPath) { $SdkPath=Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
$adbPath=Join-Path $SdkPath 'platform-tools\adb.exe'
[PSCustomObject]@{ SDK=$SdkPath; SDKPresent=(Test-Path -LiteralPath $SdkPath); ADBPresent=(Test-Path -LiteralPath $adbPath); Java=(Get-Command java -ErrorAction SilentlyContinue).Source; FreeBytes=(Get-PSDrive C).Free; DeviceAccess=$false }
if (-not $InspectDevice) { Write-Output 'Alleen lokale controle. Geen ADB gestart. Apparaatcontrole hoort bij fase 2.'; return }
if (-not $Serial) { throw '-Serial is verplicht voor expliciete fase-2 apparaatinspectie.' }
if (-not (Test-Path -LiteralPath $adbPath)) { throw 'ADB ontbreekt.' }
# Never connect automatically or scan the network. The user must establish pairing in phase 2.
foreach ($property in @('ro.product.model','ro.build.version.sdk','ro.build.fingerprint')) {
    & $adbPath -s $Serial shell getprop $property
    if($LASTEXITCODE -ne 0) { throw 'ADB-inspectie mislukt.' }
}
& $adbPath -s $Serial shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME
if($LASTEXITCODE -ne 0) { throw 'HOME-resolutie mislukt.' }
