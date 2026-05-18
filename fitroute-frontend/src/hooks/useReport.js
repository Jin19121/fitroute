// src/hooks/useReport.js
import { useState, useEffect, useCallback } from 'react';
import { getMonthlyReport, getDailyReport, recordWeight, deleteWeight } from '../api/report';

export default function useReport() {
    const today = new Date();

    const [year, setYear] = useState(today.getFullYear());
    const [month, setMonth] = useState(today.getMonth() + 1);
    const [filter, setFilter] = useState('WORKOUT'); // WORKOUT | DIET | WEIGHT
    const [selectedDate, setSelectedDate] = useState(null);    // 'YYYY-MM-DD' | null

    const [monthlyData, setMonthlyData] = useState(null);
    const [dayDetail, setDayDetail] = useState(null);
    const [loading, setLoading] = useState(false);
    const [detailLoading, setDetailLoading] = useState(false);
    const [error, setError] = useState(null);

    // ─── 월간 데이터 패칭 ──────────────────────────────────────────────────

    const fetchMonthly = useCallback(async () => {
        setLoading(true);
        setError(null);
        try {
            const data = await getMonthlyReport(year, month);
            setMonthlyData(data);
        } catch (e) {
            setError(e.message ?? '리포트를 불러오지 못했어요.');
        } finally {
            setLoading(false);
        }
    }, [year, month]);

    useEffect(() => {
        setSelectedDate(null); // 월 바뀌면 상세 카드 닫기
        setDayDetail(null);
        fetchMonthly();
    }, [fetchMonthly]);

    // ─── 날짜 클릭 — 상세 패칭 ────────────────────────────────────────────

    const selectDate = useCallback(async (dateStr) => {
        // 같은 날짜 재클릭 시 토글 닫기
        if (selectedDate === dateStr) {
            setSelectedDate(null);
            setDayDetail(null);
            return;
        }
        setSelectedDate(dateStr);
        setDetailLoading(true);
        try {
            const data = await getDailyReport(dateStr);
            setDayDetail(data);
        } catch (e) {
            setDayDetail(null);
        } finally {
            setDetailLoading(false);
        }
    }, [selectedDate]);

    // ─── 월 네비게이션 ────────────────────────────────────────────────────

    const prevMonth = useCallback(() => {
        if (month === 1) { setYear(y => y - 1); setMonth(12); }
        else { setMonth(m => m - 1); }
    }, [month]);

    const nextMonth = useCallback(() => {
        const now = new Date();
        // 미래 월로는 이동 불가
        if (year === now.getFullYear() && month === now.getMonth() + 1) return;
        if (month === 12) { setYear(y => y + 1); setMonth(1); }
        else { setMonth(m => m + 1); }
    }, [year, month]);

    const isCurrentMonth = year === today.getFullYear() && month === today.getMonth() + 1;

    // ─── 체중 기록 ────────────────────────────────────────────────────────

    const submitWeight = useCallback(async (logDate, weightKg, bodyFatPct, muscleMass, note = '') => {
        await recordWeight({ logDate, weightKg, bodyFatPct, muscleMass, note });
        await fetchMonthly();
        if (selectedDate === logDate) {
            const data = await getDailyReport(logDate);
            setDayDetail(data);
        }
    }, [fetchMonthly, selectedDate]);

    const removeWeight = useCallback(async (date) => {
        await deleteWeight(date);
        await fetchMonthly();
        if (selectedDate === date) {
            const data = await getDailyReport(date);
            setDayDetail(data);
        }
    }, [fetchMonthly, selectedDate]);

    return {
        // 상태
        year, month, filter, selectedDate,
        monthlyData, dayDetail,
        loading, detailLoading, error,
        isCurrentMonth,
        // 액션
        setFilter,
        selectDate,
        prevMonth,
        nextMonth,
        submitWeight,
        removeWeight,
        refresh: fetchMonthly,
    };
}