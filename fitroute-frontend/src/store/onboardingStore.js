// store/onboardingStore.js
import { create } from "zustand";

const useOnboardingStore = create((set) => ({
    credentials: {},   // email, password
    profile: {},       // gender, birthDate, height, weight

    setCredentials: (data) => set({ credentials: data }),
    setProfile: (data) => set((state) => ({ profile: { ...state.profile, ...data } })),
    reset: () => set({ credentials: {}, profile: {} }),
}));

export default useOnboardingStore;