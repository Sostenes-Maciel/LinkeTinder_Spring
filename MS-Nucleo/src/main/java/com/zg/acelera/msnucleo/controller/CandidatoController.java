package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.dto.CadastroCandidatoRequestDTO;
import com.zg.acelera.msnucleo.dto.MensagemResponseDTO;
import com.zg.acelera.msnucleo.service.CandidatoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/candidatos")
public class CandidatoController {

    private final CandidatoService candidatoService;

    public CandidatoController(CandidatoService candidatoService) {
        this.candidatoService = candidatoService;
    }

    @PostMapping
    public ResponseEntity<MensagemResponseDTO> cadastrar(
            @RequestBody CadastroCandidatoRequestDTO dados
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
                .body(new MensagemResponseDTO(
                        "Candidato cadastrado com sucesso."
                ));
    }
}