import renderPerfilVaga from "./PerfilVaga.ts"
import type { Candidato } from "../model/Candidato.ts"
import { calcularAfinidadePorCompetencias } from "../service/CalcularAfinidade.ts"
import { mostrarAnimacaoMatch } from "./AnimacaoMatch.ts"

const API_NUCLEO = "http://localhost:8081"
const API_COMPETENCIAS = "http://localhost:8082"

interface StatusMatchVaga {
    idVaga: number
    status: "curtido" | "match"
    nomeEmpresa?: string | null
}

interface VagaApi {
    idVaga: number
    titulo: string
    pais?: string | null
    estado?: string | null
    descricao?: string | null
}

interface CompetenciaApi {
    idCompetencia?: number
    nome?: string
}

interface VagaLista {
    id: number
    titulo: string
    pais: string
    estado: string
    descricao: string
    competencias: string[]
}

interface ResultadoCurtida {
    mensagem?: string
    match?: boolean
    idMatch?: number
}

export async function renderListaVagas(
    candidato?: Candidato,
    animarTransicao: boolean = true
): Promise<void> {
    const app = document.querySelector<HTMLDivElement>("#app")
    if (!app) return

    renderizarTemplateBase(app, animarTransicao)

    const cpf = candidato?.cpf.replace(/\D/g, "")

    const [vagasDoBanco, statusMatches] = await Promise.all([
        buscarVagasDoBanco(),
        cpf
            ? buscarStatusMatchesCandidato(cpf)
            : Promise.resolve([])
    ])

    renderizarLinhasVagas(vagasDoBanco, statusMatches, candidato)

    configurarEventos(
        vagasDoBanco,
        statusMatches,
        candidato
    )
}

function renderizarTemplateBase(
    app: HTMLDivElement,
    animarTransicao: boolean
): void {
    app.innerHTML = `
        <section class="lista-vagas ${animarTransicao ? "tela-com-transicao" : ""}">
            <h1>Vagas disponíveis</h1>

            <div class="filtro-vagas">
                <label for="input-filtro-vagas">Buscar vaga</label>

                <input
                    type="text"
                    id="input-filtro-vagas"
                    placeholder="Ex.: Java, Python, Tech Global..."
                >
            </div>

            <table class="tabela-vagas">
                <thead>
                    <tr>
                        <th>Vaga</th>
                        <th>Empresa</th>
                        <th>Afinidade</th>
                        <th>Ação</th>
                    </tr>
                </thead>

                <tbody id="lista-vagas-corpo">
                    <tr>
                        <td colspan="4">Carregando vagas...</td>
                    </tr>
                </tbody>
            </table>
        </section>
    `
}

async function buscarVagasDoBanco(): Promise<VagaLista[]> {
    try {
        const resposta = await fetch(`${API_NUCLEO}/api/vagas`)

        if (!resposta.ok) {
            throw new Error(
                `Falha ao buscar vagas (HTTP ${resposta.status}).`
            )
        }

        const dados: unknown = await resposta.json()
        const vagasApi = extrairLista<VagaApi>(dados)

        if (!vagasApi) {
            throw new Error("Resposta inválida ao consultar vagas.")
        }

        return await Promise.all(
            vagasApi.map(async vaga => {
                const id = Number(vaga.idVaga)

                return {
                    id,
                    titulo: vaga.titulo,
                    pais: vaga.pais ?? "",
                    estado: vaga.estado ?? "",
                    descricao: vaga.descricao ?? "",
                    competencias: await buscarCompetenciasVaga(id)
                }
            })
        )

    } catch (erro) {
        console.error("Erro ao buscar vagas:", erro)
        return []
    }
}

async function buscarCompetenciasVaga(
    idVaga: number
): Promise<string[]> {
    try {
        const resposta = await fetch(
            `${API_COMPETENCIAS}/api/vagas/${encodeURIComponent(idVaga)}/competencias`
        )

        if (!resposta.ok) {
            throw new Error(
                `Falha ao buscar competências da vaga ${idVaga} (HTTP ${resposta.status}).`
            )
        }

        const dados: unknown = await resposta.json()
        const competencias = extrairLista<CompetenciaApi>(dados)

        if (!competencias) return []

        return competencias
            .map(competencia => competencia.nome?.trim() ?? "")
            .filter(nome => nome.length > 0)

    } catch (erro) {
        console.error(
            `Erro ao buscar competências da vaga ${idVaga}:`,
            erro
        )

        return []
    }
}

async function buscarStatusMatchesCandidato(
    cpf: string
): Promise<StatusMatchVaga[]> {
    try {
        const resposta = await fetch(
            `${API_NUCLEO}/api/matches?cpf=${encodeURIComponent(cpf)}`
        )

        if (!resposta.ok) {
            throw new Error(
                `Falha ao buscar status das curtidas (HTTP ${resposta.status}).`
            )
        }

        const dados: unknown = await resposta.json()
        const status = extrairLista<StatusMatchVaga>(dados)

        if (!status) {
            throw new Error(
                "Resposta inválida ao consultar status das curtidas."
            )
        }

        return status
            .map(item => ({
                idVaga: Number(item.idVaga),
                status: item.status,
                nomeEmpresa: item.nomeEmpresa ?? null
            }))
            .filter(item =>
                Number.isInteger(item.idVaga)
                && item.idVaga > 0
                && ["curtido", "match"].includes(item.status)
            )

    } catch (erro) {
        console.error(
            "Erro ao buscar matches do candidato:",
            erro
        )

        return []
    }
}

function renderizarLinhasVagas(
    vagas: VagaLista[],
    statusMatches: StatusMatchVaga[],
    candidato?: Candidato
): void {
    const lista = document.querySelector<HTMLTableSectionElement>(
        "#lista-vagas-corpo"
    )

    if (!lista) return

    if (vagas.length === 0) {
        lista.innerHTML = `
            <tr>
                <td colspan="4" class="tabela-vazia">
                    Nenhuma vaga cadastrada.
                </td>
            </tr>
        `
        return
    }

    lista.innerHTML = ""

    vagas.forEach(vaga => {
        const linha = document.createElement("tr")

        const status = statusMatches.find(
            item => item.idVaga === vaga.id
        )

        const temMatch = status?.status === "match"
        const curtiu = status?.status === "curtido" || temMatch

        const nomeEmpresa = temMatch
            ? status?.nomeEmpresa || "Match confirmado"
            : "Match necessário para visualização"

        const afinidade = candidato
            ? calcularAfinidadePorCompetencias(
                candidato.competencias,
                vaga.competencias
            )
            : null

        linha.dataset.busca = [
            vaga.titulo,
            ...(vaga.competencias ?? [])
        ].join(" ").toLocaleLowerCase("pt-BR")

        linha.innerHTML = `
            <td>${escaparHtml(vaga.titulo)}</td>

            <td class="coluna-empresa">
                ${escaparHtml(nomeEmpresa)}
            </td>

            <td>
                ${afinidade !== null ? `${afinidade}%` : "—"}
            </td>

            <td>
                <button
                    class="btn-curtir-vaga"
                    data-id="${vaga.id}"
                    data-curtido="${curtiu}"
                    data-match="${temMatch}"
                    title="${
            temMatch
                ? "Match confirmado!"
                : curtiu
                    ? "Descurtir"
                    : "Curtir vaga"
        }"
                    ${temMatch ? "disabled" : ""}
                >
                    ${temMatch ? "🤝" : curtiu ? "♥" : "♡"}
                </button>

                <button
                    class="btn-perfil-vaga"
                    data-id="${vaga.id}"
                >
                    Ver vaga
                </button>
            </td>
        `

        lista.appendChild(linha)
    })
}

function configurarEventos(
    vagas: VagaLista[],
    statusMatches: StatusMatchVaga[],
    candidato?: Candidato
): void {
    configurarFiltroBusca()

    configurarBotoesCurtir(
        candidato,
        statusMatches
    )

    configurarBotoesPerfil(
        vagas,
        statusMatches,
        candidato
    )
}

function configurarFiltroBusca(): void {
    const inputFiltro = document.querySelector<HTMLInputElement>(
        "#input-filtro-vagas"
    )

    const lista = document.querySelector<HTMLTableSectionElement>(
        "#lista-vagas-corpo"
    )

    inputFiltro?.addEventListener("input", () => {
        const termo = inputFiltro.value.trim().toLowerCase()

        const linhas = lista?.querySelectorAll<HTMLTableRowElement>(
            "tr"
        ) ?? []

        linhas.forEach(linha => {
            const textoBusca = linha.dataset.busca ?? ""
            linha.hidden = !textoBusca.includes(termo)
        })
    })
}

function configurarBotoesCurtir(
    candidato: Candidato | undefined,
    statusMatches: StatusMatchVaga[]
): void {
    const botoesCurtir = document.querySelectorAll<HTMLButtonElement>(
        ".btn-curtir-vaga"
    )

    botoesCurtir.forEach(botao => {
        botao.addEventListener("click", async evento => {
            evento.preventDefault()

            if (!candidato?.cpf) {
                alert(
                    "Sessão de candidato inválida. Faça login novamente."
                )
                return
            }

            const vagaId = Number(botao.dataset.id)

            if (!Number.isInteger(vagaId) || vagaId <= 0) {
                alert("Vaga inválida.")
                return
            }

            if (botao.dataset.match === "true") {
                return
            }

            const jaCurtido = botao.dataset.curtido === "true"
            const metodoHTTP = jaCurtido ? "DELETE" : "POST"

            const cpfLimpo = candidato.cpf.replace(/\D/g, "")

            const payload = {
                autor: "CANDIDATO",
                cpfCandidato: cpfLimpo,
                idVaga: vagaId
            }

            const iconeOriginal = botao.innerHTML

            botao.disabled = true
            botao.innerHTML = "..."

            try {
                const url = jaCurtido
                    ? `${API_NUCLEO}/api/matches`
                    : `${API_NUCLEO}/api/curtidas`

                const resposta = await fetch(url, {
                    method: metodoHTTP,
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify(payload)
                })

                if (!resposta.ok) {
                    const mensagem = await extrairMensagemErro(resposta)

                    throw new Error(
                        mensagem || `Falha ao processar curtida (HTTP ${resposta.status}).`
                    )
                }

                if (jaCurtido) {
                    atualizarStatusEmMemoria(
                        statusMatches,
                        vagaId,
                        undefined
                    )

                    botao.innerHTML = "♡"
                    botao.title = "Curtir vaga"
                    botao.dataset.curtido = "false"
                    botao.dataset.match = "false"
                    botao.classList.remove("curtida-animada")

                    atualizarEmpresaNaLinha(botao, undefined)

                } else {
                    const resultado = await resposta.json() as ResultadoCurtida

                    const deuMatch = resultado.match === true

                    atualizarStatusEmMemoria(
                        statusMatches,
                        vagaId,
                        deuMatch ? "match" : "curtido"
                    )

                    botao.dataset.curtido = "true"
                    botao.dataset.match = String(deuMatch)

                    if (deuMatch) {
                        const statusAtualizado =
                            await buscarStatusMatchesCandidato(cpfLimpo)

                        const statusDaVaga = statusAtualizado.find(
                            status => status.idVaga === vagaId
                        )

                        if (statusDaVaga) {
                            atualizarStatusEmMemoria(
                                statusMatches,
                                vagaId,
                                "match",
                                statusDaVaga.nomeEmpresa
                            )
                        }

                        botao.innerHTML = "🤝"
                        botao.title = "Match confirmado!"
                        botao.disabled = true

                        atualizarEmpresaNaLinha(
                            botao,
                            statusDaVaga?.nomeEmpresa
                        )

                        mostrarAnimacaoMatch()

                    } else {
                        botao.innerHTML = "♥"
                        botao.title = "Descurtir"
                        animarCurtida(botao)
                    }
                }

            } catch (erro) {
                console.error("Erro ao processar ação:", erro)

                alert(
                    erro instanceof Error
                        ? erro.message
                        : "Falha na comunicação com o servidor."
                )

                botao.innerHTML = iconeOriginal

            } finally {
                if (botao.dataset.match !== "true") {
                    botao.disabled = false
                }
            }
        })
    })
}

function atualizarStatusEmMemoria(
    statusMatches: StatusMatchVaga[],
    idVaga: number,
    status?: "curtido" | "match",
    nomeEmpresa?: string | null
): void {
    const indice = statusMatches.findIndex(
        item => item.idVaga === idVaga
    )

    if (indice >= 0) {
        statusMatches.splice(indice, 1)
    }

    if (status) {
        statusMatches.push({
            idVaga,
            status,
            nomeEmpresa: nomeEmpresa ?? null
        })
    }
}

function atualizarEmpresaNaLinha(
    botao: HTMLButtonElement,
    nomeEmpresa?: string | null
): void {
    const celula = botao.closest("tr")?.querySelector<HTMLTableCellElement>(
        ".coluna-empresa"
    )

    if (!celula) return

    celula.textContent = nomeEmpresa?.trim()
        ? nomeEmpresa
        : "Match necessário para visualização"
}

function animarCurtida(botao: HTMLButtonElement): void {
    botao.classList.remove("curtida-animada")
    void botao.offsetWidth
    botao.classList.add("curtida-animada")
}

function configurarBotoesPerfil(
    vagas: VagaLista[],
    statusMatches: StatusMatchVaga[],
    candidato?: Candidato
): void {
    const botoesPerfil = document.querySelectorAll<HTMLButtonElement>(
        ".btn-perfil-vaga"
    )

    botoesPerfil.forEach(botao => {
        botao.addEventListener("click", () => {
            const vagaId = Number(botao.dataset.id)

            const vaga = vagas.find(
                item => item.id === vagaId
            )

            if (!vaga) return

            const status = statusMatches.find(
                item => item.idVaga === vagaId
            )

            const temMatch = status?.status === "match"

            void renderPerfilVaga(
                vagaId,
                () => {
                    void renderListaVagas(candidato)
                },
                temMatch,
                status?.nomeEmpresa ?? undefined
            )
        })
    })
}

function extrairLista<T>(resultado: unknown): T[] | undefined {
    if (Array.isArray(resultado)) {
        return resultado as T[]
    }

    if (typeof resultado !== "object" || resultado === null) {
        return undefined
    }

    if ("dados" in resultado && Array.isArray(resultado.dados)) {
        return resultado.dados as T[]
    }

    return undefined
}

async function extrairMensagemErro(
    resposta: Response
): Promise<string | undefined> {
    try {
        const dados = await resposta.json() as {
            mensagem?: string
            message?: string
        }

        return dados.mensagem ?? dados.message

    } catch {
        return undefined
    }
}

function escaparHtml(valor: string): string {
    return valor.replace(/[&<>"']/g, caractere => ({
        "&": "&amp;",
        "<": "&lt;",
        ">": "&gt;",
        '"': "&quot;",
        "'": "&#39;"
    })[caractere]!)
}