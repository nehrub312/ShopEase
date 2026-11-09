/* ShopEase - shared helpers */
const MAX_QTY = 5;
const MAX_MSG = 'Maximum quantity allowed is 5 per product.';

function esc(s) {
    return String(s == null ? '' : s).replace(/[&<>"']/g, c => ({'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'}[c]));
}

/** Indian rupee format, e.g. 105000 -> ₹1,05,000.00 */
function inr(n) {
    const parts = Number(n).toFixed(2).split('.');
    let i = parts[0], last = i.slice(-3), rest = i.slice(0, -3);
    if (rest) last = rest.replace(/\B(?=(\d{2})+(?!\d))/g, ',') + ',' + last;
    return '₹' + last + '.' + parts[1];
}

function imgFallback(img) {
    img.onerror = null;
    img.src = '/images/placeholder.svg';
}

function showToast(message, type) {
    type = type || 'success';
    let box = document.getElementById('toastBox');
    if (!box) {
        box = document.createElement('div');
        box.id = 'toastBox';
        box.className = 'toast-container position-fixed top-0 end-0 p-3';
        document.body.appendChild(box);
    }
    const el = document.createElement('div');
    el.className = 'toast align-items-center text-bg-' + type + ' border-0';
    el.setAttribute('role', 'alert');
    el.innerHTML = '<div class="d-flex"><div class="toast-body">' + esc(message) +
        '</div><button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button></div>';
    box.appendChild(el);
    const t = new bootstrap.Toast(el, {delay: 4000});
    el.addEventListener('hidden.bs.toast', () => el.remove());
    t.show();
}

/** fetch wrapper: returns parsed JSON, throws Error(friendly message) on failure. */
async function api(url, options) {
    let res;
    try {
        res = await fetch(url, Object.assign({headers: {'Content-Type': 'application/json'}, credentials: 'same-origin'}, options || {}));
    } catch (e) {
        throw new Error('Cannot reach the server. Please check your connection and try again.');
    }
    let data = null;
    try { data = await res.json(); } catch (e) { /* empty body */ }
    if (!res.ok) throw new Error((data && data.message) || 'Something went wrong. Please try again.');
    return data;
}

function updateBadge(count) {
    const b = document.getElementById('cartBadge');
    if (b) b.textContent = count;
}

/** Frontend validation (the backend validates again). Returns an error string or null. */
function checkQty(q) {
    if (!Number.isInteger(q) || q <= 0) return 'Quantity must be at least 1.';
    if (q > MAX_QTY) return MAX_MSG;
    return null;
}

async function addToCart(productId, qty) {
    const err = checkQty(qty);
    if (err) { showToast(err, 'danger'); return; }
    try {
        const cart = await api('/api/cart/add', {method: 'POST', body: JSON.stringify({productId: productId, quantity: qty})});
        updateBadge(cart.totalItems);
        showToast('Added to cart!', 'success');
    } catch (e) {
        showToast(e.message, 'danger');
    }
}

function stepQty(d) {
    const input = document.getElementById('qtyInput');
    const msg = document.getElementById('qtyMsg');
    const v = (parseInt(input.value, 10) || 1) + d;
    if (v > MAX_QTY) { msg.textContent = MAX_MSG; return; }
    msg.textContent = '';
    input.value = Math.max(1, v);
}

document.addEventListener('click', function (ev) {
    const add = ev.target.closest('[data-add-to-cart]');
    if (add) { addToCart(parseInt(add.getAttribute('data-add-to-cart'), 10), 1); return; }
    const det = ev.target.closest('[data-add-detail]');
    if (det) {
        const q = parseInt(document.getElementById('qtyInput').value, 10);
        const msg = document.getElementById('qtyMsg');
        const err = checkQty(q);
        msg.textContent = err || '';
        if (!err) addToCart(parseInt(det.getAttribute('data-add-detail'), 10), q);
        else showToast(err, 'danger');
    }
});

document.addEventListener('DOMContentLoaded', function () {
    api('/api/cart').then(c => updateBadge(c.totalItems)).catch(() => {});
});
