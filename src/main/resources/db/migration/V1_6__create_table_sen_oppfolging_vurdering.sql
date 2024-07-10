CREATE TABLE SEN_OPPFOLGING_VURDERING (
    id                 SERIAL PRIMARY KEY,
    uuid               CHAR(36)    NOT NULL UNIQUE,
    kandidat_id        INTEGER REFERENCES SEN_OPPFOLGING_KANDIDAT (id) ON DELETE CASCADE,
    created_at         timestamptz NOT NULL,
    veilederident      VARCHAR(7) NOT NULL,
    status             VARCHAR(20) NOT NULL,
    published_at       timestamptz
);

CREATE INDEX IX_SEN_OPPFOLGING_VURDERING_KANDIDAT_ID on SEN_OPPFOLGING_VURDERING (kandidat_id);
