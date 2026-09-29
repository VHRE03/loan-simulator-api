package com.vhre.loansimulator.modules.loan.service;

import com.vhre.base.core.exceptions.ResourceNotFoundException;
import com.vhre.loansimulator.modules.loan.dto.LoanSimulationDTO;
import com.vhre.loansimulator.modules.loan.entity.LoanSimulation;
import com.vhre.loansimulator.modules.loan.enums.SimulationStatus;
import com.vhre.loansimulator.modules.loan.mapper.LoanSimulationMapper;
import com.vhre.loansimulator.modules.loan.repository.LoanSimulationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoanSimulationServiceImpl}, following the reference
 * pattern of {@code pattern.ServiceTestPatternTest}: pure JUnit 5 + Mockito,
 * no database and no Spring context.
 *
 * <p>The mapper/repository are mocked; {@code thenAnswer} simulates the real
 * MapStruct behavior so the tests exercise the actual service flow
 * (toEntity → compute payment + DRAFT → save → toDto).</p>
 */
@ExtendWith(MockitoExtension.class)
class LoanSimulationServiceTest {

    @Mock
    LoanSimulationRepository repository;

    @Mock
    LoanSimulationMapper mapper;

    @InjectMocks
    LoanSimulationServiceImpl service;

    // -------------------------------------------------------------------------
    // Helpers: mimic MapStruct mappings without a Spring context
    // -------------------------------------------------------------------------

    private LoanSimulation entityFrom(LoanSimulationDTO dto) {
        return LoanSimulation.builder()
                .amount(dto.getAmount())
                .termMonths(dto.getTermMonths())
                .annualInterestRate(dto.getAnnualInterestRate())
                .build();
    }

    private LoanSimulationDTO dtoFrom(LoanSimulation entity) {
        LoanSimulationDTO dto = new LoanSimulationDTO();
        dto.setAmount(entity.getAmount());
        dto.setTermMonths(entity.getTermMonths());
        dto.setAnnualInterestRate(entity.getAnnualInterestRate());
        dto.setMonthlyPayment(entity.getMonthlyPayment());
        dto.setStatus(entity.getStatus());
        return dto;
    }

    /** Stubs the full save flow: toEntity maps the input, save echoes the entity, toDto maps back. */
    private void mockSaveFlow() {
        when(mapper.toEntity(any(LoanSimulationDTO.class)))
                .thenAnswer(invocation -> entityFrom(invocation.getArgument(0)));
        when(repository.save(any(LoanSimulation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toDto(any(LoanSimulation.class)))
                .thenAnswer(invocation -> dtoFrom(invocation.getArgument(0)));
    }

    private LoanSimulationDTO inputOf(String amount, int termMonths, String rate) {
        LoanSimulationDTO input = new LoanSimulationDTO();
        input.setAmount(new BigDecimal(amount));
        input.setTermMonths(termMonths);
        input.setAnnualInterestRate(new BigDecimal(rate));
        return input;
    }

    // -------------------------------------------------------------------------
    // save(): French amortization + DRAFT status
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save computes the French monthly payment, marks the simulation as DRAFT and persists it")
    void saveComputesPaymentAndPersistsDraft() {
        LoanSimulationDTO input = inputOf("150000.00", 60, "12.2500");
        LoanSimulation mapped = entityFrom(input);
        LoanSimulation persisted = LoanSimulation.builder()
                .amount(mapped.getAmount())
                .termMonths(mapped.getTermMonths())
                .annualInterestRate(mapped.getAnnualInterestRate())
                .monthlyPayment(new BigDecimal("3355.65"))
                .status(SimulationStatus.DRAFT)
                .build();
        LoanSimulationDTO result = dtoFrom(persisted);

        when(mapper.toEntity(input)).thenReturn(mapped);
        when(repository.save(any(LoanSimulation.class))).thenReturn(persisted);
        when(mapper.toDto(persisted)).thenReturn(result);

        LoanSimulationDTO actual = service.save(input);

        assertThat(actual).isNotNull();
        assertThat(actual.getMonthlyPayment()).isEqualByComparingTo("3355.65");
        assertThat(actual.getStatus()).isEqualTo(SimulationStatus.DRAFT);

        // The entity that reached the repository carries the computed values.
        ArgumentCaptor<LoanSimulation> savedEntity = ArgumentCaptor.forClass(LoanSimulation.class);
        verify(repository).save(savedEntity.capture());
        assertThat(savedEntity.getValue().getMonthlyPayment()).isEqualByComparingTo("3355.65");
        assertThat(savedEntity.getValue().getStatus()).isEqualTo(SimulationStatus.DRAFT);

        verify(mapper).toEntity(input);
        verify(mapper).toDto(persisted);
    }

    @Test
    @DisplayName("save with a 0% promotional rate divides the amount evenly across the term")
    void saveWithZeroRateDividesAmountAcrossTerm() {
        mockSaveFlow();

        LoanSimulationDTO actual = service.save(inputOf("150000.00", 60, "0.0000"));

        assertThat(actual.getMonthlyPayment()).isEqualByComparingTo("2500.00");
        assertThat(actual.getStatus()).isEqualTo(SimulationStatus.DRAFT);
    }

    @Test
    @DisplayName("save rounds the payment to 2 decimals with HALF_EVEN (banker's rounding)")
    void saveRoundsPaymentHalfEven() {
        mockSaveFlow();

        // 250,000.50 @ 14.99% for 48 months -> 6956.4337768... -> 6956.43
        LoanSimulationDTO actual = service.save(inputOf("250000.50", 48, "14.9900"));

        assertThat(actual.getMonthlyPayment()).isEqualByComparingTo("6956.43");
        assertThat(actual.getStatus()).isEqualTo(SimulationStatus.DRAFT);
    }

    // -------------------------------------------------------------------------
    // Inherited CRUD behavior, checked against this module's types
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findById throws ResourceNotFoundException when the id does not exist")
    void findByIdThrowsWhenMissing() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository).findById(unknownId);
        verify(mapper, never()).toDto(any(LoanSimulation.class));
    }

    @Test
    @DisplayName("findById returns the mapped DTO when the id exists")
    void findByIdReturnsDtoWhenFound() {
        UUID id = UUID.randomUUID();
        LoanSimulation entity = LoanSimulation.builder()
                .amount(new BigDecimal("150000.00"))
                .termMonths(60)
                .annualInterestRate(new BigDecimal("12.2500"))
                .monthlyPayment(new BigDecimal("3355.65"))
                .status(SimulationStatus.DRAFT)
                .build();
        LoanSimulationDTO dto = dtoFrom(entity);

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(mapper.toDto(entity)).thenReturn(dto);

        LoanSimulationDTO actual = service.findById(id);

        assertThat(actual.getMonthlyPayment()).isEqualByComparingTo("3355.65");
        assertThat(actual.getStatus()).isEqualTo(SimulationStatus.DRAFT);
        verify(mapper).toDto(entity);
    }
}
