package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.dto.CurtidaRequestDTO;
import com.zg.acelera.msnucleo.dto.ResultadoCurtidaResponseDTO;
import com.zg.acelera.msnucleo.model.MatchVaga;
import com.zg.acelera.msnucleo.service.CurtidaService;
import com.zg.acelera.msnucleo.service.MatchVagaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class CurtidaMatchController {

    private final CurtidaService curtidaService;
    private final MatchVagaService matchVagaService;

    public CurtidaMatchController(
            CurtidaService curtidaService,
            MatchVagaService matchVagaService
    ) {
        this.curtidaService = curtidaService;
        this.matchVagaService = matchVagaService;
    }

    @PostMapping("/curtidas")
    public ResponseEntity<ResultadoCurtidaResponseDTO> registrarCurtida(
            @RequestBody CurtidaRequestDTO dados
    ) {

        String autor = normalizarAutor(dados.autor());

        Optional<MatchVaga> match;

        if ("CANDIDATO".equals(autor)) {

            curtidaService.curtirVaga(
                    dados.cpfCandidato(),
                    dados.idVaga()
            );

        } else if ("EMPRESA".equals(autor)) {

            curtidaService.curtirCandidato(
                    dados.cnpjEmpresa(),
                    dados.cpfCandidato(),
                    dados.idVaga()
            );

        } else {
            throw new IllegalArgumentException(
                    "Informe um autor válido: CANDIDATO ou EMPRESA."
            );
        }

        match = matchVagaService.processarPossivelMatch(
                dados.cpfCandidato(),
                dados.idVaga()
        );

        ResultadoCurtidaResponseDTO resposta =
                new ResultadoCurtidaResponseDTO(
                        match.isPresent()
                                ? "Match encontrado com sucesso!"
                                : "Curtida registrada com sucesso.",
                        match.isPresent(),
                        match.map(MatchVaga::getIdMatch).orElse(null)
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(resposta);
    }

    @DeleteMapping("/matches")
    public ResponseEntity<Void> removerCurtida(
            @RequestBody CurtidaRequestDTO dados
    ) {

        String autor = normalizarAutor(dados.autor());

        if ("CANDIDATO".equals(autor)) {

            curtidaService.removerCurtidaCandidato(
                    dados.cpfCandidato(),
                    dados.idVaga()
            );

        } else if ("EMPRESA".equals(autor)) {

            curtidaService.removerCurtidaEmpresa(
                    dados.cnpjEmpresa(),
                    dados.cpfCandidato(),
                    dados.idVaga()
            );

        } else {
            throw new IllegalArgumentException(
                    "Informe um autor válido: CANDIDATO ou EMPRESA."
            );
        }

        return ResponseEntity.noContent().build();
    }

    private String normalizarAutor(String autor) {
        if (autor == null || autor.isBlank()) {
            throw new IllegalArgumentException(
                    "O autor da curtida deve ser informado."
            );
        }

        return autor.trim().toUpperCase(Locale.ROOT);
    }
}