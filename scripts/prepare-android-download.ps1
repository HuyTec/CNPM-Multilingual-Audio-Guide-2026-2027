param([switch]$SkipBuild)
$ErrorActionPreference = 'Stop'
$downloadRoot = Split-Path -Parent $PSScriptRoot
if (-not $SkipBuild) {
    Push-Location (Join-Path $downloadRoot 'android')
    try {
        & .\gradlew.bat :app:assembleDebug '-PdemoBuildDir=.verification-build'
        if ($LASTEXITCODE -ne 0) { throw 'Android build failed.' }
    } finally { Pop-Location }
}
$apkSource = Join-Path $downloadRoot 'android/.verification-build/app/outputs/apk/debug/app-debug.apk'
if (-not (Test-Path -LiteralPath $apkSource)) { throw 'Build the APK first; no current demo APK found.' }
$downloadDir = Join-Path $downloadRoot 'web-user/public/downloads'
New-Item -ItemType Directory -Path $downloadDir -Force | Out-Null
$apkTarget = Join-Path $downloadDir 'travelvoice.apk'
Copy-Item -LiteralPath $apkSource -Destination $apkTarget -Force
$release = Get-Content -LiteralPath (Join-Path $downloadRoot 'android/.verification-build/app/outputs/apk/debug/output-metadata.json') -Raw | ConvertFrom-Json
$metadata = [ordered]@{
    url = '/downloads/travelvoice.apk'
    version = $release.elements[0].versionName
    sizeBytes = (Get-Item -LiteralPath $apkTarget).Length
    sha256 = (Get-FileHash -LiteralPath $apkTarget -Algorithm SHA256).Hash.ToLowerInvariant()
}
$metadataTarget=Join-Path $downloadDir 'android-release.json'
$metadataTemporary=Join-Path $downloadDir ('android-release.'+[Guid]::NewGuid().ToString('N')+'.tmp')
[IO.File]::WriteAllText($metadataTemporary,($metadata|ConvertTo-Json),[Text.UTF8Encoding]::new($false))
for($metadataAttempt=0;$metadataAttempt -lt 20;$metadataAttempt++){
    try { Move-Item -LiteralPath $metadataTemporary -Destination $metadataTarget -Force;break }
    catch { if($metadataAttempt -eq 19){throw};Start-Sleep -Milliseconds 250 }
}
Write-Output "Prepared Android $($metadata.version), $($metadata.sizeBytes) bytes for Web User."
Write-Output 'Run the Web User build after this step to include downloads in dist.'
