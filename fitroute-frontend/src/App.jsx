import { BrowserRouter as Router, Routes, Route, Link } from 'react-router-dom';
import Signup from './pages/Signup';
import Login from './pages/Login';

function App() {
  return (
    <Router>
      <nav style={{ marginBottom: '20px' }}>
        <Link to="/login" style={{ marginRight: '10px' }}>로그인</Link>
        <Link to="/signup">회원가입</Link>
      </nav>
      <Routes>
        <Route path="/signup" element={<Signup />} />
        <Route path="/login" element={<Login />} />
        <Route path="/" element={<h1>FitRoute 대시보드 (로그인 필요)</h1>} />
      </Routes>
    </Router>
  );
}

export default App;