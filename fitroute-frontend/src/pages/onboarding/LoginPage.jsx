// src/pages/onboarding/LoginPage.jsx
import { useState } from 'react';
import { Link } from 'react-router-dom';
import PhoneFrame from '../../components/layout/PhoneFrame';
import Input from '../../components/common/Input';
import Button from '../../components/common/Button';
import useAuth from '../../hooks/useAuth';
import { validateEmail, validatePassword } from '../../utils/validators';

// ── Status bar ───────────────────────────────────────────────────────────────
const StatusBar = () => (
    <div className="flex justify-between items-center px-4 py-2 bg-[#F9F7F5]">
        <span className="text-[11px] font-bold text-[#1A1A1A]">9:41</span>
        <div className="flex gap-1 items-center">
            {[true, true, false].map((full, i) => (
                <div
                    key={i}
                    className={`w-1 h-1 rounded-full ${full ? 'bg-[#1A1A1A]' : 'bg-[#ccc]'}`}
                />
            ))}
        </div>
    </div>
);

// ── Logo mark ────────────────────────────────────────────────────────────────
const LogoMark = () => (
    <div className="text-center py-6">
        <div className="w-12 h-12 bg-[#4A7BFF] rounded-[14px] mx-auto mb-3 flex items-center justify-center shadow-lg shadow-[#4A7BFF]/30">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path
                    d="M5 12 Q5 5 12 5 Q19 5 19 12 Q19 19 12 19"
                    stroke="#fff"
                    strokeWidth="2.2"
                    strokeLinecap="round"
                />
                <circle cx="12" cy="12" r="3" fill="#fff" />
            </svg>
        </div>
        <h1 className="text-[20px] font-bold text-[#1A1A1A] tracking-tight">FitRoute</h1>
        <p className="text-[11px] text-[#B8B4AE] mt-1">AI 기반 다이어트 &amp; 운동 관리</p>
    </div>
);

// ── Main page ────────────────────────────────────────────────────────────────
const LoginPage = () => {
    const { login, isLoading, error, clearError } = useAuth();

    const [form, setForm] = useState({ email: '', password: '' });
    const [fieldErrors, setFieldErrors] = useState({});

    const handleChange = (field) => (e) => {
        clearError();
        setFieldErrors((prev) => ({ ...prev, [field]: null }));
        setForm((prev) => ({ ...prev, [field]: e.target.value }));
    };

    const validate = () => {
        const errors = {};
        const emailErr = validateEmail(form.email);
        const passErr = validatePassword(form.password);
        if (emailErr) errors.email = emailErr;
        if (passErr) errors.password = passErr;
        setFieldErrors(errors);
        return Object.keys(errors).length === 0;
    };

    const handleSubmit = (e) => {
        e.preventDefault();
        if (!validate()) return;
        login({ email: form.email.trim(), password: form.password });
    };

    // Merge server-side field error into fieldErrors
    const emailError = fieldErrors.email || (error?.field === 'email' ? error.message : null);
    const passwordError = fieldErrors.password || (error?.field === 'password' ? error.message : null);
    const globalError = error && !error.field ? error.message : null;

    return (
        <PhoneFrame>
            <StatusBar />
            <form
                onSubmit={handleSubmit}
                noValidate
                className="flex flex-col gap-4 px-4 pb-8 flex-1 justify-center"
            >
                <LogoMark />

                {/* Kakao Login (placeholder – wire up OAuth flow separately) */}
                <button
                    type="button"
                    className="bg-[#FEE500] rounded-[10px] py-[10px] flex items-center gap-2 px-4 hover:bg-[#EDD500] transition-colors"
                    onClick={() => alert('카카오 OAuth 준비 중입니다.')}
                >
                    <div className="w-5 h-5 bg-[#3C1E1E] rounded-full flex-shrink-0" />
                    <span className="flex-1 text-center text-[13px] font-semibold text-[#3C1E1E]">
                        카카오로 시작하기
                    </span>
                </button>

                {/* Divider */}
                <div className="flex items-center gap-2">
                    <div className="flex-1 h-px bg-[#EDEAE5]" />
                    <span className="text-[10px] text-[#B8B4AE]">또는 이메일</span>
                    <div className="flex-1 h-px bg-[#EDEAE5]" />
                </div>

                {/* Email */}
                <Input
                    label="이메일"
                    type="email"
                    placeholder="user@example.com"
                    value={form.email}
                    onChange={handleChange('email')}
                    error={emailError}
                    autoComplete="email"
                    inputMode="email"
                    disabled={isLoading}
                />

                {/* Password */}
                <Input
                    label="비밀번호"
                    type="password"
                    placeholder="••••••••"
                    value={form.password}
                    onChange={handleChange('password')}
                    error={passwordError}
                    autoComplete="current-password"
                    disabled={isLoading}
                />

                {/* Global server error */}
                {globalError && (
                    <p className="text-[11px] text-red-500 text-center bg-red-50 rounded-lg py-2 px-3">
                        {globalError}
                    </p>
                )}

                <Button type="submit" isLoading={isLoading}>
                    로그인
                </Button>

                <p className="text-center text-[11px] text-[#B8B4AE]">
                    계정이 없으신가요?{' '}
                    <Link
                        to="/signup"
                        className="text-[#4A7BFF] font-medium hover:underline"
                    >
                        회원가입
                    </Link>
                </p>
            </form>
        </PhoneFrame>
    );
};

export default LoginPage;
