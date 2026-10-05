package in.coderkerdos.week_01_ticket_service.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.coderkerdos.week_01_ticket_service.dto.CreateTicketRequest;
import in.coderkerdos.week_01_ticket_service.dto.TicketResponse;
import in.coderkerdos.week_01_ticket_service.entity.Ticket;
import in.coderkerdos.week_01_ticket_service.entity.TicketStatus;
import in.coderkerdos.week_01_ticket_service.repository.TicketRepository;

@Service 
public class TicketService {

    private final TicketRepository ticketRepository;
    
    public TicketService(TicketRepository ticketRepository){
        this.ticketRepository = ticketRepository;
    }

    @Transactional 
    public TicketResponse create(CreateTicketRequest request){
        Ticket ticket = new Ticket();
        ticket.setSubject(request.subject());
        ticket.setDescription(request.description());
        ticket.setRequesterEmail(request.requesterEmail());
        ticket.setStatus(TicketStatus.OPEN);

        return TicketResponse.from(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketResponse getById(Long id) throws Exception{
        return TicketResponse.from(findOrThrow(id));
    }

    @Transactional(readOnly = true) 
    public Page<TicketResponse> list(Pageable pagable){
        return
        ticketRepository.findAll(pagable).map(TicketResponse::from);

        // (?page=0&size=20)
        // Page ---content-> tickets for this page
        // totalElements, totalPages, number, size etc
    }

    @Transactional(readOnly = true)
    public TicketResponse updateStatus(Long id, TicketStatus status) throws Exception{
        Ticket ticket = findOrThrow(id);
        ticket.setStatus(status);
        return 
        TicketResponse.from(ticketRepository.save(ticket));
    }
    
    @Transactional 
    public void delete(Long id) throws Exception{
        Ticket ticket = findOrThrow(id);
        ticketRepository.delete(ticket);
    }

    private Ticket findOrThrow(Long id) throws Exception{
        return 
        ticketRepository.findById(id)
        .orElseThrow(() -> new Exception("Ticket not found" + id));
    }
    
}
