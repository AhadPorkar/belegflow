import React from 'react';
import { App, ErrorBoundary } from '../src/App.jsx';
import { WorkspaceStore } from '../src/store.ts';
import { DemoApi } from './DemoApi.js';
// This entrypoint is an explicitly labelled offline compatibility showcase only.
const api = await new DemoApi().init();
const store = new WorkspaceStore(api);
window.belegflowShowcase = { store, api };
function render() { ReactDOM.render(<ErrorBoundary store={store}><App state={store.getSnapshot()} store={store}/></ErrorBoundary>, document.getElementById('root')); }
store.subscribe(render);
render();
await store.boot();
window.addEventListener('keydown', e => { if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k' && store.getSnapshot().me) {
    e.preventDefault();
    store.openDialog('commands');
} });
