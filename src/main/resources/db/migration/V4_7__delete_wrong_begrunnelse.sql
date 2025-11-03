DELETE FROM sen_oppfolging_vurdering where uuid = 'dc7a0c76-e1d6-4efa-95eb-7a80534fc6da';

UPDATE sen_oppfolging_kandidat
SET status = 'KANDIDAT',
    published_at = null,
    updated_at = now()
WHERE uuid = '4c6837be-dade-4b3d-85b9-9f55e7fb80bf';
