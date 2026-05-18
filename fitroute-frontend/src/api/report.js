// src/api/report.js
import apiClient from './axios';

/**
 * GET /api/reports/monthly?year=2025&month=6
 * 월간 캘린더 + KPI 요약
 */
export const getMonthlyReport = (year, month) =>
    apiClient
        .get('/api/reports/monthly', { params: { year, month } })
        .then(r => r.data);

/**
 * GET /api/reports/daily/{date}
 * 날짜 클릭 시 상세 카드 데이터
 */
export const getDailyReport = (date) =>
    apiClient
        .get(`/api/reports/daily/${date}`)
        .then(r => r.data);

/**
 * POST /api/weight-logs
 * 체중 기록 (같은 날짜면 upsert)
 * @param {{ measuredAt: string, weightKg: number, note?: string }} payload
 */
export const recordWeight = (payload) =>
    apiClient
        .post('/api/weight-logs', payload)
        .then(r => r.data);

/**
 * DELETE /api/weight-logs/{date}
 * 특정 날짜 체중 삭제
 */
export const deleteWeight = (date) =>
    apiClient
        .delete(`/api/weight-logs/${date}`)
        .then(r => r.data);