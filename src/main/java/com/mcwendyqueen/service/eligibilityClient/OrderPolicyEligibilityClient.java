package com.mcwendyqueen.service.eligibilityClient;

import java.util.Random;

import lombok.extern.slf4j.Slf4j;

import com.mcwendyqueen.model.order.OrderRequestDTO;

import org.springframework.stereotype.Component;

// Base class represents the 'dependency' and will return based on its internal conditions whether
// or not an order can be created
@Component
@Slf4j
public class OrderPolicyEligibilityClient {

    public enum EligibilityStatus {
        APPROVED,
        DENIED,
        MANUAL_REVIEW
    }

    // Base case approves all
    public EligibilityStatus checkEligibility(OrderRequestDTO request) {
        double rand = new Random().nextDouble();

        if (rand <= .5) {
            return EligibilityStatus.APPROVED;
        }
        else {
            return EligibilityStatus.DENIED;
        }
    }

    public void fallback(OrderRequestDTO request) {
        log.debug("fallback for request: {}", request);
    }
}
