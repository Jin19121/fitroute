// src/pages/onboarding/AiSetupPage.jsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import useOnboardingStore from '../../store/onboardingStore';
import { signupApi } from '../../api/auth';
import { tokenStorage } from '../../api/axios';

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
                            : 'bg-white border-[#E8E4DE] text-[#6B6866]'}`}
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
            const { accessToken, refreshToken } = await signupApi({
                ...credentials,
                ...profile,
                goalType,
                activityLevel,
                exerciseExperience,
                dietStyle,
            });
            tokenStorage.setTokens(accessToken, refreshToken);
            navigate('/onboarding/ai-loading', { replace: true });

        } catch (e) {
            const message = e.response?.data?.message || '회원가입 중 오류가 발생했습니다.';
            setError(message);

            // users 또는 user_profiles 저장 실패 → 회원가입 첫 페이지로
            setTimeout(() => {
                navigate('/onboarding/signup', { replace: true });
            }, 2000);
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="flex flex-col gap-5 px-4 pb-8 min-h-screen overflow-y-auto bg-[#F9F7F5]">
            <div className="pt-6">
                <div className="text-[10px] text-[#4A7BFF] font-semibold mb-1">STEP 3 / 3</div>
                <h2 className="text-[17px] font-bold text-[#1A1A1A]">AI 플랜 설정</h2>
                <p className="text-[11px] text-[#B8B4AE] mt-1">더 정확한 추천을 위해 알려주세요</p>
            </div>

            {/* 가입 실패 메시지 */}
            {error && (
                <p className="text-[11px] text-red-500 text-center bg-red-50 rounded-lg py-2 px-3">
                    {error}
                    <br />
                    <span className="text-[10px] text-[#B8B4AE]">잠시 후 처음으로 돌아갑니다...</span>
                </p>
            )}

            <ChipGroup label="목표 유형" options={GOAL_OPTIONS} value={goalType} onChange={setGoalType} required />
            <ChipGroup label="활동 수준" options={ACTIVITY_OPTIONS} value={activityLevel} onChange={setActivityLevel} required />
            <ChipGroup label="운동 경험" options={EXPERIENCE_OPTIONS} value={exerciseExperience} onChange={setExerciseExperience} required />
            <ChipGroup label="식단 스타일" options={DIET_OPTIONS} value={dietStyle} onChange={setDietStyle} required />

            <div className="flex-1" />

            <button
                onClick={handleSubmit}
                disabled={!isValid || loading}
                className={`w-full py-3 rounded-2xl text-[14px] font-semibold text-white transition-colors
                    ${!isValid || loading ? 'bg-[#C8C4BE]' : 'bg-[#4A7BFF]'}`}
            >
                {loading ? '생성 중...' : 'AI 플랜 생성하기'}
            </button>

            <button
                type="button"
                onClick={() => navigate(-1)}
                className="text-[11px] text-[#B8B4AE] text-center hover:text-[#6B6866] transition-colors pb-4"
            >
                ← 이전으로
            </button>
        </div>
    );
}