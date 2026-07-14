package com.mcwendyqueen.service.eligibilityClient;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import com.mcwendyqueen.model.order.OrderRequestDTO;
import com.mcwendyqueen.service.eligibilityClient.OrderPolicyEligibilityClient.EligibilityStatus;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@Component
public class ResiliantOrderPolicyEligibilityClient {

    private static final int MAX_FAILURE_COUNT = 3;
    private static final int MAX_RETRY_COUNT = 2;
    private static final int BASE_RETRY_AMOUNT = 1;

    private final OrderPolicyEligibilityClient delegate;

    private int failedCount = 0;

    private final Map<OrderRequestDTO, Integer> pendingRequests = new LinkedHashMap<>();

    public ResiliantOrderPolicyEligibilityClient(OrderPolicyEligibilityClient delegate) {
        this.delegate = delegate;
    }

    public EligibilityStatus checkEligibility(OrderRequestDTO request) {
        if (failedCount >= MAX_FAILURE_COUNT) {
            return EligibilityStatus.MANUAL_REVIEW;
        }

        EligibilityStatus status = delegate.checkEligibility(request);

        // If we are under the max number of failures, approve the request and reset the count.
        if (status == EligibilityStatus.APPROVED && failedCount < MAX_FAILURE_COUNT) {
            failedCount = 0;
            pendingRequests.remove(request);

            return EligibilityStatus.APPROVED;
        }
        else if (status == EligibilityStatus.DENIED && ++failedCount < MAX_FAILURE_COUNT) {
            Integer requestCount = pendingRequests.get(request);

            // TODO havent handled in flight requests
            if (requestCount == null) {
                pendingRequests.put(request, BASE_RETRY_AMOUNT);

                try {
                    EligibilityStatus retryStatus = retry(request, BASE_RETRY_AMOUNT);

                    if (retryStatus == EligibilityStatus.APPROVED) {
                        failedCount = 0;
                        pendingRequests.remove(request);

                        return EligibilityStatus.APPROVED;
                    }

                    return retryStatus;
                }
                catch (Exception e) {
                    log.error("Error during retry for request {}", request);
                }
            }
            else {
                if (requestCount > MAX_RETRY_COUNT) {
                    // TODO we could have an additional log here for number of retries
                    return EligibilityStatus.MANUAL_REVIEW;
                }

                pendingRequests.put(request, requestCount + 1);

                try {
                    EligibilityStatus retryStatus = retry(request, requestCount * 2);

                    if (retryStatus == EligibilityStatus.APPROVED) {
                        failedCount = 0;
                        pendingRequests.remove(request);

                        return EligibilityStatus.APPROVED;
                    }

                    return retryStatus;
                }
                catch (Exception e) {
                    log.error("Error during retry for request {}", request);
                }
            }
        }
        else {
            return EligibilityStatus.MANUAL_REVIEW;
        }

        return EligibilityStatus.APPROVED;
    }

    public EligibilityStatus retry(OrderRequestDTO request, int backoffMillis) throws InterruptedException {
        /*
         * In production, this is where I would use a bounded executor, real timeouts,
         * cancellation, metrics, and probably Resilience4j. For a timed interview,
         * a simple synchronous retry loop is easier to reason about and test.
         */
        for (int attempt = 1; attempt <= MAX_RETRY_COUNT; attempt++) {
            Thread.sleep((long) backoffMillis * attempt);

            EligibilityStatus status = delegate.checkEligibility(request);

            if (status == EligibilityStatus.APPROVED) {
                return status;
            }
        }

        return EligibilityStatus.MANUAL_REVIEW;
    }

    public void fallback(OrderRequestDTO request) throws HttpClientErrorException {
        delegate.fallback(request);

        throw new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE,
                "Service Unavailable for request: " + request);
    }
}
