package in.coderkerdos.ticketservice.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component 
public class ClassifierClient {
    private final WebClient classifierWebClient;

    public ClassifierClient(WebClient classifierWebClient){
        this.classifierWebClient = classifierWebClient;
    }

    public TicketClassification classify(String subject, String description){
        return 
       classifierWebClient.post()
       .uri("/api/classify")
       .bodyValue(new ClassifyRequest(subject, description))
       .retrieve()
       .bodyToMono(TicketClassification.class)
       .block();
    }

}
