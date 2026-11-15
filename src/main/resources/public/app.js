// Front-end do SIGPesq: conversa com a API Java (/api/...) usando fetch.
// Todo texto vindo do banco e inserido com textContent (evita injecao de HTML / XSS).

// =====================================================================
// Utilitarios
// =====================================================================
const $ = (seletor) => document.querySelector(seletor);

/** Cria um elemento: h('td', { className: 'num' }, 'texto', outroElemento) */
function h(tag, props = {}, ...filhos) {
  const el = document.createElement(tag);
  for (const [chave, valor] of Object.entries(props)) {
    if (valor === undefined || valor === null || valor === false) continue;
    if (chave.startsWith('on')) el.addEventListener(chave.slice(2), valor);
    else if (chave in el) el[chave] = valor;
    else el.setAttribute(chave, valor);
  }
  for (const filho of filhos.flat()) {
    if (filho === null || filho === undefined || filho === false) continue;
    el.append(filho instanceof Node ? filho : String(filho));
  }
  return el;
}

async function api(url, opcoes) {
  const resposta = await fetch(url, opcoes);
  const dados = await resposta.json();
  if (!resposta.ok) throw new Error(dados.erro || 'Erro inesperado.');
  return dados;
}

const enviar = (url, corpo) => api(url, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(corpo),
});

const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const dinheiro = (v) => moeda.format(Number(v || 0));

function data(iso) {
  if (!iso) return '';
  const [ano, mes, dia] = iso.split('-');
  return `${dia}/${mes}/${ano}`;
}

const hoje = () => new Date().toISOString().slice(0, 10);

/** plural(2, 'projeto') -> '2 projetos'; plural(1, 'vínculo ativo', 'vínculos ativos') -> '1 vínculo ativo' */
const plural = (n, singular, pluralTexto = `${singular}s`) => `${n} ${n === 1 ? singular : pluralTexto}`;

// Os valores no banco nao tem acento (CHECK do MySQL); aqui so mostramos bonito.
const ROTULOS = {
  Concluido: 'Concluído', Tecnico: 'Técnico', Auxilio: 'Auxílio', Convenio: 'Convênio',
  'Capitulo de Livro': 'Capítulo de Livro', 'Relatorio Tecnico': 'Relatório Técnico',
};
const rotulo = (valor) => ROTULOS[valor] || valor;

function selo(texto) {
  const cores = {
    'Em andamento': '', Concluido: 'verde', Cancelado: 'vermelho',
    Docente: '', Discente: 'amarelo', Tecnico: 'cinza',
    Ativo: 'verde', Encerrado: 'cinza', Total: 'verde', Parcial: 'amarelo',
  };
  return h('span', { className: `selo ${cores[texto] ?? 'cinza'}` }, rotulo(texto));
}

/** Texto principal + linha menor embaixo, numa mesma celula. */
const duasLinhas = (principal, secundaria) =>
  h('span', {}, principal, secundaria ? h('span', { className: 'sub' }, secundaria) : null);

function avisar(mensagem) {
  const aviso = h('div', { className: 'aviso ok' }, mensagem);
  $('#avisos').append(aviso);
  setTimeout(() => aviso.remove(), 3500);
}

// Cache simples das listas usadas em <select> (recarregado a cada navegacao).
let cache = {};
async function lista(url) {
  if (!cache[url]) cache[url] = api(url);
  return cache[url];
}

// =====================================================================
// Formulario generico (um <dialog> reaproveitado por todas as telas)
// =====================================================================
const dlg = $('#dlg');
const form = $('#form');
const erro = $('#erro');
let formularioAtual = null;

/**
 * Abre o formulario a partir de uma lista de campos:
 *   { nome, rotulo, tipo: text|textarea|date|number|email|select|autores,
 *     obrigatorio, opcoes: async () => [[valor, texto]], dica, linha: 'a', numero: true }
 */
async function abrirFormulario(config) {
  formularioAtual = config;
  erro.textContent = '';
  $('#form-titulo').textContent = config.titulo;
  $('#salvar').textContent = config.botao || 'Salvar';
  const area = $('#campos');
  area.replaceChildren();

  try {
    const linhas = {};
    for (const campo of config.campos) {
      const el = await criarCampo(campo);
      if (campo.linha) {
        if (!linhas[campo.linha]) {
          linhas[campo.linha] = h('div', { className: 'linha' });
          area.append(linhas[campo.linha]);
        }
        linhas[campo.linha].append(el);
      } else {
        area.append(el);
      }
    }
    dlg.showModal();
    config.aoAbrir?.(form);
    form.querySelector('input, select, textarea')?.focus();
  } catch (e) {
    avisar(e.message);
  }
}

async function criarCampo(campo) {
  const marca = campo.obrigatorio ? ' *' : '';
  if (campo.tipo === 'autores') return criarCampoAutores(campo);

  let entrada;
  if (campo.tipo === 'select') {
    entrada = h('select', { name: campo.nome, required: campo.obrigatorio });
    entrada.append(new Option('Selecione...', ''));
    for (const [valor, texto] of await campo.opcoes()) entrada.append(new Option(texto, valor));
  } else if (campo.tipo === 'textarea') {
    entrada = h('textarea', { name: campo.nome, rows: 3, required: campo.obrigatorio });
  } else {
    entrada = h('input', {
      name: campo.nome, type: campo.tipo || 'text', required: campo.obrigatorio,
      maxLength: campo.max, placeholder: campo.placeholder, step: campo.passo, min: campo.minimo,
    });
  }
  let datalist = null;
  if (campo.sugestoes) {
    const id = `sug-${campo.nome}`;
    entrada.setAttribute('list', id);
    datalist = h('datalist', { id }, campo.sugestoes.map((s) => h('option', { value: s })));
  }
  if (campo.valor !== undefined) entrada.value = campo.valor;
  if (campo.aoDigitar) entrada.addEventListener('input', () => campo.aoDigitar(entrada));
  if (campo.aoMudar) entrada.addEventListener('change', () => campo.aoMudar(form));

  const label = h('label', {}, campo.rotulo + marca, entrada, datalist);
  if (campo.dica) label.append(h('span', { className: 'dica' }, campo.dica));
  return label;
}

/** Campo de autores: escolhe participantes um a um, na ordem de autoria (RN-06). */
async function criarCampoAutores(campo) {
  const participantes = await lista('/api/participantes');
  const escolhidos = [];
  const caixa = h('div', { className: 'autores' });
  const seletor = h('select', {}, new Option('Escolha um participante...', ''),
    participantes.map((p) => new Option(`${p.nome} (${rotulo(p.tipo)})`, p.idParticipante)));

  function desenhar() {
    caixa.replaceChildren(...escolhidos.map((id, i) => {
      const p = participantes.find((x) => x.idParticipante === id);
      return h('div', { className: 'autor' },
        h('b', {}, `${i + 1}º`), h('span', {}, p ? p.nome : id),
        h('button', { type: 'button', className: 'link perigo', onclick: () => { escolhidos.splice(i, 1); desenhar(); } }, 'remover'));
    }));
    form.dataset.autores = JSON.stringify(escolhidos);
  }

  const adicionar = h('button', {
    type: 'button', className: 'secundario',
    onclick: () => {
      const id = Number(seletor.value);
      if (id && !escolhidos.includes(id)) escolhidos.push(id);
      seletor.value = '';
      desenhar();
    },
  }, 'Adicionar');

  desenhar();
  return h('label', {}, `${campo.rotulo} *`,
    h('span', { className: 'dica' }, 'Adicione na ordem de autoria. Todos precisam estar cadastrados como participantes.'),
    caixa, h('div', { className: 'add-autor' }, seletor, adicionar));
}

/** Le o formulario e converte os tipos (numeros, campos vazios viram null). */
function lerFormulario() {
  const dados = {};
  for (const campo of formularioAtual.campos) {
    if (campo.tipo === 'autores') {
      dados[campo.nome] = JSON.parse(form.dataset.autores || '[]');
      continue;
    }
    const valor = form.elements[campo.nome].value.trim();
    dados[campo.nome] = valor === '' ? null : (campo.numero ? Number(valor) : valor);
  }
  return dados;
}

form.onsubmit = async (evento) => {
  evento.preventDefault();
  erro.textContent = '';
  const botao = $('#salvar');
  botao.disabled = true;
  try {
    await enviar(formularioAtual.url, lerFormulario());
    dlg.close();
    avisar(formularioAtual.sucesso || 'Registro salvo com sucesso.');
    await mostrarPagina();
  } catch (e) {
    erro.textContent = e.message;     // mostra a regra violada (RN-xx) vinda do Java ou do MySQL
  } finally {
    botao.disabled = false;
  }
};

$('#cancelar').onclick = () => dlg.close();

// =====================================================================
// Opcoes reutilizadas nos selects
// =====================================================================
const opProjetos = async () =>
  (await lista('/api/projetos')).map((p) => [p.codigo, `${p.codigo} — ${p.titulo}`]);
const opParticipantes = async () =>
  (await lista('/api/participantes')).map((p) => [p.idParticipante, `${p.nome} (${rotulo(p.tipo)})`]);
const opDocentes = async () => (await lista('/api/docentes')).map((d) => [d.id, d.nome]);
const opAgencias = async () => (await lista('/api/agencias')).map((a) => [a.idAgencia, `${a.sigla} — ${a.nome}`]);
const opTipos = async () => (await lista('/api/tipos-producao')).map((t) => [t.idTipoProducao, rotulo(t.nome)]);
const opFinanciamentos = async () => (await lista('/api/financiamentos')).map((f) => [
  f.idFinanciamento,
  `${f.siglaAgencia} · ${rotulo(f.tipoFomento)} · saldo ${dinheiro(f.valorTotal - f.valorAlocado)}`,
]);
const fixo = (...valores) => async () => valores.map((v) => [v, rotulo(v)]);

/** Mascara de CPF enquanto digita: 000.000.000-00 */
function mascaraCpf(input) {
  const d = input.value.replace(/\D/g, '').slice(0, 11);
  input.value = d
    .replace(/^(\d{3})(\d)/, '$1.$2')
    .replace(/^(\d{3})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1-$2');
}

// =====================================================================
// Paginas de cadastro (configuracao de cada tela)
// =====================================================================
const PAGINAS = {
  visao: {
    titulo: 'Visão geral',
    subtitulo: 'Panorama dos projetos, da produção científica e dos recursos de fomento.',
    render: renderVisaoGeral,
  },

  projetos: {
    botaoNovo: '+ Novo projeto',
    titulo: 'Projetos de pesquisa',
    subtitulo: 'Cadastro e acompanhamento das pesquisas.',
    url: '/api/projetos',
    vazio: 'Nenhum projeto cadastrado.',
    filtros: [{ chave: 'situacao', rotulo: 'Todas as situações', opcoes: fixo('Em andamento', 'Concluido', 'Cancelado'), local: true }],
    colunas: [
      ['Código', (p) => h('b', {}, p.codigo)],
      ['Título', (p) => duasLinhas(p.titulo, p.descricao)],
      ['Coordenador', (p) => p.nomeCoordenador],
      ['Situação', (p) => selo(p.situacao)],
      ['Período', (p) => `${data(p.dataInicio)} a ${data(p.dataTermino)}`],
    ],
    acoes: (p) => [h('button', { className: 'link', onclick: () => alterarSituacao(p) }, 'Alterar situação')],
    novo: {
      titulo: 'Cadastrar projeto', botao: 'Salvar projeto', url: '/api/projetos', sucesso: 'Projeto cadastrado.',
      campos: [
        { nome: 'codigo', rotulo: 'Código', obrigatorio: true, max: 20, linha: 'a', placeholder: 'Ex.: P004' },
        { nome: 'situacao', rotulo: 'Situação', tipo: 'select', obrigatorio: true, opcoes: fixo('Em andamento', 'Concluido', 'Cancelado'), valor: 'Em andamento', linha: 'a' },
        { nome: 'titulo', rotulo: 'Título', obrigatorio: true, max: 200 },
        { nome: 'descricao', rotulo: 'Descrição', tipo: 'textarea', obrigatorio: true },
        { nome: 'dataInicio', rotulo: 'Data de início', tipo: 'date', obrigatorio: true, linha: 'b' },
        { nome: 'dataTermino', rotulo: 'Data de término', tipo: 'date', obrigatorio: true, linha: 'b' },
        { nome: 'idCoordenador', rotulo: 'Coordenador principal', tipo: 'select', obrigatorio: true, numero: true, opcoes: opDocentes, dica: 'Somente docentes podem coordenar.' },
      ],
    },
  },

  participantes: {
    botaoNovo: '+ Novo participante',
    titulo: 'Participantes',
    subtitulo: 'Docentes, discentes e técnicos-administrativos da universidade.',
    url: '/api/participantes',
    vazio: 'Nenhum participante cadastrado.',
    filtros: [{ chave: 'tipo', rotulo: 'Todos os tipos', opcoes: fixo('Docente', 'Discente', 'Tecnico'), local: true }],
    colunas: [
      ['Nome', (p) => h('b', {}, p.nome)],
      ['Tipo', (p) => selo(p.tipo)],
      ['CPF', (p) => p.cpf],
      ['E-mail institucional', (p) => p.emailInstitucional],
    ],
    acoes: (p) => [h('button', { className: 'link perigo', onclick: () => inativarParticipante(p) }, 'Inativar')],
    novo: {
      titulo: 'Cadastrar participante', botao: 'Salvar participante', url: '/api/participantes', sucesso: 'Participante cadastrado.',
      campos: [
        { nome: 'nome', rotulo: 'Nome completo', obrigatorio: true, max: 150 },
        { nome: 'tipo', rotulo: 'Tipo', tipo: 'select', obrigatorio: true, opcoes: fixo('Docente', 'Discente', 'Tecnico'), linha: 'a' },
        { nome: 'cpf', rotulo: 'CPF', obrigatorio: true, placeholder: '000.000.000-00', aoDigitar: mascaraCpf, linha: 'a', dica: 'Único no sistema.' },
        { nome: 'emailInstitucional', rotulo: 'E-mail institucional', tipo: 'email', obrigatorio: true, max: 120, placeholder: 'nome@ufnes.edu.br', dica: 'Único no sistema.' },
      ],
    },
  },

  vinculos: {
    botaoNovo: '+ Novo vínculo',
    titulo: 'Vínculos de participação',
    subtitulo: 'Quem atua em cada projeto, com função e período.',
    url: '/api/vinculos',
    vazio: 'Nenhum vínculo encontrado.',
    filtros: [
      { chave: 'projeto', rotulo: 'Todos os projetos', opcoes: opProjetos },
      { chave: 'status', rotulo: 'Ativos e encerrados', opcoes: fixo('Ativo', 'Encerrado'), local: true },
    ],
    colunas: [
      ['Participante', (v) => duasLinhas(v.nomeParticipante, rotulo(v.tipoParticipante))],
      ['Projeto', (v) => duasLinhas(v.codigoProjeto, v.tituloProjeto)],
      ['Função', (v) => v.funcao],
      ['Entrada', (v) => data(v.dataEntrada)],
      ['Saída', (v) => (v.dataSaida ? data(v.dataSaida) : selo('Ativo'))],
    ],
    acoes: (v) => (v.dataSaida ? [] : [h('button', { className: 'link', onclick: () => encerrarVinculo(v) }, 'Encerrar')]),
    novo: {
      titulo: 'Vincular participante a projeto', botao: 'Salvar vínculo', url: '/api/vinculos', sucesso: 'Vínculo registrado.',
      campos: [
        { nome: 'codigoProjeto', rotulo: 'Projeto', tipo: 'select', obrigatorio: true, opcoes: opProjetos },
        { nome: 'idParticipante', rotulo: 'Participante', tipo: 'select', obrigatorio: true, numero: true, opcoes: opParticipantes },
        { nome: 'funcao', rotulo: 'Função', obrigatorio: true, max: 40, sugestoes: ['Coordenador', 'Bolsista', 'Colaborador', 'Pesquisador', 'Voluntário'], dica: '"Coordenador" só para o coordenador principal do projeto.' },
        { nome: 'dataEntrada', rotulo: 'Data de entrada', tipo: 'date', obrigatorio: true, linha: 'a' },
        { nome: 'dataSaida', rotulo: 'Data de saída', tipo: 'date', linha: 'a' },
      ],
    },
  },

  producoes: {
    botaoNovo: '+ Nova produção',
    titulo: 'Produções científicas',
    subtitulo: 'Artigos, capítulos, softwares e relatórios gerados pelos projetos.',
    url: '/api/producoes',
    vazio: 'Nenhuma produção encontrada.',
    filtros: [
      { chave: 'projeto', rotulo: 'Todos os projetos', opcoes: opProjetos },
      { chave: 'ano', rotulo: 'Todos os anos', opcoes: async () => anos() },
    ],
    colunas: [
      ['Título', (p) => duasLinhas(p.titulo, p.meioDivulgacao)],
      ['Tipo', (p) => selo(p.nomeTipo)],
      ['Ano', (p) => p.anoPublicacao, 'num'],
      ['Autores (em ordem)', (p) => p.nomesAutores || '—'],
      ['Projeto', (p) => duasLinhas(p.codigoProjeto, p.tituloProjeto)],
    ],
    novo: {
      titulo: 'Registrar produção científica', botao: 'Salvar produção', url: '/api/producoes', sucesso: 'Produção registrada com seus autores.',
      campos: [
        { nome: 'codigoProjeto', rotulo: 'Projeto que gerou a produção', tipo: 'select', obrigatorio: true, opcoes: opProjetos },
        { nome: 'titulo', rotulo: 'Título', obrigatorio: true, max: 200 },
        { nome: 'idTipoProducao', rotulo: 'Tipo', tipo: 'select', obrigatorio: true, numero: true, opcoes: opTipos, linha: 'a' },
        { nome: 'anoPublicacao', rotulo: 'Ano de publicação', tipo: 'number', obrigatorio: true, numero: true, minimo: 1900, valor: new Date().getFullYear(), linha: 'a' },
        { nome: 'meioDivulgacao', rotulo: 'Meio de divulgação', obrigatorio: true, max: 150, placeholder: 'Revista, congresso, repositório...', dica: 'O ano não pode ser anterior ao início do projeto.' },
        { nome: 'autores', rotulo: 'Autores', tipo: 'autores' },
      ],
    },
  },

  agencias: {
    botaoNovo: '+ Nova agência',
    titulo: 'Agências de fomento',
    subtitulo: 'Instituições que financiam a pesquisa, com o total concedido.',
    url: '/api/agencias',
    vazio: 'Nenhuma agência cadastrada.',
    colunas: [
      ['Sigla', (a) => h('b', {}, a.sigla)],
      ['Nome', (a) => a.nome],
      ['Financiamentos', (a) => a.qtdFinanciamentos, 'num'],
      ['Total financiado', (a) => dinheiro(a.totalFinanciado), 'num'],
    ],
    novo: {
      titulo: 'Cadastrar agência de fomento', botao: 'Salvar agência', url: '/api/agencias', sucesso: 'Agência cadastrada.',
      campos: [
        { nome: 'sigla', rotulo: 'Sigla', obrigatorio: true, max: 20, placeholder: 'Ex.: FAPEMIG' },
        { nome: 'nome', rotulo: 'Nome oficial', obrigatorio: true, max: 150 },
      ],
    },
  },

  financiamentos: {
    botaoNovo: '+ Novo financiamento',
    titulo: 'Financiamentos',
    subtitulo: 'Recursos concedidos, vigência e quanto já foi distribuído aos projetos.',
    url: '/api/financiamentos',
    vazio: 'Nenhum financiamento cadastrado.',
    colunas: [
      ['Agência', (f) => h('b', {}, f.siglaAgencia)],
      ['Tipo de fomento', (f) => rotulo(f.tipoFomento)],
      ['Vigência', (f) => `${data(f.inicioVigencia)} a ${data(f.fimVigencia)}`],
      ['Valor total', (f) => dinheiro(f.valorTotal), 'num'],
      ['Alocado', (f) => {
        const pct = f.valorTotal > 0 ? Math.round((f.valorAlocado / f.valorTotal) * 100) : 0;
        return duasLinhas(dinheiro(f.valorAlocado), `${pct}% · saldo ${dinheiro(f.valorTotal - f.valorAlocado)}`);
      }, 'num'],
    ],
    novo: {
      titulo: 'Cadastrar financiamento', botao: 'Salvar financiamento', url: '/api/financiamentos', sucesso: 'Financiamento cadastrado.',
      campos: [
        { nome: 'idAgencia', rotulo: 'Agência financiadora', tipo: 'select', obrigatorio: true, numero: true, opcoes: opAgencias },
        { nome: 'tipoFomento', rotulo: 'Tipo de fomento', obrigatorio: true, max: 50, sugestoes: ['Bolsa', 'Auxilio', 'Convenio'] },
        { nome: 'inicioVigencia', rotulo: 'Início da vigência', tipo: 'date', obrigatorio: true, linha: 'a' },
        { nome: 'fimVigencia', rotulo: 'Fim da vigência', tipo: 'date', obrigatorio: true, linha: 'a' },
        { nome: 'valorTotal', rotulo: 'Valor total (R$)', tipo: 'number', obrigatorio: true, numero: true, passo: '0.01', minimo: '0.01' },
      ],
    },
  },

  alocacoes: {
    botaoNovo: '+ Nova alocação',
    titulo: 'Alocações de recursos',
    subtitulo: 'Como cada financiamento é dividido entre os projetos, total ou parcialmente.',
    url: '/api/alocacoes',
    vazio: 'Nenhuma alocação registrada.',
    colunas: [
      ['Financiamento', (a) => duasLinhas(a.siglaAgencia, rotulo(a.tipoFomento))],
      ['Projeto', (a) => duasLinhas(a.codigoProjeto, a.tituloProjeto)],
      ['Modalidade', (a) => selo(a.modalidade)],
      ['Valor destinado', (a) => dinheiro(a.valorDestinado), 'num'],
    ],
    novo: {
      titulo: 'Alocar financiamento a projeto', botao: 'Salvar alocação', url: '/api/alocacoes', sucesso: 'Recurso alocado ao projeto.',
      campos: [
        {
          nome: 'idFinanciamento', rotulo: 'Financiamento', tipo: 'select', obrigatorio: true, numero: true, opcoes: opFinanciamentos,
          aoMudar: preencherSaldo, dica: 'A soma das alocações não pode passar do valor total.',
        },
        { nome: 'codigoProjeto', rotulo: 'Projeto', tipo: 'select', obrigatorio: true, opcoes: opProjetos },
        { nome: 'modalidade', rotulo: 'Modalidade', tipo: 'select', obrigatorio: true, opcoes: fixo('Parcial', 'Total'), linha: 'a', aoMudar: preencherSaldo },
        { nome: 'valorDestinado', rotulo: 'Valor destinado (R$)', tipo: 'number', obrigatorio: true, numero: true, passo: '0.01', minimo: '0.01', linha: 'a' },
      ],
    },
  },

  relatorios: {
    titulo: 'Relatórios',
    subtitulo: 'Consultas gerenciais sobre projetos, produções e financiamentos.',
    render: renderRelatorios,
  },
};

/** Na alocacao "Total", sugere o saldo inteiro do financiamento escolhido. */
async function preencherSaldo(f) {
  const id = Number(f.elements.idFinanciamento.value);
  if (!id || f.elements.modalidade.value !== 'Total') return;
  const fin = (await lista('/api/financiamentos')).find((x) => x.idFinanciamento === id);
  if (fin) f.elements.valorDestinado.value = (fin.valorTotal - fin.valorAlocado).toFixed(2);
}

async function anos() {
  const producoes = await lista('/api/producoes');
  return [...new Set(producoes.map((p) => p.anoPublicacao))].sort((a, b) => b - a).map((a) => [a, a]);
}

// =====================================================================
// Acoes por linha
// =====================================================================
function alterarSituacao(p) {
  abrirFormulario({
    titulo: `Situação do projeto ${p.codigo}`, botao: 'Atualizar', url: '/api/projetos/situacao', sucesso: 'Situação atualizada.',
    campos: [
      { nome: 'codigo', rotulo: 'Projeto', valor: p.codigo, tipo: 'text' },
      { nome: 'situacao', rotulo: 'Nova situação', tipo: 'select', obrigatorio: true, opcoes: fixo('Em andamento', 'Concluido', 'Cancelado'), valor: p.situacao, dica: 'Domínio fechado: Em andamento, Concluído ou Cancelado.' },
    ],
    aoAbrir: (f) => { f.elements.codigo.readOnly = true; },
  });
}

function encerrarVinculo(v) {
  abrirFormulario({
    titulo: 'Encerrar vínculo', botao: 'Encerrar vínculo', url: '/api/vinculos/encerrar', sucesso: 'Vínculo encerrado.',
    campos: [
      { nome: 'idVinculo', rotulo: 'Vínculo', valor: v.idVinculo, numero: true },
      { nome: 'dataSaida', rotulo: `Data de saída de ${v.nomeParticipante} em ${v.codigoProjeto}`, tipo: 'date', obrigatorio: true, valor: hoje(), dica: 'Deve ser posterior à entrada e dentro da vigência do projeto.' },
    ],
    aoAbrir: (f) => { f.elements.idVinculo.closest('label').hidden = true; },
  });
}

function inativarParticipante(p) {
  abrirFormulario({
    titulo: `Inativar ${p.nome}?`, botao: 'Inativar', url: '/api/participantes/inativar', sucesso: 'Participante inativado.',
    campos: [{ nome: 'idParticipante', rotulo: 'Participante', valor: p.idParticipante, numero: true }],
    aoAbrir: (f) => {
      const label = f.elements.idParticipante.closest('label');
      label.hidden = true;
      label.after(h('p', {},
        'O cadastro deixa de aparecer nas listagens, mas o histórico de vínculos e autorias é mantido.'));
    },
  });
}

// =====================================================================
// Tela de listagem generica (busca + filtros + tabela)
// =====================================================================
const filtrosAtivos = {};

async function renderLista(pagina, conteudo) {
  const estado = (filtrosAtivos[pagina.url] ||= { busca: '' });
  const parametros = new URLSearchParams();
  for (const f of pagina.filtros || []) if (!f.local && estado[f.chave]) parametros.set(f.chave, estado[f.chave]);
  const url = parametros.size ? `${pagina.url}?${parametros}` : pagina.url;
  let registros = await api(url);

  // filtros feitos no navegador (sem ida ao servidor)
  for (const f of pagina.filtros || []) {
    if (!f.local || !estado[f.chave]) continue;
    registros = registros.filter((r) => {
      if (f.chave === 'status') return estado.status === 'Ativo' ? !r.dataSaida : !!r.dataSaida;
      return r[f.chave] === estado[f.chave];
    });
  }

  const corpo = h('tbody');
  const contador = h('span', { className: 'contador' });
  const vazio = h('div', { className: 'vazio', hidden: true }, pagina.vazio);

  function desenhar() {
    const termo = estado.busca.toLowerCase();
    const visiveis = registros.filter((r) => !termo || JSON.stringify(r).toLowerCase().includes(termo));
    corpo.replaceChildren(...visiveis.map((r) => h('tr', {},
      pagina.colunas.map(([, valor, classe]) => h('td', { className: classe }, valor(r))),
      pagina.acoes ? h('td', { className: 'acoes-linha' }, pagina.acoes(r)) : null)));
    contador.textContent = visiveis.length === registros.length
      ? plural(registros.length, 'registro')
      : `${visiveis.length} de ${plural(registros.length, 'registro')}`;
    vazio.hidden = visiveis.length > 0;
  }

  const busca = h('input', {
    type: 'search', placeholder: 'Buscar...', value: estado.busca,
    oninput: (e) => { estado.busca = e.target.value; desenhar(); },
  });

  const filtros = [];
  for (const f of pagina.filtros || []) {
    const sel = h('select', {
      onchange: (e) => { estado[f.chave] = e.target.value; mostrarPagina(); },
    }, new Option(f.rotulo, ''));
    for (const [valor, texto] of await f.opcoes()) sel.append(new Option(texto, valor));
    sel.value = estado[f.chave] || '';
    filtros.push(sel);
  }

  const cabecalho = h('tr', {},
    pagina.colunas.map(([titulo, , classe]) => h('th', { className: classe }, titulo)),
    pagina.acoes ? h('th', {}, '') : null);

  conteudo.replaceChildren(
    h('div', { className: 'barra' }, busca, filtros, contador),
    h('div', { className: 'tabela' }, h('table', {}, h('thead', {}, cabecalho), corpo), vazio),
  );
  desenhar();
}

// =====================================================================
// Visao geral
// =====================================================================
async function renderVisaoGeral(conteudo) {
  const [r, agencias, producoes, projetos] = await Promise.all([
    api('/api/resumo'), api('/api/agencias'), api('/api/producoes'), api('/api/projetos'),
  ]);
  const pctAlocado = r.totalFinanciado > 0 ? Math.round((r.totalAlocado / r.totalFinanciado) * 100) : 0;

  const card = (rotuloCard, valor, detalhe, extra) =>
    h('div', { className: 'card' }, h('div', { className: 'rotulo' }, rotuloCard), h('div', { className: 'valor' }, valor),
      h('div', { className: 'detalhe' }, detalhe), extra);

  const maior = Math.max(1, ...agencias.map((a) => a.totalFinanciado));
  const barras = h('div', { className: 'barras' }, agencias.map((a) => h('div', { className: 'item' },
    h('b', {}, a.sigla),
    h('div', { className: 'trilho' }, h('div', { className: 'preenchido', style: `width:${(a.totalFinanciado / maior) * 100}%` })),
    h('span', { className: 'num' }, dinheiro(a.totalFinanciado)))));

  const contagem = {};
  for (const p of projetos) contagem[p.situacao] = (contagem[p.situacao] || 0) + 1;

  conteudo.replaceChildren(
    h('div', { className: 'cards' },
      card('Projetos em andamento', r.projetosEmAndamento, `de ${plural(r.projetos, 'projeto ativo', 'projetos ativos')}`),
      card('Participantes', r.participantes, plural(r.vinculosAtivos, 'vínculo ativo', 'vínculos ativos')),
      card('Produções científicas', r.producoes, 'registradas no sistema'),
      card('Total financiado', dinheiro(r.totalFinanciado), `${pctAlocado}% já alocado aos projetos`,
        h('div', { className: 'progresso' }, h('div', { style: `width:${pctAlocado}%` })))),
    h('div', { className: 'paineis' },
      h('div', { className: 'painel' }, h('h3', {}, 'Financiamento por agência'),
        h('small', {}, 'Soma do valor total dos financiamentos ativos.'), barras),
      h('div', { className: 'painel' }, h('h3', {}, 'Projetos por situação'),
        h('small', {}, 'Quantidade de projetos em cada situação.'),
        h('ul', { className: 'lista-simples' }, ['Em andamento', 'Concluido', 'Cancelado'].map((s) =>
          h('li', {}, selo(s), ' ', plural(contagem[s] || 0, 'projeto'))))),
      h('div', { className: 'painel' }, h('h3', {}, 'Produções mais recentes'),
        h('small', {}, 'Últimas publicações registradas.'),
        h('ul', { className: 'lista-simples' }, producoes.slice(0, 4).map((p) =>
          h('li', {}, h('b', {}, p.titulo), h('small', {}, `${rotulo(p.nomeTipo)} · ${p.anoPublicacao} · ${p.nomesAutores || ''}`)))))),
  );
}

// =====================================================================
// Relatorios (consultas do roteiro, com o SQL visivel para a apresentacao)
// =====================================================================
const RELATORIOS = [
  {
    titulo: 'Projetos com seus coordenadores',
    descricao: 'Todos os projetos e quem os coordena. Filtre por docente para ver só os projetos dele.',
    url: '/api/relatorios/projetos-coordenadores',
    filtro: { chave: 'docente', rotulo: 'Todos os docentes', opcoes: opDocentes },
    sql: `SELECT p.codigo, p.titulo, c.nome AS coordenador, p.situacao
FROM projeto_pesquisa p
JOIN participante c ON c.id_participante = p.id_coordenador
WHERE p.ativo = TRUE
ORDER BY c.nome, p.codigo;`,
    colunas: [['Coordenador', 'coordenador'], ['Código', 'codigo'], ['Título', 'titulo'], ['Situação', 'situacao', selo]],
  },
  {
    titulo: 'Produções científicas por tipo e ano',
    descricao: 'Quantidade de produções de cada tipo, ano a ano.',
    url: '/api/relatorios/producoes-tipo-ano',
    sql: `SELECT pc.ano_publicacao AS ano, tp.nome AS tipo, COUNT(*) AS quantidade
FROM producao_cientifica pc
JOIN tipo_producao tp ON tp.id_tipo_producao = pc.id_tipo_producao
GROUP BY pc.ano_publicacao, tp.nome
ORDER BY ano DESC, quantidade DESC;`,
    colunas: [['Ano', 'ano'], ['Tipo', 'tipo', rotulo], ['Quantidade', 'quantidade', null, 'num']],
  },
  {
    titulo: 'Total de financiamento por agência',
    descricao: 'Quanto cada agência concedeu, incluindo as que ainda não têm financiamento.',
    url: '/api/relatorios/total-agencia',
    sql: `SELECT ag.sigla, COUNT(f.id_financiamento) AS qtd,
       COALESCE(SUM(f.valor_total), 0) AS total
FROM agencia_fomento ag
LEFT JOIN financiamento f ON f.id_agencia = ag.id_agencia AND f.ativo = TRUE
WHERE ag.ativo = TRUE
GROUP BY ag.id_agencia, ag.sigla
ORDER BY total DESC;`,
    colunas: [['Sigla', 'sigla'], ['Agência', 'nome'], ['Financiamentos', 'qtdFinanciamentos', null, 'num'], ['Total', 'totalFinanciado', dinheiro, 'num']],
  },
  {
    titulo: 'Participantes que atuam em mais de um projeto',
    descricao: 'Participantes com vínculo ativo em dois ou mais projetos ao mesmo tempo.',
    url: '/api/relatorios/varios-projetos',
    sql: `SELECT pt.nome, COUNT(DISTINCT v.codigo_projeto) AS qtd_projetos
FROM vinculo_participacao v
JOIN participante pt ON pt.id_participante = v.id_participante
WHERE v.data_saida IS NULL
GROUP BY pt.id_participante, pt.nome
HAVING COUNT(DISTINCT v.codigo_projeto) > 1;`,
    colunas: [['Participante', 'nome'], ['Tipo', 'tipo', selo], ['Projetos ativos', 'qtdProjetos', null, 'num'], ['Quais', 'projetos']],
  },
];

async function renderRelatorios(conteudo) {
  conteudo.replaceChildren(...RELATORIOS.map((rel) => {
    const area = h('div', {}, h('div', { className: 'vazio' }, 'Carregando...'));
    const sql = h('pre', { hidden: true }, rel.sql);
    const carregar = async (valorFiltro) => {
      const url = valorFiltro ? `${rel.url}?${rel.filtro.chave}=${encodeURIComponent(valorFiltro)}` : rel.url;
      const linhas = await api(url);
      area.replaceChildren(h('div', { className: 'tabela' },
        h('table', {},
          h('thead', {}, h('tr', {}, rel.colunas.map(([t, , , c]) => h('th', { className: c }, t)))),
          h('tbody', {}, linhas.map((l) => h('tr', {}, rel.colunas.map(([, campo, formato, c]) =>
            h('td', { className: c }, formato ? formato(l[campo]) : (l[campo] ?? '—'))))))),
        linhas.length ? null : h('div', { className: 'vazio' }, 'Nenhum resultado.')));
    };

    const controles = [h('button', {
      className: 'link', onclick: (e) => { sql.hidden = !sql.hidden; e.target.textContent = sql.hidden ? 'Ver SQL' : 'Ocultar SQL'; },
    }, 'Ver SQL')];
    if (rel.filtro) {
      const sel = h('select', { onchange: (e) => carregar(e.target.value) }, new Option(rel.filtro.rotulo, ''));
      rel.filtro.opcoes().then((ops) => ops.forEach(([v, t]) => sel.append(new Option(t, v))));
      sel.style.width = 'auto';
      controles.unshift(sel);
    }
    carregar().catch((e) => area.replaceChildren(h('div', { className: 'vazio' }, e.message)));

    return h('div', { className: 'painel relatorio' },
      h('div', { className: 'topo' },
        h('div', {}, h('h3', {}, rel.titulo), h('small', {}, rel.descricao)),
        h('div', { className: 'barra' }, controles)),
      sql, area);
  }));
}

// =====================================================================
// Navegacao (o endereco #projetos, #participantes... escolhe a tela)
// =====================================================================
async function mostrarPagina() {
  cache = {};
  const chave = location.hash.slice(1) || 'visao';
  const pagina = PAGINAS[chave] || PAGINAS.visao;

  document.querySelectorAll('#menu a').forEach((a) => a.classList.toggle('ativo', a.dataset.pagina === chave));
  $('#titulo').textContent = pagina.titulo;
  $('#subtitulo').textContent = pagina.subtitulo;
  document.title = `SIGPesq - ${pagina.titulo}`;

  const novo = $('#novo');
  novo.hidden = !pagina.novo;
  if (pagina.novo) {
    novo.textContent = pagina.botaoNovo;
    novo.onclick = () => abrirFormulario(pagina.novo);
  }

  const conteudo = $('#conteudo');
  try {
    if (pagina.render) await pagina.render(conteudo);
    else await renderLista(pagina, conteudo);
  } catch (e) {
    conteudo.replaceChildren(h('div', { className: 'tabela' }, h('div', { className: 'vazio' },
      'Não foi possível carregar os dados. Verifique a conexão e tente novamente.')));
  }
}

window.addEventListener('hashchange', mostrarPagina);
mostrarPagina();
