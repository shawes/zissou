'use strict';

// ---- small helpers --------------------------------------------------------

const $ = (selector) => document.querySelector(selector);

// Builds an element; text goes in as text nodes, never as HTML.
function h(tag, attrs = {}, ...children) {
  const el = document.createElement(tag);
  for (const [key, value] of Object.entries(attrs)) {
    if (value === false || value == null) continue;
    if (key === 'class') el.className = value;
    else if (key.startsWith('on')) el.addEventListener(key.slice(2), value);
    else el.setAttribute(key, value === true ? '' : value);
  }
  for (const child of children.flat()) {
    if (child == null || child === false) continue;
    el.append(child.nodeType ? child : document.createTextNode(String(child)));
  }
  return el;
}

function svg(tag, attrs = {}, ...children) {
  const el = document.createElementNS('http://www.w3.org/2000/svg', tag);
  for (const [key, value] of Object.entries(attrs)) el.setAttribute(key, value);
  children.forEach((c) => el.append(c));
  return el;
}

// Redraws `el` only when `key` differs from last time. Rebuilding buttons and links
// under the mouse drops clicks that straddle the rebuild, so refreshes must not do it
// unless something changed.
function redraw(el, key, build) {
  const serial = typeof key === 'string' ? key : JSON.stringify(key);
  if (el._key === serial) return false;
  el._key = serial;
  build();
  return true;
}

function clear(el, ...children) {
  el.replaceChildren(...children.flat().filter((c) => c != null && c !== false));
}

async function api(method, path, body) {
  const options = { method, headers: {} };
  if (method !== 'GET') options.headers['Content-Type'] = 'application/json';
  if (body !== undefined) options.body = JSON.stringify(body);
  const response = await fetch(path, options);
  const data = await response.json().catch(() => ({}));
  if (!response.ok) {
    const error = new Error(data.error || response.statusText);
    error.data = data;
    throw error;
  }
  return data;
}

const STATUS = { running: 'Running', succeeded: 'Finished', failed: 'Failed', cancelled: 'Cancelled', interrupted: 'Interrupted' };
const formatNumber = (n) => Number(n).toLocaleString();
const formatTime = (ms) => new Date(ms).toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });

function formatDuration(from, to) {
  const seconds = Math.max(0, Math.round((to - from) / 1000));
  if (seconds < 60) return `${seconds} s`;
  const minutes = Math.floor(seconds / 60);
  return minutes < 60 ? `${minutes} min ${seconds % 60} s` : `${Math.floor(minutes / 60)} h ${minutes % 60} min`;
}

// ---- state ----------------------------------------------------------------

const state = {
  configs: [],
  runs: [],
  running: false,
  selected: null, // { type: 'config', name } or { type: 'run', id }
  editor: { name: null, text: '', saved: '', validation: null, message: null, timer: null, refs: {} },
  run: { id: null, detail: null, results: null, map: undefined, mapOptions: { settled: true, sampled: false }, logOffset: 0, refs: {} },
};

// ---- sidebar --------------------------------------------------------------

function renderSidebar() {
  const selected = state.selected;
  const configsList = $('#configs'), runsList = $('#runs');
  redraw(configsList, [state.configs.map((c) => c.name), selected], () => buildConfigList());
  redraw(runsList, [state.runs.map((r) => [r.id, r.status, r.config, r.startedAt]), selected], () => buildRunList());
}

function buildConfigList() {
  const configs = state.configs.map((config) =>
    h('li', {}, h('button', {
      class: 'item', type: 'button',
      'aria-current': state.selected?.type === 'config' && state.selected.name === config.name,
      onclick: () => selectConfig(config.name),
    }, h('span', { class: 'name' }, config.name)))
  );
  clear($('#configs'), configs.length ? configs : h('li', { class: 'empty' }, 'No configuration files found.'));
}

function buildRunList() {
  const runs = state.runs.map((run) =>
    h('li', {}, h('button', {
      class: 'item', type: 'button',
      'aria-current': state.selected?.type === 'run' && state.selected.id === run.id,
      onclick: () => selectRun(run.id),
    },
      h('span', { class: 'name' }, run.config.replace(/\.ya?ml$/, ''), h('span', { class: 'meta' }, ` · ${formatTime(run.startedAt)}`)),
      h('span', { class: `badge ${run.status}` }, STATUS[run.status] || run.status)))
  );
  clear($('#runs'), runs.length ? runs : h('li', { class: 'empty' }, 'No runs yet.'));
}

function renderStatus() {
  const active = state.runs.find((r) => r.status === 'running');
  redraw($('#status'), active ? active.config : '', () => buildStatus(active));
}

function buildStatus(active) {
  clear($('#status'),
    h('span', { class: `dot ${active ? 'running' : ''}` }),
    active ? `Running ${active.config.replace(/\.ya?ml$/, '')}` : 'Idle');
}

// ---- configuration editor -------------------------------------------------

async function selectConfig(name) {
  if (state.selected?.type === 'config' && state.selected.name === name) return;
  if (isDirty() && !confirm('Discard your unsaved changes?')) return;
  try {
    const config = await api('GET', `/api/configs/${encodeURIComponent(name)}`);
    state.selected = { type: 'config', name };
    state.editor = { ...state.editor, name, text: config.text, saved: config.text, validation: null, message: null };
    renderSidebar();
    renderConfig();
    validateNow();
  } catch (e) {
    alert(e.message);
  }
}

const isDirty = () => state.selected?.type === 'config' && state.editor.text !== state.editor.saved;

function renderConfig() {
  const refs = (state.editor.refs = {});
  refs.title = h('h1', {}, state.editor.name);
  refs.dirty = h('span', { class: 'dirty' });
  refs.save = h('button', { class: 'btn', type: 'button', onclick: saveConfig }, 'Save');
  refs.run = h('button', { class: 'btn primary', type: 'button', onclick: runConfig }, 'Run');
  refs.textarea = h('textarea', {
    class: 'editor', spellcheck: 'false', 'aria-label': `Configuration ${state.editor.name}`,
    oninput: (event) => { state.editor.text = event.target.value; refreshEditorControls(); scheduleValidate(); },
  });
  refs.textarea.value = state.editor.text;
  refs.validation = h('div', { 'aria-live': 'polite' });
  refs.message = h('div', { 'aria-live': 'polite' });

  clear($('#main'),
    h('div', { class: 'head' },
      h('div', {}, refs.title, h('div', { class: 'sub' }, 'Edit the YAML, check it below, then run it.')),
      h('div', { class: 'actions' }, refs.dirty, refs.save, refs.run)),
    h('div', { class: 'card' }, refs.textarea, refs.message, refs.validation));
  refreshEditorControls();
  renderValidation();
}

function refreshEditorControls() {
  const { refs, validation } = state.editor;
  if (!refs.save) return;
  const valid = validation ? validation.ok : false;
  refs.dirty.textContent = isDirty() ? 'Unsaved changes' : '';
  refs.save.disabled = !isDirty() || !valid;
  refs.run.disabled = state.running || !valid;
  refs.run.title = state.running ? 'A run is already in progress' : valid ? '' : 'Fix the configuration first';
}

function scheduleValidate() {
  clearTimeout(state.editor.timer);
  state.editor.timer = setTimeout(validateNow, 350);
}

async function validateNow() {
  const text = state.editor.text;
  try {
    const validation = await api('POST', '/api/validate', { text });
    if (text !== state.editor.text) return; // the text moved on while we asked
    state.editor.validation = validation;
    renderValidation();
    refreshEditorControls();
  } catch (e) { /* the connection banner covers this */ }
}

function renderValidation() {
  const { refs, validation } = state.editor;
  if (!refs.validation) return;
  if (!validation) return clear(refs.validation, h('p', { class: 'hint' }, 'Checking…'));
  if (!validation.ok) return clear(refs.validation, h('div', { class: 'notice error' }, validation.error));

  const summary = validation.summary;
  const sites = summary.sites.map((s) => h('tr', {},
    h('td', {}, s.name), h('td', { class: 'num' }, s.patch),
    h('td', { class: 'num' }, `${s.latitude}, ${s.longitude}`),
    h('td', { class: 'num' }, formatNumber(s.larvae)),
    h('td', {}, `${s.releaseStart.slice(0, 10)} to ${s.releaseEnd.slice(0, 10)}`),
    h('td', { class: 'num' }, `every ${s.interval} d`)));
  clear(refs.validation,
    h('div', { class: 'notice ok' }, `Valid. Flow period ${summary.periodStart.slice(0, 10)} to ${summary.periodEnd.slice(0, 10)}, ${summary.timeStep} steps.`),
    validation.warnings.length
      ? h('div', { class: 'notice warn' }, 'Worth a look:', h('ul', {}, validation.warnings.map((w) => h('li', {}, w))))
      : null,
    h('div', { class: 'scroll' }, h('table', {},
      h('thead', {}, h('tr', {}, ['Release site', 'Patch', 'Lat, lon', 'Larvae per release', 'Release period', 'Interval']
        .map((t, i) => h('th', { class: i === 1 || i === 3 || i === 5 ? 'num' : '' }, t)))),
      h('tbody', {}, sites))));
}

async function saveConfig() {
  const { name, text } = state.editor;
  try {
    await api('PUT', `/api/configs/${encodeURIComponent(name)}`, { text });
    state.editor.saved = text;
    clear(state.editor.refs.message, h('div', { class: 'notice ok' }, 'Saved.'));
    setTimeout(() => state.editor.refs.message && clear(state.editor.refs.message), 2000);
  } catch (e) {
    clear(state.editor.refs.message, h('div', { class: 'notice error' }, e.message));
  }
  refreshEditorControls();
}

async function runConfig() {
  const { name, text } = state.editor;
  try {
    const run = await api('POST', '/api/runs', { config: name, text });
    state.running = true;
    await refreshRuns();
    selectRun(run.id, true);
  } catch (e) {
    clear(state.editor.refs.message, h('div', { class: 'notice error' }, e.message));
  }
}

// ---- run view -------------------------------------------------------------

async function selectRun(id, force = false) {
  if (!force && state.selected?.type === 'run' && state.selected.id === id) return;
  if (state.selected?.type === 'config' && isDirty() && !confirm('Discard your unsaved changes?')) return;
  state.selected = { type: 'run', id };
  state.run = { id, detail: null, results: null, map: undefined, mapOptions: { settled: true, sampled: false }, logOffset: 0, refs: {} };
  renderSidebar();
  buildRunPanel();
  await refreshRun();
}

function buildRunPanel() {
  const refs = (state.run.refs = {});
  refs.title = h('span', {});
  refs.badge = h('span', { class: 'badge' });
  refs.sub = h('div', { class: 'sub' });
  refs.actions = h('div', { class: 'actions' });
  refs.head = h('div', { class: 'head' }, h('div', {}, h('h1', {}, refs.title, ' ', refs.badge), refs.sub), refs.actions);
  refs.progress = h('div', { class: 'card' });
  refs.metrics = h('div', { class: 'metrics' });
  refs.chart = h('div', { class: 'card' });
  refs.results = h('div');
  refs.mapCard = h('div', { class: 'card', hidden: true });
  refs.log = h('pre', { class: 'log', tabindex: '0', 'aria-label': 'Run log' });
  clear($('#main'), refs.head, refs.progress, refs.metrics, refs.chart, refs.mapCard, refs.results,
    h('div', { class: 'card' }, h('h3', {}, 'Log'), refs.log));
}

async function refreshRun() {
  const id = state.run.id;
  if (!id) return;
  try {
    const detail = await api('GET', `/api/runs/${id}`);
    const log = await api('GET', `/api/runs/${id}/log?from=${state.run.logOffset}`);
    if (id !== state.run.id) return; // the user moved to another run meanwhile
    state.run.detail = detail;
    if (log.text) {
      const box = state.run.refs.log;
      const stuck = box.scrollTop + box.clientHeight >= box.scrollHeight - 24;
      box.append(log.text);
      if (stuck) box.scrollTop = box.scrollHeight;
    }
    state.run.logOffset = log.next;
    if (detail.run.status === 'succeeded' && !state.run.results) {
      state.run.results = await api('GET', `/api/runs/${id}/results`).catch(() => null);
    }
    if (detail.run.status === 'succeeded' && state.run.map === undefined) {
      state.run.map = 'loading';
      api('GET', `/api/runs/${id}/map?tracks=400`)
        .then((map) => { if (id === state.run.id) { state.run.map = map; renderRun(); } })
        .catch(() => { if (id === state.run.id) { state.run.map = null; renderRun(); } });
    }
    renderRun();
  } catch (e) { /* the connection banner covers this */ }
}

function renderRun() {
  const { refs, detail, results } = state.run;
  if (!detail) return;
  const run = detail.run;
  const last = detail.log.days[detail.log.days.length - 1];

  // text is updated in place; the buttons are only replaced when the run changes state
  refs.title.textContent = run.config.replace(/\.ya?ml$/, '');
  refs.badge.className = `badge ${run.status}`;
  refs.badge.textContent = STATUS[run.status] || run.status;
  refs.sub.textContent = `Started ${formatTime(run.startedAt)} · ${formatDuration(run.startedAt, run.endedAt || Date.now())}`;
  redraw(refs.actions, run.status, () => clear(refs.actions,
    run.status === 'running'
      ? h('button', { class: 'btn danger', type: 'button', onclick: cancelRun }, 'Cancel run')
      : h('button', { class: 'btn danger', type: 'button', onclick: deleteRun }, 'Delete run')));

  const percent = run.status === 'succeeded' ? 100 : Math.round((detail.fraction || 0) * 100);
  const waiting = run.status === 'running' ? detail.log.waiting : null;
  redraw(refs.progress, [run.status, run.message, percent, last && last.date, detail.log.warnings, waiting && waiting.seconds, last && last.flowSeconds], () => clear(refs.progress,
    run.status === 'running' || run.status === 'succeeded'
      ? [h('div', { class: 'progress-label' },
          h('span', {}, last ? `Simulated to ${last.date}` : 'Starting…'),
          h('span', {}, run.status === 'running' ? `${percent}% of the flow period` : 'Complete')),
         h('div', { class: 'progress', role: 'progressbar', 'aria-valuenow': percent, 'aria-valuemin': 0, 'aria-valuemax': 100 },
           h('div', { style: `width:${percent}%` })),
         // what the last day took, and how much of that was the run waiting for its data rather than working
         run.status === 'running' && last
           ? h('p', { class: 'hint' }, last.flowSeconds > 0
               ? `Last day: ${last.seconds} s, of which ${last.flowSeconds} s waiting for flow data.`
               : `Last day: ${last.seconds} s, none of it waiting for flow data.`)
           : null,
         run.status === 'running' ? h('p', { class: 'hint' }, 'A run can finish before the end of the flow period, once every larva has settled or died.') : null]
      : null,
    // the run is stuck on the network, not slow: it is waiting for a day of the ocean model's flow from the server
    waiting
      ? h('div', { class: 'notice wait' },
          h('strong', {}, `Waiting for flow data for ${waiting.date}: ${waiting.seconds} s so far.`),
          h('div', {}, `The simulation is not slow: it is waiting for ${waiting.source} to send the data. A request that fails is tried again by itself.`))
      : null,
    run.message ? h('div', { class: 'notice error' }, run.message) : null,
    detail.log.warnings.length ? h('div', { class: 'notice warn' }, h('ul', {}, detail.log.warnings.map((w) => h('li', {}, w)))) : null,
    run.status === 'cancelled' ? h('p', { class: 'hint' }, 'This run was cancelled.') : null,
    run.status === 'interrupted' ? h('p', { class: 'hint' }, 'The interface was closed while this run was going, so it was stopped.') : null));
  refs.progress.hidden = !refs.progress.childNodes.length;

  const peak = detail.log.days.reduce((m, d) => Math.max(m, d.larvae), 0);
  const total = detail.log.days.reduce((m, d) => m + d.seconds, 0);
  const waited = detail.log.days.reduce((m, d) => m + d.flowSeconds, 0);
  redraw(refs.metrics, [results && results.settled, last, peak, detail.log.days.length, waited, total], () => clear(refs.metrics,
    metric(results ? formatNumber(results.settled) : '–', 'larvae settled'),
    metric(last ? formatNumber(last.larvae) : '–', 'larvae in the water'),
    metric(peak ? formatNumber(peak) : '–', 'peak in the water'),
    metric(formatNumber(detail.log.days.length), 'days simulated'),
    // how much of the time in the days was spent waiting for the ocean model's data, which is the network or the disk, not the simulation
    detail.log.days.length ? metric(`${formatNumber(waited)} s`, total > 0 ? `waiting for flow data (${Math.round(100 * waited / total)}% of ${formatNumber(total)} s)` : 'waiting for flow data') : null));

  redraw(refs.chart, detail.log.days.length, () => clear(refs.chart, h('h3', {}, 'Larvae in the water'),
    detail.log.days.length > 1 ? lineChart(detail.log.days) : h('p', { class: 'hint' }, 'Waiting for the first days to complete…')));
  renderMap();
  redraw(refs.results, results ? results.settled + ':' + results.files.length : '', () => clear(refs.results, results ? resultsView(results) : null));
}

// ---- map --------------------------------------------------------------------

function renderMap() {
  const { refs, map, mapOptions, detail } = state.run;
  const wanted = detail.run.status === 'succeeded' && map !== undefined && map !== null;
  refs.mapCard.hidden = !wanted;
  if (!wanted) return;
  if (!refs.mapHost) {
    refs.mapHost = h('div');
    refs.mapControls = h('div', { class: 'map-controls' });
    refs.mapLegend = h('div', { class: 'legend' });
    clear(refs.mapCard, h('h3', {}, 'Where larvae went'), refs.mapControls, refs.mapHost, refs.mapLegend);
  }
  redraw(refs.mapControls, map === 'loading' ? 'loading' : [map.settled.total, map.sampled.total], () => {
    if (map === 'loading') return clear(refs.mapControls, h('span', { class: 'hint' }, 'Loading the map…'));
    const option = (key, label, set) => h('label', {},
      h('input', { type: 'checkbox', checked: mapOptions[key] ? true : null, disabled: set.total === 0 ? true : null,
        onchange: (event) => { mapOptions[key] = event.target.checked; drawMap(); } }),
      `${label} (${set.tracks.length === set.total ? set.total : `${set.tracks.length} of ${set.total}`})`);
    clear(refs.mapControls, option('settled', 'Larvae that settled', map.settled), option('sampled', 'A sample of all larvae', map.sampled));
  });
  if (map !== 'loading') redraw(refs.mapHost, 'drawn', drawMap);
}

function drawMap() {
  const { refs, map, mapOptions } = state.run;
  if (!refs.mapHost || !map || map === 'loading') return;
  clear(refs.mapHost,
    ...map.notes.map((n) => h('div', { class: 'notice warn' }, n)),
    mapSvg(map, mapOptions));
  clear(refs.mapLegend, map.sites.map((site, i) => h('span', {},
    h('span', { class: 'swatch', style: `background: var(--c${i % 8})` }), site.name.includes(String(site.patch)) ? site.name : `${site.name} (patch ${site.patch})`)));
}

function niceStep(span) {
  const steps = [0.005, 0.01, 0.02, 0.05, 0.1, 0.2, 0.5, 1, 2, 5, 10, 20];
  return steps.find((s) => s >= span / 5) || 20;
}

function mapSvg(map, options) {
  const [west, south, east, north] = map.bounds;
  const k = Math.cos((((south + north) / 2) * Math.PI) / 180);
  const pixelsPerDegree = Math.min(900 / (east - west), 1100 / ((north - south) / k));
  const width = (east - west) * pixelsPerDegree;
  const height = ((north - south) / k) * pixelsPerDegree;
  const x = (lon) => ((lon - west) * pixelsPerDegree).toFixed(1);
  const y = (lat) => (((north - lat) / k) * pixelsPerDegree).toFixed(1);
  const path = (points) => points.map((p, i) => `${i ? 'L' : 'M'}${x(p[0])},${y(p[1])}`).join('');

  const grid = [];
  const lonStep = niceStep(east - west), latStep = niceStep(north - south);
  for (let lon = Math.ceil(west / lonStep) * lonStep; lon < east; lon += lonStep) {
    grid.push(svg('line', { class: 'grid', x1: x(lon), y1: 0, x2: x(lon), y2: height }), textNode(Number(x(lon)) + 4, height - 6, `${lon.toFixed(2)}°`, 'start'));
  }
  for (let lat = Math.ceil(south / latStep) * latStep; lat < north; lat += latStep) {
    grid.push(svg('line', { class: 'grid', x1: 0, y1: y(lat), x2: width, y2: y(lat) }), textNode(4, Number(y(lat)) - 4, `${lat.toFixed(2)}°`, 'start'));
  }

  const reefs = map.reefs.flatMap((reef) => {
    const outline = svg('path', { class: `reef ${reef.habitat === 'Reef' ? '' : 'other'}`, d: reef.rings.map((ring) => path(ring) + 'Z').join('') },
      svg('title', {}, document.createTextNode(`Patch ${reef.patch}${reef.habitat ? ` (${reef.habitat})` : ''}`)));
    const points = reef.rings[0];
    const centre = [(Math.min(...points.map((p) => p[0])) + Math.max(...points.map((p) => p[0]))) / 2, Math.min(...points.map((p) => p[1]))];
    return [outline, textNode(Number(x(centre[0])), Number(y(centre[1])) + 13, String(reef.patch), 'middle')];
  });

  const tracks = (set, extra) => set.tracks.map((t) =>
    svg('path', { class: `track ${extra}`, d: path(t.points), style: `stroke: var(--c${t.site < 0 ? 7 : t.site % 8})` }));

  const sites = map.sites.flatMap((site, i) => [
    svg('circle', { class: 'site', cx: x(site.longitude), cy: y(site.latitude), r: 5, style: `fill: var(--c${i % 8})` },
      svg('title', {}, document.createTextNode(`${site.name}, patch ${site.patch}`))),
    (() => { const label = textNode(Number(x(site.longitude)) + 9, Number(y(site.latitude)) + 4, site.name, 'start'); label.setAttribute('class', 'site-label'); return label; })(),
  ]);

  return svg('svg', { class: 'map', viewBox: `0 0 ${width.toFixed(0)} ${height.toFixed(0)}`, role: 'img', 'aria-label': 'Map of the reefs, release sites and larva paths' },
    ...grid, ...reefs, ...(options.sampled ? tracks(map.sampled, 'sampled') : []), ...(options.settled ? tracks(map.settled, '') : []), ...sites);
}

const metric = (value, label) => h('div', { class: 'metric' }, h('div', { class: 'value' }, value), h('div', { class: 'label' }, label));

function lineChart(days) {
  const width = 640, height = 170, left = 46, bottom = 22, top = 8, right = 8;
  const max = Math.max(...days.map((d) => d.larvae), 1);
  const x = (i) => left + (i / (days.length - 1)) * (width - left - right);
  const y = (v) => top + (1 - v / max) * (height - top - bottom);
  const points = days.map((d, i) => `${x(i).toFixed(1)},${y(d.larvae).toFixed(1)}`);
  return svg('svg', { class: 'chart', viewBox: `0 0 ${width} ${height}`, role: 'img', 'aria-label': 'Larvae in the water, by day' },
    svg('line', { class: 'axis', x1: left, y1: height - bottom, x2: width - right, y2: height - bottom }),
    svg('line', { class: 'axis', x1: left, y1: top, x2: left, y2: height - bottom }),
    svg('polygon', { class: 'area', points: `${x(0)},${height - bottom} ${points.join(' ')} ${x(days.length - 1)},${height - bottom}` }),
    svg('polyline', { class: 'line', points: points.join(' ') }),
    textNode(left - 6, top + 9, formatNumber(max), 'end'),
    textNode(left - 6, height - bottom, '0', 'end'),
    textNode(left, height - 6, days[0].date, 'start'),
    textNode(width - right, height - 6, days[days.length - 1].date, 'end'));
}

function textNode(x, y, text, anchor) {
  const node = svg('text', { x, y, 'text-anchor': anchor });
  node.textContent = text;
  return node;
}

function resultsView(results) {
  const { matrix } = results;
  const peak = Math.max(1, ...matrix.counts.flat());
  const matrixTable = matrix.sources.length === 0
    ? h('p', { class: 'hint' }, 'No larvae settled in this run.')
    : h('div', { class: 'scroll' }, h('table', {},
        h('thead', {}, h('tr', {}, h('th', {}, 'From \\ to'), matrix.sinks.map((s) => h('th', { class: 'num' }, s)), h('th', { class: 'num' }, 'Total'))),
        h('tbody', {}, matrix.sources.map((from, i) => h('tr', {},
          h('th', {}, from),
          matrix.counts[i].map((count, j) => h('td', {
            class: 'num',
            style: count ? `background: color-mix(in srgb, var(--accent) ${Math.round((count / peak) * 70) + 8}%, transparent)` : null,
            title: `${count} larvae from ${from} settled on ${matrix.sinks[j]}`,
          }, count || '')),
          h('td', { class: 'num' }, matrix.counts[i].reduce((a, b) => a + b, 0)))))));

  const sites = results.sites.length === 0 ? null : h('div', { class: 'card' },
    h('h3', {}, 'Settlement by release site'),
    h('div', { class: 'scroll' }, h('table', {},
      h('thead', {}, h('tr', {}, ['Patch', 'Site', 'Settled', 'Settled where released'].map((t, i) => h('th', { class: i > 1 ? 'num' : '' }, t)))),
      h('tbody', {}, results.sites.map((s) => h('tr', {},
        h('td', {}, s.patch), h('td', {}, s.name), h('td', { class: 'num' }, formatNumber(s.settled)),
        h('td', { class: 'num' }, `${formatNumber(s.returnedHome)} (${Math.round((s.returnedHome / s.settled) * 100)}%)`)))))));

  return h('div', {},
    sites,
    h('div', { class: 'card' }, h('h3', {}, 'Connectivity matrix'),
      h('p', { class: 'hint' }, 'Rows are the patch larvae were released from, columns the patch they settled on.'), matrixTable),
    results.kernel ? kernelView(results.kernel) : null,
    h('div', { class: 'card' }, h('h3', {}, 'Files'),
      h('div', { class: 'files' }, results.files.map((f) =>
        h('a', { href: `/api/runs/${state.run.id}/files/${encodeURIComponent(f.name)}`, download: f.name }, `${f.name} (${formatBytes(f.bytes)})`)))));
}

function kernelView(kernel) {
  const peak = Math.max(1, ...kernel.histogram.map((b) => b.count));
  return h('div', { class: 'card' },
    h('h3', {}, 'How far larvae travelled'),
    h('p', { class: 'hint' },
      `Straight-line distance from release to settlement: average ${kernel.meanCrowFliesKm.toFixed(1)} km, median ${kernel.medianCrowFliesKm.toFixed(1)} km, furthest ${kernel.maxCrowFliesKm.toFixed(1)} km. ` +
      `Distance along the path averaged ${kernel.meanDispersalKm.toFixed(1)} km, over ${kernel.meanAgeDays.toFixed(1)} days.`),
    h('div', { class: 'hist', role: 'img', 'aria-label': 'Histogram of dispersal distance' },
      kernel.histogram.map((b) => h('div', {
        class: 'bar', style: `height:${(b.count / peak) * 100}%`,
        title: `${b.fromKm.toFixed(0)} to ${b.toKm.toFixed(0)} km: ${b.count} larvae`,
      }))),
    h('div', { class: 'hist-axis' }, h('span', {}, '0 km'), h('span', {}, `${kernel.maxCrowFliesKm.toFixed(0)} km`)));
}

const formatBytes = (n) => (n < 1024 ? `${n} B` : n < 1048576 ? `${(n / 1024).toFixed(1)} KB` : `${(n / 1048576).toFixed(1)} MB`);

async function cancelRun() {
  try { await api('POST', `/api/runs/${state.run.id}/cancel`); await refreshRuns(); await refreshRun(); }
  catch (e) { alert(e.message); }
}

async function deleteRun() {
  if (!confirm('Delete this run and all of its results? This cannot be undone.')) return;
  try {
    await api('DELETE', `/api/runs/${state.run.id}`);
    state.selected = null;
    state.run = { id: null, detail: null, results: null, map: undefined, mapOptions: { settled: true, sampled: false }, logOffset: 0, refs: {} };
    await refreshRuns();
    if (state.configs.length) { state.editor.name = null; selectConfig(state.configs[0].name); } else showWelcome();
  } catch (e) { alert(e.message); }
}

function showWelcome() {
  clear($('#main'), h('div', { class: 'welcome' },
    h('h1', {}, 'Zissou'),
    h('p', {}, 'Pick a configuration on the left to edit and run it, or open an earlier run to see what it produced.')));
}

// ---- polling --------------------------------------------------------------

async function refreshRuns() {
  state.runs = await api('GET', '/api/runs');
  state.running = state.runs.some((r) => r.status === 'running');
  renderSidebar();
  renderStatus();
  refreshEditorControls();
}

async function tick() {
  try {
    await refreshRuns();
    if (state.selected?.type === 'run') await refreshRun();
    $('#offline').hidden = true;
  } catch (e) {
    $('#offline').hidden = false;
  }
  setTimeout(tick, state.running ? 1000 : 4000);
}

document.addEventListener('keydown', (event) => {
  if ((event.metaKey || event.ctrlKey) && event.key === 's' && state.selected?.type === 'config') {
    event.preventDefault();
    if (!state.editor.refs.save.disabled) saveConfig();
  }
});

(async function start() {
  try {
    state.configs = await api('GET', '/api/configs');
    await refreshRuns();
    const active = state.runs.find((r) => r.status === 'running');
    if (active) await selectRun(active.id);
    else if (state.configs.length) await selectConfig(state.configs[0].name);
    else showWelcome();
  } catch (e) {
    $('#offline').hidden = false;
  }
  tick();
})();
