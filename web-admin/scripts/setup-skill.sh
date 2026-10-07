#!/usr/bin/env bash
set -euo pipefail
DEST=".agents/skills/web-design-guidelines"
mkdir -p "$DEST"
curl -fsSL https://raw.githubusercontent.com/vercel-labs/agent-skills/main/skills/web-design-guidelines/SKILL.md -o "$DEST/SKILL.md"
curl -fsSL https://raw.githubusercontent.com/vercel-labs/web-interface-guidelines/main/command.md -o "$DEST/command.md"
echo "Skill ready: $DEST"