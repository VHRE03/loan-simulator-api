package com.vhre.loansimulator.modules.loan.service;

import com.vhre.base.core.base.service.BaseServiceImpl;
import com.vhre.base.core.exceptions.ResourceNotFoundException;
import com.vhre.loansimulator.modules.customer.entity.Customer;
import com.vhre.loansimulator.modules.customer.repository.CustomerRepository;
import com.vhre.loansimulator.modules.loan.dto.LoanSimulationDTO;
import com.vhre.loansimulator.modules.loan.entity.LoanSimulation;
import com.vhre.loansimulator.modules.loan.enums.SimulationStatus;
import com.vhre.loansimulator.modules.loan.exception.InvalidSimulationStatusException;
import com.vhre.loansimulator.modules.loan.mapper.LoanSimulationMapper;
import com.vhre.loansimulator.modules.loan.repository.LoanSimulationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

@Service
public class LoanSimulationServiceImpl extends BaseServiceImpl<LoanSimulation, LoanSimulationDTO, UUID> implements LoanSimulationService {

    /**
     * 20 significant digits for intermediate math: plenty for NUMERIC(18,2) money.
     */
    private static final MathContext MATH = new MathContext(20, RoundingMode.HALF_EVEN);

    /**
     * Final scale of money values (column: monthly_payment NUMERIC(18,2)).
     */
    private static final int MONEY_SCALE = 2;

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    private final CustomerRepository customerRepository;

    public LoanSimulationServiceImpl(LoanSimulationRepository repository,
                                     LoanSimulationMapper mapper,
                                     CustomerRepository customerRepository) {
        super(repository, mapper);
        this.customerRepository = customerRepository;
    }

    @Override
    public LoanSimulationDTO save(LoanSimulationDTO dto) {
        LoanSimulation entity = mapper.toEntity(dto);

        applyCustomer(dto.getCustomerId(), entity);
        entity.setMonthlyPayment(computeMonthlyPayment(entity.getAmount(), entity.getTermMonths(), entity.getAnnualInterestRate()));
        entity.setStatus(SimulationStatus.DRAFT);

        LoanSimulation saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    @Override
    public LoanSimulationDTO update(UUID id, LoanSimulationDTO dto) {
        LoanSimulation entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("LoanSimulation not found with id: " + id));

        mapper.updateEntityFromDto(dto, entity);
        applyCustomer(dto.getCustomerId(), entity);
        entity.setMonthlyPayment(computeMonthlyPayment(entity.getAmount(), entity.getTermMonths(), entity.getAnnualInterestRate()));

        LoanSimulation saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    @Override
    public LoanSimulationDTO completeSimulation(UUID id) {
        LoanSimulation entity = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("LoanSimulation not found with id: " + id));

        if (entity.getStatus() == SimulationStatus.EXPIRED) {
            throw new InvalidSimulationStatusException("An EXPIRED simulation cannot be completed. Simulation id: " + id);
        }

        entity.setStatus(SimulationStatus.COMPLETED);

        LoanSimulation saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    private void applyCustomer(UUID customerId, LoanSimulation entity) {
        if (customerId == null) {
            return;
        }
        Customer customer = customerRepository.findById(customerId).orElseThrow(() -> new ResourceNotFoundException("Customer not found with id: " + customerId));
        entity.setCustomer(customer);
    }

    BigDecimal computeMonthlyPayment(BigDecimal amount, Integer termMonths, BigDecimal annualInterestRate) {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(termMonths, "termMonths must not be null");
        Objects.requireNonNull(annualInterestRate, "annualInterestRate must not be null");

        BigDecimal monthlyRate = annualInterestRate.divide(ONE_HUNDRED, MATH)
                .divide(TWELVE, MATH);

        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return amount.divide(BigDecimal.valueOf(termMonths), MONEY_SCALE, RoundingMode.HALF_EVEN);
        }

        BigDecimal growthFactor = BigDecimal.ONE.add(monthlyRate).pow(termMonths, MATH);
        BigDecimal denominator = BigDecimal.ONE.subtract(BigDecimal.ONE.divide(growthFactor, MATH));

        return amount.multiply(monthlyRate, MATH)
                .divide(denominator, MONEY_SCALE, RoundingMode.HALF_EVEN);
    }
}
