package in.codekerdos.order.dto;

import in.codekerdos.order.enums.Role;

public record AuthResponse(
        String token,
        String email,
        String fullName,
        Role role
) {
}
