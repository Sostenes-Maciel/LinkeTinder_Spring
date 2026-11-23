import type { Empresa } from "../model/Empresa"
import type { Candidato } from "../model/Candidato"
import { Chart } from "chart.js/auto"
import { calcularAfinidade } from "../service/CalcularAfinidade.ts"
import {
    consultarEnderecoPorCep,
    formatarEndereco
} from "../service/ConsultaCep.ts"
import { mostrarAnimacaoMatch } from "./AnimacaoMatch.ts"

const API_NUCLEO = "http://localhost:8081"
const API_COMPETENCIAS = "http://localhost:8082"
const TAMANHO_LOTE_CPFS = 500

interface StatusMatch {
    cpfCandidato: string
    idVaga: number
    status: "curtido" | "match"
}

interface CandidatoApi {
    cpf: string
    nome: string
    sobrenome?: string
    dataNascimento?: string
    idade?: number
    email: string
    pais?: string | null
    estado?: string | null
    cep?: string | null
    descricao?: string | null
}

interface VagaApi {
    idVaga: number
    titulo: string
}

interface VagaOpcao {
    id: number
    titulo: string
}

interface CompetenciaApi {
    idCompetencia?: number
    nome?: string
}

interface ResultadoCurtida {
    mensagem?: string
    match?: boolean
    idMatch?: number
}

let graficoCompetencias: Chart | null = null

export async function renderPerfilEmpresa(
    empresa: Empresa,
    mensagemSucesso?: string,
    animarTransicao: boolean = true
): Promise<void> {
    const app = document.querySelector<HTMLDivElement>("#app")
    if (!app) return

    const cnpj = empresa.cnpj.replace(/\D/g, "")

    renderizarTemplatePerfil(
        app,
        empresa,
        mensagemSucesso,
        animarTransicao
    )

    void preencherEnderecoPerfil(empresa.cep)

    const [
        competenciasEmpresa,
        candidatosDoBanco,
        statusMatches,
        vagasDaEmpresa
    ] = await Promise.all([
        buscarCompetenciasDaEmpresa(cnpj),
        buscarCandidatosDoBanco(),
        buscarStatusMatches(cnpj),
        buscarVagasDaEmpresa(cnpj)
    ])

    const empresaComCompetencias = {
        ...empresa,
        competencias: competenciasEmpresa
    } as Empresa

    renderizarCompetenciasEmpresa(competenciasEmpresa)
    renderizarGraficoCompetencias(candidatosDoBanco)

    renderizarTabelaCandidatos(
        empresaComCompetencias,
        candidatosDoBanco,
        statusMatches,
        vagasDaEmpresa
    )

    configurarBotoesCurtirCandidato(
        empresaComCompetencias,
        statusMatches
    )

    configurarFiltroCompetencia()
}

function renderizarTemplatePerfil(
    app: HTMLDivElement,
    empresa: Empresa,
    mensagemSucesso?: string,
    animarTransicao: boolean = true
): void {
    app.innerHTML = `
        <section class="perfil-empresa ${animarTransicao ? "tela-com-transicao" : ""}">
            <h1>Perfil da Empresa</h1>

            <p
                id="mensagem"
                class="${mensagemSucesso ? "mensagem-sucesso" : ""}"
            >
                ${escaparHtml(mensagemSucesso ?? "")}
            </p>

            <div class="card-perfil card-perfil-empresa">
                <header class="perfil-cabecalho">
                    <h2>${escaparHtml(empresa.nome)}</h2>
                    <p class="perfil-subtitulo">Empresa</p>
                </header>

                <div class="perfil-grade">
                    <section class="perfil-bloco">
                        <h3>Sobre a empresa:</h3>
                        <p>
                            ${escaparHtml(
        empresa.descricao ||
        "Adicione uma descrição para apresentar sua empresa."
    )}
                        </p>
                    </section>

                    <section class="perfil-bloco">
                        <h3>Competências desejadas</h3>
                        <div class="perfil-tags" id="tags-competencias-empresa">
                            <span>Carregando competências...</span>
                        </div>
                    </section>
                </div>

                <div class="perfil-resumo-empresa">
                    <p>
                        <strong>E-mail:</strong>
                        <span>${escaparHtml(empresa.email)}</span>
                    </p>

                    <p>
                        <strong>CNPJ:</strong>
                        <span>${escaparHtml(empresa.cnpj)}</span>
                    </p>

                    <p>
                        <strong>Localização:</strong>
                        <span>
                            ${escaparHtml(
        [empresa.estado, empresa.pais]
            .filter(Boolean)
            .join(", ")
    )}
                        </span>
                    </p>

                    <p>
                        <strong>Endereço:</strong>
                        <span id="endereco-perfil-empresa">Consultando CEP...</span>
                    </p>
                </div>
            </div>

            <h2>Candidatos por competência</h2>

            <div class="grafico-container">
                <canvas id="grafico-competencias"></canvas>
            </div>

            <h2>Candidatos disponíveis</h2>

            <p class="aviso-curtida-empresa">
                Selecione uma vaga antes de curtir um candidato.
            </p>

            <div class="filtro-candidatos">
                <label for="filtro-competencia">Buscar por competência</label>

                <input
                    type="text"
                    id="filtro-competencia"
                    placeholder="Ex.: Java, Python, React..."
                >
            </div>

            <table class="tabela-candidatos">
                <thead>
                    <tr>
                        <th class="coluna-candidato">Candidato</th>
                        <th class="coluna-competencias">Competências</th>
                        <th class="coluna-afinidade">Afinidade</th>
                        <th class="coluna-acao" colspan="2">Ação</th>
                    </tr>
                </thead>

                <tbody id="lista-candidatos-empresa">
                    <tr>
                        <td colspan="5">Carregando candidatos...</td>
                    </tr>
                </tbody>
            </table>
        </section>
    `
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

async function preencherEnderecoPerfil(cep: string): Promise<void> {
    const enderecoPerfil = document.querySelector<HTMLSpanElement>(
        "#endereco-perfil-empresa"
    )

    if (!enderecoPerfil) return

    if (!cep) {
        enderecoPerfil.textContent = "Endereço não informado."
        return
    }

    try {
        const endereco = await consultarEnderecoPorCep(cep)
        enderecoPerfil.textContent = formatarEndereco(endereco)

    } catch (erro) {
        console.error(
            "Não foi possível carregar o endereço da empresa:",
            erro
        )

        enderecoPerfil.textContent = "Endereço indisponível para este CEP."
    }
}

async function buscarCandidatosDoBanco(): Promise<Candidato[]> {
    try {
        const resposta = await fetch(`${API_NUCLEO}/api/candidatos`)

        if (!resposta.ok) {
            throw new Error(
                `Falha ao buscar candidatos (HTTP ${resposta.status}).`
            )
        }

        const dados: unknown = await resposta.json()
        const candidatosApi = extrairLista<CandidatoApi>(dados)

        if (!candidatosApi) {
            throw new Error("Resposta inválida ao consultar candidatos.")
        }

        if (candidatosApi.length === 0) {
            return []
        }

        const cpfs = Array.from(
            new Set(
                candidatosApi
                    .map(candidato => candidato.cpf.replace(/\D/g, ""))
                    .filter(cpf => /^\d{11}$/.test(cpf))
            )
        )

        const competenciasPorCpf =
            await buscarCompetenciasCandidatosEmLote(cpfs)

        return candidatosApi.map(candidato => {
            const cpf = candidato.cpf.replace(/\D/g, "")

            return adaptarCandidato(
                candidato,
                competenciasPorCpf[cpf] ?? []
            )
        })

    } catch (erro) {
        console.error("Erro ao buscar candidatos:", erro)
        return []
    }
}

function adaptarCandidato(
    candidato: CandidatoApi,
    competencias: string[]
): Candidato {
    return {
        cpf: candidato.cpf.replace(/\D/g, ""),
        nome: [
            candidato.nome,
            candidato.sobrenome
        ].filter(Boolean).join(" "),
        email: candidato.email,
        pais: candidato.pais ?? "",
        estado: candidato.estado ?? "",
        cep: candidato.cep ?? "",
        descricao: candidato.descricao ?? "",
        idade: candidato.idade ?? calcularIdade(candidato.dataNascimento),
        competencias
    } as Candidato
}

function calcularIdade(dataNascimento?: string): number {
    if (!dataNascimento) return 0

    const nascimento = new Date(`${dataNascimento}T00:00:00`)

    if (Number.isNaN(nascimento.getTime())) {
        return 0
    }

    const hoje = new Date()

    let idade = hoje.getFullYear() - nascimento.getFullYear()

    const aindaNaoFezAniversario =
        hoje.getMonth() < nascimento.getMonth()
        || (
            hoje.getMonth() === nascimento.getMonth()
            && hoje.getDate() < nascimento.getDate()
        )

    if (aindaNaoFezAniversario) {
        idade--
    }

    return idade
}

async function buscarCompetenciasCandidatosEmLote(
    cpfs: string[]
): Promise<Record<string, string[]>> {
    const competenciasPorCpf: Record<string, string[]> = {}

    for (
        let inicio = 0;
        inicio < cpfs.length;
        inicio += TAMANHO_LOTE_CPFS
    ) {
        const lote = cpfs.slice(
            inicio,
            inicio + TAMANHO_LOTE_CPFS
        )

        try {
            const resposta = await fetch(
                `${API_NUCLEO}/api/integracao/competencias/candidatos/consulta-lote`,
                {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json"
                    },
                    body: JSON.stringify({
                        cpfs: lote
                    })
                }
            )

            if (!resposta.ok) {
                throw new Error(
                    `Falha ao consultar competências em lote (HTTP ${resposta.status}).`
                )
            }

            const dados: unknown = await resposta.json()
            const mapa = extrairMapaCompetencias(dados)

            lote.forEach(cpf => {
                competenciasPorCpf[cpf] = extrairNomesCompetencias(
                    mapa[cpf] ?? []
                )
            })

        } catch (erro) {
            console.error(
                `Erro ao consultar competências do lote iniciado em ${inicio}:`,
                erro
            )

            lote.forEach(cpf => {
                competenciasPorCpf[cpf] = []
            })
        }
    }

    return competenciasPorCpf
}

function extrairMapaCompetencias(
    resultado: unknown
): Record<string, unknown> {
    const dados = extrairDados(resultado)

    if (
        typeof dados !== "object"
        || dados === null
        || Array.isArray(dados)
    ) {
        throw new Error(
            "Resposta inválida da consulta de competências em lote."
        )
    }

    return dados as Record<string, unknown>
}

async function buscarCompetenciasDaEmpresa(
    cnpj: string
): Promise<string[]> {
    try {
        const resposta = await fetch(
            `${API_COMPETENCIAS}/api/empresas/${encodeURIComponent(cnpj)}/competencias`
        )

        if (!resposta.ok) {
            throw new Error(
                `Falha ao buscar competências da empresa (HTTP ${resposta.status}).`
            )
        }

        const dados: unknown = await resposta.json()
        const competencias = extrairLista<CompetenciaApi>(dados)

        return extrairNomesCompetencias(competencias ?? [])

    } catch (erro) {
        console.error(
            "Erro ao buscar competências da empresa:",
            erro
        )

        return []
    }
}

function extrairNomesCompetencias(
    competencias: unknown
): string[] {
    if (!Array.isArray(competencias)) {
        return []
    }

    return competencias
        .map(competencia => {
            if (typeof competencia === "string") {
                return competencia.trim()
            }

            if (
                typeof competencia === "object"
                && competencia !== null
                && "nome" in competencia
            ) {
                return String(competencia.nome).trim()
            }

            return ""
        })
        .filter(nome => nome.length > 0)
}

function renderizarCompetenciasEmpresa(
    competencias: string[]
): void {
    const container = document.querySelector<HTMLDivElement>(
        "#tags-competencias-empresa"
    )

    if (!container) return

    if (competencias.length === 0) {
        container.innerHTML = "<span>Nenhuma competência informada</span>"
        return
    }

    container.innerHTML = competencias
        .map(competencia => `
            <span class="perfil-tag">
                ${escaparHtml(competencia)}
            </span>
        `)
        .join("")
}

function renderizarGraficoCompetencias(
    candidatos: Candidato[]
): void {
    const canvas = document.querySelector<HTMLCanvasElement>(
        "#grafico-competencias"
    )

    if (!canvas) return

    if (graficoCompetencias) {
        graficoCompetencias.destroy()
        graficoCompetencias = null
    }

    const quantidadePorCompetencia: Record<string, number> = {}

    candidatos.forEach(candidato => {
        candidato.competencias.forEach(competencia => {
            const chave = competencia.trim().toLowerCase()

            if (!chave) return

            quantidadePorCompetencia[chave] =
                (quantidadePorCompetencia[chave] || 0) + 1
        })
    })

    if (Object.keys(quantidadePorCompetencia).length === 0) {
        return
    }

    const context = canvas.getContext("2d")
    if (!context) return

    const gradiente = context.createLinearGradient(
        0,
        0,
        0,
        canvas.height
    )

    gradiente.addColorStop(0, "#20b486")
    gradiente.addColorStop(1, "#2855a6")

    graficoCompetencias = new Chart(canvas, {
        type: "bar",
        data: {
            labels: Object.keys(quantidadePorCompetencia),
            datasets: [{
                label: "Quantidade de candidatos",
                data: Object.values(quantidadePorCompetencia),
                backgroundColor: gradiente,
                hoverBackgroundColor: "#168a68",
                borderRadius: 8,
                borderSkipped: false,
                maxBarThickness: 48
            }]
        },
        options: {
            responsive: true,
            animation: {
                duration: window.matchMedia(
                    "(prefers-reduced-motion: reduce)"
                ).matches ? 0 : 900,
                easing: "easeOutQuart",
                delay: contexto => contexto.dataIndex * 45
            },
            plugins: {
                legend: {
                    labels: {
                        color: "#25324b",
                        usePointStyle: true,
                        pointStyle: "rectRounded",
                        padding: 20
                    }
                },
                tooltip: {
                    backgroundColor: "#173b80",
                    titleColor: "#ffffff",
                    bodyColor: "#ffffff",
                    padding: 12,
                    cornerRadius: 10,
                    displayColors: false
                }
            },
            scales: {
                x: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        color: "#526176"
                    }
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1,
                        color: "#526176"
                    },
                    grid: {
                        color: "rgba(23, 59, 128, 0.08)"
                    }
                }
            }
        }
    })
}

function renderizarTabelaCandidatos(
    empresa: Empresa,
    candidatos: Candidato[],
    statusMatches: StatusMatch[],
    vagas: VagaOpcao[]
): void {
    const lista = document.querySelector<HTMLTableSectionElement>(
        "#lista-candidatos-empresa"
    )

    if (!lista) return

    if (candidatos.length === 0) {
        lista.innerHTML = `
            <tr>
                <td colspan="5" class="tabela-vazia">
                    Nenhum candidato disponível no momento.
                </td>
            </tr>
        `
        return
    }

    lista.innerHTML = ""

    candidatos.forEach(candidato => {
        const linha = document.createElement("tr")

        const cpfLimpo = candidato.cpf.replace(/\D/g, "")

        const statusCandidato = statusMatches.filter(status =>
            status.cpfCandidato.replace(/\D/g, "") === cpfLimpo
        )

        const statusMatch = statusCandidato.find(
            status => status.status === "match"
        )

        const statusPendente = statusCandidato.find(
            status => status.status === "curtido"
        )

        const vagaSelecionada =
            statusMatch?.idVaga
            ?? statusPendente?.idVaga
            ?? ""

        const statusAtual = statusCandidato.find(
            status => status.idVaga === vagaSelecionada
        )?.status

        const temMatch = statusAtual === "match"
        const curtiu = statusAtual === "curtido" || temMatch

        const nomeInicial = temMatch
            ? candidato.nome
            : "Match necessário para visualização"

        const opcoesVaga = vagas.map(vaga => `
            <option
                value="${vaga.id}"
                ${vaga.id === vagaSelecionada ? "selected" : ""}
            >
                ${escaparHtml(vaga.titulo)}
            </option>
        `).join("")

        linha.dataset.competencias = candidato.competencias
            .join(" ")
            .toLowerCase()

        linha.dataset.nomeReal = candidato.nome

        linha.innerHTML = `
            <td class="coluna-candidato">
                ${escaparHtml(nomeInicial)}
            </td>

            <td class="coluna-competencias">
                ${escaparHtml(candidato.competencias.join(", "))}
            </td>

            <td class="coluna-afinidade">
                ${calcularAfinidade(candidato, empresa)}%
            </td>

            <td class="coluna-acao">
                <button
                    class="btn-curtir-candidato"
                    data-cpf="${escaparHtml(cpfLimpo)}"
                    data-id-vaga="${vagaSelecionada}"
                    data-curtido="${curtiu}"
                    data-match="${temMatch}"
                    title="${
            temMatch
                ? "Match confirmado"
                : curtiu
                    ? "Remover curtida"
                    : "Curtir candidato"
        }"
                    ${!vagaSelecionada || temMatch ? "disabled" : ""}
                >
                    ${temMatch ? "🤝" : curtiu ? "♥" : "♡"}
                </button>
            </td>

            <td class="coluna-vaga">
                <div class="acoes-curtida-empresa">
                    <select
                        class="select-vaga-curtir"
                        data-cpf="${escaparHtml(cpfLimpo)}"
                        aria-label="Selecionar vaga para curtir candidato"
                        ${vagas.length === 0 ? "disabled" : ""}
                    >
                        <option
                            value=""
                            ${vagaSelecionada ? "" : "selected"}
                            disabled
                        >
                            Selecionar vaga
                        </option>

                        ${opcoesVaga}
                    </select>
                </div>
            </td>
        `

        lista.appendChild(linha)
    })
}

function configurarBotoesCurtirCandidato(
    empresa: Empresa,
    statusMatches: StatusMatch[]
): void {
    const botoesCurtir = document.querySelectorAll<HTMLButtonElement>(
        ".btn-curtir-candidato"
    )

    botoesCurtir.forEach(botao => {
        const linha = botao.closest<HTMLTableRowElement>("tr")

        const seletorVaga = linha?.querySelector<HTMLSelectElement>(
            ".select-vaga-curtir"
        )

        const cpf = botao.dataset.cpf?.replace(/\D/g, "")

        if (!linha || !seletorVaga || !cpf) return

        seletorVaga.addEventListener("change", () => {
            animarEscolhaVaga(seletorVaga)

            const idVagaSelecionada = Number(seletorVaga.value)

            const status = buscarStatusMatchesEmMemoria(
                statusMatches,
                cpf,
                idVagaSelecionada
            )

            atualizarBotaoCurtida(botao, status, Boolean(seletorVaga.value))

            const celulaNome = linha.querySelector<HTMLTableCellElement>(
                ".coluna-candidato"
            )

            if (celulaNome) {
                celulaNome.textContent = status === "match"
                    ? linha.dataset.nomeReal ?? "Candidato"
                    : "Match necessário para visualização"
            }

            botao.dataset.idVaga = seletorVaga.value
        })

        botao.addEventListener("click", async evento => {
            evento.preventDefault()

            const cpfCandidato = botao.dataset.cpf
            const idVaga = Number(seletorVaga.value)

            if (!cpfCandidato) return

            if (!Number.isInteger(idVaga) || idVaga <= 0) {
                alert("Selecione uma vaga válida para curtir este candidato.")
                return
            }

            if (botao.dataset.match === "true") {
                return
            }

            const jaCurtido = botao.dataset.curtido === "true"
            const metodoHTTP = jaCurtido ? "DELETE" : "POST"

            const payload = {
                autor: "EMPRESA",
                cnpjEmpresa: empresa.cnpj.replace(/\D/g, ""),
                cpfCandidato: cpfCandidato.replace(/\D/g, ""),
                idVaga
            }

            botao.disabled = true

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
                    const erro = await extrairMensagemErro(resposta)

                    throw new Error(
                        erro || `Falha ao processar curtida (HTTP ${resposta.status}).`
                    )
                }

                let resultado: ResultadoCurtida = {}

                if (!jaCurtido) {
                    resultado = await resposta.json()
                }

                const deuMatch = resultado.match === true

                const novoStatus = jaCurtido
                    ? undefined
                    : deuMatch
                        ? "match"
                        : "curtido"

                atualizarStatusEmMemoria(
                    statusMatches,
                    cpfCandidato,
                    idVaga,
                    novoStatus
                )

                atualizarBotaoCurtida(
                    botao,
                    novoStatus,
                    Boolean(seletorVaga.value)
                )

                const celulaNome = linha.querySelector<HTMLTableCellElement>(
                    ".coluna-candidato"
                )

                if (celulaNome) {
                    celulaNome.textContent = novoStatus === "match"
                        ? linha.dataset.nomeReal ?? "Candidato"
                        : "Match necessário para visualização"
                }

                if (deuMatch) {
                    mostrarAnimacaoMatch()
                } else if (novoStatus === "curtido") {
                    animarCurtida(botao)
                }

            } catch (erro) {
                console.error("Erro ao processar ação:", erro)

                alert(
                    erro instanceof Error
                        ? erro.message
                        : "Falha ao processar curtida."
                )

            } finally {
                botao.disabled =
                    botao.dataset.match === "true"
                    || !seletorVaga.value
            }
        })
    })
}

function buscarStatusMatchesEmMemoria(
    statusMatches: StatusMatch[],
    cpf: string,
    idVaga: number
): "curtido" | "match" | undefined {
    return statusMatches.find(status =>
        status.cpfCandidato.replace(/\D/g, "") === cpf
        && status.idVaga === idVaga
    )?.status
}

function atualizarStatusEmMemoria(
    statusMatches: StatusMatch[],
    cpf: string,
    idVaga: number,
    status?: "curtido" | "match"
): void {
    const indice = statusMatches.findIndex(item =>
        item.cpfCandidato.replace(/\D/g, "") === cpf
        && item.idVaga === idVaga
    )

    if (indice >= 0) {
        statusMatches.splice(indice, 1)
    }

    if (status) {
        statusMatches.push({
            cpfCandidato: cpf,
            idVaga,
            status
        })
    }
}

function atualizarBotaoCurtida(
    botao: HTMLButtonElement,
    status?: "curtido" | "match",
    vagaSelecionada: boolean = true
): void {
    const temMatch = status === "match"
    const curtiu = status === "curtido" || temMatch

    botao.innerHTML = temMatch
        ? "🤝"
        : curtiu
            ? "♥"
            : "♡"

    botao.title = temMatch
        ? "Match confirmado"
        : curtiu
            ? "Remover curtida"
            : "Curtir candidato"

    botao.dataset.curtido = String(curtiu)
    botao.dataset.match = String(temMatch)
    botao.disabled = temMatch || !vagaSelecionada
}

function animarCurtida(botao: HTMLButtonElement): void {
    botao.classList.remove("curtida-animada")
    void botao.offsetWidth
    botao.classList.add("curtida-animada")
}

function animarEscolhaVaga(seletor: HTMLSelectElement): void {
    const acoes = seletor.closest<HTMLElement>(
        ".acoes-curtida-empresa"
    )

    acoes?.classList.remove("vaga-selecionada")
    seletor.classList.remove("vaga-escolhida")

    void seletor.offsetWidth

    acoes?.classList.add("vaga-selecionada")
    seletor.classList.add("vaga-escolhida")
}

function configurarFiltroCompetencia(): void {
    const filtroCompetencia = document.querySelector<HTMLInputElement>(
        "#filtro-competencia"
    )

    const lista = document.querySelector<HTMLTableSectionElement>(
        "#lista-candidatos-empresa"
    )

    filtroCompetencia?.addEventListener("input", () => {
        const termo = filtroCompetencia.value.trim().toLowerCase()

        const linhas = lista?.querySelectorAll<HTMLTableRowElement>("tr") ?? []

        linhas.forEach(linha => {
            const competencias = linha.dataset.competencias ?? ""
            linha.hidden = !competencias.includes(termo)
        })
    })
}

async function buscarStatusMatches(cnpj: string): Promise<StatusMatch[]> {
    try {
        const resposta = await fetch(
            `${API_NUCLEO}/api/matches?cnpj=${encodeURIComponent(cnpj)}`
        )

        if (!resposta.ok) {
            throw new Error(
                `Falha ao consultar status das curtidas (HTTP ${resposta.status}).`
            )
        }

        const dados: unknown = await resposta.json()
        const status = extrairLista<StatusMatch>(dados)

        if (!status) {
            throw new Error("Resposta inválida ao consultar status das curtidas.")
        }

        return status.map(item => ({
            cpfCandidato: String(item.cpfCandidato ?? "").replace(/\D/g, ""),
            idVaga: Number(item.idVaga),
            status: item.status
        })).filter(item =>
            item.cpfCandidato.length === 11
            && Number.isInteger(item.idVaga)
            && ["curtido", "match"].includes(item.status)
        )

    } catch (erro) {
        console.error("Erro ao buscar status dos Matches:", erro)
        return []
    }
}

async function buscarVagasDaEmpresa(
    cnpj: string
): Promise<VagaOpcao[]> {
    try {
        const resposta = await fetch(
            `${API_NUCLEO}/api/vagas/empresa/${encodeURIComponent(cnpj)}`
        )

        if (!resposta.ok) {
            throw new Error(
                `Falha ao buscar vagas da empresa (HTTP ${resposta.status}).`
            )
        }

        const dados: unknown = await resposta.json()
        const vagas = extrairLista<VagaApi>(dados)

        if (!vagas) {
            throw new Error("Resposta inválida ao consultar vagas da empresa.")
        }

        return vagas.map(vaga => ({
            id: Number(vaga.idVaga),
            titulo: String(vaga.titulo)
        })).filter(vaga =>
            Number.isInteger(vaga.id)
            && vaga.id > 0
        )

    } catch (erro) {
        console.error("Erro ao buscar vagas da empresa:", erro)
        return []
    }
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

function extrairDados(resultado: unknown): unknown {
    if (
        typeof resultado === "object"
        && resultado !== null
        && "dados" in resultado
    ) {
        return resultado.dados
    }

    return resultado
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

export default renderPerfilEmpresa