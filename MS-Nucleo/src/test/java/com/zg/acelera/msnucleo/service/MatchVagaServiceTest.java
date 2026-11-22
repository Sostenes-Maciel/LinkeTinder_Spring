package com.zg.acelera.msnucleo.service;

import com.zg.acelera.msnucleo.model.*;
import com.zg.acelera.msnucleo.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MatchVagaServiceTest {

    private static final String CPF = "12345678901";
    private static final String CNPJ = "12345678000199";
    private static final Long ID_VAGA = 1L;

    @Mock
    private MatchVagaRepository matchVagaRepository;

    @Mock
    private CurtidaRepository curtidaRepository;

    @Mock
    private CandidatoRepository candidatoRepository;

    @Mock
    private VagaRepository vagaRepository;

    @InjectMocks
    private MatchVagaService matchVagaService;

    private Candidato criarCandidato() {
        return new Candidato(
                CPF, "Ana", "ana@email.com",
                "Brasil", "PE", null, null, 25
        );
    }

    private Empresa criarEmpresa() {
        return new Empresa(
                CNPJ, "Tech Global", "contato@techglobal.com",
                "Brasil", "PE", null, null
        );
    }

    private Vaga criarVaga() {
        Vaga vaga = new Vaga(
                "Desenvolvedor Java",
                "Brasil",
                "PE",
                null,
                criarEmpresa()
        );

        ReflectionTestUtils.setField(vaga, "idVaga", ID_VAGA);

        return vaga;
    }

    private void prepararEntidades() {
        when(candidatoRepository.findById(CPF))
                .thenReturn(Optional.of(criarCandidato()));

        when(vagaRepository.findById(ID_VAGA))
                .thenReturn(Optional.of(criarVaga()));
    }

    @Test
    void naoDeveCriarMatchSemCurtidasReciprocas() {
        prepararEntidades();

        when(curtidaRepository
                .existsByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
                        CPF, ID_VAGA
                )).thenReturn(true);

        when(curtidaRepository
                .existsByCandidato_CpfAndEmpresa_CnpjAndVaga_IdVaga(
                        CPF, CNPJ, ID_VAGA
                )).thenReturn(false);

        Optional<MatchVaga> resultado =
                matchVagaService.processarPossivelMatch(CPF, ID_VAGA);

        assertTrue(resultado.isEmpty());

        verify(matchVagaRepository, never()).save(any());
    }

    @Test
    void deveCriarMatchQuandoExistiremCurtidasReciprocas() {
        prepararEntidades();

        when(curtidaRepository
                .existsByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
                        CPF, ID_VAGA
                )).thenReturn(true);

        when(curtidaRepository
                .existsByCandidato_CpfAndEmpresa_CnpjAndVaga_IdVaga(
                        CPF, CNPJ, ID_VAGA
                )).thenReturn(true);

        when(matchVagaRepository
                .findByCandidato_CpfAndVaga_Empresa_CnpjAndVaga_IdVaga(
                        CPF, CNPJ, ID_VAGA
                )).thenReturn(Optional.empty());

        when(matchVagaRepository.save(any(MatchVaga.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Optional<MatchVaga> resultado =
                matchVagaService.processarPossivelMatch(CPF, ID_VAGA);

        assertTrue(resultado.isPresent());
        assertEquals(CPF, resultado.get().getCandidato().getCpf());
        assertEquals(ID_VAGA, resultado.get().getVaga().getIdVaga());

        verify(matchVagaRepository).save(any(MatchVaga.class));
    }

    @Test
    void naoDeveDuplicarMatchExistente() {
        prepararEntidades();

        MatchVaga matchExistente = new MatchVaga(
                criarCandidato(),
                criarVaga()
        );

        when(curtidaRepository
                .existsByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
                        CPF, ID_VAGA
                )).thenReturn(true);

        when(curtidaRepository
                .existsByCandidato_CpfAndEmpresa_CnpjAndVaga_IdVaga(
                        CPF, CNPJ, ID_VAGA
                )).thenReturn(true);

        when(matchVagaRepository
                .findByCandidato_CpfAndVaga_Empresa_CnpjAndVaga_IdVaga(
                        CPF, CNPJ, ID_VAGA
                )).thenReturn(Optional.of(matchExistente));

        Optional<MatchVaga> resultado =
                matchVagaService.processarPossivelMatch(CPF, ID_VAGA);

        assertTrue(resultado.isPresent());
        assertSame(matchExistente, resultado.get());

        verify(matchVagaRepository, never()).save(any());
    }
}