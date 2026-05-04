/* landing.jsx — Public marketing site (v2 premium responsive) */

const { useState, useEffect, useRef } = React;

function LandingHeader({ goto }) {
  const { t, lang, setLang } = useLang();
  const [scrolled, setScrolled] = useState(false);
  const [drawerOpen, setDrawerOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 12);
    window.addEventListener('scroll', onScroll);
    return () => window.removeEventListener('scroll', onScroll);
  }, []);

  const navItems = [
    { k: 'nav.home', target: 'top' },
    { k: 'nav.features', target: 'features' },
    { k: 'nav.vision', target: 'vision' },
    { k: 'nav.invoicing', target: 'invoicing' },
    { k: 'nav.pricing', target: 'pricing' },
    { k: 'nav.contact', target: 'contact' },
  ];

  const scrollTo = (id) => {
    const el = document.getElementById(id);
    if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  return (
    <>
      <header className="iv-header-glass" style={{
        background: scrolled ? undefined : 'transparent',
        borderBottomColor: scrolled ? undefined : 'transparent',
        backdropFilter: scrolled ? undefined : 'blur(0px)',
        WebkitBackdropFilter: scrolled ? undefined : 'blur(0px)',
      }}>
        <div className="iv-container iv-header-inner" style={{ height: 64, display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <a href="#top" style={{ display: 'flex', alignItems: 'center', textDecoration: 'none', gap: 10 }}>
            <Logo size={32} />
          </a>

          <nav style={{ display: 'flex', alignItems: 'center', gap: 4 }} className="iv-landing-nav">
            {navItems.map(n => (
              <a key={n.k} href="#" onClick={(e) => { e.preventDefault(); scrollTo(n.target); }} style={{
                padding: '8px 12px', fontSize: 13.5, color: 'var(--iv-text-muted)',
                textDecoration: 'none', borderRadius: 6, transition: 'color 0.15s'
              }}
              onMouseEnter={e => e.currentTarget.style.color = 'var(--iv-text)'}
              onMouseLeave={e => e.currentTarget.style.color = 'var(--iv-text-muted)'}>
                {t(n.k)}
              </a>
            ))}
          </nav>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <button
              onClick={() => setLang(lang === 'es' ? 'en' : 'es')}
              className="iv-btn iv-btn-ghost iv-btn-sm"
              style={{ fontFamily: 'var(--iv-font-mono)', fontSize: 11, letterSpacing: '0.1em' }}
            >
              {lang === 'es' ? 'EN' : 'ES'}
            </button>
            <button onClick={() => goto('login')} className="iv-btn iv-btn-secondary iv-btn-sm iv-landing-nav">
              {t('nav.login')}
            </button>
            <button
              onClick={() => { document.getElementById('contact')?.scrollIntoView({ behavior: 'smooth', block: 'start' }); }}
              className="iv-btn iv-btn-primary iv-btn-sm"
            >
              {t('nav.demo')}
            </button>
            <button
              className="iv-btn iv-btn-ghost iv-btn-sm iv-portal-mobile-toggle"
              style={{ padding: '0 6px' }}
              onClick={() => setDrawerOpen(true)}
            >
              <Icon name="menu" size={18} />
            </button>
          </div>
        </div>
      </header>

      {/* Mobile drawer */}
      <div className={`iv-mobile-drawer ${drawerOpen ? 'iv-open' : ''}`} onClick={() => setDrawerOpen(false)}>
        <div className="iv-mobile-drawer-panel" onClick={e => e.stopPropagation()}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
            <Logo size={28} />
            <button className="iv-btn iv-btn-ghost iv-btn-sm" onClick={() => setDrawerOpen(false)}>
              <Icon name="close" size={18} />
            </button>
          </div>
          {navItems.map(n => (
            <a key={n.k} href="#" onClick={(e) => { e.preventDefault(); setDrawerOpen(false); scrollTo(n.target); }} style={{
              padding: '12px 10px', fontSize: 15, color: 'var(--iv-text)',
              textDecoration: 'none', borderRadius: 8, display: 'block',
              borderBottom: '1px solid var(--iv-divider)'
            }}>
              {t(n.k)}
            </a>
          ))}
          <div style={{ marginTop: 'auto', display: 'flex', flexDirection: 'column', gap: 8, paddingTop: 16 }}>
            <button onClick={() => { setDrawerOpen(false); goto('login'); }} className="iv-btn iv-btn-secondary" style={{ width: '100%' }}>
              {t('nav.login')}
            </button>
            <button onClick={() => { setDrawerOpen(false); document.getElementById('contact')?.scrollIntoView({ behavior: 'smooth' }); }} className="iv-btn iv-btn-primary" style={{ width: '100%' }}>
              {t('nav.demo')}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}

function Hero() {
  const { t, lang } = useLang();
  const words = lang === 'es'
    ? ['Inventario.', 'Ventas.', 'Vision AI.', 'Facturación SRI.', 'Reportes.', 'Sucursales.']
    : ['Inventory.', 'Sales.', 'Vision AI.', 'Invoicing.', 'Reports.', 'Locations.'];

  return (
    <section id="top" className="iv-section" style={{ position: 'relative', paddingTop: 140, paddingBottom: 80, overflow: 'hidden' }}>
      <div className="iv-container" style={{ position: 'relative', textAlign: 'center' }}>
        <Reveal delay={50}>
          <span className="iv-eyebrow">{t('hero.eyebrow')}</span>
        </Reveal>

        <Reveal delay={120}>
          <h1 className="iv-hero-title" style={{
            margin: '24px auto 0', maxWidth: 920,
            fontSize: 'clamp(36px, 5.6vw, 76px)', fontWeight: 600,
            letterSpacing: '-0.035em', lineHeight: 1.04,
            fontFamily: 'var(--iv-font-display)',
            textWrap: 'balance'
          }}>
            {t('hero.title.pre')}{' '}
            <span style={{
              background: 'linear-gradient(120deg, #A78BFA 0%, #8B5CF6 50%, #4F46E5 100%)',
              WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent',
              backgroundClip: 'text', display: 'inline-block', minWidth: '6ch', textAlign: 'left'
            }}>
              <Typewriter words={words} typeSpeed={60} deleteSpeed={28} pauseAfter={1200} />
            </span>
          </h1>
        </Reveal>

        <Reveal delay={200}>
          <p style={{
            margin: '24px auto 0', maxWidth: 640,
            fontSize: 'clamp(15px, 1.4vw, 18px)', color: 'var(--iv-text-muted)',
            lineHeight: 1.55, textWrap: 'pretty'
          }}>
            {t('hero.subtitle')}
          </p>
        </Reveal>

        <Reveal delay={280}>
          <div style={{
            display: 'flex', gap: 12, justifyContent: 'center', marginTop: 32, flexWrap: 'wrap'
          }}>
            <button
              onClick={() => document.getElementById('contact')?.scrollIntoView({ behavior: 'smooth' })}
              className="iv-btn iv-btn-primary iv-btn-lg"
            >
              {t('hero.cta.primary')} <Icon name="arrowRight" size={14} />
            </button>
            <button
              onClick={() => document.getElementById('features')?.scrollIntoView({ behavior: 'smooth' })}
              className="iv-btn iv-btn-secondary iv-btn-lg"
            >
              {t('hero.cta.secondary')}
            </button>
          </div>
        </Reveal>

        <Reveal delay={360}>
          <p className="iv-mono" style={{
            marginTop: 24, fontSize: 11.5, color: 'var(--iv-text-faint)', letterSpacing: '0.05em'
          }}>
            {t('hero.trust')}
          </p>
        </Reveal>

        <Reveal delay={500}>
          <div className="iv-mockup-wrap iv-float">
            <DashboardMockup />
          </div>
        </Reveal>
      </div>
    </section>
  );
}

/* Dashboard mockup used in hero */
function DashboardMockup() {
  const sales = [320, 410, 290, 480, 540, 620, 580, 720, 690, 810, 780, 900, 950];
  const events = [12, 28, 45, 62, 38, 55, 70, 85, 60, 92, 78, 105, 120];
  return (
    <div style={{
      borderRadius: 16, overflow: 'hidden',
      border: '1px solid var(--iv-border-strong)',
      background: 'var(--iv-bg-elevated)',
      boxShadow: '0 50px 120px -30px rgba(79, 70, 229, 0.45), 0 30px 80px -20px rgba(0, 0, 0, 0.6)',
      transform: 'perspective(1800px) rotateX(2deg)',
      transformStyle: 'preserve-3d'
    }}>
      {/* fake browser bar */}
      <div style={{
        height: 36, background: 'var(--iv-surface)', borderBottom: '1px solid var(--iv-border)',
        display: 'flex', alignItems: 'center', padding: '0 14px', gap: 6
      }}>
        <span style={{ width: 10, height: 10, borderRadius: '50%', background: '#FF5F57' }} />
        <span style={{ width: 10, height: 10, borderRadius: '50%', background: '#FEBC2E' }} />
        <span style={{ width: 10, height: 10, borderRadius: '50%', background: '#28C840' }} />
        <div style={{
          marginLeft: 16, padding: '4px 12px', borderRadius: 6,
          background: 'var(--iv-surface-2)', fontFamily: 'var(--iv-font-mono)', fontSize: 11,
          color: 'var(--iv-text-faint)', flex: 1, maxWidth: 360, textAlign: 'center'
        }}>
          app.insightvisionia.cloud/dashboard
        </div>
      </div>

      <div style={{ display: 'flex', minHeight: 460 }}>
        {/* mini sidebar */}
        <div style={{
          width: 56, background: 'var(--iv-surface)', borderRight: '1px solid var(--iv-border)',
          display: 'flex', flexDirection: 'column', alignItems: 'center', padding: '14px 0', gap: 6,
          flexShrink: 0
        }}>
          <Logo variant="mark" size={24} />
          <div style={{ height: 14 }} />
          {['dashboard', 'inventory', 'sales', 'invoice', 'vision', 'reports', 'settings'].map((ic, i) => (
            <div key={ic} style={{
              width: 32, height: 32, borderRadius: 8, display: 'grid', placeItems: 'center',
              background: i === 0 ? 'rgba(79, 70, 229, 0.15)' : 'transparent',
              color: i === 0 ? 'var(--iv-accent-soft)' : 'var(--iv-text-faint)'
            }}>
              <Icon name={ic} size={16} />
            </div>
          ))}
        </div>
        {/* content */}
        <div style={{ flex: 1, padding: 20, background: 'var(--iv-bg)', overflow: 'hidden' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16, flexWrap: 'wrap', gap: 8 }}>
            <div>
              <div style={{ fontSize: 14, fontWeight: 600, letterSpacing: '-0.01em' }}>Resumen</div>
              <div className="iv-mono" style={{ fontSize: 10, color: 'var(--iv-text-faint)', marginTop: 2 }}>JUE · 02 MAY · 14:32</div>
            </div>
            <span className="iv-badge iv-badge-success iv-badge-dot iv-badge-pulse">Live</span>
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(120px, 1fr))', gap: 10, marginBottom: 14 }}>
            {[
              { label: 'Ventas', value: '$8.2k', delta: '+12%', spark: [3, 5, 4, 6, 7, 8, 9] },
              { label: 'Facturas', value: '142', delta: '+8%', spark: [4, 6, 5, 7, 6, 8, 9] },
              { label: 'Stock bajo', value: '7', delta: '-2', deltaPos: false, spark: [9, 8, 7, 6, 5, 6, 7] },
              { label: 'Eventos AI', value: '328', delta: '+24%', spark: [2, 3, 5, 6, 4, 7, 9] },
            ].map((k, i) => (
              <div key={i} style={{
                padding: 10, background: 'var(--iv-card)', border: '1px solid var(--iv-border)',
                borderRadius: 8
              }}>
                <div style={{ fontSize: 9.5, color: 'var(--iv-text-muted)', marginBottom: 4 }}>{k.label}</div>
                <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
                  <span style={{ fontSize: 16, fontWeight: 600, letterSpacing: '-0.02em' }}>{k.value}</span>
                  <span style={{ fontSize: 9, color: k.deltaPos === false ? 'var(--iv-error)' : 'var(--iv-success)' }}>
                    {k.delta}
                  </span>
                </div>
                <div style={{ marginTop: 6 }}>
                  <Sparkline data={k.spark} width={80} height={18} color={i === 3 ? 'var(--iv-accent)' : 'var(--iv-primary)'} />
                </div>
              </div>
            ))}
          </div>
          <div style={{ display: 'grid', gridTemplateColumns: '1.6fr 1fr', gap: 10 }} className="iv-dash-row">
            <div style={{ padding: 12, background: 'var(--iv-card)', border: '1px solid var(--iv-border)', borderRadius: 8, minWidth: 0 }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8, flexWrap: 'wrap' }}>
                <div style={{ fontSize: 11.5, fontWeight: 600 }}>Ventas · 14d</div>
                <div className="iv-mono" style={{ fontSize: 9, color: 'var(--iv-text-faint)' }}>$ USD</div>
              </div>
              <AreaChart data={sales} secondData={events.map(e => e * 6)} secondColor="var(--iv-accent)" width={520} height={140} />
            </div>
            <div style={{ padding: 12, background: 'var(--iv-card)', border: '1px solid var(--iv-border)', borderRadius: 8, minWidth: 0 }}>
              <div style={{ fontSize: 11.5, fontWeight: 600, marginBottom: 8 }}>Vision AI</div>
              <BarChart data={[24, 38, 52, 41, 60, 75, 88]} width={260} height={140} color="var(--iv-accent)" />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

/* ---------- Modules grid ---------- */
function ModulesSection() {
  const { t, lang } = useLang();
  const items = [
    { icon: 'inventory', title: lang === 'es' ? 'Inventario inteligente' : 'Smart inventory', desc: lang === 'es' ? 'Stock en tiempo real, alertas de mínimos, movimientos por sucursal.' : 'Real-time stock, low alerts, per-branch movements.', color: '#4F46E5' },
    { icon: 'sales', title: lang === 'es' ? 'Ventas y órdenes' : 'Sales & orders', desc: lang === 'es' ? 'Pipeline de órdenes, cierres rápidos, comisiones por vendedor.' : 'Order pipeline, fast checkouts, commissions per rep.', color: '#8B5CF6' },
    { icon: 'invoice', title: lang === 'es' ? 'Facturación SRI' : 'SRI invoicing', desc: lang === 'es' ? 'XML, RIDE PDF, autorización automática y reintentos.' : 'XML, RIDE PDF, auto authorization and retries.', color: '#22C55E' },
    { icon: 'vision', title: 'Vision AI', desc: lang === 'es' ? 'Detección con YOLO, eventos por cámara, métricas accionables.' : 'YOLO detection, per-camera events, actionable metrics.', color: '#A78BFA' },
    { icon: 'reports', title: lang === 'es' ? 'Reportes gerenciales' : 'Management reports', desc: lang === 'es' ? 'Dashboards exportables a CSV/PDF con filtros granulares.' : 'Exportable dashboards (CSV/PDF) with granular filters.', color: '#38BDF8' },
    { icon: 'building', title: lang === 'es' ? 'Multi-sucursal' : 'Multi-branch', desc: lang === 'es' ? 'Bodegas, sucursales y tenants con permisos finos.' : 'Warehouses, branches and tenants with fine-grained roles.', color: '#F59E0B' },
    { icon: 'shield', title: lang === 'es' ? 'Usuarios y permisos' : 'Users & permissions', desc: lang === 'es' ? 'Roles configurables, auditoría completa y SSO opcional.' : 'Configurable roles, full audit trail and optional SSO.', color: '#EC4899' },
    { icon: 'mail', title: lang === 'es' ? 'Correos con Resend' : 'Emails via Resend', desc: lang === 'es' ? 'Envíos transaccionales con logs y reintentos automáticos.' : 'Transactional sends with logs and automatic retries.', color: '#EF4444' },
  ];

  return (
    <section id="features" className="iv-section">
      <div className="iv-container">
        <div style={{ textAlign: 'center', maxWidth: 720, margin: '0 auto 56px' }}>
          <Reveal>
            <span className="iv-eyebrow">{t('modules.eyebrow')}</span>
          </Reveal>
          <Reveal delay={100}>
            <h2 style={{
              fontSize: 'clamp(28px, 4vw, 48px)', fontWeight: 600, letterSpacing: '-0.025em',
              margin: '20px 0 14px', lineHeight: 1.1, textWrap: 'balance'
            }}>
              {t('modules.title')}
            </h2>
          </Reveal>
          <Reveal delay={180}>
            <p style={{ color: 'var(--iv-text-muted)', fontSize: 16, lineHeight: 1.55, textWrap: 'pretty' }}>
              {t('modules.subtitle')}
            </p>
          </Reveal>
        </div>

        <div className="iv-stagger" style={{
          display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: 16
        }}>
          {items.map((m, i) => (
            <div key={i} className="iv-card iv-card-hover" style={{
              padding: 22, position: 'relative', overflow: 'hidden'
            }}>
              <div style={{
                width: 38, height: 38, borderRadius: 10,
                background: `linear-gradient(135deg, ${m.color}25, ${m.color}10)`,
                border: `1px solid ${m.color}40`,
                display: 'grid', placeItems: 'center', color: m.color, marginBottom: 14
              }}>
                <Icon name={m.icon} size={18} />
              </div>
              <h3 style={{ margin: '0 0 6px', fontSize: 15.5, fontWeight: 600, letterSpacing: '-0.01em' }}>{m.title}</h3>
              <p style={{ margin: 0, fontSize: 13.5, color: 'var(--iv-text-muted)', lineHeight: 1.5, textWrap: 'pretty' }}>{m.desc}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ---------- Vision AI section ---------- */
function VisionSection() {
  const { t, lang } = useLang();
  return (
    <section id="vision" className="iv-section" style={{ position: 'relative', overflow: 'hidden' }}>
      <div className="iv-container" style={{ position: 'relative' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 64, alignItems: 'center' }} className="iv-vision-grid">
          <div>
            <Reveal>
              <span className="iv-eyebrow">{t('vision.eyebrow')}</span>
            </Reveal>
            <Reveal delay={100}>
              <h2 style={{
                fontSize: 'clamp(26px, 3.6vw, 44px)', fontWeight: 600, letterSpacing: '-0.025em',
                margin: '20px 0 14px', lineHeight: 1.1, textWrap: 'balance'
              }}>
                {t('vision.title')}
              </h2>
            </Reveal>
            <Reveal delay={180}>
              <p style={{ color: 'var(--iv-text-muted)', fontSize: 16, lineHeight: 1.6, textWrap: 'pretty', marginBottom: 28 }}>
                {t('vision.subtitle')}
              </p>
            </Reveal>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
              {[
                { icon: 'camera', t: lang === 'es' ? 'Cámaras IP conectadas' : 'IP cameras connected', d: lang === 'es' ? 'RTSP, ONVIF y WebRTC. Sin hardware extra.' : 'RTSP, ONVIF and WebRTC. No extra hardware.' },
                { icon: 'cpu', t: lang === 'es' ? 'YOLO en tiempo real' : 'Real-time YOLO', d: lang === 'es' ? 'Detección on-prem o cloud, ajustable por cámara.' : 'On-prem or cloud detection, tunable per camera.' },
                { icon: 'zap', t: lang === 'es' ? 'Eventos accionables' : 'Actionable events', d: lang === 'es' ? 'Webhooks y notificaciones cuando algo importa.' : 'Webhooks and alerts when it actually matters.' },
              ].map((f, i) => (
                <Reveal key={i} delay={260 + i * 80}>
                  <div style={{ display: 'flex', gap: 12 }}>
                    <div style={{
                      width: 36, height: 36, borderRadius: 10, flexShrink: 0,
                      background: 'rgba(139, 92, 246, 0.12)', border: '1px solid rgba(139, 92, 246, 0.3)',
                      color: 'var(--iv-accent-soft)', display: 'grid', placeItems: 'center'
                    }}>
                      <Icon name={f.icon} size={16} />
                    </div>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 2 }}>{f.t}</div>
                      <div style={{ fontSize: 13, color: 'var(--iv-text-muted)' }}>{f.d}</div>
                    </div>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>
          <Reveal delay={200}>
            <div>
              <VisionLiveFeed />
            </div>
          </Reveal>
        </div>
      </div>
    </section>
  );
}

/* Live feed simulator with bounding boxes */
function VisionLiveFeed() {
  const [tick, setTick] = useState(0);
  const [boxes, setBoxes] = useState([]);
  useEffect(() => {
    const id = setInterval(() => setTick(t => t + 1), 1500);
    return () => clearInterval(id);
  }, []);
  useEffect(() => {
    const labels = ['person', 'cart', 'bottle', 'box', 'person'];
    const n = 2 + Math.floor(Math.random() * 3);
    const newBoxes = Array.from({ length: n }, (_, i) => ({
      id: tick * 10 + i,
      x: 5 + Math.random() * 70,
      y: 10 + Math.random() * 60,
      w: 14 + Math.random() * 18,
      h: 22 + Math.random() * 22,
      label: labels[Math.floor(Math.random() * labels.length)],
      conf: (0.78 + Math.random() * 0.21).toFixed(2),
    }));
    setBoxes(newBoxes);
  }, [tick]);

  return (
    <div style={{
      borderRadius: 14, overflow: 'hidden', border: '1px solid var(--iv-border-strong)',
      background: 'var(--iv-bg-elevated)', boxShadow: 'var(--iv-shadow-lg)'
    }}>
      <div style={{
        padding: '10px 14px', borderBottom: '1px solid var(--iv-border)',
        display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        background: 'var(--iv-surface)', flexWrap: 'wrap', gap: 8
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span className="iv-badge iv-badge-error iv-badge-dot iv-badge-pulse" style={{ fontWeight: 600 }}>LIVE</span>
          <span className="iv-mono" style={{ fontSize: 11, color: 'var(--iv-text-muted)' }}>CAM-04 · Sucursal Centro</span>
        </div>
        <div className="iv-mono" style={{ fontSize: 10.5, color: 'var(--iv-text-faint)' }}>1080p · 30fps · YOLOv8</div>
      </div>
      <div style={{
        position: 'relative', aspectRatio: '16/10',
        background: 'linear-gradient(135deg, #0a0e18, #181f30)',
        backgroundImage: `
          linear-gradient(135deg, #0a0e18, #181f30),
          repeating-linear-gradient(0deg, rgba(255,255,255,0.02) 0px, rgba(255,255,255,0.02) 2px, transparent 2px, transparent 4px)
        `,
        overflow: 'hidden'
      }}>
        {/* simulated scene shapes */}
        <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: '40%', background: 'linear-gradient(180deg, transparent, rgba(79, 70, 229, 0.15))' }} />
        <div style={{ position: 'absolute', top: '15%', left: '10%', width: '80%', height: '50%', border: '1px solid rgba(255,255,255,0.04)', borderRadius: 4 }} />

        {/* Bounding boxes */}
        {boxes.map(b => (
          <div key={b.id} style={{
            position: 'absolute',
            left: `${b.x}%`, top: `${b.y}%`, width: `${b.w}%`, height: `${b.h}%`,
            border: `1.5px solid ${b.label === 'person' ? '#22C55E' : '#A78BFA'}`,
            borderRadius: 2,
            boxShadow: `0 0 0 1px rgba(0,0,0,0.4), 0 0 20px ${b.label === 'person' ? 'rgba(34, 197, 94, 0.4)' : 'rgba(167, 139, 250, 0.4)'}`,
            animation: 'iv-fade-in 0.3s ease both'
          }}>
            <div style={{
              position: 'absolute', top: -22, left: -1.5,
              background: b.label === 'person' ? '#22C55E' : '#A78BFA',
              color: '#0a0e18', fontFamily: 'var(--iv-font-mono)',
              fontSize: 9.5, fontWeight: 700, padding: '2px 6px',
              borderRadius: '2px 2px 0 0', whiteSpace: 'nowrap'
            }}>
              {b.label} {b.conf}
            </div>
          </div>
        ))}

        {/* HUD overlay */}
        <div style={{
          position: 'absolute', top: 12, left: 12,
          fontFamily: 'var(--iv-font-mono)', fontSize: 10, color: 'rgba(255,255,255,0.5)'
        }}>
          REC 00:{String(tick % 60).padStart(2, '0')}
        </div>
        <div style={{
          position: 'absolute', bottom: 12, right: 12,
          fontFamily: 'var(--iv-font-mono)', fontSize: 10, color: 'rgba(255,255,255,0.5)'
        }}>
          {boxes.length} det · {boxes.filter(b => b.label === 'person').length} person
        </div>
      </div>
      {/* Mini metric strip */}
      <div style={{
        display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', borderTop: '1px solid var(--iv-border)',
        background: 'var(--iv-surface)'
      }}>
        {[
          { l: 'Detecciones', v: 1284 },
          { l: 'Confianza', v: '92%' },
          { l: 'Eventos hoy', v: 47 },
        ].map((s, i) => (
          <div key={i} style={{
            padding: '12px 14px',
            borderRight: i < 2 ? '1px solid var(--iv-border)' : 'none'
          }}>
            <div style={{ fontSize: 10.5, color: 'var(--iv-text-faint)', marginBottom: 2, letterSpacing: '0.04em', textTransform: 'uppercase' }}>{s.l}</div>
            <div style={{ fontSize: 18, fontWeight: 600, letterSpacing: '-0.02em' }}>{s.v}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ---------- Invoicing section ---------- */
function InvoicingSection() {
  const { t, lang } = useLang();
  return (
    <section id="invoicing" className="iv-section" style={{ position: 'relative' }}>
      <div className="iv-container">
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 64, alignItems: 'center' }} className="iv-vision-grid">
          <Reveal>
            <div>
              <InvoiceMockup />
            </div>
          </Reveal>
          <div>
            <Reveal>
              <span className="iv-eyebrow">{t('inv.eyebrow')}</span>
            </Reveal>
            <Reveal delay={100}>
              <h2 style={{
                fontSize: 'clamp(26px, 3.6vw, 44px)', fontWeight: 600, letterSpacing: '-0.025em',
                margin: '20px 0 14px', lineHeight: 1.1, textWrap: 'balance'
              }}>
                {t('inv.title')}
              </h2>
            </Reveal>
            <Reveal delay={180}>
              <p style={{ color: 'var(--iv-text-muted)', fontSize: 16, lineHeight: 1.6, textWrap: 'pretty', marginBottom: 28 }}>
                {t('inv.subtitle')}
              </p>
            </Reveal>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
              {[
                { icon: 'check', t: lang === 'es' ? 'Autorización SRI automática' : 'Automatic SRI authorization' },
                { icon: 'mail', t: lang === 'es' ? 'Entrega por correo (Resend)' : 'Email delivery via Resend' },
                { icon: 'refresh', t: lang === 'es' ? 'Reintentos inteligentes en fallos' : 'Smart retries on failure' },
                { icon: 'log', t: lang === 'es' ? 'Historial completo y auditable' : 'Full auditable history' },
              ].map((f, i) => (
                <Reveal key={i} delay={260 + i * 80}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10, fontSize: 14 }}>
                    <div style={{
                      width: 24, height: 24, borderRadius: 6, background: 'rgba(34, 197, 94, 0.12)',
                      color: 'var(--iv-success)', display: 'grid', placeItems: 'center', flexShrink: 0
                    }}>
                      <Icon name={f.icon} size={13} />
                    </div>
                    <span>{f.t}</span>
                  </div>
                </Reveal>
              ))}
            </div>
          </div>
        </div>
      </div>
    </section>
  );
}

function InvoiceMockup() {
  return (
    <div style={{ position: 'relative' }}>
      <div className="iv-halo iv-halo-primary" style={{ width: 380, height: 380, top: '50%', left: '50%', transform: 'translate(-50%, -50%)', opacity: 0.18 }} />
      <div style={{
        position: 'relative',
        background: 'var(--iv-bg-elevated)', border: '1px solid var(--iv-border-strong)',
        borderRadius: 14, padding: 24, boxShadow: 'var(--iv-shadow-lg)', maxWidth: 440, marginInline: 'auto'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 18, flexWrap: 'wrap', gap: 8 }}>
          <div>
            <div className="iv-mono" style={{ fontSize: 10.5, color: 'var(--iv-text-faint)', letterSpacing: '0.05em' }}>FACTURA · 001-001</div>
            <div style={{ fontSize: 18, fontWeight: 600, marginTop: 3, letterSpacing: '-0.01em' }}>0000004287</div>
          </div>
          <StatusBadge status="success" label="Autorizado SRI" pulse />
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14, marginBottom: 18, fontSize: 12 }}>
          <div>
            <div style={{ color: 'var(--iv-text-faint)', fontSize: 10.5, marginBottom: 3 }}>CLIENTE</div>
            <div style={{ fontWeight: 500 }}>Constructora Andina S.A.</div>
            <div style={{ color: 'var(--iv-text-muted)', fontSize: 11.5 }}>RUC 1791234567001</div>
          </div>
          <div>
            <div style={{ color: 'var(--iv-text-faint)', fontSize: 10.5, marginBottom: 3 }}>FECHA</div>
            <div style={{ fontWeight: 500 }}>02 May 2026</div>
            <div className="iv-mono" style={{ color: 'var(--iv-text-muted)', fontSize: 11 }}>14:32 · Cuenca</div>
          </div>
        </div>
        <div style={{ borderTop: '1px solid var(--iv-divider)', paddingTop: 14, marginBottom: 14 }}>
          {[
            { d: 'Cemento Holcim 50kg', q: 240, p: 8.50 },
            { d: 'Varilla 12mm 12m', q: 80, p: 14.20 },
            { d: 'Bloque alivianado 20cm', q: 1200, p: 0.95 },
          ].map((r, i) => (
            <div key={i} style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 0', fontSize: 12.5 }}>
              <span style={{ color: 'var(--iv-text-muted)' }}>{r.q}× {r.d}</span>
              <span className="iv-mono">${(r.q * r.p).toFixed(2)}</span>
            </div>
          ))}
        </div>
        <div style={{ borderTop: '1px solid var(--iv-divider)', paddingTop: 12, display: 'flex', justifyContent: 'space-between', alignItems: 'baseline', marginBottom: 16, flexWrap: 'wrap', gap: 8 }}>
          <span style={{ fontSize: 13, color: 'var(--iv-text-muted)' }}>Total con IVA</span>
          <span className="iv-mono" style={{ fontSize: 22, fontWeight: 600, letterSpacing: '-0.02em' }}>$4,397.40</span>
        </div>
        <div style={{
          padding: 12, background: 'var(--iv-surface-2)', borderRadius: 8,
          border: '1px solid var(--iv-border)', display: 'flex', alignItems: 'center', gap: 10, flexWrap: 'wrap'
        }}>
          <div style={{
            width: 30, height: 30, borderRadius: 8, background: 'rgba(34, 197, 94, 0.12)',
            color: 'var(--iv-success)', display: 'grid', placeItems: 'center', flexShrink: 0
          }}>
            <Icon name="mail" size={14} />
          </div>
          <div style={{ flex: 1, fontSize: 12, minWidth: 0 }}>
            <div style={{ fontWeight: 500 }}>Enviado por Resend</div>
            <div className="iv-mono" style={{ fontSize: 10.5, color: 'var(--iv-text-faint)', wordBreak: 'break-word' }}>contacto@constructoraandina.ec · entregado 14:32:08</div>
          </div>
          <span className="iv-badge iv-badge-primary">Resend</span>
        </div>
      </div>
    </div>
  );
}

/* ---------- Pricing ---------- */
function PricingSection() {
  const { t, lang } = useLang();
  const plans = [
    {
      name: t('price.starter'),
      tagline: lang === 'es' ? 'Para negocios que arrancan' : 'For starting businesses',
      price: t('price.from'),
      featured: false,
      features: lang === 'es'
        ? ['1 sucursal · 5 usuarios', 'Inventario + Ventas', 'Facturación SRI básica', 'Reportes esenciales', 'Soporte por correo']
        : ['1 branch · 5 users', 'Inventory + Sales', 'Basic SRI invoicing', 'Essential reports', 'Email support'],
    },
    {
      name: t('price.business'),
      tagline: lang === 'es' ? 'El más popular para retail y distribuidoras' : 'Most popular for retail & distribution',
      price: t('price.consult'),
      featured: true,
      features: lang === 'es'
        ? ['Sucursales ilimitadas', 'Usuarios ilimitados', 'Vision AI · 4 cámaras', 'Reportes avanzados + exports', 'Soporte prioritario']
        : ['Unlimited branches', 'Unlimited users', 'Vision AI · 4 cameras', 'Advanced reports + exports', 'Priority support'],
    },
    {
      name: t('price.enterprise'),
      tagline: lang === 'es' ? 'Operaciones grandes y multi-marca' : 'Large operations & multi-brand',
      price: t('price.custom'),
      featured: false,
      features: lang === 'es'
        ? ['Multi-tenant', 'Vision AI cámaras ilimitadas', 'SSO + auditoría completa', 'API privada + webhooks', 'SLA · soporte 24/7']
        : ['Multi-tenant', 'Unlimited Vision AI cameras', 'SSO + full audit', 'Private API + webhooks', 'SLA · 24/7 support'],
    },
  ];

  return (
    <section id="pricing" className="iv-section" style={{ position: 'relative' }}>
      <div className="iv-container">
        <div style={{ textAlign: 'center', maxWidth: 720, margin: '0 auto 56px' }}>
          <Reveal>
            <span className="iv-eyebrow">{t('price.eyebrow')}</span>
          </Reveal>
          <Reveal delay={100}>
            <h2 style={{ fontSize: 'clamp(28px, 4vw, 48px)', fontWeight: 600, letterSpacing: '-0.025em', margin: '20px 0 14px', lineHeight: 1.1 }}>
              {t('price.title')}
            </h2>
          </Reveal>
          <Reveal delay={180}>
            <p style={{ color: 'var(--iv-text-muted)', fontSize: 16, textWrap: 'pretty' }}>{t('price.subtitle')}</p>
          </Reveal>
        </div>
        <div className="iv-pricing-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: 16, maxWidth: 1080, margin: '0 auto' }}>
          {plans.map((p, i) => (
            <div key={i} className={`iv-card ${p.featured ? 'iv-pricing-featured' : ''}`} style={{
              padding: 28, borderRadius: 16, position: 'relative', overflow: 'hidden',
              background: p.featured ? 'linear-gradient(180deg, rgba(79, 70, 229, 0.08), var(--iv-card))' : 'var(--iv-card)',
              border: p.featured ? '1px solid rgba(79, 70, 229, 0.4)' : '1px solid var(--iv-border)',
              boxShadow: p.featured ? '0 0 60px -10px rgba(79, 70, 229, 0.4)' : 'none'
            }}>
              {p.featured && (
                <div style={{
                  position: 'absolute', top: 14, right: 14,
                  fontFamily: 'var(--iv-font-mono)', fontSize: 9.5, padding: '3px 8px',
                  background: 'var(--iv-primary)', color: '#fff', borderRadius: 999,
                  letterSpacing: '0.06em', fontWeight: 600
                }}>POPULAR</div>
              )}
              <h3 style={{ margin: '0 0 4px', fontSize: 20, fontWeight: 600, letterSpacing: '-0.015em' }}>{p.name}</h3>
              <p style={{ margin: '0 0 22px', fontSize: 13, color: 'var(--iv-text-muted)' }}>{p.tagline}</p>
              <div style={{ marginBottom: 22, fontSize: 22, fontWeight: 600, letterSpacing: '-0.02em' }}>
                {p.price}
              </div>
              <button
                onClick={() => document.getElementById('contact')?.scrollIntoView({ behavior: 'smooth' })}
                className={`iv-btn ${p.featured ? 'iv-btn-primary' : 'iv-btn-secondary'}`}
                style={{ width: '100%' }}
              >
                {t('price.cta')}
              </button>
              <div style={{ borderTop: '1px solid var(--iv-divider)', margin: '22px 0 16px' }} />
              <ul style={{ listStyle: 'none', padding: 0, margin: 0, display: 'flex', flexDirection: 'column', gap: 10 }}>
                {p.features.map((f, j) => (
                  <li key={j} style={{ display: 'flex', alignItems: 'flex-start', gap: 8, fontSize: 13.5, color: 'var(--iv-text)' }}>
                    <Icon name="check" size={14} color={p.featured ? 'var(--iv-accent-soft)' : 'var(--iv-success)'} style={{ marginTop: 2, flexShrink: 0 }} />
                    <span>{f}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
}

/* ---------- Contact ---------- */
function ContactSection() {
  const { t, lang } = useLang();
  const toast = useToast();
  const [form, setForm] = useState({});
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);

  const update = (k, v) => { setForm(f => ({ ...f, [k]: v })); setErrors(e => ({ ...e, [k]: null })); };

  const submit = (e) => {
    e.preventDefault();
    const required = ['name', 'company', 'email', 'phone', 'city', 'business', 'branches', 'interest'];
    const errs = {};
    required.forEach(k => { if (!form[k]) errs[k] = true; });
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) errs.email = true;
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;
    setSubmitting(true);
    setTimeout(() => {
      setSubmitting(false);
      setForm({});
      const stored = JSON.parse(localStorage.getItem('iv-leads') || '[]');
      stored.push({ ...form, at: new Date().toISOString() });
      localStorage.setItem('iv-leads', JSON.stringify(stored));
      toast({ type: 'success', icon: 'check', title: lang === 'es' ? '¡Solicitud enviada!' : 'Request sent!', message: lang === 'es' ? 'Te contactamos pronto.' : 'We\'ll reach out soon.' });
    }, 900);
  };

  const businessTypes = lang === 'es'
    ? ['Retail / Tienda', 'Distribuidora / Mayorista', 'Restaurante', 'Construcción', 'Servicios', 'Otro']
    : ['Retail', 'Distribution / Wholesale', 'Restaurant', 'Construction', 'Services', 'Other'];
  const interests = lang === 'es'
    ? ['ERP completo', 'Solo Vision AI', 'Solo facturación SRI', 'Inventario multi-sucursal', 'Reportes / BI']
    : ['Full ERP', 'Vision AI only', 'SRI invoicing only', 'Multi-branch inventory', 'Reports / BI'];

  return (
    <section id="contact" className="iv-section" style={{ position: 'relative' }}>
      <div className="iv-container iv-contact-grid" style={{ position: 'relative', display: 'grid', gridTemplateColumns: '1fr 1.4fr', gap: 56 }}>
        <div>
          <Reveal>
            <span className="iv-eyebrow">{t('contact.eyebrow')}</span>
          </Reveal>
          <Reveal delay={100}>
            <h2 style={{ fontSize: 'clamp(26px, 3.6vw, 44px)', fontWeight: 600, letterSpacing: '-0.025em', margin: '20px 0 14px', lineHeight: 1.1 }}>
              {t('contact.title')}
            </h2>
          </Reveal>
          <Reveal delay={180}>
            <p style={{ color: 'var(--iv-text-muted)', fontSize: 15.5, lineHeight: 1.6, textWrap: 'pretty', marginBottom: 32 }}>
              {t('contact.subtitle')}
            </p>
          </Reveal>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
            {[
              { icon: 'mail', l: 'soporte@insightvisionia.cloud' },
              { icon: 'phone', l: '+593 99 123 4567' },
              { icon: 'building', l: 'Cuenca · Ecuador' },
            ].map((c, i) => (
              <Reveal key={i} delay={260 + i * 80}>
                <div style={{ display: 'flex', alignItems: 'center', gap: 10, color: 'var(--iv-text-muted)', fontSize: 13.5 }}>
                  <Icon name={c.icon} size={15} color="var(--iv-accent-soft)" />
                  <span className="iv-mono" style={{ fontSize: 12.5 }}>{c.l}</span>
                </div>
              </Reveal>
            ))}
          </div>
        </div>
        <Reveal delay={200}>
          <form onSubmit={submit} style={{
            background: 'var(--iv-card)', border: '1px solid var(--iv-border)',
            borderRadius: 16, padding: 28
          }}>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
              {[
                { k: 'name', l: 'contact.name', span: 2 },
                { k: 'company', l: 'contact.company', span: 1 },
                { k: 'email', l: 'contact.email', span: 1 },
                { k: 'phone', l: 'contact.phone', span: 1 },
                { k: 'city', l: 'contact.city', span: 1 },
              ].map(f => (
                <div key={f.k} style={{ gridColumn: `span ${f.span}` }}>
                  <label className="iv-label">{t(f.l)}</label>
                  <input
                    className="iv-input" value={form[f.k] || ''}
                    onChange={e => update(f.k, e.target.value)}
                    style={{ borderColor: errors[f.k] ? 'var(--iv-error)' : undefined }}
                  />
                </div>
              ))}
              <div>
                <label className="iv-label">{t('contact.business')}</label>
                <select className="iv-select" value={form.business || ''} onChange={e => update('business', e.target.value)}
                  style={{ borderColor: errors.business ? 'var(--iv-error)' : undefined }}>
                  <option value="">—</option>
                  {businessTypes.map(b => <option key={b}>{b}</option>)}
                </select>
              </div>
              <div>
                <label className="iv-label">{t('contact.branches')}</label>
                <select className="iv-select" value={form.branches || ''} onChange={e => update('branches', e.target.value)}
                  style={{ borderColor: errors.branches ? 'var(--iv-error)' : undefined }}>
                  <option value="">—</option>
                  <option>1</option><option>2-5</option><option>6-15</option><option>15+</option>
                </select>
              </div>
              <div style={{ gridColumn: 'span 2' }}>
                <label className="iv-label">{t('contact.interest')}</label>
                <select className="iv-select" value={form.interest || ''} onChange={e => update('interest', e.target.value)}
                  style={{ borderColor: errors.interest ? 'var(--iv-error)' : undefined }}>
                  <option value="">—</option>
                  {interests.map(i => <option key={i}>{i}</option>)}
                </select>
              </div>
              <div style={{ gridColumn: 'span 2' }}>
                <label className="iv-label">{t('contact.message')}</label>
                <textarea className="iv-textarea" rows="3" value={form.message || ''} onChange={e => update('message', e.target.value)} />
              </div>
            </div>
            <div style={{ marginTop: 18, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 14, flexWrap: 'wrap' }}>
              <p style={{ margin: 0, fontSize: 11.5, color: 'var(--iv-text-faint)', maxWidth: 280 }}>
                {t('contact.disclaimer')}
              </p>
              <button type="submit" disabled={submitting} className="iv-btn iv-btn-primary iv-btn-lg">
                {submitting ? (lang === 'es' ? 'Enviando…' : 'Sending…') : t('contact.cta')} <Icon name="send" size={13} />
              </button>
            </div>
          </form>
        </Reveal>
      </div>
    </section>
  );
}

/* ---------- Footer ---------- */
function Footer() {
  const { lang } = useLang();
  const cols = [
    { t: lang === 'es' ? 'Producto' : 'Product', items: ['Dashboard', 'Vision AI', 'Facturación SRI', 'Inventario', 'Reportes'] },
    { t: lang === 'es' ? 'Soporte' : 'Support', items: [lang === 'es' ? 'Centro de ayuda' : 'Help center', lang === 'es' ? 'Estado del sistema' : 'System status', 'API docs', lang === 'es' ? 'Comunidad' : 'Community'] },
    { t: lang === 'es' ? 'Empresa' : 'Company', items: [lang === 'es' ? 'Acerca de' : 'About', lang === 'es' ? 'Clientes' : 'Customers', 'Blog', lang === 'es' ? 'Contacto' : 'Contact'] },
    { t: 'Legal', items: [lang === 'es' ? 'Términos' : 'Terms', lang === 'es' ? 'Privacidad' : 'Privacy', 'SLA', 'DPA'] },
  ];
  return (
    <footer style={{ borderTop: '1px solid var(--iv-border)', padding: '60px 0 32px', marginTop: 80 }}>
      <div className="iv-container">
        <div style={{ display: 'grid', gridTemplateColumns: '1.5fr repeat(4, 1fr)', gap: 40, marginBottom: 48 }} className="iv-footer-grid">
          <div>
            <Logo size={28} />
            <p style={{ margin: '16px 0 12px', maxWidth: 280, fontSize: 13, color: 'var(--iv-text-muted)', lineHeight: 1.5 }}>
              {lang === 'es'
                ? 'ERP inteligente con visión artificial para negocios que quieren escalar.'
                : 'Intelligent ERP with computer vision for businesses ready to scale.'}
            </p>
            <div className="iv-mono" style={{ fontSize: 11, color: 'var(--iv-text-faint)' }}>
              soporte@insightvisionia.cloud
            </div>
          </div>
          {cols.map((c, i) => (
            <div key={i}>
              <div style={{ fontSize: 12, fontWeight: 600, color: 'var(--iv-text)', marginBottom: 14, letterSpacing: '-0.005em' }}>{c.t}</div>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
                {c.items.map((it, j) => (
                  <a key={j} href="#" style={{ fontSize: 13, color: 'var(--iv-text-muted)', textDecoration: 'none' }}>{it}</a>
                ))}
              </div>
            </div>
          ))}
        </div>
        <div style={{
          paddingTop: 24, borderTop: '1px solid var(--iv-divider)',
          display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 12,
          fontSize: 11.5, color: 'var(--iv-text-faint)'
        }}>
          <span className="iv-mono">© 2026 InsightVision Enterprise AI · Cuenca, Ecuador</span>
          <span className="iv-mono">Built with Geist · Powered by YOLO · Resend</span>
        </div>
      </div>
    </footer>
  );
}

/* ---------- Public site root ---------- */
function LandingPage({ goto }) {
  return (
    <div className="iv-page" style={{ minHeight: '100vh' }}>
      <div className="iv-atmosphere" aria-hidden="true" />
      <LandingHeader goto={goto} />
      <Hero />
      <ModulesSection />
      <VisionSection />
      <InvoicingSection />
      <PricingSection />
      <ContactSection />
      <Footer />
    </div>
  );
}

window.LandingPage = LandingPage;
window.Footer = Footer;
