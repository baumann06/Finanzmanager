import { createRouter, createWebHistory } from 'vue-router';

import Dashboard from '../views/Dashboard.vue';
import Finance from '../views/Finance.vue';
import Crypto from '../views/Crypto.vue';
import CryptoDetail from '../views/CryptoDetail.vue';

const routes = [
    {
        path: '/',
        name: 'Dashboard',
        component: Dashboard
    },
    {
        path: '/finance',
        name: 'Finance',
        component: Finance
    },
    {
        path: '/crypto',
        name: 'Crypto',
        component: Crypto
    },
    {
        path: '/crypto/:id',
        name: 'CryptoDetail',
        component: CryptoDetail,
        props: true
    }
];

const router = createRouter({
    history: createWebHistory(),
    routes
});

export default router;