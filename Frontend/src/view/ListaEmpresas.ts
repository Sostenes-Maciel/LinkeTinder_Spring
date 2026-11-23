import { confirmarExclusao } from "../components/Confirmacao.ts"
import { renderPerfilEmpresa } from "./PerfilEmpresa.ts"
import type { Empresa } from "../model/Empresa"

const API_NUCLEO = "http://localhost:8081"

interface EmpresaApi {
    cnpj: string
    nome: string
    email: string
    pais?: string | null
    estado?: string | null
    cep?: string | null
    descricao?: string | null
}

interface RespostaErro {
    mensagem?: string
}

export async function renderListaEmpresas(mensagemSucesso?: string): Promise<void> {
    const app = document.querySelector<HTMLDivElement>("#app")
    if (!app) return

    renderizarTabelaBase(app, mensagemSucesso)
    await renderizarLinhasEmpresas()
}

function renderizarTabelaBase(app: HTMLDivElement, mensagemSucesso?: string): void {
    app.innerHTML = `
        <section class="tela-com-transicao">
            <h1>Empresas</h1>
            <p id="mensagem" class="${mensagemSucesso ? "mensagem-sucesso" : ""}">
                ${escaparHtml(mensagemSucesso ?? "")}
            </p>

            <table>
                <thead>
                    <tr>
                        <th>Nome da Empresa</th>
                        <th>CNPJ</th>
                        <th>Estado</th>
                        <th>Ação</th>
                    </tr>
                </thead>
                <tbody id="lista-empresas">
                    <tr><td colspan="4">Carregando empresas...</td></tr>
                </tbody>
            </table>
        </section>
    `
}

async function renderizarLinhasEmpresas(): Promise<void> {
    const lista = document.querySelector<HTMLTableSectionElement>("#lista-empresas")
    if (!lista) return

    try {
        const resposta = await fetch(`${API_NUCLEO}/api/empresas`)

        if (!resposta.ok) {
            lista.innerHTML = `
                <tr>
                    <td colspan="4" class="tabela-vazia">
                        Erro ao carregar as empresas do servidor.
                    </td>
                </tr>
            `
            return
        }

        const dadosDoServidor: unknown = await resposta.json()
        const dados = extrairDados<EmpresaApi[]>(dadosDoServidor)

        if (!Array.isArray(dados)) {
            lista.innerHTML = `
                <tr>
                    <td colspan="4" class="tabela-vazia">
                        Resposta inválida recebida do servidor.
                    </td>
                </tr>
            `
            return
        }

        if (dados.length === 0) {
            lista.innerHTML = `
                <tr>
                    <td colspan="4" class="tabela-vazia">
                        Nenhuma empresa cadastrada no banco de dados.
                    </td>
                </tr>
            `
            return
        }

        lista.innerHTML = ""

        dados.forEach(empresa => {
            const linha = document.createElement("tr")

            const colunaNome = linha.insertCell()
            colunaNome.className = "nome-hover"
            colunaNome.dataset.email = empresa.email
            colunaNome.textContent = empresa.nome

            const colunaCnpj = linha.insertCell()
            colunaCnpj.textContent = empresa.cnpj

            const colunaEstado = linha.insertCell()
            colunaEstado.textContent = empresa.estado ?? "—"

            const colunaAcoes = linha.insertCell()

            const botaoPerfil = document.createElement("button")
            botaoPerfil.className = "btn-perfil"
            botaoPerfil.dataset.cnpj = empresa.cnpj
            botaoPerfil.textContent = "Ver perfil"

            const botaoExcluir = document.createElement("button")
            botaoExcluir.className = "btn-excluir"
            botaoExcluir.dataset.cnpj = empresa.cnpj
            botaoExcluir.textContent = "Excluir"

            colunaAcoes.append(botaoPerfil, botaoExcluir)
            lista.appendChild(linha)
        })

        configurarEventosBotoes()

    } catch (erro) {
        lista.innerHTML = `
            <tr>
                <td colspan="4" class="tabela-vazia">
                    Falha na conexão com o servidor.
                </td>
            </tr>
        `
        console.error("Erro na API de empresas:", erro)
    }
}

function configurarEventosBotoes(): void {
    const botoesPerfil = document.querySelectorAll<HTMLButtonElement>(".btn-perfil")
    const botoesExcluir = document.querySelectorAll<HTMLButtonElement>(".btn-excluir")

    botoesPerfil.forEach(botao => {
        botao.addEventListener("click", () => abrirPerfil(botao.dataset.cnpj))
    })

    botoesExcluir.forEach(botao => {
        botao.addEventListener("click", () => processarExclusao(botao.dataset.cnpj))
    })
}

async function abrirPerfil(cnpj?: string): Promise<void> {
    if (!cnpj) return

    try {
        const cnpjLimpo = cnpj.replace(/\D/g, "")

        const resposta = await fetch(
            `${API_NUCLEO}/api/empresas/${encodeURIComponent(cnpjLimpo)}`
        )

        if (!resposta.ok) {
            console.error("Empresa não encontrada no banco.")
            return
        }

        const dadosDoServidor: unknown = await resposta.json()
        const empresa = extrairDados<EmpresaApi>(dadosDoServidor)

        if (!empresa || typeof empresa !== "object") {
            console.error("Resposta inválida ao consultar empresa.")
            return
        }

        await renderPerfilEmpresa(empresa as Empresa)

    } catch (erro) {
        console.error("Erro de conexão ao abrir perfil da empresa:", erro)
    }
}

async function processarExclusao(cnpj?: string): Promise<void> {
    if (!cnpj) {
        console.error("CNPJ da empresa não encontrado.")
        return
    }

    const confirmar = await confirmarExclusao(
        "Excluir empresa?",
        "Essa ação não poderá ser desfeita e removerá todas as vagas publicadas por esta empresa."
    )

    if (!confirmar) return

    try {
        const cnpjLimpo = cnpj.replace(/\D/g, "")

        const resposta = await fetch(
            `${API_NUCLEO}/api/empresas/${encodeURIComponent(cnpjLimpo)}`,
            {
                method: "DELETE"
            }
        )

        if (resposta.ok) {
            await renderListaEmpresas("Empresa excluída com sucesso do banco de dados!")
            return
        }

        const erroApi = await resposta.json().catch(() => null) as RespostaErro | null

        window.alert(
            `Erro do servidor: ${erroApi?.mensagem ?? "Não foi possível excluir a empresa."}`
        )

    } catch (erro) {
        console.error("Erro ao excluir empresa:", erro)
        window.alert("Não foi possível excluir a empresa. Servidor offline.")
    }
}

function extrairDados<T>(resposta: unknown): T {
    if (
        typeof resposta === "object" &&
        resposta !== null &&
        "dados" in resposta
    ) {
        return (resposta as { dados: T }).dados
    }

    return resposta as T
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