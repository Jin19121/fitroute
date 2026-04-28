import { useState } from 'react';
import { signup } from '../api/auth';

export default function SignupPage() {
  const [form, setForm] = useState({ email: '', password: '', nickname: '' });

  const handleSignup = async (e) => {
    e.preventDefault();
    try {
      await signup(form);
      alert('회원가입 성공! 로그인해 주세요.');
    } catch (err) {
      alert('회원가입 실패: ' + err.response?.data);
    }
  };

  return (
    <div>
      <h2>회원가입</h2>
      <form onSubmit={handleSignup} style={{ display: 'flex', flexDirection: 'column', gap: '10px', width: '300px' }}>
        <input type="email" placeholder="이메일" onChange={e => setForm({...form, email: e.target.value})} required />
        <input type="password" placeholder="비밀번호" onChange={e => setForm({...form, password: e.target.value})} required />
        <input type="text" placeholder="닉네임" onChange={e => setForm({...form, nickname: e.target.value})} required />
        <button type="submit">가입하기</button>
      </form>
    </div>
  );
}