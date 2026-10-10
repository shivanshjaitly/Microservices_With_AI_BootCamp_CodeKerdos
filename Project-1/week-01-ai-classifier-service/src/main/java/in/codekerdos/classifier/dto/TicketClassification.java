package in.codekerdos.classifier.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

public record TicketClassification(
    @JsonPropertyDescription("The Customer's underlying issue category.One of: BILLING, TECHNICAL_ISSUE, ACCOUNT_ACCESS, FEATURE_REQUEST,GENERAL_INQUIRY")
    String intent,
    @JsonPropertyDescription("Overall emotional tone of the ticket text. One of: POSITIVE, NEUTRAL, NEGATIVE")
    String sentiment,
    @JsonPropertyDescription("How urgently thos ticket needs to be handled. One of: LOW, MEDIUM, HIGH, URGENT")
    String priority,
    @JsonPropertyDescription("The support team best suited to own this ticket. One of: BILLING_TEAM, TECHNICAL_SUPPORT, ACCOUNT_MANAGEMENT, PRODUCT_TEAM, GENERAL_SUPPORT")
    String suggestedTeam
) {}
