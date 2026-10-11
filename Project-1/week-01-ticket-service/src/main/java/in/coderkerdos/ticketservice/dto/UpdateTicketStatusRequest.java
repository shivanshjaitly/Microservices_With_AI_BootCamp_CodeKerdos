package in.coderkerdos.ticketservice.dto;

import in.coderkerdos.ticketservice.entity.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest (

    @NotNull(message = "Status must not be null")
    TicketStatus status
){}
