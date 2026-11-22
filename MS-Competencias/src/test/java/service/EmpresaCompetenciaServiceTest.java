package service;

import com.zg.acelera.mscompetencias.dto.CompetenciaResponse;
import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.model.EmpresaCompetencia;
import com.zg.acelera.mscompetencias.model.id.EmpresaCompetenciaId;
import com.zg.acelera.mscompetencias.repository.EmpresaCompetenciaRepository;

import com.zg.acelera.mscompetencias.service.CompetenciaService;
import com.zg.acelera.mscompetencias.service.EmpresaCompetenciaService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpresaCompetenciaServiceTest {

    @Mock
    private EmpresaCompetenciaRepository associacaoRepository;

    @Mock
    private CompetenciaService competenciaService;

    @InjectMocks
    private EmpresaCompetenciaService associacaoService;

    @Test
    void deveAssociarCompetenciaComCnpjNormalizado() {

        Competencia competencia = mock(Competencia.class);

        when(competencia.getIdCompetencia()).thenReturn(20L);
        when(competencia.getNome()).thenReturn("Spring");

        when(competenciaService.buscarOuCriar("Spring"))
                .thenReturn(competencia);

        EmpresaCompetenciaId id =
                new EmpresaCompetenciaId("12345678000195", 20L);

        when(associacaoRepository.existsById(id))
                .thenReturn(false);

        when(associacaoRepository.save(any(EmpresaCompetencia.class)))
                .thenAnswer(invocacao -> invocacao.getArgument(0));

        List<CompetenciaResponse> resultado =
                associacaoService.associar(
                        "12.345.678/0001-95",
                        List.of("Spring")
                );

        assertEquals(1, resultado.size());
        assertEquals("Spring", resultado.get(0).nome());
        assertEquals(20L, resultado.get(0).idCompetencia());

        verify(associacaoRepository).save(
                any(EmpresaCompetencia.class)
        );
    }

    @Test
    void naoDeveDuplicarAssociacaoExistente() {

        Competencia competencia = mock(Competencia.class);

        when(competencia.getIdCompetencia()).thenReturn(20L);
        when(competencia.getNome()).thenReturn("Spring");

        when(competenciaService.buscarOuCriar("Spring"))
                .thenReturn(competencia);

        EmpresaCompetenciaId id =
                new EmpresaCompetenciaId("12345678000195", 20L);

        when(associacaoRepository.existsById(id))
                .thenReturn(true);

        associacaoService.associar(
                "12345678000195",
                List.of("Spring")
        );

        verify(associacaoRepository, never())
                .save(any(EmpresaCompetencia.class));
    }

    @Test
    void naoDeveAssociarComCnpjInvalido() {

        assertThrows(
                IllegalArgumentException.class,
                () -> associacaoService.associar(
                        "12.345",
                        List.of("Spring")
                )
        );

        verifyNoInteractions(
                associacaoRepository,
                competenciaService
        );
    }
}