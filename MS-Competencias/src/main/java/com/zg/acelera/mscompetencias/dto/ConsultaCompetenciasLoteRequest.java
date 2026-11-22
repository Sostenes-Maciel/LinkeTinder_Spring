package com.zg.acelera.mscompetencias.dto;

import java.util.List;

public record ConsultaCompetenciasLoteRequest(
        List<String> cpfs
) {
}