param([string]$FilePath, [int]$TimeoutMs)
Add-Type -AssemblyName System.Windows.Forms
Add-Type @'
using System;
using System.Runtime.InteropServices;
public class SaveDialogWin32 {
    [DllImport("user32.dll")] public static extern IntPtr FindWindow(string lpClassName, string lpWindowName);
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr hWnd);
}
'@

$deadline = (Get-Date).AddMilliseconds($TimeoutMs)
$hwnd = [IntPtr]::Zero
while ((Get-Date) -lt $deadline) {
    $hwnd = [SaveDialogWin32]::FindWindow("#32770", $null)
    if ($hwnd -ne [IntPtr]::Zero) { break }
    Start-Sleep -Milliseconds 200
}
if ($hwnd -eq [IntPtr]::Zero) {
    Write-Output "NOTFOUND"
    exit 1
}
[SaveDialogWin32]::SetForegroundWindow($hwnd) | Out-Null
Start-Sleep -Milliseconds 300
$escaped = $FilePath -replace '([+^%~(){}])', '{$1}'
[System.Windows.Forms.SendKeys]::SendWait("^a")
[System.Windows.Forms.SendKeys]::SendWait($escaped)
Start-Sleep -Milliseconds 200
[System.Windows.Forms.SendKeys]::SendWait("{ENTER}")
Write-Output "OK"
exit 0
