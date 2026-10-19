import React from 'react';
import { describe,it,expect,vi } from 'vitest';
import { screen,fireEvent } from '@testing-library/react';
import { Login } from '../../src/features/identity/Login.jsx';
import { show,input } from './helpers.jsx';

describe('Identity screen',()=>{
 it.each(['en','de'])('renders localized labels and submitted credentials (%s)',locale=>{
   const {store,container}=show(Login,{locale,me:null});
   expect(screen.getByRole('heading',{name:store.t('welcome')})).toBeTruthy();
   fireEvent.change(input(container,'email'),{target:{value:'user@example.test'}});
   fireEvent.change(input(container,'password'),{target:{value:'A-long-test-password!'}});
   fireEvent.submit(container.querySelector('form'));
   expect(store.login).toHaveBeenCalledWith('user@example.test','A-long-test-password!');
 });
 it('defaults to English independently of browser language',()=>{vi.spyOn(window.navigator,'language','get').mockReturnValue('de-DE');const {state}=show(Login,{me:null});expect(state.locale).toBe('en');expect(screen.getByRole('button',{name:'EN'}).getAttribute('aria-pressed')).toBe('true');});
 it('does not expose demonstration shortcuts in production',()=>{const {store}=show(Login,{demo:false,me:null});expect(screen.queryByText(store.t('demoAccounts'))).toBeNull();});
 it('routes a demo account through the store',()=>{const {store}=show(Login,{demo:true,me:null});fireEvent.click(screen.getByRole('button',{name:store.t('FINANCE')}));expect(store.loginDemo).toHaveBeenCalledWith('finance@belegflow.demo');});
 it('localizes an authentication error without printing its internals',()=>{const {store}=show(Login,{error:'AUTH_REQUIRED',me:null});expect(screen.getByRole('alert').textContent).toBe(store.t('error_AUTH_REQUIRED'));});
 it('disables submission and demo shortcuts while busy',()=>{const {store,container}=show(Login,{busy:true,demo:true,me:null});expect(container.querySelector('button[type=submit]').disabled).toBe(true);expect(screen.getByRole('button',{name:store.t('FINANCE')}).disabled).toBe(true);});
 it('exposes explicit language and theme controls',()=>{const {store}=show(Login,{me:null});fireEvent.click(screen.getByRole('button',{name:'DE'}));fireEvent.click(screen.getByRole('button',{name:store.t('theme')}));expect(store.setLocale).toHaveBeenCalledWith('de');expect(store.toggleTheme).toHaveBeenCalledOnce();});
 it('uses password and autocomplete attributes',()=>{const {container}=show(Login,{me:null});expect(input(container,'password').type).toBe('password');expect(input(container,'password').autocomplete).toBe('current-password');expect(input(container,'email').required).toBe(true);});
});

it('announces an unfinished authentication transition on the login form',()=>{
 const{container}=show(Login,{busy:true,me:null});
 expect(container.querySelector('form').getAttribute('aria-busy')).toBe('true');
 expect(container.querySelector('button[type=submit]').disabled).toBe(true);
});
