/* ================================================================
   cancelamento_detalhe.js — Admin · Bibliotroca
   ================================================================ */

let _sol = null; // solicitação carregada

/* ── UTILS ──────────────────────────────────────────────────────── */

function fmtData(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' }) +
           ' às ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

function esc(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function fmtTokens(v) {
    if (v == null) return '0,00';
    return Number(v).toLocaleString('pt-BR', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function mostrarToast(cls, msg) {
    const t = document.getElementById('toast');
    t.className = 'toast ' + cls;
    t.textContent = msg;
    t.style.display = 'block';
    setTimeout(() => { t.style.display = 'none'; }, 4500);
}

function abrirMenu() {
    document.getElementById('sidebar')?.classList.add('open');
    document.getElementById('overlay')?.classList.add('show');
}
function fecharMenu() {
    document.getElementById('sidebar')?.classList.remove('open');
    document.getElementById('overlay')?.classList.remove('show');
}

/* ── MOTIVO BADGE ────────────────────────────────────────────────── */

const MOTIVO_STYLE = {
    COMPREI_POR_ENGANO:    { bg: '#fef3c7', color: '#92400e' },
    ENCONTREI_MAIS_BARATO: { bg: '#dbeafe', color: '#1e40af' },
    PRODUTO_NAO_ESPERADO:  { bg: '#ede9fe', color: '#5b21b6' },
    OUTRO:                 { bg: '#f3f4f6', color: '#374151' },
};

function motivoBadge(s) {
    const m = MOTIVO_STYLE[s.motivoCategoria] || { bg: '#f3f4f6', color: '#374151' };
    return `<span class="badge-motivo-pill" style="background:${m.bg};color:${m.color}">` +
           esc(s.motivoCategoriaDescricao || s.motivoCategoria) + `</span>`;
}

function statusBadge(status) {
    const cls = { PENDENTE: 'badge-PENDENTE', APROVADO: 'badge-APROVADO', RECUSADO: 'badge-RECUSADO' };
    const label = { PENDENTE: 'Pendente', APROVADO: 'Aprovado', RECUSADO: 'Recusado' };
    return `<span class="badge ${cls[status] || ''}">${label[status] || esc(status)}</span>`;
}

/* ── CARREGAR ────────────────────────────────────────────────────── */

function carregarSolicitacao() {
    const id = window._SOLICITACAO_ID;
    if (!id) {
        document.getElementById('pageContent').innerHTML =
            `<div class="loading-placeholder"><i class="fa-solid fa-triangle-exclamation"></i> Solicitação não especificada.</div>`;
        return;
    }

    fetch(`/api/admin/cancelamentos/${id}`, { credentials: 'include' })
        .then(r => {
            if (r.status === 401 || r.status === 403) { window.location.href = '/admin/login'; return null; }
            if (r.status === 404) throw new Error('Solicitação não encontrada.');
            if (!r.ok) throw new Error('Erro ao carregar: HTTP ' + r.status);
            return r.json();
        })
        .then(data => {
            if (!data) return;
            _sol = data;
            renderDetalhe(data);
        })
        .catch(err => {
            document.getElementById('pageContent').innerHTML =
                `<div class="loading-placeholder">
                    <i class="fa-solid fa-triangle-exclamation"></i> ${esc(err.message)}
                    <br><br><a href="/admin/cancelamentos" style="color:#722F37;font-weight:600">← Voltar para Cancelamentos</a>
                 </div>`;
        });
}

/* ── RENDER ──────────────────────────────────────────────────────── */

function primeiraFoto(fotosUrls) {
    if (!fotosUrls) return null;
    try {
        const parsed = JSON.parse(fotosUrls);
        if (Array.isArray(parsed) && parsed.length > 0) return parsed[0];
    } catch (_) { /* não é JSON */ }
    return fotosUrls.split(',')[0].trim() || null;
}

function renderDetalhe(s) {
    // Atualiza título do topbar
    const tt = document.getElementById('topbarTitle');
    if (tt) tt.textContent = `Solicitação #${s.id}`;

    const foto = primeiraFoto(s.fotosUrls);
    const fotoHtml = foto
        ? `<img src="${esc(foto)}" alt="Capa" class="livro-foto">`
        : `<div class="livro-foto-ph"><i class="fa-solid fa-book"></i></div>`;

    const isPendente = s.status === 'PENDENTE';

    // Seção de decisão (se já decidida)
    const decisaoHtml = !isPendente ? `
        <div class="section-card">
            <div class="section-header">
                <i class="fa-solid fa-gavel"></i>
                <span class="section-title">Decisão do Administrador</span>
            </div>
            <div class="section-body">
                <div class="decisao-bloco">
                    <div class="decisao-row">
                        <span class="decisao-label">Status</span>
                        ${statusBadge(s.status)}
                    </div>
                    <div class="decisao-row">
                        <span class="decisao-label">Respondido em</span>
                        <span class="decisao-valor">${fmtData(s.dataResposta)}</span>
                    </div>
                    ${s.comentarioAdmin ? `
                    <div>
                        <span class="decisao-label">Comentário</span>
                        <div class="comentario-admin-bloco" style="margin-top:8px">"${esc(s.comentarioAdmin)}"</div>
                    </div>` : ''}
                </div>
            </div>
        </div>` : '';

    // Botões de ação (apenas se pendente)
    const acoesHtml = isPendente ? `
        <div class="acoes-area">
            <button class="btn-aprovar-full" id="btnAprovar" onclick="confirmarAprovacao()">
                <i class="fa-solid fa-check"></i> Aprovar Cancelamento
            </button>
            <button class="btn-recusar-outline" id="btnRecusar" onclick="abrirModalRecusa()">
                <i class="fa-solid fa-xmark"></i> Recusar
            </button>
        </div>` : '';

    const html = `
        <!-- BREADCRUMB -->
        <div class="breadcrumb">
            <a href="/admin/dashboard"><i class="fa-solid fa-gauge"></i> Painel Admin</a>
            <i class="fa-solid fa-chevron-right sep"></i>
            <a href="/admin/cancelamentos">Cancelamentos</a>
            <i class="fa-solid fa-chevron-right sep"></i>
            <span>Solicitação #${s.id}</span>
        </div>

        <div class="detalhe-area">

            <!-- BOTÃO VOLTAR -->
            <a href="/admin/cancelamentos" class="btn-voltar">
                <i class="fa-solid fa-arrow-left"></i> Voltar para Cancelamentos
            </a>

            <!-- SEÇÃO LIVRO -->
            <div class="section-card">
                <div class="section-header">
                    <i class="fa-solid fa-book"></i>
                    <span class="section-title">Dados do Livro</span>
                </div>
                <div class="section-body">
                    <div class="livro-layout">
                        ${fotoHtml}
                        <div style="flex:1;min-width:0">
                            <div class="livro-titulo">${esc(s.tituloLivro)}</div>
                            <div class="livro-autor">${esc(s.autorLivro || '—')}</div>
                            <div class="livro-meta">
                                ${s.isbnLivro ? `
                                <div class="meta-chip">
                                    <span class="meta-chip-label">ISBN</span>
                                    <span class="meta-chip-value" style="font-family:monospace">${esc(s.isbnLivro)}</span>
                                </div>` : ''}
                                <div class="meta-chip">
                                    <span class="meta-chip-label">Valor do pedido</span>
                                    <span class="meta-chip-value">T$ ${fmtTokens(s.precoLivro)}</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <!-- SEÇÃO COMPRADOR -->
            <div class="section-card">
                <div class="section-header">
                    <i class="fa-solid fa-user"></i>
                    <span class="section-title">Dados do Comprador</span>
                </div>
                <div class="section-body">
                    <div class="fields-grid">
                        <div class="field">
                            <span class="field-label">Nome</span>
                            <span class="field-value">${esc(s.clienteNome)}</span>
                        </div>
                        <div class="field">
                            <span class="field-label">E-mail</span>
                            <span class="field-value">${esc(s.clienteEmail)}</span>
                        </div>
                        <div class="field">
                            <span class="field-label">Pedido</span>
                            <span class="field-value">#${s.pedidoId}</span>
                        </div>
                        <div class="field">
                            <span class="field-label">Data da compra</span>
                            <span class="field-value">${fmtData(s.dataCompra)}</span>
                        </div>
                    </div>
                </div>
            </div>

            <!-- SEÇÃO SOLICITAÇÃO -->
            <div class="section-card">
                <div class="section-header">
                    <i class="fa-solid fa-file-circle-question"></i>
                    <span class="section-title">Motivo do Cancelamento</span>
                </div>
                <div class="section-body">
                    <div class="motivo-categoria">${motivoBadge(s)}</div>
                    <div style="margin-top:4px;font-size:11px;color:#b0a49a;font-weight:700;text-transform:uppercase;letter-spacing:.04em;margin-bottom:6px">Descrição do cliente</div>
                    <div class="motivo-descricao">${esc(s.motivoDescricao || '—')}</div>
                    <div style="margin-top:12px;font-size:12px;color:#7a6e65">
                        <i class="fa-regular fa-calendar"></i> Solicitado em ${fmtData(s.dataSolicitacao)}
                    </div>
                </div>
            </div>

            <!-- SEÇÃO ESTORNO -->
            <div class="section-card">
                <div class="section-header">
                    <i class="fa-solid fa-coins"></i>
                    <span class="section-title">Estorno</span>
                </div>
                <div class="section-body">
                    <div class="estorno-destaque">
                        <i class="fa-solid fa-coins"></i>
                        <div>
                            <div class="estorno-valor">T$ ${fmtTokens(s.precoLivro)}</div>
                            <div class="estorno-label">Valor a ser devolvido ao aprovar</div>
                        </div>
                        <div class="saldo-info">
                            <div class="saldo-label">Saldo atual do comprador</div>
                            <div class="saldo-valor">T$ ${fmtTokens(s.saldoAtualComprador)}</div>
                        </div>
                    </div>
                </div>
                ${acoesHtml}
            </div>

            <!-- SEÇÃO DECISÃO (se processado) -->
            ${decisaoHtml}

        </div>
    `;

    document.getElementById('pageContent').innerHTML = html;

    // Preenche o modal com info do livro
    document.getElementById('modalInfo').innerHTML =
        `<strong>${esc(s.tituloLivro)}</strong><br>
         <span style="color:#7a6e65">Pedido #${s.pedidoId} — ${esc(s.clienteNome)}</span><br>
         <span style="color:#7a6e65">Motivo: ${esc(s.motivoCategoriaDescricao)}</span>`;
}

/* ── APROVAR ─────────────────────────────────────────────────────── */

async function confirmarAprovacao() {
    const btn = document.getElementById('btnAprovar');
    if (!btn || !_sol) return;
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processando...';

    try {
        const res = await fetch(`/api/admin/cancelamentos/${_sol.id}/aprovar`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ comentarioAdmin: null }),
        });

        if (res.status === 401 || res.status === 403) { window.location.href = '/admin/login'; return; }
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.message || 'Falha na API');
        }

        mostrarToast('toast-ok', 'Cancelamento aprovado e estorno realizado.');
        setTimeout(() => { window.location.href = '/admin/cancelamentos'; }, 1800);

    } catch (e) {
        mostrarToast('toast-err', 'Erro: ' + (e.message || 'Tente novamente.'));
        if (btn) {
            btn.disabled = false;
            btn.innerHTML = '<i class="fa-solid fa-check"></i> Aprovar Cancelamento';
        }
    }
}

/* ── MODAL RECUSA ─────────────────────────────────────────────────── */

function abrirModalRecusa() {
    document.getElementById('comentarioAdmin').value = '';
    document.getElementById('btnConfirmarRecusa').disabled = false;
    document.getElementById('btnConfirmarRecusa').innerHTML = '<i class="fa-solid fa-xmark"></i> Confirmar Recusa';
    document.getElementById('modal').classList.add('show');
    document.getElementById('modalOverlay').classList.add('show');
    document.getElementById('comentarioAdmin').focus();
}

function fecharModal() {
    document.getElementById('modal').classList.remove('show');
    document.getElementById('modalOverlay').classList.remove('show');
}

async function confirmarRecusa() {
    if (!_sol) return;
    const comentario = document.getElementById('comentarioAdmin').value.trim();
    const btn = document.getElementById('btnConfirmarRecusa');
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processando...';

    try {
        const res = await fetch(`/api/admin/cancelamentos/${_sol.id}/recusar`, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ comentarioAdmin: comentario || null }),
        });

        if (res.status === 401 || res.status === 403) { window.location.href = '/admin/login'; return; }
        if (!res.ok) {
            const err = await res.json().catch(() => ({}));
            throw new Error(err.message || 'Falha na API');
        }

        fecharModal();
        mostrarToast('toast-ok', 'Cancelamento recusado.');
        setTimeout(() => { window.location.href = '/admin/cancelamentos'; }, 1800);

    } catch (e) {
        mostrarToast('toast-err', 'Erro: ' + (e.message || 'Tente novamente.'));
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-xmark"></i> Confirmar Recusa';
    }
}

/* ── INIT ─────────────────────────────────────────────────────────── */

document.addEventListener('DOMContentLoaded', () => {
    const hoje = document.getElementById('dataHoje');
    if (hoje) {
        hoje.textContent = new Date().toLocaleDateString('pt-BR', {
            weekday: 'long', day: '2-digit', month: 'long', year: 'numeric'
        });
    }
    carregarSolicitacao();
});
