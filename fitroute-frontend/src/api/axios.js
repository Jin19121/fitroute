// src/api/axios.js
import axios from 'axios';

const BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080';

// ── Token storage keys (single source of truth) ─────────────────────────────
const ACCESS_TOKEN_KEY = 'fitroute_at';
const REFRESH_TOKEN_KEY = 'fitroute_rt';

export const tokenStorage = {
    getAccess: () => sessionStorage.getItem(ACCESS_TOKEN_KEY),
    getRefresh: () => localStorage.getItem(REFRESH_TOKEN_KEY),
    setTokens: (at, rt) => {
        // Access token: sessionStorage (tab-scoped, gone on browser close)
        sessionStorage.setItem(ACCESS_TOKEN_KEY, at);
        // Refresh token: localStorage (survives tab close, cleared on logout)
        localStorage.setItem(REFRESH_TOKEN_KEY, rt);
    },
    clearTokens: () => {
        sessionStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
    },
};

// ── Axios instance ───────────────────────────────────────────────────────────
const apiClient = axios.create({
    baseURL: BASE_URL,
    timeout: 10_000,
    headers: { 'Content-Type': 'application/json' },
});

// ── Request interceptor: attach access token ─────────────────────────────────
apiClient.interceptors.request.use(
    (config) => {
        const token = tokenStorage.getAccess();
        if (token) {
            config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
    },
    (error) => Promise.reject(error),
);

// ── Silent refresh state (prevent concurrent refresh storms) ─────────────────
let isRefreshing = false;
let pendingQueue = []; // [{resolve, reject}]

const processQueue = (error, token = null) => {
    pendingQueue.forEach(({ resolve, reject }) => {
        if (error) reject(error);
        else resolve(token);
    });
    pendingQueue = [];
};

// ── Response interceptor: handle 401 → silent refresh ────────────────────────
apiClient.interceptors.response.use(
    (response) => response,
    async (error) => {
        const original = error.config;

        // Only attempt refresh on 401, only once per request
        if (
            error.response?.status === 401 &&
            !original._retried &&
            original.url !== '/api/auth/refresh'
        ) {
            original._retried = true;

            if (isRefreshing) {
                // Queue callers while refresh is in-flight
                return new Promise((resolve, reject) => {
                    pendingQueue.push({ resolve, reject });
                })
                    .then((token) => {
                        original.headers.Authorization = `Bearer ${token}`;
                        return apiClient(original);
                    })
                    .catch((err) => Promise.reject(err));
            }

            isRefreshing = true;

            try {
                const refreshToken = tokenStorage.getRefresh();
                if (!refreshToken) {
                    throw new Error('No refresh token');
                }

                // Call refresh without going through the interceptor again
                const { data } = await axios.post(
                    `${BASE_URL}/api/auth/refresh`,
                    { refreshToken },
                    { headers: { 'Content-Type': 'application/json' } },
                );

                const { accessToken, refreshToken: newRt } = data;
                tokenStorage.setTokens(accessToken, newRt);

                // Notify zustand store via custom event (avoids circular import)
                window.dispatchEvent(
                    new CustomEvent('fitroute:token-refreshed', { detail: { accessToken } }),
                );

                processQueue(null, accessToken);
                original.headers.Authorization = `Bearer ${accessToken}`;
                return apiClient(original);
            } catch (refreshError) {
                processQueue(refreshError, null);
                tokenStorage.clearTokens();
                window.dispatchEvent(new CustomEvent('fitroute:session-expired'));
                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }

        return Promise.reject(error);
    },
);

export default apiClient;