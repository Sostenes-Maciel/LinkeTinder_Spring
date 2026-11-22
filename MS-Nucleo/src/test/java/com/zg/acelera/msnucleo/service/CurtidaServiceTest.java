package com.zg.acelera.msnucleo.service;

import com.zg.acelera.msnucleo.model.*;
import com.zg.acelera.msnucleo.repository.*;
import com.zg.acelera.msnucleo.CurtidaService;
import com.zg.acelera.msnucleo.MatchVagaService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurtidaServiceTest {

    private static final String CPF = "12345678901";
    private static final String CNPJ = "12345678000199";
    private static final Long ID_VAGA = 1L;

    @Mock
    private CurtidaRepository curtidaRepository;

    @Mock
    private CandidatoRepository candidatoRepository;

    @Mock
    private EmpresaRepository empresaRepository;

    @Mock
    private VagaRepository vagaRepository;

    @Mock
    private MatchVagaService matchVagaService;

    @InjectMocks
    private CurtidaService curtidaService;

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
        return new Vaga(
                "Desenvolvedor Java",
                "Brasil",
                "PE",
                null,
                criarEmpresa()
        );
    }

    @Test
    void candidatoDeveCurtirVaga() {
        when(candidatoRepository.findById(CPF))
                .thenReturn(Optional.of(criarCandidato()));

        when(vagaRepository.findById(ID_VAGA))
                .thenReturn(Optional.of(criarVaga()));

        when(curtidaRepository
                .existsByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
                        CPF, ID_VAGA
                )).thenReturn(false);

        when(curtidaRepository.save(any(Curtida.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(matchVagaService.processarPossivelMatch(CPF, ID_VAGA))
                .thenReturn(Optional.empty());

        Curtida resultado =
                curtidaService.curtirVaga(CPF, ID_VAGA);

        assertEquals(CPF, resultado.getCandidato().getCpf());
        assertNull(resultado.getEmpresa());

        verify(curtidaRepository).save(any(Curtida.class));
        verify(matchVagaService)
                .processarPossivelMatch(CPF, ID_VAGA);
    }

    @Test
    void naoDevePermitirCurtidaDuplicadaDoCandidato() {
        when(candidatoRepository.findById(CPF))
                .thenReturn(Optional.of(criarCandidato()));

        when(vagaRepository.findById(ID_VAGA))
                .thenReturn(Optional.of(criarVaga()));

        when(curtidaRepository
                .existsByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
                        CPF, ID_VAGA
                )).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> curtidaService.curtirVaga(CPF, ID_VAGA)
        );

        verify(curtidaRepository, never()).save(any());
    }

    @Test
    void empresaDeveCurtirCandidatoParaVagaPropria() {
        when(empresaRepository.findById(CNPJ))
                .thenReturn(Optional.of(criarEmpresa()));

        when(candidatoRepository.findById(CPF))
                .thenReturn(Optional.of(criarCandidato()));

        when(vagaRepository.findByIdVagaAndCnpjEmpresa(ID_VAGA, CNPJ))
                .thenReturn(Optional.of(criarVaga()));

        when(curtidaRepository
                .existsByCandidato_CpfAndEmpresa_CnpjAndVaga_IdVaga(
                        CPF, CNPJ, ID_VAGA
                )).thenReturn(false);

        when(curtidaRepository.save(any(Curtida.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(matchVagaService.processarPossivelMatch(CPF, ID_VAGA))
                .thenReturn(Optional.empty());

        Curtida resultado =
                curtidaService.curtirCandidato(CNPJ, CPF, ID_VAGA);

        assertEquals(CNPJ, resultado.getEmpresa().getCnpj());
        assertEquals(CPF, resultado.getCandidato().getCpf());

        verify(matchVagaService)
                .processarPossivelMatch(CPF, ID_VAGA);
    }

    @Test
    void empresaNaoDeveCurtirCandidatoParaVagaDeOutraEmpresa() {
        when(empresaRepository.findById(CNPJ))
                .thenReturn(Optional.of(criarEmpresa()));

        when(candidatoRepository.findById(CPF))
                .thenReturn(Optional.of(criarCandidato()));

        when(vagaRepository.findByIdVagaAndCnpjEmpresa(ID_VAGA, CNPJ))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> curtidaService.curtirCandidato(CNPJ, CPF, ID_VAGA)
        );

        verify(curtidaRepository, never()).save(any());
    }

    @Test
    void naoDeveRemoverCurtidaQuandoJaExisteMatch() {
        Curtida curtida = new Curtida(
                criarCandidato(),
                criarVaga()
        );

        when(curtidaRepository
                .findByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
                        CPF, ID_VAGA
                )).thenReturn(Optional.of(curtida));

        when(matchVagaService.existeMatch(CPF, CNPJ, ID_VAGA))
                .thenReturn(true);

        assertThrows(
                IllegalStateException.class,
                () -> curtidaService.removerCurtidaCandidato(CPF, ID_VAGA)
        );

        verify(curtidaRepository, never()).delete(any());
    }

    @Test
    void deveRemoverCurtidaQuandoNaoExisteMatch() {
        Curtida curtida = new Curtida(
                criarCandidato(),
                criarVaga()
        );

        when(curtidaRepository
                .findByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
                        CPF, ID_VAGA
                )).thenReturn(Optional.of(curtida));

        when(matchVagaService.existeMatch(CPF, CNPJ, ID_VAGA))
                .thenReturn(false);

        assertDoesNotThrow(
                () -> curtidaService.removerCurtidaCandidato(CPF, ID_VAGA)
        );

        verify(curtidaRepository).delete(curtida);
    }

    @Test
    void deveLancarExcecaoQuandoCurtidaNaoExiste() {
        when(curtidaRepository
                .findByCandidato_CpfAndVaga_IdVagaAndEmpresaIsNull(
                        CPF, ID_VAGA
                )).thenReturn(Optional.empty());

        assertThrows(
                EntityNotFoundException.class,
                () -> curtidaService.removerCurtidaCandidato(CPF, ID_VAGA)
        );
    }
}