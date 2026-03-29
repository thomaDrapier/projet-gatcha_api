// ==========================================
// CONFIGURATION DES APIS
// ==========================================
const PLAYER_API = "http://localhost:8082/player";
const INVOCATION_API = "http://localhost:8083/invocation"; 
const MONSTER_API = "http://localhost:8084/monsters"; 
const COMBAT_API = "http://localhost:8085/battle";

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
// GESTION DES PERSONNAGES (L'existant conservé)
// ==========================================
async function renderCharacters() {
    charactersList.innerHTML = '<p style="color: var(--primary);">Synchronisation...</p>';
    if (myCharacters.length === 0) { charactersList.innerHTML = '<p class="empty-msg">Aucun héros.</p>'; return; }
    
    charactersList.innerHTML = ''; 
    for (let i = 0; i < myCharacters.length; i++) {
        const charName = myCharacters[i];
        const res = await fetchApi(`${PLAYER_API}/${charName}`, 'GET');
        loadedData[charName] = (res && res.status === 200) ? res.data : { username: charName, level: 1, xp: 0, monsters: [] };

        const data = loadedData[charName];
        const safeId = `btn-summon-${i}`;
        const card = document.createElement('div');
        card.className = 'char-card';
        card.innerHTML = `
            <h4>${data.username}</h4>
            <div class="char-level">Niveau ${data.level || 1}</div>
            <div class="card-actions">
                <button class="card-btn" onclick="openProfileModal('${charName}')">📜 Profil</button>
                <button class="card-btn" onclick="openMonstersModal('${charName}')">🐉 Bestiaire</button>
                <button id="${safeId}" class="summon-btn cooldown" onclick="executeSummon('${charName}', '${safeId}')">⏳ Calcul...</button>
            </div>
        `;
        charactersList.appendChild(card);
    }
    updateDashboardTimers();
}

// Modales Profil & Bestiaire
window.openProfileModal = function(charName) {
    const data = loadedData[charName];
    if (!data) return;
    const level = data.level || 1;
    const currentXp = data.xp || 0;
    const xpRequired = level * 1000;
    let xpPercentage = (currentXp / xpRequired) * 100;
    const creationDate = data.createdAt ? new Date(data.createdAt).toLocaleDateString('fr-FR') : "Inconnue";

    modalContent.innerHTML = `
        <h2 class="modal-title">Héros : <span>${data.username}</span></h2>
        <div class="profile-grid">
            <p>⚔️ Combats : <strong>${data.totalBattles || 0}</strong></p>
            <p>📅 Création : <strong>${creationDate}</strong></p>
        </div>
        <div class="xp-section" style="margin-top:20px;">
            <div class="xp-text"><span>Progression Niveau ${level}</span><span>${currentXp}/${xpRequired}</span></div>
            <div class="xp-bar-bg"><div class="xp-bar-fill" style="width: ${xpPercentage}%"></div></div>
        </div>
    `;
    modalOverlay.classList.remove('hidden');
};

window.openMonstersModal = async function(charName) {
    const data = loadedData[charName];
    const monsterIds = data.monsters || []; 
    modalContent.innerHTML = `<h2 class="modal-title">Inventaire de <span>${charName}</span></h2>`;
    modalOverlay.classList.remove('hidden');
    const inv = document.createElement('div');
    inv.className = 'monster-inventory';
    modalContent.appendChild(inv);

    for (const id of monsterIds) {
        const res = await fetch(`${MONSTER_API}/${id}`);
        if (res.ok) {
            const m = await res.json();
            const t = monsterTemplates[m.templateId] || monsterTemplates.default;
            inv.innerHTML += `<div class="monster-item"><img src="${t.img}"><span class="monster-name-mini">${t.name}</span></div>`;
        }
    }
};

// ==========================================
// INVOCATION ET TIMERS
// ==========================================
const COOLDOWN_MS = 10000;
function updateDashboardTimers() {
    myCharacters.forEach((name, i) => {
        const btn = document.getElementById(`btn-summon-${i}`);
        if (!btn) return;
        const last = parseInt(localStorage.getItem(`gatcha_timer_${name}`)) || 0;
        const diff = Date.now() - last;
        if (diff >= COOLDOWN_MS) {
            btn.className = "summon-btn ready"; btn.innerHTML = "🔮 INVOQUER !"; btn.disabled = false;
        } else {
            btn.className = "summon-btn cooldown"; btn.innerHTML = `⏳ ${Math.ceil((COOLDOWN_MS - diff)/1000)}s`; btn.disabled = true;
        }
    });
}
setInterval(updateDashboardTimers, 1000);

window.executeSummon = async function(name, id) {
    const res = await fetchApi(`${INVOCATION_API}/${name}`, 'POST');
    if (res && res.status === 200) {
        const t = monsterTemplates[res.data.templateId] || monsterTemplates.default;
        monsterRevealArea.innerHTML = `<div class="monster-card glass"><h3>${t.name}</h3><img src="${t.img}" width="150"><button onclick="closeRevealModal('${name}')" class="action-btn">Génial !</button></div>`;
        monsterRevealArea.classList.remove('hidden'); summonModal.classList.remove('hidden');
        if (loadedData[name]) loadedData[name].monsters.push(res.data.instanceId);
        fillArenaDropdowns(); // Mise à jour Arène
    }
};

window.closeRevealModal = function(name) {
    summonModal.classList.add('hidden'); monsterRevealArea.classList.add('hidden');
    localStorage.setItem(`gatcha_timer_${name}`, Date.now().toString());
};

// ==========================================
// NOUVEAU : SYSTÈME DE COMBAT
// ==========================================
async function fillArenaDropdowns() {
    try {
        const res = await fetch(MONSTER_API);
        const monsters = await res.json();
        const s1 = document.getElementById('arena-m1');
        const s2 = document.getElementById('arena-m2');
        s1.innerHTML = ''; s2.innerHTML = '';
        monsters.forEach(m => {
            const t = monsterTemplates[m.templateId] || monsterTemplates.default;
            const opt = `<option value="${m.id}">[Lv.${m.level}] ${t.name} (HP:${m.hp})</option>`;
            s1.innerHTML += opt; s2.innerHTML += opt;
        });
    } catch (e) { console.error("Erreur chargement arène"); }
}

window.startBattle = async function() {
    const m1Id = document.getElementById('arena-m1').value;
    const m2Id = document.getElementById('arena-m2').value;
    if (m1Id === m2Id) return alert("Sélectionnez deux monstres différents !");

    const btn = document.getElementById('btn-fight');
    btn.disabled = true; btn.innerText = "Calcul...";

    try {
        const res = await fetch(`${COMBAT_API}/start?monster1Id=${m1Id}&monster2Id=${m2Id}`, { method: 'POST' });
        const battle = await res.json();
        const m1 = await (await fetch(`${MONSTER_API}/${m1Id}`)).json();
        const m2 = await (await fetch(`${MONSTER_API}/${m2Id}`)).json();
        
        document.getElementById('battle-display').classList.remove('hidden');
        animateBattle(battle, m1, m2);
    } catch (e) { alert("Service de combat indisponible."); btn.disabled = false; btn.innerText = "LANCER"; }
};

async function animateBattle(battle, m1, m2) {
    const logs = document.getElementById('battle-logs');
    logs.innerHTML = "";
    document.getElementById('f1-name').innerText = (monsterTemplates[m1.templateId] || monsterTemplates.default).name;
    document.getElementById('f2-name').innerText = (monsterTemplates[m2.templateId] || monsterTemplates.default).name;

    for (const step of battle.replayLogs) {
        await new Promise(r => setTimeout(r, 1000));
        logs.innerHTML += `<div style="margin-bottom:5px;">> <span style="color:var(--primary)">Tour ${step.turn}</span>: ${step.description}</div>`;
        logs.scrollTop = logs.scrollHeight;

        if (step.attackerName === m1.id) {
            const pct = (step.targetRemainingHp / m2.hp) * 100;
            document.getElementById('f2-hp-bar').style.width = pct + "%";
            document.getElementById('f2-hp-text').innerText = `${Math.max(0, step.targetRemainingHp)}/${m2.hp} HP`;
        } else {
            const pct = (step.targetRemainingHp / m1.hp) * 100;
            document.getElementById('f1-hp-bar').style.width = pct + "%";
            document.getElementById('f1-hp-text').innerText = `${Math.max(0, step.targetRemainingHp)}/${m1.hp} HP`;
        }
    }
    document.getElementById('battle-status').innerText = "🏁 Combat Terminé !";
    document.getElementById('btn-fight').disabled = false;
    document.getElementById('btn-fight').innerText = "LANCER";
}

// Initialisation
renderCharacters();
fillArenaDropdowns();

document.getElementById('btn-logout').addEventListener('click', () => { localStorage.clear(); window.location.href = 'index.html'; });