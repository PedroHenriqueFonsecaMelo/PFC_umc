/* ================================================================
   estoque.js — Gestão de Estoque · Admin Bibliotroca
   ================================================================ */

let todosLivros = [];
let modoEdicao  = false;

/* ── DATA NO TOPBAR ── */
document.getElementById("dataHoje").textContent =
    new Date().toLocaleDateString("pt-BR", { weekday: "long", day: "2-digit", month: "long", year: "numeric" });

/* ── UTILS ── */
function promoValida(promocaoExpira) {
    if (!promocaoExpira) return true; // sem expiração = válida indefinidamente
    return new Date(promocaoExpira).getTime() > Date.now();
}

function primeiraFoto(fotosUrls) {
    try {
        const arr = JSON.parse(fotosUrls);
        if (Array.isArray(arr) && arr.length > 0) return arr[0];
    } catch (_) {}
    return null;
}

const ESTADO_LABEL = {
    NOVO: "Novo", OTIMO: "Ótimo", BOM: "Bom", DESGASTADO: "Desgastado", RUIM: "Ruim"
};

/* ── CARREGAR LIVROS ── */
async function carregarEstoque() {
    try {
        const res = await fetch("/api/admin/livros/aprovados");
        if (!res.ok) throw new Error();
        todosLivros = await res.json();
        renderGrid(todosLivros);
    } catch (_) {
        document.getElementById("estoqueGrid").innerHTML =
            `<p style="color:#722f37;grid-column:1/-1;padding:2rem;text-align:center">Erro ao carregar estoque.</p>`;
    }
}

function renderGrid(livros) {
    const grid   = document.getElementById("estoqueGrid");
    const vazio  = document.getElementById("estoqueVazio");
    const cont   = document.getElementById("contadorLivros");

    cont.textContent = `${livros.length} livro${livros.length !== 1 ? "s" : ""}`;

    if (livros.length === 0) {
        grid.innerHTML = "";
        vazio.style.display = "block";
        return;
    }
    vazio.style.display = "none";

    grid.innerHTML = livros.map(l => {

        const foto  = l.fotoUrl || primeiraFoto(l.fotosUrls) || "";
        const imgSrc = foto || "https://via.placeholder.com/300x180?text=📚";
        const estado = l.estadoAprovado || "BOM";

        const promoAtiva = l.emPromocao && l.precoOriginal != null && promoValida(l.promocaoExpira);

        let precoHtml;
        if (promoAtiva) {
            const desconto = Math.round((1 - l.precoAprovado / l.precoOriginal) * 100);
            precoHtml = `
                <div class="card-preco" id="adm-preco-${l.id}">
                    <span class="preco-original">T$ ${Number(l.precoOriginal).toFixed(2)}</span>
                    <span class="preco-promo">T$ ${Number(l.precoAprovado).toFixed(2)}</span>
                </div>`;
        } else {
            const preco = l.precoAprovado != null ? `T$ ${Number(l.precoAprovado).toFixed(2)}` : "—";
            precoHtml = `<div class="card-preco" id="adm-preco-${l.id}">${preco}</div>`;
        }

        const promoBadge = promoAtiva
            ? `<span class="card-promo-badge" id="adm-badge-${l.id}">${Math.round((1 - l.precoAprovado / l.precoOriginal) * 100)}% OFF</span>`
            : "";

        const countdownHtml = (promoAtiva && l.promocaoExpira)
            ? `<div class="promo-countdown"
                    data-livro-id="${l.id}"
                    data-expira="${l.promocaoExpira}"
                    data-preco-original="${l.precoOriginal}"
                    style="display:flex;align-items:center;gap:.35rem;
                           margin-top:.4rem;padding:.25rem .5rem;
                           background:#fff0f3;border:1.5px solid #e11d48;
                           border-radius:8px;line-height:1.3;">
                   <span style="font-size:.9rem;">🔥</span>
                   <span style="font-size:.7rem;color:#e11d48;font-weight:700;">
                       Expira em: <span id="adm-timer-${l.id}" style="font-weight:800;">...</span>
                   </span>
               </div>`
            : "";

        return `
        <div class="livro-card-admin" id="adm-card-${l.id}">
            <img class="card-img" src="${imgSrc}" alt="${l.titulo}"
                 onerror="this.src='https://via.placeholder.com/300x180?text=📚'"/>
            <div class="card-body">
                <div style="display:flex;gap:6px;flex-wrap:wrap;align-items:center">
                    <span class="card-estado estado-${estado}">${ESTADO_LABEL[estado] || estado}</span>
                    ${promoBadge}
                </div>
                <div class="card-titulo">${l.titulo}</div>
                <div class="card-autor">${l.autor}</div>
                ${l.isbn ? `<div class="card-isbn">${l.isbn}</div>` : ""}
                ${precoHtml}
                ${countdownHtml}
            </div>
            <div class="card-actions">
                <button class="btn-editar" onclick='abrirModalEdit(${JSON.stringify(l)})'>
                    <i class="fa-solid fa-pen"></i> Editar
                </button>
                <button class="btn-excluir" onclick="confirmarExclusao(${l.id}, '${l.titulo.replace(/'/g, "\\'")}')">
                    <i class="fa-solid fa-trash"></i> Excluir
                </button>
            </div>
        </div>`;
    }).join("");

    iniciarContadoresAdmin();
}

/* ── FILTRO ── */
function filtrar() {
    const q = document.getElementById("searchInput").value.toLowerCase();
    if (!q) { renderGrid(todosLivros); return; }
    renderGrid(todosLivros.filter(l =>
        (l.titulo || "").toLowerCase().includes(q) ||
        (l.autor  || "").toLowerCase().includes(q) ||
        (l.isbn   || "").toLowerCase().includes(q)
    ));
}

/* ── PROMO HELPERS ── */
function togglePromo() {
    const ativo = document.getElementById("fEmPromocao").checked;
    document.getElementById("promoSection").style.display = ativo ? "block" : "none";
    if (!ativo) {
        document.getElementById("fDesconto").value   = "";
        document.getElementById("fPrecoPromo").value = "";
        document.getElementById("fPromoExpira").value = "";
    }
}

function calcularPrecoPromo() {
    const preco    = parseFloat(document.getElementById("fPreco").value) || 0;
    const desconto = parseFloat(document.getElementById("fDesconto").value) || 0;
    const promoEl  = document.getElementById("fPrecoPromo");
    if (preco > 0 && desconto > 0 && desconto < 100) {
        promoEl.value = (preco * (1 - desconto / 100)).toFixed(2);
    } else {
        promoEl.value = "";
    }
}

function limparCamposPromo() {
    document.getElementById("fEmPromocao").checked  = false;
    document.getElementById("promoSection").style.display = "none";
    document.getElementById("fDesconto").value      = "";
    document.getElementById("fPrecoPromo").value    = "";
    document.getElementById("fPromoExpira").value   = "";
}

/* ── MODAL ADD ── */
function abrirModalAdd() {
    modoEdicao = false;
    document.getElementById("modalTitulo").textContent = "Adicionar Livro";
    document.getElementById("livroId").value = "";
    document.getElementById("fTitulo").value = "";
    document.getElementById("fAutor").value  = "";
    document.getElementById("fIsbn").value   = "";
    document.getElementById("fPreco").value  = "";
    document.getElementById("fEstado").value = "BOM";
    document.getElementById("fResumo").value = "";
    limparCamposPromo();
    document.getElementById("btnSalvar").textContent = "Adicionar";
    esconderErro();
    abrirModal();
}

/* ── MODAL EDIT ── */
function abrirModalEdit(livro) {
    modoEdicao = true;
    document.getElementById("modalTitulo").textContent = "Editar Livro";
    document.getElementById("livroId").value = livro.id;
    document.getElementById("fTitulo").value = livro.titulo || "";
    document.getElementById("fAutor").value  = livro.autor  || "";
    document.getElementById("fIsbn").value   = livro.isbn   || "";
    document.getElementById("fEstado").value = livro.estadoAprovado || "BOM";
    document.getElementById("fResumo").value = livro.resumoOficial  || "";

    // Preço: se em promoção, mostra o preço original no campo
    const precoBase = livro.emPromocao && livro.precoOriginal != null
        ? livro.precoOriginal : (livro.precoAprovado != null ? livro.precoAprovado : "");
    document.getElementById("fPreco").value = precoBase;

    // Promo fields
    if (livro.emPromocao && livro.precoOriginal != null) {
        const desconto = Math.round((1 - livro.precoAprovado / livro.precoOriginal) * 100);
        document.getElementById("fEmPromocao").checked           = true;
        document.getElementById("promoSection").style.display   = "block";
        document.getElementById("fDesconto").value              = desconto;
        document.getElementById("fPrecoPromo").value            = Number(livro.precoAprovado).toFixed(2);
        document.getElementById("fPromoExpira").value           = livro.promocaoExpira
            ? livro.promocaoExpira.substring(0, 16) : "";
    } else {
        limparCamposPromo();
    }

    document.getElementById("btnSalvar").textContent = "Salvar alterações";
    esconderErro();
    abrirModal();
}

function abrirModal() {
    document.getElementById("modal").classList.add("open");
    document.getElementById("modalOverlay").classList.add("open");
}

function fecharModal() {
    document.getElementById("modal").classList.remove("open");
    document.getElementById("modalOverlay").classList.remove("open");
}

function mostrarErro(msg) {
    const el = document.getElementById("modalErro");
    el.textContent = msg;
    el.style.display = "block";
}

function esconderErro() {
    document.getElementById("modalErro").style.display = "none";
}

/* ── SALVAR (ADD OU EDIT) ── */
async function salvarLivro(e) {
    e.preventDefault();
    esconderErro();

    const btn = document.getElementById("btnSalvar");
    btn.disabled = true;
    btn.textContent = "Salvando...";

    const emPromocao = document.getElementById("fEmPromocao").checked;
    const desconto   = parseFloat(document.getElementById("fDesconto").value) || 0;
    const promoExpiraRaw = document.getElementById("fPromoExpira").value;
    // datetime-local gives "YYYY-MM-DDTHH:mm", backend expects "YYYY-MM-DDTHH:mm:ss"
    const promocaoExpira = promoExpiraRaw ? promoExpiraRaw + ":00" : null;

    const payload = {
        titulo:              document.getElementById("fTitulo").value.trim(),
        autor:               document.getElementById("fAutor").value.trim(),
        isbn:                document.getElementById("fIsbn").value.trim(),
        preco:               parseFloat(document.getElementById("fPreco").value),
        estado:              document.getElementById("fEstado").value,
        resumo:              document.getElementById("fResumo").value.trim(),
        emPromocao:          emPromocao,
        percentualDesconto:  emPromocao ? desconto : null,
        promocaoExpira:      emPromocao ? promocaoExpira : null,
    };

    try {
        const id  = document.getElementById("livroId").value;
        const url = modoEdicao ? `/api/admin/livros/${id}` : "/api/admin/livros/novo";
        const method = modoEdicao ? "PUT" : "POST";

        const res = await fetch(url, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });

        const data = await res.json().catch(() => ({}));

        if (!res.ok) {
            mostrarErro(typeof data === "string" ? data : (data.message || "Erro ao salvar."));
            btn.disabled = false;
            btn.textContent = modoEdicao ? "Salvar alterações" : "Adicionar";
            return;
        }

        btn.disabled = false;
        btn.textContent = modoEdicao ? "Salvar alterações" : "Adicionar";
        fecharModal();
        await carregarEstoque();

    } catch (_) {
        mostrarErro("Erro de conexão. Tente novamente.");
        btn.disabled = false;
        btn.textContent = modoEdicao ? "Salvar alterações" : "Adicionar";
    }
}

/* ── EXCLUIR ── */
function confirmarExclusao(id, titulo) {
    document.getElementById("confirmMsg").textContent =
        `Tem certeza que deseja remover "${titulo}" do estoque? Esta ação não pode ser desfeita.`;
    document.getElementById("btnConfirmDelete").onclick = () => excluirLivro(id);
    document.getElementById("confirmModal").classList.add("open");
    document.getElementById("confirmOverlay").classList.add("open");
}

function fecharConfirm() {
    document.getElementById("confirmModal").classList.remove("open");
    document.getElementById("confirmOverlay").classList.remove("open");
}

async function excluirLivro(id) {
    fecharConfirm();
    try {
        const res = await fetch(`/api/admin/livros/${id}`, { method: "DELETE" });
        if (res.ok) {
            todosLivros = todosLivros.filter(l => l.id !== id);
            renderGrid(todosLivros);
        }
    } catch (_) {}
}

/* ── CONTADOR REGRESSIVO (ADMIN) ── */
let admContadorInterval = null;

function iniciarContadoresAdmin() {
    if (admContadorInterval) clearInterval(admContadorInterval);

    const atualizar = () => {
        const agora = Date.now();
        document.querySelectorAll('.promo-countdown').forEach(el => {
            const expira = new Date(el.dataset.expira).getTime();
            const diff   = expira - agora;
            const id     = el.dataset.livroId;

            if (diff <= 0) {
                el.style.display = 'none';
                const badge = document.getElementById('adm-badge-' + id);
                if (badge) badge.style.display = 'none';
                const precoEl = document.getElementById('adm-preco-' + id);
                if (precoEl) {
                    const precoOriginal = parseFloat(el.dataset.precoOriginal) || 0;
                    precoEl.innerHTML = `T$ ${precoOriginal.toFixed(2)}`;
                }
            } else {
                const h = Math.floor(diff / 3600000);
                const m = Math.floor((diff % 3600000) / 60000);
                const s = Math.floor((diff % 60000) / 1000);
                const timerEl = document.getElementById('adm-timer-' + id);
                if (timerEl) timerEl.textContent = `${h}h ${m}m ${s}s`;
            }
        });
    };

    atualizar();
    admContadorInterval = setInterval(atualizar, 1000);
}

/* ── SIDEBAR MOBILE ── */
function abrirMenu() {
    document.getElementById("sidebar").classList.add("aberto");
    document.getElementById("overlay").style.display = "block";
}
function fecharMenu() {
    document.getElementById("sidebar").classList.remove("aberto");
    document.getElementById("overlay").style.display = "none";
}

/* ── INIT ── */
carregarEstoque();
