// src/store/authStore.js
import { create } from 'zustand';
import { tokenStorage } from '../api/axios';

/**
 * FitRoute Auth Store (Zustand)
 *
 * Responsibilities:
 *   - Track authentication state (accessToken, user metadata)
 *   - Hydrate from storage on app boot
 *   - Expose actions consumed by useAuth hook
 *   - Listen for token refresh / session expiry events from axios interceptor
 */
const useAuthStore = create((set, get) => ({
    // ── State ────────────────────────────────────────────────────────────────
    accessToken: null,
    userId: null,
    isAuthenticated: false,
    isHydrated: false, // becomes true after storage check on boot

    // ── Actions ──────────────────────────────────────────────────────────────

    /**
     * Called once at app startup.
     * Reads tokens from storage so a page refresh doesn't require re-login.
     */
    hydrate: () => {
        const at = tokenStorage.getAccess();
        const rt = tokenStorage.getRefresh();

        if (at && rt) {
            // We have tokens; mark authenticated optimistically.
            // The axios interceptor will handle silent refresh if AT is expired.
            set({
                accessToken: at,
                isAuthenticated: true,
                isHydrated: true,
            });
        } else {
            set({ isHydrated: true });
        }
    },

    /**
     * Persist tokens and mark user as authenticated.
     * Called after successful login or token refresh.
     */
    setAuth: ({ accessToken, refreshToken }) => {
        tokenStorage.setTokens(accessToken, refreshToken);
        set({
            accessToken,
            isAuthenticated: true,
        });
    },

    /**
     * Wipe all auth state.
     * Called on explicit logout or session expiry.
     */
    clearAuth: () => {
        tokenStorage.clearTokens();
        set({
            accessToken: null,
            userId: null,
            isAuthenticated: false,
        });
    },

    /**
     * Update access token only (used after silent refresh via event).
     */
    updateAccessToken: (accessToken) => {
        set({ accessToken });
    },
}));

// ── Event bridge from axios interceptor → store ──────────────────────────────
// Avoids circular import: interceptor dispatches DOM events, store listens here.
if (typeof window !== 'undefined') {
    window.addEventListener('fitroute:token-refreshed', (e) => {
        useAuthStore.getState().updateAccessToken(e.detail.accessToken);
    });

    window.addEventListener('fitroute:session-expired', () => {
        useAuthStore.getState().clearAuth();
        // Navigation is handled by PrivateRoute reacting to isAuthenticated
    });
}

export default useAuthStore;
