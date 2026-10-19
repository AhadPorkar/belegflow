import React from 'react';
import { describe,it,expect,vi } from 'vitest';
import { screen,fireEvent,render } from '@testing-library/react';
import { App,ErrorBoundary } from '../../src/App.jsx';
import { Sidebar,Topbar } from '../../src/components/Shell.jsx';
import { Button,IconButton,Badge,Avatar,Empty,Field,finance } from '../../src/components/ui.jsx';
import { me } from '../support/fixtures.mjs';
import { show,props } from './helpers.jsx';

describe('Shell, routing and application states',()=>{
 it('renders boot state before authentication finishes',()=>{const {store,container}=show(App,{ready:false,me:null});expect(screen.getByText(store.t('loading'))).toBeTruthy();expect(container.querySelector('.login-box')).toBeNull();});
 it('renders authentication when no session exists',()=>{const {container}=show(App,{me:null});expect(container.querySelector('.login-box')).toBeTruthy();expect(container.querySelector('.app-shell')).toBeNull();});
 it.each([['inbox','inboxTitle'],['approvals','approvalTitle'],['suppliers','supplierTitle'],['projects','projectTitle'],['exports','exportTitle'],['audit','auditTitle'],['settings','settingsTitle']])('routes %s to its module', (view,title)=>{const {store}=show(App,{view});expect(screen.getByRole('heading',{name:store.t(title)})).toBeTruthy();});
 it('denies accounting view to a viewer even when route is forced',()=>{const {store}=show(App,{view:'exports',me:{...me,role:'VIEWER'}});expect(screen.getByText(store.t('error_FORBIDDEN'))).toBeTruthy();expect(screen.queryByRole('heading',{name:store.t('exportTitle')})).toBeNull();});
 it('exposes a working main-content skip target',()=>{const {container}=show(App);expect(container.querySelector('.skip-link').getAttribute('href')).toBe('#main-content');expect(container.querySelector('#main-content')).toBeTruthy();});
 it('renders a global error, retries and dismisses explicitly',()=>{const {store}=show(App,{view:'projects',error:'NETWORK_ERROR'});expect(screen.getByRole('alert').textContent).toContain(store.t('error_NETWORK_ERROR'));fireEvent.click(screen.getByRole('button',{name:store.t('retry')}));fireEvent.click(screen.getByRole('button',{name:store.t('close')}));expect(store.refresh).toHaveBeenCalledOnce();expect(store.patch).toHaveBeenCalledWith({error:null});});
 it('places an active transaction error inside its dialog only',()=>{const {container}=show(App,{error:'INVALID_INPUT',dialog:{kind:'supplier'}});expect(screen.getAllByRole('alert').length).toBe(1);expect(container.querySelector('.global-error')).toBeNull();});
 it('announces completed work with a status region',()=>{const {store}=show(App,{toast:'saved'});expect(screen.getByRole('status').textContent).toBe(store.t('saved'));});
 it('catches a component exception without exposing its stack',()=>{vi.spyOn(console,'error').mockImplementation(()=>{});function Broken(){throw Error('secret internal failure');}const {store}=props();render(<ErrorBoundary store={store}><Broken/></ErrorBoundary>);expect(screen.getByText(store.t('errorGeneric'))).toBeTruthy();expect(screen.queryByText('secret internal failure')).toBeNull();});
 it('marks the current sidebar route and dispatches navigation',()=>{const {store}=show(Sidebar,{view:'projects'});expect(screen.getByRole('button',{name:store.t('projects')}).getAttribute('aria-current')).toBe('page');fireEvent.click(screen.getByRole('button',{name:store.t('suppliers')}));expect(store.navigate).toHaveBeenCalledWith('suppliers');});
 it('signs out through the shell',()=>{const {store}=show(Sidebar);fireEvent.click(screen.getByRole('button',{name:store.t('logout')}));expect(store.logout).toHaveBeenCalledOnce();});
 it('exposes topbar command, notification, language and theme actions',()=>{const {store}=show(Topbar);fireEvent.click(screen.getByRole('button',{name:store.t('searchCommands')}));fireEvent.click(screen.getByRole('button',{name:store.t('notifications')}));fireEvent.click(screen.getByRole('button',{name:'DE'}));fireEvent.click(screen.getByRole('button',{name:store.t('theme')}));expect(store.openDialog.mock.calls).toEqual([['commands'],['notifications']]);expect(store.setLocale).toHaveBeenCalledWith('de');expect(store.toggleTheme).toHaveBeenCalledOnce();});
 it('shows the unread indicator only for unread items',()=>{const {container}=show(Topbar,{notifications:[{id:'n1',is_read:false}]});expect(container.querySelector('.notification-wrap i')).toBeTruthy();});
});

describe('Shared design-system controls',()=>{
 it('uses non-submitting buttons by default',()=>{render(<Button>Action</Button>);expect(screen.getByRole('button').type).toBe('button');});
 it('makes an icon-only button accessible by name',()=>{render(<IconButton icon="download" label="Download original"/>);expect(screen.getByRole('button',{name:'Download original'})).toBeTruthy();});
 it('does not activate a disabled control',()=>{const click=vi.fn();render(<Button disabled onClick={click}>Action</Button>);fireEvent.click(screen.getByRole('button'));expect(click).not.toHaveBeenCalled();});
 it('labels a field using its surrounding label',()=>{render(<Field label="Example name"><input name="example"/></Field>);expect(screen.getByLabelText('Example name')).toBeTruthy();});
 it('renders localized status text',()=>{render(<Badge stage="APPROVED" t={()=>'Approved'}/>);expect(screen.getByText('Approved')).toBeTruthy();});
 it('renders avatars and helpful empty states without external images',()=>{render(<><Avatar name="Nora Becker"/><Empty title="No invoices" body="Import your first invoice."/></>);expect(screen.getByText('NB')).toBeTruthy();expect(screen.getByText('Import your first invoice.')).toBeTruthy();});
 it('limits finance capabilities to finance and administrator roles',()=>{expect(finance({...me,role:'ADMIN'})).toBe(true);expect(finance(me)).toBe(true);expect(finance({...me,role:'APPROVER'})).toBe(false);expect(finance(null)).toBe(false);});
});

describe('Explicit field-label contract',()=>{
 it('does not include select options in the associated label',()=>{
  render(<Field id="format-test" label="Number format"><select><option>English example</option><option>German example</option></select></Field>);
  const select=screen.getByRole('combobox',{name:'Number format',exact:true});
  expect(screen.getByLabelText('Number format',{exact:true})).toBe(select);
  expect(select.labels[0].textContent).toBe('Number format');
 });
 it('connects hints as descriptions rather than expanding the label',()=>{
  render(<Field id="threshold-test" label="Threshold" hint="Separate explanatory text"><input type="number"/></Field>);
  const control=screen.getByLabelText('Threshold',{exact:true});
  expect(control.getAttribute('aria-describedby')).toBe('threshold-test-hint');
  expect(document.getElementById('threshold-test-hint').textContent).toBe('Separate explanatory text');
 });
 it('preserves control props and a pre-existing description',()=>{
  render(<Field id="extra-test" label="Amount" hint="Help"><input name="amount" required aria-describedby="external-help"/></Field>);
  const input=screen.getByLabelText('Amount');expect(input.name).toBe('amount');expect(input.required).toBe(true);
  expect(input.getAttribute('aria-describedby')).toBe('external-help extra-test-hint');
 });
 it('keeps two explicit fields distinct even when their labels match',()=>{
  render(<><Field id="first-test" label="Name"><input/></Field><Field id="second-test" label="Name"><input/></Field></>);
  expect(screen.getAllByLabelText('Name').map(input=>input.id)).toEqual(['first-test','second-test']);
 });
 it('does not add a description attribute when there is no hint',()=>{
  render(<Field id="plain-test" label="Plain"><input/></Field>);
  expect(screen.getByLabelText('Plain').hasAttribute('aria-describedby')).toBe(false);
 });
});
