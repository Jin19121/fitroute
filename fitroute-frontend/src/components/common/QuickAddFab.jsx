// src/components/common/QuickAddFab.jsx
// 빠른 추가 FAB — BottomNav 중앙에 통합 가능
import { useState, useRef, useEffect } from 'react';
import apiClient from '../../api/axios';
import { logTodayWeight, getTodayWeight, getLatestWeight } from '../../api/weight';

// ─── 상수 ─────────────────────────────────────────────────────────────────
const WORKOUT_CATS = [
    { val: 'CHEST', label: '가슴' },
    { val: 'BACK', label: '등' },
    { val: 'LEGS', label: '하체' },
    { val: 'SHOULDERS', label: '어깨' },
    { val: 'ARMS', label: '팔' },
    { val: 'CORE', label: '코어' },
    { val: 'CARDIO', label: '유산소' },
];

const MEAL_TYPES = [
    { val: 'BREAKFAST', label: '아침' },
    { val: 'LUNCH', label: '점심' },
    { val: 'DINNER', label: '저녁' },
    { val: 'SNACK', label: '간식' },
];

// ─── 공통 인풋 스타일 ──────────────────────────────────────────────────────
const inputCls =
    'w-full bg-[#f5f3f0] border-none rounded-xl px-3 py-2.5 text-[13px] text-[#1a1a1a] outline-none focus:ring-2 focus:ring-[#4a7bff] placeholder:text-[#b8b4ae]';

// ─── 완수 뱃지 ─────────────────────────────────────────────────────────────
function CompletedBadge() {
    return (
        <div className="flex items-center gap-1.5 bg-[#edfaf3] rounded-xl px-3 py-2 mb-4">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
                <circle cx="7" cy="7" r="6" fill="#1a9e75" />
                <path d="M4 7l2.2 2.5L10 4.5" stroke="#fff" strokeWidth="1.4" strokeLinecap="round" />
            </svg>
            <span className="text-[11px] text-[#1a6b40] font-medium">
                추가 즉시 <strong>완수</strong> 처리돼요. 탭해서 변경 가능해요.
            </span>
        </div>
    );
}

// ─── 칩 버튼 ──────────────────────────────────────────────────────────────
function ChipBtn({ label, active, onClick }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className={`text-[11px] px-3 py-1.5 rounded-full border transition-colors ${active
                ? 'bg-[#eef3ff] border-[#4a7bff] text-[#2a5cc5]'
                : 'bg-[#f5f3f0] border-transparent text-[#6b6866]'
                }`}
        >
            {label}
        </button>
    );
}

// ─── 탭 버튼 ──────────────────────────────────────────────────────────────
function TabBtn({ label, active, onClick }) {
    return (
        <button
            type="button"
            onClick={onClick}
            className={`flex-1 py-1.5 rounded-lg text-[11px] font-semibold transition-colors ${active ? 'bg-white text-[#1a1a1a]' : 'text-[#8a8680]'
                }`}
        >
            {label}
        </button>
    );
}

// ─── 운동 추가 폼 ──────────────────────────────────────────────────────────
function WorkoutForm({ onSave, onClose }) {
    const [cat, setCat] = useState('CHEST');
    const [name, setName] = useState('');
    const [sets, setSets] = useState('');
    const [reps, setReps] = useState('');
    const [kcal, setKcal] = useState('');
    const [duration, setDuration] = useState('');
    const [loading, setLoading] = useState(false);
    const nameRef = useRef(null);

    useEffect(() => { nameRef.current?.focus(); }, []);

    const handleSubmit = async () => {
        if (!name.trim()) { alert('운동명을 입력해 주세요.'); return; }
        if (!kcal) { alert('칼로리를 입력해 주세요.'); return; }

        setLoading(true);
        try {
            await apiClient.post('/api/plans/items', {
                type: 'WORKOUT',
                category: cat,
                name: name.trim(),
                calories: Number(kcal),
                sets: sets ? Number(sets) : undefined,
                reps: reps ? Number(reps) : undefined,
                durationMin: duration ? Number(duration) : undefined,
                status: 'COMPLETED',
            });
            onSave();
            onClose();
        } catch {
            alert('운동 추가에 실패했어요. 다시 시도해 주세요.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex flex-col gap-3">
            <CompletedBadge />

            <div>
                <p className="text-[11px] text-[#6b6866] font-medium mb-2">부위</p>
                <div className="flex flex-wrap gap-1.5">
                    {WORKOUT_CATS.map(c => (
                        <ChipBtn key={c.val} label={c.label} active={cat === c.val} onClick={() => setCat(c.val)} />
                    ))}
                </div>
            </div>

            <div>
                <p className="text-[11px] text-[#6b6866] mb-1">운동명</p>
                <input
                    ref={nameRef}
                    type="text"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    placeholder="예: 벤치프레스"
                    className={inputCls}
                />
            </div>

            <div className="flex gap-2">
                {[
                    { label: '세트', val: sets, set: setSets, ph: '4' },
                    { label: '횟수', val: reps, set: setReps, ph: '10' },
                    { label: '시간(분)', val: duration, set: setDuration, ph: '30' },
                ].map(({ label, val, set, ph }) => (
                    <div key={label} className="flex-1">
                        <p className="text-[11px] text-[#6b6866] mb-1">{label}</p>
                        <input
                            type="number"
                            value={val}
                            onChange={e => set(e.target.value)}
                            placeholder={ph}
                            min={0}
                            className={inputCls}
                        />
                    </div>
                ))}
            </div>

            <div>
                <p className="text-[11px] text-[#6b6866] mb-1">소모 칼로리 (kcal)</p>
                <input
                    type="number"
                    value={kcal}
                    onChange={e => setKcal(e.target.value)}
                    placeholder="예: 200"
                    min={0}
                    className={inputCls}
                />
            </div>

            <button
                onClick={handleSubmit}
                disabled={loading}
                className="w-full bg-[#4a7bff] text-white font-bold py-3 rounded-xl text-[13px] disabled:opacity-60 mt-1"
            >
                {loading ? '추가 중...' : '🏋️ 운동 완수로 추가'}
            </button>
        </div>
    );
}

// ─── 식단 추가 폼 ──────────────────────────────────────────────────────────
function MealForm({ onSave, onClose }) {
    const [mealType, setMealType] = useState('BREAKFAST');
    const [name, setName] = useState('');
    const [kcal, setKcal] = useState('');
    const [protein, setProtein] = useState('');
    const [carbs, setCarbs] = useState('');
    const [fat, setFat] = useState('');
    const [loading, setLoading] = useState(false);
    const nameRef = useRef(null);

    useEffect(() => { nameRef.current?.focus(); }, []);

    const handleSubmit = async () => {
        if (!name.trim()) { alert('음식명을 입력해 주세요.'); return; }
        if (!kcal) { alert('칼로리를 입력해 주세요.'); return; }

        setLoading(true);
        try {
            await apiClient.post('/api/plans/items', {
                type: 'MEAL',
                category: mealType,
                name: name.trim(),
                calories: Number(kcal),
                protein: protein ? Number(protein) : 0,
                carbs: carbs ? Number(carbs) : 0,
                fat: fat ? Number(fat) : 0,
                status: 'COMPLETED',
            });
            onSave();
            onClose();
        } catch {
            alert('식단 추가에 실패했어요. 다시 시도해 주세요.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex flex-col gap-3">
            <CompletedBadge />

            <div className="flex gap-1 bg-[#f5f3f0] rounded-xl p-1">
                {MEAL_TYPES.map(t => (
                    <TabBtn key={t.val} label={t.label} active={mealType === t.val} onClick={() => setMealType(t.val)} />
                ))}
            </div>

            <div>
                <p className="text-[11px] text-[#6b6866] mb-1">음식명</p>
                <input
                    ref={nameRef}
                    type="text"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    placeholder="예: 오트밀 + 바나나"
                    className={inputCls}
                />
            </div>

            <div className="flex gap-2">
                {[
                    { label: '칼로리', val: kcal, set: setKcal, ph: '320' },
                    { label: '단백질g', val: protein, set: setProtein, ph: '12' },
                ].map(({ label, val, set, ph }) => (
                    <div key={label} className="flex-1">
                        <p className="text-[11px] text-[#6b6866] mb-1">{label}</p>
                        <input type="number" value={val} onChange={e => set(e.target.value)} placeholder={ph} min={0} className={inputCls} />
                    </div>
                ))}
            </div>

            <div className="flex gap-2">
                {[
                    { label: '탄수화물g', val: carbs, set: setCarbs, ph: '54' },
                    { label: '지방g', val: fat, set: setFat, ph: '6' },
                ].map(({ label, val, set, ph }) => (
                    <div key={label} className="flex-1">
                        <p className="text-[11px] text-[#6b6866] mb-1">{label}</p>
                        <input type="number" value={val} onChange={e => set(e.target.value)} placeholder={ph} min={0} className={inputCls} />
                    </div>
                ))}
            </div>

            <button
                onClick={handleSubmit}
                disabled={loading}
                className="w-full bg-[#1a9e75] text-white font-bold py-3 rounded-xl text-[13px] disabled:opacity-60 mt-1"
            >
                {loading ? '추가 중...' : '🥗 식단 완수로 추가'}
            </button>
        </div>
    );
}

// ─── 체중 기록 폼 ──────────────────────────────────────────────────────────
function WeightForm({ onClose }) {
    const [weightKg, setWeightKg] = useState(60.0);
    const [bodyFatPct, setBodyFatPct] = useState('');
    const [muscleMass, setMuscleMass] = useState('');
    const [note, setNote] = useState('');
    const [loading, setLoading] = useState(false);
    const [initLoading, setInitLoading] = useState(true);

    useEffect(() => {
        getTodayWeight()
            .then(data => {
                if (data?.weightKg) {
                    setWeightKg(data.weightKg);
                    if (data.bodyFatPct) setBodyFatPct(String(data.bodyFatPct));
                    if (data.muscleMass) setMuscleMass(String(data.muscleMass));
                    if (data.note) setNote(data.note);
                } else {
                    return getLatestWeight();
                }
            })
            .then(data => {
                if (data?.weightKg) setWeightKg(data.weightKg);
            })
            .catch(() => { })
            .finally(() => setInitLoading(false));
    }, []);

    const handleSubmit = async () => {
        setLoading(true);
        try {
            await logTodayWeight({
                logDate: new Date().toISOString().slice(0, 10),
                weightKg,
                bodyFatPct: bodyFatPct ? Number(bodyFatPct) : null,
                muscleMass: muscleMass ? Number(muscleMass) : null,
                note: note || null,
            });
            onClose();
        } catch {
            alert('체중 기록에 실패했어요. 다시 시도해 주세요.');
        } finally {
            setLoading(false);
        }
    };

    if (initLoading) {
        return (
            <div className="flex items-center justify-center h-32">
                <div className="w-5 h-5 border-2 border-[#ff8c42] border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    const adj = (setter, delta, min, max) =>
        setter(v => Math.round(Math.max(min, Math.min(max, v + delta)) * 10) / 10);

    return (
        <div className="flex flex-col gap-4">
            <div>
                <p className="text-[11px] text-[#6b6866] font-medium mb-2">
                    체중 <span className="text-red-400">*필수</span>
                </p>
                <div className="flex items-center justify-between bg-[#f5f3f0] rounded-xl px-3 py-3">
                    <div className="flex gap-1">
                        {[[-1, '−1'], [-0.1, '−0.1']].map(([d, lbl]) => (
                            <button
                                key={lbl}
                                onClick={() => adj(setWeightKg, d, 20, 300)}
                                className="w-9 h-9 rounded-lg bg-white text-[12px] font-bold text-[#1a1a1a] shadow-sm"
                            >
                                {lbl}
                            </button>
                        ))}
                    </div>
                    <div className="flex items-baseline gap-1">
                        <span className="text-[28px] font-bold text-[#1a1a1a] tabular-nums">
                            {weightKg.toFixed(1)}
                        </span>
                        <span className="text-[13px] text-[#8a8680]">kg</span>
                    </div>
                    <div className="flex gap-1">
                        {[[0.1, '+0.1'], [1, '+1']].map(([d, lbl]) => (
                            <button
                                key={lbl}
                                onClick={() => adj(setWeightKg, d, 20, 300)}
                                className="w-9 h-9 rounded-lg bg-white text-[12px] font-bold text-[#1a1a1a] shadow-sm"
                            >
                                {lbl}
                            </button>
                        ))}
                    </div>
                </div>
            </div>

            <div>
                <p className="text-[11px] text-[#6b6866] font-medium mb-1">
                    체지방률 <span className="text-[#b8b4ae]">(선택)</span>
                </p>
                <div className="flex items-center gap-2 bg-[#f5f3f0] rounded-xl px-3 py-2.5">
                    <input
                        type="number"
                        value={bodyFatPct}
                        onChange={e => setBodyFatPct(e.target.value)}
                        placeholder="예: 18.5"
                        min={0} max={70} step={0.1}
                        className="flex-1 bg-transparent text-[13px] text-[#1a1a1a] outline-none placeholder:text-[#b8b4ae]"
                    />
                    <span className="text-[12px] text-[#8a8680]">%</span>
                </div>
            </div>

            <div>
                <p className="text-[11px] text-[#6b6866] font-medium mb-1">
                    골격근량 <span className="text-[#b8b4ae]">(선택)</span>
                </p>
                <div className="flex items-center gap-2 bg-[#f5f3f0] rounded-xl px-3 py-2.5">
                    <input
                        type="number"
                        value={muscleMass}
                        onChange={e => setMuscleMass(e.target.value)}
                        placeholder="예: 28.3"
                        min={0} max={200} step={0.1}
                        className="flex-1 bg-transparent text-[13px] text-[#1a1a1a] outline-none placeholder:text-[#b8b4ae]"
                    />
                    <span className="text-[12px] text-[#8a8680]">kg</span>
                </div>
            </div>

            <div>
                <p className="text-[11px] text-[#6b6866] font-medium mb-1">
                    메모 <span className="text-[#b8b4ae]">(선택)</span>
                </p>
                <input
                    type="text"
                    value={note}
                    onChange={e => setNote(e.target.value)}
                    placeholder="오늘 컨디션은 어땠나요?"
                    maxLength={200}
                    className="w-full bg-[#f5f3f0] rounded-xl px-3 py-2.5 text-[13px] text-[#1a1a1a] outline-none placeholder:text-[#b8b4ae]"
                />
            </div>

            <button
                onClick={handleSubmit}
                disabled={loading}
                className="w-full bg-[#ff8c42] text-white font-bold py-3 rounded-xl text-[13px] disabled:opacity-60"
            >
                {loading ? '기록 중...' : '⚖️ 체중 기록하기'}
            </button>
        </div>
    );
}

// ─── 바텀 시트 래퍼 ───────────────────────────────────────────────────────
function BottomSheet({ title, children, onClose }) {
    return (
        <>
            <div
                className="fixed inset-0 bg-black/40 z-40"
                onClick={onClose}
            />
            <div
                className="fixed bottom-0 left-0 right-0 z-50 bg-white rounded-t-3xl px-5 pt-3 pb-8 shadow-2xl max-h-[90vh] overflow-y-auto"
                style={{ animation: 'fabSlideUp 0.22s ease-out' }}
                onClick={e => e.stopPropagation()}
            >
                <div className="w-9 h-1 bg-[#d5d0ca] rounded-full mx-auto mb-4" />
                <h2 className="text-[15px] font-bold text-[#1a1a1a] mb-4">{title}</h2>
                {children}
            </div>
        </>
    );
}

// ─── FAB 메뉴 아이템 ──────────────────────────────────────────────────────
const FAB_ITEMS = [
    { type: 'workout', emoji: '🏋️', label: '운동 추가', color: '#1a1a1a' },
    { type: 'meal', emoji: '🥗', label: '식단 추가', color: '#1a9e75' },
    { type: 'weight', emoji: '⚖️', label: '체중 기록', color: '#ff8c42' },
];

// ─── 삼각형 배치 계산 ─────────────────────────────────────────────────────
// 3개 항목을 삼각형으로 배치 (원형으로 배치)
// 중앙 아래 FAB가 중심이고, 위로 3개 항목이 배치됨
function getItemPosition(index) {
    const radius = 80; // 중앙에서의 거리
    const angles = [
        (3 * Math.PI) / 2 - Math.PI / 3,  // 좌상단 (120도 각도)
        (3 * Math.PI) / 2,                 // 상단 (중앙 위)
        (3 * Math.PI) / 2 + Math.PI / 3,  // 우상단 (120도 각도)
    ];

    const angle = angles[index];
    const x = Math.cos(angle) * radius;
    const y = Math.sin(angle) * radius;

    return { x, y };
}

// ─── 메인 FAB 컴포넌트 ─────────────────────────────────────────────────────
export default function QuickAddFab({
    onDataSaved,
    isBottomNavIntegrated = false,
    isMenuOpen = false,
    setMenuOpen = null,
}) {
    const [menuOpen, setMenuOpenInternal] = useState(false);
    const [sheet, setSheet] = useState(null);

    // 통합 모드면 외부 상태 사용, 아니면 내부 상태 사용
    const actualMenuOpen = isBottomNavIntegrated ? isMenuOpen : menuOpen;
    const setActualMenuOpen = isBottomNavIntegrated ? setMenuOpen : setMenuOpenInternal;

    const openSheet = type => {
        setActualMenuOpen(false);
        setSheet(type);
    };

    const closeSheet = () => setSheet(null);

    const handleSave = () => {
        onDataSaved?.();
    };

    const SHEET_TITLES = {
        workout: '🏋️ 운동 추가',
        meal: '🥗 식단 추가',
        weight: '⚖️ 체중 기록',
    };

    // 통합 모드 (BottomNav 중앙)
    if (isBottomNavIntegrated) {
        return (
            <>
                <style>{`
                    @keyframes fabSlideUp {
                        from { transform: translateY(100%); opacity: 0; }
                        to   { transform: translateY(0);    opacity: 1; }
                    }
                    @keyframes fabItemIn {
                        from { opacity: 0; transform: scale(0.5) translate(0, 20px); }
                        to   { opacity: 1; transform: scale(1) translate(0, 0); }
                    }
                `}</style>

                {/* 배경 어두워짐 */}
                {actualMenuOpen && (
                    <div
                        className="fixed inset-0 z-15 bg-black transition-opacity duration-200"
                        style={{ opacity: 0.4 }}
                        onClick={() => setActualMenuOpen(false)}
                    />
                )}

                {/* FAB 메뉴 아이템들 (삼각형 배치) */}
                {actualMenuOpen && (
                    <div
                        className="fixed z-30"
                        style={{
                            bottom: 88,
                            // left: '50%',
                            transform: 'translateX(-25%)',
                            width: '100px',
                            height: '70px',
                            pointerEvents: 'none',
                        }}
                    >
                        {FAB_ITEMS.map((item, index) => {
                            const { x, y } = getItemPosition(index);
                            return (
                                <div
                                    key={item.type}
                                    className="absolute flex flex-col items-center gap-2 cursor-pointer"
                                    style={{
                                        left: `calc(50% + ${x}px)`,
                                        top: `calc(50% + ${y}px)`,
                                        // transform: 'translate(-50%, -50%)',
                                        animation: `fabItemIn 0.3s ease-out ${index * 0.05}s backwards`,
                                        pointerEvents: 'auto',
                                        willChange: 'transform, opacity',
                                    }}
                                    onClick={() => openSheet(item.type)}
                                >
                                    <div
                                        className="w-12 h-12 rounded-full flex items-center justify-center text-xl shadow-lg flex-shrink-0"
                                        style={{ background: item.color }}
                                    >
                                        {item.emoji}
                                    </div>
                                    <span className="text-[10px] font-semibold text-[#1a1a1a] bg-white rounded-full px-2 py-0.5 whitespace-nowrap">
                                        {item.label}
                                    </span>
                                </div>
                            );
                        })}
                    </div>
                )}

                {/* 중앙 FAB 버튼 */}
                <button
                    onClick={() => setActualMenuOpen(o => !o)}
                    className="w-12 h-12 bg-[#4a7bff] rounded-full flex items-center justify-center shadow-lg shadow-[#4a7bff]/40 z-20"
                    style={{
                        transition: 'transform 0.2s',
                        transform: actualMenuOpen ? 'rotate(45deg)' : 'rotate(0deg)',
                    }}
                    aria-label="빠른 추가"
                >
                    <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
                        <path d="M11 4v14M4 11h14" stroke="#fff" strokeWidth="2.2" strokeLinecap="round" />
                    </svg>
                </button>

                {/* 바텀 시트들 */}
                {sheet === 'workout' && (
                    <BottomSheet title={SHEET_TITLES.workout} onClose={closeSheet}>
                        <WorkoutForm onSave={handleSave} onClose={closeSheet} />
                    </BottomSheet>
                )}
                {sheet === 'meal' && (
                    <BottomSheet title={SHEET_TITLES.meal} onClose={closeSheet}>
                        <MealForm onSave={handleSave} onClose={closeSheet} />
                    </BottomSheet>
                )}
                {sheet === 'weight' && (
                    <BottomSheet title={SHEET_TITLES.weight} onClose={closeSheet}>
                        <WeightForm onClose={() => { handleSave(); closeSheet(); }} />
                    </BottomSheet>
                )}
            </>
        );
    }

    // 기존 모드 (우측 고정 FAB)
    return (
        <>
            <style>{`
                @keyframes fabSlideUp {
                    from { transform: translateY(100%); opacity: 0; }
                    to   { transform: translateY(0);    opacity: 1; }
                }
                @keyframes fabMenuIn {
                    from { opacity: 0; transform: translateY(8px); }
                    to   { opacity: 1; transform: translateY(0); }
                }
            `}</style>

            {menuOpen && (
                <div
                    className="fixed inset-0 z-20"
                    onClick={() => setMenuOpenInternal(false)}
                />
            )}

            {menuOpen && (
                <div
                    className="fixed right-4 z-30 flex flex-col gap-2"
                    style={{
                        bottom: 82 + 52 + 12,
                        animation: 'fabMenuIn 0.15s ease-out'
                    }}
                >
                    {FAB_ITEMS.map(({ type, emoji, label, color }) => (
                        <div
                            key={type}
                            className="flex items-center gap-2 justify-end cursor-pointer"
                            onClick={() => openSheet(type)}
                        >
                            <span className="text-[11px] font-bold text-white px-3 py-1.5 rounded-full shadow-md"
                                style={{ background: '#1a1a1a' }}>
                                {label}
                            </span>
                            <div
                                className="w-11 h-11 rounded-full flex items-center justify-center text-lg shadow-md flex-shrink-0"
                                style={{ background: color }}
                            >
                                {emoji}
                            </div>
                        </div>
                    ))}
                </div>
            )}

            <button
                onClick={() => setMenuOpenInternal(o => !o)}
                className="fixed right-4 z-30 w-[52px] h-[52px] bg-[#4a7bff] rounded-full flex items-center justify-center shadow-lg shadow-[#4a7bff]/40"
                style={{
                    bottom: 82,
                    transition: 'transform 0.2s',
                    transform: menuOpen ? 'rotate(45deg)' : 'rotate(0deg)',
                }}
                aria-label="빠른 추가"
            >
                <svg width="22" height="22" viewBox="0 0 22 22" fill="none">
                    <path d="M11 4v14M4 11h14" stroke="#fff" strokeWidth="2.2" strokeLinecap="round" />
                </svg>
            </button>

            {sheet === 'workout' && (
                <BottomSheet title={SHEET_TITLES.workout} onClose={closeSheet}>
                    <WorkoutForm onSave={handleSave} onClose={closeSheet} />
                </BottomSheet>
            )}
            {sheet === 'meal' && (
                <BottomSheet title={SHEET_TITLES.meal} onClose={closeSheet}>
                    <MealForm onSave={handleSave} onClose={closeSheet} />
                </BottomSheet>
            )}
            {sheet === 'weight' && (
                <BottomSheet title={SHEET_TITLES.weight} onClose={closeSheet}>
                    <WeightForm onClose={() => { handleSave(); closeSheet(); }} />
                </BottomSheet>
            )}
        </>
    );
}