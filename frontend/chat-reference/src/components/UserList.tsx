import { useEffect, useState } from 'react';
import { userApi } from '../services/api';
import type { User } from '../types';

export default function UserList() {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchUsers = () => {
      userApi.getUsers().then((res) => {
        setUsers(res.data);
        setLoading(false);
      }).catch(() => setLoading(false));
    };

    fetchUsers();
    const interval = setInterval(fetchUsers, 15000);
    return () => clearInterval(interval);
  }, []);

  const onlineUsers = users.filter((u) => u.status === 'ONLINE');
  const offlineUsers = users.filter((u) => u.status !== 'ONLINE');

  if (loading) {
    return (
      <div className="flex items-center justify-center py-8">
        <div className="animate-spin w-6 h-6 border-2 border-dark-accent border-t-transparent rounded-full" />
      </div>
    );
  }

  return (
    <div className="overflow-y-auto">
      {onlineUsers.length > 0 && (
        <div>
          <div className="px-4 py-2 text-xs font-medium text-dark-muted uppercase tracking-wider">
            Online - {onlineUsers.length}
          </div>
          {onlineUsers.map((user) => (
            <div key={user.id} className="px-4 py-2 flex items-center gap-3 hover:bg-dark-hover transition-colors">
              <div className="relative flex-shrink-0">
                <div className="w-8 h-8 bg-dark-accent/30 rounded-full flex items-center justify-center text-xs font-medium text-dark-accent">
                  {user.nickname.charAt(0)}
                </div>
                <div className="absolute -bottom-0.5 -right-0.5 w-3 h-3 bg-green-400 rounded-full border-2 border-dark-sidebar" />
              </div>
              <span className="text-sm truncate">{user.nickname}</span>
            </div>
          ))}
        </div>
      )}

      {offlineUsers.length > 0 && (
        <div>
          <div className="px-4 py-2 text-xs font-medium text-dark-muted uppercase tracking-wider">
            Offline - {offlineUsers.length}
          </div>
          {offlineUsers.map((user) => (
            <div key={user.id} className="px-4 py-2 flex items-center gap-3 hover:bg-dark-hover transition-colors opacity-50">
              <div className="w-8 h-8 bg-dark-hover rounded-full flex items-center justify-center text-xs font-medium text-dark-muted flex-shrink-0">
                {user.nickname.charAt(0)}
              </div>
              <span className="text-sm truncate">{user.nickname}</span>
            </div>
          ))}
        </div>
      )}

      {users.length === 0 && (
        <div className="text-center py-8 text-dark-muted text-sm">
          접속중인 유저가 없습니다
        </div>
      )}
    </div>
  );
}
