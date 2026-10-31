package in.codekerdos.inventory.service;

import in.codekerdos.inventory.entity.StockItem;
import in.codekerdos.inventory.repository.StockItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private StockItemRepository stockItemRepository;

    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        inventoryService = new InventoryService(stockItemRepository);
    }

    @Test
    void reserve_returnsTrue_whenEnoughStockIsAvailable() {
        when(stockItemRepository.existsById("SKU-LAPTOP-001")).thenReturn(true);
        when(stockItemRepository.decrementIfAvailable(eq("SKU-LAPTOP-001"), eq(5), any(Instant.class)))
                .thenReturn(1);

        boolean reserved = inventoryService.reserve("SKU-LAPTOP-001", 5);

        assertThat(reserved).isTrue();
    }

    @Test
    void reserve_returnsFalse_whenStockIsInsufficient() {
        when(stockItemRepository.existsById("SKU-HEADSET-003")).thenReturn(true);
        when(stockItemRepository.decrementIfAvailable(eq("SKU-HEADSET-003"), eq(100), any(Instant.class)))
                .thenReturn(0);

        boolean reserved = inventoryService.reserve("SKU-HEADSET-003", 100);

        assertThat(reserved).isFalse();
    }

    @Test
    void reserve_returnsFalse_whenSkuIsUnknown() {
        when(stockItemRepository.existsById("SKU-UNKNOWN-999")).thenReturn(false);

        boolean reserved = inventoryService.reserve("SKU-UNKNOWN-999", 1);

        assertThat(reserved).isFalse();
    }

    @Test
    void rejectionReason_explainsInsufficientStock() {
        StockItem item = new StockItem("SKU-HEADSET-003", 5, Instant.now());
        when(stockItemRepository.findById("SKU-HEADSET-003")).thenReturn(Optional.of(item));

        String reason = inventoryService.rejectionReason("SKU-HEADSET-003", 100);

        assertThat(reason).contains("Insufficient stock").contains("SKU-HEADSET-003");
    }

    @Test
    void rejectionReason_explainsUnknownSku() {
        when(stockItemRepository.findById("SKU-UNKNOWN-999")).thenReturn(Optional.empty());

        String reason = inventoryService.rejectionReason("SKU-UNKNOWN-999", 1);

        assertThat(reason).contains("Unknown SKU");
    }
}
