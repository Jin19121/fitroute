// src/api/auth.js
import apiClient from './axios';

/**
 * POST /api/auth/signup
 * Backend: AES-encrypts email, BCrypt-hashes password, saves User + UserProfile
 */
export const signupApi = async (payload) => {
  // payload: { email, password, height, weight, targetWeight, targetPeriod }
  const { data } = await apiClient.post('/api/auth/signup', payload);
  return data; // 201 Created → no body
};

/**
 * POST /api/auth/login
 * Backend: AES-encrypts email for lookup, BCrypt compares password
 * Returns: { accessToken, refreshToken }
 */
export const loginApi = async ({ email, password }) => {
  const { data } = await apiClient.post('/api/auth/login', { email, password });
  return data; // { accessToken, refreshToken }
};

/**
 * POST /api/auth/refresh
 * Backend: validates RT, compares with Redis, issues new AT + RT (rotation)
 * Returns: { accessToken, refreshToken }
 */
export const refreshApi = async (refreshToken) => {
  const { data } = await apiClient.post('/api/auth/refresh', { refreshToken });
  return data;
};

/**
 * POST /api/auth/logout
 * Backend: deletes RT from Redis (requires Authorization header)
 */
export const logoutApi = async () => {
  await apiClient.post('/api/auth/logout');
};