// src/pages/dashboard/DashboardPage.jsx
// 변경점: PlanItemActionSheet 연동 + onApply → PATCH /api/plans/items/{id}/action

import { useState, useEffect, useCallback, useRef } from "react";
import PlanItemActionSheet from "../../components/PlanItemActionSheet";

// ─── API 계층 ─────────────────────────────────────
const API_BASE = "/api";
const authHeader = () => ({
    "Content-Type": "application/json",
    Authorization: `Bearer ${localStorage.getItem("accessToken") ?? ""}`,
});

const fetchDashboard = () =>
    fetch(`${API_BASE}/dashboard/today`, { headers: authHeader() }).then((r) => {
        if (!r.ok) throw new Error(`${r.status}`);
        return r.json();
    });

/**
 * 완수 / 미실행 / 수정 / 되돌리기 통합 액션 엔드포인트
 * payload: { status, modifiedName?, modifiedCalories?, ... }
 */
const applyItemAction = (itemId, payload) =>
    fetch(`${API_BASE}/plans/items/${itemId}/action`, {
        method: "PATCH",
        headers: authHeader(),
        body: JSON.stringify(payload),
    }).then((r) => { if (!r.ok) throw new Error(`${r.status}`); });

// ─── 낙관적 업데이트 헬퍼 ──────────────────────────
function applyOptimistic(prev, itemId, payload) {
    if (!prev?.today) return prev;

    const patch = (list) =>
        list.map((item) => {
            if (item.id !== itemId) return item;
            const { status, modifiedName, modifiedCalories,
                modifiedProtein, modifiedCarbs, modifiedFat,
                modifiedSets, modifiedReps } = payload;

            const next = { ...item, status };

            if (status === "MODIFIED") {
                next.effectiveName = modifiedName ?? item.effectiveName ?? item.foodName ?? item.exerciseName;
                next.effectiveCalories = modifiedCalories ?? item.effectiveCalories ?? item.calories;
                next.isModified = true;
                if (modifiedProtein != null) next.protein = modifiedProtein;
                if (modifiedCarbs != null) next.carbs = modifiedCarbs;
                if (modifiedFat != null) next.fat = modifiedFat;
                if (modifiedSets != null) next.sets = modifiedSets;
                if (modifiedReps != null) next.reps = modifiedReps;
            } else if (status === "PENDING") {
                next.effectiveName = item.foodName ?? item.exerciseName;
                next.effectiveCalories = item.calories;
                next.isModified = false;
            } else {
                next.isModified = false;
            }
            return next;
        });

    const meals = patch(prev.today.meals);
    const workouts = patch(prev.today.workouts);

    // 칼로리 재계산
    const consumed = meals
        .filter((m) => m.status === "COMPLETED" || m.status === "MODIFIED")
        .reduce((s, m) => s + (m.effectiveCalories ?? m.calories), 0);
    const burned = workouts
        .filter((w) => w.status === "COMPLETED" || w.status === "MODIFIED")
        .reduce((s, w) => s + (w.effectiveCalories ?? w.calories), 0);
    const remaining = Math.max(0, (prev.targetCaloriesPerDay ?? 0) - consumed);

    return {
        ...prev,
        today: { ...prev.today, meals, workouts, consumedCalories: consumed, burnedCalories: burned, remainingCalories: remaining },
    };
}

// ─── Custom hook ──────────────────────────────────
const POLL_MS = 3000;

function useDashboard() {
    const [data, setData] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const poll = useRef(null);

    const load = useCallback(async () => {
        try {
            const result = await fetchDashboard();
            setData(result);
            setError(null);
            if (result.planStatus === "GENERATING") {
                poll.current = setTimeout(load, POLL_MS);
            }
        } catch (e) {
            setError(e.message);
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => { load(); return () => clearTimeout(poll.current); }, [load]);

    const applyAction = useCallback(async (itemId, payload) => {
        // 낙관적 반영
        setData((prev) => applyOptimistic(prev, itemId, payload));
        try {
            await applyItemAction(itemId, payload);
        } catch (e) {
            // 롤백
            load();
            throw e;
        }
    }, [load]);

    return { data, loading, error, applyAction, reload: load };
}

// ─── Sub-components ────────────────────────────────
const STATUS_BADGE = {
    COMPLETED: { cls: "bg-[#eef3ff] text-[#2a5cc5]", label: "완수" },
    SKIPPED: { cls: "bg-[#f0ece5] text-[#8a8680]", label: "미실행" },
    MODIFIED: { cls: "bg-[#edfaf3] text-[#1a6b40]", label: "수정" },
};

const MEAL_BADGE = {
    BREAKFAST: { bg: "bg-[#fff1e6]", text: "text-[#b55a00]", label: "아침" },
    LUNCH: { bg: "bg-[#eef3ff]", text: "text-[#2a5cc5]", label: "점심" },
    DINNER: { bg: "bg-[#edfaf3]", text: "text-[#1a6b40]", label: "저녁" },
    SNACK: { bg: "bg-[#faf0ff]", text: "text-[#7b2fab]", label: "간식" },
};

const WO_COLOR = {
    CHEST: "#4a7bff", BACK: "#ff8c42", LEGS: "#1a9e75", SHOULDERS: "#a855f7",
    ARMS: "#f59e0b", CORE: "#ef4444", CARDIO: "#06b6d4", REST: "#9ca3af",
};

// 아이템 행 — 탭하면 시트 열림
function ItemRow({ item, onTap }) {
    const done = item.status === "COMPLETED";
    const skip = item.status === "SKIPPED";
    const mod = item.status === "MODIFIED";
    const muted = done || skip;
    const badge = STATUS_BADGE[item.status];

    return (
        <div
            onClick={() => onTap(item)}
            className={`flex items-center gap-2 py-3 cursor-pointer select-none
        ${mod ? "border-l-2 border-[#1a9e75] pl-3 -ml-3" : ""}`}
        >
            {/* 상태 인디케이터 */}
            <div
                className={`w-5 h-5 rounded-full flex-shrink-0 flex items-center justify-center
          ${done || mod ? "bg-blue-500" : skip ? "bg-[#f0ece5]" : "border-2 border-[#d5d0ca]"}`}
            >
                {(done || mod) && (
                    <svg width="9" height="9" viewBox="0 0 9 9" fill="none">
                        <path d="M1.5 4.5L3.8 7L7.5 2" stroke="#fff" strokeWidth="1.4" strokeLinecap="round" />
                    </svg>
                )}
                {skip && (
                    <svg width="8" height="8" viewBox="0 0 8 8" fill="none">
                        <path d="M2 2L6 6M6 2L2 6" stroke="#8a8680" strokeWidth="1.2" strokeLinecap="round" />
                    </svg>
                )}
            </div>

            <div className="flex-1 min-w-0">
                <div className={`text-[11px] font-medium truncate
          ${muted ? "line-through text-[#b8b4ae]" : "text-[#1a1a1a]"}`}>
                    {item.effectiveName ?? item.foodName ?? item.exerciseName}
                </div>
                {mod && item.foodName && item.effectiveName !== item.foodName && (
                    <div className="text-[9px] text-[#1a9e75] mt-0.5">
                        원본: {item.foodName} → {item.effectiveCalories} kcal
                    </div>
                )}
                {mod && item.exerciseName && item.effectiveName !== item.exerciseName && (
                    <div className="text-[9px] text-[#1a9e75] mt-0.5">
                        원본: {item.exerciseName} → {item.effectiveCalories} kcal
                    </div>
                )}
            </div>

            {badge ? (
                <span className={`text-[9px] font-medium px-2 py-0.5 rounded-full flex-shrink-0 ${badge.cls}`}>
                    {badge.label}
                </span>
            ) : null}

            <span className={`text-[11px] font-semibold flex-shrink-0
        ${done || mod ? "text-[#b8b4ae]" : "text-blue-500"}`}>
                {item.effectiveCalories ?? item.calories}
            </span>
            <svg width="10" height="10" viewBox="0 0 10 10" fill="none" className="flex-shrink-0">
                <path d="M3.5 2L7 5L3.5 8" stroke="#d5d0ca" strokeWidth="1.4" strokeLinecap="round" />
            </svg>
        </div>
    );
}

function MealSection({ meals, onTap }) {
    const grouped = ["BREAKFAST", "LUNCH", "DINNER", "SNACK"].reduce((acc, cat) => {
        acc[cat] = meals.filter((m) => m.category === cat);
        return acc;
    }, {});

    return (
        <div>
            <div className="flex justify-between items-center mb-2">
                <span className="text-[13px] font-bold text-[#1a1a1a]">🥗 오늘 식단</span>
                <span className="text-[11px] text-blue-500 font-medium">상세보기</span>
            </div>
            <div className="bg-white rounded-2xl px-3 divide-y divide-[#f0ece5]">
                {Object.entries(grouped).map(([cat, items]) => {
                    if (!items.length) return null;
                    const b = MEAL_BADGE[cat];
                    const done = items.every((i) => i.status !== "PENDING");
                    const totalKcal = items.reduce((s, i) => s + (i.effectiveCalories ?? i.calories), 0);
                    return (
                        <div key={cat} className="py-2">
                            <div className="flex items-center gap-2 mb-1">
                                <span className={`text-[9px] font-semibold px-2 py-0.5 rounded-full ${b.bg} ${b.text}`}>{b.label}</span>
                                <span className={`text-[9px] ml-auto ${done ? "text-[#b8b4ae]" : "text-blue-500 font-semibold"}`}>
                                    {done ? `${totalKcal} kcal ✓` : "미기록"}
                                </span>
                            </div>
                            {items.map((item) => (
                                <ItemRow key={item.id} item={item} onTap={onTap} />
                            ))}
                        </div>
                    );
                })}
            </div>
        </div>
    );
}

function WorkoutSection({ workouts, onTap }) {
    return (
        <div>
            <div className="flex justify-between items-center mb-2">
                <span className="text-[13px] font-bold text-[#1a1a1a]">🏋️ 오늘 운동</span>
                <span className="text-[11px] text-blue-500 font-medium">상세보기</span>
            </div>
            <div className="bg-white rounded-2xl px-3 divide-y divide-[#f0ece5]">
                {workouts.map((item) => (
                    <div key={item.id} className="flex items-center gap-2 py-3 cursor-pointer" onClick={() => onTap(item)}>
                        <div className="w-2 h-2 rounded-full flex-shrink-0"
                            style={{ background: WO_COLOR[item.category] ?? "#9ca3af" }} />
                        <ItemRow item={item} onTap={onTap} />
                    </div>
                ))}
            </div>
        </div>
    );
}

// ─── Dashboard ─────────────────────────────────────
export default function DashboardPage() {
    const { data, loading, error, applyAction } = useDashboard();
    const [activeItem, setActiveItem] = useState(null);

    if (loading) return (
        <div className="flex-1 flex items-center justify-center bg-[#1a1a1a] text-[#666] text-sm">
            로딩 중...
        </div>
    );

    if (error || !data) return (
        <div className="flex-1 flex flex-col items-center justify-center bg-[#f5f3f0] gap-2 p-6">
            <div className="text-[13px] font-medium text-[#1a1a1a]">오류가 발생했어요</div>
            <div className="text-[11px] text-[#8a8680]">{error}</div>
        </div>
    );

    if (data.planStatus === "GENERATING") {
        return (
            <div className="flex-1 bg-[#1a1a1a] flex flex-col items-center justify-center p-6 gap-3">
                <div className="w-12 h-12 bg-blue-500 rounded-2xl flex items-center justify-center">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                        <path d="M6 12Q6 4 12 4Q18 4 18 12Q18 19 12 19" stroke="#fff" strokeWidth="2.2" strokeLinecap="round" />
                        <circle cx="12" cy="12" r="3" fill="#fff" />
                    </svg>
                </div>
                <p className="text-[16px] font-bold text-white">플랜 생성 중</p>
                <p className="text-[12px] text-[#555]">잠시만 기다려 주세요...</p>
            </div>
        );
    }

    const { today } = data;
    const pct = data.targetCaloriesPerDay > 0
        ? Math.min(1, today.consumedCalories / data.targetCaloriesPerDay) : 0;
    const r = 26, circ = 2 * Math.PI * r;

    return (
        <div className="flex flex-col h-full bg-[#f5f3f0] relative">
            {/* Header */}
            <div className="bg-[#1a1a1a] px-5 pb-5 flex-shrink-0">
                <div className="flex justify-between items-center py-3">
                    <span className="text-[11px] text-[#666]">안녕하세요, {data.userName}님 👋</span>
                    <div className="w-8 h-8 bg-blue-500 rounded-full flex items-center justify-center text-[11px] text-white font-bold">
                        {data.userName?.charAt(0) ?? "?"}
                    </div>
                </div>
                <div className="text-[22px] font-extrabold text-white leading-tight tracking-tight">
                    오늘도<br />잘 해봐요!
                </div>
                <div className="text-[11px] text-[#555] mt-1">
                    D-{data.daysRemaining} · 이번 주{" "}
                    <span className="text-blue-400 font-semibold">{data.weeklyAchievementRate}%</span> 달성
                </div>
                <div className="flex gap-2 mt-3">
                    {[
                        { n: data.goalWeight?.toFixed(1), u: "kg", l: "목표 체중" },
                        { n: `-${data.weightToLose?.toFixed(1)}`, u: "kg", l: "남은 감량", c: "#ff8c42" },
                        { n: data.targetPeriodWeeks, u: "주", l: "목표 기간" },
                    ].map(({ n, u, l, c }) => (
                        <div key={l} className="flex-1 bg-white/[0.07] rounded-xl p-2 text-center">
                            <div className="text-[13px] font-bold" style={{ color: c ?? "#fff" }}>
                                {n}<span className="text-[9px] text-[#555] font-normal"> {u}</span>
                            </div>
                            <div className="text-[9px] text-[#555] mt-0.5">{l}</div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Body */}
            <div className="flex-1 overflow-y-auto px-4 py-3 flex flex-col gap-3 pb-24">
                {/* Calorie ring */}
                <div className="bg-blue-500 rounded-2xl p-4 flex items-center gap-4">
                    <svg width="72" height="72" viewBox="0 0 64 64" className="flex-shrink-0">
                        <circle cx="32" cy="32" r={r} fill="none" stroke="rgba(255,255,255,0.2)" strokeWidth="6" />
                        <circle cx="32" cy="32" r={r} fill="none" stroke="#fff" strokeWidth="6"
                            strokeDasharray={`${circ * pct} ${circ * (1 - pct)}`}
                            strokeLinecap="round" transform="rotate(-90 32 32)"
                            style={{ transition: "stroke-dasharray 0.6s ease" }} />
                        <text x="32" y="36" textAnchor="middle" fontSize="12" fontWeight="800" fill="#fff">
                            {Math.round(pct * 100)}%
                        </text>
                    </svg>
                    <div>
                        <div className="text-[10px] text-white/60">오늘 섭취</div>
                        <div className="text-[26px] font-extrabold text-white leading-none">
                            {today.consumedCalories.toLocaleString()}
                            <span className="text-[13px] font-normal text-white/60"> kcal</span>
                        </div>
                        <div className="text-[10px] text-white/60 mt-1">
                            목표 {data.targetCaloriesPerDay?.toLocaleString()} · 소모 {today.burnedCalories}
                        </div>
                        <div className="inline-block bg-white/20 rounded-lg px-2 py-0.5 text-[10px] text-white mt-1.5">
                            {today.remainingCalories.toLocaleString()} kcal 남음
                        </div>
                    </div>
                </div>

                {today.meals.length > 0 && (
                    <MealSection meals={today.meals} onTap={setActiveItem} />
                )}
                {today.workouts.length > 0 && (
                    <WorkoutSection workouts={today.workouts} onTap={setActiveItem} />
                )}

                {/* Progress */}
                <div className="bg-white rounded-2xl p-3 flex flex-col gap-3">
                    {[
                        {
                            label: "이번 주 달성률",
                            value: `${data.weeklyAchievementRate}%`,
                            pct: data.weeklyAchievementRate,
                            color: "#4a7bff",
                        },
                        {
                            label: "오늘 칼로리",
                            value: `${today.consumedCalories.toLocaleString()} / ${data.targetCaloriesPerDay?.toLocaleString()} kcal`,
                            pct: Math.round(pct * 100),
                            color: "#ff8c42",
                        },
                        {
                            label: "운동 달성",
                            value: `${today.workouts.filter((w) => w.status !== "PENDING" && w.status !== "SKIPPED").length} / ${today.workouts.filter((w) => w.status !== "SKIPPED").length} 완료`,
                            pct: today.workouts.filter((w) => w.status !== "SKIPPED").length > 0
                                ? Math.round(today.workouts.filter((w) => w.status !== "PENDING" && w.status !== "SKIPPED").length
                                    / today.workouts.filter((w) => w.status !== "SKIPPED").length * 100)
                                : 0,
                            color: "#1a9e75",
                        },
                    ].map(({ label, value, pct: p, color }) => (
                        <div key={label}>
                            <div className="flex justify-between mb-1">
                                <span className="text-[10px] text-[#8a8680]">{label}</span>
                                <span className="text-[10px] font-semibold text-[#1a1a1a]">{value}</span>
                            </div>
                            <div className="bg-[#edeae5] rounded h-1.5 overflow-hidden">
                                <div className="h-full rounded transition-all duration-700"
                                    style={{ width: `${p}%`, background: color }} />
                            </div>
                        </div>
                    ))}
                </div>
            </div>

            {/* Bottom nav */}
            <div className="absolute bottom-0 left-0 right-0 bg-white border-t border-[#f0ece5] flex py-2 pb-5">
                {[
                    { label: "홈", active: true }, { label: "운동" },
                    { label: "식단" }, { label: "리포트" },
                ].map(({ label, active }) => (
                    <div key={label} className="flex-1 flex flex-col items-center gap-1">
                        <div className={`w-7 h-7 rounded-2xl flex items-center justify-center text-xs
              ${active ? "bg-blue-500" : "bg-[#f0ece5]"}`}>
                            <span>{active ? "🏠" : "·"}</span>
                        </div>
                        <span className={`text-[8px] ${active ? "text-blue-500 font-semibold" : "text-[#b8b4ae]"}`}>{label}</span>
                    </div>
                ))}
            </div>

            {/* Action Sheet */}
            <PlanItemActionSheet
                item={activeItem}
                onClose={() => setActiveItem(null)}
                onApply={applyAction}
            />
        </div>
    );
}