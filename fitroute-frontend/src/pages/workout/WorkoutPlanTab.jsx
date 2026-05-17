// src/pages/workout/WorkoutPlanTab.jsx
import { useState, useEffect } from "react";
import { getWeeklyWorkoutPlan } from "../../api/workout";

const WO_COLOR = {
    CHEST: "#4a7bff",
    BACK: "#ff8c42",
    LEGS: "#1a6b40",
    SHOULDERS: "#a855f7",
    ARMS: "#f59e0b",
    CORE: "#ef4444",
    CARDIO: "#06b6d4",
    REST: "#9ca3af",
};

const WO_BADGE = {
    CHEST: "가슴",
    BACK: "등",
    LEGS: "다리",
    SHOULDERS: "어깨",
    ARMS: "팔",
    CORE: "코어",
    CARDIO: "유산소",
    REST: "휴식",
};

// 이번 주 월요일 계산
function getThisMonday() {
    const today = new Date();
    const day = today.getDay(); // 0=일, 1=월
    const diff = day === 0 ? -6 : 1 - day;
    const monday = new Date(today);
    monday.setDate(today.getDate() + diff);
    return monday.toISOString().split("T")[0]; // YYYY-MM-DD
}

// 날짜 포맷 (M월 D일)
function formatDate(dateStr) {
    const d = new Date(dateStr);
    return `${d.getMonth() + 1}/${d.getDate()}`;
}

// 루틴 블록 컴포넌트
function RoutineBlock({ routine }) {
    const [open, setOpen] = useState(false);
    const color = WO_COLOR[routine.category] ?? "#9ca3af";
    const badge = WO_BADGE[routine.category] ?? routine.category;

    return (
        <div className="bg-white rounded-2xl overflow-hidden">
            {/* 헤더 */}
            <div
                className="flex items-center gap-3 px-4 py-3 cursor-pointer"
                onClick={() => setOpen((prev) => !prev)}
            >
                <div
                    className="w-2 h-2 rounded-sm flex-shrink-0"
                    style={{ background: color }}
                />
                <div className="flex-1 min-w-0">
                    <div className="text-[12px] font-semibold text-[#1a1a1a] truncate">
                        {routine.name}
                    </div>
                    <div className="text-[10px] text-[#b8b4ae] mt-0.5">
                        <span
                            className="font-medium mr-1.5"
                            style={{ color }}
                        >
                            {badge}
                        </span>
                        {routine.duration > 0 && `${routine.duration}분`}
                        {routine.sets > 0 && routine.reps > 0 && ` · ${routine.sets}×${routine.reps}`}
                    </div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                    <span className="text-[11px] font-semibold text-blue-500">
                        {routine.calories > 0 ? `${routine.calories}kcal` : ""}
                    </span>
                    <svg
                        width="12" height="12" viewBox="0 0 12 12" fill="none"
                        className="transition-transform duration-200"
                        style={{ transform: open ? "rotate(180deg)" : "rotate(0deg)" }}
                    >
                        <path d="M2.5 4.5L6 8L9.5 4.5" stroke="#b8b4ae" strokeWidth="1.4" strokeLinecap="round" />
                    </svg>
                </div>
            </div>

            {/* 상세 내용 */}
            {open && (
                <div className="px-4 pb-3 border-t border-[#f0ece5]">
                    <div className="pt-3 flex flex-col gap-3">

                        {/* 운동 목록 */}
                        {routine.exercises && routine.exercises.length > 0 && (
                            <div className="flex flex-col gap-2">
                                {routine.exercises.map((ex, idx) => (
                                    <div
                                        key={idx}
                                        className="flex items-center justify-between py-2 border-b border-[#f5f3f0] last:border-0"
                                    >
                                        <span className="text-[12px] font-medium text-[#1a1a1a]">
                                            {ex.name}
                                        </span>
                                        <div className="flex items-center gap-2">
                                            {ex.sets > 0 && ex.reps > 0 && (
                                                <span className="text-[10px] text-[#8a8680]">
                                                    {ex.sets}세트 × {ex.reps}회
                                                </span>
                                            )}
                                            {routine.duration > 0 && ex.sets === 0 && (
                                                <span className="text-[10px] text-[#8a8680]">
                                                    {routine.duration}분
                                                </span>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* 총 칼로리 */}
                        {routine.calories > 0 && (
                            <div className="flex justify-between items-center pt-1">
                                <span className="text-[10px] text-[#8a8680]">총 소모 칼로리</span>
                                <span className="text-[11px] font-bold text-blue-500">
                                    {routine.calories}kcal
                                </span>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

// 요일 카드 컴포넌트
function DayCard({ day }) {
    const isToday = day.date === new Date().toISOString().split("T")[0];
    const hasWorkout = day.routines.length > 0;

    return (
        <div className={`rounded-2xl overflow-hidden border ${isToday ? "border-blue-400" : "border-transparent"}`}>
            {/* 요일 헤더 */}
            <div
                className={`flex items-center gap-2 px-4 py-2.5 ${isToday ? "bg-blue-500" : "bg-[#f0ece5]"
                    }`}
            >
                <span
                    className={`text-[11px] font-bold ${isToday ? "text-white" : "text-[#1a1a1a]"
                        }`}
                >
                    {day.dayName}요일
                </span>
                <span
                    className={`text-[10px] ${isToday ? "text-white/70" : "text-[#8a8680]"
                        }`}
                >
                    {formatDate(day.date)}
                </span>
                {isToday && (
                    <span className="ml-auto text-[9px] bg-white/20 text-white px-2 py-0.5 rounded-full font-medium">
                        오늘
                    </span>
                )}
                {!isToday && hasWorkout && (
                    <span className="ml-auto text-[9px] text-[#8a8680]">
                        {day.routines.length}개
                    </span>
                )}
            </div>

            {/* 운동 목록 */}
            <div className="p-2 flex flex-col gap-2 bg-[#f8f6f3]">
                {hasWorkout ? (
                    day.routines.map((routine) => (
                        <RoutineBlock key={routine.id} routine={routine} />
                    ))
                ) : (
                    <div className="flex items-center justify-center py-3 text-[10px] text-[#b8b4ae]">
                        휴식일
                    </div>
                )}
            </div>
        </div>
    );
}

export default function WorkoutPlanTab() {
    const [weeklyPlan, setWeeklyPlan] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        const startDate = getThisMonday();
        setLoading(true);

        getWeeklyWorkoutPlan(startDate)
            .then((data) => {
                setWeeklyPlan(data);
                setError(null);
            })
            .catch((e) => {
                setError("계획을 불러오지 못했어요.");
            })
            .finally(() => setLoading(false));
    }, []);

    if (loading) {
        return (
            <div className="flex-1 flex items-center justify-center text-[13px] text-[#8a8680]">
                로딩 중...
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex-1 flex flex-col items-center justify-center p-6 gap-2">
                <div className="text-[13px] font-medium text-[#1a1a1a]">{error}</div>
                <button
                    onClick={() => window.location.reload()}
                    className="text-[12px] text-blue-500 font-semibold"
                >
                    다시 시도
                </button>
            </div>
        );
    }

    if (weeklyPlan.length === 0) {
        return (
            <div className="flex-1 flex flex-col items-center justify-center p-6 gap-3">
                <div className="text-4xl">📋</div>
                <div className="text-[14px] font-bold text-[#1a1a1a]">이번 주 계획이 없어요</div>
                <div className="text-[12px] text-[#8a8680] text-center">
                    AI 계획을 먼저 생성해 주세요
                </div>
            </div>
        );
    }

    // 날짜 범위 텍스트
    const firstDate = weeklyPlan[0]?.date;
    const lastDate = weeklyPlan[weeklyPlan.length - 1]?.date;
    const rangeText = firstDate && lastDate
        ? `${formatDate(firstDate)} — ${formatDate(lastDate)}`
        : "";

    return (
        <div className="flex flex-col h-full">
            {/* 헤더 정보 */}
            <div className="px-5 py-3 bg-white border-b border-[#f0ece5]">
                <div className="flex items-center justify-between">
                    <div>
                        <div className="text-[11px] font-semibold text-[#1a1a1a]">
                            이번 주 운동 계획
                        </div>
                        <div className="text-[10px] text-[#8a8680] mt-0.5">
                            {rangeText} · {weeklyPlan.length}일
                        </div>
                    </div>
                    <div className="bg-[#eef3ff] rounded-xl px-3 py-1.5 text-[10px] font-semibold text-blue-500">
                        AI 추천
                    </div>
                </div>
            </div>

            {/* 주간 목록 */}
            <div className="flex-1 overflow-y-auto px-4 py-4 flex flex-col gap-3 pb-24">
                {weeklyPlan.map((day) => (
                    <DayCard key={day.date} day={day} />
                ))}
            </div>
        </div>
    );
}