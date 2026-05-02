/* ── Constantes ── */
const CORES_VENDAS = [
  "#722F37", "#8a3a44", "#a04550", "#b85060", "#5a2530",
  "#3d1820", "#c46070", "#d47080", "#6b2b32", "#7a3038", "#903545", "#a03a48",
];
const CORES_POSTS = [
  "#4A5D23", "#5a7029", "#6b8535", "#3d4e1d", "#7a9a40",
  "#8aaa4a", "#2e3b18", "#526625", "#638030", "#74943c", "#496020", "#557028",
];

/* ── Data ── */
document.getElementById("dataHoje").textContent = new Date().toLocaleDateString("pt-BR", {
  weekday: "long", day: "numeric", month: "long", year: "numeric",
});

/* ── Helpers ── */
function fmt(n) {
  return Number(n).toLocaleString("pt-BR");
}

function tooltipBase() {
  return {
    backgroundColor: "#FFFFFF",
    titleColor: "#2C241B",
    bodyColor: "rgba(44,36,27,0.65)",
    borderColor: "rgba(74,93,35,0.15)",
    borderWidth: 1,
    padding: 10,
  };
}

function criarPizza(canvasId, labels, dados, cores, legendId) {
  new Chart(document.getElementById(canvasId), {
    type: "doughnut",
    data: {
      labels,
      datasets: [{
        data: dados,
        backgroundColor: cores,
        borderWidth: 2,
        borderColor: "#fff",
        hoverOffset: 6,
      }],
    },
    options: {
      responsive: true,
      cutout: "55%",
      plugins: {
        tooltip: Object.assign(tooltipBase(), {
          callbacks: { label: (ctx) => "  " + ctx.parsed + " registros" },
        }),
      },
    },
  });

  const el = document.getElementById(legendId);
  el.innerHTML = labels.map((m, i) =>
    `<div class="pie-legend-item">
      <span class="pie-dot" style="background:${cores[i % cores.length]}"></span>
      <span style="color:#475569;font-weight:500;flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">${m}</span>
      <span style="color:#94a3b8;font-weight:600">${dados[i]}</span>
    </div>`
  ).join("");
}

/* ── Renderiza tudo com dados da API ── */
function renderDashboard(d) {
  document.getElementById("valTotalClientes").textContent = fmt(d.totalClientes);
  document.getElementById("valTotalLivros").textContent = fmt(d.totalLivros);
  document.getElementById("valTotalVisitas").textContent = fmt(d.totalVisitas);
  document.getElementById("valTotalAdquiridos").textContent = fmt(d.totalAdquiridos);
  document.getElementById("valTokensDisp").textContent = "T$ " + fmt(d.tokensDisponibilizados);
  document.getElementById("valTokensUtil").textContent = "T$ " + fmt(d.tokensUtilizados);

  const pct = d.tokensDisponibilizados > 0
    ? ((d.tokensUtilizados / d.tokensDisponibilizados) * 100).toFixed(1) + "% dos disponíveis"
    : "—";
  document.getElementById("badgeTokensUtil").textContent = pct;

  const primeiro = d.rotulos[0];
  const ultimo = d.rotulos[d.rotulos.length - 1];
  const periodo = `Últimos 12 meses · ${primeiro} – ${ultimo}`;
  document.getElementById("subBarras").textContent = periodo;
  document.getElementById("subPizzaVendas").textContent = `Distribuição · ${primeiro} – ${ultimo}`;
  document.getElementById("subPizzaPostagens").textContent = `Distribuição · ${primeiro} – ${ultimo}`;

  new Chart(document.getElementById("chartBarras"), {
    type: "bar",
    data: {
      labels: d.rotulos,
      datasets: [{
        label: "Novos Clientes",
        data: d.clientesPorMes,
        backgroundColor: "#722F37",
        borderRadius: 6,
        borderSkipped: false,
        maxBarThickness: 44,
      }],
    },
    options: {
      responsive: true,
      plugins: {
        tooltip: Object.assign(tooltipBase(), {
          callbacks: { label: (ctx) => " " + ctx.parsed.y + " novos clientes" },
        }),
      },
      scales: {
        x: { grid: { display: false }, border: { display: false }, ticks: { color: "rgba(44,36,27,0.4)" } },
        y: { grid: { color: "rgba(74,93,35,0.08)" }, border: { display: false }, ticks: { color: "rgba(44,36,27,0.4)" } },
      },
    },
  });

  new Chart(document.getElementById("chartLinha"), {
    type: "line",
    data: {
      labels: d.rotulos,
      datasets: [{
        label: "Clientes",
        data: d.clientesPorMes,
        borderColor: "#4A5D23",
        backgroundColor: "rgba(74,93,35,0.08)",
        borderWidth: 2.5,
        pointBackgroundColor: "#4A5D23",
        pointRadius: 4,
        pointHoverRadius: 6,
        tension: 0.35,
        fill: true,
      }],
    },
    options: {
      responsive: true,
      plugins: { tooltip: tooltipBase() },
      scales: {
        x: { grid: { display: false }, border: { display: false }, ticks: { color: "rgba(44,36,27,0.4)" } },
        y: { grid: { color: "rgba(74,93,35,0.08)" }, border: { display: false }, ticks: { color: "rgba(44,36,27,0.4)" } },
      },
    },
  });

  criarPizza("chartPizzaVendas", d.rotulos, d.vendasPorMes, CORES_VENDAS, "legendVendas");
  criarPizza("chartPizzaPostagens", d.rotulos, d.postagensPorMes, CORES_POSTS, "legendPostagens");

  const tbody = document.getElementById("tabelaCorpo");
  tbody.innerHTML = d.rotulos.map((mes, i) => {
    const vendas = d.vendasPorMes[i];
    const postagens = d.postagensPorMes[i];
    const conv = postagens > 0 ? ((vendas / postagens) * 100).toFixed(1) : "—";
    const alto = conv !== "—" && parseFloat(conv) > 70;
    return `<tr>
      <td>${mes}</td>
      <td>${d.clientesPorMes[i]}</td>
      <td style="color:#722F37;font-weight:600">${vendas}</td>
      <td style="color:#4A5D23;font-weight:600">${postagens}</td>
      <td><span class="pill ${alto ? "pill-green" : "pill-gray"}">${conv !== "—" ? conv + "%" : "—"}</span></td>
    </tr>`;
  }).join("");

  document.getElementById("spinnerWrapper").style.display = "none";
  document.getElementById("mainContent").style.display = "flex";
}

/* ── Carrega dados da API ── */
fetch("/api/admin/dashboard/metricas", { credentials: "include" })
  .then((res) => {
    if (!res.ok) throw new Error("HTTP " + res.status);
    return res.json();
  })
  .then((data) => renderDashboard(data))
  .catch((err) => {
    document.getElementById("spinnerWrapper").innerHTML =
      `<div style="text-align:center;color:#722F37;padding:40px">
        <i class="fa-solid fa-circle-exclamation" style="font-size:36px;margin-bottom:12px"></i>
        <p style="font-weight:600;color:#2C241B">Erro ao carregar métricas</p>
        <p style="font-size:12px;color:rgba(44,36,27,0.45);margin-top:6px">${err.message}</p>
        <button onclick="location.reload()" style="margin-top:16px;padding:8px 20px;background:#722F37;color:#F9F6F0;border:none;border-radius:8px;cursor:pointer;font-family:'DM Sans',sans-serif">
          Tentar novamente
        </button>
      </div>`;
  });

/* ── Menu mobile ── */
function abrirMenu() {
  document.getElementById("sidebar").classList.add("open");
  document.getElementById("overlay").classList.add("open");
}
function fecharMenu() {
  document.getElementById("sidebar").classList.remove("open");
  document.getElementById("overlay").classList.remove("open");
}
