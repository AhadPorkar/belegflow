package com.belegflow.server.exports;
import com.belegflow.domain.*;
import com.belegflow.server.support.ServiceFixture;
import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;
import java.util.zip.ZipInputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExportModuleTest extends ServiceFixture {
    final ExportService service=new ExportService(db,identity,repo,audit,clock);
    ExportService.Request request(UUID key){return new ExportService.Request(key,List.of(new ExportService.Entry(invoiceId,3)));}
    Map<String,Object> approved(){var r=row("APPROVED");r.put("first_approved_by",approverId);when(db.queryForObject("SELECT coalesce(sum(size_bytes),0) FROM document WHERE tenant_id=? AND invoice_id=?",Long.class,tenant,invoiceId)).thenReturn(10L);return r;}
    @Test void emptyMissingDuplicateOrNegativeVersionRequestRejectedBeforeDatabase(){
        for(var request:List.of(new ExportService.Request(null,List.of()),new ExportService.Request(UUID.randomUUID(),List.of()),new ExportService.Request(UUID.randomUUID(),List.of(new ExportService.Entry(invoiceId,-1))),new ExportService.Request(UUID.randomUUID(),List.of(new ExportService.Entry(invoiceId,3),new ExportService.Entry(invoiceId,3))))) {
            assertEquals("INVALID_INPUT",assertThrows(BusinessException.class,()->service.create(request)).code());
        }
        verifyNoInteractions(db,repo,audit);
    }
    @Test void moreThanTwentyInvoicesRejected(){
        List<ExportService.Entry> list=new ArrayList<>();for(int i=0;i<21;i++)list.add(new ExportService.Entry(UUID.randomUUID(),1));
        assertThrows(BusinessException.class,()->service.create(new ExportService.Request(UUID.randomUUID(),list)));verifyNoInteractions(db);
    }
    @Test void repeatedRequestReturnsPersistedExportWithoutReexport(){
        UUID key=UUID.randomUUID(),export=UUID.randomUUID();String fingerprint=DuplicateFingerprint.sha256(("|"+invoiceId+":3").getBytes(StandardCharsets.UTF_8));
        when(db.queryForList("SELECT id,request_fingerprint FROM accounting_export WHERE tenant_id=? AND idempotency_key=?",tenant,key)).thenReturn(List.of(Map.of("id",export,"request_fingerprint",fingerprint)));
        assertEquals(export,service.create(request(key)));verifyNoInteractions(repo,audit);assertTrue(writes().isEmpty());
    }
    @Test void reusedKeyForDifferentPayloadIsConflict(){
        UUID key=UUID.randomUUID();when(db.queryForList("SELECT id,request_fingerprint FROM accounting_export WHERE tenant_id=? AND idempotency_key=?",tenant,key)).thenReturn(List.of(Map.of("id",UUID.randomUUID(),"request_fingerprint","different")));
        assertEquals("IDEMPOTENCY_CONFLICT",assertThrows(BusinessException.class,()->service.create(request(key))).code());verifyNoInteractions(repo,audit);
    }
    @Test void packageSizeBoundCheckedBeforeMutations(){approved();when(db.queryForObject("SELECT coalesce(sum(size_bytes),0) FROM document WHERE tenant_id=? AND invoice_id=?",Long.class,tenant,invoiceId)).thenReturn(50L*1024*1024+1);assertEquals("EXPORT_TOO_LARGE",assertThrows(BusinessException.class,()->service.create(request(UUID.randomUUID()))).code());assertTrue(writes().isEmpty());}
    @Test void unapprovedInvoiceCannotBeExported(){row("REVIEWED");assertEquals("EXPORT_REQUIRES_APPROVAL",assertThrows(BusinessException.class,()->service.create(request(UUID.randomUUID()))).code());assertTrue(writes().isEmpty());}
    @Test void missingIndependentSecondApprovalBlocksPackage(){var r=approved();r.put("second_required",true);assertEquals("SECOND_APPROVAL_REQUIRED",assertThrows(BusinessException.class,()->service.create(request(UUID.randomUUID()))).code());assertTrue(writes().isEmpty());}
    @Test void generatedZipContainsOriginalsManifestBilingualInstructionsAndNeutralizedCsv() throws Exception {
        var r=approved();r.put("supplier_name","=HYPERLINK(\"bad\")");byte[] original="<Invoice>synthetic</Invoice>".getBytes(StandardCharsets.UTF_8);
        String docSql="SELECT id,kind,filename,sha256,content FROM document WHERE tenant_id=? AND invoice_id=? ORDER BY kind,id";
        when(db.queryForList(docSql,tenant,invoiceId)).thenReturn(List.of(Map.of("id",UUID.randomUUID(),"kind","ORIGINAL","filename","source.xml","sha256",DuplicateFingerprint.sha256(original),"content",original)));
        UUID id=service.create(request(UUID.randomUUID()));byte[] archive=(byte[])arg("INSERT INTO accounting_export",8);
        assertEquals(DuplicateFingerprint.sha256(archive),arg("INSERT INTO accounting_export",7));
        Map<String,byte[]> entries=new HashMap<>();try(var zip=new ZipInputStream(new ByteArrayInputStream(archive),StandardCharsets.UTF_8)){java.util.zip.ZipEntry e;while((e=zip.getNextEntry())!=null)entries.put(e.getName(),zip.readAllBytes());}
        assertArrayEquals(original,entries.get("originals/"+invoiceId+".xml"));
        assertTrue(entries.keySet().containsAll(Set.of("manifest.csv","invoices.csv","audit.csv","README.en.txt","README.de.txt")));
        assertTrue(new String(entries.get("manifest.csv"),StandardCharsets.UTF_8).contains(DuplicateFingerprint.sha256(original)));
        assertTrue(new String(entries.get("invoices.csv"),StandardCharsets.UTF_8).contains("'=HYPERLINK"));
        verify(audit).append(actor,invoiceId,"ACCOUNTING_PACKAGE_CREATED",id.toString());assertEquals(tenant,arg("UPDATE invoice",1));
    }
    @Test void downloadScopesToTenantAndPreservesBytes(){UUID id=UUID.randomUUID();byte[] bytes={80,75,1,2};when(db.queryForList("SELECT filename,sha256,content FROM accounting_export WHERE tenant_id=? AND id=?",tenant,id)).thenReturn(List.of(Map.of("filename","export.zip","sha256","hash","content",bytes)));var d=service.download(id);assertArrayEquals(bytes,d.bytes());assertEquals("application/zip",d.contentType());}
    @Test void viewerCannotListCreateOrDownloadPackages(){as(Role.VIEWER);assertThrows(BusinessException.class,service::list);assertThrows(BusinessException.class,()->service.download(UUID.randomUUID()));assertThrows(BusinessException.class,()->service.create(request(UUID.randomUUID())));verifyNoInteractions(db);}
}
