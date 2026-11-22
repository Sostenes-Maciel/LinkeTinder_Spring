package com.zg.acelera.msnucleo.dto;

public record CadastroEmpresaRequestDTO(
        String cnpj,
        String nome,
        String email,
        String pais,
        String estado,
        String cep,
        String descricao
) {
}