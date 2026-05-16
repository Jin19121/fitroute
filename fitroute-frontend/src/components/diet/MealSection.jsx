// src/components/diet/MealSection.jsx
import FoodItem from '../../pages/diet/FoodItem';

const MEAL_BADGE = {
    BREAKFAST: { bg: '#FFF1E6', color: '#B55A00', label: '아침' },
    LUNCH: { bg: '#EEF3FF', color: '#2A5CC5', label: '점심' },
    DINNER: { bg: '#EDFAF3', color: '#1A6B40', label: '저녁' },
    SNACK: { bg: '#FAF0FF', color: '#7B2FAB', label: '간식' },
};

export default function MealSection({ label, items = [], mealType, onTap }) {
    const badge = MEAL_BADGE[mealType] ?? { bg: '#F2EEE8', color: '#8A8680', label };

    const planned = items.reduce((s, i) => s + (i.calories ?? 0), 0);
    const consumed = items
        .filter(i => i.status === 'COMPLETED' || i.status === 'MODIFIED')
        .reduce((s, i) => s + (i.effectiveCalories ?? i.calories ?? 0), 0);
    const allDone = items.length > 0 && items.every(i => i.status !== 'PENDING');

    return (
        <div style={{ marginBottom: 6 }}>
            {/* 식사 헤더 */}
            <div style={{ display: 'flex', alignItems: 'center', gap: 5, marginBottom: 4 }}>
                <span style={{
                    fontSize: 8, fontWeight: 600,
                    padding: '2px 7px', borderRadius: 10,
                    background: badge.bg, color: badge.color,
                }}>
                    {badge.label}
                </span>
                <span style={{
                    fontSize: 8, marginLeft: 'auto',
                    color: allDone ? '#B8B4AE' : '#4A7BFF',
                    fontWeight: allDone ? 400 : 600,
                }}>
                    {allDone ? `${consumed} kcal ✓` : '미기록'}
                </span>
            </div>

            {/* 아이템 목록 */}
            {items.map((item, idx) => (
                <FoodItem
                    key={item.id}
                    planItem={item}
                    isLast={idx === items.length - 1}
                    onTap={onTap}
                />
            ))}
        </div>
    );
}