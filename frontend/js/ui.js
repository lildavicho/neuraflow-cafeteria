// UI Helper Module with GSAP animations and utilities

/**
 * Show toast notification
 */
export function showToast(type, message, duration = 5000) {
    const toastContainer = document.querySelector('.toast-container') || createToastContainer();
    
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `
        <div class="d-flex align-items-center">
            <div class="toast-icon me-2">
                ${getToastIcon(type)}
            </div>
            <div class="flex-grow-1">${message}</div>
            <button type="button" class="btn-close ms-2" onclick="this.parentElement.parentElement.remove()"></button>
        </div>
    `;
    
    toastContainer.appendChild(toast);
    
    // GSAP animation
    gsap.from(toast, {
        x: 100,
        opacity: 0,
        duration: 0.3,
        ease: "power2.out"
    });
    
    // Auto remove
    setTimeout(() => {
        gsap.to(toast, {
            x: 100,
            opacity: 0,
            duration: 0.3,
            ease: "power2.in",
            onComplete: () => toast.remove()
        });
    }, duration);
}

function createToastContainer() {
    const container = document.createElement('div');
    container.className = 'toast-container';
    container.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 9999;';
    document.body.appendChild(container);
    return container;
}

function getToastIcon(type) {
    const icons = {
        success: '<i class="ti ti-check text-success"></i>',
        error: '<i class="ti ti-x text-danger"></i>',
        warning: '<i class="ti ti-alert-triangle text-warning"></i>',
        info: '<i class="ti ti-info-circle text-info"></i>'
    };
    return icons[type] || icons.info;
}

/**
 * Format currency
 */
export function formatCurrency(amount, currency = 'USD') {
    return new Intl.NumberFormat('es-EC', {
        style: 'currency',
        currency: currency,
        minimumFractionDigits: 2
    }).format(amount);
}

/**
 * Format date
 */
export function formatDate(date, format = 'short') {
    const d = new Date(date);
    if (format === 'short') {
        return d.toLocaleDateString('es-EC');
    } else if (format === 'long') {
        return d.toLocaleDateString('es-EC', {
            weekday: 'long',
            year: 'numeric',
            month: 'long',
            day: 'numeric'
        });
    } else if (format === 'time') {
        return d.toLocaleTimeString('es-EC', {
            hour: '2-digit',
            minute: '2-digit'
        });
    }
    return d.toLocaleString('es-EC');
}

/**
 * Generate receipt HTML
 */
export function generateReceiptHTML(sale) {
    const currentDate = new Date();
    return `
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                @media print {
                    body { margin: 0; font-family: 'Courier New', monospace; }
                    .receipt { width: 80mm; padding: 10mm; }
                }
                body { font-family: 'Courier New', monospace; font-size: 12px; }
                .receipt { max-width: 300px; margin: 0 auto; padding: 20px; }
                .header { text-align: center; margin-bottom: 20px; }
                .header h1 { margin: 0; font-size: 18px; }
                .header p { margin: 5px 0; font-size: 11px; }
                .divider { border-bottom: 1px dashed #000; margin: 10px 0; }
                .items { margin: 15px 0; }
                .item { display: flex; justify-content: space-between; margin: 5px 0; }
                .totals { margin-top: 15px; }
                .total-line { display: flex; justify-content: space-between; margin: 5px 0; }
                .total-line.final { font-weight: bold; font-size: 14px; margin-top: 10px; }
                .footer { text-align: center; margin-top: 20px; font-size: 11px; }
                .barcode { text-align: center; margin: 15px 0; font-family: 'Libre Barcode 39', monospace; font-size: 40px; }
            </style>
        </head>
        <body>
            <div class="receipt">
                <div class="header">
                    <h1>CAFETERÍA UCACUE</h1>
                    <p>Universidad Católica de Cuenca</p>
                    <p>Av. de las Américas y Humboldt</p>
                    <p>RUC: 0190123456001</p>
                    <p>Tel: (07) 2831608</p>
                </div>
                
                <div class="divider"></div>
                
                <div style="text-align: center;">
                    <strong>FACTURA</strong><br>
                    ${sale.invoiceNumber || 'FAC-' + Date.now()}
                </div>
                
                <div class="divider"></div>
                
                <div>
                    <div>Fecha: ${formatDate(currentDate)}</div>
                    <div>Hora: ${formatDate(currentDate, 'time')}</div>
                    <div>Cajero: ${sale.userName || 'Sistema'}</div>
                    <div>Cliente: ${sale.customerName || 'Consumidor Final'}</div>
                </div>
                
                <div class="divider"></div>
                
                <div class="items">
                    <div style="font-weight: bold; margin-bottom: 5px;">
                        <span>DESCRIPCIÓN</span>
                        <span style="float: right;">TOTAL</span>
                    </div>
                    ${(sale.items || []).map(item => `
                        <div class="item">
                            <span>${item.quantity}x ${item.name}</span>
                            <span>$${(item.quantity * item.unitPrice).toFixed(2)}</span>
                        </div>
                    `).join('')}
                </div>
                
                <div class="divider"></div>
                
                <div class="totals">
                    <div class="total-line">
                        <span>Subtotal:</span>
                        <span>$${sale.subtotal?.toFixed(2) || '0.00'}</span>
                    </div>
                    ${sale.discount > 0 ? `
                        <div class="total-line">
                            <span>Descuento:</span>
                            <span>-$${sale.discount.toFixed(2)}</span>
                        </div>
                    ` : ''}
                    <div class="total-line">
                        <span>IVA (15%):</span>
                        <span>$${sale.iva?.toFixed(2) || '0.00'}</span>
                    </div>
                    <div class="total-line final">
                        <span>TOTAL:</span>
                        <span>$${sale.total?.toFixed(2) || '0.00'}</span>
                    </div>
                </div>
                
                <div class="divider"></div>
                
                <div>
                    <div>Forma de Pago: ${sale.paymentMethod || 'EFECTIVO'}</div>
                    ${sale.cashReceived ? `
                        <div>Recibido: $${sale.cashReceived.toFixed(2)}</div>
                        <div>Cambio: $${sale.change?.toFixed(2) || '0.00'}</div>
                    ` : ''}
                </div>
                
                <div class="barcode">
                    *${sale.invoiceNumber || Date.now()}*
                </div>
                
                <div class="divider"></div>
                
                <div class="footer">
                    <p>¡GRACIAS POR SU COMPRA!</p>
                    <p>Conserve este comprobante</p>
                    <p>www.ucacue.edu.ec</p>
                </div>
            </div>
            <script>
                window.onload = function() {
                    window.print();
                }
            </script>
        </body>
        </html>
    `;
}

/**
 * Show loading overlay
 */
export function showLoading(message = 'Cargando...') {
    hideLoading(); // Remove any existing loading
    
    const overlay = document.createElement('div');
    overlay.id = 'loadingOverlay';
    overlay.className = 'loading-overlay';
    overlay.innerHTML = `
        <div class="loading-content">
            <div class="spinner-border text-brand" role="status"></div>
            <div class="mt-3">${message}</div>
        </div>
    `;
    
    document.body.appendChild(overlay);
    
    gsap.from(overlay, {
        opacity: 0,
        duration: 0.3
    });
}

/**
 * Hide loading overlay
 */
export function hideLoading() {
    const overlay = document.getElementById('loadingOverlay');
    if (overlay) {
        gsap.to(overlay, {
            opacity: 0,
            duration: 0.3,
            onComplete: () => overlay.remove()
        });
    }
}

/**
 * Show confirmation modal
 */
export function showConfirm(title, message, onConfirm, onCancel) {
    const modal = document.createElement('div');
    modal.className = 'modal fade';
    modal.innerHTML = `
        <div class="modal-dialog modal-dialog-centered">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">${title}</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <p>${message}</p>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                    <button type="button" class="btn btn-brand confirm-btn">Confirmar</button>
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
    const bsModal = new bootstrap.Modal(modal);
    
    modal.querySelector('.confirm-btn').addEventListener('click', () => {
        if (onConfirm) onConfirm();
        bsModal.hide();
    });
    
    modal.addEventListener('hidden.bs.modal', () => {
        if (onCancel) onCancel();
        modal.remove();
    });
    
    bsModal.show();
}

/**
 * Initialize GSAP ScrollTrigger for animations
 */
export function initScrollAnimations() {
    // Fade in elements on scroll
    gsap.utils.toArray('.animate-on-scroll').forEach(element => {
        gsap.from(element, {
            y: 30,
            opacity: 0,
            duration: 0.5,
            scrollTrigger: {
                trigger: element,
                start: 'top 80%',
                end: 'bottom 20%',
                toggleActions: 'play none none reverse'
            }
        });
    });
    
    // Stagger animations for lists
    gsap.utils.toArray('.stagger-list').forEach(list => {
        gsap.from(list.children, {
            y: 20,
            opacity: 0,
            duration: 0.3,
            stagger: 0.1,
            scrollTrigger: {
                trigger: list,
                start: 'top 80%'
            }
        });
    });
}

/**
 * Create animated counter
 */
export function animateCounter(element, endValue, duration = 1) {
    const startValue = 0;
    const obj = { value: startValue };
    
    gsap.to(obj, {
        value: endValue,
        duration: duration,
        ease: "power2.out",
        onUpdate: () => {
            element.textContent = Math.floor(obj.value).toLocaleString('es-EC');
        }
    });
}

/**
 * Initialize theme switcher
 */
export function initThemeSwitcher() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    document.documentElement.setAttribute('data-theme', savedTheme);
    
    const themeSwitcher = document.getElementById('themeSwitcher');
    if (themeSwitcher) {
        themeSwitcher.addEventListener('click', () => {
            const currentTheme = document.documentElement.getAttribute('data-theme');
            const newTheme = currentTheme === 'light' ? 'dark' : 'light';
            
            document.documentElement.setAttribute('data-theme', newTheme);
            localStorage.setItem('theme', newTheme);
            
            // Animate theme transition
            gsap.to(document.body, {
                opacity: 0.8,
                duration: 0.2,
                yoyo: true,
                repeat: 1
            });
        });
    }
}

/**
 * Export all functions
 */
export default {
    showToast,
    formatCurrency,
    formatDate,
    generateReceiptHTML,
    showLoading,
    hideLoading,
    showConfirm,
    initScrollAnimations,
    animateCounter,
    initThemeSwitcher
};
