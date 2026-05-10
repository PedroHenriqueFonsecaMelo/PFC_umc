/* ================================================================
   estante.js — Página da Estante (/livros/estante)
   Lê livros do localStorage, permite selecionar e prosseguir
   para o checkout com os IDs selecionados via sessionStorage.
   ================================================================ */

const CART_KEY     = 'bibliotroca_carrinho';
const CHECKOUT_KEY = 'bibliotroca_checkout_ids';

/* ── Helpers do carrinho ── */

function getCarrinho() {
    try { return JSON.parse(localStorage.getItem(CART_KEY)) || []; }
    catch (_) { return []; }
}

function saveCarrinho(itens) {
    localStorage.setItem(CART_KEY, JSON.stringify(itens));
}

/* ── Saldo na navbar ── */
function carregarSaldo() {
    fetch('/clientes/meu-perfil-json', { credentials: 'include' })
        .then(r => r.ok ? r.json() : null)
        .then(c => {
            if (!c) return;
            const el = document.getElementById('navSaldo');
            if (el) el.textContent = `T$ ${(c.saldoTokens || 0).toFixed(2)}`;
        }).catch(() => {});
}

/* ── Classe CSS por estado ── */
function classeEstado(estado) {
    const mapa = { 'ÓTIMO': 'otimo', 'OTIMO': 'otimo', 'BOM': 'bom',
                   'COMO_NOVO': 'otimo', 'REGULAR': 'regular', 'RUIM': 'ruim' };
    return mapa[(estado || '').toUpperCase()] || 'bom';
}

/* ── Renderiza a lista de livros ── */
function renderEstante() {
    const itens    = getCarrinho();
    const vazia    = document.getElementById('estanteVazia');
    const conteudo = document.getElementById('estanteConteudo');
    const lista    = document.getElementById('listaEstante');

    if (!lista) return;

    if (itens.length === 0) {
        if (vazia)    vazia.style.display    = 'block';
        if (conteudo) conteudo.style.display = 'none';
        return;
    }

    if (vazia)    vazia.style.display    = 'none';
    if (conteudo) conteudo.style.display = 'block';

    lista.innerHTML = itens.map(item => {
        let foto = 'https://via.placeholder.com/54x72?text=📚';
        try {
            const arr = JSON.parse(item.fotosUrls);
            if (Array.isArray(arr) && arr.length > 0) foto = arr[0];
        } catch (_) {}

        const estado    = item.estadoAprovado || 'BOM';
        const preco     = (item.precoAprovado || 0).toFixed(2);
        const estadoLabel = estado.replace('_', ' ');

        return `
        <div class="estante-item" id="item-${item.id}">
            <input type="checkbox" class="estante-item-check" id="chk-${item.id}"
                   data-id="${item.id}" data-preco="${item.precoAprovado || 0}"
                   onchange="atualizarSubtotal()">
            <img class="estante-item-img" src="${foto}" alt="${escHtml(item.titulo)}"
                 onerror="this.src='https://via.placeholder.com/54x72?text=📚'">
            <div class="estante-item-info">
                <div class="estante-item-titulo">${escHtml(item.titulo)}</div>
                <div class="estante-item-autor">${escHtml(item.autor || '')}</div>
                <span class="estante-item-estado estado-${classeEstado(estado)}">${estadoLabel}</span>
            </div>
            <span class="estante-item-preco">T$ ${preco}</span>
            <button class="estante-item-remover" onclick="removerItem(${item.id})"
                    title="Remover da estante">✕</button>
        </div>`;
    }).join('');

    atualizarSubtotal();
}

/* ── Escapa HTML para evitar XSS ── */
function escHtml(str) {
    return String(str || '')
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/* ── Atualiza subtotal e estado dos botões ── */
function atualizarSubtotal() {
    const checks      = document.querySelectorAll('.estante-item-check:checked');
    const total       = Array.from(checks).reduce((s, c) => s + parseFloat(c.dataset.preco || 0), 0);
    const qtd         = checks.length;

    const subtotalEl  = document.getElementById('subtotalValor');
    const qtdEl       = document.getElementById('qtdSelecionados');
    const btnPross    = document.getElementById('btnProsseguir');
    const btnRemover  = document.getElementById('btnRemoverSelecionados');
    const chkTodos    = document.getElementById('chkSelecionarTodos');

    if (subtotalEl) subtotalEl.textContent = `T$ ${total.toFixed(2)}`;
    if (qtdEl)      qtdEl.textContent      = qtd;
    if (btnPross)   btnPross.disabled      = qtd === 0;
    if (btnRemover) btnRemover.disabled    = qtd === 0;

    // Atualiza estado do "Selecionar todos"
    if (chkTodos) {
        const totalItens = document.querySelectorAll('.estante-item-check').length;
        chkTodos.checked      = totalItens > 0 && qtd === totalItens;
        chkTodos.indeterminate = qtd > 0 && qtd < totalItens;
    }

    // Destaca visualmente os itens selecionados
    document.querySelectorAll('.estante-item-check').forEach(c => {
        const item = document.getElementById('item-' + c.dataset.id);
        if (item) item.classList.toggle('selecionado', c.checked);
    });
}

/* ── Selecionar / desselecionar todos ── */
window.toggleSelecionarTodos = function(chk) {
    document.querySelectorAll('.estante-item-check').forEach(c => { c.checked = chk.checked; });
    atualizarSubtotal();
};

/* ── Remover item individual ── */
window.removerItem = function(id) {
    saveCarrinho(getCarrinho().filter(i => i.id !== id));
    renderEstante();
};

/* ── Remover itens selecionados ── */
window.removerSelecionados = function() {
    const selecionados = new Set(
        Array.from(document.querySelectorAll('.estante-item-check:checked'))
             .map(c => parseInt(c.dataset.id, 10))
    );
    saveCarrinho(getCarrinho().filter(i => !selecionados.has(i.id)));
    renderEstante();
};

/* ── Prosseguir para o checkout ── */
window.prosseguirCheckout = function() {
    const ids = Array.from(document.querySelectorAll('.estante-item-check:checked'))
                     .map(c => parseInt(c.dataset.id, 10));

    if (ids.length === 0) return;

    // Persiste os IDs selecionados para a página de checkout
    sessionStorage.setItem(CHECKOUT_KEY, JSON.stringify(ids));
    window.location.href = '/livros/checkout';
};

/* ── Init ── */
document.addEventListener('DOMContentLoaded', () => {
    carregarSaldo();
    renderEstante();
});
