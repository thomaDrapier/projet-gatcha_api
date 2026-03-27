const AUTH_API = "http://localhost:8081/auth";
const PLAYER_API = "http://localhost:8082/player";

// Redirection automatique si déjà connecté
if (localStorage.getItem('gatcha_token')) {
    window.location.href = 'dashboard.html';
}

const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const authMessage = document.getElementById('auth-message');

function showMessage(msg, isSuccess = false) {
    authMessage.innerText = msg;
    authMessage.className = isSuccess ? 'msg-box msg-success' : 'msg-box msg-error';
}

// Basculer entre les formulaires
document.getElementById('show-register').addEventListener('click', (e) => {
    e.preventDefault();
    loginForm.style.display = 'none';
    registerForm.style.display = 'block';
    showMessage('');
});

document.getElementById('show-login').addEventListener('click', (e) => {
    e.preventDefault();
    registerForm.style.display = 'none';
    loginForm.style.display = 'block';
    showMessage('');
});

// Utilitaire d'appel API
async function fetchApi(url, method, body = null) {
    const headers = { 'Accept': '*/*', 'Content-Type': 'application/json' };
    const options = { method, headers };
    if (body) options.body = JSON.stringify(body);
    
    try {
        const response = await fetch(url, options);
        const text = await response.text();
        try { return { status: response.status, data: JSON.parse(text) }; } 
        catch { return { status: response.status, data: text }; }
    } catch (error) {
        return { status: 500, data: "Erreur réseau." };
    }
}

// Inscription
registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('reg-username').value;
    const pwd1 = document.getElementById('reg-password').value;
    const pwd2 = document.getElementById('reg-confirm').value;

    if (pwd1 !== pwd2) return showMessage("Les mots de passe ne correspondent pas !");

    const res = await fetchApi(`${AUTH_API}/register`, 'POST', { username, password: pwd1 });
    
    if (res.status === 200) {
        // Initialisation silencieuse du joueur
        await fetchApi(`${PLAYER_API}/init/${username}`, 'POST');
        
        registerForm.reset();
        registerForm.style.display = 'none';
        loginForm.style.display = 'block';
        showMessage("Compte créé avec succès ! Veuillez vous connecter.", true);
    } else {
        showMessage("Erreur : " + (res.data.error || res.data));
    }
});

// Connexion
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('login-username').value;
    const pwd = document.getElementById('login-password').value;

    const res = await fetchApi(`${AUTH_API}/login`, 'POST', { username, password: pwd });

    if (res.status === 200) {
        localStorage.setItem('gatcha_token', res.data.token || res.data);
        localStorage.setItem('gatcha_username', username);
        // Redirection vers le tableau de bord
        window.location.href = 'dashboard.html';
    } else {
        showMessage("Identifiants incorrects.");
    }
});