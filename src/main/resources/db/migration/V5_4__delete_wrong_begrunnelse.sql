DELETE FROM sen_oppfolging_vurdering where uuid = '4e71d218-3208-4411-8912-1ff3da03b67e';

UPDATE sen_oppfolging_kandidat
SET status = 'KANDIDAT',
    published_at = null,
    updated_at = now()
WHERE uuid = '2dab2f0e-2c74-4e7a-bc59-170c68d6e82e';