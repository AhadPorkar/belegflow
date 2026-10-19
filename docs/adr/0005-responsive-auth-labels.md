# ADR 0005: Reachable account controls and exact accessible labels

Status: accepted in 0.2.2.

The uploaded CI reached the real browser for the first time: the backend and client build passed, then administration and preferences timed out. Locally reproducing the same UI revealed an offscreen fixed-sidebar footer and a label containing option text. Changing the test to force a click or match arbitrary CSS would conceal user-facing defects.

Use overflow containment for the fixed rail, a responsive topbar alternative, and explicit native label/control relationships in settings. Keep exact EN/DE assertions and normal actionable clicks. Keep the implicit Field path compatible for callers without an explicit ID.

Authentication handoff is a separate transaction: private state clears immediately but the next login is disabled until logout finishes. Real-server tests observe the HTTP response; they do not replace authentication with a fixture response. Failures release controls but remain visible. This does not solve forced browser closure during network loss, which is not claimed.

References: [W3C form labels](https://www.w3.org/WAI/tutorials/forms/labels/) and [Playwright locators](https://playwright.dev/docs/locators). These explain the API/label approach; [the supplied CI analysis](../testing/CI-106036450086.md) records the project evidence.
