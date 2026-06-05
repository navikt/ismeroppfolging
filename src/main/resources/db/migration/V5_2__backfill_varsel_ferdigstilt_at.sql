UPDATE KARTLEGGINGSSPORSMAL_KANDIDAT
SET varsel_ferdigstilt_at = created_at
WHERE status IN ('SVAR_MOTTATT', 'FERDIGBEHANDLET')
  AND varsel_ferdigstilt_at IS NULL;
