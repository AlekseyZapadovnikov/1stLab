import {api, header, element, notice, subscribe, humanTable, confirmAction} from './common.js';

header('special');
const message = document.querySelector('[data-message]');
const search = document.querySelector('#search');
let searchActive = false;
let searchVersion = 0;
let activeSubstring = '';
async function refreshSearch() {
    if (!searchActive) return;
    const version = ++searchVersion;
    const data = await api(`special/search?${new URLSearchParams({substring: activeSubstring})}`);
    if (version === searchVersion) humanTable(document.querySelector('#search-results'), data.items);
}
async function refresh() {
    const [average, groups] = await Promise.all([api('special/average'), api('special/groups')]);
    let averageText = 'Нет объектов';
    if (average.average !== null) averageText = `${average.average} мин`;
    document.querySelector('#average').textContent = averageText;
    const table = element('table');
    const head = element('thead');
    const row = element('tr');
    for (const text of ['Имя', 'Количество']) {
        const cell = element('th', text);
        cell.scope = 'col';
        row.append(cell);
    }
    head.append(row);
    const body = element('tbody');
    for (const group of groups.groups) {
        const row = element('tr');
        row.append(element('td', group.name), element('td', group.count, 'data'));
        body.append(row);
    }
    if (!groups.groups.length) {
        const row = element('tr');
        const cell = element('td', 'Нет объектов для группировки');
        cell.colSpan = 2;
        row.append(cell);
        body.append(row);
    }
    table.append(head, body);
    document.querySelector('#groups').replaceChildren(table);
    await refreshSearch();
}
search.addEventListener('submit', async event => {
    event.preventDefault();
    searchActive = true;
    activeSubstring = search.elements.substring.value;
    try { await refreshSearch(); } catch (error) { notice(message, error.message); }
});
for (const [id, path, confirmation, success] of [
    ['delete-heroes', 'delete-without-toothpicks', 'Удалить всех героев без зубочисток?', 'Удалено объектов'],
    ['make-gloomy', 'make-gloomy', 'Установить всем героям настроение GLOOM?', 'Изменено объектов']
]) {
    const button = document.getElementById(id);
    button.onclick = async () => {
        if (!await confirmAction(confirmation, 'Выполнить')) return;
        button.disabled = true;
        try {
            const result = await api(`special/${path}`, {method: 'POST'});
            notice(message, `${success}: ${result.count}`, 'success');
            await refresh();
        } catch (error) { notice(message, error.message); }
        finally { button.disabled = false; }
    };
}
try { await refresh(); } catch (error) { notice(message, error.message); }
subscribe(refresh);
