-- First, drop the existing foreign key constraint
ALTER TABLE KARTLEGGINGSSPORSMAL_KANDIDAT
DROP CONSTRAINT kartleggingssporsmal_kandidat_generated_by_stoppunkt_id_fkey;

-- Add the foreign key constraint back with ON DELETE CASCADE
ALTER TABLE KARTLEGGINGSSPORSMAL_KANDIDAT
    ADD CONSTRAINT kartleggingssporsmal_kandidat_generated_by_stoppunkt_id_fkey
        FOREIGN KEY (generated_by_stoppunkt_id)
            REFERENCES KARTLEGGINGSSPORSMAL_STOPPUNKT(id)
            ON DELETE CASCADE;

-- Add UNIQUE constraint to generated_by_stoppunkt_id
ALTER TABLE KARTLEGGINGSSPORSMAL_KANDIDAT
    ADD CONSTRAINT kartleggingssporsmal_kandidat_generated_by_stoppunkt_id_unique
        UNIQUE (generated_by_stoppunkt_id);
