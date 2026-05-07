// src/App.jsx
import { useEffect } from 'react';
import {
  BrowserRouter,
  Routes,
  Route,
  Navigate,
} from 'react-router-dom';
import useAuthStore from './store/authStore';
import PrivateRoute from './components/layout/PrivateRoute';
import DietPage from './pages/Diet/DietPage';
import WorkoutPage from './pages/Workout/WorkoutPage';
import ReportPage from './pages/Report/ReportPage';

// Pages — lazy-loaded for code splitting
import { lazy, Suspense } from 'react';

const LoginPage = lazy(() => import('./pages/onboarding/LoginPage'));
const SignupPage = lazy(() => import('./pages/onboarding/SignupPage'));
const AiSetupPage = lazy(() => import('./pages/onboarding/AiSetupPage'));
const AiLoadingPage = lazy(() => import('./pages/onboarding/AiLoadingPage'));
const DashboardPage = lazy(() => import('./pages/dashboard/DashboardPage'));

const PageLoader = () => (
  <div className="min-h-screen bg-[#E4E1DC] flex items-center justify-center">
    <div className="w-6 h-6 border-2 border-[#4A7BFF] border-t-transparent rounded-full animate-spin" />
  </div>
);

const App = () => {
  const hydrate = useAuthStore((s) => s.hydrate);

  // Hydrate auth state from storage on first render
  useEffect(() => {
    hydrate();
  }, [hydrate]);

  return (
    <BrowserRouter>
      <Suspense fallback={<PageLoader />}>
        <Routes>
          {/* Public routes */}
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />

          {/* 온보딩은 인증 불필요 - PrivateRoute 밖으로 */}
          <Route path="/onboarding/ai-setup" element={<AiSetupPage />} />
          <Route path="/onboarding/ai-loading" element={<AiLoadingPage />} />

          {/* Main app (requires auth) */}
          <Route element={<PrivateRoute />}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/diet" element={<DietPage />} />
            <Route path="/workout" element={<WorkoutPage />} />
            <Route path="/report" element={<ReportPage />} />
          </Route>

          {/* Catch-all */}
          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  );
};

export default App;