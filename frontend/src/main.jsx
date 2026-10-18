import React, { useSyncExternalStore } from 'react';
import { createRoot } from 'react-dom/client';
import { App, ErrorBoundary } from './App.jsx';
import { HttpApi } from './api.ts';
import { WorkspaceStore } from './store.ts';
import './styles.css';
const store = new WorkspaceStore(new HttpApi());
function Root() { const state = useSyncExternalStore(store.subscribe, store.getSnapshot); return <App state={state} store={store}/>; }
createRoot(document.getElementById('root')).render(<ErrorBoundary store={store}><Root /></ErrorBoundary>);
void store.boot();
window.addEventListener('keydown', event => { if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 'k' && store.getSnapshot().me) {
    event.preventDefault();
    store.openDialog('commands');
} });
