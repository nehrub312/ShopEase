/* ShopEase - cart page (renders from GET /api/cart, every action calls the REST API) */
let cartState = null;

async function loadCart() {
    try {
        render(await api('/api/cart'));
    } catch (e) {
        document.getElementById('cartRoot').innerHTML =
            '<div class="alert alert-danger">' + esc(e.message) + ' <button class="btn btn-sm btn-outline-danger ms-2" onclick="loadCart()">Retry</button></div>';
    }
}

function render(c) {
    cartState = c;
    updateBadge(c.totalItems);
    const root = document.getElementById('cartRoot');
    if (!c.items.length) {
        root.innerHTML = '<div class="empty-state"><i class="bi bi-cart-x"></i><h4 class="mt-2">Your cart is empty</h4>' +
            '<p class="text-muted">Looks like you have not added anything yet.</p><a class="btn btn-primary" href="/products">Start Shopping</a></div>';
        return;
    }
    const rows = c.items.map(i => `
      <div class="cart-row ${i.overLimit ? 'over-limit' : ''}" data-item="${i.itemId}">
        <img src="${esc(i.imageUrl)}" alt="${esc(i.productName)}" onerror="imgFallback(this)">
        <div>
          <a class="fw-semibold text-dark" href="/products/${i.productId}">${esc(i.productName)}</a>
          <div class="text-muted small">${inr(i.unitPrice)} each &middot; ${i.stock} in stock</div>
        </div>
        <div class="qty-ctl">
          <button class="btn btn-outline-secondary btn-sm" onclick="decrease(${i.itemId})" ${i.quantity <= 1 ? 'disabled' : ''} title="Decrease">-</button>
          <input type="number" class="form-control form-control-sm" value="${i.quantity}" min="1" onchange="typed(${i.itemId}, this)" aria-label="Quantity">
          <button class="btn btn-outline-secondary btn-sm" onclick="increase(${i.itemId})" ${i.overLimit || i.quantity >= MAX_QTY ? 'disabled' : ''} title="Increase">+</button>
        </div>
        <div class="fw-bold text-nowrap">${inr(i.subtotal)}</div>
        <button class="btn btn-outline-danger btn-sm" onclick="removeItem(${i.itemId})"><i class="bi bi-trash"></i> Remove</button>
        ${i.overLimit ? `<div class="alert alert-danger warn py-2"><i class="bi bi-exclamation-triangle-fill"></i> ${esc(i.warning)} Reduce the quantity to 5 or fewer.</div>` : ''}
      </div>`).join('');
    root.innerHTML = `
      ${c.hasOverLimit ? '<div class="alert alert-danger"><b>Warning:</b> Existing cart quantity exceeds the maximum limit of 5. Please reduce the highlighted item(s).</div>' : ''}
      <div class="row g-3">
        <div class="col-lg-8">${rows}</div>
        <div class="col-lg-4"><div class="summary">
          <h5>Order Summary</h5>
          <div class="d-flex justify-content-between"><span>Items</span><span>${c.totalItems}</span></div>
          <hr>
          <div class="d-flex justify-content-between align-items-center"><span class="fw-semibold">Cart Total</span><span class="total">${inr(c.total)}</span></div>
          <div class="small text-muted mt-1">Calculated securely on the server.</div>
          <div class="d-grid gap-2 mt-3">
            <a class="btn btn-outline-primary" href="/products">Continue Shopping</a>
            <button class="btn btn-outline-danger" onclick="clearCart()"><i class="bi bi-x-circle"></i> Clear Cart</button>
          </div>
        </div></div>
      </div>`;
}

function find(id) { return cartState.items.find(i => i.itemId === id); }

async function setQty(id, q) {
    const err = checkQty(q);
    if (err) { showToast(err, 'danger'); loadCart(); return; }
    try {
        render(await api('/api/cart/update/' + id, {method: 'PUT', body: JSON.stringify({quantity: q})}));
    } catch (e) {
        showToast(e.message, 'danger');
        loadCart();
    }
}

function increase(id) {
    const i = find(id);
    if (i.overLimit) { showToast(i.warning, 'warning'); return; }
    setQty(id, i.quantity + 1);
}

function decrease(id) {
    const i = find(id);
    // an over-limit entry is brought straight back down to the allowed maximum
    setQty(id, i.overLimit ? MAX_QTY : i.quantity - 1);
}

function typed(id, input) { setQty(id, parseInt(input.value, 10)); }

async function removeItem(id) {
    try { render(await api('/api/cart/remove/' + id, {method: 'DELETE'})); showToast('Item removed.', 'secondary'); }
    catch (e) { showToast(e.message, 'danger'); loadCart(); }
}

async function clearCart() {
    if (!confirm('Remove all items from your cart?')) return;
    try { render(await api('/api/cart/clear', {method: 'DELETE'})); showToast('Cart cleared.', 'secondary'); }
    catch (e) { showToast(e.message, 'danger'); loadCart(); }
}

document.addEventListener('DOMContentLoaded', loadCart);
