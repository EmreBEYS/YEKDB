param(
    [Parameter(Mandatory = $true)][string]$InstallationDirectory,
    [Parameter(Mandatory = $true)][int]$ParentProcessId
)

$ErrorActionPreference = 'SilentlyContinue'
$resolvedDirectory = [IO.Path]::GetFullPath($InstallationDirectory).TrimEnd('\')
$driveRoot = [IO.Path]::GetPathRoot($resolvedDirectory).TrimEnd('\')
if ([string]::IsNullOrWhiteSpace($resolvedDirectory) -or $resolvedDirectory -eq $driveRoot -or $resolvedDirectory.Length -lt 8) {
    exit 1
}

while (Get-Process -Id $ParentProcessId -ErrorAction SilentlyContinue) {
    Start-Sleep -Milliseconds 250
}
Start-Sleep -Milliseconds 500
Remove-Item -LiteralPath $resolvedDirectory -Recurse -Force
Remove-Item -LiteralPath $PSCommandPath -Force
