// src/pages/onboarding/SignupPage.jsx
import { useState, useCallback } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import PhoneFrame from '../../components/layout/PhoneFrame';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import { StepIndicator } from '../../components/common/chips.jsx';
import useAuth from '../../hooks/useAuth';
import {
    validateEmail,
    validatePassword,
    validatePasswordConfirm,
    validateHeight,
    validateWeight,
    validateTargetWeight,
    validateTargetPeriod,
} from '../../utils/validators';
import useOnboardingStore from '../../store/onboardingStore';

// ── Step 1: Account credentials ───────────────────────────────────────────────
const StepAccount = ({ form, onChange, errors }) => (
    <div className="flex flex-col gap-4">
        <div>
            <div className="text-[10px] text-[#4A7BFF] font-semibold mb-1">STEP 1 / 3</div>
            <h2 className="text-[17px] font-bold text-[#1A1A1A]">계정을 만들어요</h2>
            <p className="text-[11px] text-[#B8B4AE] mt-1">이메일로 간단하게 시작해요</p>
        </div>
        <Input
            label="이메일"
            type="email"
            placeholder="user@email.com"
            value={form.email}
            onChange={onChange('email')}
            error={errors.email}
            autoComplete="email"
            inputMode="email"
        />
        <Input
            label="비밀번호"
            hint="(8자 이상)"
            type="password"
            placeholder="••••••••••"
            value={form.password}
            onChange={onChange('password')}
            error={errors.password}
            autoComplete="new-password"
        />
        <Input
            label="비밀번호 확인"
            type="password"
            placeholder="••••••••••"
            value={form.passwordConfirm}
            onChange={onChange('passwordConfirm')}
            error={errors.passwordConfirm}
            autoComplete="new-password"
        />
    </div>
);

// ── Step 2: Physical stats ────────────────────────────────────────────────────
const NumRow = ({ icon, label, value, onChange, error, unit, min, max, step = '0.1' }) => (
    <div>
        <div className="flex items-center gap-3 py-3 border-b border-[#F2EEE8] last:border-0">
            <div className="w-7 h-7 rounded-[8px] bg-[#EEF3FF] flex items-center justify-center flex-shrink-0 text-base">
                {icon}
            </div>
            <span className="text-[12px] text-[#6B6866] flex-1">{label}</span>
            <input
                type="number"
                value={value}
                onChange={onChange}
                min={min}
                max={max}
                step={step}
                className="w-20 text-right text-[14px] font-bold text-[#1A1A1A] bg-transparent outline-none"
            />
            <span className="text-[11px] text-[#B8B4AE]">{unit}</span>
        </div>
        {error && <p className="text-[10px] text-red-500 mt-1 px-2">{error}</p>}
    </div>
);

const StepProfile = ({ form, onChange, onGenderChange, errors }) => (
    <div className="flex flex-col gap-4">
        <div>
            <div className="text-[10px] text-[#4A7BFF] font-semibold mb-1">STEP 2 / 3</div>
            <h2 className="text-[17px] font-bold text-[#1A1A1A]">기본 정보를 알려주세요</h2>
            <p className="text-[11px] text-[#B8B4AE] mt-1">AI 맞춤 플랜 생성에 사용돼요</p>
        </div>

        {/* 성별 */}
        <div>
            <p className="text-[12px] text-[#6B6866] mb-2">성별</p>
            <div className="flex gap-2">
                {['MALE', 'FEMALE'].map((g) => (
                    <button
                        key={g}
                        type="button"
                        onClick={() => onGenderChange(g)}
                        className={`flex-1 py-2 rounded-[10px] text-[12px] font-semibold border transition-colors
                            ${form.gender === g
                                ? 'bg-[#4A7BFF] text-white border-[#4A7BFF]'
                                : 'bg-white text-[#6B6866] border-[#E8E4DE]'}`}
                    >
                        {g === 'MALE' ? '남성' : '여성'}
                    </button>
                ))}
            </div>
            {errors.gender && <p className="text-[10px] text-red-500 mt-1">{errors.gender}</p>}
        </div>

        {/* 생년월일 */}
        <div>
            <p className="text-[12px] text-[#6B6866] mb-1">생년월일</p>
            <input
                type="date"
                value={form.birthDate}
                onChange={onChange('birthDate')}
                max={new Date().toISOString().split('T')[0]}
                className="w-full border border-[#E8E4DE] rounded-[10px] px-3 py-2 text-[13px] text-[#1A1A1A] bg-white outline-none"
            />
            {errors.birthDate && <p className="text-[10px] text-red-500 mt-1">{errors.birthDate}</p>}
        </div>

        {/* 기존 수치 입력들 */}
        <div className="bg-white rounded-[12px] px-4">
            <NumRow icon="📏" label="키" value={form.height} onChange={onChange('height')} error={errors.height} unit="cm" min={100} max={250} />
            <NumRow icon="⚖️" label="현재 체중" value={form.weight} onChange={onChange('weight')} error={errors.weight} unit="kg" min={20} max={300} />
            <NumRow icon="📉" label="목표 체중" value={form.targetWeight} onChange={onChange('targetWeight')} error={errors.targetWeight} unit="kg" min={20} max={300} />
            <NumRow icon="📅" label="목표 기간" value={form.targetPeriod} onChange={onChange('targetPeriod')} error={errors.targetPeriod} unit="주" min={1} max={52} step="1" />
        </div>
    </div>
);

// ── Step 3: AI preferences ────────────────────────────────────────────────────
// (Rendered in AiSetupPage, but data collected here and passed through navigate state)

// ── Validation per step ───────────────────────────────────────────────────────
const validateStep = (step, form) => {
    const errors = {};
    if (step === 1) {
        const emailErr = validateEmail(form.email);
        const passErr = validatePassword(form.password);
        const confirmErr = validatePasswordConfirm(form.password, form.passwordConfirm);
        if (emailErr) errors.email = emailErr;
        if (passErr) errors.password = passErr;
        if (confirmErr) errors.passwordConfirm = confirmErr;
    }
    if (step === 2) {
        const hErr = validateHeight(form.height);
        const wErr = validateWeight(form.weight);
        const twErr = validateTargetWeight(form.weight, form.targetWeight);
        const tpErr = validateTargetPeriod(form.targetPeriod);
        if (hErr) errors.height = hErr;
        if (wErr) errors.weight = wErr;
        if (twErr) errors.targetWeight = twErr;
        if (tpErr) errors.targetPeriod = tpErr;
        if (!form.gender) errors.gender = '성별을 선택해주세요.';
        if (!form.birthDate) errors.birthDate = '생년월일을 입력해주세요.';
    }
    return errors;
};

// ── Page ──────────────────────────────────────────────────────────────────────
const SignupPage = () => {
    const navigate = useNavigate();
    const { signup, isLoading, error, clearError } = useAuth();

    const [step, setStep] = useState(1);
    const [form, setForm] = useState({
        email: '',
        password: '',
        passwordConfirm: '',
        height: '',
        weight: '',
        targetWeight: '',
        targetPeriod: '',
        gender: null,      // 추가
        birthDate: '',     // 추가
    });
    const [fieldErrors, setFieldErrors] = useState({});

    const handleChange = useCallback(
        (field) => (e) => {
            clearError();
            setFieldErrors((prev) => ({ ...prev, [field]: null }));
            setForm((prev) => ({ ...prev, [field]: e.target.value }));
        },
        [clearError],
    );

    const handleNext = () => {
        const errors = validateStep(step, form);
        if (Object.keys(errors).length > 0) {
            setFieldErrors(errors);
            return;
        }
        setStep((s) => s + 1);
    };

    const handleBack = () => setStep((s) => s - 1);

    const { setCredentials, setProfile } = useOnboardingStore();

    // After step 2: navigate to AiSetupPage, passing partial form data
    const handleProceedToAiSetup = () => {
        const errors = validateStep(2, form);
        if (Object.keys(errors).length > 0) {
            setFieldErrors(errors);
            return;
        }
        setCredentials({ email: form.email.trim(), password: form.password });
        setProfile({
            gender: form.gender,
            birthDate: form.birthDate,
            height: parseFloat(form.height),
            weight: parseFloat(form.weight),
            targetWeight: parseFloat(form.targetWeight),
            targetPeriod: parseInt(form.targetPeriod, 10),
        });
        navigate('/onboarding/ai-setup');
    };

    const emailError = fieldErrors.email || (error?.field === 'email' ? error.message : null);
    const globalError = error && !error.field ? error.message : null;

    return (
        <PhoneFrame>
            {/* Status bar */}
            <div className="flex justify-between items-center px-4 py-2 bg-[#F9F7F5]">
                <span className="text-[11px] font-bold text-[#1A1A1A]">9:41</span>
                <div className="flex gap-1">
                    {[true, true, false].map((f, i) => (
                        <div key={i} className={`w-1 h-1 rounded-full ${f ? 'bg-[#1A1A1A]' : 'bg-[#ccc]'}`} />
                    ))}
                </div>
            </div>

            <div className="flex flex-col gap-5 px-4 pb-8 flex-1">
                <StepIndicator total={3} current={step} />

                {step === 1 && (
                    <StepAccount
                        form={form}
                        onChange={handleChange}
                        errors={{ ...fieldErrors, email: emailError }}
                    />
                )}
                {step === 2 && (
                    <StepProfile
                        form={form}
                        onChange={handleChange}
                        onGenderChange={(g) => setForm((prev) => ({ ...prev, gender: g }))}
                        errors={fieldErrors}
                    />
                )}

                {globalError && (
                    <p className="text-[11px] text-red-500 text-center bg-red-50 rounded-lg py-2 px-3">
                        {globalError}
                    </p>
                )}

                <div className="flex-1" />

                {/* Navigation buttons */}
                {step < 2 ? (
                    <Button onClick={handleNext} disabled={isLoading}>
                        다음
                    </Button>
                ) : (
                    <Button onClick={handleProceedToAiSetup} disabled={isLoading}>
                        다음
                    </Button>
                )}

                {step === 1 && (
                    <p className="text-center text-[11px] text-[#B8B4AE]">
                        이미 계정이 있으신가요?{' '}
                        <Link to="/login" className="text-[#4A7BFF] font-medium hover:underline">
                            로그인
                        </Link>
                    </p>
                )}
                {step > 1 && (
                    <button
                        type="button"
                        onClick={handleBack}
                        className="text-[11px] text-[#B8B4AE] text-center hover:text-[#6B6866] transition-colors"
                    >
                        ← 이전으로
                    </button>
                )}
            </div>
        </PhoneFrame>
    );
};

export default SignupPage;
