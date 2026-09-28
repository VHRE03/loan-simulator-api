-- Creates the loan_simulations table to store preliminary loan amortization details and status.
CREATE TABLE loan_simulations
(
    id                   UUID           NOT NULL,
    amount               NUMERIC(18, 2) NOT NULL,
    term_months          INTEGER        NOT NULL,
    annual_interest_rate NUMERIC(8, 4)  NOT NULL,
    monthly_payment      NUMERIC(18, 2) NOT NULL,
    status               VARCHAR(30)    NOT NULL,
    created_at           TIMESTAMP      NOT NULL,
    updated_at           TIMESTAMP NULL,
    is_deleted           BOOLEAN        NOT NULL DEFAULT FALSE,
    CONSTRAINT pk_loan_simulations PRIMARY KEY (id),
    CONSTRAINT chk_loan_simulations_status CHECK (status IN ('DRAFT', 'COMPLETED', 'EXPIRED'))
);