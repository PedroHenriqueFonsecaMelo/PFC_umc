/* ================================================================
   book-wall.js — Galeria infinita de capas · Bibliotroca
   Vanilla JS puro · Zero dependências externas
   ================================================================ */
(function () {
  'use strict';

  /* ── Lista de 60 livros ── */
  var LIVROS = [
    { titulo: "Dom Quixote", autor: "Cervantes", isbn: "9788535914849" },
    { titulo: "1984", autor: "George Orwell", isbn: "9780451524935" },
    { titulo: "O Senhor dos An\u00e9is", autor: "Tolkien", isbn: "9780618640157" },
    { titulo: "Harry Potter e a Pedra Filosofal", autor: "Rowling", isbn: "9780439708180" },
    { titulo: "O Pequeno Pr\u00edncipe", autor: "Saint-Exup\u00e9ry", isbn: "9780156012195" },
    { titulo: "Crime e Castigo", autor: "Dostoí\u00e9vski", isbn: "9780140449136" },
    { titulo: "O Processo", autor: "Kafka", isbn: "9780805209990" },
    { titulo: "Cem Anos de Solid\u00e3o", autor: "Garc\u00eda M\u00e1rquez", isbn: "9780060883287" },
    { titulo: "A Metamorfose", autor: "Kafka", isbn: "9780393971668" },
    { titulo: "O Alquimista", autor: "Paulo Coelho", isbn: "9780061122415" },
    { titulo: "Moby Dick", autor: "Herman Melville", isbn: "9780142437247" },
    { titulo: "Anna Karenina", autor: "Tolst\u00f3i", isbn: "9780143035008" },
    { titulo: "Dom Casmurro", autor: "Machado de Assis", isbn: "9788544001820" },
    { titulo: "Grande Sert\u00e3o: Veredas", autor: "Guimar\u00e3es Rosa", isbn: "9788526009059" },
    { titulo: "O Hobbit", autor: "Tolkien", isbn: "9780547928227" },
    { titulo: "Fahrenheit 451", autor: "Ray Bradbury", isbn: "9781451673319" },
    { titulo: "Admir\u00e1vel Mundo Novo", autor: "Huxley", isbn: "9780060850524" },
    { titulo: "O Mestre e Margarida", autor: "Bulg\u00e1kov", isbn: "9780140455465" },
    { titulo: "A Divina Com\u00e9dia", autor: "Dante", isbn: "9780142437223" },
    { titulo: "Ulisses", autor: "James Joyce", isbn: "9780679722021" },
    { titulo: "Orgulho e Preconceito", autor: "Jane Austen", isbn: "9780141439518" },
    { titulo: "Jane Eyre", autor: "Charlotte Bront\u00eb", isbn: "9780141441146" },
    { titulo: "O Grande Gatsby", autor: "F. Scott Fitzgerald", isbn: "9780743273565" },
    { titulo: "As Vinhas da Ira", autor: "Steinbeck", isbn: "9780143039433" },
    { titulo: "Em Busca do Tempo Perdido", autor: "Proust", isbn: "9780300115437" },
    { titulo: "A Montanha M\u00e1gica", autor: "Thomas Mann", isbn: "9780679772873" },
    { titulo: "O Sol \u00e9 para Todos", autor: "Harper Lee", isbn: "9780061935466" },
    { titulo: "O Velho e o Mar", autor: "Hemingway", isbn: "9780684801223" },
    { titulo: "Mem\u00f3rias P\u00f3stumas de Br\u00e1s Cubas", autor: "Machado de Assis", isbn: "9788535910568" },
    { titulo: "Iracema", autor: "Jos\u00e9 de Alencar", isbn: "9788508110223" },
    { titulo: "A Hora da Estrela", autor: "Clarice Lispector", isbn: "9788532517326" },
    { titulo: "Perto do Cora\u00e7\u00e3o Selvagem", autor: "Clarice Lispector", isbn: "9788532512772" },
    { titulo: "Vidas Secas", autor: "Graciliano Ramos", isbn: "9788503009935" },
    { titulo: "O Corti\u00e7o", autor: "Alu\u00edsio Azevedo", isbn: "9788508110063" },
    { titulo: "Capit\u00e3es da Areia", autor: "Jorge Amado", isbn: "9788535904338" },
    { titulo: "O Senhor das Moscas", autor: "Golding", isbn: "9780399501487" },
    { titulo: "A Revolu\u00e7\u00e3o dos Bichos", autor: "Orwell", isbn: "9780451526342" },
    { titulo: "Os Miser\u00e1veis", autor: "Victor Hugo", isbn: "9780451419439" },
    { titulo: "O Conde de Monte Cristo", autor: "Dumas", isbn: "9780140449266" },
    { titulo: "Frankenstein", autor: "Mary Shelley", isbn: "9780141439471" },
    { titulo: "Dr\u00e1cula", autor: "Bram Stoker", isbn: "9780141439846" },
    { titulo: "O Retrato de Dorian Gray", autor: "Oscar Wilde", isbn: "9780141439570" },
    { titulo: "O Estrangeiro", autor: "Camus", isbn: "9780679720201" },
    { titulo: "A N\u00e1usea", autor: "Sartre", isbn: "9780811201568" },
    { titulo: "O Lobo da Estepe", autor: "Hesse", isbn: "9780312278670" },
    { titulo: "Sidarta", autor: "Hermann Hesse", isbn: "9780553208849" },
    { titulo: "Demian", autor: "Hesse", isbn: "9780062196491" },
    { titulo: "O Apanhador no Campo de Centeio", autor: "Salinger", isbn: "9780316769174" },
    { titulo: "As Aventuras de Tom Sawyer", autor: "Twain", isbn: "9780143039563" },
    { titulo: "Duna", autor: "Frank Herbert", isbn: "9780441013593" },
    { titulo: "Neuromancer", autor: "Gibson", isbn: "9780441569595" },
    { titulo: "Funda\u00e7\u00e3o", autor: "Isaac Asimov", isbn: "9780553293357" },
    { titulo: "O Guia do Mochileiro das Gal\u00e1xias", autor: "Adams", isbn: "9780345391803" },
    { titulo: "Matadouro-Cinco", autor: "Vonnegut", isbn: "9780440180296" },
    { titulo: "Catch-22", autor: "Heller", isbn: "9781451626650" },
    { titulo: "Lolita", autor: "Nabokov", isbn: "9780679723165" },
    { titulo: "Pedro P\u00e1ramo", autor: "Juan Rulfo", isbn: "9780802133908" },
    { titulo: "O Amor nos Tempos do C\u00f3lera", autor: "Garc\u00eda M\u00e1rquez", isbn: "9780307389732" },
    { titulo: "Como o A\u00e7o Foi Temperado", autor: "Ostrovsky", isbn: "9785050002342" },
    { titulo: "A Guerra e a Paz", autor: "Tolst\u00f3i", isbn: "9780199232765" }
  ];

  /* ── Velocidades → durações em segundos (dur = 15 / speed) ── */
  var DURATIONS = [37.5, 25.0, 30.0, 21.4, 33.3];

  /* ── Fisher-Yates shuffle ── */
  function shuffle(arr) {
    var a = arr.slice();
    for (var i = a.length - 1; i > 0; i--) {
      var j = Math.floor(Math.random() * (i + 1));
      var tmp = a[i]; a[i] = a[j]; a[j] = tmp;
    }
    return a;
  }

  /* ── Escape HTML básico ── */
  function esc(str) {
    return String(str)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');
  }

  /* ── Cria um card de livro ── */
  function makeCard(livro) {
    var card = document.createElement('div');
    card.className = 'bw-card';
    var url = 'https://covers.openlibrary.org/b/isbn/' + livro.isbn + '-M.jpg';
    card.innerHTML =
      '<img src="' + url + '" alt="' + esc(livro.titulo) + '"' +
      ' onerror="this.style.display=\'none\';this.nextElementSibling.style.display=\'flex\'">' +
      '<div class="bw-placeholder" style="display:none">' +
        '<span class="bw-ph-title">' + esc(livro.titulo) + '</span>' +
        '<span class="bw-ph-author">' + esc(livro.autor) + '</span>' +
      '</div>' +
      '<div class="bw-overlay">' +
        '<div class="bw-ov-title">' + esc(livro.titulo) + '</div>' +
        '<div class="bw-ov-author">' + esc(livro.autor) + '</div>' +
      '</div>';
    return card;
  }

  /* ── Inicializa a galeria ── */
  function initBookWall() {
    var wall = document.getElementById('bookWall');
    if (!wall) return;

    var shuffled = shuffle(LIVROS);
    var colCount = 5;
    var perCol   = Math.ceil(shuffled.length / colCount);

    for (var i = 0; i < colCount; i++) {
      var col   = document.createElement('div');
      col.className = 'bw-col';

      var track = document.createElement('div');
      track.className = 'bw-track';

      /* Offset aleatório para que as colunas não comecem todas no mesmo ponto */
      var delay = -(Math.random() * DURATIONS[i]).toFixed(2);

      track.style.animationName           = 'bwScroll';
      track.style.animationDuration       = DURATIONS[i] + 's';
      track.style.animationTimingFunction = 'linear';
      track.style.animationIterationCount = 'infinite';
      track.style.animationDelay          = delay + 's';

      /* Fatia de livros para esta coluna */
      var colBooks = shuffled.slice(i * perCol, (i + 1) * perCol);

      /* Garante pelo menos 8 livros por coluna */
      while (colBooks.length < 8) {
        colBooks = colBooks.concat(colBooks);
      }

      /* Duplica para o loop infinito (-50% seamless) */
      var doubled = colBooks.concat(colBooks);
      doubled.forEach(function (livro) {
        track.appendChild(makeCard(livro));
      });

      col.appendChild(track);
      wall.appendChild(col);
    }
  }

  /* ── Aguarda DOM pronto ── */
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', initBookWall);
  } else {
    initBookWall();
  }
})();
