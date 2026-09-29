package com.vhre.loansimulator.modules.customer.entity;

import com.vhre.base.core.base.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "customers", uniqueConstraints = {
        @UniqueConstraint(name = "uq_customers_email", columnNames = "email"),
        @UniqueConstraint(name = "uq_customers_national_id", columnNames = "national_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Column(nullable = false, length = 100)
    private String firstName;

    @Column(nullable = false, length = 100)
    private String lastName;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 20)
    private String phoneNumber;

    @Column(nullable = false, length = 18)
    private String nationalId;

    @Column(nullable = false)
    private LocalDate dateOfBirth;
}
