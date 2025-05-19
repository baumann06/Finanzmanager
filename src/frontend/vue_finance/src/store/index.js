import { createStore } from 'vuex';
import financeModule from './modules/finance';
import cryptoModule from './modules/crypto';

export default createStore({
    state: {
        loading: false,
        error: null,
        darkMode: localStorage.getItem('darkMode') === 'true' || false
    },
    mutations: {
        SET_LOADING(state, loading) {
            state.loading = loading;
        },
        SET_ERROR(state, error) {
            state.error = error;
        },
        TOGGLE_DARK_MODE(state) {
            state.darkMode = !state.darkMode;
            localStorage.setItem('darkMode', state.darkMode);

            // Dokumentfarben anpassen
            if (state.darkMode) {
                document.documentElement.classList.add('dark');
            } else {
                document.documentElement.classList.remove('dark');
            }
        }
    },
    actions: {
        setLoading({ commit }, loading) {
            commit('SET_LOADING', loading);
        },
        setError({ commit }, error) {
            commit('SET_ERROR', error);
        },
        clearError({ commit }) {
            commit('SET_ERROR', null);
        },
        toggleDarkMode({ commit }) {
            commit('TOGGLE_DARK_MODE');
        }
    },
    modules: {
        finance: financeModule,
        crypto: cryptoModule
    }
});