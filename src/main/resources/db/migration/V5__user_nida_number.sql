-- Add NIDA number to users for registration (full name, photo, NIDA, phone).
ALTER TABLE users ADD COLUMN nida_number VARCHAR(20);
