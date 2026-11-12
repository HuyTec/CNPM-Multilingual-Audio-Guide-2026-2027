"""Validate the split guides' local links and exact Spring method/path coverage."""
from pathlib import Path
import re
from urllib.parse import unquote

ROOT=Path(__file__).resolve().parents[1]
DOCS=ROOT/'docs'
FILES=['development/BACKEND_GUIDE.md','development/ADMIN_GUIDE.md','users/ADMIN_USER_GUIDE.md','development/USER_WEB_GUIDE.md','development/ANDROID_GUIDE.md','development/API_list.md','demo/DEMO_CODE_GUIDE.md','operations/DOCKER_GUIDE.md']
checked=0
for name in FILES:
    source=(DOCS/name).read_text(encoding='utf-8')
    for target in re.findall(r'\]\(([^)]+)\)',source):
        if target.startswith(('https://','http://','#','mailto:')):continue
        path=unquote(target.split('#')[0].strip('<>'))
        assert ((DOCS/name).parent/path).exists(),f'{name}: missing {path}'
        checked+=1

actual=set()
for path in (ROOT/'backend/multi-audio-guide/src/main/java/com/application/controller').glob('*.java'):
    source=path.read_text(encoding='utf-8')
    prefix=re.search(r'@RequestMapping\("([^"]+)"\)',source)
    prefix=prefix.group(1) if prefix else ''
    for match in re.finditer(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\(([^\n]*?)\))?',source):
        route=re.search(r'(?:value\s*=\s*)?"([^"]+)"',match.group(2) or '')
        actual.add((match.group(1).upper(),prefix+(route.group(1) if route else '')))
documented=set(re.findall(r'\| (GET|POST|PUT|PATCH|DELETE) \| ([^ |]+) \|',(DOCS/'development/API_list.md').read_text(encoding='utf-8')))
assert actual==documented,f'Missing {actual-documented}; extra {documented-actual}'
print(f'PASS: {len(FILES)} guides, {checked} local links, {len(actual)} exact method/path mappings')
