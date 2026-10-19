#!/usr/bin/env python3
"""Browser regressions for the explicitly labelled offline showcase, NOT production E2E.
Requires Python Playwright and Chromium. Uses real DOM interactions and the bundled
React 16 compatibility renderer; API/session persistence is browser-local synthetic data.
"""
from pathlib import Path
from playwright.sync_api import sync_playwright, expect
import hashlib
import json
import os

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / 'preview/BelegFlow-offline-showcase.html').read_text()
RESULTS = []
ERRORS = []
PASSWORD = 'BelegFlow-Demo-2026!'


def new_page(browser, width=1280, height=720):
    page = browser.new_page(viewport={'width': width, 'height': height})
    page.set_default_timeout(7000)
    page.on('pageerror', lambda error: ERRORS.append(str(error)))
    page.expose_function('localSha256', lambda values: list(hashlib.sha256(bytes(values)).digest()))
    page.evaluate("""() => {
      const memory=new Map();Object.defineProperty(window,'localStorage',{value:{getItem:k=>memory.get(k)??null,setItem:(k,v)=>memory.set(k,v),removeItem:k=>memory.delete(k),clear:()=>memory.clear()}});
      if(!crypto.randomUUID)crypto.randomUUID=()=>{const b=crypto.getRandomValues(new Uint8Array(16));b[6]=(b[6]&15)|64;b[8]=(b[8]&63)|128;const s=Array.from(b,x=>x.toString(16).padStart(2,'0')).join('');return s.slice(0,8)+'-'+s.slice(8,12)+'-'+s.slice(12,16)+'-'+s.slice(16,20)+'-'+s.slice(20)};
      if(!crypto.subtle)Object.defineProperty(crypto,'subtle',{value:{digest:async(_a,b)=>new Uint8Array(await window.localSha256(Array.from(new Uint8Array(b)))).buffer}});
    }""")
    page.set_content(SOURCE, wait_until='load')
    page.get_by_role('button', name='Administrator', exact=True).click()
    expect(page.locator('.workspace-grid')).to_be_visible()
    page.wait_for_function('!window.belegflowShowcase.store.getSnapshot().busy')
    return page


def settings(page):
    page.keyboard.press('Control+k')
    page.get_by_role('dialog').get_by_role('button', name='Workspace settings', exact=True).click()
    expect(page.locator('.standard-page')).to_be_visible()


def sign_in(page, email):
    form = page.locator('.login-box')
    expect(form.locator('button[type=submit]')).to_be_enabled()
    form.locator('input[name=email]').fill(email)
    form.locator('input[name=password]').fill(PASSWORD)
    form.locator('button[type=submit]').click()
    expect(page.locator('.workspace-grid')).to_be_visible()
    page.wait_for_function('!window.belegflowShowcase.store.getSnapshot().busy')
    assert page.evaluate('window.belegflowShowcase.store.getSnapshot().me.email') == email


def passed(name):
    RESULTS.append({'check': name, 'status': 'passed'})
    print('PASS', name, flush=True)


with sync_playwright() as p:
    browser = p.chromium.launch(executable_path=os.environ.get('BF_CHROMIUM', '/usr/bin/chromium'), args=['--no-sandbox'])
    for width, height in [(1440, 900), (1280, 560), (1024, 768), (390, 844), (844, 390)]:
        for locale in ['en', 'de']:
            page = new_page(browser, width, height)
            settings(page)
            page.get_by_role('button', name=locale.upper(), exact=True).click()
            label = 'Number and date formatting' if locale == 'en' else 'Zahlen- und Datumsformat'
            control = page.get_by_label(label, exact=True)
            expect(control).to_have_count(1)
            expect(page.get_by_role('combobox', name=label, exact=True)).to_have_count(1)
            control.select_option('de-DE')
            expect(control).to_have_value('de-DE')
            assert control.evaluate('(el)=>el.labels[0].textContent') == label
            assert page.evaluate('document.documentElement.scrollWidth<=innerWidth')
            button = page.get_by_role('button', name='Sign out' if locale == 'en' else 'Abmelden', exact=True)
            expect(button).to_have_count(1)
            button.click()  # Auto-scroll and real actionability; never force or dispatch a click.
            expect(page.locator('.login-box button[type=submit]')).to_be_enabled()
            sign_in(page, 'viewer@belegflow.demo')
            expect(page.get_by_role('button', name='Import invoice', exact=True)).to_have_count(0)
            passed(f'{width}x{height} {locale}: exact preference label, reachable sign-out, fresh read-only login')
            page.close()

    page = new_page(browser)
    settings(page)
    page.get_by_role('button', name='Add member', exact=True).click()
    page.locator('dialog input[name=display_name]').fill('Regression Viewer')
    page.locator('dialog input[name=email]').fill('regression@example.test')
    page.locator('dialog select[name=role]').select_option('VIEWER')
    page.locator('dialog input[name=password]').fill(PASSWORD)
    page.locator('dialog button[type=submit]').click()
    expect(page.locator('dialog')).to_have_count(0)
    expect(page.locator('tbody tr').filter(has_text='regression@example.test')).to_have_count(1)
    page.get_by_role('button', name='Sign out', exact=True).click()
    sign_in(page, 'regression@example.test')
    expect(page.get_by_role('button', name='Import invoice', exact=True)).to_have_count(0)
    passed('Administration: create viewer and switch identity through normal controls at 1280x720')
    page.close()

    page = new_page(browser)
    settings(page)
    threshold = page.get_by_label('Second-approval threshold (EUR)', exact=True)
    assert threshold.get_attribute('aria-describedby') == 'settings-threshold-hint'
    threshold.fill('10000.50')
    page.locator('.settings-card form button[type=submit]').click()
    page.wait_for_function('!window.belegflowShowcase.store.getSnapshot().busy')
    page.get_by_label('Number and date formatting', exact=True).select_option('de-DE')
    page.get_by_role('button', name='DE', exact=True).click()
    expect(page.get_by_label('Zahlen- und Datumsformat', exact=True)).to_have_value('de-DE')
    page.get_by_role('button', name='EN', exact=True).click()
    page.set_content(SOURCE, wait_until='load')  # Re-bootstrap against preserved synthetic browser storage.
    page.wait_for_selector('.workspace-grid')
    settings(page)
    expect(page.get_by_label('Number and date formatting', exact=True)).to_have_value('de-DE')
    expect(page.get_by_label('Second-approval threshold (EUR)', exact=True)).to_have_value('10000.50')
    page.screenshot(path=str(ROOT/'docs/screenshots/settings-regression-en.png'), full_page=True)
    passed('Rules: threshold and number preference survive language switches and re-bootstrap independently')
    page.close()

    page = new_page(browser)
    page.evaluate("""() => {
      const api=window.belegflowShowcase.api;const original=api.logout.bind(api);
      api.logout=()=>new Promise(resolve=>{window.finishLogout=async()=>{await original();resolve();};});
    }""")
    page.get_by_role('button', name='Sign out', exact=True).click()
    expect(page.locator('.login-box form')).to_have_attribute('aria-busy', 'true')
    expect(page.locator('.login-box button[type=submit]')).to_be_disabled()
    expect(page.locator('.app-shell')).to_have_count(0)
    page.evaluate('window.finishLogout()')
    expect(page.locator('.login-box button[type=submit]')).to_be_enabled()
    sign_in(page, 'viewer@belegflow.demo')
    passed('Delayed synthetic logout: private content clears and login waits for termination')
    page.close()
    assert not ERRORS, ERRORS
    report = {'scope': 'Offline compatibility renderer (React 16), synthetic DemoApi and memory localStorage; NOT Spring/PostgreSQL/React 19 production E2E.', 'browser': browser.version, 'checks': RESULTS, 'passed': len(RESULTS), 'page_errors': ERRORS}
    (ROOT/'docs/testing/ui-regressions-0.2.2.json').write_text(json.dumps(report, indent=2))
    browser.close()
print('TOTAL', len(RESULTS))
