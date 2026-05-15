/* ================================================================
   livro_detalhe.js — Página de detalhes do livro
   Carrega dados via GET /api/livros/{id} e gerencia a estante
   (localStorage) com "Adicionar à Estante" e "Comprar agora".
   ================================================================ */

const CART_KEY      = 'bibliotroca_carrinho';
const CHECKOUT_KEY  = 'bibliotroca_checkout_ids';

/* ── Helpers do carrinho ── */

function getCarrinho() {
    try { return JSON.parse(localStorage.getItem(CART_KEY)) || []; }
    catch (_) { return []; }
}

function saveCarrinho(itens) {
    localStorage.setItem(CART_KEY, JSON.stringify(itens));
}

function jaEstaNoCarrinho(id) {
    return getCarrinho().some(i => i.id === id);
}

function adicionarAoCarrinho(livro) {
    if (jaEstaNoCarrinho(livro.id)) return false;
    const carr = getCarrinho();
    carr.push(livro);
    saveCarrinho(carr);
    return true;
}

/* ── Extrai o ID do livro da URL (/livros/123) ── */
function extrairLivroId() {
    const partes = window.location.pathname.split('/').filter(Boolean);
    const id = parseInt(partes[partes.length - 1], 10);
    return isNaN(id) ? null : id;
}

/* ── Classe CSS por estado ── */
function classeEstado(estado) {
    const mapa = { 'ÓTIMO': 'otimo', 'OTIMO': 'otimo', 'BOM': 'bom',
                   'COMO_NOVO': 'otimo', 'REGULAR': 'regular', 'RUIM': 'ruim' };
    return mapa[(estado || '').toUpperCase()] || 'bom';
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

/* ── Toast ── */
function mostrarToast(msg, tipo) {
    const t = document.getElementById('toastDetalhe');
    if (!t) return;
    t.className = 'toast toast-' + (tipo || 'info');
    t.innerHTML = msg;
    t.style.display = 'block';
    setTimeout(() => { t.style.display = 'none'; }, 3000);
}

/* ── Contador regressivo de promoção ── */
let _timerInterval = null;

function iniciarCountdown(expiraISO) {
    const timerEl = document.getElementById('promoTimer');
    const bloco   = document.getElementById('promoCountdown');
    if (!timerEl || !bloco) return;

    bloco.style.display = 'block';

    const atualizar = () => {
        const diff = new Date(expiraISO).getTime() - Date.now();
        if (diff <= 0) {
            bloco.style.display = 'none';
            clearInterval(_timerInterval);
            return;
        }
        const h = Math.floor(diff / 3600000);
        const m = Math.floor((diff % 3600000) / 60000);
        const s = Math.floor((diff % 60000) / 1000);
        timerEl.textContent = `${h}h ${m}m ${s}s`;
    };

    atualizar();
    _timerInterval = setInterval(atualizar, 1000);
}

/* ── Galeria de fotos ── */
function renderGaleria(fotos) {
    const fotoMain   = document.getElementById('fotoMain');
    const thumbsCont = document.getElementById('galeriaThumbs');
    if (!fotoMain) return;

    const primeira = fotos.length > 0 ? fotos[0] : 'https://via.placeholder.com/400x480?text=📚';
    fotoMain.src = primeira;
    fotoMain.onerror = () => { fotoMain.src = 'https://via.placeholder.com/400x480?text=📚'; };

    if (fotos.length > 1 && thumbsCont) {
        thumbsCont.innerHTML = fotos.map((url, i) =>
            `<img src="${url}" class="galeria-thumb ${i === 0 ? 'ativa' : ''}"
                  alt="Foto ${i + 1}"
                  onclick="trocarFoto('${url}', this)"
                  onerror="this.style.display='none'">`
        ).join('');
    }
}

window.trocarFoto = function(url, thumb) {
    document.getElementById('fotoMain').src = url;
    document.querySelectorAll('.galeria-thumb').forEach(t => t.classList.remove('ativa'));
    thumb.classList.add('ativa');
};

/* ── Exibe seção "Sobre o livro" com truncamento opcional ── */
const RESUMO_MAX = 400;

function exibirResumo(texto) {
    const secaoEl = document.getElementById('livroResumo');
    const textoEl = document.getElementById('livroResumoTexto');
    const btnEl   = document.getElementById('btnLerMais');
    if (!secaoEl || !textoEl) return;

    const truncar = texto.length > RESUMO_MAX;
    textoEl.dataset.full  = texto;
    textoEl.dataset.short = truncar ? texto.slice(0, RESUMO_MAX) + '…' : texto;
    textoEl.textContent   = textoEl.dataset.short;
    textoEl.dataset.expandido = 'false';

    if (btnEl) btnEl.style.display = truncar ? 'inline-block' : 'none';
    secaoEl.style.display = 'block';
}

window.toggleLerMais = function () {
    const textoEl = document.getElementById('livroResumoTexto');
    const btnEl   = document.getElementById('btnLerMais');
    if (!textoEl || !btnEl) return;
    const expandido = textoEl.dataset.expandido === 'true';
    if (expandido) {
        textoEl.textContent       = textoEl.dataset.short;
        textoEl.dataset.expandido = 'false';
        btnEl.textContent         = 'Ler mais';
    } else {
        textoEl.textContent       = textoEl.dataset.full;
        textoEl.dataset.expandido = 'true';
        btnEl.textContent         = 'Ler menos';
    }
};

/* ── Busca sinopse na Google Books API como fallback ── */
async function buscarResumoGoogleBooks(isbn) {
    try {
        const res = await fetch(
            `https://www.googleapis.com/books/v1/volumes?q=isbn:${encodeURIComponent(isbn)}`
        );
        if (!res.ok) return null;
        const data = await res.json();
        const desc = data.items && data.items[0] &&
                     data.items[0].volumeInfo &&
                     data.items[0].volumeInfo.description;
        return desc || null;
    } catch (_) {
        return null;
    }
}

/* ── Renderiza os dados do livro na página ── */
function renderLivro(livro) {
    // Fotos
    let fotos = [];
    try {
        const arr = JSON.parse(livro.fotosUrls);
        if (Array.isArray(arr) && arr.length > 0) fotos = arr;
    } catch (_) {}
    renderGaleria(fotos);

    // Badge de promoção
    if (livro.emPromocao) {
        const badge = document.getElementById('badgePromo');
        if (badge) badge.style.display = 'inline-block';
    }

    // Títulos
    document.getElementById('livroTitulo').textContent = livro.titulo || '—';
    document.getElementById('livroAutor').textContent  = `por ${livro.autor || '—'}`;
    document.getElementById('livroIsbn').textContent   = `ISBN: ${livro.isbn || '—'}`;

    // Breadcrumb
    const bcr = document.getElementById('bcrTitulo');
    if (bcr) bcr.textContent = livro.titulo || '';

    // Título da aba
    document.title = `${livro.titulo || 'Livro'} — Bibliotroca`;

    // Badge de estado
    const estado = livro.estadoAprovado || 'BOM';
    const badgeEl = document.getElementById('badgeEstado');
    if (badgeEl) {
        badgeEl.textContent = estado.replace('_', ' ');
        badgeEl.className   = `livro-estado-badge estado-${classeEstado(estado)}`;
    }

    // Preço
    const precoDiv = document.getElementById('precoHtml');
    if (precoDiv) {
        if (livro.emPromocao && livro.precoOriginal) {
            precoDiv.innerHTML =
                `<span style="text-decoration:line-through;color:#9a8a80;font-size:1.3rem;
                              margin-right:.5rem;">T$ ${livro.precoOriginal.toFixed(2)}</span>` +
                `<span style="color:#e11d48;">T$ ${(livro.precoAprovado || 0).toFixed(2)}</span>`;
        } else {
            precoDiv.textContent = `T$ ${(livro.precoAprovado || 0).toFixed(2)}`;
        }
    }

    // Countdown
    if (livro.emPromocao && livro.promocaoExpira) {
        iniciarCountdown(livro.promocaoExpira);
    }

    // Resumo/descrição
    if (livro.resumoOficial && livro.resumoOficial.trim()) {
        exibirResumo(livro.resumoOficial.trim());
    } else if (livro.isbn) {
        buscarResumoGoogleBooks(livro.isbn).then(desc => {
            if (desc) exibirResumo(desc);
        });
    }

    // Estado dos botões: já na estante?
    atualizarBotaoEstante(livro.id);
}

/* ── Atualiza visual do botão (alterna entre Adicionar e Remover) ── */
function atualizarBotaoEstante(livroId) {
    const btn   = document.getElementById('btnEstante');
    const aviso = document.getElementById('avisoEstante');
    if (!btn) return;

    if (jaEstaNoCarrinho(livroId)) {
        // Livro está na estante → botão de remoção
        btn.innerHTML = '✕ Remover da Estante';
        btn.classList.add('remover');
        btn.classList.remove('na-estante');
        if (aviso) aviso.style.display = 'block';
    } else {
        // Livro não está na estante → botão de adição
        btn.innerHTML =
            '<img src="/estante.png" style="width:80px;height:80px;object-fit:contain;opacity:.9" alt="">' +
            ' Adicionar à Estante';
        btn.classList.remove('remover');
        btn.classList.remove('na-estante');
        if (aviso) aviso.style.display = 'none';
    }
}

/* ── Helper: atualiza o badge do ícone livro na navbar ── */
function atualizarBadgeNav() {
    if (typeof window.cnAtualizarBadgeEstante === 'function') {
        window.cnAtualizarBadgeEstante();
    }
}

/* ── Handler: Adicionar à Estante / Remover da Estante (toggle) ── */
window.handleAdicionarEstante = function() {
    if (!_livroAtual) return;

    if (jaEstaNoCarrinho(_livroAtual.id)) {
        // Remove do carrinho
        saveCarrinho(getCarrinho().filter(i => i.id !== _livroAtual.id));
        atualizarBotaoEstante(_livroAtual.id);
        atualizarBadgeNav();
        mostrarToast(`<strong>${_livroAtual.titulo}</strong> removido da estante.`, 'aviso');
    } else {
        // Adiciona ao carrinho
        adicionarAoCarrinho(_livroAtual);
        atualizarBotaoEstante(_livroAtual.id);
        atualizarBadgeNav();
        mostrarToast(
            `<img src="/imagens/estante.png" style="width:18px;height:18px;object-fit:contain;vertical-align:middle;margin-right:5px" alt="">` +
            ` <strong>${_livroAtual.titulo}</strong> adicionado à estante!`,
            'info'
        );
    }
};

/* ── Handler: Comprar agora ── */
window.handleComprarAgora = function() {
    if (!_livroAtual) return;

    // Compra direta: NÃO adiciona ao localStorage (estante/mini carrinho).
    // O objeto completo vai no sessionStorage para o checkout usar sem depender do localStorage.
    sessionStorage.setItem(CHECKOUT_KEY,           JSON.stringify([_livroAtual.id]));
    sessionStorage.setItem(CHECKOUT_KEY + '_direto', JSON.stringify([_livroAtual]));
    window.location.href = '/livros/checkout';
};

/* ── Carrega o livro da API ── */
let _livroAtual = null;

async function carregarLivro(id) {
    const loading    = document.getElementById('estadoCarregando');
    const notFound   = document.getElementById('estadoNaoEncontrado');
    const conteudo   = document.getElementById('conteudoLivro');

    try {
        const res = await fetch(`/api/livros/${id}`);

        if (res.status === 404) {
            if (loading)  loading.style.display  = 'none';
            if (notFound) notFound.style.display  = 'block';
            return;
        }

        if (!res.ok) throw new Error(`HTTP ${res.status}`);

        const livro = await res.json();
        _livroAtual = livro;

        renderLivro(livro);

        if (loading)  loading.style.display  = 'none';
        if (conteudo) conteudo.style.display  = 'grid';

    } catch (err) {
        console.error('Erro ao carregar livro:', err);
        if (loading)  loading.style.display  = 'none';
        if (notFound) notFound.style.display  = 'block';
    }
}

/* ── Init ── */
document.addEventListener('DOMContentLoaded', () => {
    carregarSaldo();

    const id = extrairLivroId();
    if (!id) {
        document.getElementById('estadoCarregando').style.display = 'none';
        document.getElementById('estadoNaoEncontrado').style.display = 'block';
        return;
    }

    carregarLivro(id);
});
