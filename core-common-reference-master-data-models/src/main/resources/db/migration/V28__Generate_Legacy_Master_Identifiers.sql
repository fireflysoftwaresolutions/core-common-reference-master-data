-- New reactive entities omit their ID so PostgreSQL can return a generated identity.
-- The original legacy master tables predate the UUID defaults used by newer catalogs.
ALTER TABLE countries ALTER COLUMN country_id SET DEFAULT gen_random_uuid();
ALTER TABLE currencies ALTER COLUMN currency_id SET DEFAULT gen_random_uuid();
ALTER TABLE bank_institution_codes ALTER COLUMN institution_id SET DEFAULT gen_random_uuid();
ALTER TABLE language_locale ALTER COLUMN locale_id SET DEFAULT gen_random_uuid();
