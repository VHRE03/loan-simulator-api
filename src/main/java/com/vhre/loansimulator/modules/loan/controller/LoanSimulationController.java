package com.vhre.loansimulator.modules.loan.controller;

import com.vhre.base.core.base.controller.BaseController;
import com.vhre.loansimulator.modules.loan.dto.LoanSimulationDTO;
import com.vhre.loansimulator.modules.loan.entity.LoanSimulation;
import com.vhre.loansimulator.modules.loan.service.LoanSimulationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loan-simulations")
@Tag(name = "Loan Simulation Management", description = "Endpoints for managing Loan Simulation")
public class LoanSimulationController extends BaseController<LoanSimulation, LoanSimulationDTO, UUID> {

    private final LoanSimulationService loanSimulationService;

    public LoanSimulationController(LoanSimulationService service) {
        super(service);
        this.loanSimulationService = service;
    }

    @PatchMapping("/{id}/complete")
    @Operation(summary = "Marca la simulación como COMPLETADA (solo desde DRAFT; idempotente si ya está COMPLETADA; 409 si está EXPIRED)")
    public ResponseEntity<LoanSimulationDTO> completeSimulation(@PathVariable UUID id) {
        return ResponseEntity.ok(loanSimulationService.completeSimulation(id));
    }
}
