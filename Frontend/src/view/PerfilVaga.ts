interface VagaApi {
    idVaga: number
    titulo: string
    pais: string
    estado: string
    descricao: string
}

interface CompetenciaApi {
    idCompetencia?: number
    nome?: string
}

interface VagaPerfil {
    id: number
    titulo: string
    pais: string
    estado: string
    descricao: string
    competencias: string[]
}

const API_NUCLEO = "http://localhost:8081"
const API_COMPETENCIAS = "http://localhost:8082"

export async function renderPerfilVaga(
    vagaId: number,
    onVoltar?: () => void,
    temMatch: boolean = false,
    nomeEmpresa?: string
): Promise<void> {
    const app = document.querySelector<HTMLDivElement>("#app")
    if (!app) return

    app.innerHTML = `
        <section class="perfil-vaga">
            <p>Carregando vaga...</p>
        </section>
    `

    try {
        const resposta = await fetch(
            `${API_NUCLEO}/api/vagas/${encodeURIComponent(vagaId)}`
        )

        if (resposta.status === 404) {
            renderizarMensagem(app, "Vaga não encontrada!", onVoltar)
            return
        }

        if (!resposta.ok) {
            throw new Error(
                `Falha ao buscar vaga (HTTP ${resposta.status}).`
            )
        }

        const resultado: unknown = await resposta.json()
        const vagaApi = extrairVaga(resultado)

        if (!vagaApi) {
            renderizarMensagem(app, "Vaga não encontrada!", onVoltar)
            return
        }

        const competencias = await buscarCompetenciasVaga(vagaId)

        const vaga: VagaPerfil = {
            id: vagaApi.idVaga,
            titulo: vagaApi.titulo,
            pais: vagaApi.pais,
            estado: vagaApi.estado,
            descricao: vagaApi.descricao,
            competencias
        }

        renderizarPerfilVagaHTML(
            app,
            vaga,
            temMatch,
            nomeEmpresa
        )

        configurarEventoVoltar(onVoltar)

    } catch (erro) {
        console.error("Erro ao buscar detalhes da vaga:", erro)

        renderizarMensagem(
            app,
            "Não foi possível carregar os detalhes da vaga.",
            onVoltar
        )
    }
}

function extrairVaga(resultado: unknown): VagaApi | undefined {
    if (typeof resultado !== "object" || resultado === null) {
        return undefined
    }

    const dados = "dados" in resultado
        ? resultado.dados
        : resultado

    if (typeof dados !== "object" || dados === null) {
        return undefined
    }

    if (
        !("idVaga" in dados)
        || !("titulo" in dados)
        || !("pais" in dados)
        || !("estado" in dados)
        || !("descricao" in dados)
    ) {
        return undefined
    }

    const idVaga = Number(dados.idVaga)

    if (!Number.isInteger(idVaga) || idVaga <= 0) {
        return undefined
    }

    return {
        idVaga,
        titulo: String(dados.titulo),
        pais: String(dados.pais ?? ""),
        estado: String(dados.estado ?? ""),
        descricao: String(dados.descricao ?? "")
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
                `Falha ao buscar competências da vaga (HTTP ${resposta.status}).`
            )
        }

        const resultado: unknown = await resposta.json()
        const competencias = extrairLista<CompetenciaApi>(resultado)

        if (!competencias) {
            return []
        }

        return competencias
            .map(competencia => competencia.nome?.trim() ?? "")
            .filter(nome => nome.length > 0)

    } catch (erro) {
        console.error(
            "Erro ao buscar competências da vaga:",
            erro
        )

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

function renderizarMensagem(
    app: HTMLDivElement,
    mensagem: string,
    onVoltar?: () => void
): void {
    app.innerHTML = `
        <section class="perfil-vaga tela-com-transicao">
            <h2>${escaparHtml(mensagem)}</h2>
            <button id="btn-voltar-lista">Voltar para vagas</button>
        </section>
    `

    configurarEventoVoltar(onVoltar)
}

function renderizarPerfilVagaHTML(
    app: HTMLDivElement,
    vaga: VagaPerfil,
    temMatch: boolean,
    nomeEmpresa?: string
): void {
    const competenciasTexto = vaga.competencias.length > 0
        ? vaga.competencias.map(escaparHtml).join(", ")
        : "Nenhuma competência especificada"

    const nomeEmpresaExibido = temMatch
        ? nomeEmpresa?.trim()
            ? escaparHtml(nomeEmpresa)
            : "Match confirmado"
        : "Match necessário para visualização"

    app.innerHTML = `
        <section class="perfil-vaga tela-com-transicao">
            <h1>Detalhes da Vaga</h1>

            <div class="card-vaga">
                <h2>${escaparHtml(vaga.titulo)}</h2>

                <hr>

                <h3>Informações da Vaga</h3>

                <p>
                    <strong>Descrição:</strong>
                    ${escaparHtml(vaga.descricao)}
                </p>

                <p>
                    <strong>Local:</strong>
                    ${escaparHtml(vaga.pais)} - ${escaparHtml(vaga.estado)}
                </p>

                <p>
                    <strong>Competências:</strong>
                    ${competenciasTexto}
                </p>

                <hr>

                <h3>Informações da Empresa</h3>

                <p>
                    <strong>Empresa:</strong>
                    ${nomeEmpresaExibido}
                </p>
            </div>

            <button id="btn-voltar-lista">Voltar para Vagas</button>
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

function configurarEventoVoltar(onVoltar?: () => void): void {
    document
        .querySelector("#btn-voltar-lista")
        ?.addEventListener("click", () => {
            onVoltar?.()
        })
}

export default renderPerfilVaga