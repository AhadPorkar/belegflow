package com.belegflow.server.documents;
import com.belegflow.domain.*;
import com.belegflow.server.support.ServiceFixture;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.charset.StandardCharsets;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DocumentModuleTest extends ServiceFixture {
    final DocumentService service=new DocumentService(db,identity,repo,audit,clock);
    final byte[] evidence="Completed and inspected".getBytes(StandardCharsets.UTF_8);
    @Test void preservesBytesAndHashesAtStorageBoundary(){
        UUID id=service.insert(actor,invoiceId,"EVIDENCE","report.txt","text/plain",evidence);
        assertEquals(id,arg("INSERT INTO document",1));assertEquals(tenant,arg("INSERT INTO document",2));assertEquals(DuplicateFingerprint.sha256(evidence),arg("INSERT INTO document",7));assertArrayEquals(evidence,(byte[])arg("INSERT INTO document",9));
    }
    @Test void uploadInvalidatesEarlierAcceptance(){
        row("REVIEWED");service.evidence(invoiceId,3,"report.txt",evidence);
        assertTrue(write("UPDATE invoice").getArgument(0,String.class).contains("evidence_accepted_by=null"));verify(audit).append(actor,invoiceId,"EVIDENCE_UPLOADED","report.txt");
    }
    @Test void finalizedInvoiceRejectsEvidence(){row("APPROVED");assertEquals("INVALID_TRANSITION",assertThrows(BusinessException.class,()->service.evidence(invoiceId,3,"report.txt",evidence)).code());assertTrue(writes().isEmpty());}
    @Test void assignedApproverMayUploadEvidence(){as(Role.APPROVER);var row=row("REVIEWED");row.put("approver_id",actorId);assertNotNull(service.evidence(invoiceId,3,"report.txt",evidence));}
    @Test void unrelatedApproverCannotUpload(){as(Role.APPROVER);row("REVIEWED");assertThrows(BusinessException.class,()->service.evidence(invoiceId,3,"report.txt",evidence));assertTrue(writes().isEmpty());}
    @Test void evidenceLimitRejectsTwentyFirstFile(){row("REVIEWED");when(repo.evidenceCount(tenant,invoiceId)).thenReturn(20);assertEquals("DOCUMENT_LIMIT",assertThrows(BusinessException.class,()->service.evidence(invoiceId,3,"report.txt",evidence)).code());assertTrue(writes().isEmpty());}
    @Test void fakePdfRejectedBeforeStorage(){row("REVIEWED");assertEquals("UNSUPPORTED_FILE",assertThrows(BusinessException.class,()->service.evidence(invoiceId,3,"invoice.pdf",evidence)).code());assertTrue(writes().isEmpty());}
    @Test void downloadPreservesOriginalMetadata(){
        UUID id=UUID.randomUUID();when(db.queryForList("SELECT filename,content_type,sha256,content FROM document WHERE tenant_id=? AND id=?",tenant,id)).thenReturn(List.of(Map.of("filename","report.txt","content_type","text/plain","sha256","abc","content",evidence)));
        var download=service.download(id);assertArrayEquals(evidence,download.bytes());assertEquals("report.txt",download.filename());
    }
    @Test void controllerDownloadForcesAttachmentAndNoStore(){
        var response=DocumentController.attachment("report.txt","text/plain","abc",evidence);
        assertArrayEquals(evidence,response.getBody());assertTrue(response.getHeaders().getFirst("Content-Disposition").startsWith("attachment;"));assertEquals("no-store",response.getHeaders().getCacheControl());assertEquals("nosniff",response.getHeaders().getFirst("X-Content-Type-Options"));assertEquals("abc",response.getHeaders().getFirst("X-Content-SHA256"));
    }
    @Test void multipartControllerPassesFilenameBytesAndMetadata() throws Exception {
        var importer=mock(InvoiceImportService.class);var docs=mock(DocumentService.class);var controller=new DocumentController(docs,importer);
        var metadata=Map.of("project_id",projectId.toString());var file=new MockMultipartFile("file","original.xml","application/xml",evidence);
        controller.ingest(file,metadata);verify(importer).ingest("original.xml",evidence,metadata);
    }
    @Test void evidenceControllerReturnsStoredIdentifier() throws Exception {
        var docs=mock(DocumentService.class);UUID document=UUID.randomUUID();when(docs.evidence(invoiceId,3,"report.txt",evidence)).thenReturn(document);
        var response=new DocumentController(docs,mock(InvoiceImportService.class)).evidence(invoiceId,3,new MockMultipartFile("file","report.txt","text/plain",evidence));assertEquals(document,response.get("id"));
    }
}
