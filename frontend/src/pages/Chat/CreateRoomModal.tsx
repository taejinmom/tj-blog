import { useState } from 'react';
import { chatRoomApi } from '../../api/chat';
import { useAuth } from '../../context/AuthContext';

interface Props {
  onClose: () => void;
  onCreated: () => void;
}

export default function CreateRoomModal({ onClose, onCreated }: Props) {
  const { user } = useAuth();
  const [name, setName] = useState('');
  const [type, setType] = useState('GROUP');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !user) return;

    setLoading(true);
    setError('');

    try {
      await chatRoomApi.createRoom({
        name: name.trim(),
        type,
        creatorId: user.id,
      });
      onCreated();
    } catch {
      setError('채팅방 생성에 실패했습니다.');
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/60 flex items-center justify-center z-50 px-4">
      <div className="bg-dark-card rounded-xl p-6 w-full max-w-md border border-dark-border shadow-xl">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold">새 채팅방 만들기</h2>
          <button onClick={onClose} className="p-1 hover:bg-dark-hover rounded-lg transition-colors">
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm text-dark-muted mb-1">채팅방 이름</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-3 py-2.5 bg-dark-bg border border-dark-border rounded-lg text-dark-text
                focus:outline-none focus:border-dark-accent transition-colors"
              placeholder="채팅방 이름을 입력하세요"
              autoFocus
              required
            />
          </div>

          <div>
            <label className="block text-sm text-dark-muted mb-1">타입</label>
            <div className="flex gap-3">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="type"
                  value="GROUP"
                  checked={type === 'GROUP'}
                  onChange={(e) => setType(e.target.value)}
                  className="accent-dark-accent"
                />
                <span className="text-sm">그룹 채팅</span>
              </label>
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="radio"
                  name="type"
                  value="DIRECT"
                  checked={type === 'DIRECT'}
                  onChange={(e) => setType(e.target.value)}
                  className="accent-dark-accent"
                />
                <span className="text-sm">1:1 채팅</span>
              </label>
            </div>
          </div>

          {error && <p className="text-red-400 text-sm">{error}</p>}

          <div className="flex gap-2 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-2.5 bg-dark-hover text-dark-text rounded-lg text-sm font-medium
                hover:bg-dark-border transition-colors"
            >
              취소
            </button>
            <button
              type="submit"
              disabled={loading}
              className="flex-1 py-2.5 bg-dark-accent text-white rounded-lg text-sm font-medium
                hover:bg-blue-600 transition-colors disabled:opacity-50"
            >
              {loading ? '생성중...' : '만들기'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
