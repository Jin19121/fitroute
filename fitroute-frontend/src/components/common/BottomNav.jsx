// src/components/common/BottomNav.jsx
// QuickAddFab 통합 — 모든 메인 페이지에서 공유
import { useNavigate, useLocation } from 'react-router-dom';
import QuickAddFab from './QuickAddFab';
import { usePlanStore } from '../../store/planStore';

const TABS = [
    { label: '홈', icon: '🏠', path: '/dashboard' },
    { label: '운동', icon: '🏋️', path: '/workout' },
    { label: '식단', icon: '🥗', path: '/diet' },
    { label: '리포트', icon: '📊', path: '/report' },
];

export default function BottomNav() {
    const navigate = useNavigate();
    const { pathname } = useLocation();

    // 빠른 추가 후 planStore 재조회 (대시보드·운동·식단 페이지 모두 동기화)
    const fetchToday = usePlanStore(s => s.fetchToday);

    const handleDataSaved = () => {
        fetchToday(true); // force=true → 캐시 무시 재조회
    };

    return (
        <>
            {/* FAB — BottomNav 위에 항상 표시 */}
            <QuickAddFab onDataSaved={handleDataSaved} />

            {/* Bottom Navigation Bar */}
            <div className="absolute bottom-0 left-0 right-0 bg-white border-t border-[#f0ece5] flex py-2 pb-5 z-10">
                {TABS.map(({ label, icon, path }) => {
                    const active = pathname.startsWith(path);
                    return (
                        <button
                            key={label}
                            onClick={() => navigate(path)}
                            className="flex-1 flex flex-col items-center gap-1"
                        >
                            <div className={`w-7 h-7 rounded-2xl flex items-center justify-center text-xs transition-colors ${active ? 'bg-blue-500' : 'bg-[#f0ece5]'
                                }`}>
                                <span>{icon}</span>
                            </div>
                            <span className={`text-[8px] transition-colors ${active ? 'text-blue-500 font-semibold' : 'text-[#b8b4ae]'
                                }`}>
                                {label}
                            </span>
                        </button>
                    );
                })}
            </div>
        </>
    );
}