param([switch]$Stop, [switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$repoRoot = Split-Path $projectRoot -Parent
$runtime = Join-Path $repoRoot '.local-tools'
$stateFile = Join-Path $runtime 'phone-share.json'
$cloudflared = Join-Path $runtime 'cloudflared.exe'
$gatewayPath = Join-Path $PSScriptRoot 'public-server.mjs'
$qrPath = Join-Path $repoRoot 'docs/qr/web-user-demo'
New-Item -ItemType Directory -Path $runtime -Force | Out-Null

function Get-OwnedProcess($processId, $pattern) {
    if (!$processId) { return $null }
    $found = Get-CimInstance Win32_Process -Filter "ProcessId = $([int]$processId)" -ErrorAction SilentlyContinue
    if ($found -and $found.CommandLine -like "*$pattern*") { return $found }
    return $null
}

$saved = $null
if (Test-Path $stateFile) { $saved = Get-Content $stateFile -Raw | ConvertFrom-Json }
if ($Stop) {
    if ($saved) {
        if (Get-OwnedProcess $saved.tunnelPid $cloudflared) { Stop-Process -Id $saved.tunnelPid }
        if (Get-OwnedProcess $saved.gatewayPid $gatewayPath) { Stop-Process -Id $saved.gatewayPid }
        Remove-Item -LiteralPath $stateFile
    }
    Write-Output 'Da dung link demo va cong chia se Web User.'
    exit
}
if (!(Test-Path $cloudflared)) { throw 'Chua co .local-tools/cloudflared.exe. Xem huong dan QR trong web-user/README.md.' }

if ($saved -and !$saved.url -and (Test-Path (Join-Path $runtime 'phone-tunnel-error.log'))) {
    $previousLog = Get-Content (Join-Path $runtime 'phone-tunnel-error.log') -Raw
    if ($previousLog) {
        $previousUrl = [regex]::Match($previousLog, 'https://[a-z0-9-]+\.trycloudflare\.com')
        if ($previousUrl.Success) { $saved.url = $previousUrl.Value; $saved | ConvertTo-Json | Set-Content $stateFile }
    }
}

# Keep an existing healthy link so its printed QR remains usable.
if ($saved -and (Get-OwnedProcess $saved.gatewayPid $gatewayPath) -and (Get-OwnedProcess $saved.tunnelPid $cloudflared)) {
    try {
        $response = Invoke-WebRequest ($saved.url + '/api/categories') -UseBasicParsing -TimeoutSec 10
        if ($response.StatusCode -eq 200) {
            if (!(Test-Path ($qrPath + '.png'))) { & python (Join-Path $PSScriptRoot 'make-qr.py') $saved.url --output $qrPath }
            Write-Output $saved.url; Write-Output ($qrPath + '.png'); exit
        }
    } catch { Write-Output 'Link cu khong phan hoi; tao lai link demo.' }
}
if (!$SkipBuild) {
    Push-Location $projectRoot
    try { & npm.cmd run build; if ($LASTEXITCODE -ne 0) { throw 'Build Web User failed.' } }
    finally { Pop-Location }
}
$gateway = Get-OwnedProcess $saved.gatewayPid $gatewayPath
if (!$gateway) {
    $gateway = Start-Process -FilePath (Get-Command node.exe).Source -ArgumentList ('"' + $gatewayPath + '"') -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $runtime 'phone-gateway.log') -RedirectStandardError (Join-Path $runtime 'phone-gateway-error.log')
}
$port = if ($env:PUBLIC_WEB_PORT) { [int]$env:PUBLIC_WEB_PORT } else { 4176 }
$ready = $false
for ($i = 0; $i -lt 20; $i++) {
    try {
        $null = Invoke-WebRequest "http://127.0.0.1:$port/api/categories" -UseBasicParsing -TimeoutSec 2
        $ready = $true; break
    } catch { Start-Sleep -Milliseconds 250 }
}
if (!$ready) { throw 'Cong Web User chua san sang. Kiem tra Docker/backend va .local-tools/phone-gateway-error.log.' }
if (Get-OwnedProcess $saved.tunnelPid $cloudflared) { Stop-Process -Id $saved.tunnelPid }
$tunnelLog = Join-Path $runtime 'phone-tunnel-error.log'
$tunnel = Start-Process -FilePath $cloudflared -ArgumentList @('tunnel', '--no-autoupdate', '--protocol', 'http2', '--url', "http://127.0.0.1:$port") -WorkingDirectory $repoRoot -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $runtime 'phone-tunnel.log') -RedirectStandardError $tunnelLog
$gatewayPid = if ($gateway.ProcessId) { $gateway.ProcessId } else { $gateway.Id }
@{ gatewayPid = $gatewayPid; tunnelPid = $tunnel.Id; url = ''; startedAt = (Get-Date).ToString('o') } | ConvertTo-Json | Set-Content $stateFile
$url = $null
for ($i = 0; $i -lt 90; $i++) {
    if (Test-Path $tunnelLog) {
        $logText = Get-Content $tunnelLog -Raw
        if (![string]::IsNullOrEmpty($logText)) {
            $match = [regex]::Match($logText, 'https://[a-z0-9-]+\.trycloudflare\.com')
            if ($match.Success) { $url = $match.Value; break }
        }
    }
    if ($tunnel.HasExited) { break }
    Start-Sleep -Milliseconds 500
}
if (!$url) { throw 'Chua tao duoc HTTPS tunnel. Kiem tra .local-tools/phone-tunnel-error.log.' }
@{ gatewayPid = $gatewayPid; tunnelPid = $tunnel.Id; url = $url; startedAt = (Get-Date).ToString('o') } | ConvertTo-Json | Set-Content $stateFile
& python (Join-Path $PSScriptRoot 'make-qr.py') $url --output $qrPath
if ($LASTEXITCODE -ne 0) { throw 'Chua tao duoc QR. Cai thu vien qrcode theo README.' }
Write-Output $url
Write-Output 'QR demo can giu may tinh, Docker va tunnel dang chay. Khoi dong lai tunnel co the doi URL.'
