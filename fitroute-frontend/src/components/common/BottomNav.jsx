// components/common/BottomNav.jsx
import { useNavigate, useLocation } from 'react-router-dom';

const TABS = [
{ label: '홈', icon: '🏠', path: '/dashboard' },
{ label: '운동', icon: '🏋️', path: '/workout' },
{ label: '식단', icon: '🥗', path: '/diet' },
{ label: '리포트',icon: '📊', path: '/report' },
];

export default function BottomNav() {
const navigate = useNavigate();
const { pathname } = useLocation();

return (
<div className="absolute bottom-0 left-0 right-0 bg-white border-t border-[#f0ece5] flex py-2 pb-5">
    {TABS.map(({ label, icon, path }) => {
    const active = pathname.startsWith(path);
    return (
    <button key={label} onClick={()=> navigate(path)}
        className="flex-1 flex flex-col items-center gap-1"
        >
        <div className={`w-7 h-7 rounded-2xl flex items-center justify-center text-xs ${active ? 'bg-blue-500'
            : 'bg-[#f0ece5]' }`}>
            <span>{icon}</span>
        </div>
        <span className={`text-[8px] ${active ? 'text-blue-500 font-semibold' : 'text-[#b8b4ae]' }`}>
            {label}
        </span>
    </button>
    );
    })}
</div>
);
}