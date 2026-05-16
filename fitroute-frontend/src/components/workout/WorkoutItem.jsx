// src/components/workout/WorkoutItem.jsx
import { useState } from "react";
import PlanItemActionSheet from "../PlanItemActionSheet";

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

const STATUS_ICON = {
    COMPLETED: { color: "#2a5cc5", label: "완수" },
    SKIPPED: { color: "#8a8680", label: "미실행" },
    MODIFIED: { color: "#1a9e75", label: "수정" },
};

export default function WorkoutItem({ item, onActionApply }) {
    const [showActionSheet, setShowActionSheet] = useState(false);
    const [showDetailSheet, setShowDetailSheet] = useState(false);

    const done = item.status === "COMPLETED" || item.status === "MODIFIED";
    const skip = item.status === "SKIPPED";
    const muted = done || skip;
    const statusIcon = STATUS_ICON[item.status];

    const displayName = item.effectiveName ?? item.exerciseName;

    const handleApply = async (itemId, payload) => {
        await onActionApply(itemId, payload);
        setShowActionSheet(false);
    };

    return (
        <>
            <div
                className={`flex items-center gap-3 py-3 cursor-pointer select-none transition-colors hover:bg-[#f0ece5] rounded-lg px-2 -mx-2
                    ${item.status === "MODIFIED" ? "border-l-2 border-[#1a9e75] pl-1 -ml-1" : ""}`}
            >
                {/* 상태 체크박스 */}
                <div
                    onClick={() => setShowActionSheet(true)}
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
                <div className="flex-1 min-w-0" onClick={() => setShowDetailSheet(true)}>
                    <div
                        className={`text-[12px] font-medium truncate transition-colors
                            ${muted ? "line-through text-[#b8b4ae]" : "text-[#1a1a1a]"}`}
                    >
                        {displayName}
                    </div>

                    {/* 세트 × 횟수 */}
                    {item.sets && item.reps && (
                        <div className="text-[10px] text-[#8a8680] mt-0.5">
                            {item.sets}세트 × {item.reps}회
                        </div>
                    )}

                    {/* 수정 이력 */}
                    {item.status === "MODIFIED" && item.effectiveName !== item.exerciseName && (
                        <div className="text-[9px] text-[#1a9e75] mt-0.5">
                            원본: {item.exerciseName} → {item.effectiveCalories} kcal
                        </div>
                    )}
                </div>

                {/* 우측 정보 */}
                <div className="flex items-center gap-2 flex-shrink-0">
                    <span className={`text-[11px] font-semibold transition-colors
                        ${done ? "text-[#b8b4ae]" : "text-blue-500"}`}>
                        {item.effectiveCalories ?? item.calories}
                    </span>
                    {statusIcon && (
                        <span
                            className="text-[8px] font-semibold px-1.5 py-0.5 rounded-full"
                            style={{
                                background: statusIcon.color + "20",
                                color: statusIcon.color,
                            }}
                        >
                            {statusIcon.label}
                        </span>
                    )}
                </div>
            </div>

            {/* Action Sheet */}
            <PlanItemActionSheet
                item={showActionSheet ? item : null}
                onClose={() => setShowActionSheet(false)}
                onApply={handleApply}
            />

            {/* Detail Sheet (나중에 추가) */}
            {showDetailSheet && (
                <WorkoutDetailSheet
                    item={item}
                    onClose={() => setShowDetailSheet(false)}
                />
            )}
        </>
    );
}