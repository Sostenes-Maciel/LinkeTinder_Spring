-- Integridade das curtidas: a empresa deve ser proprietária da vaga.

DO $$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'curtida_vaga_empresa_fk'
              AND conrelid = 'public.curtida'::regclass
        ) THEN
            ALTER TABLE public.curtida
                ADD CONSTRAINT curtida_vaga_empresa_fk
                    FOREIGN KEY (id_vaga, cnpj_empresa)
                        REFERENCES public.vaga (id_vaga, cnpj_empresa)
                        ON DELETE CASCADE;
        END IF;
    END $$;

-- Evita curtidas empresariais duplicadas para a mesma combinação.

CREATE UNIQUE INDEX IF NOT EXISTS
    curtida_empresa_candidato_vaga_unica
    ON public.curtida (
                       cpf_candidato,
                       cnpj_empresa,
                       id_vaga
        )
    WHERE cnpj_empresa IS NOT NULL
        AND id_vaga IS NOT NULL;