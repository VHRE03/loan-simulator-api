package com.vhre.loansimulator.modules.loan.service;

import com.vhre.base.core.base.service.BaseServiceImpl;
import com.vhre.loansimulator.modules.loan.dto.LoanSimulationDTO;
import com.vhre.loansimulator.modules.loan.entity.LoanSimulation;
import com.vhre.loansimulator.modules.loan.enums.SimulationStatus;
import com.vhre.loansimulator.modules.loan.mapper.LoanSimulationMapper;
import com.vhre.loansimulator.modules.loan.repository.LoanSimulationRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.UUID;

/**
 * Business logic for loan simulations (módulo {@code loan}).
 *
 * <p>Extends the internal starter CRUD but customizes {@link #save}: a
 * simulation is always created as {@link SimulationStatus#DRAFT} and the fixed
 * monthly payment is computed server-side with the French amortization
 * system. Both {@code monthly_payment} and {@code status} are NOT NULL in the
 * database and READ_ONLY in the DTO, so a plain inherited save would fail.</p>
 */
@Service
public class LoanSimulationServiceImpl extends BaseServiceImpl<LoanSimulation, LoanSimulationDTO, UUID>
        implements LoanSimulationService {

    /** 20 significant digits for intermediate math: plenty for NUMERIC(18,2) money. */
    private static final MathContext MATH = new MathContext(20, RoundingMode.HALF_EVEN);

    /** Final scale of money values (column: monthly_payment NUMERIC(18,2)). */
    private static final int MONEY_SCALE = 2;

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal TWELVE = BigDecimal.valueOf(12);

    public LoanSimulationServiceImpl(LoanSimulationRepository repository, LoanSimulationMapper mapper) {
        super(repository, mapper);
    }

    /**
     * Creates a new loan simulation: computes the fixed monthly payment
     * (French amortization), forces the {@code DRAFT} status and persists it.
     *
     * <p>Same flow as the inherited implementation (toEntity → save → toDto,
     * see {@code pattern.ServiceTestPatternTest}), only enriched with the
     * domain logic before persisting.</p>
     */
    @Override
    public LoanSimulationDTO save(LoanSimulationDTO dto) {
        LoanSimulation entity = mapper.toEntity(dto);

        entity.setMonthlyPayment(
                computeMonthlyPayment(entity.getAmount(), entity.getTermMonths(), entity.getAnnualInterestRate()));
        entity.setStatus(SimulationStatus.DRAFT);

        LoanSimulation saved = repository.save(entity);
        return mapper.toDto(saved);
    }

    /**
     * Fixed monthly payment of the French amortization system:
     *
     * <pre>P * i / (1 - (1 + i)^(-n))</pre>
     *
     * where {@code i = annualRate / 100 / 12} (monthly effective rate) and
     * {@code n = termMonths}. A 0% promotional rate is allowed and degrades
     * to a simple {@code amount / term} division.
     *
     * <p>Money is rounded to scale 2 with {@code HALF_EVEN} (banker's
     * rounding), the standard for financial calculations.</p>
     */
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
