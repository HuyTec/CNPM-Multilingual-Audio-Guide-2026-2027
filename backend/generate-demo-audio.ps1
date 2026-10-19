$ErrorActionPreference = 'Stop'
$module = Join-Path $PSScriptRoot 'multi-audio-guide'
$inputDirectory = Join-Path $module 'target/demo-audio-text'
$outputDirectory = Join-Path $module 'src/main/resources/static/demo-audio'
New-Item -ItemType Directory -Force $inputDirectory, $outputDirectory | Out-Null
$seed = Get-Content -Raw -Encoding utf8 (Join-Path $module 'src/main/resources/db/demo/V3_1__seed_short_guides.sql')
$texts = [regex]::Matches($seed, "'([^']+)', 'APPROVED'")
$names = @('museum', 'gallery', 'heritage')
if ($texts.Count -ne $names.Count) { throw 'Expected three narration scripts in V3_1 seed.' }
for ($index = 0; $index -lt $names.Count; $index++) {
    [IO.File]::WriteAllText((Join-Path $inputDirectory ($names[$index] + '.txt')), $texts[$index].Groups[1].Value, [Text.UTF8Encoding]::new($false))
}
# Generate speech from exactly the same text that Flyway inserts into the database.
docker run --rm -v "${inputDirectory}:/input:ro" -v "${outputDirectory}:/output" alpine:3.22 sh -c 'apk add --no-cache espeak-ng >/dev/null && for name in museum gallery heritage; do espeak-ng -v vi -s 155 -f /input/$name.txt -w /output/$name.wav || exit; done'
if ($LASTEXITCODE -ne 0) { throw 'Audio generation failed.' }
Get-ChildItem -LiteralPath $outputDirectory -Filter '*.wav' | Select-Object Name, Length
