/* ================================================================
   vitrine_livros.js — Vitrine + Minha Estante
   Estante salva em localStorage, compra via /api/livros/{id}/comprar
   ================================================================ */

/* ── CARRINHO (localStorage) ──────────────────────────────────── */

const CARRINHO_KEY = 'bibliotroca_carrinho';

function getCarrinho() {
    try { return JSON.parse(localStorage.getItem(CARRINHO_KEY)) || []; }
    catch (_) { return []; }
}

function saveCarrinho(itens) {
    localStorage.setItem(CARRINHO_KEY, JSON.stringify(itens));
}

function adicionarAoCarrinho(livro) {
    const carrinho = getCarrinho();
    const jaExiste = carrinho.some(item => item.id === livro.id);
    if (jaExiste) return false;
    carrinho.push(livro);
    saveCarrinho(carrinho);
    return true;
}

function removerDoCarrinho(id) {
    saveCarrinho(getCarrinho().filter(item => item.id !== id));
    renderCarrinho();
    atualizarBotoes();
}

function totalCarrinho() {
    return getCarrinho().reduce((sum, item) => sum + (item.precoAprovado || 0), 0);
}

/* ── RENDER CARRINHO SIDEBAR ─────────────────────────────────── */

function renderCarrinho() {
    const carrinho = getCarrinho();
    const lista = document.getElementById('carrinhoLista');
    const totalEl = document.getElementById('carrinhoTotal');
    const contador = document.getElementById('carrinhoContador');
    const vazio = document.getElementById('carrinhoVazio');
    const acoes = document.getElementById('carrinhoAcoes');

    if (!lista) return;

    if (contador) {
        contador.textContent = carrinho.length;
        contador.style.display = carrinho.length > 0 ? 'flex' : 'none';
    }

    if (carrinho.length === 0) {
        lista.innerHTML = '';
        if (vazio) vazio.style.display = 'block';
        if (acoes) acoes.style.display = 'none';
        if (totalEl) totalEl.textContent = 'T$ 0,00';
        return;
    }

    if (vazio) vazio.style.display = 'none';
    if (acoes) acoes.style.display = 'flex';

    lista.innerHTML = carrinho.map(item => {
        let foto = 'https://via.placeholder.com/48x64?text=📚';
        try {
            const arr = JSON.parse(item.fotosUrls);
            if (Array.isArray(arr) && arr.length > 0) foto = arr[0];
        } catch (_) { }

        return `
        <div class="carr-item" id="carr-item-${item.id}">
            <img class="carr-item-img" src="${foto}" alt="${item.titulo}"
                 onerror="this.src='https://via.placeholder.com/48x64?text=📚'"/>
            <div class="carr-item-info">
                <div class="carr-item-titulo">${item.titulo}</div>
                <div class="carr-item-autor">${item.autor}</div>
                <div class="carr-item-preco">T$ ${(item.precoAprovado || 0).toFixed(2)}</div>
            </div>
            <button class="carr-item-remover" onclick="removerDoCarrinho(${item.id})" title="Remover">✕</button>
        </div>`;
    }).join('');

    if (totalEl) totalEl.textContent = `T$ ${totalCarrinho().toFixed(2)}`;
}

/* ── ATUALIZA BOTÕES DA VITRINE ──────────────────────────────── */

function atualizarBotoes() {
    const ids = new Set(getCarrinho().map(i => i.id));
    document.querySelectorAll('.btn-carrinho').forEach(btn => {
        const id = parseInt(btn.dataset.id);
        if (ids.has(id)) {
            btn.innerHTML = '✕ Remover da estante';
            btn.classList.add('no-carrinho');
            btn.disabled = false;
        } else {
            btn.innerHTML = '<img src="/imagens/estante.png" style="width:20px;height:20px;object-fit:contain"> Colocar na estante';
            btn.classList.remove('no-carrinho');
            btn.disabled = false;
        }
    });
}

/* ── SIDEBAR TOGGLE ──────────────────────────────────────────── */

function toggleCarrinho() {
    const sidebar = document.getElementById('carrinhoSidebar');
    const overlay = document.getElementById('carrinhoOverlay');
    const aberto = sidebar.classList.toggle('aberto');
    overlay.style.display = aberto ? 'block' : 'none';
    if (aberto) renderCarrinho();
}

function fecharCarrinho() {
    document.getElementById('carrinhoSidebar').classList.remove('aberto');
    document.getElementById('carrinhoOverlay').style.display = 'none';
}

/* ── FINALIZAR COMPRA ────────────────────────────────────────── */

async function finalizarCompra() {
    const carrinho = getCarrinho();
    if (carrinho.length === 0) return;

    const btn = document.getElementById('btnFinalizar');
    btn.disabled = true;
    btn.textContent = 'Processando...';

    try {
        const res = await fetch('/api/livros/carrinho/comprar', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ livroIds: carrinho.map(i => i.id) })
        });

        const data = await res.json();

        if (!res.ok) {
            const toast = document.getElementById('toastCompra');
            if (toast) {
                toast.className = 'toast toast-erro';
                toast.innerHTML = `❌ ${typeof data === 'string' ? data : (data.falhas?.[0]?.motivo || 'Erro ao processar a compra.')}`;
                toast.style.display = 'block';
                setTimeout(() => { toast.style.display = 'none'; }, 6000);
            }
            return;
        }

        // Remove do carrinho apenas os livros comprados com sucesso
        if (data.comprados && data.comprados.length > 0) {
            const idsComprados = new Set(data.comprados.map(c => c.livroId));
            saveCarrinho(getCarrinho().filter(i => !idsComprados.has(i.id)));
            renderCarrinho();
            atualizarBotoes();
            carregarSaldo();
        }

        mostrarToastResultado(data);

    } catch (err) {
        console.error('Erro ao finalizar compra:', err);
        const toast = document.getElementById('toastCompra');
        if (toast) {
            toast.className = 'toast toast-erro';
            toast.innerHTML = '❌ Erro de conexão. Tente novamente.';
            toast.style.display = 'block';
            setTimeout(() => { toast.style.display = 'none'; }, 6000);
        }
    } finally {
        btn.disabled = false;
        btn.textContent = 'Finalizar Compra';
    }
}

function limparCarrinho() {
    saveCarrinho([]);
    renderCarrinho();
    atualizarBotoes();
    const toast = document.getElementById('toastCompra');
    if (toast) {
        toast.className = 'toast toast-info';
        toast.innerHTML = 'Estante limpa.';
        toast.style.display = 'block';
        setTimeout(() => { toast.style.display = 'none'; }, 2500);
    }
}

function mostrarToastResultado(data) {
    const toast = document.getElementById('toastCompra');
    if (!toast) return;

    const ok = data.totalComprados || 0;
    const fail = (data.falhas || []).length;

    if (ok > 0 && fail === 0) {
        toast.className = 'toast toast-sucesso';
        toast.innerHTML = `✅ ${ok} livro(s) comprado(s)! Saldo restante: T$ ${data.saldoRestante.toFixed(2)}`;
    } else if (ok > 0 && fail > 0) {
        toast.className = 'toast toast-aviso';
        toast.innerHTML = `⚠️ ${ok} comprado(s), ${fail} falhou. Saldo: T$ ${data.saldoRestante.toFixed(2)}`;
    } else {
        const motivo = data.falhas?.[0]?.motivo || 'Falha desconhecida';
        toast.className = 'toast toast-erro';
        toast.innerHTML = `❌ ${motivo}`;
    }

    toast.style.display = 'block';
    setTimeout(() => { toast.style.display = 'none'; }, 6000);
}

/* ── SALDO ────────────────────────────────────────────────────── */

async function carregarSaldo() {
    try {
        const res = await fetch('/clientes/meu-perfil-json');
        if (res.ok) {
            const c = await res.json();
            const el = document.getElementById('saldoUsuario');
            if (el) el.innerHTML = `<i class="fa-solid fa-coins mr-1 text-yellow-400"></i> T$ ${c.saldoTokens.toFixed(2)}`;
            const navSaldo = document.getElementById('navSaldo');
            if (navSaldo) navSaldo.textContent = `T$ ${(c.saldoTokens || 0).toFixed(2)}`;
        }
    } catch (_) { }
}

/* ── PROMOÇÃO TOGGLE ─────────────────────────────────────────── */

let modoPromo = false;

function togglePromo() {
    modoPromo = !modoPromo;
    const btn = document.getElementById('btnPromo');
    const titulo = document.getElementById('vitrineTitulo');
    if (modoPromo) {
        btn.style.background = '#722f37';
        btn.style.color = '#fff';
        btn.textContent = '✕ Ver Todos';
        if (titulo) titulo.textContent = 'Promoções';
    } else {
        btn.style.background = '#fff';
        btn.style.color = '#722f37';
        btn.textContent = '🏷 Ver Promoções';
        if (titulo) titulo.textContent = 'Livros Disponíveis';
    }
    carregarLivros();
}

/* ── CARREGAR LIVROS ─────────────────────────────────────────── */

async function carregarLivros() {
    const grid = document.getElementById('gridLivros');
    try {
        const url = modoPromo ? '/api/livros/todos?emPromocao=true' : '/api/livros/todos';
        const livros = await fetch(url).then(r => r.json());

        if (!livros.length) {
            const msg = modoPromo
                ? 'Nenhum livro em promoção no momento.'
                : 'Nenhum livro disponível ainda.';
            grid.innerHTML = `<p style="grid-column:1/-1;text-align:center;color:#7A6E65;padding:2.5rem 0;font-family:'IM Fell English',serif;font-style:italic">${msg}</p>`;
            return;
        }

        grid.innerHTML = livros.map(livro => {
            let foto = 'https://via.placeholder.com/300x400?text=Sem+Foto';
            try {
                const arr = JSON.parse(livro.fotosUrls);
                if (Array.isArray(arr) && arr.length > 0) foto = arr[0];
            } catch (_) { }

            const livroJson = JSON.stringify(livro).replace(/'/g, "\\'").replace(/"/g, '&quot;');

            // Badge de promoção
            const badgePromo = livro.emPromocao
                ? `<span style="position:absolute;top:.5rem;left:.5rem;background:#e11d48;color:#fff;
                               font-size:.7rem;font-weight:700;padding:.2rem .55rem;border-radius:20px;
                               text-transform:uppercase;letter-spacing:.04em;">PROMOÇÃO</span>`
                : '';

            // Preço: se emPromocao e precoOriginal, exibir original riscado
            let precoHtml;
            if (livro.emPromocao && livro.precoOriginal) {
                precoHtml = `<div class="livro-preco">
                    <span style="text-decoration:line-through;color:#9a8a80;font-size:.85rem;margin-right:.4rem;">T$ ${(livro.precoOriginal).toFixed(2)}</span>
                    <span style="color:#e11d48;font-weight:700;">T$ ${(livro.precoAprovado || 0).toFixed(2)}</span>
                </div>`;
            } else {
                precoHtml = `<div class="livro-preco">T$ ${(livro.precoAprovado || 0).toFixed(2)}</div>`;
            }

            return `
            <div class="livro-card" style="position:relative;">
                ${badgePromo}
                <div class="livro-card-img">
                    <img src="${foto}"
                         onerror="this.src='https://via.placeholder.com/300x400?text=📚'">
                </div>
                <div class="livro-card-body">
                    <span class="livro-estado">${livro.estadoAprovado || 'BOM'}</span>
                    <h3 class="livro-titulo">${livro.titulo}</h3>
                    <p class="livro-autor">por ${livro.autor}</p>
                    ${precoHtml}
                    <button
                        class="btn-carrinho"
                        data-id="${livro.id}"
                        onclick="handleAdicionarCarrinho(this, ${livroJson})">
                        <img src="/imagens/estante.png" style="width:20px;height:20px;object-fit:contain"> Colocar na estante
                    </button>
                    <p class="livro-rodape">Bibliotroca</p>
                </div>
            </div>`;
        }).join('');

        atualizarBotoes();

    } catch (err) {
        console.error(err);
        grid.innerHTML = `<p style="grid-column:1/-1;text-align:center;color:#722F37;padding:2.5rem 0;font-family:'IM Fell English',serif;font-style:italic">Erro ao carregar vitrine.</p>`;
    }
}

/* ── HANDLER BOTÃO ───────────────────────────────────────────── */

function handleAdicionarCarrinho(btn, livro) {
    const jaExiste = getCarrinho().some(item => item.id === livro.id);

    if (jaExiste) {
        removerDoCarrinho(livro.id);
        return;
    }

    adicionarAoCarrinho(livro);

    btn.innerHTML = '✕ Remover da estante';
    btn.classList.add('no-carrinho');
    btn.classList.add('btn-bounce');
    setTimeout(() => btn.classList.remove('btn-bounce'), 400);

    renderCarrinho();

    const sidebar = document.getElementById('carrinhoSidebar');
    const overlay = document.getElementById('carrinhoOverlay');
    if (sidebar && !sidebar.classList.contains('aberto')) {
        sidebar.classList.add('aberto');
        overlay.style.display = 'block';
    }

    const toast = document.getElementById('toastCompra');
    if (toast) {
        toast.className = 'toast toast-info';
        toast.innerHTML = `<img src="/imagens/estante.png" style="width:20px;height:20px;object-fit:contain;vertical-align:middle;margin-right:5px"> <strong>${livro.titulo}</strong> adicionado à estante`;
        toast.style.display = 'block';
        setTimeout(() => { toast.style.display = 'none'; }, 2500);
    }
}

/* ── INIT ─────────────────────────────────────────────────────── */

document.addEventListener('DOMContentLoaded', () => {
    carregarSaldo();
    carregarLivros();
    renderCarrinho();
    document.getElementById('carrinhoOverlay')?.addEventListener('click', fecharCarrinho);
});
