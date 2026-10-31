package in.codekerdos.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RestockRequest(
        @NotBlank(message = "sku is required") String sku,
        @Positive(message = "quantity must be positive") int quantity
) {
}
