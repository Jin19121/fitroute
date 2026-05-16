// src/components/workout/WorkoutDetailSheet.jsx
import { useState } from "react";

export default function WorkoutDetailSheet({ item, onClose }) {
    if (!item) return null;

    const displayName = item.effectiveName ?? item.exerciseName;

    return (
        <>
            <div className="fixed inset-0 bg-black/40 z-40" onClick={onClose} />

            <div className="fixed bottom-0 left-0 right-0 z-50 bg-white rounded-t-3xl px-5 pt-4 pb-8 shadow-2xl"
                style={{ animation: "slideUp 0.22s ease-out" }}>
                <style>{`@keyframes slideUp{from{transform:translateY(100%)}to{transform:translateY(0)}}`}</style>

                <div className="w-9 h-1 bg-[#d5d0ca] rounded-full mx-auto mb-4" />

                <div className="mb-4 pb-4 border-b border-[#f0ece5]">
                    <div className="text-[14px] font-semibold text-[#1a1a1a]">{displayName}</div>
                    <div className="text-[11px] text-[#8a8680] mt-1">
                        {item.sets && item.reps && (
                            <>
                                {item.sets}세트 × {item.reps}회 · {item.calories} kcal
                            </>
                        )}
                    </div>
                </div>

                {/* 추후 추가 */}
                <div className="text-[12px] text-[#8a8680]">
                    <p className="mb-2">💡 팁: 올바른 자세로 천천히 진행하세요.</p>
                    <p>영상은 운동 DB 완성 후 추가될 예정입니다.</p>
                </div>

                <button
                    onClick={onClose}
                    className="w-full mt-6 bg-[#f5f3f0] text-[#1a1a1a] text-[13px] font-semibold py-3 rounded-xl"
                >
                    닫기
                </button>
            </div>
        </>
    );
}