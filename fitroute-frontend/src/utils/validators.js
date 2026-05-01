// src/utils/validators.js

export const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

export const validateEmail = (value) => {
    if (!value || !value.trim()) return '이메일을 입력해주세요.';
    if (!EMAIL_REGEX.test(value.trim())) return '올바른 이메일 형식이 아닙니다.';
    return null;
};

export const validatePassword = (value) => {
    if (!value) return '비밀번호를 입력해주세요.';
    if (value.length < 8) return '비밀번호는 8자 이상이어야 합니다.';
    return null;
};

export const validatePasswordConfirm = (password, confirm) => {
    if (!confirm) return '비밀번호 확인을 입력해주세요.';
    if (password !== confirm) return '비밀번호가 일치하지 않습니다.';
    return null;
};

export const validateHeight = (value) => {
    const n = parseFloat(value);
    if (!value && value !== 0) return '키를 입력해주세요.';
    if (isNaN(n) || n < 100 || n > 250) return '유효한 키를 입력해주세요. (100–250cm)';
    return null;
};

export const validateWeight = (value) => {
    const n = parseFloat(value);
    if (!value && value !== 0) return '체중을 입력해주세요.';
    if (isNaN(n) || n < 20 || n > 300) return '유효한 체중을 입력해주세요. (20–300kg)';
    return null;
};

export const validateTargetWeight = (current, target) => {
    const n = parseFloat(target);
    if (!target && target !== 0) return '목표 체중을 입력해주세요.';
    if (isNaN(n) || n < 20 || n > 300) return '유효한 목표 체중을 입력해주세요.';
    return null;
};

export const validateTargetPeriod = (value) => {
    const n = parseInt(value, 10);
    if (!value) return '목표 기간을 입력해주세요.';
    if (isNaN(n) || n < 1 || n > 52) return '목표 기간은 1–52주 사이여야 합니다.';
    return null;
};
