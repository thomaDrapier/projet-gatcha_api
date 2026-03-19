// --- CONFIGURATION DES URLS ---
const AUTH_API = "http://localhost:8081/auth";
const PLAYER_API = "http://localhost:8082/player";

// --- ETAT GLOBAL ---
let currentUser = localStorage.getItem('gatcha_username');
let currentToken = localStorage.getItem('gatcha_token');

// --- ELEMENTS DU DOM ---
const authView = document.getElementById('auth-view');
const playerView = document.getElementById('player-view');
const loginForm = document.getElementById('login-form');
const registerForm = document.getElementById('register-form');
const authError = document.getElementById('auth-error');
const displayUsername = document.getElementById('display-username');
const apiResponse = document.getElementById('api-response');
const profileDisplay = document.getElementById('profile-display');

// --- INITIALISATION AU CHARGEMENT ---
window.onload = () => {
    if (currentToken && currentUser) {
        showPlayerView();
    }
};

// --- NAVIGATION ENTRE FORMULAIRES ---
document.getElementById('show-register').addEventListener('click', (e) => {
    e.preventDefault();
    loginForm.style.display = 'none';
    registerForm.style.display = 'block';
    authError.innerText = '';
});

document.getElementById('show-login').addEventListener('click', (e) => {
    e.preventDefault();
    registerForm.style.display = 'none';
    loginForm.style.display = 'block';
    authError.innerText = '';
});

// --- GESTION DE L'AFFICHAGE DES VUES ---
function showPlayerView() {
    authView.classList.remove('active');
    playerView.classList.add('active');
    displayUsername.innerText = currentUser;
}

function logout() {
    localStorage.removeItem('gatcha_token');
    localStorage.removeItem('gatcha_username');
    currentToken = null;
    currentUser = null;
    playerView.classList.remove('active');
    authView.classList.add('active');
    apiResponse.innerText = "Aucune requête envoyée...";
    profileDisplay.innerHTML = `<p><em>Cliquez sur "Récupérer le profil complet" pour afficher les données.</em></p>`;
}
document.getElementById('btn-logout').addEventListener('click', logout);

// --- FONCTION UTILITAIRE POUR LES APPELS API ---
// Gère automatiquement l'injection du token Bearer
async function fetchApi(url, method, body = null) {
    const headers = {
        'Accept': '*/*',
        'Content-Type': 'application/json'
    };
    
    if (currentToken) {
        headers['Authorization'] = `Bearer ${currentToken}`;
    }

    const options = { method, headers };
    if (body) options.body = JSON.stringify(body);

    try {
        const response = await fetch(url, options);
        // Si erreur 401, on déconnecte l'utilisateur
        if (response.status === 401) {
            alert("Session expirée, veuillez vous reconnecter.");
            logout();
            return null;
        }
        
        // On essaie de parser en JSON, sinon on renvoie du texte brut
        const text = await response.text();
        try {
            return { status: response.status, data: JSON.parse(text) };
        } catch {
            return { status: response.status, data: text };
        }
    } catch (error) {
        return { status: 500, data: "Erreur réseau (Vérifiez les CORS dans Spring Boot !)" };
    }
}

// --- LOGIQUE D'INSCRIPTION ---
registerForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('reg-username').value;
    const pwd1 = document.getElementById('reg-password').value;
    const pwd2 = document.getElementById('reg-confirm').value;

    if (pwd1 !== pwd2) {
        authError.innerText = "Les mots de passe ne correspondent pas !";
        return;
    }

    // ✅ La nouvelle méthode qui envoie un objet JSON dans le Body
    const userData = {
    username: username,
    password: pwd1
    };
const res = await fetchApi(`${AUTH_API}/register`, 'POST', userData);
    
    if (res.status === 200) {
        // Enregistrement réussi, on récupère le token et on initie le joueur
        currentToken = res.data.token_clear || res.data.token; // Adapte selon ce que ton API renvoie
        currentUser = username;
        localStorage.setItem('gatcha_token', currentToken);
        localStorage.setItem('gatcha_username', currentUser);
        
        showPlayerView();
        
        // AUTO-INIT du profil joueur dans le Player Service
        await fetchApi(`${PLAYER_API}/init/${currentUser}`, 'POST');
        alert("Compte créé et profil joueur initialisé avec succès !");
    } else {
        authError.innerText = "Erreur : " + (res.data.error || res.data);
    }
});

// --- LOGIQUE DE CONNEXION ---
loginForm.addEventListener('submit', async (e) => {
    e.preventDefault();
    const username = document.getElementById('login-username').value;
    const pwd = document.getElementById('login-password').value;

    const res = await fetchApi(`${AUTH_API}/login?username=${username}&password=${pwd}`, 'POST');

    if (res.status === 200) {
        currentToken = res.data; // Le backend renvoie le token en Raw String
        currentUser = username;
        localStorage.setItem('gatcha_token', currentToken);
        localStorage.setItem('gatcha_username', currentUser);
        showPlayerView();
    } else {
        authError.innerText = "Identifiants incorrects.";
    }
});

// --- ACTIONS DU TABLEAU DE BORD (PLAYER SERVICE) ---

function printToConsole(data) {
    apiResponse.innerText = JSON.stringify(data, null, 2);
}

// Initialiser le joueur manuellement
document.getElementById('btn-init-player').addEventListener('click', async () => {
    const res = await fetchApi(`${PLAYER_API}/init/${currentUser}`, 'POST');
    printToConsole(res.data);
    if(res.status === 200) alert("Profil initialisé !");
});

// Récupérer le Profil complet et mettre à jour l'UI
document.getElementById('btn-get-profile').addEventListener('click', async () => {
    const res = await fetchApi(`${PLAYER_API}/${currentUser}`, 'GET');
    printToConsole(res.data);
    
    if (res.status === 200) {
        const p = res.data;
        // On met à jour la belle carte d'affichage
        profileDisplay.innerHTML = `
            <h4>🪪 Profil de ${p.username || currentUser}</h4>
            <div class="stat"><span>Niveau :</span> <strong>${p.level || 1}</strong></div>
            <div class="stat"><span>Expérience (XP) :</span> <strong>${p.xp || 0}</strong></div>
            <div class="stat"><span>Monstres possédés :</span> <strong>${p.monsters ? p.monsters.length : 0}</strong></div>
        `;
    }
});

// Récupérer juste le niveau
document.getElementById('btn-get-level').addEventListener('click', async () => {
    const res = await fetchApi(`${PLAYER_API}/${currentUser}/level`, 'GET');
    printToConsole(res.data);
});

// Récupérer juste les monstres
document.getElementById('btn-get-monsters').addEventListener('click', async () => {
    const res = await fetchApi(`${PLAYER_API}/${currentUser}/monsters`, 'GET');
    printToConsole(res.data);
});

// Action factice pour la suppression (car non codée en back)
document.getElementById('btn-delete-player').addEventListener('click', () => {
    alert("Attention : La route DELETE n'est pas encore créée dans ton PlayerController !");
});