package com.zg.acelera.msnucleo.dto;

public record ResultadoCurtidaResponseDTO(
        String mensagem,
        boolean match,
        Long idMatch
) {
}