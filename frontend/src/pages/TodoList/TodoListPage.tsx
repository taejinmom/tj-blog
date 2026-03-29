import { useEffect, useState } from 'react';
import { todoApi } from '../../api/client';
import type { TodoItem, TodoStatus } from '../../types';

const STATUS_LABELS: Record<TodoStatus, string> = {
  PENDING: 'Pending',
  IN_PROGRESS: 'In Progress',
  COMPLETED: 'Completed',
};

const STATUS_COLORS: Record<TodoStatus, string> = {
  PENDING: 'bg-gray-100 text-gray-700 dark:bg-gray-700 dark:text-gray-300',
  IN_PROGRESS: 'bg-blue-100 text-blue-700 dark:bg-blue-900/30 dark:text-blue-400',
  COMPLETED: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400',
};

const NEXT_STATUS: Record<TodoStatus, TodoStatus> = {
  PENDING: 'IN_PROGRESS',
  IN_PROGRESS: 'COMPLETED',
  COMPLETED: 'PENDING',
};

// 프로젝트별 서비스 연결 정보
// title 키워드로 매칭 → 해당 서비스 URL + 헬스체크 URL
interface ServiceLink {
  url: string;
  healthUrl: string;
  label: string;
}

const SERVICE_LINKS: Record<string, ServiceLink> = {
  '채팅': {
    url: 'http://localhost:3000',
    healthUrl: 'http://localhost:8081/api/service-info',
    label: 'TJ Chat',
  },
  // 향후 추가되는 서비스들
  // 'API Gateway': { url: 'http://localhost:xxxx', healthUrl: '...', label: 'API Gateway' },
};

function findServiceLink(title: string): ServiceLink | null {
  for (const [keyword, link] of Object.entries(SERVICE_LINKS)) {
    if (title.includes(keyword)) return link;
  }
  return null;
}

function ServiceButton({ link }: { link: ServiceLink }) {
  const [alive, setAlive] = useState<boolean | null>(null);

  useEffect(() => {
    const check = () => {
      fetch(link.healthUrl, { mode: 'no-cors' })
        .then(() => setAlive(true))
        .catch(() => setAlive(false));
    };
    check();
    const interval = setInterval(check, 30000);
    return () => clearInterval(interval);
  }, [link.healthUrl]);

  return (
    <a
      href={link.url}
      target="_blank"
      rel="noopener noreferrer"
      className={`shrink-0 inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium transition-all ${
        alive
          ? 'bg-indigo-100 text-indigo-700 hover:bg-indigo-200 dark:bg-indigo-900/30 dark:text-indigo-400 dark:hover:bg-indigo-900/50'
          : alive === false
          ? 'bg-gray-100 text-gray-400 dark:bg-gray-700 dark:text-gray-500 cursor-not-allowed'
          : 'bg-gray-100 text-gray-400 dark:bg-gray-700 dark:text-gray-500'
      }`}
      title={alive ? `${link.label} 서비스 실행중 - 클릭하여 이동` : `${link.label} 서비스 확인중...`}
    >
      <span className={`w-2 h-2 rounded-full ${
        alive ? 'bg-green-500 animate-pulse' : alive === false ? 'bg-red-400' : 'bg-gray-400'
      }`} />
      <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
          d="M10 6H6a2 2 0 00-2 2v10a2 2 0 002 2h10a2 2 0 002-2v-4M14 4h6m0 0v6m0-6L10 14" />
      </svg>
      {link.label}
    </a>
  );
}

export default function TodoListPage() {
  const [todos, setTodos] = useState<TodoItem[]>([]);
  const [loading, setLoading] = useState(true);

  const fetchTodos = () => {
    todoApi.getAll()
      .then(setTodos)
      .catch(console.error)
      .finally(() => setLoading(false));
  };

  useEffect(fetchTodos, []);

  const grouped = todos.reduce<Record<string, TodoItem[]>>((acc, todo) => {
    (acc[todo.phase] ??= []).push(todo);
    return acc;
  }, {});

  const phases = Object.keys(grouped).sort();

  const totalCount = todos.length;
  const completedCount = todos.filter(t => t.status === 'COMPLETED').length;
  const progressPercent = totalCount > 0 ? Math.round((completedCount / totalCount) * 100) : 0;

  const handleStatusChange = async (todo: TodoItem) => {
    const nextStatus = NEXT_STATUS[todo.status];
    const updated = await todoApi.update(todo.id, {
      title: todo.title,
      description: todo.description,
      phase: todo.phase,
      status: nextStatus,
    });
    setTodos(prev => prev.map(t => (t.id === updated.id ? updated : t)));
  };

  if (loading) {
    return <div className="flex justify-center py-20 text-gray-500">Loading...</div>;
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-8">
      <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-6">Project Roadmap</h1>

      {/* Progress bar */}
      <div className="mb-8 p-4 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700">
        <div className="flex items-center justify-between mb-2">
          <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Overall Progress</span>
          <span className="text-sm font-bold text-primary">{progressPercent}%</span>
        </div>
        <div className="w-full bg-gray-200 dark:bg-gray-700 rounded-full h-3">
          <div
            className="bg-primary h-3 rounded-full transition-all duration-500"
            style={{ width: `${progressPercent}%` }}
          />
        </div>
        <p className="text-xs text-gray-400 mt-1">{completedCount} / {totalCount} tasks completed</p>
      </div>

      {phases.length === 0 ? (
        <p className="text-center text-gray-500 py-10">No tasks yet.</p>
      ) : (
        <div className="space-y-8">
          {phases.map(phase => (
            <section key={phase}>
              <h2 className="text-lg font-semibold text-gray-800 dark:text-gray-200 mb-3 flex items-center gap-2">
                <span className="w-2 h-2 rounded-full bg-primary inline-block" />
                {phase}
              </h2>
              <div className="space-y-2">
                {grouped[phase].map(todo => {
                  const serviceLink = findServiceLink(todo.title);
                  return (
                    <div
                      key={todo.id}
                      className="flex items-center gap-4 p-4 bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700"
                    >
                      <button
                        onClick={() => handleStatusChange(todo)}
                        className={`shrink-0 w-6 h-6 rounded-full border-2 flex items-center justify-center transition-colors ${
                          todo.status === 'COMPLETED'
                            ? 'border-green-500 bg-green-500 text-white'
                            : todo.status === 'IN_PROGRESS'
                            ? 'border-blue-500 bg-blue-100 dark:bg-blue-900/30'
                            : 'border-gray-300 dark:border-gray-600'
                        }`}
                        title={`Click to change to ${STATUS_LABELS[NEXT_STATUS[todo.status]]}`}
                      >
                        {todo.status === 'COMPLETED' && (
                          <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={3}>
                            <path strokeLinecap="round" strokeLinejoin="round" d="M5 13l4 4L19 7" />
                          </svg>
                        )}
                        {todo.status === 'IN_PROGRESS' && (
                          <span className="w-2 h-2 rounded-full bg-blue-500" />
                        )}
                      </button>

                      <div className="flex-1 min-w-0">
                        <p className={`font-medium ${todo.status === 'COMPLETED' ? 'line-through text-gray-400' : 'text-gray-900 dark:text-white'}`}>
                          {todo.title}
                        </p>
                        {todo.description && (
                          <p className="text-sm text-gray-500 dark:text-gray-400 truncate">{todo.description}</p>
                        )}
                      </div>

                      {serviceLink && <ServiceButton link={serviceLink} />}

                      <span className={`shrink-0 px-2 py-0.5 rounded text-xs font-medium ${STATUS_COLORS[todo.status]}`}>
                        {STATUS_LABELS[todo.status]}
                      </span>
                    </div>
                  );
                })}
              </div>
            </section>
          ))}
        </div>
      )}
    </div>
  );
}
