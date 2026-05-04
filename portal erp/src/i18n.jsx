/* i18n.jsx — bilingual ES/EN strings + LangContext */

const STRINGS = {
  es: {
    // Nav
    'nav.home': 'Inicio',
    'nav.features': 'Funcionalidades',
    'nav.vision': 'Vision AI',
    'nav.invoicing': 'Facturación',
    'nav.pricing': 'Precios',
    'nav.contact': 'Contacto',
    'nav.demo': 'Solicitar demo',
    'nav.login': 'Iniciar sesión',

    // Hero
    'hero.eyebrow': 'ERP + Vision AI · Multi-tenant',
    'hero.title.pre': 'La plataforma para controlar',
    'hero.subtitle': 'Inventario, ventas, facturación electrónica SRI y visión artificial con cámaras, en una sola plataforma diseñada para negocios serios.',
    'hero.cta.primary': 'Solicitar demo',
    'hero.cta.secondary': 'Ver funcionalidades',
    'hero.trust': 'Construido para negocios en Ecuador · SRI · Resend · YOLO',

    // Modules
    'modules.eyebrow': 'Una plataforma. Todos los módulos.',
    'modules.title': 'Todo lo que tu operación necesita',
    'modules.subtitle': 'Nueve módulos que se hablan entre sí. Cero hojas de cálculo. Cero parches.',

    // Vision AI
    'vision.eyebrow': 'El módulo diferenciador',
    'vision.title': 'Vision AI con tus cámaras existentes',
    'vision.subtitle': 'Conecta cámaras IP, recibe detecciones de YOLO en tiempo real y convierte cada frame en una métrica accionable.',

    // Invoicing
    'inv.eyebrow': 'Facturación electrónica SRI',
    'inv.title': 'Emite, autoriza y entrega — sin pestañas extras',
    'inv.subtitle': 'XML, RIDE PDF, envío automático por correo y reintentos inteligentes. Estado SRI siempre visible.',

    // Pricing
    'price.eyebrow': 'Planes',
    'price.title': 'Crece a tu ritmo',
    'price.subtitle': 'Sin precios sorpresa. Hablamos contigo, entendemos tu operación, te proponemos.',
    'price.starter': 'Starter',
    'price.business': 'Business',
    'price.enterprise': 'Enterprise',
    'price.cta': 'Solicitar demo',
    'price.from': 'Desde',
    'price.consult': 'Consultar precio',
    'price.custom': 'Personalizado',

    // Contact
    'contact.eyebrow': 'Hablemos',
    'contact.title': 'Solicita tu demo',
    'contact.subtitle': 'Esto no crea una cuenta automáticamente. Te contactamos en menos de 24h hábiles para conocer tu negocio.',
    'contact.name': 'Nombre completo',
    'contact.company': 'Empresa',
    'contact.email': 'Correo corporativo',
    'contact.phone': 'Teléfono',
    'contact.city': 'Ciudad',
    'contact.business': 'Tipo de negocio',
    'contact.branches': 'Número de sucursales',
    'contact.interest': 'Interés principal',
    'contact.message': 'Cuéntanos sobre tu operación',
    'contact.cta': 'Enviar solicitud',
    'contact.disclaimer': 'No registramos cuentas desde este formulario. Es solo contacto comercial.',

    // Login
    'login.title': 'Accede a tu portal',
    'login.subtitle': 'Plataforma privada para usuarios autorizados.',
    'login.email': 'Correo corporativo',
    'login.password': 'Contraseña',
    'login.cta': 'Iniciar sesión',
    'login.google': 'Continuar con Google',
    'login.microsoft': 'Continuar con Microsoft',
    'login.demo': '¿No tienes cuenta? Solicita una demo',
    'login.support': 'Contactar soporte',
    'login.notice': 'El acceso está disponible solo para usuarios autorizados por su organización.',
    'login.unauthorized.title': 'Acceso no autorizado',
    'login.unauthorized.body': 'Tu correo no está asociado a ningún negocio activo en InsightVision. Solicita una demo o contacta a soporte para activar tu acceso.',
    'login.unauthorized.back': 'Volver al login',

    // Portal
    'portal.dashboard': 'Dashboard',
    'portal.inventory': 'Inventario',
    'portal.products': 'Productos',
    'portal.sales': 'Ventas',
    'portal.orders': 'Órdenes',
    'portal.invoicing': 'Facturación',
    'portal.vision': 'Vision AI',
    'portal.reports': 'Reportes',
    'portal.customers': 'Clientes',
    'portal.users': 'Usuarios',
    'portal.settings': 'Configuración',
    'portal.search': 'Buscar productos, facturas, clientes…',
    'portal.branch': 'Sucursal',
    'portal.allBranches': 'Todas las sucursales',
    'portal.profile': 'Perfil',
    'portal.logout': 'Cerrar sesión',
    'portal.welcome': 'Buen día',
    'portal.overview': 'Resumen general · {{date}}',

    'kpi.salesToday': 'Ventas del día',
    'kpi.monthIncome': 'Ingresos del mes',
    'kpi.lowStock': 'Productos bajo stock',
    'kpi.invoices': 'Facturas emitidas',
    'kpi.emails': 'Correos enviados',
    'kpi.events': 'Eventos Vision AI',
    'kpi.pendingOrders': 'Órdenes pendientes',
    'kpi.alerts': 'Alertas activas',
  },
  en: {
    'nav.home': 'Home', 'nav.features': 'Features', 'nav.vision': 'Vision AI',
    'nav.invoicing': 'Invoicing', 'nav.pricing': 'Pricing', 'nav.contact': 'Contact',
    'nav.demo': 'Request demo', 'nav.login': 'Sign in',

    'hero.eyebrow': 'ERP + Vision AI · Multi-tenant',
    'hero.title.pre': 'The platform to control',
    'hero.subtitle': 'Inventory, sales, electronic invoicing and computer vision with cameras, in a single platform built for serious businesses.',
    'hero.cta.primary': 'Request demo',
    'hero.cta.secondary': 'See features',
    'hero.trust': 'Built for businesses in Ecuador · SRI · Resend · YOLO',

    'modules.eyebrow': 'One platform. Every module.',
    'modules.title': 'Everything your operation needs',
    'modules.subtitle': 'Nine modules that talk to each other. Zero spreadsheets. Zero patches.',

    'vision.eyebrow': 'The differentiator',
    'vision.title': 'Vision AI with your existing cameras',
    'vision.subtitle': 'Connect IP cameras, get real-time YOLO detections and turn every frame into an actionable metric.',

    'inv.eyebrow': 'SRI electronic invoicing',
    'inv.title': 'Issue, authorize and deliver — no extra tabs',
    'inv.subtitle': 'XML, RIDE PDF, automatic email delivery and smart retries. SRI status always visible.',

    'price.eyebrow': 'Plans', 'price.title': 'Grow at your pace',
    'price.subtitle': 'No surprise pricing. We talk to you, understand your operation, propose accordingly.',
    'price.starter': 'Starter', 'price.business': 'Business', 'price.enterprise': 'Enterprise',
    'price.cta': 'Request demo', 'price.from': 'From', 'price.consult': 'Get a quote', 'price.custom': 'Custom',

    'contact.eyebrow': 'Let\'s talk', 'contact.title': 'Request your demo',
    'contact.subtitle': 'This does not create an account automatically. We reach out within 24 business hours to learn about your business.',
    'contact.name': 'Full name', 'contact.company': 'Company',
    'contact.email': 'Work email', 'contact.phone': 'Phone',
    'contact.city': 'City', 'contact.business': 'Business type',
    'contact.branches': 'Number of locations', 'contact.interest': 'Primary interest',
    'contact.message': 'Tell us about your operation', 'contact.cta': 'Send request',
    'contact.disclaimer': 'We don\'t register accounts from this form. Sales contact only.',

    'login.title': 'Access your portal', 'login.subtitle': 'Private platform for authorized users.',
    'login.email': 'Work email', 'login.password': 'Password',
    'login.cta': 'Sign in', 'login.google': 'Continue with Google',
    'login.microsoft': 'Continue with Microsoft', 'login.demo': 'No account? Request a demo',
    'login.support': 'Contact support', 'login.notice': 'Access is available only to users authorized by their organization.',
    'login.unauthorized.title': 'Access not authorized',
    'login.unauthorized.body': 'Your email is not linked to any active InsightVision business. Request a demo or contact support to activate access.',
    'login.unauthorized.back': 'Back to login',

    'portal.dashboard': 'Dashboard', 'portal.inventory': 'Inventory',
    'portal.products': 'Products', 'portal.sales': 'Sales', 'portal.orders': 'Orders',
    'portal.invoicing': 'Invoicing', 'portal.vision': 'Vision AI', 'portal.reports': 'Reports',
    'portal.customers': 'Customers', 'portal.users': 'Users', 'portal.settings': 'Settings',
    'portal.search': 'Search products, invoices, customers…',
    'portal.branch': 'Branch', 'portal.allBranches': 'All branches',
    'portal.profile': 'Profile', 'portal.logout': 'Sign out',
    'portal.welcome': 'Good morning', 'portal.overview': 'Overview · {{date}}',

    'kpi.salesToday': 'Sales today', 'kpi.monthIncome': 'Monthly revenue',
    'kpi.lowStock': 'Low stock items', 'kpi.invoices': 'Invoices issued',
    'kpi.emails': 'Emails sent', 'kpi.events': 'Vision AI events',
    'kpi.pendingOrders': 'Pending orders', 'kpi.alerts': 'Active alerts',
  }
};

const LangCtx = React.createContext({ lang: 'es', setLang: () => {}, t: (k) => k });

function LangProvider({ children, defaultLang = 'es' }) {
  const [lang, setLangState] = React.useState(() => localStorage.getItem('iv-lang') || defaultLang);
  const setLang = (l) => { setLangState(l); localStorage.setItem('iv-lang', l); };
  const t = React.useCallback((key, vars = {}) => {
    let s = (STRINGS[lang] && STRINGS[lang][key]) || STRINGS.es[key] || key;
    Object.keys(vars).forEach(k => { s = s.replace(`{{${k}}}`, vars[k]); });
    return s;
  }, [lang]);
  return <LangCtx.Provider value={{ lang, setLang, t }}>{children}</LangCtx.Provider>;
}
const useLang = () => React.useContext(LangCtx);

window.LangCtx = LangCtx;
window.LangProvider = LangProvider;
window.useLang = useLang;
