package com.zg.acelera.msnucleo.repository;

import com.zg.acelera.msnucleo.model.Curtida;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CurtidaRepository extends JpaRepository<Curtida, Long> {

    List<Curtida> findByCandidato_Cpf(String cpf);

    List<Curtida> findByVaga_IdVaga(Long idVaga);

    List<Curtida> findByEmpresa_Cnpj(String cnpjEmpresa);

    // Verifica se o candidato já curtiu determinada vaga.
    boolean existsByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
            String cpf,
            Long idVaga
    );

    // Verifica se a empresa já curtiu o candidato para determinada vaga.
    boolean existsByCandidato_CpfAndEmpresa_CnpjAndVaga_IdVaga(
            String cpf,
            String cnpjEmpresa,
            Long idVaga
    );

    Optional<Curtida> findByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
            String cpf,
            Long idVaga
    );

    Optional<Curtida> findByCandidato_CpfAndEmpresa_CnpjAndVaga_IdVaga(
            String cpf,
            String cnpjEmpresa,
            Long idVaga
    );
}