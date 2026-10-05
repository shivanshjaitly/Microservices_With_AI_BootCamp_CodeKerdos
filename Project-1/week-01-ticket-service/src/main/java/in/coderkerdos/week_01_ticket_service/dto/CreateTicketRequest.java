package in.coderkerdos.week_01_ticket_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTicketRequest(
    @NotBlank(message = "Subject must not be blank") 
    @Size(max = 200, message = "Subject must not exceed 200 characters")
    String subject,

    @NotBlank(message = "Description must not be blank")
    @Size(max = 4000, message = "Description must not exceed 4000 characters")  
    String description,

     @NotBlank(message = "requesterEmail must not be blank")
    @Email(message = "requesterEmail must be a vaild email address")  
    String requesterEmail
)
{}
