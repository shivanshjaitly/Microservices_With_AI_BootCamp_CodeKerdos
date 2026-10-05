package in.coderkerdos.week_01_ticket_service.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import in.coderkerdos.week_01_ticket_service.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
}
