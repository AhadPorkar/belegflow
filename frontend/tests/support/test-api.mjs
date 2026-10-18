/** Real Node and Vitest runners share the same assertions/cases. Only teardown registration differs. */
const inVitest=process.env.VITEST==='true';
const runner=inVitest ? (await import('vitest')).test : (await import('node:test')).default;
export default function test(name,body) {
  return runner(name,async context=>{
    const cleanups=[];
    const t=inVitest ? {after:fn=>cleanups.push(fn)} : context;
    try { await body(t); }
    finally { if(inVitest) for(const cleanup of cleanups.reverse()) await cleanup(); }
  });
}
