// src/store/planStore.js
import { create } from 'zustand';
import { getTodayPlan, patchPlanItem } from '../api/diet';

// ─── action → 낙관적 상태 매핑 ───────────────────────────────────────────
// 서버의 action 값(COMPLETE, SKIP, RESET, MODIFY, COMPLETE_WITH_MODIFY)을
// UI에서 즉시 반영할 status로 변환한다.
function buildOptimisticPatch(item, payload) {
    const { action } = payload;

    // 기본 공통 필드
    const base = {
        effectiveName: item.effectiveName ?? item.foodName ?? item.exerciseName,
        effectiveCalories: item.effectiveCalories ?? item.calories,
        isModified: item.isModified ?? false,
    };

    switch (action) {
        case 'COMPLETE':
            return { ...base, status: 'COMPLETED', isModified: false };

        case 'SKIP':
            return { ...base, status: 'SKIPPED', isModified: false };

        case 'RESET':
            return {
                status: 'PENDING',
                effectiveName: item.foodName ?? item.exerciseName,
                effectiveCalories: item.calories,
                isModified: false,
            };

        case 'MODIFY':
            return {
                ...base,
                status: item.status === 'COMPLETED' ? 'COMPLETED' : 'PENDING', // 완수 상태는 유지
                isModified: true,
                effectiveName: payload.modifiedName ?? base.effectiveName,
                effectiveCalories: payload.modifiedCalories ?? base.effectiveCalories,
                ...(payload.modifiedProtein != null && { protein: payload.modifiedProtein }),
                ...(payload.modifiedCarbs != null && { carbs: payload.modifiedCarbs }),
                ...(payload.modifiedFat != null && { fat: payload.modifiedFat }),
                ...(payload.modifiedSets != null && { sets: payload.modifiedSets }),
                ...(payload.modifiedReps != null && { reps: payload.modifiedReps }),
            };

        case 'COMPLETE_WITH_MODIFY':
            return {
                ...base,
                status: 'COMPLETED',
                isModified: true,
                effectiveName: payload.modifiedName ?? base.effectiveName,
                effectiveCalories: payload.modifiedCalories ?? base.effectiveCalories,
                ...(payload.modifiedProtein != null && { protein: payload.modifiedProtein }),
                ...(payload.modifiedCarbs != null && { carbs: payload.modifiedCarbs }),
                ...(payload.modifiedFat != null && { fat: payload.modifiedFat }),
                ...(payload.modifiedSets != null && { sets: payload.modifiedSets }),
                ...(payload.modifiedReps != null && { reps: payload.modifiedReps }),
            };

        default:
            return base;
    }
}

// ─── 칼로리 합산 재계산 ──────────────────────────────────────────────────
function recalcCalories(todayData, meals, workouts) {
    const targetCal = todayData.targetCaloriesPerDay ?? 0;

    const consumed = meals
        .filter((m) => m.status === 'COMPLETED')
        .reduce((s, m) => s + (m.effectiveCalories ?? m.calories ?? 0), 0);

    const burned = workouts
        .filter((w) => w.status === 'COMPLETED')
        .reduce((s, w) => s + (w.effectiveCalories ?? w.calories ?? 0), 0);

    const remaining = Math.max(0, targetCal - consumed);

    return { consumedCalories: consumed, burnedCalories: burned, remainingCalories: remaining };
}

export const usePlanStore = create((set, get) => ({
    todayData: null,
    loading: false,
    error: null,

    fetchToday: async (force = false) => {
        if (!force && get().todayData) return;
        set({ loading: true });
        try {
            const data = await getTodayPlan();
            set({ todayData: data, loading: false, error: null });
        } catch (e) {
            set({ error: e.message, loading: false });
        }
    },

    // ─── 낙관적 업데이트 (단일 아이템) ──────────────────────────────────
    updatePlanItem: (itemId, patch) =>
        set((state) => {
            if (!state.todayData?.today) return state;

            const update = (list) =>
                list.map((item) =>
                    item.id === itemId ? { ...item, ...patch } : item
                );

            const meals = update(state.todayData.today.meals ?? []);
            const workouts = update(state.todayData.today.workouts ?? []);
            const calStats = recalcCalories(state.todayData, meals, workouts);

            return {
                todayData: {
                    ...state.todayData,
                    today: {
                        ...state.todayData.today,
                        meals,
                        workouts,
                        ...calStats,
                    },
                },
            };
        }),

    // ─── 액션 적용: 낙관적 → API 호출 → 실패 시 롤백 ───────────────────
    applyAction: async (itemId, payload) => {
        const prev = get().todayData;

        // 낙관적 업데이트를 위해 현재 아이템을 찾는다
        const allItems = [
            ...(prev?.today?.meals ?? []),
            ...(prev?.today?.workouts ?? []),
        ];
        const currentItem = allItems.find((i) => i.id === itemId);

        if (currentItem) {
            const patch = buildOptimisticPatch(currentItem, payload);
            get().updatePlanItem(itemId, patch);
        }

        try {
            await patchPlanItem(itemId, payload);
            // 성공: 서버 응답으로 정합성 보장을 위해 백그라운드 재조회 (선택)
            // 짧은 딜레이 후 silent refresh — UI 깜빡임 없이 서버 값 동기화
            setTimeout(() => get().fetchToday(true), 300);
        } catch (e) {
            // 실패: 이전 상태로 롤백
            set({ todayData: prev });
            throw e;
        }
    },
}));