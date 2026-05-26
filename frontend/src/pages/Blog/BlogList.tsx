import { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import { postApi } from '../../api/client';
import type { Page, Post } from '../../types';
import Pagination from '../../components/Pagination';

const PAGE_SIZE = 6;

export default function BlogList() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();

  const q = searchParams.get('q') ?? '';
  const tag = searchParams.get('tag') ?? '';
  const pageParam = Number(searchParams.get('page') ?? '1');
  const page = Number.isFinite(pageParam) && pageParam > 0 ? pageParam : 1;

  const [data, setData] = useState<Page<Post> | null>(null);
  const [loading, setLoading] = useState(true);
  const [searchInput, setSearchInput] = useState(q);

  // URL 의 q 가 바뀌면(뒤로가기 등) 입력창도 동기화
  useEffect(() => { setSearchInput(q); }, [q]);

  useEffect(() => {
    setLoading(true);
    postApi
      .list({ page: page - 1, size: PAGE_SIZE, q: q || undefined, tag: tag || undefined })
      .then(setData)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, [q, tag, page]);

  const updateParams = (next: Record<string, string | undefined>) => {
    const sp = new URLSearchParams(searchParams);
    Object.entries(next).forEach(([k, v]) => {
      if (v) sp.set(k, v);
      else sp.delete(k);
    });
    setSearchParams(sp);
  };

  const submitSearch = (e: React.FormEvent) => {
    e.preventDefault();
    updateParams({ q: searchInput.trim() || undefined, tag: undefined, page: undefined });
  };

  const posts = data?.content ?? [];
  const totalPages = data?.totalPages ?? 0;
  const hasFilter = Boolean(q || tag);

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-8">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Blog</h1>
        <button
          onClick={() => navigate('/write')}
          className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark transition-colors text-sm font-medium"
        >
          New Post
        </button>
      </div>

      {/* 검색 */}
      <form onSubmit={submitSearch} className="mb-4 flex gap-2">
        <input
          type="text"
          value={searchInput}
          onChange={e => setSearchInput(e.target.value)}
          placeholder="제목·내용 검색..."
          className="flex-1 px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-800 text-gray-900 dark:text-white focus:ring-2 focus:ring-primary focus:border-transparent outline-none text-sm"
        />
        <button type="submit" className="px-4 py-2 bg-gray-100 dark:bg-gray-700 rounded-lg text-sm hover:bg-gray-200 dark:hover:bg-gray-600">
          검색
        </button>
      </form>

      {/* 활성 필터 표시 */}
      {hasFilter && (
        <div className="mb-6 flex items-center gap-2 text-sm text-gray-600 dark:text-gray-400">
          <span>
            {tag ? <>태그 <b className="text-primary">#{tag}</b></> : <>검색 <b className="text-primary">"{q}"</b></>} 결과
            {data ? ` · ${data.totalElements}건` : ''}
          </span>
          <button onClick={() => updateParams({ q: undefined, tag: undefined, page: undefined })} className="underline hover:text-primary">
            필터 해제
          </button>
        </div>
      )}

      {loading ? (
        <div className="flex justify-center py-20 text-gray-500">Loading...</div>
      ) : posts.length === 0 ? (
        <p className="text-center text-gray-500 py-20">
          {hasFilter ? '검색 결과가 없습니다.' : 'No posts yet. Write your first post!'}
        </p>
      ) : (
        <>
          <div className="grid gap-6 md:grid-cols-2">
            {posts.map(post => (
              <div
                key={post.id}
                className="block p-6 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 hover:shadow-lg transition-shadow"
              >
                <Link to={`/posts/${post.id}`}>
                  <span className="inline-block px-2 py-0.5 text-xs font-medium rounded bg-primary/10 text-primary dark:text-indigo-400 mb-3">
                    {post.category}
                  </span>
                  <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2 line-clamp-2">
                    {post.title}
                  </h2>
                  <p className="text-sm text-gray-500 dark:text-gray-400 line-clamp-3">
                    {post.content.replace(/[#*`>-]/g, '').slice(0, 150)}...
                  </p>
                </Link>
                {post.tags?.length > 0 && (
                  <div className="flex flex-wrap gap-1.5 mt-3">
                    {post.tags.map(t => (
                      <button
                        key={t}
                        onClick={() => updateParams({ tag: t, q: undefined, page: undefined })}
                        className="px-2 py-0.5 text-xs rounded-full bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-300 hover:bg-primary/10 hover:text-primary"
                      >
                        #{t}
                      </button>
                    ))}
                  </div>
                )}
                <time className="block mt-3 text-xs text-gray-400">
                  {new Date(post.createdAt).toLocaleDateString()}
                </time>
              </div>
            ))}
          </div>
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={p => updateParams({ page: p > 1 ? String(p) : undefined })} />
        </>
      )}
    </div>
  );
}
