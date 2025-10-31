DELETE FROM sen_oppfolging_vurdering where uuid = '2222f28c-ab10-4a69-890f-6578a1531fd7';

UPDATE sen_oppfolging_kandidat
SET status = 'KANDIDAT',
    published_at = null,
    updated_at = now()
WHERE uuid = '0a4bde78-d317-451e-8bda-87afaab9dcc2';
