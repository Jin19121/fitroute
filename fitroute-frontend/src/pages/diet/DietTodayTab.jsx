// src/pages/diet/DietTodayTab.jsx
import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { usePlanStore } from '../../store/planStore';
import MealSection from '../../components/diet/MealSection';
import PlanItemActionSheet from '../../components/PlanItemActionSheet';

const MEAL_TYPES = ['BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'];
const MEAL_LABEL = {
    BREAKFAST: '아침', LUNCH: '점심', DINNER: '저녁', SNACK: '간식',
};

// ─── 플랜 없음 UI ─────────────────────────────────────────────────────────
function NoPlanState({ onGenerate }) {
    return (
        <div style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 16,
            padding: '0 24px',
            background: '#F5F3F0',
        }}>
            <div style={{
                width: 60, height: 60,
                background: '#FFF1E6',
                borderRadius: 18,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: 28,
            }}>
                🥗
            </div>
            <div style={{ textAlign: 'center' }}>
                <div style={{ fontSize: 16, fontWeight: 700, color: '#1A1A1A', marginBottom: 6 }}>
                    오늘 식단 계획이 없어요
                </div>
                <div style={{ fontSize: 12, color: '#8A8680', lineHeight: 1.6 }}>
                    AI가 목표 칼로리에 맞는<br />식단을 자동으로 짜드려요
                </div>
            </div>
            <button
                onClick={onGenerate}
                style={{
                    marginTop: 4,
                    background: '#4A7BFF',
                    color: '#fff',
                    fontSize: 13,
                    fontWeight: 600,
                    padding: '12px 28px',
                    borderRadius: 16,
                    border: 'none',
                    cursor: 'pointer',
                    boxShadow: '0 4px 16px rgba(74,123,255,0.30)',
                }}
            >
                ✨ AI 플랜 생성하기
            </button>
            <div style={{ fontSize: 10, color: '#B8B4AE' }}>약 10~20초 소요돼요</div>
        </div>
    );
}

// ─── 생성 중 UI ───────────────────────────────────────────────────────────
function GeneratingState() {
    return (
        <div style={{
            flex: 1,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            gap: 12,
            background: '#F5F3F0',
        }}>
            <div style={{ fontSize: 11, color: '#8A8680' }}>AI가 식단을 생성하고 있어요...</div>
            <div style={{
                display: 'flex', gap: 6,
            }}>
                {[0, 1, 2].map((i) => (
                    <div
                        key={i}
                        style={{
                            width: 6, height: 6, borderRadius: '50%',
                            background: '#4A7BFF',
                            animation: 'bounce 0.8s ease infinite',
                            animationDelay: `${i * 0.15}s`,
                        }}
                    />
                ))}
            </div>
            <style>{`
                @keyframes bounce {
                    0%, 100% { transform: translateY(0); opacity: 0.4; }
                    50%       { transform: translateY(-6px); opacity: 1; }
                }
            `}</style>
        </div>
    );
}

// ─── 메인 컴포넌트 ────────────────────────────────────────────────────────
export default function DietTodayTab() {
    const [activeItem, setActiveItem] = useState(null);
    const navigate = useNavigate();

    const todayData = usePlanStore((s) => s.todayData);
    const loading = usePlanStore((s) => s.loading);
    const error = usePlanStore((s) => s.error);
    const fetchToday = usePlanStore((s) => s.fetchToday);
    const applyAction = usePlanStore((s) => s.applyAction);

    useEffect(() => {
        fetchToday(true);
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleGeneratePlan = () => navigate('/onboarding/ai-loading');

    // ── 초기 로딩 ─────────────────────────────────────────────────────────
    if (loading && !todayData) {
        return (
            <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <div style={{ fontSize: 12, color: '#8A8680' }}>로딩 중...</div>
            </div>
        );
    }

    // ── 오류 ─────────────────────────────────────────────────────────────
    if (error) {
        return (
            <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <div style={{ fontSize: 12, color: '#B8B4AE' }}>데이터를 불러올 수 없어요</div>
            </div>
        );
    }

    // ── 생성 중 ───────────────────────────────────────────────────────────
    if (todayData?.planStatus === 'GENERATING') {
        return <GeneratingState />;
    }

    // ── 플랜 없음 ─────────────────────────────────────────────────────────
    if (!todayData?.today || todayData?.planStatus === 'NO_PLAN') {
        return <NoPlanState onGenerate={handleGeneratePlan} />;
    }

    // ── 정상 렌더 ────────────────────────────────────────────────────────
    const { today } = todayData;
    const meals = today.meals ?? [];

    const grouped = MEAL_TYPES.reduce((acc, type) => {
        acc[type] = meals.filter((m) => m.category === type);
        return acc;
    }, {});

    const pct =
        todayData.targetCaloriesPerDay > 0
            ? Math.min(1, today.consumedCalories / todayData.targetCaloriesPerDay)
            : 0;

    return (
        <>
            <div style={{
                flex: 1,
                overflowY: 'auto',
                padding: '0 12px 80px',
                display: 'flex',
                flexDirection: 'column',
                gap: 8,
                background: '#F5F3F0',
            }}>
                {/* 칼로리 카드 */}
                <div style={{
                    background: '#4A7BFF',
                    borderRadius: 14,
                    padding: '11px 12px',
                    marginTop: 8,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 10,
                }}>
                    <svg width="52" height="52" viewBox="0 0 52 52" style={{ flexShrink: 0 }}>
                        <circle cx="26" cy="26" r="21" fill="none" stroke="rgba(255,255,255,.25)" strokeWidth="5" />
                        <circle
                            cx="26" cy="26" r="21"
                            fill="none" stroke="#fff" strokeWidth="5"
                            strokeDasharray={`${132 * pct} ${132 * (1 - pct)}`}
                            strokeLinecap="round"
                            transform="rotate(-90 26 26)"
                            style={{ transition: 'stroke-dasharray 0.4s ease' }}
                        />
                        <text x="26" y="30" textAnchor="middle" fontSize="10" fontWeight="700" fill="#fff">
                            {Math.round(pct * 100)}%
                        </text>
                    </svg>
                    <div>
                        <div style={{ fontSize: 8, color: 'rgba(255,255,255,.65)' }}>오늘 섭취</div>
                        <div style={{ fontSize: 20, fontWeight: 700, color: '#fff', lineHeight: 1.1 }}>
                            {today.consumedCalories.toLocaleString()}
                            <span style={{ fontSize: 11, fontWeight: 400, color: 'rgba(255,255,255,.65)' }}> kcal</span>
                        </div>
                        <div style={{ fontSize: 8, color: 'rgba(255,255,255,.6)', marginTop: 1 }}>
                            목표 {todayData.targetCaloriesPerDay?.toLocaleString()} kcal
                        </div>
                        <div style={{
                            display: 'inline-block',
                            background: 'rgba(255,255,255,.2)',
                            borderRadius: 8,
                            fontSize: 8,
                            color: '#fff',
                            padding: '2px 7px',
                            marginTop: 3,
                        }}>
                            {today.remainingCalories?.toLocaleString()} kcal 남음
                        </div>
                    </div>
                </div>

                {/* 식단 카드 */}
                <div>
                    <div style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        marginBottom: 4,
                    }}>
                        <span style={{ fontSize: 11, fontWeight: 700, color: '#1A1A1A' }}>🥗 오늘 식단</span>
                        <span style={{ fontSize: 9, color: '#4A7BFF' }}>상세보기</span>
                    </div>
                    <div style={{ background: '#fff', borderRadius: 12, padding: '10px 11px' }}>
                        {MEAL_TYPES.map((type) =>
                            grouped[type].length > 0 ? (
                                <MealSection
                                    key={type}
                                    mealType={type}
                                    label={MEAL_LABEL[type]}
                                    items={grouped[type]}
                                    onTap={setActiveItem}
                                />
                            ) : null
                        )}
                    </div>
                </div>

                {/* 힌트 */}
                <div style={{
                    background: '#F2EEE8',
                    borderRadius: 8,
                    padding: '6px 9px',
                    display: 'flex',
                    alignItems: 'center',
                    gap: 6,
                }}>
                    <svg width="14" height="14" viewBox="0 0 14 14" style={{ flexShrink: 0 }}>
                        <circle cx="7" cy="7" r="6" fill="none" stroke="#B8B4AE" strokeWidth="1.2" />
                        <path d="M7 6 L7 10" stroke="#B8B4AE" strokeWidth="1.3" strokeLinecap="round" />
                        <circle cx="7" cy="4.5" r=".8" fill="#B8B4AE" />
                    </svg>
                    <span style={{ fontSize: 9, color: '#6B6866', lineHeight: 1.4 }}>
                        음식명 탭 시 수정 및 상태 변경이 가능해요
                    </span>
                </div>
            </div>

            <PlanItemActionSheet
                item={activeItem}
                onClose={() => setActiveItem(null)}
                onApply={async (itemId, payload) => {
                    await applyAction(itemId, payload);
                    setActiveItem(null);
                }}
            />
        </>
    );
}