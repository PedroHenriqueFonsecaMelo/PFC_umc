/* ================================================================
   minhas-compras.js — Minhas Compras · Bibliotroca
   ================================================================ */

const token = localStorage.getItem("token");
const authHeader = token ? { "Authorization": `Bearer ${token}` } : {};

function fmtData(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleDateString("pt-BR", { day: "2-digit", month: "short", year: "numeric" });
}

function fmtHora(iso) {
    if (!iso) return "";
    const d = new Date(iso);
    return d.toLocaleTimeString("pt-BR", { hour: "2-digit", minute: "2-digit" });
}

function primeiraFoto(fotosUrls) {
    try {
        const arr = JSON.parse(fotosUrls);
        if (Array.isArray(arr) && arr.length > 0) return arr[0];
    } catch (_) {}
    return null;
}

const PASSOS = [
    { key: "AGUARDANDO_ENVIO", label: "Aguardando" },
    { key: "EM_TRANSITO", label: "Em trânsito" },
    { key: "ENTREGUE", label: "Entregue" },
];

function buildTimeline(status) {
    if (status === "CANCELADO") {
        return `<div class="mt-3" style="margin-top:.75rem">
            <span class="status-badge status-CANCELADO">Cancelado</span>
        </div>`;
    }
    const ordemAtual = PASSOS.findIndex((p) => p.key === status);
    return `
        <div class="timeline">
            ${PASSOS.map((p, i) => {
                const cls = i < ordemAtual ? "done" : i === ordemAtual ? "active" : "";
                const icon = i < ordemAtual ? "✓" : i + 1;
                return `
                    <div class="tl-step ${cls}">
                        <div class="tl-dot">${icon}</div>
                        <div class="tl-label">${p.label}</div>
                    </div>`;
            }).join("")}
        </div>`;
}

function buildCard(p) {
    const foto = primeiraFoto(p.fotosUrls);
    const imgSrc = foto || "https://via.placeholder.com/72x96?text=📚";

    const rastreio = (p.codigoRastreio && p.statusEnvio === "EM_TRANSITO")
        ? `<div class="rastreio-info">
             <i class="fa-solid fa-truck-fast"></i> ${p.codigoRastreio}
           </div>`
        : "";

    const dataAtual = p.dataAtualizacaoStatus
        ? `<span style="font-size:.75rem;color:#7A6E65">Atualizado em ${fmtData(p.dataAtualizacaoStatus)}</span>`
        : "";

    return `
        <div class="pedido-card">
            <img class="pedido-capa" src="${imgSrc}" alt="${p.tituloLivro}"
                 onerror="this.src='https://via.placeholder.com/72x96?text=📚'"/>
            <div class="pedido-info">
                <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:.75rem;flex-wrap:wrap">
                    <div>
                        <div class="pedido-titulo">${p.tituloLivro}</div>
                        <div class="pedido-autor">${p.autorLivro}</div>
                    </div>
                    <span class="status-badge status-${p.statusEnvio}">${p.statusEnvioDescricao}</span>
                </div>
                <div class="pedido-meta">
                    <span>Pedido #${p.id}</span>
                    <span>·</span>
                    <span>${fmtData(p.dataCompra)} ${fmtHora(p.dataCompra)}</span>
                    <span>·</span>
                    <span class="pedido-preco">T$ ${p.precoLivro.toFixed(2)}</span>
                    ${p.statusEnvio === "CANCELADO"
                        ? `<span>·</span><span class="pedido-estorno">↩ T$ ${p.precoLivro.toFixed(2)} estornados</span>`
                        : ""}
                </div>
                ${rastreio}
                ${dataAtual}
                ${buildTimeline(p.statusEnvio)}
            </div>
        </div>`;
}

function emptyState(msg) {
    return `<div class="empty-state">
        <div class="icon">📭</div>
        <p>${msg}</p>
    </div>`;
}

async function carregarLista(endpoint, containerId) {
    const container = document.getElementById(containerId);
    try {
        const res = await fetch(endpoint, { headers: authHeader });
        if (res.status === 401) { window.location.href = "/clientes/login"; return []; }
        if (!res.ok) throw new Error("Falha na API");
        return await res.json();
    } catch (e) {
        container.innerHTML = `<p style="text-align:center;color:#722F37;padding:2rem">Erro ao carregar pedidos.</p>`;
        return [];
    }
}

function renderLista(lista, containerId, mensagemVazia) {
    const container = document.getElementById(containerId);
    if (!lista || lista.length === 0) {
        container.innerHTML = emptyState(mensagemVazia);
        return;
    }
    container.innerHTML = lista.map(buildCard).join("");
}

async function carregarPerfil() {
    try {
        const res = await fetch("/clientes/meu-perfil-json", { headers: authHeader });
        if (res.ok) {
            const c = await res.json();
            const navSaldo = document.getElementById("navSaldo");
            if (navSaldo) navSaldo.textContent = `T$ ${(c.saldoTokens || 0).toFixed(2)}`;
        }
    } catch (_) {}
}

function trocarTab(nome, btn) {
    document.querySelectorAll(".tab-btn").forEach((b) => b.classList.remove("active"));
    document.querySelectorAll(".tab-panel").forEach((p) => p.classList.remove("active"));
    btn.classList.add("active");
    document.getElementById("panel-" + nome).classList.add("active");
}

async function init() {
    carregarPerfil();

    const [pendentes, concluidos, todos] = await Promise.all([
        carregarLista("/api/pedidos/pendentes", "lista-pendentes"),
        carregarLista("/api/pedidos/concluidos", "lista-concluidos"),
        carregarLista("/api/pedidos/todos", "lista-todos"),
    ]);

    renderLista(pendentes, "lista-pendentes", "Nenhuma compra em andamento no momento.");
    renderLista(concluidos, "lista-concluidos", "Nenhuma compra concluída ainda.");
    renderLista(todos, "lista-todos", "Você ainda não realizou nenhuma compra.");

    if (pendentes.length > 0) {
        const b = document.getElementById("badgePendentes");
        b.textContent = pendentes.length;
        b.style.display = "inline-flex";
    }
    if (concluidos.length > 0) {
        const b = document.getElementById("badgeConcluidos");
        b.textContent = concluidos.length;
        b.style.display = "inline-flex";
    }
}

init();
