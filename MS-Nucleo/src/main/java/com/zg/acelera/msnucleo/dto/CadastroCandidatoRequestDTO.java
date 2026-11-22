package com.zg.acelera.msnucleo.dto;

public record CadastroCandidatoRequestDTO(
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