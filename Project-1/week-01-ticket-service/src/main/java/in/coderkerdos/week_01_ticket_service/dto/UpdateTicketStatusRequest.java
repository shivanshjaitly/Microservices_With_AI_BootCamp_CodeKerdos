package in.coderkerdos.week_01_ticket_service.dto;

import in.coderkerdos.week_01_ticket_service.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest (

    @NotNull(message = "Status must not be null")
    TicketStatus status
){}
