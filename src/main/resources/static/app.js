const sessionBox = document.querySelector('#sessionBox');
const logoutButton = document.querySelector('#logoutButton');
const messageBox = document.querySelector('#messageBox');
const issueTableBody = document.querySelector('#issueTableBody');
const authPanel = document.querySelector('#authPanel');
const overviewPanel = document.querySelector('#overviewPanel');
const issueDialog = document.querySelector('#issueDialog');
const issueForm = document.querySelector('#issueForm');
const issueWorkerUsers = document.querySelector('#issueWorkerUsers');
const authForm = document.querySelector('#authForm');
const authHeading = document.querySelector('#authHeading');
const authSubmitButton = document.querySelector('#authSubmitButton');
const authRequiredSections = [...document.querySelectorAll('[data-auth-required]')];
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
    authHeading.textContent = mode === 'login' ? 'Login' : 'Register';
    authSubmitButton.textContent = mode === 'login' ? 'Login' : 'Registrieren';
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
        closeIssueDialog();
        setAuthMode('login');
    }
}

function setActiveView(view) {
    overviewButton.setAttribute('aria-current', view === 'overview' ? 'page' : 'false');
}

function closeIssueDialog() {
    if (issueDialog.open) {
        issueDialog.close();
    }
}

function openDialog(dialog) {
    closeIssueDialog();
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
    issueForm.reset();
    issueForm.elements.id.value = '';
}

function priorityLabel(priority) {
    switch (Number(priority)) {
        case 1:
            return 'Sehr hoch';
        case 2:
            return 'Hoch';
        case 3:
            return 'Mittel';
        case 4:
            return 'Niedrig';
        case 5:
            return 'Sehr niedrig';
        default:
            return String(priority);
    }
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
    renderUserCheckboxes(issueWorkerUsers, availableUsers);
}

async function openCreateIssueDialog() {
    await loadUsers();
    clearUserSelections();
    renderUserCheckboxes(issueWorkerUsers, availableUsers);
    openDialog(issueDialog);
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
        renderUserCheckboxes(issueWorkerUsers, availableUsers);
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
        <td>${priorityLabel(issue.priority)}</td>
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
    issueForm.elements.id.value = issue.id;
    issueForm.elements.priority.value = String(issue.priority);
    issueForm.elements.title.value = issue.title;
    issueForm.elements.description.value = issue.description;
    renderUserCheckboxes(issueWorkerUsers, availableUsers, issue.workers.map(worker => worker.id));
    openDialog(issueDialog);
}

authForm.addEventListener('submit', async event => {
    event.preventDefault();
    const form = event.currentTarget;
    const username = form.elements.username.value;
    const password = form.elements.password.value;
    try {
        if (activeAuthMode === 'register') {
            const user = await request('/api/users', {
                method: 'POST',
                body: JSON.stringify({
                    username,
                    password
                })
            });
            showMessage(`User angelegt: ${user.username}`);
            setAuthMode('login');
            return;
        }

        await request('/api/session', {
            method: 'POST',
            body: JSON.stringify({
                username,
                password
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

issueForm.addEventListener('submit', async event => {
    event.preventDefault();
    try {
        const payload = {
            workerUserIds: [...issueForm.querySelectorAll('input[name="workerUserIds"]:checked')]
                .map(input => Number.parseInt(input.value, 10)),
            title: issueForm.elements.title.value,
            description: issueForm.elements.description.value,
            priority: Number.parseInt(issueForm.elements.priority.value, 10)
        };

        const id = issueForm.elements.id.value ? Number.parseInt(issueForm.elements.id.value, 10) : null;
        const issue = id === null
            ? await request('/api/issues', {
                method: 'POST',
                body: JSON.stringify(payload)
            })
            : await request(`/api/issues/${id}`, {
                method: 'PUT',
                body: JSON.stringify(payload)
            });

        showMessage(id === null ? `Issue #${issue.id} angelegt` : `Issue #${id} gespeichert`);
        issueDialog.close();
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
