[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$CertificateThumbprint,
    [Parameter(Mandatory)][string[]]$Files,
    [string]$TimestampUrl = 'http://timestamp.digicert.com'
)

$ErrorActionPreference = 'Stop'
$thumbprint = $CertificateThumbprint.Replace(' ', '').ToUpperInvariant()
$certificate = @(
    Get-ChildItem Cert:\CurrentUser\My -CodeSigningCert -ErrorAction SilentlyContinue
    Get-ChildItem Cert:\LocalMachine\My -CodeSigningCert -ErrorAction SilentlyContinue
) | Where-Object Thumbprint -eq $thumbprint | Select-Object -First 1
if (-not $certificate) { throw "Kod imzalama sertifikası bulunamadı: $thumbprint" }
if (-not $certificate.HasPrivateKey) { throw 'Kod imzalama sertifikasının özel anahtarı bulunamadı.' }
if ($certificate.NotAfter -le (Get-Date)) { throw 'Kod imzalama sertifikasının süresi dolmuş.' }

$windowsKits = Join-Path ${env:ProgramFiles(x86)} 'Windows Kits\10\bin'
$signTool = Get-ChildItem -LiteralPath $windowsKits -Filter signtool.exe -Recurse -ErrorAction SilentlyContinue |
    Where-Object FullName -Match '\\x64\\signtool\.exe$' |
    Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
if (-not $signTool) { throw 'SignTool bulunamadı. Windows SDK Signing Tools bileşenini kurun.' }

foreach ($file in $Files) {
    $resolvedFile = (Resolve-Path -LiteralPath $file).Path
    $storeArguments = if ($certificate.PSPath -like '*LocalMachine*') { @('/sm', '/s', 'My') } else { @('/s', 'My') }
    & $signTool sign /sha1 $thumbprint @storeArguments /fd SHA256 /td SHA256 /tr $TimestampUrl /d 'YEKDB' $resolvedFile
    if ($LASTEXITCODE -ne 0) { throw "Dijital imza başarısız: $resolvedFile" }
    & $signTool verify /pa /all $resolvedFile
    if ($LASTEXITCODE -ne 0) { throw "Dijital imza doğrulanamadı: $resolvedFile" }
}
