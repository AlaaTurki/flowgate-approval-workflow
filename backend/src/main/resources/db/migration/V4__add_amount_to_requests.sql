-- Add amount column to requests
ALTER TABLE requests
ADD COLUMN amount numeric(12,2);
