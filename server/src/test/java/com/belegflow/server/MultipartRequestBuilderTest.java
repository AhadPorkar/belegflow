package com.belegflow.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartHttpServletRequest;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Database-free regression for the multipart helper that failed to compile in CI run 106020289651.
 * Uses the actual integration-test helper and real Spring request builders. Only SQL lookup results
 * are mocked; these tests do not replace the PostgreSQL import/authorization integration cases.
 */
class MultipartRequestBuilderTest {
    private static final UUID TENANT = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID INVOICE = UUID.fromString("20000000-0000-0000-0000-000000000002");
    private static final UUID SUPPLIER = UUID.fromString("30000000-0000-0000-0000-000000000003");
    private ModuleIntegrationTest fixture;

    @BeforeEach
    void prepareLookupFixture() {
        // Plain construction does not start the @SpringBootTest application or connect to a database.
        fixture = new ModuleIntegrationTest();
        fixture.db = mock(JdbcTemplate.class);
        when(fixture.db.queryForObject(
            "SELECT tenant_id FROM app_user WHERE email=?", UUID.class, ModuleIntegrationTest.FINANCE))
            .thenReturn(TENANT);
        when(fixture.db.queryForObject(
            "SELECT id FROM invoice WHERE tenant_id=? AND invoice_number=? ORDER BY received_at DESC LIMIT 1",
            UUID.class, TENANT, "AC-26-104"))
            .thenReturn(INVOICE);
        when(fixture.db.queryForObject(
            "SELECT supplier_id FROM invoice WHERE id=?", UUID.class, INVOICE))
            .thenReturn(SUPPLIER);
    }

    private MockMultipartHttpServletRequest build(MockMultipartHttpServletRequestBuilder builder) {
        return assertInstanceOf(MockMultipartHttpServletRequest.class,
            builder.postProcessRequest(builder.buildRequest(new MockServletContext())));
    }

    @Test
    void helperDeclaresTheMultipartBuilderType() throws NoSuchMethodException {
        assertEquals(MockMultipartHttpServletRequestBuilder.class,
            ModuleIntegrationTest.class.getDeclaredMethod("pdf", String.class).getReturnType());
    }

    @Test
    void helperPreservesTheOriginalPdfBytesAndMetadata() throws Exception {
        var request = build(fixture.pdf("BF-REGRESSION-1"));
        assertEquals("POST", request.getMethod());
        assertEquals("/api/invoices/import", request.getRequestURI());
        var file = request.getFile("file");
        assertNotNull(file);
        assertEquals("invoice.pdf", file.getOriginalFilename());
        assertEquals("application/pdf", file.getContentType());
        assertArrayEquals("%PDF-1.7\nBF-REGRESSION-1".getBytes(StandardCharsets.UTF_8), file.getBytes());
    }

    @Test
    void helperPreservesSupplierAndExactDecimalParameters() {
        var request = build(fixture.pdf("BF-REGRESSION-2"));
        assertEquals(SUPPLIER.toString(), request.getParameter("supplier_id"));
        assertEquals("BF-REGRESSION-2", request.getParameter("invoice_number"));
        assertEquals("100.00", request.getParameter("net"));
        assertEquals("19.00", request.getParameter("tax"));
        assertEquals("119.00", request.getParameter("gross"));
    }

    @Test
    void helperLeavesDatesAbsentForMissingDateValidationCases() {
        var request = build(fixture.pdf("BF-REGRESSION-3"));
        assertNull(request.getParameter("issue_date"));
        assertNull(request.getParameter("due_date"));
    }

    @Test
    void fluentDateParametersKeepTheMultipartTypeWithoutCasts() {
        MockMultipartHttpServletRequestBuilder builder = fixture.pdf("BF-REGRESSION-4")
            .param("issue_date", "2026-10-01")
            .param("due_date", "2026-10-31");
        var request = build(builder);
        assertEquals("2026-10-01", request.getParameter("issue_date"));
        assertEquals("2026-10-31", request.getParameter("due_date"));
        assertNotNull(request.getFile("file"));
    }

    @Test
    void fluentPostProcessorsKeepTheMultipartTypeWithoutCasts() {
        MockMultipartHttpServletRequestBuilder builder = fixture.pdf("BF-REGRESSION-5")
            .with(request -> {
                request.setAttribute("belegflow.regression.processed", Boolean.TRUE);
                return request;
            });
        var request = build(builder);
        assertEquals(Boolean.TRUE, request.getAttribute("belegflow.regression.processed"));
        assertNotNull(request.getFile("file"));
    }
}
