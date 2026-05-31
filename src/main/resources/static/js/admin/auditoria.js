// ================================================================
//  auditoria.js — Painel de Auditoria Admin (v2)
// ================================================================

// Data na topbar
const dataHojeEl = document.getElementById("dataHoje");
if (dataHojeEl) {
  dataHojeEl.textContent = new Date().toLocaleDateString("pt-BR", {
    weekday: "long",
    day: "2-digit",
    month: "long",
    year: "numeric",
  });
}

// ── Tradução de nomes de ação ────────────────────────────────────
const ACOES_LABEL = {
  "LOGIN_SUCESSO": "Login realizado",
  "AUTENT_FALHA": "Falha na autenticação",
  "ALTERACAO_SENHA": "Senha alterada",
  "SENHA_ALTERADA": "Senha alterada",
  "CADASTRO_USUARIO": "Cadastro iniciado",
  "CADASTRO_COMPLETO": "Cadastro concluído",
  "UPLOAD_FOTO": "Foto de perfil atualizada",
  "COMPRA_LIVRO_SUCESSO": "Compra realizada",
  "COMPRA_CARRINHO": "Compra via carrinho",
  "PEDIDO_STATUS_ATUALIZADO": "Status do pedido atualizado",
  "PEDIDO_CANCELADO_ESTORNO": "Pedido cancelado com estorno",
  "CANCELAMENTO_SOLICITADO": "Cancelamento solicitado",
  "CANCELAMENTO_APROVADO": "Cancelamento aprovado",
  "CANCELAMENTO_RECUSADO": "Cancelamento recusado",
  "CANCELAMENTO_ADMIN": "Cancelamento pelo administrador",
  "LIVRO_CADASTRADO": "Livro cadastrado",
  "LOTE_CADASTRADO": "Lote cadastrado",
  "LIVRO_APROVADO": "Livro aprovado",
  "LIVRO_REJEITADO": "Livro rejeitado",
  "TOKENS_ADICIONADOS": "Tokens adicionados",
  "TOKENS_DEBITADOS": "Tokens debitados",
  "RECARGA_TOKENS": "Recarga de tokens",
  "PIX_FALHA": "Falha no pagamento via Pix",
  "NIVEL_SUBIU": "Nível aumentado",
  "XP_ZERADO": "XP zerado por inatividade",
  "ERRO_CUPOM": "Erro ao aplicar cupom",
  "EXPORTACAO_CSV": "Exportação CSV",
  "EXPORTACAO_PDF": "Exportação PDF",
};

function traduzirAcao(acao) {
  if (!acao) return "—";
  return ACOES_LABEL[acao.toUpperCase()] ||
    acao.replace(/_/g, " ").toLowerCase().replace(
      /\b\w/g,
      (c) => c.toUpperCase(),
    );
}

function formatarDataHora(valor) {
  if (!valor) return "—";
  try {
    const d = new Date(valor);
    if (isNaN(d.getTime())) return valor;
    const pad = (n) => String(n).padStart(2, "0");
    return pad(d.getDate()) + "/" + pad(d.getMonth() + 1) + "/" +
      d.getFullYear() +
      " " + pad(d.getHours()) + ":" + pad(d.getMinutes()) + ":" +
      pad(d.getSeconds());
  } catch (_) {
    return valor;
  }
}

// ── Classificação de badge por ação ──────────────────────────────
function classeBadge(acao) {
  if (!acao) return "badge-info";
  const a = acao.toUpperCase();
  if (
    a.includes("FALHA") || a.includes("ERRO") || a.includes("BLOQUEIO") ||
    a.includes("NEGADO") || a.includes("DELETE")
  ) {
    return "badge-critico";
  }
  if (a.includes("AVISO") || a.includes("TENTATIVA") || a.includes("WARN")) {
    return "badge-aviso";
  }
  if (a.includes("LOGIN") && !a.includes("FALHA")) {
    return "badge-sucesso";
  }
  return "badge-info";
}

function isSuspeito(acao) {
  if (!acao) return false;
  const a = acao.toUpperCase();
  return a.includes("FALHA") || a.includes("BLOQUEIO") || a.includes("NEGADO");
}

// ── Estado da tabela ─────────────────────────────────────────────
let dados = Array.isArray(LOGS_DATA) ? LOGS_DATA : [];
let paginaAtual = 1;
let itensPorPagina = 25;
let sortCol = null;
let sortDir = "desc";

function ordenar(col) {
  if (sortCol === col) {
    sortDir = sortDir === "asc" ? "desc" : "asc";
  } else {
    sortCol = col;
    sortDir = "asc";
  }
  document.querySelectorAll("th.sortable").forEach((th) => {
    th.classList.remove("sort-asc", "sort-desc");
    if (th.dataset.col === col) th.classList.add("sort-" + sortDir);
  });
  dados = [...dados].sort((a, b) => {
    const va = (a[col] || "").toString().toLowerCase();
    const vb = (b[col] || "").toString().toLowerCase();
    return sortDir === "asc" ? va.localeCompare(vb) : vb.localeCompare(va);
  });
  paginaAtual = 1;
  renderTabela();
}

function renderTabela() {
  const perPageEl = document.getElementById("perPage");
  if (perPageEl) itensPorPagina = parseInt(perPageEl.value) || 25;

  const tbody = document.getElementById("tbody");
  if (!tbody) return;

  const total = dados.length;
  const totalPaginas = Math.ceil(total / itensPorPagina);
  if (paginaAtual > totalPaginas) paginaAtual = 1;

  const inicio = (paginaAtual - 1) * itensPorPagina;
  const fim = Math.min(inicio + itensPorPagina, total);
  const pagina = dados.slice(inicio, fim);

  tbody.innerHTML = pagina.map((log, idx) => {
    const num = inicio + idx + 1;
    const badge = classeBadge(log.acao);
    const suspeito = isSuspeito(log.acao) ? "suspeito" : "";
    return `
      <tr class="${suspeito}" id="row-${inicio + idx}">
        <td>${num}</td>
        <td><span class="badge-acao ${badge}">${
      traduzirAcao(log.acao)
    }</span></td>
        <td>
          <div class="user-name">${log.emailUsuario || "—"}</div>
          <div class="user-email">ID: ${log.idUsuario || "—"}</div>
        </td>
        <td>${truncar(log.detalhes, 60)}</td>
        <td class="col-data">${formatarDataHora(log.dataHora)}</td>
        <td>
          <button class="btn-expandir" onclick="toggleDetalhe('det-${
      inicio + idx
    }')" title="Ver detalhes">
            <i class="fa-solid fa-chevron-down"></i>
          </button>
        </td>
      </tr>
      <tr class="detalhe-row" id="det-${inicio + idx}">
        <td colspan="6">
          <div class="detalhe-conteudo">${
      log.detalhes || "Sem detalhes registrados."
    }</div>
        </td>
      </tr>
    `;
  }).join("");

  renderPaginacao(total, totalPaginas, inicio, fim);
}

function truncar(str, max) {
  if (!str) return "—";
  return str.length > max ? str.substring(0, max) + "…" : str;
}

function toggleDetalhe(id) {
  const el = document.getElementById(id);
  if (!el) return;
  el.classList.toggle("visivel");
  const btn = el.previousElementSibling?.querySelector(".btn-expandir i");
  if (btn) {
    btn.className = el.classList.contains("visivel")
      ? "fa-solid fa-chevron-up"
      : "fa-solid fa-chevron-down";
  }
}

function renderPaginacao(total, totalPaginas, inicio, fim) {
  const info = document.getElementById("paginacaoInfo");
  const ctrl = document.getElementById("paginacaoControles");
  if (info) {
    info.textContent = `Mostrando ${inicio + 1}–${fim} de ${total} registros`;
  }
  if (!ctrl) return;

  let html = `<button class="btn-page" onclick="irPagina(${paginaAtual - 1})" ${
    paginaAtual === 1 ? "disabled" : ""
  }>‹</button>`;

  const maxBtns = 5;
  let start = Math.max(1, paginaAtual - 2);
  let end = Math.min(totalPaginas, start + maxBtns - 1);
  if (end - start < maxBtns - 1) start = Math.max(1, end - maxBtns + 1);

  for (let i = start; i <= end; i++) {
    html += `<button class="btn-page ${
      i === paginaAtual ? "ativa" : ""
    }" onclick="irPagina(${i})">${i}</button>`;
  }

  html += `<button class="btn-page" onclick="irPagina(${paginaAtual + 1})" ${
    paginaAtual === totalPaginas ? "disabled" : ""
  }>›</button>`;
  ctrl.innerHTML = html;
}

function irPagina(p) {
  const total = dados.length;
  const totalPaginas = Math.ceil(total / itensPorPagina);
  if (p < 1 || p > totalPaginas) return;
  paginaAtual = p;
  renderTabela();
  document.querySelector(".table-card")?.scrollIntoView({
    behavior: "smooth",
    block: "start",
  });
}

// ── Atalhos de data ──────────────────────────────────────────────
function atalho(tipo) {
  const hoje = new Date();
  const fmt = (d) => d.toISOString().split("T")[0];
  let ini = "", fim = fmt(hoje);

  document.querySelectorAll(".btn-shortcut").forEach((b) =>
    b.classList.remove("ativo")
  );
  event.target.classList.add("ativo");

  if (tipo === "hoje") {
    ini = fmt(hoje);
  } else if (tipo === "7dias") {
    const d = new Date(hoje);
    d.setDate(d.getDate() - 7);
    ini = fmt(d);
  } else if (tipo === "mes") {
    ini = fmt(new Date(hoje.getFullYear(), hoje.getMonth(), 1));
  } else {
    ini = "";
    fim = "";
  }

  document.getElementById("dataInicio").value = ini;
  document.getElementById("dataFim").value = fim;
}

function limparFiltros() {
  document.getElementById("dataInicio").value = "";
  document.getElementById("dataFim").value = "";
  document.getElementById("emailUsuario").value = "";
  document.getElementById("acaoFiltro").value = "";
  document.querySelectorAll(".btn-shortcut").forEach((b) =>
    b.classList.remove("ativo")
  );
  window.location.href = "/admin/audit";
}

// ── Exportação PDF (gerada no backend) ──────────────────────────
function exportarPDF() {
  const params = new URLSearchParams();
  const emailVal = document.getElementById("emailUsuario")?.value?.trim();
  const acaoVal = document.getElementById("acaoFiltro")?.value;
  const iniVal = document.getElementById("dataInicio")?.value;
  const fimVal = document.getElementById("dataFim")?.value;

  if (emailVal) params.set("emailUsuario", emailVal);
  if (acaoVal) params.set("acao", acaoVal);
  if (iniVal) params.set("dataInicio", iniVal);
  if (fimVal) params.set("dataFim", fimVal);

  const query = params.toString();
  window.location.href = "/admin/audit/exportar-pdf" +
    (query ? "?" + query : "");
}

// ── Inicializar ──────────────────────────────────────────────────
if (dados.length > 0) {
  dados = [...dados].sort((a, b) => {
    const va = (a.dataHora || "").toString();
    const vb = (b.dataHora || "").toString();
    return vb.localeCompare(va);
  });
  renderTabela();
}
