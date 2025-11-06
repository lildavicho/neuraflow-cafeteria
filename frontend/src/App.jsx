import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './contexts/AuthContext';
import { ThemeProvider } from './contexts/ThemeContext';
import { LanguageProvider } from './contexts/LanguageContext';
import PrivateRoute from './components/layout/PrivateRoute';
import AdminRoute from './components/layout/AdminRoute';
import RoleRedirect from './components/layout/RoleRedirect';
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
    <ThemeProvider>
      <LanguageProvider>
        <AuthProvider>
          <BrowserRouter>
            <Routes>
          <Route element={<AuthLayout />}>
            <Route path="/login" element={<LoginPage />} />
            <Route path="/register" element={<RegisterPage />} />
            <Route path="/forgot-password" element={<ForgotPasswordPage />} />
          </Route>

          <Route element={<PrivateRoute><Layout /></PrivateRoute>}>
            <Route path="/dashboard" element={<AdminRoute><DashboardPage /></AdminRoute>} />
            <Route path="/inventario" element={<AdminRoute><InventarioPage /></AdminRoute>} />
            <Route path="/reportes" element={<AdminRoute><ReportesPage /></AdminRoute>} />
            <Route path="/pos" element={<POSPage />} />
            <Route path="/perfil" element={<PerfilPage />} />
            <Route path="/notificaciones" element={<NotificacionesPage />} />
            <Route path="/ajustes" element={<AjustesPage />} />
          </Route>

          <Route path="/" element={<RoleRedirect />} />
          <Route path="*" element={<RoleRedirect />} />
            </Routes>
          </BrowserRouter>
        </AuthProvider>
      </LanguageProvider>
    </ThemeProvider>
  );
}

export default App;
