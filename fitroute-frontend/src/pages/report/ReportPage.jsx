// // pages/report/ReportPage.jsx
// import { useState } from 'react';
// import TabBar from '../../components/common/TabBar';
// import CalendarView from '../../components/common/CalendarView';
// import DayDetailSheet from '../../components/common/DayDetailSheet';
// import { useCalendar } from '../../hooks/useCalendar';  // 여기로 이동
// import StatsSection from './StatsSection';

// const PERIOD_TABS = [
//     { key: 'daily', label: '일' },
//     { key: 'weekly', label: '주' },
//     { key: 'monthly', label: '월' },
// ];

// const TYPE_TABS = [
//     { key: 'workout', label: '운동' },
//     { key: 'diet', label: '식단' },
// ];

// export default function ReportPage() {
//     const [period, setPeriod] = useState('weekly');
//     const [type, setType] = useState('workout');
//     const [selectedDate, setSelectedDate] = useState(null);
//     const { calendarData, year, month, setMonth } = useCalendar(type);

//     return (
//         <div className="flex flex-col h-full">
//             <TabBar tabs={PERIOD_TABS} active={period} onChange={setPeriod} />
//             <TabBar tabs={TYPE_TABS} active={type} onChange={setType} />

//             <div className="flex-1 overflow-y-auto p-4 space-y-4 pb-24">
//                 {/* 통계 섹션 */}
//                 <StatsSection period={period} type={type} />

//                 {/* 캘린더 (기존 기록 탭에 있던 것) */}
//                 <CalendarView
//                     type={type}
//                     data={calendarData}
//                     year={year}
//                     month={month}
//                     onMonthChange={setMonth}
//                     onSelectDate={setSelectedDate}
//                 />
//             </div>

//             {selectedDate && (
//                 <DayDetailSheet
//                     type={type}
//                     date={selectedDate}
//                     onClose={() => setSelectedDate(null)}
//                 />
//             )}
//         </div>
//     );
// }
// pages/report/ReportPage.jsx
// TODO: 리포트 페이지 추후 구현

export default function ReportPage() {
    return (
        <div className="flex-1 flex items-center justify-center text-gray-400 text-sm">
            리포트 (준비 중)
        </div>
    );
}