// src/pages/workout/WorkoutTodayTab.jsx
import { useState } from "react";
import { useWorkoutToday } from "../../hooks/useWorkoutToday";
import WorkoutItem from "../../components/workout/WorkoutItem";
import WorkoutAddSheet from "../../components/workout/WorkoutAddSheet";

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

export default function WorkoutTodayTab() {
    const { workouts, groupedByCategory, sortedCategories, loading, applyAction, reload } = useWorkoutToday();
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
            <div className="flex-1 flex flex-col items-center justify-center p-6 gap-3">
                <div className="text-4xl">🏋️</div>
                <div className="text-[14px] font-bold text-[#1a1a1a]">오늘 운동이 없어요</div>
                <div className="text-[12px] text-[#8a8680] text-center">
                    계획 탭에서 AI 운동을 생성하거나<br />아래 버튼으로 직접 추가해 보세요
                </div>
                <button
                    onClick={() => setShowAddSheet(true)}
                    className="mt-3 bg-blue-500 text-white text-[12px] font-semibold px-5 py-2.5 rounded-xl"
                >
                    운동 추가하기
                </button>
            </div>
        );
    }

    // 카테고리별 완료 여부
    const categoryProgress = sortedCategories.map((cat) => {
        const items = groupedByCategory[cat];
        const completed = items.filter((w) => w.status !== "PENDING" && w.status !== "SKIPPED").length;
        return { cat, completed, total: items.filter((w) => w.status !== "SKIPPED").length };
    });

    return (
        <div className="flex flex-col h-full">
            {/* 전체 진행률 */}
            <div className="px-5 py-4 bg-white border-b border-[#f0ece5]">
                <div className="flex justify-between mb-2">
                    <span className="text-[12px] font-semibold text-[#1a1a1a]">오늘 운동 진행률</span>
                    <span className="text-[12px] font-bold text-blue-500">
                        {workouts.filter((w) => w.status !== "PENDING" && w.status !== "SKIPPED").length} /
                        {workouts.filter((w) => w.status !== "SKIPPED").length}
                    </span>
                </div>
                <div className="bg-[#edeae5] rounded h-2 overflow-hidden">
                    <div
                        className="h-full bg-blue-500 transition-all duration-500"
                        style={{
                            width: `${workouts.filter((w) => w.status !== "SKIPPED").length > 0
                                    ? Math.round(
                                        (workouts.filter((w) => w.status !== "PENDING" && w.status !== "SKIPPED").length /
                                            workouts.filter((w) => w.status !== "SKIPPED").length) *
                                        100
                                    )
                                    : 0
                                }%`,
                        }}
                    />
                </div>
            </div>

            {/* 카테고리별 운동 리스트 */}
            <div className="flex-1 overflow-y-auto px-5 py-4 flex flex-col gap-5 pb-24">
                {sortedCategories.map((cat) => {
                    const items = groupedByCategory[cat];
                    const { completed, total } = categoryProgress.find((p) => p.cat === cat) || {};

                    return (
                        <div key={cat}>
                            {/* 카테고리 헤더 */}
                            <div className="flex items-center gap-2 mb-3">
                                <div
                                    className="w-2 h-2 rounded-full flex-shrink-0"
                                    style={{ background: WO_COLOR[cat] }}
                                />
                                <span className="text-[12px] font-bold text-[#1a1a1a]">
                                    {WO_BADGE[cat]}
                                </span>
                                <span className="text-[10px] text-[#8a8680] ml-auto">
                                    {completed}/{total}
                                </span>
                            </div>

                            {/* 운동 리스트 */}
                            <div className="bg-white rounded-2xl px-3 divide-y divide-[#f0ece5]">
                                {items.map((item) => (
                                    <WorkoutItem
                                        key={item.id}
                                        item={item}
                                        onActionApply={applyAction}
                                    />
                                ))}
                            </div>
                        </div>
                    );
                })}
            </div>

            {/* 하단 운동 추가 버튼 */}
            <div className="fixed bottom-20 right-5 flex gap-2">
                <button
                    onClick={() => setShowAddSheet(true)}
                    className="w-12 h-12 bg-blue-500 text-white rounded-full flex items-center justify-center shadow-lg hover:bg-blue-600 transition-colors text-xl"
                >
                    +
                </button>
            </div>

            {/* Add Sheet */}
            {showAddSheet && (
                <WorkoutAddSheet
                    onClose={() => setShowAddSheet(false)}
                    onAdd={() => {
                        setShowAddSheet(false);
                        reload();
                    }}
                />
            )}
        </div>
    );
}