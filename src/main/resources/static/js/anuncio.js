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
