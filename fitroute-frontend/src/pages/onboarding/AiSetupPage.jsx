// src/pages/onboarding/AiSetupPage.jsx
import { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import PhoneFrame from '../../components/layout/PhoneFrame';
import { OptionChip, DayChip, StepIndicator } from '../../components/common/Chips.jsx'; 
import Button from '../../components/common/Button';
import useAuth from '../../hooks/useAuth';

// ── Option sets ───────────────────────────────────────────────────────────────
const GOAL_OPTIONS = ['체중 감량', '근육 증가', '유지'];
const ACTIVITY_OPTIONS = ['거의 안 움직임', '가벼운 활동', '보통', '활동 많음'];
const EXPERIENCE_OPTIONS = ['초보', '중급', '고급'];
const DIET_OPTIONS = ['일반식', '다이어트식', '저탄수/고단백'];
const DAYS = ['월', '화', '수', '목', '금', '토', '일'];

const AiSetupPage = () => {
    const location = useLocation();
    const navigate = useNavigate();
    const { signup, isLoading, error } = useAuth();

    const accountData = location.state?.accountData;

    const [goalType, setGoalType] = useState('체중 감량');
    const [activityLevel, setActivity] = useState('가벼운 활동');
    const [experience, setExperience] = useState('중급');
    const [dietStyle, setDiet] = useState('일반식');
    const [activeDays, setActiveDays] = useState(['월', '화', '목', '금']);

    // side effect는 useEffect 안에서
    useEffect(() => {
        if (!accountData) {
            navigate('/signup', { replace: true });
        }
    }, [accountData, navigate]);

    // 렌더링만 막는 early return (훅 선언 이후)
    if (!accountData) return null;

    const toggleDay = (day) =>
        setActiveDays((prev) =>
            prev.includes(day) ? prev.filter((d) => d !== day) : [...prev, day],
        );

    const handleGenerate = async () => {
        if (activeDays.length === 0) {
            alert('운동 가능 요일을 하나 이상 선택해주세요.');
            return;
        }

        const fullPayload = {
            ...accountData,
            goalType,
            activityLevel,
            experience,
            dietStyle,
            workoutDays: activeDays,
        };

        try {
            await signup(fullPayload);
            // signup 성공 후 여기서만 navigate
            navigate('/onboarding/loading', { replace: true });
        } catch (_) {
            // error는 useAuth 내부에서 이미 set됨 — UI에서 표시
        }
    };

    return (
        <PhoneFrame>
            <div className="flex justify-between items-center px-4 py-2 bg-[#F9F7F5]">
                <span className="text-[11px] font-bold text-[#1A1A1A]">9:41</span>
                <div className="flex gap-1">
                    {[true, true, false].map((f, i) => (
                        <div key={i} className={`w-1 h-1 rounded-full ${f ? 'bg-[#1A1A1A]' : 'bg-[#ccc]'}`} />
                    ))}
                </div>
            </div>

            <div className="flex flex-col gap-5 px-4 pb-6 flex-1 overflow-y-auto">
                <StepIndicator total={3} current={3} />

                <div>
                    <div className="text-[10px] text-[#4A7BFF] font-semibold mb-1">STEP 3 / 3</div>
                    <h2 className="text-[17px] font-bold text-[#1A1A1A]">AI 플랜 설정</h2>
                    <p className="text-[11px] text-[#B8B4AE] mt-1">더 정확한 추천을 위해 알려주세요</p>
                </div>

                {/* Goal type */}
                <div>
                    <p className="text-[11px] text-[#6B6866] mb-2">
                        목표 유형 <span className="text-[#4A7BFF]">*</span>
                    </p>
                    <div className="flex flex-wrap gap-2">
                        {GOAL_OPTIONS.map((opt) => (
                            <OptionChip
                                key={opt}
                                label={opt}
                                selected={goalType === opt}
                                onClick={() => setGoalType(opt)}
                            />
                        ))}
                    </div>
                </div>

                {/* Activity level */}
                <div>
                    <p className="text-[11px] text-[#6B6866] mb-2">
                        활동 수준 <span className="text-[#4A7BFF]">*</span>
                    </p>
                    <div className="flex flex-wrap gap-2">
                        {ACTIVITY_OPTIONS.map((opt) => (
                            <OptionChip
                                key={opt}
                                label={opt}
                                selected={activityLevel === opt}
                                onClick={() => setActivity(opt)}
                            />
                        ))}
                    </div>
                </div>

                {/* Experience */}
                <div>
                    <p className="text-[11px] text-[#6B6866] mb-2">운동 경험</p>
                    <div className="flex flex-wrap gap-2">
                        {EXPERIENCE_OPTIONS.map((opt) => (
                            <OptionChip
                                key={opt}
                                label={opt}
                                selected={experience === opt}
                                onClick={() => setExperience(opt)}
                            />
                        ))}
                    </div>
                </div>

                {/* Workout days */}
                <div>
                    <p className="text-[11px] text-[#6B6866] mb-2">운동 가능 요일</p>
                    <div className="flex gap-2">
                        {DAYS.map((day) => (
                            <DayChip
                                key={day}
                                label={day}
                                selected={activeDays.includes(day)}
                                onClick={() => toggleDay(day)}
                            />
                        ))}
                    </div>
                </div>

                {/* Diet style */}
                <div>
                    <p className="text-[11px] text-[#6B6866] mb-2">
                        식단 스타일{' '}
                        <span className="text-[10px] text-[#B8B4AE] font-normal">선택</span>
                    </p>
                    <div className="flex flex-wrap gap-2">
                        {DIET_OPTIONS.map((opt) => (
                            <OptionChip
                                key={opt}
                                label={opt}
                                selected={dietStyle === opt}
                                onClick={() => setDiet(opt)}
                            />
                        ))}
                    </div>
                </div>

                {error && (
                    <p className="text-[11px] text-red-500 text-center bg-red-50 rounded-lg py-2 px-3">
                        {error.message}
                    </p>
                )}

                <div className="flex-1" />

                <Button onClick={handleGenerate} isLoading={isLoading}>
                    AI 플랜 생성하기
                </Button>
            </div>
        </PhoneFrame>
    );
};

export default AiSetupPage;
