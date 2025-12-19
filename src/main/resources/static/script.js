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

        // Dentro do objeto questParty() ...

        async approveMember(userId) {
            if (!this.myParty) return;

            try {
                // Chama o endpoint que acabamos de criar
                await this.api(`/parties/${this.myParty.id}/approve/${userId}`, {
                    method: 'POST'
                });

                this.showToast('Member approved!', 'success');
                await this.loadMe(); // Recarrega para atualizar a lista
            } catch (err) {
                this.showToast('Error approving: ' + err.message, 'error');
            }
        },

        async rejectMember(userId) {
            if (!this.myParty) return;

            try {
                // Chama o endpoint que acabamos de criar
                await this.api(`/parties/${this.myParty.id}/reject/${userId}`, {
                    method: 'POST'
                });

                this.showToast('Member rejected.', 'info');
                await this.loadMe(); // Recarrega para atualizar a lista
            } catch (err) {
                this.showToast('Error rejecting: ' + err.message, 'error');
            }
        },

        get uniquePendingMembers() {
            if (!this.myParty || !this.myParty.pendingMembers) return [];

            // Cria um Set para rastrear IDs já vistos
            const seen = new Set();
            return this.myParty.pendingMembers.filter(member => {
                const isDuplicate = seen.has(member.id);
                seen.add(member.id);
                return !isDuplicate;
            });
        },

        isPending(party) {
            // Proteçao contra nulos (caso o usuário ou a lista ainda não tenham carregado)
            if (!this.user || !party || !party.pendingMembers) return false;

            // Verifica se o meu ID está na lista de pendentes
            return party.pendingMembers.some(m => m.id === this.user.id);
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

            partyForm: {
                name: ''
                    description: ''
                    isPrivate: false // <--- Adicione isso aqui para iniciar como Público
                    maxMembers: 10
            }
        },

        // --- API HELPER ---
        // Substitua a função api existente por esta versão melhorada:
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

                // Tenta ler a resposta como texto primeiro
                const text = await res.text();

                // Tenta converter para JSON
                let data = null;
                try {
                    data = text ? JSON.parse(text) : null;
                } catch (e) {
                    data = text;
                }

                // Se deu erro HTTP (400, 500, etc)
                if (!res.ok) {
                    // Se o backend mandou uma mensagem JSON (como no seu log), usa ela
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

                // --- LÓGICA DE NOTIFICAÇÃO (BASEADA EM NOME) ---
                if (this.user) {
                    const oldName = this.user.partyName;
                    const newName = me.partyName;

                    // Definição de "Sem Party": pode ser null, undefined, vazio ou "Freelancer"
                    const wasLoneWolf = !oldName || oldName === 'Lone Wolf';
                    const isNowInParty = newName && newName !== 'Lone Wolf';

                    // Se antes eu era freelancer e agora tenho um nome de party, fui aceito!
                    if (wasLoneWolf && isNowInParty) {
                        this.showToast('⚔️ You have been accepted into the party!', 'success');

                        // Opcional: Efeito sonoro
                        // const audio = new Audio('https://freesound.org/data/previews/341/341695_5858296-lq.mp3');
                        // audio.play().catch(e => {});
                    }
                }
                // -----------------------------------------------

                // Atualiza o usuário se houver mudanças
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
            if (!this.partyForm.name || !this.partyForm.description) {
                this.showToast('Please fill in all fields', 'error');
                return;
            }

            try {
                this.loading = true; // Feedback visual
                await this.api('/parties', {
                    method: 'POST',
                    body: JSON.stringify({
                        partyName: this.partyForm.name,
                        partyDescription: this.partyForm.description,
                        isPrivate: this.partyForm.isPrivate,
                        maxMembers: parseInt(this.partyForm.maxMembers) || 10
                    })
                });

                this.showToast('Party created successfully!', 'success');
                this.showCreateParty = false;
                this.partyForm = { name: '', description: '', isPrivate: false, maxMembers: 10 };

                // Recarrega tudo
                await this.loadMe();
                await this.loadParties();
            } catch (error) {
                // Se der erro, mostra a mensagem limpa
                this.showToast(error.message, 'error');

                // IMPORTANTE: Recarrega o usuário mesmo no erro.
                // Se o erro foi "Você já é líder", isso fará a party antiga aparecer na tela para você dar Disband.
                await this.loadMe();
            } finally {
                this.loading = false;
            }
        },

        async joinParty(id) {
            try {
                this.loading = true;

                // 1. Descobre se a party é privada antes da requisição
                const targetParty = this.parties.find(p => p.id === id);
                const isPrivate = targetParty ? (targetParty.isPrivate || targetParty.private) : false;

                // Faz a requisição
                await this.api(`/parties/join/${id}`, { method: 'POST' });

                // Recarrega dados
                await this.loadMe();
                await this.loadParties();

                // 2. Exibe a mensagem correta baseada no tipo da party
                if (isPrivate) {
                    this.showToast('Request sent! Awaiting approval.', 'info'); // Azul/Neutro
                } else {
                    this.showToast('Joined party successfully!', 'success'); // Verde
                }

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