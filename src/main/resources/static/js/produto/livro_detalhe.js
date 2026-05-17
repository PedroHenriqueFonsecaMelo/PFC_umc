/* ================================================================
   livro_detalhe.js — Página de detalhes do livro
   Carrega dados via GET /api/livros/{id} e gerencia a estante
   (localStorage) com "Adicionar à Estante" e "Comprar agora".
   ================================================================ */

const CART_KEY      = 'bibliotroca_carrinho';
const CHECKOUT_KEY  = 'bibliotroca_checkout_ids';

/* ── Helpers do carrinho ── */

function getCarrinho() {
    try { return JSON.parse(localStorage.getItem(CART_KEY)) || []; }
    catch (_) { return []; }
}

function saveCarrinho(itens) {
    localStorage.setItem(CART_KEY, JSON.stringify(itens));
}

function jaEstaNoCarrinho(id) {
    return getCarrinho().some(i => i.id === id);
}

function adicionarAoCarrinho(livro) {
    if (jaEstaNoCarrinho(livro.id)) return false;
    const carr = getCarrinho();
    carr.push(livro);
    saveCarrinho(carr);
    return true;
}

/* ── Extrai o ID do livro da URL (/livros/123) ── */
function extrairLivroId() {
    const partes = window.location.pathname.split('/').filter(Boolean);
    const id = parseInt(partes[partes.length - 1], 10);
    return isNaN(id) ? null : id;
}

/* ── Classe CSS por estado ── */
function classeEstado(estado) {
    const mapa = { 'ÓTIMO': 'otimo', 'OTIMO': 'otimo', 'BOM': 'bom',
                   'COMO_NOVO': 'otimo', 'REGULAR': 'regular', 'RUIM': 'ruim' };
    return mapa[(estado || '').toUpperCase()] || 'bom';
}

/* ── Saldo na navbar ── */
function carregarSaldo() {
    fetch('/clientes/meu-perfil-json', { credentials: 'include' })
        .then(r => r.ok ? r.json() : null)
        .then(c => {
            if (!c) return;
            const el = document.getElementById('navSaldo');
            if (el) el.textContent = `T$ ${(c.saldoTokens || 0).toFixed(2)}`;
        }).catch(() => {});
}

/* ── Toast ── */
function mostrarToast(msg, tipo) {
    const t = document.getElementById('toastDetalhe');
    if (!t) return;
    t.className = 'toast toast-' + (tipo || 'info');
    t.innerHTML = msg;
    t.style.display = 'block';
    setTimeout(() => { t.style.display = 'none'; }, 3000);
}

/* ── Contador regressivo de promoção ── */
let _timerInterval = null;

function iniciarCountdown(expiraISO) {
    const timerEl = document.getElementById('promoTimer');
    const bloco   = document.getElementById('promoCountdown');
    if (!timerEl || !bloco) return;

    bloco.style.display = 'block';

    const atualizar = () => {
        const diff = new Date(expiraISO).getTime() - Date.now();
        if (diff <= 0) {
            bloco.style.display = 'none';
            clearInterval(_timerInterval);
            return;
        }
        const h = Math.floor(diff / 3600000);
        const m = Math.floor((diff % 3600000) / 60000);
        const s = Math.floor((diff % 60000) / 1000);
        timerEl.textContent = `${h}h ${m}m ${s}s`;
    };

    atualizar();
    _timerInterval = setInterval(atualizar, 1000);
}

/* ── Livro 3D: virar ── */
window.toggleFlip = function() {
    document.getElementById('bookInner').classList.toggle('flipped');
};

/* ── Livro 3D: preenche capa, verso e lombo com dados do livro ── */
function renderBookCover(livro) {
    // Lombo
    document.getElementById('bookSpineText').textContent = livro.titulo + ' · ' + livro.autor;

    // Placeholder (frente sem imagem)
    document.getElementById('placeholderTitle').textContent  = livro.titulo;
    document.getElementById('placeholderAuthor').textContent = livro.autor;

    // Verso: resumo truncado + ISBN
    const resumo = livro.resumoOficial || 'Sem descrição disponível para este livro.';
    document.getElementById('bookBackText').textContent  = resumo.length > 400 ? resumo.slice(0, 400) + '…' : resumo;
    document.getElementById('bookBackIsbn').textContent  = livro.isbn ? 'ISBN ' + livro.isbn : '';

    // Capa: tenta OpenLibrary pelo ISBN primeiro; cai para primeira foto do usuário
    const img  = document.getElementById('bookCoverImg');
    const ph   = document.getElementById('bookPlaceholder');

    function usarFotoUsuario() {
        try {
            const fotos = JSON.parse(livro.fotosUrls || '[]');
            if (fotos.length > 0) {
                img.src = fotos[0];
                img.style.display = 'block';
                ph.style.display  = 'none';
            }
        } catch(_) {}
    }

    if (livro.isbn) {
        // Tenta Google Books primeiro (melhor qualidade), depois OpenLibrary
        buscarDadosGoogleBooks(livro.isbn).then(gbData => {
            if (gbData && gbData.capaUrl) {
                img.src = gbData.capaUrl;
                img.onload = () => { img.style.display = 'block'; ph.style.display = 'none'; };
                img.onerror = () => {
                    img.src = 'https://covers.openlibrary.org/b/isbn/' + livro.isbn.replace(/-/g, '') + '-L.jpg';
                    img.onload = () => { img.style.display = 'block'; ph.style.display = 'none'; };
                    img.onerror = usarFotoUsuario;
                };
            } else {
                img.src = 'https://covers.openlibrary.org/b/isbn/' + livro.isbn.replace(/-/g, '') + '-L.jpg';
                img.onload = () => { img.style.display = 'block'; ph.style.display = 'none'; };
                img.onerror = usarFotoUsuario;
            }
        }).catch(() => {
            img.src = 'https://covers.openlibrary.org/b/isbn/' + livro.isbn.replace(/-/g, '') + '-L.jpg';
            img.onload = () => { img.style.display = 'block'; ph.style.display = 'none'; };
            img.onerror = usarFotoUsuario;
        });
    } else {
        usarFotoUsuario();
    }
}

/* ── Galeria de fotos ── */
function renderGaleria(fotos) {
    const fotoMain   = document.getElementById('fotoMain');
    const thumbsCont = document.getElementById('galeriaThumbs');
    if (!fotoMain) return;

    const primeira = fotos.length > 0 ? fotos[0] : 'https://via.placeholder.com/400x480?text=📚';
    fotoMain.src = primeira;
    fotoMain.onerror = () => { fotoMain.src = 'https://via.placeholder.com/400x480?text=📚'; };

    if (fotos.length > 1 && thumbsCont) {
        thumbsCont.innerHTML = fotos.map((url, i) =>
            `<img src="${url}" class="galeria-thumb ${i === 0 ? 'ativa' : ''}"
                  alt="Foto ${i + 1}"
                  onclick="trocarFoto('${url}', this)"
                  onerror="this.style.display='none'">`
        ).join('');
    }
}

window.trocarFoto = function(url, thumb) {
    document.getElementById('fotoMain').src = url;
    document.querySelectorAll('.galeria-thumb').forEach(t => t.classList.remove('ativa'));
    thumb.classList.add('ativa');
};

/* ── Exibe seção "Sobre o livro" com truncamento opcional ── */
const RESUMO_MAX = 400;

function exibirResumo(texto) {
    const secaoEl = document.getElementById('livroResumo');
    const textoEl = document.getElementById('livroResumoTexto');
    const btnEl   = document.getElementById('btnLerMais');
    if (!secaoEl || !textoEl) return;

    const truncar = texto.length > RESUMO_MAX;
    textoEl.dataset.full  = texto;
    textoEl.dataset.short = truncar ? texto.slice(0, RESUMO_MAX) + '…' : texto;
    textoEl.textContent   = textoEl.dataset.short;
    textoEl.dataset.expandido = 'false';

    if (btnEl) btnEl.style.display = truncar ? 'inline-block' : 'none';
    secaoEl.style.display = 'block';
}

window.toggleLerMais = function () {
    const textoEl = document.getElementById('livroResumoTexto');
    const btnEl   = document.getElementById('btnLerMais');
    if (!textoEl || !btnEl) return;
    const expandido = textoEl.dataset.expandido === 'true';
    if (expandido) {
        textoEl.textContent       = textoEl.dataset.short;
        textoEl.dataset.expandido = 'false';
        btnEl.textContent         = 'Ler mais';
    } else {
        textoEl.textContent       = textoEl.dataset.full;
        textoEl.dataset.expandido = 'true';
        btnEl.textContent         = 'Ler menos';
    }
};

/* ── Galeria de fotos do vendedor ── */
function renderFotosVendedor(fotos) {
    const section = document.getElementById('fotosVendedorSection');
    const grid    = document.getElementById('fotosVendedorGrid');
    if (!section || !grid || fotos.length === 0) return;

    grid.innerHTML = fotos.map((url, i) =>
        `<img src="${url}" class="foto-thumb" alt="Foto ${i + 1}"
              onerror="this.style.display='none'"
              onclick="abrirLightbox(${i})">`
    ).join('');

    section.style.display = 'block';
}

/* ── Lightbox ── */
let _lightboxFotos  = [];
let _lightboxIndice = 0;

function abrirLightbox(indice) {
    _lightboxIndice = indice;
    const overlay = document.getElementById('lightboxOverlay');
    const img     = document.getElementById('lightboxImg');
    if (!overlay || !img) return;
    img.src = _lightboxFotos[indice];
    overlay.style.display = 'flex';
    document.body.style.overflow = 'hidden';
}

window.fecharLightbox = function() {
    const overlay = document.getElementById('lightboxOverlay');
    if (overlay) overlay.style.display = 'none';
    document.body.style.overflow = '';
};

window.fecharLightboxClick = function(e) {
    if (e.target === document.getElementById('lightboxOverlay')) fecharLightbox();
};

window.navegarLightbox = function(dir) {
    _lightboxIndice = (_lightboxIndice + dir + _lightboxFotos.length) % _lightboxFotos.length;
    const img = document.getElementById('lightboxImg');
    if (img) img.src = _lightboxFotos[_lightboxIndice];
};

/* ── Busca dados enriquecidos na Google Books API ── */
async function buscarDadosGoogleBooks(isbn) {
    try {
        const res = await fetch(`/api/books/isbn/${encodeURIComponent(isbn)}`);
        if (!res.ok) return null;
        const data = await res.json();
        if (!data.items || !data.items[0]) return null;
        const info = data.items[0].volumeInfo;
        return {
            descricao:      info.description   || null,
            genero:         info.categories    ? info.categories[0] : null,
            paginas:        info.pageCount     || null,
            editora:        info.publisher     || null,
            dataPublicacao: info.publishedDate || null,
            capaUrl:        info.imageLinks
                ? (info.imageLinks.large || info.imageLinks.thumbnail)
                : null,
            idioma:         info.language      || null,
        };
    } catch (_) {
        return null;
    }
}

// Mapeamento de gêneros inglês → português
const GENERO_MAP = {
    'Fiction': 'Ficção', 'Juvenile Fiction': 'Infantojuvenil',
    'Young Adult Fiction': 'Jovem Adulto', 'Science Fiction': 'Ficção Científica',
    'Fantasy': 'Fantasia', 'Mystery': 'Mistério', 'Thriller': 'Suspense',
    'Horror': 'Terror', 'Romance': 'Romance', 'Historical Fiction': 'Ficção Histórica',
    'Adventure': 'Aventura', 'Biography & Autobiography': 'Biografia',
    'Biography': 'Biografia', 'History': 'História', 'Philosophy': 'Filosofia',
    'Psychology': 'Psicologia', 'Self-Help': 'Autoajuda',
    'Business & Economics': 'Negócios', 'Science': 'Ciências',
    'Technology': 'Tecnologia', 'Computers': 'Computação', 'Art': 'Arte',
    'Poetry': 'Poesia', 'Drama': 'Drama',
    'Comics & Graphic Novels': 'Quadrinhos', 'Religion': 'Religião',
    'Cooking': 'Culinária', 'Sports & Recreation': 'Esportes',
    'Nonfiction': 'Não-ficção', 'Literary Collections': 'Literatura',
    'Political Science': 'Política', 'Medical': 'Medicina', 'Law': 'Direito',
    'Humor': 'Humor', 'Education': 'Educação', 'Nature': 'Natureza',
};

function traduzirGenero(generoEn) {
    if (!generoEn) return null;
    for (const [en, pt] of Object.entries(GENERO_MAP)) {
        if (generoEn.includes(en)) return pt;
    }
    return generoEn;
}

function formatarDataPublicacao(data) {
    if (!data) return null;
    if (data.length === 4) return data; // só ano
    try {
        const d = new Date(data);
        return d.toLocaleDateString('pt-BR', { year: 'numeric', month: 'long', day: 'numeric' });
    } catch (_) { return data; }
}

/* ── Exibe metadados enriquecidos do Google Books ── */
function exibirMetadados(livro, gbData) {
    // Gênero
    const genPt = livro.genero || traduzirGenero(gbData.genero);
    if (genPt) {
        const el = document.getElementById('livroGeneroTag');
        if (el) { el.textContent = genPt; el.style.display = 'inline-flex'; }
    }

    // Grid de metadados
    const grid = document.getElementById('livroMetaGrid');
    if (!grid) return;

    const itens = [];

    if (gbData.paginas) {
        itens.push({ icone: '📄', label: 'Páginas', valor: gbData.paginas + ' páginas' });
    }
    if (gbData.editora) {
        itens.push({ icone: '🏢', label: 'Editora', valor: gbData.editora });
    }
    if (gbData.dataPublicacao) {
        itens.push({ icone: '📅', label: 'Publicação', valor: formatarDataPublicacao(gbData.dataPublicacao) });
    }
    if (gbData.idioma) {
        const idiomas = { 'pt': 'Português', 'en': 'Inglês', 'es': 'Espanhol',
                          'fr': 'Francês', 'de': 'Alemão', 'it': 'Italiano' };
        itens.push({ icone: '🌐', label: 'Idioma', valor: idiomas[gbData.idioma] || gbData.idioma });
    }

    if (itens.length === 0) return;

    grid.innerHTML = itens.map(i => `
        <div class="meta-item">
            <span class="meta-icone">${i.icone}</span>
            <div class="meta-info">
                <span class="meta-label">${i.label}</span>
                <span class="meta-valor">${i.valor}</span>
            </div>
        </div>
    `).join('');

    grid.style.display = 'grid';
}

/* ── Renderiza os dados do livro na página ── */
function renderLivro(livro) {
    // Livro 3D
    renderBookCover(livro);

    // Fotos do vendedor
    let fotos = [];
    try {
        const arr = JSON.parse(livro.fotosUrls);
        if (Array.isArray(arr) && arr.length > 0) fotos = arr;
    } catch (_) {}
    _lightboxFotos = fotos;
    renderGaleria(fotos);
    renderFotosVendedor(fotos);

    // Badge de promoção
    if (livro.emPromocao) {
        const badge = document.getElementById('badgePromo');
        if (badge) badge.style.display = 'inline-block';
    }

    // Títulos
    document.getElementById('livroTitulo').textContent = livro.titulo || '—';
    document.getElementById('livroAutor').textContent  = `por ${livro.autor || '—'}`;
    document.getElementById('livroIsbn').textContent   = `ISBN: ${livro.isbn || '—'}`;

    // Breadcrumb
    const bcr = document.getElementById('bcrTitulo');
    if (bcr) bcr.textContent = livro.titulo || '';

    // Título da aba
    document.title = `${livro.titulo || 'Livro'} — Bibliotroca`;

    // Badge de estado
    const estado = livro.estadoAprovado || 'BOM';
    const badgeEl = document.getElementById('badgeEstado');
    if (badgeEl) {
        badgeEl.textContent = estado.replace('_', ' ');
        badgeEl.className   = `livro-estado-badge estado-${classeEstado(estado)}`;
    }

    // Preço
    const precoDiv = document.getElementById('precoHtml');
    if (precoDiv) {
        if (livro.emPromocao && livro.precoOriginal) {
            precoDiv.innerHTML =
                `<span style="text-decoration:line-through;color:#9a8a80;font-size:1.3rem;
                              margin-right:.5rem;">T$ ${livro.precoOriginal.toFixed(2)}</span>` +
                `<span style="color:#e11d48;">T$ ${(livro.precoAprovado || 0).toFixed(2)}</span>`;
        } else {
            precoDiv.textContent = `T$ ${(livro.precoAprovado || 0).toFixed(2)}`;
        }
    }

    // Countdown
    if (livro.emPromocao && livro.promocaoExpira) {
        iniciarCountdown(livro.promocaoExpira);
    }

    // Resumo + metadados via Google Books
    if (livro.isbn) {
        buscarDadosGoogleBooks(livro.isbn).then(gbData => {
            // Resumo
            const descFinal = (livro.resumoOficial && livro.resumoOficial.trim())
                ? livro.resumoOficial.trim()
                : (gbData && gbData.descricao ? gbData.descricao : null);
            if (descFinal) exibirResumo(descFinal);

            // Metadados enriquecidos
            if (gbData) exibirMetadados(livro, gbData);
        }).catch(() => {
            if (livro.resumoOficial && livro.resumoOficial.trim()) {
                exibirResumo(livro.resumoOficial.trim());
            }
        });
    } else if (livro.resumoOficial && livro.resumoOficial.trim()) {
        exibirResumo(livro.resumoOficial.trim());
    }

    // Gênero do banco (se já tiver)
    if (livro.genero) {
        const el = document.getElementById('livroGeneroTag');
        if (el) { el.textContent = livro.genero; el.style.display = 'inline-flex'; }
    }

    // Estado dos botões: já na estante?
    atualizarBotaoEstante(livro.id);
}

/* ── Atualiza visual do botão (alterna entre Adicionar e Remover) ── */
function atualizarBotaoEstante(livroId) {
    const btn   = document.getElementById('btnEstante');
    const aviso = document.getElementById('avisoEstante');
    if (!btn) return;

    const textEl = btn.querySelector('.atce__text');

    if (jaEstaNoCarrinho(livroId)) {
        btn.classList.add('na-estante');
        btn.classList.remove('remover');
        if (textEl) textEl.textContent = 'Na Estante';
        btn.title = 'Clique para remover da estante';
        if (aviso) aviso.style.display = 'block';
    } else {
        btn.classList.remove('na-estante');
        btn.classList.remove('remover');
        if (textEl) textEl.textContent = 'Add Estante';
        btn.title = '';
        if (aviso) aviso.style.display = 'none';
    }
}

/* ── Helper: atualiza o badge do ícone livro na navbar ── */
function atualizarBadgeNav(adicionou) {
    if (typeof window.cnAtualizarBadgeEstante === 'function') {
        window.cnAtualizarBadgeEstante();
    }
    if (adicionou && typeof window.cnAnimarEstante === 'function') {
        window.cnAnimarEstante();
    }
}

/* ── Handler: Adicionar à Estante / Remover da Estante (toggle) ── */
window.handleAdicionarEstante = function() {
    if (!_livroAtual) return;

    if (jaEstaNoCarrinho(_livroAtual.id)) {
        // Remove do carrinho
        saveCarrinho(getCarrinho().filter(i => i.id !== _livroAtual.id));
        atualizarBotaoEstante(_livroAtual.id);
        atualizarBadgeNav(false);
        mostrarToast(`<strong>${_livroAtual.titulo}</strong> removido da estante.`, 'aviso');
    } else {
        // Adiciona ao carrinho
        adicionarAoCarrinho(_livroAtual);
        atualizarBotaoEstante(_livroAtual.id);
        atualizarBadgeNav(true);
        mostrarToast(
            `<strong>${_livroAtual.titulo}</strong> adicionado à estante!`,
            'info'
        );
    }
};

/* ── Handler: Comprar agora ── */
window.handleComprarAgora = function() {
    if (!_livroAtual) return;

    // Compra direta: NÃO adiciona ao localStorage (estante/mini carrinho).
    // O objeto completo vai no sessionStorage para o checkout usar sem depender do localStorage.
    sessionStorage.setItem(CHECKOUT_KEY,           JSON.stringify([_livroAtual.id]));
    sessionStorage.setItem(CHECKOUT_KEY + '_direto', JSON.stringify([_livroAtual]));
    window.location.href = '/livros/checkout';
};

/* ── Carrega o livro da API ── */
let _livroAtual = null;

async function carregarLivro(id) {
    const loading    = document.getElementById('estadoCarregando');
    const notFound   = document.getElementById('estadoNaoEncontrado');
    const conteudo   = document.getElementById('conteudoLivro');

    try {
        const res = await fetch(`/api/livros/${id}`);

        if (res.status === 404) {
            if (loading)  loading.style.display  = 'none';
            if (notFound) notFound.style.display  = 'block';
            return;
        }

        if (!res.ok) throw new Error(`HTTP ${res.status}`);

        const livro = await res.json();
        _livroAtual = livro;

        renderLivro(livro);

        if (loading)  loading.style.display  = 'none';
        if (conteudo) conteudo.style.display  = 'grid';

    } catch (err) {
        console.error('Erro ao carregar livro:', err);
        if (loading)  loading.style.display  = 'none';
        if (notFound) notFound.style.display  = 'block';
    }
}

/* ── Init ── */
document.addEventListener('DOMContentLoaded', () => {
    carregarSaldo();

    const id = extrairLivroId();
    if (!id) {
        document.getElementById('estadoCarregando').style.display = 'none';
        document.getElementById('estadoNaoEncontrado').style.display = 'block';
        return;
    }

    carregarLivro(id);

    // Teclado: Escape fecha, setas navegam no lightbox
    document.addEventListener('keydown', (e) => {
        const overlay = document.getElementById('lightboxOverlay');
        if (!overlay || overlay.style.display === 'none') return;
        if (e.key === 'Escape')      fecharLightbox();
        if (e.key === 'ArrowLeft')   navegarLightbox(-1);
        if (e.key === 'ArrowRight')  navegarLightbox(1);
    });
});

/* ── Botão ATC Estante com animação GSAP ── */
(function initAtcEstante() {
  // gsap já está disponível globalmente via script tag
  const gsap = window.gsap;
  if (!gsap) return;

  const btn = document.getElementById('btnEstante');
  if (!btn) return;

  const text           = btn.querySelector('.atce__text');
  const cart           = btn.querySelector('.atce__cart');
  const cartContent    = btn.querySelector('.atce__cart-content');
  const dummy          = btn.querySelector('.atce__cart--dummy');
  const check          = btn.querySelector('.atce__check');
  const animBorder     = btn.querySelector('.atce__border--animated');
  const staticBorder   = btn.querySelector('.atce__border--static');
  const completeBorder = btn.querySelector('.atce__border--complete');

  let running = false;

  gsap.set(cartContent, { y: -24 });

  // Sincroniza visual com localStorage
  function syncVisual() {
    if (!_livroAtual) return;
    const naEstante = jaEstaNoCarrinho(_livroAtual.id);

    // Reseta TODAS as transformações do GSAP antes de mudar estado
    gsap.killTweensOf([cart, text, check, cartContent,
                       animBorder, staticBorder, completeBorder]);
    gsap.set(cart,          { x: 0, rotate: 0, opacity: 1, clearProps: 'all' });
    gsap.set(text,          { x: 0, opacity: 1, filter: 'blur(0px)', clearProps: 'transform,filter' });
    gsap.set(check,         { opacity: 0, scale: 1, clearProps: 'all' });
    gsap.set(cartContent,   { y: -24 });
    gsap.set(animBorder,    { opacity: 1 });
    gsap.set(staticBorder,  { opacity: 0 });
    gsap.set(completeBorder,{ opacity: 0 });

    if (naEstante) {
      btn.classList.add('na-estante');
      if (text) text.textContent = 'Na Estante';
      check.style.opacity = '1';
      cart.style.display  = 'none';
      btn.title = 'Clique para remover da estante';
    } else {
      btn.classList.remove('na-estante');
      if (text) text.textContent = 'Add Estante';
      check.style.opacity = '0';
      cart.style.display  = 'inline-block';
      gsap.set(cart, { x: 0, rotate: 0, opacity: 1 });
      btn.title = '';
    }
  }

  // Animação de adicionar (carrinho voa)
  function animarAdicionar(onComplete) {
    const dummyRect = dummy.getBoundingClientRect();
    const cartRect  = cart.getBoundingClientRect();
    const distX     = dummyRect.left - cartRect.left;

    gsap.timeline({ onComplete })
      .to(cart, { x: distX, duration: 0.22 })
      .to(cart, { rotate: -20, yoyo: true, repeat: 1, duration: 0.11 }, 0)
      .to(text, { opacity: 0, x: distX, duration: 0.22, filter: 'blur(6px)' }, 0)
      .to(cartContent, { y: 0, duration: 0.1, delay: 0.1 })
      .to(staticBorder, { opacity: 1, duration: 0.1 }, '<')
      .set(animBorder, { opacity: 0 })
      .to(cart, { x: distX * 4, duration: 0.6, delay: 0.1 })
      .to(cart, { rotate: -30, duration: 0.1 }, '<')
      .to(completeBorder, { opacity: 1, duration: 0.22 }, '<')
      .to(check, {
        opacity: 1, scale: 1.3, duration: 0.25,
        yoyo: true, repeat: 1, repeatDelay: 0.1
      }, '<')
      .set(cart,        { x: 0, rotate: 0, opacity: 0 })
      .set(cartContent, { y: -24 })
      .set(text,        { x: 0 })
      .to([staticBorder, completeBorder], { opacity: 0, duration: 0.4, delay: 0.1 })
      .to(text, { opacity: 1, duration: 0.22, filter: 'blur(0px)' })
      .to(animBorder, { opacity: 1, duration: 0.5 });
  }

  // Animação de remover (simples)
  function animarRemover(onComplete) {
    gsap.timeline({ onComplete })
      .to(btn, { scale: 0.97, duration: 0.1 })
      .to(btn, { scale: 1.0,  duration: 0.2 });
  }

  // Clique no botão
  btn.addEventListener('click', () => {
    if (running || !_livroAtual) return;
    running = true;

    const naEstante = jaEstaNoCarrinho(_livroAtual.id);

    if (naEstante) {
      animarRemover(() => {
        handleAdicionarEstante();
        syncVisual();
        running = false;
      });
    } else {
      animarAdicionar(() => {
        handleAdicionarEstante();
        syncVisual();
        running = false;
      });
    }
  });

  // Espera _livroAtual carregar para sincronizar visual
  const esperar = setInterval(() => {
    if (_livroAtual) {
      clearInterval(esperar);
      syncVisual();
    }
  }, 100);

  gsap.defaults({ ease: 'power2.out' });
})();
