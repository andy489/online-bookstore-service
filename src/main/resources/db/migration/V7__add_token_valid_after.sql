-- Add token_valid_after to support JWT invalidation on password change.
-- All existing users get EPOCH so their current tokens remain valid.
ALTER TABLE users
    ADD COLUMN token_valid_after TIMESTAMPTZ NOT NULL DEFAULT '1970-01-01 00:00:00+00';
