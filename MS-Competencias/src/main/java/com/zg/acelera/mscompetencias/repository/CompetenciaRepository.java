package com.zg.acelera.mscompetencias.repository;

import com.zg.acelera.mscompetencias.model.Competencia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CompetenciaRepository extends JpaRepository<Competencia, Long> {

    Optional<Competencia> findByNome(String nome);

}