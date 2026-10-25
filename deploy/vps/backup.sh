#!/bin/sh
set -eu
cd "$(dirname "$0")/../.."
umask 077
mkdir -p .local-tools/backups
output=".local-tools/backups/audio-guide-$(date -u +%Y%m%dT%H%M%SZ).dump"
docker compose --env-file .env.production -f compose.production.yaml exec -T postgres pg_dump -U audio_guide -d audio_guide -Fc > "$output"
test -s "$output"
printf '%s\n' "$output"
