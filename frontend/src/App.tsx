import { Routes, Route } from 'react-router-dom';
import Navbar from './components/Navbar';
import BlogList from './pages/Blog/BlogList';
import BlogDetail from './pages/Blog/BlogDetail';
import BlogEditor from './pages/Blog/BlogEditor';
import TodoListPage from './pages/TodoList/TodoListPage';
import AboutPage from './pages/About/AboutPage';

export default function App() {
  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900 text-gray-900 dark:text-gray-100">
      <Navbar />
      <main>
        <Routes>
          <Route path="/" element={<BlogList />} />
          <Route path="/posts/:id" element={<BlogDetail />} />
          <Route path="/write" element={<BlogEditor />} />
          <Route path="/write/:id" element={<BlogEditor />} />
          <Route path="/todos" element={<TodoListPage />} />
          <Route path="/about" element={<AboutPage />} />
        </Routes>
      </main>
    </div>
  );
}
