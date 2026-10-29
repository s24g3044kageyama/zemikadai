/**
 * Pâtisserie Douceur - フロントエンド制御スクリプト
 * 
 * ※ 本アプリケーションのビジネスロジック（カート保持、商品追加・削除、
 *    小計および合計金額の計算）はすべてサーバー側（Java）で実行されています。
 *    クライアント側はサーバーAPIとの連携と画面表示のみを担当します。
 */

document.addEventListener('DOMContentLoaded', () => {
  // 共通: カートのバッジ情報を取得して更新
  updateCartBadge();

  // 商品一覧ページの場合
  const productGrid = document.getElementById('productGrid');
  if (productGrid) {
    loadProducts();
  }

  // カート画面の場合
  const cartContentArea = document.getElementById('cartContentArea');
  if (cartContentArea) {
    renderCartPage();
  }
});

// 通貨フォーマット (例: 1200 -> "¥1,200")
function formatYen(amount) {
  return new Intl.NumberFormat('ja-JP', { style: 'currency', currency: 'JPY' }).format(amount);
}

/**
 * サーバーから最新のカート情報を取得し、ヘッダーのバッジ数を更新
 */
async function updateCartBadge() {
  const badge = document.getElementById('cartBadge');
  if (!badge) return;

  try {
    const res = await fetch('/api/cart');
    if (!res.ok) throw new Error('カート取得失敗');
    const cart = await res.json();
    badge.textContent = cart.totalCount || 0;
  } catch (err) {
    console.error('カートバッジ更新エラー:', err);
  }
}

/**
 * サーバーから商品一覧を取得して描画 (index.html)
 */
async function loadProducts() {
  const container = document.getElementById('productGrid');
  if (!container) return;

  try {
    const res = await fetch('/api/products');
    if (!res.ok) throw new Error('商品一覧の取得に失敗しました');
    const products = await res.json();

    container.innerHTML = '';
    products.forEach(product => {
      const card = document.createElement('article');
      card.className = 'product-card';
      card.innerHTML = `
        <div class="product-image-wrap">
          <img src="${product.image}" alt="${escapeHtml(product.name)}" class="product-image" loading="lazy">
          <span class="product-category-tag">${escapeHtml(product.category)}</span>
        </div>
        <div class="product-info">
          <h3 class="product-name">${escapeHtml(product.name)}</h3>
          <p class="product-desc">${escapeHtml(product.description)}</p>
          
          <div class="product-footer">
            <div class="product-price-row">
              <span class="price-label">価格（税込）</span>
              <div>
                <span class="price-val">${formatYen(product.price)}</span>
                <span class="price-tax">税込</span>
              </div>
            </div>

            <!-- 購入個数の指定とカート追加フォーム -->
            <form class="add-to-cart-form" onsubmit="handleAddToCart(event, '${product.id}')">
              <div class="quantity-control-row">
                <span class="quantity-label">数量:</span>
                <div class="quantity-stepper">
                  <button type="button" class="stepper-btn" onclick="adjustQty('${product.id}', -1)" aria-label="個数を減らす">−</button>
                  <input type="number" id="qty-${product.id}" name="quantity" class="quantity-input" value="1" min="1" max="99" required>
                  <button type="button" class="stepper-btn" onclick="adjustQty('${product.id}', 1)" aria-label="個数を増やす">＋</button>
                </div>
              </div>
              <button type="submit" class="btn-add-cart">
                <span>🛒 カートに追加</span>
              </button>
            </form>
          </div>
        </div>
      `;
      container.appendChild(card);
    });
  } catch (err) {
    console.error(err);
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; padding: 2rem; color: #dc2626;">
        <p>商品情報の読み込みに失敗しました。サーバーが起動しているか確認してください。</p>
      </div>
    `;
  }
}

/**
 * 数量ステッパーの変更処理
 */
function adjustQty(productId, delta) {
  const input = document.getElementById(`qty-${productId}`);
  if (!input) return;
  let val = parseInt(input.value, 10) || 1;
  val = Math.max(1, Math.min(99, val + delta));
  input.value = val;
}

/**
 * カート追加処理（サーバー側のJava APIへPOST送信）
 */
async function handleAddToCart(e, productId) {
  e.preventDefault();
  const input = document.getElementById(`qty-${productId}`);
  const quantity = input ? parseInt(input.value, 10) || 1 : 1;

  try {
    const res = await fetch('/api/cart/add', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ productId, quantity })
    });

    if (!res.ok) throw new Error('カートへの追加に失敗しました');
    const updatedCart = await res.json();

    // サーバーから返された合計個数をバッジに反映
    const badge = document.getElementById('cartBadge');
    if (badge) badge.textContent = updatedCart.totalCount;

    // トースト通知を表示
    showToast(`🛒 カートに ${quantity} 点追加しました`);
  } catch (err) {
    console.error(err);
    showToast('❌ カート追加中にエラーが発生しました', true);
  }
}

/**
 * カート画面の描画 (cart.html)
 * サーバー側で計算された小計・合計・アイテムリストを表示
 */
async function renderCartPage() {
  const container = document.getElementById('cartContentArea');
  if (!container) return;

  try {
    const res = await fetch('/api/cart');
    if (!res.ok) throw new Error('カート取得エラー');
    const cart = await res.json();

    // カートが空の場合
    if (!cart.items || cart.items.length === 0) {
      container.innerHTML = `
        <div class="empty-cart-view">
          <div class="empty-icon">🍰</div>
          <h3 class="empty-title">お客様のカートは現在空です</h3>
          <p class="empty-desc">こだわりの特製ケーキを多数ご用意しております。ぜひ商品一覧をご覧ください。</p>
          <a href="/" class="btn-primary-action">
            <span>🏪 ケーキ一覧を見てみる</span>
          </a>
        </div>
      `;
      // ヘッダーバッジも同期
      updateCartBadge();
      return;
    }

    // カート内に商品がある場合のレイアウト
    // ※ 小計(item.subtotal)および合計(cart.totalPrice)はJavaサーバーが計算
    let itemsRowsHtml = '';
    cart.items.forEach(item => {
      itemsRowsHtml += `
        <tr>
          <td>
            <div class="cart-product-cell">
              <img src="${item.product.image}" alt="${escapeHtml(item.product.name)}" class="cart-thumb">
              <div>
                <div class="cart-item-name">${escapeHtml(item.product.name)}</div>
                <div class="cart-item-unit-price">単価: ${formatYen(item.product.price)} (税込)</div>
              </div>
            </div>
          </td>
          <td>
            <div class="quantity-stepper">
              <button type="button" class="stepper-btn" onclick="updateCartItemQty('${item.product.id}', ${item.quantity - 1})" aria-label="減らす">−</button>
              <input type="number" class="quantity-input" value="${item.quantity}" min="1" max="99" 
                     onchange="updateCartItemQty('${item.product.id}', this.value)">
              <button type="button" class="stepper-btn" onclick="updateCartItemQty('${item.product.id}', ${item.quantity + 1})" aria-label="増やす">＋</button>
            </div>
          </td>
          <td>
            <!-- サーバーサイドで計算された小計金額 -->
            <span class="subtotal-val">${formatYen(item.subtotal)}</span>
          </td>
          <td style="text-align: right;">
            <!-- 商品ごとの削除ボタン -->
            <button type="button" class="btn-remove-item" onclick="removeCartItem('${item.product.id}', '${escapeHtml(item.product.name)}')">
              <span>🗑️ 削除</span>
            </button>
          </td>
        </tr>
      `;
    });

    container.innerHTML = `
      <div class="cart-layout">
        <!-- カート明細一覧カード -->
        <div class="cart-items-card">
          <div class="cart-items-header">
            <span class="cart-count-info">商品合計: <strong>${cart.totalCount}</strong> 点</span>
            <button type="button" class="cart-clear-btn" onclick="clearCart()">カートをすべて空にする</button>
          </div>
          <div style="overflow-x: auto;">
            <table class="cart-table">
              <thead>
                <tr>
                  <th style="min-width: 220px;">商品名</th>
                  <th style="min-width: 120px;">数量</th>
                  <th style="min-width: 100px;">小計</th>
                  <th style="min-width: 80px; text-align: right;">操作</th>
                </tr>
              </thead>
              <tbody>
                ${itemsRowsHtml}
              </tbody>
            </table>
          </div>
        </div>

        <!-- ご注文金額サマリーカード -->
        <aside class="cart-summary-card">
          <h3 class="summary-title">お会計内容</h3>
          <div class="summary-row">
            <span style="color: var(--text-muted);">商品合計 (${cart.totalCount} 点)</span>
            <span style="font-weight: 600;">${formatYen(cart.totalPrice)}</span>
          </div>
          <div class="summary-row">
            <span style="color: var(--text-muted);">クール便配送料</span>
            <span style="font-weight: 600; color: #16a34a;">送料無料キャンペーン</span>
          </div>
          <div class="summary-row total-row">
            <!-- サーバーサイドで計算された合計金額 -->
            <span class="total-label">お支払合計 (税込)</span>
            <span class="total-amount">${formatYen(cart.totalPrice)}</span>
          </div>

          <button type="button" class="btn-checkout" onclick="checkoutDemo()">
            <span>✨ ご注文手続きへ進む</span>
          </button>
          
          <div style="margin-top: 1.25rem; text-align: center;">
            <a href="/" class="back-link" style="width: 100%; justify-content: center;">
              <span>← 商品一覧に戻ってお買い物を続ける</span>
            </a>
          </div>
        </aside>
      </div>
    `;

    // バッジも同期
    updateCartBadge();
  } catch (err) {
    console.error(err);
    container.innerHTML = `
      <div style="text-align: center; padding: 2rem; color: #dc2626;">
        <p>カート情報の読み込みに失敗しました。</p>
      </div>
    `;
  }
}

/**
 * 数量変更（サーバー側Java APIにPOST送信）
 */
async function updateCartItemQty(productId, quantity) {
  const qty = parseInt(quantity, 10);
  if (isNaN(qty)) return;

  try {
    const res = await fetch('/api/cart/update', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ productId, quantity: qty })
    });
    if (!res.ok) throw new Error('数量更新エラー');
    // 再描画
    await renderCartPage();
    showToast('数量を更新しました');
  } catch (err) {
    console.error(err);
    showToast('数量更新に失敗しました', true);
  }
}

/**
 * 商品ごとの削除（サーバー側Java APIにPOST送信）
 */
async function removeCartItem(productId, productName) {
  if (!confirm(`「${productName}」をカートから削除しますか？`)) {
    return;
  }

  try {
    const res = await fetch('/api/cart/remove', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ productId })
    });
    if (!res.ok) throw new Error('削除エラー');
    await renderCartPage();
    showToast(`🗑️ 「${productName}」を削除しました`);
  } catch (err) {
    console.error(err);
    showToast('削除に失敗しました', true);
  }
}

/**
 * カート全削除（サーバー側Java APIにPOST送信）
 */
async function clearCart() {
  if (!confirm('カート内の商品をすべて削除しますか？')) {
    return;
  }

  try {
    const res = await fetch('/api/cart/clear', {
      method: 'POST'
    });
    if (!res.ok) throw new Error('クリアエラー');
    await renderCartPage();
    showToast('カートを空にしました');
  } catch (err) {
    console.error(err);
    showToast('クリアに失敗しました', true);
  }
}

/**
 * 注文手続きデモ
 */
function checkoutDemo() {
  alert('ご購入手続きへ進みます。\n（※ デモ機能です。ご注文ありがとうございます！）');
}

/**
 * トースト通知表示
 */
function showToast(message, isError = false) {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = 'toast';
  if (isError) {
    toast.style.borderLeftColor = '#ef4444';
  }
  toast.textContent = message;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 2500);
}

/**
 * HTMLエスケープヘルパー
 */
function escapeHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}
