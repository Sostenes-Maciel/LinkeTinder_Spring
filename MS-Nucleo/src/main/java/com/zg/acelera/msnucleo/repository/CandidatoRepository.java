package com.zg.acelera.msnucleo.repository;

import com.zg.acelera.msnucleo.model.Candidato;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CandidatoRepository extends JpaRepository<Candidato, String> {

    Optional<Candidato> findByEmail(String email);

    boolean existsByEmail(String email);
}