package in.codekerdos.inventory.config;

import in.codekerdos.inventory.entity.StockItem;
import in.codekerdos.inventory.repository.StockItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Seeds a handful of demo SKUs on startup. SKU-HEADSET-003 is deliberately
 * seeded with a low quantity (5) so students can order past it in class and
 * see the INVENTORY_REJECTED path fire for real.
 */
@Component
public class DataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

    private final StockItemRepository stockItemRepository;

    public DataLoader(StockItemRepository stockItemRepository) {
        this.stockItemRepository = stockItemRepository;
    }

    @Override
    public void run(String... args) {
        seed("SKU-LAPTOP-001", 50);
        seed("SKU-PHONE-002", 100);
        seed("SKU-HEADSET-003", 5);
    }

    private void seed(String sku, int quantity) {
        if (stockItemRepository.existsById(sku)) {
            return;
        }
        stockItemRepository.save(new StockItem(sku, quantity, Instant.now()));
        log.info("Seeded stock item {} with quantity {}", sku, quantity);
    }
}
