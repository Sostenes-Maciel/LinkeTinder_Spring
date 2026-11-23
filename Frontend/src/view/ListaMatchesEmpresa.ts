interface CandidatoResumo {
    cpf?: string
    nome?: string
    sobrenome?: string
}

interface VagaResumo {
    id?: number
    idVaga?: number
    titulo?: string
    pais?: string
    estado?: string
}

interface RecrutadorResumo {
    nome?: string
}

interface MatchEmpresa {
    idMatch?: number
    candidato?: CandidatoResumo
    vaga?: VagaResumo
}

interface CurtidaPendenteEmpresa {
    candidato?: CandidatoResumo
    vaga?: VagaResumo
    recrutador?: RecrutadorResumo
}

interface RespostaMatches {
    matches?: MatchEmpresa[]
    curtidasPendentes?: CurtidaPendenteEmpresa[]
    dados?: RespostaMatches
}

const API_NUCLEO = "http://localhost:8081"

export async function renderListaMatchesEmpresa(
    empresa: { cnpj: string }
): Promise<void> {
    const app = document.querySelector<HTMLDivElement>("#app")
    if (!app) return

    app.innerHTML = `
        <section class="lista-matches tela-com-transicao">
            <h2>Carregando matches...</h2>
        </section>
    `

    try {
        const cnpjLimpo = empresa.cnpj.replace(/\D/g, "")

        if (!/^\d{14}$/.test(cnpjLimpo)) {
            throw new Error("CNPJ inválido. Faça login novamente.")
        }

        const resposta = await fetch(
            `${API_NUCLEO}/api/matches?cnpj=${encodeURIComponent(cnpjLimpo)}&completo=true`
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
        console.error(
            "Erro ao buscar Matches completos da empresa:",
            erro
        )

        app.innerHTML = `
            <section class="lista-matches tela-com-transicao">
                <h2>Não foi possível carregar os Matches da empresa.</h2>

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
    curtidas: CurtidaPendenteEmpresa[]
): string {
    if (curtidas.length === 0) {
        return `
            <div class="estado-vazio">
                <span class="estado-vazio-icone">♥</span>

                <h3>Nenhuma curtida aguardando Match</h3>

                <p>
                    Os candidatos que demonstrarem interesse e ainda
                    não tiverem reciprocidade aparecerão aqui.
                </p>
            </div>
        `
    }

    const cardsHtml = curtidas.map(curtida => `
        <div class="card-match card-curtida-pendente">
            <h3>♥ Curtida pendente</h3>

            ${
        curtida.vaga?.titulo
            ? `
                        <p>
                            <strong>Vaga:</strong>
                            ${escaparHtml(curtida.vaga.titulo)}
                        </p>
                    `
            : ""
    }

            <p>
                <strong>Candidato:</strong>
                Match necessário para visualização
            </p>

            <p>
                <strong>Recrutador:</strong>
                ${escaparHtml(
        curtida.recrutador?.nome ?? "Não identificado"
    )}
            </p>
        </div>
    `).join("")

    return `<div class="cards-matches">${cardsHtml}</div>`
}

function gerarSecaoMatchesRealizados(
    matches: MatchEmpresa[]
): string {
    if (matches.length === 0) {
        return `
            <div class="estado-vazio">
                <span class="estado-vazio-icone">🤝</span>

                <h3>Nenhum Match ainda</h3>

                <p>
                    Quando um candidato demonstrar interesse recíproco,
                    o Match aparecerá aqui.
                </p>
            </div>
        `
    }

    const cardsHtml = matches.map(match => `
        <div class="card-match card-match-confirmado">
            <h3>
                🤝 ${
        escaparHtml(
            [
                match.candidato?.nome,
                match.candidato?.sobrenome
            ].filter(Boolean).join(" ")
            || "Candidato"
        )
    }
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