(function () {
  const LIVROS = [
    { titulo: "1984", isbn: "9780451524935" },
    { titulo: "O Alquimista", isbn: "9780061122415" },
    { titulo: "Orgulho e Preconceito", isbn: "9780141439518" },
    { titulo: "O Grande Gatsby", isbn: "9780743273565" },
    { titulo: "Crime e Castigo", isbn: "9780140449136" },
    { titulo: "O Senhor dos Anéis", isbn: "9780618640157" },
    { titulo: "Admirável Mundo Novo", isbn: "9780060850524" },
    { titulo: "O Estrangeiro", isbn: "9780679720201" },
    { titulo: "Duna", isbn: "9780441013593" },
    { titulo: "O Hobbit", isbn: "9780547928227" },
    { titulo: "Fahrenheit 451", isbn: "9781451673319" },
    { titulo: "Cem Anos de Solidão", isbn: "9780060883287" },
    { titulo: "O Pequeno Príncipe", isbn: "9780156012195" },
    { titulo: "Frankenstein", isbn: "9780141439471" },
    { titulo: "O Processo", isbn: "9780805209990" },
    { titulo: "Moby Dick", isbn: "9780142437247" },
    { titulo: "Sidarta", isbn: "9780553208849" },
    { titulo: "O Velho e o Mar", isbn: "9780684801223" },
    { titulo: "Anna Karenina", isbn: "9780143035008" },
    { titulo: "Dracula", isbn: "9780141439846" },
    { titulo: "O Retrato de Dorian Gray", isbn: "9780141439570" },
    { titulo: "O Sol é para Todos", isbn: "9780061935466" },
    { titulo: "O Senhor das Moscas", isbn: "9780399501487" },
    { titulo: "Jane Eyre", isbn: "9780141441146" },
    { titulo: "Fundação", isbn: "9780553293357" },
    { titulo: "Catch-22", isbn: "9781451626650" },
    { titulo: "Lolita", isbn: "9780679723165" },
    { titulo: "A Revolução dos Bichos", isbn: "9780451526342" },
    { titulo: "Os Miseráveis", isbn: "9780451419439" },
    { titulo: "O Guia do Mochileiro das Galáxias", isbn: "9780345391803" },
  ];

  function shuffle(arr) {
    const a = [...arr];
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
  }

  function coverUrl(isbn) {
    return "https://covers.openlibrary.org/b/isbn/" + isbn + "-M.jpg";
  }

  function init() {
    const list = document.getElementById("orbitList");
    if (!list) return;

    const count = 9;
    const radius = 160;
    const selecionados = shuffle(LIVROS).slice(0, count);

    selecionados.forEach((livro, i) => {
      const angle = (360 / count) * i;
      const rad = angle * Math.PI / 180;
      const x = Math.cos(rad) * radius;
      const y = Math.sin(rad) * radius;

      const li = document.createElement("li");
      const a = document.createElement("a");
      const img = document.createElement("img");

      img.alt = livro.titulo;
      img.title = livro.titulo;
      img.src = coverUrl(livro.isbn);
      img.style.counterReset = "--angle: " + angle + "deg";

      // Fallback: placeholder colorido
      img.onerror = function () {
        this.onerror = null;
        const canvas = document.createElement("canvas");
        canvas.width = 110;
        canvas.height = 165;
        const ctx = canvas.getContext("2d");
        const colors = ["#722f37", "#2c4a7c", "#2d6a4f", "#6b4226", "#4a4e69"];
        ctx.fillStyle = colors[i % colors.length];
        ctx.fillRect(0, 0, 110, 165);
        ctx.fillStyle = "#fff";
        ctx.font = "bold 11px serif";
        ctx.textAlign = "center";
        const words = livro.titulo.split(" ");
        let line = "", lines = [], y2 = 70;
        words.forEach((w) => {
          const test = line + w + " ";
          if (ctx.measureText(test).width > 90 && line) {
            lines.push(line.trim());
            line = w + " ";
          } else line = test;
        });
        lines.push(line.trim());
        lines.forEach((l, idx) => ctx.fillText(l, 55, y2 + idx * 16));
        this.src = canvas.toDataURL();
      };

      a.href = "#";
      a.appendChild(img);
      li.appendChild(a);

      // Posiciona cada item em órbita
      li.style.transform = "rotate(" + angle + "deg) translate(" + radius +
        "px, -55px)";

      // Contra-rotação para manter a imagem upright
      img.style.setProperty("--orbit-angle", angle + "deg");

      list.appendChild(li);
    });

    // Pointer move: escala por proximidade
    document.addEventListener("pointermove", (e) => {
      const imgs = list.querySelectorAll("img");
      imgs.forEach((img) => {
        const rect = img.getBoundingClientRect();
        const cx = rect.left + rect.width / 2;
        const cy = rect.top + rect.height / 2;
        const dist = Math.hypot(e.clientX - cx, e.clientY - cy);
        const max = Math.min(window.innerWidth, window.innerHeight) * 0.6;
        const sc = Math.max(0.7, 1 - dist / max * 0.4);
        img.style.scale = sc;
      });
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", init);
  } else {
    init();
  }
})();
