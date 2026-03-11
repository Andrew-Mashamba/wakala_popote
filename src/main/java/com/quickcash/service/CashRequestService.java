package com.quickcash.service;

import com.quickcash.domain.Agent;
import com.quickcash.domain.CashRequest;
import com.quickcash.domain.PaymentMethod;
import com.quickcash.domain.User;
import com.quickcash.dto.CashRequestCreateV1;
import com.quickcash.dto.RequestCashRequest;
import com.quickcash.dto.SendCashRequest;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.notification.FcmNotificationService;
import com.quickcash.domain.AgentAssignment;
import com.quickcash.repository.AgentAssignmentRepository;
import com.quickcash.repository.AgentRepository;
import com.quickcash.bolt.BoltApiClient;
import com.quickcash.repository.CashRequestRepository;
import com.quickcash.selcom.SelcomApiClient;
import com.quickcash.sms.SmsOtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import lombok.Builder;
import lombok.Data;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashRequestService {

    private final CashRequestRepository cashRequestRepository;
    private final UserService userService;
    private final FeeCalculationService feeCalculationService;
    private final PaymentMethodService paymentMethodService;
    private final SelcomApiClient selcomApiClient;
    private final AgentRepository agentRepository;
    private final AgentAssignmentRepository agentAssignmentRepository;
    private final FcmNotificationService fcmNotificationService;
    private final BoltApiClient boltApiClient;
    private final SmsOtpService smsOtpService;
    private final AuditLogService auditLogService;

    private static final org.slf4j.Logger remotesendLog = LoggerFactory.getLogger("com.quickcash.remotesend");
    private static final int OTP_EXPIRY_SECONDS = 600; // 10 min

    /** Legacy endpoint: no fees, no payment method. */
    @Transactional
    public CashRequest createRequest(RequestCashRequest req) {
        User user = userService.getById(req.getUserId());
        CashRequest request = CashRequest.builder()
                .user(user)
                .requestedAmount(req.getRequestedAmount())
                .principalAmount(req.getRequestedAmount())
                .userLatitude(req.getUserLatitude())
                .userLongitude(req.getUserLongitude())
                .deliveryLat(req.getUserLatitude())
                .deliveryLng(req.getUserLongitude())
                .clientName(req.getName())
                .clientImageUrl(req.getImage())
                .status(CashRequest.CashRequestStatus.PENDING)
                .build();
        request = cashRequestRepository.save(request);
        log.info("Cash request created (legacy): id={}, userId={}, amount={}", request.getId(), req.getUserId(), req.getRequestedAmount());
        return request;
    }

    /** V1 API: calculate fees, set payment method, status PENDING_VERIFICATION (no Selcom yet). */
    @Transactional
    public CashRequest createRequestV1(UUID userId, CashRequestCreateV1 req) {
        User user = userService.getById(userId.toString());
        var fees = feeCalculationService.calculate(req.getAmount());
        CashRequest request = CashRequest.builder()
                .user(user)
                .requestType(req.getRequestType())
                .clientPaymentMethodId(req.getPaymentMethodId())
                .requestedAmount(req.getAmount())
                .principalAmount(fees.getPrincipalAmount())
                .serviceFee(fees.getServiceFee())
                .transportFee(fees.getTransportFee())
                .agentFee(fees.getAgentFee())
                .totalClientCharge(fees.getTotalClientCharge())
                .totalAgentPayment(fees.getTotalAgentPayment())
                .userLatitude(req.getLatitude())
                .userLongitude(req.getLongitude())
                .deliveryLat(req.getLatitude())
                .deliveryLng(req.getLongitude())
                .deliveryAddress(req.getDeliveryAddress())
                .clientName(user.getDisplayName())
                .clientImageUrl(user.getPhotoUrl())
                .status(CashRequest.CashRequestStatus.PENDING_VERIFICATION)
                .build();
        request = cashRequestRepository.save(request);
        log.info("Cash request created (v1): id={}, userId={}, amount={}, totalCharge={}", request.getId(), userId, req.getAmount(), fees.getTotalClientCharge());
        auditLogService.log("CASH_REQUEST_CREATED", "CashRequest", request.getId(), userId, "USER", "amount=" + req.getAmount());

        if (req.getPaymentMethodId() != null) {
            PaymentMethod pm = paymentMethodService.getByIdAndUser(req.getPaymentMethodId(), userId);
            if (pm.getMethodType() == PaymentMethod.MethodType.BANK_ACCOUNT && pm.getBankCode() != null && pm.getAccountNumber() != null) {
                request.setClientBankCode(pm.getBankCode());
                request.setClientAccountNumber(pm.getAccountNumber());
                request.setClientAccountName(pm.getAccountName());
                var verifyResult = selcomApiClient.verifyAccount(pm.getBankCode(), pm.getAccountNumber(), pm.getAccountName(), request.getTotalClientCharge());
                request.setSelcomRequestId(verifyResult.getRequestId());
                request.setSelcomVerificationStatus(verifyResult.isVerified() ? CashRequest.SelcomVerificationStatus.VERIFIED : CashRequest.SelcomVerificationStatus.FAILED);
                if (verifyResult.isVerified()) {
                    request.setVerifiedAt(Instant.now());
                    request.setStatus(CashRequest.CashRequestStatus.VERIFIED);
                    if (Boolean.TRUE.equals(req.getCollectNow())) {
                        var collectResult = selcomApiClient.collectPayment(
                                verifyResult.getRequestId(), pm.getBankCode(), pm.getAccountNumber(), pm.getAccountName(),
                                request.getTotalClientCharge(), "QC-" + request.getId());
                        if (collectResult.isSuccess()) {
                            request.setStatus(CashRequest.CashRequestStatus.SEARCHING_AGENT);
                            request = cashRequestRepository.save(request);
                            dispatchAfterPayment(request);
                            log.info("Cash request verified and collected: id={}, moving to SEARCHING_AGENT", request.getId());
                        } else {
                            log.warn("Selcom collect failed for requestId={}: {}", request.getId(), collectResult.getError());
                        }
                    }
                } else {
                    log.warn("Selcom verify failed for requestId={}: {}", request.getId(), verifyResult.getError());
                }
            }
            request = cashRequestRepository.save(request);
        }
        return request;
    }

    /** Remote send: create request with recipient info and OTP; SMS stub. Logs to remotesend.log. */
    @Transactional
    public CashRequest createSendRequest(UUID userId, SendCashRequest req) {
        User user = userService.getById(userId.toString());
        var fees = feeCalculationService.calculate(req.getAmount());
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        Instant otpExpires = Instant.now().plusSeconds(OTP_EXPIRY_SECONDS);

        CashRequest request = CashRequest.builder()
                .user(user)
                .requestType(CashRequest.RequestType.REMOTE_SEND)
                .clientPaymentMethodId(req.getPaymentMethodId())
                .requestedAmount(req.getAmount())
                .principalAmount(fees.getPrincipalAmount())
                .serviceFee(fees.getServiceFee())
                .transportFee(fees.getTransportFee())
                .agentFee(fees.getAgentFee())
                .totalClientCharge(fees.getTotalClientCharge())
                .totalAgentPayment(fees.getTotalAgentPayment())
                .userLatitude(req.getDeliveryLatitude())
                .userLongitude(req.getDeliveryLongitude())
                .deliveryLat(req.getDeliveryLatitude())
                .deliveryLng(req.getDeliveryLongitude())
                .deliveryAddress(req.getDeliveryAddress())
                .clientName(user.getDisplayName())
                .clientImageUrl(user.getPhotoUrl())
                .recipientPhone(req.getRecipientPhone())
                .recipientName(req.getRecipientName())
                .recipientLocationLat(req.getDeliveryLatitude())
                .recipientLocationLng(req.getDeliveryLongitude())
                .recipientLocationAddress(req.getDeliveryAddress())
                .recipientOtp(otp)
                .recipientOtpExpiresAt(otpExpires)
                .recipientOtpVerified(false)
                .status(CashRequest.CashRequestStatus.PENDING_VERIFICATION)
                .build();
        request = cashRequestRepository.save(request);
        remotesendLog.info("Remote send created: id={}, senderUserId={}, recipientPhone={}, amount={}, otpExpires={}",
                request.getId(), userId, maskPhone(req.getRecipientPhone()), req.getAmount(), otpExpires);
        smsOtpService.sendOtp(req.getRecipientPhone(), "Your Quick Cash OTP: " + otp + ". Valid 10 minutes.");
        auditLogService.log("REMOTE_SEND_CREATED", "CashRequest", request.getId(), userId, "USER", "recipient=" + maskPhone(req.getRecipientPhone()) + ", amount=" + req.getAmount());

        if (req.getPaymentMethodId() != null) {
            PaymentMethod pm = paymentMethodService.getByIdAndUser(req.getPaymentMethodId(), userId);
            if (pm.getMethodType() == PaymentMethod.MethodType.BANK_ACCOUNT && pm.getBankCode() != null && pm.getAccountNumber() != null) {
                request.setClientBankCode(pm.getBankCode());
                request.setClientAccountNumber(pm.getAccountNumber());
                request.setClientAccountName(pm.getAccountName());
                var verifyResult = selcomApiClient.verifyAccount(pm.getBankCode(), pm.getAccountNumber(), pm.getAccountName(), request.getTotalClientCharge());
                request.setSelcomRequestId(verifyResult.getRequestId());
                request.setSelcomVerificationStatus(verifyResult.isVerified() ? CashRequest.SelcomVerificationStatus.VERIFIED : CashRequest.SelcomVerificationStatus.FAILED);
                if (verifyResult.isVerified()) {
                    request.setVerifiedAt(Instant.now());
                    request.setStatus(CashRequest.CashRequestStatus.VERIFIED);
                    if (Boolean.TRUE.equals(req.getCollectNow())) {
                        var collectResult = selcomApiClient.collectPayment(
                                verifyResult.getRequestId(), pm.getBankCode(), pm.getAccountNumber(), pm.getAccountName(),
                                request.getTotalClientCharge(), "QC-" + request.getId());
                        if (collectResult.isSuccess()) {
                            request.setStatus(CashRequest.CashRequestStatus.SEARCHING_AGENT);
                            request = cashRequestRepository.save(request);
                            dispatchAfterPayment(request);
                            remotesendLog.info("Remote send payment collected: id={}, SEARCHING_AGENT", request.getId());
                        }
                    }
                }
            }
            request = cashRequestRepository.save(request);
        }
        return request;
    }

    /** Verify recipient OTP for remote send; returns true if valid. */
    @Transactional
    public boolean verifyRecipientOtp(UUID requestId, String otp) {
        CashRequest request = cashRequestRepository.findById(requestId).orElseThrow(() -> new ResourceNotFoundException("CashRequest", requestId));
        if (request.getRequestType() != CashRequest.RequestType.REMOTE_SEND) return false;
        if (request.getRecipientOtp() == null || request.getRecipientOtpExpiresAt() == null) return false;
        if (Instant.now().isAfter(request.getRecipientOtpExpiresAt())) {
            remotesendLog.warn("Remote send OTP expired: requestId={}", requestId);
            return false;
        }
        boolean valid = request.getRecipientOtp().equals(otp);
        if (valid) {
            request.setRecipientOtpVerified(true);
            cashRequestRepository.save(request);
            remotesendLog.info("Remote send OTP verified: requestId={}", requestId);
        }
        return valid;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) return "****";
        return "****" + phone.substring(phone.length() - 4);
    }

    /** Notify nearby available agents: create PENDING assignment per agent, then FCM so each can accept/reject. */
    private void notifyAgentsForRequest(CashRequest request) {
        List<Agent> agents = agentRepository.findByIsAvailableTrue();
        String title = "New Quick Cash request";
        String body = String.format("TZS %s requested nearby", request.getPrincipalAmount() != null ? request.getPrincipalAmount().toPlainString() : request.getRequestedAmount().toPlainString());
        Map<String, String> data = Map.of("type", "cash_request", "requestId", request.getId().toString(), "amount", request.getRequestedAmount().toPlainString());
        int sent = 0;
        for (Agent agent : agents) {
            AgentAssignment pending = AgentAssignment.builder()
                    .request(request)
                    .agent(agent)
                    .status(AgentAssignment.AssignmentStatus.PENDING)
                    .assignmentMethod(AgentAssignment.AssignmentMethod.BROADCAST)
                    .build();
            agentAssignmentRepository.save(pending);
            User agentUser = agent.getUser();
            if (agentUser != null && agentUser.getFcmToken() != null && !agentUser.getFcmToken().isBlank()) {
                boolean ok = fcmNotificationService.sendToToken(agentUser.getId(), request.getId(), agentUser.getFcmToken(), title, body, data);
                if (ok) sent++;
            }
        }
        log.info("Created {} PENDING assignments and sent FCM to {} agents for requestId={}", agents.size(), sent, request.getId());
    }

    public CashRequest getById(UUID id) {
        return cashRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CashRequest", id));
    }

    public CashRequest getByIdAndUser(UUID id, UUID userId) {
        CashRequest r = getById(id);
        if (!r.getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("CashRequest", id);
        }
        return r;
    }

    /** Track info for client: status, optional agent location (Agent track), optional Bolt tracking URL (Bolt track). */
    public TrackInfo getTrackInfo(UUID id, UUID userId) {
        CashRequest r = getByIdAndUser(id, userId);
        String trackingUrl = null;
        if (r.getBoltJobId() != null) {
            var boltJob = boltApiClient.getJob(r.getBoltJobId());
            if (boltJob.isFound()) trackingUrl = boltJob.getTrackingUrl();
        }
        TrackInfo.TrackInfoBuilder b = TrackInfo.builder()
                .requestId(r.getId())
                .status(r.getStatus().name())
                .trackingUrl(trackingUrl);
        Optional<AgentAssignment> assignment = agentAssignmentRepository.findByRequestAndStatus(r, AgentAssignment.AssignmentStatus.ACCEPTED)
                .or(() -> agentAssignmentRepository.findByRequestAndStatus(r, AgentAssignment.AssignmentStatus.EN_ROUTE))
                .or(() -> agentAssignmentRepository.findByRequestAndStatus(r, AgentAssignment.AssignmentStatus.ARRIVED))
                .or(() -> agentAssignmentRepository.findByRequestAndStatus(r, AgentAssignment.AssignmentStatus.DELIVERED));
        if (assignment.isPresent()) {
            Agent a = assignment.get().getAgent();
            b.agentLat(a != null ? a.getCurrentLat() : null)
             .agentLng(a != null ? a.getCurrentLng() : null)
             .assignmentStatus(assignment.get().getStatus().name());
        } else {
            b.agentLat(null).agentLng(null).assignmentStatus(null);
        }
        return b.build();
    }

    @Data
    @Builder
    public static class TrackInfo {
        private UUID requestId;
        private String status;
        private String trackingUrl;
        private Double agentLat;
        private Double agentLng;
        private String assignmentStatus;
    }

    public List<CashRequest> listByUserId(String userId, int limit) {
        User user = userService.getById(userId);
        return cashRequestRepository.findByUserOrderByCreatedAtDesc(user, PageRequest.of(0, limit));
    }

    /** Trigger Selcom collect for a VERIFIED request; on success move to SEARCHING_AGENT and notify agents. */
    @Transactional
    public CashRequest collectPayment(UUID id, UUID userId) {
        CashRequest request = getByIdAndUser(id, userId);
        if (request.getStatus() != CashRequest.CashRequestStatus.VERIFIED) {
            throw new IllegalStateException("Request must be VERIFIED to collect: " + request.getStatus());
        }
        if (request.getClientBankCode() == null || request.getSelcomRequestId() == null) {
            throw new IllegalStateException("Request has no bank details for collection");
        }
        var collectResult = selcomApiClient.collectPayment(
                request.getSelcomRequestId(), request.getClientBankCode(), request.getClientAccountNumber(), request.getClientAccountName(),
                request.getTotalClientCharge(), "QC-" + request.getId());
        if (!collectResult.isSuccess()) {
            throw new IllegalStateException("Collection failed: " + collectResult.getError());
        }
        request.setStatus(CashRequest.CashRequestStatus.SEARCHING_AGENT);
        request = cashRequestRepository.save(request);
        dispatchAfterPayment(request);
        log.info("Payment collected for requestId={}, moving to SEARCHING_AGENT", id);
        return request;
    }

    /** After payment: Bolt track = create Bolt job (idempotent by reference_id); Agent track = notify agents (PENDING assignments + FCM). */
    private void dispatchAfterPayment(CashRequest request) {
        if (request.getRequestType() == CashRequest.RequestType.BOLT_DELIVERY) {
            if (request.getBoltJobId() != null && !request.getBoltJobId().isBlank()) {
                log.debug("Bolt job already exists for requestId={}, idempotent skip", request.getId());
                return;
            }
            var result = boltApiClient.createJobForRequest(request);
            if (result.isSuccess() && result.getJobId() != null) {
                request.setBoltJobId(result.getJobId());
                cashRequestRepository.save(request);
                log.info("Bolt job created for requestId={}, boltJobId={}", request.getId(), result.getJobId());
            } else {
                log.warn("Bolt createJob failed for requestId={}: {}", request.getId(), result.getError());
            }
        } else {
            notifyAgentsForRequest(request);
        }
    }

    @Transactional
    public CashRequest cancel(UUID id, UUID userId) {
        CashRequest r = getByIdAndUser(id, userId);
        switch (r.getStatus()) {
            case PENDING_VERIFICATION, VERIFIED, SEARCHING_AGENT, PENDING -> {
                if (r.getBoltJobId() != null) {
                    var cancelResult = boltApiClient.cancelJob(r.getBoltJobId());
                    if (cancelResult.isSuccess()) log.info("Bolt job cancelled for requestId={}, boltJobId={}", id, r.getBoltJobId());
                    else log.warn("Bolt cancelJob failed for requestId={}: {}", id, cancelResult.getError());
                }
                r.setStatus(CashRequest.CashRequestStatus.CANCELLED);
            }
            default -> throw new IllegalArgumentException("Request cannot be cancelled in status: " + r.getStatus());
        }
        return cashRequestRepository.save(r);
    }
}
