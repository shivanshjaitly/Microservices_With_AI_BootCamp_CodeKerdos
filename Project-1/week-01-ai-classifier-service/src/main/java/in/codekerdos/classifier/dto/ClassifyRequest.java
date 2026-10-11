package in.codekerdos.classifier.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;

public record ClassifyRequest(
    @NotBlank(message = "Subject must not be blank")
    @JsonProperty("subject")
    String subject,

    @NotBlank(message = "Description must not be blank")
    @JsonProperty("description")
    String description
) {}
