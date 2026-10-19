#!/usr/bin/env python3
"""Optional documentation QA. Requires Python Playwright and a Chromium executable."""
from pathlib import Path
import json
import os
from playwright.sync_api import sync_playwright, expect

ROOT = Path(__file__).resolve().parents[1]
checks = []
def passed(name):
    checks.append({'name': name, 'status': 'passed'})
    print('PASS:', name)

with sync_playwright() as p:
    browser = p.chromium.launch(executable_path=os.environ.get('CHROMIUM_PATH', '/usr/bin/chromium'), args=['--no-sandbox'])
    page = browser.new_page(viewport={'width': 1440, 'height': 1050})
    errors = []
    page.on('pageerror', lambda error: errors.append(str(error)))
    page.set_content((ROOT / 'docs/BelegFlow-Handbook.html').read_text(), wait_until='load')
    expect(page.locator('html')).to_have_attribute('lang', 'en')
    expect(page.locator('.article:not([hidden])')).to_have_count(1)
    expect(page.locator('#article-en-user')).to_be_visible()
    expect(page.locator('#nav button')).to_have_count(12)
    passed('English is default; twelve guides and one active article')
    notice = page.locator('#notice').inner_text()
    assert all(value in notice for value in ('491', '224', '0.2.2', '100', 'must still run'))
    assert '0.2.2' in page.title()
    passed('Current release and correctly scoped CI/local evidence')
    page.locator('#nav button').filter(has_text='Module reference').click()
    expect(page.locator('#article-en-modules')).to_be_visible()
    expect(page.locator('#article-en-modules')).to_contain_text('0.2.2')
    page.get_by_role('button', name='DE', exact=True).click()
    expect(page.locator('html')).to_have_attribute('lang', 'de')
    expect(page.locator('#article-de-modules')).to_be_visible()
    passed('Module navigation and German switch retain the selected guide')
    page.locator('#search').fill('Abmeldung')
    expect(page.locator('#results')).to_be_visible()
    assert page.locator('#results .result').count() >= 1
    page.locator('#results .result').first.click()
    expect(page.locator('.article:not([hidden])')).to_have_count(1)
    expect(page.locator('#search')).to_have_value('')
    passed('Updated German search index opens a matching article')
    page.get_by_role('button', name='EN', exact=True).click()
    page.locator('#nav button').filter(has_text='Error diagnosis').click()
    expect(page.locator('#article-en-troubleshooting')).to_be_visible()
    page.screenshot(path=str(ROOT/'docs/screenshots/handbook-0.2.2-en.png'), full_page=False)
    page.set_viewport_size({'width':390,'height':844})
    assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
    page.get_by_role('button', name='DE', exact=True).click()
    assert page.evaluate('document.documentElement.scrollWidth <= innerWidth')
    passed('Handbook fits a 390px viewport in both languages')
    page.set_content((ROOT/'START-HERE.html').read_text(), wait_until='load')
    page.evaluate("location.hash=''")
    expect(page.locator('html')).to_have_attribute('lang', 'en')
    page.get_by_role('button', name='DE', exact=True).click()
    expect(page.locator('html')).to_have_attribute('lang','de')
    assert all(page.locator('[data-lang=de]').nth(i).is_visible() for i in range(page.locator('[data-lang=de]').count()))
    page.get_by_role('button', name='EN', exact=True).click()
    expect(page.locator('html')).to_have_attribute('lang','en')
    passed('START-HERE language buttons switch actual content')
    assert not errors, errors
    passed('No uncaught browser errors in documentation interactions')
    browser.close()

output = {'release':'0.2.2','scope':'Local Chromium documentation, not production application tests','passed':len(checks),'checks':checks}
(ROOT/'docs/testing/documentation-browser-0.2.2.json').write_text(json.dumps(output,indent=2)+'\n')
print(str(len(checks))+' documentation scenarios passed.')
