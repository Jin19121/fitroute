// src/hooks/useAuth.js
import { useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { loginApi, signupApi, logoutApi } from '../api/auth';
import useAuthStore from '../store/authStore';

const useAuth = () => {
    const { setAuth, clearAuth } = useAuthStore();
    const navigate = useNavigate();

    const [isLoading, setIsLoading] = useState(false);
    const [error, setError] = useState(null);

    const clearError = useCallback(() => setError(null), []);

    const parseApiError = (err) => {
        const status = err.response?.status;
        const message = err.response?.data?.message || err.message;

        if (status === 409) return { field: 'email', message: '이미 사용 중인 이메일입니다.' };
        if (status === 401) {
            if (message?.includes('비밀번호')) return { field: 'password', message: '비밀번호가 일치하지 않습니다.' };
            return { field: null, message: '인증에 실패했습니다. 다시 시도해주세요.' };
        }
        if (status === 404) return { field: 'email', message: '존재하지 않는 이메일입니다.' };
        if (status === 400) return { field: null, message: message || '입력값을 확인해주세요.' };
        if (!navigator.onLine) return { field: null, message: '네트워크 연결을 확인해주세요.' };
        return { field: null, message: '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.' };
    };

    const login = useCallback(async ({ email, password }) => {
        setIsLoading(true);
        setError(null);
        try {
            const tokens = await loginApi({ email, password });
            setAuth(tokens);
            navigate('/dashboard', { replace: true });
        } catch (err) {
            setError(parseApiError(err));
        } finally {
            setIsLoading(false);
        }
    }, [setAuth, navigate]);

    const signup = useCallback(async (payload) => {
        setIsLoading(true);
        setError(null);
        try {
            await signupApi(payload);
            const tokens = await loginApi({
                email: payload.email,
                password: payload.password,
            });
            setAuth(tokens);
        } catch (err) {
            setError(parseApiError(err));
            throw err;
        } finally {
            setIsLoading(false);
        }
    }, [setAuth]);

    const logout = useCallback(async () => {
        setIsLoading(true);
        try {
            await logoutApi();
        } catch (_) {
            // 서버 실패해도 로컬 초기화
        } finally {
            clearAuth();
            setIsLoading(false);
            navigate('/login', { replace: true });
        }
    }, [clearAuth, navigate]);

    return { isLoading, error, clearError, login, signup, logout };
};

export default useAuth;