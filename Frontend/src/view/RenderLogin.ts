import {renderPerfilCandidato} from './PerfilCandidato'
import type {Candidato} from '../model/Candidato'
import {renderPerfilEmpresa} from './PerfilEmpresa'
import type {Empresa} from '../model/Empresa'

export function renderLogin(): void {
    const app = document.querySelector<HTMLDivElement>('#app')
    if (!app) return

    renderizarFormularioLogin(app)
    configurarEventosLogin()
}

function renderizarFormularioLogin(app: HTMLDivElement): void {
    app.innerHTML = `
        <section class="login-container tela-com-transicao">
            <h1>Login no Linketinder</h1>
            <p class="form-descricao">Acesse seu espaço e continue sua jornada profissional.</p>

            <form id="form-login">
                <div>
                    <label for="tipoUsuario">Entrar como:</label>
                    <select id="tipoUsuario" name="tipoUsuario" required>
                        <option value="candidato">Candidato</option>
                        <option value="empresa">Empresa</option>
                    </select>
                </div>

                <div>
                    <label for="email">E-mail</label>
                    <input type="email" id="email" name="email" required>
                </div>

                <div id="campo-documento-login">
                    <label id="labelDocumento" for="documento">CPF</label>
                    <input type="text" id="documento" name="documento" required>
                </div>

                <button type="submit">Entrar</button>

                <p id="mensagem-login"></p>
            </form>
        </section>
    `
}

function configurarEventosLogin(): void {
    const formulario = document.querySelector<HTMLFormElement>('#form-login')

    formulario?.addEventListener('submit', async (evento) => {
        evento.preventDefault()
        await processarLogin(formulario)
    })

    const tipo = formulario?.querySelector<HTMLSelectElement>('#tipoUsuario')
    const documento = formulario?.querySelector<HTMLInputElement>('#documento')
    const labelDocumento = formulario?.querySelector<HTMLLabelElement>('#labelDocumento')

    tipo?.addEventListener('change', () => {
        const ehCandidato = tipo.value === 'candidato'

        if (labelDocumento) {
            labelDocumento.textContent = ehCandidato ? 'CPF' : 'CNPJ'
        }

        if (documento) {
            documento.value = ''
            documento.placeholder = ehCandidato
                ? 'Digite seu CPF'
                : 'Digite seu CNPJ'
        }
    })
}

async function processarLogin(formulario: HTMLFormElement): Promise<void> {
    const mensagem = document.querySelector<HTMLParagraphElement>('#mensagem-login')
    const dados = new FormData(formulario)

    const tipoUsuario = dados.get('tipoUsuario') as string
    const email = (dados.get('email') as string).trim()
    const documentoBruto = dados.get('documento') as string

    const documentoLimpo = (documentoBruto ?? '').replace(/\D/g, '')

    if (mensagem) {
        mensagem.className = ''
        mensagem.textContent = ''
    }

    const endpoint = tipoUsuario === 'candidato'
        ? 'http://localhost:8081/api/candidatos/login'
        : 'http://localhost:8081/api/empresas/login'

    const payload = tipoUsuario === 'candidato'
        ? {email, cpf: documentoLimpo}
        : {email, cnpj: documentoLimpo}

    try {
        const resposta = await fetch(endpoint, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        })

        if (!resposta.ok) {
            let erroBackend: {mensagem?: string} | null = null

            try {
                erroBackend = await resposta.json()
            } catch {
                erroBackend = null
            }

            exibirMensagemLogin(
                mensagem,
                erroBackend?.mensagem ?? 'E-mail ou documento incorretos.',
                'mensagem-erro'
            )

            return
        }

        const retorno = await resposta.json()

        const usuarioAutenticado =
            retorno.candidato ??
            retorno.empresa ??
            retorno.dados ??
            retorno.usuario ??
            retorno

        localStorage.setItem(
            'usuarioLogado',
            JSON.stringify(usuarioAutenticado)
        )

        localStorage.setItem('tipoUsuario', tipoUsuario)

        window.dispatchEvent(new Event('auth-change'))

        if (tipoUsuario === 'candidato') {
            renderPerfilCandidato(usuarioAutenticado as Candidato)
        } else {
            await renderPerfilEmpresa(usuarioAutenticado as Empresa)
        }

        document.querySelector('#app')?.scrollIntoView({
            behavior: 'smooth',
            block: 'start'
        })

    } catch (erro) {
        console.error('Erro ao realizar login:', erro)

        exibirMensagemLogin(
            mensagem,
            'Servidor indisponível.',
            'mensagem-erro'
        )
    }
}

function exibirMensagemLogin(
    elementoHtml: HTMLElement | null,
    texto: string,
    classeCss: string
): void {
    if (elementoHtml) {
        elementoHtml.textContent = texto
        elementoHtml.className = classeCss

        setTimeout(() => {
            elementoHtml.textContent = ''
            elementoHtml.className = ''
        }, 3000)
    }
}