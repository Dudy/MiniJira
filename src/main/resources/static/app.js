const sessionBox = document.querySelector('#sessionBox');
const userMenuWrapper = document.querySelector('#userMenuWrapper');
const userMenuButton = document.querySelector('#userMenuButton');
const userMenu = document.querySelector('#userMenu');
const profileButton = document.querySelector('[data-profile-button]');
const menuLogoutButton = document.querySelector('[data-logout-button]');
const dummyButton = document.querySelector('#dummyButton');
const messageBox = document.querySelector('#messageBox');
const issueTableBody = document.querySelector('#issueTableBody');
const issueSortButtons = [...document.querySelectorAll('[data-sort-field]')];
const issueFilterControls = [...document.querySelectorAll('[data-filter-field]')];
const issueFilterButtons = [...document.querySelectorAll('[data-filter-button]')];
const issueFilterMenus = [...document.querySelectorAll('[data-filter-menu]')];
const authPanel = document.querySelector('#authPanel');
const profilePanel = document.querySelector('#profilePanel');
const overviewPanel = document.querySelector('#overviewPanel');
const passwordDialog = document.querySelector('#passwordDialog');
const profileForm = document.querySelector('#profileForm');
const profileUserIdInput = document.querySelector('#profileUserId');
const profileUsernameInput = document.querySelector('#profileUsername');
const profileDisplayNameInput = document.querySelector('#profileDisplayName');
const profileOfficeInput = document.querySelector('#profileOffice');
const openPasswordDialogButton = document.querySelector('#openPasswordDialogButton');
const profileCancelButton = document.querySelector('#profileCancelButton');
const passwordForm = document.querySelector('#passwordForm');
const currentPasswordInput = document.querySelector('#currentPassword');
const newPasswordInput = document.querySelector('#newPassword');
const confirmNewPasswordInput = document.querySelector('#confirmNewPassword');
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
let currentSession = null;
let currentView = 'overview';
let isAuthenticated = false;
let issueSortState = {field: null, direction: null};
let issueFilterState = {author: null, worker: null, status: null, priority: null};
let issueListCache = [];
let openIssueFilterField = null;

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

function setAuthenticatedView(authState) {
    isAuthenticated = Boolean(authState);
    authPanel.hidden = isAuthenticated;
    for (const section of authRequiredSections) {
        section.hidden = !isAuthenticated;
    }
    if (!isAuthenticated) {
        currentView = 'overview';
        issueTableBody.replaceChildren();
        closeIssueFilter();
        clearUserSelections();
        closeIssueDialog();
        closePasswordDialog();
        setAuthMode('login');
    }
}

function setActiveView(view) {
    currentView = view;
    overviewButton.setAttribute('aria-current', view === 'overview' ? 'page' : 'false');
    dummyButton?.setAttribute('aria-current', 'false');
    if (!isAuthenticated) {
        overviewPanel.hidden = true;
        profilePanel.hidden = true;
        return;
    }
    overviewPanel.hidden = view !== 'overview';
    profilePanel.hidden = view !== 'profile';
}

function populateProfileForm(session) {
    profileUserIdInput.value = String(session.userId ?? '');
    profileUsernameInput.value = session.username ?? '';
    profileDisplayNameInput.value = session.displayName ?? '';
    profileOfficeInput.value = session.office ?? '';
}

function openProfileView() {
    if (!currentSession) {
        showMessage('Keine aktive Sitzung', true);
        return;
    }
    populateProfileForm(currentSession);
    setActiveView('profile');
    closePasswordDialog();
    profileUsernameInput.focus();
    profileUsernameInput.select();
}

function showOverviewView({focusTrigger = false} = {}) {
    closePasswordDialog();
    setActiveView('overview');
    if (focusTrigger) {
        overviewButton.focus();
    }
}

function closeIssueDialog() {
    if (issueDialog.open) {
        issueDialog.close();
    }
}

function closePasswordDialog() {
    if (passwordDialog.open) {
        passwordDialog.close();
    }
}

function openDialog(dialog) {
    closeIssueDialog();
    closePasswordDialog();
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

function getUserInitial(username) {
    const trimmed = username.trim();
    return trimmed ? trimmed.charAt(0).toUpperCase() : '?';
}

function closeUserMenu({focusTrigger = false} = {}) {
    userMenu.hidden = true;
    userMenuButton.setAttribute('aria-expanded', 'false');
    if (focusTrigger) {
        userMenuButton.focus();
    }
}

function openUserMenu() {
    userMenu.hidden = false;
    userMenuButton.setAttribute('aria-expanded', 'true');
    profileButton.focus();
}

function toggleUserMenu() {
    if (userMenu.hidden) {
        openUserMenu();
    } else {
        closeUserMenu();
    }
}

function setUserMenu(session) {
    userMenuWrapper.hidden = false;
    closeUserMenu();
    userMenuButton.textContent = getUserInitial(session.username);
    userMenuButton.setAttribute('aria-label', `Benutzermenü für ${session.username}`);
    userMenuButton.title = session.username;
}

function hideUserMenu() {
    closeUserMenu();
    userMenuWrapper.hidden = true;
    userMenuButton.textContent = '';
    userMenuButton.removeAttribute('aria-label');
    userMenuButton.removeAttribute('title');
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

function statusLabel(status) {
    switch (Number(status)) {
        case 1:
            return 'to do';
        case 2:
            return 'doing';
        case 3:
            return 'testing';
        case 4:
            return 'reviewing';
        case 5:
            return 'done';
        default:
            return String(status);
    }
}

function getIssueSortValue(issue, field) {
    switch (field) {
        case 'id':
            return issue.id ?? 0;
        case 'author':
            return issue.author?.username ?? '';
        case 'worker':
            return [...(issue.workers ?? [])]
                .map(worker => worker.username)
                .sort((left, right) => left.localeCompare(right, 'de', {numeric: true, sensitivity: 'base'}))
                .join(', ');
        case 'title':
            return issue.title ?? '';
        case 'status':
            return issue.status ?? 0;
        case 'priority':
            return issue.priority ?? 0;
        default:
            return issue.id ?? 0;
    }
}

function compareIssueValues(left, right, direction) {
    const factor = direction === 'desc' ? -1 : 1;
    if (typeof left === 'number' && typeof right === 'number') {
        return (left - right) * factor;
    }
    return String(left).localeCompare(String(right), 'de', {numeric: true, sensitivity: 'base'}) * factor;
}

function compareIssues(left, right) {
    if (!issueSortState.field || !issueSortState.direction) {
        return 0;
    }
    return compareIssueValues(
        getIssueSortValue(left, issueSortState.field),
        getIssueSortValue(right, issueSortState.field),
        issueSortState.direction
    );
}

function getFilteredIssues() {
    return issueListCache.filter(issue => {
        for (const field of Object.keys(issueFilterState)) {
            const selection = issueFilterState[field];
            if (!selection || selection.size === 0) {
                continue;
            }
            const values = getIssueFilterValues(issue, field);
            if (field === 'worker') {
                if (!values.some(value => selection.has(value))) {
                    return false;
                }
                continue;
            }
            if (!values.some(value => selection.has(value))) {
                return false;
            }
        }
        return true;
    });
}

function getSortedIssues() {
    if (!issueSortState.field || !issueSortState.direction) {
        return [...getFilteredIssues()];
    }
    return [...getFilteredIssues()].sort(compareIssues);
}

function updateIssueSortIndicators() {
    for (const button of issueSortButtons) {
        const field = button.dataset.sortField;
        const isActive = issueSortState.field === field && Boolean(issueSortState.direction);
        const indicator = button.querySelector('.sort-indicator');
        button.closest('th')?.setAttribute('aria-sort', isActive
            ? (issueSortState.direction === 'asc' ? 'ascending' : 'descending')
            : 'none');
        button.dataset.active = String(isActive);
        if (indicator) {
            indicator.textContent = isActive
                ? (issueSortState.direction === 'asc' ? '↑' : '↓')
                : '↕';
        }
    }
}

function renderIssueList() {
    updateIssueSortIndicators();
    issueTableBody.replaceChildren(...getSortedIssues().map(toIssueRow));
}

function setIssueSort(field) {
    if (issueSortState.field !== field) {
        issueSortState = {field, direction: 'asc'};
    } else if (issueSortState.direction === 'asc') {
        issueSortState = {field, direction: 'desc'};
    } else if (issueSortState.direction === 'desc') {
        issueSortState = {field: null, direction: null};
    } else {
        issueSortState = {field, direction: 'asc'};
    }
    renderIssueList();
}

const issueFilterConfig = {
    author: {
        label: 'Autor',
        optionLabel: value => value
    },
    worker: {
        label: 'Worker',
        optionLabel: value => value
    },
    status: {
        label: 'Status',
        optionLabel: value => statusLabel(value)
    },
    priority: {
        label: 'Priorität',
        optionLabel: value => priorityLabel(value)
    }
};

function getIssueFilterValues(issue, field) {
    switch (field) {
        case 'author':
            return [issue.author?.username ?? ''];
        case 'worker':
            return [...(issue.workers ?? [])]
                .map(worker => worker.username)
                .filter(value => value !== '');
        case 'status':
            return [String(issue.status ?? '')];
        case 'priority':
            return [String(issue.priority ?? '')];
        default:
            return [];
    }
}

function getIssueFilterOptions(field) {
    const seen = new Map();
    const selected = issueFilterState[field];
    for (const issue of issueListCache) {
        for (const value of getIssueFilterValues(issue, field)) {
            if (!seen.has(value)) {
                seen.set(value, value);
            }
        }
    }
    if (selected) {
        for (const value of selected) {
            if (!seen.has(value)) {
                seen.set(value, value);
            }
        }
    }
    const values = [...seen.values()];
    if (field === 'status' || field === 'priority') {
        return values.sort((left, right) => Number.parseInt(left, 10) - Number.parseInt(right, 10));
    }
    return values.sort((left, right) => left.localeCompare(right, 'de', {numeric: true, sensitivity: 'base'}));
}

function getIssueFilterSelection(field, options) {
    const selection = issueFilterState[field];
    if (!selection || selection.size === 0) {
        return new Set(options);
    }
    return new Set(selection);
}

function setIssueFilter(field, selectedValues) {
    const optionCount = getIssueFilterOptions(field).length;
    const normalized = selectedValues.size === 0 || selectedValues.size === optionCount
        ? null
        : new Set(selectedValues);
    issueFilterState = {...issueFilterState, [field]: normalized};
    renderIssueFilters();
    renderIssueList();
}

function getIssueFilterSummaryText(field, options) {
    const selection = issueFilterState[field];
    if (!selection || selection.size === 0 || selection.size === options.length) {
        return 'alle';
    }
    return `${selection.size} ausgewählt`;
}

function renderIssueFilterMenu(field) {
    const control = issueFilterControls.find(entry => entry.dataset.filterField === field);
    const menu = control?.querySelector('[data-filter-menu]');
    if (!menu) {
        return;
    }

    const config = issueFilterConfig[field];
    const options = getIssueFilterOptions(field);
    const selection = getIssueFilterSelection(field, options);
    const allSelected = selection.size === options.length;

    menu.replaceChildren();
    menu.dataset.optionSignature = options.join('\u0000');

    const allLabel = document.createElement('label');
    allLabel.className = 'user-checkbox filter-option filter-option-all';
    allLabel.dataset.filterAll = 'true';
    allLabel.hidden = !allSelected;

    const allInput = document.createElement('input');
    allInput.type = 'checkbox';
    allInput.checked = allSelected;
    allInput.disabled = !allSelected;
    allInput.tabIndex = -1;

    allLabel.append(allInput, document.createTextNode('alle'));
    menu.append(allLabel);

    for (const option of options) {
        const label = document.createElement('label');
        label.className = 'user-checkbox filter-option';

        const input = document.createElement('input');
        input.type = 'checkbox';
        input.value = option;
        input.checked = selection.has(option);

        label.append(input, document.createTextNode(config.optionLabel(option)));
        menu.append(label);
    }
}

function syncIssueFilterMenuState(field) {
    const control = issueFilterControls.find(entry => entry.dataset.filterField === field);
    const menu = control?.querySelector('[data-filter-menu]');
    if (!menu || menu.children.length === 0) {
        return;
    }

    const options = getIssueFilterOptions(field);
    const selection = getIssueFilterSelection(field, options);
    const allSelected = selection.size === options.length;
    const allLabel = menu.querySelector('[data-filter-all]');
    const allInput = allLabel?.querySelector('input');
    if (allLabel) {
        allLabel.hidden = !allSelected;
    }
    if (allInput) {
        allInput.checked = allSelected;
        allInput.disabled = !allSelected;
    }

    for (const label of menu.querySelectorAll('.filter-option:not([data-filter-all])')) {
        const input = label.querySelector('input[type="checkbox"]');
        if (!input) {
            continue;
        }
        input.checked = selection.has(input.value);
    }
}

function renderIssueFilters() {
    for (const control of issueFilterControls) {
        const field = control.dataset.filterField;
        const options = getIssueFilterOptions(field);
        const summary = control.querySelector('[data-filter-summary]');
        const button = control.querySelector('[data-filter-button]');
        const menu = control.querySelector('[data-filter-menu]');

        if (summary) {
            summary.textContent = getIssueFilterSummaryText(field, options);
        }
        if (button) {
            button.setAttribute('aria-expanded', String(openIssueFilterField === field));
        }
        if (menu) {
            menu.hidden = openIssueFilterField !== field;
        }
        if (!menu || menu.children.length === 0 || menu.dataset.optionSignature !== options.join('\u0000')) {
            renderIssueFilterMenu(field);
        } else {
            syncIssueFilterMenuState(field);
        }
    }
}

function openIssueFilter(field) {
    if (openIssueFilterField === field) {
        closeIssueFilter();
        return;
    }
    closeIssueFilter();
    openIssueFilterField = field;
    renderIssueFilters();
}

function closeIssueFilter(field = openIssueFilterField, {focusTrigger = false} = {}) {
    if (!field) {
        openIssueFilterField = null;
        renderIssueFilters();
        return;
    }
    const control = issueFilterControls.find(entry => entry.dataset.filterField === field);
    if (!control) {
        openIssueFilterField = null;
        return;
    }
    control.querySelector('[data-filter-button]')?.setAttribute('aria-expanded', 'false');
    control.querySelector('[data-filter-menu]')?.setAttribute('hidden', '');
    if (focusTrigger) {
        control.querySelector('[data-filter-button]')?.focus();
    }
    if (openIssueFilterField === field) {
        openIssueFilterField = null;
    }
    renderIssueFilters();
}

function toggleIssueFilter(field) {
    if (openIssueFilterField === field) {
        closeIssueFilter(field);
    } else {
        openIssueFilter(field);
    }
}

function isIssueFilterEventTarget(target) {
    return issueFilterControls.some(control => control.contains(target));
}

function handleIssueFilterMenuChange(event) {
    const menu = event.currentTarget;
    const control = menu.closest('[data-filter-field]');
    const field = control?.dataset.filterField;
    if (!field) {
        return;
    }

    const options = getIssueFilterOptions(field);
    const allCheckbox = menu.querySelector('[data-filter-all] input');
    const optionInputs = [...menu.querySelectorAll('input[type="checkbox"]')].filter(input => input !== allCheckbox);
    const selectedValues = new Set(optionInputs.filter(input => input.checked).map(input => input.value));

    if (event.target === allCheckbox) {
        selectedValues.clear();
        for (const option of options) {
            selectedValues.add(option);
        }
    }

    setIssueFilter(field, selectedValues);
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
        currentSession = session;
        sessionBox.textContent = `Eingeloggt als ${session.username}`;
        setUserMenu(session);
        setAuthenticatedView(true);
        setActiveView('overview');
        await loadUsers();
        await reloadIssues();
    } else {
        currentSession = null;
        sessionBox.textContent = 'Nicht eingeloggt';
        hideUserMenu();
        availableUsers = [];
        renderUserCheckboxes(issueWorkerUsers, availableUsers);
        setAuthenticatedView(false);
        setActiveView('overview');
    }
}

async function reloadIssues() {
    issueListCache = await request('/api/issues?sort=id&direction=desc');
    renderIssueFilters();
    renderIssueList();
}

function toIssueRow(issue) {
    const tr = document.createElement('tr');
    tr.classList.add('issue-row');
    tr.innerHTML = `
        <td>${issue.id}</td>
        <td>${userLabel(issue.author)}</td>
        <td>${formatUserList(issue.workers)}</td>
        <td>${escapeHtml(issue.title)}</td>
        <td>${statusLabel(issue.status)}</td>
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
    issueForm.elements.status.value = String(issue.status);
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

userMenuButton.addEventListener('click', () => {
    toggleUserMenu();
});

profileButton.addEventListener('click', () => {
    closeUserMenu({focusTrigger: true});
    openProfileView();
});

menuLogoutButton.addEventListener('click', async () => {
    try {
        closeUserMenu({focusTrigger: true});
        await request('/api/session', {method: 'DELETE'});
        await refreshSession();
        showMessage('Logout erfolgreich');
    } catch (e) {
        showMessage(e.message, true);
    }
});

userMenuWrapper.addEventListener('focusout', event => {
    if (!userMenuWrapper.contains(event.relatedTarget)) {
        closeUserMenu();
    }
});

openPasswordDialogButton.addEventListener('click', () => {
    currentPasswordInput.value = '';
    newPasswordInput.value = '';
    confirmNewPasswordInput.value = '';
    openDialog(passwordDialog);
    currentPasswordInput.focus();
});

profileCancelButton.addEventListener('click', () => {
    showOverviewView({focusTrigger: true});
});

profileForm.addEventListener('submit', async event => {
    event.preventDefault();
    try {
        await request('/api/session/profile', {
            method: 'PUT',
            body: JSON.stringify({
                username: profileUsernameInput.value,
                displayName: profileDisplayNameInput.value,
                office: profileOfficeInput.value
            })
        });
        await refreshSession();
        showOverviewView({focusTrigger: true});
        showMessage('Profil gespeichert');
    } catch (e) {
        showMessage(e.message, true);
    }
});

passwordForm.addEventListener('submit', async event => {
    event.preventDefault();
    if (newPasswordInput.value !== confirmNewPasswordInput.value) {
        showMessage('Neue Passwörter stimmen nicht überein', true);
        return;
    }
    try {
        await request('/api/session/password', {
            method: 'PUT',
            body: JSON.stringify({
                currentPassword: currentPasswordInput.value,
                newPassword: newPasswordInput.value
            })
        });
        closePasswordDialog();
        showMessage('Passwort geändert');
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
            priority: Number.parseInt(issueForm.elements.priority.value, 10),
            status: Number.parseInt(issueForm.elements.status.value, 10)
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
    showOverviewView();
});

openCreateIssueButton.addEventListener('click', () => {
    openCreateIssueDialog().catch(e => showMessage(e.message, true));
});

document.querySelector('#reloadIssuesButton').addEventListener('click', () => reloadIssues().catch(e => showMessage(e.message, true)));

for (const button of issueSortButtons) {
    button.addEventListener('click', () => {
        setIssueSort(button.dataset.sortField);
    });
}

for (const button of issueFilterButtons) {
    button.addEventListener('click', event => {
        event.stopPropagation();
        const field = button.closest('[data-filter-field]')?.dataset.filterField;
        if (field) {
            toggleIssueFilter(field);
        }
    });
}

for (const menu of issueFilterMenus) {
    menu.addEventListener('change', handleIssueFilterMenuChange);
}

document.addEventListener('pointerdown', event => {
    if (!userMenu.hidden && !userMenuWrapper.contains(event.target)) {
        closeUserMenu();
    }
    if (openIssueFilterField && !isIssueFilterEventTarget(event.target)) {
        closeIssueFilter();
    }
});

document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !userMenu.hidden) {
        closeUserMenu({focusTrigger: true});
        return;
    }
    if (event.key === 'Escape' && openIssueFilterField) {
        closeIssueFilter(openIssueFilterField, {focusTrigger: true});
    }
});

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
