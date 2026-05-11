/* ================================================================
   vitrine_livros.js — Vitrine de livros · Bibliotroca
   Cards navegam para /livros/{id} ao serem clicados.
   O gerenciamento da estante ocorre em livro_detalhe.js e estante.js.
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
                // Promoção expirou — oculta badge e restaura preço original
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

    atualizar(); // executa imediatamente
    contadorInterval = setInterval(atualizar, 1000);
}

/* ── Toggle: somente promoções ── */
let modoPromo = false;

function togglePromo() {
    modoPromo = !modoPromo;
    const btn    = document.getElementById('btnPromo');
    const titulo = document.getElementById('vitrineTitulo');

    if (modoPromo) {
        btn.style.background = '#722f37';
        btn.style.color      = '#fff';
        btn.textContent      = '✕ Ver Todos';
        if (titulo) titulo.textContent = 'Promoções';
    } else {
        btn.style.background = '#fff';
        btn.style.color      = '#722f37';
        btn.textContent      = '🏷 Ver Promoções';
        if (titulo) titulo.textContent = 'Livros Disponíveis';
    }
    carregarLivros();
}

/* ── Classe CSS por estado ── */
function classeEstado(estado) {
    const mapa = {
        'ÓTIMO': 'otimo', 'OTIMO': 'otimo',
        'BOM':   'bom',   'COMO_NOVO': 'otimo',
        'REGULAR': 'regular', 'RUIM': 'ruim'
    };
    return mapa[(estado || '').toUpperCase()] || 'bom';
}

/* ── Carrega e renderiza os livros da vitrine ── */
async function carregarLivros() {
    const grid = document.getElementById('gridLivros');
    if (!grid) return;

    grid.innerHTML = '<p style="grid-column:1/-1;text-align:center;color:#7A6E65;' +
        'padding:3rem 0;font-family:\'IM Fell English\',serif;font-style:italic;">Carregando…</p>';

    try {
        const url    = modoPromo ? '/api/livros/todos?emPromocao=true' : '/api/livros/todos';
        const livros = await fetch(url).then(r => r.json());

        if (!livros.length) {
            const msg = modoPromo
                ? 'Nenhum livro em promoção no momento.'
                : 'Nenhum livro disponível ainda.';
            grid.innerHTML = `<p style="grid-column:1/-1;text-align:center;color:#7A6E65;` +
                `padding:2.5rem 0;font-family:'IM Fell English',serif;font-style:italic">${msg}</p>`;
            return;
        }

        grid.innerHTML = livros.map(livro => {
            // Extrai primeira foto
            let foto = 'https://via.placeholder.com/300x400?text=Sem+Foto';
            try {
                const arr = JSON.parse(livro.fotosUrls);
                if (Array.isArray(arr) && arr.length > 0) foto = arr[0];
            } catch (_) {}

            // Badge de promoção
            const badgePromo = livro.emPromocao
                ? `<span id="badge-${livro.id}" style="position:absolute;top:.5rem;left:.5rem;
                       background:#e11d48;color:#fff;font-size:.7rem;font-weight:700;
                       padding:.2rem .55rem;text-transform:uppercase;letter-spacing:.04em;">PROMOÇÃO</span>`
                : '';

            // Bloco de preço (com ou sem promoção)
            let precoHtml;
            if (livro.emPromocao && livro.precoOriginal) {
                precoHtml = `<div class="livro-preco" id="preco-${livro.id}">
                    <span style="text-decoration:line-through;color:#9a8a80;
                                 font-size:.85rem;margin-right:.4rem;">
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

            // Contador regressivo
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

            // Card inteiro é um link para /livros/{id}
            return `
            <a href="/livros/${livro.id}" class="livro-card" style="position:relative;text-decoration:none;color:inherit;"
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

    } catch (err) {
        console.error('Erro ao carregar vitrine:', err);
        grid.innerHTML = `<p style="grid-column:1/-1;text-align:center;color:#722F37;` +
            `padding:2.5rem 0;font-family:'IM Fell English',serif;font-style:italic">
            Erro ao carregar vitrine.</p>`;
    }
}

/* ── Init ── */
document.addEventListener('DOMContentLoaded', () => {
    carregarSaldo();
    carregarLivros();
});
