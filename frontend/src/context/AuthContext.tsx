import {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import { jwtDecode } from 'jwt-decode';
import axios from 'axios';
import type { AuthUser, JwtAccessClaims, TokenResponse } from '../types';

interface AuthContextValue {
  user: AuthUser | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, password: string) => Promise<void>;
  signup: (email: string, password: string, displayName: string) => Promise<void>;
  logout: () => Promise<void>;
}

const ACCESS_TOKEN_KEY = 'accessToken';
const REFRESH_TOKEN_KEY = 'refreshToken';

const AuthContext = createContext<AuthContextValue | null>(null);

function decodeUser(token: string): AuthUser {
  const claims = jwtDecode<JwtAccessClaims>(token);
  return {
    id: Number(claims.sub),
    email: claims.email,
    displayName: '',
    roles: claims.roles,
  };
}

function isExpired(token: string): boolean {
  try {
    const { exp } = jwtDecode<JwtAccessClaims>(token);
    if (typeof exp !== 'number') return true;
    return exp * 1000 <= Date.now();
  } catch {
    return true;
  }
}

async function fetchMe(token: string): Promise<AuthUser | null> {
  try {
    const res = await axios.get<AuthUser>('/api/users/me', {
      headers: { Authorization: `Bearer ${token}` },
    });
    return res.data;
  } catch {
    return null;
  }
}

interface AuthProviderProps {
  children: ReactNode;
}

export function AuthProvider({ children }: AuthProviderProps) {
  const [user, setUser] = useState<AuthUser | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;

    const bootstrap = async () => {
      try {
        const storedAccess = localStorage.getItem(ACCESS_TOKEN_KEY);
        if (!storedAccess) {
          return;
        }
        if (isExpired(storedAccess)) {
          localStorage.removeItem(ACCESS_TOKEN_KEY);
          localStorage.removeItem(REFRESH_TOKEN_KEY);
          return;
        }

        const claimsUser = decodeUser(storedAccess);
        if (cancelled) return;
        setAccessToken(storedAccess);
        setUser(claimsUser);

        const me = await fetchMe(storedAccess);
        if (cancelled) return;
        if (me) {
          setUser(me);
        }
      } catch {
        localStorage.removeItem(ACCESS_TOKEN_KEY);
        localStorage.removeItem(REFRESH_TOKEN_KEY);
        if (!cancelled) {
          setAccessToken(null);
          setUser(null);
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    };

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email: string, password: string) => {
    const res = await axios.post<TokenResponse>('/api/auth/login', { email, password });
    const { accessToken: nextAccess, refreshToken: nextRefresh } = res.data;

    localStorage.setItem(ACCESS_TOKEN_KEY, nextAccess);
    localStorage.setItem(REFRESH_TOKEN_KEY, nextRefresh);

    const claimsUser = decodeUser(nextAccess);
    setAccessToken(nextAccess);
    setUser(claimsUser);

    const me = await fetchMe(nextAccess);
    if (me) {
      setUser(me);
    }
  }, []);

  const signup = useCallback(
    async (email: string, password: string, displayName: string) => {
      await axios.post('/api/auth/signup', { email, password, displayName });
    },
    [],
  );

  const logout = useCallback(async () => {
    const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
    if (refreshToken) {
      await axios.post('/api/auth/logout', { refreshToken }).catch(() => {});
    }
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    setAccessToken(null);
    setUser(null);
  }, []);

  const value: AuthContextValue = {
    user,
    accessToken,
    isAuthenticated: Boolean(user && accessToken),
    isLoading,
    login,
    signup,
    logout,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return ctx;
}
