package com.zg.acelera.mscompetencias.repository;

import com.zg.acelera.mscompetencias.model.Competencia;
import com.zg.acelera.mscompetencias.model.CandidatoCompetencia;
import com.zg.acelera.mscompetencias.model.EmpresaCompetencia;
import com.zg.acelera.mscompetencias.model.VagaCompetencia;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class CompetenciaRepositoryIntegracaoTest {

    @Autowired
    private CompetenciaRepository competenciaRepository;

    @Autowired
    private CandidatoCompetenciaRepository candidatoRepository;

    @Autowired
    private EmpresaCompetenciaRepository empresaRepository;

    @Autowired
    private VagaCompetenciaRepository vagaRepository;

    @Test
    void devePersistirEConsultarCompetencia() {

        Competencia competencia =
                competenciaRepository.saveAndFlush(
                        new Competencia("JUnit - Persistencia")
                );

        Optional<Competencia> resultado =
                competenciaRepository.findByNome(
                        "JUnit - Persistencia"
                );

        assertTrue(resultado.isPresent());

        assertNotNull(competencia.getIdCompetencia());

        assertEquals(
                "JUnit - Persistencia",
                resultado.get().getNome()
        );
    }

    @Test
    void devePersistirAssociacaoEntreCandidatoECompetencia() {

        Competencia competencia =
                competenciaRepository.saveAndFlush(
                        new Competencia("JUnit - Candidato")
                );

        CandidatoCompetencia associacao =
                new CandidatoCompetencia(
                        "12345678900",
                        competencia
                );

        candidatoRepository.saveAndFlush(associacao);

        List<CandidatoCompetencia> resultado =
                candidatoRepository.buscarPorCpfCandidato(
                        "12345678900"
                );

        assertEquals(1, resultado.size());

        assertEquals(
                "12345678900",
                resultado.get(0).getCpfCandidato()
        );

        assertEquals(
                "JUnit - Candidato",
                resultado.get(0).getCompetencia().getNome()
        );
    }

    @Test
    void devePersistirAssociacaoEntreEmpresaECompetencia() {

        String cnpjTeste = String.format(
                "%014d",
                Math.floorMod(
                        System.nanoTime(),
                        100_000_000_000_000L
                )
        );

        Competencia competencia =
                competenciaRepository.saveAndFlush(
                        new Competencia("JUnit - Empresa")
                );

        EmpresaCompetencia associacao =
                new EmpresaCompetencia(
                        cnpjTeste,
                        competencia
                );

        empresaRepository.saveAndFlush(associacao);

        List<EmpresaCompetencia> resultado =
                empresaRepository.buscarPorCnpjEmpresa(cnpjTeste);

        assertEquals(1, resultado.size());

        assertEquals(
                cnpjTeste,
                resultado.get(0).getCnpjEmpresa()
        );

        assertEquals(
                "JUnit - Empresa",
                resultado.get(0).getCompetencia().getNome()
        );
    }

    @Test
    void devePersistirAssociacaoEntreVagaECompetencia() {

        Competencia competencia =
                competenciaRepository.saveAndFlush(
                        new Competencia("JUnit - Vaga")
                );

        VagaCompetencia associacao =
                new VagaCompetencia(
                        999L,
                        competencia
                );

        vagaRepository.saveAndFlush(associacao);

        List<VagaCompetencia> resultado =
                vagaRepository.buscarPorIdVaga(999L);

        assertEquals(1, resultado.size());

        assertEquals(
                999L,
                resultado.get(0).getIdVaga()
        );

        assertEquals(
                "JUnit - Vaga",
                resultado.get(0).getCompetencia().getNome()
        );
    }
}