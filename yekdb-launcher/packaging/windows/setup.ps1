$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName PresentationFramework, PresentationCore, WindowsBase, System.Windows.Forms

[xml]$xaml = @'
<Window xmlns="http://schemas.microsoft.com/winfx/2006/xaml/presentation" Title="YEKDB Setup" Width="820" Height="500" WindowStartupLocation="CenterScreen" ResizeMode="NoResize" Background="#0B0910" Foreground="#F5F1FA">
  <Grid>
    <Grid.ColumnDefinitions><ColumnDefinition Width="280"/><ColumnDefinition Width="*"/></Grid.ColumnDefinitions>
    <Border Grid.Column="0" Background="#100D17"><Grid><Image Name="Banner" Stretch="UniformToFill"/><Border Background="#30000000"/></Grid></Border>
    <Grid Grid.Column="1" Margin="34,28,34,24">
      <Grid.RowDefinitions><RowDefinition Height="*"/><RowDefinition Height="Auto"/></Grid.RowDefinitions>
      <StackPanel>
        <TextBlock Text="YEKDB KURULUMU" Foreground="#C084FC" FontSize="12" FontWeight="Bold"/>
        <TextBlock Text="Launcher ve Database Engine" FontSize="28" FontWeight="Bold" Margin="0,10,0,8"/>
        <TextBlock Text="Bu yükleyici YEKDB Launcher ile YEKDB veritabanı çekirdeğini birlikte kurar." Foreground="#B8AFBF" TextWrapping="Wrap" FontSize="14" Margin="0,0,0,26"/>
        <TextBlock Text="Kurulum konumu (YEKDB klasörü otomatik oluşturulur)" FontWeight="SemiBold" Margin="0,0,0,7"/>
        <Grid><Grid.ColumnDefinitions><ColumnDefinition Width="*"/><ColumnDefinition Width="Auto"/></Grid.ColumnDefinitions>
          <TextBox Name="InstallPath" Height="34" Padding="8" Background="#17131E" Foreground="White" BorderBrush="#49365E"/>
          <Button Name="BrowseButton" Grid.Column="1" Content="Gözat" Margin="8,0,0,0" Width="74" Background="#28202F" Foreground="White" BorderBrush="#5B4471"/>
        </Grid>
        <CheckBox Name="DesktopShortcut" Content="Masaüstü kısayolu oluştur" IsChecked="True" Margin="0,18,0,0" Foreground="#D6CEDF"/>
        <ProgressBar Name="Progress" Height="8" Margin="0,28,0,0" IsIndeterminate="True" Visibility="Collapsed" Foreground="#9B5DE5" Background="#241C2C"/>
        <TextBlock Name="Status" Text="Kuruluma hazır." Foreground="#91869D" Margin="0,10,0,0"/>
      </StackPanel>
      <StackPanel Grid.Row="1" Orientation="Horizontal" HorizontalAlignment="Right">
        <Button Name="CancelButton" Content="İptal" Width="86" Height="36" Margin="0,0,10,0" Background="#221C29" Foreground="White" BorderBrush="#473752"/>
        <Button Name="InstallButton" Content="Kur" Width="100" Height="36" Background="#8B5CF6" Foreground="White" BorderBrush="#A77AF7" FontWeight="Bold"/>
      </StackPanel>
    </Grid>
  </Grid>
</Window>
'@

$window = [Windows.Markup.XamlReader]::Load((New-Object System.Xml.XmlNodeReader $xaml))
$installPath = $window.FindName('InstallPath'); $browseButton = $window.FindName('BrowseButton')
$installButton = $window.FindName('InstallButton'); $cancelButton = $window.FindName('CancelButton')
$desktopShortcut = $window.FindName('DesktopShortcut'); $progress = $window.FindName('Progress')
$status = $window.FindName('Status'); $banner = $window.FindName('Banner')
$existingInstall = (Get-ItemProperty -LiteralPath 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\YEKDB' -Name InstallLocation -ErrorAction SilentlyContinue).InstallLocation
$installPath.Text = if ($existingInstall -and (Test-Path -LiteralPath $existingInstall)) { $existingInstall } else { Join-Path $env:LOCALAPPDATA 'Programs\YEKDB' }
$banner.Source = New-Object Windows.Media.Imaging.BitmapImage([Uri](Join-Path $PSScriptRoot 'installer-banner.png'))

$browseButton.Add_Click({
    $dialog = New-Object System.Windows.Forms.FolderBrowserDialog
    $dialog.Description = 'Ana klasörü seçin. Yükleyici bunun içinde YEKDB klasörünü oluşturacak.'
    if ($dialog.ShowDialog() -eq [System.Windows.Forms.DialogResult]::OK) {
        $selected = [IO.Path]::GetFullPath($dialog.SelectedPath)
        $registeredInstall = if ($existingInstall) { [IO.Path]::GetFullPath($existingInstall).TrimEnd('\') } else { $null }
        $selectedNormalized = $selected.TrimEnd('\')
        $isInstalledFolder = (Test-Path -LiteralPath (Join-Path $selected 'YEKDB Launcher.exe')) -or
            ($registeredInstall -and $selectedNormalized -ieq $registeredInstall)
        $installPath.Text = if ($isInstalledFolder) { $selectedNormalized } else { Join-Path $selected 'YEKDB' }
    }
})
$cancelButton.Add_Click({ $window.Close() })
$installButton.Add_Click({
    $destination = $null
    $staging = $null
    $backup = $null
    $installedNewVersion = $false
    $rollbackRestored = $false
    try {
        $requestedDestination = $installPath.Text.Trim()
        if ([string]::IsNullOrWhiteSpace($requestedDestination)) { throw 'Kurulum klasörü boş olamaz.' }
        $fullDestination = [IO.Path]::GetFullPath($requestedDestination)
        $destination = $fullDestination.TrimEnd('\')
        $driveRoot = [IO.Path]::GetPathRoot($fullDestination).TrimEnd('\')
        if ($destination -ieq $driveRoot) { throw 'Disk köküne doğrudan kurulum yapılamaz. YEKDB klasörünü seçin.' }
        $parent = Split-Path -Parent $destination
        if ([string]::IsNullOrWhiteSpace($parent)) { throw 'Geçersiz kurulum klasörü.' }
        if (Test-Path -LiteralPath $destination) {
            $containsFiles = $null -ne (Get-ChildItem -LiteralPath $destination -Force -ErrorAction Stop | Select-Object -First 1)
            $isYekdbInstall = (Test-Path -LiteralPath (Join-Path $destination 'YEKDB Launcher.exe')) -and
                (Test-Path -LiteralPath (Join-Path $destination 'app\YEKDB Launcher.cfg'))
            if ($containsFiles -and -not $isYekdbInstall) {
                throw 'Seçilen hedefte YEKDB kurulumu olmayan dolu bir klasör var. Bir üst klasörü seçin; yükleyici YEKDB klasörünü kendisi oluştursun.'
            }
        }
        $staging = Join-Path $parent ('.yekdb-install-' + [Guid]::NewGuid().ToString('N'))
        $backup = Join-Path $parent ('.yekdb-backup-' + [Guid]::NewGuid().ToString('N'))

        $installButton.IsEnabled = $false; $browseButton.IsEnabled = $false; $progress.Visibility = 'Visible'; $status.Text = 'Güncelleme paketi hazırlanıyor…'
        [System.Windows.Threading.Dispatcher]::CurrentDispatcher.Invoke([Action]{}, 'Background')
        New-Item -ItemType Directory -Force -Path $parent, $staging | Out-Null
        Expand-Archive -LiteralPath (Join-Path $PSScriptRoot 'payload.zip') -DestinationPath $staging -Force
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'uninstall.ps1') -Destination $staging -Force
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'uninstall-cleanup.ps1') -Destination $staging -Force
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'installer-banner.png') -Destination (Join-Path $staging 'uninstaller-banner.png') -Force
        foreach ($preservedDirectory in @('data', 'config')) {
            $existingData = Join-Path $destination $preservedDirectory
            if ((Test-Path -LiteralPath $existingData) -and -not (Test-Path -LiteralPath (Join-Path $staging $preservedDirectory))) {
                Copy-Item -LiteralPath $existingData -Destination $staging -Recurse -Force
            }
        }

        $stagedLauncher = Join-Path $staging 'YEKDB Launcher.exe'
        $stagedEngine = Join-Path $staging 'YEKDB Engine.exe'
        $stagedUninstaller = Join-Path $staging 'YEKDB Uninstall.exe'
        $stagedConfig = Join-Path $staging 'app\YEKDB Launcher.cfg'
        if (-not (Test-Path -LiteralPath $stagedLauncher) -or -not (Test-Path -LiteralPath $stagedEngine) -or -not (Test-Path -LiteralPath $stagedUninstaller) -or -not (Test-Path -LiteralPath $stagedConfig)) {
            throw 'Güncelleme paketi eksik veya bozuk.'
        }

        $status.Text = 'Çalışan YEKDB işlemleri güvenle kapatılıyor…'
        [System.Windows.Threading.Dispatcher]::CurrentDispatcher.Invoke([Action]{}, 'Background')
        $running = @(Get-Process -Name 'YEKDB Launcher', 'YEKDB Engine' -ErrorAction SilentlyContinue | Where-Object {
            try { $_.Path -and [IO.Path]::GetFullPath($_.Path).StartsWith($destination, [StringComparison]::OrdinalIgnoreCase) }
            catch { $false }
        })
        foreach ($process in $running) {
            if ($process.CloseMainWindow()) { $process.WaitForExit(1500) | Out-Null }
            if (-not $process.HasExited) { Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue }
        }

        $status.Text = 'Yeni sürüm kuruluyor…'
        [System.Windows.Threading.Dispatcher]::CurrentDispatcher.Invoke([Action]{}, 'Background')
        if (Test-Path -LiteralPath $destination) { Move-Item -LiteralPath $destination -Destination $backup }
        Move-Item -LiteralPath $staging -Destination $destination
        $installedNewVersion = $true

        $shell = New-Object -ComObject WScript.Shell
        $launcher = Join-Path $destination 'YEKDB Launcher.exe'
        $icon = Join-Path $destination 'yekdb.ico'
        if (-not (Test-Path -LiteralPath $launcher)) { throw "YEKDB Launcher bulunamadı: $launcher" }
        $startMenu = Join-Path $env:APPDATA 'Microsoft\Windows\Start Menu\Programs\YEKDB Launcher.lnk'
        $shortcut = $shell.CreateShortcut($startMenu); $shortcut.TargetPath = $launcher; $shortcut.WorkingDirectory = $destination
        if (Test-Path -LiteralPath $icon) { $shortcut.IconLocation = "$icon,0" }
        $shortcut.Save()
        $uninstallScript = Join-Path $destination 'uninstall.ps1'
        $uninstaller = Join-Path $destination 'YEKDB Uninstall.exe'
        $uninstallShortcut = Join-Path $env:APPDATA 'Microsoft\Windows\Start Menu\Programs\YEKDB Kaldır.lnk'
        $shortcut = $shell.CreateShortcut($uninstallShortcut)
        $shortcut.TargetPath = $uninstaller
        $shortcut.WorkingDirectory = $destination
        if (Test-Path -LiteralPath $icon) { $shortcut.IconLocation = "$icon,0" }
        $shortcut.Save()
        if ($desktopShortcut.IsChecked) {
            $desktop = Join-Path ([Environment]::GetFolderPath('Desktop')) 'YEKDB Launcher.lnk'
            $shortcut = $shell.CreateShortcut($desktop); $shortcut.TargetPath = $launcher; $shortcut.WorkingDirectory = $destination
            if (Test-Path -LiteralPath $icon) { $shortcut.IconLocation = "$icon,0" }
            $shortcut.Save()
        }

        $configFile = Join-Path $destination 'app\YEKDB Launcher.cfg'
        $version = '1.0.0'
        if (Test-Path -LiteralPath $configFile) {
            $versionMatch = [regex]::Match((Get-Content -Raw -LiteralPath $configFile), 'jpackage\.app-version=([^\r\n]+)')
            if ($versionMatch.Success) { $version = $versionMatch.Groups[1].Value.Trim() }
        }
        $uninstallKey = 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Uninstall\YEKDB'
        New-Item -Path $uninstallKey -Force | Out-Null
        $estimatedSize = [Math]::Ceiling(((Get-ChildItem -LiteralPath $destination -Recurse -File | Measure-Object Length -Sum).Sum) / 1KB)
        New-ItemProperty -Path $uninstallKey -Name DisplayName -Value 'YEKDB Launcher ve Database Engine' -PropertyType String -Force | Out-Null
        New-ItemProperty -Path $uninstallKey -Name DisplayVersion -Value $version -PropertyType String -Force | Out-Null
        New-ItemProperty -Path $uninstallKey -Name Publisher -Value 'YEKDB' -PropertyType String -Force | Out-Null
        New-ItemProperty -Path $uninstallKey -Name InstallLocation -Value $destination -PropertyType String -Force | Out-Null
        New-ItemProperty -Path $uninstallKey -Name DisplayIcon -Value "$icon,0" -PropertyType String -Force | Out-Null
        New-ItemProperty -Path $uninstallKey -Name UninstallString -Value "`"$uninstaller`"" -PropertyType String -Force | Out-Null
        New-ItemProperty -Path $uninstallKey -Name EstimatedSize -Value ([int]$estimatedSize) -PropertyType DWord -Force | Out-Null
        New-ItemProperty -Path $uninstallKey -Name NoModify -Value 1 -PropertyType DWord -Force | Out-Null
        New-ItemProperty -Path $uninstallKey -Name NoRepair -Value 1 -PropertyType DWord -Force | Out-Null
        New-ItemProperty -Path $uninstallKey -Name InstalledDate -Value (Get-Date -Format 'yyyyMMdd') -PropertyType String -Force | Out-Null
        $progress.IsIndeterminate = $false; $progress.Value = 92; $status.Text = 'Yeni sürüm doğrulanıyor…'
        $launched = Start-Process -FilePath $launcher -PassThru
        Start-Sleep -Seconds 3
        if ($launched.HasExited) { throw "Yeni Launcher başlatılamadı (çıkış kodu: $($launched.ExitCode))." }
        if (Test-Path -LiteralPath $backup) { Remove-Item -LiteralPath $backup -Recurse -Force -ErrorAction SilentlyContinue }
        $progress.Value = 100; $status.Text = 'Kurulum tamamlandı. Verileriniz korundu.'
        Start-Sleep -Milliseconds 700; $window.Close()
    } catch {
        if ($installedNewVersion -and $destination -and (Test-Path -LiteralPath $destination)) {
            @(Get-Process -Name 'YEKDB Launcher', 'YEKDB Engine' -ErrorAction SilentlyContinue | Where-Object {
                try { $_.Path -and [IO.Path]::GetFullPath($_.Path).StartsWith($destination, [StringComparison]::OrdinalIgnoreCase) }
                catch { $false }
            }) | Stop-Process -Force -ErrorAction SilentlyContinue
            Remove-Item -LiteralPath $destination -Recurse -Force -ErrorAction SilentlyContinue
        }
        if ($backup -and (Test-Path -LiteralPath $backup) -and -not (Test-Path -LiteralPath $destination)) {
            Move-Item -LiteralPath $backup -Destination $destination -ErrorAction SilentlyContinue
            $rollbackRestored = Test-Path -LiteralPath $destination
        }
        if ($staging -and (Test-Path -LiteralPath $staging)) { Remove-Item -LiteralPath $staging -Recurse -Force -ErrorAction SilentlyContinue }
        $progress.Visibility = 'Collapsed'; $installButton.IsEnabled = $true; $browseButton.IsEnabled = $true
        $rollbackText = if ($rollbackRestored) { ' Önceki sürüm geri yüklendi.' } else { '' }
        $errorMessage = if ($_.Exception -is [System.UnauthorizedAccessException]) {
            'Bu konuma yazma yetkiniz yok. Belgeler, Masaüstü veya kullanıcı klasörünüzün içinden başka bir ana klasör seçin.'
        } else { $_.Exception.Message }
        $status.Text = "Kurulum başarısız.$rollbackText $errorMessage"
        [System.Windows.MessageBox]::Show($errorMessage, 'YEKDB Kurulum Hatası', 'OK', 'Error') | Out-Null
    }
})
$window.ShowDialog() | Out-Null
