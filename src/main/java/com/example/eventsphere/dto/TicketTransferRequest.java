package com.example.eventsphere.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketTransferRequest {

    @NotBlank(message = "The new attendee's name is required")
    private String newName;

    @Email(message = "Must be a valid email address")
    @NotBlank(message = "The new attendee's email is required")
    private String newEmail;

    @NotBlank(message = "The new attendee's cnic is required")
    @Size(min=13,max = 13, message = "CNIC must be 13 characters")
    private String newCnic;

    @NotBlank(message = "The new attendee's phone is required")
    @Size(min=11,max = 11, message="Phone Number should be 11")
    private String newPhone;

}