/* ── Scroll reveal ── */
const observer = new IntersectionObserver((entries) => {
  entries.forEach((e) => {
    if (e.isIntersecting) e.target.classList.add("visible");
  });
}, { threshold: 0.12 });

document.querySelectorAll(".reveal").forEach((el) => observer.observe(el));

/* ── Contador animado nas stats ── */
function animarContadores() {
  document.querySelectorAll(".stat-number").forEach((el) => {
    const raw = el.textContent.trim();
    const match = raw.match(/^[\d.]+/);
    if (!match) return;
    const target = parseFloat(match[0].replace(/\./g, "").replace(",", "."));
    if (isNaN(target) || target < 10) return; // ignora 4.9★
    const suffix = raw.slice(match[0].length);
    const isDecimal = match[0].includes(".");
    let start = null;
    const duration = 1400;
    function step(ts) {
      if (!start) start = ts;
      const p = Math.min((ts - start) / duration, 1);
      const ease = 1 - Math.pow(1 - p, 3);
      const cur = isDecimal
        ? (ease * target).toFixed(1)
        : Math.round(ease * target).toLocaleString("pt-BR");
      el.textContent = cur + suffix;
      if (p < 1) requestAnimationFrame(step);
    }
    requestAnimationFrame(step);
  });
}

const statsObs = new IntersectionObserver((entries) => {
  if (entries[0].isIntersecting) {
    animarContadores();
    statsObs.disconnect();
  }
}, { threshold: 0.4 });
const statsBar = document.querySelector(".stats-bar");
if (statsBar) statsObs.observe(statsBar);

/* ── Livros em destaque ── */
async function carregarLivrosDestaque() {
  const track = document.getElementById("carrosselTrack");
  const dotsWrap = document.getElementById("carrosselDots");
  const btnPrev = document.getElementById("carrosselPrev");
  const btnNext = document.getElementById("carrosselNext");
  if (!track) return;

  try {
    const res = await fetch("/api/livros/todos");
    if (!res.ok) throw new Error();
    const livros = await res.json();
    const destaque = livros.slice(0, 10);
    if (destaque.length === 0) return;

    let activeIndex = Math.min(3, Math.floor(destaque.length / 2));

    // Monta slides
    destaque.forEach((livro, i) => {
      let foto = null;
      try {
        const arr = JSON.parse(livro.fotosUrls);
        if (Array.isArray(arr) && arr.length > 0) foto = arr[0];
      } catch (_) {}

      // Tenta capa via OpenLibrary se não tiver foto
      const capaUrl = foto ||
        (livro.isbn
          ? `https://covers.openlibrary.org/b/isbn/${
            livro.isbn.replace(/-/g, "")
          }-M.jpg`
          : null);

      const slide = document.createElement("div");
      slide.className = "carousel-slide";
      slide.dataset.index = i;
      slide.innerHTML = `
        <div class="carousel-slide-inner">
          <div class="carousel-img-wrap">
            ${
        capaUrl
          ? `<img src="${capaUrl}" alt="${livro.titulo}" loading="lazy"
                      onerror="this.parentElement.innerHTML='<span class=\\'carousel-no-img\\'>📚</span>'">`
          : `<span class="carousel-no-img">📚</span>`
      }
          </div>
          <div class="carousel-slide-info">
            <div class="carousel-slide-titulo">${livro.titulo}</div>
            <div class="carousel-slide-autor">${livro.autor || ""}</div>
          </div>
        </div>`;
      slide.addEventListener("click", () => {
        if (parseInt(slide.dataset.index) !== activeIndex) {
          goTo(parseInt(slide.dataset.index));
        } else {
          window.location.href = `/livros/${livro.id}`;
        }
      });
      track.appendChild(slide);
    });

    // Dots
    destaque.forEach((_, i) => {
      const dot = document.createElement("div");
      dot.className = "carousel-dot";
      dot.addEventListener("click", () => goTo(i));
      dotsWrap.appendChild(dot);
    });

    function updateCarousel() {
      const slides = track.querySelectorAll(".carousel-slide");
      const dots = dotsWrap.querySelectorAll(".carousel-dot");
      const total = slides.length;
      const slideW = slides[0] ? slides[0].offsetWidth : 220;
      const wrapW = track.parentElement.offsetWidth;

      // Centraliza o slide ativo na tela
      const offsetX = (wrapW / 2) - (activeIndex * slideW) - (slideW / 2);
      track.style.transform = `translateX(${offsetX}px)`;

      slides.forEach((slide, i) => {
        const diff = i - activeIndex;
        const rotY = diff * 45;
        const scale = i === activeIndex ? 1 : 0.82;
        const isActive = i === activeIndex;
        const opacity = Math.abs(diff) > 2 ? 0 : 1;

        slide.style.transform = `rotateY(${rotY}deg) scale(${scale})`;
        slide.style.zIndex = isActive ? 10 : Math.max(1, 8 - Math.abs(diff));
        slide.style.opacity = opacity;

        const info = slide.querySelector(".carousel-slide-info");
        if (info) {
          info.style.opacity = isActive ? "1" : "0";
          info.style.filter = isActive ? "blur(0)" : "blur(3px)";
        }
      });

      dots.forEach((dot, i) => {
        dot.classList.toggle("active", i === activeIndex);
      });

      if (btnPrev) btnPrev.disabled = activeIndex === 0;
      if (btnNext) btnNext.disabled = activeIndex === total - 1;
    }

    function goTo(index) {
      activeIndex = Math.max(0, Math.min(destaque.length - 1, index));
      updateCarousel();
    }

    if (btnPrev) btnPrev.addEventListener("click", () => goTo(activeIndex - 1));
    if (btnNext) btnNext.addEventListener("click", () => goTo(activeIndex + 1));

    // Swipe touch
    let touchStartX = 0;
    track.addEventListener("touchstart", (e) => {
      touchStartX = e.touches[0].clientX;
    });
    track.addEventListener("touchend", (e) => {
      const diff = touchStartX - e.changedTouches[0].clientX;
      if (Math.abs(diff) > 40) goTo(activeIndex + (diff > 0 ? 1 : -1));
    });

    updateCarousel();
  } catch (_) {
    if (track) {
      track.innerHTML = `<p style="grid-column:1/-1;text-align:center;
      color:var(--muted);padding:3rem 0;font-style:italic;">
      <a href="/livros/vitrine" style="color:var(--accent)">Ver vitrine completa →</a></p>`;
    }
  }
}

/* ── Ranking homepage — pódio top 3 ── */
async function carregarRanking() {
  const container = document.getElementById("rankingList");
  try {
    const res = await fetch("/api/ranking/top?limite=3&periodo=mes");
    if (!res.ok) throw new Error();
    const ranking = await res.json();

    if (ranking.length === 0) {
      container.innerHTML =
        `<p style="text-align:center;color:rgba(249,246,240,.3);padding:2rem 0;font-style:italic">
        Nenhum leitor no ranking ainda.</p>`;
      return;
    }

    const medalha = (pos) => ["🥇", "🥈", "🥉"][pos - 1] || "";
    const inicial = (nome) => (nome || "?").charAt(0).toUpperCase();
    const esc = (s) =>
      String(s || "").replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(
        />/g,
        "&gt;",
      );

    container.innerHTML = ranking.map((u) => {
      const nomeCurto = esc(u.primeiroNome) +
        (u.inicialSobrenome ? " " + esc(u.inicialSobrenome) : "");
      const avatarHtml = u.fotoPerfil
        ? `<img src="${
          esc(u.fotoPerfil)
        }" alt="${nomeCurto}" class="rhi-avatar-img">`
        : `<div class="rhi-avatar">${inicial(u.primeiroNome)}</div>`;
      return `
      <div class="rhi-card pos-${u.posicao}">
        <div class="rhi-medalha">${medalha(u.posicao)}</div>
        ${avatarHtml}
        <div class="rhi-nome" title="${nomeCurto}">${nomeCurto}</div>
        <div class="rhi-nivel">${esc(u.nivel)}</div>
        <div class="rhi-xp">${u.xpTotal.toLocaleString("pt-BR")}</div>
        <div class="rhi-xp-label">XP</div>
      </div>`;
    }).join("");
  } catch (_) {
    container.innerHTML =
      `<p style="text-align:center;color:rgba(249,246,240,.3);padding:2rem 0;font-style:italic">
      Ranking indisponível no momento.</p>`;
  }
}

/* ── Nav — detecta sessão e troca estado ── */
async function carregarNavUsuario() {
  try {
    const res = await fetch("/clientes/meu-perfil-json", {
      credentials: "include",
    });

    if (res.ok) {
      const cliente = await res.json();
      const primeiroNome = (cliente.nome || "").split(" ")[0];
      const fotoHtml = cliente.fotoPerfil
        ? `<img src="${cliente.fotoPerfil}" style="width:84px;height:84px;border-radius:50%;object-fit:cover;vertical-align:middle;margin-right:.4rem;">`
        : "";
      document.getElementById("mastheadTagline").innerHTML = fotoHtml +
        "Olá, <strong>" + primeiroNome + "</strong>";
      document.getElementById("navGuest").style.display = "none";
      document.getElementById("navUser").style.display = "flex";
      document.getElementById("navMinhaConta").href = "/clientes/meu-perfil";
      const btn = document.getElementById("btnQueroVender");
      if (btn) btn.href = "/livros/vender";
      // Botão do ranking: usuário logado → "Ver ranking completo"
      const ctaRanking = document.getElementById("rankingCtaBtn");
      if (ctaRanking) {
        ctaRanking.href = "/ranking";
        ctaRanking.textContent = "Ranking Completo";
      }
      return;
    }

    // 404 → pode ser admin (não está na tabela de clientes)
    if (res.status === 404) {
      const adminRes = await fetch("/api/admin/me", { credentials: "include" });
      if (!adminRes.ok) return;
      const admin = await adminRes.json();
      const primeiroNome = (admin.nome || "Administrador").split(" ")[0];
      document.getElementById("mastheadTagline").innerHTML = "Olá, <strong>" +
        primeiroNome + "</strong> · Admin";
      document.getElementById("navGuest").style.display = "none";
      document.getElementById("navUser").style.display = "flex";
      // Redireciona links do nav para o painel
      const perfilLink = document.querySelector("#navUser a");
      if (perfilLink) {
        perfilLink.href = "/admin/painel";
        perfilLink.textContent = "Painel Admin";
      }
      document.getElementById("navMinhaConta").href = "/admin/painel";
      document.getElementById("navMinhaConta").textContent = "— Painel Admin";
      // Oculta Vitrine e Vender — admin não é cliente
      document.querySelectorAll(".masthead-nav a:not(#navMinhaConta)").forEach(
        function (el) {
          el.style.display = "none";
        },
      );
      // Redireciona todos os CTAs de cliente para o painel admin
      ["/livros/vitrine", "/livros/vender", "/clientes/novo-cadastro"].forEach(
        function (path) {
          document.querySelectorAll('a[href="' + path + '"]').forEach(
            function (el) {
              el.href = "/admin/painel";
            },
          );
        },
      );
    }
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

    grid.innerHTML = posts.slice(0, 3).map((p, i) => {
      const imgHTML = p.imagemUrl
        ? `<img src="${p.imagemUrl}" alt="${p.titulo}" loading="lazy" />`
        : `<span style="font-size:2rem;opacity:.4">📝</span>`;
      return `
      <a href="/blog/${p.id}" class="blog-card-link reveal" style="text-decoration:none;color:inherit;display:block;--delay:${i * 0.12}s">
        <article class="blog-card" style="cursor:pointer">
          <div class="blog-card-img">${imgHTML}</div>
          <div class="blog-card-body">
            <div class="blog-card-meta">
              <span>✍️ ${p.autorNome}</span>
              <span>·</span>
              <span>${p.dataPublicacao}</span>
            </div>
            <div class="blog-card-titulo">${p.titulo}</div>
            <div class="blog-card-conteudo">${
        p.conteudo.length > 120 ? p.conteudo.slice(0, 120) + "…" : p.conteudo
      }</div>
          </div>
        </article>
      </a>`;
    }).join("");

    grid.querySelectorAll(".reveal").forEach((el) => observer.observe(el));
    const verTodosEl = document.getElementById("blogVerTodos");
    if (verTodosEl) verTodosEl.style.display = "";
  } catch (_) {}
}

/* ── Flash: lote enviado ── */
(function () {
  if (new URLSearchParams(window.location.search).get("enviado") === "1") {
    const banner = document.getElementById("flashEnviado");
    if (banner) {
      banner.style.display = "flex";
      // limpa o param da URL sem recarregar a página
      history.replaceState(null, "", window.location.pathname);
      // fecha automaticamente após 8 s
      const timer = setTimeout(() => banner.style.display = "none", 8000);
      document.getElementById("flashClose").addEventListener("click", () => {
        clearTimeout(timer);
        banner.style.display = "none";
      });
    }
  }
})();

/* ── Init ── */
carregarNavUsuario();
carregarLivrosDestaque();
carregarBlog();
carregarRanking();
