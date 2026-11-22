package com.zg.acelera.msnucleo.controller;

import com.zg.acelera.msnucleo.service.EmpresaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/empresas")
public class EmpresaController {

    private final EmpresaService empresaService;

    public EmpresaController(EmpresaService empresaService) {
        this.empresaService = empresaService;
    }

    public record CadastroEmpresaRequest(
            String cnpj,
            String nome,
            String email,
            String pais,
            String estado,
            String cep,
            String descricao
    ) {
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> cadastrar(
            @RequestBody CadastroEmpresaRequest dados
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
                .body(Map.of(
                        "mensagem",
                        "Empresa cadastrada com sucesso."
                ));
    }
}