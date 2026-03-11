package com.quickcash.service;

import com.quickcash.domain.*;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.repository.AgentAssignmentRepository;
import com.quickcash.repository.AgentRepository;
import com.quickcash.repository.CashRequestRepository;
import com.quickcash.repository.QrCollectionRequestRepository;
import com.quickcash.selcom.SelcomApiClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * QR collection: generate one-time QR, agent scan and complete. Logs to qr.log.
 */
@Service
@RequiredArgsConstructor
public class QrCollectionService {

    private static final Logger log = LoggerFactory.getLogger("com.quickcash.qr");
    private static final int QR_VALID_SECONDS = 30 * 60; // 30 min

    private final QrCollectionRequestRepository qrRepository;
    private final CashRequestRepository cashRequestRepository;
    private final AgentAssignmentRepository agentAssignmentRepository;
    private final AgentRepository agentRepository;
    private final UserService userService;
    private final FeeCalculationService feeCalculationService;
    private final PaymentMethodService paymentMethodService;
    private final SelcomApiClient selcomApiClient;
    private final SettlementService settlementService;

    @Transactional
    public QrCollectionRequest generate(UUID userId, BigDecimal amount, UUID paymentMethodId, Boolean collectNow) {
        User user = userService.getById(userId.toString());
        var fees = feeCalculationService.calculate(amount);
        Double lat = user.getLatitude() != null ? user.getLatitude() : 0.0;
        Double lng = user.getLongitude() != null ? user.getLongitude() : 0.0;

        CashRequest request = CashRequest.builder()
                .user(user)
                .requestType(CashRequest.RequestType.LOCAL_CASH)
                .clientPaymentMethodId(paymentMethodId)
                .requestedAmount(amount)
                .principalAmount(fees.getPrincipalAmount())
                .serviceFee(fees.getServiceFee())
                .transportFee(fees.getTransportFee())
                .agentFee(fees.getAgentFee())
                .totalClientCharge(fees.getTotalClientCharge())
                .totalAgentPayment(fees.getTotalAgentPayment())
                .userLatitude(lat)
                .userLongitude(lng)
                .deliveryLat(lat)
                .deliveryLng(lng)
                .clientName(user.getDisplayName())
                .status(CashRequest.CashRequestStatus.PENDING_VERIFICATION)
                .build();
        request = cashRequestRepository.save(request);

        if (paymentMethodId != null) {
            PaymentMethod pm = paymentMethodService.getByIdAndUser(paymentMethodId, userId);
            if (pm.getMethodType() == PaymentMethod.MethodType.BANK_ACCOUNT && pm.getBankCode() != null) {
                request.setClientBankCode(pm.getBankCode());
                request.setClientAccountNumber(pm.getAccountNumber());
                request.setClientAccountName(pm.getAccountName());
                var verifyResult = selcomApiClient.verifyAccount(pm.getBankCode(), pm.getAccountNumber(), pm.getAccountName(), request.getTotalClientCharge());
                request.setSelcomRequestId(verifyResult.getRequestId());
                request.setSelcomVerificationStatus(verifyResult.isVerified() ? CashRequest.SelcomVerificationStatus.VERIFIED : CashRequest.SelcomVerificationStatus.FAILED);
                if (verifyResult.isVerified()) {
                    request.setVerifiedAt(Instant.now());
                    request.setStatus(CashRequest.CashRequestStatus.VERIFIED);
                    if (Boolean.TRUE.equals(collectNow)) {
                        var collectResult = selcomApiClient.collectPayment(
                                verifyResult.getRequestId(), pm.getBankCode(), pm.getAccountNumber(), pm.getAccountName(),
                                request.getTotalClientCharge(), "QC-QR-" + request.getId());
                        if (collectResult.isSuccess()) {
                            request.setStatus(CashRequest.CashRequestStatus.SEARCHING_AGENT);
                        }
                    }
                }
            }
            request = cashRequestRepository.save(request);
        }

        String token = UUID.randomUUID().toString().replace("-", "") + Integer.toHexString(ThreadLocalRandom.current().nextInt(0, 0xffff));
        if (token.length() > 64) token = token.substring(0, 64);
        Instant expiresAt = Instant.now().plusSeconds(QR_VALID_SECONDS);

        QrCollectionRequest qr = QrCollectionRequest.builder()
                .user(user)
                .amount(amount)
                .totalClientCharge(request.getTotalClientCharge())
                .cashRequest(request)
                .qrToken(token)
                .expiresAt(expiresAt)
                .status(QrCollectionRequest.QrStatus.PENDING)
                .build();
        qr = qrRepository.save(qr);
        log.info("QR collection generated: qrId={}, requestId={}, userId={}, amount={}, expiresAt={}", qr.getId(), request.getId(), userId, amount, expiresAt);
        return qr;
    }

    public QrCollectionRequest scanByToken(String token, UUID agentUserId) {
        QrCollectionRequest qr = qrRepository.findByQrToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("QR collection", token));
        if (qr.getStatus() != QrCollectionRequest.QrStatus.PENDING) {
            throw new IllegalStateException("QR already used or expired: " + qr.getStatus());
        }
        if (Instant.now().isAfter(qr.getExpiresAt())) {
            qr.setStatus(QrCollectionRequest.QrStatus.EXPIRED);
            qrRepository.save(qr);
            log.warn("QR scan failed: expired, qrId={}", qr.getId());
            throw new IllegalStateException("QR expired");
        }
        Agent agent = agentRepository.findByUserId(agentUserId).orElseThrow(() -> new ResourceNotFoundException("Agent not found for userId: " + agentUserId));
        qr.setAgent(agent);
        qr.setStatus(QrCollectionRequest.QrStatus.SCANNED);
        qr.setScannedAt(Instant.now());
        qr = qrRepository.save(qr);
        CashRequest request = qr.getCashRequest();
        if (request != null) {
            AgentAssignment assignment = AgentAssignment.builder()
                    .request(request)
                    .agent(agent)
                    .status(AgentAssignment.AssignmentStatus.ACCEPTED)
                    .assignmentMethod(AgentAssignment.AssignmentMethod.MANUAL)
                    .build();
            agentAssignmentRepository.save(assignment);
            request.setStatus(CashRequest.CashRequestStatus.AGENT_ASSIGNED);
            request.setAgentAssignedAt(Instant.now());
            cashRequestRepository.save(request);
        }
        log.info("QR scanned: qrId={}, token={}, agentId={}", qr.getId(), token, agent.getId());
        return qr;
    }

    @Transactional
    public QrCollectionRequest completeByToken(String token, UUID agentUserId) {
        QrCollectionRequest qr = qrRepository.findByQrToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("QR collection", token));
        if (qr.getStatus() != QrCollectionRequest.QrStatus.SCANNED) {
            throw new IllegalStateException("QR must be scanned first: " + qr.getStatus());
        }
        Agent agent = agentRepository.findByUserId(agentUserId).orElseThrow(() -> new ResourceNotFoundException("Agent not found for userId: " + agentUserId));
        if (!agent.getId().equals(qr.getAgent().getId())) {
            throw new IllegalArgumentException("Only the scanning agent can complete");
        }
        qr.setStatus(QrCollectionRequest.QrStatus.COMPLETED);
        qr.setCompletedAt(Instant.now());
        qr = qrRepository.save(qr);

        CashRequest request = qr.getCashRequest();
        if (request != null) {
            var assignment = agentAssignmentRepository.findByRequestAndStatus(request, AgentAssignment.AssignmentStatus.ACCEPTED)
                    .orElseThrow(() -> new IllegalStateException("No assignment for QR request"));
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
            var creditResult = selcomApiClient.creditAgent(agent.getSelcomAccountId(), agentAmount, "QC-QR-" + request.getId());
            if (creditResult.isSuccess()) {
                settlementService.markAgentCredited(settlement.getId(), creditResult.getTransactionId());
                assignment.setSettlementStatus(AgentAssignment.SettlementStatus.CREDITED);
                assignment.setSettlementReference(creditResult.getTransactionId());
                assignment.setSettledAt(Instant.now());
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
        }
        log.info("QR collection completed: qrId={}, token={}, agentId={}", qr.getId(), token, agent.getId());
        return qr;
    }
}
