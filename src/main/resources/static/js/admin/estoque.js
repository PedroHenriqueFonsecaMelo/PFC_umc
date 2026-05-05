/* ================================================================
   estoque.js — Gestão de Estoque · Admin Bibliotroca
   ================================================================ */

let todosLivros = [];
let modoEdicao  = false;

/* ── DATA NO TOPBAR ── */
document.getElementById("dataHoje").textContent =
    new Date().toLocaleDateString("pt-BR", { weekday: "long", day: "2-digit", month: "long", year: "numeric" });

/* ── UTILS ── */
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
        const preco  = l.precoAprovado != null ? `T$ ${Number(l.precoAprovado).toFixed(2)}` : "—";

        return `
        <div class="livro-card-admin">
            <img class="card-img" src="${imgSrc}" alt="${l.titulo}"
                 onerror="this.src='https://via.placeholder.com/300x180?text=📚'"/>
            <div class="card-body">
                <span class="card-estado estado-${estado}">${ESTADO_LABEL[estado] || estado}</span>
                <div class="card-titulo">${l.titulo}</div>
                <div class="card-autor">${l.autor}</div>
                ${l.isbn ? `<div class="card-isbn">${l.isbn}</div>` : ""}
                <div class="card-preco">${preco}</div>
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
    document.getElementById("fPreco").value  = livro.precoAprovado != null ? livro.precoAprovado : "";
    document.getElementById("fEstado").value = livro.estadoAprovado || "BOM";
    document.getElementById("fResumo").value = livro.resumoOficial  || "";
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

    const payload = {
        titulo: document.getElementById("fTitulo").value.trim(),
        autor:  document.getElementById("fAutor").value.trim(),
        isbn:   document.getElementById("fIsbn").value.trim(),
        preco:  parseFloat(document.getElementById("fPreco").value),
        estado: document.getElementById("fEstado").value,
        resumo: document.getElementById("fResumo").value.trim(),
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
