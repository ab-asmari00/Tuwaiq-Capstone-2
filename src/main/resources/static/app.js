import { api as request, list, escapeHtml as e, positiveId, safeHttps, saudiDateTime, saudiToday, saudiNow } from './api.js';
import { t, lang, setLanguage, formatDateTime as date, days, dayLabel } from './i18n.js';

const $ = selector => document.querySelector(selector);
const T = key => e(t(key));
const app = $('#app');
const dialog = $('#dialog');
const state = { user: null, page: 'today', authTab: 'login', items: [], showInactive: false, doctors: [], appointments: [], checking: false, reminders: [], reminderError: '', search: null };
let renderVersion = 0;
let identityVersion = 0;
let modalSubmit = null;
let modalVersion = 0;
let polling = false;
async function api(...args) {
  const identity = identityVersion;
  const response = await request(...args);
  if (identity !== identityVersion) throw new Error(t('Your session has changed. Please try again.'));
  return response;
}

try {
  const saved = JSON.parse(sessionStorage.getItem('jura-user'));
  if (positiveId(saved?.id) && ['PATIENT', 'DOCTOR'].includes(saved.role)) {
    state.user = { id: saved.id, name: String(saved.name || ''), email: String(saved.email || ''), role: saved.role };
    state.page = saved.role === 'DOCTOR' ? 'appointments' : 'today';
  }
} catch { sessionStorage.removeItem('jura-user'); }

const paths = {
  today: '<rect x="3" y="4" width="18" height="17" rx="3"/><path d="M8 2v4m8-4v4M3 10h18m-13 5h2m4 0h2"/>',
  pill: '<path d="m8 3 13 13a6 6 0 0 1-8 8L0 11a6 6 0 0 1 8-8Z" transform="translate(2 0) scale(.85)"/><path d="m7 17 10-10"/>',
  shield: '<path d="m12 3 8 3v6c0 5-8 9-8 9s-8-4-8-9V6Z"/><path d="m8 12 3 3 5-6"/>',
  doctor: '<path d="M6 3v6a5 5 0 0 0 10 0V3M4 3h4m6 0h4m-7 11v3a4 4 0 0 0 8 0v-3"/><circle cx="19" cy="12" r="2"/>',
  user: '<circle cx="12" cy="8" r="4"/><path d="M4 21v-2a8 8 0 0 1 16 0v2"/>',
  plus: '<path d="M12 5v14M5 12h14"/>', close: '<path d="m6 6 12 12M6 18 18 6"/>',
  refresh: '<path d="M20 8a9 9 0 1 0 1 8M20 3v5h-5"/>', bell: '<path d="M5 17h14l-2-3V9a5 5 0 0 0-10 0v5Zm5 3h4"/>',
  leaf: '<path d="M20 3C6 2 1 8 5 15s17 4 15-12Z"/><path d="m4 21 12-12"/>',
  logout: '<path d="M10 4H4v16h6m4-4 4-4-4-4m-5 4h12"/>',
  clock: '<circle cx="12" cy="12" r="9"/><path d="M12 7v6l4 2"/>',
  check: '<path d="m5 12 4 4L19 6"/>', video: '<rect x="3" y="5" width="12" height="14" rx="2"/><path d="m15 9 6-3v12l-6-3"/>',
  search: '<circle cx="10" cy="10" r="6"/><path d="m15 15 6 6"/>'
};
const icon = name => `<svg class="icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${paths[name] || paths.pill}</svg>`;
const logo = () => `<a class="logo" href="#" data-action="home"><img src="logo.png" alt=""><span><strong>${lang() === 'ar' ? 'جرعة' : 'Jur’a'}</strong><small>${lang() === 'ar' ? 'JUR’A · DOSE' : 'جرعة · DOSE'}</small></span></a>`;
const button = (text, action, id = '', style = '', symbol = '', disabled = false) => `<button type="button" class="btn ${style}" data-action="${action}" ${id === '' ? '' : `data-id="${e(id)}"`} ${disabled ? 'disabled' : ''}>${symbol ? icon(symbol) : ''}${T(text)}</button>`;
const languageButton = () => `<button type="button" class="icon-btn" data-action="language" aria-label="${lang() === 'ar' ? 'Switch to English' : 'التبديل للعربية'}">${lang() === 'ar' ? 'English' : 'العربية'}</button>`;
const statusKey = { DRUG: 'Medication', SUPPLEMENT: 'Supplement', INACTIVE: 'Inactive', PENDING: 'Pending', TAKEN: 'Taken', SKIPPED: 'Skipped', MISSED: 'Missed', APPROVED: 'Approved', REJECTED: 'Rejected', CANCELLED: 'Cancelled', HIGH: 'High', MODERATE: 'Moderate', NONE_IDENTIFIED: 'None identified', UNKNOWN: 'Unknown' };
const badge = (status, appointment = false) => `<span class="badge ${e(status)}">${T(appointment && status === 'PENDING' ? 'Awaiting approval' : (statusKey[status] || 'Unknown'))}</span>`;
const empty = (title, description = '', symbol = 'pill') => `<div class="empty">${icon(symbol)}<h3>${T(title)}</h3>${description ? `<p>${T(description)}</p>` : ''}</div>`;
const loading = () => `<div class="loading" role="status"><span class="spinner"></span>${T('Loading…')}</div>`;
const field = (name, label, value = '', type = 'text', extra = '') => `<label class="field"><span>${T(label)}</span><input name="${name}" type="${type}" value="${e(value)}" ${extra}></label>`;
const textarea = (name, label, value = '', extra = '') => `<label class="field"><span>${T(label)}</span><textarea name="${name}" ${extra}>${e(value)}</textarea></label>`;
const feedback = () => '<p class="form-error" role="alert"></p>';
const formActions = (label = 'Save') => `${feedback()}<div class="dialog-actions">${button('Cancel', 'close', '', 'secondary')}<button class="btn" type="submit">${T(label)}</button></div>`;
const ownedItem = id => state.items.find(item => item.id === positiveId(id));
const itemName = id => ownedItem(id)?.displayName || t('Unknown item');
const identityMatches = version => version === identityVersion && state.user;

function toast(message, error = false) {
  const node = document.createElement('div');
  node.className = 'toast' + (error ? ' error' : '');
  node.textContent = t(message);
  $('#toasts').append(node);
  setTimeout(() => node.remove(), error ? 9000 : 4500);
}
function showError(error) { toast(error.message || 'Something went wrong', true); }
function closeDialog() { dialog.close(); modalSubmit = null; modalVersion++; dialog.innerHTML = ''; }
function openDialog(title, content, onSubmit = null) {
  modalVersion++;
  modalSubmit = onSubmit;
  dialog.innerHTML = `<div class="dialog-header"><h2 id="dialog-title">${e(title)}</h2><button type="button" class="icon-btn" data-action="close" aria-label="${T('Close')}">${icon('close')}</button></div><div class="dialog-body">${content}</div>`;
  if (!dialog.open) dialog.showModal();
}
function confirmAction(title, message, action, label = 'Confirm', danger = false) {
  openDialog(t(title), `<form><p>${T(message)}</p>${danger ? `<p class="caption">${T('This action cannot be undone.')}</p>` : ''}${feedback()}<div class="dialog-actions">${button('Cancel', 'close', '', 'secondary')}<button type="submit" class="btn ${danger ? 'danger' : ''}">${T(label)}</button></div></form>`, action);
}
function saveIdentity(user) {
  state.user = { id: user.id, name: user.name, email: user.email, role: user.role };
  sessionStorage.setItem('jura-user', JSON.stringify(state.user));
}
function signOut() {
  identityVersion++;
  state.user = null;
  sessionStorage.removeItem('jura-user');
  state.items = []; state.appointments = []; state.doctors = []; state.reminders = []; state.reminderError = ''; state.checking = false;
  closeDialog(); render();
}

function renderAuth() {
  const signup = state.authTab === 'signup';
  app.innerHTML = `<main class="auth" id="main"><section class="auth-story">${logo()}<div><span class="eyebrow">${T('PERSONAL HEALTH COMPANION')}</span><h1>${T('Your health, one dose at a time.')}</h1><p>${T('A simple place for your medications, daily doses, and doctor consultations.')}</p></div><div class="auth-illustration" aria-hidden="true"><div class="orbit"></div><div class="orbit second"></div><div class="pill-illustration"></div><div class="floating-note one">${icon('clock')}${T('Keep your doses on track')}</div><div class="floating-note two">${icon('shield')}${T('Your medications, together')}</div></div><span class="footnote">${T('Course prototype · Use demonstration data')}</span></section><section class="auth-panel"><div class="auth-form"><div class="auth-top">${languageButton()}</div><div class="tabs">${['login', 'signup'].map(tab => `<button type="button" class="${state.authTab === tab ? 'active' : ''}" data-action="auth-tab" data-id="${tab}">${T(tab === 'login' ? 'Sign in' : 'Create account')}</button>`).join('')}</div><h2>${T(signup ? 'Create your account' : 'Welcome back')}</h2><p>${T(signup ? 'Let’s start with a few details.' : 'Sign in to continue your health journey.')}</p><form id="auth-form">${signup ? field('name', 'Full name', '', 'text', 'required maxlength="100" autocomplete="name"') : ''}${field('email', 'Email', '', 'email', 'required maxlength="255" autocomplete="username"')}${field('password', 'Password', '', 'password', `required maxlength="255" autocomplete="${signup ? 'new-password' : 'current-password'}"`)}${signup ? `<label class="field"><span>${T('Account type')}</span><select name="role"><option value="PATIENT">${T('Patient')}</option><option value="DOCTOR">${T('Doctor')}</option></select></label>` : ''}${feedback()}<button type="submit" class="btn">${T(signup ? 'Create account' : 'Sign in')}</button></form><p class="caption" style="margin-top:22px">${T('Course prototype · Use demonstration data')}</p></div></section></main>`;
}
const pageMeta = {
  today: ['PERSONAL HEALTH COMPANION', 'Hello', 'A clear view of your day, all in one place.'],
  items: ['YOUR PERSONAL LIST', 'My medications', 'Your medications and supplements, without the clutter.'],
  interactions: ['INGREDIENTS, IN CONTEXT', 'Look at the whole picture.', 'Choose one active item to check against the rest of your active list. Your dosage notes and medical conditions are included.'],
  consultations: ['CARE, WHEN YOU NEED IT', 'Talk to a doctor.', 'Choose a doctor and request a time. The doctor confirms the appointment.'],
  appointments: ['DOCTOR WORKSPACE', 'Your consultation day.', 'Review patient requests and join your approved consultations.'],
  profile: ['ACCOUNT DETAILS', 'Make it personal.', 'Keep your details up to date.']
};
function renderShell() {
  const doctor = state.user.role === 'DOCTOR';
  const nav = doctor ? [['appointments','Appointments','doctor'], ['profile','My profile','user']] : [['today','Today','today'], ['items','My medications','pill'], ['interactions','Interaction check','shield'], ['consultations','Consultations','doctor'], ['profile','My profile','user']];
  const meta = pageMeta[state.page];
  app.innerHTML = `<div class="shell"><aside class="sidebar">${logo()}<nav class="nav" aria-label="${T('Your daily health companion')}">${nav.map(([page,title,symbol]) => `<button type="button" data-action="navigate" data-id="${page}" class="${state.page === page ? 'active' : ''}" ${state.page === page ? 'aria-current="page"' : ''}>${icon(symbol)}${T(title)}</button>`).join('')}</nav><div class="sidebar-bottom">${icon('leaf')}<strong>${T('Your daily health companion')}</strong><p>${T('Course prototype · Use demonstration data')}</p></div></aside><div class="workspace"><header class="topbar"><span class="eyebrow">${T(doctor ? 'DOCTOR WORKSPACE' : 'PERSONAL HEALTH COMPANION')}</span><div class="actions"><div class="identity"><div class="avatar" aria-hidden="true">${e(state.user.name.slice(0,1))}</div><div><strong><bdi>${e(state.user.name)}</bdi></strong><small>${T(doctor ? 'Doctor' : 'Patient')}</small></div></div>${languageButton()}<button type="button" class="icon-btn" data-action="logout" aria-label="${T('Sign out')}" title="${T('Sign out')}">${icon('logout')}</button></div></header><main class="content" id="main" tabindex="-1"><div class="page-head"><div><span class="eyebrow">${T(meta[0])}</span><h1>${T(meta[1])}${state.page === 'today' ? `، <bdi>${e(state.user.name.split(' ')[0])}</bdi>` : ''}</h1><p>${T(meta[2])}</p></div><div class="date">${e(date(saudiToday()))}<br><small>${T('RIYADH TIME')}</small></div></div>${doctor ? '' : '<div id="reminders" class="reminder-panel"></div>'}<div id="page-content">${loading()}</div></main></div></div>`;
  renderReminders();
}
async function loadItems() {
  const userId = state.user.id;
  return list(await api('user-item/getAll')).filter(item => item.userId === userId);
}
async function userName(id) {
  try { const user = await api(`user/get-by-id/${id}`); return String(user.name || t('Name unavailable')); } catch { return t('Name unavailable'); }
}
async function render() {
  const version = ++renderVersion;
  if (!state.user) { renderAuth(); return; }
  renderShell();
  try {
    let html;
    switch (state.page) {
      case 'today': html = await todayPage(); break;
      case 'items': html = await itemsPage(version); break;
      case 'interactions': html = await interactionsPage(version); break;
      case 'consultations': html = await consultationsPage(version); break;
      case 'appointments': html = await doctorPage(version); break;
      case 'profile': html = await profilePage(version); break;
    }
    if (version !== renderVersion || !state.user) return;
    $('#page-content').innerHTML = html;
  } catch (error) {
    if (version !== renderVersion) return;
    $('#page-content').innerHTML = `<div class="notice error"><h3>${T('Something went wrong')}</h3><p>${e(t(error.message))}</p>${button('Retry','refresh','','secondary','refresh')}</div>`;
  }
}
function navigate(page) {
  const allowed = state.user?.role === 'DOCTOR' ? ['appointments','profile'] : ['today','items','interactions','consultations','profile'];
  if (!allowed.includes(page)) return;
  state.page = page; closeDialog(); render();
}

// PATIENT DASHBOARD & REMINDERS
function doseRow(dose, reminder = false) {
  return `<div class="${reminder ? 'reminder-row' : 'list-row'}"><div><h3><bdi>${e(dose.displayName)}</bdi></h3><p>${e(date(dose.dueAt))} ${dose.dosageText ? `· <bdi>${e(dose.dosageText)}</bdi>` : ''}</p></div><div class="actions">${badge(dose.status)}${dose.status !== 'TAKEN' ? `<button type="button" class="btn small" data-action="record-dose" data-dose="${e(JSON.stringify(dose))}" data-status="TAKEN">${T('Mark taken')}</button>` : ''}${dose.status === 'PENDING' ? `<button type="button" class="btn secondary small" data-action="record-dose" data-dose="${e(JSON.stringify(dose))}" data-status="SKIPPED">${T('Skip dose')}</button>` : ''}</div></div>`;
}
async function todayPage() {
  const userId = state.user.id;
  const [doses, adherence, appointments] = await Promise.all([api(`dose-schedule/today/${userId}`), api(`dose-log/adherence-by-user/${userId}`), api(`appointment/get-by-patient/${userId}`)]);
  const upcoming = list(appointments).filter(a => ['APPROVED','PENDING'].includes(a.status) && saudiDateTime(a.endAt) > new Date()).sort((a,b) => a.startAt.localeCompare(b.startAt)).slice(0,3);
  return `<div class="stack"><div class="hero"><div><span class="eyebrow">${T('Your daily health companion')}</span><h2>${T('A routine that works for you.')}</h2><p>${T('Keep your medications and supplements together, then check their ingredients for possible interactions.')}</p><div class="actions">${button('Manage medications','navigate','items','','pill')}${button('Book a consultation','navigate','consultations','secondary','doctor')}</div></div><div class="hero-art" aria-hidden="true">${icon('pill')}</div></div><div class="metric-grid">${[['Recorded adherence',adherence.takenPercentage == null ? '—' : adherence.takenPercentage + '%','accent'],['Taken',adherence.taken,''],['Skipped',adherence.skipped,''],['Missed',adherence.missed,'']].map(([title,value,style])=>`<div class="metric ${style}"><span class="label">${T(title)}</span><span class="value">${e(value)}</span></div>`).join('')}</div><p class="caption">${T('Based on taken, skipped, and missed records; pending doses are excluded.')}</p><div class="split"><section class="card"><div class="section-head"><h2>${T('Today’s doses')}</h2>${button('Refresh','refresh','','secondary small','refresh')}</div><div id="today-doses">${list(doses).length ? list(doses).map(d => doseRow(d)).join('') : empty('No doses scheduled today','Add an intake schedule from My medications.','clock')}</div><p class="caption" style="margin-top:18px">${T('Unrecorded doses become missed two hours after their scheduled time.')}</p></section><section class="card"><div class="section-head"><h2>${T('Next consultations')}</h2></div>${upcoming.length ? upcoming.map(a=>`<div class="list-row"><div>${badge(a.status,true)}<p style="margin-top:10px">${e(date(a.startAt))}</p></div></div>`).join('') : empty('No upcoming appointments','Browse doctors and send a request for a time that suits you.','doctor')}${button('My appointments','navigate','consultations','ghost')}</section></div></div>`;
}
function renderReminders() {
  const node = $('#reminders');
  if (!node) return;
  node.innerHTML = state.reminderError ? `<p class="form-error">${e(t(state.reminderError))}</p><p class="caption">${T('Reminders update every 30 seconds while this page is open.')}</p>` : state.reminders.length ? `<h3>${icon('bell')} ${T('Dose reminders')}</h3><p class="caption">${T('Reminders update every 30 seconds while this page is open.')}</p>${state.reminders.map(d => doseRow(d,true)).join('')}` : '';
}
async function pollReminders() {
  if (polling || !state.user || state.user.role !== 'PATIENT' || document.hidden) return;
  polling = true;
  const version = identityVersion;
  const view = renderVersion;
  const userId = state.user.id;
  try {
    const reminders = await api(`dose-schedule/reminders/${userId}`);
    if (!identityMatches(version)) return;
    state.reminders = list(reminders); state.reminderError = ''; renderReminders();
    if (state.page === 'today') {
      const [doses, adherence] = await Promise.all([api(`dose-schedule/today/${userId}`), api(`dose-log/adherence-by-user/${userId}`)]);
      if (identityMatches(version) && view === renderVersion && $('#today-doses')) $('#today-doses').innerHTML = list(doses).length ? list(doses).map(d => doseRow(d)).join('') : empty('No doses scheduled today','Add an intake schedule from My medications.','clock');
      if (identityMatches(version) && view === renderVersion && state.page === 'today') {
        const values = $('#page-content')?.querySelectorAll('.metric .value');
        if (values?.length === 4) [adherence.takenPercentage == null ? '—' : adherence.takenPercentage + '%', adherence.taken, adherence.skipped, adherence.missed].forEach((value,index)=>{ values[index].textContent = String(value); });
      }
    }
  } catch (error) {
    if (identityMatches(version)) { state.reminderError = error.message; renderReminders(); }
  } finally { polling = false; }
}
async function recordDose(dose, status) {
  const payload = { scheduleId: dose.scheduleId, dueAt: dose.dueAt, status, takenAt: null };
  let logId = dose.logId;
  try {
    await api(logId ? `dose-log/update/${logId}` : 'dose-log/add', logId ? 'PUT' : 'POST', payload);
  } catch (error) {
    const duplicate = error.status === 409 || (error.status === 400 &&
      error.message === 'This scheduled dose already has a log; update the existing log');
    if (!duplicate || logId) throw error;
    // The missed-dose scanner can create a row after the page was loaded.
    const records = list(await api(`dose-log/get-by-schedule/${dose.scheduleId}`));
    const existing = records.find(log => saudiDateTime(log.dueAt).getTime() === saudiDateTime(dose.dueAt).getTime());
    if (!existing) throw error;
    await api(`dose-log/update/${existing.id}`, 'PUT', payload);
  }
  toast('Saved successfully'); await render(); await pollReminders();
}

// MEDICATIONS & SUPPLEMENTS
async function itemsPage(version) {
  const items = await loadItems();
  if (version !== renderVersion) return '';
  state.items = items;
  const visible = items.filter(item => state.showInactive || item.active);
  return `<div class="stack"><div class="row between"><div class="actions">${button('Add medication','drug-search','','','plus')}${button('Add supplement','add-supplement','','secondary','plus')}</div><label class="check"><input type="checkbox" id="show-inactive" ${state.showInactive ? 'checked' : ''}>${T('Show inactive items')}</label></div>${visible.length ? `<div class="grid">${visible.map(item => `<article class="card item-card ${item.active ? '' : 'inactive'}"><div class="row between"><div class="row"><div class="item-symbol ${item.type}">${icon(item.type === 'DRUG' ? 'pill' : 'leaf')}</div><h3><bdi>${e(item.displayName)}</bdi></h3></div><div class="actions">${badge(item.type)}${item.active ? '' : badge('INACTIVE')}</div></div><p class="description"><bdi>${e(item.dosageText || t('No dosage notes'))}</bdi></p><div class="actions">${button('Ingredients','ingredients',item.id,'secondary small')}${button('Schedules','schedules',item.id,'secondary small')}${button('History','dose-history',item.id,'secondary small')}</div><div class="row between"><div class="actions">${item.active ? button('Check interactions','check-item',item.id,'small','shield',state.checking) : ''}${button('Edit','edit-item',item.id,'ghost small')}</div>${button('Delete','delete-item',item.id,'danger small')}</div></article>`).join('')}</div>` : empty('No items in your list','Search for a Saudi medication or add a supplement with its ingredients.')}</div>`;
}
function openDrugSearch() {
  state.search = null;
  openDialog(t('Search Saudi medications'), `<p class="caption">${T('Search by trade name in Arabic or English. Results come from the SFDA catalogue stored on the server.')}</p><form id="drug-search-form" class="search-bar">${field('q','Trade name','','search','required maxlength="255" autofocus')}<button class="btn" type="submit">${icon('search')}${T('Search')}</button>${feedback()}</form><div id="drug-matches" style="margin-top:20px">${empty('Search for a medication to see matches.','','search')}</div>`, form => searchDrugs(new FormData(form).get('q').trim(),1));
}
async function searchDrugs(q, page) {
  if (!q) return;
  const version = modalVersion;
  const node = $('#drug-matches');
  node.innerHTML = loading();
  try {
    const result = await api(`drugs/search?q=${encodeURIComponent(q)}&page=${page}`);
    if (version !== modalVersion || !dialog.open) return;
    state.search = { ...result, q };
    node.innerHTML = list(result.drugs).length ? `${list(result.drugs).map(drug => `<div class="result-row"><div><h3><bdi>${e((lang() === 'ar' ? drug.tradeNameAr : drug.tradeNameEn) || drug.tradeNameEn || drug.tradeNameAr)}</bdi></h3><p><bdi>${e((lang() === 'ar' ? drug.tradeNameEn : drug.tradeNameAr) || '')}</bdi></p><p class="muted" dir="auto">${e(drug.scientificName || '')}</p></div>${button('Select','select-drug',drug.id,'secondary small')}</div>`).join('')}<div class="pagination">${button('Previous','drug-page',result.page - 1,'secondary small','',result.page <= 1)}<small>${T('Page')} ${e(result.page)} ${T('of')} ${e(result.totalPages)}</small>${button('Next','drug-page',result.page + 1,'secondary small','',result.page >= result.totalPages)}</div><p class="caption" style="text-align:center">${e(result.totalResults)} ${T('matches')}</p>` : empty('No matching medications','Try another trade name or spelling.','search');
  } catch (error) {
    if (version !== modalVersion) return;
    node.innerHTML = `<div class="notice error">${e(t(error.message))}</div>`;
  }
}
function selectDrug(id) {
  const drug = state.search?.drugs.find(d => d.id === id);
  if (!drug) return;
  openDialog(t('Add medication'), `<form><h3><bdi>${e(drug.tradeNameAr || drug.tradeNameEn)}</bdi></h3><p class="muted" dir="auto">${e(drug.scientificName || '')}</p>${drug.scientificName ? '' : `<div class="notice">${T('Ingredient data is unavailable for this product. Interaction checks may return UNKNOWN.')}</div>`}${field('dosageText','Dosage notes','','text',`maxlength="255" placeholder="${T('Example: one tablet daily, as prescribed')}"`)}${formActions('Add to my list')}</form>`, async form => {
    await api('user-item/add','POST',{ userId: state.user.id, type: 'DRUG', drugCacheId: drug.id, dosageText: new FormData(form).get('dosageText').trim() || null });
    closeDialog(); toast('Saved successfully'); state.page = 'items'; await render();
  });
}
function ingredientRow() {
  return `<div class="ingredient-row">${field('nameEn','Ingredient name in English','','text','required maxlength="255" dir="ltr"')}${field('nameAr','Ingredient name in Arabic','','text',`maxlength="255" dir="rtl" placeholder="${T('Optional')}"`)}<button type="button" class="icon-btn" data-action="remove-ingredient-row" aria-label="${T('Remove ingredient')}">${icon('close')}</button></div>`;
}
function addSupplement() {
  openDialog(t('Add supplement'), `<form>${field('displayName','Supplement name','','text','required maxlength="255"')}${field('dosageText','Dosage notes','','text','maxlength="255"')}<p class="caption">${T('Use the ingredients on the product label. Add a separate row for each ingredient.')}</p><div id="ingredient-rows" class="stack">${ingredientRow()}</div>${button('Add another ingredient','add-ingredient-row','','secondary','plus')}${formActions('Add to my list')}</form>`, async form => {
    const data = new FormData(form);
    const english = data.getAll('nameEn'); const arabic = data.getAll('nameAr');
    const ingredients = english.map((name,index) => ({ nameEn: name.trim(), nameAr: arabic[index].trim() || null }));
    if (new Set(ingredients.map(i => i.nameEn.toLowerCase())).size !== ingredients.length) throw new Error(t('Duplicate ingredients are not allowed.'));
    await api('user-item/add-supplement','POST',{ userId: state.user.id, displayName: data.get('displayName').trim(), dosageText: data.get('dosageText').trim() || null, ingredients });
    closeDialog(); toast('Saved successfully'); state.page = 'items'; await render();
  });
}
function editItem(item) {
  openDialog(t('Edit item'), `<form>${item.type === 'SUPPLEMENT' ? field('displayName','Supplement name',item.displayName,'text','required maxlength="255"') : `<h3><bdi>${e(item.displayName)}</bdi></h3>`}${field('dosageText','Dosage notes',item.dosageText,'text','maxlength="255"')}<label class="check"><input type="checkbox" name="active" ${item.active ? 'checked' : ''}>${T('Active')}</label>${formActions()}</form>`, async form => {
    const data = new FormData(form);
    await api(`user-item/update/${item.id}`,'PUT',{ userId: state.user.id, type: item.type, drugCacheId: item.drugCacheId, displayName: item.type === 'SUPPLEMENT' ? data.get('displayName').trim() : item.displayName, dosageText: data.get('dosageText').trim() || null, active: data.has('active') });
    closeDialog(); toast('Saved successfully'); await render(); await pollReminders();
  });
}
async function openIngredients(item) {
  openDialog(t('Ingredients') + ' · ' + item.displayName, loading());
  const version = modalVersion;
  const ingredients = list(await api(`item-ingredient/get-by-item/${item.id}`));
  if (version !== modalVersion) return;
  const drug = item.type === 'DRUG';
  openDialog(t('Ingredients') + ' · ' + item.displayName, `<p class="caption">${T(drug ? 'Drug ingredients come from the SFDA catalogue and cannot be edited here.' : 'A supplement needs at least one ingredient.')}</p>${ingredients.length ? ingredients.map(i=>`<div class="list-row"><div><h3><bdi>${e(i.nameEn)}</bdi></h3><p><bdi>${e(i.nameAr || '')}</bdi></p></div>${drug ? '' : `<div class="actions">${button('Edit','edit-ingredient',i.id,'secondary small')}${button('Delete','delete-ingredient',i.id,'danger small','',ingredients.length <= 1)}</div>`}</div>`).join('') : empty('No ingredients recorded')}<div class="actions" style="margin-top:20px">${button(drug ? 'Refresh drug ingredients' : 'Add ingredient',drug ? 'sync-ingredients' : 'add-ingredient',item.id,'secondary','plus')}${button('Close','close','','ghost')}</div>`);
  state.ingredientContext = { item, ingredients };
}
function ingredientForm(item, ingredient = null) {
  openDialog(t(ingredient ? 'Edit ingredient' : 'Add ingredient'), `<form>${field('nameEn','Ingredient name in English',ingredient?.nameEn,'text','required maxlength="255" dir="ltr"')}${field('nameAr','Ingredient name in Arabic',ingredient?.nameAr,'text','maxlength="255" dir="rtl"')}${formActions()}</form>`, async form => {
    const data = new FormData(form);
    await api(ingredient ? `item-ingredient/update/${ingredient.id}` : 'item-ingredient/add',ingredient ? 'PUT' : 'POST',{ itemId: item.id, nameEn: data.get('nameEn').trim(), nameAr: data.get('nameAr').trim() || null });
    toast('Saved successfully'); await openIngredients(item);
  });
}

// SCHEDULES & ADHERENCE HISTORY
async function openSchedules(item) {
  openDialog(t('Intake schedules') + ' · ' + item.displayName,loading());
  const version = modalVersion;
  const schedules = list(await api(`dose-schedule/get-by-item/${item.id}`));
  if (version !== modalVersion) return;
  state.scheduleContext = { item, schedules };
  openDialog(t('Intake schedules') + ' · ' + item.displayName, `<p class="caption">${T('Add one schedule for each daily intake time. All times use Riyadh time.')}</p>${schedules.length ? schedules.map(s => `<div class="list-row"><div><span class="schedule-time">${e(s.localTime.slice(0,5))}</span><p>${e(s.daysOfWeek.split(',').map(dayLabel).join(' · '))}</p><p>${e(date(s.startDate))} — ${s.endDate ? e(date(s.endDate)) : T('No end date')}</p></div><div class="actions">${button('Edit','edit-schedule',s.id,'secondary small')}${button('Delete','delete-schedule',s.id,'danger small')}</div></div>`).join('') : empty('No intake schedules','','clock')}<div class="actions" style="margin-top:20px">${button('Add schedule','add-schedule',item.id,'','plus',!item.active)}${button('Close','close','','secondary')}</div>`);
}
function scheduleForm(item, schedule = null) {
  openDialog(t(schedule ? 'Edit schedule' : 'Add schedule'), `<form><div class="form-grid">${field('localTime','Time',schedule?.localTime.slice(0,5) || '08:00','time','required')}${field('startDate','Start date',schedule?.startDate || saudiToday(),'date','required')}${field('endDate','End date',schedule?.endDate || '','date')}<div class="field full"><span>${T('Days of the week')}</span><div class="days">${days.map(day=>`<label><input type="checkbox" name="days" value="${day}" ${!schedule || schedule.daysOfWeek.split(',').includes(day) ? 'checked' : ''}>${e(dayLabel(day))}</label>`).join('')}</div></div></div><p class="caption">${T('RIYADH TIME')}</p>${formActions()}</form>`, async form => {
    const data = new FormData(form); const selected = data.getAll('days');
    if (!selected.length) throw new Error(t('Select at least one day.'));
    if (data.get('endDate') && data.get('endDate') < data.get('startDate')) throw new Error(t('End date must be on or after the start date.'));
    await api(schedule ? `dose-schedule/update/${schedule.id}` : 'dose-schedule/add',schedule ? 'PUT' : 'POST',{ itemId: item.id, localTime: data.get('localTime') + ':00', daysOfWeek: selected.join(','), startDate: data.get('startDate'), endDate: data.get('endDate') || null });
    toast('Saved successfully'); await openSchedules(item); await pollReminders();
  });
}
async function openHistory(item) {
  openDialog(t('Dose history') + ' · ' + item.displayName,loading());
  const version = modalVersion;
  const logs = list(await api(`dose-log/get-by-item/${item.id}`));
  if (version !== modalVersion) return;
  state.historyContext = { item, logs };
  openDialog(t('Dose history') + ' · ' + item.displayName, logs.length ? logs.map(log => `<div class="list-row"><div>${badge(log.status)}<p style="margin-top:9px">${T('Scheduled')}: ${e(date(log.dueAt))}</p>${log.takenAt ? `<p>${T('Taken at')}: ${e(date(log.takenAt))}</p>` : ''}</div><div class="actions">${button('Correct record','edit-log',log.id,'secondary small')}${button('Delete','delete-log',log.id,'danger small')}</div></div>`).join('') : empty('No dose records yet','','clock'));
}
function correctLog(log) {
  const item = state.historyContext.item;
  openDialog(t('Correct dose record'), `<form><p class="caption">${T('Scheduled')}: ${e(date(log.dueAt))}</p><label class="field"><span>${T('Dose status')}</span><select name="status">${['TAKEN','SKIPPED','MISSED'].map(status=>`<option value="${status}" ${status === log.status ? 'selected' : ''}>${T(statusKey[status])}</option>`).join('')}</select></label>${field('takenAt','Taken at',log.takenAt?.slice(0,16) || '','datetime-local')}<p class="caption">${T('Set a past taken time, or leave blank to use the current time.')}</p>${formActions()}</form>`, async form => {
    const data = new FormData(form); const status = data.get('status');
    await api(`dose-log/update/${log.id}`,'PUT',{ scheduleId: log.scheduleId, dueAt: log.dueAt, status, takenAt: status === 'TAKEN' ? data.get('takenAt') || saudiNow() : null });
    toast('Saved successfully'); await openHistory(item); await pollReminders();
  });
}

// EXPLICIT AI INTERACTION CHECKS
function assessment(result) {
  let sources = [];
  try { sources = list(JSON.parse(result.sourceLinksJson)).map(safeHttps).filter(Boolean); } catch { /* no usable source URLs */ }
  const origin = result.modelName === 'MANUAL' ? 'Manual record' : result.modelName === 'INPUT_VALIDATION' ? 'Input validation' : 'AI assessment';
  return `<article class="card ai-result ${e(result.resultStatus)}"><div class="row between"><h3><bdi>${e(itemName(result.itemAId))}</bdi> + <bdi>${e(itemName(result.itemBId))}</bdi></h3>${badge(result.resultStatus)}</div>${lang() === 'en' ? `<p class="caption">${T('Assessment text is provided in Arabic by the current backend.')}</p>` : ''}<div class="ai-copy" lang="ar"><strong>${T('Explanation')}</strong><p>${e(result.explanationAr)}</p><strong>${T('Advice')}</strong><p>${e(result.adviceAr)}</p></div><div class="caption">${T(origin)} · ${e(date(result.checkedAt))}</div><div class="row between" style="margin-top:12px">${sources.length ? `<details><summary>${T('Source links')}</summary>${sources.map(url=>`<p><a href="${e(url)}" target="_blank" rel="noopener noreferrer"><bdi>${e(url)}</bdi></a></p>`).join('')}</details>` : `<small>${T('No verified source links were returned.')}</small>`}${button('Delete','delete-assessment',result.id,'danger small')}</div></article>`;
}
async function interactionsPage(version) {
  const items = await loadItems();
  if (version !== renderVersion) return '';
  state.items = items;
  const active = items.filter(i => i.active);
  const histories = await Promise.all(items.map(item => api(`interaction/get-by-item/${item.id}`)));
  const results = [...new Map(histories.flatMap(list).map(result => [result.id,result])).values()].sort((a,b) => b.checkedAt.localeCompare(a.checkedAt) || b.id - a.id);
  return `<div class="stack"><div class="notice">${T('AI results are informational and may be incomplete or incorrect. A result with no identified interaction does not prove safety. Consult a pharmacist before changing medicines.')}</div><section class="card"><form id="interaction-form" class="search-bar"><label class="field" style="flex:1"><span>${T('Item to check')}</span><select name="itemId" required><option value="">${T('Choose an item')}</option>${active.map(i=>`<option value="${i.id}">${e(i.displayName)}</option>`).join('')}</select></label><button type="submit" class="btn" style="align-self:end" ${active.length < 2 || state.checking ? 'disabled' : ''}>${state.checking ? '<span class="spinner"></span>' : icon('shield')}${T(state.checking ? 'Checking ingredients…' : 'Run check')}</button>${feedback()}</form>${active.length < 2 ? `<p class="caption" style="margin-top:14px">${T('Run a check after you have added at least two active items with ingredients.')}</p>` : ''}</section><div class="section-head"><h2>${T('Latest results and history')}</h2>${button('Refresh','refresh','','secondary small','refresh')}</div><p class="caption">${T('Previous assessments may no longer match your current ingredients, doses, or medical conditions. Run a new check after changes.')}</p>${results.length ? results.map(assessment).join('') : empty('No interaction results yet','Run a check after you have added at least two active items with ingredients.','shield')}</div>`;
}
async function runCheck(id) {
  if (state.checking) return;
  const identity = identityVersion;
  state.checking = true;
  state.page = 'interactions'; render();
  try {
    const response = await api(`interaction/check/${id}`,'POST');
    if (!identityMatches(identity)) return;
    toast(list(response.results).length ? 'Check complete' : 'No other active items to compare.');
  } finally {
    if (identityMatches(identity)) { state.checking = false; if (state.page === 'interactions') await render(); }
  }
}

// DOCTOR DISCOVERY, APPOINTMENT REQUESTS & TIME-GATED JOIN
function appointmentCard(appointment, counterpart, doctor = false) {
  const now = new Date(); const start = saudiDateTime(appointment.startAt); const end = saudiDateTime(appointment.endAt);
  const canJoin = appointment.status === 'APPROVED' && now >= start && now <= end;
  return `<article class="card"><div class="row between"><div class="row">${icon(doctor ? 'user' : 'doctor')}<h3 style="margin:0"><bdi>${e(counterpart)}</bdi></h3></div>${badge(appointment.status,true)}</div><p style="margin:17px 0 5px;font-size:14px">${e(date(appointment.startAt))}</p><p class="caption">${T('End time')}: ${e(date(appointment.endAt))} · ${T('RIYADH TIME')}</p><div class="actions" style="margin-top:17px">${appointment.status === 'PENDING' ? doctor ? `${button('Approve & create meeting','approve-appointment',appointment.id,'small','video')}${button('Reject request','reject-appointment',appointment.id,'secondary small')}` : button('Edit request','edit-appointment',appointment.id,'secondary small') : ''}${appointment.status === 'APPROVED' ? button('Join meeting','join',appointment.id,'small','video',!canJoin) : ''}${button('Delete appointment','delete-appointment',appointment.id,'danger small')}</div>${appointment.status === 'APPROVED' ? `<p class="caption" style="margin-top:13px">${T(now > end ? 'Appointment ended' : 'Join opens during the scheduled window.')}</p>` : ''}</article>`;
}
async function consultationsPage(version) {
  const userId = state.user.id;
  const [profiles, appointments] = await Promise.all([api('doctor/getAll'),api(`appointment/get-by-patient/${userId}`)]);
  const ids = [...new Set([...list(profiles).map(d => d.userId),...list(appointments).map(a => a.doctorId)])];
  const names = await Promise.all(ids.map(userName)); const nameMap = new Map(ids.map((id,index)=>[id,names[index]]));
  if (version !== renderVersion) return '';
  state.doctors = list(profiles).map(d => ({ ...d, name: nameMap.get(d.userId) })); state.appointments = list(appointments); state.doctorNames = nameMap;
  return `<div class="stack"><section class="card"><div class="section-head"><h2>${T('Browse doctors')}</h2></div><form id="doctor-search-form" class="search-bar">${field('specialty','Specialty','','search',`maxlength="100" placeholder="${T('All specialties')}"`)}<button class="btn secondary" type="submit">${icon('search')}${T('Search')}</button>${feedback()}</form><div id="doctor-results" style="margin-top:20px">${doctorResults(state.doctors)}</div></section><div class="section-head"><h2>${T('My appointments')}</h2>${button('Refresh','refresh','','secondary small','refresh')}</div><div id="appointment-list" class="grid">${state.appointments.length ? state.appointments.map(a=>appointmentCard(a,nameMap.get(a.doctorId))).join('') : `<div class="full">${empty('No appointments yet','Browse doctors and send a request for a time that suits you.','doctor')}</div>`}</div></div>`;
}
function doctorResults(profiles) {
  return profiles.length ? profiles.map(profile => `<div class="result-row"><div><h3><bdi>${e(profile.name)}</bdi></h3><p><bdi>${e(profile.specialty)}</bdi></p>${profile.bio ? `<p class="muted"><bdi>${e(profile.bio)}</bdi></p>` : ''}</div>${button('Request appointment','request-appointment',profile.userId,'secondary small','plus')}</div>`).join('') : empty('No doctors found','No doctor profiles are available for this search.','doctor');
}
async function searchDoctors(form) {
  const specialty = new FormData(form).get('specialty').trim(); const version = renderVersion;
  const profiles = list(await api(specialty ? `doctor/search?specialty=${encodeURIComponent(specialty)}` : 'doctor/getAll'));
  const named = await Promise.all(profiles.map(async profile => ({ ...profile, name: await userName(profile.userId) })));
  if (version !== renderVersion) return;
  state.doctors = named; $('#doctor-results').innerHTML = doctorResults(named);
}
function appointmentForm(doctorId, appointment = null) {
  openDialog(t(appointment ? 'Edit request' : 'Request appointment'), `<form>${appointment ? '' : `<h3><bdi>${e(state.doctors.find(d=>d.userId === doctorId)?.name || t('Doctor'))}</bdi></h3>`}<p class="caption">${T('The requested time needs the doctor’s approval. All times are Riyadh time.')}</p>${field('startAt','Start time',appointment?.startAt.slice(0,16) || '','datetime-local','required')}${field('endAt','End time',appointment?.endAt.slice(0,16) || '','datetime-local','required')}${formActions(appointment ? 'Save' : 'Send request')}</form>`, async form => {
    const data = new FormData(form); const startAt = data.get('startAt'); const endAt = data.get('endAt');
    if (endAt <= startAt) throw new Error(t('End time must be after start time.'));
    if (saudiDateTime(startAt) <= new Date()) throw new Error(t('Choose a future appointment time.'));
    await api(appointment ? `appointment/update/${appointment.id}` : 'appointment/add',appointment ? 'PUT' : 'POST',{ ...(appointment ? {} : { patientId: state.user.id, doctorId }), startAt, endAt });
    closeDialog(); toast('Saved successfully'); await render();
  });
}
async function doctorPage(version) {
  let profile;
  try { profile = await api(`doctor/get-by-user-id/${state.user.id}`); } catch (error) { if (error.status !== 404 && !(error.status === 400 && error.message === 'Doctor profile not found')) throw error; }
  if (!profile) return `<div class="notice info"><h2>${T('Finish your doctor profile')}</h2><p>${T('Add your specialty and bio in My profile to receive appointment requests.')}</p>${button('My profile','navigate','profile')}</div>`;
  const appointments = list(await api(`appointment/get-by-doctor/${state.user.id}`));
  const ids = [...new Set(appointments.map(a=>a.patientId))]; const names = await Promise.all(ids.map(userName)); const nameMap = new Map(ids.map((id,i)=>[id,names[i]]));
  if (version !== renderVersion) return '';
  state.appointments = appointments; state.patientNames = nameMap;
  const pending = appointments.filter(a=>a.status === 'PENDING').sort((a,b)=>a.startAt.localeCompare(b.startAt));
  return `<div class="stack"><div class="metric-grid"><div class="metric accent"><span class="label">${T('Pending requests')}</span><strong class="value">${pending.length}</strong></div><div class="metric"><span class="label">${T('Approved consultations')}</span><strong class="value">${appointments.filter(a=>a.status==='APPROVED').length}</strong></div></div><div class="section-head"><h2>${T('Pending requests')}</h2>${button('Refresh','refresh','','secondary small','refresh')}</div><div class="grid">${pending.length ? pending.map(a=>appointmentCard(a,nameMap.get(a.patientId),true)).join('') : `<div class="full">${empty('No appointments yet','','doctor')}</div>`}</div><div class="section-head"><h2>${T('All appointments')}</h2></div><div id="appointment-list" class="grid">${appointments.filter(a=>a.status !== 'PENDING').length ? appointments.filter(a=>a.status !== 'PENDING').map(a=>appointmentCard(a,nameMap.get(a.patientId),true)).join('') : `<div class="full">${empty('No appointments yet','','doctor')}</div>`}</div></div>`;
}
async function joinMeeting(id) {
  const response = await api(`meeting/join/${id}`);
  const url = safeHttps(response.joinUrl);
  if (!url) throw new Error(t('The meeting link is unavailable.'));
  openDialog(t('Your appointment is ready.'), `<p>${T('Open the meeting in a new tab.')}</p><div class="actions"><a class="btn" href="${e(url)}" target="_blank" rel="noopener noreferrer">${icon('video')}${T('Open Zoom')}</a>${button('Close','close','','secondary')}</div>`);
}

// PERSONAL & PROFESSIONAL PROFILE
async function profilePage(version) {
  const userId = state.user.id;
  const raw = await api(`user/get-by-id/${userId}`);
  // The course GET endpoint returns passwordHash. Project only the display fields.
  const profile = { name: raw.name, email: raw.email, medicalConditions: raw.medicalConditions };
  let doctorProfile = null;
  if (state.user.role === 'DOCTOR') {
    try { doctorProfile = await api(`doctor/get-by-user-id/${userId}`); } catch (error) { if (error.status !== 404 && !(error.status === 400 && error.message === 'Doctor profile not found')) throw error; }
  }
  if (version !== renderVersion) return '';
  state.hasDoctorProfile = !!doctorProfile;
  return `<div class="grid"><section class="card"><h2>${T('Account information')}</h2><form id="profile-form" class="stack">${field('name','Full name',profile.name,'text','required maxlength="100" autocomplete="name"')}${field('email','Email',profile.email,'email','required maxlength="255" autocomplete="username"')}${state.user.role === 'PATIENT' ? `${textarea('medicalConditions','Medical conditions',profile.medicalConditions,'maxlength="10000"')}<p class="caption">${T('Include conditions and allergies that may matter for your medicines.')}</p>` : `<input type="hidden" name="medicalConditions" value="${e(profile.medicalConditions || '')}">`}${field('password','Password','','password','required maxlength="255" autocomplete="new-password"')}<p class="caption">${T('Use your current password, or enter a new one to change it.')}</p>${feedback()}<button type="submit" class="btn">${T('Save profile')}</button></form></section>${state.user.role === 'DOCTOR' ? `<section class="card"><h2>${T('Doctor profile')}</h2><form id="doctor-profile-form" class="stack">${field('specialty','Specialty',doctorProfile?.specialty,'text','required maxlength="100"')}${textarea('bio','Bio',doctorProfile?.bio,'maxlength="10000"')}${feedback()}<button type="submit" class="btn">${T(doctorProfile ? 'Save doctor profile' : 'Create doctor profile')}</button></form>${doctorProfile ? `<div class="divider"></div>${button('Delete doctor profile','delete-doctor-profile','','danger small')}` : ''}</section>` : `<section class="card"><div class="item-symbol">${icon('shield')}</div><h2 style="margin-top:20px">${T('Your medications, together')}</h2><p class="muted">${T('Choose one active item to check against the rest of your active list. Your dosage notes and medical conditions are included.')}</p>${button('Interaction check','navigate','interactions','secondary','shield')}</section>`}<section class="card danger-zone full"><h3>${T('Delete account')}</h3><p class="caption">${T('Deleting your account also deletes your local medications, schedules, records, appointments, and meetings.')}</p>${button('Delete account','delete-account','','danger small')}</section></div>`;
}
async function saveProfile(form) {
  const data = new FormData(form);
  const user = { name: data.get('name').trim(), email: data.get('email').trim(), passwordHash: data.get('password'), medicalConditions: data.get('medicalConditions').trim() || null, role: state.user.role };
  await api(`user/update/${state.user.id}`,'PUT',user);
  saveIdentity({ ...state.user, name: user.name, email: user.email });
  toast('Saved successfully'); await render();
}

// ONE EVENT HANDLER FOR BUTTONS. Models keep the course's existing field names.
document.addEventListener('click', async event => {
  const target = event.target.closest('[data-action]');
  if (!target || target.disabled) return;
  event.preventDefault();
  const action = target.dataset.action; const id = positiveId(target.dataset.id);
  const identity = identityVersion;
  try {
    switch (action) {
      case 'home': if (state.user) navigate(state.user.role === 'DOCTOR' ? 'appointments' : 'today'); break;
      case 'language': setLanguage(lang() === 'ar' ? 'en' : 'ar'); closeDialog(); await render(); break;
      case 'auth-tab': state.authTab = target.dataset.id; renderAuth(); break;
      case 'logout': signOut(); break;
      case 'close': closeDialog(); break;
      case 'navigate': navigate(target.dataset.id); break;
      case 'refresh': await render(); await pollReminders(); break;
      case 'drug-search': openDrugSearch(); break;
      case 'drug-page': target.disabled = true; await searchDrugs(state.search.q, id); break;
      case 'select-drug': selectDrug(id); break;
      case 'add-supplement': addSupplement(); break;
      case 'add-ingredient-row': $('#ingredient-rows').insertAdjacentHTML('beforeend',ingredientRow()); break;
      case 'remove-ingredient-row': if ($('#ingredient-rows').children.length > 1) target.closest('.ingredient-row').remove(); else toast('A supplement needs at least one ingredient.'); break;
      case 'edit-item': editItem(ownedItem(id)); break;
      case 'delete-item': confirmAction('Confirm deletion','Delete this item and its schedules, dose history, and interaction results?',async()=>{ await api(`user-item/delete/${id}`,'DELETE'); closeDialog(); toast('Deleted successfully'); await render(); await pollReminders(); },'Delete permanently',true); break;
      case 'ingredients': await openIngredients(ownedItem(id)); break;
      case 'sync-ingredients': target.disabled = true; await api(`item-ingredient/sync-drug/${id}`,'POST'); toast('Saved successfully'); await openIngredients(ownedItem(id)); break;
      case 'add-ingredient': ingredientForm(ownedItem(id)); break;
      case 'edit-ingredient': ingredientForm(state.ingredientContext.item,state.ingredientContext.ingredients.find(i=>i.id===id)); break;
      case 'delete-ingredient': { const item = state.ingredientContext.item; confirmAction('Confirm deletion','Delete this ingredient?',async()=>{ await api(`item-ingredient/delete/${id}`,'DELETE'); toast('Deleted successfully'); await openIngredients(item); },'Delete permanently',true); break; }
      case 'schedules': await openSchedules(ownedItem(id)); break;
      case 'add-schedule': scheduleForm(ownedItem(id)); break;
      case 'edit-schedule': scheduleForm(state.scheduleContext.item,state.scheduleContext.schedules.find(s=>s.id===id)); break;
      case 'delete-schedule': { const item = state.scheduleContext.item; confirmAction('Confirm deletion','Delete this schedule and all its dose records?',async()=>{ await api(`dose-schedule/delete/${id}`,'DELETE'); toast('Deleted successfully'); await openSchedules(item); await pollReminders(); },'Delete permanently',true); break; }
      case 'dose-history': await openHistory(ownedItem(id)); break;
      case 'edit-log': correctLog(state.historyContext.logs.find(l=>l.id===id)); break;
      case 'delete-log': { const item = state.historyContext.item; confirmAction('Confirm deletion','Delete this dose record? An overdue dose may be marked missed again automatically.',async()=>{ await api(`dose-log/delete/${id}`,'DELETE'); toast('Deleted successfully'); await openHistory(item); await pollReminders(); },'Delete permanently',true); break; }
      case 'record-dose': target.disabled = true; await recordDose(JSON.parse(target.dataset.dose),target.dataset.status); break;
      case 'check-item': await runCheck(id); break;
      case 'delete-assessment': confirmAction('Confirm deletion','Delete this assessment?',async()=>{ await api(`interaction/delete/${id}`,'DELETE'); closeDialog(); toast('Deleted successfully'); await render(); },'Delete permanently',true); break;
      case 'request-appointment': appointmentForm(id); break;
      case 'edit-appointment': appointmentForm(null,state.appointments.find(a=>a.id===id)); break;
      case 'delete-appointment': confirmAction('Confirm deletion','Delete this appointment? Its local meeting record will be removed. An existing Zoom meeting will not be cancelled in Zoom.',async()=>{ await api(`appointment/delete/${id}`,'DELETE'); closeDialog(); toast('Deleted successfully'); await render(); },'Delete permanently',true); break;
      case 'approve-appointment': confirmAction('Confirm approval','Approve this request and create its Zoom meeting?',async()=>{ await api(`appointment/approve/${id}`,'PUT'); closeDialog(); toast('Saved successfully'); await render(); },'Approve & create meeting'); break;
      case 'reject-appointment': confirmAction('Confirm rejection','Reject this appointment request?',async()=>{ await api(`appointment/reject/${id}`,'PUT'); closeDialog(); toast('Saved successfully'); await render(); },'Reject request'); break;
      case 'join': target.disabled = true; await joinMeeting(id); break;
      case 'delete-doctor-profile': confirmAction('Confirm deletion','Delete your doctor profile and its appointments? Your user account will remain. Remote Zoom meetings will not be cancelled.',async()=>{ await api(`doctor/delete/${state.user.id}`,'DELETE'); closeDialog(); toast('Deleted successfully'); await render(); },'Delete permanently',true); break;
      case 'delete-account': confirmAction('Confirm deletion','Delete your account and all its local records? Remote Zoom meetings will not be cancelled.',async()=>{ await api(`user/delete/${state.user.id}`,'DELETE'); signOut(); toast('Deleted successfully'); },'Delete permanently',true); break;
    }
  } catch (error) { if (identity === identityVersion) showError(error); }
  finally { if (target.isConnected && !['drug-page'].includes(action)) target.disabled = false; }
});

document.addEventListener('submit', async event => {
  const form = event.target;
  if (!(form instanceof HTMLFormElement)) return;
  event.preventDefault();
  if (form.dataset.busy) return;
  const submit = form.querySelector('button[type="submit"]'); const errorNode = form.querySelector('.form-error');
  const original = submit?.innerHTML; const wasDisabled = submit?.disabled;
  const identity = identityVersion; const modal = modalVersion;
  form.dataset.busy = 'true'; if (submit) { submit.disabled = true; submit.innerHTML = T('Working…'); } if (errorNode) errorNode.textContent = '';
  try {
    if (form.id === 'auth-form') {
      const data = new FormData(form);
      if (state.authTab === 'signup') {
        await api('user/add','POST',{ name: data.get('name').trim(), email: data.get('email').trim(), passwordHash: data.get('password'), role: data.get('role'), medicalConditions: null });
        if (identity !== identityVersion) return;
        state.authTab = 'login'; toast('You can now sign in.'); renderAuth();
      } else {
        const user = await api('user/login','POST',{ email: data.get('email').trim(), password: data.get('password') });
        if (identity !== identityVersion) return;
        identityVersion++; saveIdentity(user); state.page = user.role === 'DOCTOR' ? 'appointments' : 'today'; await render(); await pollReminders();
      }
    } else if (form.id === 'interaction-form') {
      await runCheck(positiveId(new FormData(form).get('itemId')));
    } else if (form.id === 'doctor-search-form') {
      await searchDoctors(form);
    } else if (form.id === 'profile-form') {
      await saveProfile(form);
    } else if (form.id === 'doctor-profile-form') {
      const data = new FormData(form);
      await api(state.hasDoctorProfile ? `doctor/update/${state.user.id}` : 'doctor/add',state.hasDoctorProfile ? 'PUT' : 'POST',{ userId: state.user.id, specialty: data.get('specialty').trim(), bio: data.get('bio').trim() || null });
      toast('Doctor profile saved'); await render();
    } else if (dialog.contains(form) && modalSubmit) {
      await modalSubmit(form);
    }
  } catch (error) {
    if (identity !== identityVersion) return;
    if (errorNode?.isConnected && (!dialog.contains(form) || modal === modalVersion)) errorNode.textContent = t(error.message);
    else showError(error);
  } finally {
    delete form.dataset.busy;
    if (submit?.isConnected) { submit.disabled = wasDisabled || false; submit.innerHTML = original; }
  }
});
document.addEventListener('change', event => {
  if (event.target.id === 'show-inactive') { state.showInactive = event.target.checked; render(); }
  if (event.target.name === 'status' && dialog.contains(event.target)) {
    const input = dialog.querySelector('[name="takenAt"]'); if (input) { input.disabled = event.target.value !== 'TAKEN'; }
  }
});
dialog.addEventListener('cancel',event=>{event.preventDefault(); closeDialog();});
// Update time-gated buttons without replacing forms or losing typed data.
setInterval(() => {
  const now = new Date();
  document.querySelectorAll('[data-action="join"]').forEach(node => {
    const appointment = state.appointments.find(a=>a.id===positiveId(node.dataset.id));
    if (appointment) node.disabled = appointment.status !== 'APPROVED' || now < saudiDateTime(appointment.startAt) || now > saudiDateTime(appointment.endAt);
  });
},10000);
setInterval(pollReminders,30000);
document.addEventListener('visibilitychange',()=>{if (!document.hidden) pollReminders();});
render().then(pollReminders);
