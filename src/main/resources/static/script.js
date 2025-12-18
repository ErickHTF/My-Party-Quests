const API_BASE = 'http://localhost:8080';

function questParty() {
    return {
        // State
        user: null,
        token: null,
        loading: false,
        showParties: true,
        toasts: [],

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
        myParty: null,

        // Computed
        get isPartyOwner() {
            if (!this.myParty || !this.user) return false;

            // CORREÇÃO: Verifica dentro do objeto 'owner' ou 'ownerId' direto
            const ownerId = this.myParty.owner ? this.myParty.owner.id : this.myParty.ownerId;
            return ownerId === this.user.id;
        },

        // Initialize
        init() {
            // Tenta ler com o prefixo novo 'quest_'
            const savedToken = localStorage.getItem('quest_token');
            const savedUser = localStorage.getItem('quest_user');

            if (savedToken && savedUser) {
                // Se achou, carrega para a memória
                this.token = savedToken;
                this.user = JSON.parse(savedUser);

                // Define o header padrão para as próximas chamadas
                // (Isso é crucial! Sem isso, o loadData falha e desloga você)
                this.loadData().catch(err => {
                    console.error("Sessão expirada ou erro ao carregar:", err);
                    this.logout(); // Se der erro ao carregar, limpa tudo
                });
            }
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
                return text ? JSON.parse(text) : null;
            } catch (err) {
                console.error('API Error:', err);
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
            localStorage.removeItem('token');
            localStorage.removeItem('user');
            this.showToast('Farewell, adventurer!', 'info');
        },

        // Load Data
        async loadData() {
            await Promise.all([
                this.loadParties(),
                this.loadQuests(),
                this.loadUsers(),
            ]);
        },

        async loadParties() {
            try {
                this.parties = await this.api('/parties/all');
            } catch (err) {
                console.error('Failed to load parties:', err);
                this.parties = [];
            }
        },

        async loadQuests() {
            try {
                this.quests = await this.api('/quests/all');
            } catch (err) {
                console.error('Failed to load quests:', err);
                this.quests = [];
            }
        },

        async loadUsers() {
            try {
                this.users = await this.api('/user/all');
            } catch (err) {
                console.error('Failed to load users:', err);
                this.users = [];
            }
        },

        async loadMyParty() {
            try {
                // Try to find user's party from the parties list
                const userParty = this.parties.find(p =>
                    p.members?.some(m => m.id === this.user?.id)
                );
                this.myParty = userParty || null;
            } catch (err) {
                console.error('Failed to load my party:', err);
                this.myParty = null;
            }
        },

        // Party Actions
        async createParty() {
            try {
                await this.api('/parties', {
                    method: 'POST',
                    // AQUI ESTÁ A CORREÇÃO:
                    // Mapeamos 'name' -> 'partyName' e 'description' -> 'partyDescription'
                    body: JSON.stringify({
                        partyName: this.partyForm.name,
                        partyDescription: this.partyForm.description
                    })
                });

                this.showToast('Party created successfully!', 'success');
                this.showCreateParty = false;
                this.partyForm = { name: '', description: '' };
                await this.loadParties();

                // Opcional: Já carrega a party nova como "minha party" se o backend já te colocar nela
                // await this.loadMyParty(); // (Isso agora é automático pelo computed, então só carregar parties basta)

            } catch (err) {
                this.showToast('Failed to create party: ' + err.message, 'error');
            }
        },

        async joinParty(id) {
            try {
                this.loading = true; // Mostra loading
                await this.api(`/parties/join/${id}`, { method: 'POST' });

                alert('Joined successfully! Reloading...'); // Alerta simples (opcional)

                // FORÇA O RECARREGAMENTO DA PÁGINA
                // Isso resolve qualquer problema de cache ou atraso de atualização
                window.location.reload();

            } catch (err) {
                this.showToast('Failed to join party: ' + err.message, 'error');
                this.loading = false;
            }
        },

        async leaveParty() {
            try {
                await this.api('/parties/leave', { method: 'POST' });
                this.showToast('Left the party.', 'info');
                await this.loadParties();
                this.myParty = null;
            } catch (err) {
                this.showToast('Failed to leave party: ' + err.message, 'error');
            }
        },

        async startPlanning() {
            try {
                await this.api(`/parties/${this.myParty.id}/start-planning`, { method: 'POST' });
                this.showToast('Planning phase started!', 'success');
                await this.loadParties();
                await this.loadMyParty();
            } catch (err) {
                this.showToast('Failed to start planning: ' + err.message, 'error');
            }
        },

        async startExecution() {
            try {
                await this.api(`/parties/${this.myParty.id}/start-execution`, { method: 'POST' });
                this.showToast('Adventure started! Good luck!', 'success');
                await this.loadParties();
                await this.loadMyParty();
            } catch (err) {
                this.showToast('Failed to start adventure: ' + err.message, 'error');
            }
        },

        async disbandParty() {
            if (!confirm('Are you sure you want to disband the party?')) return;
            try {
                await this.api(`/parties/${this.myParty.id}`, { method: 'DELETE' });
                this.showToast('Party disbanded.', 'info');
                this.myParty = null;
                await this.loadParties();
            } catch (err) {
                this.showToast('Failed to disband party: ' + err.message, 'error');
            }
        },

        // Quest Actions
        async createQuest() {
            try {
                // Aqui usamos o 'currentReward' que calculamos
                const goldAmount = this.currentReward;

                await this.api('/quests', {
                    method: 'POST',
                    body: JSON.stringify({
                        title: this.questForm.title,
                        description: this.questForm.description,
                        rarity: this.questForm.rarity,
                        goldReward: goldAmount // Envia o valor certo pro banco
                    })
                });

                this.showToast(`Quest posted! Reward: ${goldAmount} Gold`, 'success');

                // Limpa o formulário
                this.questForm = { title: '', description: '', rarity: 'COMMON', gold: 0 };

                await this.loadQuests();
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
                await this.loadQuests();
            } catch (err) {
                this.showToast('Failed to review quest: ' + err.message, 'error');
            }
        },

        async completeQuest(id) {
            try {
                await this.api(`/quests/${id}/complete`, { method: 'POST' });
                this.showToast('Quest completed! Rewards claimed!', 'success');
                await this.loadQuests();
            } catch (err) {
                this.showToast('Failed to complete quest: ' + err.message, 'error');
            }
        },

        // 1. Crie esta função nova para buscar SÓ os seus dados
        async loadMe() {
            try {
                // Chama o endpoint novo que criamos no Java
                const me = await this.api('/user/me');

                // Atualiza os dados na memória e no navegador
                this.user = me;
                localStorage.setItem('quest_user', JSON.stringify(this.user));

            } catch (e) {
                console.error("Erro ao carregar perfil", e);
            }
        },

        get myParty() {
            // Se não tem user ou a lista ainda não carregou, retorna null
            if (!this.user || !this.parties.length) return null;

            return this.parties.find(p =>
                // Verifica se sou o Dono (pelo objeto owner)
                (p.owner && p.owner.id === this.user.id) ||
                // Ou se sou Membro
                (p.members && p.members.some(m => m.id === this.user.id))
            );
        },

// 2. Atualize o login para chamar o loadMe()
        async login() {
            this.loading = true;
            try {
                // 1. Faz o login e pega o token
                const data = await this.api('/auth/login', {
                    method: 'POST',
                    body: JSON.stringify(this.authForm)
                });

                // 2. Salva APENAS o token agora
                this.token = data.token || data;
                localStorage.setItem('quest_token', this.token);

                // 3. Carrega os dados do usuário (O loadMe já salva o 'quest_user' sozinho)
                await this.loadMe();

                this.showToast(`Welcome back, ${this.user.nickname}!`, 'success');
                this.authForm = { username: '', password: '', nickname: '' };

                // 4. Carrega o resto do jogo
                this.loadData();
            } catch (err) {
                console.error(err);
                this.showToast('Login failed.', 'error');
            } finally {
                this.loading = false;
            }
        },

    };
}