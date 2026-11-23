import type { Empresa } from '../model/Empresa.ts'
import type { Vaga } from "../model/Vaga.ts"
import { getCountries, getStatesOfCountry } from "@countrystatecity/countries-browser"


export class CadastroVagasService {
    private static readonly URL_MS_NUCLEO = 'http://localhost:8081'
    private static readonly URL_MS_COMPETENCIAS = 'http://localhost:8082'

    static async cadastrar(
        titulo: string,
        descricao: string,
        pais: string,
        estado: string,
        areaAtuacao: string,
        competencias: string[],
        empresa: Empresa
    ): Promise<Vaga> {

        this.validarCampos(titulo, descricao, pais, estado, competencias)

        const cnpj = empresa.cnpj.replace(/\D/g, '')

        if (!/^\d{14}$/.test(cnpj)) {
            throw new Error('CNPJ da empresa inválido.')
        }

        const dadosVaga = {
            titulo: titulo.trim(),
            descricao: descricao.trim(),
            pais: pais.trim(),
            estado: estado.trim(),
            cnpjEmpresa: cnpj
        }

        const resposta = await fetch(
            `${this.URL_MS_NUCLEO}/api/vagas`,
            {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(dadosVaga)
            }
        )

        if (!resposta.ok) {
            throw new Error(
                await this.obterMensagemErro(
                    resposta,
                    'Erro ao cadastrar vaga.'
                )
            )
        }

        const resultado = await resposta.json()
        const idVaga = Number(resultado.idVaga)

        if (!Number.isInteger(idVaga) || idVaga <= 0) {
            throw new Error('O backend não retornou um identificador válido para a vaga.')
        }

        try {
            await this.sincronizarCompetencias(idVaga, competencias)
        } catch (erro) {
            const detalhe = erro instanceof Error
                ? erro.message
                : 'Erro desconhecido.'

            throw new Error(
                `A vaga foi cadastrada, mas ocorreu um erro ao associar suas competências: ${detalhe}`
            )
        }

        return {
            id: idVaga,
            titulo: dadosVaga.titulo,
            descricao: dadosVaga.descricao,
            pais: dadosVaga.pais,
            estado: dadosVaga.estado,
            areaAtuacao: areaAtuacao.trim(),
            competencias,
            empresa
        }
    }

    static async atualizar(
        vaga: Vaga,
        empresa: Pick<Empresa, 'cnpj'>
    ): Promise<void> {

        const competencias = vaga.competencias ?? []
        const idVaga = Number(vaga.id)
        const cnpj = empresa.cnpj.replace(/\D/g, '')

        if (!Number.isInteger(idVaga) || idVaga <= 0) {
            throw new Error('Identificador da vaga inválido.')
        }

        if (!/^\d{14}$/.test(cnpj)) {
            throw new Error('CNPJ da empresa inválido.')
        }

        this.validarCampos(
            vaga.titulo,
            vaga.descricao,
            vaga.pais,
            vaga.estado,
            competencias
        )

        const resposta = await fetch(
            `${this.URL_MS_NUCLEO}/api/vagas/${idVaga}?cnpjEmpresa=${encodeURIComponent(cnpj)}`,
            {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    titulo: vaga.titulo.trim(),
                    descricao: vaga.descricao.trim(),
                    pais: vaga.pais.trim(),
                    estado: vaga.estado.trim()
                })
            }
        )

        if (!resposta.ok) {
            throw new Error(
                await this.obterMensagemErro(
                    resposta,
                    'Erro ao atualizar vaga.'
                )
            )
        }

        await this.sincronizarCompetencias(idVaga, competencias)
    }

    static async excluir(
        idVaga: number | string,
        empresa: Pick<Empresa, 'cnpj'>
    ): Promise<void> {

        const id = Number(idVaga)
        const cnpj = empresa.cnpj.replace(/\D/g, '')

        if (!Number.isInteger(id) || id <= 0) {
            throw new Error('Identificador da vaga inválido.')
        }

        if (!/^\d{14}$/.test(cnpj)) {
            throw new Error('CNPJ da empresa inválido.')
        }

        // Primeiro, recupera os vínculos de competências no MS-Competências.
        const competencias = await this.buscarCompetencias(id)

        // Remove os vínculos de competências no MS-Competências ANTES de excluir a vaga.
        // Se falhar, a vaga não será excluída.
        const falhas: string[] = []

        for (const competencia of competencias) {
            const resposta = await fetch(
                `${this.URL_MS_COMPETENCIAS}/api/vagas/${id}/competencias/${competencia.idCompetencia}`,
                {
                    method: 'DELETE'
                }
            )

            if (!resposta.ok && resposta.status !== 404) {
                falhas.push(competencia.nome)
            }
        }

        if (falhas.length > 0) {
            throw new Error(
                `Não foi possível remover estes vínculos de competências: ${falhas.join(', ')}.`
            )
        }

        // Somente após limpar os vínculos, exclui a vaga no MS-Núcleo.
        const respostaExclusao = await fetch(
            `${this.URL_MS_NUCLEO}/api/vagas/${id}?cnpjEmpresa=${encodeURIComponent(cnpj)}`,
            {
                method: 'DELETE'
            }
        )

        if (!respostaExclusao.ok) {
            throw new Error(
                await this.obterMensagemErro(
                    respostaExclusao,
                    'Não foi possível excluir a vaga.'
                )
            )
        }
    }

    private static async sincronizarCompetencias(
        idVaga: number,
        competenciasDesejadas: string[]
    ): Promise<void> {

        const competenciasAtuais = await this.buscarCompetencias(idVaga)

        const normalizar = (valor: string): string =>
            valor.trim().toLocaleLowerCase('pt-BR')

        const desejadas = Array.from(
            new Map(
                competenciasDesejadas
                    .map(nome => nome.trim())
                    .filter(Boolean)
                    .map(nome => [normalizar(nome), nome])
            ).values()
        )

        const mapaAtuais = new Map(
            competenciasAtuais.map(competencia => [
                normalizar(competencia.nome),
                competencia
            ])
        )

        const adicionar = desejadas.filter(
            nome => !mapaAtuais.has(normalizar(nome))
        )

        const remover = competenciasAtuais.filter(
            competencia => !desejadas.some(
                nome => normalizar(nome) === normalizar(competencia.nome)
            )
        )

        // Adiciona somente as competências novas.
        if (adicionar.length > 0) {
            const resposta = await fetch(
                `${this.URL_MS_COMPETENCIAS}/api/vagas/${idVaga}/competencias`,
                {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({
                        competencias: adicionar
                    })
                }
            )

            if (!resposta.ok) {
                throw new Error(
                    await this.obterMensagemErro(
                        resposta,
                        'Erro ao adicionar competências à vaga.'
                    )
                )
            }
        }

        // Remove somente os vínculos que não são mais desejados.
        for (const competencia of remover) {
            const resposta = await fetch(
                `${this.URL_MS_COMPETENCIAS}/api/vagas/${idVaga}/competencias/${competencia.idCompetencia}`,
                {
                    method: 'DELETE'
                }
            )

            if (!resposta.ok && resposta.status !== 404) {
                throw new Error(
                    `Não foi possível remover a competência ${competencia.nome}.`
                )
            }
        }
    }

    private static async buscarCompetencias(
        idVaga: number
    ): Promise<{ idCompetencia: number; nome: string }[]> {

        const resposta = await fetch(
            `${this.URL_MS_COMPETENCIAS}/api/vagas/${idVaga}/competencias`
        )

        if (!resposta.ok) {
            throw new Error(
                await this.obterMensagemErro(
                    resposta,
                    'Não foi possível consultar as competências da vaga.'
                )
            )
        }

        const resultado = await resposta.json()

        return Array.isArray(resultado)
            ? resultado
            : resultado.dados ?? []
    }

    private static async obterMensagemErro(
        resposta: Response,
        mensagemPadrao: string
    ): Promise<string> {

        try {
            const resultado = await resposta.json()
            return resultado.mensagem || resultado.message || mensagemPadrao
        } catch {
            return mensagemPadrao
        }
    }

    private static validarCampos(
        titulo: string,
        descricao: string,
        pais: string,
        estado: string,
        competencias: string[]
    ): void {

        if (!titulo.trim()) {
            throw new Error('Título da vaga não pode estar vazio.')
        }

        if (!descricao.trim()) {
            throw new Error('Descrição da vaga não pode estar vazia.')
        }

        if (!pais.trim()) {
            throw new Error('País da vaga é obrigatório.')
        }

        if (!estado.trim()) {
            throw new Error('Estado da vaga é obrigatório.')
        }

        if (competencias.length === 0) {
            throw new Error('A vaga deve possuir pelo menos uma competência.')
        }
    }
}

export async function renderViewCadastroVaga(
    empresa: Empresa,
    onCadastroConcluido?: (mensagem?: string) => void
): Promise<void> {
    const app = document.querySelector<HTMLDivElement>('#app')
    if (!app) return

    renderizarFormulario(app)
    await configurarSelectsLocalizacao()
    configurarEventosFormulario(empresa, onCadastroConcluido)
}

export async function renderViewEditarVaga(
    vaga: Vaga,
    empresa: Pick<Empresa, 'cnpj'>,
    onAtualizacaoConcluida?: (mensagem?: string) => void
): Promise<void> {
    const app = document.querySelector<HTMLDivElement>('#app')
    if (!app) return

    renderizarFormulario(app, vaga)
    await configurarSelectsLocalizacao(vaga.pais, vaga.estado)

    document.querySelector<HTMLFormElement>('#form-vaga')?.addEventListener('submit', evento => {
        evento.preventDefault()
        void processarAtualizacao(
            evento.currentTarget as HTMLFormElement,
            vaga,
            empresa,
            onAtualizacaoConcluida
        )
    })
}

function renderizarFormulario(app: HTMLDivElement, vaga?: Vaga): void {
    app.innerHTML = `
        <section class="cadastro-container tela-com-transicao">
            <h1>Cadastro de Vaga</h1>
            <p class="form-descricao">Descreva a oportunidade e indique onde ela está disponível.</p>

            <form id="form-vaga">
                <div class="campo-vaga-titulo">
                    <label for="titulo">Título da vaga</label>
                    <input type="text" id="titulo" name="titulo" value="${escaparAtributo(vaga?.titulo ?? '')}" required>
                </div>

                <div class="campo-vaga-descricao">
                    <label for="descricao">Descrição</label>
                    <textarea id="descricao" name="descricao" required>${escaparHtml(vaga?.descricao ?? '')}</textarea>
                </div>

                <div class="campo-vaga-competencias">
                    <label for="competencias">Competências</label>
                    <input type="text" id="competencias" name="competencias" value="${escaparAtributo((vaga?.competencias ?? []).join(', '))}" placeholder="Ex.: Java, Spring, Git" required>
                </div>

                <div class="campo-vaga-pais">
                    <label for="pais">País</label>
                    <select id="pais" name="pais" required>
                        <option value="">Selecione um país</option>
                    </select>
                </div>

                <div class="campo-vaga-estado">
                    <label for="estado">Estado</label>
                    <select id="estado" name="estado" required disabled>
                        <option value="">Selecione um estado</option>
                    </select>
                </div>

                <div class="campo-vaga-area">
                    <label for="areaAtuacao">Área de atuação</label>
                    <select id="areaAtuacao" name="areaAtuacao" required>
                        <option value="">Selecione uma área</option>
                        ${['TI', 'RH', 'Marketing', 'Vendas', 'Segurança'].map(area =>
        `<option value="${area}" ${vaga?.areaAtuacao === area ? 'selected' : ''}>${area}</option>`).join('')}
                    </select>
                </div>

                <div class="acoes-form-vaga">
                    <button type="submit">${vaga ? 'Salvar alterações' : 'Cadastrar vaga'}</button>
                </div>

                <p id="mensagem"></p>
            </form>
        </section>
    `
}

async function configurarSelectsLocalizacao(
    paisInicial?: string,
    estadoInicial?: string
): Promise<void> {

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

        const paisEncontrado = paises.find(pais => pais.name === paisInicial)

        if (paisEncontrado) {
            selectPais.value = paisEncontrado.iso2
            await atualizarEstadosPorPais(
                selectPais.value,
                selectEstado,
                estadoInicial
            )
        }

    } catch (erro) {
        console.error('Erro ao carregar países:', erro)
    }

    selectPais.addEventListener('change', async () => {
        await atualizarEstadosPorPais(selectPais.value, selectEstado)
    })
}

async function atualizarEstadosPorPais(
    codigoPais: string,
    selectEstado: HTMLSelectElement,
    estadoInicial?: string
): Promise<void> {

    selectEstado.innerHTML = '<option value="">Selecione um estado</option>'
    selectEstado.disabled = true

    if (!codigoPais) return

    try {
        const estados = await getStatesOfCountry(codigoPais)

        estados.forEach(estado => {
            const option = document.createElement('option')
            option.value = estado.name
            option.textContent = estado.name
            selectEstado.appendChild(option)
        })

        if (estadoInicial) {
            selectEstado.value = estadoInicial
        }

        selectEstado.disabled = false

    } catch (erro) {
        console.error('Erro ao carregar estados:', erro)
    }
}

async function processarAtualizacao(
    formulario: HTMLFormElement,
    vagaOriginal: Vaga,
    empresa: Pick<Empresa, 'cnpj'>,
    onAtualizacaoConcluida?: (mensagem?: string) => void
): Promise<void> {

    const dados = new FormData(formulario)

    const vaga: Vaga = {
        ...vagaOriginal,
        titulo: String(dados.get('titulo') ?? ''),
        descricao: String(dados.get('descricao') ?? ''),
        pais: document.querySelector<HTMLSelectElement>('#pais')?.selectedOptions[0]?.textContent?.trim() ?? '',
        estado: String(dados.get('estado') ?? ''),
        areaAtuacao: String(dados.get('areaAtuacao') ?? ''),
        competencias: String(dados.get('competencias') ?? '')
            .split(',')
            .map(item => item.trim())
            .filter(Boolean)
    }

    try {
        await CadastroVagasService.atualizar(vaga, empresa)
        onAtualizacaoConcluida?.('Vaga atualizada com sucesso!')

    } catch (erro) {
        const mensagem = document.querySelector<HTMLParagraphElement>('#mensagem')

        if (mensagem) {
            mensagem.textContent = erro instanceof Error
                ? erro.message
                : 'Não foi possível atualizar a vaga.'

            mensagem.className = 'mensagem-erro'
        }
    }
}

function escaparAtributo(valor: string): string {
    return escaparHtml(valor)
}

function escaparHtml(valor: string): string {
    return valor.replace(/[&<>"']/g, caractere => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
    })[caractere]!)
}

function configurarEventosFormulario(
    empresa: Empresa,
    onCadastroConcluido?: (mensagem?: string) => void
): void {

    const formulario = document.querySelector<HTMLFormElement>('#form-vaga')

    formulario?.addEventListener('submit', (evento) => {
        evento.preventDefault()
        void processarSubmissao(formulario, empresa, onCadastroConcluido)
    })
}

async function processarSubmissao(
    formulario: HTMLFormElement,
    empresa: Empresa,
    onCadastroConcluido?: (mensagem?: string) => void
): Promise<void> {

    const mensagem = document.querySelector<HTMLParagraphElement>('#mensagem')
    const selectPais = document.querySelector<HTMLSelectElement>('#pais')

    if (mensagem) {
        mensagem.className = ''
        mensagem.textContent = ''
    }

    try {
        const dados = new FormData(formulario)

        const titulo = dados.get('titulo') as string
        const descricao = dados.get('descricao') as string
        const pais = selectPais?.selectedOptions[0]?.textContent?.trim() ?? ''
        const estado = dados.get('estado') as string
        const areaAtuacao = String(dados.get('areaAtuacao') ?? '')

        const competencias = (dados.get('competencias') as string)
            .split(',')
            .map(c => c.trim())
            .filter(c => c.length > 0)

        const vaga = await CadastroVagasService.cadastrar(
            titulo,
            descricao,
            pais,
            estado,
            areaAtuacao,
            competencias,
            empresa
        )

        console.log('Vaga cadastrada:', vaga)

        onCadastroConcluido?.('Vaga cadastrada com sucesso!')

    } catch (erro) {
        console.error('Erro ao cadastrar vaga:', erro)

        if (mensagem) {
            mensagem.textContent = erro instanceof Error
                ? erro.message
                : 'Não foi possível cadastrar a vaga.'

            mensagem.classList.add('mensagem-erro')
        }
    }

}
