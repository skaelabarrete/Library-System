const API_BASE = 'http://localhost:8080';
let currentUser = null;
let currentStudentId = null;
let books = [];

function saveUser(role, id) {
    localStorage.setItem('lmsUserRole', role);
    localStorage.setItem('lmsStudentId', id.toString());
}

function clearUser() {
    localStorage.removeItem('lmsUserRole');
    localStorage.removeItem('lmsStudentId');
}

function getUser() {
    const role = localStorage.getItem('lmsUserRole');
    const studentId = localStorage.getItem('lmsStudentId');
    if (!role || !studentId) return null;
    return { role, studentId: parseInt(studentId, 10) };
}

function initLoginPage() {
    const user = getUser();
    if (user) {
        window.location.href = '/dashboard.html';
    }
}

function initDashboardPage() {
    const user = getUser();
    if (!user) {
        window.location.href = '/login.html';
        return;
    }

    currentUser = user.role;
    currentStudentId = user.studentId;
    document.getElementById('dashboard-title').innerText = currentUser === 'admin' ? 'Admin Dashboard' : 'Student Dashboard';
    document.getElementById('nav-user').textContent = `${currentUser.toUpperCase()} (${currentStudentId})`;

    displayBooks();
}

async function login(role) {
    const idPrompt = role === 'student' ? 'Enter student ID' : 'Enter admin ID (any number)';
    const id = parseInt(prompt(idPrompt), 10);
    if (!id || Number.isNaN(id)) {
        document.getElementById('login-message').textContent = 'Invalid ID.';
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/users/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ role, studentId: id })
        });
        const data = await res.json();

        if (!res.ok || !data.success) {
            document.getElementById('login-message').textContent = data.message || 'Login failed.';
            return;
        }

        saveUser(role, id);
        window.location.href = '/dashboard.html';
    } catch (err) {
        document.getElementById('login-message').textContent = 'Backend error: ' + err.message;
    }
}

function logout() {
    clearUser();
    window.location.href = '/login.html';
}

function logout() {
    currentUser = null;
    currentStudentId = null;
    updateNavbar();
    document.getElementById('dashboard').classList.add('hidden');
    document.getElementById('login-screen').classList.remove('hidden');
}

async function fetchBooks() {
    try {
        const res = await fetch(`${API_BASE}/books`, { method: 'GET' });
        if (!res.ok) throw new Error('Failed to fetch books');
        books = await res.json();
        return books;
    } catch (err) {
        console.error('GET /books failed:', err);
        alert('Could not fetch books from backend. Make sure server is running.');
        return [];
    }
}

async function displayBooks() {
    const tbody = document.getElementById('book-list');
    tbody.innerHTML = '';

    const data = await fetchBooks();
    if (!data || data.length === 0) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align:center; padding:15px;">No books available.</td></tr>';
        return;
    }

    books = data;

    books.forEach((book) => {
        const row = document.createElement('tr');
        const statusLabel = book.available
            ? '<span class="badge available">Available</span>'
            : '<span class="badge unavailable">Unavailable</span>';

        const userCanIssue = currentUser === 'admin' && book.available;
        const userCanReturn = (currentUser === 'admin' || currentUser === 'student') && !book.available;

        row.innerHTML = `
            <td>${book.id}</td>
            <td>${book.title}</td>
            <td>${book.author}</td>
            <td>${book.category}</td>
            <td>${book.isbn}</td>
            <td>${statusLabel}</td>
            <td>${book.dueDate || '-'}</td>
            <td>
                <button class="btn small" ${!userCanIssue ? 'disabled' : ''} onclick="issueBook(${book.id})">Issue</button>
                <button class="btn small" ${!userCanReturn ? 'disabled' : ''} onclick="returnBook(${book.id})">Return</button>
            </td>
        `;

        tbody.appendChild(row);
    });
}

async function issueBook(bookId) {
    if (currentUser !== 'admin') {
        alert('Only admin can issue books.');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/issue`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ studentId: currentStudentId, bookId })
        });
        const data = await res.json();

        if (!res.ok || !data.success) {
            alert(data.message || 'Issue failed.');
            return;
        }

        alert(data.message || 'Book issued successfully');
        await displayBooks();
    } catch (err) {
        alert('Issue request failed: ' + err.message);
    }
}

async function returnBook(bookId) {
    if (!currentUser || (currentUser !== 'admin' && currentUser !== 'student')) {
        alert('Please login first.');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/return`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ studentId: currentStudentId, bookId })
        });
        const data = await res.json();

        if (!res.ok || !data.success) {
            alert(data.message || 'Return failed.');
            return;
        }

        alert(data.message || 'Book returned successfully');
        await displayBooks();
    } catch (err) {
        alert('Return request failed: ' + err.message);
    }
}

function toggleAdminPanel() {
    const panel = document.getElementById('admin-panel');
    panel.classList.toggle('hidden');
}

function addDummyBook() {
    alert('Add book from Admin service is not implemented in frontend yet (Call POST /admin/addBook needed).');
}

updateNavbar();
