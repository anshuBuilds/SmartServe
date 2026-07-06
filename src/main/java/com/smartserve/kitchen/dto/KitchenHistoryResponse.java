package com.smartserve.kitchen.dto;

import java.time.Instant;
import java.util.List;

public record KitchenHistoryResponse(
        Instant serverTime,
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<KitchenTicketResponse> tickets
) {}
