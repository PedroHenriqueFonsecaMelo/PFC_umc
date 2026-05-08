/* ════════════════════════════════════════
   LOTES
   ════════════════════════════════════════ */
const priceMap = { "NOVO": 50, "OTIMO": 40, "BOM": 30, "DESGASTADO": 20, "RUIM": 0 };
let livrosCache = [];

async function loadLotes() {
  const container = document.getElementById("contentArea");
  container.innerHTML = '<div class="loading">Carregando lotes pendentes...</div>';
  try {
    const res = await fetch("/api/admin/lotes/pendentes");
    const lotes = await res.json();
    if (lotes.length === 0) {
      container.innerHTML = '<div class="bcard"><h3>Nenhum lote pendente para avaliação no momento.</h3></div>';
      return;
    }
    let html = '<h3 style="font-family:\'Playfair Display\',serif;font-size:1.1rem;margin-bottom:.75rem">Lotes Aguardando Revisão</h3><div class="book-grid">';
    lotes.forEach((lote) => {
      html += `
      <div class="bcard">
        <div style="color:#7A6E65;font-size:12px;">Protocolo: ${lote.codigoProtocolo}</div>
        <h3 style="margin:10px 0;">Lote #${lote.id}</h3>
        <p><strong>Data de Envio:</strong> ${new Date(lote.dataCriacao).toLocaleDateString("pt-BR")}</p>
        <button class="btn-aprovar" onclick="loadLivrosLote(${lote.id})">Abrir Lote para Auditoria</button>
      </div>`;
    });
    html += "</div>";
    container.innerHTML = html;
  } catch (e) {
    container.innerHTML = '<p style="color:#722F37;">Erro ao conectar com o servidor.</p>';
  }
}

async function loadLivrosLote(loteId) {
  try {
    const res = await fetch("/api/admin/lotes/" + loteId);
    livrosCache = await res.json();
    renderLivros();
  } catch (e) {
    alert("Erro ao carregar livros do lote.");
  }
}

function renderLivros() {
  const container = document.getElementById("contentArea");
  let html = `<h3 style="font-family:'Playfair Display',serif;font-size:1.1rem;margin-bottom:.75rem">Auditando Livros</h3><div class="book-grid">`;
  livrosCache.forEach((b, bookIdx) => {
    const fotos = JSON.parse(b.fotosUrls || "[]");
    let fotosHtml = '<div class="fotos-wrapper">';
    fotos.forEach((url, fotoIdx) => {
      fotosHtml += `<div class="foto-item"><img src="${url}" alt="Foto">
        <button class="btn-del-foto" onclick="removerFoto(${bookIdx},${fotoIdx})">×</button></div>`;
    });
    fotosHtml += `<label class="btn-add-foto" for="upload-${bookIdx}">+</label>
      <input type="file" id="upload-${bookIdx}" style="display:none" accept="image/*" onchange="subirFoto(event,${bookIdx})"></div>`;
    html += `
    <div class="bcard" id="card-${b.id}">
      ${fotosHtml}
      <h3 style="margin-bottom:5px;">${b.titulo}</h3>
      <p style="font-size:13px;color:#7A6E65;margin:0;">Autor: ${b.autor} | ISBN: ${b.isbn}</p>
      <div style="margin-top:15px;">
        <label style="font-size:12px;font-weight:bold;color:#2C241B;">AVALIAÇÃO DO ESTADO:</label>
        <select id="sel-${b.id}" onchange="updatePrice(${b.id})">
          <option value="NOVO">Novo</option><option value="OTIMO">Ótimo</option>
          <option value="BOM" selected>Bom</option><option value="DESGASTADO">Desgastado</option>
          <option value="RUIM">Ruim (Inviável)</option>
        </select>
      </div>
      <div class="price-display" id="price-${b.id}">Sugestão: T$ 30</div>
      <button class="btn-aprovar" onclick="finalizarAprovacao(${bookIdx})">Confirmar e Publicar</button>
      <button class="btn-rejeitar" onclick="rejeitar(${b.id})">Rejeitar este Livro</button>
    </div>`;
  });
  html += `</div><br><button onclick="loadLotes()" style="background:none;color:#7A6E65;border:none;text-decoration:underline;cursor:pointer;">← Voltar para a lista de lotes</button>`;
  container.innerHTML = html;
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

async function rejeitar(id) {
  if (!confirm("Tem certeza que deseja rejeitar este livro?")) return;
  const estado = document.getElementById(`sel-${id}`).value;
  const res = await fetch(`/api/admin/livros/${id}/rejeitar`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ estado }),
  });
  if (res.ok) {
    mostrarToast("Livro rejeitado.", "aviso");
    livrosCache = livrosCache.filter((l) => l.id !== id);
    if (livrosCache.length === 0) loadLotes();
    else renderLivros();
  } else {
    const t = await res.text();
    mostrarToast("Erro: " + t, "erro");
  }
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
    if (pendentes > 0) {
      badge.textContent = pendentes;
      badge.style.display = "inline";
    } else badge.style.display = "none";

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
    ? `<div style="display:flex;align-items:center;gap:.5rem;flex-wrap:wrap">
        <input class="rastreio-input" id="rastreio-${p.id}" type="text"
               placeholder="Cód. rastreio (opcional)" value="${p.codigoRastreio || ""}" />
        <select class="select-status" id="status-${p.id}">
          ${proximos.map((s) => `<option value="${s}">${LABEL_STATUS[s]}</option>`).join("")}
        </select>
        <button class="btn-salvar-envio" onclick="salvarEnvio(${p.id})">
          <i class="fa-solid fa-floppy-disk"></i> Salvar
        </button>
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
async function carregarBlogAdmin() {
  const lista = document.getElementById("listaBlog");
  try {
    const res = await fetch("/api/blog");
    const posts = await res.json();

    if (posts.length === 0) {
      lista.innerHTML = '<p style="font-size:.875rem;color:#7A6E65;padding:1rem">Nenhum post publicado ainda.</p>';
      return;
    }

    lista.innerHTML = posts.map((p) => `
    <div class="blog-post-item">
      ${p.imagemUrl
        ? `<img src="${p.imagemUrl}" alt="" class="blog-post-img"/>`
        : '<div class="blog-post-img" style="display:flex;align-items:center;justify-content:center;font-size:1.5rem">📝</div>'}
      <div style="flex:1;min-width:0">
        <div style="font-weight:700;color:#2C241B;font-size:.9rem;margin-bottom:.25rem">${p.titulo}</div>
        <div style="font-size:.75rem;color:#7A6E65;margin-bottom:.5rem">Por ${p.autorNome} · ${p.dataPublicacao}</div>
        <div style="font-size:.85rem;color:#7A6E65;overflow:hidden;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical">${p.conteudo}</div>
      </div>
      <button onclick="deletarPost(${p.id})" class="btn-remover-post">Remover</button>
    </div>`).join("");
  } catch (e) {
    lista.innerHTML = '<p style="font-size:.875rem;color:#722F37">Erro ao carregar posts.</p>';
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

async function deletarPost(id) {
  if (!confirm("Remover este post?")) return;
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
