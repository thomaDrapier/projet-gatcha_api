// ==========================================
// CONFIGURATION DES APIS
// ==========================================
const PLAYER_API = "http://127.0.0.1:8082/player";
const INVOCATION_API = "http://127.0.0.1:8083/invocation"; 
const MONSTER_API = "http://127.0.0.1:8084/monsters"; 
const COMBAT_API = "http://127.0.0.1:8085/battle";

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
// GESTION DES PERSONNAGES
// ==========================================
// Remplace la fonction renderCharacters existante par celle-ci
async function renderCharacters() {
    charactersList.innerHTML = '<p style="color: var(--primary);">Synchronisation...</p>';
    if (myCharacters.length === 0) { 
        charactersList.innerHTML = '<p class="empty-msg">Aucun héros dans votre équipe.</p>'; 
        return; 
    }
    
    charactersList.innerHTML = ''; 
    for (let i = 0; i < myCharacters.length; i++) {
        const charName = myCharacters[i];
        const res = await fetchApi(`${PLAYER_API}/${charName}`, 'GET');
        loadedData[charName] = (res && res.status === 200) ? res.data : { username: charName, level: 1, experience: 0, monsters: [] };

        const data = loadedData[charName];
        const safeId = `btn-summon-${i}`;
        
        // --- NOUVEAUTÉ : Calcul de la barre d'XP Joueur ---
        const level = data.level || 1;
        const currentXp = data.experience || data.xp || 0;
        const xpThreshold = data.xpThreshold || (level * 1000); // Utilise le palier réel si dispo
        const xpPct = Math.min(100, (currentXp / xpThreshold) * 100);
        // --------------------------------------------------

        const card = document.createElement('div');
        card.className = 'char-card';
        card.innerHTML = `
            <div style="text-align: center; margin-bottom: 15px; background: rgba(0,0,0,0.3); padding: 10px; border-radius: 8px;">
                <h4 style="margin: 0; color: #fff;">${data.username} <span style="color: #ffd700; font-size: 0.8rem;">(Nv.${level})</span></h4>
                
                <div style="background: rgba(255,255,255,0.1); border-radius: 5px; height: 8px; width: 100%; margin-top: 8px; overflow: hidden;">
                    <div style="width: ${xpPct}%; background: #00d4ff; height: 100%; box-shadow: 0 0 5px #00d4ff; transition: width 0.5s;"></div>
                </div>
                <div style="font-size: 0.65rem; color: #aaa; margin-top: 3px; text-transform: uppercase;">XP : ${Math.floor(currentXp)} / ${xpThreshold}</div>
            </div>
            
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

const btnCreate = document.getElementById('btn-create-char');
if (btnCreate) {
    btnCreate.onclick = async () => {
        const input = document.getElementById('create-char-name');
        const name = input.value.trim();
        
        if (!name) {
            alert("Veuillez entrer un nom !");
            return;
        }

        const res = await fetchApi(`${PLAYER_API}/init/${name}`, 'POST');
        
        if (res && res.status === 200) {
            myCharacters.push(name);
            localStorage.setItem(storageKey, JSON.stringify(myCharacters));
            input.value = ''; 
            renderCharacters(); 
        } else {
            alert("Erreur lors de la création du personnage.");
        }
    };
}

window.openProfileModal = function(charName) {
    const data = loadedData[charName];
    if (!data) return;

    const level = data.level || 1;
    const currentXp = data.xp || data.experience || 0; 
    const xpRequired = level * 1000;
    let xpPercentage = (currentXp / xpRequired) * 100;
    if (xpPercentage > 100) xpPercentage = 100;

    const monsterCount = data.monsters ? data.monsters.length : 0;
    const maxCapacity = 2 + level; 
    const totalBattles = data.totalBattles || 0;

    const capacityColor = monsterCount >= maxCapacity ? "#ff4d4d" : "inherit";

    modalContent.innerHTML = `
        <h2 class="modal-title">Fiche de <span>${data.username}</span></h2>
        <div class="profile-grid" style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
            <div class="profile-info-block">
                <p>🎖️ Niveau : <strong>${level}</strong></p>
                <p>⚔️ Combats : <strong>${totalBattles}</strong></p>
            </div>
            <div class="profile-info-block">
                <p>🐉 Monstres : <strong style="color: ${capacityColor};">${monsterCount} / ${maxCapacity}</strong></p>
                <p>👤 Compte : <strong>${currentUser}</strong></p>
            </div>
        </div>
        
        <div class="xp-section">
            <div class="xp-text" style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                <span>Expérience</span>
                <span>${Math.floor(currentXp)} / ${xpRequired}</span>
            </div>
            <div class="xp-bar-bg" style="background: rgba(255,255,255,0.1); border-radius: 10px; height: 12px; overflow: hidden;">
                <div class="xp-bar-fill" style="width: ${xpPercentage}%; background: var(--primary); height: 100%; box-shadow: 0 0 10px var(--primary);"></div>
            </div>
        </div>
    `;
    modalOverlay.classList.remove('hidden');
};

window.openMonstersModal = async function(charName) {
    const data = loadedData[charName];
    const monsterIds = data.monsters || []; 
    modalContent.innerHTML = `<h2 class="modal-title">Bestiaire de <span>${charName}</span></h2>`;
    modalOverlay.classList.remove('hidden');
    
    const inv = document.createElement('div');
    inv.className = 'monster-inventory';
    modalContent.appendChild(inv);

    if (monsterIds.length === 0) {
        inv.innerHTML = '<p class="empty-msg" style="grid-column: 1/-1; text-align: center;">Ce héros n\'a pas encore de monstres.</p>';
        return;
    }

    for (const id of monsterIds) {
        try {
            const res = await fetch(`${MONSTER_API}/${id}`);
            if (res.ok) {
                const m = await res.json();
                const t = monsterTemplates[m.templateId] || monsterTemplates.default;
                
                // --- CALCUL DE L'XP DU MONSTRE ---
                const currentXp = m.xp || 0;
                const xpRequired = (m.level || 1) * 1000;
                const xpPct = Math.min(100, (currentXp / xpRequired) * 100);

                inv.innerHTML += `
                    <div class="monster-item">
                        <div class="monster-level-badge">Lv.${m.level || 1}</div>
                        <img src="${t.img}">
                        <span class="monster-name-mini">${t.name}</span>
                        
                        <div style="background: rgba(255,255,255,0.1); border-radius: 4px; height: 6px; width: 90%; margin: 5px auto 0; overflow: hidden;">
                            <div style="width: ${xpPct}%; background: #00d4ff; height: 100%; box-shadow: 0 0 5px #00d4ff;"></div>
                        </div>
                        <div style="font-size: 0.6rem; color: #aaa; text-align: center; margin-bottom: 5px;">XP : ${currentXp}/${xpRequired}</div>

                        <div class="monster-stats" style="display: grid; grid-template-columns: 1fr 1fr; gap: 5px; font-size: 0.8rem;">
                            <div class="stat-hp">❤️ ${m.hp || 0}</div>
                            <div class="stat-atk">⚔️ ${m.atk || 0}</div>
                            <div class="stat-def">🛡️ ${m.def || 0}</div>
                            <div class="stat-vit">⚡ ${m.vit || 0}</div>
                        </div>
                    </div>`;
            }
        } catch (e) { console.error("Erreur chargement monstre ID:", id); }
    }
};

// ==========================================
// INVOCATION ET TIMERS
// ==========================================
const COOLDOWN_MS = 5000;
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
    const btn = document.getElementById(id);
    const originalText = btn.innerHTML;
    btn.innerHTML = "⏳ Invocation...";
    btn.disabled = true;

    try {
        const res = await fetchApi(`${INVOCATION_API}/${name}`, 'POST');
        
        if (res && res.status === 200) {
            const t = monsterTemplates[res.data.templateId] || monsterTemplates.default;
            monsterRevealArea.innerHTML = `
                <div class="monster-card glass">
                    <h3>${t.name}</h3>
                    <img src="${t.img}" width="150">
                    <button onclick="closeRevealModal('${name}')" class="action-btn">Génial !</button>
                </div>`;
            monsterRevealArea.classList.remove('hidden'); 
            summonModal.classList.remove('hidden');
            
            if (loadedData[name]) loadedData[name].monsters.push(res.data.instanceId);
            fillArenaDropdowns(); 

        } else if (res && res.status === 400) {
            const errorMsg = res.data.message || res.data || "Inventaire plein !";
            alert("❌ Invocation impossible :\n" + errorMsg);
            btn.innerHTML = originalText;
            btn.disabled = false;
        } else {
            alert("Erreur du serveur d'invocation.");
            btn.innerHTML = originalText;
            btn.disabled = false;
        }
    } catch (e) {
        console.error("Erreur d'invocation:", e);
        btn.innerHTML = originalText;
        btn.disabled = false;
    }
};

window.closeRevealModal = function(name) {
    summonModal.classList.add('hidden'); monsterRevealArea.classList.add('hidden');
    localStorage.setItem(`gatcha_timer_${name}`, Date.now().toString());
    updateDashboardTimers();
};

// ==========================================
// SYSTÈME DE COMBAT
// ==========================================
// --- VARIABLES GLOBALES POUR L'ARÈNE ---
let arenaMonsters = [];
let selectedM1 = null;
let selectedM2 = null;

async function fillArenaDropdowns() {
    try {
        const res = await fetch(MONSTER_API);
        arenaMonsters = await res.json();
        renderArenaLists();
    } catch (e) { console.error("Erreur chargement arène"); }
}

function renderArenaLists() {
    const list1 = document.getElementById('arena-m1-list');
    const list2 = document.getElementById('arena-m2-list');
    if (!list1 || !list2) return;
    
    list1.innerHTML = '';
    list2.innerHTML = '';

    // Trouver les propriétaires des monstres sélectionnés
    const ownerM1 = selectedM1 ? arenaMonsters.find(m => m.id === selectedM1)?.ownerUsername : null;
    const ownerM2 = selectedM2 ? arenaMonsters.find(m => m.id === selectedM2)?.ownerUsername : null;

    arenaMonsters.forEach(m => {
        const t = monsterTemplates[m.templateId] || monsterTemplates.default;
        
        // --- Construction HTML de la carte ---
        const buildCard = (slot, isSelected, isDisabled) => `
            <div class="monster-select-card ${isSelected ? `selected-${slot}` : ''} ${isDisabled ? 'disabled' : ''}" 
                 onclick="${isDisabled ? '' : `selectArenaMonster(${slot}, '${m.id}')`}">
                <img src="${t.img}">
                <div class="monster-select-info">
                    <span class="monster-select-name">[Lv.${m.level}] ${t.name}</span>
                    <span class="monster-select-owner">👤 Dresseur : ${m.ownerUsername}</span>
                    <span style="font-size: 0.7rem; color: #aaa;">❤️ ${m.hp} | ⚔️ ${m.atk}</span>
                </div>
            </div>
        `;

        // Slot 1 : On désactive si ce monstre appartient au dresseur 2 (ou s'il est déjà sélectionné en slot 2)
        const disableForM1 = (ownerM2 === m.ownerUsername) || (selectedM2 === m.id);
        list1.innerHTML += buildCard(1, selectedM1 === m.id, disableForM1);

        // Slot 2 : On désactive si ce monstre appartient au dresseur 1 (ou s'il est déjà sélectionné en slot 1)
        const disableForM2 = (ownerM1 === m.ownerUsername) || (selectedM1 === m.id);
        list2.innerHTML += buildCard(2, selectedM2 === m.id, disableForM2);
    });
}

window.selectArenaMonster = function(slot, monsterId) {
    if (slot === 1) {
        selectedM1 = (selectedM1 === monsterId) ? null : monsterId; // Toggle
    } else {
        selectedM2 = (selectedM2 === monsterId) ? null : monsterId; // Toggle
    }
    renderArenaLists(); // Redessine pour mettre à jour les couleurs et griser les options
};

// Remplace la fonction startBattle existante
window.startBattle = async function() {
    const m1Id = selectedM1;
    const m2Id = selectedM2;

    if (!m1Id || !m2Id) return alert("Veuillez sélectionner deux monstres dans l'arène !");

    const m1DataCheck = arenaMonsters.find(m => m.id === m1Id);
    const m2DataCheck = arenaMonsters.find(m => m.id === m2Id);
    if (m1DataCheck.ownerUsername === m2DataCheck.ownerUsername) {
        return alert("Sélectionnez deux monstres de dresseurs différents !");
    }

    const btn = document.getElementById('btn-fight');
    btn.disabled = true; 
    btn.innerText = "Calcul...";

    try {
        // --- 1. RÉCUPÉRATION DES STATS "AVANT" (MONSTRES + JOUEURS) ---
        const fetchPlayer = async (username) => {
            const r = await fetch(`${PLAYER_API}/${username}`, { headers: { 'Authorization': `Bearer ${currentToken}` } });
            if (!r.ok) return { level: 1, totalBattles: 0, experience: 0, xpThreshold: 1000 };
            return r.json();
        };

        const [resM1, resM2] = await Promise.all([fetch(`${MONSTER_API}/${m1Id}`), fetch(`${MONSTER_API}/${m2Id}`)]);
        const m1Old = await resM1.json();
        const m2Old = await resM2.json();
        const p1Old = await fetchPlayer(m1Old.ownerUsername);
        const p2Old = await fetchPlayer(m2Old.ownerUsername);

        // --- 2. LANCEMENT DU COMBAT ---
        const battleRes = await fetch(`${COMBAT_API}/start?monster1Id=${m1Id}&monster2Id=${m2Id}`, { 
            method: 'POST', headers: { 'Authorization': `Bearer ${currentToken}` }
        });
        if (!battleRes.ok) throw new Error("Erreur serveur combat.");
        const battle = await battleRes.json();

        const battleDisplay = document.getElementById('battle-display');
        battleDisplay.classList.remove('hidden');
        battleDisplay.scrollIntoView({ behavior: 'smooth' });
        
        await animateBattle(battle, m1Old, m2Old);
        await new Promise(r => setTimeout(r, 1000));

        // --- 3. RÉCUPÉRATION DES STATS "APRÈS" ---
        const [resNewM1, resNewM2] = await Promise.all([
            fetch(`${MONSTER_API}/${m1Id}`).then(r => r.json()),
            fetch(`${MONSTER_API}/${m2Id}`).then(r => r.json())
        ]);
        const p1New = await fetchPlayer(m1Old.ownerUsername);
        const p2New = await fetchPlayer(m2Old.ownerUsername);

        // --- 4. AFFICHAGE DES RÉSULTATS GLOBAUX ---
        showVictoryScreen(
            battle, 
            {old: m1Old, new: resNewM1, playerOld: p1Old, playerNew: p1New}, 
            {old: m2Old, new: resNewM2, playerOld: p2Old, playerNew: p2New}
        );
        
        renderCharacters(); 
        loadBattleHistory();

    } catch (e) {
        console.error("Erreur combat :", e);
        alert("Erreur : " + e.message);
        btn.disabled = false; btn.innerText = "LANCER";
    }
};
async function animateBattle(battle, m1, m2) {
    const logs = document.getElementById('battle-logs');
    const arenaDiv = document.querySelector('.battle-arena'); 

    if (!arenaDiv) {
        console.error("Erreur: L'élément HTML '.battle-arena' est introuvable !");
        return; 
    }

    logs.innerHTML = "";
    
    const t1 = monsterTemplates[m1.templateId] || monsterTemplates.default;
    const t2 = monsterTemplates[m2.templateId] || monsterTemplates.default;

    arenaDiv.innerHTML = `
        <div class="fighter" id="fighter-1">
            <div class="fighter-info">
                <span class="fighter-name" style="color:var(--primary); font-weight:bold;">Niv ${m1.level} - ${t1.name}</span>
                <span class="owner-name">(${m1.ownerUsername})</span>
            </div>
            <img src="${t1.img}" class="monster-img">
            <div class="hp-container">
                <div id="f1-hp-bar" class="hp-bar" style="width: 100%;"></div>
                <div id="f1-hp-text" class="hp-text">${m1.hp}/${m1.hp} HP</div>
            </div>
        </div>
        <div class="vs-badge">VS</div>
        <div class="fighter" id="fighter-2">
            <div class="fighter-info">
                <span class="fighter-name" style="color:var(--danger); font-weight:bold;">Niv ${m2.level} - ${t2.name}</span>
                <span class="owner-name">(${m2.ownerUsername})</span>
            </div>
            <img src="${t2.img}" class="monster-img">
            <div class="hp-container">
                <div id="f2-hp-bar" class="hp-bar" style="width: 100%;"></div>
                <div id="f2-hp-text" class="hp-text">${m2.hp}/${m2.hp} HP</div>
            </div>
        </div>
    `;

    // Boucle de combat
    for (const step of battle.replayLogs) {
        // On attend la fin du timer avant de passer au tour suivant
        await new Promise(r => setTimeout(r, 800));
        
        let logDesc = step.description.replace("Tour ", "T");
        logDesc = logDesc.replace(/Dégâts : (\d+)/, 'Dégâts : <b style="color: #ff4d4d;">$1</b>');

        logs.innerHTML += `<div class="log-entry">> <span class="log-turn">T${step.turn}</span>: ${logDesc}</div>`;
        logs.scrollTop = logs.scrollHeight;

        const isM1Attacking = step.attackerId ? (step.attackerId === m1.id) : (step.attackerName.includes(m1.ownerUsername));

        if (isM1Attacking) {
            const pct = (step.targetRemainingHp / m2.hp) * 100;
            document.getElementById('f2-hp-bar').style.width = Math.max(0, pct) + "%";
            document.getElementById('f2-hp-text').innerText = `${Math.max(0, step.targetRemainingHp)}/${m2.hp} HP`;
            document.getElementById('fighter-2').classList.add('shake');
            setTimeout(() => document.getElementById('fighter-2').classList.remove('shake'), 400);
        } else {
            const pct = (step.targetRemainingHp / m1.hp) * 100;
            document.getElementById('f1-hp-bar').style.width = Math.max(0, pct) + "%";
            document.getElementById('f1-hp-text').innerText = `${Math.max(0, step.targetRemainingHp)}/${m1.hp} HP`;
            document.getElementById('fighter-1').classList.add('shake');
            setTimeout(() => document.getElementById('fighter-1').classList.remove('shake'), 400);
        }
    }

    // --- CRUCIAL : On attend un peu après le dernier coup pour que le joueur voit le résultat ---
    await new Promise(r => setTimeout(r, 1000));

    document.getElementById('battle-status').innerText = "🏁 Combat Terminé !";
    document.getElementById('btn-fight').disabled = false;
    document.getElementById('btn-fight').innerText = "LANCER";
    
    // On charge l'historique en arrière-plan
    loadBattleHistory();

    // On retourne true pour confirmer à la fonction appelante que l'animation est finie
    return true; 
}

// ==========================================
// HISTORIQUE DES COMBATS
// ==========================================

async function loadBattleHistory() {
    try {
        const response = await fetch(`${COMBAT_API}/history`);
        const battles = await response.json();
        const listContainer = document.getElementById('battleHistoryList');
        
        listContainer.innerHTML = '';

        const monsterCache = {};
        const getMonster = async (id) => {
            if (monsterCache[id]) return monsterCache[id];
            try {
                const res = await fetch(`${MONSTER_API}/${id}`);
                if (res.ok) {
                    const data = await res.json();
                    monsterCache[id] = data;
                    return data;
                }
            } catch(e) {}
            return null; 
        };

        for (const b of battles.reverse()) {
            const date = new Date(b.battleDate).toLocaleString();
            const turns = b.replayLogs ? b.replayLogs.length : 0;
            
            const m1 = await getMonster(b.monster1Id);
            const m2 = await getMonster(b.monster2Id);

            const formatName = (m, fallbackId) => {
                if (m) {
                    const t = monsterTemplates[m.templateId] || monsterTemplates.default;
                    return `Niv ${m.level} - ${t.name} - ${m.ownerUsername}`;
                }
                return `Héros disparu (${fallbackId.substring(0,5)}...)`;
            };

            const m1Name = formatName(m1, b.monster1Id);
            const m2Name = formatName(m2, b.monster2Id);

            let winnerName = b.winnerMonsterId;

            if (winnerName) { 
                const parts = winnerName.split(' - ');
                if (parts.length === 3) { 
                    const t = monsterTemplates[parts[1]] || monsterTemplates.default;
                    winnerName = `${parts[0]} - ${t.name} - ${parts[2]}`;
                }
            } else {
                winnerName = "Match Nul / Inconnu"; 
            }

            const card = document.createElement('div');
            card.className = 'battle-card';
            card.onclick = () => showBattleLogs(b.id);
            
            card.innerHTML = `
                <div style="font-size: 0.75rem; color: #888; margin-bottom: 8px; display: flex; justify-content: space-between;">
                    <span>📅 ${date}</span>
                    <span style="background: #333; padding: 2px 6px; border-radius: 10px; color: #aaa;">⏱️ ${turns} tours</span>
                </div>
                
                <div style="font-size: 0.85rem; margin-bottom: 10px; text-align: center; background: rgba(0,0,0,0.4); padding: 8px; border-radius: 6px; border: 1px solid #333;">
                    <div style="color: #00d4ff; font-weight: bold;">${m1Name}</div>
                    <div style="font-size: 0.7rem; color: #ff4d4d; margin: 4px 0; font-style: italic;">VS</div>
                    <div style="color: #e74c3c; font-weight: bold;">${m2Name}</div>
                </div>
                
                <div style="color: #fff; font-size: 0.85rem; text-align: center;">
                    🏆 Victoire : <strong style="color: #ffd700;">${winnerName}</strong>
                </div>
            `;
            listContainer.appendChild(card);
        }
    } catch (error) {
        console.error("Erreur lors du chargement de l'historique :", error);
    }
}

function closeReplay() {
    document.getElementById('battleLogContainer').style.display = 'none';
    document.getElementById('battleHistoryList').style.display = 'flex';
}

async function showBattleLogs(battleId) {
    const container = document.getElementById('battleLogContainer');
    const listContainer = document.getElementById('battleHistoryList');
    const arenaContent = document.getElementById('replay-arena-content');

    listContainer.style.display = 'none';
    container.style.display = 'block';
    arenaContent.innerHTML = '<p style="text-align:center; color: var(--primary);">Chargement de la cassette...</p>';

    try {
        const response = await fetch(`${COMBAT_API}/${battleId}`);
        const battle = await response.json();

        const [resM1, resM2] = await Promise.all([
            fetch(`${MONSTER_API}/${battle.monster1Id}`),
            fetch(`${MONSTER_API}/${battle.monster2Id}`)
        ]);

        const m1 = await resM1.json();
        const m2 = await resM2.json();

        const t1 = monsterTemplates[m1.templateId] || monsterTemplates.default;
        const t2 = monsterTemplates[m2.templateId] || monsterTemplates.default;

        const m1BackendName = `Niv ${m1.level} - ${m1.templateId} - ${m1.ownerUsername}`;
        const m2BackendName = `Niv ${m2.level} - ${m2.templateId} - ${m2.ownerUsername}`;

        arenaContent.innerHTML = `
            <div style="background: #111; padding: 15px; border-radius: 10px; margin-bottom: 15px; border: 1px solid #333; box-shadow: 0 4px 6px rgba(0,0,0,0.3);">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    
                    <div style="text-align: center; width: 40%;">
                        <img src="${t1.img}" style="height: 60px; object-fit: contain; filter: drop-shadow(0 0 5px rgba(0, 212, 255, 0.5));">
                        <div style="font-size: 0.8rem; color: #fff; font-weight: bold; margin-top: 5px;">${t1.name} <span style="color: #ffd700;">Lv.${m1.level}</span></div>
                        <div class="player-name-tag">Joueur : ${m1.ownerUsername}</div>
                        <div style="background: #333; height: 8px; width: 100%; margin-top: 8px; border-radius: 4px; overflow: hidden;">
                            <div id="replay-m1-hp" style="background: #00d4ff; height: 100%; width: 100%; transition: width 0.3s;"></div>
                        </div>
                    </div>
                    
                    <div style="font-size: 1.5rem; color: #ff4d4d; font-weight: bold; font-style: italic;">VS</div>
                    
                    <div style="text-align: center; width: 40%;">
                        <img src="${t2.img}" style="height: 60px; object-fit: contain; filter: drop-shadow(0 0 5px rgba(231, 76, 60, 0.5));">
                        <div style="font-size: 0.8rem; color: #fff; font-weight: bold; margin-top: 5px;">${t2.name} <span style="color: #ffd700;">Lv.${m2.level}</span></div>
                        <div class="player-name-tag">Joueur : ${m2.ownerUsername}</div>
                        <div style="background: #333; height: 8px; width: 100%; margin-top: 8px; border-radius: 4px; overflow: hidden;">
                            <div id="replay-m2-hp" style="background: #e74c3c; height: 100%; width: 100%; transition: width 0.3s;"></div>
                        </div>
                    </div>
                </div>
            </div>
            
            <div id="replayLogsDiv" class="chat-container"></div>
        `;
        
        const logsDiv = document.getElementById('replayLogsDiv');

        for (const step of battle.replayLogs) {
            await new Promise(r => setTimeout(r, 600)); 

            const isM1Attacking = step.attackerId ? (step.attackerId === m1.id) : (step.attackerName === m1BackendName || step.attackerName === m1.id);
            
            const attackerStr = isM1Attacking ? `${t1.name}` : `${t2.name}`;
            const defenderStr = isM1Attacking ? `${t2.name}` : `${t1.name}`;
            const logText = `<b>${attackerStr}</b> attaque ${defenderStr} et inflige <b>${step.damage}</b> dégâts !`;

            const bubble = document.createElement('div');
            bubble.className = `chat-bubble ${isM1Attacking ? 'chat-left' : 'chat-right'}`;
            bubble.innerHTML = `<span style="font-size: 0.7rem; opacity: 0.7; display: block; margin-bottom: 3px;">Tour ${step.turn}</span> ${logText}`;
            
            logsDiv.appendChild(bubble);

            if (isM1Attacking) {
                const pct = Math.max(0, (step.targetRemainingHp / m2.hp) * 100);
                document.getElementById('replay-m2-hp').style.width = pct + "%";
            } else {
                const pct = Math.max(0, (step.targetRemainingHp / m1.hp) * 100);
                document.getElementById('replay-m1-hp').style.width = pct + "%";
            }

            logsDiv.scrollTop = logsDiv.scrollHeight;
        }

    } catch (error) {
        console.error("Erreur lors de la récupération du replay :", error);
        arenaContent.innerHTML = `<p style="text-align:center; color:#ff4d4d;">Erreur lors du chargement de la vidéo.</p>`;
    }
}

// Remplace la fonction showVictoryScreen existante
window.showVictoryScreen = function(battle, m1Data, m2Data) {
    const isM1Winner = battle.winnerMonsterId.includes(m1Data.old.templateId) || battle.winnerMonsterId.includes(m1Data.old.ownerUsername);
    
    // Fonction utilitaire pour calculer l'XP (gère la montée de niveau)
    const calcXp = (oldData, newData, isPlayer = false) => {
        let xpGained = (isPlayer ? newData.experience : newData.xp) - (isPlayer ? oldData.experience : oldData.xp);
        if (newData.level > oldData.level) {
            for(let l = oldData.level; l < newData.level; l++) {
                xpGained += isPlayer ? (oldData.xpThreshold || l * 1000) : (l * 1000);
            }
        }
        return Math.max(0, Math.round(xpGained));
    };

    // Générateur de carte de résultat
    const buildResultCard = (data, isWinner) => {
        const t = monsterTemplates[data.new.templateId] || monsterTemplates.default;
        const mLevelUp = data.new.level > data.old.level;
        const pLevelUp = data.playerNew.level > data.playerOld.level;
        
        return `
            <div class="gain-card" style="border: 2px solid ${isWinner ? '#00d4ff' : '#ff4d4d'}; position: relative;">
                ${isWinner ? '<div style="position:absolute; top:-15px; left:50%; transform:translateX(-50%); background:#00d4ff; color:#000; padding:2px 10px; border-radius:10px; font-weight:bold; font-size:0.8rem;">VAINQUEUR</div>' : ''}
                
                <h4 style="margin-top: 10px; color: #fff;">${data.old.ownerUsername}</h4>
                <p style="font-size: 0.8rem; color: #aaa;">Dresseur Lv.${data.playerNew.level} ${pLevelUp ? '<span style="color:#ffd700;">(UP!)</span>' : ''}</p>
                <p style="color: #00d4ff; font-weight: bold; font-size: 0.9rem;">+${calcXp(data.playerOld, data.playerNew, true)} XP Dresseur</p>
                
                <hr style="border-color: #333; margin: 10px 0;">
                
                <img src="${t.img}" style="width: 80px; filter: drop-shadow(0 0 5px ${isWinner ? '#00d4ff' : 'transparent'});">
                <h5 style="margin: 5px 0;">${t.name} ${mLevelUp ? '<span class="level-up-badge">LVL UP !</span>' : ''}</h5>
                <p style="color: #00d4ff; font-weight: bold; font-size: 0.9rem;">+${calcXp(data.old, data.new, false)} XP Monstre</p>
                <p style="font-size: 0.75rem; color: #aaa;">Matchs joués : ${data.new.totalBattles || 1}</p>
                
                ${mLevelUp ? `
                    <div style="font-size: 0.75rem; margin-top: 5px;">
                        <span class="stat-up">❤️ +${data.new.hp - data.old.hp}</span>
                        <span class="stat-up">⚔️ +${data.new.atk - data.old.atk}</span>
                    </div>
                ` : `<p style="font-size:0.75rem; color:grey;">Niveau ${data.new.level} atteint.</p>`}
            </div>
        `;
    };

    modalContent.innerHTML = `
        <div class="victory-popup" style="max-width: 600px;">
            <h1 class="victory-title" style="margin-bottom: 30px;">BILAN DU COMBAT</h1>
            
            <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
                ${buildResultCard(m1Data, isM1Winner)}
                ${buildResultCard(m2Data, !isM1Winner)}
            </div>
            
            <button onclick="modalOverlay.classList.add('hidden')" class="action-btn glow" style="margin-top: 25px;">CONTINUER</button>
        </div>
    `;
    modalOverlay.classList.remove('hidden');
};

// Initialisation
renderCharacters();
fillArenaDropdowns();
loadBattleHistory();

document.getElementById('btn-logout').addEventListener('click', () => { 
    localStorage.removeItem('gatcha_token');
    localStorage.removeItem('gatcha_username');
    window.location.href = 'index.html'; 
});