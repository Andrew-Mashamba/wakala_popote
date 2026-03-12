package com.quickcash.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quickcash.domain.PaymentMethodSubType;
import com.quickcash.domain.PaymentMethodType;
import com.quickcash.repository.PaymentMethodTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Seeds payment method types from db/seed/payment_method_types.json when table is empty.
 * Data is externalized - no hardcoding in code.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(100)
public class PaymentMethodTypeSeeder implements ApplicationRunner {

    private final PaymentMethodTypeRepository paymentMethodTypeRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (paymentMethodTypeRepository.count() > 0) {
            log.debug("Payment method types already seeded, skipping");
            return;
        }
        try {
            var resource = new ClassPathResource("db/seed/payment_method_types.json");
            List<Map<String, Object>> raw = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<>() {});
            for (Map<String, Object> m : raw) {
                var type = PaymentMethodType.builder()
                        .id((String) m.get("id"))
                        .label((String) m.get("label"))
                        .displayOrder(((Number) m.getOrDefault("displayOrder", 0)).intValue())
                        .build();
                @SuppressWarnings("unchecked")
                var subList = (List<Map<String, Object>>) m.getOrDefault("subTypes", List.of());
                for (Map<String, Object> s : subList) {
                    var sub = PaymentMethodSubType.builder()
                            .id((String) s.get("id"))
                            .label((String) s.get("label"))
                            .displayOrder(((Number) s.getOrDefault("displayOrder", 0)).intValue())
                            .paymentMethodType(type)
                            .build();
                    type.getSubTypes().add(sub);
                }
                paymentMethodTypeRepository.save(type);
            }
            log.info("Seeded {} payment method types from payment_method_types.json", raw.size());
        } catch (Exception e) {
            log.error("Failed to seed payment method types", e);
            throw new RuntimeException("Payment method type seeding failed", e);
        }
    }
}
