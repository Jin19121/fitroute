// src/store/planStore.js
import { create } from 'zustand';
import { getTodayPlan, patchPlanItem } from '../api/diet';

export const usePlanStore = create((set, get) => ({
    todayData: null,
    loading: false,
    error: null,

    fetchToday: async (force = false) => {
        if (!force && get().todayData) return;
        set({ loading: true });
        try {
            const data = await getTodayPlan();
            set({ todayData: data, loading: false });
        } catch (e) {
            set({ error: e.message, loading: false });
        }
    },

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

    // ─── 변경: action 기반으로 낙관적 업데이트 및 API 전송 ────────────────
    // payload 예시:
    //   { action: 'COMPLETE' }
    //   { action: 'SKIP' }
    //   { action: 'MODIFY', modifiedName: '...', modifiedCalories: 400 }
    //   { action: 'COMPLETE_WITH_MODIFY', modifiedCalories: 500 }
    //   { action: 'RESET' }
    applyAction: async (itemId, payload) => {
        const { action, modifiedName, modifiedCalories,
            modifiedProtein, modifiedCarbs, modifiedFat,
            modifiedSets, modifiedReps } = payload;

        const fields = {
            modifiedName, modifiedCalories, modifiedProtein,
            modifiedCarbs, modifiedFat, modifiedSets, modifiedReps
        };

        get().updatePlanItem(itemId, buildOptimisticPatch(action, fields));

        try {
            await patchPlanItem(itemId, payload);
        } catch (e) {
            set({ todayData: null });
            get().fetchToday(true);
        }
    },
}));

// ─── 액션별 낙관적 패치 계산 ────────────────────────────────────────────────
function buildOptimisticPatch(action, fields) {
    const { modifiedName, modifiedCalories, modifiedProtein,
        modifiedCarbs, modifiedFat, modifiedSets, modifiedReps } = fields;

    switch (action) {
        case 'COMPLETE':
            return { status: 'COMPLETED' };

        case 'SKIP':
            return { status: 'SKIPPED' };

        case 'MODIFY': {
            // status는 PENDING 유지, isModified=true, 수정값만 반영
            const patch = { isModified: true };
            if (modifiedName) patch.effectiveName = modifiedName;
            if (modifiedCalories) patch.effectiveCalories = modifiedCalories;
            if (modifiedProtein) patch.protein = modifiedProtein;
            if (modifiedCarbs) patch.carbs = modifiedCarbs;
            if (modifiedFat) patch.fat = modifiedFat;
            if (modifiedSets) patch.sets = modifiedSets;
            if (modifiedReps) patch.reps = modifiedReps;
            return patch;
        }

        case 'COMPLETE_WITH_MODIFY': {
            // 수정 + 완수
            const patch = { status: 'COMPLETED', isModified: true };
            if (modifiedName) patch.effectiveName = modifiedName;
            if (modifiedCalories) patch.effectiveCalories = modifiedCalories;
            if (modifiedProtein) patch.protein = modifiedProtein;
            if (modifiedCarbs) patch.carbs = modifiedCarbs;
            if (modifiedFat) patch.fat = modifiedFat;
            if (modifiedSets) patch.sets = modifiedSets;
            if (modifiedReps) patch.reps = modifiedReps;
            return patch;
        }

        case 'RESET':
            return {
                status: 'PENDING',
                isModified: false,
                effectiveName: null,
                effectiveCalories: null,
            };

        default:
            return {};
    }
}