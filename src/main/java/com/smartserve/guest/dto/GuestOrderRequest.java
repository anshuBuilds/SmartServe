package com.smartserve.guest.dto;

import com.smartserve.order.dto.CreateOrderItemRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class GuestOrderRequest {
    @NotBlank @Size(max = 100) private String customerName;
    @Size(max = 20) private String customerPhone;
    @NotNull private Boolean smsConsent;
    @Size(max = 500) private String specialInstructions;
    @Valid @NotEmpty private List<CreateOrderItemRequest> items;
}
