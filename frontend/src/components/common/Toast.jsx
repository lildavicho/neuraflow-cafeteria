import { useEffect, useRef } from 'react';
import gsap from 'gsap';

const Toast = ({ toasts, onRemove }) => {
  const toastRefs = useRef([]);

  useEffect(() => {
    toastRefs.current.forEach((toast) => {
      if (toast) {
        gsap.fromTo(
          toast,
          { opacity: 0, x: 100 },
          { opacity: 1, x: 0, duration: 0.3, ease: 'power2.out' }
        );
      }
    });
  }, [toasts]);

  const getIcon = (type) => {
    switch (type) {
      case 'success':
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M5 13l4 4L19 7" />
          </svg>
        );
      case 'error':
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
          </svg>
        );
      case 'warning':
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" />
          </svg>
        );
      default:
        return (
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
        );
    }
  };

  const getStyles = (type) => {
    switch (type) {
      case 'success':
        return 'bg-success-50 dark:bg-success-900/20 text-success-700 dark:text-success-300 border-success-200 dark:border-success-700';
      case 'error':
        return 'bg-danger-50 dark:bg-danger-900/20 text-danger-700 dark:text-danger-300 border-danger-200 dark:border-danger-700';
      case 'warning':
        return 'bg-warning-50 dark:bg-warning-900/20 text-warning-700 dark:text-warning-300 border-warning-200 dark:border-warning-700';
      default:
        return 'bg-info-50 dark:bg-info-900/20 text-info-700 dark:text-info-300 border-info-200 dark:border-info-700';
    }
  };

  return (
    <div className="fixed top-4 right-4 z-50 space-y-2">
      {toasts.map((toast, index) => (
        <div
          key={toast.id}
          ref={(el) => (toastRefs.current[index] = el)}
          className={`flex items-center gap-3 px-4 py-3 rounded-lg border shadow-lg min-w-[300px] ${getStyles(toast.type)}`}
        >
          <div className="flex-shrink-0">{getIcon(toast.type)}</div>
          <p className="flex-1 text-sm font-medium">{toast.message}</p>
          <button
            onClick={() => onRemove(toast.id)}
            className="flex-shrink-0 hover:opacity-70 transition-opacity"
          >
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12" />
            </svg>
          </button>
        </div>
      ))}
    </div>
  );
};

export default Toast;
