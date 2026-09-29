package com.vhre.loansimulator.modules.loan.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.vhre.base.core.base.dto.BaseDTO;
import com.vhre.loansimulator.modules.loan.enums.SimulationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Schema(description = "Data Transfer Object representing a Loan Simulation")
@JsonPropertyOrder({"id", "amount", "termMonths", "annualInterestRate", "monthlyPayment", "status", "customerId", "createdAt", "updatedAt", "deleted"})
public class LoanSimulationDTO extends BaseDTO {

    @Schema(description = "Monto solicitado del préstamo.", example = "150000.00")
    @NotNull
    @Positive
    @Digits(integer = 16, fraction = 2)
    private BigDecimal amount;

    @Schema(description = "Plazo del préstamo en meses (1-360).", example = "60")
    @NotNull
    @Min(1)
    @Max(360)
    private Integer termMonths;

    @Schema(description = "Tasa de interés anual nominal en porcentaje (12.25 = 12.25%).", example = "12.2500")
    @NotNull
    @DecimalMin("0.0")
    @Digits(integer = 4, fraction = 4)
    private BigDecimal annualInterestRate;

    @Schema(description = "Cuota mensual fija calculada por la API (amortización francesa). Solo lectura.", example = "3355.65", accessMode = Schema.AccessMode.READ_ONLY)
    private BigDecimal monthlyPayment;

    @Schema(description = "Estado del ciclo de vida de la simulación (inicia como DRAFT). Solo lectura.", implementation = SimulationStatus.class, accessMode = Schema.AccessMode.READ_ONLY)
    private SimulationStatus status;

    @Schema(description = "UUID del cliente asociado (opcional). Debe existir; sin él, la simulación es anónima.", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID customerId;
}
