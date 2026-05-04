// hooks/useSignup.js
const useSignup = () => {
    const signup = async (formData) => {
        try {
            const response = await fetch('/api/auth/signup', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData),
            });

            if (response.status === 403) {
                // 이 에러가 뜨면 백엔드 SecurityConfig 확인
                throw new Error('접근 권한이 없습니다. (403) - SecurityConfig 확인 필요');
            }
            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || '회원가입 실패');
            }

            return await response.json();
        } catch (err) {
            console.error('[signup]', err);
            throw err;
        }
    };

    return { signup };
};