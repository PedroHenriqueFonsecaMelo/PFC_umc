/* ================================================================
   ranking.js — Página pública de Ranking · Bibliotroca
   ================================================================ */

let _ranking    = [];   // dados completos
let _minhaPosicao = 0;  // posição do usuário logado (0 = não logado)
let _periodoAtual = 'todos';

/* ── Utils ── */
function esc(str) {
    return String(str || '')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

function inicial(primeiroNome) {
    return (primeiroNome || '?').charAt(0).toUpperCase();
}

function medalha(pos) {
    return ['🥇', '🥈', '🥉'][pos - 1] || '';
}

function corMedalha(pos) {
    return ['var(--gold)', 'var(--silver)', 'var(--bronze)'][pos - 1] || 'var(--accent)';
}

/* ── Detectar login ── */
async function detectarLogin() {
    try {
        const res = await fetch('/api/gamificacao/meu-perfil', { credentials: 'include' });
        if (!res.ok) return;
        const p = await res.json();
        _minhaPosicao = p.posicaoRanking || 0;

        if (_minhaPosicao > 0) {
            const badge = document.getElementById('rkMinhaPosicaoTexto');
            const wrap  = document.getElementById('rkMinhaPosicao');
            if (badge) badge.textContent = `Você está em ${_minhaPosicao}º lugar`;
            if (wrap)  wrap.style.display = 'block';
            // Re-renderiza com destaque se dados já carregados
            if (_ranking.length > 0) renderRanking();
        }
    } catch (_) {}
}

/* ── Carregar ranking ── */
async function carregarRanking(periodo) {
    _periodoAtual = periodo || 'todos';

    // Reset UI
    document.getElementById('rkPodio').innerHTML =
        '<div class="rk-podio-skeleton"></div>' +
        '<div class="rk-podio-skeleton rk-podio-skeleton-center"></div>' +
        '<div class="rk-podio-skeleton"></div>';
    document.getElementById('rkTabela').innerHTML = '<div class="rk-loading">Carregando ranking…</div>';

    try {
        const res = await fetch(`/api/ranking/top?limite=100&periodo=${_periodoAtual}`);
        if (!res.ok) throw new Error('HTTP ' + res.status);
        _ranking = await res.json();
        renderRanking();
    } catch (_) {
        document.getElementById('rkPodio').innerHTML = '';
        document.getElementById('rkTabela').innerHTML =
            '<div class="rk-vazio">Não foi possível carregar o ranking.</div>';
    }
}

/* ── Renderizar pódio e tabela ── */
function renderRanking() {
    const top3  = _ranking.slice(0, 3);
    const resto = _ranking.slice(3);

    renderPodio(top3);
    renderTabela(resto);

    // Subtítulo
    const sub = document.getElementById('rkSubtitulo');
    if (sub) {
        const label = { todos: 'todos os tempos', ano: 'este ano', mes: 'este mês' }[_periodoAtual] || '';
        sub.textContent = _ranking.length + ' leitores — ' + label;
    }
}

function renderPodio(top3) {
    const container = document.getElementById('rkPodio');
    if (!top3 || top3.length === 0) {
        container.innerHTML = '<p class="rk-vazio">Nenhum leitor no ranking ainda.</p>';
        return;
    }

    container.innerHTML = top3.map(u => {
        const eVoce = _minhaPosicao > 0 && u.posicao === _minhaPosicao;
        const nomeCurto = esc(u.primeiroNome) + (u.inicialSobrenome ? ' ' + esc(u.inicialSobrenome) : '');
        const pct = u.xpTotal > 0 && u.xpProximoNivel >= 0
            ? Math.min(100, Math.round((u.xpTotal / (u.xpTotal + (u.xpProximoNivel || 1))) * 100))
            : 100;
        const avatarHtml = u.fotoPerfil
            ? `<img src="${esc(u.fotoPerfil)}" alt="${nomeCurto}" class="rk-card-avatar-img" style="border-color:${corMedalha(u.posicao)}">`
            : `<div class="rk-card-avatar" style="border-color:${corMedalha(u.posicao)}">${inicial(u.primeiroNome)}</div>`;

        return `
        <div class="rk-card pos-${u.posicao}${eVoce ? ' e-voce' : ''}">
            ${eVoce ? '<span class="rk-card-voce-badge">Você</span>' : ''}
            <div class="rk-card-medalha">${medalha(u.posicao)}</div>
            ${avatarHtml}
            <div class="rk-card-nome" title="${nomeCurto}">${nomeCurto}</div>
            <div class="rk-card-nivel">${esc(u.badge)} ${esc(u.nivel)}</div>
            <div class="rk-card-xp-num">${(u.xpTotal).toLocaleString('pt-BR')}</div>
            <div class="rk-card-xp-label">XP</div>
        </div>`;
    }).join('');
}

function renderTabela(items) {
    const container = document.getElementById('rkTabela');
    if (!items || items.length === 0) {
        container.innerHTML = '<p class="rk-vazio" style="padding:1.5rem 0">Apenas top 3 no ranking.</p>';
        return;
    }

    const maxXp = _ranking.length > 0 ? (_ranking[0].xpTotal || 1) : 1;

    container.innerHTML = items.map(u => {
        const eVoce = _minhaPosicao > 0 && u.posicao === _minhaPosicao;
        const nomeCurto = esc(u.primeiroNome) + (u.inicialSobrenome ? ' ' + esc(u.inicialSobrenome) : '');
        const pct = Math.round((u.xpTotal / maxXp) * 100);
        const avatarLinha = u.fotoPerfil
            ? `<img src="${esc(u.fotoPerfil)}" alt="${nomeCurto}" class="rk-linha-avatar-img">`
            : `<div class="rk-linha-avatar">${inicial(u.primeiroNome)}</div>`;
        return `
        <div class="rk-linha${eVoce ? ' e-voce' : ''}">
            <span class="rk-linha-pos">${u.posicao}º</span>
            ${avatarLinha}
            <div class="rk-linha-info">
                <div class="rk-linha-nome">${nomeCurto}</div>
                <div class="rk-linha-nivel">${esc(u.badge)} ${esc(u.nivel)}</div>
            </div>
            <div class="rk-linha-barra-wrap">
                <div class="rk-linha-barra-track">
                    <div class="rk-linha-barra-fill" style="width:${pct}%"></div>
                </div>
            </div>
            <div class="rk-linha-xp">
                ${(u.xpTotal).toLocaleString('pt-BR')} XP
                ${eVoce ? '<span class="rk-linha-voce">Você</span>' : ''}
            </div>
        </div>`;
    }).join('');
}

/* ── Filtro de período ── */
window.rkFiltrar = function(periodo, btn) {
    document.querySelectorAll('.rk-filtro-btn').forEach(b => b.classList.remove('active'));
    if (btn) btn.classList.add('active');
    carregarRanking(periodo);
};

/* ── Init ── */
detectarLogin();
carregarRanking('todos');
