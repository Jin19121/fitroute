// src/pages/workout/WorkoutPage.jsx
import { useState } from "react";
import BottomNav from "../../components/common/BottomNav";
import WorkoutTodayTab from "./WorkoutTodayTab";
import WorkoutPlanTab from "./WorkoutPlanTab"; // 나중에 구현

const TAB_LIST = [
    { id: "today", label: "오늘" },
    { id: "plan", label: "계획" },
];

export default function WorkoutPage() {
    const [activeTab, setActiveTab] = useState("today");

    return (
        <div className="flex flex-col h-full bg-[#f5f3f0]">
            {/* Header */}
            <div className="bg-[#1a1a1a] px-5 py-4 flex-shrink-0">
                <div className="text-[22px] font-extrabold text-white leading-tight">
                    🏋️ 운동
                </div>
                <div className="text-[11px] text-[#555] mt-1">
                    오늘의 운동 계획을 확인하고 기록해요
                </div>
            </div>

            {/* Tabs */}
            <div className="flex gap-3 px-5 py-4 bg-[#1a1a1a] border-b border-[#333]">
                {TAB_LIST.map(({ id, label }) => (
                    <button
                        key={id}
                        onClick={() => setActiveTab(id)}
                        className={`text-[13px] font-semibold pb-2 border-b-2 transition-colors ${activeTab === id
                            ? "text-white border-blue-500"
                            : "text-[#666] border-transparent"
                            }`}
                    >
                        {label}
                    </button>
                ))}
            </div>

            {/* Content */}
            <div className="flex-1 overflow-y-auto">
                {activeTab === "today" && <WorkoutTodayTab />}
                {activeTab === "plan" && <WorkoutPlanTab />}
            </div>

            {/* Bottom nav */}
            <BottomNav />
        </div>
    );
}