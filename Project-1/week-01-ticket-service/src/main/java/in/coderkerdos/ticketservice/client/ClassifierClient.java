package in.coderkerdos.ticketservice.client;

import java.time.Duration;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

@Component 
public class ClassifierClient {
    private final WebClient classifierWebClient;
    private final Duration callTimeout;

    private static final Logger log = LoggerFactory.getLogger(ClassifierClient.class);

   public ClassifierClient(
            WebClient classifierWebClient,
            @Value("${app.classifier-service.timeout-ms:4000}") long timeoutMs) {
        this.classifierWebClient = classifierWebClient;
        this.callTimeout = Duration.ofMillis(timeoutMs);
    }
    
    @CircuitBreaker (name = "classifier")
    @Retry(name = "classifier", fallbackMethod = "classifyFallBack")
    public TicketClassification classify(String subject, String description){
        return 
       classifierWebClient.post()
       .uri("/api/classify")
       .bodyValue(new ClassifyRequest(subject, description))
       .retrieve()
       .bodyToMono(TicketClassification.class)
       .block();
    }

    @SuppressWarnings ("unused")
    private TicketClassification classifyFallBack(String subject, String description,
        Throwable throwable){
            log.warn("AI classifier service is unavialable. degrading to default Reason={} ", throwable.toString());

            return TicketClassification.unclassified();

        }
    
}
