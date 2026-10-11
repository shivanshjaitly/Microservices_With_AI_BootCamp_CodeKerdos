package in.codekerdos.classifier.service;

import org.springframework.stereotype.Service;

import in.codekerdos.classifier.dto.ClassifyRequest;
import in.codekerdos.classifier.dto.TicketClassification;

import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;

@Service 
public class TicketClassifierService {
    
    private static final PromptTemplate CLASSIFY_TEMPLATE = new PromptTemplate(
        """
                You are a support ticket triage assistant for a Saas company. 
                Read the ticket below and classify it honestly, using only the text
                given.

                Subject: {subject}
                Description: {description}

                Rules:
                - intent must be one of: BILLING, TECHNICAL_ISSUE, ACCOUNT_ACCESS, FEATURE_REQUEST,GENERAL_INQUIRY
                - sentiment must ne one of: POSITVE, NEUTRAL, NEGATIVE
                - priority must ne one of: LOW, MEDIUM, HIGH, URGENT
                - suggestedTeam must ne one of: The support team best suited to own this ticket. One of: BILLING_TEAM, TECHNICAL_SUPPORT, ACCOUNT_MANAGEMENT, PRODUCT_TEAM, GENERAL_SUPPORT
                """


    );

    private final ChatClient chatClient;

    public TicketClassifierService(ChatClient.Builder chatClientBuilder){
        this.chatClient = chatClientBuilder.build();
    }

    public TicketClassification classify(ClassifyRequest request){
        Prompt prompt = CLASSIFY_TEMPLATE.create(Map.of(
            "subject", request.subject(),
            "description", request.description()
        ));
        return 
        chatClient.prompt(prompt).call().entity(TicketClassification.class);

    }
}
