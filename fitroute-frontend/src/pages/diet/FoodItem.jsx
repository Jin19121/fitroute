// components/diet/FoodItem.jsx
import { useState } from 'react';
import { usePlanStore } from '../../store/planStore';
import { patchPlanItem } from '../../api/diet';

const STATUS_MAP = {
    DONE: { icon: '✅', next: 'SKIPPED' },
    SKIPPED: { icon: '⬜', next: 'DONE' },
};

export default function FoodItem({ planItem, logItem, onClickDetail }) {
    const updateLogItem = usePlanStore(s => s.updateLogItem);
    const [pending, setPending] = useState(false);

    const status = logItem?.status ?? 'SKIPPED';

    const handleToggle = async () => {
        const nextStatus = STATUS_MAP[status]?.next ?? 'DONE';
        // 낙관적 업데이트 먼저
        updateLogItem(logItem?.id, { status: nextStatus });

        setPending(true);
        try {
            await patchPlanItem(logItem.id, { status: nextStatus });
        } catch {
            // 실패 시 롤백
            updateLogItem(logItem?.id, { status });
        } finally {
            setPending(false);
        }
    };

    return (
        <div className="flex items-center px-4 py-3 gap-3">
            {/* 체크 토글 */}
            <button
                onClick={handleToggle}
                disabled={pending}
                className="text-xl flex-shrink-0"
            >
                {STATUS_MAP[status]?.icon ?? '⬜'}
            </button>

            {/* 음식 정보 — 클릭 시 상세 */}
            <button
                onClick={onClickDetail}
                className="flex-1 text-left"
            >
                <p className={`text-sm font-medium ${status === 'DONE' ? 'line-through text-gray-400' : 'text-gray-800'}`}>
                    {planItem.foodName}
                </p>
                <p className="text-xs text-gray-400 mt-0.5">{planItem.calories} kcal</p>
            </button>

            {/* 수정 버튼 */}
            <button className="text-gray-300 text-sm hover:text-gray-500">
                편집
            </button>
        </div>
    );
}