import type { ReactNode } from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

interface Props {
  children: ReactNode;
  requireRole?: string;
}

export default function ProtectedRoute({ children, requireRole }: Props) {
  const { isAuthenticated, isLoading, user } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="flex justify-center py-20 text-gray-500">Loading...</div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />;
  }

  if (requireRole && !user?.roles.includes(requireRole)) {
    return <Navigate to="/" replace />;
  }

  return <>{children}</>;
}
