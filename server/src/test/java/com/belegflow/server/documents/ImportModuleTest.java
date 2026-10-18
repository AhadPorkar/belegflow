package com.belegflow.server.documents;
import com.belegflow.domain.*;
import com.belegflow.server.support.ServiceFixture;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImportModuleTest extends ServiceFixture {
    final DocumentService documents=mock(DocumentService.class);
    final InvoiceImportService service=new InvoiceImportService(db,identity,documents,audit);
    byte[] bytes(String s){return s.getBytes(StandardCharsets.UTF_8);}
    Map<String,String> pdfMetadata(UUID supplier){return Map.of("supplier_id",supplier.toString(),"invoice_number","PDF-1","issue_date","2026-10-01","due_date","2026-10-31","net","100.00","tax","19.00","gross","119.00");}
    void validSupplier(UUID supplier){when(db.queryForObject("SELECT EXISTS(SELECT 1 FROM supplier WHERE tenant_id=? AND id=? AND active=true)",Boolean.class,tenant,supplier)).thenReturn(true);when(db.queryForObject("SELECT second_approval_threshold FROM organization WHERE id=?",BigDecimal.class,tenant)).thenReturn(new BigDecimal("10000"));}
    @Test void identicalFileReturnsOriginalWithoutParsingOrCreatingInvoice(){
        byte[] b=bytes("<not-even-an-invoice/>");when(db.queryForList("SELECT invoice_id FROM document WHERE tenant_id=? AND sha256=? AND kind='ORIGINAL'",tenant,DuplicateFingerprint.sha256(b))).thenReturn(List.of(Map.of("invoice_id",invoiceId)));
        var result=service.ingest("invoice.xml",b,Map.of());assertTrue(result.reused());assertEquals(invoiceId,result.id());assertTrue(writes().isEmpty());verifyNoInteractions(documents);verify(audit).append(actor,invoiceId,"IDENTICAL_IMPORT_IGNORED","invoice.xml");
    }
    @Test void unsafeXmlNeverCreatesSupplier(){assertEquals("INVALID_XML",assertThrows(BusinessException.class,()->service.ingest("invoice.xml",bytes("<!DOCTYPE a [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><a>&x;</a>"),Map.of())).code());assertTrue(writes().isEmpty());verifyNoInteractions(documents);}
    @Test void unsupportedFilenameRejectedBeforeTenantLock(){assertThrows(BusinessException.class,()->service.ingest("danger.exe",bytes("MZ"),Map.of()));verifyNoInteractions(db);}
    @Test void pdfCreatesInvoiceAndOriginalWithExactAmounts(){
        UUID supplier=UUID.randomUUID();validSupplier(supplier);byte[] b=bytes("%PDF-1.7\nsynthetic");
        var result=service.ingest("invoice.pdf",b,pdfMetadata(supplier));assertFalse(result.reused());assertEquals(new BigDecimal("119.00"),arg("INSERT INTO invoice",10));assertEquals(true,arg("INSERT INTO invoice",13));
        verify(documents).insert(actor,result.id(),"ORIGINAL","invoice.pdf","application/pdf",b);verify(audit).append(actor,result.id(),"BASIC_CHECKS_PASSED","");
    }
    @Test void invalidPdfTotalsAreRetainedAsReviewableValidationFailure(){
        UUID supplier=UUID.randomUUID();validSupplier(supplier);var m=new HashMap<>(pdfMetadata(supplier));m.put("gross","120.00");
        var result=service.ingest("invoice.pdf",bytes("%PDF-1.7\ninvalid-total"),m);assertEquals(false,arg("INSERT INTO invoice",13));verify(audit).append(actor,result.id(),"BASIC_CHECKS_FAILED","TOTALS_OR_CURRENCY_INVALID");
    }
    @Test void foreignSupplierIsRejected(){UUID supplier=UUID.randomUUID();assertEquals("INVALID_SUPPLIER",assertThrows(BusinessException.class,()->service.ingest("invoice.pdf",bytes("%PDF-1.7\nforeign"),pdfMetadata(supplier))).code());assertTrue(writes().isEmpty());}
    @Test void businessDuplicateCreatesWarningRatherThanDroppingOriginal(){
        UUID supplier=UUID.randomUUID();validSupplier(supplier);String key=DuplicateFingerprint.businessKey(supplier,"PDF-1");when(db.queryForObject("SELECT EXISTS(SELECT 1 FROM invoice WHERE tenant_id=? AND business_key=?)",Boolean.class,tenant,key)).thenReturn(true);
        var result=service.ingest("invoice.pdf",bytes("%PDF-1.7\nrevision"),pdfMetadata(supplier));assertTrue(result.duplicate_warning());verify(audit).append(actor,result.id(),"POSSIBLE_DUPLICATE",key);
    }
    @Test void missingPdfDatesAreClientErrorsNotInternalServerErrors(){
        var m=new HashMap<>(pdfMetadata(UUID.randomUUID()));m.remove("issue_date");
        assertEquals("INVALID_INPUT",assertThrows(BusinessException.class,()->service.ingest("invoice.pdf",bytes("%PDF-1.7\nmissing-date"),m)).code());assertTrue(writes().isEmpty());
    }
}
