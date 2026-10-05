import {header, notice, subscribe, url} from './common.js';
import {humanForm} from './form.js';

header('create');
const message = document.querySelector('[data-message]');
const form = humanForm(document.querySelector('#create-form'), null, saved => {
    window.location.assign(url(`human/view.html?id=${saved.id}`));
});
try {
    await form.ready;
} catch (error) {
    notice(message, error.message);
}
subscribe(form.refreshRelations);
