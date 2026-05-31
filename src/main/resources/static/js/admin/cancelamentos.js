/* ================================================================
   cancelamentos.js — Admin · Bibliotroca
   Lista compacta e clicável — detalhes em /admin/cancelamentos/{id}
   ================================================================ */

let _dados = [];

/* ── UTILS ──────────────────────────────────────────────────────── */

function fmtData(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleDateString("pt-BR", {
        day: "2-digit",
        month: "short",
        year: "numeric",
    }) +
        " " +
        d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

function escHtml(str) {
    if (str == null) return "";
    return String(str)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#39;");
}

function primeiraFoto(fotosUrls) {
    if (!fotosUrls) return null;
    try {
        const parsed = JSON.parse(fotosUrls);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed[0];
    } catch (_) { /* não é JSON */ }
    return fotosUrls.split(",")[0].trim() || null;
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

/* ── BADGES ─────────────────────────────────────────────────────── */

const MOTIVO_STYLE = {
    COMPREI_POR_ENGANO: { bg: "#fef3c7", color: "#92400e" },
    ENCONTREI_MAIS_BARATO: { bg: "#dbeafe", color: "#1e40af" },
    PRODUTO_NAO_ESPERADO: { bg: "#ede9fe", color: "#5b21b6" },
    OUTRO: { bg: "#f3f4f6", color: "#374151" },
};

function motivoBadge(s) {
    const m = MOTIVO_STYLE[s.motivoCategoria] ||
        { bg: "#f3f4f6", color: "#374151" };
    return `<span class="badge-motivo" style="background:${m.bg};color:${m.color}">` +
        escHtml(s.motivoCategoriaDescricao || s.motivoCategoria) + `</span>`;
}

function statusBadge(status) {
    const cls = {
        PENDENTE: "badge-PENDENTE",
        APROVADO: "badge-APROVADO",
        RECUSADO: "badge-RECUSADO",
    };
    const label = {
        PENDENTE: "Pendente",
        APROVADO: "Aprovado",
        RECUSADO: "Recusado",
    };
    return `<span class="badge ${cls[status] || ""}">${
        label[status] || escHtml(status)
    }</span>`;
}

/* ── CARDS COMPACTOS ─────────────────────────────────────────────── */

function renderCards(lista) {
    const area = document.getElementById("cardsArea");
    if (lista.length === 0) {
        area.innerHTML = `
            <div class="empty-state">
                <i class="fa-solid fa-inbox"></i>
                <p>Nenhuma solicitação encontrada.</p>
            </div>`;
        return;
    }

    area.innerHTML = lista.map((s) => {
        const foto = primeiraFoto(s.fotosUrls);
        const fotoHtml = foto
            ? `<img src="${escHtml(foto)}" alt="Capa" class="compact-foto">`
            : `<div class="compact-foto compact-foto-ph"><i class="fa-solid fa-book"></i></div>`;

        const isPendente = s.status === "PENDENTE";

        return `
        <div class="compact-card ${isPendente ? "card-pendente" : ""}"
             onclick="window.location.href='/admin/cancelamentos/${s.id}'"
             role="button" tabindex="0"
             onkeydown="if(event.key==='Enter')window.location.href='/admin/cancelamentos/${s.id}'">
            ${fotoHtml}
            <div class="compact-info">
                <div class="compact-titulo">${escHtml(s.tituloLivro)}</div>
                <div class="compact-autor">${escHtml(s.autorLivro || "—")}</div>
                <div class="compact-comprador">
                    <i class="fa-solid fa-user"></i>
                    ${escHtml(s.clienteNome)}
                    <span class="compact-email">${
            escHtml(s.clienteEmail)
        }</span>
                </div>
            </div>
            <div class="compact-meta">
                <div class="compact-badges">
                    ${motivoBadge(s)}
                    ${statusBadge(s.status)}
                </div>
                <div class="compact-bottom">
                    <span class="compact-estorno">
                        <i class="fa-solid fa-coins"></i> T$ ${
            (s.precoLivro || 0).toFixed(2)
        }
                    </span>
                    <span class="compact-data">${
            fmtData(s.dataSolicitacao)
        }</span>
                </div>
            </div>
            <div class="compact-arrow"><i class="fa-solid fa-chevron-right"></i></div>
        </div>`;
    }).join("");
}

/* ── DADOS ──────────────────────────────────────────────────────── */

async function carregarDados() {
    const area = document.getElementById("cardsArea");
    area.innerHTML =
        '<div class="loading-placeholder"><i class="fa-solid fa-spinner fa-spin"></i> Carregando solicitações...</div>';

    try {
        const res = await fetch("/api/admin/cancelamentos", {
            credentials: "include",
        });
        if (res.status === 401 || res.status === 403) {
            window.location.href = "/admin/login";
            return;
        }
        if (!res.ok) throw new Error("HTTP " + res.status);

        _dados = await res.json();
        atualizarStats();
        aplicarFiltro();
    } catch (e) {
        area.innerHTML =
            `<div class="loading-placeholder" style="color:#722F37">
            <i class="fa-solid fa-triangle-exclamation"></i> Erro ao carregar cancelamentos.
        </div>`;
    }
}

function atualizarStats() {
    document.getElementById("statTotal").textContent = _dados.length;
    document.getElementById("statPendentes").textContent =
        _dados.filter((d) => d.status === "PENDENTE").length;
    document.getElementById("statAprovados").textContent =
        _dados.filter((d) => d.status === "APROVADO").length;
    document.getElementById("statRecusados").textContent =
        _dados.filter((d) => d.status === "RECUSADO").length;
}

function aplicarFiltro() {
    const filtroStatus = document.getElementById("filtroStatus").value;
    const busca = (document.getElementById("busca")?.value || "").toLowerCase()
        .trim();

    let lista = _dados;
    if (filtroStatus) lista = lista.filter((d) => d.status === filtroStatus);
    if (busca) {
        lista = lista.filter((d) =>
            (d.tituloLivro || "").toLowerCase().includes(busca) ||
            (d.clienteNome || "").toLowerCase().includes(busca) ||
            (d.clienteEmail || "").toLowerCase().includes(busca)
        );
    }
    renderCards(lista);
}

/* ── INIT ────────────────────────────────────────────────────────── */

document.getElementById("dataHoje").textContent = new Date().toLocaleDateString(
    "pt-BR",
    { weekday: "long", day: "2-digit", month: "long", year: "numeric" },
);

carregarDados();
