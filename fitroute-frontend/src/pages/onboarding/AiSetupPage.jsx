// pages/onboarding/AiSetupPage.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import PhoneFrame from '../../components/layout/PhoneFrame';
import Button from '../../components/common/Button';
import { StepIndicator } from '../../components/common/chips.jsx';
import useOnboardingStore from '../../store/onboardingStore';
import { signupApi } from '../../api/auth';

const GOAL_OPTIONS = [
    { label: '체중 감량', value: 'WEIGHT_LOSS' },
    { label: '근육 증가', value: 'MUSCLE_GAIN' },
    { label: '체중 유지', value: 'MAINTENANCE' },
];

const ACTIVITY_OPTIONS = [
    { label: '거의 안 움직임', value: 'SEDENTARY' },
    { label: '가벼운 활동', value: 'LIGHTLY_ACTIVE' },
    { label: '보통', value: 'MODERATELY_ACTIVE' },
    { label: '활동 많음', value: 'VERY_ACTIVE' },
];

const EXPERIENCE_OPTIONS = [
    { label: '초보', value: 'BEGINNER' },
    { label: '중급', value: 'INTERMEDIATE' },
    { label: '고급', value: 'ADVANCED' },
];

const DIET_OPTIONS = [
    { label: '일반식', value: 'BALANCED' },
    { label: '다이어트식', value: 'LOW_CALORIE' },
    { label: '저탄수/고단백', value: 'LOW_CARB_HIGH_PROTEIN' },
];

// 기존 SignupPage의 NumRow, 성별 버튼과 동일한 스타일의 칩 그룹
const ChipGroup = ({ label, options, value, onChange, required }) => (
    <div>
        <p className="text-[12px] text-[#6B6866] mb-2">
            {label}
            {required && <span className="text-[#4A7BFF] ml-1">*</span>}
        </p>
        <div className="flex flex-wrap gap-2">
            {options.map((opt) => (
                <button
                    key={opt.value}
                    type="button"
                    onClick={() => onChange(opt.value)}
                    className={`px-3 py-1.5 rounded-[20px] text-[12px] font-medium border transition-colors
                        ${value === opt.value
                            ? 'bg-[#EEF3FF] border-[#4A7BFF] text-[#2A5CC5]'
                            : 'bg-white border-[#E8E4DE] text-[#6B6866]'
                        }`}
                >
                    {opt.label}
                </button>
            ))}
        </div>
    </div>
);

export default function AiSetupPage() {
    const navigate = useNavigate();
    const { profile, credentials } = useOnboardingStore();

    const [goalType, setGoalType] = useState(null);
    const [activityLevel, setActivityLevel] = useState(null);
    const [exerciseExperience, setExerciseExperience] = useState(null);
    const [dietStyle, setDietStyle] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    const isValid = goalType && activityLevel && exerciseExperience && dietStyle;

    const handleSubmit = async () => {
        if (!isValid) return;
        setLoading(true);
        setError(null);
        try {
            await signupApi({
                ...credentials,
                ...profile,
                goalType,
                activityLevel,
                exerciseExperience,
                dietStyle,
            });
            navigate('/onboarding/ai-loading');
        } catch (e) {
            setError(e.response?.data?.message || '가입 중 오류가 발생했습니다.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <PhoneFrame>
            {/* Status bar - SignupPage와 동일 */}
            <div className="flex justify-between items-center px-4 py-2 bg-[#F9F7F5]">
                <span className="text-[11px] font-bold text-[#1A1A1A]">9:41</span>
                <div className="flex gap-1">
                    {[true, true, false].map((f, i) => (
                        <div key={i} className={`w-1 h-1 rounded-full ${f ? 'bg-[#1A1A1A]' : 'bg-[#ccc]'}`} />
                    ))}
                </div>
            </div>

            <div className="flex flex-col gap-5 px-4 pb-8 flex-1 overflow-y-auto">
                <StepIndicator total={3} current={3} />

                {/* 헤더 - SignupPage StepAccount/StepProfile과 동일 구조 */}
                <div>
                    <div className="text-[10px] text-[#4A7BFF] font-semibold mb-1">STEP 3 / 3</div>
                    <h2 className="text-[17px] font-bold text-[#1A1A1A]">AI 플랜 설정</h2>
                    <p className="text-[11px] text-[#B8B4AE] mt-1">더 정확한 추천을 위해 알려주세요</p>
                </div>

                <ChipGroup
                    label="목표 유형"
                    options={GOAL_OPTIONS}
                    value={goalType}
                    onChange={setGoalType}
                    required
                />
                <ChipGroup
                    label="활동 수준"
                    options={ACTIVITY_OPTIONS}
                    value={activityLevel}
                    onChange={setActivityLevel}
                    required
                />
                <ChipGroup
                    label="운동 경험"
                    options={EXPERIENCE_OPTIONS}
                    value={exerciseExperience}
                    onChange={setExerciseExperience}
                    required
                />
                <ChipGroup
                    label="식단 스타일"
                    options={DIET_OPTIONS}
                    value={dietStyle}
                    onChange={setDietStyle}
                />

                {error && (
                    <p className="text-[11px] text-red-500 text-center bg-red-50 rounded-lg py-2 px-3">
                        {error}
                    </p>
                )}

                <div className="flex-1" />

                <Button onClick={handleSubmit} disabled={!isValid || loading}>
                    {loading ? '생성 중...' : 'AI 플랜 생성하기'}
                </Button>

                <button
                    type="button"
                    onClick={() => navigate(-1)}
                    className="text-[11px] text-[#B8B4AE] text-center hover:text-[#6B6866] transition-colors"
                >
                    ← 이전으로
                </button>
            </div>
        </PhoneFrame>
    );
}