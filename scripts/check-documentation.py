#!/usr/bin/env python3
"""Check local Markdown links and required bilingual guide pairs using the standard library."""
from pathlib import Path
import re
import sys

root=Path(__file__).resolve().parents[1]
required=('USER_GUIDE','DEVELOPER_GUIDE','MODULE_REFERENCE','TESTING','TROUBLESHOOTING','OPERATIONS','RELEASE')
errors=[]
for lang in ('en','de'):
    for name in required:
        file=root/'docs'/lang/(name+'.md')
        if not file.is_file() or len(file.read_text().strip())<100:
            errors.append('Missing/empty bilingual guide: '+str(file.relative_to(root)))
for file in [root/'README.md',root/'README.de.md',*sorted((root/'docs').rglob('*.md'))]:
    for href in re.findall(r'\]\(([^)]+)\)',file.read_text()):
        if href.startswith(('http:','https:','mailto:','#')):
            continue
        target=href.split('#')[0]
        if target and not (file.parent/target).exists():
            errors.append(str(file.relative_to(root))+': missing '+href)
for error in errors:
    print('FAIL',error)
if not errors:
    print('PASS: required EN/DE guide pairs and local Markdown links. External URLs were not fetched.')
sys.exit(1 if errors else 0)
