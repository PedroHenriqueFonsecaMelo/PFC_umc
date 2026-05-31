(function () {
  const section = document.querySelector(".bw-scroll-section");
  if (!section) return;

  // === 50 LIVROS CURADOS COM CAPA NA OPENLIBRARY ===
  const LIVROS = [
    { titulo: "1984", autor: "Orwell", isbn: "9780451524935" },
    { titulo: "O Alquimista", autor: "Paulo Coelho", isbn: "9780061122415" },
    { titulo: "Orgulho e Preconceito", autor: "Austen", isbn: "9780141439518" },
    { titulo: "O Grande Gatsby", autor: "Fitzgerald", isbn: "9780743273565" },
    { titulo: "Crime e Castigo", autor: "Dostoiévski", isbn: "9780140449136" },
    { titulo: "O Senhor dos Anéis", autor: "Tolkien", isbn: "9780618640157" },
    { titulo: "Admirável Mundo Novo", autor: "Huxley", isbn: "9780060850524" },
    { titulo: "O Estrangeiro", autor: "Camus", isbn: "9780679720201" },
    {
      titulo: "O Apanhador no Campo de Centeio",
      autor: "Salinger",
      isbn: "9780316769174",
    },
    { titulo: "Fundação", autor: "Asimov", isbn: "9780553293357" },
    { titulo: "Demian", autor: "Hesse", isbn: "9780062196491" },
    { titulo: "Duna", autor: "Herbert", isbn: "9780441013593" },
    { titulo: "O Velho e o Mar", autor: "Hemingway", isbn: "9780684801223" },
    { titulo: "Frankenstein", autor: "Shelley", isbn: "9780141439471" },
    {
      titulo: "O Pequeno Príncipe",
      autor: "Saint-Exupéry",
      isbn: "9780156012195",
    },
    { titulo: "Fahrenheit 451", autor: "Bradbury", isbn: "9781451673319" },
    { titulo: "O Processo", autor: "Kafka", isbn: "9780805209990" },
    {
      titulo: "Cem Anos de Solidão",
      autor: "García Márquez",
      isbn: "9780060883287",
    },
    { titulo: "A Metamorfose", autor: "Kafka", isbn: "9780393971668" },
    { titulo: "Moby Dick", autor: "Melville", isbn: "9780142437247" },
    { titulo: "Anna Karenina", autor: "Tolstói", isbn: "9780143035008" },
    { titulo: "O Hobbit", autor: "Tolkien", isbn: "9780547928227" },
    {
      titulo: "A Revolução dos Bichos",
      autor: "Orwell",
      isbn: "9780451526342",
    },
    { titulo: "Os Miseráveis", autor: "Victor Hugo", isbn: "9780451419439" },
    {
      titulo: "O Conde de Monte Cristo",
      autor: "Dumas",
      isbn: "9780140449266",
    },
    { titulo: "Drácula", autor: "Stoker", isbn: "9780141439846" },
    {
      titulo: "O Retrato de Dorian Gray",
      autor: "Wilde",
      isbn: "9780141439570",
    },
    { titulo: "A Náusea", autor: "Sartre", isbn: "9780811201568" },
    { titulo: "O Lobo da Estepe", autor: "Hesse", isbn: "9780312278670" },
    { titulo: "Sidarta", autor: "Hesse", isbn: "9780553208849" },
    { titulo: "Neuromancer", autor: "Gibson", isbn: "9780441569595" },
    {
      titulo: "O Guia do Mochileiro das Galáxias",
      autor: "Adams",
      isbn: "9780345391803",
    },
    { titulo: "Matadouro-Cinco", autor: "Vonnegut", isbn: "9780440180296" },
    { titulo: "Lolita", autor: "Nabokov", isbn: "9780679723165" },
    {
      titulo: "O Sol é para Todos",
      autor: "Harper Lee",
      isbn: "9780061935466",
    },
    {
      titulo: "Dom Casmurro",
      autor: "Machado de Assis",
      isbn: "9788544001820",
    },
    {
      titulo: "A Hora da Estrela",
      autor: "Clarice Lispector",
      isbn: "9788532517326",
    },
    { titulo: "Vidas Secas", autor: "Graciliano Ramos", isbn: "9788503009935" },
    {
      titulo: "Capitães da Areia",
      autor: "Jorge Amado",
      isbn: "9788535904338",
    },
    { titulo: "O Senhor das Moscas", autor: "Golding", isbn: "9780399501487" },
    { titulo: "Jane Eyre", autor: "Brontë", isbn: "9780141441146" },
    {
      titulo: "O Mestre e Margarida",
      autor: "Bulgákov",
      isbn: "9780140455465",
    },
    { titulo: "A Divina Comédia", autor: "Dante", isbn: "9780142437223" },
    { titulo: "Pedro Páramo", autor: "Juan Rulfo", isbn: "9780802133908" },
    {
      titulo: "O Amor nos Tempos do Cólera",
      autor: "García Márquez",
      isbn: "9780307389732",
    },
    { titulo: "A Guerra e a Paz", autor: "Tolstói", isbn: "9780199232765" },
    { titulo: "Catch-22", autor: "Heller", isbn: "9781451626650" },
    {
      titulo: "Grande Sertão: Veredas",
      autor: "Guimarães Rosa",
      isbn: "9788526009059",
    },
    {
      titulo: "Perto do Coração Selvagem",
      autor: "Clarice Lispector",
      isbn: "9788532512772",
    },
    { titulo: "O Processo", autor: "Kafka", isbn: "9780679722304" },
  ];

  // === EMBARALHA (Fisher-Yates) ===
  function shuffle(arr) {
    const a = [...arr];
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
  }

  function coverUrl(isbn) {
    return `https://covers.openlibrary.org/b/isbn/${isbn}-M.jpg`;
  }

  // Placeholder vinho caso a imagem quebre
  function makePlaceholder(titulo, autor) {
    const canvas = document.createElement("canvas");
    canvas.width = 200;
    canvas.height = 300;
    const ctx = canvas.getContext("2d");
    ctx.fillStyle = "#722f37";
    ctx.fillRect(0, 0, 200, 300);
    ctx.fillStyle = "#f4f1ec";
    ctx.font = "bold 14px serif";
    ctx.textAlign = "center";
    // Quebra o título em linhas
    const words = titulo.split(" ");
    let line = "", lines = [], y = 120;
    words.forEach((w) => {
      const test = line + w + " ";
      if (ctx.measureText(test).width > 160 && line) {
        lines.push(line.trim());
        line = w + " ";
      } else line = test;
    });
    lines.push(line.trim());
    lines.forEach((l, i) => ctx.fillText(l, 100, y + i * 22));
    ctx.font = "11px sans-serif";
    ctx.fillStyle = "#e8c4a0";
    ctx.fillText(autor, 100, y + lines.length * 22 + 16);
    return canvas.toDataURL();
  }

  function injectImage(div, livro) {
    const img = document.createElement("img");
    img.alt = livro.titulo;
    img.src = coverUrl(livro.isbn);
    img.title = `${livro.titulo} — ${livro.autor}`;
    img.onerror = function () {
      this.src = makePlaceholder(livro.titulo, livro.autor);
      this.onerror = null;
    };
    div.appendChild(img);
  }

  // === DISTRIBUI OS LIVROS NAS POSIÇÕES ===
  const embaralhados = shuffle(LIVROS);
  let idx = 0;

  const layer1Divs = section.querySelectorAll("#bw-layer-1 div");
  const layer2Divs = section.querySelectorAll("#bw-layer-2 div");
  const layer3Divs = section.querySelectorAll("#bw-layer-3 div");
  const scalerImg = section.querySelector("#bw-scaler img");

  // Central (scaler) — livro destaque
  const central = embaralhados[idx++];
  scalerImg.src = coverUrl(central.isbn);
  scalerImg.alt = central.titulo;
  scalerImg.title = `${central.titulo} — ${central.autor}`;
  scalerImg.onerror = function () {
    this.src = makePlaceholder(central.titulo, central.autor);
    this.onerror = null;
  };

  layer1Divs.forEach((div) => injectImage(div, embaralhados[idx++]));
  layer2Divs.forEach((div) => injectImage(div, embaralhados[idx++]));
  layer3Divs.forEach((div) => injectImage(div, embaralhados[idx++]));

  // === ANIMAÇÃO ===
  const prefersReducedMotion =
    window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  if (prefersReducedMotion) return;

  import("https://cdn.jsdelivr.net/npm/motion@11.11.16/+esm")
    .then(({ animate, scroll, cubicBezier }) => {
      const image = section.querySelector(".bw-scaler img");
      const scrollArea = section.querySelector(".bw-scroll-area");
      const layers = section.querySelectorAll(".bw-grid > .bw-layer");

      const naturalWidth = image.offsetWidth;
      const naturalHeight = image.offsetHeight;

      scroll(
        animate(image, {
          width: [window.innerWidth, naturalWidth],
          height: [window.innerHeight, naturalHeight],
        }, {
          width: { easing: cubicBezier(0.65, 0, 0.35, 1) },
          height: { easing: cubicBezier(0.42, 0, 0.58, 1) },
        }),
        { target: scrollArea, offset: ["start start", "80% end end"] },
      );

      const easings = [
        cubicBezier(0.42, 0, 0.58, 1),
        cubicBezier(0.76, 0, 0.24, 1),
        cubicBezier(0.87, 0, 0.13, 1),
      ];

      layers.forEach((layer, i) => {
        const end = `${1 - i * 0.05} end`;
        scroll(
          animate(layer, { opacity: [0, 0, 1] }, {
            offset: [0, 0.55, 1],
            easing: cubicBezier(0.61, 1, 0.88, 1),
          }),
          { target: scrollArea, offset: ["start start", end] },
        );
        scroll(
          animate(layer, { scale: [0, 0, 1] }, {
            offset: [0, 0.3, 1],
            easing: easings[i],
          }),
          { target: scrollArea, offset: ["start start", end] },
        );
      });
    });
})();
