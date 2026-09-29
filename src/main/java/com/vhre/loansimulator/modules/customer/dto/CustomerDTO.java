package com.vhre.loansimulator.modules.customer.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.vhre.base.core.base.dto.BaseDTO;
import com.vhre.loansimulator.modules.customer.validation.Adult;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Data Transfer Object representing a Customer (borrower profile)")
@JsonPropertyOrder({"id", "firstName", "lastName", "email", "phoneNumber", "nationalId", "dateOfBirth", "createdAt", "updatedAt", "deleted"})
public class CustomerDTO extends BaseDTO {

    @Schema(description = "Nombre del solicitante.", example = "Maria")
    @NotBlank
    @Size(max = 100)
    private String firstName;

    @Schema(description = "Apellido del solicitante.", example = "Gonzalez Lopez")
    @NotBlank
    @Size(max = 100)
    private String lastName;

    @Schema(description = "Correo electrónico (único por cliente).", example = "maria.gonzalez@example.com")
    @NotBlank
    @Email
    @Size(max = 255)
    private String email;

    @Schema(description = "Número de teléfono de contacto.", example = "+52 55 1234 5678")
    @NotBlank
    @Size(max = 20)
    private String phoneNumber;

    @Schema(description = "Identificador nacional (CURP, 18 caracteres). Único por cliente.", example = "GOLM900101MDFRNR09")
    @NotBlank
    @Size(max = 18)
    private String nationalId;

    @Schema(description = "Fecha de nacimiento (el solicitante debe ser mayor de 18 años).", example = "1990-01-01")
    @NotNull
    @Past
    @Adult
    private LocalDate dateOfBirth;
}
