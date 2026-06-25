import { initializeApp } from "https://www.gstatic.com/firebasejs/10.12.4/firebase-app.js";
import {
  getAuth,
  signInWithEmailAndPassword,
  signOut
} from "https://www.gstatic.com/firebasejs/10.12.4/firebase-auth.js";
import {
  getDatabase,
  ref,
  get,
  push,
  set,
  update,
  remove
} from "https://www.gstatic.com/firebasejs/10.12.4/firebase-database.js";

const firebaseConfig = {
  apiKey: "AIzaSyBBtPadHuq214e7YgpgCWMcU4uAKoSSRsw",
  authDomain: "notificationreader-9b068.firebaseapp.com",
  databaseURL: "https://notificationreader-9b068-default-rtdb.firebaseio.com",
  projectId: "notificationreader-9b068",
  storageBucket: "notificationreader-9b068.appspot.com",
  messagingSenderId: "48154620821",
  appId: "1:48154620821:web:admin-dashboard"
};


const SUPER_ADMIN_EMAIL = "asifrezan.office@gmail.com";
const SUPER_ADMIN_PASSWORD = "admin0011";
const FREE_MESSAGE_LIMIT = 10;
const MONTH_MS = 30 * 24 * 60 * 60 * 1000;

const planConfig = {
  messenger: {
    label: "Messenger only",
    price: 1000,
    platforms: ["Messenger"]
  },
  messenger_whatsapp: {
    label: "Messenger + WhatsApp",
    price: 1500,
    platforms: ["Messenger", "WhatsApp"]
  },
  all: {
    label: "All platforms",
    price: 2500,
    platforms: ["Messenger", "WhatsApp", "Facebook", "Imo", "Other"]
  }
};

const app = initializeApp(firebaseConfig);
const auth = getAuth(app);
const db = getDatabase(app);

let currentUser = null;
let isSuperAdmin = false;
let selectedPlatform = "Messenger";
let selectedAdminPlatform = "Messenger";
let messagesByPlatform = {};
let currentUserProfile = null;
let usersById = {};
let paymentsById = {};
let allMessagesByUser = {};
let currentUserPayments = {};
let pendingPaymentPayload = null;
let siteSettings = {};
let pendingConfirmAction = null;

const authView = document.querySelector("#authView");
const dashboardView = document.querySelector("#dashboardView");
const downloadAppButton = document.querySelector("#downloadAppButton");
const downloadMessage = document.querySelector("#downloadMessage");
const loginForm = document.querySelector("#loginForm");
const loginError = document.querySelector("#loginError");
const emailInput = document.querySelector("#emailInput");
const passwordInput = document.querySelector("#passwordInput");
const logoutButton = document.querySelector("#logoutButton");
const roleLabel = document.querySelector("#roleLabel");
const dashboardTitle = document.querySelector("#dashboardTitle");
const userNav = document.querySelector("#userNav");
const superNav = document.querySelector("#superNav");
const summaryPanel = document.querySelector("#summaryPanel");
const messagesTab = document.querySelector("#messagesTab");
const plansTab = document.querySelector("#plansTab");
const usersTab = document.querySelector("#usersTab");
const paymentsTab = document.querySelector("#paymentsTab");
const allMessagesTab = document.querySelector("#allMessagesTab");
const settingsTab = document.querySelector("#settingsTab");
const messageList = document.querySelector("#messageList");
const subscriptionBadge = document.querySelector("#subscriptionBadge");
const messageCounter = document.querySelector("#messageCounter");
const accessHint = document.querySelector("#accessHint");
const subscriptionProgress = document.querySelector("#subscriptionProgress");
const remainingDaysText = document.querySelector("#remainingDaysText");
const expiryText = document.querySelector("#expiryText");
const remainingDaysBar = document.querySelector("#remainingDaysBar");
const paymentForm = document.querySelector("#paymentForm");
const paymentMessage = document.querySelector("#paymentMessage");
const paymentStatusPanel = document.querySelector("#paymentStatusPanel");
const userList = document.querySelector("#userList");
const paymentList = document.querySelector("#paymentList");
const adminUserSelect = document.querySelector("#adminUserSelect");
const adminMessageUser = document.querySelector("#adminMessageUser");
const adminMessageCounter = document.querySelector("#adminMessageCounter");
const adminMessageList = document.querySelector("#adminMessageList");
const deleteSelectedUserMessages = document.querySelector("#deleteSelectedUserMessages");
const deleteAllMessages = document.querySelector("#deleteAllMessages");
const settingsForm = document.querySelector("#settingsForm");
const downloadLinkInput = document.querySelector("#downloadLinkInput");
const settingsMessage = document.querySelector("#settingsMessage");
const confirmModal = document.querySelector("#confirmModal");
const confirmText = document.querySelector("#confirmText");
const confirmCancel = document.querySelector("#confirmCancel");
const confirmSubmit = document.querySelector("#confirmSubmit");

loadPublicSettings();

downloadAppButton.addEventListener("click", async () => {
  downloadMessage.textContent = "";
  await withButtonState(downloadAppButton, "চেক করা হচ্ছে...", "প্রস্তুত", async () => {
    if (!siteSettings.appDownloadUrl) {
      downloadMessage.textContent = "অ্যাপটি এখন পাওয়া যাচ্ছে না। অনুগ্রহ করে পরে আবার চেষ্টা করুন।";
      return;
    }
    window.open(siteSettings.appDownloadUrl, "_blank", "noopener,noreferrer");
  });
});

loginForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  loginError.textContent = "";
  const button = loginForm.querySelector("button[type='submit']");

  await withButtonState(button, "Checking...", "Logged in", async () => {
    const email = emailInput.value.trim();
    const password = passwordInput.value.trim();

    if (email === SUPER_ADMIN_EMAIL && password === SUPER_ADMIN_PASSWORD) {
      isSuperAdmin = true;
      currentUser = { uid: "super-admin", email };
      await showSuperAdminDashboard();
      return;
    }

    const credential = await signInWithEmailAndPassword(auth, email, password);
    currentUser = credential.user;
    isSuperAdmin = false;
    await showUserDashboard();
  }).catch((error) => {
    loginError.textContent = error.message;
  });
});

logoutButton.addEventListener("click", async () => {
  await withButtonState(logoutButton, "Signing out...", "Signed out", async () => {
    if (!isSuperAdmin) {
      await signOut(auth);
    }
    currentUser = null;
    isSuperAdmin = false;
    authView.classList.remove("hidden");
    dashboardView.classList.add("hidden");
    loginForm.reset();
  });
});

document.querySelectorAll("[data-tab]").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll("[data-tab]").forEach((item) => item.classList.remove("active"));
    button.classList.add("active");
    showUserTab(button.dataset.tab);
  });
});

document.querySelectorAll("[data-admin-tab]").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll("[data-admin-tab]").forEach((item) => item.classList.remove("active"));
    button.classList.add("active");
    showAdminTab(button.dataset.adminTab);
  });
});

document.querySelectorAll("[data-platform]").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll("[data-platform]").forEach((item) => item.classList.remove("active"));
    button.classList.add("active");
    selectedPlatform = button.dataset.platform;
    renderMessages();
  });
});

document.querySelectorAll("[data-admin-platform]").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll("[data-admin-platform]").forEach((item) => item.classList.remove("active"));
    button.classList.add("active");
    selectedAdminPlatform = button.dataset.adminPlatform;
    renderAdminMessages();
  });
});

adminUserSelect.addEventListener("change", renderAdminMessages);

deleteSelectedUserMessages.addEventListener("click", () => {
  const uid = adminUserSelect.value || Object.keys(usersById)[0];
  if (!uid) return;
  const user = usersById[uid] || {};
  openConfirm(
    `Delete all messages for ${user.username || user.email || uid}? This cannot be undone.`,
    "Delete messages",
    () => deleteUserMessages(uid)
  );
});

deleteAllMessages.addEventListener("click", () => {
  openConfirm(
    "Delete all messages for all users? This cannot be undone.",
    "Delete all messages",
    deleteEveryMessage
  );
});

settingsForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const button = settingsForm.querySelector("button[type='submit']");
  await withButtonState(button, "Saving...", "Saved", async () => {
    const appDownloadUrl = downloadLinkInput.value.trim();
    await update(ref(db, "SiteSettings"), { appDownloadUrl });
    siteSettings.appDownloadUrl = appDownloadUrl;
    settingsMessage.textContent = appDownloadUrl ? "Download link saved." : "Download link removed. Users will see unavailable message.";
  });
});

paymentForm.addEventListener("submit", (event) => {
  event.preventDefault();
  paymentMessage.textContent = "";
  paymentMessage.classList.add("hidden");
  pendingConfirmAction = null;
  confirmSubmit.textContent = "Confirm submit";

  const selectedPlan = document.querySelector("input[name='plan']:checked").value;
  const plan = planConfig[selectedPlan];
  pendingPaymentPayload = {
    uid: currentUser.uid,
    email: currentUser.email || currentUserProfile?.email || "",
    planKey: selectedPlan,
    planLabel: plan.label,
    amount: plan.price,
    senderPhone: document.querySelector("#senderPhoneInput").value.trim(),
    transactionId: document.querySelector("#transactionIdInput").value.trim(),
    status: "pending",
    createdAt: Date.now()
  };

  confirmText.textContent = `Submit ${plan.label} payment request for ${plan.price} BDT using phone ${pendingPaymentPayload.senderPhone} and transaction ID ${pendingPaymentPayload.transactionId}?`;
  confirmModal.classList.remove("hidden");
});

confirmCancel.addEventListener("click", () => {
  pendingPaymentPayload = null;
  pendingConfirmAction = null;
  confirmSubmit.textContent = "Confirm submit";
  confirmModal.classList.add("hidden");
});

confirmSubmit.addEventListener("click", async () => {
  if (pendingConfirmAction) {
    await withButtonState(confirmSubmit, "Working...", "Done", async () => {
      const action = pendingConfirmAction;
      pendingConfirmAction = null;
      await action();
      confirmSubmit.textContent = "Confirm submit";
      confirmModal.classList.add("hidden");
    });
    return;
  }

  if (!pendingPaymentPayload) return;
  const submitButton = paymentForm.querySelector("button[type='submit']");

  await withButtonState(confirmSubmit, "Submitting...", "Submitted", async () => {
    const requestRef = push(ref(db, "PaymentRequests"));
    await set(requestRef, pendingPaymentPayload);
    currentUserPayments[requestRef.key] = pendingPaymentPayload;
    paymentForm.reset();
    paymentMessage.textContent = "Payment request submitted. Please wait 24 hours to activate subscription, or contact support at: 01627957310";
    paymentMessage.classList.remove("hidden");
    renderPaymentStatus();
    submitButton.textContent = "Request submitted";
    submitButton.classList.add("is-success");
    pendingPaymentPayload = null;
    confirmModal.classList.add("hidden");
  });
});

async function showUserDashboard() {
  roleLabel.textContent = "User Dashboard";
  dashboardTitle.textContent = currentUser.email || "Messages";
  userNav.classList.remove("hidden");
  superNav.classList.add("hidden");
  authView.classList.add("hidden");
  dashboardView.classList.remove("hidden");
  showUserTab("messages");

  const profileSnap = await get(ref(db, `Users/${currentUser.uid}`));
  currentUserProfile = profileSnap.val() || {};

  const messagesSnap = await get(ref(db, `Messages/${currentUser.uid}`));
  messagesByPlatform = messagesSnap.val() || {};
  const paymentsSnap = await get(ref(db, "PaymentRequests"));
  currentUserPayments = filterPaymentsForUser(paymentsSnap.val() || {}, currentUser.uid);
  renderUserSummary();
  renderPaymentStatus();
  renderMessages();
}

async function showSuperAdminDashboard() {
  roleLabel.textContent = "Super Admin";
  dashboardTitle.textContent = "Subscription Control";
  userNav.classList.add("hidden");
  superNav.classList.remove("hidden");
  authView.classList.add("hidden");
  dashboardView.classList.remove("hidden");
  showAdminTab("users");
  await loadAdminData();
  renderSettings();
}

function showUserTab(tabName) {
  messagesTab.classList.toggle("hidden", tabName !== "messages");
  plansTab.classList.toggle("hidden", tabName !== "plans");
  usersTab.classList.add("hidden");
  paymentsTab.classList.add("hidden");
  allMessagesTab.classList.add("hidden");
  settingsTab.classList.add("hidden");
  if (tabName === "plans") renderPaymentStatus();
}

function showAdminTab(tabName) {
  messagesTab.classList.add("hidden");
  plansTab.classList.add("hidden");
  usersTab.classList.toggle("hidden", tabName !== "users");
  paymentsTab.classList.toggle("hidden", tabName !== "payments");
  allMessagesTab.classList.toggle("hidden", tabName !== "allMessages");
  settingsTab.classList.toggle("hidden", tabName !== "settings");
  if (tabName === "allMessages") renderAdminMessages();
  if (tabName === "settings") renderSettings();
}

function renderUserSummary() {
  const totalMessages = countMessages(messagesByPlatform);
  const subscription = getSubscriptionInfo(currentUserProfile);
  summaryPanel.innerHTML = `
    <article class="summary-card"><span class="muted">Total messages</span><strong>${totalMessages}</strong></article>
    <article class="summary-card"><span class="muted">Plan</span><strong>${escapeHtml(subscription.planLabel)}</strong></article>
    <article class="summary-card"><span class="muted">Days remaining</span><strong>${subscription.remainingDays}</strong></article>
    <article class="summary-card"><span class="muted">Unlocked apps</span><strong>${subscription.platformCount}</strong></article>
  `;
}

function renderAdminSummary() {
  const users = Object.values(usersById);
  const payments = Object.values(paymentsById);
  const activeUsers = users.filter((user) => getSubscriptionInfo(user).active).length;
  const pendingPayments = payments.filter((payment) => payment.status === "pending").length;
  const totalMessages = Object.values(allMessagesByUser).reduce((sum, platformMap) => sum + countMessages(platformMap), 0);

  summaryPanel.innerHTML = `
    <article class="summary-card"><span class="muted">Users</span><strong>${users.length}</strong></article>
    <article class="summary-card"><span class="muted">Subscribed</span><strong>${activeUsers}</strong></article>
    <article class="summary-card"><span class="muted">Pending payments</span><strong>${pendingPayments}</strong></article>
    <article class="summary-card"><span class="muted">Stored messages</span><strong>${totalMessages}</strong></article>
  `;
}

function renderMessages() {
  const rawMessages = messagesByPlatform[selectedPlatform] || {};
  const messages = normalizeMessages(rawMessages);
  const subscribed = hasPlatformAccess(selectedPlatform);
  const visibleMessages = subscribed ? messages : messages.slice(0, FREE_MESSAGE_LIMIT);
  const lockedMessages = subscribed ? [] : messages.slice(FREE_MESSAGE_LIMIT);

  subscriptionBadge.textContent = subscribed ? "Subscribed" : "Free";
  messageCounter.textContent = `${messages.length} messages`;
  accessHint.textContent = subscribed ? "Full access enabled" : `Free preview: ${Math.min(FREE_MESSAGE_LIMIT, messages.length)} of ${messages.length}`;
  renderSubscriptionProgress();
  messageList.innerHTML = "";

  if (messages.length === 0) {
    messageList.innerHTML = `<p class="muted">No ${selectedPlatform} messages found.</p>`;
    return;
  }

  [...visibleMessages.map((message) => ({ ...message, locked: false })),
    ...lockedMessages.map((message) => ({ ...message, locked: true }))]
    .forEach((message) => messageList.appendChild(createMessageCard(message, message.locked)));
}

function renderPaymentStatus() {
  const payments = Object.entries(currentUserPayments)
    .map(([id, payment]) => ({ id, ...payment }))
    .sort((a, b) => Number(b.createdAt || 0) - Number(a.createdAt || 0));

  paymentStatusPanel.className = "payment-status hidden";
  paymentStatusPanel.innerHTML = "";

  if (payments.length === 0) return;

  const latest = payments[0];
  if (latest.status === "rejected") {
    paymentStatusPanel.className = "payment-status warning";
    paymentStatusPanel.innerHTML = `
      <strong>Subscription request rejected.</strong>
      <p>Please check your transaction details or contact support for help.</p>
      <a class="support-link" href="https://wa.me/8801627957310?text=Hello%2C%20my%20subscription%20payment%20was%20rejected.%20Please%20help%20me%20verify%20it." target="_blank" rel="noreferrer">Message support on WhatsApp</a>
    `;
    return;
  }

  if (latest.status === "pending") {
    paymentStatusPanel.className = "payment-status";
    paymentStatusPanel.innerHTML = `
      <strong>Payment request pending.</strong>
      <p>Plan: ${escapeHtml(latest.planLabel || latest.planKey || "")} | Transaction: ${escapeHtml(latest.transactionId || "")}</p>
      <p>Please wait 24 hours to activate subscription, or contact support at: 01627957310.</p>
      <a class="support-link" href="https://wa.me/8801627957310?text=Hello%2C%20I%20need%20support%20for%20my%20pending%20subscription%20payment." target="_blank" rel="noreferrer">Message support on WhatsApp</a>
    `;
  }
}

function renderSubscriptionProgress() {
  const subscription = getSubscriptionInfo(currentUserProfile);
  if (!subscription.active) {
    subscriptionProgress.classList.add("hidden");
    return;
  }

  subscriptionProgress.classList.remove("hidden");
  remainingDaysText.textContent = `${subscription.remainingDays} days remaining`;
  expiryText.textContent = `Expires ${formatDate(subscription.expiresAt)}`;
  remainingDaysBar.style.width = `${subscription.percentRemaining}%`;
}

function hasPlatformAccess(platform) {
  const subscription = getSubscriptionInfo(currentUserProfile);
  return subscription.active && subscription.platforms.includes(platform);
}

async function loadAdminData() {
  const [usersSnap, paymentsSnap, messagesSnap, settingsSnap] = await Promise.all([
    get(ref(db, "Users")),
    get(ref(db, "PaymentRequests")),
    get(ref(db, "Messages")),
    get(ref(db, "SiteSettings"))
  ]);

  usersById = usersSnap.val() || {};
  paymentsById = paymentsSnap.val() || {};
  allMessagesByUser = messagesSnap.val() || {};
  siteSettings = settingsSnap.val() || {};
  renderAdminSummary();
  renderUsers();
  renderPayments();
  renderAdminUserSelect();
  renderAdminMessages();
}

function renderUsers() {
  const users = Object.entries(usersById);
  userList.innerHTML = users.length ? "" : "<p class='muted'>No users found.</p>";

  users.forEach(([uid, user]) => {
    const subscription = getSubscriptionInfo(user);
    const item = document.createElement("article");
    item.className = "admin-item";
    item.innerHTML = `
      <div class="admin-meta">
        <strong>${escapeHtml(user.username || user.email || uid)}</strong>
        <span>${escapeHtml(user.subscriptionStatus || "free")}</span>
      </div>
      <p>${escapeHtml(user.email || "")}</p>
      <p class="muted">Plan: ${escapeHtml(subscription.planLabel)} | ${subscription.active ? `${subscription.remainingDays} days remaining` : "not active"}</p>
      <div class="admin-actions">
        <button data-approve-user="${uid}" data-plan="messenger">Approve Messenger</button>
        <button data-approve-user="${uid}" data-plan="messenger_whatsapp">Approve Messenger + WhatsApp</button>
        <button data-approve-user="${uid}" data-plan="all">Approve All</button>
        <button data-cancel-user="${uid}" class="danger">Cancel</button>
      </div>
    `;
    userList.appendChild(item);
  });

  document.querySelectorAll("[data-approve-user]").forEach((button) => {
    button.addEventListener("click", () => withButtonState(button, "Approving...", "Approved", () => approveSubscription(button.dataset.approveUser, button.dataset.plan)));
  });

  document.querySelectorAll("[data-cancel-user]").forEach((button) => {
    button.addEventListener("click", () => withButtonState(button, "Cancelling...", "Cancelled", () => cancelSubscription(button.dataset.cancelUser)));
  });
}

function renderPayments() {
  const payments = Object.entries(paymentsById)
    .map(([id, payment]) => ({ id, ...payment }))
    .sort((a, b) => Number(b.createdAt || 0) - Number(a.createdAt || 0));

  paymentList.innerHTML = payments.length ? "" : "<p class='muted'>No payment requests found.</p>";

  payments.forEach((payment) => {
    const item = document.createElement("article");
    item.className = "admin-item";
    item.innerHTML = `
      <div class="admin-meta">
        <strong>${escapeHtml(payment.email || payment.uid || "")}</strong>
        <span>${escapeHtml(payment.status || "pending")}</span>
      </div>
      <p>${escapeHtml(payment.planLabel || payment.planKey || "")} - ${Number(payment.amount || 0)} BDT</p>
      <p class="muted">Phone: ${escapeHtml(payment.senderPhone || "")} | Transaction: ${escapeHtml(payment.transactionId || "")}</p>
      <div class="admin-actions">
        <button data-approve-payment="${payment.id}">Confirm subscription</button>
        <button data-reject-payment="${payment.id}" class="danger">Reject</button>
      </div>
    `;
    paymentList.appendChild(item);
  });

  document.querySelectorAll("[data-approve-payment]").forEach((button) => {
    button.addEventListener("click", () => {
      const payment = paymentsById[button.dataset.approvePayment];
      withButtonState(button, "Confirming...", "Confirmed", () => approveSubscription(payment.uid, payment.planKey, button.dataset.approvePayment));
    });
  });

  document.querySelectorAll("[data-reject-payment]").forEach((button) => {
    button.addEventListener("click", () => withButtonState(button, "Rejecting...", "Rejected", () => rejectPayment(button.dataset.rejectPayment)));
  });
}

function renderAdminUserSelect() {
  const users = Object.entries(usersById);
  adminUserSelect.innerHTML = users
    .map(([uid, user]) => `<option value="${uid}">${escapeHtml(user.username || user.email || uid)}</option>`)
    .join("");
}

function renderAdminMessages() {
  const uid = adminUserSelect.value || Object.keys(usersById)[0];
  if (!uid) {
    adminMessageList.innerHTML = "<p class='muted'>No users available.</p>";
    adminMessageCounter.textContent = "0 messages";
    return;
  }

  const user = usersById[uid] || {};
  const rawMessages = allMessagesByUser[uid]?.[selectedAdminPlatform] || {};
  const messages = normalizeMessages(rawMessages);
  adminMessageUser.textContent = user.username || user.email || uid;
  adminMessageCounter.textContent = `${messages.length} messages`;
  adminMessageList.innerHTML = "";

  if (messages.length === 0) {
    adminMessageList.innerHTML = `<p class="muted">No ${selectedAdminPlatform} messages found for this user.</p>`;
    return;
  }

  messages.forEach((message) => adminMessageList.appendChild(createMessageCard(message, false)));
}

function renderSettings() {
  downloadLinkInput.value = siteSettings.appDownloadUrl || "";
}

async function approveSubscription(uid, planKey, paymentId = null) {
  const expiresAt = addOneMonth(Date.now());
  await update(ref(db, `Users/${uid}`), {
    subscriptionStatus: "subscribed",
    subscriptionPlan: planKey,
    subscriptionStartedAt: Date.now(),
    subscriptionExpiresAt: expiresAt
  });

  await set(ref(db, `Subscriptions/${uid}`), {
    status: "subscribed",
    planKey,
    planLabel: planConfig[planKey].label,
    platforms: planConfig[planKey].platforms,
    startedAt: Date.now(),
    expiresAt
  });

  if (paymentId) {
    await remove(ref(db, `PaymentRequests/${paymentId}`));
  }

  await loadAdminData();
}

async function cancelSubscription(uid) {
  await update(ref(db, `Users/${uid}`), {
    subscriptionStatus: "free",
    subscriptionPlan: "",
    subscriptionStartedAt: 0,
    subscriptionExpiresAt: 0
  });
  await set(ref(db, `Subscriptions/${uid}`), {
    status: "free",
    cancelledAt: Date.now()
  });
  await loadAdminData();
}

async function rejectPayment(paymentId) {
  await update(ref(db, `PaymentRequests/${paymentId}`), {
    status: "rejected",
    rejectedAt: Date.now(),
    rejectionMessage: "Subscription request rejected. Please contact support on WhatsApp at 01627957310."
  });
  await loadAdminData();
}

async function deleteUserMessages(uid) {
  await remove(ref(db, `Messages/${uid}`));
  delete allMessagesByUser[uid];
  if (adminUserSelect.value === uid) {
    renderAdminMessages();
  }
  renderAdminSummary();
}

async function deleteEveryMessage() {
  await remove(ref(db, "Messages"));
  allMessagesByUser = {};
  renderAdminMessages();
  renderAdminSummary();
}

async function loadPublicSettings() {
  try {
    const settingsSnap = await get(ref(db, "SiteSettings"));
    siteSettings = settingsSnap.val() || {};
  } catch (error) {
    siteSettings = {};
  }
}

function openConfirm(message, submitText, action) {
  pendingPaymentPayload = null;
  pendingConfirmAction = action;
  confirmText.textContent = message;
  confirmSubmit.textContent = submitText;
  confirmModal.classList.remove("hidden");
}

function filterPaymentsForUser(payments, uid) {
  return Object.fromEntries(
    Object.entries(payments).filter(([, payment]) => payment.uid === uid)
  );
}

function createMessageCard(message, locked) {
  const item = document.createElement("article");
  item.className = locked ? "message locked" : "message";
  item.innerHTML = `
    <div class="message-meta">
      <strong>${escapeHtml(message.contactName || "Unknown")}</strong>
      <span>${formatDate(message.timestamp)}</span>
    </div>
    <p class="message-body">${escapeHtml(message.message || "")}</p>
    ${locked ? "<p class='lock-note'>Subscribe to unlock this conversation.</p>" : ""}
  `;
  return item;
}

function getSubscriptionInfo(user) {
  const plan = planConfig[user?.subscriptionPlan] || null;
  const expiresAt = Number(user?.subscriptionExpiresAt || 0);
  const startedAt = Number(user?.subscriptionStartedAt || (expiresAt ? expiresAt - MONTH_MS : 0));
  const active = user?.subscriptionStatus === "subscribed" && expiresAt > Date.now() && Boolean(plan);
  const remainingMs = active ? Math.max(0, expiresAt - Date.now()) : 0;
  const durationMs = active ? Math.max(1, expiresAt - startedAt) : MONTH_MS;

  return {
    active,
    expiresAt,
    planLabel: plan?.label || "Free",
    platforms: plan?.platforms || [],
    platformCount: plan?.platforms.length || 0,
    remainingDays: active ? Math.ceil(remainingMs / (24 * 60 * 60 * 1000)) : 0,
    percentRemaining: active ? Math.max(0, Math.min(100, Math.round((remainingMs / durationMs) * 100))) : 0
  };
}

async function withButtonState(button, loadingText, successText, action) {
  const originalText = button.textContent;
  button.textContent = loadingText;
  button.classList.add("is-loading");
  try {
    const result = await action();
    button.classList.remove("is-loading");
    button.classList.add("is-success");
    button.textContent = successText;
    setTimeout(() => {
      button.classList.remove("is-success");
      button.textContent = originalText;
    }, 1800);
    return result;
  } catch (error) {
    button.classList.remove("is-loading");
    button.textContent = originalText;
    throw error;
  }
}

function normalizeMessages(rawMessages) {
  return Object.entries(rawMessages)
    .map(([id, message]) => ({ id, ...message }))
    .sort((a, b) => Number(b.timestamp || 0) - Number(a.timestamp || 0));
}

function countMessages(platformMap) {
  return Object.values(platformMap || {}).reduce((sum, messages) => sum + Object.keys(messages || {}).length, 0);
}

function addOneMonth(timestamp) {
  const date = new Date(timestamp);
  date.setMonth(date.getMonth() + 1);
  return date.getTime();
}

function formatDate(timestamp) {
  const value = Number(timestamp || 0);
  if (!value) return "";
  return new Date(value).toLocaleString();
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}
