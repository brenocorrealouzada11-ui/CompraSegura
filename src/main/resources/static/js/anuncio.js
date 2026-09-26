// Separadores servem apenas para leitura. O servidor valida e normaliza os IMEIs.
(() => {
    const campo = document.getElementById('imeis');
    if (!campo) return;

    function formatar(texto) {
        return texto.replace(/\r\n?/g, '\n').split(/([\n,;])/).map(linha => {
            const numero = linha.replace(/-/g, '').trim();
            // Nao apagar letras ou excesso de digitos: o usuario deve corrigir o valor.
            if (!/^[0-9]{1,15}$/.test(numero)) return linha;
            return numero.slice(0, 8)
                + (numero.length > 8 ? '-' + numero.slice(8, 14) : '')
                + (numero.length > 14 ? '-' + numero.slice(14) : '');
        }).join('');
    }

    campo.addEventListener('keydown', evento => {
        const cursor = campo.selectionStart;
        if (cursor !== campo.selectionEnd) return;
        // Permite apagar o digito ao lado de um separador sem ficar preso no traco.
        if (evento.key === 'Backspace' && campo.value[cursor - 1] === '-') {
            campo.setSelectionRange(cursor - 1, cursor - 1);
        } else if (evento.key === 'Delete' && campo.value[cursor] === '-') {
            campo.setSelectionRange(cursor + 1, cursor + 1);
        }
    });
    campo.addEventListener('input', () => {
        const original = campo.value;
        const posicao = original.slice(0, campo.selectionStart).replace(/-/g, '').length;
        const novo = formatar(original);
        if (novo === original) return;
        campo.value = novo;
        let cursor = 0;
        let caracteres = 0;
        while (cursor < novo.length && caracteres < posicao) {
            if (novo[cursor] !== '-') caracteres++;
            cursor++;
        }
        campo.setSelectionRange(cursor, cursor);
    });
    campo.value = formatar(campo.value);
})();

// A mesma regra e validada no servidor, inclusive se o JavaScript estiver desativado.
(() => {
    const reparos = document.getElementById('reparos');
    const observacoes = document.getElementById('descricao');
    const ajuda = document.getElementById('ajuda-reparos');
    const regra = document.getElementById('regra-observacoes');
    if (!reparos || !observacoes || !ajuda || !regra) return;
    function atualizar() {
        const exige = reparos.value === 'COM_REPAROS';
        observacoes.required = exige;
        if (exige) observacoes.minLength = 20;
        else observacoes.removeAttribute('minlength');
        observacoes.setCustomValidity(exige && observacoes.value.trim().length < 20
            ? 'Descreva os reparos e alterações nas observações com pelo menos 20 caracteres.' : '');
        ajuda.hidden = !exige;
        regra.textContent = exige ? '(mínimo de 20 caracteres)' : '(opcional)';
    }
    reparos.addEventListener('change', atualizar);
    observacoes.addEventListener('input', atualizar);
    atualizar();
})();

// A previa devolve somente um fragmento HTML escapado pelo Thymeleaf.
// O formulario e o arquivo selecionado permanecem no navegador, sem criar um anuncio.
(() => {
    const form = document.getElementById('form-anuncio');
    const botao = document.getElementById('verificar-aparelho');
    const resultado = document.getElementById('resultado-verificacao');
    const status = document.getElementById('status-verificacao');
    if (!form || !botao || !resultado || !status) return;
    let revisao = 0;
    let houvePrevia = false;
    botao.hidden = false;
    function invalidar() {
        revisao++;
        resultado.hidden = true;
        if (houvePrevia) status.textContent = 'Os dados foram alterados. Verifique novamente para atualizar a prévia.';
    }
    form.addEventListener('input', invalidar);
    form.addEventListener('change', invalidar);
    botao.addEventListener('click', async () => {
        if (!form.reportValidity()) return;
        const versaoEnviada = revisao;
        botao.disabled = true;
        houvePrevia = true;
        resultado.hidden = true;
        status.textContent = 'Consultando os cenários simulados e avaliando as pendências…';
        try {
            const resposta = await fetch(form.dataset.avaliacaoUrl, {method: 'POST', body: new FormData(form), credentials: 'same-origin'});
            if (versaoEnviada !== revisao) return;
            if (resposta.redirected || resposta.status === 401 || resposta.status === 403) {
                status.textContent = 'Sua sessão expirou. Entre novamente para verificar o aparelho.';
                return;
            }
            if (resposta.status === 413) {
                status.textContent = 'Escolha uma imagem PNG ou JPEG de até 5 MB.';
                return;
            }
            if (!resposta.ok && resposta.status !== 422) throw new Error('Falha na verificação');
            const html = await resposta.text();
            if (versaoEnviada !== revisao) return;
            resultado.innerHTML = html;
            resultado.hidden = false;
            status.textContent = resposta.ok ? 'Prévia concluída. Nenhum anúncio foi salvo. Você pode revisar os dados ou salvar o rascunho.' : 'Corrija os dados indicados abaixo.';
        } catch (erro) {
            if (versaoEnviada === revisao) status.textContent = 'Não foi possível verificar agora. Seus dados continuam no formulário; tente novamente.';
        } finally { botao.disabled = false; }
    });
})();
