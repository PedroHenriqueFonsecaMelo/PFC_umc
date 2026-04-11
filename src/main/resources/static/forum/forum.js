/* ============================================================
   forum.js — Módulo Fórum · Bibliotroca
   ============================================================ */

document.addEventListener('DOMContentLoaded', function () {

    // ── Modal: Criar Tópico ─────────────────────────────────────────────────

    const btnAbrirModal = document.getElementById('btn-abrir-modal');
    const modalOverlay = document.getElementById('modal-novo-topico');
    const btnFecharModal = document.getElementById('btn-fechar-modal');

    if (btnAbrirModal && modalOverlay) {
        btnAbrirModal.addEventListener('click', function () {
            modalOverlay.classList.add('open');
            document.body.style.overflow = 'hidden';
        });
    }

    function fecharModal() {
        if (modalOverlay) {
            modalOverlay.classList.remove('open');
            document.body.style.overflow = '';
        }
    }

    if (btnFecharModal) btnFecharModal.addEventListener('click', fecharModal);

    if (modalOverlay) {
        modalOverlay.addEventListener('click', function (e) {
            if (e.target === modalOverlay) fecharModal();
        });
    }

    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') fecharModal();
    });

    // Reabrir modal se houve erro de validação
    const temErroModal = document.getElementById('tem-erro-modal');
    if (temErroModal && modalOverlay) {
        modalOverlay.classList.add('open');
        document.body.style.overflow = 'hidden';
    }

    // ── Curtidas ────────────────────────────────────────────────────────────

    document.querySelectorAll('.btn-curtir').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var respostaId = this.dataset.respostaId;
            var self = this;

            fetch('/api/forum/respostas/' + respostaId + '/curtir', {
                method: 'POST',
                credentials: 'include'
            })
                .then(function (res) {
                    if (res.status === 401 || res.status === 403) {
                        window.location.href = '/clientes/login';
                        return null;
                    }
                    return res.json();
                })
                .then(function (data) {
                    if (!data) return;
                    var countEl = self.querySelector('.curtida-count');
                    if (countEl) countEl.textContent = data.curtidas;
                    if (data.liked) {
                        self.classList.add('liked');
                    } else {
                        self.classList.remove('liked');
                    }
                })
                .catch(function () { });
        });
    });

    // ── Melhor Resposta ─────────────────────────────────────────────────────

    document.querySelectorAll('.btn-melhor').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var respostaId = this.dataset.respostaId;
            var card = document.getElementById('resposta-card-' + respostaId);
            var self = this;

            fetch('/api/forum/respostas/' + respostaId + '/melhor', {
                method: 'POST',
                credentials: 'include'
            })
                .then(function (res) {
                    if (res.status === 401 || res.status === 403) {
                        return res.json().then(function (d) { alert(d.erro || 'Sem permissão.'); return null; });
                    }
                    return res.json();
                })
                .then(function (data) {
                    if (!data) return;
                    // Reload para refletir mudança de estado no servidor
                    window.location.reload();
                })
                .catch(function () { });
        });
    });

    // ── Moderação Admin: Deletar Tópico ─────────────────────────────────────

    document.querySelectorAll('.btn-del-topico').forEach(function (btn) {
        btn.addEventListener('click', function () {
            if (!confirm('Tem certeza que deseja excluir este tópico? Esta ação não pode ser desfeita.')) return;
            var topicoId = this.dataset.topicoId;

            fetch('/api/forum/topicos/' + topicoId, {
                method: 'DELETE',
                credentials: 'include'
            })
                .then(function (res) { return res.json(); })
                .then(function (data) {
                    var card = document.getElementById('topico-card-' + topicoId);
                    if (card) {
                        card.style.transition = 'opacity .3s';
                        card.style.opacity = '0';
                        setTimeout(function () { card.remove(); }, 300);
                    }
                })
                .catch(function () { });
        });
    });

    // ── Moderação Admin: Deletar Resposta ───────────────────────────────────

    document.querySelectorAll('.btn-del-resposta').forEach(function (btn) {
        btn.addEventListener('click', function () {
            if (!confirm('Excluir esta resposta?')) return;
            var respostaId = this.dataset.respostaId;

            fetch('/api/forum/respostas/' + respostaId, {
                method: 'DELETE',
                credentials: 'include'
            })
                .then(function (res) { return res.json(); })
                .then(function (data) {
                    var card = document.getElementById('resposta-card-' + respostaId);
                    if (card) {
                        card.style.transition = 'opacity .3s';
                        card.style.opacity = '0';
                        setTimeout(function () { card.remove(); }, 300);
                    }
                })
                .catch(function () { });
        });
    });

    // ── Auto-resize textareas ────────────────────────────────────────────────

    document.querySelectorAll('textarea.form-control').forEach(function (ta) {
        ta.addEventListener('input', function () {
            this.style.height = 'auto';
            this.style.height = (this.scrollHeight) + 'px';
        });
    });

});
