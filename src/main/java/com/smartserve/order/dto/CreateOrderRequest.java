package com.smartserve.order.dto;

import com.smartserve.order.enums.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateOrderRequest {

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private Long tableId;

    @NotBlank(message = "Customer name is required")
    @Size(max = 100, message = "Customer name must be at most 100 characters")
    private String customerName;

    @NotNull(message = "Order type is required")
    private OrderType orderType;

    @Size(max = 20, message = "Customer phone must be at most 20 characters")
    private String customerPhone;

    @NotNull(message = "SMS consent choice is required")
    private Boolean smsConsent;

    @Size(max = 500, message = "Special instructions must be at most 500 characters")
    private String specialInstructions;

    @Valid
    @NotEmpty(message = "Order must contain at least one item")
    private List<CreateOrderItemRequest> items;
}
