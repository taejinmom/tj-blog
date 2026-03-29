import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { postApi } from '../../api/client';
import type { Post } from '../../types';
import Pagination from '../../components/Pagination';

const POSTS_PER_PAGE = 6;

export default function BlogList() {
  const [posts, setPosts] = useState<Post[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const navigate = useNavigate();

  useEffect(() => {
    postApi.getAll()
      .then(setPosts)
      .catch(console.error)
      .finally(() => setLoading(false));
  }, []);

  const totalPages = Math.ceil(posts.length / POSTS_PER_PAGE);
  const paginated = posts.slice((page - 1) * POSTS_PER_PAGE, page * POSTS_PER_PAGE);

  if (loading) {
    return <div className="flex justify-center py-20 text-gray-500">Loading...</div>;
  }

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-8">
      <div className="flex items-center justify-between mb-8">
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Blog</h1>
        <button
          onClick={() => navigate('/write')}
          className="px-4 py-2 bg-primary text-white rounded-lg hover:bg-primary-dark transition-colors text-sm font-medium"
        >
          New Post
        </button>
      </div>

      {posts.length === 0 ? (
        <p className="text-center text-gray-500 py-20">No posts yet. Write your first post!</p>
      ) : (
        <>
          <div className="grid gap-6 md:grid-cols-2">
            {paginated.map(post => (
              <Link
                key={post.id}
                to={`/posts/${post.id}`}
                className="block p-6 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 hover:shadow-lg transition-shadow"
              >
                <span className="inline-block px-2 py-0.5 text-xs font-medium rounded bg-primary/10 text-primary dark:text-indigo-400 mb-3">
                  {post.category}
                </span>
                <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-2 line-clamp-2">
                  {post.title}
                </h2>
                <p className="text-sm text-gray-500 dark:text-gray-400 line-clamp-3">
                  {post.content.replace(/[#*`>-]/g, '').slice(0, 150)}...
                </p>
                <time className="block mt-3 text-xs text-gray-400">
                  {new Date(post.createdAt).toLocaleDateString()}
                </time>
              </Link>
            ))}
          </div>
          <Pagination currentPage={page} totalPages={totalPages} onPageChange={setPage} />
        </>
      )}
    </div>
  );
}
