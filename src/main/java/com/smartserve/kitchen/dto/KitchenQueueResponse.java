package com.smartserve.kitchen.dto;

import java.time.Instant;
import java.util.List;

public record KitchenQueueResponse(
        Instant serverTime,
        KitchenQueueCounts counts,
        List<KitchenTicketResponse> tickets
) {}
