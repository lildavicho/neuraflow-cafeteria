/* portal-pages.jsx — Dashboard, Vision AI, Invoicing, Inventory, Sales, Orders, Reports, Customers, Users, Settings (v2 responsive) */

const { useState: useStateD, useEffect: useEffectD, useMemo: useMemoD } = React;

/* ============================================================
   DASHBOARD
   ============================================================ */
function DashboardPage() {
  const { t, lang } = useLang();
  const sales14 = [3200, 4100, 2900, 4800, 5400, 6200, 5800, 7200, 6900, 8100, 7800, 9000, 9500, 11200];
  const ai14 = sales14.map(v => Math.round(v * 0.04 + Math.random() * 30));
  const labels = Array.from({ length: 14 }, (_, i) => `${i + 1} May`);

  const kpis = [
    { label: t('kpi.salesToday'), value: '$11,240', delta: '+18.2%', deltaPositive: true, sparkData: [3,5,4,6,7,8,9,11], icon: 'sales', accent: '#4F46E5' },
    { label: t('kpi.monthIncome'), value: '$184,392', delta: '+9.4%', deltaPositive: true, sparkData: [4,5,6,7,6,8,9,10], icon: 'trending', accent: '#22C55E' },
    { label: t('kpi.lowStock'), value: '14', delta: '-3', deltaPositive: false, sparkData: [9,8,7,6,5,5,6,4], icon: 'alert', accent: '#F59E0B' },
    { label: t('kpi.invoices'), value: '328', delta: '+12.0%', deltaPositive: true, sparkData: [3,4,5,6,7,7,8,9], icon: 'invoice', accent: '#38BDF8' },
    { label: t('kpi.emails'), value: '1,284', delta: '+22%', deltaPositive: true, sparkData: [2,3,4,5,6,7,8,10], icon: 'mail', accent: '#8B5CF6' },
    { label: t('kpi.events'), value: '4,287', delta: '+34%', deltaPositive: true, sparkData: [2,4,3,5,6,7,9,12], icon: 'vision', accent: '#A78BFA' },
    { label: t('kpi.pendingOrders'), value: '23', delta: '+4', deltaPositive: false, sparkData: [2,3,4,3,4,5,4,5], icon: 'orders', accent: '#EC4899' },
    { label: t('kpi.alerts'), value: '6', delta: '-2', deltaPositive: true, sparkData: [8,7,6,7,5,6,5,4], icon: 'shield', accent: '#EF4444' },
  ];

  return (
    <div className="iv-fade-in">
      <PageHeader
        title={`${t('portal.welcome')}, admin`}
        subtitle={t('portal.overview', { date: new Date().toLocaleDateString(lang === 'es' ? 'es-EC' : 'en-US', { weekday: 'long', day: 'numeric', month: 'long' }) })}
        actions={[
          <button key="1" className="iv-btn iv-btn-secondary"><Icon name="download" size={13} /> {lang === 'es' ? 'Exportar' : 'Export'}</button>,
          <button key="2" className="iv-btn iv-btn-primary"><Icon name="plus" size={13} /> {lang === 'es' ? 'Nueva venta' : 'New sale'}</button>,
        ]}
      />

      {/* KPIs */}
      <div className="iv-kpi-grid iv-stagger" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: 14, marginBottom: 20 }}>
        {kpis.map((k, i) => <KpiCard key={i} {...k} />)}
      </div>

      {/* Charts row */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 16, marginBottom: 16 }} className="iv-dash-row">
        <div className="iv-card" style={{ padding: 22 }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 18, flexWrap: 'wrap', gap: 8 }}>
            <div>
              <h3 style={{ margin: 0, fontSize: 14.5, fontWeight: 600, letterSpacing: '-0.01em' }}>{lang === 'es' ? 'Ventas e ingresos' : 'Sales & revenue'}</h3>
              <p style={{ margin: '3px 0 0', fontSize: 12, color: 'var(--iv-text-muted)' }}>{lang === 'es' ? 'Últimos 14 días · USD' : 'Last 14 days · USD'}</p>
            </div>
            <div style={{ display: 'flex', gap: 4 }}>
              {['7d', '14d', '30d', '90d'].map((p, i) => (
                <button key={p} className={`iv-btn ${i === 1 ? 'iv-btn-secondary' : 'iv-btn-ghost'} iv-btn-sm`} style={{ height: 26, fontSize: 11, padding: '0 8px' }}>{p}</button>
              ))}
            </div>
          </div>
          <div style={{ display: 'flex', gap: 18, marginBottom: 14, fontSize: 11.5 }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--iv-text-muted)' }}>
              <span style={{ width: 8, height: 8, borderRadius: 2, background: 'var(--iv-primary)' }} /> {lang === 'es' ? 'Ventas' : 'Sales'}
            </span>
            <span style={{ display: 'flex', alignItems: 'center', gap: 6, color: 'var(--iv-text-muted)' }}>
              <span style={{ width: 8, height: 8, borderRadius: 2, background: 'var(--iv-accent)' }} /> {lang === 'es' ? 'Eventos AI' : 'AI events'}
            </span>
          </div>
          <AreaChart data={sales14} secondData={ai14.map(v => v * 30)} secondColor="var(--iv-accent)" labels={labels} width={700} height={240} />
        </div>

        <div className="iv-card" style={{ padding: 22 }}>
          <h3 style={{ margin: '0 0 4px', fontSize: 14.5, fontWeight: 600, letterSpacing: '-0.01em' }}>{lang === 'es' ? 'Vision AI hoy' : 'Vision AI today'}</h3>
          <p style={{ margin: '0 0 20px', fontSize: 12, color: 'var(--iv-text-muted)' }}>{lang === 'es' ? '6 cámaras activas · YOLOv8' : '6 active cameras · YOLOv8'}</p>
          <div style={{ display: 'flex', alignItems: 'center', gap: 22, marginBottom: 22, flexWrap: 'wrap' }}>
            <Donut value={92} size={96} stroke={9} color="var(--iv-accent)" />
            <div style={{ display: 'flex', flexDirection: 'column', gap: 10, fontSize: 12.5 }}>
              <div><span style={{ color: 'var(--iv-text-muted)' }}>{lang === 'es' ? 'Detecciones' : 'Detections'}</span><div style={{ fontSize: 18, fontWeight: 600, letterSpacing: '-0.02em' }}>4,287</div></div>
              <div><span style={{ color: 'var(--iv-text-muted)' }}>{lang === 'es' ? 'Confianza prom.' : 'Avg. conf.'}</span><div style={{ fontSize: 18, fontWeight: 600, letterSpacing: '-0.02em' }}>92.4%</div></div>
            </div>
          </div>
          <div className="iv-mono" style={{ fontSize: 10.5, color: 'var(--iv-text-faint)', marginBottom: 8, letterSpacing: '0.04em' }}>{lang === 'es' ? 'POR HORA · ÚLTIMAS 12H' : 'PER HOUR · LAST 12H'}</div>
          <BarChart data={[24,38,52,41,60,75,88,72,65,80,92,105]} width={300} height={100} color="var(--iv-accent)" />
        </div>
      </div>

      {/* Activity + Alerts */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.3fr 1fr', gap: 16 }} className="iv-dash-row">
        <div className="iv-card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ padding: '16px 20px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--iv-divider)' }}>
            <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600, letterSpacing: '-0.01em' }}>{lang === 'es' ? 'Últimas operaciones' : 'Latest operations'}</h3>
            <button className="iv-btn iv-btn-ghost iv-btn-sm">{lang === 'es' ? 'Ver todo' : 'See all'} <Icon name="arrowRight" size={11} /></button>
          </div>
          <div className="iv-portal-table-wrap">
            <table className="iv-table iv-table-responsive">
              <thead><tr><th>Op</th><th>{lang === 'es' ? 'Cliente' : 'Customer'}</th><th>{lang === 'es' ? 'Monto' : 'Amount'}</th><th>{lang === 'es' ? 'Estado' : 'Status'}</th><th>{lang === 'es' ? 'Hora' : 'Time'}</th></tr></thead>
              <tbody>
                {[
                  { id: 'F-4287', c: 'Constructora Andina', a: '$4,397.40', s: 'authorized', sl: 'Autorizado', t: '14:32' },
                  { id: 'V-9012', c: 'Tienda Mi Barrio', a: '$182.50', s: 'success', sl: 'Completada', t: '14:21' },
                  { id: 'F-4286', c: 'Distribuidora Sur', a: '$1,250.00', s: 'pending', sl: 'En SRI', t: '14:18' },
                  { id: 'O-2341', c: 'Comercial Pacífico', a: '$890.30', s: 'warning', sl: 'Pendiente', t: '14:05' },
                  { id: 'F-4285', c: 'Ferretería El Sol', a: '$76.40', s: 'authorized', sl: 'Autorizado', t: '13:58' },
                  { id: 'V-9011', c: 'Cliente final', a: '$24.90', s: 'success', sl: 'Completada', t: '13:42' },
                ].map((r, i) => (
                  <tr key={i}>
                    <td data-label="Op"><span className="iv-mono" style={{ color: 'var(--iv-text-muted)' }}>{r.id}</span></td>
                    <td data-label={lang === 'es' ? 'Cliente' : 'Customer'}>{r.c}</td>
                    <td data-label={lang === 'es' ? 'Monto' : 'Amount'} className="iv-mono">{r.a}</td>
                    <td data-label={lang === 'es' ? 'Estado' : 'Status'}><StatusBadge status={r.s} label={r.sl} /></td>
                    <td data-label={lang === 'es' ? 'Hora' : 'Time'} className="iv-mono" style={{ color: 'var(--iv-text-faint)' }}>{r.t}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>

        <div className="iv-card" style={{ padding: 22 }}>
          <h3 style={{ margin: '0 0 16px', fontSize: 14, fontWeight: 600, letterSpacing: '-0.01em' }}>{lang === 'es' ? 'Alertas del sistema' : 'System alerts'}</h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
            {[
              { type: 'warning', icon: 'alert', t: lang === 'es' ? '14 productos bajo stock mínimo' : '14 products below min stock', d: lang === 'es' ? 'Sucursal Centro · revisar reposición' : 'Centro branch · review restock' },
              { type: 'error', icon: 'x', t: lang === 'es' ? 'Cámara CAM-07 desconectada' : 'Camera CAM-07 offline', d: lang === 'es' ? 'Sucursal Norte · 4 min sin señal' : 'Norte branch · 4 min no signal' },
              { type: 'info', icon: 'info', t: lang === 'es' ? '3 facturas en reintento SRI' : '3 invoices retrying with SRI', d: lang === 'es' ? 'Próximo intento en 2 min' : 'Next attempt in 2 min' },
              { type: 'success', icon: 'check', t: lang === 'es' ? 'Backup nocturno completo' : 'Nightly backup complete', d: lang === 'es' ? '02:14 · 1.2GB · S3' : '02:14 · 1.2GB · S3' },
            ].map((a, i) => (
              <div key={i} style={{
                display: 'flex', gap: 10, padding: '10px 12px',
                background: 'var(--iv-surface-2)', borderRadius: 8, border: '1px solid var(--iv-border)'
              }}>
                <div style={{
                  width: 26, height: 26, borderRadius: 7, flexShrink: 0,
                  background: `var(--iv-${a.type}-bg)`, color: `var(--iv-${a.type})`,
                  display: 'grid', placeItems: 'center'
                }}>
                  <Icon name={a.icon} size={13} />
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 12.5, fontWeight: 500 }}>{a.t}</div>
                  <div style={{ fontSize: 11.5, color: 'var(--iv-text-muted)', marginTop: 2 }}>{a.d}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   VISION AI
   ============================================================ */
function VisionPage() {
  const { lang } = useLang();
  const [cam, setCam] = useStateD(0);
  const [tick, setTick] = useStateD(0);
  const [events, setEvents] = useStateD([]);

  useEffectD(() => {
    const id = setInterval(() => setTick(t => t + 1), 1500);
    return () => clearInterval(id);
  }, []);

  const cameras = [
    { id: 'CAM-01', name: 'Centro · Entrada', status: 'online', det: 1284 },
    { id: 'CAM-02', name: 'Centro · Caja 1', status: 'online', det: 892 },
    { id: 'CAM-03', name: 'Centro · Caja 2', status: 'online', det: 745 },
    { id: 'CAM-04', name: 'Norte · Bodega', status: 'online', det: 412 },
    { id: 'CAM-05', name: 'Sur · Entrada', status: 'online', det: 654 },
    { id: 'CAM-06', name: 'Norte · Caja', status: 'online', det: 300 },
  ];

  useEffectD(() => {
    const labels = ['person', 'cart', 'box', 'bag'];
    const newEvent = {
      id: tick,
      t: new Date().toLocaleTimeString('en-GB'),
      cam: cameras[Math.floor(Math.random() * cameras.length)].id,
      branch: ['Centro', 'Norte', 'Sur'][Math.floor(Math.random() * 3)],
      label: labels[Math.floor(Math.random() * labels.length)],
      conf: (0.78 + Math.random() * 0.21).toFixed(3),
      frame: `f_${(82340 + tick).toString(16)}`,
    };
    setEvents(p => [newEvent, ...p].slice(0, 14));
  }, [tick]);

  return (
    <div className="iv-fade-in">
      <PageHeader
        title="Vision AI"
        subtitle={lang === 'es' ? 'Detección con YOLO en tiempo real · 6 cámaras activas' : 'Real-time YOLO detection · 6 active cameras'}
        breadcrumb={[lang === 'es' ? 'Portal' : 'Portal', 'Vision AI']}
        actions={[
          <span key="0" className="iv-badge iv-badge-success iv-badge-dot iv-badge-pulse">YOLO API · OK</span>,
          <button key="1" className="iv-btn iv-btn-secondary"><Icon name="key" size={13} /> API Keys</button>,
          <button key="2" className="iv-btn iv-btn-primary"><Icon name="plus" size={13} /> {lang === 'es' ? 'Conectar cámara' : 'Connect camera'}</button>,
        ]}
      />

      {/* KPIs */}
      <div className="iv-kpi-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: 14, marginBottom: 18 }}>
        <KpiCard label={lang === 'es' ? 'Cámaras activas' : 'Active cameras'} value="6" suffix="/6" icon="camera" accent="#A78BFA" sparkData={[6,6,6,5,6,6,6]} />
        <KpiCard label={lang === 'es' ? 'Eventos hoy' : 'Events today'} value="4,287" delta="+34%" icon="zap" accent="#8B5CF6" sparkData={[2,4,3,5,7,9,12]} />
        <KpiCard label={lang === 'es' ? 'Detecciones totales' : 'Total detections'} value="92,418" delta="+12%" icon="vision" accent="#4F46E5" sparkData={[3,5,6,7,8,9,11]} />
        <KpiCard label={lang === 'es' ? 'Confianza prom.' : 'Avg. confidence'} value="92.4%" delta="+0.8%" icon="trending" accent="#22C55E" sparkData={[5,6,7,7,8,9,9]} />
      </div>

      {/* Live feed + cam list */}
      <div style={{ display: 'grid', gridTemplateColumns: '1.5fr 1fr', gap: 16, marginBottom: 16 }} className="iv-dash-row">
        <div>
          <VisionLiveFeedFull camName={cameras[cam].name} camId={cameras[cam].id} tick={tick} />
        </div>
        <div className="iv-card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ padding: '14px 18px', borderBottom: '1px solid var(--iv-divider)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
            <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>{lang === 'es' ? 'Cámaras conectadas' : 'Connected cameras'}</h3>
            <span className="iv-mono" style={{ fontSize: 10.5, color: 'var(--iv-text-faint)' }}>{cameras.filter(c => c.status === 'online').length}/{cameras.length} ONLINE</span>
          </div>
          <div style={{ maxHeight: 380, overflow: 'auto' }}>
            {cameras.map((c, i) => (
              <button key={c.id} onClick={() => setCam(i)} style={{
                display: 'flex', alignItems: 'center', gap: 12, padding: '12px 18px', width: '100%',
                background: cam === i ? 'rgba(79,70,229,0.08)' : 'transparent',
                border: 0, borderBottom: '1px solid var(--iv-divider)', cursor: 'pointer',
                color: 'var(--iv-text)', textAlign: 'left'
              }}>
                <div style={{
                  width: 32, height: 32, borderRadius: 8, flexShrink: 0,
                  background: 'var(--iv-surface-2)', display: 'grid', placeItems: 'center',
                  color: c.status === 'online' ? 'var(--iv-success)' : 'var(--iv-error)',
                  border: `1px solid ${c.status === 'online' ? 'rgba(34,197,94,0.3)' : 'rgba(239,68,68,0.3)'}`
                }}>
                  <Icon name="camera" size={14} />
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 13, fontWeight: 500 }}>{c.name}</div>
                  <div className="iv-mono" style={{ fontSize: 10.5, color: 'var(--iv-text-faint)' }}>{c.id} · {c.det.toLocaleString()} det</div>
                </div>
                <StatusBadge status={c.status} label={c.status === 'online' ? 'LIVE' : 'OFF'} pulse={c.status === 'online'} />
              </button>
            ))}
          </div>
        </div>
      </div>

      {/* Events table + chart */}
      <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr', gap: 16 }} className="iv-dash-row">
        <div className="iv-card" style={{ padding: 0, overflow: 'hidden' }}>
          <div style={{ padding: '14px 18px', borderBottom: '1px solid var(--iv-divider)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
            <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>{lang === 'es' ? 'Eventos en vivo' : 'Live events'}</h3>
            <span className="iv-badge iv-badge-error iv-badge-dot iv-badge-pulse">STREAM</span>
          </div>
          <div style={{ maxHeight: 360, overflow: 'auto' }}>
            <div className="iv-portal-table-wrap">
              <table className="iv-table iv-table-responsive">
                <thead><tr><th>{lang === 'es' ? 'Hora' : 'Time'}</th><th>Cam</th><th>{lang === 'es' ? 'Sucursal' : 'Branch'}</th><th>{lang === 'es' ? 'Etiqueta' : 'Label'}</th><th>{lang === 'es' ? 'Confianza' : 'Conf.'}</th><th>Frame</th><th></th></tr></thead>
                <tbody>
                  {events.map((e, i) => (
                    <tr key={e.id} style={{ animation: i === 0 ? 'iv-fade-up 0.3s ease both' : undefined }}>
                      <td data-label={lang === 'es' ? 'Hora' : 'Time'} className="iv-mono" style={{ fontSize: 11.5, color: 'var(--iv-text-muted)' }}>{e.t}</td>
                      <td data-label="Cam" className="iv-mono" style={{ fontSize: 11.5 }}>{e.cam}</td>
                      <td data-label={lang === 'es' ? 'Sucursal' : 'Branch'}>{e.branch}</td>
                      <td data-label={lang === 'es' ? 'Etiqueta' : 'Label'}><span className="iv-badge iv-badge-primary">{e.label}</span></td>
                      <td data-label={lang === 'es' ? 'Confianza' : 'Conf.'} className="iv-mono">{e.conf}</td>
                      <td data-label="Frame" className="iv-mono" style={{ fontSize: 11, color: 'var(--iv-text-faint)' }}>{e.frame}</td>
                      <td data-label=""><StatusBadge status="success" label={lang === 'es' ? 'OK' : 'OK'} /></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div className="iv-card" style={{ padding: 22 }}>
          <h3 style={{ margin: '0 0 4px', fontSize: 14, fontWeight: 600 }}>{lang === 'es' ? 'Detecciones por hora' : 'Detections by hour'}</h3>
          <p style={{ margin: '0 0 20px', fontSize: 11.5, color: 'var(--iv-text-muted)' }}>{lang === 'es' ? 'Últimas 12 horas' : 'Last 12 hours'}</p>
          <BarChart data={[28,42,55,70,85,92,108,124,142,165,182,210]} width={320} height={180} color="var(--iv-accent)" labels={['8','9','10','11','12','13','14','15','16','17','18','19']} />
          <div style={{ borderTop: '1px solid var(--iv-divider)', marginTop: 18, paddingTop: 14 }}>
            <div className="iv-mono" style={{ fontSize: 10.5, color: 'var(--iv-text-faint)', letterSpacing: '0.04em', marginBottom: 8 }}>{lang === 'es' ? 'ESTADO API YOLO' : 'YOLO API STATUS'}</div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12 }}>
              <span style={{ color: 'var(--iv-text-muted)' }}>endpoint</span><span className="iv-mono">v8/predict</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginTop: 4 }}>
              <span style={{ color: 'var(--iv-text-muted)' }}>latencia p95</span><span className="iv-mono">142ms</span>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12, marginTop: 4 }}>
              <span style={{ color: 'var(--iv-text-muted)' }}>uptime 24h</span><span className="iv-mono" style={{ color: 'var(--iv-success)' }}>100%</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function VisionLiveFeedFull({ camName, camId, tick }) {
  const [boxes, setBoxes] = useStateD([]);
  useEffectD(() => {
    const labels = ['person', 'person', 'cart', 'box'];
    const n = 3 + Math.floor(Math.random() * 3);
    setBoxes(Array.from({ length: n }, (_, i) => ({
      id: tick * 10 + i,
      x: 5 + Math.random() * 75, y: 12 + Math.random() * 55,
      w: 10 + Math.random() * 18, h: 18 + Math.random() * 25,
      label: labels[Math.floor(Math.random() * labels.length)],
      conf: (0.78 + Math.random() * 0.21).toFixed(2),
    })));
  }, [tick]);

  return (
    <div className="iv-card" style={{ padding: 0, overflow: 'hidden' }}>
      <div style={{ padding: '12px 18px', borderBottom: '1px solid var(--iv-divider)', display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <span className="iv-badge iv-badge-error iv-badge-dot iv-badge-pulse">LIVE</span>
          <span style={{ fontSize: 13, fontWeight: 500 }}>{camName}</span>
          <span className="iv-mono" style={{ fontSize: 10.5, color: 'var(--iv-text-faint)' }}>{camId}</span>
        </div>
        <div style={{ display: 'flex', gap: 4 }}>
          <button className="iv-btn iv-btn-ghost iv-btn-sm" style={{ padding: 6, height: 28 }}><Icon name="pause" size={13} /></button>
          <button className="iv-btn iv-btn-ghost iv-btn-sm" style={{ padding: 6, height: 28 }}><Icon name="expand" size={13} /></button>
          <button className="iv-btn iv-btn-ghost iv-btn-sm" style={{ padding: 6, height: 28 }}><Icon name="settings" size={13} /></button>
        </div>
      </div>
      <div style={{
        position: 'relative', aspectRatio: '16/9',
        background: 'linear-gradient(135deg, #0a0e18, #1a2138)',
        backgroundImage: `linear-gradient(135deg, #0a0e18, #1a2138), repeating-linear-gradient(0deg, rgba(255,255,255,0.02) 0px, rgba(255,255,255,0.02) 2px, transparent 2px, transparent 4px)`,
        overflow: 'hidden'
      }}>
        <div style={{ position: 'absolute', bottom: 0, left: 0, right: 0, height: '40%', background: 'linear-gradient(180deg, transparent, rgba(79, 70, 229, 0.12))' }} />
        {boxes.map(b => {
          const color = b.label === 'person' ? '#22C55E' : b.label === 'cart' ? '#A78BFA' : '#38BDF8';
          return (
            <div key={b.id} style={{
              position: 'absolute',
              left: `${b.x}%`, top: `${b.y}%`, width: `${b.w}%`, height: `${b.h}%`,
              border: `1.5px solid ${color}`, borderRadius: 2,
              boxShadow: `0 0 20px ${color}55`, animation: 'iv-fade-in 0.3s ease both'
            }}>
              <div style={{
                position: 'absolute', top: -22, left: -1.5,
                background: color, color: '#0a0e18',
                fontFamily: 'var(--iv-font-mono)', fontSize: 10, fontWeight: 700, padding: '2px 6px',
                borderRadius: '2px 2px 0 0', whiteSpace: 'nowrap'
              }}>
                {b.label} {b.conf}
              </div>
            </div>
          );
        })}
        <div style={{ position: 'absolute', top: 14, left: 14, fontFamily: 'var(--iv-font-mono)', fontSize: 11, color: 'rgba(255,255,255,0.6)' }}>
          ● REC 00:{String(tick % 60).padStart(2, '0')}
        </div>
        <div style={{ position: 'absolute', top: 14, right: 14, fontFamily: 'var(--iv-font-mono)', fontSize: 11, color: 'rgba(255,255,255,0.6)' }}>
          1080p · 30fps
        </div>
        <div style={{ position: 'absolute', bottom: 14, right: 14, fontFamily: 'var(--iv-font-mono)', fontSize: 11, color: 'rgba(255,255,255,0.6)' }}>
          {boxes.length} det · YOLOv8
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   INVOICING
   ============================================================ */
function InvoicingPage() {
  const { lang } = useLang();
  const rows = [
    { n: '0000004287', c: 'Constructora Andina S.A.', a: 4397.40, sri: 'authorized', ride: 'sent', mail: 'sent', t: '14:32', tries: 1 },
    { n: '0000004286', c: 'Distribuidora Sur', a: 1250.00, sri: 'pending', ride: 'pending', mail: 'pending', t: '14:18', tries: 1 },
    { n: '0000004285', c: 'Ferretería El Sol', a: 76.40, sri: 'authorized', ride: 'sent', mail: 'sent', t: '13:58', tries: 1 },
    { n: '0000004284', c: 'Comercial Pacífico', a: 890.30, sri: 'authorized', ride: 'sent', mail: 'failed', t: '13:42', tries: 3 },
    { n: '0000004283', c: 'Tienda Mi Barrio', a: 182.50, sri: 'authorized', ride: 'sent', mail: 'sent', t: '13:21', tries: 1 },
    { n: '0000004282', c: 'Importadora Cuenca', a: 7820.00, sri: 'rejected', ride: 'failed', mail: 'failed', t: '13:05', tries: 2 },
    { n: '0000004281', c: 'Supermercado Norte', a: 432.10, sri: 'authorized', ride: 'sent', mail: 'sent', t: '12:48', tries: 1 },
    { n: '0000004280', c: 'Cliente final', a: 24.90, sri: 'authorized', ride: 'sent', mail: 'sent', t: '12:32', tries: 1 },
  ];

  return (
    <div className="iv-fade-in">
      <PageHeader
        title={lang === 'es' ? 'Facturación SRI' : 'SRI Invoicing'}
        subtitle={lang === 'es' ? 'Facturas electrónicas, autorización y entrega' : 'Electronic invoices, authorization and delivery'}
        breadcrumb={[lang === 'es' ? 'Portal' : 'Portal', lang === 'es' ? 'Facturación' : 'Invoicing']}
        actions={[
          <button key="1" className="iv-btn iv-btn-secondary"><Icon name="download" size={13} /> {lang === 'es' ? 'Exportar' : 'Export'}</button>,
          <button key="2" className="iv-btn iv-btn-primary"><Icon name="plus" size={13} /> {lang === 'es' ? 'Nueva factura' : 'New invoice'}</button>,
        ]}
      />

      <div className="iv-kpi-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 14, marginBottom: 18 }}>
        <KpiCard label={lang === 'es' ? 'Emitidas hoy' : 'Issued today'} value="328" delta="+12%" icon="invoice" accent="#4F46E5" sparkData={[3,4,5,6,7,8,9]} />
        <KpiCard label={lang === 'es' ? 'Autorizadas SRI' : 'SRI authorized'} value="316" suffix="/328" icon="check" accent="#22C55E" sparkData={[3,4,5,6,7,8,9]} />
        <KpiCard label={lang === 'es' ? 'En reintento' : 'Retrying'} value="9" delta="+2" deltaPositive={false} icon="refresh" accent="#F59E0B" sparkData={[2,3,4,3,4,5,4]} />
        <KpiCard label={lang === 'es' ? 'Rechazadas' : 'Rejected'} value="3" delta="-1" icon="x" accent="#EF4444" sparkData={[3,4,5,4,3,3,3]} />
      </div>

      <div className="iv-card" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ padding: 14, display: 'flex', gap: 10, borderBottom: '1px solid var(--iv-divider)', flexWrap: 'wrap' }}>
          <input className="iv-input" placeholder={lang === 'es' ? 'Buscar factura, cliente, RUC…' : 'Search invoice, customer, RUC…'} style={{ flex: 1, height: 32, minWidth: 200 }} />
          <select className="iv-select" style={{ width: 140, height: 32 }}><option>{lang === 'es' ? 'Todos los estados' : 'All statuses'}</option><option>Autorizado</option><option>Pendiente</option><option>Rechazado</option></select>
          <button className="iv-btn iv-btn-secondary iv-btn-sm"><Icon name="filter" size={12} /> {lang === 'es' ? 'Filtros' : 'Filters'}</button>
        </div>
        <div className="iv-portal-table-wrap">
          <table className="iv-table iv-table-responsive">
            <thead><tr><th>{lang === 'es' ? 'Nº Factura' : 'Invoice #'}</th><th>{lang === 'es' ? 'Cliente' : 'Customer'}</th><th>{lang === 'es' ? 'Monto' : 'Amount'}</th><th>SRI</th><th>RIDE</th><th>{lang === 'es' ? 'Correo' : 'Email'}</th><th>{lang === 'es' ? 'Reintentos' : 'Retries'}</th><th>{lang === 'es' ? 'Hora' : 'Time'}</th><th></th></tr></thead>
            <tbody>
              {rows.map((r, i) => {
                const lab = { authorized: 'Autorizado', pending: 'Pendiente', rejected: 'Rechazado', sent: 'Enviado', failed: 'Fallido' };
                return (
                  <tr key={i}>
                    <td data-label={lang === 'es' ? 'Nº Factura' : 'Invoice #'} className="iv-mono" style={{ fontSize: 12 }}>{r.n}</td>
                    <td data-label={lang === 'es' ? 'Cliente' : 'Customer'}>{r.c}</td>
                    <td data-label={lang === 'es' ? 'Monto' : 'Amount'} className="iv-mono">${r.a.toFixed(2)}</td>
                    <td data-label="SRI"><StatusBadge status={r.sri} label={lab[r.sri]} pulse={r.sri === 'pending'} /></td>
                    <td data-label="RIDE"><StatusBadge status={r.ride} label={lab[r.ride]} /></td>
                    <td data-label={lang === 'es' ? 'Correo' : 'Email'}><StatusBadge status={r.mail} label={lab[r.mail]} /></td>
                    <td data-label={lang === 'es' ? 'Reintentos' : 'Retries'} className="iv-mono" style={{ color: r.tries > 1 ? 'var(--iv-warning)' : 'var(--iv-text-muted)' }}>{r.tries}×</td>
                    <td data-label={lang === 'es' ? 'Hora' : 'Time'} className="iv-mono" style={{ color: 'var(--iv-text-faint)', fontSize: 11.5 }}>{r.t}</td>
                    <td data-label="">
                      <div style={{ display: 'flex', gap: 4 }}>
                        <button className="iv-btn iv-btn-ghost iv-btn-sm" style={{ padding: 6, height: 26 }} title="PDF"><Icon name="download" size={12} /></button>
                        <button className="iv-btn iv-btn-ghost iv-btn-sm" style={{ padding: 6, height: 26 }} title="XML"><Icon name="copy" size={12} /></button>
                        <button className="iv-btn iv-btn-ghost iv-btn-sm" style={{ padding: 6, height: 26 }}><Icon name="more" size={12} /></button>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   INVENTORY
   ============================================================ */
function InventoryPage() {
  const { lang } = useLang();
  const rows = [
    { sku: 'CEM-50-HOL', n: 'Cemento Holcim 50kg', cat: 'Construcción', s: 1240, p: 8.50, st: 'ok', mov: '2h' },
    { sku: 'VAR-12-12', n: 'Varilla 12mm × 12m', cat: 'Construcción', s: 380, p: 14.20, st: 'ok', mov: '1h' },
    { sku: 'BLO-20-AL', n: 'Bloque alivianado 20cm', cat: 'Construcción', s: 18, p: 0.95, st: 'low', mov: '4h' },
    { sku: 'ARN-50', n: 'Arena gruesa saco 50kg', cat: 'Áridos', s: 0, p: 4.20, st: 'out', mov: '12h' },
    { sku: 'CLA-3PL', n: 'Clavo 3" funda 1lb', cat: 'Ferretería', s: 220, p: 2.10, st: 'ok', mov: '6h' },
    { sku: 'TUB-PVC-2', n: 'Tubo PVC 2"', cat: 'Plomería', s: 64, p: 7.80, st: 'low', mov: '1d' },
    { sku: 'PIN-LATEX', n: 'Pintura látex galón', cat: 'Pinturas', s: 142, p: 18.50, st: 'ok', mov: '3h' },
    { sku: 'CER-30X30', n: 'Cerámica 30×30 caja', cat: 'Acabados', s: 89, p: 22.00, st: 'ok', mov: '2d' },
  ];
  const stMap = { ok: { l: 'En stock', s: 'success' }, low: { l: 'Stock bajo', s: 'warning' }, out: { l: 'Sin stock', s: 'error' } };

  return (
    <div className="iv-fade-in">
      <PageHeader
        title={lang === 'es' ? 'Inventario' : 'Inventory'}
        subtitle={lang === 'es' ? '1,284 productos · 3 sucursales' : '1,284 products · 3 branches'}
        breadcrumb={[lang === 'es' ? 'Portal' : 'Portal', lang === 'es' ? 'Inventario' : 'Inventory']}
        actions={[
          <button key="1" className="iv-btn iv-btn-secondary iv-btn-sm"><Icon name="upload" size={12} /> {lang === 'es' ? 'Importar' : 'Import'}</button>,
          <button key="2" className="iv-btn iv-btn-secondary iv-btn-sm"><Icon name="download" size={12} /> {lang === 'es' ? 'Exportar' : 'Export'}</button>,
          <button key="3" className="iv-btn iv-btn-primary"><Icon name="plus" size={13} /> {lang === 'es' ? 'Nuevo producto' : 'New product'}</button>,
        ]}
      />

      <div className="iv-kpi-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: 14, marginBottom: 18 }}>
        <KpiCard label={lang === 'es' ? 'Total productos' : 'Total products'} value="1,284" icon="inventory" accent="#4F46E5" sparkData={[8,9,10,11,12,12,13]} />
        <KpiCard label={lang === 'es' ? 'En stock' : 'In stock'} value="1,182" icon="check" accent="#22C55E" sparkData={[7,8,9,10,11,11,12]} />
        <KpiCard label={lang === 'es' ? 'Stock bajo' : 'Low stock'} value="14" delta="+2" deltaPositive={false} icon="alert" accent="#F59E0B" sparkData={[5,6,7,8,9,12,14]} />
        <KpiCard label={lang === 'es' ? 'Sin stock' : 'Out of stock'} value="3" delta="0" icon="x" accent="#EF4444" sparkData={[3,3,3,3,3,3,3]} />
      </div>

      <div className="iv-card" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ padding: 14, display: 'flex', gap: 10, borderBottom: '1px solid var(--iv-divider)', flexWrap: 'wrap' }}>
          <input className="iv-input" placeholder={lang === 'es' ? 'Buscar SKU, nombre…' : 'Search SKU, name…'} style={{ flex: 1, height: 32, minWidth: 200 }} />
          <select className="iv-select" style={{ width: 140, height: 32 }}><option>{lang === 'es' ? 'Todas categorías' : 'All categories'}</option><option>Construcción</option><option>Ferretería</option></select>
          <select className="iv-select" style={{ width: 130, height: 32 }}><option>{lang === 'es' ? 'Todo estado' : 'All status'}</option><option>En stock</option><option>Stock bajo</option></select>
        </div>
        <div className="iv-portal-table-wrap">
          <table className="iv-table iv-table-responsive">
            <thead><tr><th>SKU</th><th>{lang === 'es' ? 'Producto' : 'Product'}</th><th>{lang === 'es' ? 'Categoría' : 'Category'}</th><th>Stock</th><th>{lang === 'es' ? 'Precio' : 'Price'}</th><th>{lang === 'es' ? 'Estado' : 'Status'}</th><th>{lang === 'es' ? 'Últ. movimiento' : 'Last move'}</th><th></th></tr></thead>
            <tbody>
              {rows.map((r, i) => (
                <tr key={i}>
                  <td data-label="SKU" className="iv-mono" style={{ fontSize: 11.5, color: 'var(--iv-text-muted)' }}>{r.sku}</td>
                  <td data-label={lang === 'es' ? 'Producto' : 'Product'} style={{ fontWeight: 500 }}>{r.n}</td>
                  <td data-label={lang === 'es' ? 'Categoría' : 'Category'}><span className="iv-badge iv-badge-neutral">{r.cat}</span></td>
                  <td data-label="Stock" className="iv-mono" style={{ fontWeight: 600 }}>{r.s.toLocaleString()}</td>
                  <td data-label={lang === 'es' ? 'Precio' : 'Price'} className="iv-mono">${r.p.toFixed(2)}</td>
                  <td data-label={lang === 'es' ? 'Estado' : 'Status'}><StatusBadge status={stMap[r.st].s} label={stMap[r.st].l} /></td>
                  <td data-label={lang === 'es' ? 'Últ. mov.' : 'Last move'} className="iv-mono" style={{ color: 'var(--iv-text-faint)', fontSize: 11.5 }}>hace {r.mov}</td>
                  <td data-label=""><button className="iv-btn iv-btn-ghost iv-btn-sm" style={{ padding: 6, height: 26 }}><Icon name="more" size={12} /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   SALES
   ============================================================ */
function SalesPage() {
  const { lang } = useLang();
  return (
    <div className="iv-fade-in">
      <PageHeader title={lang === 'es' ? 'Ventas' : 'Sales'} subtitle={lang === 'es' ? 'Histórico y métricas comerciales' : 'History & commercial metrics'} breadcrumb={[lang === 'es' ? 'Portal' : 'Portal', lang === 'es' ? 'Ventas' : 'Sales']} />
      <div className="iv-kpi-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: 14, marginBottom: 18 }}>
        <KpiCard label={lang === 'es' ? 'Ventas hoy' : 'Sales today'} value="$11,240" delta="+18%" icon="sales" accent="#4F46E5" sparkData={[3,5,4,6,7,8,11]} />
        <KpiCard label={lang === 'es' ? 'Tickets' : 'Tickets'} value="142" delta="+12%" icon="orders" accent="#22C55E" sparkData={[3,4,5,6,7,8,9]} />
        <KpiCard label={lang === 'es' ? 'Ticket promedio' : 'Avg. ticket'} value="$79.15" delta="+5%" icon="trending" accent="#38BDF8" sparkData={[5,6,5,7,6,8,8]} />
        <KpiCard label={lang === 'es' ? 'Top vendedor' : 'Top rep'} value="M. López" icon="customers" accent="#A78BFA" />
      </div>
      <div className="iv-card" style={{ padding: 22, marginBottom: 16 }}>
        <h3 style={{ margin: '0 0 4px', fontSize: 14.5, fontWeight: 600 }}>{lang === 'es' ? 'Ventas por día · 30 días' : 'Sales per day · 30 days'}</h3>
        <p style={{ margin: '0 0 20px', fontSize: 12, color: 'var(--iv-text-muted)' }}>USD</p>
        <AreaChart data={Array.from({ length: 30 }, (_, i) => 3000 + Math.sin(i / 3) * 1500 + Math.random() * 2500 + i * 80)} width={900} height={240} labels={Array.from({ length: 30 }, (_, i) => `${i + 1}`)} />
      </div>
      <div className="iv-card" style={{ padding: 0, overflow: 'hidden' }}>
        <div style={{ padding: '14px 18px', borderBottom: '1px solid var(--iv-divider)' }}>
          <h3 style={{ margin: 0, fontSize: 14, fontWeight: 600 }}>{lang === 'es' ? 'Ventas recientes' : 'Recent sales'}</h3>
        </div>
        <div className="iv-portal-table-wrap">
          <table className="iv-table iv-table-responsive">
            <thead><tr><th>#</th><th>{lang === 'es' ? 'Cliente' : 'Customer'}</th><th>{lang === 'es' ? 'Vendedor' : 'Rep'}</th><th>{lang === 'es' ? 'Items' : 'Items'}</th><th>{lang === 'es' ? 'Total' : 'Total'}</th><th>{lang === 'es' ? 'Estado' : 'Status'}</th><th>{lang === 'es' ? 'Hora' : 'Time'}</th></tr></thead>
            <tbody>
              {[
                { n: 'V-9012', c: 'Tienda Mi Barrio', v: 'M. López', i: 4, a: 182.50, s: 'success', sl: 'Completada', t: '14:21' },
                { n: 'V-9011', c: 'Cliente final', v: 'P. Suárez', i: 1, a: 24.90, s: 'success', sl: 'Completada', t: '13:42' },
                { n: 'V-9010', c: 'Constructora Andina', v: 'M. López', i: 12, a: 4397.40, s: 'success', sl: 'Completada', t: '13:18' },
                { n: 'V-9009', c: 'Comercial Pacífico', v: 'A. Vega', i: 3, a: 890.30, s: 'warning', sl: 'Pendiente pago', t: '12:55' },
                { n: 'V-9008', c: 'Ferretería El Sol', v: 'M. López', i: 2, a: 76.40, s: 'success', sl: 'Completada', t: '12:32' },
              ].map((r, i) => (
                <tr key={i}>
                  <td data-label="#" className="iv-mono" style={{ fontSize: 11.5 }}>{r.n}</td>
                  <td data-label={lang === 'es' ? 'Cliente' : 'Customer'}>{r.c}</td>
                  <td data-label={lang === 'es' ? 'Vendedor' : 'Rep'}><div style={{ display: 'flex', alignItems: 'center', gap: 8 }}><Avatar name={r.v} size={22} /> <span style={{ fontSize: 12.5 }}>{r.v}</span></div></td>
                  <td data-label={lang === 'es' ? 'Items' : 'Items'} className="iv-mono">{r.i}</td>
                  <td data-label={lang === 'es' ? 'Total' : 'Total'} className="iv-mono" style={{ fontWeight: 600 }}>${r.a.toFixed(2)}</td>
                  <td data-label={lang === 'es' ? 'Estado' : 'Status'}><StatusBadge status={r.s} label={r.sl} /></td>
                  <td data-label={lang === 'es' ? 'Hora' : 'Time'} className="iv-mono" style={{ color: 'var(--iv-text-faint)', fontSize: 11.5 }}>{r.t}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   ORDERS
   ============================================================ */
function OrdersPage() {
  const { lang } = useLang();
  return (
    <div className="iv-fade-in">
      <PageHeader title={lang === 'es' ? 'Órdenes' : 'Orders'} subtitle={lang === 'es' ? 'Pipeline de pedidos y entregas' : 'Order pipeline & deliveries'} breadcrumb={[lang === 'es' ? 'Portal' : 'Portal', lang === 'es' ? 'Órdenes' : 'Orders']} actions={[<button key="1" className="iv-btn iv-btn-primary"><Icon name="plus" size={13} /> {lang === 'es' ? 'Nueva orden' : 'New order'}</button>]} />
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: 14 }} className="iv-orders-kanban iv-dash-row">
        {[
          { t: lang === 'es' ? 'Nuevas' : 'New', n: 12, c: '#38BDF8', items: ['Constructora Andina · $4,397', 'Tienda Mi Barrio · $182', 'Cliente final · $24'] },
          { t: lang === 'es' ? 'En preparación' : 'Preparing', n: 8, c: '#F59E0B', items: ['Importadora Cuenca · $7,820', 'Ferretería El Sol · $76', 'Distribuidora Sur · $1,250'] },
          { t: lang === 'es' ? 'En entrega' : 'Delivering', n: 5, c: '#A78BFA', items: ['Supermercado Norte · $432', 'Comercial Pacífico · $890'] },
          { t: lang === 'es' ? 'Completadas' : 'Completed', n: 142, c: '#22C55E', items: ['Hoy · 142 órdenes', 'Total · $48,290'] },
        ].map((col, i) => (
          <div key={i} className="iv-card" style={{ padding: 16, minHeight: 360 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ width: 8, height: 8, borderRadius: 2, background: col.c }} />
                <span style={{ fontSize: 13, fontWeight: 600 }}>{col.t}</span>
              </div>
              <span className="iv-badge iv-badge-neutral">{col.n}</span>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              {col.items.map((it, j) => (
                <div key={j} style={{
                  padding: 10, background: 'var(--iv-surface-2)', borderRadius: 8,
                  border: '1px solid var(--iv-border)', fontSize: 12.5, lineHeight: 1.4,
                  borderLeft: `2px solid ${col.c}`
                }}>{it}</div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

/* ============================================================
   REPORTS
   ============================================================ */
function ReportsPage() {
  const { lang } = useLang();
  return (
    <div className="iv-fade-in">
      <PageHeader title={lang === 'es' ? 'Reportes' : 'Reports'} subtitle={lang === 'es' ? 'Reportes gerenciales y exportables' : 'Management & exportable reports'} breadcrumb={[lang === 'es' ? 'Portal' : 'Portal', lang === 'es' ? 'Reportes' : 'Reports']} actions={[<button key="1" className="iv-btn iv-btn-secondary"><Icon name="download" size={13} /> CSV</button>, <button key="2" className="iv-btn iv-btn-secondary"><Icon name="download" size={13} /> PDF</button>]} />
      <div className="iv-card" style={{ padding: 18, marginBottom: 16, display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
        <Icon name="filter" size={14} color="var(--iv-text-muted)" />
        <select className="iv-select" style={{ width: 180, height: 32 }}><option>{lang === 'es' ? 'Ventas' : 'Sales'}</option><option>{lang === 'es' ? 'Inventario' : 'Inventory'}</option><option>Vision AI</option><option>{lang === 'es' ? 'Facturación' : 'Invoicing'}</option></select>
        <input type="date" className="iv-input" style={{ width: 140, height: 32 }} defaultValue="2026-04-01" />
        <span style={{ color: 'var(--iv-text-faint)' }}>→</span>
        <input type="date" className="iv-input" style={{ width: 140, height: 32 }} defaultValue="2026-05-02" />
        <select className="iv-select" style={{ width: 160, height: 32 }}><option>{lang === 'es' ? 'Todas sucursales' : 'All branches'}</option><option>Centro</option><option>Norte</option><option>Sur</option></select>
        <button className="iv-btn iv-btn-primary iv-btn-sm" style={{ marginLeft: 'auto' }}><Icon name="refresh" size={12} /> {lang === 'es' ? 'Aplicar' : 'Apply'}</button>
      </div>
      <div className="iv-kpi-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: 14, marginBottom: 18 }}>
        <KpiCard label={lang === 'es' ? 'Ingresos totales' : 'Total revenue'} value="$184,392" delta="+9.4%" icon="trending" accent="#22C55E" sparkData={[4,5,6,7,7,8,9]} />
        <KpiCard label={lang === 'es' ? 'Tickets emitidos' : 'Tickets issued'} value="2,841" delta="+12%" icon="invoice" accent="#4F46E5" sparkData={[3,4,5,6,7,8,9]} />
        <KpiCard label={lang === 'es' ? 'Ticket promedio' : 'Avg. ticket'} value="$64.92" delta="-2%" deltaPositive={false} icon="sales" accent="#38BDF8" sparkData={[6,7,6,7,6,5,5]} />
        <KpiCard label={lang === 'es' ? 'Margen bruto' : 'Gross margin'} value="38.4%" delta="+1.2%" icon="reports" accent="#A78BFA" sparkData={[5,6,7,7,8,9,9]} />
      </div>
      <div className="iv-card" style={{ padding: 22 }}>
        <h3 style={{ margin: '0 0 16px', fontSize: 14.5, fontWeight: 600 }}>{lang === 'es' ? 'Ingresos · 30 días' : 'Revenue · 30 days'}</h3>
        <AreaChart data={Array.from({ length: 30 }, (_, i) => 4500 + Math.sin(i / 4) * 2000 + Math.random() * 1500 + i * 60)} width={900} height={260} />
      </div>
    </div>
  );
}

/* ============================================================
   CUSTOMERS
   ============================================================ */
function CustomersPage() {
  const { lang } = useLang();
  return (
    <div className="iv-fade-in">
      <PageHeader title={lang === 'es' ? 'Clientes' : 'Customers'} subtitle="284 clientes activos" breadcrumb={[lang === 'es' ? 'Portal' : 'Portal', lang === 'es' ? 'Clientes' : 'Customers']} actions={[<button key="1" className="iv-btn iv-btn-primary"><Icon name="plus" size={13} /> {lang === 'es' ? 'Nuevo cliente' : 'New customer'}</button>]} />
      <div className="iv-card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="iv-portal-table-wrap">
          <table className="iv-table iv-table-responsive">
            <thead><tr><th>{lang === 'es' ? 'Cliente' : 'Customer'}</th><th>RUC/CI</th><th>{lang === 'es' ? 'Ciudad' : 'City'}</th><th>{lang === 'es' ? 'Compras' : 'Purchases'}</th><th>{lang === 'es' ? 'Total' : 'Total'}</th><th>{lang === 'es' ? 'Última' : 'Last'}</th></tr></thead>
            <tbody>
              {[
                { n: 'Constructora Andina S.A.', r: '1791234567001', c: 'Cuenca', q: 42, t: 124390, l: '14:32' },
                { n: 'Distribuidora Sur', r: '1792341890001', c: 'Cuenca', q: 28, t: 38420, l: 'ayer' },
                { n: 'Ferretería El Sol', r: '1793456789001', c: 'Guayaquil', q: 86, t: 52180, l: '3d' },
                { n: 'Comercial Pacífico', r: '1794567890001', c: 'Manta', q: 12, t: 19200, l: '1 sem' },
                { n: 'Importadora Cuenca', r: '1795678901001', c: 'Cuenca', q: 8, t: 78400, l: '2 sem' },
                { n: 'Tienda Mi Barrio', r: '0912345678', c: 'Cuenca', q: 124, t: 18290, l: '14:21' },
              ].map((r, i) => (
                <tr key={i}>
                  <td data-label={lang === 'es' ? 'Cliente' : 'Customer'}><div style={{ display: 'flex', alignItems: 'center', gap: 10 }}><Avatar name={r.n} size={28} /> <span style={{ fontWeight: 500 }}>{r.n}</span></div></td>
                  <td data-label="RUC/CI" className="iv-mono" style={{ fontSize: 11.5, color: 'var(--iv-text-muted)' }}>{r.r}</td>
                  <td data-label={lang === 'es' ? 'Ciudad' : 'City'}>{r.c}</td>
                  <td data-label={lang === 'es' ? 'Compras' : 'Purchases'} className="iv-mono">{r.q}</td>
                  <td data-label={lang === 'es' ? 'Total' : 'Total'} className="iv-mono" style={{ fontWeight: 600 }}>${r.t.toLocaleString()}</td>
                  <td data-label={lang === 'es' ? 'Última' : 'Last'} className="iv-mono" style={{ color: 'var(--iv-text-faint)', fontSize: 11.5 }}>{r.l}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   USERS
   ============================================================ */
function UsersPage() {
  const { lang } = useLang();
  return (
    <div className="iv-fade-in">
      <PageHeader title={lang === 'es' ? 'Usuarios y roles' : 'Users & roles'} subtitle={lang === 'es' ? '12 usuarios · 4 roles' : '12 users · 4 roles'} breadcrumb={[lang === 'es' ? 'Portal' : 'Portal', lang === 'es' ? 'Usuarios' : 'Users']} actions={[<button key="1" className="iv-btn iv-btn-primary"><Icon name="plus" size={13} /> {lang === 'es' ? 'Invitar usuario' : 'Invite user'}</button>]} />
      <div className="iv-card" style={{ padding: 0, overflow: 'hidden' }}>
        <div className="iv-portal-table-wrap">
          <table className="iv-table iv-table-responsive">
            <thead><tr><th>{lang === 'es' ? 'Usuario' : 'User'}</th><th>{lang === 'es' ? 'Rol' : 'Role'}</th><th>{lang === 'es' ? 'Sucursal' : 'Branch'}</th><th>2FA</th><th>{lang === 'es' ? 'Último acceso' : 'Last login'}</th><th>{lang === 'es' ? 'Estado' : 'Status'}</th><th></th></tr></thead>
            <tbody>
              {[
                { n: 'Andrés Vega', e: 'andres@empresa.com', role: 'Admin', b: 'Todas', tfa: true, last: '2 min', s: 'active' },
                { n: 'María López', e: 'maria@empresa.com', role: 'Vendedor', b: 'Centro', tfa: true, last: '15 min', s: 'active' },
                { n: 'Pablo Suárez', e: 'pablo@empresa.com', role: 'Vendedor', b: 'Norte', tfa: false, last: '1h', s: 'active' },
                { n: 'Carla Mendoza', e: 'carla@empresa.com', role: 'Contador', b: 'Todas', tfa: true, last: 'ayer', s: 'active' },
                { n: 'Luis Cevallos', e: 'luis@empresa.com', role: 'Bodeguero', b: 'Sur', tfa: false, last: '3d', s: 'inactive' },
              ].map((u, i) => (
                <tr key={i}>
                  <td data-label={lang === 'es' ? 'Usuario' : 'User'}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                      <Avatar name={u.n} size={30} />
                      <div>
                        <div style={{ fontWeight: 500 }}>{u.n}</div>
                        <div className="iv-mono" style={{ fontSize: 11, color: 'var(--iv-text-faint)' }}>{u.e}</div>
                      </div>
                    </div>
                  </td>
                  <td data-label={lang === 'es' ? 'Rol' : 'Role'}><span className="iv-badge iv-badge-primary">{u.role}</span></td>
                  <td data-label={lang === 'es' ? 'Sucursal' : 'Branch'}>{u.b}</td>
                  <td data-label="2FA">{u.tfa ? <StatusBadge status="success" label="ON" /> : <StatusBadge status="neutral" label="OFF" />}</td>
                  <td data-label={lang === 'es' ? 'Último acceso' : 'Last login'} className="iv-mono" style={{ color: 'var(--iv-text-faint)', fontSize: 11.5 }}>hace {u.last}</td>
                  <td data-label={lang === 'es' ? 'Estado' : 'Status'}><StatusBadge status={u.s} label={u.s === 'active' ? (lang === 'es' ? 'Activo' : 'Active') : (lang === 'es' ? 'Inactivo' : 'Inactive')} /></td>
                  <td data-label=""><button className="iv-btn iv-btn-ghost iv-btn-sm" style={{ padding: 6, height: 26 }}><Icon name="more" size={12} /></button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

/* ============================================================
   SETTINGS
   ============================================================ */
function SettingsPage() {
  const { lang } = useLang();
  const [tab, setTab] = useStateD('apikeys');
  const tabs = [
    { id: 'business', l: lang === 'es' ? 'Negocio' : 'Business', icon: 'building' },
    { id: 'branches', l: lang === 'es' ? 'Sucursales' : 'Branches', icon: 'pin' },
    { id: 'apikeys', l: 'API Keys Vision', icon: 'key' },
    { id: 'resend', l: 'Resend (correo)', icon: 'mail' },
    { id: 'sri', l: 'SRI', icon: 'invoice' },
    { id: 'security', l: lang === 'es' ? 'Seguridad' : 'Security', icon: 'shield' },
  ];

  return (
    <div className="iv-fade-in">
      <PageHeader title={lang === 'es' ? 'Configuración' : 'Settings'} subtitle={lang === 'es' ? 'Datos del negocio, integraciones y seguridad' : 'Business data, integrations & security'} breadcrumb={[lang === 'es' ? 'Portal' : 'Portal', lang === 'es' ? 'Configuración' : 'Settings']} />
      <div style={{ display: 'grid', gridTemplateColumns: '220px 1fr', gap: 16 }} className="iv-settings-grid">
        <div className="iv-card" style={{ padding: 8, height: 'fit-content' }}>
          {tabs.map(t => (
            <button key={t.id} onClick={() => setTab(t.id)} style={{
              display: 'flex', alignItems: 'center', gap: 10, width: '100%',
              padding: '8px 10px', borderRadius: 6, border: 0, cursor: 'pointer',
              background: tab === t.id ? 'rgba(79,70,229,0.12)' : 'transparent',
              color: tab === t.id ? 'var(--iv-accent-soft)' : 'var(--iv-text-muted)',
              fontFamily: 'var(--iv-font-sans)', fontSize: 13, fontWeight: tab === t.id ? 500 : 400, textAlign: 'left'
            }}>
              <Icon name={t.icon} size={14} />
              <span>{t.l}</span>
            </button>
          ))}
        </div>
        <div>
          {tab === 'apikeys' && <ApiKeysPanel />}
          {tab === 'resend' && <ResendPanel />}
          {tab === 'business' && <SimpleSettingsPanel title={lang === 'es' ? 'Datos del negocio' : 'Business data'} fields={['Razón social', 'RUC', 'Dirección fiscal', 'Teléfono', 'Logo']} />}
          {tab === 'branches' && <SimpleSettingsPanel title={lang === 'es' ? 'Sucursales' : 'Branches'} fields={['Centro · Cuenca', 'Norte · Cuenca', 'Sur · Guayaquil']} addLabel={lang === 'es' ? '+ Añadir sucursal' : '+ Add branch'} />}
          {tab === 'sri' && <SimpleSettingsPanel title="SRI · Facturación electrónica" fields={['Ambiente: Producción', 'Tipo emisión: Normal', 'Firma electrónica .p12', 'Secuenciales']} />}
          {tab === 'security' && <SimpleSettingsPanel title={lang === 'es' ? 'Seguridad' : 'Security'} fields={['2FA obligatorio: ON', 'Sesión máx: 8h', 'IP allowlist', 'Auditoría de accesos']} />}
        </div>
      </div>
    </div>
  );
}

function ApiKeysPanel() {
  const { lang } = useLang();
  const toast = useToast();
  const [keys, setKeys] = useStateD([
    { id: 1, name: 'YOLO Producción', prefix: 'iv_yolo_8K2x...', state: 'active', last: '2 min', created: '2026-04-12' },
    { id: 2, name: 'Cámaras Centro', prefix: 'iv_cam_pQ4n...', state: 'active', last: '15 min', created: '2026-03-08' },
    { id: 3, name: 'Webhook Eventos', prefix: 'iv_evt_R9mB...', state: 'inactive', last: '12d', created: '2026-01-22' },
  ]);
  const [showCreate, setShowCreate] = useStateD(false);
  const [newName, setNewName] = useStateD('');
  const [createdKey, setCreatedKey] = useStateD(null);

  const create = () => {
    if (!newName) return;
    const key = `iv_${Math.random().toString(36).slice(2, 26)}`;
    setCreatedKey(key);
    setKeys(k => [{ id: Date.now(), name: newName, prefix: key.slice(0, 11) + '...', state: 'active', last: '—', created: new Date().toISOString().slice(0, 10) }, ...k]);
    setNewName('');
  };

  return (
    <div className="iv-card" style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 6, flexWrap: 'wrap', gap: 8 }}>
        <div>
          <h3 style={{ margin: 0, fontSize: 16, fontWeight: 600 }}>API Keys · Vision AI</h3>
          <p style={{ margin: '4px 0 0', fontSize: 13, color: 'var(--iv-text-muted)' }}>{lang === 'es' ? 'Claves para tus cámaras y webhooks. La clave completa solo se muestra una vez.' : 'Keys for cameras and webhooks. Full key shown only once.'}</p>
        </div>
        <button onClick={() => setShowCreate(true)} className="iv-btn iv-btn-primary"><Icon name="plus" size={13} /> {lang === 'es' ? 'Crear nueva' : 'Create new'}</button>
      </div>
      <div style={{ marginTop: 22 }}>
        <div className="iv-portal-table-wrap">
          <table className="iv-table iv-table-responsive">
            <thead><tr><th>{lang === 'es' ? 'Nombre' : 'Name'}</th><th>{lang === 'es' ? 'Prefijo' : 'Prefix'}</th><th>{lang === 'es' ? 'Estado' : 'Status'}</th><th>{lang === 'es' ? 'Último uso' : 'Last used'}</th><th>{lang === 'es' ? 'Creada' : 'Created'}</th><th></th></tr></thead>
            <tbody>
              {keys.map(k => (
                <tr key={k.id}>
                  <td data-label={lang === 'es' ? 'Nombre' : 'Name'} style={{ fontWeight: 500 }}>{k.name}</td>
                  <td data-label={lang === 'es' ? 'Prefijo' : 'Prefix'} className="iv-mono" style={{ fontSize: 12 }}>{k.prefix}</td>
                  <td data-label={lang === 'es' ? 'Estado' : 'Status'}><StatusBadge status={k.state === 'active' ? 'success' : 'neutral'} label={k.state === 'active' ? (lang === 'es' ? 'Activa' : 'Active') : (lang === 'es' ? 'Revocada' : 'Revoked')} /></td>
                  <td data-label={lang === 'es' ? 'Último uso' : 'Last used'} className="iv-mono" style={{ color: 'var(--iv-text-faint)', fontSize: 11.5 }}>{k.last}</td>
                  <td data-label={lang === 'es' ? 'Creada' : 'Created'} className="iv-mono" style={{ color: 'var(--iv-text-faint)', fontSize: 11.5 }}>{k.created}</td>
                  <td data-label="">
                    {k.state === 'active' && <button onClick={() => setKeys(arr => arr.map(x => x.id === k.id ? { ...x, state: 'inactive' } : x))} className="iv-btn iv-btn-ghost iv-btn-sm" style={{ color: 'var(--iv-error)' }}>{lang === 'es' ? 'Revocar' : 'Revoke'}</button>}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <Modal open={showCreate} onClose={() => { setShowCreate(false); setCreatedKey(null); }} title={createdKey ? (lang === 'es' ? 'API Key creada' : 'API Key created') : (lang === 'es' ? 'Nueva API Key' : 'New API Key')}>
        {!createdKey ? (
          <div>
            <label className="iv-label">{lang === 'es' ? 'Nombre de la clave' : 'Key name'}</label>
            <input className="iv-input" value={newName} onChange={e => setNewName(e.target.value)} placeholder={lang === 'es' ? 'Ej. Cámaras Norte' : 'e.g. North cameras'} />
            <div style={{ marginTop: 16, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
              <button onClick={() => setShowCreate(false)} className="iv-btn iv-btn-secondary">{lang === 'es' ? 'Cancelar' : 'Cancel'}</button>
              <button onClick={create} className="iv-btn iv-btn-primary">{lang === 'es' ? 'Crear' : 'Create'}</button>
            </div>
          </div>
        ) : (
          <div>
            <div style={{ padding: 12, background: 'var(--iv-warning-bg)', border: '1px solid rgba(245,158,11,0.3)', borderRadius: 8, marginBottom: 14, display: 'flex', gap: 10 }}>
              <Icon name="alert" size={14} color="var(--iv-warning)" />
              <p style={{ margin: 0, fontSize: 12, color: 'var(--iv-text)' }}>{lang === 'es' ? 'Guarda esta clave ahora. No la podrás ver de nuevo.' : 'Save this key now. You won\'t see it again.'}</p>
            </div>
            <div style={{ padding: 14, background: 'var(--iv-bg)', border: '1px solid var(--iv-border-strong)', borderRadius: 8, fontFamily: 'var(--iv-font-mono)', fontSize: 12, wordBreak: 'break-all', color: 'var(--iv-accent-soft)' }}>
              {createdKey}
            </div>
            <div style={{ marginTop: 16, display: 'flex', justifyContent: 'flex-end', gap: 8 }}>
              <button onClick={() => { navigator.clipboard?.writeText(createdKey); toast({ type: 'success', icon: 'check', message: 'Key copied' }); }} className="iv-btn iv-btn-secondary"><Icon name="copy" size={13} /> {lang === 'es' ? 'Copiar' : 'Copy'}</button>
              <button onClick={() => { setShowCreate(false); setCreatedKey(null); }} className="iv-btn iv-btn-primary">{lang === 'es' ? 'Listo' : 'Done'}</button>
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
}

function ResendPanel() {
  const { lang } = useLang();
  const toast = useToast();
  return (
    <div className="iv-card" style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 20, flexWrap: 'wrap', gap: 8 }}>
        <div>
          <h3 style={{ margin: 0, fontSize: 16, fontWeight: 600 }}>Resend · {lang === 'es' ? 'Correo transaccional' : 'Transactional email'}</h3>
          <p style={{ margin: '4px 0 0', fontSize: 13, color: 'var(--iv-text-muted)' }}>{lang === 'es' ? 'Envío de facturas, notificaciones y reportes.' : 'Invoice, notification & report delivery.'}</p>
        </div>
        <StatusBadge status="success" label={lang === 'es' ? 'Conectado' : 'Connected'} pulse />
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14, marginBottom: 20 }} className="iv-dash-row">
        <div><label className="iv-label">From email</label><input className="iv-input" defaultValue="soporte@insightvisionia.cloud" /></div>
        <div><label className="iv-label">From name</label><input className="iv-input" defaultValue="InsightVision" /></div>
        <div><label className="iv-label">Reply-To</label><input className="iv-input" defaultValue="contacto@insightvisionia.cloud" /></div>
        <div><label className="iv-label">{lang === 'es' ? 'Dominio verificado' : 'Verified domain'}</label><input className="iv-input" defaultValue="insightvisionia.cloud" disabled style={{ opacity: 0.7 }} /></div>
      </div>
      <div style={{ display: 'flex', gap: 8, marginBottom: 24, flexWrap: 'wrap' }}>
        <button onClick={() => toast({ type: 'success', icon: 'mail', title: 'Test sent', message: 'Email delivered to your inbox.' })} className="iv-btn iv-btn-secondary"><Icon name="send" size={13} /> {lang === 'es' ? 'Enviar prueba' : 'Send test'}</button>
        <button className="iv-btn iv-btn-primary">{lang === 'es' ? 'Guardar cambios' : 'Save changes'}</button>
      </div>
      <h4 style={{ margin: '0 0 12px', fontSize: 13, fontWeight: 600, color: 'var(--iv-text-muted)' }}>{lang === 'es' ? 'Últimos envíos' : 'Recent logs'}</h4>
      <div style={{ border: '1px solid var(--iv-border)', borderRadius: 8, overflow: 'hidden' }}>
        <div className="iv-portal-table-wrap">
          <table className="iv-table iv-table-responsive">
            <thead><tr><th>{lang === 'es' ? 'Hora' : 'Time'}</th><th>{lang === 'es' ? 'Destinatario' : 'To'}</th><th>{lang === 'es' ? 'Asunto' : 'Subject'}</th><th>{lang === 'es' ? 'Estado' : 'Status'}</th></tr></thead>
            <tbody>
              {[
                { t: '14:32', to: 'contacto@constructoraandina.ec', s: 'Factura 001-001-0000004287', st: 'sent' },
                { t: '14:18', to: 'admin@distribuidorasur.com', s: 'Factura 001-001-0000004286', st: 'sent' },
                { t: '13:42', to: 'pacifico@correo.com', s: 'Factura 001-001-0000004284', st: 'failed' },
                { t: '13:21', to: 'tienda.mibarrio@gmail.com', s: 'Factura 001-001-0000004283', st: 'sent' },
              ].map((r, i) => (
                <tr key={i}>
                  <td data-label={lang === 'es' ? 'Hora' : 'Time'} className="iv-mono" style={{ fontSize: 11.5, color: 'var(--iv-text-muted)' }}>{r.t}</td>
                  <td data-label={lang === 'es' ? 'Destinatario' : 'To'} className="iv-mono" style={{ fontSize: 12 }}>{r.to}</td>
                  <td data-label={lang === 'es' ? 'Asunto' : 'Subject'}>{r.s}</td>
                  <td data-label={lang === 'es' ? 'Estado' : 'Status'}><StatusBadge status={r.st} label={r.st === 'sent' ? (lang === 'es' ? 'Entregado' : 'Delivered') : (lang === 'es' ? 'Falló' : 'Failed')} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

function SimpleSettingsPanel({ title, fields, addLabel }) {
  return (
    <div className="iv-card" style={{ padding: 24 }}>
      <h3 style={{ margin: '0 0 18px', fontSize: 16, fontWeight: 600 }}>{title}</h3>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
        {fields.map((f, i) => (
          <div key={i} style={{
            padding: 14, background: 'var(--iv-surface-2)', border: '1px solid var(--iv-border)',
            borderRadius: 8, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8
          }}>
            <span style={{ fontSize: 13.5 }}>{f}</span>
            <button className="iv-btn iv-btn-ghost iv-btn-sm"><Icon name="edit" size={12} /></button>
          </div>
        ))}
        {addLabel && <button className="iv-btn iv-btn-secondary" style={{ marginTop: 6 }}>{addLabel}</button>}
      </div>
    </div>
  );
}

Object.assign(window, { DashboardPage, VisionPage, InvoicingPage, InventoryPage, SalesPage, OrdersPage, ReportsPage, CustomersPage, UsersPage, SettingsPage });
