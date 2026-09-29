-- =============================================================================
-- V3__link_loan_simulations_to_customers.sql
-- =============================================================================
-- PURPOSE:
--   Optional relation loan_simulations.customer_id -> customers.id.
--   Anonymous simulations are allowed (customer_id NULL): the link is only
--   present when the client provides a customerId when creating/updating a
--   simulation. Existing rows created before this migration stay NULL.
--
-- DESIGN NOTES:
--   * Nullable FK: a simulation does not require a registered customer.
--   * The service layer validates that the referenced customer exists
--     (404 ResourceNotFoundException) before persisting.
--   * ON DELETE: no CASCADE. A customer with simulations is soft-deleted
--     (is_deleted = true), never physically removed, so the FK never breaks.
-- =============================================================================

ALTER TABLE loan_simulations
    ADD COLUMN customer_id UUID NULL;

ALTER TABLE loan_simulations
    ADD CONSTRAINT fk_loan_simulations_customer_id
        FOREIGN KEY (customer_id) REFERENCES customers (id);

CREATE INDEX ix_loan_simulations_customer_id ON loan_simulations (customer_id);

COMMENT ON COLUMN loan_simulations.customer_id
    IS 'Optional link to the borrower profile (customers.id). NULL = anonymous simulation.';
