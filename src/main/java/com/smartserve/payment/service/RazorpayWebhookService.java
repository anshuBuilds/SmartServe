package com.smartserve.payment.service;


import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.smartserve.common.exception.BadRequestException;
import com.smartserve.payment.config.RazorpayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

    private final RazorpayProperties razorpayProperties;

    public void verifySignature(String rawBody, String razorPaySignature) {
        String webhookSecret = razorpayProperties.webhookSecret();

        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalStateException("Razorpay webhook secret is not configured");
        }

        try {
            Utils.verifyWebhookSignature(
                    rawBody,
                    razorPaySignature,
                    webhookSecret
            );
        } catch (RazorpayException exception) {
            throw new BadRequestException("Invalid Razorpay webhook signature");
        }
    }
}
