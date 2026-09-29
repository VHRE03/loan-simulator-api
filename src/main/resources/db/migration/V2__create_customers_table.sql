-- =============================================================================
-- V2__create_customers_table.sql
-- =============================================================================
-- PURPOSE:
--   Creates the customers table (módulo customer): basic borrower profiles
--   that loan simulations can link to (see V3) and the data used for age
--   validations (date_of_birth, adult >= 18 rule enforced by bean validation).
--
-- DOCUMENTED DEVIATIONS FROM THE ORIGINAL DRAFT (to keep the starter contract):
--   * id: no DB default. BaseEntity/Hibernate generates the UUID
--     (GenerationType.UUID), exactly like loan_simulations (V1).
--   * created_at / updated_at: TIMESTAMP (not TIMESTAMPTZ) without DB
--     defaults. BaseEntity maps LocalDateTime and Spring Data auditing
--     fills the values at persist time; Hibernate schema validation
--     (spring.jpa.hibernate.ddl-auto=validate) would reject TIMESTAMPTZ.
--   * is_active -> is_deleted: BaseEntity maps the soft-delete flag to
--     is_deleted and applies the global Hibernate filter
--     "is_deleted = false". In this architecture deactivating a customer
--     IS the soft delete (DELETE /api/v1/customers/{id}).
--
-- STANDARD AUDIT COLUMNS (REQUIRED IN EVERY TABLE OF THIS PROJECT):
--   id / created_at / updated_at / is_deleted, see BaseEntity.
-- =============================================================================

CREATE TABLE customers
(
    id            UUID         NOT NULL,               -- PK from BaseEntity (Hibernate-generated)
    first_name    VARCHAR(100) NOT NULL,               -- borrower first name
    last_name     VARCHAR(100) NOT NULL,               -- borrower last name
    email         VARCHAR(255) NOT NULL,               -- unique contact email
    phone_number  VARCHAR(20)  NOT NULL,               -- contact phone
    national_id   VARCHAR(18)  NOT NULL,               -- national identifier (e.g. Mexican CURP, 18 chars)
    date_of_birth DATE         NOT NULL,               -- used by the adult (>= 18) validation
    created_at    TIMESTAMP    NOT NULL,               -- audit: BaseEntity
    updated_at    TIMESTAMP    NULL,                   -- audit: BaseEntity
    is_deleted    BOOLEAN      NOT NULL DEFAULT FALSE, -- audit: soft-delete flag

    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_email UNIQUE (email),
    CONSTRAINT uq_customers_national_id UNIQUE (national_id)
);

-- Index naming convention: ix_<table>_<columns>.
-- (email and national_id already have unique indexes from the uq_ constraints.)
CREATE INDEX ix_customers_is_deleted ON customers (is_deleted);

-- Database documentation, visible to any developer inspecting the schema.
COMMENT ON TABLE customers
    IS 'Borrower profiles. Deactivation is a soft delete (is_deleted = true, BaseEntity filter).';
COMMENT ON COLUMN customers.date_of_birth
    IS 'Used to enforce the adult (>= 18 years) business rule at the API layer.';
