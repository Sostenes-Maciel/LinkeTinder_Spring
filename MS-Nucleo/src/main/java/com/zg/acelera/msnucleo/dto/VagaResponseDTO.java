package com.zg.acelera.msnucleo.dto;

public record VagaResponseDTO(
        Long idVaga,
        String titulo,
        String pais,
        String estado,
        String descricao
) {
}