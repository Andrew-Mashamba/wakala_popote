-- Store hashed 4-digit PIN for phone+PIN login.
ALTER TABLE users ADD COLUMN pin_hash VARCHAR(255);
