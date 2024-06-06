CREATE TABLE SEN_OPPFOLGING_KANDIDAT (
    id                 SERIAL PRIMARY KEY,
    uuid               CHAR(36)    NOT NULL UNIQUE,
    created_at         timestamptz NOT NULL,
    updated_at         timestamptz NOT NULL,
    personident        VARCHAR(11) NOT NULL,
    varsel_at          timestamptz NOT NULL,
    svar_at            timestamptz,
    onsker_oppfolging  VARCHAR(10)
);

CREATE INDEX IX_SEN_OPPFOLGING_KANDIDAT_PERSONIDENT on SEN_OPPFOLGING_KANDIDAT (personident);
