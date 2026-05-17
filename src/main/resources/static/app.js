const sessionBox = document.querySelector('#sessionBox');
const logoutButton = document.querySelector('#logoutButton');
const messageBox = document.querySelector('#messageBox');
const issueTableBody = document.querySelector('#issueTableBody');
const authPanel = document.querySelector('#authPanel');
const overviewPanel = document.querySelector('#overviewPanel');
const createIssueDialog = document.querySelector('#createIssueDialog');
const editIssueDialog = document.querySelector('#editIssueDialog');
const createIssueForm = document.querySelector('#createIssueForm');
const editIssueForm = document.querySelector('#editIssueForm');
const createWorkerUsers = document.querySelector('#createWorkerUsers');
const editWorkerUsers = document.querySelector('#editWorkerUsers');
const authRequiredSections = [...document.querySelectorAll('[data-auth-required]')];
const authForms = new Map([...document.querySelectorAll('[data-auth-form]')].map(form => [form.dataset.authForm, form]));
const authTabs = [...document.querySelectorAll('[data-auth-target]')];
const passwordToggles = [...document.querySelectorAll('[data-password-toggle]')];
const overviewButton = document.querySelector('#overviewButton');
const openCreateIssueButton = document.querySelector('#openCreateIssueButton');
const dialogCloseButtons = [...document.querySelectorAll('[data-dialog-close]')];
let activeAuthMode = 'login';
let availableUsers = [];

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
        clearUserSelections();
        closeIssueDialogs();
        setAuthMode('login');
    }
}

function setActiveView(view) {
    overviewButton.setAttribute('aria-current', view === 'overview' ? 'page' : 'false');
}

function closeIssueDialogs() {
    for (const dialog of [createIssueDialog, editIssueDialog]) {
        if (dialog.open) {
            dialog.close();
        }
    }
}

function openDialog(dialog) {
    closeIssueDialogs();
    dialog.showModal();
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

function userLabel(user) {
    return user.username;
}

function formatUserList(users) {
    return users.map(userLabel).join(', ') || '—';
}

function clearUserSelections() {
    createIssueForm.reset();
    editIssueForm.reset();
}

function renderUserCheckboxes(container, users, selectedIds = []) {
    const selected = new Set(selectedIds.map(id => Number.parseInt(id, 10)));
    container.replaceChildren(...users.map(user => {
        const label = document.createElement('label');
        label.className = 'user-checkbox';

        const input = document.createElement('input');
        input.type = 'checkbox';
        input.name = 'workerUserIds';
        input.value = String(user.id);
        input.checked = selected.has(user.id);

        label.append(input, document.createTextNode(user.username));
        return label;
    }));
}

async function loadUsers() {
    availableUsers = await request('/api/users');
    renderUserCheckboxes(createWorkerUsers, availableUsers);
    renderUserCheckboxes(editWorkerUsers, availableUsers);
}

async function openCreateIssueDialog() {
    await loadUsers();
    createIssueForm.reset();
    openDialog(createIssueDialog);
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
        sessionBox.textContent = `Eingeloggt als ${session.username}`;
        logoutButton.hidden = false;
        setAuthenticatedView(true);
        setActiveView('overview');
        await loadUsers();
        await reloadIssues();
    } else {
        sessionBox.textContent = 'Nicht eingeloggt';
        logoutButton.hidden = true;
        availableUsers = [];
        renderUserCheckboxes(createWorkerUsers, availableUsers);
        renderUserCheckboxes(editWorkerUsers, availableUsers);
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
    tr.classList.add('issue-row');
    tr.innerHTML = `
        <td>${issue.id}</td>
        <td>${userLabel(issue.author)}</td>
        <td>${formatUserList(issue.workers)}</td>
        <td>${escapeHtml(issue.title)}</td>
        <td>${issue.priority}</td>
    `;
    tr.addEventListener('dblclick', () => loadIssueForEdit(issue.id).catch(e => showMessage(e.message, true)));
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
    const [issue] = await Promise.all([
        request(`/api/issues/${id}`),
        loadUsers()
    ]);
    editIssueForm.elements.id.value = issue.id;
    editIssueForm.elements.title.value = issue.title;
    editIssueForm.elements.description.value = issue.description;
    renderUserCheckboxes(editWorkerUsers, availableUsers, issue.workers.map(worker => worker.id));
    openDialog(editIssueDialog);
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
        showMessage(`User angelegt: ${user.username}`);
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
    try {
        const issue = await request('/api/issues', {
            method: 'POST',
            body: JSON.stringify({
                workerUserIds: [...createIssueForm.querySelectorAll('input[name="workerUserIds"]:checked')]
                    .map(input => Number.parseInt(input.value, 10)),
                title: createIssueForm.elements.title.value,
                description: createIssueForm.elements.description.value,
                priority: Number.parseInt(createIssueForm.elements.priority.value, 10)
            })
        });
        showMessage(`Issue #${issue.id} angelegt`);
        createIssueDialog.close();
        await reloadIssues();
    } catch (e) {
        showMessage(e.message, true);
    }
});

document.querySelector('#editIssueForm').addEventListener('submit', async event => {
    event.preventDefault();
    try {
        const id = Number.parseInt(editIssueForm.elements.id.value, 10);
        await request(`/api/issues/${id}`, {
            method: 'PUT',
            body: JSON.stringify({
                workerUserIds: [...editIssueForm.querySelectorAll('input[name="workerUserIds"]:checked')]
                    .map(input => Number.parseInt(input.value, 10)),
                title: editIssueForm.elements.title.value,
                description: editIssueForm.elements.description.value
            })
        });
        showMessage(`Issue #${id} gespeichert`);
        editIssueDialog.close();
        await reloadIssues();
    } catch (e) {
        showMessage(e.message, true);
    }
});

overviewButton.addEventListener('click', () => {
    overviewPanel.scrollIntoView({behavior: 'smooth', block: 'start'});
    setActiveView('overview');
});

openCreateIssueButton.addEventListener('click', () => {
    openCreateIssueDialog().catch(e => showMessage(e.message, true));
});

document.querySelector('#reloadIssuesButton').addEventListener('click', () => reloadIssues().catch(e => showMessage(e.message, true)));
document.querySelector('#sortSelect').addEventListener('change', () => reloadIssues().catch(e => showMessage(e.message, true)));
document.querySelector('#directionSelect').addEventListener('change', () => reloadIssues().catch(e => showMessage(e.message, true)));

for (const button of dialogCloseButtons) {
    button.addEventListener('click', () => {
        const dialog = button.closest('dialog');
        dialog?.close();
    });
}

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
