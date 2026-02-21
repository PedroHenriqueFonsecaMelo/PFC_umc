// /static/cliente/script.js

// fallback caso os índices não venham do Thymeleaf (para testes)
if (typeof enderecoIndex === 'undefined') {
    var enderecoIndex = 0;
}
if (typeof cartaoIndex === 'undefined') {
    var cartaoIndex = 0;
}

/**
 * reorganiza nomes de inputs para arrays (enderecos ou cartoes)
 * selectorContainer: id do container, ex: "#enderecos-list-container"
 * itemSelector: classe do item, ex: ".endereco-item"
 * arrayName: "enderecos" ou "cartoes"
 */
function reorganizarIndices(selectorContainer, itemSelector, arrayName) {
    const container = document.querySelector(selectorContainer);
    if (!container) return;
    const items = container.querySelectorAll(itemSelector);

    items.forEach((item, idx) => {
        // atualiza legend (opcional)
        const legend = item.querySelector("legend");
        if (legend) {
            legend.textContent = (arrayName === "enderecos" ? "Endereço " : "Cartão ") + (idx + 1);
        }

        // atualiza todos os atributos name dentro do item que correspondem ao array
        item.querySelectorAll("[name]").forEach(field => {
            const name = field.getAttribute("name");
            if (!name) return;
            // substitui cartoes[<n>] ou enderecos[<n>] por novo índice
            const newName = name.replace(new RegExp(arrayName + "\\[[0-9]+\\]"), arrayName + "[" + idx + "]");
            field.setAttribute("name", newName);
        });
    });
}

/* ==========================
   ADICIONAR ENDEREÇO
   ========================== */
function adicionarEndereco() {
    const container = document.getElementById("enderecos-list-container");
    if (!container) return;

    // cria novo bloco com índice temporário 0; reorganizarIndices irá ajustar para o índice correto
    const wrapper = document.createElement("div");
    wrapper.className = "endereco-item item-card";

    wrapper.innerHTML = `
        <fieldset>
            <legend>Endereço</legend>

            <input type="hidden" name="enderecos[0].id" value=""/>

            <div class="form-grid">
                <div class="form-group"><label>CEP:</label><input type="text" name="enderecos[0].cep"/></div>
                <div class="form-group"><label>Rua:</label><input type="text" name="enderecos[0].rua"/></div>
                <div class="form-group"><label>Número:</label><input type="text" name="enderecos[0].numero"/></div>
                <div class="form-group"><label>Bairro:</label><input type="text" name="enderecos[0].bairro"/></div>
                <div class="form-group"><label>Cidade:</label><input type="text" name="enderecos[0].cidade"/></div>
                <div class="form-group"><label>Estado:</label><input type="text" name="enderecos[0].estado"/></div>
                <div class="form-group"><label>País:</label><input type="text" name="enderecos[0].pais"/></div>
                <div class="form-group"><label>Complemento:</label><input type="text" name="enderecos[0].complemento"/></div>
                <div class="form-group"><label>Tipo residência:</label><input type="text" name="enderecos[0].tipoResidencia"/></div>
            </div>
        </fieldset>
    `;

    container.appendChild(wrapper);

    // reorganiza para garantir nomes corretos
    reorganizarIndices("#enderecos-list-container", ".endereco-item", "enderecos");

    // atualiza contador (útil se quiser usar)
    enderecoIndex = container.querySelectorAll(".endereco-item").length;
}

/* ==========================
   ADICIONAR CARTÃO
   ========================== */
function adicionarCartao() {
    const container = document.getElementById("cartoes-list-container");
    if (!container) return;

    const wrapper = document.createElement("div");
    wrapper.className = "cartao-item item-card";

    wrapper.innerHTML = `
        <fieldset>
            <legend>Cartão</legend>

            <input type="hidden" name="cartoes[0].id" value=""/>

            <div class="form-grid">
                <div class="form-group"><label>Número:</label><input type="text" name="cartoes[0].numero"/></div>
                <div class="form-group"><label>Nome Titular:</label><input type="text" name="cartoes[0].nomeTitular"/></div>
                <div class="form-group"><label>CPF Titular:</label><input type="text" name="cartoes[0].cpfTitular"/></div>
                <div class="form-group"><label>Validade:</label><input type="text" name="cartoes[0].validade"/></div>
                <div class="form-group"><label>Bandeira:</label><input type="text" name="cartoes[0].bandeira"/></div>
            </div>
        </fieldset>
    `;

    container.appendChild(wrapper);

    reorganizarIndices("#cartoes-list-container", ".cartao-item", "cartoes");

    cartaoIndex = container.querySelectorAll(".cartao-item").length;
}

// opcional: reorganizar ao carregar (caso markup inicial precise ajuste)
document.addEventListener("DOMContentLoaded", function() {
    reorganizarIndices("#enderecos-list-container", ".endereco-item", "enderecos");
    reorganizarIndices("#cartoes-list-container", ".cartao-item", "cartoes");
});
