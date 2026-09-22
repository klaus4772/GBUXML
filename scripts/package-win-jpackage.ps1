$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$targetDir = Join-Path $projectRoot "target"
$distBaseDir = Join-Path $projectRoot "dist\jpackage"
$distDir = Join-Path $distBaseDir "windows"
$stagingDir = Join-Path $targetDir "jpackage-staging"

if (Test-Path -LiteralPath $distBaseDir) {
    try {
        Remove-Item -LiteralPath $distBaseDir -Recurse -Force -ErrorAction Stop
    } catch {
        Write-Warning "Could not fully remove old dist/jpackage directory; retrying with a safer cleanup."
        Get-ChildItem -LiteralPath $distBaseDir -Force -Recurse -ErrorAction SilentlyContinue | Remove-Item -Recurse -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath $distBaseDir -Recurse -Force -ErrorAction SilentlyContinue
    }
}

if (Test-Path -LiteralPath $stagingDir) {
    Remove-Item -LiteralPath $stagingDir -Recurse -Force -ErrorAction SilentlyContinue
}

New-Item -ItemType Directory -Force -Path $stagingDir | Out-Null
New-Item -ItemType Directory -Force -Path $distDir | Out-Null

$jpackageCandidates = @(
    (Join-Path $env:JAVA_HOME "bin\jpackage.exe"),
    'C:\Program Files\Java\latest\bin\jpackage.exe',
    'C:\Program Files\Java\jdk-21.0.12\bin\jpackage.exe',
    'C:\Program Files\Java\jdk-26.0.1\bin\jpackage.exe'
)

$jpackageExe = $jpackageCandidates | Where-Object { $_ -and (Test-Path $_) } | Select-Object -First 1

if (-not $jpackageExe) {
    throw "jpackage.exe wurde nicht gefunden. Bitte JAVA_HOME auf ein JDK mit jpackage setzen."
}

if (-not (Test-Path $targetDir)) {
    Write-Host "Build target not found. Run 'mvn clean package' first."
    exit 1
}

$jarFile = Get-ChildItem $targetDir -Filter "*.jar" | Where-Object { $_.Name -notmatch "original" } | Select-Object -First 1

if (-not $jarFile) {
    Write-Host "No runnable jar found in target directory."
    exit 1
}

Copy-Item -Path $jarFile.FullName -Destination $stagingDir -Force
$jarName = $jarFile.Name

$cmdArgs = @(
    "--type", "app-image",
    "--name", "GBUXML",
    "--app-version", "1.1.0",
    "--vendor", "GBUXML",
    "--input", $stagingDir,
    "--main-jar", $jarName,
    "--main-class", "com.gbuxml.GBUXMLApplication",
    "--dest", $distDir
)

& $jpackageExe @cmdArgs
if ($LASTEXITCODE -ne 0) {
    throw "Windows app-image packaging failed."
}

$zipDir = Join-Path $projectRoot "dist"
if (-not (Test-Path $zipDir)) {
    New-Item -ItemType Directory -Force -Path $zipDir | Out-Null
}
$zipPath = Join-Path $zipDir "GBUXML-portable-windows-1.1.0.zip"
if (Test-Path $zipPath) {
    Remove-Item $zipPath -Force
}
Compress-Archive -Path (Join-Path $distDir "GBUXML") -DestinationPath $zipPath -Force
Write-Host "Windows app image created in $distDir"
Write-Host "Portable ZIP created at $zipPath"
