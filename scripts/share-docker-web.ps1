param([int]$Port=14176,[switch]$Stop,[switch]$Restart)
$ErrorActionPreference='Stop'
$shareRoot=Split-Path -Parent $PSScriptRoot
$shareRuntime=Join-Path $shareRoot '.local-tools'
$shareState=Join-Path $shareRuntime 'docker-web-share.json'
$shareExe=Join-Path $shareRuntime 'cloudflared.exe'
$shareLog=Join-Path $shareRuntime 'docker-web-tunnel-error.log'
New-Item -ItemType Directory -Path $shareRuntime -Force | Out-Null
$savedShare=if(Test-Path -LiteralPath $shareState){Get-Content -LiteralPath $shareState -Raw|ConvertFrom-Json}else{$null}
if($savedShare -and -not $savedShare.url -and (Test-Path -LiteralPath $shareLog)){
    $recoveredLog=Get-Content -LiteralPath $shareLog -Raw
    if(-not [string]::IsNullOrEmpty($recoveredLog)){
        $recoveredUrl=[regex]::Match($recoveredLog,'https://[a-z0-9-]+\.trycloudflare\.com')
        if($recoveredUrl.Success){$savedShare.url=$recoveredUrl.Value;$savedShare|ConvertTo-Json|Set-Content -LiteralPath $shareState -Encoding utf8}
    }
}
function Get-ShareProcess($state) {
    if(-not $state){return $null}
    $candidate=Get-CimInstance Win32_Process -Filter "ProcessId=$([int]$state.pid)" -ErrorAction SilentlyContinue
    if($candidate -and $candidate.CommandLine -like "*$shareExe*" -and $candidate.CommandLine -like "*127.0.0.1:$($state.port)*"){return $candidate}
    return $null
}
if($Stop -or $Restart){
    if(Get-ShareProcess $savedShare){Stop-Process -Id $savedShare.pid}
    if(Test-Path -LiteralPath $shareState){Remove-Item -LiteralPath $shareState}
    if($Stop){Write-Output 'Stopped the owned Docker Web tunnel.';exit}
    $savedShare=$null
}
if($Port -lt 1 -or $Port -gt 65535){throw 'Invalid port.'}
$null=Invoke-WebRequest "http://127.0.0.1:$Port/api/categories" -UseBasicParsing -TimeoutSec 10
if($savedShare -and $savedShare.url -and (Get-ShareProcess $savedShare) -and [int]$savedShare.port -eq $Port){Write-Output $savedShare.url;exit}
if(-not (Test-Path -LiteralPath $shareExe)){
    $shareVersion='2026.10.3'
    Invoke-WebRequest "https://github.com/cloudflare/cloudflared/releases/download/$shareVersion/cloudflared-windows-amd64.exe" -OutFile $shareExe
}
$shareHash=(Get-FileHash -LiteralPath $shareExe -Algorithm SHA256).Hash.ToLowerInvariant()
if($shareHash -ne '76b2805253d0d6ffacb092a7acfbee99e7af6917835c0c5410e23f3ef5052da2'){throw 'cloudflared differs from the pinned official binary; verify/update its version and digest first.'}
$shareProcess=Start-Process -FilePath $shareExe -ArgumentList @('tunnel','--no-autoupdate','--protocol','http2','--url',"http://127.0.0.1:$Port") -WorkingDirectory $shareRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $shareRuntime 'docker-web-tunnel.log') -RedirectStandardError $shareLog
@{pid=$shareProcess.Id;port=$Port;url='';startedAt=(Get-Date).ToString('o')}|ConvertTo-Json|Set-Content -LiteralPath $shareState -Encoding utf8
$shareUrl=$null
for($shareAttempt=0;$shareAttempt -lt 80;$shareAttempt++){
    if(Test-Path -LiteralPath $shareLog){$shareText=Get-Content -LiteralPath $shareLog -Raw;if(-not [string]::IsNullOrEmpty($shareText)){$shareMatch=[regex]::Match($shareText,'https://[a-z0-9-]+\.trycloudflare\.com');if($shareMatch.Success){$shareUrl=$shareMatch.Value;break}}}
    if($shareProcess.HasExited){break};Start-Sleep -Milliseconds 500
}
if(-not $shareUrl){if(-not $shareProcess.HasExited){Stop-Process -Id $shareProcess.Id};throw "No URL returned; inspect $shareLog"}
@{pid=$shareProcess.Id;port=$Port;url=$shareUrl;startedAt=(Get-Date).ToString('o')}|ConvertTo-Json|Set-Content -LiteralPath $shareState -Encoding utf8
Write-Output $shareUrl
Write-Output 'Keep Docker, this PC and cloudflared running. Restarting the tunnel may change the URL.'
