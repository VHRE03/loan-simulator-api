package com.vhre.loansimulator.modules.loan.controller;

import com.vhre.base.core.base.controller.BaseController;
import com.vhre.loansimulator.modules.loan.dto.LoanSimulationDTO;
import com.vhre.loansimulator.modules.loan.entity.LoanSimulation;
import com.vhre.loansimulator.modules.loan.service.LoanSimulationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loan-simulations")
@Tag(name = "Loan Simulation Management", description = "Endpoints for managing Loan Simulation")
public class LoanSimulationController extends BaseController<LoanSimulation, LoanSimulationDTO, UUID> {

    public LoanSimulationController(LoanSimulationService service) {
        super(service);
    }
}
