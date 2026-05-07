// src/store/planStore.js
import { create } from 'zustand';
import { getTodayPlan, patchPlanItem } from '../api/diet';

export const usePlanStore = create((set, get) => ({
    todayData: null,  // 백엔드 응답 전체 그대로 저장
    loading: false,
    error: null,

    fetchToday: async () => {
        if (get().todayData) return;
        set({ loading: true });
        try {
            const data = await getTodayPlan();
            set({ todayData: data, loading: false });
        } catch (e) {
            set({ error: e.message, loading: false });
        }
    },

    // PlanItem 상태 낙관적 업데이트 (백엔드 응답 구조 기준)
    updatePlanItem: (itemId, patch) =>
        set((state) => {
            if (!state.todayData?.today) return state;
            const update = (list) =>
                list.map((item) => item.id === itemId ? { ...item, ...patch } : item);
            return {
                todayData: {
                    ...state.todayData,
                    today: {
                        ...state.todayData.today,
                        meals: update(state.todayData.today.meals),
                        workouts: update(state.todayData.today.workouts),
                    },
                },
            };
        }),

    applyAction: async (itemId, payload) => {
        // 낙관적 업데이트 먼저
        get().updatePlanItem(itemId, { status: payload.status });
        try {
            await patchPlanItem(itemId, payload);
        } catch (e) {
            // 실패 시 재조회로 롤백
            set({ todayData: null });
            get().fetchToday();
        }
    },
}));