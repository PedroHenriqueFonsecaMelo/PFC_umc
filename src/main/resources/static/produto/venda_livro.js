const { useState } = React;

function App() {
    const MAX_LIVROS = 5;
    const [books, setBooks] = useState([{ id: Date.now(), isbn: '', titulo: '', autor: '', fotos: [] }]);
    const [submitLoading, setSubmitLoading] = useState(false);

    const addBook = () => {
        if (books.length < MAX_LIVROS) {
            setBooks([...books, { id: Date.now(), isbn: '', titulo: '', autor: '', fotos: [] }]);
        }
    };

    const removeBook = (id) => {
        if (books.length > 1) setBooks(books.filter(b => b.id !== id));
    };

    const clearBook = (id) => {
        setBooks(books.map(book =>
            book.id === id ? { id, isbn: '', titulo: '', autor: '', fotos: [] } : book
        ));
    };

    const updateBookField = (bookId, field, value) => {
        setBooks(prev => prev.map(book =>
            book.id === bookId ? { ...book, [field]: value } : book
        ));
    };

    const buscarDadosLivro = async (bookId, isbnBruto) => {
        const isbnLimpo = isbnBruto.replace(/\D/g, "");
        if (isbnLimpo.length < 10) return;

        updateBookField(bookId, 'buscandoIsbn', true);
        try {
            const res = await fetch(`https://openlibrary.org/api/books?bibkeys=ISBN:${isbnLimpo}&format=json&jscmd=data`);
            const data = await res.json();
            const info = data[`ISBN:${isbnLimpo}`];

            if (info) {
                updateBookField(bookId, 'titulo', info.title || '');
                updateBookField(bookId, 'autor', info.authors ? info.authors[0].name : '');
            }
        } catch (e) { console.error(e); }
        finally { updateBookField(bookId, 'buscandoIsbn', false); }
    };

    const handleFileChange = (bookId, e) => {
        const files = Array.from(e.target.files).slice(0, 3);
        const previews = files.map(file => ({ file, url: URL.createObjectURL(file) }));
        updateBookField(bookId, 'fotos', previews);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        setSubmitLoading(true);

        const formData = new FormData();

        const loteDados = {
            livros: books.map(b => ({
                titulo: b.titulo.trim(),
                autor: b.autor.trim(),
                isbn: b.isbn.replace(/\D/g, ""),
                quantidadedeFotos: b.fotos ? b.fotos.length : 0
            }))
        };

        // CORREÇÃO AQUI: Definir explicitamente o tipo do Blob como application/json
        const jsonBlob = new Blob([JSON.stringify(loteDados)], {
            type: 'application/json'
        });

        formData.append("loteDados", jsonBlob);

        // Anexar fotos (mesma lógica anterior)
        books.forEach(book => {
            if (book.fotos) {
                book.fotos.forEach(fotoObj => {
                    formData.append("fotos", fotoObj.file);
                });
            }
        });

        try {
            const response = await fetch('/api/livros/lotes/vender', {
                method: 'POST',
                // Importante: NÃO defina headers de Content-Type aqui
                body: formData
            });

            if (response.ok) {
                alert("Lote enviado com sucesso!");
                window.location.href = "/clientes/meu-perfil";
            } else {
                const errorMsg = await response.text();
                console.error("Erro do servidor:", errorMsg);
                alert("Erro ao enviar: " + errorMsg);
            }
        } catch (error) {
            alert("Erro de conexão.");
        } finally {
            setSubmitLoading(false);
        }
    };

    return (
        <div className="bg-white p-6 md:p-10 rounded-3xl shadow-2xl max-w-4xl mx-auto my-10 font-sans">
            <div className="flex justify-between items-center mb-8 border-b pb-6">
                <h1 className="text-2xl font-black text-indigo-600 uppercase tracking-tight">Vender Lote</h1>
                <button type="button" onClick={addBook} className="bg-green-500 text-white px-4 py-2 rounded-xl font-bold text-sm">
                    + Livro ({books.length}/5)
                </button>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
                {books.map((book, index) => (
                    <div key={book.id} className="p-6 bg-gray-50 rounded-2xl border-2 border-gray-100 relative">
                        <div className="flex justify-between mb-4">
                            <span className="font-bold text-gray-400">#0{index + 1}</span>
                            <div className="space-x-4">
                                <button type="button" onClick={() => clearBook(book.id)} className="text-xs font-bold text-orange-400 uppercase">Limpar</button>
                                {books.length > 1 && <button type="button" onClick={() => removeBook(book.id)} className="text-xs font-bold text-red-400 uppercase">Remover</button>}
                            </div>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div className="md:col-span-2">
                                <label className="text-xs font-bold text-gray-500 uppercase">ISBN (Cole ou digite tudo)</label>
                                <div className="relative">
                                    <input
                                        type="text"
                                        className="w-full p-4 rounded-xl border-2 border-white bg-white shadow-sm focus:border-indigo-400 outline-none transition-all"
                                        value={book.isbn}
                                        onChange={(e) => {
                                            const val = e.target.value;
                                            updateBookField(book.id, 'isbn', val);
                                            if (val.replace(/\D/g, "").length >= 10) buscarDadosLivro(book.id, val);
                                        }}
                                        placeholder="Ex: 9788532511010"
                                    />
                                    {book.buscandoIsbn && <div className="absolute right-4 top-4"><i className="fa-solid fa-spinner animate-spin text-indigo-500"></i></div>}
                                </div>
                            </div>
                            <input
                                type="text" placeholder="Título" required
                                className="w-full p-4 rounded-xl border-2 border-white bg-white shadow-sm focus:border-indigo-400 outline-none"
                                value={book.titulo} onChange={(e) => updateBookField(book.id, 'titulo', e.target.value)}
                            />
                            <input
                                type="text" placeholder="Autor" required
                                className="w-full p-4 rounded-xl border-2 border-white bg-white shadow-sm focus:border-indigo-400 outline-none"
                                value={book.autor} onChange={(e) => updateBookField(book.id, 'autor', e.target.value)}
                            />
                        </div>

                        <div className="mt-4 relative border-2 border-dashed border-gray-200 rounded-xl p-6 text-center bg-white">
                            <input
                                type="file" multiple accept="image/*"
                                className="absolute inset-0 w-full h-full opacity-0 cursor-pointer z-10"
                                onChange={(e) => handleFileChange(book.id, e)}
                            />
                            <div className="text-gray-400">
                                {book.fotos?.length > 0 ? (
                                    <div className="flex justify-center gap-2">
                                        {book.fotos.map((f, i) => <img key={i} src={f.url} className="w-12 h-12 object-cover rounded-md" />)}
                                    </div>
                                ) : (
                                    <p className="text-sm font-bold">Clique para adicionar fotos</p>
                                )}
                            </div>
                        </div>
                    </div>
                ))}

                <button
                    type="submit"
                    disabled={submitLoading}
                    className="w-full bg-indigo-600 text-white py-4 rounded-2xl font-black text-xl shadow-lg hover:bg-indigo-700 disabled:bg-gray-300 transition-all"
                >
                    {submitLoading ? "ENVIANDO..." : "PUBLICAR LOTE"}
                </button>
            </form>
        </div>
    );
}

const root = ReactDOM.createRoot(document.getElementById('root'));
root.render(<App />);