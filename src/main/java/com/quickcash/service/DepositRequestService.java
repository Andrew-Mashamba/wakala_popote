package com.quickcash.service;

import com.quickcash.domain.Agent;
import com.quickcash.domain.DepositRequest;
import com.quickcash.domain.User;
import com.quickcash.dto.DepositRequestCreate;
import com.quickcash.dto.DepositTrackResponse;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.notification.FcmNotificationService;
import com.quickcash.repository.AgentRepository;
import com.quickcash.repository.DepositRequestRepository;
import com.quickcash.selcom.SelcomApiClient;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Deposit flow: client request -> verify account -> search agent -> agent accept -> en-route -> arrived -> collect
 * -> complete (Selcom debit agent + TIPS credit to client bank). Logs to deposit.log via logger name.
 */
@Service
@RequiredArgsConstructor
public class DepositRequestService {

    private static final Logger log = LoggerFactory.getLogger("com.quickcash.deposit");

    private final DepositRequestRepository depositRequestRepository;
    private final UserService userService;
    private final FeeCalculationService feeCalculationService;
    private final SelcomApiClient selcomApiClient;
    private final AgentRepository agentRepository;
    private final FcmNotificationService fcmNotificationService;

    @Transactional
    public DepositRequest create(UUID clientUserId, DepositRequestCreate req) {
        User client = userService.getById(clientUserId.toString());
        var fees = feeCalculationService.calculateDeposit(req.getCashAmount());

        var verifyResult = selcomApiClient.verifyAccount(
                req.getDestinationBankCode(),
                req.getDestinationAccountNumber(),
                req.getDestinationAccountName() != null ? req.getDestinationAccountName() : "N/A",
                fees.getNetDepositAmount());

        DepositRequest.DepositStatus status = verifyResult.isVerified()
                ? DepositRequest.DepositStatus.VERIFIED
                : DepositRequest.DepositStatus.PENDING_VERIFICATION;

        DepositRequest d = DepositRequest.builder()
                .clientUser(client)
                .destinationBankCode(req.getDestinationBankCode())
                .destinationAccountNumber(req.getDestinationAccountNumber())
                .destinationAccountName(req.getDestinationAccountName())
                .collectionLat(req.getCollectionLat())
                .collectionLng(req.getCollectionLng())
                .collectionAddress(req.getCollectionAddress())
                .cashAmount(fees.getCashAmount())
                .serviceFee(fees.getServiceFee())
                .agentCommission(fees.getAgentCommission())
                .platformMargin(fees.getPlatformMargin())
                .netDepositAmount(fees.getNetDepositAmount())
                .status(status)
                .selcomDebitStatus(null)
                .tipsCreditStatus(null)
                .build();
        d.setCreatedAt(Instant.now());
        if (verifyResult.isVerified()) {
            d.setVerifiedAt(Instant.now());
            d.setStatus(DepositRequest.DepositStatus.SEARCHING_AGENT);
        }
        d = depositRequestRepository.save(d);
        log.info("Deposit request created: id={}, clientUserId={}, amount={}, status={}, verified={}",
                d.getId(), clientUserId, req.getCashAmount(), d.getStatus(), verifyResult.isVerified());
        return d;
    }

    public List<DepositRequest> listByClient(UUID clientUserId, int limit) {
        User client = userService.getById(clientUserId.toString());
        return depositRequestRepository.findByClientUserOrderByCreatedAtDesc(client, PageRequest.of(0, Math.min(limit, 100)));
    }

    public DepositRequest getByIdAndClient(UUID depositId, UUID clientUserId) {
        DepositRequest d = depositRequestRepository.findById(depositId).orElseThrow(() -> new ResourceNotFoundException("DepositRequest", depositId));
        if (!d.getClientUser().getId().equals(clientUserId)) {
            throw new ResourceNotFoundException("DepositRequest", depositId);
        }
        return d;
    }

    @Transactional
    public DepositRequest cancel(UUID depositId, UUID clientUserId, String reason) {
        DepositRequest d = getByIdAndClient(depositId, clientUserId);
        if (d.getStatus() == DepositRequest.DepositStatus.CANCELLED || d.getStatus() == DepositRequest.DepositStatus.COMPLETED
                || d.getStatus() == DepositRequest.DepositStatus.FAILED) {
            log.warn("Deposit cancel ignored: id={}, status={}", depositId, d.getStatus());
            return d;
        }
        d.setStatus(DepositRequest.DepositStatus.CANCELLED);
        d.setCancellationReason(reason);
        d = depositRequestRepository.save(d);
        log.info("Deposit cancelled: id={}, clientUserId={}, reason={}", depositId, clientUserId, reason);
        return d;
    }

    public DepositTrackResponse getTrackInfo(UUID depositId, UUID clientUserId) {
        DepositRequest d = getByIdAndClient(depositId, clientUserId);
        Double agentLat = null;
        Double agentLng = null;
        String agentName = null;
        if (d.getAssignedAgent() != null) {
            Agent a = d.getAssignedAgent();
            agentLat = a.getCurrentLat();
            agentLng = a.getCurrentLng();
            agentName = a.getUser() != null ? a.getUser().getDisplayName() : null;
        }
        return DepositTrackResponse.builder()
                .depositId(d.getId())
                .status(d.getStatus().name())
                .agentLat(agentLat)
                .agentLng(agentLng)
                .agentDisplayName(agentName)
                .build();
    }

    @Transactional
    public DepositRequest confirmCollection(UUID depositId, UUID clientUserId) {
        DepositRequest d = getByIdAndClient(depositId, clientUserId);
        if (d.getStatus() != DepositRequest.DepositStatus.AGENT_ASSIGNED && d.getStatus() != DepositRequest.DepositStatus.AGENT_EN_ROUTE) {
            log.warn("Deposit confirm-collection ignored: id={}, status={}", depositId, d.getStatus());
            return d;
        }
        log.info("Client confirmed collection (handed cash): depositId={}, clientUserId={}", depositId, clientUserId);
        return d;
    }

    // --- Agent APIs ---

    public List<DepositRequest> listForAgent(UUID agentUserId, int limit) {
        Agent agent = agentRepository.findByUserId(agentUserId).orElseThrow(() -> new ResourceNotFoundException("Agent not found for userId: " + agentUserId));
        return depositRequestRepository.findByAssignedAgentOrderByCreatedAtDesc(agent, PageRequest.of(0, Math.min(limit, 50)));
    }

    public List<DepositRequest> listAvailableForAgent(UUID agentUserId, int limit) {
        Agent agent = agentRepository.findByUserId(agentUserId).orElseThrow(() -> new ResourceNotFoundException("Agent not found for userId: " + agentUserId));
        return depositRequestRepository.findByStatusOrderByCreatedAtDesc(DepositRequest.DepositStatus.SEARCHING_AGENT, PageRequest.of(0, Math.min(limit, 50)));
    }

    public DepositRequest getByIdAndAgent(UUID depositId, UUID agentUserId) {
        DepositRequest d = depositRequestRepository.findById(depositId).orElseThrow(() -> new ResourceNotFoundException("DepositRequest", depositId));
        Agent agent = agentRepository.findByUserId(agentUserId).orElseThrow(() -> new ResourceNotFoundException("Agent not found for userId: " + agentUserId));
        if (d.getAssignedAgent() == null || !d.getAssignedAgent().getId().equals(agent.getId())) {
            throw new ResourceNotFoundException("DepositRequest", depositId);
        }
        return d;
    }

    @Transactional
    public DepositRequest agentAccept(UUID depositId, UUID agentUserId) {
        DepositRequest d = depositRequestRepository.findById(depositId).orElseThrow(() -> new ResourceNotFoundException("DepositRequest", depositId));
        if (d.getStatus() != DepositRequest.DepositStatus.SEARCHING_AGENT) {
            log.warn("Deposit accept ignored: id={}, status={}", depositId, d.getStatus());
            throw new IllegalArgumentException("Deposit not in SEARCHING_AGENT");
        }
        Agent agent = agentRepository.findByUserId(agentUserId).orElseThrow(() -> new ResourceNotFoundException("Agent not found for userId: " + agentUserId));
        d.setAssignedAgent(agent);
        d.setStatus(DepositRequest.DepositStatus.AGENT_ASSIGNED);
        d.setAgentAssignedAt(Instant.now());
        d = depositRequestRepository.save(d);
        log.info("Deposit agent accepted: id={}, agentId={}", depositId, agent.getId());
        sendDepositNotification(d.getClientUser().getId(), d.getId(), "Deposit", "An agent has been assigned to your deposit.");
        return d;
    }

    @Transactional
    public DepositRequest agentReject(UUID depositId, UUID agentUserId, String reason) {
        DepositRequest d = depositRequestRepository.findById(depositId).orElseThrow(() -> new ResourceNotFoundException("DepositRequest", depositId));
        Agent agent = agentRepository.findByUserId(agentUserId).orElseThrow(() -> new ResourceNotFoundException("Agent not found for userId: " + agentUserId));
        if (d.getAssignedAgent() != null && d.getAssignedAgent().getId().equals(agent.getId())) {
            d.setAssignedAgent(null);
            d.setStatus(DepositRequest.DepositStatus.SEARCHING_AGENT);
            d.setAgentAssignedAt(null);
            d = depositRequestRepository.save(d);
            log.info("Deposit agent rejected: id={}, agentId={}, reason={}", depositId, agent.getId(), reason);
        }
        return d;
    }

    @Transactional
    public DepositRequest agentEnRoute(UUID depositId, UUID agentUserId) {
        DepositRequest d = getByIdAndAgent(depositId, agentUserId);
        if (d.getStatus() != DepositRequest.DepositStatus.AGENT_ASSIGNED) {
            return d;
        }
        d.setStatus(DepositRequest.DepositStatus.AGENT_EN_ROUTE);
        d.setAgentEnRouteAt(Instant.now());
        d = depositRequestRepository.save(d);
        log.info("Deposit agent en-route: id={}, agentId={}", depositId, d.getAssignedAgent().getId());
        sendDepositNotification(d.getClientUser().getId(), d.getId(), "Deposit", "Agent is on the way to collect your cash.");
        return d;
    }

    @Transactional
    public DepositRequest agentArrived(UUID depositId, UUID agentUserId) {
        DepositRequest d = getByIdAndAgent(depositId, agentUserId);
        log.info("Deposit agent arrived: id={}, agentId={}", depositId, d.getAssignedAgent().getId());
        sendDepositNotification(d.getClientUser().getId(), d.getId(), "Deposit", "Agent has arrived for collection.");
        return d;
    }

    @Transactional
    public DepositRequest agentCollect(UUID depositId, UUID agentUserId) {
        DepositRequest d = getByIdAndAgent(depositId, agentUserId);
        if (d.getStatus() != DepositRequest.DepositStatus.AGENT_ASSIGNED && d.getStatus() != DepositRequest.DepositStatus.AGENT_EN_ROUTE) {
            log.warn("Deposit collect ignored: id={}, status={}", depositId, d.getStatus());
            return d;
        }
        d.setStatus(DepositRequest.DepositStatus.CASH_COLLECTED);
        d.setCashCollectedAt(Instant.now());
        d = depositRequestRepository.save(d);
        log.info("Deposit cash collected: id={}, agentId={}", depositId, d.getAssignedAgent().getId());
        return d;
    }

    @Transactional
    public DepositRequest agentComplete(UUID depositId, UUID agentUserId) {
        DepositRequest d = getByIdAndAgent(depositId, agentUserId);
        if (d.getStatus() != DepositRequest.DepositStatus.CASH_COLLECTED) {
            log.warn("Deposit complete ignored: id={}, status={}", depositId, d.getStatus());
            throw new IllegalStateException("Deposit must be CASH_COLLECTED to complete");
        }
        d.setStatus(DepositRequest.DepositStatus.PROCESSING_CREDIT);
        d = depositRequestRepository.save(d);

        Agent agent = d.getAssignedAgent();
        String debitRef = "DEP-" + d.getId();
        var debitResult = selcomApiClient.debitAgent(agent.getSelcomAccountId(), d.getCashAmount(), debitRef);
        d.setSelcomDebitReference(debitResult.getTransactionId());
        d.setSelcomDebitStatus(debitResult.isSuccess() ? DepositRequest.SelcomDebitStatus.SUCCESS : DepositRequest.SelcomDebitStatus.FAILED);

        if (!debitResult.isSuccess()) {
            d.setStatus(DepositRequest.DepositStatus.FAILED);
            d.setFailureReason("Agent debit failed: " + debitResult.getError());
            d = depositRequestRepository.save(d);
            log.error("Deposit complete failed (agent debit): id={}, error={}", depositId, debitResult.getError());
            return d;
        }

        String creditRef = "DEP-CR-" + d.getId();
        var creditResult = selcomApiClient.creditToBank(
                d.getDestinationBankCode(), d.getDestinationAccountNumber(),
                d.getDestinationAccountName() != null ? d.getDestinationAccountName() : "Client",
                d.getNetDepositAmount(), creditRef);
        d.setTipsCreditReference(creditResult.getTransactionId());
        d.setTipsCreditStatus(creditResult.isSuccess() ? DepositRequest.TipsCreditStatus.SUCCESS : DepositRequest.TipsCreditStatus.FAILED);

        if (creditResult.isSuccess()) {
            d.setStatus(DepositRequest.DepositStatus.CREDITED);
            d.setCreditCompletedAt(Instant.now());
            d.setStatus(DepositRequest.DepositStatus.COMPLETED);
            d.setCompletedAt(Instant.now());
            log.info("Deposit completed: id={}, agentId={}, netAmount={}", depositId, agent.getId(), d.getNetDepositAmount());
            sendDepositNotification(d.getClientUser().getId(), d.getId(), "Deposit complete", "Your deposit of " + d.getNetDepositAmount() + " has been credited to your account.");
        } else {
            d.setStatus(DepositRequest.DepositStatus.FAILED);
            d.setFailureReason("TIPS credit failed: " + creditResult.getError());
            log.error("Deposit complete failed (TIPS credit): id={}, error={}", depositId, creditResult.getError());
        }
        return depositRequestRepository.save(d);
    }

    private void sendDepositNotification(UUID userId, UUID depositId, String title, String body) {
        User client = userService.getById(userId.toString());
        String token = client.getFcmToken();
        if (token != null && !token.isBlank()) {
            fcmNotificationService.sendToToken(userId, depositId, token, title, body, java.util.Map.of("type", "deposit", "depositId", depositId.toString()));
        }
    }
}
