-- Orders remember the KHR-per-USD rate used when a riel amount was worked out for them (cash paid or
-- change given in KHR, or a KHR KHQR), so receipts show the riel total exactly as it was charged.
-- Hibernate (ddl-auto: update) adds this column by itself; this file is for databases updated by hand.
-- Existing orders keep NULL, which simply means "no riel rate recorded". Safe to run more than once.

ALTER TABLE orders ADD COLUMN IF NOT EXISTS exchange_rate numeric(15, 4);
