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
    const totalBattles = data.totalBattles || 0;

    modalContent.innerHTML = `
        <h2 class="modal-title">Fiche de <span>${data.username}</span></h2>
        <div class="profile-grid" style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px; margin-bottom: 20px;">
            <div class="profile-info-block">
                <p>🎖️ Niveau : <strong>${level}</strong></p>
                <p>⚔️ Combats : <strong>${totalBattles}</strong></p>
            </div>
            <div class="profile-info-block">
                <p>🐉 Monstres : <strong>${monsterCount}</strong></p>
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
                
                inv.innerHTML += `
                    <div class="monster-item">
                        <div class="monster-level-badge">Lv.${m.level || 1}</div>
                        <img src="${t.img}">
                        <span class="monster-name-mini">${t.name}</span>
                        <div class="monster-stats" style="display: grid; grid-template-columns: 1fr 1fr; gap: 5px; font-size: 0.8rem; margin-top: 5px;">
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
        fillArenaDropdowns(); 
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
async function fillArenaDropdowns() {
    try {
        const res = await fetch(MONSTER_API);
        const monsters = await res.json();
        const s1 = document.getElementById('arena-m1');
        const s2 = document.getElementById('arena-m2');
        if (!s1 || !s2) return;
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

    if (!m1Id || !m2Id || m1Id === m2Id) {
        return alert("Sélectionnez deux monstres différents !");
    }

    const btn = document.getElementById('btn-fight');
    btn.disabled = true; 
    btn.innerText = "Calcul...";

    try {
        const battleRes = await fetch(`${COMBAT_API}/start?monster1Id=${m1Id}&monster2Id=${m2Id}`, { method: 'POST' });

        if (!battleRes.ok) {
            const errorText = await battleRes.text();
            throw new Error("Le serveur de combat a renvoyé une erreur : " + errorText);
        }

        const battle = await battleRes.json();

        const [resM1, resM2] = await Promise.all([
            fetch(`${MONSTER_API}/${m1Id}`),
            fetch(`${MONSTER_API}/${m2Id}`)
        ]);

        if (!resM1.ok || !resM2.ok) throw new Error("Impossible de récupérer les stats des monstres.");

        const m1 = await resM1.json();
        const m2 = await resM2.json();

        const battleDisplay = document.getElementById('battle-display');
        if (battleDisplay) {
            battleDisplay.classList.remove('hidden');
            battleDisplay.scrollIntoView({ behavior: 'smooth' });
        }
        
        animateBattle(battle, m1, m2);

    } catch (e) {
        console.error("Détail de l'erreur JS:", e);
        alert("Erreur : " + e.message);
        btn.disabled = false; 
        btn.innerText = "LANCER";
    }
};

async function animateBattle(battle, m1, m2) {
    const logs = document.getElementById('battle-logs');
    logs.innerHTML = "";
    
    // --- CORRECTION : Création des noms formatés pour l'interface ---
    const m1DisplayName = `Niv ${m1.level} - ${m1.templateId} - ${m1.ownerUsername}`;
    const m2DisplayName = `Niv ${m2.level} - ${m2.templateId} - ${m2.ownerUsername}`;

    document.getElementById('f1-name').innerText = m1DisplayName;
    document.getElementById('f2-name').innerText = m2DisplayName;

    // --- CORRECTION : Initialisation visuelle des HPs au max ---
    document.getElementById('f1-hp-bar').style.width = "100%";
    document.getElementById('f1-hp-text').innerText = `${m1.hp}/${m1.hp} HP`;
    document.getElementById('f2-hp-bar').style.width = "100%";
    document.getElementById('f2-hp-text').innerText = `${m2.hp}/${m2.hp} HP`;

    for (const step of battle.replayLogs) {
        await new Promise(r => setTimeout(r, 1000));
        
        // Un peu de cosmétique pour l'arène
        let logDesc = step.description.replace("Tour ", "T");
        logDesc = logDesc.replace(/Dégâts : (\d+)/, 'Dégâts : <b style="color: #ff4d4d;">$1</b>');

        logs.innerHTML += `<div style="margin-bottom:5px; border-bottom: 1px solid #333; padding-bottom: 5px;">> <span style="color:var(--primary)">T${step.turn}</span>: ${logDesc}</div>`;
        logs.scrollTop = logs.scrollHeight;

        // --- CORRECTION : On compare le nom complet avec celui formaté ---
        if (step.attackerName === m1DisplayName) {
            // C'est M1 qui attaque, donc M2 perd de la vie
            const pct = (step.targetRemainingHp / m2.hp) * 100;
            document.getElementById('f2-hp-bar').style.width = pct + "%";
            document.getElementById('f2-hp-text').innerText = `${Math.max(0, step.targetRemainingHp)}/${m2.hp} HP`;
        } else {
            // C'est M2 qui attaque, donc M1 perd de la vie
            const pct = (step.targetRemainingHp / m1.hp) * 100;
            document.getElementById('f1-hp-bar').style.width = pct + "%";
            document.getElementById('f1-hp-text').innerText = `${Math.max(0, step.targetRemainingHp)}/${m1.hp} HP`;
        }
    }
    document.getElementById('battle-status').innerText = "🏁 Combat Terminé !";
    document.getElementById('btn-fight').disabled = false;
    document.getElementById('btn-fight').innerText = "LANCER";

    // Rafraîchir l'historique automatiquement !
    loadBattleHistory();
}


// ==========================================
// HISTORIQUE DES COMBATS (VERSION CHAT BUBBLES)
// ==========================================

async function loadBattleHistory() {
    try {
        const response = await fetch(`${COMBAT_API}/history`);
        const battles = await response.json();
        const listContainer = document.getElementById('battleHistoryList');
        
        listContainer.innerHTML = '';

        // On crée un petit cache pour éviter de spammer l'API Monstre
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
            return null; // Si le monstre a été supprimé
        };

        // On parcourt les combats du plus récent au plus ancien
        for (const b of battles.reverse()) {
            const date = new Date(b.battleDate).toLocaleString();
            const turns = b.replayLogs ? b.replayLogs.length : 0;
            
            // 1. On récupère les vraies données des deux monstres
            const m1 = await getMonster(b.monster1Id);
            const m2 = await getMonster(b.monster2Id);

            // 2. Fonction pour traduire les données en un joli nom : "Niv X - Nom - Joueur"
            const formatName = (m, fallbackId) => {
                if (m) {
                    const t = monsterTemplates[m.templateId] || monsterTemplates.default;
                    return `Niv ${m.level} - ${t.name} - ${m.ownerUsername}`;
                }
                return `Héros disparu (${fallbackId.substring(0,5)}...)`;
            };

            const m1Name = formatName(m1, b.monster1Id);
            const m2Name = formatName(m2, b.monster2Id);

            // 3. On traduit aussi le nom du vainqueur (qui était stocké "Niv 1 - 3 - tom")
            let winnerName = b.winnerMonsterId;
            const parts = winnerName.split(' - ');
            if (parts.length === 3) { // Si c'est bien le format du backend
                const t = monsterTemplates[parts[1]] || monsterTemplates.default;
                winnerName = `${parts[0]} - ${t.name} - ${parts[2]}`;
            }

            // 4. Création de la carte visuelle
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

// Fonction pour fermer le replay et revenir à la liste
function closeReplay() {
    document.getElementById('battleLogContainer').style.display = 'none';
    document.getElementById('battleHistoryList').style.display = 'flex';
}

async function showBattleLogs(battleId) {
    const container = document.getElementById('battleLogContainer');
    const listContainer = document.getElementById('battleHistoryList');
    const arenaContent = document.getElementById('replay-arena-content');

    // On cache la liste et on affiche la zone de replay
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

        // Construction de l'arène avec la nouvelle typographie pour les joueurs
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
            await new Promise(r => setTimeout(r, 600)); // Vitesse d'apparition des bulles

            const isM1Attacking = step.attackerName === m1BackendName || step.attackerName === m1.id;
            
            // Format du texte dans la bulle
            const attackerStr = isM1Attacking ? `${t1.name}` : `${t2.name}`;
            const defenderStr = isM1Attacking ? `${t2.name}` : `${t1.name}`;
            const logText = `<b>${attackerStr}</b> attaque ${defenderStr} et inflige <b>${step.damage}</b> dégâts !`;

            // Création de la bulle
            const bubble = document.createElement('div');
            // Si M1 attaque, la bulle est à gauche (chat-left), sinon à droite (chat-right)
            bubble.className = `chat-bubble ${isM1Attacking ? 'chat-left' : 'chat-right'}`;
            bubble.innerHTML = `<span style="font-size: 0.7rem; opacity: 0.7; display: block; margin-bottom: 3px;">Tour ${step.turn}</span> ${logText}`;
            
            logsDiv.appendChild(bubble);

            // Mise à jour visuelle des barres de vie
            if (isM1Attacking) {
                const pct = Math.max(0, (step.targetRemainingHp / m2.hp) * 100);
                document.getElementById('replay-m2-hp').style.width = pct + "%";
            } else {
                const pct = Math.max(0, (step.targetRemainingHp / m1.hp) * 100);
                document.getElementById('replay-m1-hp').style.width = pct + "%";
            }

            // Scroll auto vers le bas du chat
            logsDiv.scrollTop = logsDiv.scrollHeight;
        }

    } catch (error) {
        console.error("Erreur lors de la récupération du replay :", error);
        arenaContent.innerHTML = `<p style="text-align:center; color:#ff4d4d;">Erreur lors du chargement de la vidéo.</p>`;
    }
}
// Initialisation
renderCharacters();
fillArenaDropdowns();
loadBattleHistory();

document.getElementById('btn-logout').addEventListener('click', () => { 
    localStorage.removeItem('gatcha_token');
    localStorage.removeItem('gatcha_username');
    window.location.href = 'index.html'; 
});