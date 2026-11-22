package com.zg.acelera.msnucleo.repository;

import com.zg.acelera.msnucleo.model.MatchVaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MatchVagaRepository extends JpaRepository<MatchVaga, Long> {

    List<MatchVaga> findByCandidato_Cpf(String cpf);

    List<MatchVaga> findByVaga_Empresa_Cnpj(String cnpjEmpresa);

    boolean existsByCandidato_CpfAndVaga_IdVagaAndVaga_CnpjEmpresa(
            String cpf,
            Long idVaga,
            String cnpjEmpresa
    );

    Optional<MatchVaga> findByCandidato_CpfAndVaga_IdVagaAndVaga_CnpjEmpresa(
            String cpf,
            Long idVaga,
            String cnpjEmpresa
    );

    Optional<MatchVaga> findByCandidato_CpfAndVaga_Empresa_CnpjAndVaga_IdVaga(
            String cpf,
            String cnpjEmpresa,
            Long idVaga
    );

    boolean existsByCandidato_CpfAndVaga_Empresa_CnpjAndVaga_IdVaga(
            String cpf,
            String cnpjEmpresa,
            Long idVaga
    );
}