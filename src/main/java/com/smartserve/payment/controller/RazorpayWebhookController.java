package com.smartserve.payment.controller;

import com.smartserve.payment.service.RazorpayWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/razorpay")
@RequiredArgsConstructor
public class RazorpayWebhookController {

    private final RazorpayWebhookService razorpayWebhookService;

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(
            @RequestBody String rawBody,
            @RequestHeader("X-Razorpay-Signature")
            String razorpaySignature
    ) {
        razorpayWebhookService.verifySignature(
                rawBody,
                razorpaySignature
        );

        return ResponseEntity.ok().build();
    }


}
