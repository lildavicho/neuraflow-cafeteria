// Dashboard interactions: KPIs, low stock table, basic search
(function(){
  const $ = (sel)=>document.querySelector(sel);

  function setUserInfo(){
    try{
      const user = JSON.parse(localStorage.getItem('user')||'null');
      if (user){
        const el = document.getElementById('userName');
        if (el) el.textContent = user.name || user.email;
      }
    }catch(e){/* noop */}
  }

  function onLogout(){
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    location.href = '/pages/login.html';
  }

  async function loadKPIs(){
    try{
      // Low stock count
      const low = await window.api.get('/products/low-stock');
      const count = (low.data || low).length;
      animateNumber($('#kpiLowStock'), count);
      const now = new Date();
      $('#kpiUpdatedAt').textContent = now.toLocaleTimeString();
    }catch(e){ console.warn('Low stock KPI error', e); }

    try{
      // Sales summary (24h)
      const { data } = await window.api.get('/sales/summary?period=daily');
      const totalSales = data?.totalSales ?? 0;
      const totalCount = data?.totalCount ?? 0;
      animateNumber($('#kpiSales24h'), totalSales, true);
      animateNumber($('#kpiOrders24h'), totalCount);
    }catch(e){ console.warn('Sales KPI error', e); }
  }

  function animateNumber(el, target, isCurrency = false) {
    if (!el || !window.gsap) {
      if (el) el.textContent = isCurrency ? `$${Number(target).toFixed(2)}` : target;
      return;
    }
    const obj = { val: 0 };
    gsap.to(obj, {
      val: target,
      duration: 1.5,
      ease: 'power2.out',
      onUpdate: () => {
        el.textContent = isCurrency ? `$${obj.val.toFixed(2)}` : Math.round(obj.val);
      }
    });
  }

  function renderLowStock(rows){
    const tbody = $('#lowStockBody');
    if (!tbody) return;
    tbody.innerHTML = (rows||[]).map(p=>`
      <tr class="table-row-anim">
        <td class="px-4 py-2">${p.code || ''}</td>
        <td class="px-4 py-2 font-medium">${p.name}</td>
        <td class="px-4 py-2 text-gray-600">${p.categoryName || (p.category?.name||'')}</td>
        <td class="px-4 py-2 text-right font-semibold">$${Number(p.price).toFixed(2)}</td>
        <td class="px-4 py-2 text-right">
          <span class="px-2 py-1 rounded-full text-xs font-medium ${
            p.stock <= 5 ? 'bg-red-100 text-red-700' : 'bg-yellow-100 text-yellow-700'
          }">${p.stock}</span>
        </td>
      </tr>
    `).join('');
    
    // Animate rows
    if (window.gsap) {
      gsap.from('.table-row-anim', {
        opacity: 0,
        y: 10,
        duration: 0.3,
        stagger: 0.05,
        ease: 'power2.out'
      });
    }
  }

  async function loadLowStock(){
    try{
      const { data } = await window.api.get('/products/low-stock');
      renderLowStock(data);
    }catch(e){ console.error('Low stock load error', e); }
  }

  async function doSearch(){
    const q = $('#searchInput')?.value?.trim();
    const btn = $('#btnSearch');
    
    if (!q){ loadLowStock(); return; }
    
    // Show loading state
    if (btn && window.gsap) {
      gsap.to(btn, { scale: 0.95, duration: 0.1 });
    }
    
    try{
      const { data } = await window.api.get(`/search/products?q=${encodeURIComponent(q)}`);
      renderLowStock(data || []);
      
      // Success feedback
      if (window.ui) ui.toast(`${(data||[]).length} productos encontrados`, 'success');
    }catch(e){ 
      console.warn('Search error', e); 
      if (window.ui) ui.toast('Error al buscar productos', 'error');
    } finally {
      if (btn && window.gsap) {
        gsap.to(btn, { scale: 1, duration: 0.2 });
      }
    }
  }

  document.addEventListener('DOMContentLoaded', async () => {
    setUserInfo();
    const btn = $('#logoutBtn');
    if (btn) btn.addEventListener('click', onLogout);

    await loadKPIs();
    await loadLowStock();

    const btnSearch = $('#btnSearch');
    if (btnSearch) btnSearch.addEventListener('click', doSearch);
    const input = $('#searchInput');
    if (input) input.addEventListener('keypress', (e)=>{ if(e.key==='Enter') doSearch(); });
  });
})();
