import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import FinanceView from '../views/FinanceView.vue';
import CryptoView from '../views/CryptoView.vue';

const routes = [
    {
        path: '/',
        name: 'home',
        component: HomeView,
        meta: { title: 'Dashboard' }
    },
    {
        path: '/finance',
        name: 'finance',
        component: FinanceView,
        meta: { title: 'Finanzen' }
    },
    {
        path: '/crypto',
        name: 'crypto',
        component: CryptoView,
        meta: { title: 'Kryptowährungen' }
    }
];

const router = createRouter({
    history: createWebHistory(process.env.BASE_URL),
    routes
});

// Titel für die Seite setzen
router.beforeEach((to, from, next) => {
    document.title = to.meta.title ? `${to.meta.title} | FinCrypto` : 'FinCrypto Dashboard';
    next();
});

export default router;