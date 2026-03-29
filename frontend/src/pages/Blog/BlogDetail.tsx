import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import ReactMarkdown from 'react-markdown';
import { postApi } from '../../api/client';
import type { Post } from '../../types';

export default function BlogDetail() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [post, setPost] = useState<Post | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!id) return;
    postApi.getById(Number(id))
      .then(setPost)
      .catch(() => navigate('/'))
      .finally(() => setLoading(false));
  }, [id, navigate]);

  if (loading) return <div className="flex justify-center py-20 text-gray-500">Loading...</div>;
  if (!post) return null;

  const handleDelete = async () => {
    if (!confirm('Are you sure you want to delete this post?')) return;
    await postApi.delete(post.id);
    navigate('/');
  };

  return (
    <div className="max-w-3xl mx-auto px-4 sm:px-6 py-8">
      <button onClick={() => navigate('/')} className="text-sm text-gray-500 hover:text-primary mb-6 inline-block">
        &larr; Back to list
      </button>

      <article>
        <span className="inline-block px-2 py-0.5 text-xs font-medium rounded bg-primary/10 text-primary dark:text-indigo-400 mb-3">
          {post.category}
        </span>
        <h1 className="text-3xl font-bold text-gray-900 dark:text-white mb-2">{post.title}</h1>
        <time className="text-sm text-gray-400 block mb-8">
          {new Date(post.createdAt).toLocaleDateString()}
        </time>

        <div className="prose dark:prose-invert max-w-none">
          <ReactMarkdown>{post.content}</ReactMarkdown>
        </div>
      </article>

      <div className="flex gap-3 mt-10 pt-6 border-t border-gray-200 dark:border-gray-700">
        <button
          onClick={() => navigate(`/write/${post.id}`)}
          className="px-4 py-2 text-sm font-medium border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800"
        >
          Edit
        </button>
        <button
          onClick={handleDelete}
          className="px-4 py-2 text-sm font-medium text-red-600 border border-red-300 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20"
        >
          Delete
        </button>
      </div>
    </div>
  );
}
