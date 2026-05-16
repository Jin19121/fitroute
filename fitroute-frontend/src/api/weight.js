// src/api/weight.js
import apiClient from './axios';

export const logTodayWeight = (payload) =>
    apiClient.post('/api/weight/today', payload).then(r => r.data);

export const getTodayWeight = () =>
    apiClient.get('/api/weight/today').then(r => r.data).catch(err => {
        if (err.response?.status === 204) return null;
        throw err;
    });