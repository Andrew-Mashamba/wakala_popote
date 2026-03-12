package com.quickcash.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payment_method_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentMethodType {

    @Id
    @Column(columnDefinition = "varchar(50)", length = 50)
    private String id;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    @OneToMany(mappedBy = "paymentMethodType", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC, id ASC")
    @Builder.Default
    private List<PaymentMethodSubType> subTypes = new ArrayList<>();
}
