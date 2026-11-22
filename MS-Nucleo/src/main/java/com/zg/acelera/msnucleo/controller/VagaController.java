package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.dto.AtualizacaoVagaRequestDTO;
import com.zg.acelera.msnucleo.dto.CadastroVagaRequestDTO;
import com.zg.acelera.msnucleo.dto.VagaResponseDTO;
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

    @GetMapping
    public ResponseEntity<List<VagaResponseDTO>> listarTodas() {

        List<VagaResponseDTO> vagas = vagaService.listarTodas()
                .stream()
                .map(this::paraResposta)
                .toList();

        return ResponseEntity.ok(vagas);
    }

    @GetMapping("/{idVaga}")
    public ResponseEntity<VagaResponseDTO> buscarPorId(
            @PathVariable Long idVaga
    ) {

        Vaga vaga = vagaService.buscarPorId(idVaga);

        return ResponseEntity.ok(paraResposta(vaga));
    }

    @GetMapping("/empresa/{cnpj}")
    public ResponseEntity<List<VagaResponseDTO>> listarPorEmpresa(
            @PathVariable String cnpj
    ) {

        List<VagaResponseDTO> vagas = vagaService.listarPorEmpresa(cnpj)
                .stream()
                .map(this::paraResposta)
                .toList();

        return ResponseEntity.ok(vagas);
    }

    @PostMapping
    public ResponseEntity<VagaResponseDTO> cadastrar(
            @RequestBody CadastroVagaRequestDTO dados
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
    public ResponseEntity<VagaResponseDTO> atualizar(
            @PathVariable Long idVaga,
            @RequestParam String cnpjEmpresa,
            @RequestBody AtualizacaoVagaRequestDTO dados
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

    private VagaResponseDTO paraResposta(Vaga vaga) {

        return new VagaResponseDTO(
                vaga.getIdVaga(),
                vaga.getTitulo(),
                vaga.getPais(),
                vaga.getEstado(),
                vaga.getDescricao()
        );
    }
}