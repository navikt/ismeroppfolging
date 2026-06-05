UPDATE KARTLEGGINGSSPORSMAL_KANDIDAT k
SET varsel_ferdigstilt_at = s.svar_at
FROM KARTLEGGINGSSPORSMAL_KANDIDAT_STATUSENDRING s
WHERE s.kandidat_id = k.id
  AND s.status = 'SVAR_MOTTATT'
  AND k.varsel_ferdigstilt_at IS NULL;
