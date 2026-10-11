package in.coderkerdos.ticketservice.mapper;
import org.mapstruct.Mapper;

import in.coderkerdos.ticketservice.dto.TicketResponse;
import in.coderkerdos.ticketservice.entity.Ticket;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    TicketResponse toResponse(Ticket ticket);
}
