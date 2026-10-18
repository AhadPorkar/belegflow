package com.belegflow.server.catalog;
import org.springframework.web.bind.annotation.*;
import java.util.*;
@RestController @RequestMapping("/api") public class CatalogController {
    private final CatalogService service;
    public CatalogController(CatalogService service) {
        this.service=service;
    }
    @GetMapping("/catalog") public Map<String,Object> all() {
        return service.all();
    }
    @PostMapping("/suppliers") public Map<String,UUID> supplier(@RequestBody CatalogService.SupplierBody b) {
        return Map.of("id",service.supplier(null,b));
    }
    @PutMapping("/suppliers/{id}") public Map<String,UUID> supplier(@PathVariable UUID id,@RequestBody CatalogService.SupplierBody b) {
        return Map.of("id",service.supplier(id,b));
    }
    @PostMapping("/projects") public Map<String,UUID> project(@RequestBody CatalogService.ProjectBody b) {
        return Map.of("id",service.project(b));
    }
    @PostMapping("/cost-centers") public Map<String,UUID> costCenter(@RequestBody CatalogService.CostCenterBody b) {
        return Map.of("id",service.costCenter(b));
    }
    @PutMapping("/settings/rules") public void rules(@RequestBody CatalogService.RuleBody b) {
        service.rules(b);
    }
    @PostMapping("/delegations") public Map<String,UUID> delegation(@RequestBody CatalogService.DelegationBody b) {
        return Map.of("id",service.delegate(b));
    }
    @DeleteMapping("/delegations/{id}") public void revoke(@PathVariable UUID id) {
        service.revoke(id);
    }
    @PostMapping("/users") public Map<String,UUID> user(@RequestBody CatalogService.UserBody b) {
        return Map.of("id",service.user(null,b));
    }
    @PutMapping("/users/{id}") public Map<String,UUID> user(@PathVariable UUID id,@RequestBody CatalogService.UserBody b) {
        return Map.of("id",service.user(id,b));
    }
}
