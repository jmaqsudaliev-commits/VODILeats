// VODIL EATS - Super Admin Dashboard Script
const API_BASE = window.location.protocol === 'file:'
  ? 'http://localhost:3000/api/v1'
  : `${window.location.origin}/api/v1`;

// 20-Xonali O'zgarmas Maxfiy Parol (Faqat Platforma Egasi Uchun)
const MASTER_ADMIN_KEY = 'VODIL_EATS_2026_UZB!';

let currentTab = 'dashboard';
let currentOrderFilter = 'ALL';
let currentOrders = [];
let currentRestaurants = [];
let currentCouriers = [];
let currentCustomers = [];
let leafletMap = null;
let courierMarkers = {};
let previousOrderCount = 0;
let isPollingActive = false;

// Initialize when DOM is ready
document.addEventListener('DOMContentLoaded', () => {
  initClock();
  initTabs();
  initLeafletMap();
  checkAuthStatus();
});

// Security & Authentication System
function checkAuthStatus() {
  const token = localStorage.getItem('vodil_admin_token');
  const overlay = document.getElementById('lockScreenOverlay');
  if (token === 'vodil_admin_authorized_2026' || localStorage.getItem('vodil_admin_key') === MASTER_ADMIN_KEY) {
    if (overlay) overlay.classList.add('hidden');
    startDashboard();
  } else {
    if (overlay) overlay.classList.remove('hidden');
    const input = document.getElementById('adminPasswordInput');
    if (input) input.focus();
  }
}

function updatePasswordCounter(val) {
  const counter = document.getElementById('charCount');
  const err = document.getElementById('lockErrorMsg');
  if (err) err.style.display = 'none';
  if (counter) {
    counter.textContent = `Belgilar: ${val.length} / 20`;
    if (val.length === 20) {
      counter.style.color = '#10b981';
      counter.style.fontWeight = 'bold';
    } else {
      counter.style.color = 'var(--text-muted)';
      counter.style.fontWeight = 'normal';
    }
  }
}

function togglePasswordVisibility() {
  const input = document.getElementById('adminPasswordInput');
  const eye = document.getElementById('passwordEyeIcon');
  if (!input || !eye) return;
  if (input.type === 'password') {
    input.type = 'text';
    eye.className = 'fas fa-eye-slash';
  } else {
    input.type = 'password';
    eye.className = 'fas fa-eye';
  }
}

async function handleAdminLogin(e) {
  if (e) e.preventDefault();
  const input = document.getElementById('adminPasswordInput');
  const card = document.getElementById('lockCard');
  const err = document.getElementById('lockErrorMsg');
  if (!input) return;

  const enteredPassword = input.value.trim();

  try {
    const res = await fetch(`${API_BASE}/admin/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ password: enteredPassword })
    });

    const data = await res.json();

    if (res.ok && data.success) {
      localStorage.setItem('vodil_admin_token', data.token);
      localStorage.setItem('vodil_admin_key', enteredPassword);
      
      const overlay = document.getElementById('lockScreenOverlay');
      if (overlay) overlay.classList.add('hidden');
      
      showToast('Xush kelibsiz, platforma egasi! Tizim faollashtirildi.', 'success');
      startDashboard();
    } else {
      showLoginError();
    }
  } catch (errApi) {
    // Offline or direct password check fallback
    if (enteredPassword === MASTER_ADMIN_KEY) {
      localStorage.setItem('vodil_admin_token', 'vodil_admin_authorized_2026');
      localStorage.setItem('vodil_admin_key', enteredPassword);
      const overlay = document.getElementById('lockScreenOverlay');
      if (overlay) overlay.classList.add('hidden');
      showToast('Offline rejimda kirildi!', 'success');
      startDashboard();
    } else {
      showLoginError();
    }
  }
}

function showLoginError() {
  const card = document.getElementById('lockCard');
  const err = document.getElementById('lockErrorMsg');
  if (err) err.style.display = 'inline';
  if (card) {
    card.classList.remove('shake-anim');
    void card.offsetWidth; // trigger reflow
    card.classList.add('shake-anim');
  }
}

function handleAdminLogout() {
  localStorage.removeItem('vodil_admin_token');
  localStorage.removeItem('vodil_admin_key');
  isPollingActive = false;
  
  const overlay = document.getElementById('lockScreenOverlay');
  if (overlay) overlay.classList.remove('hidden');
  
  const input = document.getElementById('adminPasswordInput');
  if (input) {
    input.value = '';
    updatePasswordCounter('');
    input.focus();
  }
  showToast('Admin paneli qulflangan!', 'info');
}

function startDashboard() {
  if (isPollingActive) return;
  isPollingActive = true;
  refreshAllData();
  setInterval(() => {
    if (isPollingActive) refreshAllData(true);
  }, 4000);
}

// Digital Clock
function initClock() {
  const clockEl = document.getElementById('liveClock');
  function updateTime() {
    const now = new Date();
    const timeStr = now.toLocaleTimeString('uz-UZ', { hour12: false });
    if (clockEl) clockEl.textContent = `🕒 ${timeStr}`;
  }
  updateTime();
  setInterval(updateTime, 1000);
}

// Tab Switching
function initTabs() {
  const links = document.querySelectorAll('.nav-link[data-tab]');
  links.forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const tabId = link.getAttribute('data-tab');
      switchTab(tabId);
    });
  });

  // Order Filters
  const filterBtns = document.querySelectorAll('.filter-btn[data-filter]');
  filterBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      filterBtns.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      currentOrderFilter = btn.getAttribute('data-filter');
      renderOrdersTable();
    });
  });
}

function switchTab(tabId) {
  currentTab = tabId;
  
  // Update nav links
  document.querySelectorAll('.nav-link').forEach(link => {
    if (link.getAttribute('data-tab') === tabId) {
      link.classList.add('active');
    } else {
      link.classList.remove('active');
    }
  });

  // Update view containers
  document.querySelectorAll('.tab-view').forEach(view => {
    view.style.display = 'none';
  });

  const activeView = document.getElementById(`view-${tabId}`);
  if (activeView) {
    activeView.style.display = 'block';
  }

  // Update page title
  const titleMap = {
    dashboard: { title: 'Boshqaruv Paneli', desc: 'Vodil Eats platformasi bo\'yicha jonli tahlillar va statistika' },
    orders: { title: 'Jonli Buyurtmalar', desc: 'Barcha tushgan va faol yetkazib berish buyurtmalari' },
    restaurants: { title: 'Hamkor Restoranlar', desc: 'Vodil va Farg\'onadagi hamkor oshxonalar va menyular' },
    couriers: { title: 'Kuryerlar va Jonli Xarita', desc: 'Onlayn kuryerlar monitoringi va GPS treking' },
    customers: { title: 'Mijozlar Boshqaruvi', desc: 'Ro\'yxatdan o\'tgan barcha mijozlar profillari' }
  };

  const meta = titleMap[tabId] || titleMap.dashboard;
  const h1 = document.querySelector('.page-title-box h1');
  const p = document.querySelector('.page-title-box p');
  if (h1) h1.textContent = meta.title;
  if (p) p.textContent = meta.desc;

  // Invalidate map size if couriers tab opened
  if (tabId === 'couriers' && leafletMap) {
    setTimeout(() => {
      leafletMap.invalidateSize();
    }, 200);
  }
}

// Refresh Data from API
async function refreshAllData(isBackground = false) {
  try {
    const refreshBtnIcon = document.getElementById('refreshIcon');
    if (!isBackground && refreshBtnIcon) {
      refreshBtnIcon.classList.add('fa-spin');
    }

    const [stats, orders, restaurants, couriers, customers] = await Promise.all([
      fetch(`${API_BASE}/admin/stats`).then(r => r.json()).catch(() => null),
      fetch(`${API_BASE}/admin/orders`).then(r => r.json()).catch(() => []),
      fetch(`${API_BASE}/admin/restaurants`).then(r => r.json()).catch(() => []),
      fetch(`${API_BASE}/admin/couriers`).then(r => r.json()).catch(() => []),
      fetch(`${API_BASE}/admin/customers`).then(r => r.json()).catch(() => [])
    ]);

    if (!isBackground && refreshBtnIcon) {
      setTimeout(() => refreshBtnIcon.classList.remove('fa-spin'), 500);
    }

    if (stats) {
      renderStats(stats);
      updateServerStatus(true);
    } else {
      updateServerStatus(false);
    }

    if (Array.isArray(orders)) {
      // Check for new incoming orders
      if (previousOrderCount > 0 && orders.length > previousOrderCount) {
        showToast('🔔 Yangi buyurtma kelib tushdi!', 'info');
      }
      previousOrderCount = orders.length;
      currentOrders = orders;
      renderOrdersTable();
      renderRecentOrdersTicker();
    }

    if (Array.isArray(restaurants)) {
      currentRestaurants = restaurants;
      renderRestaurantsGrid();
    }

    if (Array.isArray(couriers)) {
      currentCouriers = couriers;
      renderCouriersTable();
      updateMapMarkers();
    }

    if (Array.isArray(customers)) {
      currentCustomers = customers;
      renderCustomersTable();
    }

  } catch (err) {
    console.error('Data refresh error:', err);
    updateServerStatus(false);
  }
}

// Server Connection Status
function updateServerStatus(isOnline) {
  const dot = document.getElementById('serverStatusDot');
  const text = document.getElementById('serverStatusText');
  if (dot && text) {
    if (isOnline) {
      dot.style.backgroundColor = '#10b981';
      dot.style.boxShadow = '0 0 10px #10b981';
      text.textContent = 'Server: Onlayn (3000)';
    } else {
      dot.style.backgroundColor = '#ef4444';
      dot.style.boxShadow = '0 0 10px #ef4444';
      text.textContent = 'Server: Aloqa yo\'q';
    }
  }
}

// Render Stats
function renderStats(stats) {
  const formatMoney = (val) => new Intl.NumberFormat('uz-UZ').format(val || 0) + " so'm";
  
  const elRevenue = document.getElementById('statRevenue');
  const elTotalOrders = document.getElementById('statTotalOrders');
  const elActiveOrders = document.getElementById('statActiveOrders');
  const elDelivered = document.getElementById('statDelivered');
  const elRestaurants = document.getElementById('statRestaurants');
  const elCouriers = document.getElementById('statCouriers');
  const navActiveBadge = document.getElementById('navActiveOrdersBadge');

  if (elRevenue) elRevenue.textContent = formatMoney(stats.totalRevenue);
  if (elTotalOrders) elTotalOrders.textContent = stats.totalOrders || 0;
  if (elActiveOrders) elActiveOrders.textContent = stats.activeOrders || 0;
  if (elDelivered) elDelivered.textContent = stats.deliveredOrders || 0;
  if (elRestaurants) elRestaurants.textContent = `${stats.activeRestaurants || 0} / ${stats.totalRestaurants || 0}`;
  if (elCouriers) elCouriers.textContent = `${stats.onlineCouriers || 0} / ${stats.totalCouriers || 0}`;

  if (navActiveBadge) {
    navActiveBadge.textContent = stats.activeOrders || 0;
    navActiveBadge.style.display = (stats.activeOrders > 0) ? 'inline-block' : 'none';
  }
}

// Format status badge
function getStatusBadge(status) {
  const map = {
    pending: { label: 'Kutilmoqda', class: 'badge-pending' },
    confirmed: { label: 'Tasdiqlangan', class: 'badge-confirmed' },
    preparing: { label: 'Tayyorlanmoqda', class: 'badge-preparing' },
    ready_for_pickup: { label: 'Tayyor (Kuryerga)', class: 'badge-preparing' },
    courier_assigned: { label: 'Kuryer tayinlandi', class: 'badge-delivering' },
    courier_picking_up: { label: 'Kuryer oshxonada', class: 'badge-delivering' },
    courier_picked_up: { label: 'Kuryer yo\'lda', class: 'badge-delivering' },
    delivering: { label: 'Yetkazilmoqda', class: 'badge-delivering' },
    delivered: { label: 'Yetkazildi ✅', class: 'badge-delivered' },
    cancelled: { label: 'Bekor qilingan ❌', class: 'badge-cancelled' },
  };

  const s = map[status] || { label: status, class: 'badge-pending' };
  return `<span class="badge ${s.class}">${s.label}</span>`;
}

// Render Orders Table
function renderOrdersTable() {
  const tbody = document.getElementById('ordersTableBody');
  if (!tbody) return;

  let filtered = currentOrders;
  if (currentOrderFilter === 'ACTIVE') {
    const active = ['pending', 'confirmed', 'preparing', 'ready_for_pickup', 'courier_assigned', 'courier_picking_up', 'courier_picked_up', 'delivering'];
    filtered = currentOrders.filter(o => active.includes(o.status));
  } else if (currentOrderFilter === 'DELIVERED') {
    filtered = currentOrders.filter(o => o.status === 'delivered');
  } else if (currentOrderFilter === 'CANCELLED') {
    filtered = currentOrders.filter(o => o.status === 'cancelled');
  }

  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" style="text-align: center; color: var(--text-muted); padding: 32px;">Ushbu filtr bo'yicha buyurtmalar topilmadi.</td></tr>`;
    return;
  }

  const formatMoney = (val) => new Intl.NumberFormat('uz-UZ').format(val || 0) + " so'm";
  const formatDate = (d) => {
    if (!d) return '-';
    const dt = new Date(d);
    return dt.toLocaleTimeString('uz-UZ', { hour: '2-digit', minute: '2-digit' }) + ' (' + dt.toLocaleDateString('uz-UZ') + ')';
  };

  tbody.innerHTML = filtered.map(o => `
    <tr>
      <td><strong>${o.orderNumber}</strong></td>
      <td>${formatDate(o.createdAt)}</td>
      <td>
        <div style="font-weight:600;">${o.customer ? (o.customer.firstName + ' ' + (o.customer.lastName || '')) : 'Noma\'lum'}</div>
        <div style="font-size:11px; color:var(--text-muted);">${o.customer ? o.customer.phone : '-'}</div>
      </td>
      <td>
        <div style="font-weight:600; color: #818cf8;">${o.restaurant ? o.restaurant.name : 'Noma\'lum'}</div>
      </td>
      <td style="max-width: 220px; font-size:12px; color:var(--text-secondary);">
        ${o.deliveryAddress || '-'}
        ${o.customerNote ? `<div style="color:var(--warning); font-size:11px;">📝 ${o.customerNote}</div>` : ''}
      </td>
      <td><strong style="color:var(--success);">${formatMoney(o.totalAmount)}</strong></td>
      <td>${getStatusBadge(o.status)}</td>
      <td>
        <button class="btn btn-secondary btn-sm" onclick="openOrderModal('${o.id}')">
          <i class="fas fa-eye"></i> Ko'rish
        </button>
      </td>
    </tr>
  `).join('');
}

// Render Recent Orders on Dashboard Ticker
function renderRecentOrdersTicker() {
  const container = document.getElementById('recentOrdersTicker');
  if (!container) return;

  const top5 = currentOrders.slice(0, 5);
  if (top5.length === 0) {
    container.innerHTML = `<div style="text-align:center; color:var(--text-muted); padding:20px;">Hozircha buyurtmalar yo'q.</div>`;
    return;
  }

  const formatMoney = (val) => new Intl.NumberFormat('uz-UZ').format(val || 0) + " so'm";

  container.innerHTML = top5.map(o => `
    <div style="display:flex; align-items:center; justify-content:space-between; padding:12px 16px; border-bottom:1px solid var(--border-color);">
      <div>
        <span style="font-weight:700; color:#fff;">${o.orderNumber}</span>
        <span style="font-size:12px; color:var(--text-secondary); margin-left:8px;">${o.restaurant ? o.restaurant.name : ''}</span>
      </div>
      <div style="display:flex; align-items:center; gap:12px;">
        <span style="font-weight:600; color:var(--success);">${formatMoney(o.totalAmount)}</span>
        ${getStatusBadge(o.status)}
      </div>
    </div>
  `).join('');
}

// Render Restaurants Grid
function renderRestaurantsGrid() {
  const grid = document.getElementById('restaurantsGrid');
  if (!grid) return;

  if (currentRestaurants.length === 0) {
    grid.innerHTML = `<div style="color:var(--text-muted); padding:32px;">Restoranlar topilmadi.</div>`;
    return;
  }

  grid.innerHTML = currentRestaurants.map(r => `
    <div class="restaurant-card">
      <div class="restaurant-cover" style="background-image: url('${r.coverImageUrl || r.imageUrl || 'https://images.unsplash.com/photo-1555396273-367ea4eb4db5?w=500'}');">
        <div class="restaurant-status-pill">
          ${r.isActive ? '<span class="badge badge-online">Faol</span>' : '<span class="badge badge-offline">Yopiq</span>'}
        </div>
      </div>
      <div class="restaurant-info">
        <div class="restaurant-name">${r.name}</div>
        <div class="restaurant-address"><i class="fas fa-map-marker-alt"></i> ${r.address}</div>
        
        <div style="background: rgba(99, 102, 241, 0.15); border: 1px dashed #818cf8; border-radius: 8px; padding: 6px 10px; margin: 8px 0; display: flex; align-items: center; justify-content: space-between;">
          <span style="font-size: 12px; font-weight: bold; color: #a5b4fc;">🔑 Kod: <span style="letter-spacing:1px; color:#fff; font-family:monospace; font-size:13px;">${r.accessCode || ('VDL-' + r.name.substring(0,4).toUpperCase())}</span></span>
          <button class="btn btn-secondary btn-sm" style="padding: 2px 8px; font-size: 11px;" onclick="copyRestaurantCode('${r.accessCode || ''}')" title="Kodni nusxalash">📋 Nusxa</button>
        </div>

        <div style="font-size:12px; color:var(--text-secondary); margin-bottom:12px;">
          📞 ${r.phone} | 🕒 ${r.openTime} - ${r.closeTime} | ⭐ ${r.rating} (${r.totalReviews} baho)
        </div>
        <div class="restaurant-meta">
          <span>Kategoriya: ${r.categories ? r.categories.length : 0} ta</span>
          <div style="display: flex; gap: 6px;">
            <button class="btn btn-secondary btn-sm" onclick="toggleRestaurant('${r.id}')">
              ${r.isActive ? 'Yopish' : 'Faollashtirish'}
            </button>
            <button class="btn btn-secondary btn-sm" style="color:#ef4444; border-color:rgba(239,68,68,0.3); padding:4px 8px;" onclick="deleteRestaurant('${r.id}', '${(r.name || '').replace(/'/g, "\\'")}')" title="Restoranni o'chirish">
              <i class="fas fa-trash-alt"></i> O'chirish
            </button>
          </div>
        </div>
      </div>
    </div>
  `).join('');
}

// Copy Restaurant Access Code
function copyRestaurantCode(code) {
  if (!code) return;
  navigator.clipboard.writeText(code);
  showToast(`Restoran kodi nusxalandi: ${code}`, 'success');
}

// Delete Restaurant
async function deleteRestaurant(id, name) {
  if (!confirm(`Haqiqatan ham "${name}" restoranini butunlay o'chirib tashlamoqchimisiz?`)) {
    return;
  }
  try {
    const res = await fetch(`${API_BASE}/admin/restaurants/${id}`, {
      method: 'DELETE'
    });
    if (res.ok) {
      showToast(`"${name}" restorani o'chirildi!`, 'success');
      refreshAllData(true);
    } else {
      showToast('O\'chirishda xatolik yuz berdi', 'danger');
    }
  } catch (err) {
    showToast('Server bilan bog\'lanishda xatolik', 'danger');
  }
}

// Verify or Block Courier
async function verifyCourier(id, isVerified) {
  try {
    const res = await fetch(`${API_BASE}/admin/couriers/${id}/verify`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ isVerified })
    });
    if (res.ok) {
      showToast(isVerified ? '✅ Kuryer muvaffaqiyatli tasdiqlandi!' : 'Kuryer tasdig\'i bekor qilindi', 'success');
      refreshAllData(true);
    } else {
      showToast('Xatolik yuz berdi', 'danger');
    }
  } catch (err) {
    showToast('Server bilan bog\'lanishda xatolik', 'danger');
  }
}

// Toggle Restaurant Status
async function toggleRestaurant(id) {
  try {
    const res = await fetch(`${API_BASE}/admin/restaurants/${id}/toggle`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ field: 'isActive' })
    });
    if (res.ok) {
      showToast('Restoran holati yangilandi', 'success');
      refreshAllData(true);
    }
  } catch (e) {
    showToast('Xatolik yuz berdi', 'danger');
  }
}

// Leaflet Map Initialization
function initLeafletMap() {
  const mapDiv = document.getElementById('mapContainer');
  if (!mapDiv || typeof L === 'undefined') return;

  // Center on Vodil / Fergana region (Lat: 40.1800, Lng: 71.7200)
  leafletMap = L.map('mapContainer').setView([40.1800, 71.7200], 12);

  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '© OpenStreetMap contributors',
    maxZoom: 18,
  }).addTo(leafletMap);

  // Add Restaurant Markers
  const vodilCenterMarker = L.marker([40.1800, 71.7200])
    .addTo(leafletMap)
    .bindPopup('<b>📍 Vodil Markazi</b><br>Asosiy yetkazib berish hududi');
}

// Update Map Markers with Couriers
function updateMapMarkers() {
  if (!leafletMap || typeof L === 'undefined') return;

  // Clear previous markers
  Object.values(courierMarkers).forEach(m => leafletMap.removeLayer(m));
  courierMarkers = {};

  // Default coordinates around Vodil if lat/lng is null
  const baseLat = 40.1800;
  const baseLng = 71.7200;

  currentCouriers.forEach((c, index) => {
    // Generate slight offset for demo if current coords not set
    const lat = c.currentLatitude || (baseLat + (index + 1) * 0.005);
    const lng = c.currentLongitude || (baseLng + (index + 1) * 0.006);

    const isOnline = c.status === 'online' || c.status === 'busy';
    const markerColor = c.status === 'busy' ? '#f59e0b' : (isOnline ? '#10b981' : '#6b7280');

    const customIcon = L.divIcon({
      className: 'courier-custom-marker',
      html: `<div style="background:${markerColor}; width:28px; height:28px; border-radius:50%; border:2px solid #fff; display:flex; align-items:center; justify-content:center; box-shadow:0 0 10px ${markerColor}; font-size:14px; color:#fff;">🛵</div>`,
      iconSize: [28, 28],
      iconAnchor: [14, 14],
    });

    const marker = L.marker([lat, lng], { icon: customIcon })
      .addTo(leafletMap)
      .bindPopup(`
        <div style="font-family:inherit; padding:4px;">
          <h4 style="margin:0 0 4px 0; color:#111827;">${c.user ? (c.user.firstName + ' ' + c.user.lastName) : 'Kuryer #' + (index+1)}</h4>
          <div style="font-size:12px; color:#4b5563;">Transport: ${c.vehicleType || 'Motosikl'}</div>
          <div style="font-size:12px; color:#4b5563;">Bugungi yetkazishlar: ${c.todayDeliveries || 0} ta</div>
          <div style="font-size:12px; font-weight:bold; color:${markerColor};">Holati: ${c.status}</div>
        </div>
      `);

    courierMarkers[c.id || index] = marker;
  });
}

// Render Couriers Table
function renderCouriersTable() {
  const tbody = document.getElementById('couriersTableBody');
  if (!tbody) return;

  if (currentCouriers.length === 0) {
    tbody.innerHTML = `<tr><td colspan="9" style="text-align:center; color:var(--text-muted); padding:24px;">Hozircha kuryerlar ro'yxatdan o'tmagan.</td></tr>`;
    return;
  }

  tbody.innerHTML = currentCouriers.map((c, i) => `
    <tr>
      <td><strong>#${i + 1}</strong></td>
      <td>
        <div style="font-weight:600;">${c.user ? (c.user.firstName + ' ' + (c.user.lastName || '')) : 'Kuryer'}</div>
        <div style="font-size:11px; color:var(--text-muted);">${c.vehiclePlateNumber ? ('Raqam: ' + c.vehiclePlateNumber) : ''}</div>
      </td>
      <td>${c.user ? c.user.phone : '-'}</td>
      <td><span class="badge badge-confirmed">${c.vehicleType || 'Motosikl'}</span></td>
      <td>
        ${c.isVerified ? '<span class="badge badge-online">✅ Tasdiqlangan</span>' : '<span class="badge" style="background:#f59e0b; color:#fff;">⏳ Kutilmoqda</span>'}
      </td>
      <td>⭐ ${c.rating || '5.0'}</td>
      <td>${c.todayDeliveries || 0} ta</td>
      <td>
        <span class="badge ${c.status === 'online' ? 'badge-online' : (c.status === 'busy' ? 'badge-pending' : 'badge-offline')}">
          ${c.status}
        </span>
      </td>
      <td>
        ${c.isVerified 
          ? `<button class="btn btn-secondary btn-sm" style="color:#ef4444; border-color:rgba(239,68,68,0.3); font-size:11px; padding:4px 8px;" onclick="verifyCourier('${c.id}', false)"><i class="fas fa-ban"></i> Bekor qilish</button>`
          : `<button class="btn btn-success btn-sm" style="font-size:11px; padding:4px 10px;" onclick="verifyCourier('${c.id}', true)"><i class="fas fa-check"></i> Tasdiqlash</button>`
        }
      </td>
    </tr>
  `).join('');
}

// Render Customers Table
function renderCustomersTable() {
  const tbody = document.getElementById('customersTableBody');
  if (!tbody) return;

  if (currentCustomers.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" style="text-align:center; color:var(--text-muted); padding:24px;">Mijozlar mavjud emas.</td></tr>`;
    return;
  }

  const formatDate = (d) => {
    if (!d) return '-';
    return new Date(d).toLocaleDateString('uz-UZ');
  };

  tbody.innerHTML = currentCustomers.map(u => `
    <tr>
      <td><strong>${u.firstName} ${u.lastName || ''}</strong></td>
      <td>${u.phone}</td>
      <td>${u.isPhoneVerified ? '✅ Tasdiqlangan' : '⚠️ Tasdiqlanmagan'}</td>
      <td>${formatDate(u.createdAt)}</td>
      <td><span class="badge badge-online">Faol</span></td>
    </tr>
  `).join('');
}

// Order Modal Functions
let selectedOrderId = null;

function openOrderModal(orderId) {
  selectedOrderId = orderId;
  const order = currentOrders.find(o => o.id === orderId);
  if (!order) return;

  const modal = document.getElementById('orderDetailModal');
  const modalContent = document.getElementById('modalOrderContent');
  if (!modal || !modalContent) return;

  const formatMoney = (val) => new Intl.NumberFormat('uz-UZ').format(val || 0) + " so'm";

  let itemsHtml = '';
  if (order.items && order.items.length > 0) {
    itemsHtml = order.items.map(item => `
      <div style="display:flex; justify-content:space-between; padding:8px 0; border-bottom:1px solid rgba(255,255,255,0.05);">
        <div>${item.name} × <strong>${item.quantity}</strong></div>
        <div>${formatMoney(item.totalPrice)}</div>
      </div>
    `).join('');
  } else {
    itemsHtml = `<div style="color:var(--text-muted);">Buyurtma tarkibi: Standart menyu elementlari</div>`;
  }

  modalContent.innerHTML = `
    <div style="margin-bottom:16px;">
      <div style="font-size:12px; color:var(--text-muted);">BUYURTMA RAQAMI</div>
      <div style="font-size:18px; font-weight:700; color:#fff;">${order.orderNumber}</div>
    </div>
    <div style="display:grid; grid-template-columns:1fr 1fr; gap:12px; margin-bottom:16px;">
      <div style="background:rgba(255,255,255,0.03); padding:12px; border-radius:8px;">
        <div style="font-size:11px; color:var(--text-muted);">MIJOZ</div>
        <div style="font-weight:600;">${order.customer ? (order.customer.firstName + ' ' + (order.customer.lastName || '')) : 'Noma\'lum'}</div>
        <div style="font-size:12px; color:var(--text-secondary);">${order.customer ? order.customer.phone : '-'}</div>
      </div>
      <div style="background:rgba(255,255,255,0.03); padding:12px; border-radius:8px;">
        <div style="font-size:11px; color:var(--text-muted);">RESTORAN</div>
        <div style="font-weight:600; color:#818cf8;">${order.restaurant ? order.restaurant.name : 'Noma\'lum'}</div>
        <div style="font-size:12px; color:var(--text-secondary);">${order.restaurant ? order.restaurant.address : '-'}</div>
      </div>
    </div>
    <div style="background:rgba(255,255,255,0.03); padding:12px; border-radius:8px; margin-bottom:16px;">
      <div style="font-size:11px; color:var(--text-muted);">YETKAZISH MANZILI</div>
      <div style="font-size:13px; color:#fff;">${order.deliveryAddress || 'Vodil markazi'}</div>
      ${order.customerNote ? `<div style="margin-top:6px; color:var(--warning); font-size:12px;">📝 ${order.customerNote}</div>` : ''}
    </div>
    <div style="margin-bottom:16px;">
      <div style="font-size:12px; color:var(--text-muted); margin-bottom:8px;">BUYURTMA MAHSULOTLARI</div>
      ${itemsHtml}
    </div>
    <div style="display:flex; justify-content:space-between; align-items:center; padding-top:12px; border-top:1px solid var(--border-color); font-size:15px; font-weight:700;">
      <span>JAMI SUMMA:</span>
      <span style="color:var(--success); font-size:18px;">${formatMoney(order.totalAmount)}</span>
    </div>
  `;

  modal.classList.add('active');
}

function closeOrderModal() {
  const modal = document.getElementById('orderDetailModal');
  if (modal) modal.classList.remove('active');
}

// Change Order Status directly from Admin Panel
async function changeOrderStatus(newStatus) {
  if (!selectedOrderId) return;
  try {
    const res = await fetch(`${API_BASE}/admin/orders/${selectedOrderId}/status`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus })
    });
    if (res.ok) {
      showToast(`Status muvaffaqiyatli "${newStatus}" ga o'zgartirildi`, 'success');
      closeOrderModal();
      refreshAllData(true);
    } else {
      const err = await res.json();
      showToast(err.message || 'Statusni o\'zgartirib bo\'lmadi', 'danger');
    }
  } catch (e) {
    showToast('Server bilan bog\'lanishda xatolik', 'danger');
  }
}

// Create Demo Test Order
async function triggerDemoOrder() {
  try {
    showToast('Demo buyurtma yaratilmoqda...', 'info');
    const res = await fetch(`${API_BASE}/admin/orders/demo`, { method: 'POST' });
    if (res.ok) {
      const order = await res.json();
      showToast(`✅ Yangi test buyurtma tushdi: ${order.orderNumber}`, 'success');
      refreshAllData(true);
    } else {
      showToast('Buyurtma yaratib bo\'lmadi', 'danger');
    }
  } catch (e) {
    showToast('Server bilan bog\'lanishda xatolik', 'danger');
  }
}

// Toast Notifications
function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <i class="fas ${type === 'success' ? 'fa-check-circle' : (type === 'danger' ? 'fa-exclamation-circle' : 'fa-bell')}"></i>
    <span>${message}</span>
  `;

  container.appendChild(toast);
  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.3s ease';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// ==================== MULTI-LANGUAGE SYSTEM (UZ / RU) ====================
let currentLang = localStorage.getItem('vodil_admin_lang') || 'uz';

const i18nDict = {
  uz: {
    badgeOwner: 'Platforma Egasi',
    btnNewRestaurant: 'Yangi Restoran',
    btnExportCsv: 'CSV Hisobot',
    btnLock: 'Qulflash',
    modalNewRestaurantTitle: 'Yangi Hamkor Restoran Qo\'shish',
    labelRestName: 'Restoran Nomi *',
    labelRestAddress: 'Manzili *',
    labelRestPhone: 'Telefon Raqami *',
    labelRestDeliveryTime: 'Yetkazish Vaqti (daqiqa)',
    labelRestOpen: 'Ochilish Vaqti',
    labelRestClose: 'Yopilish Vaqti',
    labelRestImage: 'Muqova Rasmi (URL)',
    btnCancel: 'Bekor qilish',
    btnSaveRest: 'Restoranni Saqlash',
    modalNewCourierTitle: 'Yangi Kuryer Qo\'shish',
    labelCourierName: 'Ismi *',
    labelCourierLastName: 'Familiyasi',
    labelCourierPhone: 'Telefon Raqami *',
    labelCourierVehicle: 'Transport Turi',
    labelCourierPlate: 'Davlat Raqami',
    btnSaveCourier: 'Kuryerni Ro\'yxatdan O\'tkazish',
    tabRestaurantsTitle: 'Vodil va Farg\'ona Restoranlari',
    tabCouriersTitle: 'Kuryerlar Ro\'yxati',
    mapTitle: 'Jonli Kuryerlar va Buyurtmalar Xaritasi (Vodil & Farg\'ona)',
    mapLegend: '🟢 Yashil: Bo\'sh onlayn | 🟡 Sariq: Buyurtmada | ⚫ Kulrang: Oflayn'
  },
  ru: {
    badgeOwner: 'Владелец платформы',
    btnNewRestaurant: 'Новый Ресторан',
    btnExportCsv: 'Экспорт в CSV',
    btnLock: 'Заблокировать',
    modalNewRestaurantTitle: 'Добавить ресторан-партнер',
    labelRestName: 'Название ресторана *',
    labelRestAddress: 'Адрес *',
    labelRestPhone: 'Номер телефона *',
    labelRestDeliveryTime: 'Время доставки (мин)',
    labelRestOpen: 'Время открытия',
    labelRestClose: 'Время закрытия',
    labelRestImage: 'Фото обложки (URL)',
    btnCancel: 'Отмена',
    btnSaveRest: 'Сохранить ресторан',
    modalNewCourierTitle: 'Добавить курьера',
    labelCourierName: 'Имя *',
    labelCourierLastName: 'Фамилия',
    labelCourierPhone: 'Номер телефона *',
    labelCourierVehicle: 'Тип транспорта',
    labelCourierPlate: 'Гос. номер',
    btnSaveCourier: 'Зарегистрировать курьера',
    tabRestaurantsTitle: 'Рестораны Водила и Ферганы',
    tabCouriersTitle: 'Список курьеров',
    mapTitle: 'Онлайн карта курьеров и заказов (Водил & Фергана)',
    mapLegend: '🟢 Зеленый: Онлайн свободен | 🟡 Желтый: На заказе | ⚫ Серый: Офлайн'
  }
};

function applyLanguage(lang) {
  currentLang = lang;
  localStorage.setItem('vodil_admin_lang', lang);
  
  const textBtn = document.getElementById('langSwitchText');
  if (textBtn) {
    textBtn.textContent = lang === 'uz' ? "🇺🇿 O'zbekcha" : "🇷🇺 Русский";
  }

  const dict = i18nDict[lang] || i18nDict.uz;
  document.querySelectorAll('[data-i18n]').forEach(el => {
    const key = el.getAttribute('data-i18n');
    if (dict[key]) el.textContent = dict[key];
  });
}

function toggleLanguage() {
  const nextLang = currentLang === 'uz' ? 'ru' : 'uz';
  applyLanguage(nextLang);
  showToast(nextLang === 'uz' ? "Til: O'zbekcha tanlandi" : "Язык: Русский выбран", 'info');
}

// Apply on load
document.addEventListener('DOMContentLoaded', () => {
  applyLanguage(currentLang);
});

// ==================== REAL RESTAURANT MANAGEMENT ====================
function openAddRestaurantModal() {
  const modal = document.getElementById('addRestaurantModal');
  if (modal) modal.classList.add('active');
}

function closeAddRestaurantModal() {
  const modal = document.getElementById('addRestaurantModal');
  if (modal) modal.classList.remove('active');
}

async function submitNewRestaurant(e) {
  e.preventDefault();
  const name = document.getElementById('newRestName').value.trim();
  const address = document.getElementById('newRestAddress').value.trim();
  const phone = document.getElementById('newRestPhone').value.trim();
  const avgDeliveryTimeMinutes = document.getElementById('newRestDeliveryTime').value;
  const openTime = document.getElementById('newRestOpen').value.trim();
  const closeTime = document.getElementById('newRestClose').value.trim();
  const imageUrl = document.getElementById('newRestImage').value.trim();
  const accessCode = document.getElementById('newRestAccessCode') ? document.getElementById('newRestAccessCode').value.trim().toUpperCase() : '';

  try {
    const res = await fetch(`${API_BASE}/admin/restaurants`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        name,
        address,
        phone,
        accessCode: accessCode || undefined,
        avgDeliveryTimeMinutes: Number(avgDeliveryTimeMinutes) || 30,
        openTime: openTime || '09:00',
        closeTime: closeTime || '23:00',
        imageUrl: imageUrl || undefined
      })
    });

    if (res.ok) {
      showToast(currentLang === 'uz' ? '✅ Restoran muvaffaqiyatli saqlandi!' : '✅ Ресторан успешно добавлен!', 'success');
      closeAddRestaurantModal();
      document.getElementById('newRestName').value = '';
      document.getElementById('newRestAddress').value = '';
      document.getElementById('newRestPhone').value = '';
      if (document.getElementById('newRestAccessCode')) document.getElementById('newRestAccessCode').value = '';
      refreshAllData(true);
    } else {
      showToast('Xatolik: Restoranni saqlab bo\'lmadi', 'danger');
    }
  } catch (err) {
    showToast('Server bilan bog\'lanishda xatolik', 'danger');
  }
}

// ==================== REAL COURIER ONBOARDING ====================
function openAddCourierModal() {
  const modal = document.getElementById('addCourierModal');
  if (modal) modal.classList.add('active');
}

function closeAddCourierModal() {
  const modal = document.getElementById('addCourierModal');
  if (modal) modal.classList.remove('active');
}

async function submitNewCourier(e) {
  e.preventDefault();
  const firstName = document.getElementById('newCourierFirstName').value.trim();
  const lastName = document.getElementById('newCourierLastName').value.trim();
  const phone = document.getElementById('newCourierPhone').value.trim();
  const vehicleType = document.getElementById('newCourierVehicle').value;
  const vehiclePlateNumber = document.getElementById('newCourierPlate').value.trim();

  try {
    const res = await fetch(`${API_BASE}/admin/couriers`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        firstName,
        lastName,
        phone,
        vehicleType,
        vehiclePlateNumber
      })
    });

    if (res.ok) {
      showToast(currentLang === 'uz' ? '✅ Kuryer muvaffaqiyatli ro\'yxatga olindi!' : '✅ Курьер успешно зарегистрирован!', 'success');
      closeAddCourierModal();
      document.getElementById('newCourierFirstName').value = '';
      document.getElementById('newCourierLastName').value = '';
      document.getElementById('newCourierPhone').value = '';
      document.getElementById('newCourierPlate').value = '';
      refreshAllData(true);
    } else {
      showToast('Xatolik: Kuryerni ro\'yxatga olib bo\'lmadi', 'danger');
    }
  } catch (err) {
    showToast('Server bilan bog\'lanishda xatolik', 'danger');
  }
}

// ==================== REAL CSV EXPORT ====================
function exportOrdersCSV() {
  if (!currentOrders || currentOrders.length === 0) {
    showToast(currentLang === 'uz' ? 'Eksport qilish uchun buyurtmalar yo\'q' : 'Нет заказов для экспорта', 'info');
    return;
  }

  let csvContent = "data:text/csv;charset=utf-8,\uFEFF"; // UTF-8 BOM for Excel Cyrillic/Uzbek support
  csvContent += "Buyurtma_ID,Sana,Mijoz,Telefon,Restoran,Manzil,Summa,Status\n";

  currentOrders.forEach(o => {
    const orderNum = o.orderNumber || o.id;
    const date = o.createdAt ? new Date(o.createdAt).toLocaleString('uz-UZ').replace(',', '') : '';
    const customer = o.customer ? `"${o.customer.firstName} ${o.customer.lastName || ''}"` : 'Noma\'lum';
    const phone = o.customer ? o.customer.phone : '';
    const rest = o.restaurant ? `"${o.restaurant.name}"` : 'Noma\'lum';
    const addr = o.deliveryAddress ? `"${o.deliveryAddress.replace(/"/g, '""')}"` : '';
    const sum = o.totalAmount || 0;
    const status = o.status || '';

    csvContent += `${orderNum},${date},${customer},${phone},${rest},${addr},${sum},${status}\n`;
  });

  const encodedUri = encodeURI(csvContent);
  const link = document.createElement("a");
  link.setAttribute("href", encodedUri);
  link.setAttribute("download", `vodil_eats_hisobot_${new Date().toISOString().slice(0, 10)}.csv`);
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);

  showToast(currentLang === 'uz' ? '📊 CSV hisobot yuklab olindi!' : '📊 Отчет CSV успешно скачан!', 'success');
}
