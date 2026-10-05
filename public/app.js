/**
 * NeighbourNet Frontend Application
 * Interacts with the Java 21 REST API server with offline fallback.
 * Features separate login sessions, cleaner requests board, and sorting by recently posted.
 */

// Application State
const state = {
  users: [],
  tasks: [],
  stats: null,
  activeUserId: localStorage.getItem('neighbournet_active_user_id') || null,
  selectedCategory: 'ALL',
  selectedStatus: 'ALL',
  selectedProximity: 'ALL',
  sortBy: 'NEWEST', // Default: Recently Posted
  searchQuery: '',
  selectedRating: 5,
  pendingAcceptTaskId: null,
  isOfflineMode: false,
  broadcasts: []
};

// Category Metadata
const CATEGORY_META = {
  ERRAND: { label: 'Errand', icon: '🛒', minTrust: 45.0, color: 'ERRAND' },
  SKILL: { label: 'Skill & Repair', icon: '⚡', minTrust: 60.0, color: 'SKILL' },
  BORROW: { label: 'Borrow Items', icon: '🔧', minTrust: 50.0, color: 'BORROW' },
  TEACH: { label: 'Teach & Tutor', icon: '📚', minTrust: 55.0, color: 'TEACH' }
};

// Rating Deltas
const RATING_DELTAS = {
  1: -5.0,
  2: -2.0,
  3: 0.0,
  4: 1.0,
  5: 2.0
};

// API Base URL
const API_BASE = window.location.protocol.startsWith('http') ? '' : 'http://localhost:8080';

// Initialize App
document.addEventListener('DOMContentLoaded', () => {
  setupEventListeners();
  fetchInitialData();
});

/* ==========================================================================
   Data Fetching & State Sync
   ========================================================================== */

async function fetchInitialData() {
  try {
    const [tasksRes, usersRes, statsRes] = await Promise.all([
      fetch(`${API_BASE}/api/tasks`).catch(() => null),
      fetch(`${API_BASE}/api/users`).catch(() => null),
      fetch(`${API_BASE}/api/stats`).catch(() => null)
    ]);

    if (tasksRes && tasksRes.ok && usersRes && usersRes.ok) {
      const tasksData = await tasksRes.json();
      const usersData = await usersRes.json();
      const statsData = statsRes && statsRes.ok ? await statsRes.json() : null;

      state.tasks = tasksData.tasks || [];
      state.users = usersData.users || [];
      state.stats = statsData ? statsData.stats : null;
      state.isOfflineMode = false;
      
      document.getElementById('connectionStatus').classList.remove('offline');
      document.getElementById('connectionStatus').querySelector('.status-text').textContent = 'Live Server';
    } else {
      useFallbackData();
    }
  } catch (err) {
    console.warn('Backend unavailable, switching to local offline simulation mode.', err);
    useFallbackData();
  }

  // Validate active logged in user ID
  if (state.activeUserId) {
    const exists = state.users.some(u => u.userId === state.activeUserId);
    if (!exists) {
      state.activeUserId = null;
      localStorage.removeItem('neighbournet_active_user_id');
    }
  }

  updateAuthUI();
  updateStatsUI();
  renderLeaderboard();
  renderResidentPicker();
  renderTasks();
  addBroadcastItem('Welcome to NeighbourNet Hyperlocal Community Exchange.');
}

function useFallbackData() {
  state.isOfflineMode = true;
  document.getElementById('connectionStatus').classList.add('offline');
  document.getElementById('connectionStatus').querySelector('.status-text').textContent = 'Mock Mode';

  state.users = [
    { userId: 'U100', name: 'Suhail Akthar', roomNo: 'A100', role: 'Student Developer', trustScore: 65.0, floor: 1 },
    { userId: 'U101', name: 'Alex Chen', roomNo: 'A101', role: 'Resident', trustScore: 52.0, floor: 1 },
    { userId: 'U102', name: 'Priya Patel', roomNo: 'A202', role: 'Tutor', trustScore: 78.5, floor: 2 },
    { userId: 'U103', name: 'Marcus Taylor', roomNo: 'A305', role: 'Tech Enthusiast', trustScore: 62.0, floor: 3 },
    { userId: 'U104', name: 'Elena Rostova', roomNo: 'A104', role: 'Resident', trustScore: 50.0, floor: 1 },
    { userId: 'U105', name: 'David Kim', roomNo: 'A401', role: 'Student', trustScore: 48.0, floor: 4 }
  ];

  const now = new Date();
  state.tasks = [
    {
      taskId: 'T1004',
      category: 'SKILL',
      categoryDesc: CATEGORY_META.SKILL.label,
      description: 'Need help debugging Java multithreading race condition for project',
      status: 'POSTED',
      minTrustScore: 60.0,
      postedBy: state.users[1],
      acceptedBy: null,
      createdAt: new Date(now.getTime() - 10 * 60 * 1000).toISOString().replace('T', ' ').substring(0, 19),
      completedAt: null,
      proofNote: '',
      verified: false
    },
    {
      taskId: 'T1003',
      category: 'ERRAND',
      categoryDesc: CATEGORY_META.ERRAND.label,
      description: 'Can someone pickup parcel package from main gate security desk?',
      status: 'POSTED',
      minTrustScore: 45.0,
      postedBy: state.users[2],
      acceptedBy: null,
      createdAt: new Date(now.getTime() - 45 * 60 * 1000).toISOString().replace('T', ' ').substring(0, 19),
      completedAt: null,
      proofNote: '',
      verified: false
    },
    {
      taskId: 'T1002',
      category: 'BORROW',
      categoryDesc: CATEGORY_META.BORROW.label,
      description: 'Borrowing an HDMI cable and extension board for study session tonight',
      status: 'ACCEPTED',
      minTrustScore: 50.0,
      postedBy: state.users[4],
      acceptedBy: state.users[0],
      createdAt: new Date(now.getTime() - 3 * 3600 * 1000).toISOString().replace('T', ' ').substring(0, 19),
      completedAt: null,
      proofNote: '',
      verified: false
    },
    {
      taskId: 'T1001',
      category: 'TEACH',
      categoryDesc: CATEGORY_META.TEACH.label,
      description: '1-hour crash course revision on SQL normal forms and query optimization',
      status: 'COMPLETED',
      minTrustScore: 55.0,
      postedBy: state.users[5],
      acceptedBy: state.users[2],
      createdAt: new Date(now.getTime() - 8 * 3600 * 1000).toISOString().replace('T', ' ').substring(0, 19),
      completedAt: new Date(now.getTime() - 2 * 3600 * 1000).toISOString().replace('T', ' ').substring(0, 19),
      proofNote: 'Completed 1-on-1 tutoring session on BCNF and Join optimization.',
      verified: false
    }
  ];
}

/* ==========================================================================
   Separate User Login & Authentication
   ========================================================================== */

function getActiveUser() {
  if (!state.activeUserId) return null;
  return state.users.find(u => u.userId === state.activeUserId) || null;
}

function loginUser(userId) {
  const user = state.users.find(u => u.userId === userId);
  if (!user) return;

  state.activeUserId = user.userId;
  localStorage.setItem('neighbournet_active_user_id', user.userId);

  updateAuthUI();
  renderTasks();
  renderResidentPicker();
  closeLoginModal();
  showToast(`Signed in as ${user.name} (Room ${user.roomNo})`, 'success');
  addBroadcastItem(`${user.name} logged into NeighbourNet.`);
}

function logoutUser() {
  const previousUser = getActiveUser();
  state.activeUserId = null;
  localStorage.removeItem('neighbournet_active_user_id');

  updateAuthUI();
  renderTasks();
  renderResidentPicker();
  showToast('Logged out successfully', 'info');
  if (previousUser) {
    addBroadcastItem(`${previousUser.name} signed out.`);
  }
}

function requireAuth(actionName = 'perform this action') {
  const activeUser = getActiveUser();
  if (activeUser) return activeUser;

  showToast(`Please sign in as a resident to ${actionName}`, 'info');
  openLoginModal();
  return null;
}

function updateAuthUI() {
  const guestControls = document.getElementById('authGuestControls');
  const userControls = document.getElementById('authUserControls');
  const activeUser = getActiveUser();

  if (activeUser) {
    guestControls.style.display = 'none';
    userControls.style.display = 'flex';

    document.getElementById('activeUserName').textContent = activeUser.name;
    document.getElementById('activeUserSub').textContent = `Room ${activeUser.roomNo} • ${activeUser.role || 'Resident'}`;
    document.getElementById('activeUserTrust').textContent = `★ ${activeUser.trustScore.toFixed(1)}`;
    document.getElementById('activeUserAvatar').textContent = activeUser.name.charAt(0).toUpperCase();

    // Modal Dynamic Text
    document.getElementById('postModalUserName').textContent = activeUser.name;
    document.getElementById('postModalUserRoom').textContent = `Room ${activeUser.roomNo}`;
  } else {
    guestControls.style.display = 'flex';
    userControls.style.display = 'none';
  }
}

function openLoginModal(tabName = 'tabSelectResident') {
  switchLoginTab(tabName);
  renderResidentPicker();
  document.getElementById('loginModal').classList.add('active');
}

function closeLoginModal() {
  document.getElementById('loginModal').classList.remove('active');
}

function switchLoginTab(tabId) {
  const tabBtns = document.querySelectorAll('.modal-tab-btn');
  const tabContents = document.querySelectorAll('.modal-tab-content');

  tabBtns.forEach(btn => {
    if (btn.dataset.tab === tabId) btn.classList.add('active');
    else btn.classList.remove('active');
  });

  tabContents.forEach(content => {
    if (content.id === tabId) content.classList.add('active');
    else content.classList.remove('active');
  });
}

function renderResidentPicker() {
  const grid = document.getElementById('residentPickerGrid');
  if (!grid) return;

  const search = (document.getElementById('residentSearchInput')?.value || '').toLowerCase().trim();
  const filtered = state.users.filter(u => {
    if (!search) return true;
    return u.name.toLowerCase().includes(search) ||
           u.roomNo.toLowerCase().includes(search) ||
           u.userId.toLowerCase().includes(search) ||
           (u.role && u.role.toLowerCase().includes(search));
  });

  if (filtered.length === 0) {
    grid.innerHTML = `<div style="grid-column: 1/-1; text-align: center; color: var(--text-muted); padding: 1.5rem 0;">No resident found matching "${escapeHtml(search)}".</div>`;
    return;
  }

  grid.innerHTML = '';
  filtered.forEach(u => {
    const card = document.createElement('div');
    const isCurrent = state.activeUserId === u.userId;
    card.className = `resident-pick-card ${isCurrent ? 'active-user' : ''}`;
    card.onclick = () => loginUser(u.userId);

    card.innerHTML = `
      <div class="pick-left">
        <div class="pick-avatar">${u.name.charAt(0).toUpperCase()}</div>
        <div class="pick-info">
          <span class="pick-name">${escapeHtml(u.name)} ${isCurrent ? '✓' : ''}</span>
          <span class="pick-sub">Room ${u.roomNo} • ${escapeHtml(u.role || 'Resident')}</span>
        </div>
      </div>
      <span class="pick-trust">★ ${u.trustScore.toFixed(1)}</span>
    `;
    grid.appendChild(card);
  });
}

/* ==========================================================================
   UI Stats & Leaderboard
   ========================================================================== */

function updateStatsUI() {
  const total = state.tasks.length;
  const posted = state.tasks.filter(t => t.status === 'POSTED').length;
  const completed = state.tasks.filter(t => t.status === 'COMPLETED' || t.status === 'CLOSED').length;
  const avgTrust = state.users.length > 0
    ? (state.users.reduce((acc, u) => acc + u.trustScore, 0) / state.users.length).toFixed(1)
    : '50.0';

  document.getElementById('statTotalTasks').textContent = total;
  document.getElementById('statActiveTasks').textContent = posted;
  document.getElementById('statCompletedTasks').textContent = completed;
  document.getElementById('statAvgTrust').textContent = avgTrust;
  document.getElementById('statTotalUsers').textContent = state.users.length;
  document.getElementById('residentCountBadge').textContent = `${state.users.length} Residents`;

  const countAll = document.getElementById('countAll');
  if (countAll) countAll.textContent = total;
}

function renderLeaderboard() {
  const list = document.getElementById('leaderboardList');
  if (!list) return;
  list.innerHTML = '';

  const sorted = [...state.users].sort((a, b) => b.trustScore - a.trustScore).slice(0, 8);

  sorted.forEach((u, idx) => {
    const item = document.createElement('div');
    item.className = 'leader-item';
    item.onclick = () => openProfileDrawer(u.userId);

    const rankClass = idx === 0 ? 'rank-1' : idx === 1 ? 'rank-2' : idx === 2 ? 'rank-3' : '';

    item.innerHTML = `
      <div class="leader-left">
        <span class="leader-rank ${rankClass}">#${idx + 1}</span>
        <div class="leader-avatar">${u.name.charAt(0).toUpperCase()}</div>
        <div class="leader-details">
          <span class="leader-name">${escapeHtml(u.name)}</span>
          <span class="leader-room">Room ${u.roomNo}</span>
        </div>
      </div>
      <span class="leader-trust">★ ${u.trustScore.toFixed(1)}</span>
    `;
    list.appendChild(item);
  });
}

/* ==========================================================================
   Cleaner Requests Board & Sorting
   ========================================================================== */

function renderTasks() {
  const container = document.getElementById('taskFeedContainer');
  const activeUser = getActiveUser();

  // 1. Filter tasks
  let filtered = state.tasks.filter(task => {
    // Category filter
    if (state.selectedCategory !== 'ALL' && task.category !== state.selectedCategory) return false;
    
    // Status filter
    if (state.selectedStatus !== 'ALL' && task.status !== state.selectedStatus) return false;

    // Proximity / Personal filter
    if (state.selectedProximity === 'MY_POSTS') {
      if (!activeUser || task.postedBy?.userId !== activeUser.userId) return false;
    }
    if (state.selectedProximity === 'MY_HELP') {
      if (!activeUser || task.acceptedBy?.userId !== activeUser.userId) return false;
    }
    if (state.selectedProximity === 'SAME_FLOOR') {
      if (activeUser && task.postedBy) {
        if (getFloor(task.postedBy.roomNo) !== getFloor(activeUser.roomNo)) return false;
      }
    }

    // Search query
    if (state.searchQuery.trim()) {
      const q = state.searchQuery.toLowerCase();
      const matchDesc = task.description.toLowerCase().includes(q);
      const matchPoster = task.postedBy?.name.toLowerCase().includes(q) || false;
      const matchRoom = task.postedBy?.roomNo.toLowerCase().includes(q) || false;
      const matchCat = task.category.toLowerCase().includes(q);
      const matchId = task.taskId.toLowerCase().includes(q);
      if (!matchDesc && !matchPoster && !matchRoom && !matchCat && !matchId) return false;
    }

    return true;
  });

  // 2. Sort tasks (Default: NEWEST - Recently Posted)
  filtered.sort((a, b) => {
    if (state.sortBy === 'NEWEST') {
      return parseDate(b.createdAt) - parseDate(a.createdAt);
    }
    if (state.sortBy === 'OLDEST') {
      return parseDate(a.createdAt) - parseDate(b.createdAt);
    }
    if (state.sortBy === 'TRUST_HIGH') {
      return (b.minTrustScore || 0) - (a.minTrustScore || 0);
    }
    if (state.sortBy === 'SAME_FLOOR' && activeUser) {
      const activeFloor = getFloor(activeUser.roomNo);
      const floorDiffA = Math.abs(getFloor(a.postedBy?.roomNo) - activeFloor);
      const floorDiffB = Math.abs(getFloor(b.postedBy?.roomNo) - activeFloor);
      return floorDiffA - floorDiffB;
    }
    return parseDate(b.createdAt) - parseDate(a.createdAt);
  });

  if (filtered.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" style="margin: 0 auto 0.75rem auto; color: var(--text-muted);">
          <circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/>
        </svg>
        <p style="font-size: 1.05rem; font-weight: 700; color: var(--text-primary); margin-bottom: 0.25rem;">No help requests found</p>
        <p style="font-size: 0.82rem;">Try switching categories, clearing search filters, or post a new request!</p>
      </div>
    `;
    return;
  }

  container.innerHTML = '';
  filtered.forEach(task => {
    const card = createCleanTaskCard(task, activeUser);
    container.appendChild(card);
  });
}

function createCleanTaskCard(task, activeUser) {
  const card = document.createElement('div');
  card.className = 'clean-task-card';

  const meta = CATEGORY_META[task.category] || { label: task.category, icon: '📌', minTrust: 50.0, color: 'ERRAND' };
  const poster = task.postedBy || { name: 'Anonymous', roomNo: '---', trustScore: 50.0, userId: '' };
  const helper = task.acceptedBy;

  const isPoster = activeUser && poster.userId === activeUser.userId;
  const isHelper = activeUser && helper && helper.userId === activeUser.userId;

  // Proximity floor calculations
  const posterFloor = getFloor(poster.roomNo);
  const activeFloor = activeUser ? getFloor(activeUser.roomNo) : null;
  const isSameFloor = activeFloor !== null && posterFloor === activeFloor;
  const floorText = isSameFloor ? '🎯 Same Floor' : `Floor ${posterFloor}`;

  // Time display
  const timeFormatted = formatRelativeTime(task.createdAt);

  // Status human label
  const statusLabel = formatStatusLabel(task.status);

  // Actions Button HTML
  let actionsHtml = '';

  if (task.status === 'POSTED') {
    if (isPoster) {
      actionsHtml = `<span class="badge-subtle">Your Request</span>`;
    } else {
      actionsHtml = `
        <button class="btn btn-primary btn-sm" onclick="openAcceptModal('${task.taskId}')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polyline points="20 6 9 17 4 12"/></svg>
          <span>Accept Request</span>
        </button>
      `;
    }
  } else if (task.status === 'ACCEPTED') {
    if (isHelper) {
      actionsHtml = `
        <button class="btn btn-success btn-sm" onclick="openCompleteModal('${task.taskId}')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
          <span>Complete Task & Attach Proof</span>
        </button>
      `;
    } else if (isPoster) {
      actionsHtml = `<span class="badge-subtle">Assigned to ${escapeHtml(helper?.name || 'Helper')}</span>`;
    } else {
      actionsHtml = `<span class="badge-subtle">In Progress</span>`;
    }
  } else if (task.status === 'COMPLETED') {
    if (isPoster) {
      actionsHtml = `
        <button class="btn btn-primary btn-sm" onclick="openRateModal('${task.taskId}')">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.2"><polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/></svg>
          <span>Rate Helper & Close</span>
        </button>
      `;
    } else if (isHelper) {
      actionsHtml = `<span class="badge-subtle">Awaiting requester's review</span>`;
    } else {
      actionsHtml = `<span class="badge-subtle">Completed</span>`;
    }
  } else if (task.status === 'CLOSED') {
    actionsHtml = `<span class="badge-subtle">✓ Closed & Verified</span>`;
  }

  // Proof note block if completed/closed
  let proofHtml = '';
  if (task.proofNote) {
    proofHtml = `
      <div class="card-proof-container">
        <strong>Completion Proof / Notes:</strong> ${escapeHtml(task.proofNote)}
      </div>
    `;
  }

  card.innerHTML = `
    <!-- Top Header Row -->
    <div class="card-header-row">
      <div class="card-pill-cluster">
        <span class="cat-badge ${meta.color}">${meta.icon} ${meta.label}</span>
        <span class="status-tag ${task.status}">
          <span class="status-dot-sm"></span>
          <span>${statusLabel}</span>
        </span>
        <span class="proximity-tag ${isSameFloor ? 'same-floor' : ''}">${floorText}</span>
      </div>
      <div class="card-time-badge" title="${task.createdAt || ''}">
        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        <span>${timeFormatted}</span>
      </div>
    </div>

    <!-- Description Body -->
    <div class="card-task-body">${escapeHtml(task.description)}</div>

    ${proofHtml}

    <!-- Footer Meta & Person Info -->
    <div class="card-footer-info">
      <!-- Poster details -->
      <div class="user-tag-link" onclick="openProfileDrawer('${poster.userId}')" title="View ${escapeHtml(poster.name)}'s Profile">
        <div class="tag-avatar">${poster.name.charAt(0).toUpperCase()}</div>
        <div>
          <span class="tag-name">${escapeHtml(poster.name)}</span>
          <span class="tag-room">(Room ${poster.roomNo})</span>
        </div>
        <span class="tag-score">★ ${poster.trustScore ? poster.trustScore.toFixed(1) : '50.0'}</span>
      </div>

      <!-- Helper / Min Trust Required Info -->
      ${helper ? `
        <div class="user-tag-link" onclick="openProfileDrawer('${helper.userId}')" title="View Helper ${escapeHtml(helper.name)}'s Profile">
          <div class="tag-avatar helper-avatar">${helper.name.charAt(0).toUpperCase()}</div>
          <div>
            <span class="tag-name">${escapeHtml(helper.name)}</span>
            <span class="tag-room">(Helper)</span>
          </div>
          <span class="tag-score">★ ${helper.trustScore ? helper.trustScore.toFixed(1) : '50.0'}</span>
        </div>
      ` : `
        <div style="font-size: 0.72rem; color: var(--text-muted); display: flex; align-items: center; gap: 0.3rem;">
          <span>Min Helper Trust:</span>
          <strong style="color: var(--text-primary); font-family: var(--font-mono);">★ ${meta.minTrust.toFixed(1)}</strong>
        </div>
      `}
    </div>

    <!-- Action Buttons -->
    <div class="card-action-bar">
      ${actionsHtml}
    </div>
  `;

  return card;
}

/* ==========================================================================
   Modals & Workflows
   ========================================================================== */

// 1. Post Task
function openPostModal() {
  const activeUser = requireAuth('post a help request');
  if (!activeUser) return;

  updateAuthUI();
  document.getElementById('taskDescription').value = '';
  document.getElementById('postTaskModal').classList.add('active');
}

async function handlePostTaskSubmit(e) {
  e.preventDefault();
  const activeUser = requireAuth('post a request');
  if (!activeUser) return;

  const category = document.querySelector('input[name="postCategory"]:checked')?.value || 'ERRAND';
  const description = document.getElementById('taskDescription').value.trim();

  if (!description) return;

  const btn = document.getElementById('submitPostTaskBtn');
  btn.disabled = true;
  btn.textContent = 'Broadcasting...';

  try {
    if (!state.isOfflineMode) {
      const res = await fetch(`${API_BASE}/api/tasks`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          posterId: activeUser.userId,
          category: category,
          description: description
        })
      });
      const data = await res.json();
      if (res.ok) {
        showToast('Request broadcasted to nearby residents!', 'success');
        addBroadcastItem(`New request posted by ${activeUser.name} (Room ${activeUser.roomNo}): "${description}"`);
        document.getElementById('postTaskModal').classList.remove('active');
        await fetchInitialData();
      } else {
        showToast(data.message || 'Error creating task', 'error');
      }
    } else {
      // Local Mock Mode
      const newId = 'T' + (1000 + state.tasks.length + 1);
      const meta = CATEGORY_META[category];
      const nowStr = new Date().toISOString().replace('T', ' ').substring(0, 19);
      const newTask = {
        taskId: newId,
        category: category,
        categoryDesc: meta.label,
        description: description,
        status: 'POSTED',
        minTrustScore: meta.minTrust,
        postedBy: activeUser,
        acceptedBy: null,
        createdAt: nowStr,
        completedAt: null,
        proofNote: '',
        verified: false
      };
      state.tasks.unshift(newTask);
      showToast('Request broadcasted to network!', 'success');
      addBroadcastItem(`New request posted by ${activeUser.name}: "${description}"`);
      document.getElementById('postTaskModal').classList.remove('active');
      updateStatsUI();
      renderTasks();
    }
  } catch (err) {
    showToast('Failed to post request', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Post Request to Network';
  }
}

// 2. Accept Task
function openAcceptModal(taskId) {
  const activeUser = requireAuth('accept this task');
  if (!activeUser) return;

  const task = state.tasks.find(t => t.taskId === taskId);
  if (!task) return;

  state.pendingAcceptTaskId = taskId;
  const meta = CATEGORY_META[task.category] || { label: task.category, minTrust: 50.0 };

  const preview = document.getElementById('acceptTaskPreview');
  preview.innerHTML = `
    <div style="font-weight: 700; font-size: 0.95rem; margin-bottom: 0.3rem;">${escapeHtml(task.description)}</div>
    <div style="font-size: 0.78rem; color: var(--text-muted);">
      Posted by <strong>${escapeHtml(task.postedBy?.name || 'Resident')}</strong> (Room ${task.postedBy?.roomNo})
    </div>
  `;

  document.getElementById('acceptMinRequired').textContent = meta.minTrust.toFixed(1);
  document.getElementById('acceptCurrentTrust').textContent = activeUser.trustScore.toFixed(1);

  const verdict = document.getElementById('acceptTrustVerdict');
  const confirmBtn = document.getElementById('confirmAcceptTaskBtn');

  if (activeUser.trustScore >= meta.minTrust) {
    verdict.className = 'trust-verdict pass';
    verdict.textContent = `✓ Qualified: Your score (${activeUser.trustScore.toFixed(1)}) meets the required ${meta.minTrust.toFixed(1)}.`;
    confirmBtn.disabled = false;
  } else {
    verdict.className = 'trust-verdict fail';
    verdict.textContent = `✗ Insufficient Trust: Requires ${meta.minTrust.toFixed(1)}, but you have ${activeUser.trustScore.toFixed(1)}. Complete smaller tasks to raise your score.`;
    confirmBtn.disabled = true;
  }

  document.getElementById('acceptTaskModal').classList.add('active');
}

async function handleConfirmAccept() {
  const activeUser = requireAuth('accept this task');
  if (!activeUser) return;

  const taskId = state.pendingAcceptTaskId;
  if (!taskId) return;

  const btn = document.getElementById('confirmAcceptTaskBtn');
  btn.disabled = true;
  btn.textContent = 'Accepting...';

  try {
    if (!state.isOfflineMode) {
      const res = await fetch(`${API_BASE}/api/tasks/${taskId}/accept`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ helperId: activeUser.userId })
      });
      const data = await res.json();
      if (res.ok) {
        showToast('Task accepted! Floor residents notified.', 'success');
        addBroadcastItem(`${activeUser.name} accepted task #${taskId}. Floor neighbors notified.`);
        document.getElementById('acceptTaskModal').classList.remove('active');
        await fetchInitialData();
      } else {
        showToast(data.message || 'Error accepting task', 'error');
      }
    } else {
      const task = state.tasks.find(t => t.taskId === taskId);
      if (task) {
        task.acceptedBy = activeUser;
        task.status = 'ACCEPTED';
        showToast('Task accepted!', 'success');
        addBroadcastItem(`${activeUser.name} accepted task #${taskId}`);
        document.getElementById('acceptTaskModal').classList.remove('active');
        renderTasks();
        updateStatsUI();
      }
    }
  } catch (err) {
    showToast('Failed to accept task', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Confirm & Accept Task';
  }
}

// 3. Complete Task
function openCompleteModal(taskId) {
  const activeUser = requireAuth('complete this task');
  if (!activeUser) return;

  document.getElementById('completeTaskId').value = taskId;
  document.getElementById('completionProofInput').value = '';
  document.getElementById('completeTaskModal').classList.add('active');
}

async function handleCompleteTaskSubmit(e) {
  e.preventDefault();
  const activeUser = requireAuth('complete this task');
  if (!activeUser) return;

  const taskId = document.getElementById('completeTaskId').value;
  const proofNote = document.getElementById('completionProofInput').value.trim();

  if (!taskId || !proofNote) return;

  const btn = document.getElementById('confirmCompleteTaskBtn');
  btn.disabled = true;
  btn.textContent = 'Submitting Proof...';

  try {
    if (!state.isOfflineMode) {
      const res = await fetch(`${API_BASE}/api/tasks/${taskId}/complete`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          helperId: activeUser.userId,
          proofNote: proofNote
        })
      });
      const data = await res.json();
      if (res.ok) {
        showToast('Task completed and proof logged!', 'success');
        addBroadcastItem(`Helper completed task #${taskId}. Proof attached: "${proofNote}"`);
        document.getElementById('completeTaskModal').classList.remove('active');
        await fetchInitialData();
      } else {
        showToast(data.message || 'Error completing task', 'error');
      }
    } else {
      const task = state.tasks.find(t => t.taskId === taskId);
      if (task) {
        task.status = 'COMPLETED';
        task.proofNote = proofNote;
        task.completedAt = new Date().toISOString().replace('T', ' ').substring(0, 19);
        showToast('Task marked as completed!', 'success');
        addBroadcastItem(`Helper completed task #${taskId}`);
        document.getElementById('completeTaskModal').classList.remove('active');
        renderTasks();
        updateStatsUI();
      }
    }
  } catch (err) {
    showToast('Failed to complete task', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Submit Proof & Complete';
  }
}

// 4. Rate Task
function openRateModal(taskId) {
  const activeUser = requireAuth('rate this task');
  if (!activeUser) return;

  const task = state.tasks.find(t => t.taskId === taskId);
  if (!task) return;

  document.getElementById('rateTaskId').value = taskId;
  state.selectedRating = 5;
  updateRatingStarsUI(5);

  const helper = task.acceptedBy || { name: 'Helper', roomNo: '---', trustScore: 50.0 };
  const card = document.getElementById('rateHelperCard');
  card.innerHTML = `
    <div style="background: var(--bg-subtle); padding: 0.75rem; border-radius: var(--radius-md); font-size: 0.82rem;">
      <div>Helper: <strong>${escapeHtml(helper.name)}</strong> (Room ${helper.roomNo})</div>
      <div>Current Trust Score: <strong>★ ${helper.trustScore.toFixed(1)}</strong></div>
      ${task.proofNote ? `<div style="margin-top: 0.35rem; color: var(--text-secondary);">Proof: "${escapeHtml(task.proofNote)}"</div>` : ''}
    </div>
  `;

  document.getElementById('rateTaskModal').classList.add('active');
}

function updateRatingStarsUI(val) {
  state.selectedRating = val;
  const stars = document.querySelectorAll('.star-btn');
  stars.forEach((s, idx) => {
    if (idx < val) s.classList.add('active');
    else s.classList.remove('active');
  });

  const delta = RATING_DELTAS[val];
  const deltaSign = delta > 0 ? `+${delta.toFixed(1)}` : delta.toFixed(1);
  const badge = document.getElementById('reputationImpact');
  badge.innerHTML = `⭐ ${val} Stars: Helper Trust Score changes by <strong>${deltaSign}</strong> points`;
}

async function handleRateTaskSubmit(e) {
  e.preventDefault();
  const activeUser = requireAuth('rate helper');
  if (!activeUser) return;

  const taskId = document.getElementById('rateTaskId').value;
  const feedback = document.getElementById('rateFeedbackInput').value.trim();

  if (!taskId) return;

  const btn = document.getElementById('confirmRateTaskBtn');
  btn.disabled = true;
  btn.textContent = 'Finalizing...';

  try {
    if (!state.isOfflineMode) {
      const res = await fetch(`${API_BASE}/api/tasks/${taskId}/rate`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          posterId: activeUser.userId,
          rating: state.selectedRating,
          feedback: feedback
        })
      });
      const data = await res.json();
      if (res.ok) {
        showToast('Rating submitted and helper trust updated!', 'success');
        addBroadcastItem(`Task #${taskId} finalized with ${state.selectedRating}★ rating. TrustEngine updated reputation scores.`);
        document.getElementById('rateTaskModal').classList.remove('active');
        await fetchInitialData();
      } else {
        showToast(data.message || 'Error rating task', 'error');
      }
    } else {
      const task = state.tasks.find(t => t.taskId === taskId);
      if (task) {
        task.status = 'CLOSED';
        task.verified = true;
        if (task.acceptedBy) {
          const delta = RATING_DELTAS[state.selectedRating] || 0;
          task.acceptedBy.trustScore = Math.max(0, Math.min(100, task.acceptedBy.trustScore + delta));
        }
        showToast('Rating submitted & task closed!', 'success');
        addBroadcastItem(`Task #${taskId} rated ${state.selectedRating}★. Trust score updated.`);
        document.getElementById('rateTaskModal').classList.remove('active');
        renderTasks();
        renderLeaderboard();
        updateAuthUI();
        updateStatsUI();
      }
    }
  } catch (err) {
    showToast('Failed to rate helper', 'error');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Submit Rating & Close';
  }
}

// 5. Direct Sign In & Registration
function handleDirectSignIn(e) {
  e.preventDefault();
  const query = document.getElementById('directUserIdInput').value.trim().toLowerCase();
  if (!query) return;

  const match = state.users.find(u =>
    u.userId.toLowerCase() === query ||
    u.roomNo.toLowerCase() === query ||
    u.name.toLowerCase() === query
  );

  if (match) {
    loginUser(match.userId);
  } else {
    showToast(`No resident account found for "${query}". Try registering as a new resident.`, 'error');
  }
}

async function handleRegisterUserSubmit(e) {
  e.preventDefault();
  const name = document.getElementById('regUserName').value.trim();
  const room = document.getElementById('regUserRoom').value.trim();
  const role = document.getElementById('regUserRole').value.trim() || 'Resident';

  if (!name || !room) return;

  try {
    if (!state.isOfflineMode) {
      const res = await fetch(`${API_BASE}/api/users`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ name, room, role })
      });
      const data = await res.json();
      if (res.ok && data.user) {
        showToast(`Resident ${name} registered!`, 'success');
        addBroadcastItem(`Welcome new neighbor ${name} (Room ${room}) to the network.`);
        await fetchInitialData();
        loginUser(data.user.userId);
      } else {
        showToast(data.message || 'Error registering resident', 'error');
      }
    } else {
      const newId = 'U' + (100 + state.users.length + 1);
      const newUser = {
        userId: newId,
        name,
        roomNo: room,
        role,
        trustScore: 50.0,
        floor: getFloor(room)
      };
      state.users.push(newUser);
      loginUser(newId);
      updateStatsUI();
      renderLeaderboard();
    }
  } catch (err) {
    showToast('Failed to register resident', 'error');
  }
}

// 6. Profile Drawer
async function openProfileDrawer(userId) {
  const user = state.users.find(u => u.userId === userId);
  if (!user) return;

  const content = document.getElementById('profileDrawerContent');
  content.innerHTML = `
    <div class="profile-card-top">
      <div class="profile-avatar-lg">${user.name.charAt(0).toUpperCase()}</div>
      <div class="profile-details-lg">
        <h3>${escapeHtml(user.name)}</h3>
        <p>Room ${user.roomNo} • ${escapeHtml(user.role || 'Resident')}</p>
        <div style="margin-top: 0.35rem;">
          <span class="trust-pill" style="font-size: 0.8rem; padding: 0.2rem 0.5rem; background: #dcfce7; color: #15803d; font-weight: 800; border-radius: 4px;">★ ${user.trustScore.toFixed(1)} Trust Score</span>
        </div>
      </div>
    </div>

    <div>
      <h4 style="font-size: 0.88rem; font-weight: 700; margin-bottom: 0.5rem;">Reputation Update History</h4>
      <div class="history-timeline" id="drawerTrustHistory">
        <div class="spinner"></div>
      </div>
    </div>
  `;

  document.getElementById('profileDrawerOverlay').classList.add('active');

  // Fetch trust history
  try {
    if (!state.isOfflineMode) {
      const res = await fetch(`${API_BASE}/api/users/${userId}`);
      if (res.ok) {
        const data = await res.json();
        renderTrustHistoryTimeline(data.trustHistory || []);
        return;
      }
    }
  } catch (ignored) {}

  // Fallback mock history
  renderTrustHistoryTimeline([
    { reason: 'Initial account setup and baseline verification', delta: 0, timestamp: 'Account Creation' },
    { reason: 'Task completion rating (5 Stars)', delta: 2.0, timestamp: 'Recent Rating' }
  ]);
}

function renderTrustHistoryTimeline(history) {
  const container = document.getElementById('drawerTrustHistory');
  if (!container) return;

  if (history.length === 0) {
    container.innerHTML = '<p style="font-size: 0.78rem; color: var(--text-muted);">No reputation score adjustments yet.</p>';
    return;
  }

  container.innerHTML = '';
  history.slice().reverse().forEach(h => {
    const item = document.createElement('div');
    item.className = 'history-item';
    const delta = typeof h.delta === 'number' ? h.delta : (h.newScore - h.oldScore) || 0;
    const deltaClass = delta >= 0 ? 'positive' : 'negative';
    const deltaSign = delta > 0 ? `+${delta.toFixed(1)}` : delta.toFixed(1);

    item.innerHTML = `
      <div class="history-header">
        <span>${escapeHtml(h.reason || 'Trust Adjustment')}</span>
        <span class="history-delta ${deltaClass}">${deltaSign}</span>
      </div>
      <div style="font-size: 0.68rem; color: var(--text-muted);">${h.timestamp || ''}</div>
    `;
    container.appendChild(item);
  });
}

/* ==========================================================================
   Activity Broadcast Feed
   ========================================================================== */

function addBroadcastItem(text) {
  const feed = document.getElementById('activityFeed');
  if (!feed) return;

  const item = document.createElement('div');
  item.className = 'activity-item';
  const time = new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });

  item.innerHTML = `
    <div>
      <div class="activity-text">${escapeHtml(text)}</div>
      <div class="activity-time">${time}</div>
    </div>
  `;

  feed.insertBefore(item, feed.firstChild);

  while (feed.children.length > 12) {
    feed.removeChild(feed.lastChild);
  }
}

/* ==========================================================================
   Toast Notifications
   ========================================================================== */

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.textContent = message;
  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateY(10px)';
    toast.style.transition = 'all 0.2s ease';
    setTimeout(() => toast.remove(), 200);
  }, 3500);
}

/* ==========================================================================
   Event Listeners
   ========================================================================== */

function setupEventListeners() {
  // Category tabs
  const tabPills = document.querySelectorAll('.tab-pill');
  tabPills.forEach(btn => {
    btn.addEventListener('click', () => {
      tabPills.forEach(b => b.classList.remove('active'));
      btn.classList.add('active');
      state.selectedCategory = btn.dataset.category;
      renderTasks();
    });
  });

  // Search input
  const searchInput = document.getElementById('searchInput');
  const clearSearchBtn = document.getElementById('clearSearchBtn');
  searchInput.addEventListener('input', (e) => {
    state.searchQuery = e.target.value;
    clearSearchBtn.style.display = state.searchQuery ? 'block' : 'none';
    renderTasks();
  });
  clearSearchBtn.addEventListener('click', () => {
    searchInput.value = '';
    state.searchQuery = '';
    clearSearchBtn.style.display = 'none';
    renderTasks();
  });

  // Sort Selector
  document.getElementById('sortBySelect').addEventListener('change', (e) => {
    state.sortBy = e.target.value;
    renderTasks();
  });

  // Filter Dropdowns
  document.getElementById('statusFilter').addEventListener('change', (e) => {
    state.selectedStatus = e.target.value;
    renderTasks();
  });

  document.getElementById('proximityFilter').addEventListener('change', (e) => {
    state.selectedProximity = e.target.value;
    renderTasks();
  });

  // Auth Action Triggers
  document.getElementById('btnOpenLoginModal').addEventListener('click', () => openLoginModal('tabSelectResident'));
  document.getElementById('btnOpenRegisterModal').addEventListener('click', () => openLoginModal('tabRegisterNew'));
  document.getElementById('btnSwitchUser').addEventListener('click', () => openLoginModal('tabSelectResident'));
  document.getElementById('btnLogout').addEventListener('click', logoutUser);
  document.getElementById('userProfileCapsule').addEventListener('click', () => {
    const active = getActiveUser();
    if (active) openProfileDrawer(active.userId);
  });

  // Login Modal Tabs
  const modalTabBtns = document.querySelectorAll('.modal-tab-btn');
  modalTabBtns.forEach(btn => {
    btn.addEventListener('click', () => switchLoginTab(btn.dataset.tab));
  });

  // Resident Search inside login modal
  const residentSearchInput = document.getElementById('residentSearchInput');
  if (residentSearchInput) {
    residentSearchInput.addEventListener('input', renderResidentPicker);
  }

  // Category Radio in Post Modal
  const categoryRadios = document.querySelectorAll('input[name="postCategory"]');
  categoryRadios.forEach(radio => {
    radio.addEventListener('change', () => {
      document.querySelectorAll('.category-radio-card').forEach(card => card.classList.remove('active'));
      radio.closest('.category-radio-card').classList.add('active');
    });
  });

  // Star Rating Buttons
  const starBtns = document.querySelectorAll('.star-btn');
  starBtns.forEach(btn => {
    btn.addEventListener('click', () => {
      const val = parseInt(btn.dataset.val, 10);
      updateRatingStarsUI(val);
    });
  });

  // Modal Open Buttons
  document.getElementById('btnPostTaskModal').addEventListener('click', openPostModal);

  // Modal Close Buttons
  document.getElementById('closeLoginModal').addEventListener('click', closeLoginModal);
  document.getElementById('closePostTaskModal').addEventListener('click', () => document.getElementById('postTaskModal').classList.remove('active'));
  document.getElementById('cancelPostTaskModal').addEventListener('click', () => document.getElementById('postTaskModal').classList.remove('active'));

  document.getElementById('closeAcceptTaskModal').addEventListener('click', () => document.getElementById('acceptTaskModal').classList.remove('active'));
  document.getElementById('cancelAcceptTaskModal').addEventListener('click', () => document.getElementById('acceptTaskModal').classList.remove('active'));

  document.getElementById('closeCompleteTaskModal').addEventListener('click', () => document.getElementById('completeTaskModal').classList.remove('active'));
  document.getElementById('cancelCompleteTaskModal').addEventListener('click', () => document.getElementById('completeTaskModal').classList.remove('active'));

  document.getElementById('closeRateTaskModal').addEventListener('click', () => document.getElementById('rateTaskModal').classList.remove('active'));
  document.getElementById('cancelRateTaskModal').addEventListener('click', () => document.getElementById('rateTaskModal').classList.remove('active'));

  document.getElementById('closeProfileDrawer').addEventListener('click', () => document.getElementById('profileDrawerOverlay').classList.remove('active'));
  document.getElementById('profileDrawerOverlay').addEventListener('click', (e) => {
    if (e.target === document.getElementById('profileDrawerOverlay')) {
      document.getElementById('profileDrawerOverlay').classList.remove('active');
    }
  });

  // Modal Forms
  document.getElementById('directSignInForm').addEventListener('submit', handleDirectSignIn);
  document.getElementById('registerUserForm').addEventListener('submit', handleRegisterUserSubmit);
  document.getElementById('postTaskForm').addEventListener('submit', handlePostTaskSubmit);
  document.getElementById('confirmAcceptTaskBtn').addEventListener('click', handleConfirmAccept);
  document.getElementById('completeTaskForm').addEventListener('submit', handleCompleteTaskSubmit);
  document.getElementById('rateTaskForm').addEventListener('submit', handleRateTaskSubmit);
}

/* ==========================================================================
   Utilities
   ========================================================================== */

function getFloor(roomNo) {
  if (!roomNo) return 1;
  const digits = roomNo.replace(/\D/g, '');
  return digits.length > 0 ? parseInt(digits.charAt(0), 10) : 1;
}

function parseDate(dateStr) {
  if (!dateStr) return 0;
  // Handle formats like "2026-08-19 14:30:00" or ISO
  const d = new Date(dateStr.replace(' ', 'T'));
  return isNaN(d.getTime()) ? 0 : d.getTime();
}

function formatRelativeTime(dateStr) {
  if (!dateStr) return 'Just now';
  const timestamp = parseDate(dateStr);
  if (!timestamp) return dateStr;

  const now = Date.now();
  const diffSec = Math.floor((now - timestamp) / 1000);

  if (diffSec < 45) return 'Just now';
  if (diffSec < 3600) return `${Math.max(1, Math.floor(diffSec / 60))}m ago`;
  if (diffSec < 86400) return `${Math.floor(diffSec / 3600)}h ago`;
  if (diffSec < 172800) return 'Yesterday';

  const d = new Date(timestamp);
  return d.toLocaleDateString([], { month: 'short', day: 'numeric' });
}

function formatStatusLabel(status) {
  switch (status) {
    case 'POSTED': return 'Open for Help';
    case 'ACCEPTED': return 'In Progress';
    case 'COMPLETED': return 'Needs Rating';
    case 'CLOSED': return 'Closed';
    default: return status;
  }
}

function escapeHtml(str) {
  if (!str) return '';
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#039;');
}

// Global modal triggers for inline onclicks
window.openAcceptModal = openAcceptModal;
window.openCompleteModal = openCompleteModal;
window.openRateModal = openRateModal;
window.openProfileDrawer = openProfileDrawer;
window.loginUser = loginUser;
