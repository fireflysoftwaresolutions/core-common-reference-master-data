-- V4 removed this cast during a lookup transition; V20 retained direct region enums.
-- Reactive enum writes use varchar, as they do for the existing status_enum cast.
CREATE CAST (varchar AS region_enum) WITH INOUT AS IMPLICIT;

-- Both values are already accepted by the public CountryDTO/RegionEnum contract.
ALTER TYPE region_enum ADD VALUE IF NOT EXISTS 'OCEANIA';
ALTER TYPE region_enum ADD VALUE IF NOT EXISTS 'ASIA';
