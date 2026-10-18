import React from 'react';
import { describe,it,expect } from 'vitest';
import { screen,fireEvent } from '@testing-library/react';
import { Suppliers,Projects } from '../../src/features/catalog/CatalogPages.jsx';
import { Settings } from '../../src/features/settings/Settings.jsx';
import { me,catalog } from '../support/fixtures.mjs';
import { show,input } from './helpers.jsx';

describe('Suppliers, projects and cost centers',()=>{
 it.each(['en','de'])('lists and edits a supplier (%s)',locale=>{const {store}=show(Suppliers,{locale});expect(screen.getByText(catalog.suppliers[0].name)).toBeTruthy();fireEvent.click(screen.getByRole('button',{name:store.t('edit')}));expect(store.openDialog).toHaveBeenCalledWith('supplier',catalog.suppliers[0]);});
 it('opens supplier creation',()=>{const {store}=show(Suppliers);fireEvent.click(screen.getByRole('button',{name:store.t('addSupplier')}));expect(store.openDialog).toHaveBeenCalledWith('supplier');});
 it('hides supplier mutations from viewers',()=>{const {store}=show(Suppliers,{me:{...me,role:'VIEWER'}});expect(screen.queryByRole('button',{name:store.t('addSupplier')})).toBeNull();expect(screen.queryByRole('button',{name:store.t('edit')})).toBeNull();});
 it('handles an empty catalog',()=>{const {container}=show(Suppliers,{catalog:{...catalog,suppliers:[]}});expect(container.querySelector('.empty-state')).toBeTruthy();});
 it('shows inactive suppliers',()=>{const {store}=show(Suppliers,{catalog:{...catalog,suppliers:[{...catalog.suppliers[0],active:false}]}});expect(screen.getByText(store.t('inactive'))).toBeTruthy();});
 it.each(['en','de'])('renders project owner and cost center (%s)',locale=>{const {store}=show(Projects,{locale});expect(screen.getByText('Leon Fischer')).toBeTruthy();expect(screen.getByText('CC-100')).toBeTruthy();fireEvent.click(screen.getByRole('button',{name:store.t('addProject')}));fireEvent.click(screen.getByRole('button',{name:store.t('addCostCenter')}));expect(store.openDialog.mock.calls).toEqual([['project'],['costCenter']]);});
 it('distinguishes projects without an evidence gate',()=>{const {store}=show(Projects,{catalog:{...catalog,projects:[{...catalog.projects[0],evidence_required:false}]}});expect(screen.getByText(store.t('notRequired'))).toBeTruthy();});
 it('keeps project creation read-only for approvers',()=>{const {store}=show(Projects,{me:{...me,role:'APPROVER'}});expect(screen.queryByRole('button',{name:store.t('addProject')})).toBeNull();expect(screen.queryByRole('button',{name:store.t('addCostCenter')})).toBeNull();});
});

describe('Administration, rules, delegations and preferences',()=>{
 it('admin changes the approval threshold',()=>{const {store,container}=show(Settings,{me:{...me,role:'ADMIN'}});fireEvent.change(input(container,'threshold'),{target:{value:'2500.50'}});fireEvent.submit(container.querySelector('form'));expect(store.saveRules).toHaveBeenCalledWith('2500.50');});
 it('non-admin can read but not edit the approval threshold',()=>{const {store,container}=show(Settings);expect(input(container,'threshold').disabled).toBe(true);expect(screen.queryByRole('button',{name:store.t('save')})).toBeNull();});
 it.each(['en','de'])('changes language separately from number formatting (%s)',locale=>{const {store}=show(Settings,{locale});fireEvent.change(screen.getByLabelText(store.t('language')),{target:{value:'de'}});fireEvent.change(screen.getByLabelText(store.t('numberFormat')),{target:{value:'de-DE'}});expect(store.setLocale).toHaveBeenCalledWith('de');expect(store.setNumberLocale).toHaveBeenCalledWith('de-DE');});
 it('opens user and delegation creation for admins',()=>{const {store}=show(Settings,{me:{...me,role:'ADMIN'}});fireEvent.click(screen.getByRole('button',{name:store.t('addUser')}));fireEvent.click(screen.getByRole('button',{name:store.t('addDelegation')}));expect(store.openDialog.mock.calls).toEqual([['user'],['delegation']]);});
 it('opens an existing team member for admin editing',()=>{const {store}=show(Settings,{me:{...me,role:'ADMIN'}});fireEvent.click(screen.getAllByRole('button',{name:store.t('edit')})[0]);expect(store.openDialog).toHaveBeenCalledWith('user',catalog.users[0]);});
 it('does not expose admin actions to finance',()=>{const {store}=show(Settings);for(const k of ['addUser','addDelegation','runReminders'])expect(screen.queryByRole('button',{name:store.t(k)})).toBeNull();});
 it('revokes an active delegation explicitly',()=>{const delegation={id:'delegation-a',owner_name:'Leon',delegate_name:'Alex',valid_from:'2026-10-18T12:00:00Z',valid_until:'2026-10-19T12:00:00Z',active:true};const {store}=show(Settings,{me:{...me,role:'ADMIN'},catalog:{...catalog,delegations:[delegation]}});fireEvent.click(screen.getByRole('button',{name:store.t('revoke')}));expect(store.revoke).toHaveBeenCalledWith('delegation-a');});
 it('runs reminders on administrator request',()=>{const {store}=show(Settings,{me:{...me,role:'ADMIN'}});fireEvent.click(screen.getByRole('button',{name:store.t('runReminders')}));expect(store.runReminders).toHaveBeenCalledOnce();});
 it('never offers demo reset outside a standalone preview',()=>{const {store}=show(Settings,{standalone:false});expect(screen.queryByRole('button',{name:store.t('resetDemo')})).toBeNull();});
 it('opens a reset confirmation only for standalone mode',()=>{const {store}=show(Settings,{standalone:true});fireEvent.click(screen.getByRole('button',{name:store.t('resetDemo')}));expect(store.openDialog).toHaveBeenCalledWith('reset');});
});
