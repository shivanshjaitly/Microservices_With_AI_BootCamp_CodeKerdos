package in.coderkerdos.week_01_ticket_service.mapper;
import org.mapstruct.Mapper;

import in.coderkerdos.week_01_ticket_service.dto.TicketResponse;
import in.coderkerdos.week_01_ticket_service.entity.Ticket;

@Mapper(componentModel = "spring")
public interface TicketMapper {

    TicketResponse toResponse(Ticket ticket);
}
