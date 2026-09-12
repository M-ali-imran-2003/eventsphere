package com.example.eventsphere.dto;

import lombok.Data;

@Data
public class ScanTicketRequest {
    String qrCodeHash;
    Double latitude;
    Double longitude;
}
