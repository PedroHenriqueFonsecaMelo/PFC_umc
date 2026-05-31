/* ============================================================
   forum.js — Módulo Fórum · Bibliotroca
   ============================================================ */

document.addEventListener("DOMContentLoaded", function () {
    // ── Modal: Criar Tópico ─────────────────────────────────────────────────

    const btnAbrirModal = document.getElementById("btn-abrir-modal");
    const modalOverlay = document.getElementById("modal-novo-topico");
    const btnFecharModal = document.getElementById("btn-fechar-modal");

    if (btnAbrirModal && modalOverlay) {
        btnAbrirModal.addEventListener("click", function () {
            modalOverlay.classList.add("open");
            document.body.style.overflow = "hidden";
        });
    }

    function fecharModal() {
        if (modalOverlay) {
            modalOverlay.classList.remove("open");
            document.body.style.overflow = "";
        }
    }

    if (btnFecharModal) btnFecharModal.addEventListener("click", fecharModal);

    if (modalOverlay) {
        modalOverlay.addEventListener("click", function (e) {
            if (e.target === modalOverlay) fecharModal();
        });
    }

    document.addEventListener("keydown", function (e) {
        if (e.key === "Escape") fecharModal();
    });

    // Reabrir modal se houve erro de validação
    const temErroModal = document.getElementById("tem-erro-modal");
    if (temErroModal && modalOverlay) {
        modalOverlay.classList.add("open");
        document.body.style.overflow = "hidden";
    }

    // ── Curtidas ────────────────────────────────────────────────────────────

    document.querySelectorAll(".btn-curtir").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var respostaId = this.dataset.respostaId;
            var self = this;

            fetch("/api/forum/respostas/" + respostaId + "/curtir", {
                method: "POST",
                credentials: "include",
            })
                .then(function (res) {
                    if (res.status === 401 || res.status === 403) {
                        window.location.href = "/clientes/login";
                        return null;
                    }
                    return res.json();
                })
                .then(function (data) {
                    if (!data) return;
                    var countEl = self.querySelector(".curtida-count");
                    if (countEl) countEl.textContent = data.curtidas;
                    if (data.liked) {
                        self.classList.add("liked");
                    } else {
                        self.classList.remove("liked");
                    }
                })
                .catch(function () {});
        });
    });

    // ── Melhor Resposta ─────────────────────────────────────────────────────

    document.querySelectorAll(".btn-melhor").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var respostaId = this.dataset.respostaId;
            var card = document.getElementById("resposta-card-" + respostaId);
            var self = this;

            fetch("/api/forum/respostas/" + respostaId + "/melhor", {
                method: "POST",
                credentials: "include",
            })
                .then(function (res) {
                    if (res.status === 401 || res.status === 403) {
                        return res.json().then(function (d) {
                            alert(d.erro || "Sem permissão.");
                            return null;
                        });
                    }
                    return res.json();
                })
                .then(function (data) {
                    if (!data) return;
                    // Reload para refletir mudança de estado no servidor
                    window.location.reload();
                })
                .catch(function () {});
        });
    });

    // ── Moderação Admin: Deletar Tópico ─────────────────────────────────────

    document.querySelectorAll(".btn-del-topico").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var topicoId = this.dataset.topicoId;

            window.abrirForumModal(function () {
                fetch("/api/forum/topicos/" + topicoId, {
                    method: "DELETE",
                    credentials: "include",
                })
                    .then(function (res) {
                        return res.json();
                    })
                    .then(function (data) {
                        var card = document.getElementById(
                            "topico-card-" + topicoId,
                        );
                        if (card) {
                            card.style.transition = "opacity .3s";
                            card.style.opacity = "0";
                            setTimeout(function () {
                                card.remove();
                            }, 300);
                        }
                    })
                    .catch(function () {});
            });
        });
    });

    // ── Moderação Admin: Deletar Resposta ───────────────────────────────────

    document.querySelectorAll(".btn-del-resposta").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var respostaId = this.dataset.respostaId;

            window.abrirForumModal(function () {
                fetch("/api/forum/respostas/" + respostaId, {
                    method: "DELETE",
                    credentials: "include",
                })
                    .then(function (res) {
                        return res.json();
                    })
                    .then(function (data) {
                        var card = document.getElementById(
                            "resposta-card-" + respostaId,
                        );
                        if (card) {
                            card.style.transition = "opacity .3s";
                            card.style.opacity = "0";
                            setTimeout(function () {
                                card.remove();
                            }, 300);
                        }
                    })
                    .catch(function () {});
            });
        });
    });

    // ── Modal de confirmação ────────────────────────────────────────────────

    // Injeta modal no DOM
    if (!document.getElementById("forumModalExcluir")) {
        document.body.insertAdjacentHTML(
            "beforeend",
            `
            <div id="forumModalExcluir" style="
                display:none;position:fixed;top:0;left:0;width:100%;height:100%;
                background:rgba(44,36,27,.45);z-index:9999;
                align-items:center;justify-content:center;">
                <div style="background:#f9f6f0;border-radius:14px;padding:2rem 2rem 1.5rem;
                    max-width:380px;width:90%;box-shadow:0 8px 40px rgba(44,36,27,.22);text-align:center;">
                    <div style="font-size:2rem;color:#722f37;margin-bottom:.75rem;">
                        <i class="fa-solid fa-trash-can"></i>
                    </div>
                    <div style="font-family:'Playfair Display',serif;font-size:1.15rem;
                        font-weight:700;color:#2c241b;margin-bottom:.5rem;">
                        Excluir resposta?
                    </div>
                    <div style="font-size:.85rem;color:#7a6e65;margin-bottom:1.5rem;line-height:1.5;">
                        Esta ação não pode ser desfeita.
                    </div>
                    <div style="display:flex;gap:.75rem;justify-content:center;">
                        <button id="forumModalCancelar" style="
                            padding:.55rem 1.4rem;background:transparent;
                            border:1px solid rgba(44,36,27,.22);color:#7a6e65;
                            border-radius:8px;font-size:.85rem;cursor:pointer;
                            font-family:'DM Sans',sans-serif;">Cancelar</button>
                        <button id="forumModalConfirmar" style="
                            padding:.55rem 1.4rem;background:#722f37;color:#f9f6f0;
                            border:none;border-radius:8px;font-size:.85rem;
                            cursor:pointer;font-family:'DM Sans',sans-serif;font-weight:700;">
                            Excluir</button>
                    </div>
                </div>
            </div>`,
        );

        var forumModal = document.getElementById("forumModalExcluir");
        var forumModalCallback = null;

        document.getElementById("forumModalCancelar").addEventListener(
            "click",
            function () {
                forumModal.style.display = "none";
                forumModalCallback = null;
            },
        );
        document.getElementById("forumModalConfirmar").addEventListener(
            "click",
            function () {
                forumModal.style.display = "none";
                if (typeof forumModalCallback === "function") {
                    forumModalCallback();
                    forumModalCallback = null;
                }
            },
        );
        forumModal.addEventListener("click", function (e) {
            if (e.target === forumModal) {
                forumModal.style.display = "none";
                forumModalCallback = null;
            }
        });

        window.abrirForumModal = function (callback) {
            forumModalCallback = callback;
            forumModal.style.display = "flex";
        };
    }

    // ── Excluir própria resposta ─────────────────────────────────────────────

    document.querySelectorAll(".btn-del-resposta-proprio").forEach(
        function (btn) {
            btn.addEventListener("click", function () {
                var respostaId = this.dataset.respostaId;
                window.abrirForumModal(function () {
                    fetch("/api/forum/respostas/" + respostaId, {
                        method: "DELETE",
                        credentials: "include",
                    })
                        .then(function (res) {
                            return res.json();
                        })
                        .then(function () {
                            var card = document.getElementById(
                                "resposta-card-" + respostaId,
                            );
                            if (card) {
                                card.style.transition = "opacity .3s";
                                card.style.opacity = "0";
                                setTimeout(function () {
                                    card.remove();
                                }, 300);
                            }
                        })
                        .catch(function () {
                            alert("Erro ao excluir resposta.");
                        });
                });
            });
        },
    );

    // Também usar modal nos botões admin existentes
    document.querySelectorAll(".btn-del-resposta").forEach(function (btn) {
        btn.replaceWith(btn.cloneNode(true));
    });
    document.querySelectorAll(".btn-del-resposta").forEach(function (btn) {
        btn.addEventListener("click", function () {
            var respostaId = this.dataset.respostaId;
            window.abrirForumModal(function () {
                fetch("/api/forum/respostas/" + respostaId, {
                    method: "DELETE",
                    credentials: "include",
                })
                    .then(function (res) {
                        return res.json();
                    })
                    .then(function () {
                        var card = document.getElementById(
                            "resposta-card-" + respostaId,
                        );
                        if (card) {
                            card.style.transition = "opacity .3s";
                            card.style.opacity = "0";
                            setTimeout(function () {
                                card.remove();
                            }, 300);
                        }
                    })
                    .catch(function () {});
            });
        });
    });

    // ── Auto-resize textareas ────────────────────────────────────────────────

    document.querySelectorAll("textarea.form-control").forEach(function (ta) {
        ta.addEventListener("input", function () {
            this.style.height = "auto";
            this.style.height = (this.scrollHeight) + "px";
        });
    });
});

(async function () {
    try {
        const res = await fetch("/clientes/meu-perfil-json", {
            credentials: "include",
        });
        if (!res.ok) return;
        const c = await res.json();
        const el = document.getElementById("navSaldo");
        if (el) el.textContent = "T$ " + (c.saldoTokens || 0).toFixed(2);
    } catch (_) {}
})();
