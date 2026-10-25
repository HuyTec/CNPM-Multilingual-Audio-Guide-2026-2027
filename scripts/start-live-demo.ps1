# TouristWeb is retained for older commands; all four services now start by default.
param([switch]$Lan, [switch]$NoBuild, [switch]$TouristWeb)
$ErrorActionPreference = 'Stop'
$demoRoot = Split-Path $PSScriptRoot -Parent
$demoEnvPath = Join-Path $demoRoot '.env.demo.local'
if (-not (Test-Path -LiteralPath $demoEnvPath)) {
    $databasePassword = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(20))
    $adminPassword = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(12))
    $bindAddress = if ($Lan) { '0.0.0.0' } else { '127.0.0.1' }
    @(
        "POSTGRES_PASSWORD=$databasePassword", 'DEMO_ADMIN_USERNAME=admin', "DEMO_ADMIN_PASSWORD=$adminPassword",
        'SPRING_PROFILES_ACTIVE=demo', 'BACKEND_PORT=8081', 'WEB_PORT=3001', 'TOURIST_WEB_PORT=14176',
        "BACKEND_BIND_ADDRESS=$bindAddress", "WEB_BIND_ADDRESS=$bindAddress"
    ) | Set-Content -LiteralPath $demoEnvPath -Encoding ascii
    Write-Output 'Created private .env.demo.local with random passwords.'
} elseif ($Lan) {
    # Preserve existing credentials. Only opt in to the requested LAN bind addresses.
    $configuration = Get-Content -LiteralPath $demoEnvPath
    $configuration = $configuration -replace '^BACKEND_BIND_ADDRESS=.*', 'BACKEND_BIND_ADDRESS=0.0.0.0'
    $configuration = $configuration -replace '^WEB_BIND_ADDRESS=.*', 'WEB_BIND_ADDRESS=0.0.0.0'
    $configuration | Set-Content -LiteralPath $demoEnvPath -Encoding ascii
}
Push-Location $demoRoot
try {
    $composeArguments = @('compose', '--env-file', $demoEnvPath, '-f', (Join-Path $demoRoot 'compose.yaml'), '-p', 'hvp-live-demo')
    $composeArguments += @('up', '-d', '--wait')
    if (-not $NoBuild) { $composeArguments += '--build' }
    & docker @composeArguments
    if ($LASTEXITCODE -ne 0) { throw 'Demo services failed to start; inspect docker compose logs.' }
    $portArguments = @('compose', '--env-file', $demoEnvPath, '-f', (Join-Path $demoRoot 'compose.yaml'), '-p', 'hvp-live-demo', 'port')
    $adminEndpoint = & docker @portArguments web-admin 80
    if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve Web Admin port.' }
    $backendEndpoint = & docker @portArguments backend 8080
    if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve backend port.' }
    $touristEndpoint = & docker @portArguments web-user 4176
    if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve Tourist Web port.' }
    $adminPort = ($adminEndpoint | Select-Object -First 1) -replace '^.*:', ''
    $backendPort = ($backendEndpoint | Select-Object -First 1) -replace '^.*:', ''
    $touristPort = ($touristEndpoint | Select-Object -First 1) -replace '^.*:', ''
    Write-Output "Web Admin: http://localhost:$adminPort/analytics"
    Write-Output "Backend: http://localhost:$backendPort"
    Write-Output "Tourist Web: http://127.0.0.1:$touristPort; share with scripts/share-docker-web.ps1 -Port $touristPort"
    Write-Output "Android emulator server: http://10.0.2.2:$backendPort/"
    Write-Output "Phone on Wi-Fi: enter http://<PC Wi-Fi IPv4>:$backendPort/ in the app."
    Write-Output 'Database demo accounts: editor / EditorDemo!2026; reviewer / ReviewerDemo!2026 (first seed only).'
    Write-Output 'Override seed passwords with DEMO_EDITOR_PASSWORD / DEMO_REVIEWER_PASSWORD before first startup. Existing DB passwords are preserved.'
    Write-Output 'All four services share one Compose project and the persistent hvp-live-demo_postgres_data volume.'
} finally { Pop-Location }
