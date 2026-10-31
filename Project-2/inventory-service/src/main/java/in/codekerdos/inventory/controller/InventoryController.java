package in.codekerdos.inventory.controller;

import in.codekerdos.inventory.dto.RestockRequest;
import in.codekerdos.inventory.dto.StockResponse;
import in.codekerdos.inventory.entity.StockItem;
import in.codekerdos.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/{sku}")
    public ResponseEntity<StockResponse> getStock(@PathVariable String sku) {
        StockItem item = inventoryService.getStock(sku);
        return ResponseEntity.ok(toResponse(item));
    }

    @PostMapping("/restock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StockResponse> restock(@Valid @RequestBody RestockRequest request) {
        StockItem item = inventoryService.restock(request.sku(), request.quantity());
        return ResponseEntity.ok(toResponse(item));
    }

    private StockResponse toResponse(StockItem item) {
        return new StockResponse(item.getSku(), item.getAvailableQuantity(), item.getUpdatedAt());
    }
}
