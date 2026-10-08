(() => {
    const form = document.getElementById('form-comparacao');
    if (!form) return;
    const key = 'comprasegura.comparacao.v1';
    const checks = [...document.querySelectorAll('[data-comparison-id]')];
    const status = document.querySelector('[data-comparison-status]');
    const selectedList = document.querySelector('[data-comparison-selected]');
    const inputs = document.querySelector('[data-comparison-inputs]');
    const submit = document.querySelector('[data-comparison-submit]');
    const clear = document.querySelector('[data-comparison-clear]');
    function valid(item) { return item && typeof item.id === 'string' && /^[1-9][0-9]{0,18}$/.test(item.id) && typeof item.title === 'string'; }
    function normalize(value) {
        if (!Array.isArray(value)) return [];
        const unique = new Map();
        for (const item of value) if (valid(item) && !unique.has(item.id) && unique.size < 3) unique.set(item.id, {id:item.id, title:item.title.slice(0,120)});
        return [...unique.values()];
    }
    let selected = [];
    try { selected = normalize(JSON.parse(sessionStorage.getItem(key) || '[]')); } catch (_) { /* Seleção funciona mesmo sem armazenamento. */ }
    if (document.querySelector('[data-comparison-current]')) selected = normalize(checks.map(c => ({id:c.dataset.comparisonId, title:c.dataset.comparisonTitle})));
    else for (const check of checks) {
        const known = selected.find(item => item.id === check.dataset.comparisonId);
        if (known) known.title = check.dataset.comparisonTitle.slice(0,120);
    }
    function save() { try { sessionStorage.setItem(key, JSON.stringify(selected)); } catch (_) { /* Seleção apenas nesta página. */ } }
    function render(message) {
        inputs.replaceChildren(); selectedList.replaceChildren();
        for (const item of selected) {
            const input = document.createElement('input'); input.type = 'hidden'; input.name = 'ids'; input.value = item.id; inputs.append(input);
            const li = document.createElement('li'); const title = document.createElement('span'); title.textContent = item.title;
            const remove = document.createElement('button'); remove.type = 'button'; remove.className = 'text-button'; remove.textContent = 'Remover'; remove.setAttribute('aria-label', `Remover ${item.title} da comparação`);
            remove.addEventListener('click', () => { selected = selected.filter(i => i.id !== item.id); save(); render('Aparelho removido da seleção.'); clear.focus(); });
            li.append(title, remove); selectedList.append(li);
        }
        for (const check of checks) check.checked = selected.some(item => item.id === check.dataset.comparisonId);
        submit.disabled = selected.length < 2;
        status.textContent = message || `${selected.length} de 3 aparelhos selecionados.`;
    }
    for (const check of checks) {
        // O formulário sem JS usa os checkboxes. Com JS, os campos ocultos incluem outras páginas.
        check.removeAttribute('name');
        check.addEventListener('change', () => {
            if (check.checked) {
                if (selected.length >= 3) { check.checked = false; render('Você pode comparar no máximo três aparelhos. Remova um para escolher outro.'); return; }
                selected.push({id:check.dataset.comparisonId, title:check.dataset.comparisonTitle.slice(0,120)});
            } else selected = selected.filter(item => item.id !== check.dataset.comparisonId);
            save(); render();
        });
    }
    clear.hidden = false;
    clear.addEventListener('click', () => { selected = []; save(); render('Seleção limpa. Escolha dois ou três aparelhos.'); });
    form.addEventListener('submit', event => { if (selected.length < 2 || selected.length > 3) { event.preventDefault(); render('Selecione dois ou três aparelhos.'); } });
    save(); render();
    window.addEventListener('pageshow', event => {
        if (!event.persisted) return;
        try { selected = normalize(JSON.parse(sessionStorage.getItem(key) || '[]')); } catch (_) { }
        render();
    });
})();
