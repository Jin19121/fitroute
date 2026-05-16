// src/pages/workout/WorkoutPlanTab.jsx
// 백엔드 API 준비 후 구현
// - GET /api/workout/plan/weekly
// - PATCH /api/workout/plan/{date}
// - POST /api/workout/plan/regenerate
// 추후 작성할 것

export default function WorkoutPlanTab() {
    return (
        <div className="flex-1 flex items-center justify-center p-6">
            <div className="text-center">
                <div className="text-4xl mb-3">🔨</div>
                <div className="text-[14px] font-bold text-[#1a1a1a]">계획 탭은 준비 중입니다</div>
                <div className="text-[12px] text-[#8a8680] mt-2">
                    백엔드 주간 API가 완성되면<br />계획 관리 기능이 활성화됩니다
                </div>
            </div>
        </div>
    );
}