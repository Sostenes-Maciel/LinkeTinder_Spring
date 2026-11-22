package com.zg.acelera.msnucleo.dto;

public record CadastroVagaRequestDTO(
        String titulo,
        String pais,
        String estado,
        String descricao,
        String cnpjEmpresa
) {
}