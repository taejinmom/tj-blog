import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { userApi } from '../services/api';

export default function LoginPage() {
  const navigate = useNavigate();
  const [isRegister, setIsRegister] = useState(false);
  const [username, setUsername] = useState('');
  const [nickname, setNickname] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      if (isRegister) {
        const res = await userApi.register({ username, nickname, password });
        localStorage.setItem('userId', String(res.data.id));
        localStorage.setItem('nickname', res.data.nickname);
        localStorage.setItem('username', res.data.username);
      } else {
        const res = await userApi.login({ username, password });
        localStorage.setItem('userId', String(res.data.id));
        localStorage.setItem('nickname', res.data.nickname);
        localStorage.setItem('username', res.data.username);
      }
      navigate('/chat');
    } catch {
      setError(isRegister ? '회원가입에 실패했습니다.' : '로그인에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-dark-bg px-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-dark-accent">TJ Chat</h1>
          <p className="text-dark-muted mt-2">실시간 채팅 서비스</p>
        </div>

        <div className="bg-dark-card rounded-xl p-6 shadow-lg border border-dark-border">
          <div className="flex mb-6 bg-dark-bg rounded-lg p-1">
            <button
              className={`flex-1 py-2 rounded-md text-sm font-medium transition-colors ${
                !isRegister ? 'bg-dark-accent text-white' : 'text-dark-muted hover:text-dark-text'
              }`}
              onClick={() => setIsRegister(false)}
            >
              로그인
            </button>
            <button
              className={`flex-1 py-2 rounded-md text-sm font-medium transition-colors ${
                isRegister ? 'bg-dark-accent text-white' : 'text-dark-muted hover:text-dark-text'
              }`}
              onClick={() => setIsRegister(true)}
            >
              회원가입
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm text-dark-muted mb-1">아이디</label>
              <input
                type="text"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                className="w-full px-3 py-2.5 bg-dark-bg border border-dark-border rounded-lg text-dark-text
                  focus:outline-none focus:border-dark-accent transition-colors"
                placeholder="아이디를 입력하세요"
                required
              />
            </div>

            {isRegister && (
              <div>
                <label className="block text-sm text-dark-muted mb-1">닉네임</label>
                <input
                  type="text"
                  value={nickname}
                  onChange={(e) => setNickname(e.target.value)}
                  className="w-full px-3 py-2.5 bg-dark-bg border border-dark-border rounded-lg text-dark-text
                    focus:outline-none focus:border-dark-accent transition-colors"
                  placeholder="닉네임을 입력하세요"
                  required
                />
              </div>
            )}

            <div>
              <label className="block text-sm text-dark-muted mb-1">비밀번호</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full px-3 py-2.5 bg-dark-bg border border-dark-border rounded-lg text-dark-text
                  focus:outline-none focus:border-dark-accent transition-colors"
                placeholder="비밀번호를 입력하세요"
                required
              />
            </div>

            {error && (
              <p className="text-red-400 text-sm">{error}</p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 bg-dark-accent text-white rounded-lg font-medium
                hover:bg-blue-600 transition-colors disabled:opacity-50"
            >
              {loading ? '처리중...' : isRegister ? '회원가입' : '로그인'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
