package in.codekerdos.inventory.dto;

import java.time.Instant;

public record StockResponse(String sku, int availableQuantity, Instant updatedAt) {
}
