/* ================================================================
   cancelamentos.js — Admin · Bibliotroca
   Auth: JWT em cookie HttpOnly → browser envia via credentials:'include'
   ================================================================ */

let _dados    = [];
let _dadosMap = {};   // id → solicitacao (evita escaping de JSON no onclick)
let _acaoAtual = null; // { tipo: 'aprovar'|'recusar', id }

/* ── UTILS ──────────────────────────────────────────────────────── */

function fmtData(iso) {
    if (!iso) return '—';
    const d = new Date(iso);
    return d.toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' }) +
           ' ' + d.toLocaleTimeString('pt-BR', { hour: '2-digit', minute: '2-digit' });
}

function escHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

function primeiraFoto(fotosUrls) {
    if (!fotosUrls) return null;
    return fotosUrls.split(',')[0].trim() || null;
}

function mostrarToast(cls, msg) {
    const t = document.getElementById('toast');
    t.className = 'toast ' + cls;
    t.textContent = msg;
    t.style.display = 'block';
    setTimeout(() => { t.style.display = 'none'; }, 4000);
}

function abrirMenu() {
    document.getElementById('sidebar')?.classList.add('open');
    document.getElementById('overlay')?.classList.add('show');
}
function fecharMenu() {
    document.getElementById('sidebar')?.classList.remove('open');
    document.getElementById('overlay')?.classList.remove('show');
}

/* ── CARDS ──────────────────────────────────────────────────────── */

const MOTIVO_STYLE = {
    COMPREI_POR_ENGANO:    { bg: '#fef3c7', color: '#92400e' },
    ENCONTREI_MAIS_BARATO: { bg: '#dbeafe', color: '#1e40af' },
    PRODUTO_NAO_ESPERADO:  { bg: '#ede9fe', color: '#5b21b6' },
    OUTRO:                 { bg: '#f3f4f6', color: '#374151' },
};

function motivoBadge(s) {
    const m = MOTIVO_STYLE[s.motivoCategoria] || { bg: '#f3f4f6', color: '#374151' };
    return `<span style="display:inline-block;padding:2px 10px;border-radius:20px;` +
           `font-size:11px;font-weight:600;background:${m.bg};color:${m.color}">` +
           escHtml(s.motivoCategoriaDescricao || s.motivoCategoria) + `</span>`;
}

function statusBadge(s) {
    const cls = { PENDENTE: 'badge-PENDENTE', APROVADO: 'badge-APROVADO', RECUSADO: 'badge-RECUSADO' };
    return `<span class="badge ${cls[s.status] || ''}">${escHtml(s.statusDescricao || s.status)}</span>`;
}

function renderCards(lista) {
    const area = document.getElementById('cardsArea');
    if (lista.length === 0) {
        area.innerHTML = '<div class="empty-state"><i class="fa-solid fa-inbox"></i><p>Nenhuma solicitação encontrada.</p></div>';
        return;
    }
    area.innerHTML = lista.map(s => {
        const foto = primeiraFoto(s.fotosUrls);
        const fotoHtml = foto
            ? `<img src="${escHtml(foto)}" alt="Capa" class="card-foto">`
            : `<div class="card-foto card-foto-placeholder"><i class="fa-solid fa-book"></i></div>`;

        const isPendente = s.status === 'PENDENTE';

        // Botões usam apenas o ID — objeto completo está em _dadosMap
        const acoesHtml = isPendente
            ? `<div class="card-acoes">
                <button class="btn-aprovar" onclick="abrirModal('aprovar', ${s.id})">
                    <i class="fa-solid fa-check"></i> Aprovar
                </button>
                <button class="btn-recusar" onclick="abrirModal('recusar', ${s.id})">
                    <i class="fa-solid fa-xmark"></i> Recusar
                </button>
               </div>`
            : `<div class="card-acoes-processado">
                ${statusBadge(s)}
                ${s.comentarioAdmin
                    ? `<div class="comentario-admin"><i class="fa-solid fa-comment"></i> ${escHtml(s.comentarioAdmin)}</div>`
                    : ''}
                ${s.dataResposta
                    ? `<div class="data-resposta">Respondido em ${fmtData(s.dataResposta)}</div>`
                    : ''}
               </div>`;

        return `<div class="cancel-card ${isPendente ? 'card-pendente' : ''}">
            <div class="card-livro">
                ${fotoHtml}
                <div class="card-livro-info">
                    <div class="card-titulo">${escHtml(s.tituloLivro)}</div>
                    <div class="card-autor">${escHtml(s.autorLivro || '—')}</div>
                    <div class="card-preco">T$ ${(s.precoLivro || 0).toFixed(2)}</div>
                </div>
            </div>
            <div class="card-divider"></div>
            <div class="card-body">
                <div class="card-meta-row">
                    <div class="card-meta-item">
                        <span class="card-meta-label">Pedido</span>
                        <span class="card-meta-value">#${s.pedidoId}</span>
                    </div>
                    <div class="card-meta-item">
                        <span class="card-meta-label">Solicitação</span>
                        <span class="card-meta-value">#${s.id}</span>
                    </div>
                    <div class="card-meta-item">
                        <span class="card-meta-label">Data</span>
                        <span class="card-meta-value">${fmtData(s.dataSolicitacao)}</span>
                    </div>
                    <div class="card-meta-item">
                        <span class="card-meta-label">Status</span>
                        <span class="card-meta-value">${statusBadge(s)}</span>
                    </div>
                </div>
                <div class="card-cliente">
                    <i class="fa-solid fa-user"></i>
                    <span>${escHtml(s.clienteNome)}</span>
                    <span class="card-email">${escHtml(s.clienteEmail)}</span>
                </div>
                <div class="card-motivo-bloco">
                    <div class="card-motivo-categoria">${motivoBadge(s)}</div>
                    <div class="card-motivo-descricao">${escHtml(s.motivoDescricao)}</div>
                </div>
                ${isPendente
                    ? `<div class="card-estorno-aviso">
                        <i class="fa-solid fa-coins"></i>
                        Estorno de <strong>T$ ${(s.precoLivro || 0).toFixed(2)}</strong> será realizado ao aprovar.
                       </div>`
                    : ''}
                ${acoesHtml}
            </div>
        </div>`;
    }).join('');
}

/* ── MODAL ──────────────────────────────────────────────────────── */

function abrirModal(tipo, id) {
    const solicitacao = _dadosMap[id];
    if (!solicitacao) {
        console.error('[cancelamentos] Solicitação não encontrada no mapa para id=', id);
        return;
    }
    console.log('[cancelamentos] abrirModal tipo=%s id=%d', tipo, id);
    _acaoAtual = { tipo, id };

    document.getElementById('modalTitulo').textContent =
        tipo === 'aprovar' ? 'Aprovar Cancelamento' : 'Recusar Cancelamento';

    document.getElementById('modalInfo').innerHTML =
        `<strong>Pedido #${solicitacao.pedidoId}</strong> — ${escHtml(solicitacao.tituloLivro)}<br>
         <span style="color:#7a6e65">Cliente: ${escHtml(solicitacao.clienteNome)} (${escHtml(solicitacao.clienteEmail)})</span><br>
         <span style="color:#7a6e65">Motivo: ${escHtml(solicitacao.motivoCategoriaDescricao)}</span><br>
         <span style="color:#7a6e65">Descrição: ${escHtml(solicitacao.motivoDescricao)}</span>
         ${tipo === 'aprovar'
             ? `<br><strong style="color:#2e7d32">Valor a estornar: T$ ${(solicitacao.precoLivro || 0).toFixed(2)}</strong>`
             : ''}`;

    const btnConfirmar = document.getElementById('btnConfirmarAcao');
    document.getElementById('comentarioAdmin').value = '';
    btnConfirmar.disabled = false;
    btnConfirmar.textContent = tipo === 'aprovar' ? 'Confirmar Aprovação' : 'Confirmar Recusa';
    btnConfirmar.style.background = tipo === 'aprovar' ? '#2e7d32' : '#722F37';

    document.getElementById('modal').classList.add('show');
    document.getElementById('modalOverlay').classList.add('show');
}

function fecharModal() {
    document.getElementById('modal').classList.remove('show');
    document.getElementById('modalOverlay').classList.remove('show');
    _acaoAtual = null;
}

async function confirmarAcao() {
    if (!_acaoAtual) return;
    const { tipo, id } = _acaoAtual;
    const comentario   = document.getElementById('comentarioAdmin').value.trim();
    const endpoint     = `/api/admin/cancelamentos/${id}/${tipo}`;

    console.log('[cancelamentos] confirmarAcao tipo=%s id=%d endpoint=%s', tipo, id, endpoint);

    const btnConfirmar = document.getElementById('btnConfirmarAcao');
    btnConfirmar.disabled = true;
    btnConfirmar.textContent = 'Processando...';

    try {
        const res = await fetch(endpoint, {
            method: 'POST',
            credentials: 'include',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ comentarioAdmin: comentario || null }),
        });

        console.log('[cancelamentos] resposta status=%d', res.status);

        if (res.status === 401 || res.status === 403) {
            console.warn('[cancelamentos] Sem autorização — redirecionando para login');
            window.location.href = '/admin/login';
            return;
        }

        if (!res.ok) {
            let msg = 'Falha na API';
            try {
                const err = await res.json();
                msg = err.message || msg;
            } catch (_) { /* ignore */ }
            throw new Error(msg);
        }

        mostrarToast('toast-ok', tipo === 'aprovar'
            ? 'Cancelamento aprovado e estorno realizado.'
            : 'Cancelamento recusado.');
        fecharModal();
        await carregarDados();

    } catch (e) {
        console.error('[cancelamentos] Erro em confirmarAcao:', e);
        mostrarToast('toast-err', 'Erro: ' + (e.message || 'Tente novamente.'));
        btnConfirmar.disabled = false;
        btnConfirmar.textContent = tipo === 'aprovar' ? 'Confirmar Aprovação' : 'Confirmar Recusa';
    }
}

/* ── DADOS ──────────────────────────────────────────────────────── */

async function carregarDados() {
    const area = document.getElementById('cardsArea');
    area.innerHTML = '<div class="loading-placeholder"><i class="fa-solid fa-spinner fa-spin"></i> Carregando solicitações...</div>';

    try {
        console.log('[cancelamentos] carregarDados → GET /api/admin/cancelamentos');
        const res = await fetch('/api/admin/cancelamentos', { credentials: 'include' });
        console.log('[cancelamentos] carregarDados status=%d', res.status);

        if (res.status === 401 || res.status === 403) {
            window.location.href = '/admin/login';
            return;
        }
        if (!res.ok) throw new Error('HTTP ' + res.status);

        _dados = await res.json();

        // Preenche o mapa id → objeto (evita escaping de JSON no onclick)
        _dadosMap = {};
        _dados.forEach(s => { _dadosMap[s.id] = s; });

        console.log('[cancelamentos] %d solicitações carregadas', _dados.length);
        atualizarStats();
        aplicarFiltro();

    } catch (e) {
        console.error('[cancelamentos] Erro ao carregar dados:', e);
        area.innerHTML = `<div class="loading-placeholder" style="color:#722F37">
            <i class="fa-solid fa-triangle-exclamation"></i> Erro ao carregar cancelamentos.
        </div>`;
    }
}

function atualizarStats() {
    document.getElementById('statTotal').textContent     = _dados.length;
    document.getElementById('statPendentes').textContent = _dados.filter(d => d.status === 'PENDENTE').length;
    document.getElementById('statAprovados').textContent = _dados.filter(d => d.status === 'APROVADO').length;
    document.getElementById('statRecusados').textContent = _dados.filter(d => d.status === 'RECUSADO').length;
}

function aplicarFiltro() {
    const filtro = document.getElementById('filtroStatus').value;
    const filtrados = _dados.filter(d => !filtro || d.status === filtro);
    renderCards(filtrados);
}

/* ── INIT ────────────────────────────────────────────────────────── */

document.getElementById('dataHoje').textContent =
    new Date().toLocaleDateString('pt-BR', { weekday: 'long', day: '2-digit', month: 'long', year: 'numeric' });

carregarDados();
