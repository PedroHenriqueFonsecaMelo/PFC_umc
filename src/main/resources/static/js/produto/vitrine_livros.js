/* ================================================================
   vitrine_livros.js — Vitrine de livros · Bibliotroca
   Busca em tempo real + filtros (estado, preço, ordem) + chips
   ================================================================ */

/* ── Saldo na navbar ── */
async function carregarSaldo() {
    try {
        const res = await fetch('/clientes/meu-perfil-json', { credentials: 'include' });
        if (!res.ok) return;
        const c = await res.json();
        const navSaldo = document.getElementById('navSaldo');
        if (navSaldo) navSaldo.textContent = `T$ ${(c.saldoTokens || 0).toFixed(2)}`;
    } catch (_) {}
}

/* ── Estado global ── */
let _todosLivros = [];   // todos os livros carregados do servidor
let modoPromo    = false;

let _filtros = {
    busca:    '',
    estados:  [],        // ex: ['BOM', 'OTIMO']
    precoMin: null,
    precoMax: null,
    ordem:    'relevancia'
};

let _debounceTimer = null;

/* ── Normalize: remove acentos e converte para minúsculo ── */
function norm(str) {
    return (str || '')
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .toLowerCase();
}

/* ── Contador regressivo das promoções ── */
let contadorInterval = null;

function iniciarContadores() {
    if (contadorInterval) clearInterval(contadorInterval);

    const atualizar = () => {
        const agora = Date.now();
        document.querySelectorAll('.promo-countdown').forEach(el => {
            const expira = new Date(el.dataset.expira).getTime();
            const diff   = expira - agora;
            const id     = el.dataset.livroId;

            if (diff <= 0) {
                el.style.display = 'none';
                const badge = document.getElementById('badge-' + id);
                if (badge) badge.style.display = 'none';
                const precoEl = document.getElementById('preco-' + id);
                if (precoEl) {
                    const precoOriginal = parseFloat(el.dataset.precoOriginal) || 0;
                    precoEl.innerHTML = `T$ ${precoOriginal.toFixed(2)}`;
                    precoEl.style.color = '';
                }
            } else {
                const h = Math.floor(diff / 3600000);
                const m = Math.floor((diff % 3600000) / 60000);
                const s = Math.floor((diff % 60000) / 1000);
                const timerEl = document.getElementById('timer-' + id);
                if (timerEl) timerEl.textContent = `${h}h ${m}m ${s}s`;
            }
        });
    };

    atualizar();
    contadorInterval = setInterval(atualizar, 1000);
}

/* ── Toggle: somente promoções ── */
function togglePromo() {
    modoPromo = !modoPromo;
    const btn    = document.getElementById('btnPromo');
    const titulo = document.getElementById('vitrineTitulo');

    if (modoPromo) {
        btn.classList.add('ativo');
        btn.textContent = '✕ Ver Todos';
        if (titulo) titulo.textContent = 'Promoções';
    } else {
        btn.classList.remove('ativo');
        btn.textContent = '🏷 Ver Promoções';
        if (titulo) titulo.textContent = 'Livros Disponíveis';
    }
    carregarLivros();
}

/* ── Classe CSS por estado ── */
function classeEstado(estado) {
    const mapa = {
        'ÓTIMO': 'otimo', 'OTIMO': 'otimo',
        'BOM':   'bom',   'COMO_NOVO': 'otimo',
        'REGULAR': 'regular', 'RUIM': 'ruim', 'NOVO': 'otimo'
    };
    return mapa[(estado || '').toUpperCase()] || 'bom';
}

/* ── Renderiza a grade com a lista já filtrada/ordenada ── */
function renderLivros(livros) {
    const grid = document.getElementById('gridLivros');
    if (!grid) return;

    atualizarContador(livros.length);

    if (livros.length === 0) {
        grid.innerHTML = `
            <div class="vitrine-vazio" style="grid-column:1/-1;">
                <div class="vazio-icon">📚</div>
                <p class="vazio-msg">Nenhum livro encontrado para sua busca.</p>
                <button class="btn-limpar-vazio" onclick="limparFiltros()">Limpar filtros</button>
            </div>`;
        return;
    }

    grid.innerHTML = livros.map(livro => {
        let foto = 'https://via.placeholder.com/300x400?text=Sem+Foto';
        try {
            const arr = JSON.parse(livro.fotosUrls);
            if (Array.isArray(arr) && arr.length > 0) foto = arr[0];
        } catch (_) {}

        const badgePromo = livro.emPromocao
            ? `<span id="badge-${livro.id}" style="position:absolute;top:.5rem;left:.5rem;
                   background:#e11d48;color:#fff;font-size:.7rem;font-weight:700;
                   padding:.2rem .55rem;text-transform:uppercase;letter-spacing:.04em;">PROMOÇÃO</span>`
            : '';

        let precoHtml;
        if (livro.emPromocao && livro.precoOriginal) {
            precoHtml = `<div class="livro-preco" id="preco-${livro.id}">
                <span style="text-decoration:line-through;color:#9a8a80;font-size:.85rem;margin-right:.4rem;">
                    T$ ${livro.precoOriginal.toFixed(2)}
                </span>
                <span style="color:#e11d48;font-weight:700;">
                    T$ ${(livro.precoAprovado || 0).toFixed(2)}
                </span>
            </div>`;
        } else {
            precoHtml = `<div class="livro-preco" id="preco-${livro.id}">
                T$ ${(livro.precoAprovado || 0).toFixed(2)}
            </div>`;
        }

        const countdownHtml = (livro.emPromocao && livro.promocaoExpira)
            ? `<div class="promo-countdown"
                    data-livro-id="${livro.id}"
                    data-expira="${livro.promocaoExpira}"
                    data-preco-original="${livro.precoOriginal || livro.precoAprovado || 0}"
                    style="margin-top:.45rem;font-size:.82rem;font-weight:700;color:#e11d48;">
                   🔥 Oferta expira em: <span id="timer-${livro.id}">…</span>
               </div>`
            : '';

        const estadoLabel = (livro.estadoAprovado || 'BOM').replace('_', ' ');

        return `
        <a href="/livros/${livro.id}" class="livro-card"
           style="position:relative;text-decoration:none;color:inherit;"
           title="Ver detalhes de ${livro.titulo}">
            ${badgePromo}
            <div class="livro-card-img">
                <img src="${foto}" alt="${livro.titulo}"
                     onerror="this.src='https://via.placeholder.com/300x400?text=📚'">
            </div>
            <div class="livro-card-body">
                <span class="livro-estado estado-${classeEstado(estadoLabel)}">${estadoLabel}</span>
                <h3 class="livro-titulo">${livro.titulo}</h3>
                <p class="livro-autor">por ${livro.autor}</p>
                ${precoHtml}
                ${countdownHtml}
            </div>
        </a>`;
    }).join('');

    iniciarContadores();
}

/* ── Aplica todos os filtros sobre _todosLivros ── */
function aplicarFiltros() {
    let lista = [..._todosLivros];

    /* Busca textual */
    if (_filtros.busca) {
        const termo = norm(_filtros.busca);
        lista = lista.filter(l =>
            norm(l.titulo).includes(termo) ||
            norm(l.autor).includes(termo)  ||
            norm(l.isbn || '').includes(termo)
        );
    }

    /* Estado físico (múltipla seleção) */
    if (_filtros.estados.length > 0) {
        lista = lista.filter(l => {
            const est = (l.estadoAprovado || 'BOM').toUpperCase();
            return _filtros.estados.includes(est);
        });
    }

    /* Faixa de preço */
    if (_filtros.precoMin !== null) {
        lista = lista.filter(l => (l.precoAprovado || 0) >= _filtros.precoMin);
    }
    if (_filtros.precoMax !== null) {
        lista = lista.filter(l => (l.precoAprovado || 0) <= _filtros.precoMax);
    }

    /* Ordenação */
    switch (_filtros.ordem) {
        case 'menor_preco':
            lista.sort((a, b) => (a.precoAprovado || 0) - (b.precoAprovado || 0)); break;
        case 'maior_preco':
            lista.sort((a, b) => (b.precoAprovado || 0) - (a.precoAprovado || 0)); break;
        case 'recente':
            lista.sort((a, b) => (b.id || 0) - (a.id || 0)); break;
        case 'az':
            lista.sort((a, b) => (a.titulo || '').localeCompare(b.titulo || '', 'pt-BR')); break;
        case 'za':
            lista.sort((a, b) => (b.titulo || '').localeCompare(a.titulo || '', 'pt-BR')); break;
    }

    renderLivros(lista);
    renderChips();
}

/* ── Lê valores do painel de filtros e aplica ── */
function lerEAplicarFiltros() {
    /* Estado: coleta checkboxes marcadas */
    const checks = document.querySelectorAll('#filtroEstados input[type=checkbox]:checked');
    _filtros.estados = Array.from(checks).map(c => c.value);

    /* Preço */
    const minVal = document.getElementById('filtroPrecoMin')?.value;
    const maxVal = document.getElementById('filtroPrecoMax')?.value;
    _filtros.precoMin = minVal !== '' && minVal !== null ? parseFloat(minVal) : null;
    _filtros.precoMax = maxVal !== '' && maxVal !== null ? parseFloat(maxVal) : null;

    /* Ordenação */
    _filtros.ordem = document.getElementById('filtroOrdem')?.value || 'relevancia';

    aplicarFiltros();
    fecharPainelFiltros();
}

/* ── Contador ── */
function atualizarContador(n) {
    const el = document.getElementById('vitrineContador');
    if (el) el.textContent = `Exibindo ${n} livro${n !== 1 ? 's' : ''}`;
}

/* ── Chips de filtros ativos ── */
const ESTADO_LABEL_MAP = {
    NOVO: 'Novo', OTIMO: 'Ótimo', BOM: 'Bom', REGULAR: 'Regular', RUIM: 'Ruim'
};
const ORDEM_LABEL_MAP = {
    menor_preco: 'Menor preço', maior_preco: 'Maior preço',
    recente: 'Mais recente', az: 'A–Z', za: 'Z–A'
};

function renderChips() {
    const container = document.getElementById('filtroChips');
    if (!container) return;
    const chips = [];

    _filtros.estados.forEach(est => {
        chips.push(`<span class="filtro-chip">Estado: ${ESTADO_LABEL_MAP[est] || est}
            <button onclick="removerChipEstado('${est}')" aria-label="Remover filtro">×</button></span>`);
    });

    if (_filtros.precoMin !== null) {
        chips.push(`<span class="filtro-chip">De T$ ${_filtros.precoMin.toFixed(2)}
            <button onclick="removerChipPrecoMin()" aria-label="Remover filtro">×</button></span>`);
    }
    if (_filtros.precoMax !== null) {
        chips.push(`<span class="filtro-chip">Até T$ ${_filtros.precoMax.toFixed(2)}
            <button onclick="removerChipPrecoMax()" aria-label="Remover filtro">×</button></span>`);
    }
    if (_filtros.ordem !== 'relevancia') {
        chips.push(`<span class="filtro-chip">Ordenar: ${ORDEM_LABEL_MAP[_filtros.ordem] || _filtros.ordem}
            <button onclick="removerChipOrdem()" aria-label="Remover filtro">×</button></span>`);
    }

    container.innerHTML = chips.join('');
}

/* ── Remoção individual de chips ── */
window.removerChipEstado = function(est) {
    _filtros.estados = _filtros.estados.filter(e => e !== est);
    /* Desmarca checkbox correspondente */
    const cb = document.querySelector(`#filtroEstados input[value="${est}"]`);
    if (cb) cb.checked = false;
    aplicarFiltros();
};

window.removerChipPrecoMin = function() {
    _filtros.precoMin = null;
    const el = document.getElementById('filtroPrecoMin');
    if (el) el.value = '';
    aplicarFiltros();
};

window.removerChipPrecoMax = function() {
    _filtros.precoMax = null;
    const el = document.getElementById('filtroPrecoMax');
    if (el) el.value = '';
    aplicarFiltros();
};

window.removerChipOrdem = function() {
    _filtros.ordem = 'relevancia';
    const sel = document.getElementById('filtroOrdem');
    if (sel) sel.value = 'relevancia';
    aplicarFiltros();
};

/* ── Busca em tempo real (debounce 400ms) ── */
function onBuscaInput() {
    const val = document.getElementById('vitrineBusca')?.value || '';
    const btnX = document.getElementById('btnLimparBusca');
    if (btnX) btnX.style.display = val ? 'flex' : 'none';

    clearTimeout(_debounceTimer);
    _debounceTimer = setTimeout(() => {
        _filtros.busca = val;
        aplicarFiltros();
    }, 400);
}

function limparBusca() {
    _filtros.busca = '';
    const input = document.getElementById('vitrineBusca');
    if (input) { input.value = ''; input.focus(); }
    const btnX = document.getElementById('btnLimparBusca');
    if (btnX) btnX.style.display = 'none';
    aplicarFiltros();
}

/* ── Limpar todos os filtros ── */
function limparFiltros() {
    _filtros = { busca: '', estados: [], precoMin: null, precoMax: null, ordem: 'relevancia' };

    const busca = document.getElementById('vitrineBusca');
    if (busca) busca.value = '';
    const btnX = document.getElementById('btnLimparBusca');
    if (btnX) btnX.style.display = 'none';

    document.querySelectorAll('#filtroEstados input[type=checkbox]')
            .forEach(cb => { cb.checked = false; });
    const min = document.getElementById('filtroPrecoMin');
    const max = document.getElementById('filtroPrecoMax');
    if (min) min.value = '';
    if (max) max.value = '';
    const sel = document.getElementById('filtroOrdem');
    if (sel) sel.value = 'relevancia';

    aplicarFiltros();
}

/* ── Painel de filtros (abrir/fechar) ── */
let _painelAberto = false;

function toggleFiltros() {
    _painelAberto ? fecharPainelFiltros() : abrirPainelFiltros();
}

function abrirPainelFiltros() {
    _painelAberto = true;
    const painel = document.getElementById('painelFiltros');
    const arrow  = document.getElementById('btnFiltrosArrow');
    const btn    = document.getElementById('btnFiltros');
    if (painel) painel.classList.add('aberto');
    if (arrow)  arrow.textContent = '▴';
    if (btn)    btn.classList.add('ativo');
}

function fecharPainelFiltros() {
    _painelAberto = false;
    const painel = document.getElementById('painelFiltros');
    const arrow  = document.getElementById('btnFiltrosArrow');
    const btn    = document.getElementById('btnFiltros');
    if (painel) painel.classList.remove('aberto');
    if (arrow)  arrow.textContent = '▾';
    if (btn)    btn.classList.remove('ativo');
}

/* ── Carrega livros do servidor ── */
async function carregarLivros() {
    const grid = document.getElementById('gridLivros');
    if (!grid) return;

    grid.innerHTML = `<p style="grid-column:1/-1;text-align:center;color:#7A6E65;
        padding:3rem 0;font-family:'IM Fell English',serif;font-style:italic;">Carregando…</p>`;
    document.getElementById('vitrineContador').textContent = '';

    try {
        const url    = modoPromo ? '/api/livros/todos?emPromocao=true' : '/api/livros/todos';
        _todosLivros = await fetch(url).then(r => r.json());
        aplicarFiltros();
    } catch (err) {
        console.error('Erro ao carregar vitrine:', err);
        grid.innerHTML = `<p style="grid-column:1/-1;text-align:center;color:#722F37;
            padding:2.5rem 0;font-family:'IM Fell English',serif;font-style:italic">
            Erro ao carregar vitrine.</p>`;
    }
}

/* ── Init ── */
document.addEventListener('DOMContentLoaded', () => {
    carregarSaldo();
    carregarLivros();
});
