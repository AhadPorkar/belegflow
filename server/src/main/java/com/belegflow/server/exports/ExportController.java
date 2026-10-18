package com.belegflow.server.exports;
import com.belegflow.server.documents.DocumentController;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.*;
@RestController @RequestMapping("/api/exports") public class ExportController {
    private final ExportService service;
    public ExportController(ExportService service) {
        this.service=service;
    }
    @GetMapping public List<Map<String,Object>> list() {
        return service.list();
    }
    @PostMapping public Map<String,UUID> create(@RequestBody ExportService.Request request) {
        return Map.of("id",service.create(request));
    }
    @GetMapping("/{id}/download") public ResponseEntity<byte[]> download(@PathVariable UUID id) {
        var d=service.download(id);
        return DocumentController.attachment(d.filename(),d.contentType(),d.sha256(),d.bytes());
    }
}
