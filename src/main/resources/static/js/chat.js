(() => {
    async function requisitar(url, opcoes = {}) {
        const controle = new AbortController();
        const timeout = setTimeout(() => controle.abort(), 12000);
        try {
            const resposta = await fetch(url, {credentials:'same-origin', cache:'no-store', ...opcoes, signal:controle.signal});
            if (resposta.redirected || resposta.status === 401 || resposta.status === 403) throw new Error('Sua sessão expirou. Entre novamente para continuar.');
            if (!resposta.ok) {
                if (resposta.status === 404) throw new Error('Esta conversa não está disponível para sua conta.');
                if (resposta.status === 409) throw new Error('Este envio já foi recebido. Copie seu texto e reabra a conversa antes de enviar uma nova mensagem.');
                let erro;
                try { erro = (await resposta.json()).erro; } catch (_) { /* Resposta sem mensagem de validacao. */ }
                throw new Error(erro || 'Não foi possível concluir agora. Tente novamente.');
            }
            return resposta;
        } finally { clearTimeout(timeout); }
    }
    const caixa = document.getElementById('caixa-conversas');
    if (caixa) {
        let ocupado = false;
        async function atualizarLista() {
            if (document.hidden || ocupado || caixa.contains(document.activeElement)) return;
            ocupado = true;
            try {
                const html = await (await requisitar(caixa.dataset.url)).text();
                // Fragmento produzido pelo Thymeleaf, que escapa textos das conversas.
                if (!document.hidden && !caixa.contains(document.activeElement)) caixa.innerHTML = html;
                document.getElementById('status-caixa').textContent = '';
            } catch (erro) { document.getElementById('status-caixa').textContent = erro.name === 'AbortError' ? 'A atualização demorou. Tentaremos novamente.' : erro.message; }
            finally { ocupado = false; }
        }
        setInterval(atualizarLista, 5000); document.addEventListener('visibilitychange', atualizarLista);
        return;
    }
    const chat = document.getElementById('chat');
    if (!chat) return;
    const lista = document.getElementById('chat-mensagens');
    const form = document.getElementById('form-chat');
    const texto = document.getElementById('texto');
    const enviar = document.getElementById('enviar-mensagem');
    const status = document.getElementById('status-chat');
    const novas = document.getElementById('ir-para-novas');
    const anteriores = document.getElementById('mensagens-anteriores');
    const chave = form.elements.namedItem('chave');
    const csrf = form.elements.namedItem('_csrf');
    const ids = new Set([...lista.querySelectorAll('[data-mensagem-id]')].map(n => Number(n.dataset.mensagemId)));
    let ultima = ids.size ? Math.max(...ids) : 0;
    let leituraConfirmada = 0;
    let lendo = false, atualizando = false, repetir = false, enviando = false, carregandoAntigas = false;
    const pessoa = chat.querySelector('h1').textContent;
    function noFim() { return lista.scrollHeight - lista.scrollTop - lista.clientHeight < 55; }
    function irAoFim() { lista.scrollTop = lista.scrollHeight; novas.hidden = true; requestAnimationFrame(marcarLida); }
    function parametros(dados) { return new URLSearchParams({...dados, _csrf:csrf.value}); }
    function adicionar(itens, prepend = false) {
        const fragmento = document.createDocumentFragment();
        for (const item of itens) {
            if (ids.has(item.id)) continue;
            ids.add(item.id);
            const bolha = document.createElement('article'); bolha.className = `chat-message ${item.propria ? 'mine' : 'received'}`; bolha.dataset.mensagemId = String(item.id);
            const autor = document.createElement('span'); autor.className = 'chat-author'; autor.textContent = item.propria ? 'Você' : pessoa;
            const conteudo = document.createElement('p'); conteudo.className = 'chat-text'; conteudo.textContent = item.texto;
            const horario = document.createElement('small'); horario.className = 'chat-time'; horario.textContent = item.horario;
            bolha.append(autor, conteudo, horario); fragmento.append(bolha);
        }
        if (ids.size) document.getElementById('chat-vazio')?.remove();
        if (prepend) lista.prepend(fragmento); else lista.append(fragmento);
    }
    async function marcarLida() {
        if (document.hidden || !document.hasFocus() || !noFim() || lendo || ultima <= leituraConfirmada) return;
        lendo = true; const ate = ultima;
        try {
            await requisitar(chat.dataset.lida, {method:'POST', body:parametros({ate})});
            leituraConfirmada = Math.max(leituraConfirmada, ate);
            window.dispatchEvent(new Event('chat-atualizado'));
        } catch (_) { /* Sem confirmar leitura quando a rede falha. */ }
        finally { lendo = false; }
    }
    async function atualizar() {
        if (document.hidden) return;
        if (atualizando) { repetir = true; return; }
        atualizando = true; repetir = false;
        try {
            const lote = await (await requisitar(`${chat.dataset.novas}?apos=${ultima}`)).json();
            if (document.hidden) return;
            const acompanhar = noFim();
            adicionar(lote.mensagens);
            if (lote.mensagens.length) ultima = lote.mensagens[lote.mensagens.length - 1].id;
            if (lote.anuncioId == null) {
                document.getElementById('link-anuncio-chat')?.remove();
                document.getElementById('anuncio-indisponivel').hidden = false;
            }
            if (acompanhar) irAoFim(); else if (lote.mensagens.length) novas.hidden = false;
            if (lote.temMais) repetir = true;
            if (status.dataset.tipo === 'conexao') { status.textContent = ''; status.dataset.tipo = ''; }
            requestAnimationFrame(marcarLida);
        } catch (erro) {
            if (!enviando && status.dataset.tipo !== 'envio') { status.dataset.tipo = 'conexao'; status.textContent = erro.name === 'AbortError' ? 'A atualização demorou. Tentaremos novamente.' : erro.message; }
        } finally { atualizando = false; if (repetir && !document.hidden) setTimeout(atualizar, 100); }
    }
    form.addEventListener('submit', async evento => {
        evento.preventDefault();
        if (enviando || !form.reportValidity()) return;
        if (!texto.value.trim()) { status.textContent = 'Escreva uma mensagem.'; return; }
        enviando = true; enviar.disabled = true; texto.readOnly = true;
        status.dataset.tipo = 'envio'; status.textContent = 'Enviando…';
        try {
            const resultado = await (await requisitar(chat.dataset.enviar, {method:'POST', body:new FormData(form)})).json();
            texto.value = ''; chave.value = resultado.proximaChave;
            status.textContent = 'Mensagem enviada.'; status.dataset.tipo = '';
            irAoFim(); await atualizar(); texto.focus();
        } catch (erro) { status.textContent = erro.name === 'AbortError' ? 'Não foi possível confirmar o envio. Tente novamente; seu texto foi mantido.' : erro.message; }
        finally { enviando = false; enviar.disabled = false; texto.readOnly = false; }
    });
    texto.addEventListener('keydown', evento => { if ((evento.ctrlKey || evento.metaKey) && evento.key === 'Enter') { evento.preventDefault(); form.requestSubmit(); } });
    novas.addEventListener('click', irAoFim);
    lista.addEventListener('scroll', () => { if (noFim()) { novas.hidden = true; marcarLida(); } });
    if (anteriores) anteriores.addEventListener('click', async evento => {
        evento.preventDefault(); if (carregandoAntigas) return;
        carregandoAntigas = true;
        try {
            const primeiro = Number(lista.querySelector('[data-mensagem-id]').dataset.mensagemId);
            const lote = await (await requisitar(`${chat.dataset.historico}?antes=${primeiro}`)).json();
            const altura = lista.scrollHeight; adicionar(lote.mensagens, true); lista.scrollTop += lista.scrollHeight - altura;
            if (!lote.temMais) anteriores.remove();
        } catch (erro) { status.textContent = erro.name === 'AbortError' ? 'Não foi possível carregar o histórico. Tente novamente.' : erro.message; }
        finally { carregandoAntigas = false; }
    });
    if (!new URLSearchParams(location.search).has('antes')) irAoFim();
    atualizar(); setInterval(atualizar, 5000);
    document.addEventListener('visibilitychange', atualizar); window.addEventListener('focus', atualizar);
})();
