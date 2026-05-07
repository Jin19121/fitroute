// components/diet/MealSection.jsx
import FoodItem from '../../pages/diet/FoodItem';

export default function MealSection({ label, items = [] }) {
    const planned = items.reduce((sum, i) => sum + (i.calories ?? 0), 0);
    const consumed = items
        .filter((i) => i.status === 'COMPLETED' || i.status === 'MODIFIED')
        .reduce((sum, i) => sum + (i.effectiveCalories ?? i.calories ?? 0), 0);

    return (
        <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
            <div className="flex justify-between items-center px-4 py-3 border-b border-gray-100">
                <span className="font-semibold text-gray-800">{label}</span>
                <span className="text-xs text-gray-400">{consumed} / {planned} kcal</span>
            </div>

            <div className="divide-y divide-gray-50">
                {items.length === 0 ? (
                    <p className="text-sm text-gray-400 p-4 text-center">계획된 식단이 없습니다</p>
                ) : (
                    items.map((item) => (
                        <FoodItem
                            key={item.id}
                            planItem={item}
                        />
                    ))
                )}
            </div>

            <button className="w-full py-2 text-sm text-green-500 hover:bg-green-50 transition-colors">
                + 음식 추가
            </button>
        </div>
    );
}