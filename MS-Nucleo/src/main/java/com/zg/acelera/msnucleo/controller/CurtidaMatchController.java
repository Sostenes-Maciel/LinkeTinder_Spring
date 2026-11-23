package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.model.Curtida;
import com.zg.acelera.msnucleo.model.MatchVaga;
import com.zg.acelera.msnucleo.service.CurtidaService;
import com.zg.acelera.msnucleo.service.MatchVagaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    public record CurtidaRequest(
            String autor,
            String cpfCandidato,
            String cnpjEmpresa,
            Long idVaga
    ) {
    }

    public record ResultadoCurtidaResponse(
            String mensagem,
            boolean match,
            Long idMatch
    ) {
    }

    public record StatusMatchVagaResponse(
            Long idVaga,
            String status,
            String nomeEmpresa
    ) {
    }

    @GetMapping("/matches")
    @Transactional(readOnly = true)
    public ResponseEntity<List<StatusMatchVagaResponse>> listarStatusCurtidasCandidato(
            @RequestParam String cpf
    ) {
        List<MatchVaga> matches = matchVagaService.listarPorCandidato(cpf);

        Map<Long, MatchVaga> matchesPorVaga = new HashMap<>();

        for (MatchVaga match : matches) {
            matchesPorVaga.put(
                    match.getVaga().getIdVaga(),
                    match
            );
        }

        List<Curtida> curtidas = curtidaService.listarPorCandidato(cpf);
        List<StatusMatchVagaResponse> resposta = new ArrayList<>();

        for (Curtida curtida : curtidas) {
            if (curtida.getEmpresa() != null) {
                continue;
            }

            Long idVaga = curtida.getVaga().getIdVaga();
            MatchVaga match = matchesPorVaga.get(idVaga);

            resposta.add(
                    new StatusMatchVagaResponse(
                            idVaga,
                            match != null ? "match" : "curtido",
                            match != null ? match.getEmpresa().getNome() : null
                    )
            );
        }

        return ResponseEntity.ok(resposta);
    }

    @PostMapping("/curtidas")
    public ResponseEntity<ResultadoCurtidaResponse> registrarCurtida(
            @RequestBody CurtidaRequest dados
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

        ResultadoCurtidaResponse resposta = new ResultadoCurtidaResponse(
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
            @RequestBody CurtidaRequest dados
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