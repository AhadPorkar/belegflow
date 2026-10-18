import {test,expect} from '@playwright/test';
import {readFileSync} from 'node:fs';
import {randomUUID} from 'node:crypto';
const password='BelegFlow-Demo-2026!';
const suffix=()=>randomUUID().slice(0,8);
async function login(page,email='finance@belegflow.demo',secret=password){
 await page.goto('/');
 await page.locator('input[name=email]').fill(email);
 await page.locator('input[name=password]').fill(secret);
 await page.locator('.login-box button[type=submit]').click();
 await expect(page.locator('.workspace-grid')).toBeVisible();
 await page.getByRole('button',{name:'EN',exact:true}).click();
 await expect(page.locator('html')).toHaveAttribute('lang','en');
}
async function api(page,path,method='GET',body){
 return page.evaluate(async({path,method,body})=>{
  const csrf=await fetch('/api/csrf').then(r=>r.json());
  const response=await fetch('/api'+path,{method,headers:{'Content-Type':'application/json','X-CSRF-TOKEN':csrf.token},body:body===undefined?undefined:JSON.stringify(body)});
  return {status:response.status,data:await response.json().catch(()=>null)};
 },{path,method,body});
}
async function nav(page,title){await page.locator('.sidebar').getByRole('button',{name:title,exact:true}).click();await expect(page.locator('.standard-page')).toBeVisible();}
async function save(page){await page.locator('dialog button[type=submit]').click();await expect(page.locator('dialog')).toHaveCount(0);await expect(page.locator('.global-error')).toHaveCount(0);}
async function row(page,number){await page.locator('#invoice-search').fill(number);const result=page.locator('.invoice-row').filter({hasText:number}).first();await expect(result).toBeVisible();await result.click();await expect(page.locator('.paper-title')).toContainText(number);}

test('supplier module: create, edit, deactivate and verify persisted values',async({page})=>{
 await login(page);await nav(page,'Suppliers');const name='E2E supplier '+suffix();
 await page.getByRole('button',{name:'Add supplier',exact:true}).click();
 await page.locator('dialog input[name=name]').fill(name);
 await page.locator('dialog input[name=email]').fill('billing@example.test');await save(page);
 const supplier=page.locator('tbody tr').filter({hasText:name});await expect(supplier).toBeVisible();
 await supplier.getByRole('button',{name:'Edit',exact:true}).click();
 await page.locator('dialog input[name=active]').uncheck();await save(page);
 await expect(supplier).toContainText('Inactive');
 const data=(await api(page,'/catalog')).data.suppliers.find(s=>s.name===name);expect(data.active).toBe(false);
});

test('project and cost-center modules: create both through their forms',async({page})=>{
 await login(page);await nav(page,'Projects');const key=suffix();
 await page.getByRole('button',{name:'Add project',exact:true}).click();
 await page.locator('dialog input[name=code]').fill('E2E-P-'+key);
 await page.locator('dialog input[name=name]').fill('Project '+key);await save(page);
 await expect(page.locator('.project-card').filter({hasText:'Project '+key})).toContainText('Evidence before approval');
 await page.getByRole('button',{name:'Add cost center',exact:true}).click();
 await page.locator('dialog input[name=code]').fill('E2E-C-'+key);
 await page.locator('dialog input[name=name]').fill('Cost center '+key);await save(page);
 await expect(page.locator('.cost-center').filter({hasText:'E2E-C-'+key})).toBeVisible();
});

test('document module: import XML, preserve original and reuse exact upload',async({page})=>{
 await login(page);const number='E2E-XML-'+suffix();
 const source=readFileSync(new URL('../../../samples/invoice-ubl.xml',import.meta.url),'utf8').replace('DEMO-2026-9001',number);
 const file={name:'e2e.xml',mimeType:'application/xml',buffer:Buffer.from(source)};
 await page.getByRole('button',{name:'Import invoice',exact:true}).click();await page.locator('dialog input[type=file]').setInputFiles(file);await save(page);
 await expect(page.locator('.paper-title')).toContainText(number);
 const first=(await api(page,'/invoices?q='+number)).data.items[0];
 const downloadPromise=page.waitForEvent('download');await page.getByRole('button',{name:'Download original',exact:true}).click();
 const download=await downloadPromise;const stream=await download.createReadStream();const chunks=[];for await(const chunk of stream)chunks.push(chunk);expect(Buffer.concat(chunks).toString()).toBe(source);
 await page.getByRole('button',{name:'Import invoice',exact:true}).click();await page.locator('dialog input[type=file]').setInputFiles(file);await save(page);
 const repeated=(await api(page,'/invoices?q='+number)).data;expect(repeated.total).toBe(1);expect(repeated.items[0].id).toBe(first.id);
});

test('manual PDF import: required dates and totals are entered through the screen',async({page})=>{
 await login(page);const catalog=(await api(page,'/catalog')).data,number='E2E-PDF-'+suffix();
 await page.getByRole('button',{name:'Import invoice',exact:true}).click();
 await page.locator('dialog input[type=file]').setInputFiles({name:'synthetic.pdf',mimeType:'application/pdf',buffer:Buffer.from('%PDF-1.7\nSynthetic file signature for form testing '+number)});
 await page.locator('dialog select[name=supplier_id]').selectOption(catalog.suppliers.find(s=>s.active).id);
 for(const [field,value] of Object.entries({invoice_number:number,issue_date:'2026-10-01',due_date:'2026-10-31',net:'100.00',tax:'19.00',gross:'119.00'}))await page.locator('dialog [name='+field+']').fill(value);
 await save(page);await expect(page.locator('.paper-title')).toContainText(number);
 expect((await api(page,'/invoices?q='+number)).data.items[0].gross).toBe('119.00');
});

test('discussion module: literal text survives reload without executing markup',async({page})=>{
 await login(page);await row(page,'AC-26-104');await page.getByRole('tab',{name:/Discussion/}).click();
 const comment='<img src=x onerror=alert(1)> '+suffix();
 await page.locator('.comment-form textarea').fill(comment);await page.getByRole('button',{name:'Post comment',exact:true}).click();
 await expect(page.locator('.comment').filter({hasText:comment})).toBeVisible();await expect(page.locator('.discussion img')).toHaveCount(0);
 await page.reload();await expect(page.locator('.workspace-grid')).toBeVisible();await row(page,'AC-26-104');await page.getByRole('tab',{name:/Discussion/}).click();
 await expect(page.locator('.comment').filter({hasText:comment})).toBeVisible();
});

test('administration module: create a viewer and verify its read-only interface',async({page})=>{
 await login(page,'admin@belegflow.demo');await nav(page,'Workspace settings');const email='viewer-'+suffix()+'@example.test';
 await page.getByRole('button',{name:'Add member',exact:true}).click();
 await page.locator('dialog input[name=display_name]').fill('E2E Viewer');await page.locator('dialog input[name=email]').fill(email);
 await page.locator('dialog select[name=role]').selectOption('VIEWER');await page.locator('dialog input[name=password]').fill(password);await save(page);
 await page.locator('.sidebar').getByRole('button',{name:'Sign out',exact:true}).click();await expect(page.locator('.login-box')).toBeVisible();
 await login(page,email);await expect(page.getByRole('button',{name:'Import invoice',exact:true})).toHaveCount(0);
 const list=(await api(page,'/invoices')).data;
 expect((await api(page,'/invoices/'+list.items[0].id+'/comments','POST',{body:'Forbidden'})).status).toBe(403);
});

test('rules module: persist threshold and keep display preferences separate',async({page})=>{
 await login(page,'admin@belegflow.demo');await nav(page,'Workspace settings');
 const previous=(await api(page,'/catalog')).data.threshold;
 await page.locator('input[name=threshold]').fill('10000.50');await page.locator('.settings-card form button[type=submit]').click();
 await expect.poll(async()=>(await api(page,'/catalog')).data.threshold).toBe('10000.50');
 await page.getByLabel('Number and date formatting',{exact:true}).selectOption('de-DE');
 await page.getByRole('button',{name:'DE',exact:true}).click();await expect(page.locator('html')).toHaveAttribute('lang','de');
 await page.getByRole('button',{name:'EN',exact:true}).click();
 expect((await api(page,'/catalog')).data.threshold).toBe('10000.50');
 expect((await api(page,'/settings/rules','PUT',{threshold:previous})).status).toBe(200);
});

test('delegation module: create and revoke a bounded delegation',async({page})=>{
 await login(page,'admin@belegflow.demo');await nav(page,'Workspace settings');const users=(await api(page,'/catalog')).data.users;
 const owner=users.find(u=>u.email==='leon@belegflow.demo'),delegate=users.find(u=>u.email==='mira@belegflow.demo');
 await page.getByRole('button',{name:'Add delegation',exact:true}).click();
 await page.locator('dialog select[name=owner_id]').selectOption(owner.id);await page.locator('dialog select[name=delegate_id]').selectOption(delegate.id);
 await save(page);const result=(await api(page,'/catalog')).data.delegations.find(d=>d.owner_id===owner.id&&d.delegate_id===delegate.id&&d.active);expect(result).toBeTruthy();
 const row=page.locator('tbody tr').filter({hasText:'Leon Fischer'}).filter({hasText:'Mira Hoffmann'});await row.getByRole('button',{name:'Revoke',exact:true}).click();
 await expect.poll(async()=>(await api(page,'/catalog')).data.delegations.find(d=>d.id===result.id).active).toBe(false);
});

test('notification module: run reminders and mark only own notification read',async({page})=>{
 await login(page,'admin@belegflow.demo');await nav(page,'Workspace settings');
 await page.getByRole('button',{name:'Run reminder check',exact:true}).click();
 await expect.poll(async()=>(await api(page,'/notifications')).data.length).toBeGreaterThan(0);
 await page.locator('.topbar').getByRole('button',{name:'Notifications',exact:true}).click();
 const unreadBefore=(await api(page,'/notifications')).data.filter(n=>!n.is_read).length;
 expect(unreadBefore).toBeGreaterThan(0);
 const pending=page.locator('dialog .notification.unread').first();await expect(pending).toBeVisible();
 await pending.getByRole('button',{name:'Mark read',exact:true}).click();
 await expect.poll(async()=>(await api(page,'/notifications')).data.filter(n=>!n.is_read).length).toBe(unreadBefore-1);
});

test('audit module and keyboard navigation expose a readable evidence trail',async({page})=>{
 await login(page);await page.keyboard.press('Control+k');await expect(page.getByRole('dialog')).toBeVisible();
 await page.locator('dialog').getByRole('button',{name:'Audit trail',exact:true}).click();await expect(page.getByRole('dialog')).toHaveCount(0);
 await expect(page.locator('tbody tr').first()).toBeVisible();
 const invoiceLink=page.locator('tbody .text-button').first();await invoiceLink.click();await expect(page.locator('.workspace-grid')).toBeVisible();
 await page.keyboard.press('Control+k');await expect(page.getByRole('dialog')).toBeVisible();await page.keyboard.press('Escape');await expect(page.getByRole('dialog')).toHaveCount(0);
});
