// src/pages/diet/DietTodayTab.jsx
import { useEffect, useState } from 'react';
import { usePlanStore } from '../../store/planStore';
import MealSection from '../../components/diet/MealSection';
import PlanItemActionSheet from '../../components/PlanItemActionSheet';

const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];
const MEAL_LABEL = {
    BREAKFAST: '아침', LUNCH: '점심', DINNER: '저녁', SNACK: '간식',
};

export default function DietTodayTab() {
    const [activeItem, setActiveItem] = useState(null);
    const { todayData, loading, error, fetchToday, applyAction } = usePlanStore();

    useEffect(() => {
        fetchToday(true); // ← force 재조회
    }, []);

    if (loading) return (
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <div style={{ fontSize: 12, color: '#8A8680' }}>로딩 중...</div>
        </div>
    );

    if (error || !todayData?.today) return (
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <div style={{ fontSize: 12, color: '#B8B4AE' }}>데이터를 불러올 수 없어요</div>
        </div>
    );

    const { today } = todayData;
    const meals = today.meals ?? [];

    const grouped = MEAL_TYPES.reduce((acc, type) => {
        acc[type] = meals.filter(m => m.category === type);
        return acc;
    }, {});

    const pct = todayData.targetCaloriesPerDay > 0
        ? Math.min(1, today.consumedCalories / todayData.targetCaloriesPerDay) : 0;

    return (
        <>
            <div style={{
                flex: 1, overflowY: 'auto',
                padding: '0 12px 80px',
                display: 'flex', flexDirection: 'column', gap: 8,
                background: '#F5F3F0',
            }}>
                {/* 칼로리 카드 */}
                <div style={{
                    background: '#4A7BFF', borderRadius: 14,
                    padding: '11px 12px', marginTop: 8,
                    display: 'flex', alignItems: 'center', gap: 10,
                }}>
                    <svg width="52" height="52" viewBox="0 0 52 52" style={{ flexShrink: 0 }}>
                        <circle cx="26" cy="26" r="21" fill="none" stroke="rgba(255,255,255,.25)" strokeWidth="5" />
                        <circle cx="26" cy="26" r="21" fill="none" stroke="#fff" strokeWidth="5"
                            strokeDasharray={`${132 * pct} ${132 * (1 - pct)}`}
                            strokeLinecap="round" transform="rotate(-90 26 26)"
                            style={{ transition: 'stroke-dasharray 0.6s ease' }}
                        />
                        <text x="26" y="30" textAnchor="middle" fontSize="10" fontWeight="700" fill="#fff">
                            {Math.round(pct * 100)}%
                        </text>
                    </svg>
                    <div>
                        <div style={{ fontSize: 8, color: 'rgba(255,255,255,.65)' }}>오늘 섭취</div>
                        <div style={{ fontSize: 20, fontWeight: 700, color: '#fff', lineHeight: 1.1 }}>
                            {today.consumedCalories.toLocaleString()}
                            <span style={{ fontSize: 11, fontWeight: 400, color: 'rgba(255,255,255,.65)' }}> kcal</span>
                        </div>
                        <div style={{ fontSize: 8, color: 'rgba(255,255,255,.6)', marginTop: 1 }}>
                            목표 {todayData.targetCaloriesPerDay?.toLocaleString()} kcal
                        </div>
                        <div style={{
                            display: 'inline-block',
                            background: 'rgba(255,255,255,.2)', borderRadius: 8,
                            fontSize: 8, color: '#fff', padding: '2px 7px', marginTop: 3,
                        }}>
                            {today.remainingCalories?.toLocaleString()} kcal 남음
                        </div>
                    </div>
                </div>

                {/* 식단 카드 */}
                <div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 4 }}>
                        <span style={{ fontSize: 11, fontWeight: 700, color: '#1A1A1A' }}>🥗 오늘 식단</span>
                        <span style={{ fontSize: 9, color: '#4A7BFF' }}>상세보기</span>
                    </div>
                    <div style={{ background: '#fff', borderRadius: 12, padding: '10px 11px' }}>
                        {MEAL_TYPES.map(type =>
                            grouped[type].length > 0 ? (
                                <MealSection
                                    key={type}
                                    mealType={type}
                                    label={MEAL_LABEL[type]}
                                    items={grouped[type]}
                                    onTap={setActiveItem}
                                />
                            ) : null
                        )}
                    </div>
                </div>

                {/* 힌트 */}
                <div style={{
                    background: '#F2EEE8', borderRadius: 8,
                    padding: '6px 9px', display: 'flex', alignItems: 'center', gap: 6,
                }}>
                    <svg width="14" height="14" viewBox="0 0 14 14" style={{ flexShrink: 0 }}>
                        <circle cx="7" cy="7" r="6" fill="none" stroke="#B8B4AE" strokeWidth="1.2" />
                        <path d="M7 6 L7 10" stroke="#B8B4AE" strokeWidth="1.3" strokeLinecap="round" />
                        <circle cx="7" cy="4.5" r=".8" fill="#B8B4AE" />
                    </svg>
                    <span style={{ fontSize: 9, color: '#6B6866', lineHeight: 1.4 }}>
                        음식명 탭 시 재료 구성과 조리법을 볼 수 있어요
                    </span>
                </div>
            </div>

            <PlanItemActionSheet
                item={activeItem}
                onClose={() => setActiveItem(null)}
                onApply={async (itemId, payload) => {
                    await applyAction(itemId, payload);
                    setActiveItem(null);
                }}
            />
        </>
    );
}