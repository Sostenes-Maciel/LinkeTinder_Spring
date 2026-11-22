package com.zg.acelera.mscompetencias.dto;

import com.zg.acelera.mscompetencias.model.Competencia;

public record CompetenciaResponse(
        Long idCompetencia,
        String nome
) {

    public static CompetenciaResponse from(Competencia competencia) {
        return new CompetenciaResponse(
                competencia.getIdCompetencia(),
                competencia.getNome()
        );
    }
}