INSERT INTO kartleggingssporsmal_kandidat_statusendring (uuid, kandidat_id, created_at, status)
SELECT gen_random_uuid(), id, created_at, status
FROM kartleggingssporsmal_kandidat k
WHERE NOT EXISTS (
    SELECT 1 FROM kartleggingssporsmal_kandidat_statusendring kps
    WHERE kps.kandidat_id = k.id
);
