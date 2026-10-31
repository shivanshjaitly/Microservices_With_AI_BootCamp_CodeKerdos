package in.codekerdos.inventory.repository;

import in.codekerdos.inventory.entity.StockItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface StockItemRepository extends JpaRepository<StockItem, String> {

    /**
     * Conditional decrement in a single UPDATE statement — the WHERE clause's
     * {@code availableQuantity >= :quantity} check and the decrement happen
     * atomically at the database level, so concurrent reservations for the
     * same SKU can never oversell (no separate read-then-write race window).
     * Returns the number of rows updated: 1 if the reservation succeeded, 0 if
     * there wasn't enough stock.
     */
    @Modifying
    @Query("update StockItem s set s.availableQuantity = s.availableQuantity - :quantity, s.updatedAt = :now " +
            "where s.sku = :sku and s.availableQuantity >= :quantity")
    int decrementIfAvailable(@Param("sku") String sku, @Param("quantity") int quantity, @Param("now") Instant now);
}
