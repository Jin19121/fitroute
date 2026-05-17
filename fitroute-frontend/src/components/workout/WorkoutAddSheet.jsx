// src/components/workout/WorkoutAddSheet.jsx
import { useState, useRef, useEffect } from "react";
import { addWorkoutItem } from "../../api/workout";

const CATEGORY_OPTIONS = [
    { value: "CHEST", label: "가슴" },
    { value: "BACK", label: "등" },
    { value: "LEGS", label: "다리" },
    { value: "SHOULDERS", label: "어깨" },
    { value: "ARMS", label: "팔" },
    { value: "CORE", label: "코어" },
    { value: "CARDIO", label: "유산소" },
    { value: "REST", label: "휴식" },
];

export default function WorkoutAddSheet({ onClose, onAdd }) {
    const [name, setName] = useState("");
    const [category, setCategory] = useState("CHEST");
    const [sets, setSets] = useState("");
    const [reps, setReps] = useState("");
    const [calories, setCalories] = useState("");
    const [loading, setLoading] = useState(false);
    const nameRef = useRef(null);

    useEffect(() => {
        nameRef.current?.focus();
    }, []);

    const handleSubmit = async () => {
        if (!name.trim()) {
            alert("운동명을 입력해 주세요.");
            return;
        }

        setLoading(true);
        try {
            await addWorkoutItem({
                name: name.trim(),
                category,
                sets: sets ? Number(sets) : null,
                reps: reps ? Number(reps) : null,
                calories: calories ? Number(calories) : 0,
            });

            onAdd();
        } catch (e) {
            alert("운동 추가에 실패했어요. 다시 시도해 주세요.");
        } finally {
            setLoading(false);
        }
    };

    return (
        <>
            <div className="fixed inset-0 bg-black/40 z-40" onClick={onClose} />

            <div
                className="fixed bottom-0 left-0 right-0 z-50 bg-white rounded-t-3xl px-5 pt-4 pb-8 shadow-2xl"
                style={{ animation: "slideUp 0.22s ease-out" }}
            >
                <style>{`@keyframes slideUp{from{transform:translateY(100%)}to{transform:translateY(0)}}`}</style>

                <div className="w-9 h-1 bg-[#d5d0ca] rounded-full mx-auto mb-4" />

                <h2 className="text-[15px] font-bold text-[#1a1a1a] mb-4">운동 추가하기</h2>

                <div className="flex flex-col gap-3">
                    {/* 운동명 */}
                    <div className="flex flex-col gap-1">
                        <label className="text-[11px] text-[#8a8680] font-medium">운동명</label>
                        <input
                            ref={nameRef}
                            type="text"
                            value={name}
                            onChange={(e) => setName(e.target.value)}
                            placeholder="예: 벤치프레스, 스쿼트"
                            className="border border-[#e5e1db] rounded-xl px-3 py-2.5 text-[13px] outline-none focus:border-blue-400"
                        />
                    </div>

                    {/* 카테고리 */}
                    <div className="flex flex-col gap-1">
                        <label className="text-[11px] text-[#8a8680] font-medium">부위</label>
                        <select
                            value={category}
                            onChange={(e) => setCategory(e.target.value)}
                            className="border border-[#e5e1db] rounded-xl px-3 py-2.5 text-[13px] outline-none focus:border-blue-400 bg-white"
                        >
                            {CATEGORY_OPTIONS.map(({ value, label }) => (
                                <option key={value} value={value}>
                                    {label}
                                </option>
                            ))}
                        </select>
                    </div>

                    {/* 세트 × 횟수 */}
                    <div className="flex gap-2">
                        {[
                            { label: "세트", val: sets, set: setSets, ph: "4" },
                            { label: "횟수", val: reps, set: setReps, ph: "10" },
                        ].map(({ label, val, set, ph }) => (
                            <div key={label} className="flex-1 flex flex-col gap-1">
                                <label className="text-[11px] text-[#8a8680]">{label}</label>
                                <input
                                    type="number"
                                    value={val}
                                    onChange={(e) => set(e.target.value)}
                                    placeholder={ph}
                                    min={0}
                                    className="border border-[#e5e1db] rounded-xl px-3 py-2 text-[13px] outline-none focus:border-blue-400"
                                />
                            </div>
                        ))}
                    </div>

                    {/* 칼로리 */}
                    <div className="flex flex-col gap-1">
                        <label className="text-[11px] text-[#8a8680] font-medium">칼로리 소모</label>
                        <input
                            type="number"
                            value={calories}
                            onChange={(e) => setCalories(e.target.value)}
                            placeholder="0"
                            min={0}
                            className="border border-[#e5e1db] rounded-xl px-3 py-2.5 text-[13px] outline-none focus:border-blue-400"
                        />
                    </div>

                    {/* 버튼 */}
                    <button
                        onClick={handleSubmit}
                        disabled={loading}
                        className="w-full bg-blue-500 text-white text-[13px] font-semibold py-3 rounded-xl mt-2 disabled:opacity-50 disabled:cursor-not-allowed"
                    >
                        {loading ? "추가 중..." : "운동 추가"}
                    </button>

                    <button
                        onClick={onClose}
                        className="text-[12px] text-[#8a8680] text-center py-3"
                    >
                        취소
                    </button>
                </div>

                {loading && (
                    <div className="absolute inset-0 bg-white/60 flex items-center justify-center rounded-t-3xl">
                        <div className="w-6 h-6 border-2 border-blue-500 border-t-transparent rounded-full animate-spin" />
                    </div>
                )}
            </div>
        </>
    );
}