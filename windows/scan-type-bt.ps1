# L2 Scan Bridge - Bluetooth (SPP) receiver
# 1) On the L2: app -> "BT lathatosag (parositashoz)" button, then pair from Windows.
# 2) Find the outgoing COM port: Windows Bluetooth settings -> More Bluetooth options
#    -> COM Ports tab -> "Outgoing - L2 Scan Bridge (COMx)"
# Usage:  powershell -ExecutionPolicy Bypass -File .\scan-type-bt.ps1 -Port COM5
#   -NoEnter       do not press Enter after the code

param(
    [string]$Port = "COM5",
    [switch]$NoEnter
)

Add-Type -AssemblyName System.Windows.Forms

$sp = New-Object System.IO.Ports.SerialPort($Port, 115200)
$sp.NewLine = "`n"
$sp.Encoding = [System.Text.Encoding]::UTF8

try {
    $sp.Open()
} catch {
    Write-Host ("Cannot open " + $Port + " - check the COM port number!") -ForegroundColor Red
    exit 1
}

Write-Host ("L2 Scan Bridge (BT) listening on " + $Port) -ForegroundColor Green
Write-Host "Scans are typed into the focused window. Ctrl+C to stop."

while ($true) {
    try {
        $code = $sp.ReadLine()
        if ($code) {
            Write-Host ("{0}  {1}" -f (Get-Date -Format 'HH:mm:ss'), $code)
            $escaped = $code -replace '([+^%~{}()\[\]])', '{$1}'
            [System.Windows.Forms.SendKeys]::SendWait($escaped)
            if (-not $NoEnter) {
                [System.Windows.Forms.SendKeys]::SendWait('{ENTER}')
            }
        }
    } catch {
        Start-Sleep -Milliseconds 200
    }
}
