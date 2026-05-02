/* ==========================================================
   gamificacao.js — Widget de Ranking & XP da Bibliotroca
   Consome: GET /api/gamificacao/ranking
            GET /api/gamificacao/meu-perfil  (requer JWT)
   ========================================================== */

(function () {
  "use strict";

  /* -------------------------------------------------------
     UTILITÁRIOS
  ------------------------------------------------------- */

  function getToken() {
    return localStorage.getItem("jwtToken") || sessionStorage.getItem("jwtToken") || null;
  }

  function medalha(posicao) {
    return ["🥇", "🥈", "🥉"][posicao - 1] || posicao + "º";
  }

  function corBarra(posicao) {
    return ["#f5c542", "#b0b8c1", "#cd7f32"][posicao - 1] || "#7c6bc9";
  }

  /* -------------------------------------------------------
     BARRA DE PROGRESSO DE XP
  ------------------------------------------------------- */

  function renderBarraXp(xpAtual, xpProximo, nivel) {
    const nivelMaximo = xpProximo === 0;
    const pct = nivelMaximo ? 100 : Math.min(100, Math.round((xpAtual / (xpAtual + xpProximo)) * 100));

    return `
      <div class="gami-barra-label">
        <span>${nivel}</span>
        <span>${nivelMaximo ? "Nível máximo!" : xpProximo + " XP para o próximo"}</span>
      </div>
      <div class="gami-barra-track">
        <div class="gami-barra-fill" style="width:${pct}%"></div>
      </div>`;
  }

  /* -------------------------------------------------------
     CARD DO USUÁRIO LOGADO
  ------------------------------------------------------- */

  async function carregarMeuPerfil() {
    const container = document.getElementById("gami-meu-perfil");
    if (!container) return;

    const token = getToken();
    if (!token) {
      container.innerHTML = `<p class="gami-aviso">Faça <a href="/login">login</a> para ver seu progresso.</p>`;
      return;
    }

    try {
      const res = await fetch("/api/gamificacao/meu-perfil", {
        headers: { Authorization: "Bearer " + token },
      });
      if (!res.ok) throw new Error("não autenticado");
      const p = await res.json();

      container.innerHTML = `
        <div class="gami-perfil-card">
          <div class="gami-perfil-topo">
            <span class="gami-badge-grande">${p.badge}</span>
            <div class="gami-perfil-info">
              <strong class="gami-perfil-nome">${p.nome}</strong>
              <span class="gami-perfil-nivel">${p.nivel}</span>
              <span class="gami-perfil-xp">${p.xpTotal} XP  •  ${p.posicaoRanking}º no ranking</span>
            </div>
          </div>
          ${renderBarraXp(p.xpTotal, p.xpProximoNivel, p.nivel)}
          <div class="gami-detalhes">
            <div class="gami-detalhe-item">
              <span class="gami-detalhe-icone">✅</span>
              <span class="gami-detalhe-label">Livros aprovados</span>
              <span class="gami-detalhe-xp">+${p.xpLivrosAprovados} XP</span>
            </div>
            <div class="gami-detalhe-item">
              <span class="gami-detalhe-icone">🛒</span>
              <span class="gami-detalhe-label">Compras realizadas</span>
              <span class="gami-detalhe-xp">+${p.xpCompras} XP</span>
            </div>
            <div class="gami-detalhe-item">
              <span class="gami-detalhe-icone">⭐</span>
              <span class="gami-detalhe-label">Avaliações feitas</span>
              <span class="gami-detalhe-xp">+${p.xpAvaliacoes} XP</span>
            </div>
          </div>
        </div>`;
    } catch {
      container.innerHTML = `<p class="gami-aviso">Não foi possível carregar seu perfil.</p>`;
    }
  }

  /* -------------------------------------------------------
     RANKING TOP 5
  ------------------------------------------------------- */

  async function carregarRanking() {
    const lista = document.getElementById("gami-ranking-lista");
    const empty = document.getElementById("gami-ranking-vazio");
    if (!lista) return;

    try {
      const res = await fetch("/api/gamificacao/ranking");
      if (!res.ok) throw new Error("erro");
      const dados = await res.json();

      if (!dados || dados.length === 0) {
        if (empty) empty.style.display = "block";
        return;
      }

      const xpMax = dados[0].xpTotal || 1;

      lista.innerHTML = dados
        .map(
          (u) => `
        <li class="gami-rank-item" style="--pos:${u.posicao}">
          <span class="gami-rank-medal">${medalha(u.posicao)}</span>
          <span class="gami-rank-badge">${u.badge}</span>
          <div class="gami-rank-info">
            <span class="gami-rank-nome">${u.nome}</span>
            <div class="gami-rank-barra-track">
              <div class="gami-rank-barra-fill"
                   style="width:${Math.round((u.xpTotal / xpMax) * 100)}%;
                          background:${corBarra(u.posicao)}">
              </div>
            </div>
          </div>
          <span class="gami-rank-xp">${u.xpTotal} XP</span>
        </li>`
        )
        .join("");
    } catch {
      lista.innerHTML = `<li class="gami-aviso">Não foi possível carregar o ranking.</li>`;
    }
  }

  /* -------------------------------------------------------
     INICIALIZAÇÃO
  ------------------------------------------------------- */

  document.addEventListener("DOMContentLoaded", function () {
    carregarMeuPerfil();
    carregarRanking();
  });
})();
