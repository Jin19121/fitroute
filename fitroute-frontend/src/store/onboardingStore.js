// src/store/onboardingStore.js
import { create } from 'zustand';
import { signupApi } from '../api/auth';
import useAuthStore from './authStore'; // 추가

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

        const { accessToken, refreshToken } = await signupApi(payload);

        // localStorage 직접 저장 대신 setAuth() 호출 → isAuthenticated: true 처리까지 한 번에
        useAuthStore.getState().setAuth({ accessToken, refreshToken });
    },
}));

export default useOnboardingStore;