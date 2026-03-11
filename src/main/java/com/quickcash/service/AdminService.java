package com.quickcash.service;

import com.quickcash.domain.*;
import com.quickcash.exception.ResourceNotFoundException;
import com.quickcash.repository.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Admin operations: requests, deposits, settlements, agents, applications, compliance, fraud.
 * Logs to admin.log via logger name.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger("com.quickcash.admin");

    private final CashRequestRepository cashRequestRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final SettlementRepository settlementRepository;
    private final AgentRepository agentRepository;
    private final SelcomAccountApplicationRepository applicationRepository;
    private final AdminFlagRepository adminFlagRepository;
    private final KycApplicationService kycApplicationService;
    private final AuditLogService auditLogService;

    public List<CashRequest> listRequests(int limit) {
        return cashRequestRepository.findAll(PageRequest.of(0, Math.min(limit, 100))).getContent();
    }

    public List<DepositRequest> listDeposits(int limit) {
        return depositRequestRepository.findAll(PageRequest.of(0, Math.min(limit, 100))).getContent();
    }

    public List<Settlement> listSettlements(int limit) {
        return settlementRepository.findAll(PageRequest.of(0, Math.min(limit, 100))).getContent();
    }

    public List<Agent> listAgents(int limit) {
        return agentRepository.findAll(PageRequest.of(0, Math.min(limit, 100))).getContent();
    }

    public Agent getAgent(UUID agentId) {
        return agentRepository.findById(agentId).orElseThrow(() -> new ResourceNotFoundException("Agent", agentId));
    }

    @Transactional
    public Agent verifyAgent(UUID agentId) {
        Agent a = getAgent(agentId);
        a.setAgentTier(Agent.AgentTier.VERIFIED);
        a = agentRepository.save(a);
        log.info("Admin: agent verified: agentId={}", agentId);
        auditLogService.log("ADMIN_AGENT_VERIFIED", "Agent", agentId, null, "ADMIN", "agentId=" + agentId);
        return a;
    }

    @Transactional
    public Agent suspendAgent(UUID agentId) {
        Agent a = getAgent(agentId);
        a.setSelcomAccountStatus(Agent.SelcomAccountStatus.SUSPENDED);
        a.setIsAvailable(false);
        a = agentRepository.save(a);
        log.info("Admin: agent suspended: agentId={}", agentId);
        auditLogService.log("ADMIN_AGENT_SUSPENDED", "Agent", agentId, null, "ADMIN", "agentId=" + agentId);
        return a;
    }

    @Transactional
    public Agent activateAgent(UUID agentId) {
        Agent a = getAgent(agentId);
        a.setSelcomAccountStatus(Agent.SelcomAccountStatus.ACTIVE);
        a.setIsAvailable(true);
        a = agentRepository.save(a);
        log.info("Admin: agent activated: agentId={}", agentId);
        auditLogService.log("ADMIN_AGENT_ACTIVATED", "Agent", agentId, null, "ADMIN", "agentId=" + agentId);
        return a;
    }

    @Transactional
    public Agent setAgentTier(UUID agentId, String tier) {
        Agent a = getAgent(agentId);
        try {
            a.setAgentTier(Agent.AgentTier.valueOf(tier.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid tier: " + tier);
        }
        a = agentRepository.save(a);
        log.info("Admin: agent tier set: agentId={}, tier={}", agentId, tier);
        auditLogService.log("ADMIN_AGENT_TIER", "Agent", agentId, null, "ADMIN", "agentId=" + agentId + ", tier=" + tier);
        return a;
    }

    public List<SelcomAccountApplication> listApplications(int limit) {
        return applicationRepository.findAll(PageRequest.of(0, Math.min(limit, 100))).getContent();
    }

    public SelcomAccountApplication getApplication(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("KycApplication", applicationId));
    }

    @Transactional
    public SelcomAccountApplication approveApplication(UUID applicationId) {
        SelcomAccountApplication app = getApplication(applicationId);
        if (app.getStatus() != SelcomAccountApplication.ApplicationStatus.APPROVED
                && app.getStatus() != SelcomAccountApplication.ApplicationStatus.MANUAL_REVIEW) {
            app.setStatus(SelcomAccountApplication.ApplicationStatus.APPROVED);
            app.setApprovedAt(java.time.Instant.now());
            app = applicationRepository.save(app);
        }
        kycApplicationService.createSelcomAccount(applicationId);
        app = applicationRepository.findById(applicationId).orElse(app);
        log.info("Admin: application approved and Selcom account created: applicationId={}", applicationId);
        auditLogService.log("ADMIN_APPLICATION_APPROVED", "KycApplication", applicationId, null, "ADMIN", "applicationId=" + applicationId);
        return app;
    }

    @Transactional
    public SelcomAccountApplication rejectApplication(UUID applicationId, String reason) {
        SelcomAccountApplication app = getApplication(applicationId);
        app.setStatus(SelcomAccountApplication.ApplicationStatus.REJECTED);
        app.setRejectionReason(reason);
        app = applicationRepository.save(app);
        log.info("Admin: application rejected: applicationId={}, reason={}", applicationId, reason);
        auditLogService.log("ADMIN_APPLICATION_REJECTED", "KycApplication", applicationId, null, "ADMIN", "applicationId=" + applicationId);
        return app;
    }

    @Transactional
    public SelcomAccountApplication manualReviewApplication(UUID applicationId) {
        SelcomAccountApplication app = getApplication(applicationId);
        app.setStatus(SelcomAccountApplication.ApplicationStatus.MANUAL_REVIEW);
        app = applicationRepository.save(app);
        log.info("Admin: application set to manual review: applicationId={}", applicationId);
        auditLogService.log("ADMIN_APPLICATION_MANUAL_REVIEW", "KycApplication", applicationId, null, "ADMIN", "applicationId=" + applicationId);
        return app;
    }

    public List<AdminFlag> listComplianceFlags() {
        return adminFlagRepository.findByFlagTypeAndResolvedFalseOrderByCreatedAtDesc(AdminFlag.FlagType.COMPLIANCE);
    }

    @Transactional
    public void clearComplianceFlag(UUID flagId) {
        AdminFlag f = adminFlagRepository.findById(flagId).orElseThrow(() -> new ResourceNotFoundException("AdminFlag", flagId));
        if (f.getFlagType() != AdminFlag.FlagType.COMPLIANCE) {
            throw new IllegalArgumentException("Not a compliance flag");
        }
        f.setResolved(true);
        adminFlagRepository.save(f);
        log.info("Admin: compliance flag cleared: flagId={}", flagId);
        auditLogService.log("ADMIN_COMPLIANCE_CLEARED", "AdminFlag", flagId, null, "ADMIN", "flagId=" + flagId);
    }

    public List<AdminFlag> listFraudAlerts() {
        return adminFlagRepository.findByFlagTypeAndBlockedFalseOrderByCreatedAtDesc(AdminFlag.FlagType.FRAUD);
    }

    @Transactional
    public void blockFraud(UUID flagId) {
        AdminFlag f = adminFlagRepository.findById(flagId).orElseThrow(() -> new ResourceNotFoundException("AdminFlag", flagId));
        if (f.getFlagType() != AdminFlag.FlagType.FRAUD) {
            throw new IllegalArgumentException("Not a fraud flag");
        }
        f.setBlocked(true);
        f.setResolved(true);
        adminFlagRepository.save(f);
        log.info("Admin: fraud flag blocked: flagId={}", flagId);
        auditLogService.log("ADMIN_FRAUD_BLOCKED", "AdminFlag", flagId, null, "ADMIN", "flagId=" + flagId);
    }
}
