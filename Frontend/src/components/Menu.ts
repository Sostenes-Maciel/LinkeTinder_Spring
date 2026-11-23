import { renderCadastroCandidato } from '../service/CadastroCandidato.ts'
import { renderCadastroEmpresa } from '../service/CadastroEmpresa.ts'
import { CadastroVagasService, renderViewCadastroVaga } from '../service/CadastroVaga.ts'
import { renderLogin } from "../view/RenderLogin.ts"
import { renderPerfilCandidato } from '../view/PerfilCandidato.ts'
import { renderPerfilEmpresa } from '../view/PerfilEmpresa.ts'
import { renderListaVagas } from '../view/ListaVagas.ts'
import { renderListaMatchesCandidato } from '../view/ListaMatchesCandidato.ts'
import { renderListaMatchesEmpresa } from '../view/ListaMatchesEmpresa.ts'
import { confirmarExclusao } from './Confirmacao.ts'
import type { Candidato } from '../model/Candidato.ts'
import type { Empresa } from '../model/Empresa.ts'
import type { RecrutadorSessao } from '../model/Recrutador.ts'
import { renderCadastroRecrutador } from '../view/CadastroRecrutador.ts'
import { renderPerfilRecrutador } from '../view/PerfilRecrutador.ts'
import { renderListaVagasRecrutador } from '../view/ListaVagasRecrutador.ts'
import { renderListaCandidatosRecrutador } from '../view/ListaCandidatosRecrutador.ts'
import { renderListaMatchesRecrutador } from '../view/ListaMatchesRecrutador.ts'

const URL_MS_NUCLEO = 'http://localhost:8081'

function obterSessaoAtiva(): { tipo: 'candidato'; usuario: Candidato } | { tipo: 'empresa'; usuario: Empresa } | { tipo: 'recrutador'; usuario: RecrutadorSessao } | null {
    const usuarioSalvo = localStorage.getItem('usuarioLogado')
    const tipoUsuario = localStorage.getItem('tipoUsuario')

    if (!usuarioSalvo || !['candidato', 'empresa', 'recrutador'].includes(tipoUsuario ?? '')) {
        return null
    }

    try {
        const usuario: unknown = JSON.parse(usuarioSalvo)
        if (typeof usuario !== 'object' || usuario === null) return null

        if (tipoUsuario === 'candidato' && 'cpf' in usuario) {
            return { tipo: 'candidato', usuario: usuario as Candidato }
        }

        if (tipoUsuario === 'empresa' && 'cnpj' in usuario) {
            return { tipo: 'empresa', usuario: usuario as Empresa }
        }

        if (tipoUsuario === 'recrutador' && 'areaAtuacao' in usuario) {
            return { tipo: 'recrutador', usuario: usuario as RecrutadorSessao }
        }

        return null
    } catch {
        return null
    }
}

function fecharMenuConta(recolherAteSairDoHover = false): void {
    const menuConta = document.querySelector<HTMLElement>('#conta-menu')
    const botaoConta = document.querySelector<HTMLButtonElement>('#menu-conta-trigger')

    menuConta?.classList.remove('aberto')
    menuConta?.classList.toggle('recolhido', recolherAteSairDoHover)
    botaoConta?.setAttribute('aria-expanded', 'false')
}

function atualizarMenuConta(): void {
    const sessao = obterSessaoAtiva()
    const botaoLogin = document.querySelector<HTMLButtonElement>('#menu-login')
    const menuConta = document.querySelector<HTMLElement>('#conta-menu')
    const nomeUsuario = document.querySelector<HTMLElement>('#menu-nome-usuario')
    const opcoes = document.querySelector<HTMLDivElement>('#menu-conta-opcoes')

    if (!botaoLogin || !menuConta || !nomeUsuario || !opcoes) return

    botaoLogin.hidden = Boolean(sessao)
    menuConta.hidden = !sessao
    fecharMenuConta()

    if (!sessao) {
        opcoes.replaceChildren()
        return
    }

    nomeUsuario.textContent = sessao.usuario.nome

    const itens = sessao.tipo === 'candidato'
        ? [
            ['perfil', 'Meu perfil'],
            ['vagas', 'Vagas disponíveis'],
            ['matches', 'Meus matches'],
            ['editar', 'Editar perfil'],
            ['excluir', 'Excluir perfil'],
            ['sair', 'Sair']
        ]
        : sessao.tipo === 'empresa' ? [
            ['perfil', 'Meu perfil'],
            ['vaga', 'Cadastrar vaga'],
            ['excluir-vaga', 'Excluir vaga'],
            ['recrutador', 'Cadastrar recrutador'],
            ['matches', 'Meus matches'],
            ['editar', 'Editar perfil'],
            ['excluir', 'Excluir perfil'],
            ['sair', 'Sair']
        ] : [
            ['perfil', 'Meu perfil'],
            ['vagas-recrutador', 'Vagas da minha área'],
            ['candidatos-recrutador', 'Candidatos da minha área'],
            ['matches-recrutador', 'Meus matches'],
            ['sair', 'Sair']
        ]

    opcoes.innerHTML = itens.map(([acao, titulo]) =>
        `<button type="button" role="menuitem" data-acao="${acao}">${titulo}</button>`
    ).join('')
}

function limparSessao(): void {
    localStorage.removeItem('usuarioLogado')
    localStorage.removeItem('tipoUsuario')

    window.dispatchEvent(new Event('auth-change'))
    window.dispatchEvent(new Event('app:logout'))
}

async function excluirPerfil(sessao: NonNullable<ReturnType<typeof obterSessaoAtiva>>): Promise<void> {
    const candidato = sessao.tipo === 'candidato'

    const confirmou = await confirmarExclusao(
        candidato ? 'Excluir perfil de candidato?' : 'Excluir perfil da empresa?',
        candidato
            ? 'Essa ação não poderá ser desfeita e removerá seu perfil e dados vinculados.'
            : 'Essa ação não poderá ser desfeita e removerá as vagas e dados vinculados à empresa.'
    )

    if (!confirmou) return

    const documento = candidato
        ? (sessao.usuario as Candidato).cpf
        : (sessao.usuario as Empresa).cnpj

    const endpoint = candidato ? 'candidatos' : 'empresas'

    try {
        const resposta = await fetch(
            `${URL_MS_NUCLEO}/api/${endpoint}/${encodeURIComponent(documento)}`,
            { method: 'DELETE' }
        )

        if (!resposta.ok) {
            const erroApi = await resposta.json().catch(() => ({}))

            throw new Error(
                erroApi.mensagem ?? `Falha ao excluir perfil (HTTP ${resposta.status}).`
            )
        }

        limparSessao()
    } catch (erro) {
        console.error('Erro ao excluir perfil:', erro)

        window.alert(
            erro instanceof Error
                ? erro.message
                : 'Não foi possível excluir o perfil.'
        )
    }
}

interface VagaDaEmpresa {
    idVaga: number
    titulo: string
    pais: string
    estado: string
    descricao: string
}

type RespostaVagasEmpresa = VagaDaEmpresa[] | {
    dados?: VagaDaEmpresa[]
}

async function gerenciarExclusaoVagas(empresa: Empresa): Promise<void> {
    const app = document.querySelector<HTMLDivElement>('#app')
    if (!app) return

    app.innerHTML = `
        <section class="lista-vagas-empresa tela-com-transicao">
            <h1>Excluir vagas</h1>

            <p class="form-descricao">
                Selecione uma das vagas cadastradas pela sua empresa para excluí-la.
            </p>

            <p id="mensagem-gerenciamento-vagas" role="status" aria-live="polite"></p>

            <div id="lista-vagas-empresa">
                <p>Carregando vagas...</p>
            </div>

            <button type="button" id="btn-voltar-perfil-empresa">
                Voltar ao perfil
            </button>
        </section>
    `

    document.querySelector<HTMLButtonElement>('#btn-voltar-perfil-empresa')
        ?.addEventListener('click', () => {
            void renderPerfilEmpresa(empresa)
        })

    await carregarVagasDaEmpresa(empresa)
}

async function carregarVagasDaEmpresa(empresa: Empresa): Promise<void> {
    const lista = document.querySelector<HTMLDivElement>('#lista-vagas-empresa')
    if (!lista) return

    lista.replaceChildren()

    try {
        const cnpj = empresa.cnpj.replace(/\D/g, '')

        const resposta = await fetch(
            `${URL_MS_NUCLEO}/api/vagas/empresa/${encodeURIComponent(cnpj)}`
        )

        if (!resposta.ok) {
            throw new Error(
                `Não foi possível carregar as vagas da empresa (HTTP ${resposta.status}).`
            )
        }

        const dados: RespostaVagasEmpresa = await resposta.json()
        const vagas = Array.isArray(dados) ? dados : dados.dados ?? []

        if (vagas.length === 0) {
            const mensagem = document.createElement('p')
            mensagem.className = 'tabela-vazia'
            mensagem.textContent = 'Sua empresa ainda não possui vagas cadastradas.'

            lista.appendChild(mensagem)
            return
        }

        vagas.forEach(vaga => {
            const cartao = document.createElement('article')
            cartao.className = 'card-vaga-gerenciamento'

            const titulo = document.createElement('h2')
            titulo.textContent = vaga.titulo

            const localizacao = document.createElement('p')
            localizacao.textContent = `Local: ${vaga.pais} - ${vaga.estado}`

            const descricao = document.createElement('p')
            descricao.textContent = vaga.descricao

            const botaoExcluir = document.createElement('button')
            botaoExcluir.type = 'button'
            botaoExcluir.className = 'btn-excluir-vaga'
            botaoExcluir.textContent = 'Excluir vaga'

            botaoExcluir.addEventListener('click', () => {
                void excluirVagaDaEmpresa(vaga, empresa, botaoExcluir)
            })

            cartao.append(titulo, localizacao, descricao, botaoExcluir)
            lista.appendChild(cartao)
        })

    } catch (erro) {
        console.error('Erro ao carregar vagas da empresa:', erro)

        const mensagem = document.createElement('p')
        mensagem.className = 'mensagem-erro'
        mensagem.textContent = erro instanceof Error
            ? erro.message
            : 'Não foi possível carregar as vagas da empresa.'

        lista.appendChild(mensagem)
    }
}

async function excluirVagaDaEmpresa(
    vaga: VagaDaEmpresa,
    empresa: Empresa,
    botao: HTMLButtonElement
): Promise<void> {

    const confirmou = await confirmarExclusao(
        `Excluir a vaga "${vaga.titulo}"?`,
        'Essa ação não poderá ser desfeita. A vaga e seus vínculos de competências serão removidos.'
    )

    if (!confirmou) return

    botao.disabled = true
    botao.textContent = 'Excluindo...'

    let mensagem: string
    let classeMensagem: string

    try {
        await CadastroVagasService.excluir(vaga.idVaga, empresa)

        mensagem = `A vaga "${vaga.titulo}" foi excluída com sucesso.`
        classeMensagem = 'mensagem-sucesso'

    } catch (erro) {
        console.error('Erro ao excluir vaga:', erro)

        mensagem = erro instanceof Error
            ? erro.message
            : 'Não foi possível excluir a vaga.'

        classeMensagem = 'mensagem-erro'
    }

    // Atualiza a relação mesmo quando ocorre falha parcial na limpeza dos vínculos.
    await carregarVagasDaEmpresa(empresa)

    const elementoMensagem = document.querySelector<HTMLParagraphElement>(
        '#mensagem-gerenciamento-vagas'
    )

    if (elementoMensagem) {
        elementoMensagem.textContent = mensagem
        elementoMensagem.className = classeMensagem
    }
}

async function executarAcaoMenu(acao: string): Promise<void> {
    const sessao = obterSessaoAtiva()

    if (!sessao) {
        atualizarMenuConta()
        return
    }

    fecharMenuConta(true)
    document.querySelector<HTMLButtonElement>('#menu-conta-trigger')?.focus()

    switch (acao) {
        case 'perfil':
            if (sessao.tipo === 'candidato') {
                await renderPerfilCandidato(sessao.usuario)
            } else if (sessao.tipo === 'empresa') {
                await renderPerfilEmpresa(sessao.usuario)
            } else {
                await renderPerfilRecrutador(sessao.usuario)
            }
            break

        case 'vagas':
            if (sessao.tipo === 'candidato') {
                await renderListaVagas(sessao.usuario)
            }
            break

        case 'vagas-recrutador':
            if (sessao.tipo === 'recrutador') {
                await renderListaVagasRecrutador(sessao.usuario)
            }
            break

        case 'candidatos-recrutador':
            if (sessao.tipo === 'recrutador') {
                await renderListaCandidatosRecrutador(sessao.usuario)
            }
            break

        case 'matches-recrutador':
            if (sessao.tipo === 'recrutador') {
                await renderListaMatchesRecrutador(sessao.usuario)
            }
            break

        case 'matches':
            if (sessao.tipo === 'candidato') {
                await renderListaMatchesCandidato(sessao.usuario)
            } else if (sessao.tipo === 'empresa') {
                await renderListaMatchesEmpresa(sessao.usuario)
            }
            break

        case 'vaga':
            if (sessao.tipo === 'empresa') {
                await renderViewCadastroVaga(
                    sessao.usuario,
                    mensagem => void renderPerfilEmpresa(sessao.usuario, mensagem)
                )
            }
            break

        case 'excluir-vaga':
            if (sessao.tipo === 'empresa') {
                await gerenciarExclusaoVagas(sessao.usuario)
            }
            break

        case 'recrutador':
            if (sessao.tipo === 'empresa') {
                await renderCadastroRecrutador()
            }
            break

        case 'editar':
            if (sessao.tipo === 'candidato') {
                await renderCadastroCandidato(sessao.usuario)
            } else if (sessao.tipo === 'empresa') {
                await renderCadastroEmpresa(sessao.usuario)
            } else {
                await renderPerfilRecrutador(sessao.usuario)
            }
            break

        case 'excluir':
            await excluirPerfil(sessao)
            break

        case 'sair':
            limparSessao()
            break
    }

    document.querySelector('#app')?.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
    })
}

export function configurarMenu(): void {
    document.querySelector<HTMLButtonElement>(
        '#menu-cadastro-candidato'
    )?.addEventListener('click', () => {
        renderCadastroCandidato().catch((erro: unknown) => {
            console.error('Erro ao carregar caandidato:', erro)
        })
    })

    document.querySelector<HTMLButtonElement>(
        '#menu-cadastro-empresa'
    )?.addEventListener('click', () => {
        renderCadastroEmpresa().catch((erro: unknown) => {
            console.error('Erro ao cadastrar empresa:', erro)
        })
    })

    document.querySelector<HTMLButtonElement>('#menu-login')?.addEventListener('click', () => {
        renderLogin()

        document.querySelector('#app')?.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
        })
    })

    const menuConta = document.querySelector<HTMLElement>('#conta-menu')
    const botaoConta = document.querySelector<HTMLButtonElement>('#menu-conta-trigger')
    const opcoesConta = document.querySelector<HTMLDivElement>('#menu-conta-opcoes')

    botaoConta?.addEventListener('click', () => {
        menuConta?.classList.remove('recolhido')

        const aberto = menuConta?.classList.toggle('aberto') ?? false
        botaoConta.setAttribute('aria-expanded', String(aberto))
    })

    menuConta?.addEventListener('pointerleave', () => {
        menuConta.classList.remove('recolhido')
    })

    opcoesConta?.addEventListener('click', evento => {
        const botao = (evento.target as HTMLElement).closest<HTMLButtonElement>('[data-acao]')

        if (botao?.dataset.acao) {
            void executarAcaoMenu(botao.dataset.acao)
        }
    })

    document.addEventListener('click', evento => {
        if (menuConta && !menuConta.contains(evento.target as Node)) {
            fecharMenuConta()
        }
    })

    document.addEventListener('keydown', evento => {
        const menuEstavaEmUso = menuConta?.classList.contains('aberto')
            || menuConta?.contains(document.activeElement)

        if (evento.key === 'Escape' && menuEstavaEmUso) {
            fecharMenuConta(true)
            botaoConta?.focus()
        }
    })

    window.addEventListener('app:logout', () => {
        renderLogin()

        document.querySelector('#app')?.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
        })
    })

    window.addEventListener('auth-change', atualizarMenuConta)

    atualizarMenuConta()
}