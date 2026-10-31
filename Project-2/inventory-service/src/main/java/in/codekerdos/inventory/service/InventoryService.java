package in.codekerdos.inventory.service;

import in.codekerdos.inventory.entity.StockItem;
import in.codekerdos.inventory.exception.ResourceNotFoundException;
import in.codekerdos.inventory.repository.StockItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class InventoryService {

    private final StockItemRepository stockItemRepository;

    public InventoryService(StockItemRepository stockItemRepository) {
        this.stockItemRepository = stockItemRepository;
    }

    public StockItem getStock(String sku) {
        return stockItemRepository.findById(sku)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown SKU: " + sku));
    }

    /**
     * Adds {@code quantity} to the SKU's available stock. If the SKU has never
     * been seen before, it is created — this lets an admin introduce a brand
     * new SKU purely via the restock endpoint.
     */
    @Transactional
    public StockItem restock(String sku, int quantity) {
        StockItem item = stockItemRepository.findById(sku)
                .orElseGet(() -> new StockItem(sku, 0, Instant.now()));
        item.setAvailableQuantity(item.getAvailableQuantity() + quantity);
        item.setUpdatedAt(Instant.now());
        return stockItemRepository.save(item);
    }

    /**
     * Attempts to reserve {@code quantity} units of {@code sku}. Uses a single
     * conditional UPDATE (see {@link StockItemRepository#decrementIfAvailable})
     * so the check-and-decrement is atomic at the database level — concurrent
     * reservations for the same SKU cannot oversell it.
     *
     * @return true if the reservation succeeded, false if the SKU is unknown
     *         or there isn't enough stock.
     */
    @Transactional
    public boolean reserve(String sku, int quantity) {
        if (!stockItemRepository.existsById(sku)) {
            return false;
        }
        return stockItemRepository.decrementIfAvailable(sku, quantity, Instant.now()) == 1;
    }

    /** Builds a human-readable reason to explain why a {@link #reserve} call failed. */
    public String rejectionReason(String sku, int quantity) {
        return stockItemRepository.findById(sku)
                .map(item -> "Insufficient stock for SKU " + sku + ": requested " + quantity
                        + ", available " + item.getAvailableQuantity())
                .orElse("Unknown SKU: " + sku);
    }
}
