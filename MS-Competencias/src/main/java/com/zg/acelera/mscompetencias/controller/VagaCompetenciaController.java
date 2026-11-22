package com.zg.acelera.mscompetencias.controller;

import com.zg.acelera.mscompetencias.dto.AssociarCompetenciasRequest;
import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.service.VagaCompetenciaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vagas/{idVaga}/competencias")
public class VagaCompetenciaController {

    private final VagaCompetenciaService associacaoService;

    public VagaCompetenciaController(
            VagaCompetenciaService associacaoService
    ) {
        this.associacaoService = associacaoService;
    }

    @PostMapping
    public ResponseEntity<List<CompetenciaResponse>> associar(
            @PathVariable Long idVaga,
            @RequestBody AssociarCompetenciasRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Os dados das competências são obrigatórios."
            );
        }

        return ResponseEntity.ok(
                associacaoService.associar(
                        idVaga,
                        request.competencias()
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<CompetenciaResponse>> listar(
            @PathVariable Long idVaga
    ) {
        return ResponseEntity.ok(
                associacaoService.listarPorVaga(idVaga)
        );
    }

    @DeleteMapping("/{idCompetencia}")
    public ResponseEntity<Void> remover(
            @PathVariable Long idVaga,
            @PathVariable Long idCompetencia
    ) {
        associacaoService.removerAssociacao(
                idVaga,
                idCompetencia
        );

        return ResponseEntity.noContent().build();
    }
}