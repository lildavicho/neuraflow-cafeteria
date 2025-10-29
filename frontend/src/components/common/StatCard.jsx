import { useEffect, useRef } from 'react';
import gsap from 'gsap';

const StatCard = ({ title, value, change, icon, color = 'blue', index = 0 }) => {
  const cardRef = useRef(null);

  useEffect(() => {
    if (cardRef.current) {
      gsap.fromTo(
        cardRef.current,
        { opacity: 0, y: 30 },
        { opacity: 1, y: 0, duration: 0.4, delay: index * 0.1, ease: 'power2.out' }
      );
    }
  }, [index]);

  const colorClasses = {
    brand: 'bg-brand-100 dark:bg-brand-900/20 text-brand-600 dark:text-brand-400',
    success: 'bg-success-100 dark:bg-success-900/20 text-success-600 dark:text-success-400',
    info: 'bg-info-100 dark:bg-info-900/20 text-info-600 dark:text-info-400',
    warning: 'bg-warning-100 dark:bg-warning-900/20 text-warning-600 dark:text-warning-400',
    danger: 'bg-danger-100 dark:bg-danger-900/20 text-danger-600 dark:text-danger-400',
    // Aliases
    blue: 'bg-info-100 dark:bg-info-900/20 text-info-600 dark:text-info-400',
    green: 'bg-success-100 dark:bg-success-900/20 text-success-600 dark:text-success-400',
    purple: 'bg-brand-100 dark:bg-brand-900/20 text-brand-600 dark:text-brand-400',
    orange: 'bg-warning-100 dark:bg-warning-900/20 text-warning-600 dark:text-warning-400',
    red: 'bg-danger-100 dark:bg-danger-900/20 text-danger-600 dark:text-danger-400',
  };

  return (
    <div
      ref={cardRef}
      className="card-brand"
    >
      <div className="flex items-center justify-between">
        <div>
          <p className="text-sm font-medium text-gray-600 dark:text-gray-400 mb-1">{title}</p>
          <p className="text-3xl font-bold text-gray-900 dark:text-gray-100">{value}</p>
          {change && (
            <p className={`text-sm mt-1 font-medium ${change.startsWith('+') ? 'text-success-600 dark:text-success-400' : 'text-danger-600 dark:text-danger-400'}`}>
              {change}
            </p>
          )}
        </div>
        <div className={`w-12 h-12 rounded-full flex items-center justify-center ${colorClasses[color] || colorClasses.brand}`}>
          <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d={icon} />
          </svg>
        </div>
      </div>
    </div>
  );
};

export default StatCard;
