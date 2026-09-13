-- Adds Order.amountTenderedCurrency (USD vs KHR — see CashPaymentRequest.currency) so a cash
-- payment records which currency it was actually tendered in, not just assumed USD. The existing
-- amount_tendered column is unchanged/reused; ddl-auto: update already adds a nullable enum
-- column like this on its own on next boot, but this makes the backfill explicit and repeatable.
--
-- Safe to run more than once. Run against every database that already has this table: local dev
-- DB and production. Run this AFTER deploying the code that adds Order.amountTenderedCurrency.
--
-- Every existing cash order predates KHR-tendered cash, so its amount_tendered was entirely USD.
ALTER TABLE orders ADD COLUMN IF NOT EXISTS amount_tendered_currency VARCHAR(3);
UPDATE orders SET amount_tendered_currency = 'USD'
WHERE amount_tendered IS NOT NULL AND amount_tendered_currency IS NULL;
