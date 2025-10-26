// POS (Point of Sale) Module
import apiClient from './api.js';
import { validateAmount } from './validations.js';
import { showToast, formatCurrency, generateReceiptHTML } from './ui.js';

class POSSystem {
    constructor() {
        this.cart = [];
        this.products = [];
        this.categories = [];
        this.currentOrder = null;
        this.holdOrders = new Map();
        this.taxRate = 0.15; // 15% IVA
        this.init();
    }

    async init() {
        await this.loadProducts();
        await this.loadCategories();
        this.setupEventListeners();
        this.renderProducts();
        this.updateCartDisplay();
        
        // Initialize Algolia search
        this.initializeSearch();
        
        // Load held orders from localStorage
        this.loadHeldOrders();
    }

    async loadProducts() {
        try {
            const response = await apiClient.getPublicProducts();
            this.products = response.content || response;
        } catch (error) {
            showToast('error', 'Error al cargar productos');
            console.error('Error loading products:', error);
        }
    }

    async loadCategories() {
        try {
            const response = await apiClient.request('/categories');
            this.categories = response;
        } catch (error) {
            console.error('Error loading categories:', error);
        }
    }

    setupEventListeners() {
        // Category filter
        document.querySelectorAll('.category-filter').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const categoryId = e.target.dataset.categoryId;
                this.filterByCategory(categoryId);
            });
        });

        // Search input
        const searchInput = document.getElementById('productSearch');
        if (searchInput) {
            searchInput.addEventListener('input', (e) => {
                this.searchProducts(e.target.value);
            });
        }

        // Payment method buttons
        document.querySelectorAll('.payment-method-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const method = e.target.dataset.method;
                this.processPayment(method);
            });
        });

        // Clear cart button
        const clearCartBtn = document.getElementById('clearCartBtn');
        if (clearCartBtn) {
            clearCartBtn.addEventListener('click', () => this.clearCart());
        }

        // Hold order button
        const holdOrderBtn = document.getElementById('holdOrderBtn');
        if (holdOrderBtn) {
            holdOrderBtn.addEventListener('click', () => this.holdOrder());
        }

        // Retrieve order button
        const retrieveOrderBtn = document.getElementById('retrieveOrderBtn');
        if (retrieveOrderBtn) {
            retrieveOrderBtn.addEventListener('click', () => this.showHeldOrders());
        }
    }

    renderProducts(productsToRender = this.products) {
        const container = document.getElementById('productsContainer');
        if (!container) return;

        container.innerHTML = '';
        
        productsToRender.forEach(product => {
            const productCard = this.createProductCard(product);
            container.appendChild(productCard);
        });

        // Animate product cards
        gsap.from(container.children, {
            opacity: 0,
            y: 20,
            duration: 0.3,
            stagger: 0.05,
            ease: "power2.out"
        });
    }

    createProductCard(product) {
        const card = document.createElement('div');
        card.className = 'product-card';
        card.innerHTML = `
            <div class="product-image">
                ${product.imageUrl ? 
                    `<img src="${product.imageUrl}" alt="${product.name}">` :
                    `<div class="product-placeholder">
                        <i class="ti ti-package"></i>
                    </div>`
                }
            </div>
            <h5 class="product-name">${product.name}</h5>
            <p class="product-price text-brand fw-bold">$${product.price.toFixed(2)}</p>
            <div class="product-stock">
                <small class="${product.stock > 0 ? 'text-success' : 'text-danger'}">
                    ${product.stock > 0 ? `Stock: ${product.stock}` : 'Sin stock'}
                </small>
            </div>
            ${product.stock > 0 ? 
                `<button class="btn btn-sm btn-brand w-100" onclick="pos.addToCart('${product.id}')">
                    <i class="ti ti-plus"></i> Agregar
                </button>` :
                `<button class="btn btn-sm btn-secondary w-100" disabled>Sin stock</button>`
            }
        `;
        
        return card;
    }

    addToCart(productId) {
        const product = this.products.find(p => p.id == productId);
        if (!product) return;

        if (product.stock <= 0) {
            showToast('warning', 'Producto sin stock');
            return;
        }

        const cartItem = this.cart.find(item => item.productId == productId);
        
        if (cartItem) {
            if (cartItem.quantity >= product.stock) {
                showToast('warning', 'Stock insuficiente');
                return;
            }
            cartItem.quantity++;
        } else {
            this.cart.push({
                productId: product.id,
                name: product.name,
                price: product.price,
                quantity: 1,
                maxStock: product.stock
            });
        }

        this.updateCartDisplay();
        showToast('success', `${product.name} agregado al carrito`);
        
        // Animate the product card
        const card = event.target.closest('.product-card');
        if (card) {
            gsap.to(card, {
                scale: 1.1,
                duration: 0.1,
                yoyo: true,
                repeat: 1,
                ease: "power2.inOut"
            });
        }
    }

    removeFromCart(productId) {
        const index = this.cart.findIndex(item => item.productId == productId);
        if (index > -1) {
            this.cart.splice(index, 1);
            this.updateCartDisplay();
        }
    }

    updateQuantity(productId, quantity) {
        const cartItem = this.cart.find(item => item.productId == productId);
        if (!cartItem) return;

        if (quantity <= 0) {
            this.removeFromCart(productId);
            return;
        }

        if (quantity > cartItem.maxStock) {
            showToast('warning', 'Stock insuficiente');
            return;
        }

        cartItem.quantity = quantity;
        this.updateCartDisplay();
    }

    updateCartDisplay() {
        const cartContainer = document.getElementById('cartItems');
        const cartCount = document.getElementById('cartCount');
        
        if (cartContainer) {
            if (this.cart.length === 0) {
                cartContainer.innerHTML = `
                    <div class="text-center text-muted p-4">
                        <i class="ti ti-shopping-cart-off" style="font-size: 3rem;"></i>
                        <p>Carrito vacío</p>
                    </div>
                `;
            } else {
                cartContainer.innerHTML = this.cart.map(item => `
                    <div class="cart-item">
                        <div class="cart-item-info">
                            <h6 class="mb-0">${item.name}</h6>
                            <small class="text-muted">$${item.price.toFixed(2)} c/u</small>
                        </div>
                        <div class="cart-item-actions">
                            <div class="input-group input-group-sm" style="width: 120px;">
                                <button class="btn btn-outline-secondary" 
                                        onclick="pos.updateQuantity('${item.productId}', ${item.quantity - 1})">
                                    <i class="ti ti-minus"></i>
                                </button>
                                <input type="number" class="form-control text-center" 
                                       value="${item.quantity}" 
                                       onchange="pos.updateQuantity('${item.productId}', this.value)"
                                       min="1" max="${item.maxStock}">
                                <button class="btn btn-outline-secondary" 
                                        onclick="pos.updateQuantity('${item.productId}', ${item.quantity + 1})">
                                    <i class="ti ti-plus"></i>
                                </button>
                            </div>
                            <button class="btn btn-sm btn-danger ms-2" 
                                    onclick="pos.removeFromCart('${item.productId}')">
                                <i class="ti ti-trash"></i>
                            </button>
                        </div>
                        <div class="cart-item-total">
                            <strong>$${(item.price * item.quantity).toFixed(2)}</strong>
                        </div>
                    </div>
                `).join('');
            }
        }

        // Update totals
        const totals = this.calculateTotals();
        
        if (document.getElementById('cartSubtotal')) {
            document.getElementById('cartSubtotal').textContent = `$${totals.subtotal.toFixed(2)}`;
            document.getElementById('cartIva').textContent = `$${totals.iva.toFixed(2)}`;
            document.getElementById('cartTotal').textContent = `$${totals.total.toFixed(2)}`;
        }

        // Update cart count badge
        if (cartCount) {
            const totalItems = this.cart.reduce((sum, item) => sum + item.quantity, 0);
            cartCount.textContent = totalItems;
            cartCount.style.display = totalItems > 0 ? 'inline-block' : 'none';
        }
    }

    calculateTotals() {
        const subtotal = this.cart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
        const discount = 0; // Can be implemented based on promotions
        const subtotalAfterDiscount = subtotal - discount;
        const iva = subtotalAfterDiscount * this.taxRate;
        const total = subtotalAfterDiscount + iva;

        return {
            subtotal,
            discount,
            iva,
            total
        };
    }

    async processPayment(method) {
        if (this.cart.length === 0) {
            showToast('warning', 'El carrito está vacío');
            return;
        }

        const totals = this.calculateTotals();
        
        // Show payment modal based on method
        if (method === 'CASH') {
            this.showCashPaymentModal(totals);
        } else {
            await this.completeSale(method, totals);
        }
    }

    showCashPaymentModal(totals) {
        const modal = document.createElement('div');
        modal.className = 'modal fade';
        modal.innerHTML = `
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">Pago en Efectivo</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div class="mb-3">
                            <label class="form-label">Total a Pagar</label>
                            <div class="fs-2 fw-bold text-brand">$${totals.total.toFixed(2)}</div>
                        </div>
                        <div class="mb-3">
                            <label for="cashReceived" class="form-label">Efectivo Recibido</label>
                            <input type="number" class="form-control fs-3" id="cashReceived" 
                                   min="${totals.total}" step="0.01" placeholder="0.00">
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Cambio</label>
                            <div class="fs-3 fw-bold text-success" id="changeAmount">$0.00</div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
                        <button type="button" class="btn btn-brand" id="confirmCashPayment">Confirmar Pago</button>
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();

        // Calculate change on input
        const cashInput = modal.querySelector('#cashReceived');
        const changeDisplay = modal.querySelector('#changeAmount');
        const confirmBtn = modal.querySelector('#confirmCashPayment');

        cashInput.addEventListener('input', () => {
            const received = parseFloat(cashInput.value) || 0;
            const change = received - totals.total;
            changeDisplay.textContent = `$${Math.max(0, change).toFixed(2)}`;
            confirmBtn.disabled = received < totals.total;
        });

        confirmBtn.addEventListener('click', async () => {
            const received = parseFloat(cashInput.value);
            if (received >= totals.total) {
                bsModal.hide();
                await this.completeSale('CASH', totals, {
                    cashReceived: received,
                    change: received - totals.total
                });
                modal.remove();
            }
        });

        modal.addEventListener('hidden.bs.modal', () => {
            modal.remove();
        });
    }

    async completeSale(paymentMethod, totals, paymentDetails = {}) {
        try {
            const saleData = {
                items: this.cart.map(item => ({
                    productId: item.productId,
                    quantity: item.quantity,
                    unitPrice: item.price
                })),
                paymentMethod,
                orderType: 'CARRY',
                notes: '',
                ...paymentDetails
            };

            const response = await apiClient.createSale(saleData);
            
            if (response && response.id) {
                showToast('success', 'Venta completada exitosamente');
                
                // Print receipt
                this.printReceipt(response);
                
                // Clear cart
                this.clearCart();
                
                // Get product recommendations
                this.showRecommendations(response.items);
            }
        } catch (error) {
            showToast('error', 'Error al procesar la venta: ' + error.message);
            console.error('Sale error:', error);
        }
    }

    printReceipt(sale) {
        const receiptHTML = generateReceiptHTML(sale);
        const printWindow = window.open('', '_blank', 'width=300,height=600');
        printWindow.document.write(receiptHTML);
        printWindow.document.close();
        printWindow.print();
    }

    async showRecommendations(saleItems) {
        if (!saleItems || saleItems.length === 0) return;
        
        try {
            const productId = saleItems[0].productId;
            const recommendations = await apiClient.getRecommendations(productId);
            
            if (recommendations && recommendations.length > 0) {
                const modal = document.createElement('div');
                modal.className = 'modal fade';
                modal.innerHTML = `
                    <div class="modal-dialog modal-dialog-centered">
                        <div class="modal-content">
                            <div class="modal-header">
                                <h5 class="modal-title">¿Algo más?</h5>
                                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                            </div>
                            <div class="modal-body">
                                <p>Otros clientes también compraron:</p>
                                <div class="recommendations-grid">
                                    ${recommendations.map(product => `
                                        <div class="recommendation-item p-2 border rounded text-center">
                                            <h6>${product.name}</h6>
                                            <p class="text-brand fw-bold">$${product.price.toFixed(2)}</p>
                                            <button class="btn btn-sm btn-brand" 
                                                    onclick="pos.addToCart('${product.id}'); this.closest('.modal').querySelector('.btn-close').click();">
                                                Agregar
                                            </button>
                                        </div>
                                    `).join('')}
                                </div>
                            </div>
                        </div>
                    </div>
                `;

                document.body.appendChild(modal);
                const bsModal = new bootstrap.Modal(modal);
                bsModal.show();
                
                modal.addEventListener('hidden.bs.modal', () => {
                    modal.remove();
                });
            }
        } catch (error) {
            console.error('Error getting recommendations:', error);
        }
    }

    clearCart() {
        this.cart = [];
        this.updateCartDisplay();
        showToast('info', 'Carrito limpiado');
    }

    holdOrder() {
        if (this.cart.length === 0) {
            showToast('warning', 'El carrito está vacío');
            return;
        }

        const orderId = Date.now().toString();
        this.holdOrders.set(orderId, [...this.cart]);
        this.saveHeldOrders();
        
        this.clearCart();
        showToast('success', 'Orden retenida');
    }

    showHeldOrders() {
        if (this.holdOrders.size === 0) {
            showToast('info', 'No hay órdenes retenidas');
            return;
        }

        const modal = document.createElement('div');
        modal.className = 'modal fade';
        modal.innerHTML = `
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">Órdenes Retenidas</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        ${Array.from(this.holdOrders.entries()).map(([orderId, items]) => {
                            const total = items.reduce((sum, item) => sum + (item.price * item.quantity), 0);
                            return `
                                <div class="held-order border rounded p-2 mb-2">
                                    <div class="d-flex justify-content-between align-items-center">
                                        <div>
                                            <strong>Orden #${orderId.slice(-6)}</strong>
                                            <br>
                                            <small>${items.length} items - Total: $${total.toFixed(2)}</small>
                                        </div>
                                        <button class="btn btn-sm btn-brand" 
                                                onclick="pos.retrieveOrder('${orderId}'); this.closest('.modal').querySelector('.btn-close').click();">
                                            Recuperar
                                        </button>
                                    </div>
                                </div>
                            `;
                        }).join('')}
                    </div>
                </div>
            </div>
        `;

        document.body.appendChild(modal);
        const bsModal = new bootstrap.Modal(modal);
        bsModal.show();
        
        modal.addEventListener('hidden.bs.modal', () => {
            modal.remove();
        });
    }

    retrieveOrder(orderId) {
        const order = this.holdOrders.get(orderId);
        if (order) {
            this.cart = [...order];
            this.holdOrders.delete(orderId);
            this.saveHeldOrders();
            this.updateCartDisplay();
            showToast('success', 'Orden recuperada');
        }
    }

    saveHeldOrders() {
        const ordersArray = Array.from(this.holdOrders.entries());
        localStorage.setItem('heldOrders', JSON.stringify(ordersArray));
    }

    loadHeldOrders() {
        const saved = localStorage.getItem('heldOrders');
        if (saved) {
            try {
                const ordersArray = JSON.parse(saved);
                this.holdOrders = new Map(ordersArray);
            } catch (error) {
                console.error('Error loading held orders:', error);
            }
        }
    }

    filterByCategory(categoryId) {
        if (categoryId === 'all') {
            this.renderProducts(this.products);
        } else {
            const filtered = this.products.filter(p => p.categoryId == categoryId);
            this.renderProducts(filtered);
        }
        
        // Update active button
        document.querySelectorAll('.category-filter').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.categoryId === categoryId);
        });
    }

    async searchProducts(query) {
        if (!query || query.length < 2) {
            this.renderProducts(this.products);
            return;
        }

        try {
            const results = await apiClient.searchProducts(query);
            this.renderProducts(results);
        } catch (error) {
            // Fallback to local search
            const filtered = this.products.filter(p => 
                p.name.toLowerCase().includes(query.toLowerCase()) ||
                p.code.toLowerCase().includes(query.toLowerCase())
            );
            this.renderProducts(filtered);
        }
    }

    async initializeSearch() {
        // Initialize Algolia search if configured
        if (window.algoliasearch) {
            const client = algoliasearch('YOUR_APP_ID', 'YOUR_SEARCH_KEY');
            const index = client.initIndex('products');
            
            const searchInput = document.getElementById('productSearch');
            if (searchInput) {
                searchInput.addEventListener('input', async (e) => {
                    const query = e.target.value;
                    if (query.length >= 2) {
                        try {
                            const { hits } = await index.search(query);
                            this.renderProducts(hits);
                        } catch (error) {
                            console.error('Algolia search error:', error);
                            this.searchProducts(query); // Fallback
                        }
                    } else {
                        this.renderProducts(this.products);
                    }
                });
            }
        }
    }
}

// Initialize POS system
const pos = new POSSystem();
window.pos = pos; // Make available globally for onclick handlers

export default pos;
