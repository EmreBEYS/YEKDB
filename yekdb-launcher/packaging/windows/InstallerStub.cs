using System;
using System.Diagnostics;
using System.IO;
using System.Reflection;
using System.Text;
using System.Windows.Forms;

internal static class InstallerStub
{
    [STAThread]
    private static int Main()
    {
        string tempDirectory = Path.Combine(Path.GetTempPath(), "YEKDB-Setup-" + Guid.NewGuid().ToString("N"));

        try
        {
            Directory.CreateDirectory(tempDirectory);
            ExtractResource("YEKDB.Payload", Path.Combine(tempDirectory, "payload.zip"), false);
            ExtractResource("YEKDB.Banner", Path.Combine(tempDirectory, "installer-banner.png"), false);
            ExtractResource("YEKDB.SetupScript", Path.Combine(tempDirectory, "setup.ps1"), true);
            ExtractResource("YEKDB.UninstallScript", Path.Combine(tempDirectory, "uninstall.ps1"), true);
            ExtractResource("YEKDB.UninstallCleanupScript", Path.Combine(tempDirectory, "uninstall-cleanup.ps1"), true);

            var process = Process.Start(new ProcessStartInfo
            {
                FileName = "powershell.exe",
                Arguments = "-NoLogo -NoProfile -ExecutionPolicy Bypass -File \"" + Path.Combine(tempDirectory, "setup.ps1") + "\"",
                UseShellExecute = false,
                CreateNoWindow = true,
                WorkingDirectory = tempDirectory
            });

            if (process == null)
                throw new InvalidOperationException("YEKDB kurulum ekranı başlatılamadı.");

            process.WaitForExit();
            return process.ExitCode;
        }
        catch (Exception error)
        {
            MessageBox.Show(error.Message, "YEKDB Setup", MessageBoxButtons.OK, MessageBoxIcon.Error);
            return 1;
        }
        finally
        {
            try { Directory.Delete(tempDirectory, true); } catch { }
        }
    }

    private static void ExtractResource(string resourceName, string outputPath, bool addUtf8Bom)
    {
        using (Stream input = Assembly.GetExecutingAssembly().GetManifestResourceStream(resourceName))
        {
            if (input == null)
                throw new InvalidOperationException("Kurulum kaynağı bulunamadı: " + resourceName);

            using (var output = new FileStream(outputPath, FileMode.Create, FileAccess.Write, FileShare.None))
            {
                if (addUtf8Bom)
                {
                    byte[] bom = Encoding.UTF8.GetPreamble();
                    output.Write(bom, 0, bom.Length);
                }
                input.CopyTo(output);
            }
        }
    }
}
