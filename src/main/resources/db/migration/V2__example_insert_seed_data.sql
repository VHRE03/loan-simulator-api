-- =============================================================================
-- Flyway example migration: V2__example_insert_seed_data.sql
-- =============================================================================
-- PURPOSE:
--   Example of a DATA migration (DML). Flyway versions the SCHEMA (DDL) and the
--   DATA (DML) with the same mechanism: V2 runs automatically after V1 because
--   its version number (2) is greater than the last applied version (1).
--
-- TYPICAL USE CASES FOR DATA MIGRATIONS:
--   - Seed reference/catalog tables (bank codes, states, countries, roles...).
--   - Backfill or transform existing rows after a schema change
--     (e.g. split a column into two, convert formats).
--   - Insert configuration rows the application expects to find.
--
-- GOOD PRACTICES:
--   - Flyway guarantees each migration runs EXACTLY ONCE, so you do NOT need
--     "IF NOT EXISTS" or "WHERE NOT EXISTS" guards. Keep it simple and
--     deterministic: the same input must always produce the same output.
--   - Always write the explicit column list (never INSERT INTO t VALUES ...).
--   - Hardcode UUIDs for seed rows so the data is identical in every
--     environment (never call random functions for primary keys).
--   - Spring Data auditing does NOT run here (no application is executing this
--     file), so audit columns must be filled manually.
-- =============================================================================

INSERT INTO example_items (id, name, description, is_active, created_at, updated_at, is_deleted)
VALUES ('0b8a6a33-4c14-4d9e-9f6f-1a2b3c4d5e6f',
        'Reference item',
        'Row inserted by the V2 example migration to document DML conventions',
        TRUE,
        NOW(),
        NOW(),
        FALSE);
