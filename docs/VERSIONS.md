# Version baseline and source ledger

Checked 2026-10-18 against official project release/documentation pages. A version listed by its maintainer is not proof that the whole dependency graph or a particular container image digest has resolved in this environment.

| Component | Configured version / policy | Primary source |
|---|---|---|
| Spring Boot | 4.1.1 | https://spring.io/projects/spring-boot/ |
| Java | 25 LTS; current compatible vendor security patch at release | https://docs.spring.io/spring-boot/system-requirements.html |
| PostgreSQL | 18.6; 19 RC excluded | https://www.postgresql.org/ |
| React / ReactDOM | 19.3.0 | https://react.dev/versions |
| TypeScript | 7.0.2 | https://github.com/microsoft/TypeScript/releases/latest |
| Vite | 8.3.4 | https://github.com/vitejs/vite/releases/latest |
| Node | 24.22.0 LTS rather than non-LTS Current | https://nodejs.org/en/about/previous-releases |
| Maven | 3.10.0 | https://maven.apache.org/download.cgi |
| Playwright | 1.64.1 | https://github.com/microsoft/playwright/releases/latest |
| GitHub checkout | v7.0.1 | https://github.com/actions/checkout/releases/latest |
| GitHub setup-java | v6.1.0 | https://github.com/actions/setup-java/releases/latest |
| GitHub setup-node | v7.1.0 | https://github.com/actions/setup-node/releases/latest |
| GitHub upload-artifact | v7.0.2 | https://github.com/actions/upload-artifact/releases/latest |

Use the latest stable supported compatible release, not every newest non-LTS or release candidate. Boot 4.1.1 documentation lists support up to Java 26; Java 27 is therefore not silently assumed supported. Java 25 LTS is an explicit compatibility choice.

Spring BOM-managed versions are not individually overridden. npm's top-level dependencies are exact; the transitive lockfile still requires the first networked resolution and review. Eclipse Temurin images use the 25 LTS family tag to receive vendor patches; production release must record and pin tested digests. The GitHub runner uses a moving hosted image and action release tags, not immutable commit hashes; tighten these to approved hashes/digests as part of the release gate.

**Actual local tools:** JDK 21.0.11, Node 22.16.0, TypeScript 5.8.3 and a bundled React/ReactDOM 16.0.0 compatibility renderer. These tools exercised only the documented local scopes. They were not substituted into production manifests, and their success does not verify the configured modern dependency matrix.
