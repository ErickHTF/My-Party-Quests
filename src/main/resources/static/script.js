const API_BASE = 'http://localhost:8080';

function questParty() {
    return {
        // --- STATE ---
        user: null,
        token: null,
        loading: false,
        showParties: false,
        toasts: [],


        headerImages: ['1.png', '2.png'],
        currentHeaderImage: 0,
        headerInterval: null,

        // MODAIS
        showEditModal: false,
        showReviewModal: false,
        reviewTarget: null,
        showConfirmModal: false, // Controla se o modal aparece
        confirmMessage: '',      // O texto da pergunta
        pendingAction: null,     // Guarda a função que vai rodar se clicar em "SIM"
        editForm: { id: null, title: '', description: '', rarity: 'COMMON', feedback: '' },
        reviewForm: { feedback: '' },

        // DADOS
        myQuestsList: [],
        reviewQuestsList: [],
        partyPendingQuests: [],
        parties: [],
        users: [],

        // NAV
        currentView: 'tavern',
        authMode: 'login',
        showCreateParty: false,

        // FORMS
        authForm: { username: '', password: '', nickname: '' },
        partyForm: { name: '', description: '', isPrivate: false, maxMembers: 10 },
        questForm: { title: '', description: '', rarity: 'COMMON' },

        // CONFIGS DE RECOMPENSA (Visual apenas, o real é no Java)
        rewardsMap: {
            'COMMON':    { gold: 5,  xp: 50 },
            'RARE':      { gold: 10, xp: 150 },
            'EPIC':      { gold: 15, xp: 500 },
            'LEGENDARY': { gold: 20, xp: 1500 }
        },

        statusLabels: {
            'LOBBY':      'At the Tavern',
            'PLANNING':   'Getting Ready',
            'EXECUTION':  'On a Quest',
            'REVIEW':     'Returned'
        },

        askConfirm(message, actionCallback) {
            this.confirmMessage = message;
            this.pendingAction = actionCallback; // Guarda a função para depois
            this.showConfirmModal = true;        // Abre o modal
        },

        executeConfirmedAction() {
            if (this.pendingAction) {
                this.pendingAction(); // Executa a função guardada
            }
            this.showConfirmModal = false; // Fecha o modal
            this.pendingAction = null;     // Limpa a ação
        },

        // --- COMPUTED PROPERTIES (Lógica Visual) ---

        getPartyStatusLabel(status) {
            return this.statusLabels[status] || status || 'Unknown';
        },

        // Helpers para formulário
        get currentGoldReward() { return this.rewardsMap[this.questForm.rarity]?.gold || 0; },
        get currentXpReward() { return this.rewardsMap[this.questForm.rarity]?.xp || 0; },

        // Dados do Usuário
        get userLevel() { return (this.user && this.user.level) ? this.user.level : 1; },
        get nextLevelXp() { return (this.user && this.user.nextLevelXp) ? this.user.nextLevelXp : 100; },

        // Porcentagem REAL (Vem do Backend)
        get xpPercentage() {
            return (this.user && this.user.progressPercentage) ? this.user.progressPercentage : 0;
        },

        // XP PENDENTE (Soma visual do que vai entrar)
        get pendingXp() {
            if (!this.myQuestsList || this.myQuestsList.length === 0) return 0;
            return this.myQuestsList
                // Filtra quests que já foram aprovadas ou completadas mas ainda não pagas
                .filter(q => (q.status === 'APPROVED' || q.status === 'COMPLETED') && !q.rewardClaimed)
                .reduce((total, q) => total + (this.rewardsMap[q.rarity]?.xp || 0), 0);
        },

        // GOLD PENDENTE
        get pendingRewardsGold() {
            if (!this.myQuestsList || this.myQuestsList.length === 0) return 0;
            return this.myQuestsList
                .filter(q => (q.status === 'APPROVED' || q.status === 'COMPLETED') && !q.rewardClaimed)
                .reduce((total, q) => total + (this.rewardsMap[q.rarity]?.gold || 0), 0);
        },

        // GHOST BAR (Projeção Visual na Barra)
        get projectedXpPercentage() {
            if (!this.user || !this.user.nextLevelXp) return 0;

            // 1. Quanto falta de XP real para upar?
            const xpNeeded = this.user.nextLevelXp - (this.user.xp || 0);

            // Se já tiver XP suficiente para upar, enche a barra visualmente
            if (xpNeeded <= 0) return 100;

            // 2. Quanto espaço visual (em %) está vazio na barra?
            const emptySpace = 100 - this.xpPercentage;

            // 3. Regra de 3: Quanto a pendência preenche do espaço vazio?
            const visualGain = (this.pendingXp / xpNeeded) * emptySpace;

            return Math.min(100, this.xpPercentage + visualGain);
        },

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

        get uniquePendingMembers() {
            if (!this.myParty || !this.myParty.pendingMembers) return [];
            const seen = new Set();
            return this.myParty.pendingMembers.filter(member => {
                const isDuplicate = seen.has(member.id);
                seen.add(member.id);
                return !isDuplicate;
            });
        },

        // --- CORE ---
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

            this.startHeaderRotation();

            // Polling Lento (Listas Globais - 2s)
            setInterval(() => {
                if (this.token) {
                    this.loadParties();
                    this.loadUsers();
                }
            }, 2000);

            setInterval(() => {
                if (this.token && this.user) {
                    // 1. Dados do Usuário (XP, Gold, Level)
                    this.loadMe();

                    // 2. Dados da Party (Para mudar de fase PLANNING -> EXECUTION sozinho)
                    this.loadParties();

                    // 3. Listas de Quests (Para aparecerem sem F5)
                    this.loadMySpecificQuests();
                    this.loadReviewQuests();       // <--- FALTAVA ISSO AQUI
                    this.fetchPartyPendingQuests();

                    // 4. Guilda (Ranking)
                    this.loadUsers();
                }
            }, 1000);
        },

        startHeaderRotation() {
            if (this.headerInterval) return;
            this.headerInterval = setInterval(() => {
                this.currentHeaderImage = (this.currentHeaderImage + 1) % this.headerImages.length;
            }, 10000);
        },

        isPending(party) {
            if (!this.user || !party || !party.pendingMembers) return false;
            return party.pendingMembers.some(m => m.id === this.user.id);
        },

        // --- API CALLS ---
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

                const text = await res.text();
                let data = null;
                try { data = text ? JSON.parse(text) : null; } catch (e) { data = text; }

                if (!res.ok) {
                    const errorMessage = (data && data.message) ? data.message : (typeof data === 'string' ? data : `HTTP ${res.status}`);
                    throw new Error(errorMessage);
                }
                return data;
            } catch (err) {
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
            } catch (err) { this.showToast('Login failed.', 'error'); }
            finally { this.loading = false; }
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
                this.showToast('Success! Please login.', 'success');
                this.authMode = 'login';
                this.authForm = { username: '', password: '', nickname: '' };
            } catch (err) { this.showToast('Error: ' + err.message, 'error'); }
            finally { this.loading = false; }
        },

        logout() {
            this.user = null;
            this.token = null;
            localStorage.removeItem('quest_token');
            localStorage.removeItem('quest_user');
            this.showToast('Farewell!', 'info');
        },

        // --- LOADS ---
        async loadData() {
            await Promise.all([
                this.loadParties(),
                this.loadUsers(),
                this.loadMySpecificQuests(),
                this.loadReviewQuests(),
                this.fetchPartyPendingQuests(),
                this.loadMe()
            ]);
        },

        async loadMe() {
            try {
                const me = await this.api('/user/me');
                if (this.user && JSON.stringify(this.user) !== JSON.stringify(me)) {
                    this.user = me;
                    localStorage.setItem('quest_user', JSON.stringify(this.user));
                } else if (!this.user) {
                    this.user = me;
                }
            } catch (e) { }
        },

        async loadReviewQuests() { try { this.reviewQuestsList = await this.api('/quests/to-review'); } catch (e) { } },
        async loadMySpecificQuests() { try { this.myQuestsList = await this.api('/quests/my-quests'); } catch (e) { } },
        async loadParties() { try { this.parties = await this.api('/parties/all'); } catch (e) { } },

        async loadUsers() {
            try {
                const users = await this.api('/user/all');
                // ORDENAÇÃO DE GUILDA: Maior Nível primeiro, depois Maior XP
                this.users = users.sort((a, b) => {
                    if ((b.level || 1) !== (a.level || 1)) {
                        return (b.level || 1) - (a.level || 1);
                    }
                    return (b.xp || 0) - (a.xp || 0);
                });
            } catch (e) { }
        },

        async fetchPartyPendingQuests() {
            if (!this.user || !this.isPartyOwner) return;
            try { this.partyPendingQuests = await this.api('/quests/party-pending'); } catch (e) { }
        },

        // --- GAME ACTIONS ---
        async createParty() {
            if (!this.partyForm.name) { this.showToast('Name required', 'error'); return; }
            try {
                this.loading = true;
                await this.api('/parties', {
                    method: 'POST',
                    body: JSON.stringify({
                        partyName: this.partyForm.name,
                        partyDescription: this.partyForm.description,
                        isPrivate: this.partyForm.isPrivate,
                        maxMembers: parseInt(this.partyForm.maxMembers) || 10
                    })
                });
                this.showToast('Party created!', 'success');
                this.showCreateParty = false;
                this.partyForm = { name: '', description: '', isPrivate: false, maxMembers: 10 };
                await this.loadMe();
                await this.loadParties();
            } catch (error) { this.showToast(error.message, 'error'); await this.loadMe(); }
            finally { this.loading = false; }
        },

        async joinParty(id) {
            try {
                this.loading = true;
                const targetParty = this.parties.find(p => p.id === id);
                const isPrivate = targetParty ? (targetParty.isPrivate || targetParty.private) : false;
                await this.api(`/parties/join/${id}`, { method: 'POST' });
                await this.loadMe();
                await this.loadParties();
                if (isPrivate) this.showToast('Request sent!', 'info');
                else this.showToast('Joined party!', 'success');
            } catch (err) { this.showToast(err.message, 'error'); }
            finally { this.loading = false; }
        },

        async leaveParty() {
            this.askConfirm("Are you sure you want to leave the party?", async () => {
                try {
                    this.loading = true;
                    await this.api('/parties/leave', { method: 'POST' });
                    this.user.currentParty = null;
                    await this.loadParties();
                    this.showParties = true;
                    this.showToast('Left party.', 'info');
                } catch (err) { this.showToast(err.message, 'error'); }
                finally { this.loading = false; }
            });
        },

        async approveMember(userId) {
            if (!this.myParty) return;
            try {
                await this.api(`/parties/${this.myParty.id}/approve/${userId}`, { method: 'POST' });
                this.showToast('Member approved!', 'success');
                await this.loadMe();
            } catch (err) { this.showToast(err.message, 'error'); }
        },

        async rejectMember(userId) {
            if (!this.myParty) return;
            try {
                await this.api(`/parties/${this.myParty.id}/reject/${userId}`, { method: 'POST' });
                this.showToast('Member rejected.', 'info');
                await this.loadMe();
            } catch (err) { this.showToast(err.message, 'error'); }
        },

        async startPlanning() {
            this.askConfirm("Start Planning Phase? This will close the lobby for new members.", async () => {
                try {
                    await this.api(`/parties/${this.myParty.id}/start-planning`, { method: 'POST' });
                    await this.loadParties();
                } catch (err) { this.showToast(err.message, 'error'); }
            });
        },

        async startExecution() {
            this.askConfirm("Start Adventure? Make sure all quests are written and approved!", async () => {
                try {
                    await this.api(`/parties/${this.myParty.id}/start-execution`, { method: 'POST' });
                    await this.loadParties();
                } catch (err) { this.showToast(err.message, 'error'); }
            });
        },

        async startReviewPhase() {
            this.askConfirm("Finish Quests? Only quests marked as 'COMPLETED' will be paid. This cannot be undone.", async () => {
                try {
                    this.loading = true;
                    await this.api(`/parties/${this.myParty.id}/start-review`, { method: 'POST' });
                    await this.loadParties();
                } catch (err) { this.showToast(err.message, 'error'); }
                finally { this.loading = false; }
            });
        },

        async resetLobby() {
            this.askConfirm("Reset to Lobby? This will DELETE all current quests and restart the cycle.", async () => {
                try {
                    this.loading = true;
                    await this.api(`/parties/${this.myParty.id}/reset-lobby`, { method: 'POST' });
                    await this.loadParties();
                } catch (err) { this.showToast(err.message, 'error'); }
                finally { this.loading = false; }
            });
        },

        async createQuest() {
            try {
                await this.api('/quests', {
                    method: 'POST',
                    body: JSON.stringify({
                        title: this.questForm.title,
                        description: this.questForm.description,
                        rarity: this.questForm.rarity,
                    })
                });
                this.showToast(`Quest posted!`, 'success');
                this.questForm.title = '';
                this.questForm.description = '';
                await this.loadMySpecificQuests();
                await this.loadReviewQuests();
            } catch (err) { this.showToast(err.message, 'error'); }
        },

        openEditQuest(quest) {
            this.editForm = { id: quest.id, title: quest.title, description: quest.description, rarity: quest.rarity, feedback: quest.reviewerFeedback || 'Fix details.' };
            this.showEditModal = true;
        },

        async updateQuest() {
            try {
                await this.api(`/quests/${this.editForm.id}`, { method: 'PUT', body: JSON.stringify({ title: this.editForm.title, description: this.editForm.description, rarity: this.editForm.rarity }) });
                this.showToast('Quest updated!', 'success');
                this.showEditModal = false;
                this.loadData();
            } catch (err) { this.showToast(err.message, 'error'); }
        },

        openReviewModal(quest) {
            this.reviewTarget = quest;
            this.reviewForm.feedback = '';
            this.showReviewModal = true;
        },

        async submitReview(approved) {
            try {
                await this.api(`/quests/${this.reviewTarget.id}/review`, { method: 'POST', body: JSON.stringify({ approved, feedback: this.reviewForm.feedback || (approved ? 'Approved!' : 'Rejected.') }) });
                this.showToast(approved ? 'Approved!' : 'Rejected!', 'info');
                this.showReviewModal = false;
                this.loadData();
            } catch (err) { this.showToast(err.message, 'error'); }
        },

        async completeQuest(id) {
            try {
                await this.api(`/quests/${id}/complete`, { method: 'POST' });
                this.showToast('Quest Completed!', 'success');
                await this.loadMySpecificQuests();
            } catch (err) { this.showToast(err.message, 'error'); }
        },

        async disbandParty() {
            this.askConfirm("DISBAND PARTY? This action is permanent!", async () => {
                try {
                    await this.api(`/parties/${this.myParty.id}`, { method: 'DELETE' });
                    await this.loadParties();
                } catch (err) {
                    this.showToast(err.message, 'error');
                }
            });
        },

        async kickMember(memberId, memberName) {
            if (!this.myParty) return;

            this.askConfirm(`Kick ${memberName}? They will be kicked immediately.`, async () => {
                try {
                    await this.api(`/parties/${this.myParty.id}/kick/${memberId}`, { method: 'POST' });

                    // --- MUDANÇA AQUI: O '0' no final torna a mensagem persistente ---
                    this.showToast(` ${memberName} has been kicked from the party!`, 'success', 0);

                    await this.loadParties();
                    await this.loadMe();
                } catch (err) {
                    this.showToast(err.message, 'error');
                }
            });
        },


        showToast(message, type = 'info', duration = 3000) {
            const id = Date.now() + Math.random(); // ID único
            this.toasts.push({ id, message, type });

            // Só configura o sumiço automático se a duração for maior que 0
            if (duration > 0) {
                setTimeout(() => {
                    this.removeToast(id);
                }, duration);
            }
        },

        removeToast(id) {
            this.toasts = this.toasts.filter(t => t.id !== id);
        },

        get sortedMembers() {
            if (!this.myParty || !this.myParty.members) return [];

            const unique = this.myParty.members.filter((item, index, self) =>
                index === self.findIndex(t => t.id === item.id)
            );

            if (this.myParty.owner) {
                return unique.sort((a, b) => {
                    if (a.id === this.myParty.owner.id) return -1;
                    if (b.id === this.myParty.owner.id) return 1;
                    return 0; // Mantém a ordem dos outros
                });
            }

            return unique;
        },

    };
}