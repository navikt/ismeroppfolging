UPDATE KARTLEGGINGSSPORSMAL_KANDIDAT k
SET varsel_ferdigstilt_at = s.latest_svar_at
FROM (
    SELECT kandidat_id, MAX(svar_at) AS latest_svar_at
    FROM KARTLEGGINGSSPORSMAL_KANDIDAT_STATUSENDRING
    WHERE status = 'SVAR_MOTTATT'
      AND svar_at IS NOT NULL
    GROUP BY kandidat_id
) s
WHERE s.kandidat_id = k.id
  AND k.varsel_ferdigstilt_at IS NULL;
