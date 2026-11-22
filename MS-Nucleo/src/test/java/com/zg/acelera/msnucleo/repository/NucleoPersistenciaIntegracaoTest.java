package com.zg.acelera.msnucleo.repository;

import com.zg.acelera.msnucleo.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class NucleoPersistenciaIntegracaoTest {

    @Autowired
    private CandidatoRepository candidatoRepository;

    @Autowired
    private EmpresaRepository empresaRepository;

    @Autowired
    private VagaRepository vagaRepository;

    @Autowired
    private CurtidaRepository curtidaRepository;

    @Autowired
    private MatchVagaRepository matchVagaRepository;

    private Candidato salvarCandidato() {
        String cpf = String.format(
                "%011d",
                Math.floorMod(System.nanoTime(), 100_000_000_000L)
        );

        Candidato candidato = new Candidato(
                cpf,
                "Candidato Teste",
                "candidato-" + UUID.randomUUID() + "@teste.com",
                "Brasil",
                "PE",
                "50000-000",
                "Perfil de teste",
                25
        );

        return candidatoRepository.saveAndFlush(candidato);
    }

    private Empresa salvarEmpresa() {
        String cnpj = String.format(
                "%014d",
                Math.floorMod(System.nanoTime(), 100_000_000_000_000L)
        );

        Empresa empresa = new Empresa(
                cnpj,
                "Empresa Teste",
                "empresa-" + UUID.randomUUID() + "@teste.com",
                "Brasil",
                "PE",
                "50000-000",
                "Empresa de testes"
        );

        return empresaRepository.saveAndFlush(empresa);
    }

    private Vaga salvarVaga(Empresa empresa) {
        Vaga vaga = new Vaga(
                "Desenvolvedor Java",
                "Brasil",
                "PE",
                "Vaga para testes de integração",
                empresa
        );

        return vagaRepository.saveAndFlush(vaga);
    }

    @Test
    void devePersistirCurtidasEMatchComRelacionamentosCorretos() {
        Candidato candidato = salvarCandidato();
        Empresa empresa = salvarEmpresa();
        Vaga vaga = salvarVaga(empresa);

        Curtida curtidaCandidato = curtidaRepository.saveAndFlush(
                new Curtida(candidato, vaga)
        );

        Curtida curtidaEmpresa = curtidaRepository.saveAndFlush(
                new Curtida(candidato, vaga, empresa)
        );

        MatchVaga match = matchVagaRepository.saveAndFlush(
                new MatchVaga(candidato, vaga)
        );

        assertNotNull(curtidaCandidato.getIdCurtida());
        assertNotNull(curtidaEmpresa.getIdCurtida());
        assertNotNull(match.getIdMatch());

        assertNull(curtidaCandidato.getEmpresa());

        assertEquals(
                empresa.getCnpj(),
                curtidaEmpresa.getEmpresa().getCnpj()
        );

        assertEquals(
                empresa.getCnpj(),
                match.getEmpresa().getCnpj()
        );
    }

    @Test
    void naoDevePermitirCurtidaEmpresarialEmVagaDeOutraEmpresa() {
        Candidato candidato = salvarCandidato();
        Empresa proprietaria = salvarEmpresa();
        Empresa outraEmpresa = salvarEmpresa();

        Vaga vaga = salvarVaga(proprietaria);

        assertThrows(
                DataIntegrityViolationException.class,
                () -> curtidaRepository.saveAndFlush(
                        new Curtida(candidato, vaga, outraEmpresa)
                )
        );
    }

    @Test
    void naoDevePermitirCurtidaEmpresarialDuplicada() {
        Candidato candidato = salvarCandidato();
        Empresa empresa = salvarEmpresa();
        Vaga vaga = salvarVaga(empresa);

        curtidaRepository.saveAndFlush(
                new Curtida(candidato, vaga, empresa)
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> curtidaRepository.saveAndFlush(
                        new Curtida(candidato, vaga, empresa)
                )
        );
    }

    @Test
    void naoDevePermitirMatchDuplicadoParaMesmaTupla() {
        Candidato candidato = salvarCandidato();
        Empresa empresa = salvarEmpresa();
        Vaga vaga = salvarVaga(empresa);

        matchVagaRepository.saveAndFlush(
                new MatchVaga(candidato, vaga)
        );

        assertThrows(
                DataIntegrityViolationException.class,
                () -> matchVagaRepository.saveAndFlush(
                        new MatchVaga(candidato, vaga)
                )
        );
    }

    @Test
    void naoDevePermitirCurtidaSemVagaEsemEmpresa() {
        Candidato candidato = salvarCandidato();

        assertThrows(
                DataIntegrityViolationException.class,
                () -> curtidaRepository.saveAndFlush(
                        new Curtida(candidato, null)
                )
        );
    }
}