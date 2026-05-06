// fitroute-frontend/src/api/client.js
import axios from 'axios';

const api = axios.create({
    baseURL: '/api', // Vite 프록시 덕분에 상대 경로 가능
});

api.interceptors.request.use((config) => {
    const token = localStorage.getItem('accessToken');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

api.interceptors.response.use(
    (res) => res,
    (err) => {
        if (err.response?.status === 401) {
            // 토큰 만료 → 로그인 페이지로
            localStorage.removeItem('accessToken');
            window.location.href = '/login';
        }
        return Promise.reject(err);
    }
);

export default api;