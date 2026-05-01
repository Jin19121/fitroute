// src/components/layout/PrivateRoute.jsx
import { Navigate, Outlet } from 'react-router-dom';
import useAuthStore from '../../store/authStore';

const PrivateRoute = () => {
    const { isAuthenticated, isHydrated } = useAuthStore();

    if (!isHydrated) {
        return (
            <div className="min-h-screen bg-[#E4E1DC] flex items-center justify-center">
                <div className="w-6 h-6 border-2 border-[#4A7BFF] border-t-transparent rounded-full animate-spin" />
            </div>
        );
    }

    return isAuthenticated ? <Outlet /> : <Navigate to="/login" replace />;
};

export default PrivateRoute;
// PhoneFrame은 components/layout/PhoneFrame.jsx 로 분리됨