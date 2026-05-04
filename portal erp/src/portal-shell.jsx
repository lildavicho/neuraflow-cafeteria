/* portal-shell.jsx — Sidebar + Topbar + theme toggle, hosts portal pages (v2 responsive) */

const { useState: useStateP, useEffect: useEffectP } = React;

function PortalShell({ goto, currentPage, setPortalPage }) {
  const { t, lang, setLang } = useLang();
  const [theme, setTheme] = useStateP(() => localStorage.getItem('iv-theme') || 'dark');
  const [collapsed, setCollapsed] = useStateP(false);
  const [mobileOpen, setMobileOpen] = useStateP(false);
  const [branch, setBranch] = useStateP('Centro');
  const [user] = useStateP(() => {
    try { return JSON.parse(localStorage.getItem('iv-user') || '{"email":"admin@empresa.com","name":"admin"}'); }
    catch { return { email: 'admin@empresa.com', name: 'admin' }; }
  });

  useEffectP(() => {
    document.documentElement.setAttribute('data-theme', theme);
    localStorage.setItem('iv-theme', theme);
  }, [theme]);

  useEffectP(() => () => document.documentElement.removeAttribute('data-theme'), []);

  const navItems = [
    { id: 'dashboard', icon: 'dashboard', k: 'portal.dashboard' },
    { id: 'vision',    icon: 'vision',    k: 'portal.vision', accent: true },
    { id: 'invoicing', icon: 'invoice',   k: 'portal.invoicing' },
    { id: 'inventory', icon: 'inventory', k: 'portal.inventory' },
    { id: 'sales',     icon: 'sales',     k: 'portal.sales' },
    { id: 'orders',    icon: 'orders',    k: 'portal.orders' },
    { id: 'reports',   icon: 'reports',   k: 'portal.reports' },
    { id: 'customers', icon: 'customers', k: 'portal.customers' },
    { id: 'users',     icon: 'users',     k: 'portal.users' },
    { id: 'settings',  icon: 'settings',  k: 'portal.settings' },
  ];

  const sidebarW = collapsed ? 64 : 240;

  return (
    <div style={{ minHeight: '100vh', background: 'var(--iv-bg)', color: 'var(--iv-text)' }}>
      {/* Mobile overlay */}
      {mobileOpen && (
        <div
          style={{
            position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 35,
            backdropFilter: 'blur(4px)'
          }}
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside className={`iv-portal-sidebar ${mobileOpen ? 'iv-open' : ''}`} style={{
        position: 'fixed', top: 0, left: 0, bottom: 0, width: sidebarW,
        background: 'var(--iv-bg-elevated)', borderRight: '1px solid var(--iv-border)',
        display: 'flex', flexDirection: 'column', padding: 12,
        zIndex: 40
      }}>
        <div style={{
          height: 44, display: 'flex', alignItems: 'center', justifyContent: collapsed ? 'center' : 'space-between',
          padding: collapsed ? 0 : '0 6px', marginBottom: 8
        }}>
          {!collapsed ? <Logo size={24} /> : <Logo variant="mark" size={26} />}
          {!collapsed && (
            <button
              className="iv-btn iv-btn-ghost iv-btn-sm"
              style={{ display: 'none' }}
              onClick={() => setMobileOpen(false)}
            >
              <Icon name="close" size={16} />
            </button>
          )}
        </div>

        <nav style={{ display: 'flex', flexDirection: 'column', gap: 2, flex: 1 }}>
          {navItems.map(item => {
            const active = currentPage === item.id;
            return (
              <button
                key={item.id}
                onClick={() => { setPortalPage(item.id); setMobileOpen(false); }}
                style={{
                  display: 'flex', alignItems: 'center', gap: 10,
                  padding: collapsed ? '8px' : '8px 10px',
                  justifyContent: collapsed ? 'center' : 'flex-start',
                  borderRadius: 8, border: 0, cursor: 'pointer',
                  background: active ? 'rgba(79, 70, 229, 0.12)' : 'transparent',
                  color: active ? 'var(--iv-accent-soft)' : 'var(--iv-text-muted)',
                  fontFamily: 'var(--iv-font-sans)', fontSize: 13.5, fontWeight: active ? 500 : 400,
                  transition: 'all 0.12s ease', position: 'relative',
                  letterSpacing: '-0.005em'
                }}
                onMouseEnter={e => { if (!active) { e.currentTarget.style.background = 'var(--iv-surface-2)'; e.currentTarget.style.color = 'var(--iv-text)'; } }}
                onMouseLeave={e => { if (!active) { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = 'var(--iv-text-muted)'; } }}
                title={collapsed ? t(item.k) : ''}
              >
                {active && <span style={{ position: 'absolute', left: -12, top: 8, bottom: 8, width: 2, borderRadius: 2, background: 'var(--iv-accent)' }} />}
                <Icon name={item.icon} size={16} />
                {!collapsed && <span>{t(item.k)}</span>}
                {!collapsed && item.accent && <span className="iv-badge iv-badge-primary" style={{ marginLeft: 'auto', fontSize: 9.5, padding: '1px 6px' }}>NEW</span>}
              </button>
            );
          })}
        </nav>

        <div style={{ marginTop: 'auto', borderTop: '1px solid var(--iv-divider)', paddingTop: 10 }}>
          <button onClick={() => setCollapsed(c => !c)} style={{
            width: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
            padding: '7px', borderRadius: 6, border: 0, background: 'transparent',
            color: 'var(--iv-text-faint)', cursor: 'pointer', fontSize: 11
          }}>
            <Icon name={collapsed ? 'arrowRight' : 'menu'} size={13} />
            {!collapsed && <span>{lang === 'es' ? 'Colapsar' : 'Collapse'}</span>}
          </button>
        </div>
      </aside>

      {/* Topbar */}
      <header className="iv-portal-topbar" style={{
        position: 'fixed', top: 0, left: sidebarW, right: 0, height: 56,
        background: 'rgba(7, 9, 14, 0.7)', backdropFilter: 'blur(16px)', WebkitBackdropFilter: 'blur(16px)',
        borderBottom: '1px solid var(--iv-border)', zIndex: 30,
        display: 'flex', alignItems: 'center', padding: '0 24px', gap: 14
      }}>
        {/* Mobile toggle */}
        <button
          className="iv-btn iv-btn-ghost iv-btn-sm iv-portal-mobile-toggle"
          style={{ padding: '0 6px', height: 32 }}
          onClick={() => setMobileOpen(true)}
        >
          <Icon name="menu" size={18} />
        </button>

        {/* Search */}
        <div className="iv-portal-search" style={{ position: 'relative', flex: 1, maxWidth: 480 }}>
          <Icon name="search" size={14} style={{ position: 'absolute', left: 11, top: 11, color: 'var(--iv-text-faint)' }} />
          <input className="iv-input" placeholder={t('portal.search')} style={{ height: 34, paddingLeft: 32, fontSize: 13 }} />
          <kbd className="iv-mono" style={{
            position: 'absolute', right: 8, top: 8, fontSize: 10,
            padding: '2px 6px', background: 'var(--iv-surface-2)', borderRadius: 4,
            border: '1px solid var(--iv-border)', color: 'var(--iv-text-faint)'
          }}>⌘K</kbd>
        </div>

        <div style={{ flex: 1 }} />

        {/* Branch selector */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: 8, padding: '6px 10px',
          background: 'var(--iv-surface)', border: '1px solid var(--iv-border)', borderRadius: 6,
          fontSize: 12.5, color: 'var(--iv-text-muted)', cursor: 'pointer'
        }}>
          <Icon name="building" size={13} />
          <span style={{ color: 'var(--iv-text)' }}>{branch}</span>
          <Icon name="chevronDown" size={11} />
        </div>

        {/* Lang */}
        <button onClick={() => setLang(lang === 'es' ? 'en' : 'es')} className="iv-btn iv-btn-ghost iv-btn-sm" style={{ height: 32, fontFamily: 'var(--iv-font-mono)', fontSize: 11 }}>
          {lang === 'es' ? 'EN' : 'ES'}
        </button>

        {/* Theme */}
        <button onClick={() => setTheme(t => t === 'dark' ? 'light' : 'dark')} className="iv-btn iv-btn-ghost iv-btn-sm" style={{ height: 32, padding: '0 8px' }} title="Theme">
          <Icon name={theme === 'dark' ? 'sun' : 'moon'} size={15} />
        </button>

        {/* Notif */}
        <button className="iv-btn iv-btn-ghost iv-btn-sm" style={{ height: 32, padding: '0 8px', position: 'relative' }}>
          <Icon name="bell" size={15} />
          <span style={{ position: 'absolute', top: 6, right: 6, width: 6, height: 6, borderRadius: '50%', background: 'var(--iv-error)' }} />
        </button>

        {/* User */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, paddingLeft: 8, borderLeft: '1px solid var(--iv-divider)' }}>
          <Avatar name={user.name} size={28} />
          <div className="iv-portal-user-label" style={{ fontSize: 12 }}>
            <div style={{ fontWeight: 500, color: 'var(--iv-text)' }}>{user.name}</div>
            <div className="iv-mono" style={{ color: 'var(--iv-text-faint)', fontSize: 10 }}>{user.email}</div>
            {user.tenant && <div className="iv-mono" style={{ color: 'var(--iv-accent-soft)', fontSize: 10, marginTop: 1 }}>{user.tenant}</div>}
          </div>
          <button onClick={() => { localStorage.removeItem('iv-user'); goto('landing'); }} className="iv-btn iv-btn-ghost iv-btn-sm" title={t('portal.logout')} style={{ padding: 6, height: 28 }}>
            <Icon name="logout" size={14} />
          </button>
        </div>
      </header>

      {/* Content */}
      <main className="iv-portal-main" style={{
        marginLeft: sidebarW, paddingTop: 56, minHeight: '100vh'
      }}>
        <div style={{ padding: 28 }}>
          {currentPage === 'dashboard' && <DashboardPage />}
          {currentPage === 'vision' && <VisionPage />}
          {currentPage === 'invoicing' && <InvoicingPage />}
          {currentPage === 'inventory' && <InventoryPage />}
          {currentPage === 'sales' && <SalesPage />}
          {currentPage === 'orders' && <OrdersPage />}
          {currentPage === 'reports' && <ReportsPage />}
          {currentPage === 'customers' && <CustomersPage />}
          {currentPage === 'users' && <UsersPage />}
          {currentPage === 'settings' && <SettingsPage />}
        </div>
      </main>
    </div>
  );
}

/* ============================================================
   PageHeader
   ============================================================ */
function PageHeader({ title, subtitle, actions, breadcrumb }) {
  return (
    <div style={{ marginBottom: 24 }}>
      {breadcrumb && (
        <div className="iv-mono" style={{ fontSize: 11, color: 'var(--iv-text-faint)', marginBottom: 8, display: 'flex', alignItems: 'center', gap: 6, letterSpacing: '0.02em' }}>
          {breadcrumb.map((b, i) => (
            <React.Fragment key={i}>
              <span>{b}</span>
              {i < breadcrumb.length - 1 && <Icon name="chevronRight" size={11} />}
            </React.Fragment>
          ))}
        </div>
      )}
      <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: 16, flexWrap: 'wrap' }}>
        <div>
          <h1 style={{ margin: 0, fontSize: 24, fontWeight: 600, letterSpacing: '-0.02em' }}>{title}</h1>
          {subtitle && <p style={{ margin: '4px 0 0', color: 'var(--iv-text-muted)', fontSize: 13.5 }}>{subtitle}</p>}
        </div>
        {actions && <div className="iv-portal-actions" style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>{actions}</div>}
      </div>
    </div>
  );
}

window.PortalShell = PortalShell;
window.PageHeader = PageHeader;
