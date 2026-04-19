import { lazy, Suspense, useEffect, type ReactNode } from 'react';
import { Routes, Route, Navigate, useNavigate } from 'react-router-dom';
import Navbar from './components/Navbar';
import BlogList from './pages/Blog/BlogList';
import BlogDetail from './pages/Blog/BlogDetail';
import BlogEditor from './pages/Blog/BlogEditor';
import TodoListPage from './pages/TodoList/TodoListPage';
import AboutPage from './pages/About/AboutPage';
import LoginPage from './pages/Auth/LoginPage';
import SignupPage from './pages/Auth/SignupPage';
import ProtectedRoute from './components/ProtectedRoute';
import { useAuth } from './context/AuthContext';

// Chat pulls in @stomp/stompjs which is heavy — lazy-load it.
const ChatPage = lazy(() => import('./pages/Chat/ChatPage'));

function RedirectIfAuthed({ children }: { children: ReactNode }) {
  const { isAuthenticated, isLoading } = useAuth();
  if (isLoading) return null;
  return isAuthenticated ? <Navigate to="/" replace /> : <>{children}</>;
}

function ChatFallback() {
  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 py-10 text-sm text-gray-500 dark:text-gray-400">
      Loading chat...
    </div>
  );
}

export default function App() {
  const navigate = useNavigate();

  // Listen for the `auth:expired` event dispatched by the axios interceptor
  // when a refresh-token exchange fails. Redirect to /login so the user can
  // re-authenticate. AuthContext already clears its own state via the same
  // storage updates the interceptor performs.
  useEffect(() => {
    const handler = () => {
      navigate('/login', { replace: true });
    };
    window.addEventListener('auth:expired', handler);
    return () => window.removeEventListener('auth:expired', handler);
  }, [navigate]);

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-gray-100">
      <Navbar />
      <main>
        <Routes>
          {/* Public */}
          <Route path="/" element={<BlogList />} />
          <Route path="/posts/:id" element={<BlogDetail />} />
          <Route path="/todos" element={<TodoListPage />} />
          <Route path="/about" element={<AboutPage />} />

          {/* Admin-only blog editor */}
          <Route
            path="/write"
            element={
              <ProtectedRoute requireRole="ROLE_ADMIN">
                <BlogEditor />
              </ProtectedRoute>
            }
          />
          <Route
            path="/write/:id"
            element={
              <ProtectedRoute requireRole="ROLE_ADMIN">
                <BlogEditor />
              </ProtectedRoute>
            }
          />

          {/* Chat (auth required, lazy-loaded) */}
          <Route
            path="/chat"
            element={
              <ProtectedRoute>
                <Suspense fallback={<ChatFallback />}>
                  <ChatPage />
                </Suspense>
              </ProtectedRoute>
            }
          />
          <Route
            path="/chat/:roomId"
            element={
              <ProtectedRoute>
                <Suspense fallback={<ChatFallback />}>
                  <ChatPage />
                </Suspense>
              </ProtectedRoute>
            }
          />

          {/* Auth pages — redirect if already authed */}
          <Route
            path="/login"
            element={
              <RedirectIfAuthed>
                <LoginPage />
              </RedirectIfAuthed>
            }
          />
          <Route
            path="/signup"
            element={
              <RedirectIfAuthed>
                <SignupPage />
              </RedirectIfAuthed>
            }
          />
        </Routes>
      </main>
    </div>
  );
}
