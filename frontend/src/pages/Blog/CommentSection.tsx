import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { commentApi } from '../../api/client';
import { useAuth } from '../../context/AuthContext';
import type { Comment } from '../../types';

export default function CommentSection({ postId }: { postId: number }) {
  const { user, isAuthenticated } = useAuth();
  const [comments, setComments] = useState<Comment[]>([]);
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const isAdmin = user?.roles?.includes('ROLE_ADMIN') ?? false;

  useEffect(() => {
    commentApi.list(postId)
      .then(setComments)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [postId]);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!content.trim()) return;
    setSubmitting(true);
    try {
      const created = await commentApi.create(postId, content.trim());
      setComments(prev => [...prev, created]);
      setContent('');
    } catch (err) {
      console.error(err);
      alert('댓글 작성에 실패했습니다.');
    } finally {
      setSubmitting(false);
    }
  };

  const remove = async (id: number) => {
    if (!confirm('이 댓글을 삭제할까요?')) return;
    try {
      await commentApi.delete(id);
      setComments(prev => prev.filter(c => c.id !== id));
    } catch (err) {
      console.error(err);
      alert('삭제에 실패했습니다.');
    }
  };

  return (
    <section className="mt-12 pt-8 border-t border-gray-200 dark:border-gray-700">
      <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-5">
        댓글 {loading ? '' : `(${comments.length})`}
      </h2>

      {/* 작성 폼 */}
      {isAuthenticated ? (
        <form onSubmit={submit} className="mb-8">
          <textarea
            value={content}
            onChange={e => setContent(e.target.value)}
            rows={3}
            placeholder="댓글을 입력하세요..."
            className="w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary focus:border-transparent outline-none text-sm"
          />
          <div className="flex justify-end mt-2">
            <button
              type="submit"
              disabled={submitting || !content.trim()}
              className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark transition-colors text-sm font-medium disabled:opacity-50"
            >
              {submitting ? '작성 중...' : '댓글 작성'}
            </button>
          </div>
        </form>
      ) : (
        <p className="mb-8 text-sm text-gray-500 dark:text-gray-400">
          댓글을 작성하려면 <Link to="/login" className="text-primary hover:underline">로그인</Link>하세요.
        </p>
      )}

      {/* 목록 */}
      {loading ? (
        <p className="text-sm text-gray-400">불러오는 중...</p>
      ) : comments.length === 0 ? (
        <p className="text-sm text-gray-400">첫 댓글을 남겨보세요.</p>
      ) : (
        <ul className="space-y-4">
          {comments.map(c => {
            const canDelete = user && (user.id === c.authorId || isAdmin);
            return (
              <li key={c.id} className="p-4 bg-white dark:bg-gray-800 rounded-lg border border-gray-200 dark:border-gray-700">
                <div className="flex items-center justify-between mb-1">
                  <span className="text-sm font-medium text-gray-800 dark:text-gray-200">{c.authorName}</span>
                  <div className="flex items-center gap-3">
                    <time className="text-xs text-gray-400">{new Date(c.createdAt).toLocaleString()}</time>
                    {canDelete && (
                      <button onClick={() => remove(c.id)} className="text-xs text-red-500 hover:underline">삭제</button>
                    )}
                  </div>
                </div>
                <p className="text-sm text-gray-700 dark:text-gray-300 whitespace-pre-wrap">{c.content}</p>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}
