package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.dto.CadastroEmpresaRequestDTO;
import com.zg.acelera.msnucleo.dto.MensagemResponseDTO;
import com.zg.acelera.msnucleo.service.EmpresaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    @PostMapping
    public ResponseEntity<MensagemResponseDTO> cadastrar(
            @RequestBody CadastroEmpresaRequestDTO dados
    ) {

        empresaService.cadastrar(
                dados.cnpj(),
                dados.nome(),
                dados.email(),
                dados.pais(),
                dados.estado(),
                dados.cep(),
                dados.descricao()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new MensagemResponseDTO(
                        "Empresa cadastrada com sucesso."
                ));
    }
}