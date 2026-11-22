package com.zg.acelera.mscompetencias.controller;

import com.zg.acelera.mscompetencias.dto.ConsultaCompetenciasLoteRequest;
import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.service.CandidatoCompetenciaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/competencias/candidatos")
public class CandidatoCompetenciaLoteController {

    private final CandidatoCompetenciaService associacaoService;

    public CandidatoCompetenciaLoteController(
            CandidatoCompetenciaService associacaoService
    ) {
        this.associacaoService = associacaoService;
    }

    @PostMapping("/consulta-lote")
    public ResponseEntity<Map<String, List<CompetenciaResponse>>> consultarEmLote(
            @RequestBody ConsultaCompetenciasLoteRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Os dados da consulta são obrigatórios."
            );
        }

        return ResponseEntity.ok(
                associacaoService.listarPorCandidatos(
                        request.cpfs()
                )
        );
    }
}