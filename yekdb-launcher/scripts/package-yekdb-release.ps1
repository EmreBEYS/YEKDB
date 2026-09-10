[CmdletBinding()]
param(
    [Parameter(Mandatory)] [string]$Version,
    [ValidateSet('windows', 'macos', 'linux')] [string]$Platform = 'windows',
    [string]$CoreJar = '..\target\yekdb-1.0.0.jar'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$stage = Join-Path $projectRoot "target\release-$Platform"
$archive = Join-Path $projectRoot "dist\yekdb-$Version-$Platform.zip"

if (-not (Test-Path -LiteralPath $CoreJar)) { throw "Core JAR not found: $CoreJar" }
if (Test-Path -LiteralPath $stage) { Remove-Item -LiteralPath $stage -Recurse -Force }
New-Item -ItemType Directory -Force -Path $stage | Out-Null
New-Item -ItemType Directory -Force -Path (Split-Path $archive -Parent) | Out-Null
Copy-Item -LiteralPath $CoreJar -Destination (Join-Path $stage 'yekdb.jar')
Set-Content -LiteralPath (Join-Path $stage 'version.txt') -Value $Version -Encoding UTF8
Compress-Archive -Path (Join-Path $stage '*') -DestinationPath $archive -Force
Write-Host "Release asset ready: $archive"
