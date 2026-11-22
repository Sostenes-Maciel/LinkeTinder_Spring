package com.zg.acelera.mscompetencias.controller;

import com.zg.acelera.mscompetencias.dto.CompetenciaRequest;
import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.service.CompetenciaService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/competencias")
public class CompetenciaController {

    private final CompetenciaService competenciaService;

    public CompetenciaController(CompetenciaService competenciaService) {
        this.competenciaService = competenciaService;
    }

    @PostMapping
    public ResponseEntity<CompetenciaResponse> criar(
            @RequestBody CompetenciaRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Os dados da competência são obrigatórios.");
        }

        Competencia competencia = competenciaService.criar(request.nome());

        URI localizacao = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(competencia.getIdCompetencia())
                .toUri();

        return ResponseEntity
                .created(localizacao)
                .body(CompetenciaResponse.from(competencia));
    }

    @GetMapping
    public ResponseEntity<List<CompetenciaResponse>> listarTodas() {

        List<CompetenciaResponse> competencias = competenciaService
                .listarTodas()
                .stream()
                .map(CompetenciaResponse::from)
                .toList();

        return ResponseEntity.ok(competencias);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompetenciaResponse> buscarPorId(
            @PathVariable Long id) {

        Competencia competencia = competenciaService.buscarPorId(id);

        return ResponseEntity.ok(
                CompetenciaResponse.from(competencia)
        );
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompetenciaResponse> atualizar(
            @PathVariable Long id,
            @RequestBody CompetenciaRequest request) {

        if (request == null) {
            throw new IllegalArgumentException("Os dados da competência são obrigatórios.");
        }

        Competencia competencia = competenciaService.atualizar(
                id,
                request.nome()
        );

        return ResponseEntity.ok(
                CompetenciaResponse.from(competencia)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {

        competenciaService.excluir(id);

        return ResponseEntity.noContent().build();
    }
}