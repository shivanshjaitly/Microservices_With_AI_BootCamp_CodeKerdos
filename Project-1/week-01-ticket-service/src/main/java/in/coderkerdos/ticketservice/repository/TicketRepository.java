package in.coderkerdos.ticketservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import in.coderkerdos.ticketservice.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    
}
