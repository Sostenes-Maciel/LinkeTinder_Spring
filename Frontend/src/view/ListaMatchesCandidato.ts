interface EmpresaResumo {
    cnpj?: string
    nome?: string
}

interface VagaResumo {
    id?: number
    idVaga?: number
    titulo?: string
    pais?: string
    estado?: string
}

interface MatchCandidato {
    idMatch?: number
    empresa?: EmpresaResumo
    vaga?: VagaResumo
}

interface CurtidaPendenteCandidato {
    vaga?: VagaResumo
}

interface RespostaMatches {
    matches?: MatchCandidato[]
    curtidasPendentes?: CurtidaPendenteCandidato[]
    dados?: RespostaMatches
}

const API_NUCLEO = "http://localhost:8081"

export async function renderListaMatchesCandidato(
    candidato: { cpf: string }
): Promise<void> {
    const app = document.querySelector<HTMLDivElement>("#app")
    if (!app) return

    app.innerHTML = `
        <section class="lista-matches tela-com-transicao">
            <h2>Carregando matches...</h2>
        </section>
    `

    try {
        const cpfLimpo = candidato.cpf.replace(/\D/g, "")

        if (!/^\d{11}$/.test(cpfLimpo)) {
            throw new Error("CPF inválido. Faça login novamente.")
        }

        const resposta = await fetch(
            `${API_NUCLEO}/api/matches?cpf=${encodeURIComponent(cpfLimpo)}&completo=true`
        )

        if (!resposta.ok) {
            throw new Error(
                `Falha ao consultar Matches (HTTP ${resposta.status}).`
            )
        }

        const resultado: RespostaMatches = await resposta.json()
        const dados = resultado.dados ?? resultado

        const matches = Array.isArray(dados.matches)
            ? dados.matches
            : []

        const curtidasPendentes = Array.isArray(dados.curtidasPendentes)
            ? dados.curtidasPendentes
            : []

        app.innerHTML = `
            <section class="lista-matches tela-com-transicao">
                <h1>Meus Matches</h1>

                <h2>Curtidas aguardando Match</h2>
                ${gerarSecaoCurtidasPendentes(curtidasPendentes)}

                <h2>Meus Matches</h2>
                ${gerarSecaoMatchesRealizados(matches)}
            </section>
        `

    } catch (erro) {
        console.error("Erro ao buscar Matches completos:", erro)

        app.innerHTML = `
            <section class="lista-matches tela-com-transicao">
                <h2>Não foi possível carregar seus Matches.</h2>
                <p>
                    ${escaparHtml(
            erro instanceof Error
                ? erro.message
                : "Verifique a conexão com o servidor."
        )}
                </p>
            </section>
        `
    }
}

function gerarSecaoCurtidasPendentes(
    curtidas: CurtidaPendenteCandidato[]
): string {
    if (curtidas.length === 0) {
        return `
            <div class="estado-vazio">
                <span class="estado-vazio-icone">♥</span>
                <h3>Nenhuma curtida aguardando Match</h3>
                <p>
                    As vagas que você curtir e ainda não tiverem
                    reciprocidade aparecerão aqui.
                </p>
            </div>
        `
    }

    const cardsHtml = curtidas.map(curtida => `
        <div class="card-match card-curtida-pendente">
            <h3>♥ ${escaparHtml(curtida.vaga?.titulo ?? "Vaga")}</h3>

            <p>
                <strong>Empresa:</strong>
                Match necessário para visualização
            </p>
        </div>
    `).join("")

    return `<div class="cards-matches">${cardsHtml}</div>`
}

function gerarSecaoMatchesRealizados(
    matches: MatchCandidato[]
): string {
    if (matches.length === 0) {
        return `
            <div class="estado-vazio">
                <span class="estado-vazio-icone">🤝</span>
                <h3>Nenhum Match ainda</h3>
                <p>
                    Quando houver interesse dos dois lados,
                    o seu Match aparecerá aqui.
                </p>
            </div>
        `
    }

    const cardsHtml = matches.map(match => `
        <div class="card-match card-match-confirmado">
            <h3>
                🤝 ${escaparHtml(match.empresa?.nome ?? "Empresa")}
            </h3>

            <p>
                <strong>Vaga:</strong>
                ${escaparHtml(match.vaga?.titulo ?? "Vaga")}
            </p>

            <p>
                <strong>Local:</strong>
                ${escaparHtml(match.vaga?.pais ?? "")}
                -
                ${escaparHtml(match.vaga?.estado ?? "")}
            </p>
        </div>
    `).join("")

    return `<div class="cards-matches">${cardsHtml}</div>`
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