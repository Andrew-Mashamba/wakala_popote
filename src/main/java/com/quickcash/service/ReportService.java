package com.quickcash.service;

import com.quickcash.domain.CashRequest;
import com.quickcash.domain.DepositRequest;
import com.quickcash.domain.Settlement;
import com.quickcash.repository.CashRequestRepository;
import com.quickcash.repository.DepositRequestRepository;
import com.quickcash.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Admin reporting: summary and export. Logs to reporting.log.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger("com.quickcash.reporting");

    private final CashRequestRepository cashRequestRepository;
    private final DepositRequestRepository depositRequestRepository;
    private final SettlementRepository settlementRepository;

    public Map<String, Object> getSummary(Instant from, Instant to) {
        final Instant fromInclusive = from != null ? from : Instant.now().minus(30, ChronoUnit.DAYS);
        final Instant toInclusive = to != null ? to : Instant.now();

        List<CashRequest> requests = cashRequestRepository.findAll().stream()
                .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(fromInclusive) && !r.getCreatedAt().isAfter(toInclusive))
                .toList();
        List<DepositRequest> deposits = depositRequestRepository.findAll().stream()
                .filter(d -> d.getCreatedAt() != null && !d.getCreatedAt().isBefore(fromInclusive) && !d.getCreatedAt().isAfter(toInclusive))
                .toList();
        List<Settlement> settlements = settlementRepository.findAll().stream()
                .filter(s -> s.getCreatedAt() != null && !s.getCreatedAt().isBefore(fromInclusive) && !s.getCreatedAt().isAfter(toInclusive))
                .toList();

        BigDecimal totalRequestAmount = requests.stream()
                .map(r -> r.getTotalClientCharge() != null ? r.getTotalClientCharge() : r.getRequestedAmount() != null ? r.getRequestedAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalDepositAmount = deposits.stream()
                .map(d -> d.getCashAmount() != null ? d.getCashAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long deliveredCount = requests.stream().filter(r -> r.getStatus() == CashRequest.CashRequestStatus.DELIVERED || r.getStatus() == CashRequest.CashRequestStatus.SETTLED).count();
        long completedDeposits = deposits.stream().filter(d -> d.getStatus() == DepositRequest.DepositStatus.COMPLETED).count();

        Map<String, Object> summary = new HashMap<>();
        summary.put("from", fromInclusive.toString());
        summary.put("to", toInclusive.toString());
        summary.put("cashRequestsCount", requests.size());
        summary.put("cashRequestsTotalAmount", totalRequestAmount);
        summary.put("deliveredCount", deliveredCount);
        summary.put("depositsCount", deposits.size());
        summary.put("depositsTotalAmount", totalDepositAmount);
        summary.put("completedDepositsCount", completedDeposits);
        summary.put("settlementsCount", settlements.size());
        log.info("Report summary: from={}, to={}, requests={}, deposits={}", fromInclusive, toInclusive, requests.size(), deposits.size());
        return summary;
    }

    public List<Map<String, String>> exportRequests(Instant from, Instant to, int limit) {
        final Instant fromInclusive = from != null ? from : Instant.now().minus(30, ChronoUnit.DAYS);
        final Instant toInclusive = to != null ? to : Instant.now();
        List<CashRequest> requests = cashRequestRepository.findAll().stream()
                .filter(r -> r.getCreatedAt() != null && !r.getCreatedAt().isBefore(fromInclusive) && !r.getCreatedAt().isAfter(toInclusive))
                .limit(limit)
                .toList();
        log.info("Report export requests: from={}, to={}, count={}", fromInclusive, toInclusive, requests.size());
        return requests.stream().map(r -> Map.<String, String>of(
                "id", r.getId().toString(),
                "status", r.getStatus().name(),
                "amount", r.getRequestedAmount() != null ? r.getRequestedAmount().toPlainString() : "",
                "createdAt", r.getCreatedAt() != null ? r.getCreatedAt().toString() : ""
        )).collect(Collectors.toList());
    }
}
