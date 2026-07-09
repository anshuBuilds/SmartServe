package com.smartserve.guest.dto;

import com.smartserve.restaurant.enums.TableStatus;

public record GuestSessionResponse(Long branchId, String branchName, String restaurantName,
                                   Long tableId, String tableNumber, TableStatus tableStatus,
                                   boolean orderingEnabled) {}
