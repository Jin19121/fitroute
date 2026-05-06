import apiClient from './axios';

const API = axios.create({ baseURL: 'http://localhost:8080/api' });

// export const planApi = {
//     generate: () => apiClient.post('/api/plans/generate'),

//     getStatus: async () => {
//         const { data } = await apiClient.get('/api/plans/status');
//         return data; // { status: 'GENERATING' | 'ACTIVE' | 'FAILED' }
//     },
// };

API.interceptors.request.use(config => {
    const token = localStorage.getItem('accessToken');
    if (token) config.headers.Authorization = `Bearer ${token}`;
    return config;
});

export const generateTodayPlan = () => API.post('/plans/today/generate');
export const getTodayPlan = () => API.get('/plans/today');