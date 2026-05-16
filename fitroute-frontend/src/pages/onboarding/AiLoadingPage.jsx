// src/pages/onboarding/AiLoadingPage.jsx
import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import apiClient from '../../api/axios';

const STEPS = [
    { label: '사용자 데이터 분석', duration: 1800 },
    { label: '식단 계획 생성', duration: 2200 },
    { label: '운동 계획 생성', duration: 2000 },
    { label: '대시보드 준비', duration: 1000 },
];

const AiLoadingPage = () => {
    const navigate = useNavigate();
    const [currentStep, setCurrentStep] = useState(0);
    const [progress, setProgress] = useState(0);
    const [failed, setFailed] = useState(false);

    const timerRef = useRef(null);
    const progressRef = useRef(null);
    const calledRef = useRef(false);

    useEffect(() => {
        if (calledRef.current) return;
        calledRef.current = true;

        const generatePlan = async () => {
            try {
                await apiClient.post('/api/plans/today/generate', {}, { timeout: 120000 });

                clearInterval(progressRef.current);
                setProgress(100);
                setCurrentStep(STEPS.length);

                setTimeout(() => navigate('/dashboard', { replace: true }), 500);
            } catch (err) {
                const status = err?.response?.status;
                console.error('[AiLoading] 플랜 생성 실패:', status, err?.message);

                if (status === 401) {
                    navigate('/login', { replace: true });
                    return;
                }
                setFailed(true);
                clearInterval(progressRef.current);
            }
        };

        generatePlan();
    }, [navigate]);

    useEffect(() => {
        let stepIdx = 0;

        const advanceStep = () => {
            if (stepIdx >= STEPS.length) return;
            setCurrentStep(stepIdx);
            timerRef.current = setTimeout(() => {
                stepIdx++;
                advanceStep();
            }, STEPS[stepIdx]?.duration ?? 1500);
        };

        let pct = 0;
        progressRef.current = setInterval(() => {
            pct = Math.min(pct + 0.8, 95);
            setProgress(pct);
        }, 60);

        advanceStep();

        return () => {
            clearTimeout(timerRef.current);
            clearInterval(progressRef.current);
        };
    }, []);

    if (failed) {
        return (
            <div className="flex flex-col items-center justify-center min-h-screen bg-[#1A1A1A] px-6 gap-6">
                <div className="text-center">
                    <div className="w-14 h-14 bg-red-500/20 rounded-[20px] mx-auto mb-4 flex items-center justify-center">
                        <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
                            <path d="M14 8v8M14 20h.01" stroke="#EF4444"
                                strokeWidth="2.5" strokeLinecap="round" />
                            <circle cx="14" cy="14" r="12"
                                stroke="#EF4444" strokeWidth="2" />
                        </svg>
                    </div>
                    <h2 className="text-[16px] font-bold text-white mb-1">플랜 생성에 실패했어요</h2>
                    <p className="text-[12px] text-[#555] mb-6">잠시 후 다시 시도해 주세요</p>
                    <button
                        onClick={() => { calledRef.current = false; setFailed(false); setProgress(0); }}
                        className="px-6 py-2.5 bg-[#4A7BFF] text-white text-[13px] font-semibold rounded-xl"
                    >
                        다시 시도
                    </button>
                </div>
            </div>
        );
    }

    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-[#1A1A1A] px-6 gap-8">
            <div className="text-center">
                <div className="w-16 h-16 bg-[#4A7BFF] rounded-[20px] mx-auto mb-4 flex items-center justify-center shadow-xl shadow-[#4A7BFF]/40">
                    <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
                        <path d="M7 16 Q7 7 16 7 Q25 7 25 16 Q25 25 16 25"
                            stroke="white" strokeWidth="2.8" strokeLinecap="round" />
                        <circle cx="16" cy="16" r="4" fill="white" />
                    </svg>
                </div>
                <h2 className="text-[18px] font-bold text-white mb-1">플랜 생성 중</h2>
                <p className="text-[12px] text-[#555]">AI가 오늘 하루 맞춤 플랜을 만들고 있어요</p>
            </div>

            <div className="w-full bg-white/5 rounded-[16px] p-4 flex flex-col gap-3">
                {STEPS.map((s, i) => {
                    const isDone = i < currentStep;
                    const isNow = i === currentStep;
                    return (
                        <div key={i} className="flex items-center gap-3">
                            <div className={[
                                'w-5 h-5 rounded-full flex items-center justify-center flex-shrink-0 transition-all duration-400',
                                isDone ? 'bg-[#4A7BFF]'
                                    : isNow ? 'border-2 border-[#4A7BFF]'
                                        : 'border-2 border-[#333]',
                            ].join(' ')}>
                                {isDone && (
                                    <svg width="10" height="10" viewBox="0 0 10 10" fill="none">
                                        <path d="M1.5 5L4 7.5L8.5 2"
                                            stroke="white" strokeWidth="1.5" strokeLinecap="round" />
                                    </svg>
                                )}
                                {isNow && (
                                    <div className="w-2 h-2 rounded-full bg-[#4A7BFF] animate-pulse" />
                                )}
                            </div>
                            <span className={[
                                'text-[12px] transition-colors duration-300',
                                isDone ? 'text-[#4A7BFF]'
                                    : isNow ? 'text-[#888]'
                                        : 'text-[#333]',
                            ].join(' ')}>
                                {isDone ? `${s.label} 완료` : `${s.label} 중...`}
                            </span>
                        </div>
                    );
                })}
            </div>

            <div className="w-full">
                <div className="w-full bg-white/10 rounded-full h-1.5 overflow-hidden">
                    <div
                        className="h-full bg-[#4A7BFF] rounded-full transition-all duration-300 ease-out"
                        style={{ width: `${progress}%` }}
                    />
                </div>
                <p className="text-[10px] text-[#444] text-right mt-1">
                    {Math.round(progress)}%
                </p>
            </div>
        </div>
    );
};

export default AiLoadingPage;