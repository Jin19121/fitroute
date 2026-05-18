// src/components/report/MonthCalendar.jsx
import { useMemo } from 'react';

// ─── 날짜별 셀 스타일 계산 ────────────────────────────────────────────────

function getCellStyle(filter, dayData) {
    if (!dayData) return { bg: '', text: 'text-[#d5d0ca]' };

    if (filter === 'WORKOUT') {
        const s = dayData.workout?.status;
        if (s === 'FULL') return { bg: 'bg-[#4a7bff]', text: 'text-white' };
        if (s === 'PART') return { bg: 'bg-[#f59e0b]', text: 'text-white' };
        if (s === 'NONE') return { bg: 'bg-[#e5e1db]', text: 'text-[#8a8680]' };
        return { bg: '', text: 'text-[#1a1a1a]' }; // NO_PLAN
    }

    if (filter === 'DIET') {
        const s = dayData.diet?.status;
        if (s === 'ACHIEVED') return { bg: 'bg-[#1a9e75]', text: 'text-white' };
        if (s === 'EXCEEDED') return { bg: 'bg-[#f59e0b]', text: 'text-white' };
        if (s === 'NO_RECORD') return { bg: 'bg-[#e5e1db]', text: 'text-[#8a8680]' };
        return { bg: '', text: 'text-[#1a1a1a]' }; // NO_PLAN
    }

    if (filter === 'WEIGHT') {
        const measured = dayData.weight?.measured;
        if (measured) return { bg: 'bg-[#ff8c42]', text: 'text-white' };
        return { bg: 'bg-[#e5e1db]', text: 'text-[#8a8680]' };
    }

    return { bg: '', text: 'text-[#1a1a1a]' };
}

// ─── 서브 값 (kcal / 체중) ─────────────────────────────────────────────────

function getSubValue(filter, dayData) {
    if (!dayData) return null;

    if (filter === 'WORKOUT') {
        const kcal = dayData.workout?.burnedKcal;
        return kcal > 0 ? `-${kcal}` : null;
    }
    if (filter === 'DIET') {
        const kcal = dayData.diet?.consumedKcal;
        return kcal > 0 ? `${kcal}` : null;
    }
    if (filter === 'WEIGHT') {
        const kg = dayData.weight?.weightKg;
        return kg != null ? `${kg}` : null;
    }
    return null;
}

// ─── 범례 ─────────────────────────────────────────────────────────────────

const LEGENDS = {
    WORKOUT: [
        { color: 'bg-[#4a7bff]', label: '완료' },
        { color: 'bg-[#f59e0b]', label: '일부' },
        { color: 'bg-[#e5e1db]', label: '안함' },
    ],
    DIET: [
        { color: 'bg-[#1a9e75]', label: '목표달성' },
        { color: 'bg-[#f59e0b]', label: '초과' },
        { color: 'bg-[#e5e1db]', label: '미기록' },
    ],
    WEIGHT: [
        { color: 'bg-[#ff8c42]', label: '측정일' },
        { color: 'bg-[#e5e1db]', label: '미측정' },
    ],
};

const DAY_HEADERS = ['일', '월', '화', '수', '목', '금', '토'];

// ─── 메인 컴포넌트 ────────────────────────────────────────────────────────

export default function MonthCalendar({
    year,
    month,
    days,          // MonthlyReportResponse.days 배열
    filter,
    selectedDate,
    onSelectDate,
}) {
    const todayStr = new Date().toISOString().slice(0, 10);

    // days 배열을 date 키로 맵핑
    const dayMap = useMemo(() => {
        if (!days) return {};
        return Object.fromEntries(days.map(d => [d.date, d]));
    }, [days]);

    // 달력 그리드 — 1일의 요일 기준으로 앞에 빈 셀 삽입
    const firstDow = new Date(year, month - 1, 1).getDay(); // 0=일 ~ 6=토
    const daysInMonth = new Date(year, month, 0).getDate();

    const cells = [
        ...Array(firstDow).fill(null),          // 빈 셀
        ...Array.from({ length: daysInMonth }, (_, i) => i + 1), // 1~말일
    ];

    return (
        <div className="flex flex-col gap-3">
            {/* 요일 헤더 */}
            <div className="grid grid-cols-7 text-center">
                {DAY_HEADERS.map((d, i) => (
                    <span
                        key={d}
                        className={`text-[10px] font-semibold py-1
                            ${i === 0 ? 'text-red-400' : i === 6 ? 'text-blue-400' : 'text-[#8a8680]'}`}
                    >
                        {d}
                    </span>
                ))}
            </div>

            {/* 날짜 그리드 */}
            <div className="grid grid-cols-7 gap-y-2">
                {cells.map((day, idx) => {
                    if (!day) return <div key={`empty-${idx}`} />;

                    const dateStr = `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`;
                    const dayData = dayMap[dateStr];
                    const { bg, text } = getCellStyle(filter, dayData);
                    const subVal = getSubValue(filter, dayData);
                    const isToday = dateStr === todayStr;
                    const isSelected = dateStr === selectedDate;

                    return (
                        <button
                            key={dateStr}
                            onClick={() => onSelectDate(dateStr)}
                            className="flex flex-col items-center gap-0.5"
                        >
                            {/* 날짜 원 */}
                            <div className={[
                                'w-7 h-7 rounded-full flex items-center justify-center text-[11px] font-semibold transition-all',
                                bg || 'bg-transparent',
                                text,
                                isToday && !bg
                                    ? 'ring-2 ring-[#4a7bff] ring-offset-1'
                                    : '',
                                isSelected
                                    ? 'ring-2 ring-offset-1 ring-[#1a1a1a]'
                                    : '',
                            ].join(' ')}>
                                {day}
                            </div>

                            {/* 서브 값 */}
                            {subVal ? (
                                <span className="text-[8px] text-[#8a8680] leading-none">
                                    {subVal}
                                </span>
                            ) : (
                                <span className="text-[8px] leading-none opacity-0">-</span>
                            )}
                        </button>
                    );
                })}
            </div>

            {/* 범례 */}
            <div className="flex items-center gap-3 pt-1 flex-wrap">
                {LEGENDS[filter].map(({ color, label }) => (
                    <div key={label} className="flex items-center gap-1">
                        <div className={`w-2.5 h-2.5 rounded-full ${color}`} />
                        <span className="text-[9px] text-[#8a8680]">{label}</span>
                    </div>
                ))}
                {filter === 'WORKOUT' && (
                    <div className="flex items-center gap-1 ml-auto">
                        <span className="text-[9px] text-[#8a8680]">숫자 = 소모kcal</span>
                    </div>
                )}
                {filter === 'DIET' && (
                    <div className="flex items-center gap-1 ml-auto">
                        <span className="text-[9px] text-[#8a8680]">숫자 = 섭취kcal</span>
                    </div>
                )}
                {filter === 'WEIGHT' && (
                    <div className="flex items-center gap-1 ml-auto">
                        <span className="text-[9px] text-[#8a8680]">숫자 = 체중kg</span>
                    </div>
                )}
            </div>
        </div>
    );
}