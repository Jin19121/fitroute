// src/api/auth.js
import api from './client';

/**
 * POST /api/auth/signup
 * payload: { email, password, height, weight, targetWeight, targetPeriod }
 */
export const signupApi = async (payload) => {
  const { data } = await api.post('/auth/signup', payload);
  return data;
};

/**
 * POST /api/auth/login
 * returns: { accessToken, refreshToken }
 */
export const loginApi = async ({ email, password }) => {
  const { data } = await api.post('/auth/login', { email, password });
  return data;
};

/**
 * POST /api/auth/refresh
 * returns: { accessToken, refreshToken }
 */
export const refreshApi = async (refreshToken) => {
  const { data } = await api.post('/auth/refresh', { refreshToken });
  return data;
};

/**
 * POST /api/auth/logout
 * Authorization 헤더는 client.js interceptor가 자동 삽입
 */
export const logoutApi = async () => {
  await api.post('/auth/logout');
};