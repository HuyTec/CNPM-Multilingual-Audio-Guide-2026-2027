"""Prepare the two VI recordings for the new, immutable Xá Lợi media migration.

The public guide text is sent to Microsoft's speech service (edge-tts 7.3.1).
Do not regenerate resources after V8.2 has been applied; use a new migration.
"""
import asyncio
import hashlib
import json
from pathlib import Path
import edge_tts

ROOT = Path(__file__).resolve().parents[2]
OUTPUT = ROOT / 'backend/multi-audio-guide/src/main/resources/xa-loi'


async def main():
    source = json.loads((ROOT / 'docs/vi/xa_loi_content.json').read_text(encoding='utf-8'))
    OUTPUT.mkdir(parents=True, exist_ok=True)
    entries = []
    for index, mode in enumerate(['FULL', 'SHORT'], 1):
        text = source['guides']['VI'][mode]
        filename = f'108-{mode.lower()}.mp3'
        target = OUTPUT / filename
        if not target.exists():
            for attempt in range(3):
                try:
                    await asyncio.wait_for(edge_tts.Communicate(text, 'vi-VN-HoaiMyNeural', rate='-8%', pitch='-2Hz').save(str(target)), timeout=90)
                    break
                except Exception:
                    if attempt == 2:
                        raise
                    await asyncio.sleep(2)
        audio = target.read_bytes()
        if len(audio) < 1024:
            raise ValueError(f'Invalid narration: {filename}')
        entries.append('|'.join([
            f'32000000-0000-0000-0000-{index:012d}',
            hashlib.sha256(text.encode('utf-8')).hexdigest(), filename,
            hashlib.sha256(audio).hexdigest(),
        ]))
        print(f'{filename}: {len(audio)} bytes', flush=True)
    (OUTPUT / 'manifest.txt').write_text('\n'.join(entries) + '\n', encoding='utf-8')


if __name__ == '__main__':
    asyncio.run(main())
