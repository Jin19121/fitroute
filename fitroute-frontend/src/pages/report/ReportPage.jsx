// src/pages/report/ReportPage.jsx
import { useState } from 'react';
import useReport from '../../hooks/useReport';
import MonthCalendar from '../../components/report/MonthCalendar';
import DayDetailCard from '../../components/report/DayDetailCard';
import KpiCards from '../../components/report/KpiCards';
import WeightChart from '../../components/report/WeightChart';
import BottomNav from '../../components/common/BottomNav';

// ─── 상수 ─────────────────────────────────────────────────────────────────

const FILTERS = [
    { key: 'WORKOUT', label: '운동', color: 'bg-[#4a7bff]' },
    { key: 'DIET', label: '식단', color: 'bg-[#1a9e75]' },
    { key: 'WEIGHT', label: '체중', color: 'bg-[#ff8c42]' },
];

// ─── 체중 기록 모달 ────────────────────────────────────────────────────────

function WeightModal({ date, onSubmit, onClose }) {
    const [kg, setKg] = useState('');
    const [note, setNote] = useState('');
    const [loading, setLoading] = useState(false);

    const handleSubmit = async () => {
        const val = parseFloat(kg);
        if (!kg || isNaN(val) || val < 20 || val > 300) return;
        setLoading(true);
        try {
            await onSubmit(date, val, note);
            onClose();
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <div className="fixed inset-0 bg-black/40 z-40" onClick={onClose} />
            <div className="fixed bottom-0 left-0 right-0 z-50 bg-white rounded-t-3xl px-5 pt-4 pb-10"
                style={{ animation: 'slideUp 0.22s ease-out' }}>
                <style>{`@keyframes slideUp{from{transform:translateY(100%)}to{transform:translateY(0)}}`}</style>
                <div className="w-9 h-1 bg-[#d5d0ca] rounded-full mx-auto mb-5" />
                <p className="text-[14px] font-bold text-[#1a1a1a] mb-4">
                    {date?.slice(5).replace('-', '/')} 체중 기록
                </p>
                <div className="flex flex-col gap-3">
                    <div>
                        <label className="text-[11px] text-[#8a8680] mb-1 block">체중 (kg)</label>
                        <input
                            type="number"
                            value={kg}
                            onChange={e => setKg(e.target.value)}
                            placeholder="예: 67.5"
                            min={20} max={300} step={0.1}
                            className="w-full border border-[#edeae5] rounded-xl px-3 py-2.5 text-[14px] outline-none focus:border-[#ff8c42]"
                            autoFocus
                        />
                    </div>
                    <div>
                        <label className="text-[11px] text-[#8a8680] mb-1 block">메모 (선택)</label>
                        <input
                            type="text"
                            value={note}
                            onChange={e => setNote(e.target.value)}
                            placeholder="오늘 컨디션은 어땠나요?"
                            maxLength={200}
                            className="w-full border border-[#edeae5] rounded-xl px-3 py-2.5 text-[13px] outline-none focus:border-[#ff8c42]"
                        />
                    </div>
                    <button
                        onClick={handleSubmit}
                        disabled={loading || !kg}
                        className="w-full py-3 rounded-2xl text-[14px] font-semibold text-white bg-[#ff8c42] disabled:bg-[#d5d0ca] mt-1"
                    >
                        {loading ? '저장 중...' : '저장'}
                    </button>
                </div>
            </div>
        </>
    );
}

// ─── 메인 페이지 ──────────────────────────────────────────────────────────

export default function ReportPage() {
    const {
        year, month, filter, selectedDate,
        monthlyData, dayDetail,
        loading, detailLoading, error,
        isCurrentMonth,
        setFilter,
        selectDate,
        prevMonth, nextMonth,
        submitWeight,
    } = useReport();

    const [weightModalDate, setWeightModalDate] = useState(null);

    const handleRecordWeight = (date) => setWeightModalDate(date ?? selectedDate);

    if (loading) {
        return (
            <div className="flex flex-col h-full bg-[#f5f3f0] relative">
                <div className="flex-1 flex items-center justify-center">
                    <div className="w-6 h-6 border-2 border-[#4a7bff] border-t-transparent rounded-full animate-spin" />
                </div>
                <BottomNav />
            </div>
        );
    }

    if (error) {
        return (
            <div className="flex flex-col h-full bg-[#f5f3f0] relative">
                <div className="flex-1 flex flex-col items-center justify-center gap-2 p-6">
                    <p className="text-[13px] font-medium text-[#1a1a1a]">오류가 발생했어요</p>
                    <p className="text-[11px] text-[#8a8680]">{error}</p>
                </div>
                <BottomNav />
            </div>
        );
    }

    const summary = monthlyData?.summary;
    const measurements = summary?.weight?.measurements ?? [];

    return (
        <div className="flex flex-col h-full bg-[#f5f3f0] relative">

            {/* 헤더 */}
            <div className="bg-[#1a1a1a] px-5 pb-4 flex-shrink-0">
                <div className="py-3">
                    <p className="text-[11px] text-[#666]">FitRoute</p>
                    <p className="text-[20px] font-extrabold text-white mt-0.5">리포트</p>
                </div>

                {/* 월 네비게이션 */}
                <div className="flex items-center justify-between mb-4">
                    <button
                        onClick={prevMonth}
                        className="w-7 h-7 flex items-center justify-center rounded-full bg-white/10"
                    >
                        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                            <path d="M9 2L4 7L9 12" stroke="#fff" strokeWidth="1.6" strokeLinecap="round" />
                        </svg>
                    </button>
                    <span className="text-[14px] font-bold text-white">
                        {year}.{String(month).padStart(2, '0')}
                    </span>
                    <button
                        onClick={nextMonth}
                        disabled={isCurrentMonth}
                        className="w-7 h-7 flex items-center justify-center rounded-full bg-white/10 disabled:opacity-30"
                    >
                        <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                            <path d="M5 2L10 7L5 12" stroke="#fff" strokeWidth="1.6" strokeLinecap="round" />
                        </svg>
                    </button>
                </div>

                {/* 필터 Pill */}
                <div className="flex gap-2">
                    {FILTERS.map(({ key, label, color }) => (
                        <button
                            key={key}
                            onClick={() => setFilter(key)}
                            className={[
                                'flex-1 py-2 rounded-xl text-[12px] font-semibold transition-all',
                                filter === key
                                    ? `${color} text-white`
                                    : 'bg-white/10 text-[#888]',
                            ].join(' ')}
                        >
                            {label}
                        </button>
                    ))}
                </div>
            </div>

            {/* 바디 */}
            <div className="flex-1 overflow-y-auto px-4 py-4 flex flex-col gap-4 pb-24">

                {/* 캘린더 */}
                <div className="bg-white rounded-2xl p-4">
                    <MonthCalendar
                        year={year}
                        month={month}
                        days={monthlyData?.days}
                        filter={filter}
                        selectedDate={selectedDate}
                        onSelectDate={selectDate}
                    />
                </div>

                {/* 날짜 상세 카드 */}
                {(selectedDate || detailLoading) && (
                    <DayDetailCard
                        dayDetail={dayDetail}
                        loading={detailLoading}
                        filter={filter}
                        onRecordWeight={() => handleRecordWeight(selectedDate)}
                    />
                )}

                {/* KPI 카드 */}
                <KpiCards filter={filter} summary={summary} />

                {/* 체중 추이 차트 — 체중 필터일 때만 */}
                {filter === 'WEIGHT' && (
                    <WeightChart measurements={measurements} />
                )}

            </div>

            <BottomNav />

            {/* 체중 기록 모달 */}
            {weightModalDate && (
                <WeightModal
                    date={weightModalDate}
                    onSubmit={submitWeight}
                    onClose={() => setWeightModalDate(null)}
                />
            )}
        </div>
    );
}