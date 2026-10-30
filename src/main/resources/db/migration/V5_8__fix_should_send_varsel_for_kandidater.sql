UPDATE kartleggingssporsmal_kandidat
SET should_send_varsel = true
WHERE uuid in (
               '49727bb1-77d7-4311-8467-a19ae61ae956',
               '92b45f96-c695-468f-8eee-d29aac11177e',
               '9c7b0ec0-caa8-41ca-9299-509029005812'
    );
