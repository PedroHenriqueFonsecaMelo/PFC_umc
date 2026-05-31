/* ================================================================
   clientes.js — Admin · Bibliotroca
   ================================================================ */

let _todos = [];
let _filtroAtivo = "todos";

/* ── UTILS ──────────────────────────────────────────────────── */
function fmtData(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleDateString("pt-BR", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    });
}

function fmtTokens(v) {
    if (v == null) return "0";
    return Number(v).toLocaleString("pt-BR", {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
    });
}

function inicial(nome) {
    if (!nome) return "?";
    return nome.trim().charAt(0).toUpperCase();
}

function mostrarToast(cls, msg) {
    const t = document.getElementById("toast");
    t.className = "toast " + cls;
    t.textContent = msg;
    t.style.display = "block";
    setTimeout(() => {
        t.style.display = "none";
    }, 4000);
}

function abrirMenu() {
    document.getElementById("sidebar")?.classList.add("open");
    document.getElementById("overlay")?.classList.add("show");
}
function fecharMenu() {
    document.getElementById("sidebar")?.classList.remove("open");
    document.getElementById("overlay")?.classList.remove("show");
}

/* ── FETCH ──────────────────────────────────────────────────── */
function carregarDados() {
    const card = document.getElementById("tableCard");
    card.innerHTML =
        '<div class="loading-placeholder"><i class="fa-solid fa-spinner fa-spin"></i> Carregando clientes...</div>';

    fetch("/api/admin/clientes", { credentials: "include" })
        .then((r) => {
            if (!r.ok) throw new Error("Falha ao carregar clientes");
            return r.json();
        })
        .then((data) => {
            _todos = data;
            renderTabela(_todos);
            const total = document.getElementById("totalLabel");
            if (total) {
                total.textContent = `${data.length} cliente${
                    data.length !== 1 ? "s" : ""
                } cadastrado${data.length !== 1 ? "s" : ""}`;
            }
        })
        .catch((err) => {
            card.innerHTML =
                '<div class="empty-state"><i class="fa-solid fa-triangle-exclamation"></i><p>Erro ao carregar dados. Tente novamente.</p></div>';
            mostrarToast("toast-err", "Erro ao carregar clientes.");
            console.error(err);
        });
}

/* ── FILTROS ────────────────────────────────────────────────── */
function setFiltro(filtro) {
    _filtroAtivo = filtro;
    document.querySelectorAll(".filter-tab").forEach((el) => {
        el.classList.toggle("ativo", el.dataset.filtro === filtro);
    });
    aplicarFiltros();
}

function aplicarFiltros() {
    const busca = (document.getElementById("busca")?.value || "").toLowerCase()
        .trim();

    let lista = _todos;
    if (_filtroAtivo === "ativos") lista = lista.filter((c) => c.ativo);
    if (_filtroAtivo === "inativos") lista = lista.filter((c) => !c.ativo);
    if (busca) {
        lista = lista.filter((c) =>
            (c.nome || "").toLowerCase().includes(busca) ||
            (c.email || "").toLowerCase().includes(busca)
        );
    }
    renderTabela(lista);
}

/* ── RENDER ─────────────────────────────────────────────────── */
function badgeNivel(nivel) {
    const cls = {
        Bronze: "nivel-Bronze",
        Prata: "nivel-Prata",
        Ouro: "nivel-Ouro",
        Platina: "nivel-Platina",
    };
    return `<span class="badge-nivel ${cls[nivel] || "nivel-Bronze"}">${
        nivel || "Bronze"
    }</span>`;
}

function badgeStatus(ativo) {
    return ativo
        ? `<span class="badge-status status-ativo"><i class="fa-solid fa-circle" style="font-size:7px"></i> Ativo</span>`
        : `<span class="badge-status status-inativo"><i class="fa-solid fa-circle" style="font-size:7px"></i> Inativo</span>`;
}

function renderTabela(lista) {
    const card = document.getElementById("tableCard");
    if (lista.length === 0) {
        card.innerHTML = `
            <div class="empty-state">
                <i class="fa-solid fa-users-slash"></i>
                <p>Nenhum cliente encontrado.</p>
            </div>`;
        return;
    }

    const linhas = lista.map((c) => `
        <tr>
            <td>
                <div class="cliente-info">
                    <div class="cliente-avatar">${inicial(c.nome)}</div>
                    <div>
                        <div class="cliente-nome">${esc(c.nome)}</div>
                        <div class="cliente-email">${esc(c.email)}</div>
                    </div>
                </div>
            </td>
            <td>${fmtData(c.dataCadastro)}</td>
            <td>${fmtTokens(c.saldoTokens)} tk</td>
            <td>${badgeNivel(c.nivel)}</td>
            <td>${c.totalCompras}</td>
            <td>${badgeStatus(c.ativo)}</td>
            <td>
                <a href="/admin/clientes/${c.id}" class="btn-detalhes">
                    <i class="fa-solid fa-eye"></i> Ver Detalhes
                </a>
            </td>
        </tr>
    `).join("");

    card.innerHTML = `
        <table class="clientes-table">
            <thead>
                <tr>
                    <th>Cliente</th>
                    <th>Cadastro</th>
                    <th>Saldo</th>
                    <th>Nível</th>
                    <th>Compras</th>
                    <th>Status</th>
                    <th></th>
                </tr>
            </thead>
            <tbody>${linhas}</tbody>
        </table>`;
}

function esc(str) {
    if (str == null) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

/* ── INIT ───────────────────────────────────────────────────── */
document.addEventListener("DOMContentLoaded", () => {
    const hoje = document.getElementById("dataHoje");
    if (hoje) {
        hoje.textContent = new Date().toLocaleDateString("pt-BR", {
            weekday: "long",
            day: "2-digit",
            month: "long",
            year: "numeric",
        });
    }
    carregarDados();
});
