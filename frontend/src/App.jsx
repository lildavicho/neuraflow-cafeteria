import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import PrivateRoute from './components/layout/PrivateRoute';
import Layout from './components/layout/Layout';
import AuthLayout from './components/layout/AuthLayout';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import DashboardPage from './pages/DashboardPage';
import InventarioPage from './pages/InventarioPage';
import POSPage from './pages/POSPage';
import ReportesPage from './pages/ReportesPage';
import PerfilPage from './pages/PerfilPage';
import NotificacionesPage from './pages/NotificacionesPage';
import AjustesPage from './pages/AjustesPage';

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<AuthLayout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          </Route>

          <Route element={<PrivateRoute><Layout /></PrivateRoute>}>
            <Route path="/dashboard" element={<DashboardPage />} />
            <Route path="/inventario" element={<InventarioPage />} />
            <Route path="/pos" element={<POSPage />} />
            <Route path="/reportes" element={<ReportesPage />} />
            <Route path="/perfil" element={<PerfilPage />} />
            <Route path="/notificaciones" element={<NotificacionesPage />} />
            <Route path="/ajustes" element={<AjustesPage />} />
          </Route>

          <Route path="/" element={<Navigate to="/login" replace />} />
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
