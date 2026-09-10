$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName PresentationFramework, PresentationCore, WindowsBase

$installationDirectory = $PSScriptRoot
$dataDirectory = Join-Path $env:LOCALAPPDATA 'YEKDB'

[xml]$xaml = @'
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" Title="YEKDB Kaldırma" Width="820" Height="500" WindowStartupLocation="CenterScreen" ResizeMode="NoResize" Background="#0B0910" Foreground="#F5F1FA">
  <Grid>
    <Grid.ColumnDefinitions><ColumnDefinition Width="280"/><ColumnDefinition Width="*"/></Grid.ColumnDefinitions>
    <Border Grid.Column="0" Background="#100D17"><Grid><Image Name="Banner" Stretch="UniformToFill"/><Border Background="#30000000"/></Grid></Border>
    <Grid Grid.Column="1" Margin="34,28,34,24">
      <Grid.RowDefinitions><RowDefinition Height="*"/><RowDefinition Height="Auto"/></Grid.RowDefinitions>
      <StackPanel>
        <TextBlock Text="YEKDB KALDIRMA" Foreground="#C084FC" FontSize="12" FontWeight="Bold"/>
        <TextBlock Text="Launcher ve Database Engine" FontSize="28" FontWeight="Bold" Margin="0,10,0,8"/>
        <TextBlock Text="YEKDB program dosyaları ve kısayolları bu bilgisayardan kaldırılacak." Foreground="#B8AFBF" TextWrapping="Wrap" FontSize="14" Margin="0,0,0,26"/>
        <Border Background="#17131E" BorderBrush="#49365E" BorderThickness="1" CornerRadius="8" Padding="16">
          <StackPanel>
            <TextBlock Text="Veritabanı verileriniz korunacak" FontSize="15" FontWeight="SemiBold"/>
            <TextBlock Name="DataPath" Foreground="#A99DB5" TextWrapping="Wrap" Margin="0,8,0,0"/>
          </StackPanel>
        </Border>
        <ProgressBar Name="Progress" Height="8" Margin="0,28,0,0" Minimum="0" Maximum="100" Value="0" Foreground="#9B5DE5" Background="#241C2C"/>
        <TextBlock Name="Status" Text="Kaldırmaya hazır." Foreground="#91869D" Margin="0,10,0,0" TextWrapping="Wrap"/>
      </StackPanel>
      <StackPanel Grid.Row="1" Orientation="Horizontal" HorizontalAlignment="Right">
        <Button Name="CancelButton" Content="İptal" Width="86" Height="36" Margin="0,0,10,0" Background="#221C29" Foreground="White" BorderBrush="#473752"/>
        <Button Name="UninstallButton" Content="Kaldır" Width="100" Height="36" Background="#8B5CF6" Foreground="White" BorderBrush="#A77AF7" FontWeight="Bold"/>
      </StackPanel>
    </Grid>
  </Grid>
</Window>
'@

$window = [Windows.Markup.XamlReader]::Load((New-Object System.Xml.XmlNodeReader $xaml))
$banner = $window.FindName('Banner')
$dataPath = $window.FindName('DataPath')
$progress = $window.FindName('Progress')
$status = $window.FindName('Status')
$cancelButton = $window.FindName('CancelButton')
$uninstallButton = $window.FindName('UninstallButton')
$dataPath.Text = $dataDirectory
$bannerFile = Join-Path $installationDirectory 'uninstaller-banner.png'
if (Test-Path -LiteralPath $bannerFile) {
    $banner.Source = New-Object Windows.Media.Imaging.BitmapImage([Uri]$bannerFile)
}

$cancelButton.Add_Click({ $window.Close() })
$uninstallButton.Add_Click({
    try {
        $uninstallButton.IsEnabled = $false
        $cancelButton.IsEnabled = $false
        $status.Text = 'Açık YEKDB işlemleri kapatılıyor…'
        $progress.Value = 20
        [System.Windows.Threading.Dispatcher]::CurrentDispatcher.Invoke([Action]{}, 'Background')

        $normalizedInstall = [IO.Path]::GetFullPath($installationDirectory).TrimEnd('\')
        $running = @(Get-Process -Name 'YEKDB Launcher', 'YEKDB Engine' -ErrorAction SilentlyContinue | Where-Object {
            try { $_.Path -and [IO.Path]::GetFullPath($_.Path).StartsWith($normalizedInstall, [StringComparison]::OrdinalIgnoreCase) }
            catch { $false }
        })
        foreach ($process in $running) {
            if ($process.CloseMainWindow()) { $process.WaitForExit(1500) | Out-Null }
            if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue }
        }

        $status.Text = 'Kısayollar ve Windows uygulama kaydı kaldırılıyor…'
        $progress.Value = 55
        [System.Windows.Threading.Dispatcher]::CurrentDispatcher.Invoke([Action]{}, 'Background')
        $desktopShortcut = Join-Path ([Environment]::GetFolderPath('Desktop')) 'YEKDB Launcher.lnk'
        $startMenuDirectory = Join-Path $env:APPDATA 'Microsoft\Windows\Start Menu\Programs'
        Remove-Item -LiteralPath $desktopShortcut -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath (Join-Path $startMenuDirectory 'YEKDB Launcher.lnk') -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath (Join-Path $startMenuDirectory 'YEKDB Kaldır.lnk') -Force -ErrorAction SilentlyContinue
        Remove-Item -LiteralPath 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\YEKDB' -Recurse -Force -ErrorAction SilentlyContinue

        $status.Text = 'Program dosyaları temizleniyor…'
        $progress.Value = 85
        [System.Windows.Threading.Dispatcher]::CurrentDispatcher.Invoke([Action]{}, 'Background')
        $cleanupScript = Join-Path $env:TEMP ("YEKDB-Uninstall-" + [Guid]::NewGuid().ToString('N') + '.ps1')
        Copy-Item -LiteralPath (Join-Path $installationDirectory 'uninstall-cleanup.ps1') -Destination $cleanupScript -Force
        Start-Process -FilePath 'powershell.exe' -WindowStyle Hidden -ArgumentList @(
            '-NoLogo', '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $cleanupScript,
            '-InstallationDirectory', $installationDirectory, '-ParentProcessId', $PID
        )

        $progress.Value = 100
        $status.Text = 'YEKDB kaldırıldı. Veritabanı verileriniz korundu.'
        [System.Windows.Threading.Dispatcher]::CurrentDispatcher.Invoke([Action]{}, 'Background')
        Start-Sleep -Milliseconds 900
        $window.Close()
    } catch {
        $uninstallButton.IsEnabled = $true
        $cancelButton.IsEnabled = $true
        $status.Text = "Kaldırma tamamlanamadı: $($_.Exception.Message)"
        [System.Windows.MessageBox]::Show($_.Exception.Message, 'YEKDB Kaldırma Hatası', 'OK', 'Error') | Out-Null
    }
})

$window.ShowDialog() | Out-Null
