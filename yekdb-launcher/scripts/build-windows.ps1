[CmdletBinding()]
param(
    [string]$Version = '1.0.0',
    [string]$CertificateThumbprint,
    [string]$TimestampUrl = 'http://timestamp.digicert.com'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$repositoryRoot = Split-Path -Parent $projectRoot
$maven = Join-Path $projectRoot 'mvnw.cmd'
$packageInput = Join-Path $projectRoot 'target\package-input'
$imageRoot = Join-Path $projectRoot 'target\windows-image'
$bundleRoot = Join-Path $projectRoot 'target\windows-setup'
$destination = Join-Path $projectRoot 'dist'
$installer = Join-Path $destination "YEKDB-Setup-$Version.exe"
$numericVersion = if ($Version -match '^\d+\.\d+\.\d+$') { "$Version.0" } else { throw 'Version must use the x.y.z format.' }

Push-Location $projectRoot
try {
    & $maven -f (Join-Path $repositoryRoot 'pom.xml') clean package '-Dmaven.test.skip=true'
    if ($LASTEXITCODE -ne 0) { throw 'YEKDB core build failed.' }
    & $maven clean package
    if ($LASTEXITCODE -ne 0) { throw 'Launcher build failed.' }
    Copy-Item -LiteralPath (Join-Path $repositoryRoot 'target\yekdb-1.0.0.jar') -Destination (Join-Path $packageInput 'yekdb.jar') -Force

    if (Test-Path -LiteralPath $imageRoot) { Remove-Item -LiteralPath $imageRoot -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $imageRoot, $bundleRoot, $destination | Out-Null
    & jpackage --type app-image --name 'YEKDB Launcher' --app-version $Version --vendor 'YEKDB' `
        --copyright 'Copyright © YEKDB 2026' `
        --description 'YEKDB Launcher and Database Engine' --input $packageInput `
        --main-jar 'yekdb-launcher-1.0.0.jar' --main-class 'com.yekdb.launcher.LauncherMain' `
        --add-launcher "YEKDB Engine=$(Join-Path $projectRoot 'packaging\windows\yekdb-engine.properties')" `
        --icon (Join-Path $projectRoot 'packaging\icons\yekdb.ico') --dest $imageRoot
    if ($LASTEXITCODE -ne 0) { throw 'jpackage app-image failed.' }

    $compiler = Join-Path $env:WINDIR 'Microsoft.NET\Framework64\v4.0.30319\csc.exe'
    if (-not (Test-Path -LiteralPath $compiler)) { throw '.NET Framework C# compiler could not be found.' }
    $uninstallerVersionInfo = Join-Path $bundleRoot 'UninstallerVersionInfo.cs'
    (Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'packaging\windows\UninstallerVersionInfo.template.cs')).Replace('__VERSION__', $Version).Replace('__NUMERIC_VERSION__', $numericVersion) |
        Set-Content -LiteralPath $uninstallerVersionInfo -Encoding UTF8
    & $compiler /nologo /target:winexe /optimize+ /platform:anycpu /reference:System.Windows.Forms.dll `
        "/win32icon:$(Join-Path $projectRoot 'packaging\icons\yekdb.ico')" `
        "/out:$(Join-Path $imageRoot 'YEKDB Launcher\YEKDB Uninstall.exe')" `
        (Join-Path $projectRoot 'packaging\windows\UninstallerStub.cs') $uninstallerVersionInfo
    if ($LASTEXITCODE -ne 0) { throw 'Windows uninstaller could not be created.' }

    if ($CertificateThumbprint) {
        & (Join-Path $projectRoot 'scripts\sign-windows.ps1') -CertificateThumbprint $CertificateThumbprint `
            -TimestampUrl $TimestampUrl -Files @(
                (Join-Path $imageRoot 'YEKDB Launcher\YEKDB Launcher.exe'),
                (Join-Path $imageRoot 'YEKDB Launcher\YEKDB Engine.exe'),
                (Join-Path $imageRoot 'YEKDB Launcher\YEKDB Uninstall.exe')
            )
    }

    Copy-Item -LiteralPath (Join-Path $projectRoot 'packaging\icons\yekdb.ico') `
        -Destination (Join-Path $imageRoot 'YEKDB Launcher\yekdb.ico') -Force

    Compress-Archive -Path (Join-Path $imageRoot 'YEKDB Launcher\*') -DestinationPath (Join-Path $bundleRoot 'payload.zip') -Force
    Copy-Item -LiteralPath (Join-Path $projectRoot 'packaging\windows\setup.ps1') -Destination $bundleRoot -Force
    Copy-Item -LiteralPath (Join-Path $projectRoot 'packaging\windows\uninstall.ps1') -Destination $bundleRoot -Force
    Copy-Item -LiteralPath (Join-Path $projectRoot 'packaging\windows\uninstall-cleanup.ps1') -Destination $bundleRoot -Force
    Copy-Item -LiteralPath (Join-Path $projectRoot 'packaging\windows\installer-banner.png') -Destination $bundleRoot -Force

    if (Test-Path -LiteralPath $installer) { Remove-Item -LiteralPath $installer -Force }
    $setupVersionInfo = Join-Path $bundleRoot 'SetupVersionInfo.cs'
    (Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'packaging\windows\SetupVersionInfo.template.cs')).Replace('__VERSION__', $Version).Replace('__NUMERIC_VERSION__', $numericVersion) |
        Set-Content -LiteralPath $setupVersionInfo -Encoding UTF8
    & $compiler /nologo /target:winexe /optimize+ /platform:anycpu /reference:System.Windows.Forms.dll `
        "/win32icon:$(Join-Path $projectRoot 'packaging\icons\yekdb.ico')" "/out:$installer" `
        "/resource:$(Join-Path $bundleRoot 'payload.zip'),YEKDB.Payload" `
        "/resource:$(Join-Path $bundleRoot 'installer-banner.png'),YEKDB.Banner" `
        "/resource:$(Join-Path $bundleRoot 'setup.ps1'),YEKDB.SetupScript" `
        "/resource:$(Join-Path $bundleRoot 'uninstall.ps1'),YEKDB.UninstallScript" `
        "/resource:$(Join-Path $bundleRoot 'uninstall-cleanup.ps1'),YEKDB.UninstallCleanupScript" `
        (Join-Path $projectRoot 'packaging\windows\InstallerStub.cs') $setupVersionInfo
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $installer)) { throw 'Windows setup executable could not be created.' }
    if ($CertificateThumbprint) {
        & (Join-Path $projectRoot 'scripts\sign-windows.ps1') -CertificateThumbprint $CertificateThumbprint `
            -TimestampUrl $TimestampUrl -Files @($installer)
    }
    Write-Host "Windows installer ready: $installer"
} finally { Pop-Location }
