package com.smartserve.guest.dto;

import com.smartserve.order.dto.OrderResponse;

public record GuestOrderCreatedResponse(String trackingToken, OrderResponse order) {}
