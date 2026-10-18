# ADR 0004: preserve multipart builder typing in upload tests

Status: implemented in 0.2.1; networked regression execution pending.

## Context

CI run 106020289651 failed to compile `ModuleIntegrationTest.pdf(String)`. The returned value is a Spring multipart builder, while the signature claimed the ordinary concrete request builder. In the current API the multipart builder extends the multipart abstract builder, which extends the shared abstract HTTP builder, not the ordinary concrete builder.

## Decision

Return `MockMultipartHttpServletRequestBuilder` and keep the request body unchanged. Do not cast or downgrade dependencies. Verify the actual helper with database-free tests that mock SQL lookups but use real request builders. Expose compilation as an explicit CI phase before focused configuration and builder regressions.

## Consequences

No business behavior or schema change. Date-negative tests retain their deliberately incomplete requests. Authorization and PostgreSQL import behavior remain the responsibility of the retained integration suite.

Reference: https://docs.spring.io/spring-framework/docs/7.0.9/javadoc-api/org/springframework/test/web/servlet/request/MockMultipartHttpServletRequestBuilder.html
