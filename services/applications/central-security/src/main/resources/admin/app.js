// ─── Global state ─────────────────────────────────────────────────────────────
const state = {
  accessToken: localStorage.getItem("admin_access_token") || "",
  refreshToken: localStorage.getItem("admin_refresh_token") || "",
  pendingChallengeId: "",
  pendingChallengeChannel: "",
  me: null,
  rolesMap: {},        // id -> name
  privilegeMap: {},    // id -> name
  allPrivileges: [],   // [{id, name}] full list
};

const permissions = {
  can(code) {
    return !!state.me?.authorities?.includes(code);
  },
};

const ui = {
  authOutput: document.getElementById("auth-output"),
  toolbar: document.getElementById("toolbar"),
  identity: document.getElementById("identity"),
  matrixCard: document.getElementById("matrix-card"),
  permMatrixCard: document.getElementById("perm-matrix-card"),
  permGroupedCard: document.getElementById("perm-grouped-card"),
  driftCard: document.getElementById("drift-card"),
  policyCard: document.getElementById("policy-card"),
  sodCard: document.getElementById("sod-card"),
  hierarchyCard: document.getElementById("hierarchy-card"),
  usersCard: document.getElementById("users-card"),
  rolesCard: document.getElementById("roles-card"),
  privilegesCard: document.getElementById("privileges-card"),
  urlsCard: document.getElementById("urls-card"),
  roleDepsCard: document.getElementById("role-deps-card"),
  patientsCard: document.getElementById("patients-card"),
};

const API_V1_BASE = "/api/v1";
const apiV1 = (path) => `${API_V1_BASE}${path}`;
const SYSTEM_ROLE_NAMES = new Set(["SUPER_ADMIN", "ADMIN", "USER", "NICU_ADMIN", "HOSPITAL", "AMBULANCE", "MODERATOR"]);
const CRUD_ACTIONS = ["CREATE", "READ", "UPDATE", "DELETE"];

function isSystemRoleName(name) {
  return SYSTEM_ROLE_NAMES.has((name || "").toUpperCase());
}

function authHeaders() {
  return state.accessToken ? { Authorization: `Bearer ${state.accessToken}` } : {};
}

async function api(path, options = {}) {
  const response = await fetch(path, {
    headers: {
      "Content-Type": "application/json",
      ...authHeaders(),
      ...(options.headers || {}),
    },
    ...options,
  });
  const text = await response.text();
  let payload = null;
  try {
    payload = text ? JSON.parse(text) : null;
  } catch (_err) {
    payload = text;
  }
  if (!response.ok) {
    const message = typeof payload === "string" ? payload : JSON.stringify(payload);
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }
  return payload;
}

function parseErrorMessage(error) {
  if (!error?.message) return "Unknown error";
  try {
    const parsed = JSON.parse(error.message);
    return parsed?.message || parsed?.error || error.message;
  } catch (_ignored) {
    return error.message;
  }
}

function renderAuthOutput(message, isError = false) {
  ui.authOutput.textContent = message;
  ui.authOutput.style.color = isError ? "#ff7f7f" : "#c4f1be";
}

function parsePage(data) {
  if (!data) return [];
  const payload = Object.prototype.hasOwnProperty.call(data, "data") ? data.data : data;
  if (Array.isArray(payload)) return payload;
  if (Array.isArray(payload.content)) return payload.content;
  return [];
}

function unwrapData(payload) {
  if (!payload || typeof payload !== "object") return payload;
  return Object.prototype.hasOwnProperty.call(payload, "data") ? payload.data : payload;
}

function responseMessage(payload, fallback = "Done.") {
  if (typeof payload === "string" && payload.trim()) return payload;
  const unwrapped = unwrapData(payload);
  if (typeof unwrapped === "string" && unwrapped.trim()) return unwrapped;
  if (unwrapped && typeof unwrapped === "object") {
    if (typeof unwrapped.message === "string" && unwrapped.message.trim()) return unwrapped.message;
    if (unwrapped.message && typeof unwrapped.message.text === "string" && unwrapped.message.text.trim()) {
      return unwrapped.message.text;
    }
    if (typeof unwrapped.details === "string" && unwrapped.details.trim()) return unwrapped.details;
  }
  if (payload && typeof payload === "object" && typeof payload.message === "string" && payload.message.trim()) {
    return payload.message;
  }
  if (payload && typeof payload === "object" && payload.message && typeof payload.message.text === "string" && payload.message.text.trim()) {
    return payload.message.text;
  }
  return fallback;
}

function requestData(data) {
  return JSON.stringify({ data });
}

function renderBadges(values) {
  return (values || []).map((v) => `<span class="badge">${v}</span>`).join("");
}

function splitIds(value) {
  if (!value) return [];
  return value.split(",").map((p) => p.trim()).filter(Boolean).map(Number);
}

function buildUserUpdateDto(user, overrides = {}) {
  return {
    id: user.id,
    name: user.name ?? null,
    email: user.email ?? null,
    phone: user.phone ?? null,
    gender: user.gender ?? null,
    relation: user.relation ?? null,
    username: user.username ?? "",
    role: Array.isArray(user.role) ? user.role : [],
    twoFactorEnabled: !!user.twoFactorEnabled,
    totpSecret: user.totpSecret ?? null,
    smsMfaEnabled: !!user.smsMfaEnabled,
    emailMfaEnabled: !!user.emailMfaEnabled,
    preferredMfaFactor: user.preferredMfaFactor ?? null,
    emailVerified: !!user.emailVerified,
    phoneVerified: !!user.phoneVerified,
    accountEnabled: user.accountEnabled !== false,
    ...overrides,
  };
}

// ─── Card visibility ───────────────────────────────────────────────────────────
const ALL_CARDS = () => [
  ui.matrixCard, ui.permMatrixCard, ui.permGroupedCard, ui.usersCard, ui.rolesCard,
  ui.privilegesCard, ui.urlsCard, ui.driftCard, ui.policyCard,
  ui.sodCard, ui.hierarchyCard, ui.roleDepsCard, ui.patientsCard, ui.toolbar,
];

function showCards() {
  ALL_CARDS().forEach((c) => c.classList.remove("hidden"));
}

function hideCards() {
  ALL_CARDS().forEach((c) => c.classList.add("hidden"));
}

function hasAdminConsoleAccess() {
  return ["user:read", "role:read", "privilege:read", "url:read", "policy:read"].some((p) => permissions.can(p));
}

function hasHardAdminAccess() {
  return permissions.can("matrix:manage") || permissions.can("module:manage") || permissions.can("policy:manage");
}

function isSuperAdmin() {
  return Array.isArray(state.me?.roles) && state.me.roles.includes("SUPER_ADMIN");
}

async function resetMatrixToDefaultState() {
  return api(apiV1("/security/permission-matrix/reset"), { method: "POST" });
}

// ─── Drawer helper ─────────────────────────────────────────────────────────────
function openDrawer(title, bodyHtml, saveCallback) {
  document.getElementById("drawer-title").textContent = title;
  document.getElementById("drawer-body").innerHTML = bodyHtml;
  document.getElementById("drawer-save-btn").onclick = saveCallback;
  document.getElementById("drawer").classList.remove("hidden");
}

function closeDrawer() {
  document.getElementById("drawer").classList.add("hidden");
}

// ─── Login ─────────────────────────────────────────────────────────────────────
async function login(event) {
  event.preventDefault();
  try {
    const body = {
      username: document.getElementById("username").value,
      password: document.getElementById("password").value,
      otp: document.getElementById("otp").value || null,
      otpChannel: document.getElementById("otp-channel").value || null,
      challengeId: state.pendingChallengeId || null,
    };
    const response = await api(apiV1("/authenticate"), { method: "POST", body: JSON.stringify(body) });
    if (response.requiresMfa) {
      state.pendingChallengeId = response.mfaChallengeId || "";
      state.pendingChallengeChannel = response.mfaChannel || "";
      document.getElementById("mfa-challenge-hint").textContent =
        `MFA challenge required via ${response.mfaChannel || "selected channel"}. Enter OTP and submit again.`;
      renderAuthOutput(response.mfaMessage || "MFA challenge issued.");
      return;
    }
    state.accessToken = response.accessToken;
    state.refreshToken = response.refreshToken;
    state.pendingChallengeId = "";
    state.pendingChallengeChannel = "";
    document.getElementById("mfa-challenge-hint").textContent = "";
    localStorage.setItem("admin_access_token", state.accessToken);
    localStorage.setItem("admin_refresh_token", state.refreshToken || "");
    renderAuthOutput("Login successful.");
    await refreshAll();
  } catch (error) {
    renderAuthOutput(`Login failed: ${parseErrorMessage(error)}`, true);
  }
}

// ─── My permissions summary (compact view) ────────────────────────────────────
async function loadMyPermissions() {
  state.me = await api(apiV1("/me/permissions"));
  document.getElementById("roles").innerHTML = `<strong>Roles:</strong> ${renderBadges(state.me.roles)}`;
  ui.identity.textContent = `Signed in as ${state.me.username} (${(state.me.roles || []).join(", ") || "no-role"})`;
  const matrixBody = document.querySelector("#matrix-table tbody");
  matrixBody.innerHTML = "";
  Object.entries(state.me.matrix || {}).forEach(([resource, actions]) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td>${resource}</td><td>${renderBadges(Object.keys(actions))}</td>`;
    matrixBody.appendChild(tr);
  });
}

// ─── Policy Drift ──────────────────────────────────────────────────────────────
async function loadPolicyDrift() {
  if (!permissions.can("matrix:manage")) {
    ui.driftCard.classList.add("hidden");
    return;
  }
  ui.driftCard.classList.remove("hidden");
  let report = null;
  try {
    report = await api(apiV1("/security/policy/drift"));
  } catch (error) {
    if (error.status === 403) { ui.driftCard.classList.add("hidden"); return; }
    throw error;
  }
  document.getElementById("drift-summary").innerHTML =
    `<strong>Generated:</strong> ${report.generatedAt || "n/a"} | <strong>Issues:</strong> ${report.issueCount || 0}`;
  const tbody = document.querySelector("#drift-table tbody");
  tbody.innerHTML = "";
  (report.issues || []).forEach((issue) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td><span class="badge ${issue.severity === "ERROR" ? "badge-danger" : "badge-warn"}">${issue.severity || "WARN"}</span></td>
      <td>${issue.type || ""}</td>
      <td>${issue.method || ""}</td>
      <td>${issue.endpoint || ""}</td>
      <td>${issue.authority || ""}</td>
      <td>${issue.details || ""}</td>`;
    tbody.appendChild(tr);
  });
}

function renderForm(containerId, html) {
  document.getElementById(containerId).innerHTML = html;
}

// ─── Lookup helpers ────────────────────────────────────────────────────────────
async function loadRoleValues() {
  if (!permissions.can("role:read")) { state.rolesMap = {}; return; }
  state.rolesMap = await api(apiV1("/user/roleValues"));
}

async function loadPrivilegeValues() {
  if (!permissions.can("privilege:read")) { state.privilegeMap = {}; state.allPrivileges = []; return; }
  state.privilegeMap = await api(apiV1("/url/privilegeValues"));
  const list = await api(apiV1("/privileges"));
  state.allPrivileges = parsePage(list);
}

// ─── Users ─────────────────────────────────────────────────────────────────────
async function loadUsers() {
  if (!permissions.can("user:read")) { ui.usersCard.classList.add("hidden"); return; }
  ui.usersCard.classList.remove("hidden");

  renderForm("create-user-form", permissions.can("user:create") ? `
    <label>Name <input id="new-user-name"></label>
    <label>Email <input id="new-user-email"></label>
    <label>Phone <input id="new-user-phone"></label>
    <label>Username <input id="new-user-username"></label>
    <label>Password <input id="new-user-password" type="password"></label>
    <label>Email Verified <input id="new-user-email-verified" type="checkbox"></label>
    <label>Phone Verified <input id="new-user-phone-verified" type="checkbox"></label>
    <label>SMS MFA Enabled <input id="new-user-sms-mfa" type="checkbox"></label>
    <label>Email MFA Enabled <input id="new-user-email-mfa" type="checkbox"></label>
    <label>Preferred MFA <input id="new-user-preferred-mfa" placeholder="TOTP/SMS/EMAIL"></label>
    <label>Role IDs (comma separated) <input id="new-user-role-ids" placeholder="10001,10002"></label>
    <button type="button" id="create-user-btn">Create User</button>
  ` : "<p class='hint'>Missing user:create permission.</p>");

  const users = parsePage(await api(apiV1("/user")));
  const tbody = document.querySelector("#users-table tbody");
  tbody.innerHTML = "";

  users.forEach((user) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${user.id}</td>
      <td>${user.username || ""}</td>
      <td>${user.name || ""}</td>
      <td>${user.email || ""}</td>
      <td>${user.phone || ""}</td>
      <td>${renderBadges([user.twoFactorEnabled ? "TOTP" : "", user.smsMfaEnabled ? "SMS" : "", user.emailMfaEnabled ? "EMAIL" : ""].filter(Boolean))}</td>
      <td>${renderBadges((user.role || []).map((id) => state.rolesMap[id] || id))}</td>
      <td class="actions"></td>`;
    const actions = tr.querySelector(".actions");

    if (permissions.can("user:update")) {
      const btn = document.createElement("button");
      btn.className = "secondary";
      btn.textContent = "Edit";
      btn.onclick = () => openUserEditDrawer(user);
      actions.appendChild(btn);
    }

    if (permissions.can("user:delete")) {
      const btn = document.createElement("button");
      btn.className = "danger";
      btn.textContent = "Delete";
      btn.onclick = async () => {
        if (confirm(`Delete user ${user.username}?`)) {
          await api(apiV1(`/user/${user.id}`), { method: "DELETE" });
          await loadUsers();
        }
      };
      actions.appendChild(btn);
    }

    tbody.appendChild(tr);
  });

  const createBtn = document.getElementById("create-user-btn");
  if (createBtn) {
    createBtn.onclick = async () => {
      await api(apiV1("/user"), {
        method: "POST",
        body: requestData({
          name: document.getElementById("new-user-name").value,
          email: document.getElementById("new-user-email").value,
          phone: document.getElementById("new-user-phone").value,
          username: document.getElementById("new-user-username").value,
          password: document.getElementById("new-user-password").value,
          emailVerified: document.getElementById("new-user-email-verified").checked,
          phoneVerified: document.getElementById("new-user-phone-verified").checked,
          smsMfaEnabled: document.getElementById("new-user-sms-mfa").checked,
          emailMfaEnabled: document.getElementById("new-user-email-mfa").checked,
          preferredMfaFactor: document.getElementById("new-user-preferred-mfa").value,
          role: splitIds(document.getElementById("new-user-role-ids").value),
        }),
      });
      await loadUsers();
    };
  }
}

function openUserEditDrawer(user) {
  const roleOptions = Object.entries(state.rolesMap)
    .map(([id, name]) => `<label class="priv-chip">
      <input type="checkbox" data-role-id="${id}" ${(user.role || []).includes(Number(id)) ? "checked" : ""}> ${name}
    </label>`).join("");

  const body = `
    <label>Name <input id="de-user-name" value="${escHtml(user.name || "")}"></label>
    <label>Email <input id="de-user-email" value="${escHtml(user.email || "")}"></label>
    <label>Phone <input id="de-user-phone" value="${escHtml(user.phone || "")}"></label>
    <label>Username <input id="de-user-username" value="${escHtml(user.username || "")}"></label>
    <label>New Password (optional) <input id="de-user-new-password" type="password"></label>
    <label>Current Password (required only when changing password) <input id="de-user-current-password" type="password"></label>
    <label>Preferred MFA <input id="de-user-preferred-mfa" value="${escHtml(user.preferredMfaFactor || "")}"></label>
    <div style="display:flex;gap:20px;margin-bottom:12px">
      <label class="priv-chip"><input type="checkbox" id="de-user-totp" ${user.twoFactorEnabled ? "checked" : ""}> TOTP MFA</label>
      <label class="priv-chip"><input type="checkbox" id="de-user-sms" ${user.smsMfaEnabled ? "checked" : ""}> SMS MFA</label>
      <label class="priv-chip"><input type="checkbox" id="de-user-email-mfa" ${user.emailMfaEnabled ? "checked" : ""}> Email MFA</label>
    </div>
    <label>Roles</label>
    <div class="priv-grid">${roleOptions}</div>
  `;

  openDrawer(`Edit User: ${user.username}`, body, async () => {
    const checkedRoles = [...document.querySelectorAll("[data-role-id]:checked")].map((el) => Number(el.dataset.roleId));
    const response = await api(apiV1("/user"), {
      method: "PATCH",
      body: requestData(buildUserUpdateDto(user, {
        name: document.getElementById("de-user-name").value,
        email: document.getElementById("de-user-email").value,
        phone: document.getElementById("de-user-phone").value,
        username: document.getElementById("de-user-username").value,
        twoFactorEnabled: document.getElementById("de-user-totp").checked,
        smsMfaEnabled: document.getElementById("de-user-sms").checked,
        emailMfaEnabled: document.getElementById("de-user-email-mfa").checked,
        preferredMfaFactor: document.getElementById("de-user-preferred-mfa").value,
        role: checkedRoles,
        newPassword: document.getElementById("de-user-new-password").value || null,
        currentPassword: document.getElementById("de-user-current-password").value || null,
      })),
    });
    closeDrawer();
    await loadUsers();
  });
}

// ─── Roles ─────────────────────────────────────────────────────────────────────
async function loadRoles() {
  if (!permissions.can("role:read")) { ui.rolesCard.classList.add("hidden"); return; }
  ui.rolesCard.classList.remove("hidden");

  renderForm("create-role-form", permissions.can("role:create") ? `
    <label>Name <input id="new-role-name"></label>
    <label>Description <input id="new-role-description"></label>
    <button type="button" id="create-role-btn">Create Role</button>
  ` : "<p class='hint'>Missing role:create permission.</p>");

  const roles = parsePage(await api(apiV1("/roles")));
  const tbody = document.querySelector("#roles-table tbody");
  tbody.innerHTML = "";

  roles.forEach((role) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${role.id}</td>
      <td>${role.name}</td>
      <td>${role.description}</td>
      <td>${renderBadges((role.privilege || []).map((id) => state.privilegeMap[id] || id))}</td>
      <td class="actions"></td>`;
    const actions = tr.querySelector(".actions");

    if (permissions.can("role:update")) {
      const btn = document.createElement("button");
      btn.className = "secondary";
      btn.textContent = "Edit";
      const isSystemRole = isSystemRoleName(role.name);
      if (isSystemRole) {
        btn.disabled = true;
        btn.title = "System roles cannot be edited";
      }
      btn.onclick = () => openRoleEditDrawer(role);
      actions.appendChild(btn);
    }

    if (permissions.can("role:delete")) {
      const btn = document.createElement("button");
      btn.className = "danger";
      btn.textContent = "Delete";
      const isSystemRole = isSystemRoleName(role.name);
      if (isSystemRole) {
        btn.disabled = true;
        btn.title = "System roles cannot be deleted";
      }
      btn.onclick = async () => {
        if (confirm(`Delete role ${role.name}?`)) {
          try {
            await api(apiV1(`/roles/${role.id}`), { method: "DELETE" });
            await loadRoles();
            renderAuthOutput(`Role ${role.name} deleted successfully.`);
          } catch (error) {
            const message = parseErrorMessage(error);
            renderAuthOutput(`Delete failed: ${message}`, true);
          }
        }
      };
      actions.appendChild(btn);
    }

    tbody.appendChild(tr);
  });

  const createBtn = document.getElementById("create-role-btn");
  if (createBtn) {
    createBtn.onclick = async () => {
      try {
        await api(apiV1("/roles"), {
          method: "POST",
          body: requestData({
            name: document.getElementById("new-role-name").value,
            description: document.getElementById("new-role-description").value,
            privilege: [],
          }),
        });
        document.getElementById("new-role-name").value = "";
        document.getElementById("new-role-description").value = "";
        await loadRoles();
        renderAuthOutput("Role created successfully.");
      } catch (error) {
        const message = parseErrorMessage(error);
        renderAuthOutput(`Create failed: ${message}`, true);
      }
    };
  }
}

function openRoleEditDrawer(role) {
  const searchId = `priv-search-${role.id}`;
  const gridId = `priv-grid-${role.id}`;
  const assignedIds = new Set(role.privilege || []);
  const isSystemRole = isSystemRoleName(role.name);

  const privItems = state.allPrivileges.map((p) =>
    `<label class="priv-chip">
      <input type="checkbox" data-priv-id="${p.id}" ${assignedIds.has(p.id) ? "checked" : ""} ${isSystemRole ? "disabled" : ""}> ${escHtml(p.name)}
    </label>`
  ).join("");

  const body = `
    <label>Name <input id="de-role-name" value="${escHtml(role.name)}" ${isSystemRole ? 'readonly' : ''}></label>
    <label>Description <input id="de-role-description" value="${escHtml(role.description || "")}" ${isSystemRole ? 'readonly' : ''}></label>
    <label>Privileges</label>
    <input class="priv-search" placeholder="Filter privileges…" oninput="filterPrivGrid(this, '${gridId}')" ${isSystemRole ? 'disabled' : ''}>
    <div class="priv-grid" id="${gridId}">${privItems}</div>
    ${isSystemRole ? '<p style="color: #ff7f7f; font-size: 0.9em; margin-top: 10px;"><strong>System Role:</strong> This role is read-only and cannot be modified.</p>' : ''}
  `;

  openDrawer(`Edit Role: ${role.name}`, body, async () => {
    if (isSystemRole) {
      renderAuthOutput("Cannot edit system roles.", true);
      return;
    }
    const checkedPrivs = [...document.querySelectorAll("[data-priv-id]:checked")].map((el) => Number(el.dataset.privId));
    try {
      await api(apiV1(`/roles/${role.id}`), {
        method: "PUT",
        body: requestData({
          id: role.id,
          name: document.getElementById("de-role-name").value,
          description: document.getElementById("de-role-description").value,
          privilege: checkedPrivs,
        }),
      });
      closeDrawer();
      await loadRoles();
      renderAuthOutput(`Role ${role.name} updated successfully.`);
    } catch (error) {
      const message = parseErrorMessage(error);
      renderAuthOutput(`Update failed: ${message}`, true);
    }
  });
}

function filterPrivGrid(input, gridId) {
  const q = input.value.toLowerCase();
  document.querySelectorAll(`#${gridId} .priv-chip`).forEach((chip) => {
    chip.style.display = chip.textContent.toLowerCase().includes(q) ? "" : "none";
  });
}

// ─── Privileges ────────────────────────────────────────────────────────────────
async function loadPrivileges() {
  if (!permissions.can("privilege:read")) { ui.privilegesCard.classList.add("hidden"); return; }
  ui.privilegesCard.classList.remove("hidden");

  renderForm("create-privilege-form", permissions.can("privilege:create") ? `
    <label>Privilege Code <input id="new-priv-name" placeholder="user:export"></label>
    <button type="button" id="create-priv-btn">Create Privilege</button>
  ` : "<p class='hint'>Missing privilege:create permission.</p>");

  const privileges = state.allPrivileges.length ? state.allPrivileges : parsePage(await api(apiV1("/privileges")));
  const tbody = document.querySelector("#privileges-table tbody");
  tbody.innerHTML = "";

  privileges.forEach((privilege) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `<td>${privilege.id}</td><td>${privilege.name}</td><td class="actions"></td>`;
    const actions = tr.querySelector(".actions");

    if (permissions.can("privilege:update")) {
      const btn = document.createElement("button");
      btn.className = "secondary";
      btn.textContent = "Edit";
      btn.onclick = () => {
        openDrawer(`Edit Privilege`, `<label>Privilege Code <input id="de-priv-name" value="${escHtml(privilege.name)}"></label>`, async () => {
          await api(apiV1("/privileges"), {
            method: "PATCH",
            body: requestData({
              id: privilege.id,
              name: document.getElementById("de-priv-name").value,
            }),
          });
          closeDrawer();
          await loadPrivileges();
        });
      };
      actions.appendChild(btn);
    }

    if (permissions.can("privilege:delete")) {
      const btn = document.createElement("button");
      btn.className = "danger";
      btn.textContent = "Delete";
      btn.onclick = async () => {
        if (confirm(`Delete privilege ${privilege.name}?`)) {
          await api(apiV1(`/privileges/${privilege.id}`), { method: "DELETE" });
          await loadPrivileges();
        }
      };
      actions.appendChild(btn);
    }

    tbody.appendChild(tr);
  });

  const createBtn = document.getElementById("create-priv-btn");
  if (createBtn) {
    createBtn.onclick = async () => {
      await api(apiV1("/privileges"), { method: "POST", body: requestData({ name: document.getElementById("new-priv-name").value }) });
      await loadPrivileges();
    };
  }
}

// ─── URLs ──────────────────────────────────────────────────────────────────────
async function loadUrls() {
  if (!permissions.can("url:read")) { ui.urlsCard.classList.add("hidden"); return; }
  ui.urlsCard.classList.remove("hidden");

  renderForm("create-url-form", permissions.can("url:create") ? `
    <label>Endpoint <input id="new-url-endpoint" placeholder="/api/v1/custom/report"></label>
    <label>Method <input id="new-url-method" placeholder="GET"></label>
    <label>Privilege IDs (comma separated) <input id="new-url-privileges" placeholder="10010,10011"></label>
    <button type="button" id="create-url-btn">Create URL</button>
  ` : "<p class='hint'>Missing url:create permission.</p>");

  const urls = parsePage(await api(apiV1("/url")));
  const tbody = document.querySelector("#urls-table tbody");
  tbody.innerHTML = "";

  urls.forEach((url) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${url.id}</td>
      <td>${url.endpoint}</td>
      <td>${url.method}</td>
      <td>${renderBadges((url.privileges || []).map((id) => state.privilegeMap[id] || id))}</td>
      <td class="actions"></td>`;
    const actions = tr.querySelector(".actions");

    if (permissions.can("url:update")) {
      const btn = document.createElement("button");
      btn.className = "secondary";
      btn.textContent = "Edit";
      btn.onclick = () => {
        const privItems = state.allPrivileges.map((p) =>
          `<label class="priv-chip"><input type="checkbox" data-url-priv-id="${p.id}" ${(url.privileges || []).includes(p.id) ? "checked" : ""}> ${escHtml(p.name)}</label>`
        ).join("");
        openDrawer(`Edit URL: ${url.method} ${url.endpoint}`, `
          <label>Endpoint <input id="de-url-endpoint" value="${escHtml(url.endpoint)}"></label>
          <label>Method <input id="de-url-method" value="${escHtml(url.method)}"></label>
          <label>Privileges</label>
          <input class="priv-search" placeholder="Filter…" oninput="filterPrivGrid(this, 'de-url-priv-grid')">
          <div class="priv-grid" id="de-url-priv-grid">${privItems}</div>
        `, async () => {
          const checked = [...document.querySelectorAll("[data-url-priv-id]:checked")].map((el) => Number(el.dataset.urlPrivId));
          await api(apiV1("/url"), {
            method: "PUT",
            body: requestData({
              id: url.id,
              endpoint: document.getElementById("de-url-endpoint").value,
              method: document.getElementById("de-url-method").value,
              privileges: checked,
            }),
          });
          closeDrawer();
          await loadUrls();
        });
      };
      actions.appendChild(btn);
    }

    if (permissions.can("url:delete")) {
      const btn = document.createElement("button");
      btn.className = "danger";
      btn.textContent = "Delete";
      btn.onclick = async () => {
        if (confirm(`Delete URL ${url.endpoint} ${url.method}?`)) {
          await api(apiV1(`/url/${url.id}`), { method: "DELETE" });
          await loadUrls();
        }
      };
      actions.appendChild(btn);
    }

    tbody.appendChild(tr);
  });

  const createBtn = document.getElementById("create-url-btn");
  if (createBtn) {
    createBtn.onclick = async () => {
      await api(apiV1("/url"), {
        method: "POST",
        body: requestData({
          endpoint: document.getElementById("new-url-endpoint").value,
          method: document.getElementById("new-url-method").value,
          privileges: splitIds(document.getElementById("new-url-privileges").value),
        }),
      });
      await loadUrls();
    };
  }
}

// ─── Alpine.js — Permission Matrix ────────────────────────────────────────────
function permMatrixApp() {
  return {
    loading: false,
    roles: [],
    privileges: [],
    rolePrivileges: {},   // roleId -> Set<privilegeId>
    original: {},         // roleId -> Set<privilegeId> (for change tracking)
    pendingChanges: 0,
    canEdit: false,
    canResetAll: false,
    saving: false,
    resetting: false,

    async tryInit() {
      if (!state.accessToken) return;
      this.canEdit = permissions.can("matrix:manage");
      this.canResetAll = isSuperAdmin();
      if (!permissions.can("matrix:read")) return;
      await this.loadMatrix();
    },

    async clear() {
      await this.tryInit();
    },

    async loadMatrix() {
      this.loading = true;
      try {
        const data = unwrapData(await api(apiV1("/security/permission-matrix"))) || {};
        this.roles = Array.isArray(data.roles) ? data.roles : [];
        this.privileges = Array.isArray(data.privileges) ? data.privileges : [];
        this.rolePrivileges = {};
        this.original = {};
        for (const role of this.roles) {
          const ids = Array.isArray(role.privilegeIds) ? role.privilegeIds : [];
          this.rolePrivileges[role.id] = new Set(ids);
          this.original[role.id] = new Set(ids);
        }
        this.pendingChanges = 0;
      } finally {
        this.loading = false;
      }
    },

    hasPrivilege(roleId, privId) {
      return this.rolePrivileges[roleId]?.has(privId) || false;
    },

    togglePrivilege(roleId, privId, event) {
      if (event.target.checked) {
        this.rolePrivileges[roleId].add(privId);
      } else {
        this.rolePrivileges[roleId].delete(privId);
      }
      this.pendingChanges = this.countChanges();
    },

    countChanges() {
      let count = 0;
      for (const role of this.roles) {
        const cur = this.rolePrivileges[role.id];
        const orig = this.original[role.id];
        for (const id of cur) { if (!orig.has(id)) count++; }
        for (const id of orig) { if (!cur.has(id)) count++; }
      }
      return count;
    },

    async saveMatrix() {
      this.saving = true;
      try {
        const updates = this.roles.map((r) => ({
          roleId: r.id,
          privilegeIds: [...this.rolePrivileges[r.id]],
        }));
        const result = await api(apiV1("/security/permission-matrix"), { method: "PATCH", body: JSON.stringify(updates) });
        renderAuthOutput(responseMessage(result, "Permission matrix saved."));
        await this.loadMatrix();
      } catch (err) {
        renderAuthOutput(`Matrix save failed: ${parseErrorMessage(err)}`, true);
      } finally {
        this.saving = false;
      }
    },

    async resetToDefaults() {
      if (!this.canResetAll) return;
      if (!confirm("Reset the permission matrix to default startup state? This will overwrite current role-permission assignments.")) {
        return;
      }
      this.resetting = true;
      try {
        const result = await resetMatrixToDefaultState();
        renderAuthOutput(responseMessage(result, "Permission matrix reset to default state."));
        await refreshAll();
      } catch (err) {
        renderAuthOutput(`Matrix reset failed: ${parseErrorMessage(err)}`, true);
      } finally {
        this.resetting = false;
      }
    },

    async tryInitAndRefresh() {
      await this.tryInit();
    },
  };
}

// ─── Alpine.js — Grouped Permission Matrix (domain x CRUD) ───────────────────
function groupedPermMatrixApp() {
  return {
    loading: false,
    saving: false,
    resetting: false,
    canEdit: false,
    canResetAll: false,
    roles: [],
    domains: [],
    grouped: {},           // domain -> action -> [{id, name}]
    rolePrivileges: {},    // roleId -> Set<privilegeId>
    original: {},          // roleId -> Set<privilegeId>
    pendingChanges: 0,

    async tryInit() {
      if (!state.accessToken) return;
      this.canEdit = permissions.can("matrix:manage");
      this.canResetAll = isSuperAdmin();
      if (!permissions.can("matrix:read")) return;
      await this.load();
    },

    async clear() {
      await this.load();
    },

    async load() {
      this.loading = true;
      try {
        const [matrixRaw, groupedRaw] = await Promise.all([
          api(apiV1("/security/permission-matrix")),
          api(apiV1("/security/permission-matrix/grouped")),
        ]);
        const matrix = unwrapData(matrixRaw) || {};
        const groupedData = unwrapData(groupedRaw) || {};

        this.roles = Array.isArray(matrix.roles) ? matrix.roles : [];
        this.grouped = groupedData.privilegesByDomainAndCrud || {};
        this.domains = Object.keys(this.grouped).sort((a, b) => a.localeCompare(b));

        this.rolePrivileges = {};
        this.original = {};
        for (const role of this.roles) {
          const ids = Array.isArray(role.privilegeIds) ? role.privilegeIds : [];
          this.rolePrivileges[role.id] = new Set(ids);
          this.original[role.id] = new Set(ids);
        }
        this.pendingChanges = 0;
      } finally {
        this.loading = false;
      }
    },

    actionPrivilegeIds(domain, action) {
      const list = this.grouped?.[domain]?.[action];
      if (!Array.isArray(list)) return [];
      return list.map((p) => p.id).filter((id) => typeof id === "number");
    },

    hasAnyActionPrivileges(domain, action) {
      return this.actionPrivilegeIds(domain, action).length > 0;
    },

    hasDomainAction(roleId, domain, action) {
      const ids = this.actionPrivilegeIds(domain, action);
      if (!ids.length) return false;
      const assigned = this.rolePrivileges[roleId] || new Set();
      return ids.every((id) => assigned.has(id));
    },

    isDomainActionPartial(roleId, domain, action) {
      const ids = this.actionPrivilegeIds(domain, action);
      if (!ids.length) return false;
      const assigned = this.rolePrivileges[roleId] || new Set();
      const selectedCount = ids.filter((id) => assigned.has(id)).length;
      return selectedCount > 0 && selectedCount < ids.length;
    },

    toggleDomainAction(roleId, domain, action, event) {
      const ids = this.actionPrivilegeIds(domain, action);
      if (!ids.length) return;
      if (!this.rolePrivileges[roleId]) this.rolePrivileges[roleId] = new Set();
      if (event.target.checked) {
        ids.forEach((id) => this.rolePrivileges[roleId].add(id));
      } else {
        ids.forEach((id) => this.rolePrivileges[roleId].delete(id));
      }
      this.pendingChanges = this.countChanges();
    },

    domainSelection(roleId, domain, source) {
      return {
        create: this.hasDomainActionFrom(roleId, domain, "CREATE", source),
        read: this.hasDomainActionFrom(roleId, domain, "READ", source),
        update: this.hasDomainActionFrom(roleId, domain, "UPDATE", source),
        delete: this.hasDomainActionFrom(roleId, domain, "DELETE", source),
      };
    },

    hasDomainActionFrom(roleId, domain, action, source) {
      const ids = this.actionPrivilegeIds(domain, action);
      if (!ids.length) return false;
      const assigned = source?.[roleId] || new Set();
      return ids.every((id) => assigned.has(id));
    },

    countChanges() {
      let changed = 0;
      for (const role of this.roles) {
        for (const domain of this.domains) {
          for (const action of CRUD_ACTIONS) {
            const current = this.hasDomainActionFrom(role.id, domain, action, this.rolePrivileges);
            const original = this.hasDomainActionFrom(role.id, domain, action, this.original);
            if (current !== original) changed += 1;
          }
        }
      }
      return changed;
    },

    buildUpdates() {
      const updates = [];
      for (const role of this.roles) {
        const domains = {};
        for (const domain of this.domains) {
          const current = this.domainSelection(role.id, domain, this.rolePrivileges);
          const original = this.domainSelection(role.id, domain, this.original);
          if (
            current.create !== original.create
            || current.read !== original.read
            || current.update !== original.update
            || current.delete !== original.delete
          ) {
            domains[domain] = current;
          }
        }
        if (Object.keys(domains).length) {
          updates.push({ roleId: role.id, domains });
        }
      }
      return updates;
    },

    async save() {
      const updates = this.buildUpdates();
      if (!updates.length) return;
      this.saving = true;
      try {
        const result = await api(apiV1("/security/permission-matrix/grouped"), {
          method: "PATCH",
          body: JSON.stringify(updates),
        });
        renderAuthOutput(responseMessage(result, "Grouped permission matrix saved."));
        await this.load();
      } catch (err) {
        renderAuthOutput(`Grouped matrix save failed: ${parseErrorMessage(err)}`, true);
      } finally {
        this.saving = false;
      }
    },

    async resetToDefaults() {
      if (!this.canResetAll) return;
      if (!confirm("Reset the permission matrix to default startup state? This will overwrite current role-permission assignments.")) {
        return;
      }
      this.resetting = true;
      try {
        const result = await resetMatrixToDefaultState();
        renderAuthOutput(responseMessage(result, "Permission matrix reset to default state."));
        await refreshAll();
      } catch (err) {
        renderAuthOutput(`Matrix reset failed: ${parseErrorMessage(err)}`, true);
      } finally {
        this.resetting = false;
      }
    },
  };
}

function parseChannelFlags(value, fallbackCsv) {
  const parts = (value || fallbackCsv || "SMS")
    .split(",")
    .map((s) => s.trim().toUpperCase())
    .filter(Boolean);
  return {
    SMS: parts.includes("SMS"),
    EMAIL: parts.includes("EMAIL"),
    PUSH: parts.includes("PUSH"),
  };
}

function channelsToCsv(flags) {
  return Object.entries(flags)
    .filter(([, enabled]) => enabled)
    .map(([key]) => key)
    .join(",");
}

// ─── Alpine.js — Policy ───────────────────────────────────────────────────────
function policyApp() {
  return {
    loading: false,
    editable: false,
    saving: false,
    verificationRequired: false,
    channels: { EMAIL: true, SMS: true },
    defaultChannel: "EMAIL",
    notificationDefaultChannels: { SMS: true, EMAIL: false, PUSH: false },
    bedReservationChannels: { SMS: true, EMAIL: false, PUSH: false },
    referralChannels: { SMS: true, EMAIL: false, PUSH: false },
    mfaOptional: true,
    mfaRoles: {
      SUPER_ADMIN: false, ADMIN: false, USER: false,
      NICU_ADMIN: false, HOSPITAL: false, AMBULANCE: false, MODERATOR: false,
    },
    mfaFactors: {
      USER: { TOTP: true, SMS: true, EMAIL: true },
      ADMIN: { TOTP: true, EMAIL: true, SMS: false },
      SUPER_ADMIN: { TOTP: true, EMAIL: true, SMS: false },
    },

    async tryInit() {
      if (!state.accessToken || !permissions.can("policy:read")) {
        ui.policyCard.classList.add("hidden");
        return;
      }
      ui.policyCard.classList.remove("hidden");
      this.editable = permissions.can("policy:manage");
      this.loading = true;
      try {
        const policy = await api(apiV1("/security/policy"));
        this.verificationRequired = (policy["registration.verification.required"] || "false") === "true";
        const ch = (policy["registration.allowed.channels"] || "EMAIL,SMS").split(",").map((s) => s.trim().toUpperCase());
        this.channels = { EMAIL: ch.includes("EMAIL"), SMS: ch.includes("SMS") };
        this.defaultChannel = (policy["registration.default.channel"] || "EMAIL").trim().toUpperCase();
        const defaultNotificationCsv = (policy["notification.default.channels"] || "SMS").trim();
        this.notificationDefaultChannels = parseChannelFlags(defaultNotificationCsv, "SMS");
        this.bedReservationChannels = parseChannelFlags(policy["notification.bed_reservation.channels"], defaultNotificationCsv);
        this.referralChannels = parseChannelFlags(policy["notification.referral.channels"], defaultNotificationCsv);
        this.mfaOptional = (policy["mfa.optional.per.user"] || "true") === "true";
        const enforcedRoles = (policy["mfa.enforced.roles"] || "").split(",").map((s) => s.trim().toUpperCase()).filter(Boolean);
        this.mfaRoles = {
          SUPER_ADMIN: enforcedRoles.includes("SUPER_ADMIN"),
          ADMIN: enforcedRoles.includes("ADMIN"),
          USER: enforcedRoles.includes("USER"),
          NICU_ADMIN: enforcedRoles.includes("NICU_ADMIN"),
          HOSPITAL: enforcedRoles.includes("HOSPITAL"),
          AMBULANCE: enforcedRoles.includes("AMBULANCE"),
          MODERATOR: enforcedRoles.includes("MODERATOR"),
        };
        const parseFactors = (val, defaults) => {
          const parts = (val || defaults).split(",").map((s) => s.trim().toUpperCase());
          return { TOTP: parts.includes("TOTP"), SMS: parts.includes("SMS"), EMAIL: parts.includes("EMAIL") };
        };
        this.mfaFactors = {
          USER: parseFactors(policy["mfa.allowed.factors.user"], "TOTP,SMS,EMAIL"),
          ADMIN: parseFactors(policy["mfa.allowed.factors.admin"], "TOTP,EMAIL"),
          SUPER_ADMIN: parseFactors(policy["mfa.allowed.factors.super_admin"], "TOTP,EMAIL"),
        };
      } finally {
        this.loading = false;
      }
    },

    registrationChannelsDisplay() {
      return Object.entries(this.channels).filter(([, v]) => v).map(([k]) => k).join(", ") || "none";
    },

    notificationChannelsDisplay(flags) {
      return Object.entries(flags || {}).filter(([, v]) => v).map(([k]) => k).join(", ") || "SMS";
    },

    toggleFactor(roleKey, factor, event) {
      this.mfaFactors[roleKey][factor] = event.target.checked;
    },

    buildFactorString(roleKey) {
      return Object.entries(this.mfaFactors[roleKey]).filter(([, v]) => v).map(([k]) => k).join(",");
    },

    async savePolicy() {
      this.saving = true;
      try {
        const allowedChannels = Object.entries(this.channels).filter(([, v]) => v).map(([k]) => k).join(",");
        const enforcedRoles = Object.entries(this.mfaRoles).filter(([, v]) => v).map(([k]) => k).join(",");
        const notificationDefaultChannels = channelsToCsv(this.notificationDefaultChannels) || "SMS";
        const bedReservationChannels = channelsToCsv(this.bedReservationChannels) || notificationDefaultChannels;
        const referralChannels = channelsToCsv(this.referralChannels) || notificationDefaultChannels;
        const payload = [
          { key: "registration.verification.required", value: String(this.verificationRequired) },
          { key: "registration.allowed.channels", value: allowedChannels },
          { key: "registration.default.channel", value: this.defaultChannel },
          { key: "notification.default.channels", value: notificationDefaultChannels },
          { key: "notification.bed_reservation.channels", value: bedReservationChannels },
          { key: "notification.referral.channels", value: referralChannels },
          { key: "mfa.optional.per.user", value: String(this.mfaOptional) },
          { key: "mfa.enforced.roles", value: enforcedRoles },
          { key: "mfa.allowed.factors.user", value: this.buildFactorString("USER") },
          { key: "mfa.allowed.factors.admin", value: this.buildFactorString("ADMIN") },
          { key: "mfa.allowed.factors.super_admin", value: this.buildFactorString("SUPER_ADMIN") },
        ];
        await api(apiV1("/security/policy"), { method: "PATCH", body: JSON.stringify(payload) });
        renderAuthOutput("Policy saved.");
      } catch (err) {
        renderAuthOutput(`Policy save failed: ${parseErrorMessage(err)}`, true);
      } finally {
        this.saving = false;
      }
    },
  };
}

// ─── Alpine.js — SoD Constraints ─────────────────────────────────────────────
function sodApp() {
  return {
    loading: false,
    saving: false,
    canManage: false,
    constraints: [],
    form: { roleNameA: "", roleNameB: "", reason: "", dynamic: false },

    async tryInit() {
      if (!state.accessToken || !permissions.can("sod:read")) {
        ui.sodCard.classList.add("hidden");
        return;
      }
      ui.sodCard.classList.remove("hidden");
      this.canManage = permissions.can("sod:manage");
      await this.load();
    },

    async load() {
      this.loading = true;
      try {
        this.constraints = await api(apiV1("/security/sod"));
      } finally {
        this.loading = false;
      }
    },

    async createConstraint() {
      this.saving = true;
      try {
        await api(apiV1("/security/sod"), {
          method: "POST",
          body: JSON.stringify({
            roleNameA: this.form.roleNameA,
            roleNameB: this.form.roleNameB,
            reason: this.form.reason,
            dynamic: this.form.dynamic === true || this.form.dynamic === "true",
          }),
        });
        this.form = { roleNameA: "", roleNameB: "", reason: "", dynamic: false };
        await this.load();
        renderAuthOutput("SoD constraint created.");
      } catch (err) {
        renderAuthOutput(`SoD create failed: ${parseErrorMessage(err)}`, true);
      } finally {
        this.saving = false;
      }
    },

    async deleteConstraint(id) {
      if (!confirm("Delete this SoD constraint?")) return;
      await api(apiV1(`/security/sod/${id}`), { method: "DELETE" });
      await this.load();
      renderAuthOutput("SoD constraint deleted.");
    },
  };
}

// ─── Alpine.js — Role Hierarchy ───────────────────────────────────────────────
function hierarchyApp() {
  return {
    loading: false,
    saving: false,
    canManage: false,
    allRoles: [],
    form: { roleId: "", parentId: "" },

    async tryInit() {
      if (!state.accessToken || !permissions.can("hierarchy:read")) {
        ui.hierarchyCard.classList.add("hidden");
        return;
      }
      ui.hierarchyCard.classList.remove("hidden");
      this.canManage = permissions.can("hierarchy:manage");
      await this.load();
    },

    async load() {
      this.loading = true;
      try {
        const [tree, roles] = await Promise.all([
          api(apiV1("/roles/hierarchy")),
          api(apiV1("/roles")),
        ]);
        const rolesPage = parsePage(roles);
        this.allRoles = rolesPage;
        renderHierarchyTree(tree, document.getElementById("hier-tree-container"));
      } finally {
        this.loading = false;
      }
    },

    async setParent() {
      if (!this.form.roleId) return;
      this.saving = true;
      try {
        const parentId = this.form.parentId ? Number(this.form.parentId) : null;
        await api(apiV1(`/roles/${this.form.roleId}/parent`), {
          method: "PUT",
          body: JSON.stringify({ parentId }),
        });
        this.form = { roleId: "", parentId: "" };
        await this.load();
        renderAuthOutput("Role hierarchy updated.");
      } catch (err) {
        renderAuthOutput(`Hierarchy update failed: ${parseErrorMessage(err)}`, true);
      } finally {
        this.saving = false;
      }
    },
  };
}

// ─── Alpine.js — Role Dependencies ────────────────────────────────────────────
function roleDepsApp() {
  return {
    loading: false,
    saving: false,
    canManage: false,
    deps: [],
    allRoles: [],
    form: { dependentRoleId: "", requiredRoleId: "", reason: "" },

    async tryInit() {
      if (!state.accessToken || !permissions.can("role:dependency:read")) {
        ui.roleDepsCard.classList.add("hidden");
        return;
      }
      ui.roleDepsCard.classList.remove("hidden");
      this.canManage = permissions.can("role:dependency:manage");
      await this.load();
    },

    async load() {
      this.loading = true;
      try {
        const [depsData, rolesData] = await Promise.all([
          api(apiV1("/role-dependencies")),
          api(apiV1("/roles")),
        ]);
        this.deps = Array.isArray(depsData) ? depsData : [];
        this.allRoles = parsePage(rolesData);
      } finally {
        this.loading = false;
      }
    },

    async create() {
      if (!this.form.dependentRoleId || !this.form.requiredRoleId) return;
      this.saving = true;
      try {
        await api(apiV1("/role-dependencies"), {
          method: "POST",
          body: JSON.stringify({
            dependentRoleId: Number(this.form.dependentRoleId),
            requiredRoleId: Number(this.form.requiredRoleId),
            reason: this.form.reason,
          }),
        });
        this.form = { dependentRoleId: "", requiredRoleId: "", reason: "" };
        await this.load();
        renderAuthOutput("Role dependency created.");
      } catch (err) {
        renderAuthOutput(`Role dependency create failed: ${parseErrorMessage(err)}`, true);
      } finally {
        this.saving = false;
      }
    },

    async remove(id) {
      if (!confirm("Delete this role dependency?")) return;
      try {
        await api(apiV1(`/role-dependencies/${id}`), { method: "DELETE" });
        await this.load();
        renderAuthOutput("Role dependency deleted.");
      } catch (err) {
        renderAuthOutput(`Role dependency delete failed: ${parseErrorMessage(err)}`, true);
      }
    },
  };
}

// ─── Patients (read-only view) ─────────────────────────────────────────────────
async function loadPatients() {
  if (!permissions.can("admission:read")) { ui.patientsCard.classList.add("hidden"); return; }
  ui.patientsCard.classList.remove("hidden");

  const patients = parsePage(await api(apiV1("/patients")));
  const tbody = document.querySelector("#patients-table tbody");
  tbody.innerHTML = "";

  patients.forEach((p) => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${p.id}</td>
      <td>${escHtml(p.mrn || "")}</td>
      <td>${escHtml(p.babyName || "")}</td>
      <td>${escHtml(p.gender || "")}</td>
      <td>${p.dateOfBirth ? new Date(p.dateOfBirth).toLocaleDateString() : ""}</td>
      <td>${escHtml(p.bloodType || "")}</td>`;
    tbody.appendChild(tr);
  });
}

function renderHierarchyTree(nodes, container) {
  if (!container) return;
  container.innerHTML = "";
  if (!nodes || !nodes.length) {
    container.innerHTML = "<p class='hint'>No hierarchy configured.</p>";
    return;
  }
  function buildNode(node, isRoot) {
    const div = document.createElement("div");
    div.className = isRoot ? "hier-root hier-tree" : "hier-node";
    const label = document.createElement("span");
    label.className = "hier-label";
    label.innerHTML = `<span class="badge">${escHtml(node.name || "")}</span>`;
    div.appendChild(label);
    if (node.children && node.children.length) {
      node.children.forEach((child) => div.appendChild(buildNode(child, false)));
    }
    return div;
  }
  nodes.forEach((root) => container.appendChild(buildNode(root, true)));
}

// ─── Utility ───────────────────────────────────────────────────────────────────
function escHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

// ─── Main refresh ──────────────────────────────────────────────────────────────
async function refreshAll() {
  if (!state.accessToken) { hideCards(); return; }
  try {
    await loadMyPermissions();
    if (!hasAdminConsoleAccess()) {
      ui.matrixCard.classList.remove("hidden");
      ui.toolbar.classList.remove("hidden");
      [ui.permMatrixCard, ui.driftCard, ui.usersCard, ui.rolesCard,
       ui.permGroupedCard, ui.privilegesCard, ui.urlsCard, ui.policyCard, ui.sodCard, ui.hierarchyCard]
        .forEach((c) => c.classList.add("hidden"));
      renderAuthOutput("Signed in with non-admin role. Matrix view only.");
      return;
    }

    const issues = [];
    const loadGuarded = async (title, fn) => {
      try { await fn(); } catch (error) {
        if (error.status === 403) { issues.push(`${title}: forbidden`); return; }
        throw error;
      }
    };

    await loadGuarded("Role values", loadRoleValues);
    await loadGuarded("Privilege values", loadPrivilegeValues);
    await loadGuarded("Users", loadUsers);
    await loadGuarded("Roles", loadRoles);
    await loadGuarded("Privileges", loadPrivileges);
    await loadGuarded("URL policies", loadUrls);
    await loadGuarded("Policy drift", loadPolicyDrift);
    await loadGuarded("Patients", loadPatients);

    // Trigger Alpine.js component initializations
    document.querySelectorAll("[x-data]").forEach((el) => {
      if (el._x_dataStack?.[0]?.tryInit) {
        el._x_dataStack[0].tryInit().catch(() => {});
      }
    });

    showCards();
    const modeLabel = hasHardAdminAccess() ? "policy-admin" : "admin";
    renderAuthOutput(issues.length
      ? `Signed in (${modeLabel}) — partial access. ${issues.join(" | ")}`
      : `Signed in (${modeLabel}).`);
  } catch (error) {
    if (error.status === 401) { logout(); renderAuthOutput("Session expired. Please sign in again.", true); return; }
    renderAuthOutput(`Session error: ${parseErrorMessage(error)}`, true);
    hideCards();
  }
}

function logout() {
  state.accessToken = "";
  state.refreshToken = "";
  state.pendingChallengeId = "";
  state.pendingChallengeChannel = "";
  state.me = null;
  state.allPrivileges = [];
  localStorage.removeItem("admin_access_token");
  localStorage.removeItem("admin_refresh_token");
  document.getElementById("mfa-challenge-hint").textContent = "";
  hideCards();
  renderAuthOutput("Signed out.");
}

// ─── Event listeners ──────────────────────────────────────────────────────────
document.getElementById("login-form").addEventListener("submit", login);
document.getElementById("refresh-btn").addEventListener("click", refreshAll);
document.getElementById("logout-btn").addEventListener("click", logout);

refreshAll();
