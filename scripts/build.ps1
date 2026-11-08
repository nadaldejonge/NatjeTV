[CmdletBinding()]
param([string]$SdkPath = $env:ANDROID_HOME)
$ErrorActionPreference = 'Stop'
$projectPath = Split-Path $PSScriptRoot -Parent
Push-Location $projectPath
try {
    if (-not $SdkPath) { $SdkPath = Join-Path $env:LOCALAPPDATA 'Android\Sdk' }
    if (-not (Test-Path -LiteralPath $SdkPath)) { throw 'Android SDK ontbreekt; geef -SdkPath op.' }
    if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Java 17 of hoger ontbreekt in PATH.' }
    [System.IO.File]::WriteAllText((Join-Path $projectPath 'local.properties'),('sdk.dir=' + $SdkPath.Replace('\','/').Replace(':','\:') + "`n"),[System.Text.UTF8Encoding]::new($false))
    New-Item -ItemType Directory -Force 'logs','artifacts' | Out-Null
    & .\gradlew.bat --console=plain :app:assembleDebug :app:assembleRelease 2>&1 | Tee-Object -FilePath 'logs\build.log'
    if ($LASTEXITCODE -ne 0) { throw "Gradle build mislukt: $LASTEXITCODE" }
    $versionMatch = [regex]::Match((Get-Content -LiteralPath 'app\build.gradle.kts' -Raw), 'versionName\s*=\s*"([^"]+)"')
    if (-not $versionMatch.Success) { throw 'versionName ontbreekt in app/build.gradle.kts.' }
    $appVersion = $versionMatch.Groups[1].Value
    Copy-Item -LiteralPath 'app\build\outputs\apk\debug\app-debug.apk' -Destination "artifacts\NatjeTV-$appVersion-debug.apk"
    Copy-Item -LiteralPath 'app\build\outputs\apk\release\app-release-unsigned.apk' -Destination "artifacts\NatjeTV-$appVersion-release-unsigned.apk"
    Get-ChildItem -LiteralPath 'artifacts' -Filter "NatjeTV-$appVersion-*.apk" | ForEach-Object {
        [PSCustomObject]@{ File=$_.Name; Bytes=$_.Length; SHA256=(Get-FileHash -LiteralPath $_.FullName -Algorithm SHA256).Hash.ToLowerInvariant() }
    } | ConvertTo-Json | Set-Content -LiteralPath 'artifacts\APK-MANIFEST.json' -Encoding utf8
} finally { Pop-Location }
