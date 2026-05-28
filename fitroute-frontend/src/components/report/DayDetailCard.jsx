// src/components/report/DayDetailCard.jsx

const STATUS_LABEL = {
    FULL: { text: '완수', cls: 'bg-[#eef3ff] text-[#2a5cc5]' },
    PART: { text: '일부완수', cls: 'bg-[#fef9ee] text-[#b45309]' },
    NONE: { text: '미실행', cls: 'bg-[#f0ece5] text-[#8a8680]' },
    NO_PLAN: { text: '계획없음', cls: 'bg-[#f0ece5] text-[#8a8680]' },
    ACHIEVED: { text: '달성', cls: 'bg-[#edfaf3] text-[#1a6b40]' },
    EXCEEDED: { text: '초과', cls: 'bg-[#fef9ee] text-[#b45309]' },
    NO_RECORD: { text: '미기록', cls: 'bg-[#f0ece5] text-[#8a8680]' },
};

const MEAL_LABEL = {
    breakfast: '아침',
    lunch: '점심',
    dinner: '저녁',
    snack: '간식',
};

const hasDiff = (diff) => diff != null && diff !== 0;

// ─── 운동 상세 ────────────────────────────────────────────────────────────

function WorkoutDetail({ workout }) {
    const badge = STATUS_LABEL[workout?.status] ?? STATUS_LABEL.NO_PLAN;

    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
                <span className="text-[11px] font-bold text-[#1a1a1a]">
                    🏋️ 운동
                </span>

                <span
                    className={`text-[9px] font-semibold px-2 py-0.5 rounded-full ${badge.cls}`}
                >
                    {badge.text}
                </span>

                {workout?.burnedKcal > 0 && (
                    <span className="text-[10px] text-[#4a7bff] font-semibold ml-auto">
                        -{workout.burnedKcal} kcal
                    </span>
                )}
            </div>

            {workout?.items?.length > 0 && (
                <div className="flex flex-col gap-1">
                    {workout.items.map((item, i) => {
                        const done = item.status === 'COMPLETED';

                        return (
                            <div
                                key={i}
                                className="flex items-center gap-2"
                            >
                                <div
                                    className={`w-4 h-4 rounded-full flex-shrink-0 flex items-center justify-center
                                    ${done ? 'bg-[#4a7bff]' : 'bg-[#e5e1db]'}`}
                                >
                                    {done && (
                                        <svg
                                            width="8"
                                            height="8"
                                            viewBox="0 0 8 8"
                                            fill="none"
                                        >
                                            <path
                                                d="M1 4L3 6.5L7 1.5"
                                                stroke="#fff"
                                                strokeWidth="1.3"
                                                strokeLinecap="round"
                                            />
                                        </svg>
                                    )}
                                </div>

                                <span
                                    className={`text-[11px] flex-1 ${done
                                        ? 'text-[#1a1a1a]'
                                        : 'text-[#b8b4ae] line-through'
                                        }`}
                                >
                                    {item.name}
                                </span>

                                {item.isModified && (
                                    <span className="text-[9px] text-[#b45309] font-semibold">
                                        수정됨
                                    </span>
                                )}

                                <span className="text-[10px] text-[#8a8680]">
                                    {item.actualCalories ?? item.originalCalories} kcal
                                </span>

                                {hasDiff(item.diffCalories) && (
                                    <span
                                        className={`text-[9px] ml-1 font-semibold
                                        ${item.diffCalories > 0
                                                ? 'text-red-400'
                                                : 'text-[#4a7bff]'
                                            }`}
                                    >
                                        {item.diffCalories > 0
                                            ? `+${item.diffCalories}`
                                            : item.diffCalories}{' '}
                                        kcal
                                    </span>
                                )}
                            </div>
                        );
                    })}
                </div>
            )}
        </div>
    );
}

// ─── 식단 상세 ────────────────────────────────────────────────────────────

function DietDetail({ diet }) {
    const badge = STATUS_LABEL[diet?.status] ?? STATUS_LABEL.NO_PLAN;
    const meals = diet?.meals ?? {};

    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
                <span className="text-[11px] font-bold text-[#1a1a1a]">
                    🥗 식단
                </span>

                <span
                    className={`text-[9px] font-semibold px-2 py-0.5 rounded-full ${badge.cls}`}
                >
                    {badge.text}
                </span>

                {diet?.consumedKcal > 0 && (
                    <span className="text-[10px] text-[#1a9e75] font-semibold ml-auto">
                        {diet.consumedKcal} / {diet.targetKcal} kcal
                    </span>
                )}
            </div>

            <div className="flex flex-col gap-1">
                {Object.entries(MEAL_LABEL).map(([key, label]) => {
                    const meal = meals[key];

                    if (!meal) return null;

                    const done = meal.status === 'COMPLETED';

                    return (
                        <div
                            key={key}
                            className="flex items-center gap-2"
                        >
                            <span className="text-[9px] text-[#8a8680] w-6">
                                {label}
                            </span>

                            <span
                                className={`text-[11px] flex-1 ${done
                                    ? 'text-[#1a1a1a]'
                                    : 'text-[#b8b4ae]'
                                    }`}
                            >
                                {meal.name}
                            </span>

                            {meal.isModified && (
                                <span className="text-[9px] text-[#b45309] font-semibold">
                                    수정됨
                                </span>
                            )}

                            <span className="text-[10px] text-[#8a8680]">
                                {meal.actualCalories ?? meal.originalCalories} kcal
                            </span>

                            {hasDiff(meal.diffCalories) && (
                                <span
                                    className={`text-[9px] font-semibold ${meal.diffCalories > 0
                                            ? 'text-red-400'
                                            : 'text-[#4a7bff]'
                                        }`}
                                >
                                    {meal.diffCalories > 0
                                        ? `+${meal.diffCalories}`
                                        : meal.diffCalories}
                                </span>
                            )}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}