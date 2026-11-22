package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.model.Vaga;
import com.zg.acelera.msnucleo.service.VagaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vagas")
public class VagaController {

    private final VagaService vagaService;

    public VagaController(VagaService vagaService) {
        this.vagaService = vagaService;
    }

    public record CadastroVagaRequest(
            String titulo,
            String pais,
            String estado,
            String descricao,
            String cnpjEmpresa
    ) {
    }

    public record AtualizacaoVagaRequest(
            String titulo,
            String pais,
            String estado,
            String descricao
    ) {
    }

    public record VagaResponse(
            Long idVaga,
            String titulo,
            String pais,
            String estado,
            String descricao
    ) {
    }


    @GetMapping
    public ResponseEntity<List<VagaResponse>> listarTodas() {

        List<VagaResponse> vagas = vagaService.listarTodas()
                .stream()
                .map(this::paraResposta)
                .toList();

        return ResponseEntity.ok(vagas);
    }


    @GetMapping("/{idVaga}")
    public ResponseEntity<VagaResponse> buscarPorId(
            @PathVariable Long idVaga
    ) {

        Vaga vaga = vagaService.buscarPorId(idVaga);

        return ResponseEntity.ok(paraResposta(vaga));
    }


    @GetMapping("/empresa/{cnpj}")
    public ResponseEntity<List<VagaResponse>> listarPorEmpresa(
            @PathVariable String cnpj
    ) {

        List<VagaResponse> vagas = vagaService.listarPorEmpresa(cnpj)
                .stream()
                .map(this::paraResposta)
                .toList();

        return ResponseEntity.ok(vagas);
    }


    @PostMapping
    public ResponseEntity<VagaResponse> cadastrar(
            @RequestBody CadastroVagaRequest dados
    ) {

        Vaga vaga = vagaService.cadastrar(
                dados.titulo(),
                dados.pais(),
                dados.estado(),
                dados.descricao(),
                dados.cnpjEmpresa()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(paraResposta(vaga));
    }

    @PutMapping("/{idVaga}")
    public ResponseEntity<VagaResponse> atualizar(
            @PathVariable Long idVaga,
            @RequestParam String cnpjEmpresa,
            @RequestBody AtualizacaoVagaRequest dados
    ) {

        Vaga vaga = vagaService.atualizar(
                idVaga,
                cnpjEmpresa,
                dados.titulo(),
                dados.pais(),
                dados.estado(),
                dados.descricao()
        );

        return ResponseEntity.ok(paraResposta(vaga));
    }

    @DeleteMapping("/{idVaga}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long idVaga,
            @RequestParam String cnpjEmpresa
    ) {

        vagaService.excluir(idVaga, cnpjEmpresa);

        return ResponseEntity.noContent().build();
    }

    private VagaResponse paraResposta(Vaga vaga) {

        return new VagaResponse(
                vaga.getIdVaga(),
                vaga.getTitulo(),
                vaga.getPais(),
                vaga.getEstado(),
                vaga.getDescricao()
        );
    }
}