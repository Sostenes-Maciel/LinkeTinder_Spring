package com.zg.acelera.mscompetencias.controller;

import com.zg.acelera.mscompetencias.dto.AssociarCompetenciasRequest;
import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.service.EmpresaCompetenciaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/empresas/{cnpj}/competencias")
public class EmpresaCompetenciaController {

    private final EmpresaCompetenciaService associacaoService;

    public EmpresaCompetenciaController(
            EmpresaCompetenciaService associacaoService
    ) {
        this.associacaoService = associacaoService;
    }

    @PostMapping
    public ResponseEntity<List<CompetenciaResponse>> associar(
            @PathVariable String cnpj,
            @RequestBody AssociarCompetenciasRequest request
    ) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Os dados das competências são obrigatórios."
            );
        }

        return ResponseEntity.ok(
                associacaoService.associar(
                        cnpj,
                        request.competencias()
                )
        );
    }

    @GetMapping
    public ResponseEntity<List<CompetenciaResponse>> listar(
            @PathVariable String cnpj
    ) {
        return ResponseEntity.ok(
                associacaoService.listarPorEmpresa(cnpj)
        );
    }

    @DeleteMapping("/{idCompetencia}")
    public ResponseEntity<Void> remover(
            @PathVariable String cnpj,
            @PathVariable Long idCompetencia
    ) {

        associacaoService.removerAssociacao(cnpj, idCompetencia);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> removerTodas(
            @PathVariable String cnpj
    ) {
        associacaoService.removerTodasPorEmpresa(cnpj);

        return ResponseEntity.noContent().build();
    }
}