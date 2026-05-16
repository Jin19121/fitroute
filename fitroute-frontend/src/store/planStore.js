// src/store/planStore.js
import { create } from 'zustand';
import { getTodayPlan, patchPlanItem } from '../api/diet';

export const usePlanStore = create((set, get) => ({
    todayData: null,  // 백엔드 응답 전체 그대로 저장
    loading: false,
    error: null,

    fetchToday: async (force = false) => {
        if (!force && get().todayData) return; // force=true면 캐시 무시
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
        // 낙관적 업데이트 — 수정 필드까지 반영
        const optimisticPatch = { status: payload.status };

        if (payload.status === 'MODIFIED' || payload.status === 'EDITED') {
            if (payload.modifiedName) optimisticPatch.effectiveName = payload.modifiedName;
            if (payload.modifiedCalories) optimisticPatch.effectiveCalories = payload.modifiedCalories;
            if (payload.modifiedProtein) optimisticPatch.protein = payload.modifiedProtein;
            if (payload.modifiedCarbs) optimisticPatch.carbs = payload.modifiedCarbs;
            if (payload.modifiedFat) optimisticPatch.fat = payload.modifiedFat;
        }

        if (payload.status === 'PENDING') {
            // 되돌리기 — effectiveName/effectiveCalories는 유지 (수정 내용 보존)
            // status만 PENDING으로
        }

        get().updatePlanItem(itemId, optimisticPatch);

        try {
            await patchPlanItem(itemId, payload);
        } catch (e) {
            set({ todayData: null });
            get().fetchToday(true);
        }
    },
}));