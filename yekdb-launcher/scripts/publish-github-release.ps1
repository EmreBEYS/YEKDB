[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^\d+\.\d+\.\d+$')][string]$Version,
    [string]$Repository = 'EmreBEYS/YEKDB',
    [string]$NotesFile
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$installer = Join-Path $projectRoot "dist\YEKDB-Setup-$Version.exe"
$checksumFile = Join-Path $projectRoot "dist\SHA256SUMS-$Version.txt"
$tag = "v$Version"

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw 'GitHub CLI bulunamadı. Önce https://cli.github.com/ adresinden GitHub CLI kurun.'
}
& gh auth status
if ($LASTEXITCODE -ne 0) { throw 'GitHub oturumu açık değil. Önce gh auth login çalıştırın.' }
if (-not (Test-Path -LiteralPath $installer)) { throw "Yükleyici bulunamadı: $installer" }

$hash = (Get-FileHash -LiteralPath $installer -Algorithm SHA256).Hash
[IO.File]::WriteAllText($checksumFile, "$hash  $(Split-Path $installer -Leaf)`r`n", [Text.UTF8Encoding]::new($false))

$arguments = @('release', 'create', $tag, $installer, $checksumFile, '--repo', $Repository,
    '--title', "YEKDB Windows $tag", '--latest')
if ($NotesFile) {
    $arguments += @('--notes-file', (Resolve-Path -LiteralPath $NotesFile).Path)
} else {
    $arguments += '--generate-notes'
}
& gh @arguments
if ($LASTEXITCODE -ne 0) { throw 'GitHub Release yayımlanamadı.' }

Write-Host "GitHub Release yayımlandı: https://github.com/$Repository/releases/tag/$tag"
