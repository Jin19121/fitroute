// src/components/PlanItemActionSheet.jsx
import { useState, useEffect, useRef } from "react";

// ─── 아이콘 SVG ──────────────────────────────────
const CheckIcon = () => (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
        <path d="M3.5 9L7.5 13L14.5 5" stroke="#2a5cc5" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
);
const CrossIcon = () => (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
        <path d="M4.5 4.5L13.5 13.5M13.5 4.5L4.5 13.5" stroke="#6b7280" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
);
const EditIcon = () => (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
        <path d="M12 3L15 6L6.5 14.5H3.5V11.5L12 3Z" stroke="#b55a00" strokeWidth="1.6" strokeLinejoin="round" />
    </svg>
);
const UndoIcon = () => (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
        <path d="M5 7H12a3.5 3.5 0 010 7H8" stroke="#6b7280" strokeWidth="1.6" strokeLineround="round" />
        <path d="M7.5 4.5L5 7L7.5 9.5" stroke="#6b7280" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
);
const BackIcon = () => (
    <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
        <path d="M10 3L5 8L10 13" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
);

// ─── 수정 폼 — 식단 ──────────────────────────────
function MealEditForm({ item, onSubmit, onBack }) {
    const [name, setName] = useState(item?.effectiveName ?? item?.foodName ?? "");
    const [cal, setCal] = useState(item?.effectiveCalories ?? item?.calories ?? "");
    const [protein, setProtein] = useState(item?.protein ?? "");
    const [carbs, setCarbs] = useState(item?.carbs ?? "");
    const [fat, setFat] = useState(item?.fat ?? "");
    const nameRef = useRef(null);

    useEffect(() => { nameRef.current?.focus(); }, []);

    const handleSubmit = (targetAction) => {
        if (!name.trim() && !cal) {
            alert("음식명 또는 칼로리를 입력해 주세요.");
            return;
        }
        onSubmit({
            action: targetAction,
            modifiedName: name.trim() || undefined,
            modifiedCalories: cal ? Number(cal) : undefined,
            modifiedProtein: protein ? Number(protein) : undefined,
            modifiedCarbs: carbs ? Number(carbs) : undefined,
            modifiedFat: fat ? Number(fat) : undefined,
        });
    };

    return (
        <div className="flex flex-col gap-3 mt-1">
            <p className="text-[11px] text-[#8a8680] font-medium">실제로 먹은 음식으로 수정해요</p>

            <div className="flex flex-col gap-1">
                <label className="text-[11px] text-[#8a8680]">음식명</label>
                <input
                    ref={nameRef}
                    type="text"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    placeholder="예: 현미밥 + 불고기"
                    className="border border-[#e5e1db] rounded-xl px-3 py-2 text-[13px] outline-none focus:border-blue-400"
                />
            </div>

            <div className="flex gap-2">
                {[
                    { label: "칼로리 (kcal)", val: cal, set: setCal, ph: "610" },
                    { label: "단백질 (g)", val: protein, set: setProtein, ph: "32" },
                ].map(({ label, val, set, ph }) => (
                    <div key={label} className="flex-1 flex flex-col gap-1">
                        <label className="text-[11px] text-[#8a8680]">{label}</label>
                        <input
                            type="number"
                            value={val}
                            onChange={e => set(e.target.value)}
                            placeholder={ph}
                            min={0}
                            className="border border-[#e5e1db] rounded-xl px-3 py-2 text-[13px] outline-none focus:border-blue-400 w-full"
                        />
                    </div>
                ))}
            </div>

            <div className="flex gap-2">
                {[
                    { label: "탄수화물 (g)", val: carbs, set: setCarbs, ph: "78" },
                    { label: "지방 (g)", val: fat, set: setFat, ph: "14" },
                ].map(({ label, val, set, ph }) => (
                    <div key={label} className="flex-1 flex flex-col gap-1">
                        <label className="text-[11px] text-[#8a8680]">{label}</label>
                        <input
                            type="number"
                            value={val}
                            onChange={e => set(e.target.value)}
                            placeholder={ph}
                            min={0}
                            className="border border-[#e5e1db] rounded-xl px-3 py-2 text-[13px] outline-none focus:border-blue-400 w-full"
                        />
                    </div>
                ))}
            </div>

            <button
                onClick={() => handleSubmit('COMPLETE_WITH_MODIFY')}
                className="w-full bg-blue-500 text-white text-[13px] font-semibold py-3 rounded-xl mt-1"
            >
                수정 후 완수
            </button>
            <button
                onClick={() => handleSubmit('MODIFY')}
                className="w-full bg-[#fff8e6] text-[#b55a00] border border-[#f5e0b0] text-[13px] font-semibold py-3 rounded-xl"
            >
                수정만 하기
            </button>

            <button
                onClick={onBack}
                className="flex items-center justify-center gap-1 text-[12px] text-[#8a8680] py-2"
            >
                <BackIcon /> 뒤로
            </button>
        </div>
    );
}

// ─── 수정 폼 — 운동 ──────────────────────────────
function WorkoutEditForm({ item, onSubmit, onBack }) {
    const [name, setName] = useState(item?.effectiveName ?? item?.exerciseName ?? "");
    const [sets, setSets] = useState(item?.sets ?? "");
    const [reps, setReps] = useState(item?.reps ?? "");
    const [cal, setCal] = useState(item?.effectiveCalories ?? item?.calories ?? "");
    const nameRef = useRef(null);

    useEffect(() => { nameRef.current?.focus(); }, []);

    const handleSubmit = (targetAction) => {
        if (!name.trim() && !cal) {
            alert("운동명 또는 칼로리를 입력해 주세요.");
            return;
        }
        onSubmit({
            action: targetAction,
            modifiedName: name.trim() || undefined,
            modifiedCalories: cal ? Number(cal) : undefined,
            modifiedSets: sets ? Number(sets) : undefined,
            modifiedReps: reps ? Number(reps) : undefined,
        });
    };

    return (
        <div className="flex flex-col gap-3 mt-1">
            <p className="text-[11px] text-[#8a8680] font-medium">실제로 한 운동으로 수정해요</p>

            <div className="flex flex-col gap-1">
                <label className="text-[11px] text-[#8a8680]">운동명</label>
                <input
                    ref={nameRef}
                    type="text"
                    value={name}
                    onChange={e => setName(e.target.value)}
                    placeholder="예: 인클라인 벤치프레스"
                    className="border border-[#e5e1db] rounded-xl px-3 py-2 text-[13px] outline-none focus:border-blue-400"
                />
            </div>

            <div className="flex gap-2">
                {[
                    { label: "세트", val: sets, set: setSets, ph: "4" },
                    { label: "횟수", val: reps, set: setReps, ph: "10" },
                    { label: "kcal", val: cal, set: setCal, ph: "200" },
                ].map(({ label, val, set, ph }) => (
                    <div key={label} className="flex-1 flex flex-col gap-1">
                        <label className="text-[11px] text-[#8a8680]">{label}</label>
                        <input
                            type="number"
                            value={val}
                            onChange={e => set(e.target.value)}
                            placeholder={ph}
                            min={0}
                            className="border border-[#e5e1db] rounded-xl px-3 py-2 text-[13px] outline-none focus:border-blue-400 w-full"
                        />
                    </div>
                ))}
            </div>

            <button
                onClick={() => handleSubmit('COMPLETE_WITH_MODIFY')}
                className="w-full bg-blue-500 text-white text-[13px] font-semibold py-3 rounded-xl mt-1"
            >
                수정 후 완수
            </button>
            <button
                onClick={() => handleSubmit('MODIFY')}
                className="w-full bg-[#fff8e6] text-[#b55a00] border border-[#f5e0b0] text-[13px] font-semibold py-3 rounded-xl"
            >
                수정만 하기
            </button>

            <button
                onClick={onBack}
                className="flex items-center justify-center gap-1 text-[12px] text-[#8a8680] py-2"
            >
                <BackIcon /> 뒤로
            </button>
        </div>
    );
}

// ─── 액션 버튼 ────────────────────────────────────
function ActionBtn({ icon, label, desc, onClick, iconBg }) {
    return (
        <button
            onClick={onClick}
            className="flex items-center gap-3 w-full bg-[#f5f3f0] hover:bg-[#edeae5] rounded-xl px-4 py-3 transition-colors duration-150"
        >
            <div className={`w-9 h-9 rounded-xl flex items-center justify-center flex-shrink-0 ${iconBg}`}>
                {icon}
            </div>
            <div className="text-left">
                <div className="text-[13px] font-medium text-[#1a1a1a]">{label}</div>
                <div className="text-[11px] text-[#8a8680] mt-0.5">{desc}</div>
            </div>
        </button>
    );
}

// ─── 메인 컴포넌트 ────────────────────────────────
export default function PlanItemActionSheet({ item, onClose, onApply }) {
    const [view, setView] = useState("main");
    const [loading, setLoading] = useState(false);

    useEffect(() => { setView("main"); }, [item]);

    if (!item) return null;

    // 미완료(PENDING)를 제외한 액션 이력이 있는 상태인지 체크
    const isProcessed = ["COMPLETED", "SKIPPED"].includes(item.status) || item.isModified;
    const isMeal = item.foodName !== undefined;
    const displayName = item.effectiveName ?? item.foodName ?? item.exerciseName ?? "-";

    const handle = async (payload) => {
        setLoading(true);
        try {
            await onApply(item.id, payload);
            onClose();
        } catch (e) {
            alert("저장에 실패했어요. 다시 시도해 주세요.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <div className="fixed inset-0 bg-black/40 z-40" onClick={onClose} />

            <div className="fixed bottom-0 left-0 right-0 z-50 bg-white rounded-t-3xl px-5 pt-4 pb-8 shadow-2xl"
                style={{ animation: "slideUp 0.22s ease-out" }}>
                <style>{`@keyframes slideUp{from{transform:translateY(100%)}to{transform:translateY(0)}}`}</style>

                <div className="w-9 h-1 bg-[#d5d0ca] rounded-full mx-auto mb-4" />

                <div className="mb-4 pb-4 border-b border-[#f0ece5]">
                    <div className="text-[14px] font-semibold text-[#1a1a1a]">{displayName}</div>
                    <div className="text-[11px] text-[#8a8680] mt-0.5">
                        {isMeal
                            ? `${item.calories} kcal · P${item.protein ?? 0}g C${item.carbs ?? 0}g F${item.fat ?? 0}g`
                            : `${item.calories} kcal · ${item.sets ?? "-"}세트 × ${item.reps ?? "-"}회`}
                        {item.isModified && (
                            <span className="ml-2 text-[#1a9e75] font-medium">
                                (수정됨: {item.effectiveCalories} kcal)
                            </span>
                        )}
                    </div>
                </div>

                {view === "main" && (
                    <div className="flex flex-col gap-2">
                        {item.status !== "COMPLETED" && (
                            <ActionBtn
                                icon={<CheckIcon />}
                                label="완수"
                                desc="계획대로 완료했어요"
                                iconBg="bg-[#eef3ff]"
                                onClick={() => handle({ action: "COMPLETE" })}
                            />
                        )}
                        {item.status !== "SKIPPED" && (
                            <ActionBtn
                                icon={<CrossIcon />}
                                label="미실행"
                                desc="오늘은 건너뛸게요"
                                iconBg="bg-[#f5f3f0]"
                                onClick={() => handle({ action: "SKIP" })}
                            />
                        )}
                        <ActionBtn
                            icon={<EditIcon />}
                            label="수정"
                            desc="내용을 변경하거나 변경 후 완수해요"
                            iconBg="bg-[#fff8e6]"
                            onClick={() => setView(isMeal ? "editMeal" : "editWorkout")}
                        />
                        {isProcessed && (
                            <ActionBtn
                                icon={<UndoIcon />}
                                label="되돌리기"
                                desc="미완료 상태로 되돌려요"
                                iconBg="bg-[#f5f3f0]"
                                onClick={() => handle({ action: "RESET" })}
                            />
                        )}
                        <button onClick={onClose} className="text-[12px] text-[#8a8680] text-center py-3">
                            취소
                        </button>
                    </div>
                )}

                {view === "editMeal" && (
                    <MealEditForm item={item} onSubmit={handle} onBack={() => setView("main")} />
                )}

                {view === "editWorkout" && (
                    <WorkoutEditForm item={item} onSubmit={handle} onBack={() => setView("main")} />
                )}

                {loading && (
                    <div className="absolute inset-0 bg-white/60 flex items-center justify-center rounded-t-3xl">
                        <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
                    </div>
                )}
            </div>
        </>
    );
}