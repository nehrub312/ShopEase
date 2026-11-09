/* ShopEase - admin dashboard (all calls hit the admin-protected REST API) */
document.addEventListener('DOMContentLoaded', function () {
    const modalEl = document.getElementById('productModal');
    const modal = new bootstrap.Modal(modalEl);
    const $ = id => document.getElementById(id);
    const errBox = $('formError');

    function fill(p) {
        $('pId').value = p.id || '';
        $('pName').value = p.name || '';
        $('pCategory').value = p.category || $('pCategory').options[0].value;
        $('pPrice').value = p.price || '';
        $('pStock').value = p.stock != null ? p.stock : '';
        $('pImage').value = p.image || '';
        $('pDescription').value = p.description || '';
        errBox.classList.add('d-none');
    }

    $('btnAdd').addEventListener('click', () => { $('modalTitle').textContent = 'Add Product'; fill({}); modal.show(); });

    $('btnSave').addEventListener('click', async () => {
        const id = $('pId').value;
        const price = parseFloat($('pPrice').value), stock = parseInt($('pStock').value, 10);
        const body = {
            name: $('pName').value.trim(), category: $('pCategory').value,
            description: $('pDescription').value.trim(), price: price, stock: stock, imageUrl: $('pImage').value.trim()
        };
        let problem = null;
        if (!body.name) problem = 'Product name is required.';
        else if (!(price > 0)) problem = 'Price must be greater than zero.';
        else if (!(stock >= 0)) problem = 'Stock cannot be negative.';
        if (problem) { errBox.textContent = problem; errBox.classList.remove('d-none'); return; }
        try {
            await api(id ? '/api/products/' + id : '/api/products', {method: id ? 'PUT' : 'POST', body: JSON.stringify(body)});
            location.reload();
        } catch (e) { errBox.textContent = e.message; errBox.classList.remove('d-none'); }
    });

    document.addEventListener('click', async ev => {
        const edit = ev.target.closest('[data-edit]');
        if (edit) {
            $('modalTitle').textContent = 'Edit Product';
            fill({id: edit.dataset.edit, name: edit.dataset.name, category: edit.dataset.category, description: edit.dataset.description,
                  price: edit.dataset.price, stock: edit.dataset.qty, image: edit.dataset.image});
            modal.show();
            return;
        }
        const del = ev.target.closest('[data-delete]');
        if (del) {
            if (!confirm('Delete "' + del.dataset.name + '"? It will also be removed from all carts.')) return;
            try { await api('/api/products/' + del.dataset.delete, {method: 'DELETE'}); location.reload(); }
            catch (e) { showToast(e.message, 'danger'); }
            return;
        }
        const st = ev.target.closest('[data-stock]');
        if (st) {
            const id = st.dataset.stock, v = parseInt($('stock-' + id).value, 10);
            if (!(v >= 0)) { showToast('Stock cannot be negative.', 'danger'); return; }
            try { await api('/api/products/' + id + '/stock', {method: 'PATCH', body: JSON.stringify({stock: v})}); showToast('Stock updated.', 'success'); }
            catch (e) { showToast(e.message, 'danger'); }
        }
    });
});
