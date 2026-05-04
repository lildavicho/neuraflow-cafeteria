/* login.jsx — Login + unauthorized screens (v2 responsive) */

const { useState: useStateL } = React;

const ALLOWED_USERS = new Set([
  'admin@insightvisionia.cloud',
  'demo@empresa.com',
  'user@insightvisionia.cloud',
  'gerente@cuencabar.ec',
]);
const PUBLIC_DOMAINS = new Set(['gmail.com','outlook.com','hotmail.com','yahoo.com','live.com','aol.com']);

function LoginPage({ goto }) {
  const { t, lang, setLang } = useLang();
  const [email, setEmail] = useStateL('');
  const [password, setPassword] = useStateL('');
  const [tenant, setTenant] = useStateL('');
  const [showPwd, setShowPwd] = useStateL(false);
  const [err, setErr] = useStateL(null);
  const [loading, setLoading] = useStateL(false);

  const submit = (e) => {
    e.preventDefault();
    setErr(null);
    if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { setErr('email'); return; }
    if (!tenant || tenant.length < 2) { setErr('tenant'); return; }
    if (!password || password.length < 4) { setErr('password'); return; }
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      const domain = email.split('@')[1]?.toLowerCase();
      if (PUBLIC_DOMAINS.has(domain) || !ALLOWED_USERS.has(email.toLowerCase())) {
        goto('unauthorized');
        return;
      }
      localStorage.setItem('iv-user', JSON.stringify({ email, name: email.split('@')[0], tenant }));
      goto('portal');
    }, 800);
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <div style={{ flex: 1, display: 'grid', gridTemplateColumns: '1fr 1.1fr' }} className="iv-login-grid">
        {/* Left: brand panel */}
        <div className="iv-login-brand" style={{
          position: 'relative', overflow: 'hidden', padding: 48,
          background: 'linear-gradient(180deg, #07090E 0%, #0c1020 100%)',
          display: 'flex', flexDirection: 'column', justifyContent: 'space-between'
        }}>
          <div className="iv-grid-bg" style={{ position: 'absolute', inset: 0, opacity: 0.4, maskImage: 'radial-gradient(circle at 30% 50%, black, transparent 70%)', WebkitMaskImage: 'radial-gradient(circle at 30% 50%, black, transparent 70%)' }} />
          <div className="iv-halo iv-halo-primary" style={{ width: 500, height: 500, top: '-20%', left: '-10%', opacity: 0.4 }} />
          <div className="iv-halo iv-halo-accent" style={{ width: 400, height: 400, bottom: '-20%', right: '-10%', opacity: 0.3 }} />

          <a href="#" onClick={(e) => { e.preventDefault(); goto('landing'); }} style={{ position: 'relative', textDecoration: 'none', display: 'inline-flex' }}>
            <Logo size={34} />
          </a>

          <div style={{ position: 'relative' }}>
            <span className="iv-eyebrow" style={{ marginBottom: 24 }}>{lang === 'es' ? 'Plataforma privada' : 'Private platform'}</span>
            <h1 style={{
              fontSize: 'clamp(28px, 3.4vw, 44px)', fontWeight: 600, letterSpacing: '-0.025em',
              margin: '20px 0 16px', lineHeight: 1.1, textWrap: 'balance', maxWidth: 460
            }}>
              {lang === 'es' ? 'El control de tu negocio, en una sola pantalla.' : 'Your entire business, in one screen.'}
            </h1>
            <p style={{ color: 'var(--iv-text-muted)', fontSize: 15, lineHeight: 1.55, maxWidth: 440, textWrap: 'pretty' }}>
              {lang === 'es' ? 'Inventario, ventas, facturación SRI y Vision AI. Tu equipo, tus sucursales, tus métricas.' : 'Inventory, sales, SRI invoicing and Vision AI. Your team, your branches, your metrics.'}
            </p>

            {/* Mini status strip */}
            <div style={{ display: 'flex', gap: 24, marginTop: 32, flexWrap: 'wrap' }}>
              {[
                { l: lang === 'es' ? 'Uptime' : 'Uptime', v: '99.98%' },
                { l: lang === 'es' ? 'Latencia' : 'Latency', v: '< 80ms' },
                { l: lang === 'es' ? 'Sucursales' : 'Branches', v: '142+' },
              ].map((s, i) => (
                <div key={i}>
                  <div className="iv-mono" style={{ fontSize: 10, color: 'var(--iv-text-faint)', letterSpacing: '0.06em' }}>{s.l.toUpperCase()}</div>
                  <div style={{ fontSize: 18, fontWeight: 600, marginTop: 2, letterSpacing: '-0.02em' }}>{s.v}</div>
                </div>
              ))}
            </div>
          </div>

          <div className="iv-mono" style={{ position: 'relative', fontSize: 11, color: 'var(--iv-text-faint)' }}>
            © 2026 InsightVision · soporte@insightvisionia.cloud
          </div>
        </div>

        {/* Right: form */}
        <div style={{ padding: 48, display: 'flex', flexDirection: 'column', justifyContent: 'center', position: 'relative' }}>
          <button
            onClick={() => setLang(lang === 'es' ? 'en' : 'es')}
            className="iv-btn iv-btn-ghost iv-btn-sm"
            style={{ position: 'absolute', top: 24, right: 24, fontFamily: 'var(--iv-font-mono)' }}
          >
            {lang === 'es' ? 'EN' : 'ES'}
          </button>
          <form onSubmit={submit} style={{ width: '100%', maxWidth: 380, margin: '0 auto' }}>
            <h2 style={{ margin: '0 0 6px', fontSize: 26, fontWeight: 600, letterSpacing: '-0.02em' }}>{t('login.title')}</h2>
            <p style={{ margin: '0 0 28px', color: 'var(--iv-text-muted)', fontSize: 14 }}>{t('login.subtitle')}</p>

            <div style={{ marginBottom: 14 }}>
              <label className="iv-label">{t('login.email')}</label>
              <input
                type="email" className="iv-input" placeholder="tu@empresa.com"
                value={email} onChange={e => setEmail(e.target.value)}
                style={{ borderColor: err === 'email' ? 'var(--iv-error)' : undefined }}
              />
            </div>

            <div style={{ marginBottom: 14 }}>
              <label className="iv-label">{lang === 'es' ? 'Empresa / Tenant' : 'Tenant / Organization'}</label>
              <input
                type="text" className="iv-input" placeholder={lang === 'es' ? 'Nombre de tu empresa' : 'Your organization name'}
                value={tenant} onChange={e => setTenant(e.target.value)}
                style={{ borderColor: err === 'tenant' ? 'var(--iv-error)' : undefined }}
              />
            </div>

            <div style={{ marginBottom: 18 }}>
              <label className="iv-label">{t('login.password')}</label>
              <div style={{ position: 'relative' }}>
                <input
                  type={showPwd ? 'text' : 'password'} className="iv-input"
                  value={password} onChange={e => setPassword(e.target.value)}
                  style={{ paddingRight: 40, borderColor: err === 'password' ? 'var(--iv-error)' : undefined }}
                />
                <button type="button" onClick={() => setShowPwd(s => !s)} style={{
                  position: 'absolute', right: 8, top: 6, width: 26, height: 26, padding: 0,
                  border: 0, background: 'transparent', color: 'var(--iv-text-faint)', cursor: 'pointer'
                }}>
                  <Icon name={showPwd ? 'eyeOff' : 'eye'} size={15} />
                </button>
              </div>
            </div>

            <button type="submit" className="iv-btn iv-btn-primary iv-btn-lg" style={{ width: '100%' }} disabled={loading}>
              {loading ? (lang === 'es' ? 'Verificando…' : 'Verifying…') : t('login.cta')}
            </button>

            <div style={{ display: 'flex', alignItems: 'center', gap: 12, margin: '20px 0', color: 'var(--iv-text-faint)', fontSize: 11 }}>
              <div style={{ flex: 1, height: 1, background: 'var(--iv-divider)' }} />
              <span className="iv-mono">{lang === 'es' ? 'O CONTINÚA CON' : 'OR CONTINUE WITH'}</span>
              <div style={{ flex: 1, height: 1, background: 'var(--iv-divider)' }} />
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, marginBottom: 22 }}>
              <button type="button" onClick={() => goto('unauthorized')} className="iv-btn iv-btn-secondary">
                <svg width="14" height="14" viewBox="0 0 48 48"><path fill="#FFC107" d="M43.6 20.1H42V20H24v8h11.3c-1.6 4.7-6.1 8-11.3 8-6.6 0-12-5.4-12-12s5.4-12 12-12c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4 12.9 4 4 12.9 4 24s8.9 20 20 20 20-8.9 20-20c0-1.3-.1-2.6-.4-3.9z"/><path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.7 16 19 13 24 13c3.1 0 5.8 1.2 7.9 3.1l5.7-5.7C34 6.1 29.3 4 24 4c-7.6 0-14.2 4.3-17.7 10.7z"/><path fill="#4CAF50" d="M24 44c5.2 0 9.9-2 13.4-5.3l-6.2-5.2c-2 1.5-4.5 2.5-7.2 2.5-5.2 0-9.6-3.3-11.3-7.9l-6.5 5C9.5 39.6 16.2 44 24 44z"/><path fill="#1976D2" d="M43.6 20.1H42V20H24v8h11.3c-.8 2.3-2.3 4.3-4.1 5.6l6.2 5.2C41 35.5 44 30.2 44 24c0-1.3-.1-2.6-.4-3.9z"/></svg>
                Google
              </button>
              <button type="button" onClick={() => goto('unauthorized')} className="iv-btn iv-btn-secondary">
                <svg width="13" height="13" viewBox="0 0 23 23"><path fill="#F25022" d="M0 0h11v11H0z"/><path fill="#7FBA00" d="M12 0h11v11H12z"/><path fill="#00A4EF" d="M0 12h11v11H0z"/><path fill="#FFB900" d="M12 12h11v11H12z"/></svg>
                Microsoft
              </button>
            </div>

            <div style={{
              padding: 12, background: 'rgba(56, 189, 248, 0.06)',
              border: '1px solid rgba(56, 189, 248, 0.2)', borderRadius: 8,
              display: 'flex', gap: 10, marginBottom: 20
            }}>
              <Icon name="info" size={14} color="var(--iv-info)" style={{ marginTop: 2, flexShrink: 0 }} />
              <p style={{ margin: 0, fontSize: 12, color: 'var(--iv-text-muted)', lineHeight: 1.5 }}>{t('login.notice')}</p>
            </div>

            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12.5, flexWrap: 'wrap', gap: 8 }}>
              <a href="#" onClick={(e) => { e.preventDefault(); goto('landing'); setTimeout(() => document.getElementById('contact')?.scrollIntoView({ behavior: 'smooth' }), 100); }}
                style={{ color: 'var(--iv-accent-soft)', textDecoration: 'none' }}>
                {t('login.demo')}
              </a>
              <a href="mailto:soporte@insightvisionia.cloud" style={{ color: 'var(--iv-text-muted)', textDecoration: 'none' }}>{t('login.support')}</a>
            </div>
          </form>
        </div>
      </div>
      {typeof Footer !== 'undefined' && <Footer />}
    </div>
  );
}

function UnauthorizedPage({ goto }) {
  const { t } = useLang();
  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <div style={{ flex: 1, display: 'grid', placeItems: 'center', padding: 24, position: 'relative', overflow: 'hidden' }}>
        <div className="iv-halo iv-halo-primary" style={{ width: 600, height: 600, top: '-20%', left: '50%', transform: 'translateX(-50%)', opacity: 0.2 }} />
        <div className="iv-grid-bg" style={{ position: 'absolute', inset: 0, opacity: 0.3, maskImage: 'radial-gradient(circle at center, black, transparent 60%)', WebkitMaskImage: 'radial-gradient(circle at center, black, transparent 60%)' }} />
        <div className="iv-scale-in" style={{
          position: 'relative', maxWidth: 460, width: '100%',
          background: 'var(--iv-bg-elevated)', border: '1px solid var(--iv-border-strong)',
          borderRadius: 16, padding: 36, textAlign: 'center', boxShadow: 'var(--iv-shadow-lg)'
        }}>
          <div style={{
            width: 64, height: 64, margin: '0 auto 20px', borderRadius: 16,
            background: 'rgba(245, 158, 11, 0.12)', border: '1px solid rgba(245, 158, 11, 0.3)',
            display: 'grid', placeItems: 'center', color: 'var(--iv-warning)'
          }}>
            <Icon name="shield" size={28} />
          </div>
          <h2 style={{ margin: '0 0 10px', fontSize: 22, fontWeight: 600, letterSpacing: '-0.02em' }}>{t('login.unauthorized.title')}</h2>
          <p style={{ margin: '0 0 24px', color: 'var(--iv-text-muted)', fontSize: 14, lineHeight: 1.55 }}>{t('login.unauthorized.body')}</p>
          <div style={{ display: 'flex', gap: 8, justifyContent: 'center', flexWrap: 'wrap' }}>
            <button onClick={() => { goto('landing'); setTimeout(() => document.getElementById('contact')?.scrollIntoView({ behavior: 'smooth' }), 100); }} className="iv-btn iv-btn-primary">
              {t('nav.demo')}
            </button>
            <button onClick={() => goto('login')} className="iv-btn iv-btn-secondary">
              {t('login.unauthorized.back')}
            </button>
          </div>
        </div>
      </div>
      {typeof Footer !== 'undefined' && <Footer />}
    </div>
  );
}

window.LoginPage = LoginPage;
window.UnauthorizedPage = UnauthorizedPage;
