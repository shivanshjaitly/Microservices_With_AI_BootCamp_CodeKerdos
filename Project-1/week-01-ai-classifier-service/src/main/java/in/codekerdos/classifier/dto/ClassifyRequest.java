package in.codekerdos.classifier.dto;

import jakarta.validation.constraints.NotBlank;

public record ClassifyRequest(
    @NotBlank(message = "Subject must not be blank")
    String subject,

    @NotBlank(message = "Description must not be blank")
    String description
) {}
