package com.zg.acelera.mscompetencias.repository;

import com.zg.acelera.mscompetencias.model.VagaCompetencia;
import com.zg.acelera.mscompetencias.model.id.VagaCompetenciaId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VagaCompetenciaRepository
        extends JpaRepository<VagaCompetencia, VagaCompetenciaId> {

    @Query("""
            SELECT associacao
            FROM VagaCompetencia associacao
            JOIN FETCH associacao.competencia
            WHERE associacao.id.idVaga = :idVaga
            ORDER BY associacao.competencia.nome
            """)
    List<VagaCompetencia> buscarPorIdVaga(
            @Param("idVaga") Long idVaga
    );
}