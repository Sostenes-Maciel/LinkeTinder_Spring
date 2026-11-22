package com.zg.acelera.mscompetencias.repository;

import com.zg.acelera.mscompetencias.model.CandidatoCompetencia;
import com.zg.acelera.mscompetencias.model.id.CandidatoCompetenciaId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CandidatoCompetenciaRepository
        extends JpaRepository<CandidatoCompetencia, CandidatoCompetenciaId> {

    @Query("""
            SELECT associacao
            FROM CandidatoCompetencia associacao
            JOIN FETCH associacao.competencia
            WHERE associacao.id.cpfCandidato = :cpf
            ORDER BY associacao.competencia.nome
            """)
    List<CandidatoCompetencia> buscarPorCpfCandidato(
            @Param("cpf") String cpf
    );

    @Query("""
            SELECT associacao
            FROM CandidatoCompetencia associacao
            JOIN FETCH associacao.competencia
            WHERE associacao.id.cpfCandidato IN :cpfs
            ORDER BY associacao.id.cpfCandidato,
                     associacao.competencia.nome
            """)
    List<CandidatoCompetencia> buscarPorCpfsCandidatos(
            @Param("cpfs") List<String> cpfs
    );
}