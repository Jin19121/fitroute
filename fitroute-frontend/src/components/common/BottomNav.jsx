// src/components/common/BottomNav.jsx
// QuickAddFab 통합 — 하단 메뉴바 중앙에 빠른 추가 버튼 배치
import { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { usePlanStore } from '../../store/planStore';
import QuickAddFab from './QuickAddFab';

const TABS = [
    { label: '홈', icon: '🏠', path: '/dashboard' },
    { label: '운동', icon: '🏋️', path: '/workout' },
    // 중앙에 FAB 배치
    { label: '식단', icon: '🥗', path: '/diet' },
    { label: '리포트', icon: '📊', path: '/report' },
];

export default function BottomNav() {
    const navigate = useNavigate();
    const { pathname } = useLocation();
    const [fabMenuOpen, setFabMenuOpen] = useState(false);

    // 빠른 추가 후 planStore 재조회
    const fetchToday = usePlanStore(s => s.fetchToday);

    const handleDataSaved = () => {
        fetchToday(true);
        window.dispatchEvent(new CustomEvent('fitroute:plan-updated'));
        setFabMenuOpen(false);
    };

    // 탭 분할 (좌측 2개, 우측 2개)
    const leftTabs = TABS.slice(0, 2);
    const rightTabs = TABS.slice(2, 4);

    return (
        <>
            {/* Bottom Navigation Bar — 화면 고정 (fixed, z-20) */}
            <div className="fixed bottom-0 left-0 right-0 bg-white border-t border-[#f0ece5] flex items-center justify-center h-20 z-20">
                {/* 좌측 탭 (홈, 운동) */}
                <div className="flex-1 flex items-center justify-around px-4">
                    {leftTabs.map(({ label, icon, path }) => {
                        const active = pathname.startsWith(path);
                        return (
                            <button
                                key={label}
                                onClick={() => navigate(path)}
                                className="flex flex-col items-center gap-1 py-2"
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

                {/* 중앙 FAB 버튼 */}
                <QuickAddFab
                    onDataSaved={handleDataSaved}
                    isBottomNavIntegrated={true}
                    isMenuOpen={fabMenuOpen}
                    setMenuOpen={setFabMenuOpen}
                />

                {/* 우측 탭 (식단, 리포트) */}
                <div className="flex-1 flex items-center justify-around px-4">
                    {rightTabs.map(({ label, icon, path }) => {
                        const active = pathname.startsWith(path);
                        return (
                            <button
                                key={label}
                                onClick={() => navigate(path)}
                                className="flex flex-col items-center gap-1 py-2"
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
            </div>
        </>
    );
}