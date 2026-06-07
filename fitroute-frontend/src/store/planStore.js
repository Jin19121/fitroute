// src/store/planStore.js
import { create } from 'zustand';
import { getTodayPlan, patchPlanItem } from '../api/diet';

// ─── 집계 재계산 헬퍼 ────────────────────────────────────────────────────────
/**
 * meals 배열을 순회해 consumedCalories / remainingCalories 를 재산출한다.
 *
 * 분자: status === 'COMPLETED' 인 MEAL 아이템의 effectiveCalories ?? calories 합산
 * 분모(remaining 계산): targetCaloriesPerDay (나중에 plannedMealCalories 로 교체 예정)
 *
 * @param {Array}  meals               - 패치 이후의 최신 meals 배열
 * @param {number} targetCaloriesPerDay - 하루 목표 칼로리 (0 나누기 방어 포함)
 * @returns {{ consumedCalories: number, remainingCalories: number }}
 */
function recalculateMealSummary(meals, targetCaloriesPerDay) {
    const consumed = meals
        .filter((m) => m.status === 'COMPLETED')
        .reduce((sum, m) => sum + (m.effectiveCalories ?? m.calories ?? 0), 0);

    const remaining = Math.max(0, (targetCaloriesPerDay ?? 0) - consumed);

    return { consumedCalories: consumed, remainingCalories: remaining };
}

// ─── 액션별 낙관적 패치 계산 ────────────────────────────────────────────────
/**
 * 단일 아이템에 적용할 낙관적 패치 객체를 반환한다.
 * 집계(consumedCalories 등)는 이 함수 밖에서 처리한다.
 */
function buildOptimisticPatch(action, fields) {
    const {
        modifiedName,
        modifiedCalories,
        modifiedProtein,
        modifiedCarbs,
        modifiedFat,
        modifiedSets,
        modifiedReps,
    } = fields;

    switch (action) {
        case 'COMPLETE':
            return { status: 'COMPLETED' };

        case 'SKIP':
            return { status: 'SKIPPED' };

        case 'MODIFY': {
            // status 는 PENDING 유지, isModified=true, 수정값만 반영
            const patch = { isModified: true };
            if (modifiedName != null) patch.effectiveName = modifiedName;
            if (modifiedCalories != null) patch.effectiveCalories = modifiedCalories;
            if (modifiedProtein != null) patch.protein = modifiedProtein;
            if (modifiedCarbs != null) patch.carbs = modifiedCarbs;
            if (modifiedFat != null) patch.fat = modifiedFat;
            if (modifiedSets != null) patch.sets = modifiedSets;
            if (modifiedReps != null) patch.reps = modifiedReps;
            return patch;
        }

        case 'COMPLETE_WITH_MODIFY': {
            // 수정 + 완수
            const patch = { status: 'COMPLETED', isModified: true };
            if (modifiedName != null) patch.effectiveName = modifiedName;
            if (modifiedCalories != null) patch.effectiveCalories = modifiedCalories;
            if (modifiedProtein != null) patch.protein = modifiedProtein;
            if (modifiedCarbs != null) patch.carbs = modifiedCarbs;
            if (modifiedFat != null) patch.fat = modifiedFat;
            if (modifiedSets != null) patch.sets = modifiedSets;
            if (modifiedReps != null) patch.reps = modifiedReps;
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

// ─── Store ───────────────────────────────────────────────────────────────────
export const usePlanStore = create((set, get) => ({
    todayData: null,
    loading: false,
    error: null,

    // ── 오늘 데이터 패칭 ────────────────────────────────────────────────────
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

    // ── 단일 아이템 패치 (내부 헬퍼) ────────────────────────────────────────
    updatePlanItem: (itemId, patch) =>
        set((state) => {
            if (!state.todayData?.today) return state;

            const update = (list) =>
                list.map((item) =>
                    item.id === itemId ? { ...item, ...patch } : item,
                );

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

    // ── 메인 액션 — 낙관적 업데이트 + 집계 재계산 ───────────────────────────
    /**
     * payload 예시:
     *   { action: 'COMPLETE' }
     *   { action: 'SKIP' }
     *   { action: 'MODIFY', modifiedName: '...', modifiedCalories: 400 }
     *   { action: 'COMPLETE_WITH_MODIFY', modifiedCalories: 500 }
     *   { action: 'RESET' }
     */
    applyAction: async (itemId, payload) => {
        const {
            action,
            modifiedName,
            modifiedCalories,
            modifiedProtein,
            modifiedCarbs,
            modifiedFat,
            modifiedSets,
            modifiedReps,
        } = payload;

        const fields = {
            modifiedName,
            modifiedCalories,
            modifiedProtein,
            modifiedCarbs,
            modifiedFat,
            modifiedSets,
            modifiedReps,
        };

        // ── Step 1: 단일 아이템 낙관적 패치 ──────────────────────────────────
        get().updatePlanItem(itemId, buildOptimisticPatch(action, fields));

        // ── Step 2: 패치 후 최신 state 를 읽어 집계 재계산 ───────────────────
        // updatePlanItem 은 동기 set() 이므로 get() 으로 바로 최신값 접근 가능
        const state = get();
        const today = state.todayData?.today;

        if (today) {
            const targetCaloriesPerDay = state.todayData.targetCaloriesPerDay ?? 0;
            const { consumedCalories, remainingCalories } =
                recalculateMealSummary(today.meals, targetCaloriesPerDay);

            set((prev) => {
                if (!prev.todayData?.today) return prev;
                return {
                    todayData: {
                        ...prev.todayData,
                        today: {
                            ...prev.todayData.today,
                            consumedCalories,
                            remainingCalories,
                        },
                    },
                };
            });
        }

        // ── Step 3: 서버 동기화 (실패 시 전체 재조회) ────────────────────────
        try {
            await patchPlanItem(itemId, payload);
        } catch (e) {
            // 서버 실패 시 낙관적 업데이트 롤백 — 재조회로 원복
            set({ todayData: null });
            get().fetchToday(true);
            throw e; // 호출자(UI)가 에러 처리할 수 있도록 rethrow
        }
    },
}));