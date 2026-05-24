/* ================================================================
   blog_post.js — Detalhe do Post · Bibliotroca
   ================================================================ */

const postId = document.getElementById('btnCurtir')?.dataset?.postId;
let jaCurtiu = false;
let nomeUsuarioLogado = null;

function escHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;').replace(/</g, '&lt;')
        .replace(/>/g, '&gt;').replace(/"/g, '&quot;').replace(/'/g, '&#39;');
}

/* ── CSS dinâmico ── */
(function() {
    const style = document.createElement('style');
    style.textContent = `
        .btn-excluir-comentario {
            background: none; border: none; cursor: pointer;
            color: #b0a49a; font-size: .75rem; padding: .2rem .4rem;
            border-radius: 4px; margin-left: auto;
            transition: color .2s, background .2s;
        }
        .btn-excluir-comentario:hover { color: #722f37; background: rgba(114,47,55,.08); }
        .comentario-header { display: flex; align-items: center; gap: .5rem; flex-wrap: wrap; }
        .btn-curtir.curtido { color: #722f37; }
        .btn-curtir.curtido .fa-heart { color: #722f37; }

        /* Modal de confirmação de exclusão */
        .blog-modal-overlay {
            display: none; position: fixed; top: 0; left: 0;
            width: 100%; height: 100%; background: rgba(44,36,27,.45);
            z-index: 9999; align-items: center; justify-content: center;
        }
        .blog-modal-overlay.open { display: flex; }
        .blog-modal-box {
            background: #f9f6f0; border-radius: 14px;
            padding: 2rem 2rem 1.5rem;
            max-width: 380px; width: 90%;
            box-shadow: 0 8px 40px rgba(44,36,27,.22);
            text-align: center;
        }
        .blog-modal-icone {
            font-size: 2rem; color: #722f37; margin-bottom: .75rem;
        }
        .blog-modal-titulo {
            font-family: 'Playfair Display', serif;
            font-size: 1.15rem; font-weight: 700;
            color: #2c241b; margin-bottom: .5rem;
        }
        .blog-modal-subtitulo {
            font-size: .85rem; color: #7a6e65;
            margin-bottom: 1.5rem; line-height: 1.5;
        }
        .blog-modal-acoes {
            display: flex; gap: .75rem; justify-content: center;
        }
        .blog-modal-btn-cancelar {
            padding: .55rem 1.4rem; background: transparent;
            border: 1px solid rgba(44,36,27,.22); color: #7a6e65;
            border-radius: 8px; font-size: .85rem; cursor: pointer;
            font-family: 'DM Sans', sans-serif; transition: background .2s;
        }
        .blog-modal-btn-cancelar:hover { background: rgba(44,36,27,.06); }
        .blog-modal-btn-confirmar {
            padding: .55rem 1.4rem; background: #722f37; color: #f9f6f0;
            border: none; border-radius: 8px; font-size: .85rem;
            cursor: pointer; font-family: 'DM Sans', sans-serif;
            font-weight: 700; transition: background .2s;
        }
        .blog-modal-btn-confirmar:hover { background: #5c2530; }
    `;
    document.head.appendChild(style);

    /* Cria o modal no DOM */
    const modalHtml = `
        <div class="blog-modal-overlay" id="blogModalExcluir">
            <div class="blog-modal-box">
                <div class="blog-modal-icone"><i class="fa-solid fa-trash-can"></i></div>
                <div class="blog-modal-titulo">Excluir comentário?</div>
                <div class="blog-modal-subtitulo">Esta ação não pode ser desfeita.</div>
                <div class="blog-modal-acoes">
                    <button class="blog-modal-btn-cancelar" id="blogModalCancelar">Cancelar</button>
                    <button class="blog-modal-btn-confirmar" id="blogModalConfirmar">Excluir</button>
                </div>
            </div>
        </div>`;
    document.body.insertAdjacentHTML('beforeend', modalHtml);

    const overlay = document.getElementById('blogModalExcluir');
    document.getElementById('blogModalCancelar').addEventListener('click', () => {
        overlay.classList.remove('open');
        window._blogModalCallback = null;
    });
    overlay.addEventListener('click', (e) => {
        if (e.target === overlay) {
            overlay.classList.remove('open');
            window._blogModalCallback = null;
        }
    });
    document.getElementById('blogModalConfirmar').addEventListener('click', () => {
        overlay.classList.remove('open');
        if (typeof window._blogModalCallback === 'function') {
            window._blogModalCallback();
            window._blogModalCallback = null;
        }
    });
})();

/* ── Init: carrega usuário logado PRIMEIRO, depois comentários ── */
async function init() {
    try {
        const res = await fetch('/clientes/meu-perfil-json', {
            credentials: 'include', redirect: 'manual'
        });
        if (res.ok && res.type !== 'opaqueredirect') {
            const data = await res.json();
            nomeUsuarioLogado = data.nome || null;
            const el = document.getElementById('navSaldo');
            if (el) el.textContent = 'T$ ' + (data.saldoTokens || 0).toFixed(2);
        }
    } catch (_) {}
    // Só carrega comentários depois de saber quem é o usuário
    await carregarComentarios();
}

/* ── CURTIR / DESCURTIR ── */
const btnCurtir = document.getElementById('btnCurtir');

if (btnCurtir && !btnCurtir.classList.contains('btn-curtir-disabled')) {
    btnCurtir.addEventListener('click', async () => {
        try {
            const res = await fetch(`/api/blog/${postId}/curtir`, {
                method: 'POST',
                credentials: 'include',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ descurtir: jaCurtiu })
            });

            if (res.status === 401) { window.location.href = '/clientes/login'; return; }
            if (!res.ok) return;

            const data = await res.json();
            document.getElementById('curtidas-count').textContent = data.curtidas;

            jaCurtiu = !jaCurtiu;
            btnCurtir.classList.toggle('curtido', jaCurtiu);
            btnCurtir.title = jaCurtiu ? 'Descurtir' : 'Curtir';
        } catch (_) {}
    });
}

/* ── COMENTÁRIOS ── */
async function carregarComentarios() {
    if (!postId) return;
    const lista = document.getElementById('comentarios-lista');
    if (!lista) return;

    try {
        const res = await fetch(`/api/blog/${postId}/comentarios`);
        if (!res.ok) throw new Error();
        const comentarios = await res.json();

        if (comentarios.length === 0) {
            lista.innerHTML = '<div class="comentario-vazio">Nenhum comentário ainda. Seja o primeiro!</div>';
            return;
        }

        lista.innerHTML = comentarios.map(c => {
            const ehDono = nomeUsuarioLogado && c.autorNome === nomeUsuarioLogado;
            const btnExcluir = ehDono
                ? `<button class="btn-excluir-comentario" onclick="excluirComentario(${c.id}, this)" title="Excluir comentário">
                    <i class="fa-solid fa-trash"></i>
                   </button>`
                : '';
            return `
            <div class="comentario-card" id="comentario-${c.id}">
                <div class="comentario-header">
                    <span class="comentario-autor">${escHtml(c.autorNome)}</span>
                    <span class="comentario-data">${escHtml(c.dataCriacao)}</span>
                    ${btnExcluir}
                </div>
                <div class="comentario-conteudo">${escHtml(c.conteudo)}</div>
            </div>`;
        }).join('');
    } catch (_) {
        lista.innerHTML = '<div class="comentario-vazio">Erro ao carregar comentários.</div>';
    }
}

window.excluirComentario = async function(comentarioId, btn) {
    window._blogModalCallback = async function() {
        btn.disabled = true;
        try {
            const res = await fetch(`/api/blog/${postId}/comentarios/${comentarioId}`, {
                method: 'DELETE', credentials: 'include'
            });
            if (res.status === 401) { window.location.href = '/clientes/login'; return; }
            if (res.status === 403) { alert('Sem permissão para excluir.'); return; }
            if (!res.ok) { alert('Erro ao excluir comentário.'); return; }

            const card = document.getElementById(`comentario-${comentarioId}`);
            if (card) {
                card.style.transition = 'opacity 0.3s ease';
                card.style.opacity = '0';
                setTimeout(() => card.remove(), 300);
            }
        } catch (_) {
            alert('Erro ao excluir comentário.');
        } finally {
            btn.disabled = false;
        }
    };
    document.getElementById('blogModalExcluir').classList.add('open');
};

/* ── FORM COMENTÁRIO ── */
const formComentario = document.getElementById('formComentario');

if (formComentario) {
    formComentario.addEventListener('submit', async (e) => {
        e.preventDefault();
        const input = document.getElementById('inputComentario');
        const btn = document.getElementById('btnComentar');
        const conteudo = input.value.trim();
        if (!conteudo) { input.focus(); return; }

        btn.disabled = true;
        btn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Publicando...';

        try {
            const res = await fetch(`/api/blog/${postId}/comentarios`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include',
                body: JSON.stringify({ conteudo })
            });

            if (res.status === 401) { window.location.href = '/clientes/login'; return; }
            if (!res.ok) {
                const err = await res.json().catch(() => ({}));
                throw new Error(err.erro || 'Erro ao publicar');
            }

            const novo = await res.json();
            const lista = document.getElementById('comentarios-lista');
            const vazio = lista.querySelector('.comentario-vazio');
            if (vazio) vazio.remove();

            const card = document.createElement('div');
            card.className = 'comentario-card';
            card.id = `comentario-${novo.id}`;
            card.innerHTML = `
                <div class="comentario-header">
                    <span class="comentario-autor">${escHtml(novo.autorNome)}</span>
                    <span class="comentario-data">${escHtml(novo.dataCriacao)}</span>
                    <button class="btn-excluir-comentario"
                            onclick="excluirComentario(${novo.id}, this)"
                            title="Excluir comentário">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </div>
                <div class="comentario-conteudo">${escHtml(novo.conteudo)}</div>`;
            lista.appendChild(card);
            input.value = '';
        } catch (err) {
            alert(err.message || 'Erro ao publicar comentário.');
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fa-solid fa-paper-plane"></i> Publicar';
        }
    });
}

init();
