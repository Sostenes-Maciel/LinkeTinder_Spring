package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.client.CompetenciasClient;
import com.zg.acelera.msnucleo.dto.CompetenciaResponseDTO;
import com.zg.acelera.msnucleo.dto.ConsultaCompetenciasLoteRequestDTO;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/integracao/competencias")
public class CompetenciasIntegracaoController {

    private final CompetenciasClient competenciasClient;

    public CompetenciasIntegracaoController(
            CompetenciasClient competenciasClient
    ) {
        this.competenciasClient = competenciasClient;
    }

    @PostMapping("/candidatos/consulta-lote")
    public ResponseEntity<Map<String, List<CompetenciaResponseDTO>>>
    consultarCandidatosEmLote(
            @RequestBody ConsultaCompetenciasLoteRequestDTO request
    ) {
        if (request == null) {
            throw new IllegalArgumentException(
                    "Os dados da consulta são obrigatórios."
            );
        }

        return ResponseEntity.ok(
                competenciasClient.consultarPorCandidatosEmLote(
                        request.cpfs()
                )
        );
    }

    @GetMapping("/candidatos/{cpf}/competencias")
    public ResponseEntity<List<CompetenciaResponseDTO>>
    listarCompetenciasDoCandidato(@PathVariable String cpf) {

        return ResponseEntity.ok(
                competenciasClient.listarPorCandidato(cpf)
        );
    }

    @GetMapping("/empresas/{cnpj}/competencias")
    public ResponseEntity<List<CompetenciaResponseDTO>>
    listarCompetenciasDaEmpresa(@PathVariable String cnpj) {

        return ResponseEntity.ok(
                competenciasClient.listarPorEmpresa(cnpj)
        );
    }

    @GetMapping("/vagas/{idVaga}/competencias")
    public ResponseEntity<List<CompetenciaResponseDTO>>
    listarCompetenciasDaVaga(@PathVariable Long idVaga) {

        return ResponseEntity.ok(
                competenciasClient.listarPorVaga(idVaga)
        );
    }
}