// src/pages/diet/DietTodayTab.jsx
import { useEffect } from 'react';
import { usePlanStore } from '../../store/planStore';
import MealSection from '../../components/diet/MealSection';

const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];
const MEAL_LABEL = {
    BREAKFAST: '🌅 아침',
    LUNCH: '☀️ 점심',
    DINNER: '🌙 저녁',
    SNACK: '🍎 간식',
};

export default function DietTodayTab() {
    const { todayData, loading, error, fetchToday } = usePlanStore();

    useEffect(() => { fetchToday(); }, []);

    if (loading) return (
        <div className="flex-1 flex items-center justify-center text-gray-400 text-sm">
            로딩 중...
        </div>
    );

    if (error) return (
        <div className="flex-1 flex items-center justify-center text-red-400 text-sm">
            오류가 발생했어요
        </div>
    );

    if (!todayData?.today) return null;

    const { today } = todayData;
    const meals = today.meals ?? [];

    // category 기준으로 그룹핑
    const grouped = MEAL_TYPES.reduce((acc, type) => {
        acc[type] = meals.filter((m) => m.category === type);
        return acc;
    }, {});

    const pct = todayData.targetCaloriesPerDay > 0
        ? Math.min(1, today.consumedCalories / todayData.targetCaloriesPerDay)
        : 0;

    return (
        <div className="flex-1 overflow-y-auto p-4 space-y-4 pb-24">

            {/* 칼로리 요약 */}
            <div className="bg-green-50 rounded-2xl p-4">
                <div className="flex justify-between text-sm text-gray-500 mb-2">
                    <span>오늘 섭취</span>
                    <span>{today.consumedCalories} / {todayData.targetCaloriesPerDay} kcal</span>
                </div>
                <div className="w-full h-2 bg-gray-200 rounded-full">
                    <div
                        className="h-2 bg-green-500 rounded-full transition-all duration-500"
                        style={{ width: `${pct * 100}%` }}
                    />
                </div>
                <div className="text-xs text-gray-400 mt-1 text-right">
                    {today.remainingCalories} kcal 남음
                </div>
            </div>

            {/* 식사별 섹션 */}
            {MEAL_TYPES.map((type) =>
                grouped[type].length > 0 ? (
                    <MealSection
                        key={type}
                        label={MEAL_LABEL[type]}
                        items={grouped[type]}
                    />
                ) : null
            )}
        </div>
    );
}