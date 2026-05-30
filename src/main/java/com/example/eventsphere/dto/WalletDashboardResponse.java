package com.example.eventsphere.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class WalletDashboardResponse {
    private BigDecimal availableBalance;
    private List<WalletTransactionDto> recentTransactions;
}