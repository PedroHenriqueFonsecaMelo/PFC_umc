let _gsTimer = null;
let _gsClientesCache = null;

function globalSearchInput(val) {
    const box = document.getElementById("globalSearchResults");
    if (!val || val.trim().length < 2) {
        box.style.display = "none";
        return;
    }
    clearTimeout(_gsTimer);
    _gsTimer = setTimeout(() => executarBuscaGlobal(val.trim()), 350);
}

function normGs(s) {
    return (s || "").normalize("NFD").replace(/[\u0300-\u036f]/g,"").toLowerCase();
}

async function executarBuscaGlobal(termo) {
    const box = document.getElementById("globalSearchResults");
    box.style.display = "block";
    box.innerHTML = `<div style="padding:1rem;text-align:center;color:#9c968f;font-size:.82rem;">Buscando...</div>`;

    try {
        const termoNorm = normGs(termo);

        // Buscar clientes (com cache)
        if (!_gsClientesCache) {
            const r = await fetch("/api/admin/clientes", {credentials:"include"});
            _gsClientesCache = r.ok ? await r.json() : [];
        }

        // Filtrar clientes
        const listaClientes = (_gsClientesCache || [])
            .filter(c =>
                normGs(c.nome).includes(termoNorm) ||
                normGs(c.email).includes(termoNorm)
            ).slice(0, 5);

        // Buscar livros server-side
        const livrosRes = await fetch(`/api/livros/vitrine?busca=${encodeURIComponent(termo)}&size=5`, {credentials:"include"});
        const livrosData = livrosRes.ok ? await livrosRes.json() : {content:[]};
        const listaLivros = (livrosData.content || []).slice(0, 5);

        if (listaClientes.length === 0 && listaLivros.length === 0) {
            box.innerHTML = `<div style="padding:1rem;text-align:center;color:#9c968f;font-size:.82rem;">Nenhum resultado encontrado.</div>`;
            return;
        }

        let html = "";

        if (listaClientes.length > 0) {
            html += `<div style="padding:.5rem .75rem;font-size:.68rem;font-weight:700;
                text-transform:uppercase;letter-spacing:.08em;color:#9c968f;
                border-bottom:1px solid #f0ece6;background:#faf8f5;">
                👤 Clientes (${listaClientes.length})
            </div>`;
            const cores = {INICIANTE:"#888",BRONZE:"#cd7f32",PRATA:"#aaa",OURO:"#d4a017",PLATINA:"#8a2be2"};
            listaClientes.forEach(c => {
                const nivel = (c.nivel || "BRONZE").toUpperCase();
                html += `
                <a href="/admin/clientes/${c.id}" style="display:flex;align-items:center;
                    gap:.75rem;padding:.65rem .75rem;text-decoration:none;color:#2c241b;
                    border-bottom:1px solid #f9f6f0;transition:background .15s;"
                    onmouseover="this.style.background='#faf8f5'"
                    onmouseout="this.style.background='#fff'">
                    <div style="width:34px;height:34px;border-radius:50%;
                        background:#722f37;color:#fff;display:flex;align-items:center;
                        justify-content:center;font-weight:700;font-size:.85rem;flex-shrink:0;">
                        ${(c.nome||"?")[0].toUpperCase()}
                    </div>
                    <div style="flex:1;min-width:0;">
                        <div style="font-weight:600;font-size:.85rem;
                            white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">
                            ${c.nome}
                        </div>
                        <div style="font-size:.72rem;color:#9c968f;">
                            ${c.email} · ${c.totalCompras || 0} compras
                        </div>
                    </div>
                    <div style="text-align:right;flex-shrink:0;">
                        <div style="font-size:.7rem;font-weight:700;color:${cores[nivel]||'#888'};">
                            ${nivel}
                        </div>
                        <div style="font-size:.7rem;color:#9c968f;">
                            T$ ${(c.saldoTokens||0).toFixed(0)}
                        </div>
                    </div>
                </a>`;
            });
            if (listaClientes.length === 5) {
                html += `<a href="/admin/clientes" style="display:block;padding:.5rem .75rem;
                    font-size:.78rem;color:#722f37;text-decoration:none;text-align:center;
                    border-top:1px solid #f0ece6;font-weight:600;">
                    Ver todos os clientes →
                </a>`;
            }
        }

        if (listaLivros.length > 0) {
            html += `<div style="padding:.5rem .75rem;font-size:.68rem;font-weight:700;
                text-transform:uppercase;letter-spacing:.08em;color:#9c968f;
                border-bottom:1px solid #f0ece6;border-top:2px solid #f0ece6;background:#faf8f5;">
                📚 Livros (${listaLivros.length})
            </div>`;
            listaLivros.forEach(l => {
                let capa = "/img/logo-bibliotroca.png";
                try {
                    const fotos = JSON.parse(l.fotosUrls || l.fotoUrl || "[]");
                    if (Array.isArray(fotos) && fotos[0]) capa = fotos[0];
                } catch(e) {}
                html += `
                <a href="/admin/livros/${l.id}" style="display:flex;align-items:center;
                    gap:.75rem;padding:.65rem .75rem;text-decoration:none;color:#2c241b;
                    border-bottom:1px solid #f9f6f0;transition:background .15s;"
                    onmouseover="this.style.background='#faf8f5'"
                    onmouseout="this.style.background='#fff'">
                    <img src="${capa}" style="width:28px;height:38px;object-fit:cover;
                        border-radius:3px;flex-shrink:0;"
                        onerror="this.src='/img/logo-bibliotroca.png'">
                    <div style="flex:1;min-width:0;">
                        <div style="font-weight:600;font-size:.85rem;
                            white-space:nowrap;overflow:hidden;text-overflow:ellipsis;">
                            ${l.titulo}
                        </div>
                        <div style="font-size:.72rem;color:#9c968f;">
                            ${l.autor}
                        </div>
                    </div>
                    <div style="text-align:right;flex-shrink:0;">
                        <div style="font-size:.78rem;font-weight:700;color:#722f37;">
                            T$ ${(l.precoAprovado||0).toFixed(0)}
                        </div>
                        <div style="font-size:.68rem;color:#9c968f;">
                            ${l.estadoAprovado||""}
                        </div>
                    </div>
                </a>`;
            });
        }

        box.innerHTML = html;

    } catch(e) {
        console.error(e);
        box.innerHTML = `<div style="padding:1rem;text-align:center;color:#c0392b;font-size:.82rem;">Erro ao buscar.</div>`;
    }
}

document.addEventListener("click", e => {
    if (!e.target.closest("#globalSearch") && !e.target.closest("#globalSearchResults")) {
        const box = document.getElementById("globalSearchResults");
        if (box) box.style.display = "none";
    }
});
