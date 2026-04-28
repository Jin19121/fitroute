import { useState } from 'react';
import { login } from '../api/auth';

export default function LoginPage() {
  const [form, setForm] = useState({ email: '', password: '' });

  const handleLogin = async (e) => {
    e.preventDefault();
    try {
      const res = await login(form);
      // 서버에서 내려준 JWT 토큰 저장
      localStorage.setItem('accessToken', res.data.accessToken);
      alert('로그인 성공!');
    } catch (err) {
      alert('로그인 실패');
    }
  };

  return (
    <div>
      <h2>로그인</h2>
      <form onSubmit={handleLogin} style={{ display: 'flex', flexDirection: 'column', gap: '10px', width: '300px' }}>
        <input type="email" placeholder="이메일" onChange={e => setForm({...form, email: e.target.value})} required />
        <input type="password" placeholder="비밀번호" onChange={e => setForm({...form, password: e.target.value})} required />
        <button type="submit">로그인</button>
      </form>
    </div>
  );
}