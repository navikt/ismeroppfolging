CREATE TABLE KARTLEGGINGSSPORSMAL_STOPPUNKT (
    id                              SERIAL      PRIMARY KEY,
    uuid                            CHAR(36)    NOT NULL UNIQUE,
    created_at                      timestamptz NOT NULL,
    personident                     CHAR(11)    NOT NULL,
    tilfelle_bit_referanse_uuid     CHAR(36)    NOT NULL,
    stoppunkt_at                    DATE        NOT NULL,
    processed_at                    timestamptz
);

CREATE INDEX IX_KARTLEGGINGSSPORSMAL_STOPPUNKT_PERSONIDENT on KARTLEGGINGSSPORSMAL_STOPPUNKT (personident);

CREATE TABLE KARTLEGGINGSSPORSMAL_KANDIDAT (
    id                          SERIAL      PRIMARY KEY,
    uuid                        CHAR(36)    NOT NULL UNIQUE,
    created_at                  timestamptz NOT NULL,
    personident                 CHAR(11)    NOT NULL,
    generated_by_stoppunkt_id   INTEGER     NOT NULL REFERENCES KARTLEGGINGSSPORSMAL_STOPPUNKT(id),
    status                      TEXT        NOT NULL,
    varslet_at                  timestamptz
);

CREATE INDEX IX_KARTLEGGINGSSPORSMAL_KANDIDAT_PERSONIDENT on KARTLEGGINGSSPORSMAL_KANDIDAT (personident);
CREATE INDEX IX_KARTLEGGINGSSPORSMAL_KANDIDAT_GENERATED_BY_STOPPUNKT_ID on KARTLEGGINGSSPORSMAL_KANDIDAT (generated_by_stoppunkt_id);
