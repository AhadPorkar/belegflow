/** Synthetic, stable fixtures shared by core and React component tests. No demo adapter needed. */
export const me = { id:'user-finance',tenant_id:'tenant-a',name:'Nora Becker',role:'FINANCE',organization:'Example Services GmbH',email:'finance@example.test',locale:'en',threshold:'10000.00',demo:false };
export const users = [
 {id:'user-finance',display_name:'Nora Becker',email:'finance@example.test',role:'FINANCE',active:true},
 {id:'user-approver',display_name:'Leon Fischer',email:'approver@example.test',role:'APPROVER',active:true},
 {id:'user-admin',display_name:'Alex Weber',email:'admin@example.test',role:'ADMIN',active:true},
 {id:'user-inactive',display_name:'Inactive User',email:'inactive@example.test',role:'APPROVER',active:false},
];
export const catalog={
 suppliers:[{id:'supplier-a',name:'Test Electrical GmbH',email:'sales@example.test',vat_id:'DE123456789',active:true}],
 projects:[{id:'project-a',code:'PRJ-100',name:'Berlin expansion',manager_id:'user-approver',manager_name:'Leon Fischer',evidence_required:true,active:true}],
 cost_centers:[{id:'cost-a',code:'CC-100',name:'Facilities',active:true}],users,delegations:[],threshold:'10000.00',
};
export const summary={total:1,review:1,awaiting:0,approved:0,exported:0,overdue:0,open_value:'1190.00',approved_value:'0.00'};
export const detail={
 id:'invoice-a',tenant_id:'tenant-a',supplier_id:'supplier-a',supplier_name:'Test Electrical GmbH',supplier_email:'sales@example.test',supplier_vat_id:'DE123456789',
 invoice_number:'TEST-2026-001',description:'Electrical maintenance',issue_date:'2026-10-01',due_date:'2026-10-31',net:'1000.00',tax:'190.00',gross:'1190.00',currency:'EUR',source_format:'UBL',
 basic_valid:true,validation_notes:'',project_id:'project-a',project_name:'Berlin expansion',project_code:'PRJ-100',project_manager_id:'user-approver',cost_center_id:'cost-a',cost_center_name:'Facilities',cost_center_code:'CC-100',
 approver_id:'user-approver',approver_name:'Leon Fischer',evidence_required:true,evidence_accepted_by:null,evidence_accepted_name:null,evidence_accepted_at:null,evidence_note:'',stage:'REVIEWED',
 duplicate_warning:false,duplicate_cleared:false,second_required:false,first_approved_by:null,second_approved_by:null,created_by:'user-finance',creator_name:'Nora Becker',received_at:'2026-10-18T12:00:00Z',updated_at:'2026-10-18T12:00:00Z',version:3,business_key:'synthetic-business-key',
 documents:[{id:'document-a',kind:'ORIGINAL',filename:'invoice.xml',content_type:'application/xml',sha256:'a'.repeat(64),size_bytes:1000,created_at:'2026-10-18T12:00:00Z'}],
 discussion:[],audit:[{id:3,invoice_id:'invoice-a',invoice_number:'TEST-2026-001',actor_name:'Nora Becker',event_type:'INVOICE_REVIEWED',detail:'Checked',created_at:'2026-10-18T12:00:00Z'}],
 possible_duplicates:[],allowed_actions:['CLARIFY','REJECT'],can_edit:true,can_upload_evidence:true,can_accept_evidence:false,open_questions:0,
};
export class FixtureApi {
 constructor(){this.calls=[];this.me=structuredClone(me);this.catalog=structuredClone(catalog);this.detail=structuredClone(detail);this.summary=structuredClone(summary);this.notifications=[];this.exports=[];this.audit={items:structuredClone(detail.audit),next:3};}
 async get(path){this.calls.push(['GET',path]);if(path==='/public/config')return{demo:false};if(path==='/session')return this.me;if(path==='/catalog')return this.catalog;if(path==='/invoices/summary')return this.summary;if(path==='/notifications')return this.notifications;if(path==='/exports')return this.exports;if(path.startsWith('/audit'))return this.audit;if(path.startsWith('/invoices?'))return{items:[this.detail],total:1,page:0,size:50};if(path.startsWith('/invoices/'))return this.detail;throw Error('Unhandled fixture GET '+path);}
 async send(method,path,body){this.calls.push([method,path,body]);return{id:'created-id'};}
 async upload(path,body){this.calls.push(['UPLOAD',path,body]);return{id:this.detail.id,reused:false};}
 async login(email,password){this.calls.push(['LOGIN',email,password]);return this.me;}
 async logout(){this.calls.push(['LOGOUT']);}
 async download(path,name){this.calls.push(['DOWNLOAD',path,name]);}
}
export function deferred(){let resolve,reject;const promise=new Promise((a,b)=>{resolve=a;reject=b;});return{promise,resolve,reject};}
