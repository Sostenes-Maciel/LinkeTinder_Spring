package com.zg.acelera.mscompetencias.repository;

import com.zg.acelera.mscompetencias.model.EmpresaCompetencia;
import com.zg.acelera.mscompetencias.model.id.EmpresaCompetenciaId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EmpresaCompetenciaRepository
        extends JpaRepository<EmpresaCompetencia, EmpresaCompetenciaId> {

    @Query("""
            SELECT associacao
            FROM EmpresaCompetencia associacao
            JOIN FETCH associacao.competencia
            WHERE associacao.id.cnpjEmpresa = :cnpj
            ORDER BY associacao.competencia.nome
            """)
    List<EmpresaCompetencia> buscarPorCnpjEmpresa(
            @Param("cnpj") String cnpj
    );
}