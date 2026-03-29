// ==========================================
// CONFIGURATION DES APIS
// ==========================================
const PLAYER_API = "http://localhost:8082/player";
const INVOCATION_API = "http://localhost:8083/invocation"; 
const MONSTER_API = "http://localhost:8084/monsters"; 

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

// --- DICTIONNAIRE DES NOMS ET IMAGES (Via Template ID) ---
const monsterTemplates = {
    1: { name: "Slime Commun", img: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/132.png" },
    2: { name: "Loup-Garou Rare", img: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/262.png" },
    3: { name: "Chevalier Noir Épique", img: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/823.png" },
    4: { name: "Dragon Ancien Légendaire", img: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/other/official-artwork/384.png" },
    "default": { name: "Inconnu", img: "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/items/poke-ball.png" }
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
    charactersList.innerHTML = '<p style="color: var(--primary);">Synchronisation avec le serveur...</p>';
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
// MODALES PROFIL & BESTIAIRE
// ==========================================
window.openProfileModal = function(charName) {
    const data = loadedData[charName];
    if (!data) return;

    // --- Calculs de l'XP ---
    const level = data.level || 1;
    const currentXp = data.xp || 0;
    const xpRequired = level * 1000; 
    let xpPercentage = (currentXp / xpRequired) * 100;
    if(xpPercentage > 100) xpPercentage = 100;
    const xpRemaining = xpRequired - currentXp;

    // --- Formatage de la date ---
    // Si createdAt existe (ex: "2026-03-29T10:00:00"), on le formate en français
    const dateOptions = { year: 'numeric', month: 'long', day: 'numeric' };
    const creationDate = data.createdAt 
        ? new Date(data.createdAt).toLocaleDateString('fr-FR', dateOptions) 
        : "Inconnue (Ancien compte)";

    modalContent.innerHTML = `
        <h2 class="modal-title">Fiche de Personnage</h2>
        
        <div class="profile-grid">
            <div class="profile-info-block">
                <p>👤 Détenteur : <strong>${currentUser}</strong></p>
                <p>🎭 Nom du Héros : <strong>${data.username}</strong></p>
                <p>📅 Créé le : <strong>${creationDate}</strong></p>
            </div>

            <div class="profile-stats-block">
                <p>🐉 Monstres possédés : <strong>${(data.monsters || []).length}</strong></p>
                <p>⚔️ Combats effectués : <strong>${data.totalBattles || 0}</strong></p>
            </div>
        </div>

        <div class="xp-section" style="margin-top: 20px;">
            <div style="text-align: center; margin-bottom: 10px;">
                Niveau : <span style="font-size: 1.5rem; color: var(--primary); font-weight: bold;">${level}</span>
            </div>
            <div class="xp-text">
                <span>Progression XP</span>
                <span>${currentXp} / ${xpRequired} (${xpPercentage.toFixed(1)}%)</span>
            </div>
            <div class="xp-bar-bg">
                <div class="xp-bar-fill" style="width: 0%;"></div>
            </div>
            <p style="text-align: center; font-size: 0.85rem; color: #a5b1c2; margin-top: 10px;">
                Il manque <strong>${xpRemaining > 0 ? xpRemaining : 0} XP</strong> pour atteindre le niveau ${level + 1}
            </p>
        </div>

        <div style="margin-top: 30px; display: flex; gap: 10px;">
             <button class="action-btn glow" style="flex: 1;" onclick="alert('Bientôt disponible : Système de combat !')">⚔️ Lancer un Combat</button>
        </div>
    `;

    modalOverlay.classList.remove('hidden');
    
    // Animation de la barre d'XP
    setTimeout(() => { 
        const fillBar = document.querySelector('.xp-bar-fill'); 
        if(fillBar) fillBar.style.width = `${xpPercentage}%`; 
    }, 100);
};

window.openMonstersModal = async function(charName) {
    const data = loadedData[charName];
    const monsterIds = data.monsters || []; 
    
    modalContent.innerHTML = `<h2 class="modal-title">Bestiaire de <span>${charName}</span></h2>`;
    modalOverlay.classList.remove('hidden');

    // Correction : Message immédiat si aucun monstre
    if (monsterIds.length === 0) {
        modalContent.innerHTML += '<p class="empty-msg" style="margin-top:20px;">Vous n\'avez encore invoqué aucun monstre.</p>';
        return;
    }

    // Affichage d'un conteneur vide pour recevoir les monstres
    const inventoryDiv = document.createElement('div');
    inventoryDiv.className = 'monster-inventory';
    inventoryDiv.innerHTML = '<p class="empty-msg">Récupération de vos monstres...</p>';
    modalContent.appendChild(inventoryDiv);

    let gridItems = '';
    let foundCount = 0;

    for (const id of monsterIds) {
        try {
            const res = await fetch(`${MONSTER_API}/${id}`);
            if (res.ok) {
                const monster = await res.json();
                const template = monsterTemplates[monster.templateId] || monsterTemplates["default"];
                const element = (monster.element || "neutre").toLowerCase();

                let elEmoji = "⚪";
                if(['fire', 'feu'].includes(element)) elEmoji = "🔥";
                if(['water', 'eau'].includes(element)) elEmoji = "💧";
                if(['wind', 'vent'].includes(element)) elEmoji = "🌪️";

                gridItems += `
                    <div class="monster-item">
                        <button class="delete-monster-btn" onclick="deleteMonster('${charName}', '${id}')" title="Relâcher">✖</button>
                        <div class="monster-level-badge">Lv.${monster.level}</div>
                        <div class="monster-element-badge">${elEmoji}</div>
                        <img src="${template.img}" alt="${template.name}">
                        <span class="monster-name-mini">${template.name}</span>
                        <div class="monster-stats">
                            <div class="stat-hp">❤️ ${monster.hp} HP</div>
                            <div class="stat-atk">⚔️ ${monster.atk}</div>
                            <div class="stat-def">🛡️ ${monster.def}</div>
                            <div class="stat-vit">⚡ VIT: ${monster.vit}</div>
                        </div>
                    </div>
                `;
                foundCount++;
            }
        } catch (e) { console.warn("ID introuvable ou erreur serveur pour :", id); }
    }

    if (foundCount === 0) {
        inventoryDiv.innerHTML = '<p class="empty-msg">Vos monstres actuels sont incompatibles ou introuvables.</p>';
    } else {
        inventoryDiv.innerHTML = gridItems;
        const countInfo = document.createElement('div');
        countInfo.style = "text-align: center; margin-bottom: 20px; color: #a5b1c2;";
        countInfo.innerHTML = `Total : <strong>${foundCount}</strong> monstre(s)`;
        modalContent.insertBefore(countInfo, inventoryDiv);
    }
};

// ==========================================
// SUPPRESSION D'UN MONSTRE
// ==========================================
window.deleteMonster = async function(charName, monsterId) {
    if(!confirm(`Voulez-vous vraiment relâcher ce monstre ?`)) return; 
    
    // Mise à jour locale du tableau de données pour le personnage
    if (loadedData[charName]) {
        loadedData[charName].monsters = loadedData[charName].monsters.filter(id => id !== monsterId);
    }
    
    // Ici, tu pourras ajouter ton appel DELETE vers le Player Service pour supprimer l'ID de la liste MongoDB
    // await fetchApi(`${PLAYER_API}/${charName}/monsters/${monsterId}`, 'DELETE');

    openMonstersModal(charName);
};

// ==========================================
// MÉCANIQUE D'INVOCATION
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
        const tId = res.data.templateId;
        const instId = res.data.instanceId;
        const template = monsterTemplates[tId] || monsterTemplates["default"];

        monsterRevealArea.innerHTML = `
            <div class="monster-card glass">
                <div class="result-header">Nouveau Compagnon</div>
                <div class="monster-image-container"><img src="${template.img}" alt="${template.name}"></div>
                <h3 class="epic-text">${template.name}</h3>
                <p style="color:#a5b1c2; font-size:0.8rem; margin-top:5px;">Il a rejoint votre bestiaire !</p>
                <button id="close-summon-modal" class="action-btn glow" style="width: 100%; margin-top:15px;">Merveilleux !</button>
            </div>
        `;

        monsterRevealArea.classList.remove('hidden');
        summonModal.classList.remove('hidden');

        // Ajout immédiat à la mémoire locale pour que le Bestiaire soit à jour sans recharger
        if (loadedData[charName]) {
            if (!loadedData[charName].monsters) loadedData[charName].monsters = [];
            loadedData[charName].monsters.push(instId);
        }

        document.getElementById('close-summon-modal').addEventListener('click', closeRevealModal);
    } else {
        alert("L'invocation a échoué.");
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

// --- INITIALISATION ---
document.getElementById('btn-create-char')?.addEventListener('click', async () => {
    const charNameInput = document.getElementById('create-char-name');
    const charName = charNameInput.value.trim();
    if(!charName) return alert("Nom invalide !");
    
    const res = await fetchApi(`${PLAYER_API}/init/${charName}`, 'POST');
    if(res.status === 200) {
        myCharacters.push(charName);
        localStorage.setItem(storageKey, JSON.stringify(myCharacters));
        charNameInput.value = ''; 
        renderCharacters(); 
    } else alert("Erreur.");
});

document.getElementById('btn-logout').addEventListener('click', () => {
    localStorage.removeItem('gatcha_token');
    localStorage.removeItem('gatcha_username');
    window.location.href = 'index.html';
});

renderCharacters();