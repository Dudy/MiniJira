const sessionBox = document.querySelector('#sessionBox');
const logoutButton = document.querySelector('#logoutButton');
const messageBox = document.querySelector('#messageBox');
const issueTableBody = document.querySelector('#issueTableBody');
const authPanel = document.querySelector('#authPanel');
const editPanel = document.querySelector('#editPanel');
const authRequiredSections = [...document.querySelectorAll('[data-auth-required]')];
const authForms = new Map([...document.querySelectorAll('[data-auth-form]')].map(form => [form.dataset.authForm, form]));
const authTabs = [...document.querySelectorAll('[data-auth-target]')];
const passwordToggles = [...document.querySelectorAll('[data-password-toggle]')];
let activeAuthMode = 'login';

function eyeIcon(isVisible) {
    return isVisible
        ? `
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <path d="M3.6 12c1.7-4.2 5.3-7 8.4-7s6.7 2.8 8.4 7c-1.7 4.2-5.3 7-8.4 7s-6.7-2.8-8.4-7Z" fill="none" stroke="currentColor" stroke-width="1.8" />
                <circle cx="12" cy="12" r="2.8" fill="none" stroke="currentColor" stroke-width="1.8" />
                <path d="M4 4 20 20" fill="none" stroke="currentColor" stroke-width="1.8" />
            </svg>
        `
        : `
            <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
                <path d="M2.5 12c1.8-4.5 5.6-7.5 9.5-7.5s7.7 3 9.5 7.5c-1.8 4.5-5.6 7.5-9.5 7.5S4.3 16.5 2.5 12Z" fill="none" stroke="currentColor" stroke-width="1.8" />
                <circle cx="12" cy="12" r="2.8" fill="none" stroke="currentColor" stroke-width="1.8" />
            </svg>
        `;
}

function setPasswordVisibility(button, input, isVisible) {
    input.type = isVisible ? 'text' : 'password';
    button.setAttribute('aria-pressed', String(isVisible));
    button.setAttribute('title', isVisible ? 'Passwort verbergen' : 'Passwort anzeigen');
    button.setAttribute('aria-label', isVisible ? 'Passwort verbergen' : 'Passwort anzeigen');
    button.innerHTML = eyeIcon(isVisible);
}

function setAuthMode(mode) {
    activeAuthMode = mode;
    for (const [formMode, form] of authForms) {
        form.hidden = formMode !== mode;
    }
    for (const tab of authTabs) {
        const isActive = tab.dataset.authTarget === mode;
        tab.setAttribute('aria-pressed', String(isActive));
    }
}

function setAuthenticatedView(isAuthenticated) {
    authPanel.hidden = isAuthenticated;
    for (const section of authRequiredSections) {
        section.hidden = !isAuthenticated;
    }
    if (!isAuthenticated) {
        issueTableBody.replaceChildren();
        editPanel.hidden = true;
        setAuthMode('login');
    }
}

async function request(url, options = {}) {
    const response = await fetch(url, {
        headers: {'Content-Type': 'application/json', ...(options.headers ?? {})},
        credentials: 'same-origin',
        ...options
    });

    const text = await response.text();
    const body = text ? JSON.parse(text) : null;

    if (!response.ok) {
        const msg = body?.message ?? `${response.status} ${response.statusText}`;
        throw new Error(msg);
    }
    return body;
}

function showMessage(message, isError = false) {
    messageBox.textContent = message;
    messageBox.className = isError ? 'error' : 'ok';
}

function parseIds(value) {
    if (!value || !value.trim()) {
        return [];
    }
    return value.split(',')
        .map(v => Number.parseInt(v.trim(), 10))
        .filter(Number.isInteger);
}

function userLabel(user) {
    return `${user.username} (#${user.id})`;
}

function prepareLoginAfterRegistration(username, password) {
    const loginForm = document.querySelector('#loginForm');
    loginForm.elements.username.value = username;
    loginForm.elements.password.value = password;
    setAuthMode('login');
}

async function refreshSession() {
    const session = await request('/api/session');
    if (session.loggedIn) {
        sessionBox.textContent = `Eingeloggt als ${session.username} (#${session.userId})`;
        document.querySelector('input[name="authorUserId"]').value = session.userId;
        logoutButton.hidden = false;
        setAuthenticatedView(true);
        await reloadIssues();
    } else {
        sessionBox.textContent = 'Nicht eingeloggt';
        logoutButton.hidden = true;
        setAuthenticatedView(false);
    }
}

async function reloadIssues() {
    const sort = document.querySelector('#sortSelect').value;
    const direction = document.querySelector('#directionSelect').value;
    const issues = await request(`/api/issues?sort=${encodeURIComponent(sort)}&direction=${encodeURIComponent(direction)}`);
    issueTableBody.replaceChildren(...issues.map(toIssueRow));
}

function toIssueRow(issue) {
    const tr = document.createElement('tr');
    tr.innerHTML = `
        <td>${issue.id}</td>
        <td>${userLabel(issue.author)}</td>
        <td>${issue.workers.map(userLabel).join(', ')}</td>
        <td>${escapeHtml(issue.title)}</td>
        <td>${issue.priority}</td>
        <td><button data-edit-id="${issue.id}">Bearbeiten</button></td>
    `;
    tr.querySelector('button').addEventListener('click', () => loadIssueForEdit(issue.id));
    return tr;
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

async function loadIssueForEdit(id) {
    const issue = await request(`/api/issues/${id}`);
    const form = document.querySelector('#editIssueForm');
    form.elements.id.value = issue.id;
    form.elements.workerUserIds.value = issue.workers.map(w => w.id).join(',');
    form.elements.title.value = issue.title;
    form.elements.description.value = issue.description;
    editPanel.hidden = false;
    editPanel.scrollIntoView({behavior: 'smooth'});
}

document.querySelector('#createUserForm').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const username = form.elements.username.value;
    const password = form.elements.password.value;
    try {
        const user = await request('/api/users', {
            method: 'POST',
            body: JSON.stringify({
                username,
                password
            })
        });
        showMessage(`User angelegt: ${userLabel(user)}`);
        prepareLoginAfterRegistration(username, password);
        form.reset();
    } catch (e) {
        showMessage(e.message, true);
    }
});

document.querySelector('#loginForm').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    try {
        await request('/api/session', {
            method: 'POST',
            body: JSON.stringify({
                username: form.elements.username.value,
                password: form.elements.password.value
            })
        });
        await refreshSession();
        showMessage('Login erfolgreich');
    } catch (e) {
        showMessage(e.message, true);
    }
});

logoutButton.addEventListener('click', async () => {
    try {
        await request('/api/session', {method: 'DELETE'});
        await refreshSession();
        showMessage('Logout erfolgreich');
    } catch (e) {
        showMessage(e.message, true);
    }
});

document.querySelector('#createIssueForm').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    try {
        const issue = await request('/api/issues', {
            method: 'POST',
            body: JSON.stringify({
                authorUserId: Number.parseInt(form.elements.authorUserId.value, 10),
                workerUserIds: parseIds(form.elements.workerUserIds.value),
                title: form.elements.title.value,
                description: form.elements.description.value,
                priority: Number.parseInt(form.elements.priority.value, 10)
            })
        });
        showMessage(`Issue #${issue.id} angelegt`);
        form.elements.title.value = '';
        form.elements.description.value = '';
        await reloadIssues();
    } catch (e) {
        showMessage(e.message, true);
    }
});

document.querySelector('#editIssueForm').addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    try {
        const id = Number.parseInt(form.elements.id.value, 10);
        await request(`/api/issues/${id}`, {
            method: 'PUT',
            body: JSON.stringify({
                workerUserIds: parseIds(form.elements.workerUserIds.value),
                title: form.elements.title.value,
                description: form.elements.description.value
            })
        });
        showMessage(`Issue #${id} gespeichert`);
        await reloadIssues();
    } catch (e) {
        showMessage(e.message, true);
    }
});

document.querySelector('#reloadIssuesButton').addEventListener('click', () => reloadIssues().catch(e => showMessage(e.message, true)));
document.querySelector('#sortSelect').addEventListener('change', () => reloadIssues().catch(e => showMessage(e.message, true)));
document.querySelector('#directionSelect').addEventListener('change', () => reloadIssues().catch(e => showMessage(e.message, true)));

for (const tab of authTabs) {
    tab.addEventListener('click', () => setAuthMode(tab.dataset.authTarget));
}

for (const toggle of passwordToggles) {
    const input = document.querySelector(`#${toggle.dataset.passwordInput}`);
    setPasswordVisibility(toggle, input, false);
    toggle.addEventListener('click', () => {
        const isVisible = input.type === 'password';
        setPasswordVisibility(toggle, input, isVisible);
    });
}

setAuthMode(activeAuthMode);
await refreshSession();
