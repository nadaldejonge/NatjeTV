$ErrorActionPreference='Stop'
$projectPath=Split-Path $PSScriptRoot -Parent
$results=@()
Get-ChildItem -LiteralPath $PSScriptRoot -Filter '*.ps1' | ForEach-Object {
    $tokens=$null; $parseErrors=$null
    [System.Management.Automation.Language.Parser]::ParseFile($_.FullName,[ref]$tokens,[ref]$parseErrors) | Out-Null
    if($parseErrors.Count) { throw "Syntaxfout in $($_.Name): $($parseErrors.Message -join ', ')" }
    $results+="PARSE PASS: $($_.Name)"
}
# A fake SDK with no adb makes accidental device invocation fail immediately.
$dryRunOutput=& (Join-Path $PSScriptRoot 'install-phase2.ps1') -SdkPath 'C:\NatjeTV-nonexistent-sdk'
if(($dryRunOutput -join ' ') -notmatch 'DRY-RUN') { throw 'Install dry-run ontbreekt' }
$results+='DRY-RUN PASS: install-phase2 (geen SDK nodig)'
$dryRunOutput=& (Join-Path $PSScriptRoot 'restore-original-launcher.ps1') -SdkPath 'C:\NatjeTV-nonexistent-sdk'
if(($dryRunOutput -join ' ') -notmatch 'DRY-RUN') { throw 'Recovery dry-run ontbreekt' }
$results+='DRY-RUN PASS: restore-original-launcher (geen SDK nodig)'
$scriptText=Get-Content -LiteralPath (Join-Path $PSScriptRoot 'install-phase2.ps1') -Raw
if($scriptText -match 'disable-user|pm.+uninstall|adb.+connect') { throw 'Verboden automatische launcher-mutatie/verbinding in installscript' }
$results+='PASS: geen disable-user, uninstall of automatische verbinding in installatiepad'
$results | Tee-Object -FilePath (Join-Path $projectPath 'logs\script-verification.log')
