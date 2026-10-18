import {test,expect} from '@playwright/test';
const password='BelegFlow-Demo-2026!';
async function login(page,email='finance@belegflow.demo') {
 await page.goto('/');
 await page.locator('input[name="email"]').fill(email);
 await page.locator('input[name="password"]').fill(password);
 await page.locator('.login-box button[type="submit"]').click();
 await expect(page.locator('.workspace-grid')).toBeVisible();
}
async function api(page,path,method='GET',body) {
 return page.evaluate(async({path,method,body})=>{
  const csrf=await fetch('/api/csrf').then(r=>r.json());
  const r=await fetch('/api'+path,{method,headers:{'Content-Type':'application/json','X-CSRF-TOKEN':csrf.token},body:body===undefined?undefined:JSON.stringify(body)});
  return {status:r.status,data:await r.json().catch(()=>null)};
 },{path,method,body});
}
test('English default, German switch, mobile layout and dark theme',async({page})=>{
 await login(page);
 await expect(page.locator('html')).toHaveAttribute('lang','en');
 await page.getByRole('button',{name:'Appearance',exact:true}).click();
 await expect(page.locator('html')).toHaveAttribute('data-theme','dark');
 await page.getByRole('button',{name:'Appearance',exact:true}).click();
 await page.getByRole('button',{name:'DE',exact:true}).click();
 await expect(page.locator('html')).toHaveAttribute('lang','de');
 await page.getByRole('button',{name:'EN',exact:true}).click();
 await page.setViewportSize({width:390,height:844});
 expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBe(true);
});
test('read-only identity cannot mutate; server rejects forged writes',async({page})=>{
 await login(page,'viewer@belegflow.demo');
 const invoices=await api(page,'/invoices');
 const response=await api(page,'/invoices/'+invoices.data.items[0].id+'/commands','POST',{version:0,action:'REVIEW'});
 expect(response.status).toBe(403);
});
test('service acceptance and independent approval gate accounting export',async({page})=>{
 await login(page);
 const list=(await api(page,'/invoices?q=KE-2026-1048')).data.items;
 const invoice=list.find(i=>i.stage==='REVIEWED');expect(invoice).toBeTruthy();
 let result=await api(page,'/invoices/'+invoice.id+'/commands','POST',{version:invoice.version,action:'SUBMIT'});
 expect(result.status).toBe(422);
 await page.locator('.sidebar select').selectOption('leon@belegflow.demo');
 await expect(page.locator('.sidebar select')).toHaveValue('leon@belegflow.demo');
 await expect.poll(async()=> (await api(page,'/session')).data.email).toBe('leon@belegflow.demo');
 result=await api(page,'/invoices/'+invoice.id+'/evidence-acceptance','POST',{version:invoice.version,note:'Service evidence verified in end-to-end test.'});expect(result.status).toBe(200);
 await page.locator('.sidebar select').selectOption('finance@belegflow.demo');
 await expect.poll(async()=> (await api(page,'/session')).data.email).toBe('finance@belegflow.demo');
 let detail=(await api(page,'/invoices/'+invoice.id)).data;
 expect((await api(page,'/invoices/'+invoice.id+'/commands','POST',{version:detail.version,action:'SUBMIT'})).status).toBe(200);
 await page.locator('.sidebar select').selectOption('leon@belegflow.demo');
 await expect.poll(async()=> (await api(page,'/session')).data.email).toBe('leon@belegflow.demo');
 detail=(await api(page,'/invoices/'+invoice.id)).data;
 expect((await api(page,'/invoices/'+invoice.id+'/commands','POST',{version:detail.version,action:'APPROVE',note:'Approved.'})).status).toBe(200);
 await page.locator('.sidebar select').selectOption('finance@belegflow.demo');
 await expect.poll(async()=> (await api(page,'/session')).data.email).toBe('finance@belegflow.demo');
 detail=(await api(page,'/invoices/'+invoice.id)).data;
 const key=await page.evaluate(()=>crypto.randomUUID());
 const payload={idempotency_key:key,invoices:[{id:invoice.id,version:detail.version}]};
 const first=await api(page,'/exports','POST',payload);expect(first.status).toBe(200);
 expect((await api(page,'/exports','POST',payload)).data.id).toBe(first.data.id);
 const download=await page.request.get('/api/exports/'+first.data.id+'/download');
 expect(download.status()).toBe(200);expect(download.headers()['content-type']).toContain('application/zip');
 expect((await download.body()).subarray(0,2).toString()).toBe('PK');
});
test('cross-organization invoice and original access is denied',async({page})=>{
 await login(page);
 const invoice=(await api(page,'/invoices')).data.items[0];
 const detail=(await api(page,'/invoices/'+invoice.id)).data;
 await page.locator('.sidebar select').selectOption('isolated@belegflow.demo');
 await expect.poll(async()=> (await api(page,'/session')).data.email).toBe('isolated@belegflow.demo');
 expect((await api(page,'/invoices/'+invoice.id)).status).toBe(404);
 const response=await page.request.get('/api/documents/'+detail.documents[0].id);expect(response.status()).toBe(404);
});
