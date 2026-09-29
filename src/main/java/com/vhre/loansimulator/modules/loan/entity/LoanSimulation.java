package com.vhre.loansimulator.modules.loan.entity;

import com.vhre.base.core.base.entity.BaseEntity;
import com.vhre.loansimulator.modules.customer.entity.Customer;
import com.vhre.loansimulator.modules.loan.enums.SimulationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "loan_simulations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoanSimulation extends BaseEntity {

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer termMonths;

    @Column(nullable = false, precision = 8, scale = 4)
    private BigDecimal annualInterestRate;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal monthlyPayment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SimulationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;
}
