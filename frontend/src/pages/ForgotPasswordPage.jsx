import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { resetPassword } from '../services/authService';
import gsap from 'gsap';
import logo from '../assets/logo.svg';

const ForgotPasswordPage = () => {
  const [email, setEmail] = useState('');
  const [error, setError] = useState('');
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);
  
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

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess(false);
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/;
    if (!emailPattern.test(email.trim())) {
      setError('Ingresa un correo válido');
      return;
    }
    setLoading(true);

    try {
      await resetPassword(email);
      setSuccess(true);
      setEmail('');
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
        <img src={logo} alt="Logo Cafetería" className="w-full h-full drop-shadow-2xl" />
      </div>

      {/* Card principal */}
      <div ref={cardRef} className="card-brand">
        {/* Header */}
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900 dark:text-gray-100 mb-2">Recuperar Contraseña</h1>
          <p className="text-gray-600 dark:text-gray-400">Ingresa tu correo para recibir instrucciones</p>
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

        {/* Success message */}
        {success && (
          <div className="bg-success-50 dark:bg-success-900/20 border-2 border-success-200 dark:border-success-700 text-success-700 dark:text-success-300 px-4 py-3 rounded-lg mb-6 flex items-center gap-3">
            <svg className="w-5 h-5 flex-shrink-0" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
            </svg>
            <span className="text-sm font-medium">Correo de recuperación enviado. Revisa tu bandeja de entrada.</span>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label className="block text-sm font-semibold text-gray-900 dark:text-gray-100 mb-2">
              Correo Electrónico
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              placeholder="tu@email.com"
              className="input-brand"
              autoComplete="email"
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
                Enviando...
              </>
            ) : (
              'Enviar Correo de Recuperación'
            )}
          </button>
        </form>

        {/* Back to login link */}
        <div className="mt-6 text-center">
          <Link to="/login" className="link-brand text-sm font-semibold inline-flex items-center gap-2">
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M10 19l-7-7m0 0l7-7m-7 7h18" />
            </svg>
            Volver al inicio de sesión
          </Link>
        </div>
      </div>

      {/* Footer */}
      <p className="text-center text-xs text-gray-500 dark:text-gray-400 mt-6">
        © 2025 NeuraFlow. Todos los derechos reservados.
      </p>
    </div>
  );
};

export default ForgotPasswordPage;
