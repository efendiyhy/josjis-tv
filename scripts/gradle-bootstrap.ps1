param([Parameter(ValueFromRemainingArguments = $true)][string[]] $GradleArgs)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$version = '8.13'
$installed = Get-Command gradle -ErrorAction SilentlyContinue
if ($installed) {
    & $installed.Source -p $repo @GradleArgs
    exit $LASTEXITCODE
}
$base = Join-Path $HOME '.gradle\josjis-bootstrap'
$exec = Join-Path $base "gradle-$version\bin\gradle.bat"
if (-not (Test-Path $exec)) {
    New-Item -ItemType Directory -Force -Path $base | Out-Null
    $url = "https://services.gradle.org/distributions/gradle-$version-bin.zip"
    $zip = Join-Path $base "gradle-$version-bin.zip"
    Write-Host "Downloading Gradle $version..."
    Invoke-WebRequest -Uri $url -OutFile $zip
    Expand-Archive -Path $zip -DestinationPath $base -Force
    Remove-Item $zip
}
& $exec -p $repo @GradleArgs
exit $LASTEXITCODE
