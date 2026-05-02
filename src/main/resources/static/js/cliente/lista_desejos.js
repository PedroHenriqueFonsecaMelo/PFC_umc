/* ================================================================
   lista_desejos.js — Lista de Desejos · Bibliotroca
   ================================================================ */

const token = localStorage.getItem('token');
const authHeaders = {
    'Content-Type': 'application/json',
    ...(token ? { 'Authorization': 'Bearer ' + token } : {})
};

let isbnsSalvos = new Set();
const metaCache = {};

function mostrarToast(msg, tipo) {
    const t = document.getElementById('toast');
    t.textContent = msg;
    t.className = 'show ' + tipo;
    setTimeout(() => { t.className = ''; }, 3200);
}

async function carregarPerfil() {
    try {
        const res = await fetch('/clientes/meu-perfil-json', { headers: authHeaders });
        if (res.ok) {
            const c = await res.json();
            const navSaldo = document.getElementById('navSaldo');
            if (navSaldo) navSaldo.textContent = 'T$ ' + (c.saldoTokens || 0).toFixed(2);
        }
    } catch (_) {}
}

function fmtData(iso) {
    if (!iso) return '';
    const d = new Date(iso);
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' });
}

function capaUrl(isbn) {
    if (!isbn) return null;
    const isbn13 = isbn.replace(/[^0-9X]/g, '');
    if (isbn13.length === 10 || isbn13.length === 13) {
        return 'https://covers.openlibrary.org/b/isbn/' + isbn13 + '-M.jpg';
    }
    return null;
}

async function buscarLivros() {
    const input = document.getElementById('inputBusca');
    const query = input.value.trim();
    if (!query) { input.focus(); return; }

    const btn = document.getElementById('btnBuscar');
    const container = document.getElementById('resultadosBusca');

    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Buscando...';
    container.innerHTML = '';

    try {
        const url = 'https://openlibrary.org/search.json?title=' + encodeURIComponent(query) + '&limit=10&fields=title,author_name,isbn,cover_i';
        const res = await fetch(url);
        if (!res.ok) throw new Error('Falha na API');
        const data = await res.json();

        const docs = (data.docs || []).filter(d => d.isbn && d.isbn.length > 0);

        if (docs.length === 0) {
            container.innerHTML = '<p class="search-hint">Nenhum resultado encontrado para "' + escHtml(query) + '".</p>';
            return;
        }

        container.innerHTML = docs.slice(0, 8).map(doc => buildResultadoItem(doc)).join('');

    } catch (e) {
        container.innerHTML = '<p class="search-error"><i class="fa-solid fa-triangle-exclamation"></i> Erro ao buscar. Verifique a conexão e tente novamente.</p>';
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-search"></i> Buscar';
    }
}

function buildResultadoItem(doc) {
    const titulo = doc.title || 'Sem título';
    const autores = doc.author_name ? doc.author_name.slice(0, 2).join(', ') : 'Autor desconhecido';

    const isbns = doc.isbn || [];
    const isbn13 = isbns.find(i => i.replace(/[^0-9]/g,'').length === 13);
    const isbn10 = isbns.find(i => i.replace(/[^0-9X]/gi,'').length === 10);
    const isbnEscolhido = isbn13 || isbn10 || isbns[0];

    if (!isbnEscolhido) return '';

    let imgHtml;
    if (doc.cover_i) {
        imgHtml = `<img class="resultado-capa"
            src="https://covers.openlibrary.org/b/id/${doc.cover_i}-M.jpg"
            alt="Capa" onerror="this.outerHTML='<div class=resultado-capa-placeholder>📚</div>'">`;
    } else {
        const capa = capaUrl(isbnEscolhido);
        imgHtml = capa
            ? `<img class="resultado-capa" src="${capa}" alt="Capa"
                 onerror="this.outerHTML='<div class=resultado-capa-placeholder>📚</div>'">`
            : `<div class="resultado-capa-placeholder">📚</div>`;
    }

    const jaAdicionado = isbnsSalvos.has(isbnEscolhido);
    const btnHtml = jaAdicionado
        ? `<span class="btn-ja-adicionado"><i class="fa-solid fa-check"></i> Salvo</span>`
        : `<button class="btn-adicionar" onclick="adicionarDesejo('${escHtml(isbnEscolhido)}', '${escHtml(titulo)}', '${escHtml(autores)}', this)">
             <i class="fa-solid fa-heart"></i> Salvar
           </button>`;

    return `
        <div class="resultado-item" id="res-${escHtml(isbnEscolhido)}">
            ${imgHtml}
            <div class="resultado-info">
                <div class="resultado-titulo">${escHtml(titulo)}</div>
                <div class="resultado-autor">${escHtml(autores)}</div>
                <div class="resultado-isbn">ISBN: <span>${escHtml(isbnEscolhido)}</span></div>
            </div>
            ${btnHtml}
        </div>`;
}

async function adicionarDesejo(isbn, titulo, autor, btn) {
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i>';

    try {
        const res = await fetch('/api/lista-desejos', {
            method: 'POST',
            headers: authHeaders,
            body: JSON.stringify({ isbn })
        });

        if (res.status === 401) { window.location.href = '/clientes/login'; return; }

        if (!res.ok) {
            const txt = await res.text();
            throw new Error(txt || 'Erro ao adicionar');
        }

        metaCache[isbn] = { titulo, autor };
        isbnsSalvos.add(isbn);
        btn.outerHTML = `<span class="btn-ja-adicionado"><i class="fa-solid fa-check"></i> Salvo</span>`;
        mostrarToast('Livro adicionado à lista de desejos!', 'sucesso');
        carregarListaSalva();

    } catch (e) {
        mostrarToast(e.message || 'Erro ao adicionar.', 'erro');
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-heart"></i> Salvar';
    }
}

async function removerDesejo(id, card) {
    card.style.opacity = '0.5';
    try {
        const res = await fetch('/api/lista-desejos/' + id, {
            method: 'DELETE',
            headers: authHeaders
        });
        if (res.status === 401) { window.location.href = '/clientes/login'; return; }
        if (!res.ok) throw new Error('Erro ao remover');

        card.remove();
        mostrarToast('Livro removido da lista de desejos.', 'sucesso');
        atualizarBadge();
        const isbnEl = card.querySelector('.desejo-isbn');
        if (isbnEl) isbnsSalvos.delete(isbnEl.textContent.trim());
        atualizarBotoesResultados();

    } catch (e) {
        mostrarToast('Erro ao remover. Tente novamente.', 'erro');
        card.style.opacity = '1';
    }
}

async function carregarListaSalva() {
    const container = document.getElementById('wishlistContainer');
    try {
        const res = await fetch('/api/lista-desejos', { headers: authHeaders });
        if (res.status === 401) { window.location.href = '/clientes/login'; return; }
        if (!res.ok) throw new Error('Falha na API');

        const lista = await res.json();

        isbnsSalvos.clear();
        lista.forEach(d => isbnsSalvos.add(d.isbn));

        if (lista.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <div class="icon">🔖</div>
                    <p>Sua lista de desejos está vazia. Busque livros acima e salve os que deseja adquirir.</p>
                </div>`;
            atualizarBadge(0);
            return;
        }

        container.innerHTML = `<div class="skeleton"></div><div class="skeleton"></div>`;
        const itensComMeta = await Promise.all(lista.map(d => enriquecerDesejo(d)));
        container.innerHTML = itensComMeta.map(buildDesejoCard).join('');
        atualizarBadge(lista.length);

    } catch (e) {
        container.innerHTML = `<p style="text-align:center;color:var(--accent);padding:2rem">Erro ao carregar a lista de desejos.</p>`;
    }
}

async function enriquecerDesejo(desejo) {
    if (metaCache[desejo.isbn]) {
        return { ...desejo, ...metaCache[desejo.isbn] };
    }
    try {
        const url = 'https://openlibrary.org/search.json?isbn=' + encodeURIComponent(desejo.isbn) + '&limit=1&fields=title,author_name';
        const res = await fetch(url);
        if (!res.ok) return desejo;
        const data = await res.json();
        const doc = data.docs && data.docs[0];
        if (doc) {
            const meta = {
                titulo: doc.title || null,
                autor: doc.author_name ? doc.author_name.slice(0, 2).join(', ') : null
            };
            metaCache[desejo.isbn] = meta;
            return { ...desejo, ...meta };
        }
    } catch (_) {}
    return desejo;
}

function buildDesejoCard(d) {
    const capa = capaUrl(d.isbn);
    const imgHtml = capa
        ? `<img class="desejo-capa" src="${capa}" alt="Capa"
             onerror="this.outerHTML='<div class=desejo-capa-placeholder>📚</div>'">`
        : `<div class="desejo-capa-placeholder">📚</div>`;

    const tituloHtml = d.titulo
        ? `<div class="desejo-titulo">${escHtml(d.titulo)}</div>`
        : `<div class="desejo-titulo" style="color:var(--muted);font-style:italic">Livro ISBN ${escHtml(d.isbn)}</div>`;
    const autorHtml = d.autor
        ? `<div class="desejo-autor">${escHtml(d.autor)}</div>`
        : '';

    return `
        <div class="desejo-card" id="desejo-${d.id}">
            ${imgHtml}
            <div class="desejo-info">
                ${tituloHtml}
                ${autorHtml}
                <div class="desejo-meta">
                    <span class="desejo-isbn">${escHtml(d.isbn)}</span>
                    <span class="desejo-data"><i class="fa-regular fa-calendar"></i> ${fmtData(d.dataAdicao)}</span>
                </div>
            </div>
            <button class="btn-remover" onclick="removerDesejo(${d.id}, document.getElementById('desejo-${d.id}'))">
                <i class="fa-solid fa-trash-can"></i> Remover
            </button>
        </div>`;
}

function atualizarBadge(n) {
    const badge = document.getElementById('badgeTotal');
    const total = n !== undefined ? n : document.querySelectorAll('.desejo-card').length;
    if (total > 0) {
        badge.textContent = total;
        badge.style.display = 'inline-block';
    } else {
        badge.style.display = 'none';
    }
}

function atualizarBotoesResultados() {
    document.querySelectorAll('.resultado-item').forEach(item => {
        const isbnEl = item.querySelector('.resultado-isbn span');
        if (!isbnEl) return;
        const isbn = isbnEl.textContent.trim();
        const btnWrap = item.querySelector('.btn-adicionar, .btn-ja-adicionado');
        if (!btnWrap) return;
        if (isbnsSalvos.has(isbn)) {
            btnWrap.outerHTML = `<span class="btn-ja-adicionado"><i class="fa-solid fa-check"></i> Salvo</span>`;
        }
    });
}

function escHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

document.getElementById('inputBusca').addEventListener('keydown', function(e) {
    if (e.key === 'Enter') buscarLivros();
});

carregarPerfil();
carregarListaSalva();
