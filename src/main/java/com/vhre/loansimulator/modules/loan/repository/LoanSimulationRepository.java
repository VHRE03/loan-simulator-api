package com.vhre.loansimulator.modules.loan.repository;

import com.vhre.loansimulator.modules.loan.entity.LoanSimulation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface LoanSimulationRepository extends JpaRepository<LoanSimulation, UUID> {
}
