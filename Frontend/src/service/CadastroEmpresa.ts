import type { Empresa } from '../model/Empresa.ts'
import { ValidarEmpresa } from './ValidarEmpresa'
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

export async function renderCadastroEmpresa(empresaEmEdicao?: Empresa): Promise<void> {
    const app = document.querySelector<HTMLDivElement>('#app')
    if (!app) return

    let empresaParaEdicao = empresaEmEdicao

    // Recupera as competências do MS-Competências antes de abrir a edição.
    if (empresaEmEdicao) {
        app.innerHTML = `
            <section class="cadastro-container">
                <p>Carregando dados e competências da empresa...</p>
            </section>
        `

        try {
            const competencias = await buscarCompetenciasEmpresa(
                empresaEmEdicao.cnpj
            )

            empresaParaEdicao = {
                ...empresaEmEdicao,
                competencias
            }

        } catch (erro) {
            console.error(
                'Erro ao carregar competências da empresa:',
                erro
            )

            app.innerHTML = ''

            const mensagem = document.createElement('p')
            mensagem.className = 'mensagem-erro'
            mensagem.textContent = erro instanceof Error
                ? erro.message
                : 'Não foi possível carregar as competências da empresa.'

            app.appendChild(mensagem)
            return
        }
    }

    renderizarFormulario(
        app,
        empresaParaEdicao !== undefined
    )

    await configurarSelectsLocalizacao()

    if (empresaParaEdicao) {
        await preencherFormularioEdicao(empresaParaEdicao)
    }

    configurarEventosFormulario(
        configurarBuscaCep(),
        empresaParaEdicao
    )
}

function renderizarFormulario(
    app: HTMLDivElement,
    modoEdicao: boolean
): void {

    app.innerHTML = `
        <section class="cadastro-container tela-com-transicao">
            <h1>${modoEdicao ? 'Editar perfil da empresa' : 'Cadastro de Empresa'}</h1>

            <p class="form-descricao">
                ${modoEdicao
        ? 'Atualize os dados da sua empresa.'
        : 'Apresente sua empresa e conecte-se a profissionais que combinam com suas vagas.'}
            </p>

            <form id="form-empresa" data-modo-edicao="${modoEdicao}">
                <div>
                    <label for="nome">Nome</label>
                    <input type="text" id="nome" name="nome" required>
                    <p id="erro-nome" class="erro-campo"></p>
                </div>

                <div>
                    <label for="email">E-mail</label>
                    <input type="email" id="email" name="email" required>
                    <p id="erro-email" class="erro-campo"></p>
                </div>

                <div>
                    <label for="cnpj">CNPJ</label>
                    <input type="text" id="cnpj" name="cnpj" required>
                    <p id="erro-cnpj" class="erro-campo"></p>
                </div>

                <div class="campo-codigo-postal">
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
                        placeholder="Ex.: Java, Angular, TypeScript"
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

async function preencherFormularioEdicao(empresa: Empresa): Promise<void> {
    const pais = document.querySelector<HTMLSelectElement>('#pais')
    const estado = document.querySelector<HTMLSelectElement>('#estado')
    const codigoPostal = document.querySelector<HTMLInputElement>('#codigoPostal')
    const formulario = document.querySelector<HTMLFormElement>('#form-empresa')

    if (!pais || !estado || !codigoPostal || !formulario) return

    formulario.querySelector<HTMLInputElement>('#nome')!.value = empresa.nome
    formulario.querySelector<HTMLInputElement>('#email')!.value = empresa.email

    const campoCnpj = formulario.querySelector<HTMLInputElement>('#cnpj')!
    campoCnpj.value = empresa.cnpj
    campoCnpj.readOnly = true

    codigoPostal.value = empresa.cep

    formulario.querySelector<HTMLTextAreaElement>('#descricao')!.value =
        empresa.descricao

    const competencias = Array.isArray(empresa.competencias)
        ? empresa.competencias
        : []

    formulario.querySelector<HTMLInputElement>('#competencias')!.value =
        competencias.join(', ')

    const opcaoPais = Array.from(pais.options).find(
        opcao => opcao.textContent?.trim() === empresa.pais
    )

    if (opcaoPais) {
        pais.value = opcaoPais.value
        await atualizarEstadosPorPais(pais.value, estado)
    }

    estado.value = empresa.estado
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
    empresaEmEdicao?: Empresa
): void {

    const formulario = document.querySelector<HTMLFormElement>('#form-empresa')

    formulario?.addEventListener('submit', async evento => {
        evento.preventDefault()

        await processarSubmissao(
            formulario,
            validarCep,
            empresaEmEdicao
        )
    })

    configurarValidacaoPorCampo(
        'nome',
        valor => ValidarEmpresa.validarNome(valor)
    )

    configurarValidacaoPorCampo(
        'email',
        valor => ValidarEmpresa.validarEmail(valor)
    )

    configurarValidacaoPorCampo(
        'cnpj',
        valor => ValidarEmpresa.validarCnpj(valor)
    )

    configurarValidacaoPorCampo(
        'descricao',
        valor => ValidarEmpresa.validarDescricao(valor)
    )

    configurarValidacaoPorCampo('competencias', valor => {
        const competencias = valor
            .split(',')
            .map(c => c.trim())
            .filter(c => c.length > 0)

        ValidarEmpresa.validarCompetencias(competencias)
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
    empresaEmEdicao?: Empresa
): Promise<void> {

    const mensagem = document.querySelector<HTMLParagraphElement>('#mensagem')
    const selectPais = document.querySelector<HTMLSelectElement>('#pais')

    if (mensagem) {
        mensagem.className = ''
        mensagem.textContent = ''
    }

    try {
        await validarCep()

        const empresa = extrairEValidarDados(formulario, selectPais)
        const emEdicao = empresaEmEdicao !== undefined

        // Separa as competências dos dados cadastrais.
        const { competencias, ...dadosSemCompetencias } = empresa

        // Cadastro ou atualização dos dados principais no MS-Núcleo.
        const resposta = await fetch(
            `${URL_MS_NUCLEO}/api/empresas`,
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
                    'Não foi possível salvar os dados da empresa no MS-Núcleo.'
                )
            )
        }

        // Sincronização das competências no MS-Competências.
        try {
            await sincronizarCompetenciasEmpresa(
                empresa.cnpj,
                competencias
            )

        } catch (erroCompetencias) {
            const detalhe = erroCompetencias instanceof Error
                ? erroCompetencias.message
                : 'Erro desconhecido ao sincronizar competências.'

            throw new Error(
                `${emEdicao
                    ? 'Dados cadastrais atualizados'
                    : 'Empresa cadastrada'} no MS-Núcleo, mas houve uma falha ao salvar as competências no MS-Competências: ${detalhe}`
            )
        }

        if (emEdicao) {
            localStorage.setItem(
                'usuarioLogado',
                JSON.stringify(empresa)
            )

            window.dispatchEvent(new Event('auth-change'))

            const { renderPerfilEmpresa } =
                await import('../view/PerfilEmpresa.ts')

            await renderPerfilEmpresa(empresa)

            mostrarNotificacaoSucesso(
                'Perfil da empresa e competências atualizados com sucesso!'
            )

        } else {
            exibirMensagem(
                mensagem,
                'Empresa cadastrada com sucesso!',
                'mensagem-sucesso'
            )

            formulario.reset()
        }

    } catch (erro) {
        console.error('Erro ao salvar empresa:', erro)

        const textoErro = erro instanceof Error
            ? erro.message
            : 'Servidor indisponível. Verifique se os microsserviços estão em execução.'

        exibirMensagem(mensagem, textoErro, 'mensagem-erro')
    }
}

async function buscarCompetenciasEmpresa(
    cnpj: string
): Promise<string[]> {

    const cnpjLimpo = cnpj.replace(/\D/g, '')

    if (!/^\d{14}$/.test(cnpjLimpo)) {
        throw new Error('CNPJ inválido para consultar competências.')
    }

    const resposta = await fetch(
        `${URL_MS_COMPETENCIAS}/api/empresas/${encodeURIComponent(cnpjLimpo)}/competencias`
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

async function sincronizarCompetenciasEmpresa(
    cnpj: string,
    competenciasDesejadas: string[]
): Promise<void> {

    const cnpjLimpo = cnpj.replace(/\D/g, '')

    if (!/^\d{14}$/.test(cnpjLimpo)) {
        throw new Error('CNPJ inválido para sincronizar as competências.')
    }

    const urlAssociacoes =
        `${URL_MS_COMPETENCIAS}/api/empresas/${encodeURIComponent(cnpjLimpo)}/competencias`

    // Consulta os vínculos atuais.
    const respostaConsulta = await fetch(urlAssociacoes)

    if (!respostaConsulta.ok) {
        throw new Error(
            await obterMensagemErro(
                respostaConsulta,
                'Não foi possível consultar as competências atuais da empresa.'
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

    // Remove duplicidades informadas no formulário.
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

    // Adiciona somente competências novas.
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

    // Remove somente os vínculos que deixaram de ser desejados.
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
): Empresa {

    const dados = new FormData(formulario)

    const nome = dados.get('nome') as string
    const email = dados.get('email') as string
    const cnpj = dados.get('cnpj') as string
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

    ValidarEmpresa.validarNome(nome)
    ValidarEmpresa.validarEmail(email)
    ValidarEmpresa.validarCnpj(cnpj)
    ValidarEmpresa.validarPais(pais)
    ValidarEmpresa.validarEstado(estado)
    ValidarEmpresa.validarCodigoPostal(
        codigoPostal,
        selectPais?.value ?? ''
    )
    ValidarEmpresa.validarDescricao(descricao)
    ValidarEmpresa.validarCompetencias(competencias)

    const cnpjLimpo = cnpj.replace(/\D/g, '')
    const codigoPostalLimpo = codigoPostal.replace(/\D/g, '')

    return {
        nome,
        email,
        cnpj: formulario.dataset.modoEdicao === 'true'
            ? cnpj
            : cnpjLimpo,
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