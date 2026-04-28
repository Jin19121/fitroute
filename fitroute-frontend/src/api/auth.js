import axios from 'axios';

// 백엔드 주소 (Vite 환경 변수 사용 추천)
const API = axios.create({
  baseURL: 'http://localhost:8080/api/auth'
});

export const signup = (userData) => API.post('/signup', userData);
export const login = (userData) => API.post('/login', userData);