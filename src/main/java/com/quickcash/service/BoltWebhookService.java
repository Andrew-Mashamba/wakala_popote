package com.quickcash.service;

import com.quickcash.domain.CashRequest;
import com.quickcash.repository.CashRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Process Bolt webhooks: job.accepted, job.completed, job.cancelled (PROJECT_BOLT.md §7.1).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BoltWebhookService {

    private final CashRequestRepository cashRequestRepository;
    private final SettlementService settlementService;

    @Transactional
    public void processWebhook(Map<String, Object> payload) {
        String event = getString(payload, "event");
        String jobId = getString(payload, "job_id");
        String referenceId = getString(payload, "reference_id");
        String status = getString(payload, "status");
        log.info("Bolt webhook: event={}, job_id={}, reference_id={}, status={}", event, jobId, referenceId, status);

        CashRequest request = findRequest(jobId, referenceId);
        if (request == null) {
            log.warn("Bolt webhook: no cash request found for job_id={}, reference_id={}", jobId, referenceId);
            return;
        }

        switch (event != null ? event : status != null ? status : "") {
            case "job.completed":
            case "completed":
                onJobCompleted(request, jobId, payload);
                break;
            case "job.cancelled":
            case "cancelled":
                onJobCancelled(request);
                break;
            case "job.accepted":
            case "accepted":
                onJobAccepted(request);
                break;
            default:
                log.debug("Bolt webhook: unhandled event/status {}", event != null ? event : status);
        }
    }

    private void onJobCompleted(CashRequest request, String boltJobId, Map<String, Object> payload) {
        if (request.getStatus() == CashRequest.CashRequestStatus.DELIVERED
                || request.getStatus() == CashRequest.CashRequestStatus.SETTLED) {
            log.debug("Bolt job already marked delivered: requestId={}", request.getId());
            return;
        }
        request.setStatus(CashRequest.CashRequestStatus.DELIVERED);
        request.setDeliveredAt(Instant.now());
        request.setCompletedAt(Instant.now());
        cashRequestRepository.save(request);
        BigDecimal boltPayout = request.getTransportFee() != null ? request.getTransportFee() : BigDecimal.ZERO;
        if (request.getAgentFee() != null) boltPayout = boltPayout.add(request.getAgentFee());
        settlementService.createOnBoltDelivery(request, boltJobId, boltPayout);
        request.setStatus(CashRequest.CashRequestStatus.SETTLED);
        request.setSettledAt(Instant.now());
        cashRequestRepository.save(request);
        log.info("Bolt delivery completed: requestId={}, boltJobId={}, boltPayout={}", request.getId(), boltJobId, boltPayout);
    }

    private void onJobCancelled(CashRequest request) {
        if (request.getStatus() == CashRequest.CashRequestStatus.SEARCHING_AGENT
                || request.getStatus() == CashRequest.CashRequestStatus.CANCELLED) {
            request.setStatus(CashRequest.CashRequestStatus.CANCELLED);
            cashRequestRepository.save(request);
            log.info("Bolt job cancelled: requestId={}", request.getId());
        }
    }

    private void onJobAccepted(CashRequest request) {
        if (request.getStatus() == CashRequest.CashRequestStatus.SEARCHING_AGENT) {
            request.setStatus(CashRequest.CashRequestStatus.AGENT_ASSIGNED);
            request.setAgentAssignedAt(Instant.now());
            cashRequestRepository.save(request);
            log.info("Bolt driver assigned: requestId={}", request.getId());
        }
    }

    private CashRequest findRequest(String jobId, String referenceId) {
        if (jobId != null && !jobId.isBlank()) {
            return cashRequestRepository.findByBoltJobId(jobId).orElse(null);
        }
        if (referenceId != null && referenceId.startsWith("qc_req_")) {
            String uuidStr = referenceId.substring("qc_req_".length());
            try {
                UUID requestId = UUID.fromString(uuidStr);
                return cashRequestRepository.findById(requestId).orElse(null);
            } catch (Exception e) {
                log.debug("Invalid reference_id uuid: {}", uuidStr);
            }
        }
        return null;
    }

    private static String getString(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v != null ? v.toString() : null;
    }
}
