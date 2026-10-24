"""Render the public, approved VI demo scripts once; never called by a visitor.

Install edge-tts==7.3.1, then run with --base-url http://127.0.0.1:8081.
The public text is sent to Microsoft's online speech service. No credentials.
Commit the resulting immutable audio + manifest with the demo migration.
"""
import argparse
import asyncio
import hashlib
import json
from pathlib import Path
from urllib.request import urlopen

import edge_tts


def digest(data):
    return hashlib.sha256(data).hexdigest()


async def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--base-url', default='http://127.0.0.1:8081')
    args = parser.parse_args()
    output = Path(__file__).resolve().parents[1] / 'multi-audio-guide/src/main/resources/demo-narration'
    output.mkdir(parents=True, exist_ok=True)
    manifest = output / 'manifest.txt'
    previous = {row.split('|')[0]: row.split('|') for row in manifest.read_text(encoding='utf-8').splitlines()} if manifest.exists() else {}
    entries = []
    for number in range(101, 108):
        location_id = f'20000000-0000-0000-0000-{number:012d}'
        for script_type in ['FULL', 'SHORT']:
            with urlopen(f'{args.base_url}/api/locations/{location_id}/guide?language=VI&scriptType={script_type}', timeout=30) as response:
                guide = json.load(response)
            if guide['languageCode'] != 'VI' or guide['scriptType'] != script_type or guide['locationId'] != location_id:
                raise ValueError('Public guide did not match requested variant')
            filename = f'{number}-{script_type.lower()}.mp3'
            target = output / filename
            text_hash = digest(guide['scriptText'].encode('utf-8'))
            cached = previous.get(guide['contentId'])
            if cached and cached[1] == text_hash and target.exists() and digest(target.read_bytes()) == cached[3]:
                entries.append('|'.join(cached))
                continue
            for attempt in range(3):
                try:
                    await asyncio.wait_for(edge_tts.Communicate(guide['scriptText'], 'vi-VN-HoaiMyNeural', rate='-8%', pitch='-2Hz').save(str(target)), timeout=90)
                    break
                except Exception:
                    if attempt == 2:
                        raise
                    await asyncio.sleep(2)
            audio = target.read_bytes()
            if len(audio) < 1024:
                raise ValueError(f'Empty narration: {filename}')
            entries.append('|'.join([guide['contentId'], text_hash, filename, digest(audio)]))
            # Persist each successful rendering, so an interrupted run can resume safely.
            manifest.write_text('\n'.join(entries) + '\n', encoding='utf-8')
            print(f'{filename}: {len(audio)} bytes', flush=True)
    manifest.write_text('\n'.join(entries) + '\n', encoding='utf-8')


if __name__ == '__main__':
    asyncio.run(main())
