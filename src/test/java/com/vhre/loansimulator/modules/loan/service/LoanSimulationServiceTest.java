package com.vhre.loansimulator.modules.loan.service;

import com.vhre.base.core.exceptions.ResourceNotFoundException;
import com.vhre.loansimulator.modules.customer.entity.Customer;
import com.vhre.loansimulator.modules.customer.repository.CustomerRepository;
import com.vhre.loansimulator.modules.loan.dto.LoanSimulationDTO;
import com.vhre.loansimulator.modules.loan.entity.LoanSimulation;
import com.vhre.loansimulator.modules.loan.enums.SimulationStatus;
import com.vhre.loansimulator.modules.loan.exception.InvalidSimulationStatusException;
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
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link LoanSimulationServiceImpl}, following the reference
 * pattern of {@code pattern.ServiceTestPatternTest}: pure JUnit 5 + Mockito,
 * no database and no Spring context.
 *
 * <p>The mappers/repository are mocked; {@code thenAnswer} simulates the real
 * MapStruct behavior so the tests exercise the actual service flow
 * (toEntity → resolve customer + compute payment + DRAFT → save → toDto).</p>
 */
@ExtendWith(MockitoExtension.class)
class LoanSimulationServiceTest {

    @Mock
    LoanSimulationRepository repository;

    @Mock
    LoanSimulationMapper mapper;

    @Mock
    CustomerRepository customerRepository;

    @InjectMocks
    LoanSimulationServiceImpl service;

    // -------------------------------------------------------------------------
    // Helpers: mimic MapStruct mappings without a Spring context
    // -------------------------------------------------------------------------

    private LoanSimulation entityFrom(LoanSimulationDTO dto) {
        // Mirrors LoanSimulationMapper.toEntity: customer is ignored (service sets it).
        return LoanSimulation.builder()
                .amount(dto.getAmount())
                .termMonths(dto.getTermMonths())
                .annualInterestRate(dto.getAnnualInterestRate())
                .build();
    }

    private LoanSimulationDTO dtoFrom(LoanSimulation entity) {
        // Mirrors LoanSimulationMapper.toDto: customerId is flattened from customer.id.
        LoanSimulationDTO dto = new LoanSimulationDTO();
        dto.setAmount(entity.getAmount());
        dto.setTermMonths(entity.getTermMonths());
        dto.setAnnualInterestRate(entity.getAnnualInterestRate());
        dto.setMonthlyPayment(entity.getMonthlyPayment());
        dto.setStatus(entity.getStatus());
        if (entity.getCustomer() != null) {
            dto.setCustomerId(entity.getCustomer().getId());
        }
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

    private Customer customerOf(UUID id) {
        Customer customer = Customer.builder()
                .firstName("Maria")
                .lastName("Gonzalez Lopez")
                .email("maria.gonzalez@example.com")
                .phoneNumber("+52 55 1234 5678")
                .nationalId("GOLM900101MDFRNR09")
                .dateOfBirth(LocalDate.of(1990, 1, 1))
                .build();
        customer.setId(id);
        return customer;
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

        // No customerId on the input: the simulation is anonymous, no customer lookup.
        verifyNoInteractions(customerRepository);

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
    // save()/update(): customer link
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("save links the simulation to the customer when the DTO carries a valid customerId")
    void saveLinksExistingCustomer() {
        UUID customerId = UUID.randomUUID();
        Customer customer = customerOf(customerId);

        LoanSimulationDTO input = inputOf("150000.00", 60, "12.2500");
        input.setCustomerId(customerId);

        mockSaveFlow();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));

        LoanSimulationDTO actual = service.save(input);

        assertThat(actual.getCustomerId()).isEqualTo(customerId);

        ArgumentCaptor<LoanSimulation> savedEntity = ArgumentCaptor.forClass(LoanSimulation.class);
        verify(repository).save(savedEntity.capture());
        assertThat(savedEntity.getValue().getCustomer()).isSameAs(customer);

        verify(customerRepository).findById(customerId);
    }

    @Test
    @DisplayName("save fails with 404 and persists nothing when the customerId does not exist")
    void saveRejectsUnknownCustomer() {
        UUID unknownCustomerId = UUID.randomUUID();
        LoanSimulationDTO input = inputOf("150000.00", 60, "12.2500");
        input.setCustomerId(unknownCustomerId);

        when(mapper.toEntity(input)).thenReturn(entityFrom(input));
        when(customerRepository.findById(unknownCustomerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(input))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer not found");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("update keeps the current customer when the request does not carry a customerId")
    void updateWithoutCustomerIdKeepsCurrentCustomer() {
        UUID simulationId = UUID.randomUUID();
        UUID currentCustomerId = UUID.randomUUID();
        Customer currentCustomer = customerOf(currentCustomerId);

        LoanSimulation existing = LoanSimulation.builder()
                .amount(new BigDecimal("150000.00"))
                .termMonths(60)
                .annualInterestRate(new BigDecimal("12.2500"))
                .monthlyPayment(new BigDecimal("3355.65"))
                .status(SimulationStatus.DRAFT)
                .customer(currentCustomer)
                .build();

        LoanSimulationDTO update = inputOf("180000.00", 48, "11.5000"); // no customerId

        when(repository.findById(simulationId)).thenReturn(Optional.of(existing));
        // Mirrors the real generated mapper: copies only amount/term/rate; the mapper's
        // @Mapping ignores keep id, audit fields, status and customer untouched.
        doAnswer(invocation -> {
            LoanSimulationDTO source = invocation.getArgument(0);
            LoanSimulation target = invocation.getArgument(1);
            target.setAmount(source.getAmount());
            target.setTermMonths(source.getTermMonths());
            target.setAnnualInterestRate(source.getAnnualInterestRate());
            return null;
        }).when(mapper).updateEntityFromDto(update, existing);
        when(repository.save(existing)).thenReturn(existing);
        // Map at invocation time (like the real MapStruct), AFTER update + recalculation.
        when(mapper.toDto(existing)).thenAnswer(invocation -> dtoFrom(invocation.getArgument(0)));

        LoanSimulationDTO actual = service.update(simulationId, update);

        assertThat(actual.getCustomerId()).isEqualTo(currentCustomerId);
        assertThat(actual.getAmount()).isEqualByComparingTo("180000.00");
        // monthlyPayment is recalculated from the NEW parameters (180000 @ 11.5% / 48).
        assertThat(actual.getMonthlyPayment()).isEqualByComparingTo("4696.02");
        // status is server-managed: the DTO carried null and the DRAFT status survived.
        assertThat(actual.getStatus()).isEqualTo(SimulationStatus.DRAFT);

        verifyNoInteractions(customerRepository);
    }

    @Test
    @DisplayName("update recalculates the monthly payment from the new parameters (anonymous simulation)")
    void updateRecalculatesMonthlyPaymentFromNewParameters() {
        UUID simulationId = UUID.randomUUID();
        LoanSimulation existing = LoanSimulation.builder()
                .amount(new BigDecimal("150000.00"))
                .termMonths(60)
                .annualInterestRate(new BigDecimal("12.2500"))
                .monthlyPayment(new BigDecimal("3355.65"))
                .status(SimulationStatus.DRAFT)
                .build();

        LoanSimulationDTO update = inputOf("100000.00", 24, "0.0000"); // no customerId

        when(repository.findById(simulationId)).thenReturn(Optional.of(existing));
        doAnswer(invocation -> {
            LoanSimulationDTO source = invocation.getArgument(0);
            LoanSimulation target = invocation.getArgument(1);
            target.setAmount(source.getAmount());
            target.setTermMonths(source.getTermMonths());
            target.setAnnualInterestRate(source.getAnnualInterestRate());
            return null;
        }).when(mapper).updateEntityFromDto(update, existing);
        when(repository.save(existing)).thenReturn(existing);
        when(mapper.toDto(existing)).thenAnswer(invocation -> dtoFrom(invocation.getArgument(0)));

        LoanSimulationDTO actual = service.update(simulationId, update);

        // 100000 at 0% promotional rate over 24 months -> simple division.
        assertThat(actual.getMonthlyPayment()).isEqualByComparingTo("4166.67");
        assertThat(actual.getStatus()).isEqualTo(SimulationStatus.DRAFT);
    }

    @Test
    @DisplayName("update fails with 404 when the simulation id does not exist")
    void updateRejectsUnknownSimulation() {
        UUID unknownId = UUID.randomUUID();
        LoanSimulationDTO update = inputOf("100000.00", 24, "10.0000");

        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(unknownId, update))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("LoanSimulation not found");

        verify(repository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // completeSimulation(): lifecycle state machine
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("completeSimulation transitions a DRAFT simulation to COMPLETED and returns the updated DTO")
    void completeSimulationMarksDraftAsCompleted() {
        UUID id = UUID.randomUUID();
        LoanSimulation entity = LoanSimulation.builder()
                .amount(new BigDecimal("150000.00"))
                .termMonths(60)
                .annualInterestRate(new BigDecimal("12.2500"))
                .monthlyPayment(new BigDecimal("3355.65"))
                .status(SimulationStatus.DRAFT)
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenAnswer(invocation -> dtoFrom(invocation.getArgument(0)));

        LoanSimulationDTO actual = service.completeSimulation(id);

        assertThat(actual.getStatus()).isEqualTo(SimulationStatus.COMPLETED);
        assertThat(actual.getMonthlyPayment()).isEqualByComparingTo("3355.65");
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("completeSimulation is idempotent: a COMPLETED simulation stays COMPLETED")
    void completeSimulationIsIdempotentWhenAlreadyCompleted() {
        UUID id = UUID.randomUUID();
        LoanSimulation entity = LoanSimulation.builder()
                .amount(new BigDecimal("150000.00"))
                .termMonths(60)
                .annualInterestRate(new BigDecimal("12.2500"))
                .monthlyPayment(new BigDecimal("3355.65"))
                .status(SimulationStatus.COMPLETED)
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toDto(entity)).thenAnswer(invocation -> dtoFrom(invocation.getArgument(0)));

        LoanSimulationDTO actual = service.completeSimulation(id);

        assertThat(actual.getStatus()).isEqualTo(SimulationStatus.COMPLETED);
    }

    @Test
    @DisplayName("completeSimulation rejects an EXPIRED simulation with 409 and writes nothing")
    void completeSimulationRejectsExpired() {
        UUID id = UUID.randomUUID();
        LoanSimulation entity = LoanSimulation.builder()
                .amount(new BigDecimal("150000.00"))
                .termMonths(60)
                .annualInterestRate(new BigDecimal("12.2500"))
                .monthlyPayment(new BigDecimal("3355.65"))
                .status(SimulationStatus.EXPIRED)
                .build();

        when(repository.findById(id)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> service.completeSimulation(id))
                .isInstanceOf(InvalidSimulationStatusException.class)
                .hasMessageContaining("EXPIRED");

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("completeSimulation fails with 404 when the id does not exist")
    void completeSimulationThrowsWhenMissing() {
        UUID unknownId = UUID.randomUUID();
        when(repository.findById(unknownId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.completeSimulation(unknownId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(repository, never()).save(any());
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
