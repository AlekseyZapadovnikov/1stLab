import {api, header, element, display, notice, subscribe, confirmAction, url} from './common.js';
import {editHuman, refreshEditor} from './form.js';

header('');
const message = document.querySelector('[data-message]');
const details = document.querySelector('#details');
const actions = document.querySelector('#object-actions');
let id = new URLSearchParams(location.search).get('id');
let human;
let request = 0;
const lookup = document.querySelector('#lookup');
lookup.elements.id.value = id || '';
function item(label, value) {
    const container = element('div', null, 'detail');
    container.append(element('dt', label), element('dd', display(value)));
    return container;
}
async function load() {
    if (!id) return;
    const version = ++request;
    try {
        const loaded = await api(`humans/${encodeURIComponent(id)}`);
        if (request !== version) return;
        human = loaded;
        document.querySelector('#title').textContent = `HumanBeing #${human.id}`;
        const fields = [
            ['Имя', human.name], ['Дата создания', human.creationDate], ['Герой', human.realHero],
            ['Есть зубочистка', human.hasToothpick], ['Настроение', human.mood],
            ['Скорость удара', human.impactSpeed], ['Ожидание, минуты', human.minutesOfWaiting],
            ['Оружие', human.weaponType], ['Машина: ID', human.car.id], ['Крутая машина', human.car.cool],
            ['Координаты: ID', human.coordinates.id], ['X', human.coordinates.x], ['Y', human.coordinates.y]
        ];
        details.replaceChildren(...fields.map(([label, value]) => item(label, value)));
        details.hidden = false;
        actions.hidden = false;
    } catch (error) {
        if (request !== version) return;
        if (error.status === 404 || error.status === 400) {
            human = null;
            details.replaceChildren();
            actions.hidden = true;
        }
        throw error;
    }
}
async function safeLoad() {
    try { await load(); } catch (error) { notice(message, error.message); }
}
lookup.addEventListener('submit', event => {
    event.preventDefault();
    id = lookup.elements.id.value;
    history.replaceState(null, '', `?id=${encodeURIComponent(id)}`);
    notice(message, '');
    safeLoad();
});
document.querySelector('#edit').onclick = () => editHuman(human.id, async () => {
    notice(message, 'Изменения сохранены', 'success');
    await safeLoad();
});
document.querySelector('#delete').onclick = async () => {
    if (!human || !await confirmAction(`Удалить «${human.name}» (ID ${human.id})?`)) return;
    try {
        await api(`humans/${human.id}`, {method: 'DELETE'});
        location.assign(url('index.html'));
    } catch (error) { notice(message, error.message); }
};
await safeLoad();
subscribe(async () => { await load(); await refreshEditor(); });
