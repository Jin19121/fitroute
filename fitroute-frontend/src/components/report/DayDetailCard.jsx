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

// ─── 운동 상세 ────────────────────────────────────────────────────────────

function WorkoutDetail({ workout }) {
    const badge = STATUS_LABEL[workout?.status] ?? STATUS_LABEL.NO_PLAN;

    return (
        <div className="flex flex-col gap-2">
            <div className="flex items-center gap-2">
                <span className="text-[11px] font-bold text-[#1a1a1a]">🏋️ 운동</span>
                <span className={`text-[9px] font-semibold px-2 py-0.5 rounded-full ${badge.cls}`}>
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
                        const done = item.status === 'COMPLETED' || item.status === 'MODIFIED';
                        return (
                            <div key={i} className="flex items-center gap-2">
                                <div className={`w-4 h-4 rounded-full flex-shrink-0 flex items-center justify-center
                                    ${done ? 'bg-[#4a7bff]' : 'bg-[#e5e1db]'}`}>
                                    {done && (
                                        <svg width="8" height="8" viewBox="0 0 8 8" fill="none">
                                            <path d="M1 4L3 6.5L7 1.5" stroke="#fff" strokeWidth="1.3" strokeLinecap="round" />
                                        </svg>
                                    )}
                                </div>
                                <span className={`text-[11px] flex-1 ${done ? 'text-[#1a1a1a]' : 'text-[#b8b4ae] line-through'}`}>
                                    {item.name}
                                </span>
                                <span className="text-[10px] text-[#8a8680]">{item.calories} kcal</span>
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
                <span className="text-[11px] font-bold text-[#1a1a1a]">🥗 식단</span>
                <span className={`text-[9px] font-semibold px-2 py-0.5 rounded-full ${badge.cls}`}>
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
                    const done = meal.status === 'COMPLETED' || meal.status === 'MODIFIED';
                    return (
                        <div key={key} className="flex items-center gap-2">
                            <span className="text-[9px] text-[#8a8680] w-6">{label}</span>
                            <span className={`text-[11px] flex-1 ${done ? 'text-[#1a1a1a]' : 'text-[#b8b4ae]'}`}>
                                {meal.name}
                            </span>
                            <span className="text-[10px] text-[#8a8680]">{meal.calories} kcal</span>
                        </div>
                    );
                })}
            </div>
        </div>
    );
}

// ─── 체중 상세 ────────────────────────────────────────────────────────────

function WeightDetail({ weight, onRecord }) {
    return (
        <div className="flex items-center justify-between">
            <span className="text-[11px] font-bold text-[#1a1a1a]">⚖️ 체중</span>
            {weight?.measured ? (
                <span className="text-[16px] font-extrabold text-[#ff8c42]">
                    {weight.weightKg} <span className="text-[11px] font-normal text-[#8a8680]">kg</span>
                </span>
            ) : (
                <button
                    onClick={onRecord}
                    className="text-[11px] text-[#4a7bff] font-semibold"
                >
                    + 체중 기록하기
                </button>
            )}
        </div>
    );
}

// ─── 메인 컴포넌트 ────────────────────────────────────────────────────────

export default function DayDetailCard({ dayDetail, loading, filter, onRecordWeight }) {
    if (loading) {
        return (
            <div className="bg-white rounded-2xl p-4 flex items-center justify-center h-24">
                <div className="w-5 h-5 border-2 border-[#4a7bff] border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    if (!dayDetail) return null;

    return (
        <div className="bg-white rounded-2xl p-4 flex flex-col gap-4">
            {/* 날짜 헤더 */}
            <div className="flex items-center gap-2 pb-3 border-b border-[#f0ece5]">
                <span className="text-[13px] font-bold text-[#1a1a1a]">
                    {dayDetail.date?.slice(5).replace('-', '/')}
                </span>
                <span className="text-[11px] text-[#8a8680]">{dayDetail.dayOfWeek}</span>
            </div>

            {/* 필터에 따라 해당 섹션만 먼저 노출, 나머지는 요약 */}
            {filter === 'WORKOUT' && <WorkoutDetail workout={dayDetail.workout} />}
            {filter === 'DIET' && <DietDetail diet={dayDetail.diet} />}
            {filter === 'WEIGHT' && (
                <WeightDetail weight={dayDetail.weight} onRecord={onRecordWeight} />
            )}

            {/* 다른 필터 요약 */}
            {filter !== 'WORKOUT' && dayDetail.workout?.status !== 'NO_PLAN' && (
                <div className="pt-3 border-t border-[#f0ece5]">
                    <WorkoutDetail workout={dayDetail.workout} />
                </div>
            )}
            {filter !== 'DIET' && dayDetail.diet?.status !== 'NO_PLAN' && (
                <div className="pt-3 border-t border-[#f0ece5]">
                    <DietDetail diet={dayDetail.diet} />
                </div>
            )}
            {filter !== 'WEIGHT' && (
                <div className="pt-3 border-t border-[#f0ece5]">
                    <WeightDetail weight={dayDetail.weight} onRecord={onRecordWeight} />
                </div>
            )}
        </div>
    );
}