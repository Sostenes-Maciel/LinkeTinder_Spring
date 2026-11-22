package com.zg.acelera.msnucleo.dto;

public record CurtidaRequestDTO(
        String autor,
        String cpfCandidato,
        String cnpjEmpresa,
        Long idVaga
) {
}