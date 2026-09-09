$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$targetDir = Join-Path $projectRoot "target"
$distDir = Join-Path $targetDir "jpackage"

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

$jarName = Get-ChildItem $targetDir -Filter "*.jar" | Where-Object { $_.Name -notmatch "original" } | Select-Object -First 1 | ForEach-Object { $_.Name }

if (-not $jarName) {
    Write-Host "No runnable jar found in target directory."
    exit 1
}

New-Item -ItemType Directory -Force -Path $distDir | Out-Null

$packageType = "exe"
$cmdArgs = @(
    "--type", $packageType,
    "--name", "GBUXML",
    "--app-version", "1.0",
    "--vendor", "GBUXML",
    "--input", $targetDir,
    "--main-jar", $jarName,
    "--main-class", "com.gbuxml.GBUXMLApplication",
    "--dest", $distDir,
    "--win-menu",
    "--win-shortcut",
    "--win-console", "false"
)

try {
    & $jpackageExe @cmdArgs
    if ($LASTEXITCODE -ne 0) { throw "jpackage exited with code $LASTEXITCODE" }
    Write-Host "Installer created in $distDir"
}
catch {
    Write-Warning "Windows installer dependencies are missing. Falling back to a runnable app image."
    $fallbackArgs = @(
        "--type", "app-image",
        "--name", "GBUXML",
        "--app-version", "1.0",
        "--vendor", "GBUXML",
        "--input", $targetDir,
        "--main-jar", $jarName,
        "--main-class", "com.gbuxml.GBUXMLApplication",
        "--dest", $distDir
    )
    & $jpackageExe @fallbackArgs
    if ($LASTEXITCODE -ne 0) { throw "Fallback app-image packaging also failed." }
    Write-Host "App image created in $distDir"
}
