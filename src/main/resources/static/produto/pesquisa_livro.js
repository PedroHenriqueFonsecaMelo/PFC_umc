document.addEventListener("DOMContentLoaded", async () => {
    const pathParts = window.location.pathname.split('/').filter(Boolean);

    const isbn = pathParts[1];

    console.log("ISBN extraído:", isbn);

    if (isbn && isbn.length > 5) {
        await carregarDadosLivro(isbn);
    } else {
        console.error("ISBN não encontrado na posição esperada.");
    }
});

async function carregarDadosLivro(isbn) {
    console.log("Buscando dados para o ISBN:", isbn);

    let infoOL = null;
    let resumoEncontrado = "";

    // Busca na Open Library
    try {
        const resOL = await fetch(`https://openlibrary.org/api/books?bibkeys=ISBN:${isbn}&format=json&jscmd=data`);
        const dataOL = await resOL.json();
        infoOL = dataOL[`ISBN:${isbn}`];
    } catch (err) { console.error(err); }

    // Busca no Google Books
    try {
        const resGB = await fetch(`https://www.googleapis.com/books/v1/volumes?q=isbn:${isbn}`);
        const dataGB = await resGB.json();
        if (dataGB.totalItems > 0 && dataGB.items[0].volumeInfo.description) {
            resumoEncontrado = dataGB.items[0].volumeInfo.description;
        }
    } catch (err) { console.error(err); }

    // Lógica de Renderização
    let resumoFinal = resumoEncontrado;
    if (!resumoFinal && infoOL?.description) {
        resumoFinal = typeof infoOL.description === 'string' ? infoOL.description : infoOL.description.value;
    }

    if (!resumoFinal && infoOL?.subjects) {
        const temas = infoOL.subjects.slice(0, 5).map(s => s.name.replace("series:", "").replace(/_/g, " ")).join(", ");
        resumoFinal = `Sinopse indisponível. Temas: ${temas}.`;
    }

    // Atualiza o HTML
    if (infoOL) {
        document.getElementById('tituloLivro').textContent = infoOL.title;
        if (infoOL.cover) {
            document.getElementById('containerCapa').innerHTML = `<img src="${infoOL.cover.large}" class="w-48 shadow-lg">`;
        }
    }
    document.getElementById('containerResumo').innerHTML = `<p>${resumoFinal || "Dados não encontrados."}</p>`;
}