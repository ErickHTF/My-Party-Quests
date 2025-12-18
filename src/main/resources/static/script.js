const API_BASE = 'http://localhost:8080';

function questParty() {
    return {
        // State
        user: null,
        token: null,
        loading: false, // Loading visual apenas para ações manuais
        showParties: false,
        toasts: [],
        myQuestsList: [],
        reviewQuestsList: [],

        // Views
        currentView: 'tavern',
        authMode: 'login',
        showCreateParty: false,

        // Forms
        authForm: { username: '', password: '', nickname: '' },
        partyForm: { name: '', description: '' },
        questForm: { title: '', description: '', rarity: 'COMMON', gold: 100 },
        rarityMap: {
            'COMMON': 15,
            'RARE': 40,
            'EPIC': 150,
            'LEGENDARY': 500
        },

        get currentReward() {
            return this.rarityMap[this.questForm.rarity] || 0;
        },

        // Data
        parties: [],
        quests: [],
        users: [],

        // Computed
        get isPartyOwner() {
            if (!this.myParty || !this.user) return false;
            const ownerId = this.myParty.owner ? this.myParty.owner.id : this.myParty.ownerId;
            return ownerId === this.user.id;
        },

        get myParty() {
            if (!this.user || !this.parties.length) return null;
            return this.parties.find(p =>
                (p.owner && p.owner.id === this.user.id) ||
                (p.members && p.members.some(m => m.id === this.user.id))
            );
        },

        // --- INICIALIZAÇÃO E AUTO-UPDATE ---
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

            // --- AQUI ESTÁ A MÁGICA (POLLING) ---
            // A cada 2 segundos, recarrega os dados se estiver logado
            setInterval(() => {
                if (this.token && this.user) {
                    // Chamamos loadData em "silêncio" (sem ativar this.loading)
                    this.loadData();
                }
            }, 2000);
        },

        // Toast
        showToast(message, type = 'info') {
            const id = Date.now();
            this.toasts.push({ id, message, type });
            setTimeout(() => {
                this.toasts = this.toasts.filter(t => t.id !== id);
            }, 3000);
        },

        // API Helper
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

                try {
                    return JSON.parse(text);
                } catch (e) {
                    return text;
                }
            } catch (err) {
                // console.error('API Error:', err); // Comentei para não poluir o console no loop
                throw err;
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

        // Load Data
        async loadData() {
            // Note que não setamos this.loading = true aqui
            // para não ficar piscando a tela a cada 2 segundos
            await Promise.all([
                this.loadParties(),
                this.loadUsers(),
                this.loadMySpecificQuests(),
                this.loadReviewQuests(),
                this.loadMe()
            ]);
        },

        async loadReviewQuests() {
            try { this.reviewQuestsList = await this.api('/quests/to-review'); } catch (e) { }
        },

        async loadMySpecificQuests() {
            try { this.myQuestsList = await this.api('/quests/my-quests'); } catch (e) { }
        },

        async loadParties() {
            try { this.parties = await this.api('/parties/all'); } catch (e) { }
        },

        async loadQuests() {
            try { this.quests = await this.api('/quests/all'); } catch (e) { }
        },

        async loadUsers() {
            try { this.users = await this.api('/user/all'); } catch (e) { }
        },

        async loadMe() {
            try {
                const me = await this.api('/user/me');
                // Só atualiza se mudou algo crítico para evitar re-render desnecessário
                if (JSON.stringify(this.user) !== JSON.stringify(me)) {
                    this.user = me;
                    localStorage.setItem('quest_user', JSON.stringify(this.user));
                }
            } catch (e) { }
        },

        // Party Actions
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
                this.showToast('Failed to create party: ' + err.message, 'error');
            }
        },

        async joinParty(id) {
            try {
                this.loading = true;
                await this.api(`/parties/join/${id}`, { method: 'POST' });
                await this.loadMe();
                await this.loadParties();
                this.showParties = false;
                this.showToast('Joined successfully! Welcome to the party.', 'success');
            } catch (err) {
                this.showToast('Failed to join party: ' + err.message, 'error');
            } finally {
                this.loading = false;
            }
        },

        async leaveParty() {
            try {
                this.loading = true;
                await this.api('/parties/leave', { method: 'POST' });

                if (this.user) {
                    this.user.currentParty = null;
                    localStorage.setItem('quest_user', JSON.stringify(this.user));
                }

                if (this.currentView === 'quests') {
                    this.currentView = 'tavern';
                }

                await this.loadParties();
                this.showParties = true;
                this.showToast('Left the party.', 'info');
            } catch (err) {
                this.showToast('Error leaving party: ' + err.message, 'error');
            } finally {
                this.loading = false;
            }
        },

        async startPlanning() {
            try {
                await this.api(`/parties/${this.myParty.id}/start-planning`, { method: 'POST' });
                this.showToast('Planning phase started!', 'success');
                await this.loadParties();
            } catch (err) {
                this.showToast('Failed to start planning: ' + err.message, 'error');
            }
        },

        async startExecution() {
            try {
                await this.api(`/parties/${this.myParty.id}/start-execution`, { method: 'POST' });
                this.showToast('Adventure started! Good luck!', 'success');
                await this.loadParties();
            } catch (err) {
                this.showToast('Failed to start adventure: ' + err.message, 'error');
            }
        },

        async disbandParty() {
            try {
                await this.api(`/parties/${this.myParty.id}`, { method: 'DELETE' });
                if (this.user) {
                    this.user.currentParty = null;
                    localStorage.setItem('quest_user', JSON.stringify(this.user));
                }
                this.showToast('Party disbanded.', 'info');
                await this.loadParties();
            } catch (err) {
                this.showToast('Failed to disband party: ' + err.message, 'error');
            }
        },

        // Quest Actions
        async createQuest() {
            try {
                const goldAmount = this.currentReward;

                await this.api('/quests', {
                    method: 'POST',
                    body: JSON.stringify({
                        title: this.questForm.title,
                        description: this.questForm.description,
                        rarity: this.questForm.rarity,
                        goldReward: goldAmount
                    })
                });

                this.showToast(`Quest posted! Reward: ${goldAmount} Gold`, 'success');
                this.questForm = { title: '', description: '', rarity: 'COMMON', gold: 0 };

                await this.loadMySpecificQuests();
                await this.loadReviewQuests();
            } catch (err) {
                this.showToast('Failed to create quest: ' + err.message, 'error');
            }
        },

        async reviewQuest(id, approved) {
            try {
                await this.api(`/quests/${id}/review`, {
                    method: 'POST',
                    body: JSON.stringify({ approved, feedback: approved ? 'Approved' : 'Rejected' })
                });
                this.showToast(approved ? 'Quest approved!' : 'Quest rejected.', approved ? 'success' : 'info');

                await this.loadReviewQuests();
                await this.loadMySpecificQuests();
            } catch (err) {
                this.showToast('Failed to review quest: ' + err.message, 'error');
            }
        },

        async completeQuest(id) {
            try {
                await this.api(`/quests/${id}/complete`, { method: 'POST' });
                this.showToast('Quest completed! Rewards claimed!', 'success');

                await this.loadMySpecificQuests();
            } catch (err) {
                this.showToast('Failed to complete quest: ' + err.message, 'error');
            }
        },

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
                console.error(err);
                this.showToast('Login failed.', 'error');
            } finally {
                this.loading = false;
            }
        },

        async startReviewPhase() {
            try {
                this.loading = true;
                await this.api(`/parties/${this.myParty.id}/start-review`, { method: 'POST' });
                this.showToast('Missão Cumprida! Iniciando a fase de Revisão.', 'success');
                await this.loadParties();
            } catch (err) {
                this.showToast('Erro ao iniciar revisão: ' + err.message, 'error');
            } finally {
                this.loading = false;
            }
        },

        async resetLobby() {
            try {
                this.loading = true;
                await this.api(`/parties/${this.myParty.id}/reset-lobby`, { method: 'POST' });

                await this.loadParties();
                await this.loadMySpecificQuests();
                await this.loadReviewQuests();

                this.showToast('Guilda resetada! Prontos para a próxima sprint.', 'success');
            } catch (err) {
                this.showToast('Erro ao resetar: ' + err.message, 'error');
            } finally {
                this.loading = false;
            }
        }
    };
}