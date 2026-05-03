(async function () {
    try {
        const res = await fetch('/clientes/meu-perfil-json', { credentials: 'include' });
        if (!res.ok) return;
        const c = await res.json();
        const el = document.getElementById('navSaldo');
        if (el) el.textContent = 'T$ ' + (c.saldoTokens || 0).toFixed(2);
    } catch (_) {}
})();
