// ==========================================
// CONFIGURATION DES APIS
// ==========================================
const PLAYER_API = "http://localhost:8082/player";
const INVOCATION_API = "http://localhost:8083/invocation"; 

const currentToken = localStorage.getItem('gatcha_token');
const currentUser = localStorage.getItem('gatcha_username');

if (!currentToken) window.location.href = 'index.html';

document.getElementById('display-username').innerText = currentUser;
const charactersList = document.getElementById('characters-list');

const storageKey = `gatcha_chars_${currentUser}`;
let myCharacters = JSON.parse(localStorage.getItem(storageKey)) || [];
let loadedData = {}; 

const modalOverlay = document.getElementById('custom-modal');
const modalContent = document.getElementById('modal-content');
const summonModal = document.getElementById('summon-modal');
const monsterRevealArea = document.getElementById('monster-reveal-area');

document.getElementById('close-modal').addEventListener('click', () => modalOverlay.classList.add('hidden'));

// --- DICTIONNAIRE DES IMAGES ---
const monsterImages = {
    "Slime_Commun": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/132.png",
    "Loup_Garou_Rare": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/262.png",
    "Chevalier_Noir_Epique": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/823.png",
    "Dragon_Ancien_Légendaire": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/384.png",
    "Inconnu": "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png"
};

// ==========================================
// UTILITAIRE API
// ==========================================
async function fetchApi(url, method, body = null) {
    const headers = { 'Accept': '*/*', 'Content-Type': 'application/json', 'Authorization': `Bearer ${currentToken}` };
    const options = { method, headers };
    if (body) options.body = JSON.stringify(body);
    
    try {
        const response = await fetch(url, options);
        if (response.status === 401) { alert("Session expirée."); document.getElementById('btn-logout').click(); return null; }
        const text = await response.text();
        try { return { status: response.status, data: JSON.parse(text) }; } 
        catch { return { status: response.status, data: text }; }
    } catch (error) { return { status: 500, data: "Erreur réseau." }; }
}

// ==========================================
// AFFICHAGE DES CARTES DES PERSONNAGES
// ==========================================
async function renderCharacters() {
    charactersList.innerHTML = '<p style="color: var(--primary);">Invocation des données en cours...</p>';
    if (myCharacters.length === 0) { 
        charactersList.innerHTML = '<p class="empty-msg">Aucun héros dans votre équipe. Créez-en un !</p>'; 
        return; 
    }
    
    charactersList.innerHTML = ''; 

    for (let i = 0; i < myCharacters.length; i++) {
        const charName = myCharacters[i];
        const res = await fetchApi(`${PLAYER_API}/${charName}`, 'GET');
        
        if (res && res.status === 200) loadedData[charName] = res.data;
        else loadedData[charName] = { username: charName, level: 1, xp: 0, monsters: [] };

        const data = loadedData[charName];
        const safeId = `btn-summon-${i}`;

        const card = document.createElement('div');
        card.className = 'char-card';
        card.innerHTML = `
            <h4>${data.username}</h4>
            <div class="char-level">Niveau ${data.level || 1}</div>
            <div class="card-actions">
                <button class="card-btn" onclick="openProfileModal('${charName}')">📜 Profil Complet</button>
                <button class="card-btn" onclick="openMonstersModal('${charName}')">🐉 Bestiaire</button>
                <button id="${safeId}" class="summon-btn cooldown" onclick="executeSummon('${charName}', '${safeId}')">⏳ Calcul...</button>
            </div>
        `;
        charactersList.appendChild(card);
    }
    updateDashboardTimers();
}

// ==========================================
// MODALES PROFIL & BESTIAIRE (VRAIES STATS)
// ==========================================
window.openProfileModal = function(charName) {
    const data = loadedData[charName];
    const level = data.level || 1;
    const currentXp = data.xp || 0;
    const xpRequiredForNextLevel = level * 1000; 
    let xpPercentage = (currentXp / xpRequiredForNextLevel) * 100;
    if(xpPercentage > 100) xpPercentage = 100;
    const xpRemaining = xpRequiredForNextLevel - currentXp;

    modalContent.innerHTML = `
        <h2 class="modal-title">Profil de <span>${data.username}</span></h2>
        <div style="text-align: center; font-size: 1.2rem; margin-bottom: 10px;">Niveau Actuel : <strong>${level}</strong></div>
        <div class="xp-section">
            <div class="xp-text"><span>XP : <strong>${currentXp}</strong> / ${xpRequiredForNextLevel}</span><span>${xpPercentage.toFixed(1)}%</span></div>
            <div class="xp-bar-bg"><div class="xp-bar-fill" style="width: 0%;"></div></div>
            <div style="text-align: center; font-size: 0.8rem; margin-top: 10px; color: #a5b1c2;">Encore ${xpRemaining > 0 ? xpRemaining : 0} XP avant le niveau ${level + 1} !</div>
        </div>
    `;
    modalOverlay.classList.remove('hidden');
    setTimeout(() => { const fillBar = document.querySelector('.xp-bar-fill'); if(fillBar) fillBar.style.width = `${xpPercentage}%`; }, 100);
};

window.openMonstersModal = function(charName) {
    const data = loadedData[charName];
    const monsters = data.monsters || [];
    
    let monstersHTML = '';

    if (monsters.length === 0) {
        monstersHTML = '<p class="empty-msg">Aucun monstre capturé pour le moment.</p>';
    } else {
        const gridItems = monsters.map((monsterObj, index) => {
            
            // Sécurité : Si l'API renvoie encore un simple texte au lieu d'un objet
            const isObject = typeof monsterObj === 'object';
            const monsterName = isObject ? (monsterObj.name || monsterObj.monsterId || "Inconnu") : monsterObj;
            const cleanName = monsterName.replace(/_/g, ' '); 
            
            const imageUrl = monsterImages[monsterName] || monsterImages["Inconnu"];

            // Récupération des VRAIES statistiques (ou "?" si l'API n'est pas encore prête)
            const lvl = isObject && monsterObj.level ? monsterObj.level : 1;
            const hp = isObject && monsterObj.hp ? monsterObj.hp : "?";
            const atk = isObject && monsterObj.atk ? monsterObj.atk : "?";
            const def = isObject && monsterObj.def ? monsterObj.def : "?";
            const vit = isObject && monsterObj.vit ? monsterObj.vit : "?";
            const element = isObject && monsterObj.typeElementaire ? monsterObj.typeElementaire.toLowerCase() : "neutre";

            // Attribution de l'emoji d'élément
            let elEmoji = "⚪";
            if(element === 'feu') elEmoji = "🔥";
            if(element === 'eau') elEmoji = "💧";
            if(element === 'vent') elEmoji = "🌪️";
            
            return `
                <div class="monster-item">
                    <button class="delete-monster-btn" onclick="deleteMonster('${charName}', ${index})" title="Relâcher ce monstre">✖</button>
                    <div class="monster-level-badge">Lv.${lvl}</div>
                    <div class="monster-element-badge" title="Type: ${element}">${elEmoji}</div>
                    
                    <img src="${imageUrl}" alt="${cleanName}">
                    <span class="monster-name-mini">${cleanName}</span>
                    
                    <div class="monster-stats">
                        <div class="stat-hp">❤️ ${hp} HP</div>
                        <div class="stat-atk">⚔️ ${atk}</div>
                        <div class="stat-def">🛡️ ${def}</div>
                        <div class="stat-vit">⚡ VIT: ${vit}</div>
                    </div>
                </div>
            `;
        }).join('');

        monstersHTML = `<div class="monster-inventory">${gridItems}</div>`;
    }
    
    modalContent.innerHTML = `
        <h2 class="modal-title">Bestiaire de <span>${data.username}</span></h2>
        <div style="text-align: center; margin-bottom: 20px; color: #a5b1c2;">
            Total : <strong>${monsters.length}</strong> monstre(s)
        </div>
        ${monstersHTML}
    `;
    
    modalOverlay.classList.remove('hidden');
};

// ==========================================
// SUPPRESSION D'UN MONSTRE
// ==========================================
window.deleteMonster = async function(charName, monsterIndex) {
    const data = loadedData[charName];
    const monsterObj = data.monsters[monsterIndex];
    
    const isObject = typeof monsterObj === 'object';
    const monsterName = isObject ? (monsterObj.name || monsterObj.monsterId || "Ce monstre") : monsterObj;
    const cleanName = monsterName.replace(/_/g, ' ');

    if(!confirm(`Voulez-vous vraiment relâcher "${cleanName}" dans la nature ?\nCette action est irréversible !`)) {
        return; 
    }

    // 1. Mise à jour de la vue locale Front-end
    data.monsters.splice(monsterIndex, 1);
    
    // 🚧 NOTE POUR TON BACKEND : 
    // Quand ton API Java sera prête à gérer la suppression, décommente le fetch ci-dessous 
    // et adapte l'URL vers ton MonsterController !
    // await fetchApi(`http://localhost:8084/monsters/${charName}/${monsterName}`, 'DELETE');

    // 2. Rafraîchissement visuel de la modale
    openMonstersModal(charName);
};

// ==========================================
// MÉCANIQUE D'INVOCATION (GACHA PULL)
// ==========================================
const COOLDOWN_MS = 10000;
let currentSummonChar = null;

setInterval(updateDashboardTimers, 1000);

function updateDashboardTimers() {
    for (let i = 0; i < myCharacters.length; i++) {
        const charName = myCharacters[i];
        const btn = document.getElementById(`btn-summon-${i}`);
        if (!btn) continue;

        const lastSummonStr = localStorage.getItem(`gatcha_timer_${charName}`);
        const lastSummonTime = lastSummonStr ? parseInt(lastSummonStr) : 0;
        const timePassed = Date.now() - lastSummonTime;

        if (timePassed >= COOLDOWN_MS) {
            btn.className = "summon-btn ready";
            btn.innerHTML = "🔮 INVOQUER !";
            btn.disabled = false;
        } else {
            const timeLeftSec = Math.ceil((COOLDOWN_MS - timePassed) / 1000);
            btn.className = "summon-btn cooldown";
            btn.innerHTML = `⏳ Recharge : ${timeLeftSec}s`;
            btn.disabled = true;
        }
    }
}

window.executeSummon = async function(charName, btnId) {
    currentSummonChar = charName;
    const btn = document.getElementById(btnId);
    if(btn) { btn.disabled = true; btn.innerHTML = "⌛ Invocation..."; }

    const res = await fetchApi(`${INVOCATION_API}/${charName}`, 'POST');

    const flash = document.createElement('div');
    flash.className = 'flash-overlay flash-anim';
    document.body.appendChild(flash);
    setTimeout(() => flash.remove(), 1500);

    if (res && res.status === 200) {
        // Gère à la fois le retour d'un objet complet ou d'une string
        let monsterName = "Monstre Inconnu";
        let monsterObjToStore = res.data;

        if (typeof res.data === 'string') {
            monsterName = res.data;
        } else if (res.data.monsterId || res.data.name) {
            monsterName = res.data.name || res.data.monsterId;
        }

        const cleanMonsterName = monsterName.replace(/_/g, ' ');
        const monsterImageUrl = monsterImages[monsterName] || monsterImages["Inconnu"];

        monsterRevealArea.innerHTML = `
            <div class="monster-card glass">
                <div class="result-header">Nouveau Compagnon</div>
                <div class="monster-image-container"><img src="${monsterImageUrl}" alt="${cleanMonsterName}"></div>
                <h3 class="epic-text">${cleanMonsterName}</h3>
                <button id="close-summon-modal" class="action-btn glow" style="width: 100%;">Continuer</button>
            </div>
        `;

        monsterRevealArea.classList.remove('hidden');
        summonModal.classList.remove('hidden');

        // Ajout du monstre invoqué dans la liste (Front-end)
        if (loadedData[charName]) {
            if (!loadedData[charName].monsters) loadedData[charName].monsters = [];
            loadedData[charName].monsters.push(monsterObjToStore);
        }

        document.getElementById('close-summon-modal').addEventListener('click', closeRevealModal);
    } else {
        alert("L'invocation a échoué : " + (res && res.data ? JSON.stringify(res.data) : "Erreur"));
        if(btn) { btn.disabled = false; btn.innerHTML = "🔮 INVOQUER !"; }
    }
};

function closeRevealModal() {
    summonModal.classList.add('hidden');
    monsterRevealArea.classList.add('hidden');
    if (currentSummonChar) {
        localStorage.setItem(`gatcha_timer_${currentSummonChar}`, Date.now().toString());
        currentSummonChar = null;
        updateDashboardTimers();
    }
}

// ==========================================
// CRÉATION, SUPPRESSION ET DÉCONNEXION
// ==========================================
document.getElementById('btn-create-char').addEventListener('click', async () => {
    const charNameInput = document.getElementById('create-char-name');
    const charName = charNameInput.value.trim();
    if(!charName) return alert("Veuillez entrer un nom !");
    if(myCharacters.includes(charName)) return alert("Ce héros est déjà dans votre équipe !");

    const res = await fetchApi(`${PLAYER_API}/init/${charName}`, 'POST');
    if(res.status === 200) {
        myCharacters.push(charName);
        localStorage.setItem(storageKey, JSON.stringify(myCharacters));
        charNameInput.value = ''; 
        renderCharacters(); 
    } else alert("Erreur de création : " + JSON.stringify(res.data));
});

document.getElementById('btn-delete-char').addEventListener('click', () => {
    const charNameInput = document.getElementById('delete-char-name');
    const charName = charNameInput.value.trim();
    if(!charName) return alert("Veuillez entrer le nom à sacrifier !");
    if(myCharacters.includes(charName)) {
        myCharacters = myCharacters.filter(name => name !== charName);
        localStorage.setItem(storageKey, JSON.stringify(myCharacters));
        delete loadedData[charName]; 
        charNameInput.value = '';
        renderCharacters();
    } else alert("Ce héros n'est pas dans votre équipe !");
});

document.getElementById('btn-logout').addEventListener('click', () => {
    localStorage.removeItem('gatcha_token');
    localStorage.removeItem('gatcha_username');
    window.location.href = 'index.html';
});

// ==========================================
// DÉMARRAGE DE L'APPLICATION
// ==========================================
renderCharacters();