// src/pages/workout/WorkoutTodayTab.jsx
import { useState } from "react";
import { useWorkoutToday } from "../../hooks/useWorkoutToday";
import PlanItemActionSheet from "../../components/PlanItemActionSheet";
import WorkoutAddSheet from "../../components/workout/WorkoutAddSheet";

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

// ─── 변경: MODIFIED / EDITED 제거, isModified 플래그로 판단 ──────────────
const STATUS_CONFIG = {
    COMPLETED: { color: "#2a5cc5", label: "완수", bg: "#eef3ff" },
    SKIPPED: { color: "#8a8680", label: "미실행", bg: "#f0ece5" },
};

// ── 단일 운동 아이템 ──────────────────────────────
function WorkoutItem({ item, onTap }) {
    const [open, setOpen] = useState(false);

    // ─── 변경: done 판정을 status === "COMPLETED" 단일 조건으로 단순화 ───
    const done = item.status === "COMPLETED";
    const skip = item.status === "SKIPPED";
    const muted = done || skip;

    const statusCfg = STATUS_CONFIG[item.status];
    const color = WO_COLOR[item.category] ?? "#9ca3af";
    const badge = WO_BADGE[item.category] ?? item.category;
    const displayName = item.effectiveName ?? item.exerciseName;

    return (
        <div
            className={`bg-white rounded-2xl overflow-hidden
                ${item.isModified ? "border-l-2 border-[#1a9e75]" : ""}`}
        >
            {/* 헤더 행 */}
            <div className="flex items-center gap-3 px-4 py-3">
                {/* 체크박스 */}
                <div
                    onClick={() => onTap(item)}
                    className={`w-5 h-5 rounded-full flex-shrink-0 flex items-center justify-center cursor-pointer transition-all
                        ${done ? "bg-blue-500" : skip ? "bg-[#f0ece5]" : "border-2 border-[#d5d0ca] hover:border-blue-400"}`}
                >
                    {(done || skip) && (
                        <svg width="9" height="9" viewBox="0 0 9 9" fill="none">
                            <path
                                d="M1.5 4.5L3.8 7L7.5 2"
                                stroke={done ? "#fff" : "#8a8680"}
                                strokeWidth="1.4"
                                strokeLinecap="round"
                            />
                        </svg>
                    )}
                </div>

                {/* 운동 정보 */}
                <div
                    className="flex-1 min-w-0 cursor-pointer"
                    onClick={() => setOpen((p) => !p)}
                >
                    <div className={`text-[12px] font-semibold truncate
                        ${muted ? "line-through text-[#b8b4ae]" : "text-[#1a1a1a]"}`}>
                        {displayName}
                    </div>
                    <div className="flex items-center gap-1.5 mt-0.5">
                        <span
                            className="text-[7px] font-semibold px-1.5 py-0.5 rounded-full"
                            style={{ background: color + "18", color }}
                        >
                            {badge}
                        </span>
                        {item.sets > 0 && item.reps > 0 && (
                            <span className="text-[10px] text-[#b8b4ae]">
                                {item.sets}×{item.reps}
                            </span>
                        )}
                        {item.durationMin > 0 && (
                            <span className="text-[10px] text-[#b8b4ae]">
                                {item.durationMin}분
                            </span>
                        )}
                        {/* ─── 변경: isModified 플래그로 수정 뱃지 표시 ─── */}
                        {item.isModified && (
                            <span className="text-[7px] font-semibold px-1.5 py-0.5 rounded-full bg-[#edfaf3] text-[#1a6b40]">
                                수정됨
                            </span>
                        )}
                    </div>
                </div>

                {/* 우측 */}
                <div className="flex items-center gap-2 flex-shrink-0">
                    <span className={`text-[11px] font-semibold
                        ${muted ? "text-[#b8b4ae]" : "text-blue-500"}`}>
                        {item.effectiveCalories ?? item.calories}kcal
                    </span>
                    {statusCfg && (
                        <span
                            className="text-[8px] font-semibold px-1.5 py-0.5 rounded-full"
                            style={{ background: statusCfg.bg, color: statusCfg.color }}
                        >
                            {statusCfg.label}
                        </span>
                    )}
                    <svg
                        width="12" height="12" viewBox="0 0 12 12" fill="none"
                        className="transition-transform duration-200"
                        style={{ transform: open ? "rotate(180deg)" : "rotate(0deg)" }}
                        onClick={() => setOpen((p) => !p)}
                    >
                        <path d="M2.5 4.5L6 8L9.5 4.5" stroke="#b8b4ae" strokeWidth="1.4" strokeLinecap="round" />
                    </svg>
                </div>
            </div>

            {/* 상세 펼침 */}
            {open && (
                <div className="px-4 pb-3 border-t border-[#f0ece5]">
                    <div className="pt-3 flex flex-col gap-2">
                        {item.sets > 0 && item.reps > 0 && (
                            <div className="flex gap-2">
                                {[
                                    { label: "세트", val: item.effectiveSets ?? item.sets },
                                    { label: "횟수", val: item.effectiveReps ?? item.reps },
                                    ...(item.durationMin > 0
                                        ? [{ label: "분", val: item.durationMin }]
                                        : []),
                                ].map(({ label, val }) => (
                                    <div
                                        key={label}
                                        className="flex-1 bg-[#f5f3f0] rounded-xl p-2 text-center"
                                    >
                                        <div className="text-[14px] font-bold text-[#1a1a1a]">{val}</div>
                                        <div className="text-[9px] text-[#8a8680]">{label}</div>
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* ─── 변경: isModified + 이름 변경 여부로 수정 이력 표시 ─── */}
                        {item.isModified && item.effectiveName !== item.exerciseName && (
                            <div className="text-[9px] text-[#1a9e75]">
                                원본: {item.exerciseName} → {item.effectiveCalories}kcal
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}

// ── 카테고리 그룹 ─────────────────────────────────
function CategoryGroup({ category, items, onTap }) {
    const color = WO_COLOR[category] ?? "#9ca3af";
    const badge = WO_BADGE[category] ?? category;

    // ─── 변경: 완수 판정을 status === "COMPLETED" 단일 조건으로 단순화 ───
    const completed = items.filter((w) => w.status === "COMPLETED").length;
    const total = items.filter((w) => w.status !== "SKIPPED").length;

    return (
        <div>
            <div className="flex items-center gap-2 mb-2 px-1">
                <div className="w-2 h-2 rounded-sm flex-shrink-0" style={{ background: color }} />
                <span className="text-[11px] font-bold text-[#1a1a1a]">{badge}</span>
                <span className="text-[10px] text-[#8a8680] ml-auto">{completed}/{total}</span>
            </div>
            <div className="flex flex-col gap-2">
                {items.map((item) => (
                    <WorkoutItem key={item.id} item={item} onTap={onTap} />
                ))}
            </div>
        </div>
    );
}

// ── 메인 컴포넌트 ─────────────────────────────────
export default function WorkoutTodayTab() {
    const { workouts, groupedByCategory, sortedCategories, loading, applyAction, reload } =
        useWorkoutToday();
    const [activeItem, setActiveItem] = useState(null);
    const [showAddSheet, setShowAddSheet] = useState(false);

    if (loading) {
        return (
            <div className="flex-1 flex items-center justify-center text-[13px] text-[#8a8680]">
                로딩 중...
            </div>
        );
    }

    if (workouts.length === 0) {
        return (
            <>
                <div className="flex-1 flex flex-col items-center justify-center p-6 gap-3">
                    <div className="text-4xl">🏋️</div>
                    <div className="text-[14px] font-bold text-[#1a1a1a]">오늘 운동이 없어요</div>
                    <div className="text-[12px] text-[#8a8680] text-center">
                        계획 탭에서 AI 운동을 생성하거나<br />직접 추가해 보세요
                    </div>
                    <button
                        onClick={() => setShowAddSheet(true)}
                        className="mt-3 bg-blue-500 text-white text-[12px] font-semibold px-5 py-2.5 rounded-xl"
                    >
                        운동 추가하기
                    </button>
                </div>

                {showAddSheet && (
                    <WorkoutAddSheet
                        onClose={() => setShowAddSheet(false)}
                        onAdd={() => { setShowAddSheet(false); reload(); }}
                    />
                )}
            </>
        );
    }

    // ─── 변경: 진행률 계산을 status === "COMPLETED" 단일 조건으로 단순화 ───
    const totalActive = workouts.filter((w) => w.status !== "SKIPPED").length;
    const totalDone = workouts.filter((w) => w.status === "COMPLETED").length;
    const pct = totalActive > 0 ? Math.round((totalDone / totalActive) * 100) : 0;

    return (
        <>
            <div className="flex flex-col h-full">
                {/* 진행률 바 */}
                <div className="px-5 py-3 bg-white border-b border-[#f0ece5] flex-shrink-0">
                    <div className="flex justify-between mb-1.5">
                        <span className="text-[11px] font-semibold text-[#1a1a1a]">오늘 진행률</span>
                        <span className="text-[11px] font-bold text-blue-500">
                            {totalDone}/{totalActive} 완료
                        </span>
                    </div>
                    <div className="bg-[#edeae5] rounded-full h-2 overflow-hidden">
                        <div
                            className="h-full bg-blue-500 rounded-full transition-all duration-500"
                            style={{ width: `${pct}%` }}
                        />
                    </div>
                </div>

                {/* 운동 리스트 */}
                <div className="flex-1 overflow-y-auto px-4 py-4 flex flex-col gap-5 pb-24">
                    {sortedCategories.map((cat) => (
                        <CategoryGroup
                            key={cat}
                            category={cat}
                            items={groupedByCategory[cat]}
                            onTap={setActiveItem}
                        />
                    ))}
                </div>
            </div>

            {/* Action Sheet */}
            <PlanItemActionSheet
                item={activeItem}
                onClose={() => setActiveItem(null)}
                onApply={applyAction}
            />

            {/* Add Sheet */}
            {showAddSheet && (
                <WorkoutAddSheet
                    onClose={() => setShowAddSheet(false)}
                    onAdd={() => { setShowAddSheet(false); reload(); }}
                />
            )}
        </>
    );
}