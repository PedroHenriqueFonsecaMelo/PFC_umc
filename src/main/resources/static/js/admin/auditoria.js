const d = new Date();
document.getElementById("dataHoje").textContent = d.toLocaleDateString(
  "pt-BR",
  { weekday: "long", day: "2-digit", month: "long", year: "numeric" },
);
