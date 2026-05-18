// src/components/report/KpiCards.jsx

// ─── 운동 KPI ────────────────────────────────────────────────────────────

function WorkoutKpi({ workout }) {
    const achieved = workout?.achievedDays ?? 0;
    const total = workout?.totalPlanDays ?? 0;
    const pct = total > 0 ? Math.round((achieved / total) * 100) : 0;

    return (
        <div className="flex gap-3">
            <div className="flex-1 bg-white rounded-2xl p-4">
                <div className="text-[10px] text-[#8a8680] mb-1">운동 달성</div>
                <div className="text-[22px] font-extrabold text-[#1a1a1a] leading-none">
                    {achieved}
                    <span className="text-[13px] font-normal text-[#8a8680]"> / {total}일</span>
                </div>
                <div className="mt-2 bg-[#edeae5] rounded-full h-1.5 overflow-hidden">
                    <div
                        className="h-full bg-[#4a7bff] rounded-full transition-all duration-700"
                        style={{ width: `${pct}%` }}
                    />
                </div>
                <div className="text-[10px] text-[#4a7bff] font-semibold mt-1">{pct}%</div>
            </div>

            <div className="flex-1 bg-white rounded-2xl p-4">
                <div className="text-[10px] text-[#8a8680] mb-1">이번달 체중변화</div>
                <WeightChangeBadge summary={null} />
            </div>
        </div>
    );
}

// ─── 식단 KPI ────────────────────────────────────────────────────────────

function DietKpi({ diet }) {
    const achieved = diet?.achievedDays ?? 0;
    const total = diet?.totalPlanDays ?? 0;
    const avgKcal = diet?.avgDailyKcal ?? 0;
    const pct = total > 0 ? Math.round((achieved / total) * 100) : 0;

    return (
        <div className="flex gap-3">
            <div className="flex-1 bg-white rounded-2xl p-4">
                <div className="text-[10px] text-[#8a8680] mb-1">식단 달성</div>
                <div className="text-[22px] font-extrabold text-[#1a1a1a] leading-none">
                    {achieved}
                    <span className="text-[13px] font-normal text-[#8a8680]"> / {total}일</span>
                </div>
                <div className="mt-2 bg-[#edeae5] rounded-full h-1.5 overflow-hidden">
                    <div
                        className="h-full bg-[#1a9e75] rounded-full transition-all duration-700"
                        style={{ width: `${pct}%` }}
                    />
                </div>
                <div className="text-[10px] text-[#1a9e75] font-semibold mt-1">{pct}%</div>
            </div>

            <div className="flex-1 bg-white rounded-2xl p-4">
                <div className="text-[10px] text-[#8a8680] mb-1">일 평균 섭취</div>
                <div className="text-[22px] font-extrabold text-[#1a1a1a] leading-none">
                    {avgKcal.toLocaleString()}
                    <span className="text-[13px] font-normal text-[#8a8680]"> kcal</span>
                </div>
            </div>
        </div>
    );
}

// ─── 체중 KPI ────────────────────────────────────────────────────────────

function WeightKpi({ weight }) {
    const change = weight?.changeKg;
    const goal = weight?.goalWeight;
    const latest = weight?.latestWeight;
    const daysToGoal = weight?.daysToGoal ?? 0;

    const changeStr = change != null
        ? `${change > 0 ? '+' : ''}${change.toFixed(1)}`
        : '-';
    const changeColor = change < 0 ? 'text-[#4a7bff]' : change > 0 ? 'text-red-400' : 'text-[#1a1a1a]';

    return (
        <div className="flex flex-col gap-3">
            <div className="flex gap-3">
                <div className="flex-1 bg-white rounded-2xl p-4">
                    <div className="text-[10px] text-[#8a8680] mb-1">이번달 변화</div>
                    <div className={`text-[22px] font-extrabold leading-none ${changeColor}`}>
                        {changeStr}
                        <span className="text-[13px] font-normal text-[#8a8680]"> kg</span>
                    </div>
                </div>

                <div className="flex-1 bg-white rounded-2xl p-4">
                    <div className="text-[10px] text-[#8a8680] mb-1">현재 체중</div>
                    <div className="text-[22px] font-extrabold text-[#1a1a1a] leading-none">
                        {latest?.toFixed(1) ?? '-'}
                        <span className="text-[13px] font-normal text-[#8a8680]"> kg</span>
                    </div>
                </div>
            </div>

            <div className="flex gap-3">
                <div className="flex-1 bg-white rounded-2xl p-4">
                    <div className="text-[10px] text-[#8a8680] mb-1">목표 체중</div>
                    <div className="text-[22px] font-extrabold text-[#ff8c42] leading-none">
                        {goal?.toFixed(1) ?? '-'}
                        <span className="text-[13px] font-normal text-[#8a8680]"> kg</span>
                    </div>
                </div>

                <div className="flex-1 bg-white rounded-2xl p-4">
                    <div className="text-[10px] text-[#8a8680] mb-1">목표까지</div>
                    <div className="text-[22px] font-extrabold text-[#1a1a1a] leading-none">
                        D-{daysToGoal}
                    </div>
                </div>
            </div>
        </div>
    );
}

// ─── 내부 헬퍼 ───────────────────────────────────────────────────────────

function WeightChangeBadge({ summary }) {
    // 운동 KPI 카드의 두 번째 칸 — 체중 변화량 (summary.weight에서 가져옴)
    if (!summary?.weight?.changeKg) {
        return <div className="text-[22px] font-extrabold text-[#b8b4ae]">-</div>;
    }
    const c = summary.weight.changeKg;
    const color = c < 0 ? 'text-[#4a7bff]' : 'text-red-400';
    return (
        <div className={`text-[22px] font-extrabold leading-none ${color}`}>
            {c > 0 ? '+' : ''}{c.toFixed(1)}
            <span className="text-[13px] font-normal text-[#8a8680]"> kg</span>
        </div>
    );
}

// ─── 메인 컴포넌트 ────────────────────────────────────────────────────────

export default function KpiCards({ filter, summary }) {
    if (!summary) return null;

    return (
        <div>
            {filter === 'WORKOUT' && <WorkoutKpi workout={summary.workout} />}
            {filter === 'DIET' && <DietKpi diet={summary.diet} />}
            {filter === 'WEIGHT' && <WeightKpi weight={summary.weight} />}
        </div>
    );
}