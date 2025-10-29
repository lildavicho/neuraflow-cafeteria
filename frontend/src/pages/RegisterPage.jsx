import { useState, useEffect, useRef } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { register } from '../services/authService';
import gsap from 'gsap';
import logo from '../assets/logo.svg';

const RegisterPage = () => {
  const [formData, setFormData] = useState({
    displayName: '',
    email: '',
    password: '',
    confirmPassword: '',
  });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [passwordStrength, setPasswordStrength] = useState(0);
  const navigate = useNavigate();
  
  const cardRef = useRef(null);
  const logoRef = useRef(null);

  useEffect(() => {
    gsap.fromTo(
      logoRef.current,
      { scale: 0, rotate: -180 },
      { scale: 1, rotate: 0, duration: 0.6, ease: 'back.out(1.7)' }
    );

    gsap.fromTo(
      cardRef.current,
      { opacity: 0, y: 30 },
      { opacity: 1, y: 0, duration: 0.5, delay: 0.2, ease: 'power2.out' }
    );
  }, []);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));

    if (name === 'password') {
      calculatePasswordStrength(value);
    }
  };

  const calculatePasswordStrength = (password) => {
    let strength = 0;
    if (password.length >= 8) strength += 25;
    if (/[a-z]/.test(password) && /[A-Z]/.test(password)) strength += 25;
    if (/\d/.test(password)) strength += 25;
    if (/[^a-zA-Z\d]/.test(password)) strength += 25;
    setPasswordStrength(strength);
  };

  const getStrengthColor = () => {
    if (passwordStrength <= 25) return 'bg-danger';
    if (passwordStrength <= 50) return 'bg-warning';
    if (passwordStrength <= 75) return 'bg-warning-400';
    return 'bg-success';
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (formData.password !== formData.confirmPassword) {
      setError('Las contraseñas no coinciden');
      return;
    }

    if (formData.password.length < 6) {
      setError('La contraseña debe tener al menos 6 caracteres');
      return;
    }

    setLoading(true);

    try {
      await register(formData.email, formData.password, formData.displayName);
      navigate('/dashboard');
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="w-full max-w-md">
      {/* Logo animado */}
      <div ref={logoRef} className="w-24 h-24 mx-auto mb-6">
        <img src={logo} alt="UCACUE Bar Logo" className="w-full h-full drop-shadow-2xl" />
      </div>

      {/* Card principal */}
      <div ref={cardRef} className="card-brand">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-2">Crear Cuenta</h1>
          <p className="text-gray-600 dark:text-gray-400">Únete al sistema UCACUE Bar</p>
        </div>

        {/* Error message */}
        {error && (
          <div className="bg-danger-50 dark:bg-danger-900/20 border-2 border-danger-200 dark:border-danger-700 text-danger-700 dark:text-danger-300 px-4 py-3 rounded-lg mb-6 flex items-center gap-3 animate-shake">
            <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 8v4m0 4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span className="text-sm font-medium">{error}</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-5">
          {/* Nombre Completo */}
          <div>
            <label className="block text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
              Nombre Completo
            </label>
            <input
              type="text"
              name="displayName"
              value={formData.displayName}
              onChange={handleChange}
              required
              placeholder="Juan Pérez"
              className="input-brand"
              autoComplete="name"
            />
          </div>

          {/* Email */}
          <div>
            <label className="block text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
              Correo Electrónico
            </label>
            <input
              type="email"
              name="email"
              value={formData.email}
              onChange={handleChange}
              required
              placeholder="usuario@ucacue.edu.ec"
              className="input-brand"
              autoComplete="email"
            />
          </div>

          {/* Password */}
          <div>
            <label className="block text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
              Contraseña
            </label>
            <input
              type="password"
              name="password"
              value={formData.password}
              onChange={handleChange}
              required
              minLength="6"
              placeholder="Mínimo 6 caracteres"
              className="input-brand"
              autoComplete="new-password"
            />
            {/* Password strength indicator */}
            <div className="mt-2 h-1 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden">
              <div
                className={`h-full transition-all duration-300 ${getStrengthColor()}`}
                style={{ width: `${passwordStrength}%` }}
              />
            </div>
          </div>

          {/* Confirm Password */}
          <div>
            <label className="block text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
              Confirmar Contraseña
            </label>
            <input
              type="password"
              name="confirmPassword"
              value={formData.confirmPassword}
              onChange={handleChange}
              required
              placeholder="Repite tu contraseña"
              className="input-brand"
              autoComplete="new-password"
            />
          </div>

          {/* Submit button */}
          <button
            type="submit"
            disabled={loading}
            className="btn-brand w-full"
          >
            {loading ? (
              <>
                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                Creando cuenta...
              </>
            ) : (
              'Crear Cuenta'
            )}
          </button>
        </form>

        {/* Login link */}
        <p className="text-center text-sm text-gray-600 dark:text-gray-400 mt-6">
          ¿Ya tienes cuenta?{' '}
          <Link to="/login" className="link-brand font-semibold">
            Inicia sesión aquí
          </Link>
        </p>
      </div>

      {/* Footer */}
      <p className="text-center text-xs text-gray-500 dark:text-gray-400 mt-6">
        © 2025 UCACUE Bar. Todos los derechos reservados.
      </p>
    </div>
  );
};

export default RegisterPage;
