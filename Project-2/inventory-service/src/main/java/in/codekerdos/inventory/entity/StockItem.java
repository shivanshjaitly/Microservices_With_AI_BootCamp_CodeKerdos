package in.codekerdos.inventory.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "stock_items")
public class StockItem {

    @Id
    private String sku;

    private int availableQuantity;

    private Instant updatedAt;

    public StockItem() {
    }

    public StockItem(String sku, int availableQuantity, Instant updatedAt) {
        this.sku = sku;
        this.availableQuantity = availableQuantity;
        this.updatedAt = updatedAt;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }

    public void setAvailableQuantity(int availableQuantity) {
        this.availableQuantity = availableQuantity;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
