// src/hooks/useWorkoutToday.js
import { useEffect } from "react";
import { usePlanStore } from "../store/planStore";

/**
 * 오늘 운동 데이터 + 액션 제공
 * planStore를 그대로 사용하여 대시보드와 동기화
 */
export function useWorkoutToday() {
    const todayData = usePlanStore((state) => state.todayData);
    const fetchToday = usePlanStore((state) => state.fetchToday);
    const applyAction = usePlanStore((state) => state.applyAction);

    // 페이지 마운트 시 데이터 로드
    useEffect(() => {
        fetchToday();
    }, [fetchToday]);

    // 페이지에 돌아올 때 재조회 (대시보드와 동기화)
    useEffect(() => {
        const onVisible = () => {
            if (document.visibilityState === "visible") {
                fetchToday(true); // force=true로 캐시 무시
            }
        };
        document.addEventListener("visibilitychange", onVisible);
        return () => document.removeEventListener("visibilitychange", onVisible);
    }, [fetchToday]);

    const workouts = todayData?.today?.workouts ?? [];

    // 카테고리별로 그룹화
    const groupedByCategory = workouts.reduce((acc, wo) => {
        const cat = wo.category ?? "CARDIO";
        if (!acc[cat]) acc[cat] = [];
        acc[cat].push(wo);
        return acc;
    }, {});

    const categoryOrder = ["CHEST", "BACK", "LEGS", "SHOULDERS", "ARMS", "CORE", "CARDIO", "REST"];
    const sortedCategories = categoryOrder.filter((cat) => groupedByCategory[cat]);

    return {
        workouts,
        groupedByCategory,
        sortedCategories,
        loading: !todayData,
        applyAction,
        reload: () => fetchToday(true),
    };
}