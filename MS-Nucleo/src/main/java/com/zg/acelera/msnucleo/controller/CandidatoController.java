package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.model.Candidato;
import com.zg.acelera.msnucleo.service.CandidatoService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/candidatos")
public class CandidatoController {

    private final CandidatoService candidatoService;

    public CandidatoController(CandidatoService candidatoService) {
        this.candidatoService = candidatoService;
    }

    public record CadastroCandidatoRequest(
            String cpf,
            String nome,
            String email,
            String pais,
            String estado,
            String cep,
            String descricao,
            Integer idade
    ) {
    }

    public record AtualizarCandidatoRequest(
            String cpf,
            String nome,
            String email,
            String pais,
            String estado,
            String cep,
            String descricao,
            Integer idade
    ) {
    }

    public record LoginCandidatoRequest(
            String email,
            String cpf
    ) {
    }

    @GetMapping
    public ResponseEntity<List<Candidato>> listarTodos() {
        return ResponseEntity.ok(candidatoService.listarTodos());
    }

    @GetMapping("/{cpf}")
    public ResponseEntity<Candidato> buscarPorCpf(
            @PathVariable String cpf
    ) {
        return ResponseEntity.ok(candidatoService.buscarPorCpf(cpf));
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> cadastrar(
            @RequestBody CadastroCandidatoRequest dados
    ) {
        candidatoService.cadastrar(
                dados.cpf(),
                dados.nome(),
                dados.email(),
                dados.pais(),
                dados.estado(),
                dados.cep(),
                dados.descricao(),
                dados.idade()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of(
                        "mensagem",
                        "Candidato cadastrado com sucesso."
                ));
    }

    @PutMapping
    public ResponseEntity<?> atualizar(
            @RequestBody AtualizarCandidatoRequest dados
    ) {
        try {
            Candidato candidatoAtualizado = candidatoService.atualizar(
                    dados.cpf(),
                    dados.nome(),
                    dados.email(),
                    dados.pais(),
                    dados.estado(),
                    dados.cep(),
                    dados.descricao(),
                    dados.idade()
            );

            return ResponseEntity.ok(candidatoAtualizado);

        } catch (EntityNotFoundException erro) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(Map.of("mensagem", erro.getMessage()));

        } catch (IllegalArgumentException erro) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("mensagem", erro.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginCandidatoRequest dados
    ) {
        if (dados == null ||
                dados.email() == null ||
                dados.cpf() == null ||
                dados.email().isBlank()) {

            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensagem", "E-mail ou CPF incorretos."));
        }

        try {
            Candidato candidato = candidatoService.buscarPorCpf(dados.cpf());

            if (!candidato.getEmail().equalsIgnoreCase(dados.email().trim())) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("mensagem", "E-mail ou CPF incorretos."));
            }

            return ResponseEntity.ok(
                    Map.of("candidato", candidato)
            );

        } catch (IllegalArgumentException | EntityNotFoundException erro) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensagem", "E-mail ou CPF incorretos."));
        }
    }

    @DeleteMapping("/{cpf}")
    public ResponseEntity<Void> excluir(
            @PathVariable String cpf
    ) {
        candidatoService.excluir(cpf);
        return ResponseEntity.noContent().build();
    }
}