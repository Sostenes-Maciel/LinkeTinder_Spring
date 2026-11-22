package com.zg.acelera.msnucleo.repository;

import com.zg.acelera.msnucleo.model.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    List<Vaga> findByCnpjEmpresa(String cnpjEmpresa);

    Optional<Vaga> findByIdVagaAndCnpjEmpresa(
            Long idVaga,
            String cnpjEmpresa
    );

    boolean existsByIdVagaAndCnpjEmpresa(
            Long idVaga,
            String cnpjEmpresa
    );
}