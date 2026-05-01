// src/components/layout/PrivateRoute.jsx
import { Navigate, Outlet } from 'react-router-dom';
import useAuthStore from '../../store/authStore';

/**
 * PrivateRoute
 * Blocks unauthenticated access to protected routes.
 * Shows nothing until hydration is complete to avoid flash.
 */
const PrivateRoute = () => {
    const { isAuthenticated, isHydrated } = useAuthStore();

    if (!isHydrated) {
        // Hydration check: avoid redirect on page refresh before tokens are read
        return (
            <div className="min-h-screen bg-[#E4E1DC] flex items-center justify-center">
                <div className="w-6 h-6 border-2 border-[#4A7BFF] border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
};

export default PrivateRoute;

// ─────────────────────────────────────────────────────────────────────────────

// src/components/layout/PhoneFrame.jsx
/**
 * PhoneFrame
 * Wraps onboarding pages in the phone mockup shell shown in the design.
 * On mobile viewports it renders full-screen instead.
 */
export const PhoneFrame = ({ children, dark = false }) => (
    <div
        className={[
            'min-h-screen flex items-center justify-center',
            dark ? 'bg-[#111]' : 'bg-[#E4E1DC]',
        ].join(' ')}
    >
        {/* Desktop: phone shell */}
        <div className="hidden sm:block w-[390px] bg-[#1A1A1A] rounded-[38px] p-[9px] shadow-2xl">
            <div
                className={[
                    'rounded-[30px] overflow-hidden flex flex-col',
                    dark ? 'bg-[#1A1A1A]' : 'bg-[#F9F7F5]',
                ].join(' ')}
                style={{ minHeight: 780 }}
            >
                {children}
            </div>
        </div>

        {/* Mobile: full screen */}
        <div
            className={[
                'sm:hidden w-full flex flex-col',
                dark ? 'bg-[#1A1A1A]' : 'bg-[#F9F7F5]',
            ].join(' ')}
            style={{ minHeight: '100dvh' }}
        >
            {children}
        </div>
    </div>
);
