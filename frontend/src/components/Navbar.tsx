import { Link, useLocation } from 'react-router-dom';

const navItems = [
  { path: '/', label: 'Blog' },
  { path: '/todos', label: 'TodoList' },
  { path: '/about', label: 'About' },
];

export default function Navbar() {
  const { pathname } = useLocation();

  return (
    <nav className="bg-white dark:bg-gray-900 border-b border-gray-200 dark:border-gray-700 sticky top-0 z-50">
      <div className="max-w-5xl mx-auto px-4 sm:px-6">
        <div className="flex items-center justify-between h-16">
          <Link to="/" className="text-xl font-bold text-primary dark:text-indigo-400">
            Portfolio Blog
          </Link>
          <div className="flex gap-1">
            {navItems.map(({ path, label }) => {
              const active = path === '/' ? pathname === '/' : pathname.startsWith(path);
              return (
                <Link
                  key={path}
                  to={path}
                  className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors ${
                    active
                      ? 'bg-primary/10 text-primary dark:text-indigo-400'
                      : 'text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800'
                  }`}
                >
                  {label}
                </Link>
              );
            })}
          </div>
        </div>
      </div>
    </nav>
  );
}
