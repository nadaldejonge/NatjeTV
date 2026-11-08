[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    New-Item -ItemType Directory -Force 'logs' | Out-Null
    & .\gradlew.bat --console=plain :app:lintDebug :app:testDebugUnitTest 2>&1 | Tee-Object -FilePath 'logs\verify.log'
    if ($LASTEXITCODE -ne 0) { throw "Verificatie mislukt: $LASTEXITCODE" }
    Write-Output 'Lokale lint- en JVM/Android-UI-tests geslaagd. Geen ADB gebruikt. Hardwaretests blijven apart.'
} finally { Pop-Location }
