/* ================================================================
   checkout.js — Página de Checkout (/livros/checkout)
   Lê IDs selecionados do sessionStorage, busca dados no localStorage,
   exibe resumo, valida saldo via backend e confirma a compra via
   POST /api/livros/carrinho/comprar (transação atômica no servidor).
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

/* ── Dados do checkout (preenchidos no init) ── */
let _itensSelecionados = [];
let _saldoAtual        = null;
// true  → veio da estante (localStorage); false → compra direta (sessionStorage)
let _compraViaEstante  = true;

/* ── Perfil completo (saldo + endereços) ── */
async function carregarPerfil() {
    try {
        const res = await fetch('/clientes/meu-perfil-json', { credentials: 'include' });
        if (res.status === 401) { window.location.href = '/clientes/login'; return null; }
        if (!res.ok) return null;
        const c = await res.json();
        _saldoAtual = c.saldoTokens || 0;
        const navEl = document.getElementById('navSaldo');
        if (navEl) navEl.textContent = `T$ ${_saldoAtual.toFixed(2)}`;
        return c;
    } catch (_) { return null; }
}

/* ── Saldo do usuário ── */
async function carregarSaldo() {
    const c = await carregarPerfil();
    return c ? (c.saldoTokens || 0) : null;
}

/* ── Exibe / valida endereço de entrega ── */
function carregarEnderecoEntrega(perfil) {
    const card     = document.getElementById('enderecoEntregaCard');
    const alertaEl = document.getElementById('alertaEndereco');
    const btn      = document.getElementById('btnConfirmar');

    if (!perfil || !perfil.enderecos || perfil.enderecos.length === 0) {
        if (card) card.innerHTML = '';
        if (alertaEl) {
            alertaEl.innerHTML = '⚠ Você precisa <a href="/clientes/homepage?aba=enderecos" style="color:var(--accent,#722f37);font-weight:600;text-decoration:underline;">cadastrar um endereço de entrega</a> antes de concluir a compra.';
            alertaEl.style.display = 'block';
        }
        if (btn) btn.disabled = true;
        return;
    }

    const endereco = perfil.enderecos.find(e => e.id === perfil.enderecoSelecionadoId)
                  || perfil.enderecos[0];

    if (card) {
        card.innerHTML =
            `<strong>${escHtml(endereco.rua)}, ${escHtml(endereco.numero)}</strong>` +
            (endereco.complemento ? `<br>${escHtml(endereco.complemento)}` : '') +
            `<br>${escHtml(endereco.bairro)} — ${escHtml(endereco.cidade)}/${escHtml(endereco.estado)}` +
            `<br>CEP: ${escHtml(endereco.cep)}` +
            `<br><a href="/clientes/homepage?aba=enderecos" style="font-size:.8rem;color:var(--accent,#722f37);">Alterar endereço →</a>`;
    }
    if (alertaEl) alertaEl.style.display = 'none';
}

/* ── Escapa HTML ── */
function escHtml(str) {
    return String(str || '')
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;');
}

/* ── Renderiza o resumo dos itens ── */
function renderItens(itens) {
    const lista = document.getElementById('listaCheckout');
    if (!lista) return;

    lista.innerHTML = itens.map(item => {
        let foto = 'https://via.placeholder.com/50x66?text=📚';
        try {
            const arr = JSON.parse(item.fotosUrls);
            if (Array.isArray(arr) && arr.length > 0) foto = arr[0];
        } catch (_) {}

        return `
        <div class="checkout-item">
            <img class="checkout-item-img" src="${foto}" alt="${escHtml(item.titulo)}"
                 onerror="this.src='https://via.placeholder.com/50x66?text=📚'">
            <div class="checkout-item-info">
                <div class="checkout-item-titulo">${escHtml(item.titulo)}</div>
                <div class="checkout-item-autor">${escHtml(item.autor || '')}</div>
            </div>
            <span class="checkout-item-preco">T$ ${(item.precoAprovado || 0).toFixed(2)}</span>
        </div>`;
    }).join('');
}

/* ── Atualiza os valores de saldo / total / saldo pós ── */
function renderFinanceiro(saldo, total) {
    const saldoEl  = document.getElementById('saldoAtual');
    const totalEl  = document.getElementById('totalDebitar');
    const aposEl   = document.getElementById('saldoApos');
    const saldoPos = saldo - total;

    if (saldoEl) saldoEl.textContent = `T$ ${saldo.toFixed(2)}`;
    if (totalEl) totalEl.textContent = `T$ ${total.toFixed(2)}`;

    if (aposEl) {
        aposEl.textContent = `T$ ${Math.max(0, saldoPos).toFixed(2)}`;
        aposEl.classList.toggle('negativo', saldoPos < 0);
    }

    // Alerta de saldo insuficiente (pré-validação local, antes do click)
    const alerta = document.getElementById('alertaSaldo');
    const btn    = document.getElementById('btnConfirmar');
    if (saldoPos < 0) {
        const falta = Math.abs(saldoPos).toFixed(2);
        const msg   = document.getElementById('alertaSaldoMsg');
        if (msg) msg.textContent = ` Você precisa de mais T$ ${falta} para concluir esta compra.`;
        if (alerta) alerta.style.display = 'block';
        if (btn)    btn.disabled         = true;
    } else {
        if (alerta) alerta.style.display = 'none';
        if (btn)    btn.disabled         = false;
    }
}

/* ── Toast ── */
function mostrarToast(msg, tipo) {
    const t = document.getElementById('toastCheckout');
    if (!t) return;
    t.className = 'toast toast-' + (tipo || 'info');
    t.innerHTML = msg;
    t.style.display = 'block';
    setTimeout(() => { t.style.display = 'none'; }, 5000);
}

/* ── Confirmar compra ── */
window.confirmarCompra = async function() {
    if (_itensSelecionados.length === 0) return;

    const btn = document.getElementById('btnConfirmar');
    btn.disabled    = true;
    btn.textContent = 'Processando…';

    // Esconde alertas anteriores
    document.getElementById('alertaErro').style.display  = 'none';
    document.getElementById('alertaSaldo').style.display = 'none';

    const livroIds = _itensSelecionados.map(i => i.id);

    try {
        const res = await fetch('/api/livros/carrinho/comprar', {
            method:  'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ livroIds })
        });

        if (res.status === 401) {
            window.location.href = '/clientes/login';
            return;
        }

        const data = await res.json();

        if (!res.ok) {
            // Erro retornado pelo backend (saldo insuficiente, livro indisponível, etc.)
            const msg = typeof data === 'string' ? data : (data.falhas?.[0]?.motivo || 'Erro ao processar a compra.');
            const alertaErro = document.getElementById('alertaErro');
            const alertaMsg  = document.getElementById('alertaErroMsg');

            // Saldo insuficiente → alerta dedicado
            if (msg.toLowerCase().includes('saldo')) {
                const alertaSaldo = document.getElementById('alertaSaldo');
                const alertaSaldoMsg = document.getElementById('alertaSaldoMsg');
                if (alertaSaldoMsg) alertaSaldoMsg.textContent = ' ' + msg;
                if (alertaSaldo)   alertaSaldo.style.display = 'block';
            } else {
                if (alertaMsg)  alertaMsg.textContent      = msg;
                if (alertaErro) alertaErro.style.display   = 'block';
            }
            btn.disabled    = false;
            btn.textContent = 'Confirmar compra';
            return;
        }

        // ── Sucesso ──
        // Monta o set de IDs comprados (usa resposta da API; fallback para os itens enviados)
        const idsComprados = new Set(
            (data.comprados && data.comprados.length > 0)
                ? data.comprados.map(c => c.livroId)
                : _itensSelecionados.map(i => i.id)
        );

        // Remove os livros comprados do localStorage em qualquer fluxo de compra
        saveCarrinho(getCarrinho().filter(i => !idsComprados.has(i.id)));

        // Limpa sessão de checkout
        sessionStorage.removeItem(CHECKOUT_KEY);
        sessionStorage.removeItem(CHECKOUT_KEY + '_direto');

        // Monta dados completos para a página de confirmação
        const agora      = new Date();
        const dataCompra = agora.toLocaleDateString('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' })
                         + ' às ' + agora.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });

        const confirmacaoData = {
            comprados: (data.comprados || []).map(c => {
                const itemLocal = _itensSelecionados.find(i => i.id === c.livroId) || {};
                let foto = null;
                try {
                    const arr = JSON.parse(itemLocal.fotosUrls);
                    if (Array.isArray(arr) && arr.length > 0) foto = arr[0];
                } catch (_) {}
                return {
                    pedidoId: c.pedidoId,
                    livroId:  c.livroId,
                    titulo:   c.titulo || itemLocal.titulo || '',
                    autor:    itemLocal.autor || '',
                    preco:    c.preco,
                    foto
                };
            }),
            totalGasto:    data.totalGasto,
            saldoRestante: data.saldoRestante,
            dataCompra
        };
        sessionStorage.setItem('bibliotroca_confirmacao', JSON.stringify(confirmacaoData));

        window.location.href = '/livros/pedido-confirmado';

    } catch (err) {
        console.error('Erro ao confirmar compra:', err);
        const alertaErro = document.getElementById('alertaErro');
        const alertaMsg  = document.getElementById('alertaErroMsg');
        if (alertaMsg)  alertaMsg.textContent    = 'Erro de conexão. Verifique sua internet e tente novamente.';
        if (alertaErro) alertaErro.style.display = 'block';
        btn.disabled    = false;
        btn.textContent = 'Confirmar compra';
    }
};

/* ── Init ── */
document.addEventListener('DOMContentLoaded', async () => {

    // 1. Lê IDs selecionados no sessionStorage
    let ids = [];
    try {
        ids = JSON.parse(sessionStorage.getItem(CHECKOUT_KEY)) || [];
    } catch (_) {}

    const vazio  = document.getElementById('estadoVazio');
    const layout = document.getElementById('checkoutLayout');

    if (ids.length === 0) {
        // Nenhum item selecionado — redireciona para a estante
        if (vazio)  vazio.style.display  = 'block';
        if (layout) layout.style.display = 'none';
        setTimeout(() => { window.location.href = '/livros/estante'; }, 2000);
        return;
    }

    // 2. Detecta fluxo: compra direta (sessionStorage) ou via estante (localStorage)
    const idsSet = new Set(ids);
    let dadosDireto = [];
    try {
        dadosDireto = JSON.parse(sessionStorage.getItem(CHECKOUT_KEY + '_direto')) || [];
    } catch (_) {}

    if (dadosDireto.length > 0) {
        // Compra direta — dados vêm do sessionStorage; localStorage não é tocado
        _compraViaEstante  = false;
        _itensSelecionados = dadosDireto.filter(i => idsSet.has(i.id));
    } else {
        // Via estante — dados vêm do localStorage
        _compraViaEstante  = true;
        _itensSelecionados = getCarrinho().filter(i => idsSet.has(i.id));
    }

    if (_itensSelecionados.length === 0) {
        if (vazio)  vazio.style.display  = 'block';
        if (layout) layout.style.display = 'none';
        return;
    }

    // 3. Exibe o layout
    if (vazio)  vazio.style.display  = 'none';
    if (layout) layout.style.display = 'grid';

    // 4. Renderiza itens
    renderItens(_itensSelecionados);

    // 5. Carrega perfil, valida endereço e calcula financeiro
    const perfil = await carregarPerfil();
    carregarEnderecoEntrega(perfil);
    const total = _itensSelecionados.reduce((s, i) => s + (i.precoAprovado || 0), 0);
    renderFinanceiro(perfil ? (perfil.saldoTokens || 0) : 0, total);
});
