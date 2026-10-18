package com.belegflow.server.documents;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
import java.util.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
@RestController @RequestMapping("/api") public class DocumentController {
    private final DocumentService documents;
    private final InvoiceImportService importer;
    public DocumentController(DocumentService documents,InvoiceImportService importer) {
        this.documents=documents;
        this.importer=importer;
    }
    @PostMapping(value="/invoices/import",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public InvoiceImportService.Result ingest(@RequestPart("file") MultipartFile file,@RequestParam Map<String,String> metadata)throws IOException {
        return importer.ingest(file.getOriginalFilename(),file.getBytes(),metadata);
    }
    @PostMapping(value="/invoices/{id}/evidence",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public Map<String,UUID> evidence(@PathVariable UUID id,@RequestParam int version,@RequestPart("file") MultipartFile file)throws IOException {
        return Map.of("id",documents.evidence(id,version,file.getOriginalFilename(),file.getBytes()));
    }
    @GetMapping("/documents/{id}") public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        var d=documents.download(id);
        return attachment(d.filename(),d.contentType(),d.sha256(),d.bytes());
    }
    public static ResponseEntity<byte[]> attachment(String name,String type,String hash,byte[] bytes) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type)).cacheControl(CacheControl.noStore()) .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(name,StandardCharsets.UTF_8).build().toString()) .header("X-Content-SHA256",hash).header("X-Content-Type-Options","nosniff").body(bytes);
    }
}
