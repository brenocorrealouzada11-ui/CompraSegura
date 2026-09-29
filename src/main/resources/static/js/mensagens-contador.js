(() => {
    const contadores = [...document.querySelectorAll('[data-contador-mensagens]')];
    if (!contadores.length) return;
    let atualizando = false;
    let sessaoEncerrada = false;
    async function atualizar() {
        if (atualizando || sessaoEncerrada || document.hidden) return;
        atualizando = true;
        const controle = new AbortController();
        const timeout = setTimeout(() => controle.abort(), 12000);
        try {
            const resposta = await fetch(contadores[0].dataset.contadorMensagens, {credentials: 'same-origin', cache: 'no-store', signal: controle.signal});
            if (resposta.redirected || resposta.status === 401 || resposta.status === 403) { sessaoEncerrada = true; return; }
            if (!resposta.ok) return;
            const dados = await resposta.json();
            for (const contador of contadores) {
                contador.textContent = dados.total > 99 ? '99+' : String(dados.total);
                contador.hidden = dados.total === 0;
                contador.setAttribute('aria-label', `${dados.total} mensagens não lidas`);
            }
        } catch (_) { /* Mantem o ultimo contador confirmado e tenta no proximo intervalo. */ }
        finally { clearTimeout(timeout); atualizando = false; }
    }
    atualizar(); setInterval(atualizar, 10000);
    document.addEventListener('visibilitychange', atualizar);
    window.addEventListener('chat-atualizado', atualizar);
})();
