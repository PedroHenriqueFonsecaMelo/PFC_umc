/* ================================================================
   cupons.js — Gestão de Cupons · Admin Bibliotroca
   ================================================================ */

let todosCupons = [];

/* ── DATA NO TOPBAR ── */
document.getElementById("dataHoje").textContent =
    new Date().toLocaleDateString("pt-BR", { weekday: "long", day: "2-digit", month: "long", year: "numeric" });

/* ── UTILS ── */
function statusCupom(c) {
    if (c.usado) return "usado";
    if (new Date(c.expiracao) < new Date()) return "expirado";
    return "ativo";
}

function formatarData(iso) {
    if (!iso) return "—";
    return new Date(iso).toLocaleDateString("pt-BR", { day: "2-digit", month: "2-digit", year: "numeric" });
}

function venceBreve(iso) {
    if (!iso) return false;
    const diff = new Date(iso) - new Date();
    return diff > 0 && diff < 7 * 24 * 60 * 60 * 1000; // menos de 7 dias
}

/* ── CARREGAR CUPONS ── */
async function carregarCupons() {
    try {
        const res = await fetch("/api/admin/cupons");
        if (!res.ok) throw new Error();
        todosCupons = await res.json();
        atualizarStats();
        filtrar();
    } catch (_) {
        document.getElementById("cuponsBody").innerHTML =
            `<tr><td colspan="9" class="loading-row" style="color:#722f37">Erro ao carregar cupons.</td></tr>`;
    }
}

/* ── STATS ── */
function atualizarStats() {
    const agora = new Date();
    let ativos = 0, usados = 0, expirados = 0;
    todosCupons.forEach(c => {
        if (c.usado) usados++;
        else if (new Date(c.expiracao) < agora) expirados++;
        else ativos++;
    });
    document.getElementById("statTotal").textContent    = todosCupons.length;
    document.getElementById("statAtivos").textContent   = ativos;
    document.getElementById("statUsados").textContent   = usados;
    document.getElementById("statExpirados").textContent = expirados;
}

/* ── FILTRAR ── */
function filtrar() {
    const q       = document.getElementById("searchInput").value.toLowerCase().trim();
    const tipo    = document.getElementById("filtroTipo").value;
    const status  = document.getElementById("filtroStatus").value;

    let lista = todosCupons.filter(c => {
        const matchQ = !q ||
            c.codigo.toLowerCase().includes(q) ||
            (c.clienteNome  || "").toLowerCase().includes(q) ||
            (c.clienteEmail || "").toLowerCase().includes(q);
        const matchTipo   = !tipo   || c.tipo === tipo;
        const matchStatus = !status || statusCupom(c) === status;
        return matchQ && matchTipo && matchStatus;
    });

    renderTabela(lista);
}

/* ── RENDER TABELA ── */
function renderTabela(lista) {
    const tbody = document.getElementById("cuponsBody");
    const vazio = document.getElementById("cuponsVazio");
    const cont  = document.getElementById("contador");

    cont.textContent = `${lista.length} cupom${lista.length !== 1 ? "s" : ""}`;

    if (lista.length === 0) {
        tbody.innerHTML = "";
        document.getElementById("cuponsTable").style.display = "none";
        vazio.style.display = "block";
        return;
    }

    document.getElementById("cuponsTable").style.display = "";
    vazio.style.display = "none";

    tbody.innerHTML = lista.map(c => {
        const st = statusCupom(c);

        const badgeTipo = c.tipo === "PONTUACAO"
            ? `<span class="badge-tipo badge-xp"><i class="fa-solid fa-star"></i> XP</span>`
            : `<span class="badge-tipo badge-promo"><i class="fa-solid fa-tag"></i> Promo</span>`;

        const badgeStatus = st === "ativo"
            ? `<span class="badge-status badge-ativo"><i class="fa-solid fa-circle" style="font-size:.45rem"></i> Disponível</span>`
            : st === "usado"
            ? `<span class="badge-status badge-usado"><i class="fa-solid fa-check"></i> Utilizado</span>`
            : `<span class="badge-status badge-expirado"><i class="fa-solid fa-clock"></i> Expirado</span>`;

        const clienteHtml = c.clienteNome
            ? `<div class="cliente-nome">${esc(c.clienteNome)}</div>
               <div class="cliente-email">${esc(c.clienteEmail || "")}</div>`
            : `<span class="badge-publico"><i class="fa-solid fa-globe"></i> Público</span>`;

        const expClass = st === "expirado" ? "expirada" : (venceBreve(c.expiracao) ? "vence-breve" : "");

        const usosHtml = c.quantidadeMaxima != null
            ? `${c.quantidadeUsada || 0}/${c.quantidadeMaxima}`
            : `${c.quantidadeUsada || 0}/∞`;

        const acaoHtml = st === "ativo"
            ? `<button class="btn-invalidar" onclick="confirmarInvalidacao(${c.id}, '${esc(c.codigo)}')">
                 <i class="fa-solid fa-ban"></i> Invalidar
               </button>`
            : `<span style="color:#9c968f;font-size:.78rem">—</span>`;

        return `<tr>
          <td><span class="codigo-cell">${esc(c.codigo)}</span></td>
          <td>${badgeTipo}</td>
          <td><span class="valor-cell">${Number(c.percentualDesconto).toFixed(0)}% off</span></td>
          <td><span style="font-size:.82rem;color:#4b4540">${usosHtml}</span></td>
          <td>${clienteHtml}</td>
          <td><span class="data-cell">${formatarData(c.dataCriacao)}</span></td>
          <td><span class="data-cell ${expClass}">${formatarData(c.expiracao)}</span></td>
          <td>${badgeStatus}</td>
          <td>${acaoHtml}</td>
        </tr>`;
    }).join("");
}

function esc(s) {
    if (!s) return "";
    return String(s)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;");
}

/* ── MODAL NOVO CUPOM ── */
function abrirModalNovo() {
    document.getElementById("fPercentual").value   = "";
    document.getElementById("fDataValidade").value = "";
    document.getElementById("fCodigo").value       = "";
    document.getElementById("fQtdMaxima").value    = "";
    document.getElementById("fClienteId").value    = "";
    document.getElementById("modalErro").style.display = "none";
    document.getElementById("btnSalvar").disabled  = false;
    document.getElementById("btnSalvar").textContent = "Gerar Cupom";
    document.getElementById("modal").classList.add("open");
    document.getElementById("modalOverlay").classList.add("open");
    document.getElementById("fPercentual").focus();
}

function fecharModal() {
    document.getElementById("modal").classList.remove("open");
    document.getElementById("modalOverlay").classList.remove("open");
}

async function criarCupom(e) {
    e.preventDefault();
    const btn = document.getElementById("btnSalvar");
    btn.disabled = true;
    btn.textContent = "Gerando...";
    document.getElementById("modalErro").style.display = "none";

    const percentual    = parseFloat(document.getElementById("fPercentual").value);
    const dataValidade  = document.getElementById("fDataValidade").value; // "yyyy-MM-ddTHH:mm"
    const codigo        = document.getElementById("fCodigo").value.trim().toUpperCase();
    const qtdMaxima     = document.getElementById("fQtdMaxima").value.trim();
    const clienteId     = document.getElementById("fClienteId").value.trim();

    const payload = {
        percentualDesconto: percentual,
        dataValidade: dataValidade + ":00"  // adiciona segundos para ISO-8601 completo
    };
    if (codigo)     payload.codigo          = codigo;
    if (qtdMaxima)  payload.quantidadeMaxima = parseInt(qtdMaxima, 10);
    if (clienteId)  payload.clienteId       = parseInt(clienteId, 10);

    try {
        const res = await fetch("/api/admin/cupons", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
        });
        const data = await res.json().catch(() => ({}));

        if (!res.ok) {
            mostrarErroModal(data.erro || "Erro ao criar cupom.");
            btn.disabled = false;
            btn.textContent = "Gerar Cupom";
            return;
        }

        fecharModal();
        mostrarToast(`Cupom ${data.codigo} criado — ${Number(data.percentualDesconto).toFixed(0)}% off`, "ok");
        await carregarCupons();
    } catch (_) {
        mostrarErroModal("Erro de conexão. Tente novamente.");
        btn.disabled = false;
        btn.textContent = "Gerar Cupom";
    }
}

function mostrarErroModal(msg) {
    const el = document.getElementById("modalErro");
    el.textContent = msg;
    el.style.display = "block";
}

/* ── CONFIRMAR INVALIDAÇÃO ── */
function confirmarInvalidacao(id, codigo) {
    document.getElementById("confirmMsg").textContent =
        `Tem certeza que deseja invalidar o cupom "${codigo}"? Ele não poderá mais ser utilizado.`;
    document.getElementById("btnConfirmDelete").onclick = () => invalidarCupom(id, codigo);
    document.getElementById("confirmModal").classList.add("open");
    document.getElementById("confirmOverlay").classList.add("open");
}

function fecharConfirm() {
    document.getElementById("confirmModal").classList.remove("open");
    document.getElementById("confirmOverlay").classList.remove("open");
}

async function invalidarCupom(id, codigo) {
    fecharConfirm();
    try {
        const res = await fetch(`/api/admin/cupons/${id}`, { method: "DELETE" });
        const data = await res.json().catch(() => ({}));

        if (res.ok) {
            mostrarToast(`Cupom ${codigo} invalidado.`, "ok");
            await carregarCupons();
        } else {
            mostrarToast(data.erro || "Erro ao invalidar.", "erro");
        }
    } catch (_) {
        mostrarToast("Erro de conexão.", "erro");
    }
}

/* ── TOAST ── */
let toastTimer;
function mostrarToast(msg, tipo) {
    const el = document.getElementById("toast");
    el.textContent = msg;
    el.className = `toast show ${tipo}`;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { el.classList.remove("show"); }, 3500);
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
carregarCupons();
