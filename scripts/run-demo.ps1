$ErrorActionPreference = 'Stop'
Set-Location (Split-Path $PSScriptRoot -Parent)
$candidates = @()
if ($env:JAVA_HOME) { $candidates += (Join-Path $env:JAVA_HOME 'bin\java.exe') }
$found = Get-Command java.exe -ErrorAction SilentlyContinue
if ($found) { $candidates += $found.Source }
$roots = @("$env:USERPROFILE\.jdks", "$env:ProgramFiles\Java", "$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Microsoft", "$env:ProgramFiles\Zulu")
foreach ($root in $roots) {
  if (Test-Path $root) {
    Get-ChildItem $root -Directory -ErrorAction SilentlyContinue | ForEach-Object { $candidates += (Join-Path $_.FullName 'bin\java.exe') }
  }
}
$java = $null
foreach ($candidate in ($candidates | Select-Object -Unique)) {
  if (Test-Path $candidate) {
    $previous = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    $version = (& $candidate -version 2>&1 | Out-String)
    $ErrorActionPreference = $previous
    if ($version -match 'version "21[."]') { $java = $candidate; break }
  }
}
if (!$java) {
  $jdk = Read-Host 'JDK 21 not auto-detected. Paste its installation folder (not bin)'
  $jdk = $jdk.Trim('"')
  $java = Join-Path $jdk 'bin\java.exe'
  if (!(Test-Path $java)) { throw 'Java executable not found. Install JDK 21 and retry.' }
}
Write-Host 'Starting Basic Auth backend. PostgreSQL must be running.'
Write-Host 'Database settings: src/main/resources/application.yml'
& $java -jar '.\dist\home-loan-backend-1.0.0.jar' '--spring.config.location=file:./src/main/resources/application.yml'
if ($LASTEXITCODE -ne 0) { throw 'Backend stopped with an error. Read the startup message above.' }
