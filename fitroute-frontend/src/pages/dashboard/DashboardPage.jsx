// src/pages/dashboard/DashboardPage.jsx
import useAuth from '../../hooks/useAuth';

const ProgressBar = ({ label, value, max, color }) => {
    const pct = Math.min((value / max) * 100, 100);
    return (
        <div className="mb-2 last:mb-0">
            <div className="flex justify-between mb-1">
                <span className="text-[11px] text-[#6B6866]">{label}</span>
                <span className="text-[11px] font-semibold text-[#1A1A1A]">
                    {value.toLocaleString()} / {max.toLocaleString()} kcal
                </span>
            </div>
            <div className="h-[5px] bg-[#EDEAE5] rounded-full overflow-hidden">
                <div
                    className="h-full rounded-full transition-all duration-700"
                    style={{ width: `${pct}%`, background: color }}
                />
            </div>
        </div>
    );
};

const Ring = ({ percent, consumed, goal }) => {
    const r = 21;
    const circ = 2 * Math.PI * r;
    const dash = (percent / 100) * circ;
    return (
        <svg width="52" height="52" viewBox="0 0 52 52">
            <circle cx="26" cy="26" r={r} fill="none" stroke="rgba(255,255,255,.25)" strokeWidth="5" />
            <circle
                cx="26" cy="26" r={r}
                fill="none"
                stroke="white"
                strokeWidth="5"
                strokeDasharray={`${dash} ${circ - dash}`}
                strokeLinecap="round"
                transform="rotate(-90 26 26)"
            />
            <text x="26" y="30" textAnchor="middle" fontSize="10" fontWeight="700" fill="white">
                {percent}%
            </text>
        </svg>
    );
};

const NavItem = ({ icon, label, active }) => (
    <div className="flex-1 flex flex-col items-center gap-0.5">
        <div className={`w-7 h-7 rounded-[8px] flex items-center justify-center ${active ? 'bg-[#4A7BFF]' : 'bg-[#F2EEE8]'}`}>
            <span className="text-sm">{icon}</span>
        </div>
        <span className={`text-[10px] font-${active ? 'semibold' : 'normal'} ${active ? 'text-[#4A7BFF]' : 'text-[#B8B4AE]'}`}>
            {label}
        </span>
    </div>
);

const DashboardPage = () => {
    const { logout, isLoading } = useAuth();

    return (
        <div className="min-h-screen bg-[#E4E1DC] flex items-center justify-center">
            {/* Phone frame */}
            <div className="w-[390px] bg-[#1A1A1A] rounded-[38px] p-[9px] shadow-2xl">
                <div className="bg-[#F9F7F5] rounded-[30px] overflow-hidden flex flex-col" style={{ height: 780 }}>

                    {/* Dark header */}
                    <div className="bg-[#1A1A1A] px-4 pb-5">
                        <div className="flex justify-between items-center py-3">
                            <span className="text-[11px] text-[#888]">안녕하세요, 지민님 👋</span>
                            <button
                                onClick={logout}
                                disabled={isLoading}
                                className="w-8 h-8 bg-[#4A7BFF] rounded-full flex items-center justify-center text-white text-[11px] font-bold"
                            >
                                지민
                            </button>
                        </div>
                        <h1 className="text-[20px] font-bold text-white leading-tight">
                            오늘도<br />잘 해봐요!
                        </h1>
                        <p className="text-[11px] text-[#555] mt-1">D-62 · 이번 주 달성률 71%</p>
                        <div className="flex gap-2 mt-3">
                            {[
                                { n: '65.0', u: 'kg', l: '목표 체중' },
                                { n: '-7.0', u: 'kg', l: '남은 감량' },
                                { n: '12', u: '주', l: '목표 기간' },
                            ].map((g) => (
                                <div key={g.l} className="flex-1 bg-white/8 rounded-[10px] p-2 text-center" style={{ background: 'rgba(255,255,255,0.08)' }}>
                                    <div className="text-[14px] font-bold text-white">
                                        {g.n}<span className="text-[10px] font-normal text-[#666]"> {g.u}</span>
                                    </div>
                                    <div className="text-[9px] text-[#555] mt-0.5">{g.l}</div>
                                </div>
                            ))}
                        </div>
                    </div>

                    {/* Scrollable body */}
                    <div className="flex-1 overflow-y-auto px-3 py-3 flex flex-col gap-3">

                        {/* Calorie ring card */}
                        <div className="bg-[#4A7BFF] rounded-[14px] p-3 flex items-center gap-3 shadow-lg shadow-[#4A7BFF]/30">
                            <Ring percent={60} consumed={792} goal={1320} />
                            <div>
                                <p className="text-[10px] text-white/70">오늘 섭취</p>
                                <p className="text-[22px] font-bold text-white leading-none">
                                    792 <span className="text-[13px] font-normal text-white/70">kcal</span>
                                </p>
                                <p className="text-[10px] text-white/60 mt-0.5">목표 1,320 kcal</p>
                                <span className="inline-block mt-1 bg-white/20 rounded-full text-[10px] text-white px-2 py-0.5">
                                    528 kcal 남음
                                </span>
                            </div>
                        </div>

                        {/* Meal section */}
                        <div>
                            <div className="flex justify-between items-center mb-2">
                                <span className="text-[13px] font-bold text-[#1A1A1A]">🥗 식단</span>
                                <span className="text-[11px] text-[#4A7BFF] cursor-pointer">상세보기</span>
                            </div>
                            <div className="bg-white rounded-[14px] p-3">
                                {[
                                    { time: '아침', kcal: '320', done: true, food: '오트밀 + 바나나', color: '#FFF1E6', tc: '#B55A00' },
                                    { time: '점심', kcal: '472', done: true, food: '닭가슴살 샐러드', color: '#EEF3FF', tc: '#2A5CC5' },
                                    { time: '저녁', kcal: '미기록', done: false, food: '두부 된장국 + 나물', color: '#EDFAF3', tc: '#1A6B40' },
                                ].map((meal) => (
                                    <div key={meal.time} className="flex items-center gap-2 py-2 border-b border-[#F2EEE8] last:border-0">
                                        <div className={`w-5 h-5 rounded-full flex items-center justify-center flex-shrink-0 ${meal.done ? 'bg-[#4A7BFF]' : 'border border-[#D5D0CA]'}`}>
                                            {meal.done && (
                                                <svg width="9" height="9" viewBox="0 0 9 9" fill="none">
                                                    <path d="M1.5 4.5L3.8 7L7.5 2" stroke="white" strokeWidth="1.4" strokeLinecap="round" />
                                                </svg>
                                            )}
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <p className={`text-[12px] font-medium truncate ${meal.done ? 'text-[#B8B4AE] line-through' : 'text-[#1A1A1A]'}`}>
                                                {meal.food}
                                            </p>
                                        </div>
                                        <span className="inline-block text-[9px] font-semibold px-2 py-0.5 rounded-full" style={{ background: meal.color, color: meal.tc }}>
                                            {meal.time}
                                        </span>
                                        <span className="text-[11px] font-semibold text-[#B8B4AE] ml-1">{meal.kcal}</span>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Exercise section */}
                        <div>
                            <div className="flex justify-between items-center mb-2">
                                <span className="text-[13px] font-bold text-[#1A1A1A]">🏋️ 운동</span>
                                <span className="text-[11px] text-[#4A7BFF] cursor-pointer">상세보기</span>
                            </div>
                            <div className="bg-white rounded-[14px] p-3">
                                {[
                                    { name: '런닝머신', sub: '30분 · 280 kcal', done: true },
                                    { name: '상체 근력', sub: '3세트 × 5종목', done: false },
                                ].map((ex) => (
                                    <div key={ex.name} className="flex items-center gap-2 py-2 border-b border-[#F2EEE8] last:border-0">
                                        <div className={`w-5 h-5 rounded-full flex items-center justify-center flex-shrink-0 ${ex.done ? 'bg-[#4A7BFF]' : 'border border-[#D5D0CA]'}`}>
                                            {ex.done && (
                                                <svg width="9" height="9" viewBox="0 0 9 9" fill="none">
                                                    <path d="M1.5 4.5L3.8 7L7.5 2" stroke="white" strokeWidth="1.4" strokeLinecap="round" />
                                                </svg>
                                            )}
                                        </div>
                                        <div>
                                            <p className={`text-[12px] font-medium ${ex.done ? 'text-[#B8B4AE] line-through' : 'text-[#1A1A1A]'}`}>{ex.name}</p>
                                            <p className="text-[10px] text-[#B8B4AE]">{ex.sub}</p>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        </div>

                        {/* Progress card */}
                        <div className="bg-white rounded-[14px] p-3">
                            <ProgressBar label="이번 주 달성률" value={71} max={100} color="#4A7BFF" />
                            <ProgressBar label="오늘 칼로리" value={792} max={1320} color="#FF8C42" />
                        </div>
                    </div>

                    {/* Bottom nav */}
                    <div className="flex border-t border-[#EDEAE5] px-2 py-2 bg-white">
                        <NavItem icon="📊" label="홈" active />
                        <NavItem icon="🏋️" label="운동" active={false} />
                        <NavItem icon="🥗" label="식단" active={false} />
                        <NavItem icon="📈" label="리포트" active={false} />
                    </div>
                </div>
            </div>
        </div>
    );
};

export default DashboardPage;
