// src/api/auth.js
import apiClient from './axios';

export const signupApi = async (payload) => {
  const { data } = await apiClient.post('/api/auth/signup', payload);
  return data;
};

export const loginApi = async ({ email, password }) => {
  const { data } = await apiClient.post('/api/auth/login', { email, password });
  return data;
};

export const refreshApi = async (refreshToken) => {
  const { data } = await apiClient.post('/api/auth/refresh', { refreshToken });
  return data;
};

export const logoutApi = async () => {
  await apiClient.post('/api/auth/logout');
};