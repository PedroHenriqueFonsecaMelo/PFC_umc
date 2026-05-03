/* ── Scroll reveal ── */
const observer = new IntersectionObserver((entries) => {
  entries.forEach((e) => {
    if (e.isIntersecting) e.target.classList.add("visible");
  });
}, { threshold: 0.12 });

document.querySelectorAll(".reveal").forEach((el) => observer.observe(el));

/* ── Livros em destaque ── */
async function carregarLivrosDestaque() {
  const grid = document.getElementById("livrosGrid");
  try {
    const res = await fetch("/api/livros/todos");
    if (!res.ok) throw new Error("Falha na API");
    const livros = await res.json();
    const destaque = livros.slice(0, 8);

    if (destaque.length === 0) {
      grid.innerHTML = `<p style="grid-column:1/-1;text-align:center;color:var(--muted);padding:3rem 0;font-style:italic;font-family:var(--fell)">
        Nenhum livro disponível no momento.
      </p>`;
      return;
    }

    grid.innerHTML = destaque.map((livro) => {
      let foto = null;
      try {
        const arr = JSON.parse(livro.fotosUrls);
        if (Array.isArray(arr) && arr.length > 0) foto = arr[0];
      } catch (_) {}

      const imgHTML = foto
        ? `<img src="${foto}" alt="${livro.titulo}" loading="lazy" />`
        : `📚`;

      return `
      <a class="livro-card" href="/livros/vitrine">
        <div class="livro-card-img">${imgHTML}</div>
        <div class="livro-card-body">
          <span class="livro-card-estado">${livro.estadoAprovado || "BOM"}</span>
          <div class="livro-card-titulo">${livro.titulo}</div>
          <div class="livro-card-autor">${livro.autor}</div>
          <div class="livro-card-preco">
            T$ ${(livro.precoAprovado || 0).toFixed(2)}
            <small> / token</small>
          </div>
        </div>
      </a>`;
    }).join("");
  } catch (err) {
    grid.innerHTML = `<p style="grid-column:1/-1;text-align:center;color:var(--muted);padding:3rem 0;font-style:italic;font-family:var(--fell)">
      Não foi possível carregar os livros. <a href="/livros/vitrine" style="color:var(--accent)">Ver vitrine completa →</a>
    </p>`;
  }
}

/* ── Ranking ── */
async function carregarRanking() {
  const list = document.getElementById("rankingList");
  try {
    const res = await fetch("/api/gamificacao/ranking");
    if (!res.ok) throw new Error();
    const ranking = await res.json();
    const maxXp = ranking.length > 0 ? ranking[0].xpTotal : 1;

    list.innerHTML = ranking.map((item, i) => {
      const topClass = i < 3 ? `top-${i + 1}` : "";
      const pct = Math.round((item.xpTotal / maxXp) * 100);
      const medalha = i === 0 ? "🥇" : i === 1 ? "🥈" : i === 2 ? "🥉" : item.badge || "📖";
      return `
      <div class="rank-item ${topClass}">
        <div class="rank-pos">${item.posicao}</div>
        <div class="rank-badge">${medalha}</div>
        <div class="rank-info">
          <div class="rank-nome">${item.nome}</div>
          <div class="rank-nivel">${item.nivel || "Leitor"}</div>
        </div>
        <div class="rank-xp-block">
          <div class="rank-xp">${item.xpTotal.toLocaleString("pt-BR")}</div>
          <div class="rank-xp-label">XP</div>
          <div class="rank-bar-wrap">
            <div class="rank-bar" style="width:${pct}%"></div>
          </div>
        </div>
      </div>`;
    }).join("");
  } catch (_) {
    list.innerHTML = `<p style="text-align:center;color:rgba(249,246,240,.3);padding:2rem 0;font-style:italic">
      Ranking indisponível no momento.
    </p>`;
  }
}

/* ── Nav — detecta sessão e troca estado ── */
async function carregarNavUsuario() {
  try {
    const res = await fetch("/clientes/meu-perfil-json", { credentials: "include" });
    if (!res.ok) return;
    const cliente = await res.json();
    const primeiroNome = (cliente.nome || "").split(" ")[0];

    const fotoHtml = cliente.fotoPerfil
      ? `<img src="${cliente.fotoPerfil}" style="width:84px;height:84px;border-radius:50%;object-fit:cover;vertical-align:middle;margin-right:.4rem;">`
      : "";
    document.getElementById("mastheadTagline").innerHTML = fotoHtml + "Olá, <strong>" + primeiroNome + "</strong>";
    document.getElementById("navGuest").style.display = "none";
    document.getElementById("navUser").style.display = "flex";
    document.getElementById("navMinhaConta").href = "/clientes/meu-perfil";
    const btn = document.getElementById("btnQueroVender");
    if (btn) btn.href = "/livros/vender";
  } catch (_) {}
}

/* ── Blog ── */
async function carregarBlog() {
  const grid = document.getElementById("blogGrid");
  const section = document.getElementById("blogSection");
  try {
    const res = await fetch("/api/blog");
    if (!res.ok) return;
    const posts = await res.json();
    if (posts.length === 0) return;

    section.style.display = "";

    if (window.location.hash === "#blogSection") {
      setTimeout(() => section.scrollIntoView({ behavior: "smooth" }), 100);
    }

    grid.innerHTML = posts.slice(0, 3).map((p) => {
      const imgHTML = p.imagemUrl
        ? `<img src="${p.imagemUrl}" alt="${p.titulo}" loading="lazy" />`
        : `<span style="font-size:2rem;opacity:.4">📝</span>`;
      return `
      <a href="/blog/${p.id}" class="blog-card-link" style="text-decoration:none;color:inherit;display:block">
        <article class="blog-card" style="cursor:pointer">
          <div class="blog-card-img">${imgHTML}</div>
          <div class="blog-card-body">
            <div class="blog-card-meta">
              <span>✍️ ${p.autorNome}</span>
              <span>·</span>
              <span>${p.dataPublicacao}</span>
            </div>
            <div class="blog-card-titulo">${p.titulo}</div>
            <div class="blog-card-conteudo">${p.conteudo.length > 120 ? p.conteudo.slice(0, 120) + '…' : p.conteudo}</div>
          </div>
        </article>
      </a>`;
    }).join("");

    const verTodosEl = document.getElementById("blogVerTodos");
    if (verTodosEl) verTodosEl.style.display = "";
  } catch (_) {}
}

/* ── Init ── */
carregarNavUsuario();
carregarLivrosDestaque();
carregarBlog();
carregarRanking();
