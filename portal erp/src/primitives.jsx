/* primitives.jsx — shared chart + UI atoms used across portal pages */

const { useState, useEffect, useRef, useMemo } = React;

/* ---------- Typewriter with rotating words ---------- */
function Typewriter({ words = [], typeSpeed = 70, deleteSpeed = 35, pauseAfter = 1400, className = '', cursor = true }) {
  const [text, setText] = useState('');
  const [wIdx, setWIdx] = useState(0);
  const [phase, setPhase] = useState('typing'); // typing | pausing | deleting

  useEffect(() => {
    const word = words[wIdx % words.length] || '';
    let t;
    if (phase === 'typing') {
      if (text.length < word.length) {
        t = setTimeout(() => setText(word.slice(0, text.length + 1)), typeSpeed);
      } else {
        t = setTimeout(() => setPhase('deleting'), pauseAfter);
      }
    } else if (phase === 'deleting') {
      if (text.length > 0) {
        t = setTimeout(() => setText(word.slice(0, text.length - 1)), deleteSpeed);
      } else {
        setWIdx(i => i + 1);
        setPhase('typing');
      }
    }
    return () => clearTimeout(t);
  }, [text, phase, wIdx, words, typeSpeed, deleteSpeed, pauseAfter]);

  return (
    <span className={className}>
      {text}
      {cursor && <span className="iv-caret" aria-hidden="true" />}
    </span>
  );
}

/* ---------- Sparkline ---------- */
function Sparkline({ data, width = 80, height = 24, color = 'var(--iv-primary)', fill = true }) {
  if (!data || data.length === 0) return null;
  const min = Math.min(...data);
  const max = Math.max(...data);
  const range = max - min || 1;
  const stepX = width / (data.length - 1);
  const points = data.map((v, i) => `${i * stepX},${height - ((v - min) / range) * (height - 4) - 2}`);
  const path = `M ${points.join(' L ')}`;
  const area = `${path} L ${width},${height} L 0,${height} Z`;
  const id = useMemo(() => `spark-${Math.random().toString(36).slice(2, 8)}`, []);
  return (
    <svg width={width} height={height} viewBox={`0 0 ${width} ${height}`} style={{ display: 'block' }}>
      <defs>
        <linearGradient id={id} x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.35" />
          <stop offset="100%" stopColor={color} stopOpacity="0" />
        </linearGradient>
      </defs>
      {fill && <path d={area} fill={`url(#${id})`} />}
      <path d={path} fill="none" stroke={color} strokeWidth="1.5" strokeLinejoin="round" strokeLinecap="round" />
    </svg>
  );
}

/* ---------- KPI Card ---------- */
function KpiCard({ label, value, delta, deltaPositive = true, sparkData, icon, suffix = '', accent }) {
  return (
    <div className="iv-card iv-card-hover" style={{ padding: 18, position: 'relative', overflow: 'hidden' }}>
      {accent && (
        <div style={{
          position: 'absolute', top: -40, right: -40, width: 140, height: 140,
          background: `radial-gradient(circle, ${accent}33, transparent 70%)`,
          filter: 'blur(20px)', pointerEvents: 'none'
        }} />
      )}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 14, position: 'relative' }}>
        <span style={{ fontSize: 12.5, color: 'var(--iv-text-muted)', letterSpacing: '-0.005em' }}>{label}</span>
        {icon && (
          <div style={{
            width: 28, height: 28, borderRadius: 8, display: 'grid', placeItems: 'center',
            background: 'var(--iv-surface-2)', color: 'var(--iv-text-muted)',
            border: '1px solid var(--iv-border)'
          }}>
            <Icon name={icon} size={15} />
          </div>
        )}
      </div>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 8, position: 'relative' }}>
        <span style={{
          fontSize: 26, fontWeight: 600, letterSpacing: '-0.02em',
          fontFamily: 'var(--iv-font-display)', color: 'var(--iv-text)'
        }}>
          {value}{suffix && <span style={{ fontSize: 16, color: 'var(--iv-text-muted)', fontWeight: 500, marginLeft: 2 }}>{suffix}</span>}
        </span>
      </div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 8, position: 'relative' }}>
        {delta && (
          <span style={{
            display: 'inline-flex', alignItems: 'center', gap: 3,
            fontSize: 12, fontWeight: 500,
            color: deltaPositive ? 'var(--iv-success)' : 'var(--iv-error)'
          }}>
            <Icon name={deltaPositive ? 'arrowUp' : 'arrowDown'} size={11} />
            {delta}
          </span>
        )}
        {sparkData && <Sparkline data={sparkData} width={70} height={22} color={accent || 'var(--iv-primary)'} />}
      </div>
    </div>
  );
}

/* ---------- Area Chart ---------- */
function AreaChart({ data, width = 600, height = 220, color = 'var(--iv-primary)', secondColor, secondData, labels = [], yLabels = true }) {
  const padL = yLabels ? 38 : 12;
  const padR = 12;
  const padT = 16;
  const padB = 28;
  const chartW = width - padL - padR;
  const chartH = height - padT - padB;
  const all = secondData ? [...data, ...secondData] : data;
  const min = Math.min(...all);
  const max = Math.max(...all);
  const range = max - min || 1;
  const id = useMemo(() => `area-${Math.random().toString(36).slice(2, 8)}`, []);
  const id2 = id + '-2';

  const toPath = (arr) => {
    const stepX = chartW / (arr.length - 1);
    const pts = arr.map((v, i) => [padL + i * stepX, padT + chartH - ((v - min) / range) * chartH]);
    let d = `M ${pts[0][0]},${pts[0][1]}`;
    for (let i = 1; i < pts.length; i++) {
      const [x0, y0] = pts[i - 1];
      const [x1, y1] = pts[i];
      const cx = (x0 + x1) / 2;
      d += ` C ${cx},${y0} ${cx},${y1} ${x1},${y1}`;
    }
    return { d, last: pts[pts.length - 1] };
  };

  const { d } = toPath(data);
  const area = `${d} L ${padL + chartW},${padT + chartH} L ${padL},${padT + chartH} Z`;
  const second = secondData ? toPath(secondData) : null;

  // Y axis ticks
  const ticks = 4;
  const tickVals = Array.from({ length: ticks + 1 }, (_, i) => min + (range * i) / ticks);

  return (
    <svg width="100%" viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none" style={{ display: 'block' }}>
      <defs>
        <linearGradient id={id} x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.4" />
          <stop offset="100%" stopColor={color} stopOpacity="0" />
        </linearGradient>
        {secondColor && (
          <linearGradient id={id2} x1="0" x2="0" y1="0" y2="1">
            <stop offset="0%" stopColor={secondColor} stopOpacity="0.3" />
            <stop offset="100%" stopColor={secondColor} stopOpacity="0" />
          </linearGradient>
        )}
      </defs>
      {/* grid */}
      {tickVals.map((tv, i) => {
        const y = padT + chartH - ((tv - min) / range) * chartH;
        return (
          <g key={i}>
            <line x1={padL} x2={padL + chartW} y1={y} y2={y} stroke="var(--iv-border-soft)" strokeWidth="1" />
            {yLabels && (
              <text x={padL - 8} y={y + 3.5} textAnchor="end" fontSize="10" fontFamily="var(--iv-font-mono)" fill="var(--iv-text-faint)">
                {Math.round(tv).toLocaleString()}
              </text>
            )}
          </g>
        );
      })}
      {/* x labels */}
      {labels.map((lbl, i) => {
        const stepX = chartW / (data.length - 1);
        const x = padL + i * stepX;
        if (i % Math.ceil(labels.length / 8) !== 0 && i !== labels.length - 1) return null;
        return (
          <text key={i} x={x} y={height - 8} textAnchor="middle" fontSize="10" fontFamily="var(--iv-font-mono)" fill="var(--iv-text-faint)">
            {lbl}
          </text>
        );
      })}
      {/* area + line */}
      <path d={area} fill={`url(#${id})`} />
      <path d={d} fill="none" stroke={color} strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" />
      {second && secondColor && (
        <>
          <path d={`${second.d} L ${padL + chartW},${padT + chartH} L ${padL},${padT + chartH} Z`} fill={`url(#${id2})`} />
          <path d={second.d} fill="none" stroke={secondColor} strokeWidth="2" strokeDasharray="4 4" strokeLinejoin="round" />
        </>
      )}
    </svg>
  );
}

/* ---------- Bar Chart ---------- */
function BarChart({ data, width = 600, height = 200, color = 'var(--iv-primary)', labels = [] }) {
  const padL = 28, padR = 12, padT = 12, padB = 24;
  const chartW = width - padL - padR;
  const chartH = height - padT - padB;
  const max = Math.max(...data) || 1;
  const barW = (chartW / data.length) * 0.6;
  const gap = (chartW / data.length) * 0.4;
  const id = useMemo(() => `bar-${Math.random().toString(36).slice(2, 8)}`, []);
  return (
    <svg width="100%" viewBox={`0 0 ${width} ${height}`} preserveAspectRatio="none" style={{ display: 'block' }}>
      <defs>
        <linearGradient id={id} x1="0" x2="0" y1="0" y2="1">
          <stop offset="0%" stopColor={color} stopOpacity="0.95" />
          <stop offset="100%" stopColor={color} stopOpacity="0.4" />
        </linearGradient>
      </defs>
      {[0.25, 0.5, 0.75, 1].map((p, i) => (
        <line key={i} x1={padL} x2={padL + chartW} y1={padT + chartH * (1 - p)} y2={padT + chartH * (1 - p)} stroke="var(--iv-border-soft)" />
      ))}
      {data.map((v, i) => {
        const h = (v / max) * chartH;
        const x = padL + i * (chartW / data.length) + gap / 2;
        const y = padT + chartH - h;
        return (
          <g key={i}>
            <rect x={x} y={y} width={barW} height={h} rx="3" fill={`url(#${id})`} />
            {labels[i] && (
              <text x={x + barW / 2} y={height - 8} textAnchor="middle" fontSize="10" fontFamily="var(--iv-font-mono)" fill="var(--iv-text-faint)">
                {labels[i]}
              </text>
            )}
          </g>
        );
      })}
    </svg>
  );
}

/* ---------- Donut ---------- */
function Donut({ value = 75, size = 80, stroke = 8, color = 'var(--iv-primary)', label }) {
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  const off = c - (value / 100) * c;
  return (
    <div style={{ position: 'relative', width: size, height: size, display: 'inline-block' }}>
      <svg width={size} height={size}>
        <circle cx={size / 2} cy={size / 2} r={r} stroke="var(--iv-surface-3)" strokeWidth={stroke} fill="none" />
        <circle
          cx={size / 2} cy={size / 2} r={r} stroke={color} strokeWidth={stroke} fill="none"
          strokeDasharray={c} strokeDashoffset={off} strokeLinecap="round"
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
          style={{ transition: 'stroke-dashoffset 0.6s ease' }}
        />
      </svg>
      <div style={{
        position: 'absolute', inset: 0, display: 'grid', placeItems: 'center',
        fontSize: size / 4.5, fontWeight: 600, fontFamily: 'var(--iv-font-display)',
        color: 'var(--iv-text)', letterSpacing: '-0.02em'
      }}>
        {label ?? `${value}%`}
      </div>
    </div>
  );
}

/* ---------- Status Badge ---------- */
function StatusBadge({ status, label, dot = true, pulse = false }) {
  const map = {
    success: 'iv-badge-success', authorized: 'iv-badge-success', sent: 'iv-badge-success', active: 'iv-badge-success', online: 'iv-badge-success',
    warning: 'iv-badge-warning', pending: 'iv-badge-warning', retrying: 'iv-badge-warning',
    error: 'iv-badge-error', failed: 'iv-badge-error', rejected: 'iv-badge-error', offline: 'iv-badge-error',
    info: 'iv-badge-info',
    neutral: 'iv-badge-neutral', inactive: 'iv-badge-neutral',
    primary: 'iv-badge-primary',
  };
  const cls = map[status] || 'iv-badge-neutral';
  return (
    <span className={`iv-badge ${cls} ${dot ? 'iv-badge-dot' : ''} ${pulse ? 'iv-badge-pulse' : ''}`}>
      {label}
    </span>
  );
}

/* ---------- Empty State ---------- */
function EmptyState({ icon = 'database', title, description, action }) {
  return (
    <div style={{
      padding: '60px 24px', textAlign: 'center',
      border: '1px dashed var(--iv-border-strong)', borderRadius: 'var(--iv-r-lg)',
      background: 'var(--iv-surface)'
    }}>
      <div style={{
        width: 56, height: 56, margin: '0 auto 16px', borderRadius: 14,
        background: 'var(--iv-surface-2)', border: '1px solid var(--iv-border)',
        display: 'grid', placeItems: 'center', color: 'var(--iv-text-faint)'
      }}>
        <Icon name={icon} size={26} />
      </div>
      <h4 style={{ margin: '0 0 6px', fontSize: 16, fontWeight: 600, letterSpacing: '-0.01em' }}>{title}</h4>
      <p style={{ margin: '0 0 16px', color: 'var(--iv-text-muted)', fontSize: 13.5 }}>{description}</p>
      {action}
    </div>
  );
}

/* ---------- Toast (simple) ---------- */
const ToastCtx = React.createContext(null);
function ToastProvider({ children }) {
  const [items, setItems] = useState([]);
  const push = (toast) => {
    const id = Date.now() + Math.random();
    setItems(p => [...p, { id, ...toast }]);
    setTimeout(() => setItems(p => p.filter(t => t.id !== id)), toast.duration || 3500);
  };
  return (
    <ToastCtx.Provider value={push}>
      {children}
      <div style={{
        position: 'fixed', bottom: 24, right: 24, zIndex: 9999,
        display: 'flex', flexDirection: 'column', gap: 8, maxWidth: 360
      }}>
        {items.map(t => (
          <div key={t.id} style={{
            background: 'var(--iv-surface-2)', border: '1px solid var(--iv-border-strong)',
            borderRadius: 'var(--iv-r-md)', padding: '10px 14px',
            color: 'var(--iv-text)', fontSize: 13,
            boxShadow: 'var(--iv-shadow-lg)',
            display: 'flex', alignItems: 'flex-start', gap: 10,
            animation: 'iv-fade-up 0.3s ease both',
            borderLeft: `3px solid var(--iv-${t.type || 'primary'})`
          }}>
            {t.icon && <Icon name={t.icon} size={16} color={`var(--iv-${t.type || 'primary'})`} />}
            <div>
              {t.title && <div style={{ fontWeight: 600, marginBottom: 2 }}>{t.title}</div>}
              <div style={{ color: 'var(--iv-text-muted)' }}>{t.message}</div>
            </div>
          </div>
        ))}
      </div>
    </ToastCtx.Provider>
  );
}
const useToast = () => React.useContext(ToastCtx);

/* ---------- Modal ---------- */
function Modal({ open, onClose, title, children, footer, width = 480 }) {
  if (!open) return null;
  return (
    <div
      onClick={onClose}
      style={{
        position: 'fixed', inset: 0, background: 'rgba(0, 0, 0, 0.6)',
        backdropFilter: 'blur(8px)', WebkitBackdropFilter: 'blur(8px)',
        zIndex: 9000, display: 'grid', placeItems: 'center',
        animation: 'iv-fade-in 0.2s ease both', padding: 24
      }}
    >
      <div
        onClick={(e) => e.stopPropagation()}
        style={{
          width: '100%', maxWidth: width, background: 'var(--iv-bg-elevated)',
          border: '1px solid var(--iv-border-strong)', borderRadius: 'var(--iv-r-xl)',
          boxShadow: 'var(--iv-shadow-lg)', overflow: 'hidden',
          animation: 'iv-fade-up 0.3s cubic-bezier(0.16, 1, 0.3, 1) both'
        }}
      >
        <div style={{
          padding: '16px 20px', borderBottom: '1px solid var(--iv-divider)',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between'
        }}>
          <h3 style={{ margin: 0, fontSize: 15.5, fontWeight: 600, letterSpacing: '-0.01em' }}>{title}</h3>
          <button onClick={onClose} className="iv-btn iv-btn-ghost iv-btn-sm" style={{ padding: 6, height: 28 }}>
            <Icon name="close" size={14} />
          </button>
        </div>
        <div style={{ padding: 20 }}>{children}</div>
        {footer && (
          <div style={{
            padding: '14px 20px', borderTop: '1px solid var(--iv-divider)',
            display: 'flex', justifyContent: 'flex-end', gap: 8,
            background: 'var(--iv-surface)'
          }}>
            {footer}
          </div>
        )}
      </div>
    </div>
  );
}

/* ---------- Avatar ---------- */
function Avatar({ name = '', size = 28, color }) {
  const initials = name.split(' ').filter(Boolean).slice(0, 2).map(s => s[0]).join('').toUpperCase();
  const palette = ['#4F46E5', '#8B5CF6', '#22C55E', '#F59E0B', '#38BDF8', '#EF4444', '#EC4899'];
  const c = color || palette[name.charCodeAt(0) % palette.length];
  return (
    <div style={{
      width: size, height: size, borderRadius: '50%',
      background: `linear-gradient(135deg, ${c}, ${c}aa)`,
      display: 'grid', placeItems: 'center',
      fontSize: size * 0.42, fontWeight: 600, color: '#fff',
      letterSpacing: '-0.02em', flexShrink: 0,
      border: '1px solid rgba(255,255,255,0.1)'
    }}>
      {initials || '?'}
    </div>
  );
}

/* ---------- Placeholder image ---------- */
function PlaceholderImg({ label = 'image', height = 200, accent = 'var(--iv-primary)' }) {
  return (
    <div style={{
      height, borderRadius: 'var(--iv-r-md)',
      background: `repeating-linear-gradient(135deg, var(--iv-surface-2) 0 8px, var(--iv-surface) 8px 16px)`,
      border: '1px solid var(--iv-border)', display: 'grid', placeItems: 'center',
      fontFamily: 'var(--iv-font-mono)', fontSize: 11,
      color: 'var(--iv-text-faint)', letterSpacing: '0.05em',
      position: 'relative', overflow: 'hidden'
    }}>
      <span>[ {label} ]</span>
    </div>
  );
}

/* ---------- Reveal: animate child in when scrolled into view ---------- */
function Reveal({ children, delay = 0, as: Tag = 'div', style = {}, className = '', threshold = 0.12 }) {
  const ref = useRef(null);
  const [shown, setShown] = useState(false);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (typeof IntersectionObserver === 'undefined') { setShown(true); return; }
    const obs = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting) { setShown(true); obs.unobserve(el); }
    }, { threshold, rootMargin: '0px 0px -8% 0px' });
    obs.observe(el);
    return () => obs.disconnect();
  }, [threshold]);
  return (
    <Tag
      ref={ref}
      className={`iv-reveal ${shown ? 'iv-in' : ''} ${className}`}
      style={{ transitionDelay: shown ? `${delay}ms` : '0ms', ...style }}
    >
      {children}
    </Tag>
  );
}

/* ---------- CountUp: animated number ---------- */
function CountUp({ to = 100, duration = 1500, prefix = '', suffix = '', decimals = 0 }) {
  const [val, setVal] = useState(0);
  const ref = useRef(null);
  const started = useRef(false);
  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const obs = new IntersectionObserver(([entry]) => {
      if (entry.isIntersecting && !started.current) {
        started.current = true;
        const t0 = performance.now();
        const tick = (t) => {
          const p = Math.min(1, (t - t0) / duration);
          const eased = 1 - Math.pow(1 - p, 3);
          setVal(eased * to);
          if (p < 1) requestAnimationFrame(tick);
        };
        requestAnimationFrame(tick);
      }
    }, { threshold: 0.4 });
    obs.observe(el);
    return () => obs.disconnect();
  }, [to, duration]);
  return <span ref={ref}>{prefix}{val.toFixed(decimals)}{suffix}</span>;
}

Object.assign(window, {
  Typewriter, Sparkline, KpiCard, AreaChart, BarChart, Donut,
  StatusBadge, EmptyState, ToastProvider, useToast, ToastCtx,
  Modal, Avatar, PlaceholderImg, Reveal, CountUp
});
