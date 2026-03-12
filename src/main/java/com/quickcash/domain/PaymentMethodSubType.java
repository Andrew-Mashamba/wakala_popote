package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_method_sub_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodSubType {

    @Id
    @Column(columnDefinition = "varchar(50)", length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_type_id", nullable = false)
    private PaymentMethodType paymentMethodType;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;
}
