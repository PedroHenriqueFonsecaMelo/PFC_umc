// Função para buscar CEP usando a API ViaCEP
async function buscarCep(cepInput) {
    const cep = cepInput.value.replace(/\D/g, '');

    if (cep.length !== 8) return;

    const container = cepInput.closest('.endereco-item');
    if (!container) return;

    function preencherCampo(field, valor) {
        const el = container.querySelector(`[data-cep-field="${field}"]`);
        if (el && valor) el.value = valor;
    }

    try {
        const response = await fetch(`https://viacep.com.br/ws/${cep}/json/`);
        const data = await response.json();

        if (!data.erro) {
            preencherCampo('rua', data.logradouro);
            preencherCampo('bairro', data.bairro);
            preencherCampo('cidade', data.localidade);
            preencherCampo('estado', data.uf);
            preencherCampo('pais', 'Brasil');
            // data.complemento do ViaCEP é um intervalo técnico de numeração, não o complemento do usuário
            const numero = container.querySelector('[data-cep-field="numero"]');
            if (numero) numero.focus();
        } else {
            alert("CEP não encontrado.");
        }
    } catch (error) {
        console.error("Erro ao buscar CEP:", error);
    }
}

// Função para adicionar novo bloco de endereço
function adicionarEndereco() {
    const container = document.getElementById('enderecos-list-container');
    const index = enderecoIndex; // Usa a variável global iniciada no HTML

    const novoEnderecoHtml = `
        <div class="endereco-item item-card novo-endereco animate-fade-in">
            <fieldset>
                <legend>Novo Endereço</legend>
                <div class="form-grid">
                    <div class="form-group">
                        <label>CEP:</label>
                        <input type="text" name="enderecos[${index}].cep"
                               onblur="buscarCep(this)" placeholder="00000-000" required />
                    </div>
                    <div class="form-group">
                        <label>Rua:</label>
                        <input type="text" name="enderecos[${index}].rua" data-cep-field="rua" required />
                    </div>
                    <div class="form-group">
                        <label>Número:</label>
                        <input type="text" name="enderecos[${index}].numero" data-cep-field="numero" required />
                    </div>
                    <div class="form-group">
                        <label>Bairro:</label>
                        <input type="text" name="enderecos[${index}].bairro" data-cep-field="bairro" required />
                    </div>
                    <div class="form-group">
                        <label>Cidade:</label>
                        <input type="text" name="enderecos[${index}].cidade" data-cep-field="cidade" required />
                    </div>
                    <div class="form-group">
                        <label>Estado:</label>
                        <input type="text" name="enderecos[${index}].estado" data-cep-field="estado" maxlength="2" required />
                    </div>
                    <div class="form-group">
                        <label>País:</label>
                        <input type="text" name="enderecos[${index}].pais" data-cep-field="pais" value="Brasil" required />
                    </div>
                    <div class="form-group">
                        <label>Complemento:</label>
                        <input type="text" name="enderecos[${index}].complemento" data-cep-field="complemento" />
                    </div>
                    <div class="form-group">
                        <label>Tipo de residência:</label>
                        <input type="text" name="enderecos[${index}].tipoResidencia" placeholder="Ex: Casa, Apto" />
                    </div>
                </div>
                <button type="button" class="btn-cancelar-js" onclick="this.closest('.endereco-item').remove()">
                    ✕ Cancelar
                </button>
            </fieldset>
        </div>
    `;

    container.insertAdjacentHTML('beforeend', novoEnderecoHtml);
    enderecoIndex++; // Incrementa para o próximo endereço
}

// Função para adicionar novo cartão (mantendo a lógica do seu formulário)
function adicionarCartao() {
    const container = document.getElementById('cartoes-list-container');
    const index = cartaoIndex;

    const novoCartaoHtml = `
        <div class="cartao-item item-card novo-cartao">
            <fieldset>
                <legend>Novo Cartão</legend>
                <div class="form-grid">
                    <div class="form-group">
                        <label>Número do Cartão:</label>
                        <input type="text" name="cartoes[${index}].numero" required />
                    </div>
                    <div class="form-group">
                        <label>Nome no Cartão:</label>
                        <input type="text" name="cartoes[${index}].nomeTitular" required />
                    </div>
                    <div class="form-group">
                        <label>Validade:</label>
                        <input type="text" name="cartoes[${index}].validade" placeholder="MM/AAAA" required />
                    </div>
                </div>
                <button type="button" class="btn-cancelar-js" onclick="this.closest('.cartao-item').remove()">
                    ✕ Cancelar
                </button>
            </fieldset>
        </div>
    `;

    container.insertAdjacentHTML('beforeend', novoCartaoHtml);
    cartaoIndex++;
}