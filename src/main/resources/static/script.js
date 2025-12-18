const API_BASE = 'http://localhost:8080';

function questParty() {
    return {
        // --- STATE ---
        user: null,
        token: null,
        loading: false,
        showParties: false,
        toasts: [],

        // Listas de dados
        myQuestsList: [],
        reviewQuestsList: [],
        parties: [],
        quests: [],
        users: [],

        // Views e Modais
        currentView: 'tavern',
        authMode: 'login',
        showCreateParty: false,

        // Forms
        authForm: { username: '', password: '', nickname: '' },
        partyForm: { name: '', description: '' },
        questForm: { title: '', description: '', rarity: 'COMMON', gold: 100 },

        // Configuração de Raridade
        rarityMap: {
            'COMMON': 15,
            'RARE': 40,
            'EPIC': 150,
            'LEGENDARY': 500
        },

        // --- COMPUTED PROPERTIES (A Mágica da Interface) ---

        // 1. Recompensa atual baseada no select do form
        get currentReward() {
            return this.rarityMap[this.questForm.rarity] || 0;
        },

        // 2. Nível (Lê direto do Backend agora)
        get userLevel() {
            return (this.user && this.user.level) ? this.user.level : 1;
        },

        // 3. XP para o próximo nível (Lê direto do Backend)
        get nextLevelXp() {
            return (this.user && this.user.nextLevelXp) ? this.user.nextLevelXp : 100;
        },

        // 4. Porcentagem da Barra Roxa (Lê direto do Backend)
        get xpPercentage() {
            return (this.user && this.user.progressPercentage) ? this.user.progressPercentage : 0;
        },

        // 5. Ouro Pendente (Calcula no Front baseado nas missões ativas)
        get pendingRewards() {
            if (!this.myQuestsList || this.myQuestsList.length === 0) return 0;

            // Soma o ouro de todas as quests que estão Aprovadas ou Completas mas NÃO pagas
            return this.myQuestsList
                .filter(q => (q.status === 'APPROVED' || q.status === 'COMPLETED') && !q.rewardClaimed)
                .reduce((total, q) => total + (q.goldReward || 0), 0);
        },

        // 6. Verifica se sou dono da party atual
        get isPartyOwner() {
            if (!this.myParty || !this.user) return false;
            const ownerId = this.myParty.owner ? this.myParty.owner.id : this.myParty.ownerId;
            return ownerId === this.user.id;
        },

        // 7. Encontra a minha party na lista
        get myParty() {
            if (!this.user || !this.parties.length) return null;
            return this.parties.find(p =>
                (p.owner && p.owner.id === this.user.id) ||
                (p.members && p.members.some(m => m.id === this.user.id))
            );
        },

        // --- INICIALIZAÇÃO ---
        init() {
            const savedToken = localStorage.getItem('quest_token');
            try {
                const savedUser = JSON.parse(localStorage.getItem('quest_user'));
                if (savedToken && savedUser) {
                    this.token = savedToken;
                    this.user = savedUser;
                    this.showParties = false;
                    this.loadData();
                } else {
                    this.showParties = true;
                }
            } catch (e) {
                this.logout();
            }

            // Polling: Atualiza dados a cada 2s para ver o XP subir em tempo real
            setInterval(() => {
                if (this.token && this.user) {
                    this.loadData();
                }
            }, 2000);
        },

        // --- API HELPER ---
        async api(endpoint, options = {}) {
            const headers = {
                'Content-Type': 'application/json',
                ...(this.token ? { 'Authorization': `Bearer ${this.token}` } : {})
            };

            try {
                const res = await fetch(`${API_BASE}${endpoint}`, {
                    ...options,
                    headers: { ...headers, ...options.headers }
                });

                if (!res.ok) {
                    const error = await res.text();
                    throw new Error(error || `HTTP ${res.status}`);
                }

                const text = await res.text();
                if (!text) return null;
                try { return JSON.parse(text); } catch (e) { return text; }
            } catch (err) {
                // console.error(err); // Silencia erros de polling no console
                throw err;
            }
        },

        // --- AUTH ---
        async login() {
            this.loading = true;
            try {
                const data = await this.api('/auth/login', {
                    method: 'POST',
                    body: JSON.stringify(this.authForm)
                });

                this.token = data.token || data;
                localStorage.setItem('quest_token', this.token);

                await this.loadMe();
                this.showToast(`Welcome back, ${this.user.nickname}!`, 'success');
                this.authMode = 'login';
                this.authForm = { username: '', password: '', nickname: '' };
                this.loadData();
            } catch (err) {
                this.showToast('Login failed.', 'error');
            } finally {
                this.loading = false;
            }
        },

        async register() {
            this.loading = true;
            try {
                await this.api('/auth/register', {
                    method: 'POST',
                    body: JSON.stringify({
                        username: this.authForm.username,
                        nickname: this.authForm.nickname,
                        password: this.authForm.password,
                        role: 'USER'
                    })
                });
                this.showToast('Registration successful! Please login.', 'success');
                this.authMode = 'login';
                this.authForm = { username: '', password: '', nickname: '' };
            } catch (err) {
                this.showToast('Registration failed: ' + err.message, 'error');
            } finally {
                this.loading = false;
            }
        },

        logout() {
            this.user = null;
            this.token = null;
            localStorage.removeItem('quest_token');
            localStorage.removeItem('quest_user');
            this.showToast('Farewell, adventurer!', 'info');
        },

        // --- DATA LOADING ---
        async loadData() {
            await Promise.all([
                this.loadParties(),
                this.loadUsers(),
                this.loadMySpecificQuests(),
                this.loadReviewQuests(),
                this.loadMe()
            ]);
        },

        async loadMe() {
            try {
                const me = await this.api('/user/me');
                // Só atualiza localstorage se mudar algo para economizar escrita
                if (JSON.stringify(this.user) !== JSON.stringify(me)) {
                    this.user = me;
                    localStorage.setItem('quest_user', JSON.stringify(this.user));
                }
            } catch (e) { }
        },

        async loadReviewQuests() { try { this.reviewQuestsList = await this.api('/quests/to-review'); } catch (e) { } },
        async loadMySpecificQuests() { try { this.myQuestsList = await this.api('/quests/my-quests'); } catch (e) { } },
        async loadParties() { try { this.parties = await this.api('/parties/all'); } catch (e) { } },
        async loadUsers() { try { this.users = await this.api('/user/all'); } catch (e) { } },

        // --- ACTIONS: PARTY ---
        async createParty() {
            try {
                await this.api('/parties', {
                    method: 'POST',
                    body: JSON.stringify({
                        partyName: this.partyForm.name,
                        partyDescription: this.partyForm.description
                    })
                });
                this.showToast('Party created successfully!', 'success');
                this.showCreateParty = false;
                this.partyForm = { name: '', description: '' };
                await this.loadParties();
                await this.loadMe();
            } catch (err) {
                this.showToast('Failed: ' + err.message, 'error');
            }
        },

        async joinParty(id) {
            try {
                this.loading = true;
                await this.api(`/parties/join/${id}`, { method: 'POST' });
                await this.loadMe();
                await this.loadParties();
                this.showParties = false;
                this.showToast('Joined successfully!', 'success');
            } catch (err) {
                this.showToast('Failed: ' + err.message, 'error');
            } finally {
                this.loading = false;
            }
        },

        async leaveParty() {
            try {
                this.loading = true;
                await this.api('/parties/leave', { method: 'POST' });
                if (this.user) this.user.currentParty = null;
                await this.loadParties();
                this.showParties = true;
                this.showToast('Left the party.', 'info');
            } catch (err) {
                this.showToast('Error: ' + err.message, 'error');
            } finally {
                this.loading = false;
            }
        },

        // --- ACTIONS: PHASES ---
        async startPlanning() {
            try {
                await this.api(`/parties/${this.myParty.id}/start-planning`, { method: 'POST' });
                this.showToast('Planning phase started!', 'success');
                await this.loadParties();
            } catch (err) { this.showToast(err.message, 'error'); }
        },

        async startExecution() {
            try {
                await this.api(`/parties/${this.myParty.id}/start-execution`, { method: 'POST' });
                this.showToast('Adventure started!', 'success');
                await this.loadParties();
            } catch (err) { this.showToast(err.message, 'error'); }
        },

        async startReviewPhase() {
            try {
                this.loading = true;
                await this.api(`/parties/${this.myParty.id}/start-review`, { method: 'POST' });
                this.showToast('Sprint Finished! Rewards Distributed!', 'success');
                await this.loadParties();
            } catch (err) {
                this.showToast('Error finishing sprint: ' + err.message, 'error');
            } finally {
                this.loading = false;
            }
        },

        async resetLobby() {
            try {
                this.loading = true;
                await this.api(`/parties/${this.myParty.id}/reset-lobby`, { method: 'POST' });
                await this.loadParties();
                this.showToast('Lobby reset. Ready for next sprint.', 'success');
            } catch (err) { this.showToast(err.message, 'error'); }
            finally { this.loading = false; }
        },

        async disbandParty() {
            try {
                await this.api(`/parties/${this.myParty.id}`, { method: 'DELETE' });
                this.showToast('Party disbanded.', 'info');
                await this.loadParties();
            } catch (err) { this.showToast(err.message, 'error'); }
        },

        // --- ACTIONS: QUESTS ---
        async createQuest() {
            try {
                await this.api('/quests', {
                    method: 'POST',
                    body: JSON.stringify({
                        title: this.questForm.title,
                        description: this.questForm.description,
                        rarity: this.questForm.rarity,
                        goldReward: this.currentReward
                    })
                });
                this.showToast(`Quest posted! (${this.currentReward} G)`, 'success');
                this.questForm.title = '';
                this.questForm.description = '';
                await this.loadMySpecificQuests();
                await this.loadReviewQuests();
            } catch (err) { this.showToast(err.message, 'error'); }
        },

        async reviewQuest(id, approved) {
            try {
                await this.api(`/quests/${id}/review`, {
                    method: 'POST',
                    body: JSON.stringify({ approved, feedback: approved ? 'Approved' : 'Rejected' })
                });
                this.showToast(approved ? 'Approved!' : 'Rejected.', approved ? 'success' : 'info');
                await this.loadReviewQuests();
                await this.loadMySpecificQuests();
            } catch (err) { this.showToast(err.message, 'error'); }
        },

        async completeQuest(id) {
            try {
                await this.api(`/quests/${id}/complete`, { method: 'POST' });
                this.showToast('Quest marked as Complete!', 'success');
                await this.loadMySpecificQuests();
            } catch (err) { this.showToast(err.message, 'error'); }
        },

        // --- UI HELPER ---
        showToast(message, type = 'info') {
            const id = Date.now();
            this.toasts.push({ id, message, type });
            setTimeout(() => {
                this.toasts = this.toasts.filter(t => t.id !== id);
            }, 3000);
        }
    };
}