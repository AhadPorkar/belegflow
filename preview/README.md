# Offline compatibility showcase

Open `BelegFlow-offline-showcase.html` in a recent Chrome/Edge browser. It works with synthetic, browser-local data; there is no real authentication, backend or financial integration here. It is **not** a production build and must never receive real invoices. The matching production application is in `server/` and `frontend/src/main.jsx`.

The HTML embeds React/ReactDOM **16.0.0**, the rendering binaries available to the isolated creation environment. The real frontend targets **19.3.0**. The same view components and client store are exercised, but that does not establish React 19 compatibility or server correctness. See THIRD-PARTY-NOTICE.md.

Demo sign-in: choose Finance, Approver or Administrator; password for synthetic accounts is `BelegFlow-Demo-2026!`. Reset button in the demo banner resets only this browser's synthetic records. Data uses localStorage; blocked storage or its small quota can prevent import. On browsers restricting file-origin storage/WebCrypto, serve the preview from localhost (for example `python -m http.server 8765` from the project root), then open the corresponding preview path. Do not expose that server publicly.

The showcase accepts the provided UBL fixture; CII is implemented/tested in Java, not duplicated into the preview parser. Showcase seed invoice numbers and export layout differ from the server fixtures. Follow the first `KE-2026-1048` scenario in either version. For the high-value preview use `KE-2026-1012`.

## Reproduce the compatibility artifact

The already-built HTML is included. To rebuild this *legacy compatibility artifact*, provide separately licensed React/ReactDOM 16 UMD paths through `BF_REACT_UMD` and `BF_REACT_DOM_UMD`, with a TypeScript module available at `frontend/node_modules/typescript` or `BF_TYPESCRIPT_MODULE`, then run `node scripts/build-compatibility-showcase.cjs`. This script is not the modern production build command.

`python scripts/test-showcase.py` requires Python Playwright and Chromium (`BF_CHROMIUM` can name the executable). It uses an about:blank DOM, an in-memory storage shim and a SHA-256 bridge because local file/navigation access was restricted during creation. Its report is compatibility evidence only. Use `npm run test:e2e` against the actual running Spring application for real full-stack tests.
