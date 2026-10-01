package com.shopsphere.inventory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {
    private final InventoryService service;
    public InventoryController(InventoryService service) { this.service = service; }
    @GetMapping("/{productId}")
    public InventoryEvents.InventoryResponse get(@PathVariable Long productId) { return InventoryService.response(service.get(productId)); }
    @PutMapping("/{productId}")
    public InventoryEvents.InventoryResponse update(@PathVariable Long productId, @Valid @RequestBody StockRequest request) {
        return InventoryService.response(service.update(productId, request.sku(), request.quantity()));
    }
    @PostMapping("/reserve")
    public InventoryEvents.InventoryResponse reserve(@Valid @RequestBody ReserveRequest request) {
        return InventoryService.response(service.reserve(request.productId(), request.orderId(), request.quantity()));
    }
    @PostMapping("/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@Valid @RequestBody ReleaseRequest request) { service.release(request.productId(), request.orderId()); }
    public record StockRequest(@NotBlank String sku, @Min(0) int quantity) {}
    public record ReserveRequest(@NotNull Long productId, @NotNull Long orderId, @Min(1) int quantity) {}
    public record ReleaseRequest(@NotNull Long productId, @NotNull Long orderId) {}
}