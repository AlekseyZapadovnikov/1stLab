import {api, element, notice} from './common.js';

export function humanForm(host, human, onSave) {
    const form = element('form');
    // Static markup only. All server-provided values are assigned as properties.
    form.innerHTML = `
        <div class="notice" role="alert" data-form-message hidden></div>
        <div class="form-grid">
            <label>Имя<input name="name" required autocomplete="off"></label>
            <label>Ожидание, минуты<input name="minutesOfWaiting" type="number" step="any" required></label>
            <label class="check"><input name="realHero" type="checkbox">Герой</label>
            <label class="check"><input name="hasToothpick" type="checkbox">Есть зубочистка</label>
            <label>Настроение<select name="mood"></select></label>
            <label>Оружие<select name="weaponType"></select></label>
            <label>Скорость удара<input name="impactSpeed" inputmode="numeric" pattern="[+-]?[0-9]+"><span class="field-hint">Целое число; можно оставить пустым</span></label>
        </div>
        <fieldset><legend>Машина</legend>
            <label>Связать с машиной<select name="carId"><option value="new">Создать новую</option></select></label>
            <div data-new-car class="relation-fields"><label class="check"><input name="cool" type="checkbox">Крутая машина</label></div>
        </fieldset>
        <fieldset><legend>Координаты</legend>
            <label>Связать с координатами<select name="coordinatesId"><option value="new">Создать новые</option></select></label>
            <div data-new-coordinates class="form-grid relation-fields">
                <label>X<input name="x" inputmode="numeric" pattern="[+-]?[0-9]+" required><span class="field-hint">Целое число</span></label>
                <label>Y<input name="y" type="number" step="any" max="791" required><span class="field-hint">Не больше 791</span></label>
            </div>
        </fieldset>
        <div class="actions"><button type="submit" data-save>Сохранить</button></div>`;
    const fields = form.elements;
    let saving = false;
    let initialized = false;
    const errorBox = form.querySelector('[data-form-message]');
    form.querySelector('[data-save]').disabled = true;
    fields.name.value = '';
    fields.minutesOfWaiting.value = '0';
    if (human) {
        fields.name.value = human.name;
        fields.minutesOfWaiting.value = human.minutesOfWaiting;
        fields.realHero.checked = human.realHero;
        fields.hasToothpick.checked = human.hasToothpick;
        fields.impactSpeed.value = human.impactSpeed ?? '';
    }
    function relationMode() {
        const newCar = fields.carId.value === 'new';
        const newCoordinates = fields.coordinatesId.value === 'new';
        form.querySelector('[data-new-car]').hidden = !newCar;
        fields.cool.disabled = !newCar;
        form.querySelector('[data-new-coordinates]').hidden = !newCoordinates;
        fields.x.disabled = !newCoordinates;
        fields.y.disabled = !newCoordinates;
    }
    fields.carId.addEventListener('change', relationMode);
    fields.coordinatesId.addEventListener('change', relationMode);
    const option = (value, label) => {
        const node = element('option', label);
        node.value = String(value);
        return node;
    };
    async function loadRelations(initial = false) {
        initial = initial || !initialized;
        const relations = await api('relations');
        let carId = fields.carId.value;
        let coordinatesId = fields.coordinatesId.value;
        if (initial && human) {
            carId = String(human.car.id);
            coordinatesId = String(human.coordinates.id);
        }
        fields.carId.replaceChildren(option('new', 'Создать новую'));
        for (const car of relations.cars) {
            let label = `#${car.id} — обычная`;
            if (car.cool) label = `#${car.id} — крутая`;
            fields.carId.append(option(car.id, label));
        }
        fields.coordinatesId.replaceChildren(option('new', 'Создать новые'));
        for (const coordinates of relations.coordinates) {
            fields.coordinatesId.append(option(coordinates.id, `#${coordinates.id} — X: ${coordinates.x}, Y: ${coordinates.y}`));
        }
        for (const [select, value] of [[fields.carId, carId], [fields.coordinatesId, coordinatesId]]) {
            if (![...select.options].some(item => item.value === value)) {
                const missing = option(value, `#${value} — объект больше недоступен`);
                missing.disabled = true;
                select.append(missing);
            }
            select.value = value;
        }
        if (initial) {
            for (const [field, values] of [['mood', relations.moods], ['weaponType', relations.weaponTypes]]) {
                fields[field].replaceChildren(option('', 'Не указано'));
                values.forEach(value => fields[field].append(option(value, value)));
                if (human) fields[field].value = human[field] ?? '';
            }
        }
        initialized = true;
        relationMode();
        form.querySelector('[data-save]').disabled = saving;
    }
    form.addEventListener('submit', async event => {
        event.preventDefault();
        if (saving || !form.reportValidity()) return;
        saving = true;
        const button = form.querySelector('[data-save]');
        button.disabled = true;
        notice(errorBox, '');
        try {
            let car;
            if (fields.carId.value === 'new') car = {cool: fields.cool.checked};
            else car = {id: Number(fields.carId.value)};
            let coordinates;
            if (fields.coordinatesId.value === 'new') coordinates = {x: fields.x.value, y: Number(fields.y.value)};
            else coordinates = {id: Number(fields.coordinatesId.value)};
            let impactSpeed = null;
            if (fields.impactSpeed.value !== '') impactSpeed = fields.impactSpeed.value;
            let mood = null;
            if (fields.mood.value) mood = fields.mood.value;
            let weaponType = null;
            if (fields.weaponType.value) weaponType = fields.weaponType.value;
            const payload = {
                name: fields.name.value, minutesOfWaiting: Number(fields.minutesOfWaiting.value),
                realHero: fields.realHero.checked, hasToothpick: fields.hasToothpick.checked,
                impactSpeed, mood, weaponType, car, coordinates
            };
            let method = 'POST';
            let path = 'humans';
            if (human) { method = 'PUT'; path = `humans/${human.id}`; }
            const saved = await api(path, {method, body: JSON.stringify(payload)});
            await onSave(saved);
        } catch (error) {
            notice(errorBox, error.message);
        } finally {
            saving = false;
            button.disabled = false;
        }
    });
    host.replaceChildren(form);
    return {ready: loadRelations(true), refreshRelations: () => loadRelations(false)};
}

let editor;
export async function editHuman(id, onSaved) {
    if (editor?.open) return;
    const dialog = element('dialog');
    editor = dialog;
    const heading = element('div', null, 'dialog-heading');
    const title = element('h2', `Изменить HumanBeing #${id}`);
    title.id = 'edit-title';
    dialog.setAttribute('aria-labelledby', title.id);
    const close = element('button', 'Закрыть', 'secondary');
    close.type = 'button';
    close.onclick = () => dialog.close();
    heading.append(title, close);
    const status = element('div');
    status.setAttribute('role', 'status');
    status.hidden = true;
    const host = element('div', 'Загрузка…');
    dialog.append(heading, status, host);
    document.body.append(dialog);
    dialog.addEventListener('close', () => { dialog.remove(); editor = null; }, {once: true});
    dialog.showModal();
    try {
        const human = await api(`humans/${id}`);
        if (!dialog.open) return;
        const form = humanForm(host, human, async saved => {
            dialog.close();
            await onSaved(saved);
        });
        dialog.refreshRelations = form.refreshRelations;
        await form.ready;
    } catch (error) {
        if (dialog.open) {
            if (!host.querySelector('form')) host.textContent = '';
            notice(status, error.message);
        }
    }
    dialog.updateNotice = () => notice(status,
        'Данные изменились. Введённые значения сохранены в форме. При сохранении они заменят текущие значения объекта.', '');
}

export async function refreshEditor() {
    if (editor?.open) {
        editor.updateNotice?.();
        await editor.refreshRelations?.();
    }
}
