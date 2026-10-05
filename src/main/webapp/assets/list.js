import {api, header, notice, subscribe, humanTable, confirmAction} from './common.js';
import {editHuman, refreshEditor} from './form.js';

header('list');
const message = document.querySelector('[data-message]');
const filters = document.querySelector('#filters');
const controls = document.querySelector('#sort-controls');
let page = 1;
let request = 0;
async function load() {
    const version = ++request;
    const params = new URLSearchParams(new FormData(filters));
    for (const [key, value] of new FormData(controls)) params.set(key, value);
    params.set('page', String(page));
    const data = await api(`humans?${params}`);
    if (version !== request) return;
    page = data.page;
    humanTable(document.querySelector('#humans'), data.items, {
        edit: id => editHuman(id, async () => { notice(message, 'Изменения сохранены', 'success'); await safeLoad(); }),
        remove: async human => {
            if (!await confirmAction(`Удалить «${human.name}» (ID ${human.id})?`)) return;
            try {
                await api(`humans/${human.id}`, {method: 'DELETE'});
                notice(message, 'Объект удалён', 'success');
                await load();
            } catch (error) { notice(message, error.message); }
        }
    });
    document.querySelector('#summary').textContent = `Объектов: ${data.total} · Страница ${data.page} из ${data.pages}`;
    document.querySelector('#previous').disabled = page === 1;
    document.querySelector('#next').disabled = page === data.pages;
}
async function safeLoad() {
    try { await load(); } catch (error) { notice(message, error.message); }
}
filters.addEventListener('submit', event => { event.preventDefault(); page = 1; safeLoad(); });
filters.addEventListener('reset', () => { page = 1; queueMicrotask(safeLoad); });
controls.addEventListener('change', () => { page = 1; safeLoad(); });
document.querySelector('#previous').onclick = () => { page--; safeLoad(); };
document.querySelector('#next').onclick = () => { page++; safeLoad(); };
await safeLoad();
subscribe(async () => { await load(); await refreshEditor(); });
