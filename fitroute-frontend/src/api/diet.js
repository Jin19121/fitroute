// api/diet.js
import apiClient from './axios';

export const getTodayPlan = () => apiClient.get('/api/dashboard/today').then(r => r.data);
export const patchPlanItem = (itemId, body) =>
    apiClient.patch(`/api/plans/items/${itemId}/action`, body).then(r => r.data);

// export const patchLogItem = (id, body) => apiClient.patch(`/api/log-items/${id}`, body).then(r => r.data);
// export const getCalendar = (params) => apiClient.get('/api/calendar', { params }).then(r => r.data);
// export const getWeeklyPlan = () => apiClient.get('/api/diet/plan/weekly').then(r => r.data);

// 아키텍처 문서 기준 엔드포인트
// export const getDayDetail = (date, type) => axios.get(`/api/plans/date/${date}`, { params: { type } }).then(r => r.data);