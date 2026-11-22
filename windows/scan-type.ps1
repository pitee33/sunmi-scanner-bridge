# L2 Scan Bridge - Windows receiver
# Types every scan received from the Sunmi L2 into the focused window.
# Usage:  powershell -ExecutionPolicy Bypass -File .\scan-type.ps1
#   -Port 8989     change port
#   -NoEnter       do not press Enter after the code
# The Windows Firewall asks on first start - allow it on the private network!

param(
    [int]$Port = 8989,
    [switch]$NoEnter
)

Add-Type -AssemblyName System.Windows.Forms

$listener = New-Object System.Net.Sockets.TcpListener([System.Net.IPAddress]::Any, $Port)
$listener.Start()
Write-Host ("L2 Scan Bridge listening on port {0}" -f $Port) -ForegroundColor Green
Write-Host "Scans are typed into the focused window. Ctrl+C to stop."

while ($true) {
    $client = $listener.AcceptTcpClient()
    try {
        $stream = $client.GetStream()
        $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
        while (-not $reader.EndOfStream) {
            $code = $reader.ReadLine()
            if ($code) {
                Write-Host ("{0}  {1}" -f (Get-Date -Format 'HH:mm:ss'), $code)
                $escaped = $code -replace '([+^%~{}()\[\]])', '{$1}'
                [System.Windows.Forms.SendKeys]::SendWait($escaped)
                if (-not $NoEnter) {
                    [System.Windows.Forms.SendKeys]::SendWait('{ENTER}')
                }
            }
        }
    } catch {
        Write-Host ("Error: " + $_.Exception.Message) -ForegroundColor Red
    } finally {
        $client.Close()
    }
}
