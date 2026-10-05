export const base = new URL(document.body.dataset.base || './', window.location.href);
export const url = path => new URL(path, base).href;
export const element = (tag, text, className) => {
    const node = document.createElement(tag);
    if (text !== undefined && text !== null) node.textContent = String(text);
    if (className) node.className = className;
    return node;
};
export const display = value => {
    if (value === null || value === undefined) return '—';
    if (typeof value === 'boolean') {
        if (value) return 'Да';
        return 'Нет';
    }
    return String(value);
};
export function notice(target, message, kind = 'error') {
    target.textContent = message;
    target.className = `notice ${kind}`;
    target.hidden = !message;
}
export async function api(path, options = {}) {
    const headers = {Accept: 'application/json', ...options.headers};
    if (options.body !== undefined) headers['Content-Type'] = 'application/json';
    let response;
    try {
        response = await fetch(url(`api/${path}`), {...options, headers, cache: 'no-store'});
    } catch {
        throw new Error('Нет связи с сервером. Проверьте подключение и повторите действие.');
    }
    if (response.status === 204) return null;
    let data;
    try {
        data = await response.json();
    } catch {
        throw new Error(`Сервер вернул неожиданный ответ (${response.status}).`);
    }
    if (!response.ok) {
        const error = new Error(data.message || `Ошибка запроса (${response.status})`);
        error.status = response.status;
        throw error;
    }
    return data;
}

export function header(current) {
    const wrapper = element('div', null, 'header-inner');
    const brand = element('a', 'HumanBeing', 'brand');
    brand.href = url('index.html');
    const nav = element('nav');
    nav.setAttribute('aria-label', 'Главное меню');
    for (const [path, label, key] of [
        ['index.html', 'Все объекты', 'list'],
        ['human/create.html', 'Создать', 'create'],
        ['special.html', 'Специальные операции', 'special']
    ]) {
        const link = element('a', label);
        link.href = url(path);
        if (key === current) link.setAttribute('aria-current', 'page');
        nav.append(link);
    }
    wrapper.append(brand, nav);
    document.querySelector('header').append(wrapper);
}

export function subscribe(refresh) {
    const state = document.querySelector('[data-connection]');
    let socket;
    let timer;
    let stopped = false;
    let delay = 1000;
    let running = false;
    let pending = false;
    async function update() {
        pending = true;
        if (running) return;
        running = true;
        try {
            while (pending && !stopped) {
                pending = false;
                await refresh();
            }
        } catch (error) {
            notice(document.querySelector('[data-message]'), error.message);
        } finally {
            running = false;
        }
    }
    function status(text, connected) {
        if (!state) return;
        state.textContent = text;
        state.classList.toggle('connected', connected);
    }
    function connect() {
        if (stopped) return;
        const endpoint = new URL('changes', base);
        if (endpoint.protocol === 'https:') endpoint.protocol = 'wss:';
        else endpoint.protocol = 'ws:';
        status('Подключаем автоматическое обновление…', false);
        socket = new WebSocket(endpoint);
        socket.onopen = () => {
            delay = 1000;
            status('Автоматическое обновление подключено', true);
            // Refresh on reconnect, including changes missed while disconnected.
            update();
        };
        socket.onmessage = event => {
            try {
                if (JSON.parse(event.data).type === 'changed') update();
            } catch { /* Ignore messages outside the update protocol. */ }
        };
        socket.onclose = () => {
            if (stopped) return;
            status('Автообновление отключено. Восстанавливаем соединение…', false);
            timer = setTimeout(connect, delay);
            delay = Math.min(delay * 2, 15000);
        };
        socket.onerror = () => socket.close();
    }
    const visible = () => {
        if (document.visibilityState === 'visible') update();
    };
    document.addEventListener('visibilitychange', visible);
    window.addEventListener('pagehide', () => {
        stopped = true;
        clearTimeout(timer);
        if (socket) socket.close();
        document.removeEventListener('visibilitychange', visible);
    });
    window.addEventListener('pageshow', event => {
        if (event.persisted) {
            stopped = false;
            document.addEventListener('visibilitychange', visible);
            connect();
        }
    });
    connect();
}

export function humanTable(container, humans, {edit, remove} = {}) {
    const table = element('table');
    const caption = element('caption', 'Объекты HumanBeing');
    caption.hidden = true;
    const head = element('thead');
    const heading = element('tr');
    for (const label of ['ID', 'Имя', 'Координаты', 'Дата создания', 'Герой', 'Зубочистка', 'Машина', 'Настроение', 'Скорость удара', 'Ожидание, мин', 'Оружие', 'Действия']) {
        const th = element('th', label);
        th.scope = 'col';
        heading.append(th);
    }
    head.append(heading);
    const body = element('tbody');
    for (const human of humans) {
        const row = element('tr');
        row.dataset.id = human.id;
        const coordinate = `#${human.coordinates.id}: ${human.coordinates.x}; ${human.coordinates.y}`;
        const car = `#${human.car.id}, крутая: ${display(human.car.cool)}`;
        for (const value of [human.id, human.name, coordinate, human.creationDate, display(human.realHero), display(human.hasToothpick), car, human.mood, human.impactSpeed, human.minutesOfWaiting, human.weaponType]) {
            row.append(element('td', display(value)));
        }
        row.firstChild.className = 'data';
        const actions = element('td');
        const group = element('div', null, 'cell-actions');
        const link = element('a', 'Открыть', 'button secondary');
        link.href = url(`human/view.html?id=${human.id}`);
        group.append(link);
        if (edit) {
            const button = element('button', 'Изменить', 'secondary');
            button.type = 'button';
            button.addEventListener('click', () => edit(human.id));
            group.append(button);
        }
        if (remove) {
            const button = element('button', 'Удалить', 'danger');
            button.type = 'button';
            button.addEventListener('click', () => remove(human));
            group.append(button);
        }
        actions.append(group);
        row.append(actions);
        body.append(row);
    }
    if (humans.length === 0) {
        const row = element('tr');
        const cell = element('td', 'Объекты не найдены. Измените фильтры или создайте новый объект.');
        cell.colSpan = 12;
        row.append(cell);
        body.append(row);
    }
    table.append(caption, head, body);
    container.replaceChildren(table);
}

export async function confirmAction(message, actionLabel = 'Удалить') {
    const dialog = element('dialog');
    const title = element('h2', 'Подтверждение');
    const id = `confirm-${Date.now()}`;
    title.id = id;
    dialog.setAttribute('aria-labelledby', id);
    dialog.append(title, element('p', message));
    const buttons = element('div', null, 'actions');
    const cancel = element('button', 'Отмена', 'secondary');
    const accept = element('button', actionLabel, 'danger');
    buttons.append(cancel, accept);
    dialog.append(buttons);
    document.body.append(dialog);
    return new Promise(resolve => {
        let confirmed = false;
        cancel.onclick = () => dialog.close();
        accept.onclick = () => { confirmed = true; dialog.close(); };
        dialog.addEventListener('close', () => { dialog.remove(); resolve(confirmed); }, {once: true});
        dialog.showModal();
        cancel.focus();
    });
}
