(function() {
  const LIVROS = [
    { titulo: '1984', isbn: '9780451524935' },
    { titulo: 'O Alquimista', isbn: '9780061122415' },
    { titulo: 'Orgulho e Preconceito', isbn: '9780141439518' },
    { titulo: 'O Grande Gatsby', isbn: '9780743273565' },
    { titulo: 'Crime e Castigo', isbn: '9780140449136' },
    { titulo: 'O Senhor dos Anéis', isbn: '9780618640157' },
    { titulo: 'Admirável Mundo Novo', isbn: '9780060850524' },
    { titulo: 'O Estrangeiro', isbn: '9780679720201' },
    { titulo: 'Duna', isbn: '9780441013593' },
    { titulo: 'O Hobbit', isbn: '9780547928227' },
    { titulo: 'Fahrenheit 451', isbn: '9781451673319' },
    { titulo: 'Cem Anos de Solidão', isbn: '9780060883287' },
    { titulo: 'O Pequeno Príncipe', isbn: '9780156012195' },
    { titulo: 'Frankenstein', isbn: '9780141439471' },
    { titulo: 'O Processo', isbn: '9780805209990' },
    { titulo: 'Moby Dick', isbn: '9780142437247' },
    { titulo: 'Sidarta', isbn: '9780553208849' },
    { titulo: 'O Velho e o Mar', isbn: '9780684801223' },
    { titulo: 'Anna Karenina', isbn: '9780143035008' },
    { titulo: 'Dracula', isbn: '9780141439846' },
    { titulo: 'O Retrato de Dorian Gray', isbn: '9780141439570' },
    { titulo: 'O Sol é para Todos', isbn: '9780061935466' },
    { titulo: 'O Senhor das Moscas', isbn: '9780399501487' },
    { titulo: 'Jane Eyre', isbn: '9780141441146' },
    { titulo: 'Fundação', isbn: '9780553293357' },
    { titulo: 'Catch-22', isbn: '9781451626650' },
    { titulo: 'Lolita', isbn: '9780679723165' },
    { titulo: 'A Revolução dos Bichos', isbn: '9780451526342' },
    { titulo: 'Os Miseráveis', isbn: '9780451419439' },
    { titulo: 'O Guia do Mochileiro', isbn: '9780345391803' },
  ];

  const CORES = ['#722f37','#2c4a7c','#2d6a4f','#6b4226','#4a4e69',
                 '#5c3d2e','#1b4332','#3d405b','#7b2d8b','#c62a2f'];

  function shuffle(arr) {
    const a = [...arr];
    for (let i = a.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [a[i], a[j]] = [a[j], a[i]];
    }
    return a;
  }

  function coverUrl(isbn) {
    return 'https://covers.openlibrary.org/b/isbn/' + isbn + '-M.jpg';
  }

  function makePlaceholder(titulo, cor) {
    const canvas = document.createElement('canvas');
    canvas.width = 200; canvas.height = 200;
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = cor;
    ctx.fillRect(0, 0, 200, 200);
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 14px serif';
    ctx.textAlign = 'center';
    const words = titulo.split(' ');
    let line = '', lines = [], y = 90;
    words.forEach(w => {
      const test = line + w + ' ';
      if (ctx.measureText(test).width > 160 && line) {
        lines.push(line.trim()); line = w + ' ';
      } else { line = test; }
    });
    lines.push(line.trim());
    lines.forEach((l, i) => ctx.fillText(l, 100, y + i * 20));
    return canvas.toDataURL();
  }

  const COUNT      = 9;
  const DURATION   = 30000;
  const IMAGE_SIZE = 130; // px
  const CENTER_SPACE = 140; // px

  let startTime = null;

  function init() {
    const list = document.getElementById('orbitList');
    if (!list) return;

    const selecionados = shuffle(LIVROS).slice(0, COUNT);

    selecionados.forEach((livro, i) => {
      const angle = i / COUNT; // 0..1
      const level = 360 * angle; // graus

      const li  = document.createElement('li');
      const a   = document.createElement('a');
      const img = document.createElement('img');

      // Armazena angle e level para o loop
      li.dataset.angle = angle;
      li.dataset.level = level;

      img.alt   = livro.titulo;
      img.title = livro.titulo;
      img.src   = coverUrl(livro.isbn);
      img.onerror = function() {
        this.onerror = null;
        this.src = makePlaceholder(livro.titulo, CORES[i % CORES.length]);
      };

      // Estilos do <li> — posicionamento em órbita
      Object.assign(li.style, {
        position:        'absolute',
        top:             '0',
        left:            '0',
        transformOrigin: '0 0',
        transform:       `rotate(${level}deg) translate(${CENTER_SPACE}px, ${CENTER_SPACE}px)`,
      });
      li.addEventListener('mouseenter', () => li.style.zIndex = 999);
      li.addEventListener('mouseleave', () => li.style.zIndex = '');

      // Estilos do <a> — tamanho e contra-rotação inicial
      Object.assign(a.style, {
        display:      'block',
        width:        IMAGE_SIZE + 'px',
        aspectRatio:  '2/3',
        transform:    `rotate(${-level}deg)`,
        overflow:     'hidden',
        borderRadius: '4px',
        background:   'transparent',
        padding:      '0',
        border:       'none',
        outline:      'none',
      });
      a.href = '#';
      a.addEventListener('click', e => e.preventDefault());

      // Estilos da imagem
      Object.assign(img.style, {
        display:    'block',
        width:      '100%',
        aspectRatio:'2/3',
        objectFit:  'cover',
        transition: 'scale 150ms ease-out',
      });

      a.appendChild(img);
      li.appendChild(a);
      list.appendChild(li);
    });

    // Loop de animação
    function animate(ts) {
      if (!startTime) startTime = ts;
      const elapsed = (ts - startTime) % DURATION;
      const rotate  = elapsed / DURATION; // 0..1

      // Rotaciona a lista inteira
      list.style.rotate = (rotate * 360) + 'deg';

      // Cada <a> contra-rotaciona: rotate(calc(-360deg*(angle+rotate)-level))
      list.querySelectorAll('li').forEach(li => {
        const angle = parseFloat(li.dataset.angle);
        const level = parseFloat(li.dataset.level);
        const a = li.querySelector('a');
        if (a) {
          const currentListRotation = rotate * 360;
          const itemBaseAngle = parseFloat(li.dataset.level);
          const deg = -(currentListRotation + itemBaseAngle);
          a.style.transform = `rotate(${deg}deg)`;
        }
      });

      requestAnimationFrame(animate);
    }
    requestAnimationFrame(animate);

    // Efeito de escala por proximidade — igual ao original
    document.addEventListener('pointermove', (e) => {
      list.querySelectorAll('img').forEach(img => {
        const rect   = img.getBoundingClientRect();
        const cx     = rect.left + rect.width  / 2;
        const cy     = rect.top  + rect.height / 2;
        const dist   = Math.hypot(e.clientX - cx, e.clientY - cy);
        const maxDist = Math.min(window.innerWidth, window.innerHeight);
        const scale  = Math.max(0.1, 1 - dist / maxDist);
        img.style.scale = scale;
      });
    });
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }
})();
