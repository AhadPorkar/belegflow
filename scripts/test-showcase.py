from pathlib import Path
from playwright.sync_api import sync_playwright
import hashlib,json,zipfile,tempfile
ROOT=Path(__file__).resolve().parents[1]; shots=ROOT/'docs/screenshots'; results=[]; errors=[]
def passed(name):
 results.append({'check':name,'status':'passed'});print('PASS',name,flush=True)
with sync_playwright() as p:
 browser=p.chromium.launch(executable_path=__import__('os').environ.get('BF_CHROMIUM', '/usr/bin/chromium'),headless=True,args=['--no-sandbox'])
 page=browser.new_page(viewport={'width':1440,'height':1040},device_scale_factor=1,accept_downloads=True)
 page.on('pageerror',lambda e:errors.append(str(e)))
 page.expose_function('localSha256',lambda values:list(hashlib.sha256(bytes(values)).digest()))
 page.evaluate("""() => {
 const memory=new Map();Object.defineProperty(window,'localStorage',{value:{getItem:k=>memory.get(k)??null,setItem:(k,v)=>memory.set(k,v),removeItem:k=>memory.delete(k),clear:()=>memory.clear()}});
 if(!crypto.randomUUID)crypto.randomUUID=()=>{let b=crypto.getRandomValues(new Uint8Array(16));b[6]=(b[6]&15)|64;b[8]=(b[8]&63)|128;const s=Array.from(b,x=>x.toString(16).padStart(2,'0')).join('');return s.slice(0,8)+'-'+s.slice(8,12)+'-'+s.slice(12,16)+'-'+s.slice(16,20)+'-'+s.slice(20)};
 if(!crypto.subtle)Object.defineProperty(crypto,'subtle',{value:{digest:async(_a,b)=>new Uint8Array(await window.localSha256(Array.from(new Uint8Array(b)))).buffer}});
 }""")
 page.set_content((ROOT/'preview/BelegFlow-offline-showcase.html').read_text(),wait_until='load')
 page.wait_for_selector('.login-box')
 assert page.locator('html').get_attribute('lang')=='en';passed('English is the initial interface language')
 page.get_by_role('button',name='Finance',exact=True).click();page.wait_for_selector('.workspace-grid')
 def stable():page.wait_for_function('window.belegflowShowcase.store.getSnapshot().busy===false && !!window.belegflowShowcase.store.getSnapshot().detail')
 def t(key):return page.evaluate('(key)=>window.belegflowShowcase.store.t(key)',key)
 def state(expr):return page.evaluate('window.belegflowShowcase.store.getSnapshot().'+expr)
 def role(name):
  page.locator('.sidebar select').select_option(name+'@belegflow.demo')
  page.wait_for_function('(email)=>window.belegflowShowcase.store.getSnapshot().me?.email===email && !window.belegflowShowcase.store.getSnapshot().busy',arg=name+'@belegflow.demo')
 def formnote(note):
  page.locator('dialog textarea[name=note]').fill(note);page.locator('dialog button[type=submit]').click();page.wait_for_function('!window.belegflowShowcase.store.getSnapshot().busy')
 stable();passed('Finance demo login loads the three-pane invoice workspace')
 assert page.get_by_role('button',name=t('SUBMIT'),exact=True).is_disabled();passed('Submission is visibly blocked until service evidence is accepted')
 role('leon');page.get_by_role('tab',name=t('evidence')).click();page.get_by_role('button',name=t('acceptEvidence'),exact=True).click();formnote('Verified installation and completion report.');assert state('detail.evidence_accepted_by')=='user-2';passed('Project approver accepts service evidence through the actual dialog')
 role('finance');page.get_by_role('button',name=t('SUBMIT'),exact=True).click();page.wait_for_function('window.belegflowShowcase.store.getSnapshot().detail?.stage==="READY_FOR_APPROVAL"');passed('Finance submits after the evidence gate is satisfied')
 role('leon');page.get_by_role('button',name=t('APPROVE'),exact=True).click();formnote('Approved after independent review.');assert state('detail.stage')=='APPROVED';passed('Independent approver moves invoice to approved')
 role('finance');page.get_by_role('button',name=t('EXPORT'),exact=True).click()
 with page.expect_download() as dl:
  page.locator('dialog button[type=submit]').click()
 download=dl.value;path=Path(tempfile.mkdtemp())/'handoff.zip';download.save_as(path)
 with zipfile.ZipFile(path) as z:
  assert z.testzip() is None
  names=z.namelist();assert any(n.endswith('.csv') for n in names)
  original=next(n for n in names if '/original/' in n)
  original_hash=hashlib.sha256(z.read(original)).hexdigest()
  expected=page.evaluate('window.belegflowShowcase.api.data.invoices[0].documents.find(d=>d.kind==="ORIGINAL").sha256')
  assert original_hash==expected
 assert state('detail.stage')=='EXPORTED';passed('Downloaded showcase accounting ZIP is readable and preserves the original SHA-256')
 # Screens, navigation and high-value approval.
 role('admin');page.evaluate('window.belegflowShowcase.store.openInvoice("invoice-7")');stable();page.wait_for_function('window.belegflowShowcase.store.getSnapshot().detail?.id==="invoice-7"')
 page.get_by_role('button',name=t('APPROVE'),exact=True).click();formnote('Independent second approval.');assert state('detail.stage')=='APPROVED';passed('Administrator completes a high-value second approval')
 for view in ['suppliers','projects','exports','audit','settings']:
  page.evaluate('(v)=>window.belegflowShowcase.store.navigate(v)',view)
  page.wait_for_timeout(90)
  assert state('view')==view and page.locator('.standard-page').count()>0
  assert not state('error')
 passed('Supplier, project, export, audit and settings screens render without runtime errors')
 page.wait_for_timeout(220);page.screenshot(path=str(shots/'settings-en.png'),full_page=True)
 role('finance');page.evaluate('window.belegflowShowcase.store.openInvoice("invoice-4")');page.wait_for_function('window.belegflowShowcase.store.getSnapshot().detail?.id==="invoice-4"')
 # Resolve through the store-driven real dialog; opening the dialog is the same UI code path.
 q=state('detail.discussion[0].id');page.evaluate('(q)=>window.belegflowShowcase.store.openDialog("resolveQuestion",undefined,undefined,q)',q);formnote('Both meeting rooms were completed.');assert state('detail.open_questions')==0
 page.get_by_role('button',name=t('RESOLVE'),exact=True).click();page.wait_for_function('window.belegflowShowcase.store.getSnapshot().detail?.stage==="REVIEWED"');passed('Clarification resolution clears the question and returns the invoice to review')
 # Import is exercised through the form using an actual fixture.
 page.get_by_role('button',name=t('import'),exact=True).click();page.locator('dialog input[type=file]').set_input_files(str(ROOT/'samples/invoice-ubl.xml'))
 page.locator('dialog button[type=submit]').click();page.wait_for_function('!window.belegflowShowcase.store.getSnapshot().busy')
 assert not state('error'),state('error');imported_id=state('selectedId');passed('UBL fixture imports through the real upload form in the compatibility showcase')
 page.get_by_role('button',name=t('import'),exact=True).click();page.locator('dialog input[type=file]').set_input_files(str(ROOT/'samples/invoice-ubl.xml'));page.locator('dialog button[type=submit]').click();page.wait_for_function('!window.belegflowShowcase.store.getSnapshot().busy');assert state('selectedId')==imported_id;passed('Exact re-import reuses the same showcase invoice')
 # Reset only synthetic data; the server build deliberately has no reset endpoint.
 page.evaluate('async()=>{await window.belegflowShowcase.api.send("POST","/demo/reset");await window.belegflowShowcase.store.loginDemo("finance@belegflow.demo");}');stable()
 page.evaluate('window.belegflowShowcase.store.patch({toast:null,documentTab:"document",mobilePane:"queue"})');page.get_by_role('button',name='DE',exact=True).click();assert page.locator('html').get_attribute('lang')=='de';page.wait_for_timeout(220);page.screenshot(path=str(shots/'workspace-de.png'),full_page=True)
 assert page.evaluate('document.documentElement.scrollWidth<=window.innerWidth');passed('German workspace renders without horizontal overflow at 1440 px')
 page.get_by_role('button',name='EN',exact=True).click();page.evaluate('window.belegflowShowcase.store.toggleTheme()');page.wait_for_timeout(220);page.screenshot(path=str(shots/'workspace-dark.png'),full_page=True);passed('Dark theme renders with the same workspace structure')
 page.evaluate('window.belegflowShowcase.store.toggleTheme();window.belegflowShowcase.store.patch({mobilePane:"queue",documentTab:"document",toast:null})');page.set_viewport_size({'width':390,'height':844});page.wait_for_timeout(100)
 assert page.evaluate('document.documentElement.scrollWidth<=window.innerWidth');page.wait_for_timeout(220);page.screenshot(path=str(shots/'mobile-en.png'),full_page=True);passed('390 px mobile inbox fits the viewport without horizontal overflow')
 page.evaluate('window.belegflowShowcase.store.patch({mobilePane:"decision"})');page.wait_for_timeout(220);page.screenshot(path=str(shots/'mobile-decision-en.png'),full_page=True)
 assert page.get_by_role('button',name=t('SUBMIT'),exact=True).is_visible();passed('Mobile decision pane keeps the primary approval action visible')
 page.set_viewport_size({'width':1440,'height':1040});page.evaluate('window.belegflowShowcase.store.patch({mobilePane:"queue",documentTab:"document"})');page.wait_for_timeout(220);page.screenshot(path=str(shots/'workspace-en.png'),full_page=True)
 assert not errors,errors;passed('No uncaught browser runtime errors during the compatibility journeys')
 (ROOT/'docs/testing/browser-compatibility.json').write_text(json.dumps({'scope':'Offline compatibility showcase only; React 16 local runtime, in-memory storage and a SHA-256 test bridge. This does not verify the Spring/PostgreSQL/React 19 production build.','checks':results,'passed':len(results),'page_errors':errors,'browser':browser.version},indent=2))
 browser.close()
print('TOTAL',len(results))
