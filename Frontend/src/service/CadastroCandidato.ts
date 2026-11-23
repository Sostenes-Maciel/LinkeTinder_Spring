import type { Candidato } from '../model/Candidato.ts'
import { ValidarCandidato } from './ValidarCandidato'
import { configurarAutocompletarCep } from './ConsultaCep.ts'
import { mostrarNotificacaoSucesso } from '../components/Notificacao.ts'
import {
    getCountries,
    getStatesOfCountry
} from '@countrystatecity/countries-browser'

const URL_MS_NUCLEO = 'http://localhost:8081'
const URL_MS_COMPETENCIAS = 'http://localhost:8082'

interface CompetenciaAssociada {
    idCompetencia: number
    nome: string
}

export async function renderCadastroCandidato(candidatoEmEdicao?: Candidato): Promise<void> {
    const app = document.querySelector<HTMLDivElement>('#app')
    if (!app) return

    let candidatoParaEdicao = candidatoEmEdicao

    // Busca as competências no MS-Competências antes de abrir a edição.
    if (candidatoEmEdicao) {
        app.innerHTML = `
            <section class="cadastro-container">
                <p>Carregando dados e competências do candidato...</p>
            </section>
        `

        try {
            const competencias = await buscarCompetenciasCandidato(
                candidatoEmEdicao.cpf
            )

            candidatoParaEdicao = {
                ...candidatoEmEdicao,
                competencias
            }

        } catch (erro) {
            console.error(
                'Erro ao carregar competências do candidato:',
                erro
            )

            app.innerHTML = ''

            const mensagem = document.createElement('p')
            mensagem.className = 'mensagem-erro'
            mensagem.textContent = erro instanceof Error
                ? erro.message
                : 'Não foi possível carregar as competências do candidato.'

            app.appendChild(mensagem)
            return
        }
    }

    renderizarFormulario(
        app,
        candidatoParaEdicao !== undefined
    )

    await configurarSelectsLocalizacao()

    if (candidatoParaEdicao) {
        await preencherFormularioEdicao(candidatoParaEdicao)
    }

    configurarEventosFormulario(
        configurarBuscaCep(),
        candidatoParaEdicao
    )
}

function renderizarFormulario(
    app: HTMLDivElement,
    modoEdicao: boolean
): void {

    app.innerHTML = `
        <section class="cadastro-container tela-com-transicao">
            <h1>${modoEdicao ? 'Editar perfil de candidato' : 'Cadastro de Candidato'}</h1>
            <p class="form-descricao">
                ${modoEdicao
        ? 'Atualize seus dados de perfil.'
        : 'Crie seu perfil e encontre oportunidades alinhadas às suas competências.'}
            </p>

            <form id="form-candidato" data-modo-edicao="${modoEdicao}">
                <div>
                    <label for="nome">Nome</label>
                    <input type="text" id="nome" name="nome" required>
                    <p id="erro-nome" class="erro-campo"></p>
                </div>

                <div>
                    <label for="email">E-mail</label>
                    <input type="email" id="email" name="email" placeholder="Ex.: exemplo@gmail.com" required>
                    <p id="erro-email" class="erro-campo"></p>
                </div>

                <div class="campo-idade">
                    <label for="idade">Idade</label>
                    <input type="number" id="idade" name="idade" min="18" max="120" inputmode="numeric" placeholder="Ex.: 29" required>
                    <p id="erro-idade" class="erro-campo"></p>
                </div>

                <div>
                    <label for="cpf">CPF</label>
                    <input type="text" id="cpf" name="cpf" placeholder="Ex.: 111.222.333-44" required>
                    <p id="erro-cpf" class="erro-campo"></p>
                </div>

                <div>
                    <label for="codigoPostal">Código Postal</label>
                    <input type="text" id="codigoPostal" name="codigoPostal" placeholder="Ex.: 55299-300" required>
                    <p id="mensagem-cep" aria-live="polite"></p>
                    <p id="erro-codigoPostal" class="erro-campo"></p>
                </div>

                <div>
                    <label for="pais">País</label>
                    <select id="pais" name="pais" required>
                        <option value="">Selecione um país</option>
                    </select>
                    <p id="erro-pais" class="erro-campo"></p>
                </div>

                <div>
                    <label for="estado">Estado</label>
                    <select id="estado" name="estado" required disabled>
                        <option value="">Selecione um país primeiro</option>
                    </select>
                    <p id="erro-estado" class="erro-campo"></p>
                </div>

                <div>
                    <label for="logradouro">Endereço</label>
                    <input type="text" id="logradouro" readonly>
                </div>

                <div>
                    <label for="bairro">Bairro</label>
                    <input type="text" id="bairro" readonly>
                </div>

                <div>
                    <label for="cidade">Cidade</label>
                    <input type="text" id="cidade" readonly>
                </div>

                <div>
                    <label for="descricao">Descrição</label>
                    <textarea id="descricao" name="descricao" required></textarea>
                    <p id="erro-descricao" class="erro-campo"></p>
                </div>

                <div>
                    <label for="competencias">Competências</label>
                    <input
                        type="text"
                        id="competencias"
                        name="competencias"
                        placeholder="Ex.: Java, Python, Groovy"
                        required
                    >
                    <p id="erro-competencias" class="erro-campo"></p>
                </div>

                <button type="submit">
                    ${modoEdicao ? 'Salvar alterações' : 'Cadastrar'}
                </button>

                <p id="mensagem"></p>
            </form>
        </section>
    `
}

async function preencherFormularioEdicao(
    candidato: Candidato
): Promise<void> {

    const pais = document.querySelector<HTMLSelectElement>('#pais')
    const estado = document.querySelector<HTMLSelectElement>('#estado')
    const codigoPostal = document.querySelector<HTMLInputElement>('#codigoPostal')
    const formulario = document.querySelector<HTMLFormElement>('#form-candidato')

    if (!pais || !estado || !codigoPostal || !formulario) return

    formulario.querySelector<HTMLInputElement>('#nome')!.value = candidato.nome
    formulario.querySelector<HTMLInputElement>('#email')!.value = candidato.email
    formulario.querySelector<HTMLInputElement>('#idade')!.value = String(candidato.idade)

    const campoCpf = formulario.querySelector<HTMLInputElement>('#cpf')!
    campoCpf.value = candidato.cpf
    campoCpf.readOnly = true

    codigoPostal.value = candidato.cep

    formulario.querySelector<HTMLTextAreaElement>('#descricao')!.value =
        candidato.descricao

    const competencias = Array.isArray(candidato.competencias)
        ? candidato.competencias
        : []

    formulario.querySelector<HTMLInputElement>('#competencias')!.value =
        competencias.join(', ')

    const opcaoPais = Array.from(pais.options).find(
        opcao => opcao.textContent?.trim() === candidato.pais
    )

    if (opcaoPais) {
        pais.value = opcaoPais.value
        await atualizarEstadosPorPais(pais.value, estado)
    }

    estado.value = candidato.estado
}

async function configurarSelectsLocalizacao(): Promise<void> {
    const selectPais = document.querySelector<HTMLSelectElement>('#pais')
    const selectEstado = document.querySelector<HTMLSelectElement>('#estado')

    if (!selectPais || !selectEstado) return

    try {
        const paises = await getCountries()

        paises.forEach(pais => {
            const option = document.createElement('option')
            option.value = pais.iso2
            option.textContent = pais.name
            selectPais.appendChild(option)
        })

    } catch (erro) {
        console.error('Erro ao carregar países:', erro)
    }

    selectPais.addEventListener('change', async () => {
        await atualizarEstadosPorPais(
            selectPais.value,
            selectEstado
        )
    })
}

async function atualizarEstadosPorPais(
    codigoPais: string,
    selectEstado: HTMLSelectElement
): Promise<void> {

    selectEstado.innerHTML =
        '<option value="">Selecione um estado</option>'

    selectEstado.disabled = true

    if (!codigoPais) return

    try {
        const estados = await getStatesOfCountry(codigoPais)

        estados.forEach(estado => {
            const option = document.createElement('option')
            option.value = estado.iso2
            option.textContent = estado.name
            option.dataset.uf = estado.iso2
            selectEstado.appendChild(option)
        })

        selectEstado.disabled = false

    } catch (erro) {
        console.error('Erro ao carregar estados:', erro)
    }
}

function configurarBuscaCep(): () => Promise<void> {
    const campoCep = document.querySelector<HTMLInputElement>('#codigoPostal')
    const campoPais = document.querySelector<HTMLSelectElement>('#pais')
    const campoEstado = document.querySelector<HTMLSelectElement>('#estado')
    const campoLogradouro = document.querySelector<HTMLInputElement>('#logradouro')
    const campoBairro = document.querySelector<HTMLInputElement>('#bairro')
    const campoCidade = document.querySelector<HTMLInputElement>('#cidade')
    const mensagemCep = document.querySelector<HTMLParagraphElement>('#mensagem-cep')

    if (
        !campoCep ||
        !campoPais ||
        !campoEstado ||
        !campoLogradouro ||
        !campoBairro ||
        !campoCidade ||
        !mensagemCep
    ) {
        return async () => undefined
    }

    return configurarAutocompletarCep(
        campoCep,
        campoPais,
        mensagemCep,
        campoEstado,
        campoLogradouro,
        campoBairro,
        campoCidade,
        async () => {
            campoPais.value = 'BR'
            await atualizarEstadosPorPais('BR', campoEstado)
        }
    )
}

function configurarEventosFormulario(
    validarCep: () => Promise<void>,
    candidatoEmEdicao?: Candidato
): void {

    const formulario = document.querySelector<HTMLFormElement>('#form-candidato')

    formulario?.addEventListener('submit', async evento => {
        evento.preventDefault()

        await processarSubmissao(
            formulario,
            validarCep,
            candidatoEmEdicao
        )
    })

    configurarValidacaoPorCampo(
        'nome',
        valor => ValidarCandidato.validarNome(valor)
    )

    configurarValidacaoPorCampo(
        'email',
        valor => ValidarCandidato.validarEmail(valor)
    )

    configurarValidacaoPorCampo(
        'idade',
        valor => ValidarCandidato.validarIdade(Number(valor))
    )

    configurarValidacaoPorCampo(
        'cpf',
        valor => ValidarCandidato.validarCpf(valor)
    )

    configurarValidacaoPorCampo(
        'descricao',
        valor => ValidarCandidato.validarDescricao(valor)
    )

    configurarValidacaoPorCampo('competencias', valor => {
        const competencias = valor
            .split(',')
            .map(c => c.trim())
            .filter(c => c.length > 0)

        ValidarCandidato.validarCompetencias(competencias)
    })
}

function configurarValidacaoPorCampo(
    campoId: string,
    validar: (valor: string) => void
): void {

    const campo = document.getElementById(campoId) as
        HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement | null

    const erro = document.getElementById(
        `erro-${campoId}`
    ) as HTMLParagraphElement | null

    if (!campo || !erro) return

    campo.addEventListener('blur', () => {
        limparErroCampo(campo, erro)

        try {
            validar(campo.value)

        } catch (erroValidacao) {
            mostrarErroCampo(
                campo,
                erro,
                erroValidacao instanceof Error
                    ? erroValidacao.message
                    : 'Campo inválido.'
            )
        }
    })

    campo.addEventListener('input', () => {
        limparErroCampo(campo, erro)
    })
}

function mostrarErroCampo(
    campo: HTMLElement,
    erro: HTMLParagraphElement,
    mensagem: string
): void {

    campo.classList.add('input-erro')
    erro.textContent = mensagem
    erro.classList.add('ativo')
}

function limparErroCampo(
    campo: HTMLElement,
    erro: HTMLParagraphElement
): void {

    campo.classList.remove('input-erro')
    erro.textContent = ''
    erro.classList.remove('ativo')
}

async function processarSubmissao(
    formulario: HTMLFormElement,
    validarCep: () => Promise<void>,
    candidatoEmEdicao?: Candidato
): Promise<void> {

    const mensagem = document.querySelector<HTMLParagraphElement>('#mensagem')
    const selectPais = document.querySelector<HTMLSelectElement>('#pais')

    if (mensagem) {
        mensagem.className = ''
        mensagem.textContent = ''
    }

    try {
        await validarCep()

        const candidato = extrairEValidarDados(formulario, selectPais)
        const emEdicao = candidatoEmEdicao !== undefined

        // Separa os dados cadastrais das competências.
        const { competencias, ...dadosSemCompetencias } = candidato

        // Persiste os dados cadastrais exclusivamente no MS-Núcleo.
        const resposta = await fetch(
            `${URL_MS_NUCLEO}/api/candidatos`,
            {
                method: emEdicao ? 'PUT' : 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(dadosSemCompetencias)
            }
        )

        if (!resposta.ok) {
            throw new Error(
                await obterMensagemErro(
                    resposta,
                    'Não foi possível salvar os dados do candidato no MS-Núcleo.'
                )
            )
        }

        // Persiste e sincroniza as competências exclusivamente no MS-Competências.
        try {
            await sincronizarCompetenciasCandidato(
                candidato.cpf,
                competencias
            )

        } catch (erroCompetencias) {
            const detalhe = erroCompetencias instanceof Error
                ? erroCompetencias.message
                : 'Erro desconhecido ao sincronizar competências.'

            throw new Error(
                `${emEdicao
                    ? 'Dados cadastrais atualizados'
                    : 'Candidato cadastrado'} no MS-Núcleo, mas houve uma falha ao salvar as competências no MS-Competências: ${detalhe}`
            )
        }

        if (emEdicao) {
            // Atualiza a sessão somente depois que os dois serviços responderem corretamente.
            localStorage.setItem(
                'usuarioLogado',
                JSON.stringify(candidato)
            )

            window.dispatchEvent(new Event('auth-change'))

            const { renderPerfilCandidato } =
                await import('../view/PerfilCandidato.ts')

            await renderPerfilCandidato(candidato)

            mostrarNotificacaoSucesso(
                'Perfil e competências atualizados com sucesso!'
            )

        } else {
            exibirMensagem(
                mensagem,
                'Candidato cadastrado com sucesso!',
                'mensagem-sucesso'
            )

            formulario.reset()
        }

    } catch (erro) {
        console.error('Erro ao salvar candidato:', erro)

        const textoErro = erro instanceof Error
            ? erro.message
            : 'Servidor indisponível. Verifique se os microsserviços estão em execução.'

        exibirMensagem(
            mensagem,
            textoErro,
            'mensagem-erro'
        )
    }
}

async function buscarCompetenciasCandidato(
    cpf: string
): Promise<string[]> {

    const cpfLimpo = cpf.replace(/\D/g, '')

    if (!/^\d{11}$/.test(cpfLimpo)) {
        throw new Error('CPF inválido para consultar competências.')
    }

    const resposta = await fetch(
        `${URL_MS_COMPETENCIAS}/api/candidatos/${encodeURIComponent(cpfLimpo)}/competencias`
    )

    if (!resposta.ok) {
        throw new Error(
            await obterMensagemErro(
                resposta,
                'Não foi possível consultar as competências no MS-Competências.'
            )
        )
    }

    const resultado: unknown = await resposta.json()

    let competencias: CompetenciaAssociada[] = []

    if (Array.isArray(resultado)) {
        competencias = resultado as CompetenciaAssociada[]

    } else if (
        typeof resultado === 'object' &&
        resultado !== null &&
        'dados' in resultado &&
        Array.isArray(resultado.dados)
    ) {
        competencias = resultado.dados as CompetenciaAssociada[]
    }

    return competencias.map(competencia => competencia.nome)
}

async function sincronizarCompetenciasCandidato(
    cpf: string,
    competenciasDesejadas: string[]
): Promise<void> {

    const cpfLimpo = cpf.replace(/\D/g, '')

    if (!/^\d{11}$/.test(cpfLimpo)) {
        throw new Error('CPF inválido para sincronizar as competências.')
    }

    const urlAssociacoes =
        `${URL_MS_COMPETENCIAS}/api/candidatos/${encodeURIComponent(cpfLimpo)}/competencias`

    // Consulta os vínculos atuais no MS-Competências.
    const respostaConsulta = await fetch(urlAssociacoes)

    if (!respostaConsulta.ok) {
        throw new Error(
            await obterMensagemErro(
                respostaConsulta,
                'Não foi possível consultar as competências atuais.'
            )
        )
    }

    const resultadoConsulta: unknown = await respostaConsulta.json()

    let competenciasAtuais: CompetenciaAssociada[] = []

    if (Array.isArray(resultadoConsulta)) {
        competenciasAtuais = resultadoConsulta as CompetenciaAssociada[]

    } else if (
        typeof resultadoConsulta === 'object' &&
        resultadoConsulta !== null &&
        'dados' in resultadoConsulta &&
        Array.isArray(resultadoConsulta.dados)
    ) {
        competenciasAtuais = resultadoConsulta.dados as CompetenciaAssociada[]
    }

    const normalizar = (nome: string): string =>
        nome.trim().toLocaleLowerCase('pt-BR')

    // Elimina duplicidades informadas no formulário.
    const mapaDesejadas = new Map<string, string>()

    competenciasDesejadas.forEach(nome => {
        const nomeLimpo = nome.trim()

        if (nomeLimpo) {
            mapaDesejadas.set(
                normalizar(nomeLimpo),
                nomeLimpo
            )
        }
    })

    const desejadasUnicas = Array.from(mapaDesejadas.values())

    const nomesAtuais = new Set(
        competenciasAtuais.map(
            competencia => normalizar(competencia.nome)
        )
    )

    const nomesDesejados = new Set(
        desejadasUnicas.map(normalizar)
    )

    const competenciasParaAdicionar = desejadasUnicas.filter(
        nome => !nomesAtuais.has(normalizar(nome))
    )

    const competenciasParaRemover = competenciasAtuais.filter(
        competencia => !nomesDesejados.has(normalizar(competencia.nome))
    )

    // Adiciona somente as novas competências.
    if (competenciasParaAdicionar.length > 0) {
        const respostaAssociacao = await fetch(
            urlAssociacoes,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    competencias: competenciasParaAdicionar
                })
            }
        )

        if (!respostaAssociacao.ok) {
            throw new Error(
                await obterMensagemErro(
                    respostaAssociacao,
                    'Não foi possível adicionar as novas competências.'
                )
            )
        }
    }

    // Remove somente os vínculos retirados pelo usuário.
    for (const competencia of competenciasParaRemover) {
        const respostaRemocao = await fetch(
            `${urlAssociacoes}/${competencia.idCompetencia}`,
            {
                method: 'DELETE'
            }
        )

        if (!respostaRemocao.ok && respostaRemocao.status !== 404) {
            throw new Error(
                `Não foi possível remover a competência "${competencia.nome}".`
            )
        }
    }
}

async function obterMensagemErro(
    resposta: Response,
    mensagemPadrao: string
): Promise<string> {

    try {
        const resultado = await resposta.json()

        return resultado.mensagem ||
            resultado.message ||
            mensagemPadrao

    } catch {
        return mensagemPadrao
    }
}

function extrairEValidarDados(
    formulario: HTMLFormElement,
    selectPais: HTMLSelectElement | null
): Candidato {

    const dados = new FormData(formulario)

    const nome = dados.get('nome') as string
    const email = dados.get('email') as string
    const idade = Number(dados.get('idade'))
    const cpf = dados.get('cpf') as string
    const pais = selectPais?.selectedOptions[0]?.textContent?.trim() ?? ''
    const estado = dados.get('estado') as string
    const codigoPostal = dados.get('codigoPostal') as string
    const descricao = dados.get('descricao') as string

    const competenciasStr = dados.get('competencias') as string

    if (competenciasStr.trim().includes(' ') && !competenciasStr.includes(',')) {
        if (competenciasStr.trim().split(/\s+/).length > 2) {
            throw new Error(
                'Por favor, separe as competências usando vírgulas (Ex: Java, Python, Groovy).'
            )
        }
    }

    const competencias = competenciasStr
        .split(',')
        .map(competencia => competencia.trim())
        .filter(competencia => competencia.length > 0)

    ValidarCandidato.validarNome(nome)
    ValidarCandidato.validarEmail(email)
    ValidarCandidato.validarCpf(cpf)
    ValidarCandidato.validarPais(pais)
    ValidarCandidato.validarIdade(idade)
    ValidarCandidato.validarEstado(estado)
    ValidarCandidato.validarCodigoPostal(
        codigoPostal,
        selectPais?.value ?? ''
    )
    ValidarCandidato.validarDescricao(descricao)
    ValidarCandidato.validarCompetencias(competencias)

    const cpfLimpo = cpf.replace(/\D/g, '')
    const codigoPostalLimpo = codigoPostal.replace(/\D/g, '')

    return {
        nome,
        email,
        idade,
        cpf: formulario.dataset.modoEdicao === 'true'
            ? cpf
            : cpfLimpo,
        pais,
        estado,
        cep: codigoPostalLimpo,
        descricao,
        competencias
    }
}

function exibirMensagem(
    elemento: HTMLParagraphElement | null,
    texto: string,
    classeCSS: string
): void {

    if (elemento) {
        elemento.textContent = texto
        elemento.classList.add(classeCSS)
    }
}