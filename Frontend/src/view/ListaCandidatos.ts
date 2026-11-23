import { renderPerfilCandidato } from "./PerfilCandidato.ts"
import { confirmarExclusao } from "../components/Confirmacao.ts"
import type { Candidato } from "../model/Candidato"

const API_NUCLEO = "http://localhost:8081"

interface CandidatoApi {
    cpf: string
    nome: string
    sobrenome?: string
    email: string
    pais?: string
    estado?: string
    cep?: string
    descricao?: string
    idade?: number
    dataNascimento?: string
}

interface CompetenciaApi {
    idCompetencia: number
    nome: string
}

export async function renderListaCandidatos(mensagemSucesso?: string): Promise<void> {
    const app = document.querySelector<HTMLDivElement>("#app")
    if (!app) return

    renderizarTabelaBase(app, mensagemSucesso)
    await renderizarLinhasCandidatos()
}

function renderizarTabelaBase(app: HTMLDivElement, mensagemSucesso?: string): void {
    app.innerHTML = `
        <section class="tela-com-transicao">
            <h1>Candidatos</h1>
            <p id="mensagem" class="${mensagemSucesso ? "mensagem-sucesso" : ""}">
                ${escaparHtml(mensagemSucesso)}
            </p>

            <table>
                <thead>
                    <tr>
                        <th>Nome</th>
                        <th>Estado</th>
                        <th>Competências</th>
                        <th>Ação</th>
                    </tr>
                </thead>
                <tbody id="lista-candidatos">
                    <tr><td colspan="4">Carregando candidatos...</td></tr>
                </tbody>
            </table>
        </section>
    `
}

async function renderizarLinhasCandidatos(): Promise<void> {
    const lista = document.querySelector<HTMLTableSectionElement>("#lista-candidatos")
    if (!lista) return

    try {
        const resposta = await fetch(`${API_NUCLEO}/api/candidatos`)

        if (!resposta.ok) {
            lista.innerHTML = '<tr><td colspan="4" class="tabela-vazia">Erro ao carregar os candidatos do servidor.</td></tr>'
            return
        }

        const dadosDoServidor = await resposta.json()

        const candidatosApi: CandidatoApi[] = Array.isArray(dadosDoServidor)
            ? dadosDoServidor
            : dadosDoServidor.dados ?? []

        if (candidatosApi.length === 0) {
            lista.innerHTML = '<tr><td colspan="4" class="tabela-vazia">Nenhum candidato encontrado.</td></tr>'
            return
        }

        const cpfs = Array.from(new Set(
            candidatosApi
                .map(candidato => candidato.cpf.replace(/\D/g, ""))
                .filter(cpf => /^\d{11}$/.test(cpf))
        ))

        const competenciasPorCpf = await buscarCompetenciasEmLote(cpfs)

        const candidatos = candidatosApi.map(candidato => {
            const cpf = candidato.cpf.replace(/\D/g, "")

            return adaptarCandidato(
                candidato,
                competenciasPorCpf[cpf] ?? []
            )
        })

        lista.innerHTML = ""

        candidatos.forEach(candidato => {
            const linha = document.createElement("tr")

            linha.innerHTML = `
                <td class="nome-hover"
                    data-email="${escaparHtml(candidato.email)}"
                    data-descricao="${escaparHtml(candidato.descricao)}">
                    ${escaparHtml(candidato.nome)}
                </td>
                <td>${escaparHtml(candidato.estado)}</td>
                <td>${escaparHtml(candidato.competencias.join(", "))}</td>
                <td>
                    <button class="btn-perfil" data-cpf="${escaparHtml(candidato.cpf)}">Ver perfil</button>
                    <button class="btn-excluir" data-cpf="${escaparHtml(candidato.cpf)}">Excluir</button>
                </td>
            `

            lista.appendChild(linha)
        })

        configurarEventosBotoes()

    } catch (erro) {
        lista.innerHTML = '<tr><td colspan="4" class="tabela-vazia">Falha na conexão com o servidor.</td></tr>'
        console.error("Erro ao carregar candidatos:", erro)
    }
}

async function buscarCompetenciasEmLote(
    cpfs: string[]
): Promise<Record<string, string[]>> {

    if (cpfs.length === 0) return {}

    const resposta = await fetch(
        `${API_NUCLEO}/api/integracao/competencias/candidatos/consulta-lote`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ cpfs })
        }
    )

    if (!resposta.ok) {
        throw new Error(`Falha ao consultar competências. HTTP ${resposta.status}`)
    }

    const dados: unknown = await resposta.json()

    const mapa = typeof dados === "object" && dados !== null && "dados" in dados
        ? dados.dados
        : dados

    if (typeof mapa !== "object" || mapa === null || Array.isArray(mapa)) {
        throw new Error("Resposta inválida ao consultar competências.")
    }

    const respostaPorCpf = mapa as Record<string, CompetenciaApi[]>

    return Object.fromEntries(
        Object.entries(respostaPorCpf).map(([cpf, competencias]) => [
            cpf.replace(/\D/g, ""),
            competencias.map(competencia => competencia.nome)
        ])
    )
}

function adaptarCandidato(
    dados: CandidatoApi,
    competencias: string[]
): Candidato {

    const nome = [dados.nome, dados.sobrenome]
        .filter(Boolean)
        .join(" ")

    return {
        ...dados,
        cpf: dados.cpf.replace(/\D/g, ""),
        nome,
        idade: dados.idade ?? calcularIdade(dados.dataNascimento),
        competencias
    } as Candidato
}

function calcularIdade(dataNascimento?: string): number {
    if (!dataNascimento) return 0

    const nascimento = new Date(`${dataNascimento}T00:00:00`)

    if (Number.isNaN(nascimento.getTime())) return 0

    const hoje = new Date()

    let idade = hoje.getFullYear() - nascimento.getFullYear()

    const aniversarioAindaNaoOcorreu =
        hoje.getMonth() < nascimento.getMonth() ||
        (
            hoje.getMonth() === nascimento.getMonth() &&
            hoje.getDate() < nascimento.getDate()
        )

    if (aniversarioAindaNaoOcorreu) idade--

    return idade
}

function configurarEventosBotoes(): void {
    const botoesPerfil = document.querySelectorAll<HTMLButtonElement>(".btn-perfil")
    const botoesExcluir = document.querySelectorAll<HTMLButtonElement>(".btn-excluir")

    botoesPerfil.forEach(botao => {
        botao.addEventListener("click", () =>
            abrirPerfil(botao.dataset.cpf)
        )
    })

    botoesExcluir.forEach(botao => {
        botao.addEventListener("click", () =>
            processarExclusao(botao.dataset.cpf)
        )
    })
}

async function abrirPerfil(cpf?: string): Promise<void> {
    if (!cpf) return

    try {
        const resposta = await fetch(
            `${API_NUCLEO}/api/candidatos/${encodeURIComponent(cpf)}`
        )

        if (!resposta.ok) {
            console.error("Candidato não encontrado no banco.")
            return
        }

        const dados: CandidatoApi = await resposta.json()
        const cpfLimpo = dados.cpf.replace(/\D/g, "")

        const competenciasPorCpf = await buscarCompetenciasEmLote([cpfLimpo])

        const candidato = adaptarCandidato(
            dados,
            competenciasPorCpf[cpfLimpo] ?? []
        )

        await renderPerfilCandidato(candidato)

    } catch (erro) {
        console.error("Erro ao abrir perfil do candidato:", erro)
    }
}

async function processarExclusao(cpf?: string): Promise<void> {
    if (!cpf) {
        console.error("CPF do candidato não encontrado.")
        return
    }

    const confirmar = await confirmarExclusao(
        "Excluir candidato?",
        "Essa ação não poderá ser desfeita e removerá os matches vinculados."
    )

    if (!confirmar) return

    try {
        const resposta = await fetch(
            `${API_NUCLEO}/api/candidatos/${encodeURIComponent(cpf)}`,
            {
                method: "DELETE"
            }
        )

        if (resposta.ok) {
            await renderListaCandidatos(
                "Candidato excluído com sucesso do banco de dados!"
            )
        } else {
            const erroApi = await resposta.json().catch(() => ({}))
            window.alert(`Erro do Servidor: ${erroApi.mensagem ?? "Não foi possível excluir o candidato."}`)
        }

    } catch (erro) {
        console.error("Erro ao excluir candidato:", erro)
        window.alert("Não foi possível excluir o candidato. Servidor offline.")
    }
}

function escaparHtml(valor: string | null | undefined): string {
    return (valor ?? "").replace(/[&<>"']/g, caractere => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[caractere]!)
}