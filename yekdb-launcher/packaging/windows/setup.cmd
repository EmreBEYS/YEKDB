@echo off
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -Command "$p = Join-Path '%~dp0' 'setup.ps1'; $c = [IO.File]::ReadAllText($p, (New-Object Text.UTF8Encoding($false))); & ([ScriptBlock]::Create($c))"
