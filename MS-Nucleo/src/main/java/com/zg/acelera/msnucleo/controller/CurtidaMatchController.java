package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.model.Candidato;
import com.zg.acelera.msnucleo.model.Curtida;
import com.zg.acelera.msnucleo.model.Empresa;
import com.zg.acelera.msnucleo.model.MatchVaga;
import com.zg.acelera.msnucleo.model.Vaga;
import com.zg.acelera.msnucleo.service.CurtidaService;
import com.zg.acelera.msnucleo.service.MatchVagaService;
import com.zg.acelera.msnucleo.service.VagaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class CurtidaMatchController {

    private final CurtidaService curtidaService;
    private final MatchVagaService matchVagaService;
    private final VagaService vagaService;

    public CurtidaMatchController(
            CurtidaService curtidaService,
            MatchVagaService matchVagaService,
            VagaService vagaService
    ) {
        this.curtidaService = curtidaService;
        this.matchVagaService = matchVagaService;
        this.vagaService = vagaService;
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

    public record StatusMatchCandidatoResponse(
            Long idVaga,
            String status,
            String nomeEmpresa
    ) {
    }

    public record StatusMatchEmpresaResponse(
            String cpfCandidato,
            Long idVaga,
            String status
    ) {
    }

    public record VagaResumoResponse(
            Long id,
            String titulo,
            String pais,
            String estado
    ) {
    }

    public record EmpresaResumoResponse(
            String nome
    ) {
    }

    public record CandidatoResumoResponse(
            String nome
    ) {
    }

    public record RecrutadorResumoResponse(
            String nome
    ) {
    }

    public record MatchCandidatoResponse(
            Long idMatch,
            EmpresaResumoResponse empresa,
            VagaResumoResponse vaga
    ) {
    }

    public record MatchEmpresaResponse(
            Long idMatch,
            CandidatoResumoResponse candidato,
            VagaResumoResponse vaga
    ) {
    }

    public record CurtidaPendenteCandidatoResponse(
            VagaResumoResponse vaga
    ) {
    }

    public record CurtidaPendenteEmpresaResponse(
            CandidatoResumoResponse candidato,
            VagaResumoResponse vaga,
            RecrutadorResumoResponse recrutador
    ) {
    }

    @PostMapping("/curtidas")
    @Transactional
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
    @Transactional
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

    @GetMapping("/matches")
    @Transactional(readOnly = true)
    public ResponseEntity<?> listarMatches(
            @RequestParam(required = false) String cpf,
            @RequestParam(required = false) String cnpj,
            @RequestParam(defaultValue = "false") boolean completo
    ) {
        if (cpf != null && !cpf.isBlank()) {

            if (completo) {
                return ResponseEntity.ok(
                        listarMatchesCompletosCandidato(cpf)
                );
            }

            return ResponseEntity.ok(
                    listarStatusCandidato(cpf)
            );
        }

        if (cnpj != null && !cnpj.isBlank()) {

            if (completo) {
                return ResponseEntity.ok(
                        listarMatchesCompletosEmpresa(cnpj)
                );
            }

            return ResponseEntity.ok(
                    listarStatusEmpresa(cnpj)
            );
        }

        return ResponseEntity.badRequest().body(
                Map.of(
                        "mensagem",
                        "Informe o CPF do candidato ou o CNPJ da empresa."
                )
        );
    }

    private List<StatusMatchCandidatoResponse> listarStatusCandidato(
            String cpf
    ) {
        List<MatchVaga> matches =
                matchVagaService.listarPorCandidato(cpf);

        Map<Long, MatchVaga> matchesPorVaga = new HashMap<>();

        for (MatchVaga match : matches) {
            matchesPorVaga.put(
                    match.getVaga().getIdVaga(),
                    match
            );
        }

        List<StatusMatchCandidatoResponse> resultado =
                new ArrayList<>();

        for (Curtida curtida : curtidaService.listarPorCandidato(cpf)) {

            if (curtida.getEmpresa() != null || curtida.getVaga() == null) {
                continue;
            }

            Long idVaga = curtida.getVaga().getIdVaga();
            MatchVaga match = matchesPorVaga.get(idVaga);

            resultado.add(
                    new StatusMatchCandidatoResponse(
                            idVaga,
                            match != null ? "match" : "curtido",
                            match != null && match.getEmpresa() != null
                                    ? match.getEmpresa().getNome()
                                    : null
                    )
            );
        }

        return resultado;
    }

    private List<StatusMatchEmpresaResponse> listarStatusEmpresa(
            String cnpj
    ) {
        List<MatchVaga> matches =
                matchVagaService.listarPorEmpresa(cnpj);

        Set<String> matchesExistentes = new HashSet<>();

        for (MatchVaga match : matches) {
            matchesExistentes.add(
                    chaveMatch(
                            match.getCandidato().getCpf(),
                            match.getVaga().getIdVaga()
                    )
            );
        }

        List<StatusMatchEmpresaResponse> resultado =
                new ArrayList<>();

        for (Curtida curtida : curtidaService.listarPorEmpresa(cnpj)) {

            if (curtida.getCandidato() == null || curtida.getVaga() == null) {
                continue;
            }

            String cpf = curtida.getCandidato().getCpf();
            Long idVaga = curtida.getVaga().getIdVaga();

            resultado.add(
                    new StatusMatchEmpresaResponse(
                            cpf,
                            idVaga,
                            matchesExistentes.contains(chaveMatch(cpf, idVaga))
                                    ? "match"
                                    : "curtido"
                    )
            );
        }

        return resultado;
    }

    private Map<String, Object> listarMatchesCompletosCandidato(
            String cpf
    ) {
        List<MatchVaga> matches =
                matchVagaService.listarPorCandidato(cpf);

        Set<Long> vagasComMatch = new HashSet<>();

        List<MatchCandidatoResponse> matchesResponse =
                matches.stream()
                        .map(match -> {
                            vagasComMatch.add(
                                    match.getVaga().getIdVaga()
                            );

                            return paraMatchCandidato(match);
                        })
                        .toList();

        List<CurtidaPendenteCandidatoResponse> curtidasPendentes =
                new ArrayList<>();

        for (Curtida curtida : curtidaService.listarPorCandidato(cpf)) {

            if (curtida.getEmpresa() != null || curtida.getVaga() == null) {
                continue;
            }

            Vaga vaga = curtida.getVaga();

            if (!vagasComMatch.contains(vaga.getIdVaga())) {
                curtidasPendentes.add(
                        new CurtidaPendenteCandidatoResponse(
                                paraVagaResumo(vaga)
                        )
                );
            }
        }

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("matches", matchesResponse);
        resposta.put("curtidasPendentes", curtidasPendentes);

        return resposta;
    }

    private Map<String, Object> listarMatchesCompletosEmpresa(
            String cnpj
    ) {
        List<MatchVaga> matches =
                matchVagaService.listarPorEmpresa(cnpj);

        List<MatchEmpresaResponse> matchesResponse =
                matches.stream()
                        .map(this::paraMatchEmpresa)
                        .toList();

        Set<String> matchesExistentes = new HashSet<>();

        for (MatchVaga match : matches) {
            matchesExistentes.add(
                    chaveMatch(
                            match.getCandidato().getCpf(),
                            match.getVaga().getIdVaga()
                    )
            );
        }

        List<CurtidaPendenteEmpresaResponse> curtidasPendentes =
                new ArrayList<>();

        Set<String> curtidasProcessadas = new HashSet<>();

        List<Vaga> vagasEmpresa = vagaService.listarPorEmpresa(cnpj);

        for (Vaga vaga : vagasEmpresa) {

            Long idVaga = vaga.getIdVaga();

            for (Curtida curtida : curtidaService.listarPorVaga(idVaga)) {

                if (curtida.getEmpresa() != null || curtida.getCandidato() == null) {
                    continue;
                }

                String cpf = curtida.getCandidato().getCpf();
                String chave = chaveMatch(cpf, idVaga);

                if (
                        matchesExistentes.contains(chave)
                                || !curtidasProcessadas.add(chave)
                ) {
                    continue;
                }

                curtidasPendentes.add(
                        new CurtidaPendenteEmpresaResponse(
                                new CandidatoResumoResponse(null),
                                paraVagaResumo(vaga),
                                new RecrutadorResumoResponse(null)
                        )
                );
            }
        }

        Map<String, Object> resposta = new HashMap<>();
        resposta.put("matches", matchesResponse);
        resposta.put("curtidasPendentes", curtidasPendentes);

        return resposta;
    }

    private MatchCandidatoResponse paraMatchCandidato(
            MatchVaga match
    ) {
        Empresa empresa = match.getEmpresa();

        return new MatchCandidatoResponse(
                match.getIdMatch(),
                new EmpresaResumoResponse(empresa.getNome()),
                paraVagaResumo(match.getVaga())
        );
    }

    private MatchEmpresaResponse paraMatchEmpresa(
            MatchVaga match
    ) {
        Candidato candidato = match.getCandidato();

        String nomeCompleto = (
                candidato.getNome()
        ).trim();

        return new MatchEmpresaResponse(
                match.getIdMatch(),
                new CandidatoResumoResponse(nomeCompleto),
                paraVagaResumo(match.getVaga())
        );
    }

    private VagaResumoResponse paraVagaResumo(Vaga vaga) {
        return new VagaResumoResponse(
                vaga.getIdVaga(),
                vaga.getTitulo(),
                vaga.getPais(),
                vaga.getEstado()
        );
    }

    private String chaveMatch(String cpf, Long idVaga) {
        return cpf + "|" + idVaga;
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