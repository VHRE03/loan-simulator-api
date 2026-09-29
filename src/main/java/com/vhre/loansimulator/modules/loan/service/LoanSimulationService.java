package com.vhre.loansimulator.modules.loan.service;

import com.vhre.base.core.base.service.BaseService;
import com.vhre.loansimulator.modules.loan.dto.LoanSimulationDTO;

import java.util.UUID;

public interface LoanSimulationService extends BaseService<LoanSimulationDTO, UUID> {
    LoanSimulationDTO completeSimulation(UUID id);
}
