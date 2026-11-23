package com.zg.acelera.mscompetencias.controller;

import com.zg.acelera.mscompetencias.dto.AssociarCompetenciasRequest;
import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.service.CandidatoCompetenciaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/candidatos/{cpf}/competencias")
public class CandidatoCompetenciaController {

    private final CandidatoCompetenciaService associacaoService;

    public CandidatoCompetenciaController(
            CandidatoCompetenciaService associacaoService
    ) {
        this.associacaoService = associacaoService;
    }

    @PostMapping
    public ResponseEntity<List<CompetenciaResponse>> associar(
            @PathVariable String cpf,
            @RequestBody AssociarCompetenciasRequest request
    ) {
        List<CompetenciaResponse> competencias =
                associacaoService.associar(
                        cpf,
                        request.competencias()
                );

        return ResponseEntity.ok(competencias);
    }

    @GetMapping
    public ResponseEntity<List<CompetenciaResponse>> listar(
            @PathVariable String cpf
    ) {
        return ResponseEntity.ok(
                associacaoService.listarPorCandidato(cpf)
        );
    }

    @DeleteMapping("/{idCompetencia}")
    public ResponseEntity<Void> remover(
            @PathVariable String cpf,
            @PathVariable Long idCompetencia
    ) {
        associacaoService.removerAssociacao(cpf, idCompetencia);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> removerTodas(
            @PathVariable String cpf
    ) {
        associacaoService.removerTodasPorCandidato(cpf);

        return ResponseEntity.noContent().build();
    }
}