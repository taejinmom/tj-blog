import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const publicNavItems = [
  { path: '/', label: 'Blog' },
  { path: '/todos', label: 'TodoList' },
  { path: '/about', label: 'About' },
];

const authedNavItems = [
  { path: '/', label: 'Blog' },
  { path: '/todos', label: 'TodoList' },
  { path: '/chat', label: 'Chat' },
  { path: '/about', label: 'About' },
];

export default function Navbar() {
  const { pathname } = useLocation();
  const navigate = useNavigate();
  const { user, isAuthenticated, logout } = useAuth();

  const navItems = isAuthenticated ? authedNavItems : publicNavItems;

  const handleLogout = async () => {
    await logout();
    navigate('/');
  };

  const linkClasses = (active: boolean) =>
    `px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
      active
        ? 'bg-primary/10 text-primary dark:text-indigo-400'
        : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'
    }`;

  return (
    <nav className="bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-50">
      <div className="max-w-5xl mx-auto px-4 sm:px-6">
        <div className="flex items-center justify-between h-16">
          <Link to="/" className="text-xl font-bold text-primary dark:text-indigo-400">
            Portfolio Blog
          </Link>
          <div className="flex items-center gap-1">
            {navItems.map(({ path, label }) => {
              const active = path === '/' ? pathname === '/' : pathname.startsWith(path);
              return (
                <Link key={path} to={path} className={linkClasses(active)}>
                  {label}
                </Link>
              );
            })}

            {isAuthenticated ? (
              <>
                <span className="px-3 py-2 text-sm text-gray-700 dark:text-gray-300">
                  {user?.displayName || user?.email}
                </span>
                <button
                  type="button"
                  onClick={handleLogout}
                  className="px-4 py-2 rounded-lg text-sm font-medium text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800"
                >
                  Logout
                </button>
              </>
            ) : (
              <Link to="/login" className={linkClasses(pathname.startsWith('/login'))}>
                Login
              </Link>
            )}
          </div>
        </div>
      </div>
    </nav>
  );
}
