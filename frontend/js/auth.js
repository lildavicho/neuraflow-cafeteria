// Auth helper for login page using Axios-based api client
(function(){
  async function doLogin(e){
    e && e.preventDefault && e.preventDefault();
    const email = document.getElementById('email')?.value?.trim();
    const password = document.getElementById('password')?.value;
    const require2FA = document.getElementById('require2FA')?.checked || false;

    const btn = document.querySelector('#loginForm button[type="submit"]');
    const btnText = document.getElementById('loginBtnText');
    const spinner = document.getElementById('loginSpinner');
    try{
      if (btn) btn.disabled = true;
      if (btnText) btnText.textContent = 'Iniciando...';
      if (spinner) spinner.classList.remove('d-none');

      // Prefer global axios instance if present
      if (window.api){
        const { data } = await window.api.post('/auth/login', { email, password, require2FA });
        handleAuthResponse(data);
      } else if (window.fetch){
        const res = await fetch('/api/auth/login', {
          method: 'POST', headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ email, password, require2FA })
        });
        const data = await res.json();
        if (!res.ok){ throw new Error(data.message || 'Credenciales inválidas'); }
        handleAuthResponse(data);
      }
    }catch(err){
      const msg = err?.response?.data?.message || err?.message || 'Error al iniciar sesión';
      const el = document.getElementById('loginError');
      if (el) el.textContent = msg;
    }finally{
      if (btn) btn.disabled = false;
      if (btnText) btnText.textContent = 'Iniciar Sesión';
      if (spinner) spinner.classList.add('d-none');
    }
  }

  function handleAuthResponse(data){
    if (data.requires2FA){
      const modalEl = document.getElementById('twoFactorModal');
      if (modalEl && window.bootstrap){
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
        localStorage.setItem('temp_email', document.getElementById('email')?.value?.trim() || '');
      }
      return;
    }
    localStorage.setItem('token', data.token);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('user', JSON.stringify({ id: data.userId, email: data.email, name: data.fullName, role: data.role }));
    const target = data.role === 'ADMIN' ? '/pages/dashboard.html' : '/pages/pos.html';
    location.href = target;
  }

  // Wire up form if present
  document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('loginForm');
    if (form) form.addEventListener('submit', doLogin);
  });

  // Expose
  window.doLogin = doLogin;
})();
