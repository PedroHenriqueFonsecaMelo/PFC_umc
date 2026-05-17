/* ════════════════════════════════════════
   LOTES
   ════════════════════════════════════════ */
const priceMap = { "NOVO": 50, "OTIMO": 40, "BOM": 30, "DESGASTADO": 20, "RUIM": 0 };
let livrosCache     = [];
let loteAtualMeta   = null; // { id, codigoProtocolo, nomeVendedor, emailVendedor, quantidadeLivros }
let todosLotes      = [];
let loteSearchTimer = null;

async function loadLotes() {
  // Oculta breadcrumb de lote ao voltar para a lista
  const bcLote = document.getElementById('breadcrumb-lote');
  if (bcLote) bcLote.style.display = 'none';

  const container = document.getElementById("contentArea");
  container.innerHTML = '<div class="loading">Carregando lotes pendentes...</div>';
  try {
    const res = await fetch("/api/admin/lotes/pendentes");
    todosLotes = await res.json();
    renderLotesTabela();
  } catch (e) {
    container.innerHTML = '<p style="color:#722F37;padding:1rem">Erro ao conectar com o servidor.</p>';
  }
}

function renderLotesTabela(filtro) {
  const container = document.getElementById("contentArea");

  let lotes = todosLotes;

  // Filtro de texto
  if (filtro && filtro.trim()) {
    const q = filtro.trim().toLowerCase();
    lotes = lotes.filter(l =>
      (l.codigoProtocolo || "").toLowerCase().includes(q) ||
      (l.nomeVendedor    || "").toLowerCase().includes(q) ||
      (l.emailVendedor   || "").toLowerCase().includes(q) ||
      String(l.id).includes(q)
    );
  }

  const totalPendentes = todosLotes.length;

  if (totalPendentes === 0) {
    container.innerHTML = `
      <div class="lotes-empty">
        <i class="fa-solid fa-box-open" style="font-size:2.5rem;color:#c5bfb7;margin-bottom:.75rem"></i>
        <h3 style="font-family:'Playfair Display',serif;font-size:1.1rem;color:#2c241b;margin:0 0 .35rem">Nenhum lote aguardando revisão</h3>
        <p style="font-size:.85rem;color:#7a6e65;margin:0">Todos os lotes foram avaliados. Novas submissões aparecerão aqui.</p>
      </div>`;
    return;
  }

  let html = `
    <div class="lotes-header-bar">
      <div>
        <span class="lotes-header-title">Lotes Pendentes</span>
        <span class="lotes-header-badge">${totalPendentes}</span>
      </div>
      <div class="lotes-search-wrap">
        <i class="fa-solid fa-magnifying-glass lotes-search-icon"></i>
        <input class="lotes-search-input" id="lotesSearchInput" type="text"
               placeholder="Buscar por protocolo, vendedor ou e-mail…"
               value="${esc(filtro || '')}"
               oninput="onLoteSearch(this.value)" />
      </div>
    </div>`;

  if (lotes.length === 0) {
    html += `<div style="padding:2rem;text-align:center;color:#7a6e65;font-size:.88rem">Nenhum lote corresponde à busca.</div>`;
    container.innerHTML = html;
    return;
  }

  html += `
    <table class="lotes-table">
      <thead>
        <tr>
          <th class="lotes-th">#</th>
          <th class="lotes-th">Protocolo</th>
          <th class="lotes-th">Vendedor</th>
          <th class="lotes-th">E-mail</th>
          <th class="lotes-th" style="text-align:center">Livros</th>
          <th class="lotes-th">Recebido em</th>
          <th class="lotes-th" style="text-align:center">Ação</th>
        </tr>
      </thead>
      <tbody>`;

  lotes.forEach(lote => {
    const data = lote.dataCriacao
      ? new Date(lote.dataCriacao).toLocaleDateString("pt-BR", { day: "2-digit", month: "short", year: "numeric" })
      : "—";
    html += `
      <tr class="lotes-row" id="lote-row-${lote.id}">
        <td class="lotes-td lotes-td-id">#${lote.id}</td>
        <td class="lotes-td"><span class="lote-protocolo-pill">${esc(lote.codigoProtocolo || "—")}</span></td>
        <td class="lotes-td lotes-td-vendedor">
          <span class="lotes-avatar">${esc((lote.nomeVendedor || "?").charAt(0).toUpperCase())}</span>
          ${esc(lote.nomeVendedor || "—")}
        </td>
        <td class="lotes-td lotes-td-email">${esc(lote.emailVendedor || "—")}</td>
        <td class="lotes-td" style="text-align:center">
          <span class="lotes-qtd-badge">${lote.quantidadeLivros}</span>
        </td>
        <td class="lotes-td lotes-td-data">${data}</td>
        <td class="lotes-td" style="text-align:center">
          <button class="btn-auditar-lote" onclick="loadLivrosLote(${lote.id})">
            <i class="fa-solid fa-magnifying-glass"></i> Auditar
          </button>
        </td>
      </tr>`;
  });

  html += `</tbody></table>`;
  container.innerHTML = html;
}

function onLoteSearch(val) {
  clearTimeout(loteSearchTimer);
  loteSearchTimer = setTimeout(() => renderLotesTabela(val), 220);
}

function avatarInicial(nome) {
  return (nome || "?").charAt(0).toUpperCase();
}

function esc(str) {
  return String(str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

async function loadLivrosLote(loteId) {
  const container = document.getElementById("contentArea");
  container.innerHTML = '<div class="loading">Carregando livros do lote...</div>';
  try {
    const res = await fetch("/api/admin/lotes/" + loteId + "/detalhes");
    const data = await res.json();
    loteAtualMeta = {
      id:              data.id,
      codigoProtocolo: data.codigoProtocolo,
      nomeVendedor:    data.nomeVendedor,
      emailVendedor:   data.emailVendedor,
      quantidadeLivros: data.quantidadeLivros,
    };
    livrosCache = data.livros || [];

    // Exibe e atualiza breadcrumb de lote
    const bcLote = document.getElementById('breadcrumb-lote');
    const bcrNum = document.getElementById('bcrLoteNumero');
    if (bcLote) bcLote.style.display = 'block';
    if (bcrNum) bcrNum.textContent = 'Lote #' + (data.codigoProtocolo || data.id);

    renderLivros();
  } catch (e) {
    container.innerHTML = '<p style="color:#722F37;padding:1rem">Erro ao carregar livros do lote.</p>';
  }
}

const ESTADO_BADGE = {
  NOVO:       { label: "Novo",       bg: "rgba(74,93,35,.12)",    color: "#4a5d23" },
  OTIMO:      { label: "Ótimo",      bg: "rgba(74,93,35,.12)",    color: "#4a5d23" },
  BOM:        { label: "Bom",        bg: "rgba(37,99,235,.1)",    color: "#1d4ed8" },
  DESGASTADO: { label: "Desgastado", bg: "rgba(217,119,6,.12)",   color: "#b45309" },
  RUIM:       { label: "Ruim",       bg: "rgba(185,28,28,.1)",    color: "#b91c1c" },
};

function renderLivros() {
  garantirModalRejeicao();
  const container = document.getElementById("contentArea");

  const total      = loteAtualMeta ? loteAtualMeta.quantidadeLivros : livrosCache.length;
  const restantes  = livrosCache.length;
  const revisados  = total - restantes;
  const pct        = total > 0 ? Math.round((revisados / total) * 100) : 0;

  const vendedorHtml = loteAtualMeta ? `
    <div class="audit-vendedor-bar">
      <div class="audit-vendedor-info">
        <span class="audit-vendedor-avatar">${avatarInicial(loteAtualMeta.nomeVendedor)}</span>
        <div>
          <div class="audit-vendedor-nome">${esc(loteAtualMeta.nomeVendedor)}</div>
          <div class="audit-vendedor-email">${esc(loteAtualMeta.emailVendedor)}</div>
        </div>
      </div>
      <div class="audit-protocolo">
        <i class="fa-solid fa-hashtag"></i> ${esc(loteAtualMeta.codigoProtocolo)}
      </div>
    </div>
    <div class="audit-progress-wrap">
      <div class="audit-progress-labels">
        <span>${revisados} de ${total} livros revisados</span>
        <span>${pct}%</span>
      </div>
      <div class="audit-progress-track">
        <div class="audit-progress-fill" style="width:${pct}%"></div>
      </div>
    </div>` : "";

  let html = `
    <div class="audit-topbar">
      <button class="btn-voltar-lotes" onclick="loadLotes()">
        <i class="fa-solid fa-arrow-left"></i> Voltar aos Lotes
      </button>
      <h3 class="audit-title">
        Auditoria do Lote
        <span class="audit-count">${restantes} livro(s) pendente(s)</span>
      </h3>
    </div>
    ${vendedorHtml}
    <div class="book-grid">`;

  livrosCache.forEach((b, bookIdx) => {
    const fotos = JSON.parse(b.fotosUrls || "[]");
    const estadoAtual = (b.estadoFisico || "BOM").toUpperCase();
    const badge = ESTADO_BADGE[estadoAtual] || ESTADO_BADGE.BOM;

    let fotosHtml = `<div class="fotos-wrapper">`;
    fotos.forEach((url, fotoIdx) => {
      fotosHtml += `
        <div class="foto-item">
          <img src="${url}" alt="Foto ${fotoIdx + 1}" style="width:88px;height:88px;object-fit:cover;border-radius:2px;border:1px solid rgba(44,36,27,.12);cursor:pointer" onclick="ampliarFoto('${url}')">
          <button class="btn-del-foto" onclick="removerFoto(${bookIdx},${fotoIdx})" title="Remover foto">×</button>
        </div>`;
    });
    fotosHtml += `
      <label class="btn-add-foto" for="upload-${bookIdx}" title="Adicionar foto">
        <i class="fa-solid fa-plus" style="font-size:.85rem"></i>
      </label>
      <input type="file" id="upload-${bookIdx}" style="display:none" accept="image/*" onchange="subirFoto(event,${bookIdx})">
    </div>`;

    html += `
    <div class="bcard audit-card" id="card-${b.id}">
      ${fotosHtml}

      <div class="audit-info">
        <div style="display:flex;align-items:flex-start;justify-content:space-between;gap:.5rem;margin-bottom:.6rem">
          <h3 style="font-family:'Playfair Display',serif;font-size:1.05rem;font-weight:700;color:#2c241b;line-height:1.25;margin:0;flex:1">${b.titulo}</h3>
          <span style="flex-shrink:0;font-size:.62rem;font-weight:700;letter-spacing:.07em;text-transform:uppercase;padding:.2rem .65rem;border-radius:999px;background:${badge.bg};color:${badge.color};border:1px solid ${badge.color}22;white-space:nowrap">${badge.label}</span>
        </div>
        <p style="font-family:'DM Sans',sans-serif;font-size:.82rem;font-style:italic;color:#7a6e65;margin:0 0 .3rem">por ${b.autor}</p>
        ${b.isbn ? `<p style="font-family:'Courier New',monospace;font-size:.72rem;color:rgba(44,36,27,.45);margin:0">ISBN ${b.isbn}</p>` : ""}
      </div>

      <div class="audit-divider"></div>

      <div class="audit-avaliacao">
        <label style="font-size:.62rem;font-weight:700;text-transform:uppercase;letter-spacing:.1em;color:rgba(74,93,35,.7);display:block;margin-bottom:.4rem">Estado físico avaliado</label>
        <select id="sel-${b.id}" onchange="updatePrice(${b.id})" style="width:100%;margin:0;padding:.55rem .75rem;font-size:.85rem;font-family:'DM Sans',sans-serif;border:1.5px solid rgba(74,93,35,.25);border-radius:2px;background:#f9f6f0;color:#2c241b;cursor:pointer">
          <option value="NOVO">Novo</option>
          <option value="OTIMO">Ótimo</option>
          <option value="BOM" selected>Bom</option>
          <option value="DESGASTADO">Desgastado</option>
          <option value="RUIM">Ruim (Inviável)</option>
        </select>
        <div class="price-display" id="price-${b.id}" style="font-size:.9rem;font-weight:700;color:#4a5d23;background:rgba(74,93,35,.07);border:1px solid rgba(74,93,35,.15);padding:.4rem .75rem;margin-top:.5rem;text-align:center;border-radius:2px">Sugestão: T$ 30</div>
      </div>

      <div class="audit-divider"></div>

      <div class="audit-actions">
        <button class="btn-publicar-livro" onclick="finalizarAprovacao(${bookIdx})">
          <i class="fa-solid fa-check"></i> Confirmar e Publicar
        </button>
        <button class="btn-rejeitar-livro" onclick="abrirModalRejeicao(${b.id}, ${bookIdx})">
          <i class="fa-solid fa-xmark"></i> Rejeitar este Livro
        </button>
      </div>
    </div>`;
  });

  html += `</div>`;
  container.innerHTML = html;
}

function ampliarFoto(url) {
  const overlay = document.createElement("div");
  overlay.style.cssText = "position:fixed;inset:0;background:rgba(0,0,0,.82);z-index:10000;display:flex;align-items:center;justify-content:center;cursor:zoom-out";
  overlay.innerHTML = `<img src="${url}" style="max-width:90vw;max-height:90vh;object-fit:contain;border:2px solid rgba(255,255,255,.15)">`;
  overlay.addEventListener("click", () => overlay.remove());
  document.body.appendChild(overlay);
}

/* ── Modal de Rejeição ── */
function garantirModalRejeicao() {
  if (document.getElementById("modalRejeicao")) return;
  const el = document.createElement("div");
  el.id = "modalRejeicao";
  el.style.cssText = "display:none;position:fixed;inset:0;background:rgba(44,36,27,.55);z-index:9998;align-items:center;justify-content:center;padding:1rem";
  el.innerHTML = `
    <div style="background:#fff;border:1.5px solid rgba(44,36,27,.12);border-top:3px solid #722f37;padding:2rem;max-width:480px;width:100%;position:relative;box-shadow:0 12px 40px rgba(44,36,27,.2)">
      <div style="display:flex;align-items:center;gap:.75rem;margin-bottom:1.25rem">
        <span style="width:2rem;height:2rem;border-radius:50%;background:rgba(185,28,28,.1);color:#b91c1c;display:flex;align-items:center;justify-content:center;font-size:.85rem;flex-shrink:0"><i class="fa-solid fa-xmark"></i></span>
        <div>
          <div style="font-family:'Playfair Display',serif;font-size:1.05rem;font-weight:700;color:#2c241b">Rejeitar Livro</div>
          <div id="modalRejeicaoSubtitle" style="font-size:.78rem;color:#7a6e65;margin-top:.1rem"></div>
        </div>
      </div>
      <label style="font-size:.63rem;font-weight:700;text-transform:uppercase;letter-spacing:.1em;color:#7a6e65;display:block;margin-bottom:.4rem">Motivo da rejeição <span style="color:#b91c1c">*</span></label>
      <textarea id="modalRejeicaoMotivo" rows="4" placeholder="Descreva o motivo da rejeição (será enviado por e-mail ao vendedor)…"
        style="width:100%;border:1.5px solid rgba(44,36,27,.18);border-radius:2px;padding:.65rem .85rem;font-size:.875rem;font-family:'DM Sans',sans-serif;background:#f9f6f0;color:#2c241b;resize:vertical;outline:none;margin:0"></textarea>
      <div id="modalRejeicaoErro" style="display:none;font-size:.78rem;color:#b91c1c;margin-top:.35rem"><i class="fa-solid fa-circle-exclamation"></i> O motivo é obrigatório.</div>
      <div style="display:flex;gap:.75rem;margin-top:1.25rem">
        <button onclick="fecharModalRejeicao()" style="flex:1;padding:.65rem;background:#fff;border:1.5px solid rgba(44,36,27,.2);color:#7a6e65;font-size:.78rem;font-weight:600;letter-spacing:.07em;text-transform:uppercase;cursor:pointer;border-radius:2px">Cancelar</button>
        <button id="btnConfirmarRejeicao" onclick="confirmarRejeicao()" style="flex:1;padding:.65rem;background:#b91c1c;border:2px solid #b91c1c;color:#fff;font-size:.78rem;font-weight:700;letter-spacing:.07em;text-transform:uppercase;cursor:pointer;border-radius:2px">
          <i class="fa-solid fa-xmark"></i> Confirmar Rejeição
        </button>
      </div>
    </div>`;
  document.body.appendChild(el);
}

let _rejeicaoPendente = null;

function abrirModalRejeicao(livroId, bookIdx) {
  const livro = livrosCache[bookIdx];
  _rejeicaoPendente = { livroId, bookIdx };
  document.getElementById("modalRejeicaoSubtitle").textContent = livro ? `"${livro.titulo}"` : "";
  document.getElementById("modalRejeicaoMotivo").value = "";
  document.getElementById("modalRejeicaoErro").style.display = "none";
  const modal = document.getElementById("modalRejeicao");
  modal.style.display = "flex";
  setTimeout(() => document.getElementById("modalRejeicaoMotivo").focus(), 80);
}

function fecharModalRejeicao() {
  document.getElementById("modalRejeicao").style.display = "none";
  _rejeicaoPendente = null;
}

async function confirmarRejeicao() {
  const motivo = document.getElementById("modalRejeicaoMotivo").value.trim();
  if (!motivo) {
    document.getElementById("modalRejeicaoErro").style.display = "block";
    return;
  }
  document.getElementById("modalRejeicaoErro").style.display = "none";

  const { livroId, bookIdx } = _rejeicaoPendente;
  const estado = document.getElementById(`sel-${livroId}`) ? document.getElementById(`sel-${livroId}`).value : "BOM";

  const btn = document.getElementById("btnConfirmarRejeicao");
  btn.disabled = true;
  btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Rejeitando…';

  const res = await fetch(`/api/admin/livros/${livroId}/rejeitar`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ estado, comentario: motivo }),
  });

  btn.disabled = false;
  btn.innerHTML = '<i class="fa-solid fa-xmark"></i> Confirmar Rejeição';
  fecharModalRejeicao();

  if (res.ok) {
    mostrarToast("Livro rejeitado. E-mail de notificação enviado ao vendedor.", "aviso");
    livrosCache.splice(bookIdx, 1);
    if (livrosCache.length === 0) loadLotes();
    else renderLivros();
  } else {
    const t = await res.text();
    mostrarToast("Erro: " + t, "erro");
  }
}

function updatePrice(id) {
  const val = document.getElementById(`sel-${id}`).value;
  document.getElementById(`price-${id}`).innerText = `Sugestão: T$ ${priceMap[val]}`;
}

function removerFoto(bookIdx, fotoIdx) {
  let fotos = JSON.parse(livrosCache[bookIdx].fotosUrls);
  fotos.splice(fotoIdx, 1);
  livrosCache[bookIdx].fotosUrls = JSON.stringify(fotos);
  renderLivros();
}

function subirFoto(event, bookIdx) {
  const file = event.target.files[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = function (e) {
    let fotos = JSON.parse(livrosCache[bookIdx].fotosUrls || "[]");
    fotos.push(e.target.result);
    livrosCache[bookIdx].fotosUrls = JSON.stringify(fotos);
    renderLivros();
  };
  reader.readAsDataURL(file);
}

async function finalizarAprovacao(bookIdx) {
  const livro = livrosCache[bookIdx];
  const estado = document.getElementById(`sel-${livro.id}`).value;
  const payload = {
    estadoAprovado: estado,
    comentario: "Aprovado via Painel Admin",
    fotosUrls: livro.fotosUrls,
  };
  const res = await fetch(`/api/admin/livros/${livro.id}/aprovar`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  if (res.ok) {
    mostrarToast("✅ Livro publicado com sucesso!", "sucesso");
    livrosCache.splice(bookIdx, 1);
    if (livrosCache.length === 0) loadLotes();
    else renderLivros();
  } else mostrarToast("❌ Erro ao aprovar livro.", "erro");
}


/* ════════════════════════════════════════
   PEDIDOS
   ════════════════════════════════════════ */
let todosPedidos = [];
let filtroAtual = "TODOS";

async function carregarPedidos() {
  const lista = document.getElementById("listaPedidos");
  lista.innerHTML = '<div class="skel"></div><div class="skel"></div><div class="skel"></div>';
  try {
    const res = await fetch("/api/admin/pedidos");
    if (!res.ok) throw new Error();
    todosPedidos = await res.json();

    const pendentes = todosPedidos.filter((p) => p.statusEnvio === "AGUARDANDO_ENVIO").length;
    const badge = document.getElementById("badgePedidos");
    if (badge) {
      if (pendentes > 0) {
        badge.textContent = pendentes;
        badge.style.display = "inline";
      } else {
        badge.style.display = "none";
      }
    }

    renderPedidos();
  } catch (e) {
    lista.innerHTML = '<p style="text-align:center;color:#722F37;padding:2rem">Erro ao carregar pedidos.</p>';
  }
}

function filtrarPedidos(status, btn) {
  filtroAtual = status;
  document.querySelectorAll(".filter-btn").forEach((b) => b.classList.remove("active"));
  btn.classList.add("active");
  renderPedidos();
}

function renderPedidos() {
  const lista = document.getElementById("listaPedidos");
  const busca = (document.getElementById("buscaPedido").value || "").toLowerCase();

  let pedidos = todosPedidos;
  if (filtroAtual !== "TODOS") {
    pedidos = pedidos.filter((p) => p.statusEnvio === filtroAtual);
  }
  if (busca) {
    pedidos = pedidos.filter((p) =>
      p.tituloLivro.toLowerCase().includes(busca) ||
      (p.compradorNome || "").toLowerCase().includes(busca) ||
      (p.compradorEmail || "").toLowerCase().includes(busca) ||
      String(p.id).includes(busca)
    );
  }

  if (pedidos.length === 0) {
    lista.innerHTML = `<div style="text-align:center;padding:3rem;color:#7A6E65">
      <div style="font-size:2.5rem;margin-bottom:.75rem">📭</div>
      <p style="font-style:italic">Nenhum pedido encontrado para este filtro.</p>
    </div>`;
    return;
  }

  lista.innerHTML = pedidos.map((p) => buildPedidoRow(p)).join("");
}

const PROXIMOS_STATUS = {
  "AGUARDANDO_ENVIO": ["EM_TRANSITO", "CANCELADO"],
  "EM_TRANSITO": ["ENTREGUE", "CANCELADO"],
  "ENTREGUE": [],
  "CANCELADO": [],
};
const LABEL_STATUS = {
  "AGUARDANDO_ENVIO": "Aguardando Envio",
  "EM_TRANSITO": "Em Trânsito",
  "ENTREGUE": "Entregue",
  "CANCELADO": "Cancelado",
};

function buildPedidoRow(p) {
  let foto = "https://via.placeholder.com/52x70?text=📚";
  try {
    const arr = JSON.parse(p.fotosUrls || "[]");
    if (arr.length > 0) foto = arr[0];
  } catch (_) {}

  const dataCompra = p.dataCompra
    ? new Date(p.dataCompra).toLocaleDateString("pt-BR", { day: "2-digit", month: "short", year: "numeric" })
    : "—";

  const proximos = PROXIMOS_STATUS[p.statusEnvio] || [];
  const podeAtualizar = proximos.length > 0;

  const acoesHtml = podeAtualizar
    ? `<div style="display:flex;flex-direction:column;gap:.4rem;max-width:460px">
        <input class="rastreio-input" id="rastreio-${p.id}" type="text"
               placeholder="Cód. rastreio (opcional)" value="${p.codigoRastreio || ""}"
               style="width:100%" />
        <div style="display:flex;gap:.4rem;align-items:center">
          <select class="select-status" id="status-${p.id}" style="flex:1;margin:0">
            ${proximos.map((s) => `<option value="${s}">${LABEL_STATUS[s]}</option>`).join("")}
          </select>
          <button class="btn-salvar-envio" onclick="salvarEnvio(${p.id})" style="white-space:nowrap;flex-shrink:0">
            <i class="fa-solid fa-floppy-disk"></i> Atualizar
          </button>
        </div>
      </div>`
    : `<span style="font-size:.75rem;color:#7A6E65;font-style:italic">Status final — sem ações disponíveis</span>`;

  const rastreioInfo = p.codigoRastreio && p.statusEnvio === "EM_TRANSITO"
    ? `<div style="font-size:.75rem;color:#4A5D23;font-family:monospace;background:rgba(74,93,35,.08);padding:.2rem .6rem;border:1px solid rgba(74,93,35,.2);display:inline-flex;align-items:center;gap:.35rem;margin-top:.35rem">
        <i class="fa-solid fa-truck-fast"></i>${p.codigoRastreio}
      </div>`
    : "";

  return `
  <div class="pedido-row" id="row-${p.id}">
    <img class="pedido-capa" src="${foto}" onerror="this.src='https://via.placeholder.com/52x70?text=📚'"/>
    <div style="flex:1;min-width:0">
      <div style="display:flex;justify-content:space-between;align-items:flex-start;gap:.5rem;flex-wrap:wrap">
        <div>
          <div style="font-weight:700;color:#2C241B">${p.tituloLivro}</div>
          <div style="font-size:.75rem;color:#7A6E65">${p.autorLivro}</div>
        </div>
        <span class="status-pill s-${p.statusEnvio}">${p.statusEnvioDescricao}</span>
      </div>
      <div style="display:flex;align-items:center;gap:.5rem;font-size:.75rem;color:#7A6E65;margin-top:.35rem;flex-wrap:wrap">
        <span>Pedido <strong style="color:#2C241B">#${p.id}</strong></span>
        <span>·</span>
        <span>📅 ${dataCompra}</span>
        <span>·</span>
        <span class="pedido-preco">T$ ${p.precoLivro.toFixed(2)}</span>
        ${p.compradorNome ? `<span>·</span><span>👤 ${p.compradorNome}</span>` : ""}
        ${p.compradorEmail ? `<span style="font-family:monospace">(${p.compradorEmail})</span>` : ""}
      </div>
      ${p.compradorEndereco ? `
      <div style="display:inline-flex;align-items:center;gap:.4rem;font-size:.75rem;color:#4A5D23;background:rgba(74,93,35,.07);border:1px solid rgba(74,93,35,.2);padding:.25rem .65rem;margin-top:.35rem;max-width:100%">
        <i class="fa-solid fa-location-dot" style="flex-shrink:0"></i>
        <span style="overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${p.compradorEndereco}</span>
      </div>` : `
      <div style="display:inline-flex;align-items:center;gap:.4rem;font-size:.75rem;color:#9C968F;background:#F5F2ED;border:1px solid #E0D9D0;padding:.25rem .65rem;margin-top:.35rem">
        <i class="fa-solid fa-location-dot" style="flex-shrink:0"></i>
        <span>Endereço não cadastrado</span>
      </div>`}
      ${rastreioInfo}
      <div style="margin-top:.6rem">${acoesHtml}</div>
    </div>
  </div>`;
}

async function salvarEnvio(pedidoId) {
  const novoStatus = document.getElementById(`status-${pedidoId}`).value;
  const codigoRastreio = document.getElementById(`rastreio-${pedidoId}`).value.trim();

  if (novoStatus === "CANCELADO") {
    if (!confirm("Tem certeza que deseja CANCELAR este pedido? O valor será estornado ao comprador. Esta operação não pode ser desfeita.")) {
      return;
    }
  }

  const btn = document.querySelector(`#row-${pedidoId} .btn-salvar-envio`);
  btn.disabled = true;
  btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Salvando...';

  try {
    const res = await fetch(`/api/admin/pedidos/${pedidoId}/envio`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ statusEnvio: novoStatus, codigoRastreio: codigoRastreio || null }),
    });

    if (!res.ok) throw new Error(await res.text());
    const atualizado = await res.json();

    const idx = todosPedidos.findIndex((p) => p.id === pedidoId);
    if (idx !== -1) todosPedidos[idx] = atualizado;

    if (novoStatus === "CANCELADO" && atualizado.saldoAposEstorno != null) {
      mostrarToast(
        `✅ Pedido #${pedidoId} cancelado — T$ ${atualizado.precoLivro.toFixed(2)} estornados. ` +
        `Novo saldo do comprador: T$ ${atualizado.saldoAposEstorno.toFixed(2)}`,
        "sucesso",
      );
    } else {
      mostrarToast(`✅ Pedido #${pedidoId} atualizado para "${LABEL_STATUS[novoStatus]}"`, "sucesso");
    }

    renderPedidos();
  } catch (e) {
    mostrarToast("❌ Erro: " + e.message, "erro");
    btn.disabled = false;
    btn.innerHTML = '<i class="fa-solid fa-floppy-disk"></i> Salvar';
  }
}

/* ════════════════════════════════════════
   TABS — usa URLSearchParams
   ════════════════════════════════════════ */
function trocarTab(nome) {
  document.querySelectorAll(".tab-panel").forEach((p) => p.classList.remove("active"));
  document.querySelectorAll(".tab-btn").forEach((b) => b.classList.remove("active"));
  const panel = document.getElementById("panel-" + nome);
  if (panel) panel.classList.add("active");
  const btn = document.querySelector(`.tab-btn[data-tab="${nome}"]`);
  if (btn) btn.classList.add("active");
  if (nome === "lotes")   loadLotes();
  if (nome === "pedidos") carregarPedidos();
  if (nome === "blog")    carregarBlogAdmin();

  document.querySelectorAll(".sidebar .nav-item").forEach(a => a.classList.remove("ativo"));
  const href = nome === "lotes" ? "/admin/painel" : `/admin/painel?tab=${nome}`;
  const sidebarLink = document.querySelector(`.sidebar .nav-item[href="${href}"]`);
  if (sidebarLink) sidebarLink.classList.add("ativo");
}

document.getElementById("dataHoje").textContent = new Date().toLocaleDateString("pt-BR", {
  weekday: "long", day: "numeric", month: "long", year: "numeric",
});

/* ════════════════════════════════════════
   TOAST
   ════════════════════════════════════════ */
function mostrarToast(msg, tipo) {
  const cores = {
    sucesso: "background:#dcfce7;color:#15803d;border:1px solid #bbf7d0",
    erro: "background:#fee2e2;color:#b91c1c;border:1px solid #fecaca",
    aviso: "background:#fef9c3;color:#a16207;border:1px solid #fef08a",
  };
  const el = document.createElement("div");
  el.style.cssText = `
    position:fixed;bottom:1.5rem;left:50%;transform:translateX(-50%);
    padding:.75rem 1.5rem;border-radius:2px;font-size:.875rem;font-weight:500;
    z-index:9999;box-shadow:4px 4px 0 rgba(44,36,27,.15);
    ${cores[tipo] || cores.aviso}`;
  el.textContent = msg;
  document.body.appendChild(el);
  setTimeout(() => el.remove(), 4000);
}

/* ════════════════════════════════════════
   BLOG
   ════════════════════════════════════════ */
let postsCache = [];

async function carregarBlogAdmin() {
  const lista = document.getElementById("listaBlog");
  try {
    const res = await fetch("/api/admin/blog");
    postsCache = await res.json();

    if (postsCache.length === 0) {
      lista.innerHTML = '<p style="font-size:.875rem;color:#7A6E65;padding:1rem">Nenhum post publicado ainda.</p>';
      return;
    }

    lista.innerHTML = postsCache.map((p) => `
    <div class="blog-post-item" data-post-id="${p.id}">
      ${p.imagemUrl
        ? `<img src="${p.imagemUrl}" alt="" class="blog-post-img"/>`
        : '<div class="blog-post-img" style="display:flex;align-items:center;justify-content:center;font-size:1.5rem">📝</div>'}
      <div style="flex:1;min-width:0">
        <div style="font-weight:700;color:#2C241B;font-size:.9rem;margin-bottom:.25rem">${escBlog(p.titulo)}</div>
        <div style="font-size:.75rem;color:#7A6E65;margin-bottom:.5rem">Por ${escBlog(p.autorNome)} · ${p.dataPublicacao} · <span style="font-weight:600">${p.status}</span></div>
        <div style="font-size:.85rem;color:#7A6E65;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical">${escBlog(p.conteudo)}</div>
      </div>
      <div style="display:flex;flex-direction:column;gap:.35rem;flex-shrink:0">
        <button onclick="blogAbrirEdicao(${p.id})" class="btn-editar-post" title="Editar post">
          <i class="fa-solid fa-pen-to-square"></i>
        </button>
        <button onclick="blogConfirmarDel(${p.id}, this)" class="btn-remover-post" title="Remover post">
          <i class="fa-solid fa-trash-can"></i>
        </button>
      </div>
    </div>`).join("");
  } catch (e) {
    lista.innerHTML = '<p style="font-size:.875rem;color:#722F37">Erro ao carregar posts.</p>';
  }
}

function escBlog(str) {
  return String(str || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
}

function garantirModalEditarBlog() {
  if (document.getElementById("modalEditarBlog")) return;
  const overlay = document.createElement("div");
  overlay.id = "modalEditarBlog";
  overlay.style.cssText = "display:none;position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:1000;align-items:center;justify-content:center";
  overlay.innerHTML = `
    <div style="background:#fff;border-radius:2px;width:min(560px,95vw);max-height:90vh;overflow-y:auto;padding:2rem;box-shadow:0 8px 32px rgba(0,0,0,.18);position:relative">
      <button onclick="fecharModalEditarBlog()" style="position:absolute;top:1rem;right:1rem;background:none;border:none;cursor:pointer;font-size:1.1rem;color:#7A6E65">✕</button>
      <h3 style="font-family:'Playfair Display',serif;font-size:1.15rem;margin-bottom:1.25rem;color:#2C241B">Editar Post</h3>
      <form id="formEditarBlog" onsubmit="salvarEdicaoBlog(event)">
        <input type="hidden" id="editBlogId"/>
        <div style="margin-bottom:1rem">
          <label style="display:block;font-size:.8rem;font-weight:600;margin-bottom:.35rem;color:#2C241B">Título</label>
          <input id="editBlogTitulo" type="text" required
            style="width:100%;padding:.6rem .75rem;border:1px solid rgba(44,36,27,.2);border-radius:2px;font-size:.9rem;box-sizing:border-box"/>
        </div>
        <div style="margin-bottom:1rem">
          <label style="display:block;font-size:.8rem;font-weight:600;margin-bottom:.35rem;color:#2C241B">Conteúdo</label>
          <textarea id="editBlogConteudo" rows="8" required
            style="width:100%;padding:.6rem .75rem;border:1px solid rgba(44,36,27,.2);border-radius:2px;font-size:.875rem;resize:vertical;box-sizing:border-box"></textarea>
        </div>
        <div style="margin-bottom:1rem">
          <label style="display:block;font-size:.8rem;font-weight:600;margin-bottom:.35rem;color:#2C241B">Imagem atual</label>
          <div id="editBlogImgAtual" style="margin-bottom:.5rem"></div>
          <label style="display:block;font-size:.8rem;font-weight:600;margin-bottom:.35rem;color:#2C241B">Nova imagem (opcional)</label>
          <input id="editBlogImagem" type="file" accept="image/*" style="font-size:.85rem"/>
        </div>
        <div style="display:flex;gap:.75rem;justify-content:flex-end;margin-top:1.5rem">
          <button type="button" onclick="fecharModalEditarBlog()"
            style="padding:.55rem 1.25rem;border:1px solid rgba(44,36,27,.2);background:none;border-radius:2px;cursor:pointer;font-size:.85rem">Cancelar</button>
          <button type="submit" id="btnSalvarEdicaoBlog"
            style="padding:.55rem 1.5rem;background:#722F37;color:#fff;border:none;border-radius:2px;cursor:pointer;font-size:.85rem;font-weight:600">Salvar</button>
        </div>
      </form>
    </div>`;
  overlay.addEventListener("click", (e) => { if (e.target === overlay) fecharModalEditarBlog(); });
  document.body.appendChild(overlay);
}

function blogAbrirEdicao(id) {
  garantirModalEditarBlog();
  const post = postsCache.find(p => p.id === id);
  if (!post) return;
  document.getElementById("editBlogId").value = id;
  document.getElementById("editBlogTitulo").value = post.titulo;
  document.getElementById("editBlogConteudo").value = post.conteudo;
  const imgAtual = document.getElementById("editBlogImgAtual");
  imgAtual.innerHTML = post.imagemUrl
    ? `<img src="${post.imagemUrl}" style="max-height:120px;border:1px solid rgba(44,36,27,.15);display:block"/>`
    : '<span style="font-size:.8rem;color:#7A6E65">Nenhuma imagem</span>';
  document.getElementById("editBlogImagem").value = "";
  const modal = document.getElementById("modalEditarBlog");
  modal.style.display = "flex";
}

function fecharModalEditarBlog() {
  const modal = document.getElementById("modalEditarBlog");
  if (modal) modal.style.display = "none";
}

async function salvarEdicaoBlog(e) {
  e.preventDefault();
  const id = document.getElementById("editBlogId").value;
  const btn = document.getElementById("btnSalvarEdicaoBlog");
  btn.disabled = true;
  btn.textContent = "Salvando…";

  const formData = new FormData();
  formData.append("titulo", document.getElementById("editBlogTitulo").value);
  formData.append("conteudo", document.getElementById("editBlogConteudo").value);
  const img = document.getElementById("editBlogImagem").files[0];
  if (img) formData.append("imagem", img);

  try {
    const res = await fetch("/api/blog/" + id, { method: "PUT", body: formData, credentials: "include" });
    if (!res.ok) throw new Error();
    fecharModalEditarBlog();
    mostrarToast("Post atualizado com sucesso!", "sucesso");
    carregarBlogAdmin();
  } catch (_) {
    mostrarToast("Erro ao salvar edição.", "erro");
  } finally {
    btn.disabled = false;
    btn.textContent = "Salvar";
  }
}

async function publicarPost(e) {
  e.preventDefault();
  const form = document.getElementById("formBlog");
  const msg = document.getElementById("blogMsg");
  const data = new FormData(form);

  try {
    const res = await fetch("/api/blog", { method: "POST", body: data, credentials: "include" });
    if (!res.ok) throw new Error();
    msg.textContent = "Post publicado!";
    msg.style.display = "inline";
    form.reset();
    document.getElementById("blogImgPreview").innerHTML = "";
    setTimeout(() => { msg.style.display = "none"; }, 3000);
    carregarBlogAdmin();
    mostrarToast("Post publicado com sucesso!", "sucesso");
  } catch (_) {
    mostrarToast("Erro ao publicar post.", "erro");
  }
}

function blogCancelarDel(naoBtn) {
  const item = naoBtn.closest('.blog-post-item');
  naoBtn.closest('.blog-confirm-del').remove();
  item.querySelector('.btn-remover-post').style.display = '';
}

function blogConfirmarDel(id, btn) {
  const item = btn.closest('.blog-post-item');
  btn.style.display = 'none';
  const div = document.createElement('div');
  div.className = 'blog-confirm-del';
  div.innerHTML = `
    <span>Remover?</span>
    <button class="blog-confirm-sim" onclick="deletarPost(${id})">Sim</button>
    <button class="blog-confirm-nao" onclick="blogCancelarDel(this)">✕</button>
  `;
  item.appendChild(div);
}

async function deletarPost(id) {
  try {
    const res = await fetch("/api/blog/" + id, { method: "DELETE", credentials: "include" });
    if (!res.ok) throw new Error();
    mostrarToast("Post removido.", "sucesso");
    carregarBlogAdmin();
  } catch (_) {
    mostrarToast("Erro ao remover post.", "erro");
  }
}

/* ════════════════════════════════════════
   INIT
   ════════════════════════════════════════ */
document.addEventListener("DOMContentLoaded", () => {
  const imgInput = document.getElementById("blogImagem");
  if (imgInput) {
    imgInput.addEventListener("change", () => {
      const preview = document.getElementById("blogImgPreview");
      const file = imgInput.files[0];
      if (file) {
        const url = URL.createObjectURL(file);
        preview.innerHTML = `<img src="${url}" style="margin-top:.5rem;max-height:160px;border:1px solid rgba(44,36,27,.15)"/>`;
      } else {
        preview.innerHTML = "";
      }
    });
  }

  const params = new URLSearchParams(window.location.search);
  const tab = params.get("tab") || "lotes";
  trocarTab(tab);
});
