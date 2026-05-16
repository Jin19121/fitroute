// src/pages/diet/FoodItem.jsx
import { useState } from 'react';
import { usePlanStore } from '../../store/planStore';
import { patchPlanItem } from '../../api/diet';

export default function FoodItem({ planItem, isLast, onTap }) {
    const [pending, setPending] = useState(false);
    const applyAction = usePlanStore(s => s.applyAction);

    const done = planItem.status === 'COMPLETED' || planItem.status === 'MODIFIED';
    const skipped = planItem.status === 'SKIPPED';

    const handleToggle = async (e) => {
        e.stopPropagation(); // ← 이벤트 버블링 차단
        if (pending) return;
        const nextStatus = done ? 'PENDING' : 'COMPLETED';
        setPending(true);
        try {
            await applyAction(planItem.id, { status: nextStatus });
        } finally {
            setPending(false);
        }
    };

    const handleTap = (e) => {
        e.stopPropagation(); // ← 체크 토글과 분리
        onTap?.(planItem);
    };

    return (
        <div style={{
            display: 'flex', alignItems: 'center', gap: 7,
            padding: '6px 0',
            borderBottom: isLast ? 'none' : '1px solid #F2EEE8',
            opacity: pending ? 0.6 : 1,
        }}>
            {/* 체크 서클 — 클릭 시 토글만 */}
            <button onClick={handleToggle} disabled={pending} style={{
                width: 17, height: 17, borderRadius: '50%', flexShrink: 0,
                border: done ? 'none' : '1.5px solid #D5D0CA',
                background: done ? '#4A7BFF' : skipped ? '#F0ECE5' : 'transparent',
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                cursor: 'pointer', padding: 0,
            }}>
                {done && <svg width="9" height="9" viewBox="0 0 9 9" fill="none"><path d="M1.5 4.5L3.8 7L7.5 2" stroke="#fff" strokeWidth="1.4" strokeLinecap="round" /></svg>}
                {skipped && <svg width="8" height="8" viewBox="0 0 8 8" fill="none"><path d="M2 2L6 6M6 2L2 6" stroke="#8A8680" strokeWidth="1.2" strokeLinecap="round" /></svg>}
            </button>

            {/* 음식명 — 클릭 시 액션시트 */}
            <button onClick={handleTap} style={{
                flex: 1, minWidth: 0, textAlign: 'left',
                background: 'none', border: 'none', padding: 0, cursor: 'pointer',
            }}>
                <div style={{
                    fontSize: 10, fontWeight: 500,
                    color: done ? '#B8B4AE' : '#1A1A1A',
                    textDecoration: done ? 'line-through' : 'none',
                    overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
                }}>
                    {planItem.effectiveName ?? planItem.foodName}
                </div>
                {planItem.status === 'MODIFIED' && (
                    <div style={{ fontSize: 8, color: '#1A9E75', marginTop: 1 }}>
                        수정됨 · {planItem.effectiveCalories} kcal
                    </div>
                )}
            </button>

            <span style={{ fontSize: 10, fontWeight: 600, flexShrink: 0, color: done ? '#B8B4AE' : '#4A7BFF' }}>
                {planItem.effectiveCalories ?? planItem.calories}
            </span>

            {/* 수정 버튼 — 클릭 시 액션시트 */}
            <button onClick={handleTap} style={{
                fontSize: 8, color: '#4A7BFF', flexShrink: 0,
                background: 'none', border: 'none', cursor: 'pointer', padding: '2px 4px',
            }}>
                수정
            </button>
        </div>
    );
}