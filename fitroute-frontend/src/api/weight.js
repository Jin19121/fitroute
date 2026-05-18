// src/api/weight.js
import apiClient from './axios';

export const logTodayWeight = (payload) =>
    apiClient.post('/api/weight-logs', payload).then(r => r.data);

export const getTodayWeight = () =>
    apiClient.get('/api/weight-logs/today').then(r => r.data).catch(err => {
        if (err.response?.status === 204) return null;
        throw err;
    });

// ★ 추가: 가장 최근 체중 조회
export const getLatestWeight = () =>
    apiClient.get('/api/weight-logs/latest').then(r => r.data).catch(() => null);