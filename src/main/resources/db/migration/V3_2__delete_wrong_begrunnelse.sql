DELETE FROM sen_oppfolging_vurdering where uuid = '832ff8dd-2ce1-4981-9562-940238e93f7c';

UPDATE sen_oppfolging_kandidat
SET status = 'KANDIDAT',
    published_at = null,
    updated_at = now()
WHERE uuid = '5820dd28-fcb2-46c1-a0ea-0c46151b9897';