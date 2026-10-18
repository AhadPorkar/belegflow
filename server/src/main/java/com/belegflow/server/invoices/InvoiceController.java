package com.belegflow.server.invoices;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api/invoices") public class InvoiceController {
    private final InvoiceService service;
    public InvoiceController(InvoiceService service) {
        this.service=service;
    }
    @GetMapping public Map<String,Object> list(@RequestParam(defaultValue="") String q,@RequestParam(defaultValue="") String stage,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size) {
        return service.list(q,stage,page,size);
    }
    @GetMapping("/summary") public Map<String,Object> summary() {
        return service.summary();
    }
    @GetMapping("/{id}") public Map<String,Object> get(@PathVariable UUID id) {
        return service.detail(id);
    }
    @PostMapping("/{id}/commands") public void command(@PathVariable UUID id,@RequestBody InvoiceService.Command b) {
        service.command(id,b);
    }
    @PutMapping("/{id}/allocation") public void allocation(@PathVariable UUID id,@RequestBody InvoiceService.Allocation b) {
        service.allocate(id,b);
    }
    @PostMapping("/{id}/duplicate-resolution") public void duplicate(@PathVariable UUID id,@RequestBody InvoiceService.Note b) {
        service.clearDuplicate(id,b);
    }
    @PostMapping("/{id}/evidence-acceptance") public void evidence(@PathVariable UUID id,@RequestBody InvoiceService.Note b) {
        service.acceptEvidence(id,b);
    }
    @PostMapping("/{id}/comments") public Map<String,UUID> comment(@PathVariable UUID id,@RequestBody InvoiceService.Comment b) {
        return Map.of("id",service.comment(id,b));
    }
    @PostMapping("/{id}/questions/{question}/resolution") public void resolve(@PathVariable UUID id,@PathVariable UUID question,@RequestBody InvoiceService.Comment b) {
        service.resolveQuestion(id,question,b);
    }
}
