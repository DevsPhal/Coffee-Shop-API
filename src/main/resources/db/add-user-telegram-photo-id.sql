-- Remembers which Telegram profile photo an avatar was copied from, so Telegram logins can refresh it
-- without ever replacing a photo that was uploaded by hand (those keep telegram_photo_id NULL).
ALTER TABLE admins ADD COLUMN IF NOT EXISTS telegram_photo_id VARCHAR(128);
ALTER TABLE baristas ADD COLUMN IF NOT EXISTS telegram_photo_id VARCHAR(128);
ALTER TABLE customers ADD COLUMN IF NOT EXISTS telegram_photo_id VARCHAR(128);
