using System;
using System.Diagnostics;
using System.IO;
using System.Windows.Forms;

internal static class UninstallerStub
{
    [STAThread]
    private static int Main()
    {
        try
        {
            string installationDirectory = AppDomain.CurrentDomain.BaseDirectory;
            string script = Path.Combine(installationDirectory, "uninstall.ps1");
            if (!File.Exists(script))
                throw new FileNotFoundException("YEKDB kaldırma dosyası bulunamadı.", script);

            var process = Process.Start(new ProcessStartInfo
            {
                FileName = "powershell.exe",
                Arguments = "-NoLogo -NoProfile -WindowStyle Hidden -ExecutionPolicy Bypass -File \"" + script + "\"",
                UseShellExecute = false,
                CreateNoWindow = true,
                WorkingDirectory = installationDirectory
            });
            if (process == null) throw new InvalidOperationException("YEKDB kaldırma arayüzü başlatılamadı.");
            process.WaitForExit();
            return process.ExitCode;
        }
        catch (Exception error)
        {
            MessageBox.Show(error.Message, "YEKDB Kaldırma", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return 1;
        }
    }
}
