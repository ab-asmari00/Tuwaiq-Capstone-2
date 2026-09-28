// Same-origin requests: Spring Boot serves both the interface and the REST API.
export class ApiError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}
export async function api(path, method = 'GET', body) {
  let response;
  try {
    response = await fetch('/api/v1/' + path, {
      method,
      headers: { Accept: 'application/json', ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) },
      ...(body === undefined ? {} : { body: JSON.stringify(body) }),
      cache: 'no-store'
    });
  } catch {
    throw new ApiError('Cannot reach the server. Check that Spring Boot is running.', 0);
  }
  const text = await response.text();
  let data;
  try { data = text ? JSON.parse(text) : null; } catch {
    throw new ApiError('The server returned an unexpected response.', response.status);
  }
  if (!response.ok) throw new ApiError(data?.message || 'The request failed. Please try again.', response.status);
  return data;
}
// A few existing list controllers return a message object when their list is empty.
export const list = value => Array.isArray(value) ? value : [];
export function escapeHtml(value = '') {
  return String(value ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
export const positiveId = value => /^\d+$/.test(String(value)) && Number(value) > 0 ? Number(value) : null;
export function safeHttps(value) {
  try { const url = new URL(value); return url.protocol === 'https:' ? url.href : null; } catch { return null; }
}
export function saudiDateTime(value) {
  // Backend LocalDateTime values represent Riyadh wall time, regardless of browser timezone.
  return new Date(value + '+03:00');
}
export function saudiToday() {
  const parts = new Intl.DateTimeFormat('en-CA', { timeZone: 'Asia/Riyadh', year: 'numeric', month: '2-digit', day: '2-digit' }).formatToParts(new Date());
  const get = type => parts.find(p => p.type === type).value;
  return `${get('year')}-${get('month')}-${get('day')}`;
}
export function saudiNow() {
  return new Date(Date.now() + 3 * 60 * 60 * 1000).toISOString().slice(0,19);
}
