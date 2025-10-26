(function(){
  window.tailwind = window.tailwind || {};
  window.tailwind.config = {
    darkMode: 'class',
    theme: {
      extend: {
        colors: {
          brand: '#a30606',
          brandDark: '#1d1d1d',
          brandLight: '#f04e4e',
          neutral900: '#1d1d1d'
        },
        fontFamily: {
          sans: ['Inter', 'system-ui', 'sans-serif']
        },
        container: { center: true, padding: '1rem' }
      }
    }
  };

  const ui = {};
  const ensureRoot = () => {
    let root = document.getElementById('toast-root');
    if (!root){
      root = document.createElement('div');
      root.id = 'toast-root';
      root.className = 'fixed top-4 right-4 z-[9999] space-y-2';
      document.body.appendChild(root);
    }
    return root;
  };

  ui.toast = (msg, type='info') => {
    const root = ensureRoot();
    const colors = {
      success: 'bg-green-50 text-green-800 border-green-200',
      error: 'bg-red-50 text-red-800 border-red-200',
      warning: 'bg-yellow-50 text-yellow-800 border-yellow-200',
      info: 'bg-blue-50 text-blue-800 border-blue-200'
    };
    const div = document.createElement('div');
    div.className = `border px-4 py-3 rounded-xl shadow ${colors[type]||colors.info}`;
    div.setAttribute('role','alert');
    div.innerHTML = `<div class="flex items-center gap-2"><span class="flex-1">${msg}</span><button class="text-sm opacity-70 hover:opacity-100" aria-label="Cerrar">✕</button></div>`;
    div.querySelector('button').onclick = () => div.remove();
    root.appendChild(div);
    if (window.gsap){
      gsap.from(div,{x:60,opacity:0,duration:.25,ease:'power2.out'});
      setTimeout(()=>{ if (!div.isConnected) return; gsap.to(div,{x:60,opacity:0,duration:.2,onComplete:()=>div.remove()}); }, 4000);
    } else {
      setTimeout(()=>div.remove(),4000);
    }
  };

  let overlay = null;
  ui.loading = (on=true) => {
    if (on){
      if (!overlay){
        overlay = document.createElement('div');
        overlay.className = 'fixed inset-0 z-[9998] grid place-items-center bg-black/30 backdrop-blur-sm';
        overlay.innerHTML = '<div class="w-10 h-10 rounded-full border-4 border-white border-t-transparent animate-spin"></div>';
      }
      document.body.appendChild(overlay);
    } else if (overlay && overlay.parentNode) {
      overlay.parentNode.removeChild(overlay);
    }
  };

  ui.confirm = (msg) => Promise.resolve(window.confirm(msg));

  ui.animIn = (el, variant='fade') => {
    if (!window.gsap || !el) return;
    const t = {fade:{opacity:0}, slide:{y:16,opacity:0}}[variant]||{opacity:0};
    gsap.from(el,{...t,duration:.35,ease:'power2.out'});
  };

  // subtle animated background gradient using CSS variables
  ui.animateBackground = () => {
    if (!window.gsap) return;
    const body = document.body;
    let hue = 0;
    gsap.to({}, {duration: 20, repeat: -1, onUpdate(){
      hue = (hue + 0.2) % 360;
      body.style.backgroundImage = `linear-gradient(180deg, hsl(${hue}, 20%, 96%) 0%, #ffffff 100%)`;
    }});
  };

  ui.setTheme = (mode) => {
    const html = document.documentElement;
    if (mode === 'dark') html.classList.add('dark');
    else html.classList.remove('dark');
    localStorage.setItem('theme', mode);
  };
  ui.initTheme = () => {
    const saved = localStorage.getItem('theme');
    if (saved) ui.setTheme(saved);
  };

  window.ui = ui;
})();
