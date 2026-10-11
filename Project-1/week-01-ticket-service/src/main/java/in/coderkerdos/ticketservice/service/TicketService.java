package in.coderkerdos.ticketservice.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import in.coderkerdos.ticketservice.client.ClassifierClient;
import in.coderkerdos.ticketservice.client.TicketClassification;
import in.coderkerdos.ticketservice.dto.CreateTicketRequest;
import in.coderkerdos.ticketservice.dto.TicketResponse;
import in.coderkerdos.ticketservice.entity.Ticket;
import in.coderkerdos.ticketservice.entity.TicketStatus;
import in.coderkerdos.ticketservice.repository.TicketRepository;

@Service 
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ClassifierClient classifierClient;
    
    public TicketService(TicketRepository ticketRepository, ClassifierClient classifierClient){
        this.ticketRepository = ticketRepository;
        this.classifierClient = classifierClient;
    }

    @Transactional 
    public TicketResponse create(CreateTicketRequest request){

        TicketClassification classification = classifierClient.
        classify(request.subject(), request.description());
        Ticket ticket = new Ticket();
        ticket.setSubject(request.subject());
        ticket.setDescription(request.description());
        ticket.setRequesterEmail(request.requesterEmail());
        ticket.setStatus(TicketStatus.OPEN);

        ticket.setIntent(classification.intent());
        ticket.setSentiment(classification.sentiment());
        ticket.setPriority(classification.priority());
        ticket.setSuggestedTeam(classification.suggestedTeam());
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
