// store/onboardingStore.js
import { create } from 'zustand';
import { signupApi, loginApi } from '../api/auth';

const useOnboardingStore = create((set, get) => ({
    credentials: {},
    profile: {},

    setCredentials: (data) => set({ credentials: data }),
    setProfile: (data) => set((state) => ({ profile: { ...state.profile, ...data } })),
    reset: () => set({ credentials: {}, profile: {} }),

    signupAndLogin: async (aiPreferences) => {
        const { credentials, profile } = get();

        const payload = {
            email: credentials.email,
            password: credentials.password,
            gender: profile.gender,
            birthDate: profile.birthDate,
            height: profile.height,
            weight: profile.weight,
            targetWeight: profile.targetWeight,
            targetPeriod: profile.targetPeriod,
            goalType: aiPreferences.goalType,
            activityLevel: aiPreferences.activityLevel,
            exerciseExperience: aiPreferences.exerciseExperience,
            dietStyle: aiPreferences.dietStyle,
        };

        // 1. 회원가입
        await signupApi(payload);

        // 2. 자동 로그인
        const { accessToken, refreshToken } = await loginApi({
            email: credentials.email,
            password: credentials.password,
        });

        // 3. 토큰 저장
        localStorage.setItem('accessToken', accessToken);
        localStorage.setItem('refreshToken', refreshToken);
    },
}));

export default useOnboardingStore;