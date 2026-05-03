/* ================================================================
   blog_post.js — Detalhe do Post · Bibliotroca
   ================================================================ */

(async function () {
    try {
        const res = await fetch('/clientes/meu-perfil-json', { credentials: 'include' });
        if (!res.ok) return;
        const c = await res.json();
        const el = document.getElementById('navSaldo');
        if (el) el.textContent = 'T$ ' + (c.saldoTokens || 0).toFixed(2);
    } catch (_) {}
})();

const postId = document.getElementById('btnCurtir')?.dataset?.postId;
let jaCurtiu = false;

function escHtml(str) {
    if (!str) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#39;');
}

/* ── CURTIR ── */

const btnCurtir = document.getElementById('btnCurtir');

if (btnCurtir && !btnCurtir.classList.contains('btn-curtir-disabled')) {
    btnCurtir.addEventListener('click', async () => {
        if (jaCurtiu) return;

        try {
            const res = await fetch(`/api/blog/${postId}/curtir`, {
                method: 'POST',
                credentials: 'include'
            });

            if (res.status === 401) {
                window.location.href = '/clientes/login';
                return;
            }

            if (!res.ok) return;

            const data = await res.json();
            document.getElementById('curtidas-count').textContent = data.curtidas;
            btnCurtir.classList.add('curtido');
            jaCurtiu = true;
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

        lista.innerHTML = comentarios.map(c => `
            <div class="comentario-card">
                <div class="comentario-header">
                    <span class="comentario-autor">${escHtml(c.autorNome)}</span>
                    <span class="comentario-data">${escHtml(c.dataCriacao)}</span>
                </div>
                <div class="comentario-conteudo">${escHtml(c.conteudo)}</div>
            </div>`).join('');
    } catch (_) {
        lista.innerHTML = '<div class="comentario-vazio">Erro ao carregar comentários.</div>';
    }
}

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

            if (res.status === 401) {
                window.location.href = '/clientes/login';
                return;
            }

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
            card.innerHTML = `
                <div class="comentario-header">
                    <span class="comentario-autor">${escHtml(novo.autorNome)}</span>
                    <span class="comentario-data">${escHtml(novo.dataCriacao)}</span>
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

carregarComentarios();
