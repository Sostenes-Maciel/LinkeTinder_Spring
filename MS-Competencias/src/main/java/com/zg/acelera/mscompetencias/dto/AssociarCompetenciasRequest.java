package com.zg.acelera.mscompetencias.dto;

import java.util.List;

public record AssociarCompetenciasRequest(
        List<String> competencias
) {
}