package com.quickcash.service;

import com.quickcash.domain.*;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.notification.FcmNotificationService;
import com.quickcash.repository.AgentAssignmentRepository;
import com.quickcash.repository.AgentRepository;
import com.quickcash.repository.CashRequestRepository;
import com.quickcash.selcom.SelcomApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentService {

    private final AgentRepository agentRepository;
    private final AgentAssignmentRepository agentAssignmentRepository;
    private final UserService userService;
    private final CashRequestRepository cashRequestRepository;
    private final CashRequestService cashRequestService;
    private final SettlementService settlementService;
    private final SelcomApiClient selcomApiClient;
    private final FcmNotificationService fcmNotificationService;

    @Transactional
    public Agent register(UUID userId, String selcomAccountId, String selcomAccountName) {
        User user = userService.getById(userId.toString());
        if (agentRepository.findByUserId(userId).isPresent()) {
            throw new IllegalArgumentException("User is already registered as agent");
        }
        var agent = Agent.builder()
                .user(user)
                .selcomAccountId(selcomAccountId)
                .selcomAccountName(selcomAccountName != null ? selcomAccountName : user.getDisplayName())
                .build();
        agent = agentRepository.save(agent);
        log.info("Agent registered: id={}, userId={}, selcomAccountId={}", agent.getId(), userId, selcomAccountId);
        return agent;
    }

    public Agent getByUserId(UUID userId) {
        return agentRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent for user", userId));
    }

    @Transactional
    public Agent setAvailability(UUID userId, boolean available) {
        Agent agent = getByUserId(userId);
        agent.setIsAvailable(available);
        agent = agentRepository.save(agent);
        log.debug("Agent availability updated: agentId={}, available={}", agent.getId(), available);
        return agent;
    }

    @Transactional
    public Agent updateLocation(UUID userId, Double lat, Double lng) {
        Agent agent = getByUserId(userId);
        agent.setCurrentLat(lat);
        agent.setCurrentLng(lng);
        agent.setLastLocationUpdate(Instant.now());
        agent = agentRepository.save(agent);
        return agent;
    }

    /** List pending cash requests (SEARCHING_AGENT) for matching; simple distance filter. */
    public List<CashRequest> listAvailableRequests(UUID agentUserId, int limit) {
        Agent agent = getByUserId(agentUserId);
        if (!Boolean.TRUE.equals(agent.getIsAvailable())) {
            return List.of();
        }
        List<CashRequest> all = cashRequestRepository.findByStatusOrderByCreatedAtDesc(CashRequest.CashRequestStatus.SEARCHING_AGENT, PageRequest.of(0, limit * 2));
        return all.stream()
                .filter(r -> withinRadius(agent.getCurrentLat(), agent.getCurrentLng(), r.getDeliveryLat(), r.getDeliveryLng(), 50.0))
                .limit(limit)
                .toList();
    }

    private static boolean withinRadius(Double lat1, Double lng1, Double lat2, Double lng2, double radiusKm) {
        if (lat1 == null || lng1 == null || lat2 == null || lng2 == null) return true;
        double d = haversineKm(lat1, lng1, lat2, lng2);
        return d <= radiusKm;
    }

    private static double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double p = 0.017453292519943295;
        double a = 0.5 - Math.cos((lat2 - lat1) * p) / 2 +
                Math.cos(lat1 * p) * Math.cos(lat2 * p) * (1 - Math.cos((lon2 - lon1) * p)) / 2;
        return 12742 * Math.asin(Math.sqrt(a));
    }

    @Transactional
    public AgentAssignment accept(UUID agentUserId, UUID requestId, Double lat, Double lng) {
        Agent agent = getByUserId(agentUserId);
        CashRequest request = cashRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("CashRequest", requestId));
        if (request.getStatus() != CashRequest.CashRequestStatus.SEARCHING_AGENT) {
            throw new IllegalStateException("Request is not available for acceptance: " + request.getStatus());
        }
        AgentAssignment assignment = agentAssignmentRepository.findByRequestAndAgentAndStatus(request, agent, AgentAssignment.AssignmentStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("AgentAssignment (PENDING) for request", requestId));
        if (agentAssignmentRepository.findByRequestAndStatus(request, AgentAssignment.AssignmentStatus.ACCEPTED).isPresent()) {
            throw new IllegalStateException("Request already accepted by another agent");
        }
        assignment.setStatus(AgentAssignment.AssignmentStatus.ACCEPTED);
        assignment.setAcceptedAt(Instant.now());
        assignment.setAgentLatAtAccept(lat);
        assignment.setAgentLngAtAccept(lng);
        assignment.setSettlementStatus(AgentAssignment.SettlementStatus.PENDING);
        agentAssignmentRepository.save(assignment);
        for (AgentAssignment other : agentAssignmentRepository.findByRequestAndStatusIn(request, List.of(AgentAssignment.AssignmentStatus.PENDING))) {
            if (!other.getId().equals(assignment.getId())) {
                other.setStatus(AgentAssignment.AssignmentStatus.CANCELLED);
                agentAssignmentRepository.save(other);
            }
        }
        request.setStatus(CashRequest.CashRequestStatus.AGENT_ASSIGNED);
        request.setAgentUserId(agent.getUser().getId());
        request.setAgentAssignedAt(Instant.now());
        cashRequestRepository.save(request);
        notifyClientOfStatus(request, "agent_assigned", "Agent assigned", "An agent is assigned to your cash delivery.");
        log.info("Agent accepted request: assignmentId={}, requestId={}, agentId={}", assignment.getId(), requestId, agent.getId());
        return assignment;
    }

    @Transactional
    public void reject(UUID agentUserId, UUID requestId, String reason) {
        Agent agent = getByUserId(agentUserId);
        CashRequest request = cashRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("CashRequest", requestId));
        var assignment = agentAssignmentRepository.findByRequestAndStatus(request, AgentAssignment.AssignmentStatus.PENDING)
                .orElseThrow(() -> new ResourceNotFoundException("AgentAssignment for request", requestId));
        if (!assignment.getAgent().getId().equals(agent.getId())) {
            throw new IllegalArgumentException("Assignment does not belong to this agent");
        }
        assignment.setStatus(AgentAssignment.AssignmentStatus.REJECTED);
        assignment.setRejectedAt(Instant.now());
        assignment.setRejectionReason(reason);
        agentAssignmentRepository.save(assignment);
        log.info("Agent rejected request: assignmentId={}, requestId={}, reason={}", assignment.getId(), requestId, reason);
    }

    @Transactional
    public void enRoute(UUID agentUserId, UUID requestId) {
        CashRequest request = cashRequestRepository.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("CashRequest", requestId));
        updateAssignmentStatus(agentUserId, requestId, AgentAssignment.AssignmentStatus.EN_ROUTE, CashRequest.CashRequestStatus.AGENT_EN_ROUTE);
        notifyClientOfStatus(request, "agent_en_route", "Agent on the way", "Your agent is on the way to you.");
    }

    @Transactional
    public void arrived(UUID agentUserId, UUID requestId) {
        CashRequest request = cashRequestRepository.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("CashRequest", requestId));
        updateAssignmentStatus(agentUserId, requestId, AgentAssignment.AssignmentStatus.ARRIVED, null);
        notifyClientOfStatus(request, "agent_arrived", "Agent arrived", "Your agent has arrived at the delivery location.");
    }

    @Transactional
    public void deliver(UUID agentUserId, UUID requestId) {
        deliver(agentUserId, requestId, null);
    }

    @Transactional
    public void deliver(UUID agentUserId, UUID requestId, String recipientOtp) {
        Agent agent = getByUserId(agentUserId);
        CashRequest request = cashRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("CashRequest", requestId));
        var assignment = agentAssignmentRepository.findByRequestAndStatus(request, AgentAssignment.AssignmentStatus.ARRIVED)
                .or(() -> agentAssignmentRepository.findByRequestAndStatus(request, AgentAssignment.AssignmentStatus.EN_ROUTE))
                .orElseThrow(() -> new ResourceNotFoundException("AgentAssignment for request", requestId));
        if (!assignment.getAgent().getId().equals(agent.getId())) {
            throw new IllegalArgumentException("Assignment does not belong to this agent");
        }
        if (request.getRequestType() == CashRequest.RequestType.REMOTE_SEND && !Boolean.TRUE.equals(request.getRecipientOtpVerified())) {
            if (recipientOtp == null || recipientOtp.isBlank()) {
                throw new IllegalArgumentException("Recipient OTP required for remote send delivery");
            }
            if (!cashRequestService.verifyRecipientOtp(requestId, recipientOtp)) {
                throw new IllegalArgumentException("Invalid or expired OTP");
            }
            assignment.setRecipientOtpEntered(recipientOtp);
            agentAssignmentRepository.save(assignment);
        }
        assignment.setStatus(AgentAssignment.AssignmentStatus.DELIVERED);
        assignment.setDeliveryConfirmedAt(Instant.now());
        assignment.setSettlementAmount(request.getTotalAgentPayment());
        agentAssignmentRepository.save(assignment);
        request.setStatus(CashRequest.CashRequestStatus.DELIVERED);
        request.setDeliveredAt(Instant.now());
        request.setCompletedAt(Instant.now());
        cashRequestRepository.save(request);

        BigDecimal agentAmount = request.getTotalAgentPayment() != null ? request.getTotalAgentPayment() : request.getRequestedAmount();
        var settlement = settlementService.createOnDelivery(
                request, assignment,
                request.getClientBankCode(), request.getClientAccountNumber(), request.getTotalClientCharge(),
                agent.getSelcomAccountId(), agentAmount);
        assignment.setSettlementStatus(AgentAssignment.SettlementStatus.PENDING);
        agentAssignmentRepository.save(assignment);

        var creditResult = selcomApiClient.creditAgent(agent.getSelcomAccountId(), agentAmount, "QC-" + requestId);
        if (creditResult.isSuccess()) {
            settlementService.markAgentCredited(settlement.getId(), creditResult.getTransactionId());
            assignment.setSettlementStatus(AgentAssignment.SettlementStatus.CREDITED);
            assignment.setSettlementReference(creditResult.getTransactionId());
            assignment.setSettledAt(Instant.now());
            request.setStatus(CashRequest.CashRequestStatus.SETTLED);
            request.setSettledAt(Instant.now());
        } else {
            settlementService.markAgentCreditFailed(settlement.getId(), creditResult.getError());
            assignment.setSettlementStatus(AgentAssignment.SettlementStatus.FAILED);
        }
        agentAssignmentRepository.save(assignment);
        cashRequestRepository.save(request);

        agent.setTotalDeliveries((agent.getTotalDeliveries() != null ? agent.getTotalDeliveries() : 0) + 1);
        agent.setTotalEarnings(agent.getTotalEarnings() != null ? agent.getTotalEarnings().add(agentAmount) : agentAmount);
        agentRepository.save(agent);
        log.info("Delivery confirmed: requestId={}, agentId={}, settlementId={}, creditSuccess={}", requestId, agent.getId(), settlement.getId(), creditResult.isSuccess());
    }

    private void updateAssignmentStatus(UUID agentUserId, UUID requestId, AgentAssignment.AssignmentStatus assignmentStatus, CashRequest.CashRequestStatus requestStatus) {
        Agent agent = getByUserId(agentUserId);
        CashRequest request = cashRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("CashRequest", requestId));
        var list = agentAssignmentRepository.findByRequestOrderByAssignedAtDesc(request);
        AgentAssignment assignment = list.stream().filter(a -> a.getAgent().getId().equals(agent.getId())).findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("AgentAssignment for request", requestId));
        assignment.setStatus(assignmentStatus);
        agentAssignmentRepository.save(assignment);
        if (requestStatus != null) {
            request.setStatus(requestStatus);
            cashRequestRepository.save(request);
        }
        log.debug("Assignment status updated: requestId={}, status={}", requestId, assignmentStatus);
    }

    public List<AgentAssignment> listAssignments(UUID agentUserId, int limit) {
        Agent agent = getByUserId(agentUserId);
        return agentAssignmentRepository.findByAgentOrderByAssignedAtDesc(agent, PageRequest.of(0, limit));
    }

    /** Notify client (request owner) via FCM of status update (Phase 3: accept, en-route, arrived). */
    private void notifyClientOfStatus(CashRequest request, String statusKey, String title, String body) {
        User client = request.getUser();
        if (client == null || client.getFcmToken() == null || client.getFcmToken().isBlank()) {
            log.debug("FCM to client skipped: no token for requestId={}", request.getId());
            return;
        }
        Map<String, String> data = Map.of("type", "cash_request_status", "requestId", request.getId().toString(), "status", statusKey);
        fcmNotificationService.sendToToken(client.getId(), request.getId(), client.getFcmToken(), title, body, data);
        log.info("Notified client of status: requestId={}, status={}", request.getId(), statusKey);
    }

    /** Earnings history: delivered assignments with amount and date (Phase 3 §5.5). */
    public List<EarningEntry> listEarningsHistory(UUID agentUserId, int limit) {
        Agent agent = getByUserId(agentUserId);
        List<AgentAssignment> assignments = agentAssignmentRepository.findByAgentOrderByAssignedAtDesc(agent, PageRequest.of(0, limit * 2));
        List<EarningEntry> out = new ArrayList<>();
        for (AgentAssignment a : assignments) {
            if (a.getStatus() != AgentAssignment.AssignmentStatus.DELIVERED) continue;
            if (out.size() >= limit) break;
            out.add(EarningEntry.builder()
                    .requestId(a.getRequest().getId())
                    .assignmentId(a.getId())
                    .amount(a.getSettlementAmount() != null ? a.getSettlementAmount() : BigDecimal.ZERO)
                    .completedAt(a.getDeliveryConfirmedAt())
                    .build());
        }
        return out;
    }

    @lombok.Data
    @lombok.Builder
    public static class EarningEntry {
        private UUID requestId;
        private UUID assignmentId;
        private BigDecimal amount;
        private Instant completedAt;
    }
}
