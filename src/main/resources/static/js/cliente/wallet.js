console.log("[wallet.js] VERSÃO NOVA — sem alerts");
const TOKENS_POR_REAL = 2;

let intervaloCheck = null;

function mostrarConfirmacaoPix(valor) {
    return new Promise((resolve) => {
        const overlay = document.createElement("div");
        overlay.style.cssText =
            "position:fixed;inset:0;background:rgba(0,0,0,.45);z-index:2000;display:flex;align-items:center;justify-content:center";

        const tokens = (valor * TOKENS_POR_REAL).toFixed(0);
        overlay.innerHTML = `
          <div style="background:#f9f6f0;border-radius:14px;padding:2rem 2rem 1.5rem;max-width:360px;width:90%;box-shadow:0 8px 32px rgba(0,0,0,.18);text-align:center">
            <div style="font-size:2rem;margin-bottom:.75rem">
              <i class="fa-solid fa-pix" style="color:#32bcad"></i>
            </div>
            <p style="font-family:'Playfair Display',serif;font-size:1.05rem;font-weight:700;color:#2c241b;margin:0 0 .5rem">
              Confirmar pagamento via PIX?
            </p>
            <p style="font-size:.875rem;color:#7a6e65;margin:0 0 1.25rem;line-height:1.5">
              Será gerado um QR Code para pagamento de <strong style="color:#2c241b">R$ ${
            valor.toFixed(2)
        }</strong>,
              creditando <strong style="color:#722f37">T$ ${tokens}</strong> na sua carteira.
            </p>
            <div style="display:flex;gap:.75rem;justify-content:center">
              <button id="pixConfNao" style="padding:.6rem 1.4rem;border:1.5px solid rgba(44,36,27,.2);background:transparent;border-radius:8px;cursor:pointer;font-size:.85rem;color:#7a6e65;font-family:'DM Sans',sans-serif">
                Cancelar
              </button>
              <button id="pixConfSim" style="padding:.6rem 1.6rem;background:#32bcad;color:#fff;border:none;border-radius:8px;cursor:pointer;font-size:.85rem;font-weight:700;font-family:'DM Sans',sans-serif;display:flex;align-items:center;gap:.5rem">
                <i class="fa-solid fa-qrcode"></i> Gerar QR Code
              </button>
            </div>
          </div>`;

        document.body.appendChild(overlay);

        overlay.querySelector("#pixConfSim").addEventListener("click", () => {
            document.body.removeChild(overlay);
            resolve(true);
        });
        overlay.querySelector("#pixConfNao").addEventListener("click", () => {
            document.body.removeChild(overlay);
            resolve(false);
        });
        overlay.addEventListener("click", (ev) => {
            if (ev.target === overlay) {
                document.body.removeChild(overlay);
                resolve(false);
            }
        });
    });
}

document.addEventListener("DOMContentLoaded", () => {
    carregarSaldo();
    carregarHistorico();
    document.getElementById("formCompra").addEventListener(
        "submit",
        efetuarCompra,
    );
});

function mostrarErroForm(msg) {
    const el = document.getElementById("formErro");
    if (!el) return;
    el.textContent = msg;
    el.style.display = "flex";
}

function ocultarErroForm() {
    const el = document.getElementById("formErro");
    if (el) el.style.display = "none";
}

function atualizarPreview() {
    ocultarErroForm();
    const valor = parseFloat(document.getElementById("valor").value) || 0;
    const preview = document.getElementById("tokensPreview");
    const previewTokens = document.getElementById("previewTokens");

    if (valor >= 1) {
        previewTokens.textContent = (valor * TOKENS_POR_REAL).toFixed(0);
        preview.style.display = "flex";
    } else {
        preview.style.display = "none";
    }
}

async function carregarSaldo() {
    try {
        const res = await fetch("/clientes/meu-perfil-json", {
            credentials: "include",
        });
        if (res.ok) {
            const data = await res.json();
            document.getElementById("displaySaldo").textContent =
                (data.saldoTokens || 0).toFixed(2);
            const navSaldo = document.getElementById("navSaldo");
            if (navSaldo) {
                navSaldo.textContent = "T$ " +
                    (data.saldoTokens || 0).toFixed(2);
            }
        }
    } catch (e) {
        console.error("Erro ao carregar saldo:", e);
    }
}

async function efetuarCompra(e) {
    e.preventDefault();
    ocultarErroForm();
    const btn = document.getElementById("btnComprar");
    const valor = parseFloat(document.getElementById("valor").value);

    if (!valor || valor < 1) {
        mostrarErroForm("Informe um valor mínimo de R$ 1,00.");
        return;
    }

    const confirmado = await mostrarConfirmacaoPix(valor);
    if (!confirmado) return;

    btn.disabled = true;
    btn.innerHTML =
        '<i class="fa-solid fa-circle-notch fa-spin"></i> Gerando PIX...';

    try {
        const res = await fetch("/api/tokens/comprar", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            credentials: "include",
            body: JSON.stringify({ valor }),
        });

        const data = await res.json();

        if (!res.ok) {
            mostrarErroForm(data.message || data || "Erro ao gerar o PIX.");
            return;
        }

        // Exibe modal com QR Code
        const tokens = (valor * TOKENS_POR_REAL).toFixed(0);
        document.getElementById("pixValorInfo").textContent = `R$ ${
            valor.toFixed(2)
        } → T$ ${tokens} tokens`;
        document.getElementById("imgQrCode").src = data.qrCodeBase64;
        document.getElementById("textoCopiaECola").value = data.pixCopiaECola;
        document.getElementById("pixStatus").innerHTML =
            '<i class="fa-solid fa-circle-notch fa-spin"></i> Aguardando confirmação do pagamento...';
        document.getElementById("modalPix").style.display = "flex";

        iniciarVerificacao(data.pagamentoId);
    } catch (err) {
        console.error(err);
        mostrarErroForm("Erro de conexão. Tente novamente.");
    } finally {
        btn.disabled = false;
        btn.innerHTML =
            '<i class="fa-solid fa-qrcode"></i> Confirmar e pagar via PIX';
    }
}

function copyPix() {
    const input = document.getElementById("textoCopiaECola");
    input.select();

    const btn = document.querySelector(".btn-copiar");
    const feedback = () => {
        if (!btn) return;
        btn.innerHTML = '<i class="fa-solid fa-check"></i>';
        btn.classList.add("btn-copiar--copiado");
        setTimeout(() => {
            btn.innerHTML = '<i class="fa-solid fa-copy"></i>';
            btn.classList.remove("btn-copiar--copiado");
        }, 2000);
    };

    navigator.clipboard.writeText(input.value)
        .then(feedback)
        .catch(() => {
            document.execCommand("copy");
            feedback();
        });
}

function fecharModalPix() {
    if (intervaloCheck) clearInterval(intervaloCheck);
    document.getElementById("modalPix").style.display = "none";
    carregarSaldo();
    carregarHistorico();
}

function iniciarVerificacao(pagamentoId) {
    if (intervaloCheck) clearInterval(intervaloCheck);

    intervaloCheck = setInterval(async () => {
        try {
            const res = await fetch(
                `/api/tokens/verificar-pagamento/${pagamentoId}`,
                {
                    credentials: "include",
                },
            );
            const data = await res.json();

            if (data.status === "APROVADO") {
                clearInterval(intervaloCheck);
                document.getElementById("pixStatus").innerHTML =
                    '<i class="fa-solid fa-check-circle" style="color:#4a5d23"></i> Pagamento confirmado! Tokens creditados.';

                setTimeout(() => {
                    fecharModalPix();
                }, 2500);
            }
        } catch (e) {
            console.error("Erro ao verificar PIX:", e);
        }
    }, 4000);
}

async function carregarHistorico() {
    const tbody = document.getElementById("lista-transacoes");
    try {
        const res = await fetch("/api/tokens/historico", {
            credentials: "include",
        });
        if (!res.ok) return;

        const transacoes = await res.json();

        if (!transacoes.length) {
            tbody.innerHTML =
                '<tr><td colspan="4" style="text-align:center;color:#999;padding:1.5rem;">Nenhuma transação ainda.</td></tr>';
            return;
        }

        tbody.innerHTML = transacoes.map((t) => {
            const statusClass = t.status === "CONCLUIDO"
                ? "status-ok"
                : "status-pendente";
            const statusLabel = t.status === "CONCLUIDO"
                ? "Confirmado"
                : "Pendente";
            const data = new Date(t.dataHora).toLocaleString("pt-BR");
            const id = t.pagamentoId
                ? t.pagamentoId.substring(0, 12) + "..."
                : "-";
            return `
                <tr>
                    <td>${data}</td>
                    <td class="valor-tokens">T$ ${t.valor.toFixed(2)}</td>
                    <td><span class="badge-status ${statusClass}">${statusLabel}</span></td>
                    <td class="ref-id" title="${t.pagamentoId || ""}">${id}</td>
                </tr>`;
        }).join("");
    } catch (e) {
        console.error("Erro ao carregar histórico:", e);
    }
}
