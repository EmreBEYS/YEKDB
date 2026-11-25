[CmdletBinding()]
param(
    [Parameter(Mandatory)][ValidatePattern('^\d+\.\d+\.\d+$')][string]$Version,
    [string]$Repository = 'EmreBEYS/YEKDB',
    [string]$NotesFile,
    [string]$ReleaseTitle,
    [ValidatePattern('^[0-9a-fA-F]{40}$')][string]$TargetCommit
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$repositoryRoot = Split-Path -Parent $projectRoot
$installer = Join-Path $projectRoot "dist\YEKDB-Setup-$Version.exe"
$checksumFile = Join-Path $projectRoot "dist\SHA256SUMS-$Version.txt"
$macChecksumFile = Join-Path $projectRoot "dist\SHA256SUMS-macOS-$Version.txt"
$tag = "v$Version"
if (-not $ReleaseTitle) { $ReleaseTitle = "YEKDB $tag" }
if (-not $TargetCommit) {
    $TargetCommit = (& git -C $repositoryRoot rev-parse HEAD).Trim()
    if ($LASTEXITCODE -ne 0 -or $TargetCommit -notmatch '^[0-9a-fA-F]{40}$') {
        throw 'Release hedef commit bilgisi alınamadı.'
    }
}
if (& git -C $repositoryRoot status --porcelain) {
    throw 'Release yayımlamadan önce Git çalışma ağacını commit edin veya temizleyin.'
}

if (-not (Get-Command gh -ErrorAction SilentlyContinue)) {
    throw 'GitHub CLI bulunamadı. Önce https://cli.github.com/ adresinden GitHub CLI kurun.'
}
& gh auth status
if ($LASTEXITCODE -ne 0) { throw 'GitHub oturumu açık değil. Önce gh auth login çalıştırın.' }
if (-not (Test-Path -LiteralPath $installer)) { throw "Yükleyici bulunamadı: $installer" }

$escapedVersion = [regex]::Escape($Version)
$macPackages = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'dist') -File | Where-Object {
    $_.Name -match "^YEKDB-$escapedVersion-(arm64|x86_64)(-app\.zip|\.dmg)$"
} | Sort-Object Name)
if (-not ($macPackages.Name -match '\.dmg$')) {
    throw "En az bir macOS DMG paketi bulunamadı: dist\YEKDB-$Version-<arch>.dmg"
}

$hash = (Get-FileHash -LiteralPath $installer -Algorithm SHA256).Hash
[IO.File]::WriteAllText($checksumFile, "$hash  $(Split-Path $installer -Leaf)`r`n", [Text.UTF8Encoding]::new($false))
$macHashes = foreach ($package in $macPackages) {
    "$((Get-FileHash -LiteralPath $package.FullName -Algorithm SHA256).Hash)  $($package.Name)"
}
[IO.File]::WriteAllText($macChecksumFile, ($macHashes -join "`n") + "`n", [Text.UTF8Encoding]::new($false))

$assets = @($installer, $checksumFile, $macChecksumFile) + @($macPackages.FullName)

$arguments = @('release', 'create', $tag) + $assets + @('--repo', $Repository,
    '--title', $ReleaseTitle, '--target', $TargetCommit, '--latest')
if ($NotesFile) {
    $arguments += @('--notes-file', (Resolve-Path -LiteralPath $NotesFile).Path)
} else {
    $arguments += '--generate-notes'
}
& gh @arguments
if ($LASTEXITCODE -ne 0) { throw 'GitHub Release yayımlanamadı.' }

Write-Host "GitHub Release yayımlandı: https://github.com/$Repository/releases/tag/$tag"
