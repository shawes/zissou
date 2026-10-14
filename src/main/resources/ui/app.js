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
  maxConcurrent: 1,
  runningCount: 0,
  selected: null, // { type: 'config', name } or { type: 'run', id }
  editor: { name: null, text: '', saved: '', validation: null, message: null, timer: null, refs: {} },
  run: { id: null, detail: null, results: null, map: undefined, mapOptions: { settled: true, sampled: false }, logOffset: 0, refs: {} },
};

// ---- sidebar --------------------------------------------------------------

// what the two lists are narrowed to, and whether the older runs are shown
const sidebarFilter = { configs: '', runs: '', allRuns: false };
const RECENT_RUNS = 12;

const matches = (text, filter) => filter.split(/\s+/).filter(Boolean).every((word) => text.toLowerCase().includes(word.toLowerCase()));

function renderSidebar() {
  const selected = state.selected;
  const configsList = $('#configs'), runsList = $('#runs');
  redraw(configsList, [state.configs.map((c) => c.name), selected, sidebarFilter.configs], () => buildConfigList());
  redraw(runsList, [state.runs.map((r) => [r.id, r.status, r.config, r.startedAt]), selected, sidebarFilter.runs, sidebarFilter.allRuns], () => buildRunList());
}

$('#config-filter').addEventListener('input', (event) => { sidebarFilter.configs = event.target.value; renderSidebar(); });
$('#run-filter').addEventListener('input', (event) => { sidebarFilter.runs = event.target.value; renderSidebar(); });

// ---- a new configuration ---------------------------------------------------

const CONFIG_NAME = /^[A-Za-z0-9][A-Za-z0-9._-]*$/;

function newConfigDialog() {
  if (isDirty() && !confirm('Discard your unsaved changes?')) return;
  const name = h('input', { type: 'text', id: 'new-name', spellcheck: 'false', autocomplete: 'off', placeholder: 'my-experiment' });
  const from = h('select', { id: 'new-from' }, h('option', { value: '' }, 'A blank configuration'),
    state.configs.map((c) => h('option', { value: c.name }, `A copy of ${c.name}`)));
  const error = h('div', { class: 'field-error', role: 'alert' });
  const create = h('button', { class: 'btn primary', type: 'submit' }, 'Create');
  const dialog = h('dialog', { class: 'picker small', 'aria-label': 'New configuration' },
    h('form', { method: 'dialog', onsubmit: (event) => { event.preventDefault(); submit(); } },
      h('h3', {}, 'New configuration'),
      h('div', { class: 'field' }, h('label', { for: 'new-name' }, 'Name', h('span', { class: 'unit' }, ' (.yaml is added)')), name, error),
      h('div', { class: 'field' }, h('label', { for: 'new-from' }, 'Start from'), from),
      h('p', { class: 'hint' }, 'A copy gets its own output folder and log, named after the new configuration.'),
      h('div', { class: 'picker-foot' }, h('span', {}),
        h('div', { class: 'actions' }, h('button', { class: 'btn', type: 'button', onclick: () => dialog.close() }, 'Cancel'), create))));
  dialog.addEventListener('close', () => dialog.remove());
  document.body.append(dialog);
  dialog.showModal();
  name.focus();

  async function submit() {
    const stem = name.value.trim().replace(/\.ya?ml$/, '');
    const file = `${stem}.yaml`;
    if (!CONFIG_NAME.test(stem)) { error.textContent = 'Use letters, digits, ".", "-" and "_", starting with a letter or digit.'; return; }
    if (state.configs.some((c) => c.name === file || c.name === `${stem}.yml`)) { error.textContent = `${file} already exists.`; return; }
    create.disabled = true;
    try {
      let text = ConfigForm.TEMPLATE;
      const source = from.value;
      if (source) text = (await api('GET', `/api/configs/${encodeURIComponent(source)}`)).text;
      text = ZissouEditor.edit(text, (doc) => renameOutputs(doc, source.replace(/\.ya?ml$/, ''), stem));
      await api('PUT', `/api/configs/${encodeURIComponent(file)}`, { text });
      dialog.close();
      state.configs = await api('GET', '/api/configs');
      state.editor.name = null;
      state.selected = null;
      await selectConfig(file);
    } catch (e) {
      error.textContent = e.data?.validation?.error || e.message;
      create.disabled = false;
    }
  }
}

// a copy would write over the source's results, so what names the source in the output
// settings now names the copy; the blank template names itself "new"
function renameOutputs(doc, oldStem, stem) {
  const old = oldStem || 'new';
  for (const key of ['saveOutputFilePath', 'logFile', 'prefix']) {
    const value = doc.getIn(['output', key]);
    // whole names only: copying "a" must not change the "a" in "data"
    const named = new RegExp(`(^|[/._-])${old.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}(?=$|[/._-])`, 'g');
    if (typeof value === 'string' && named.test(value)) doc.setIn(['output', key], value.replace(named, `$1${stem}`));
  }
}

function buildConfigList() {
  const shown = state.configs.filter((config) => matches(config.name, sidebarFilter.configs));
  const configs = shown.map((config) =>
    h('li', {}, h('button', {
      class: 'item', type: 'button', title: config.name,
      'aria-current': state.selected?.type === 'config' && state.selected.name === config.name,
      onclick: () => selectConfig(config.name),
    }, h('span', { class: 'name' }, config.name)))
  );
  clear($('#configs'), configs.length ? configs
    : h('li', { class: 'empty' }, state.configs.length ? 'No configuration matches.' : 'No configuration files found.'));
}

// The day a run started, as a heading for the runs of that day
function dayHeading(ms) {
  const day = new Date(ms), today = new Date();
  const same = (a, b) => a.toDateString() === b.toDateString();
  const yesterday = new Date(today); yesterday.setDate(today.getDate() - 1);
  return same(day, today) ? 'Today' : same(day, yesterday) ? 'Yesterday'
    : day.toLocaleDateString([], { weekday: 'short', month: 'short', day: 'numeric' });
}

function buildRunList() {
  // the runs that are running first, then the rest from the newest, under the day they started; only the latest few
  // unless the list is narrowed or the rest is asked for
  const found = state.runs.filter((run) => matches(`${run.config} ${STATUS[run.status] || run.status}`, sidebarFilter.runs));
  const running = found.filter((run) => run.status === 'running');
  const rest = found.filter((run) => run.status !== 'running').sort((a, b) => b.startedAt - a.startedAt);
  const narrowed = sidebarFilter.runs.trim() !== '';
  const visible = narrowed || sidebarFilter.allRuns ? rest : rest.slice(0, RECENT_RUNS);

  const row = (run) => h('li', {}, h('button', {
    class: 'item two-line', type: 'button', title: run.config,
    'aria-current': state.selected?.type === 'run' && state.selected.id === run.id,
    onclick: () => selectRun(run.id),
  },
    h('span', { class: 'name' }, run.config.replace(/\.ya?ml$/, ''),
      h('span', { class: 'meta block' }, new Date(run.startedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }))),
    h('span', { class: `badge ${run.status}` }, STATUS[run.status] || run.status)));

  const items = [];
  if (running.length) items.push(h('li', { class: 'group' }, 'Running'), ...running.map(row));
  let heading = null;
  visible.forEach((run) => {
    const day = dayHeading(run.startedAt);
    if (day !== heading) { items.push(h('li', { class: 'group' }, day)); heading = day; }
    items.push(row(run));
  });
  if (!narrowed && rest.length > RECENT_RUNS) {
    items.push(h('li', {}, h('button', { class: 'item more', type: 'button', onclick: () => { sidebarFilter.allRuns = !sidebarFilter.allRuns; renderSidebar(); } },
      sidebarFilter.allRuns ? 'Show fewer' : `Show ${rest.length - RECENT_RUNS} older runs`)));
  }
  $('#run-count').textContent = state.runs.length ? (narrowed ? `${found.length} of ${state.runs.length}` : `${state.runs.length}`) : '';
  clear($('#runs'), items.length ? items : h('li', { class: 'empty' }, state.runs.length ? 'No run matches.' : 'No runs yet.'));
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

// Opens the map to choose a site's position, a new site, or a region of the flow.
// `target` is the path of the site, or of the bounds, in the configuration.
function pickOnMap(mode, target) {
  const form = state.editor.refs.form;
  const data = form.data() || {};
  const sites = (data.spawn?.spawningLocation ?? []).map((s, i) => ({ index: i, name: s.name, longitude: s.site?.longitude, latitude: s.site?.latitude }))
    .filter((s) => Number.isFinite(s.longitude) && Number.isFinite(s.latitude));
  const box = (b) => b && [b.west, b.south, b.east, b.north].every(Number.isFinite) ? [b.west, b.south, b.east, b.north] : null;
  const main = box(data.flow?.bounds);
  const nested = (data.flow?.nested ?? []).map((n) => box(n.bounds));
  const set = (doc, path, value) => doc.setIn(path, value);

  if (mode === 'region') {
    const own = target.includes('nested') ? nested[target[target.indexOf('nested') + 1]] : main;
    const others = target.includes('nested') ? [main, ...nested.filter((_, i) => i !== target[target.indexOf('nested') + 1])] : nested;
    MapPicker.open({
      mode: 'region', title: 'Choose the area', region: own, sites, regions: others.filter(Boolean),
      view: MapPicker.fit([own, main, ...sites.map((s) => [s.longitude, s.latitude, s.longitude, s.latitude])], 0.3),
      onChoose: (r) => form.change((doc) => ['south', 'north', 'west', 'east'].forEach((k) => set(doc, [...target, k], r[k]))),
    });
  } else if (mode === 'site') {
    const index = target[2];
    const current = sites.find((s) => s.index === index);
    MapPicker.open({
      mode: 'point', title: `Place ${data.spawn.spawningLocation[index]?.name ?? 'the site'}`,
      point: current ? [current.longitude, current.latitude] : null, sites: sites.filter((s) => s.index !== index), regions: [main, ...nested].filter(Boolean),
      view: MapPicker.fit([main, current && [current.longitude, current.latitude, current.longitude, current.latitude]], 0.1),
      onChoose: (p) => form.change((doc) => { set(doc, [...target, 'site', 'latitude'], p.latitude); set(doc, [...target, 'site', 'longitude'], p.longitude); }),
    });
  } else {
    const template = data.spawn?.spawningLocation?.at(-1);
    MapPicker.open({
      mode: 'point', title: 'Add a release site', sites, regions: [main, ...nested].filter(Boolean),
      view: MapPicker.fit([main, ...sites.map((s) => [s.longitude, s.latitude, s.longitude, s.latitude])], 0.15),
      onChoose: (p) => form.change((doc) => {
        const site = template ? structuredClone(template) : { patchNumber: 0, numberOfLarvae: 1000, interval: 7, site: { depth: 5 }, releasePeriod: { start: data.flow?.period?.start ?? '2020-01-01', end: data.flow?.period?.end ?? '2020-01-31' } };
        site.name = `Site ${sites.length + 1}`;
        site.site = { ...site.site, latitude: p.latitude, longitude: p.longitude };
        if (!doc.hasIn(['spawn', 'spawningLocation'])) doc.setIn(['spawn', 'spawningLocation'], doc.createNode([]));
        doc.addIn(['spawn', 'spawningLocation'], doc.createNode(site));
      }),
    });
  }
}

const viewPreference = {
  get() { try { return localStorage.getItem('zissou.configView') === 'yaml' ? 'yaml' : 'form'; } catch (e) { return 'form'; } },
  set(view) { try { localStorage.setItem('zissou.configView', view); } catch (e) { /* private window */ } },
};

// the form and the code editor each hold a copy of the text, and the one that is not on show
// is brought up to date when its tab is opened
function teardownEditor() {
  const { refs } = state.editor;
  if (refs && refs.codeEditor) refs.codeEditor.destroy();
  if (state.editor.refs) state.editor.refs = {};
}

function renderConfig() {
  teardownEditor();
  const refs = (state.editor.refs = {});
  state.editor.view = viewPreference.get();
  refs.title = h('h1', {}, state.editor.name);
  refs.dirty = h('span', { class: 'dirty' });
  refs.save = h('button', { class: 'btn', type: 'button', onclick: saveConfig }, 'Save');
  refs.run = h('button', { class: 'btn primary', type: 'button', onclick: runConfig }, 'Run');
  refs.validation = h('div', { 'aria-live': 'polite' });
  refs.message = h('div', { 'aria-live': 'polite' });
  refs.formPane = h('div', { class: 'form-pane', role: 'tabpanel', id: 'pane-form' });
  refs.yamlPane = h('div', { class: 'yaml-pane', role: 'tabpanel', id: 'pane-yaml' });
  refs.tabs = ['form', 'yaml'].map((view) => h('button', {
    type: 'button', role: 'tab', class: 'tab', 'data-view': view, 'aria-controls': `pane-${view}`,
    onclick: () => showEditorView(view),
  }, view === 'form' ? 'Form' : 'YAML'));

  clear($('#main'),
    h('div', { class: 'head' },
      h('div', {}, refs.title, h('div', { class: 'sub' }, 'Edit the settings in the form or the YAML; both change the same file.')),
      h('div', { class: 'actions' }, refs.dirty, refs.save, refs.run)),
    h('div', { class: 'tabs', role: 'tablist', 'aria-label': 'Editing view' }, refs.tabs),
    refs.validation,
    refs.message,
    refs.formPane, refs.yamlPane);
  showEditorView(state.editor.view, false);
  refreshEditorControls();
  renderValidation();
}

function showEditorView(view, remember = true) {
  const { refs } = state.editor;
  state.editor.view = view;
  if (remember) viewPreference.set(view);
  refs.tabs.forEach((tab) => tab.setAttribute('aria-selected', String(tab.dataset.view === view)));
  refs.formPane.hidden = view !== 'form';
  refs.yamlPane.hidden = view !== 'yaml';

  if (view === 'form') {
    if (!refs.form) {
      refs.form = ConfigForm.create(refs.formPane, { text: state.editor.text, onText: onTextFromForm, pick: pickOnMap });
      refs.formStale = false;
    } else if (refs.formStale) {
      refs.form.setText(state.editor.text);
      refs.formStale = false;
    }
    refs.form.setProblems(currentProblems());
  } else {
    if (!refs.codeEditor) {
      refs.codeEditor = ZissouEditor.createEditor(refs.yamlPane, { text: state.editor.text, onChange: onTextFromCode });
    } else {
      refs.codeEditor.setText(state.editor.text);
    }
    refs.codeEditor.setProblems(currentProblems());
  }
}

function onTextFromForm(text) {
  state.editor.text = text;
  state.editor.refs.codeEditor?.setText(text);
  textChanged();
}

function onTextFromCode(text) {
  state.editor.text = text;
  state.editor.refs.formStale = true;
  textChanged();
}

function textChanged() {
  refreshEditorControls();
  scheduleValidate();
}

const currentProblems = () => state.editor.validation?.problems ?? [];

const isDirty = () => state.selected?.type === 'config' && state.editor.text !== state.editor.saved;

function refreshEditorControls() {
  const { refs, validation } = state.editor;
  if (!refs || !refs.save) return;
  const valid = validation ? validation.ok : false;
  refs.dirty.textContent = isDirty() ? 'Unsaved changes' : '';
  refs.save.disabled = !isDirty() || !valid;
  const full = state.runningCount >= state.maxConcurrent;
  refs.run.disabled = full || !valid;
  refs.run.title = full
    ? (state.maxConcurrent === 1 ? 'A run is already in progress' : `${state.maxConcurrent} runs are already in progress`)
    : valid ? '' : 'Fix the configuration first';
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
    const { refs } = state.editor;
    refs.form?.setProblems(validation.problems);
    refs.codeEditor?.setProblems(validation.problems);
  } catch (e) { /* the connection banner covers this */ }
}

// where a problem is in the text, for jumping to it from the YAML view
function offsetOf(problem) {
  const text = state.editor.text;
  if (problem.line) {
    const lines = text.split('\n');
    const line = Math.min(problem.line, lines.length) - 1;
    return lines.slice(0, line).reduce((n, l) => n + l.length + 1, 0) + Math.max((problem.column || 1) - 1, 0);
  }
  return ZissouEditor.locate(text, problem.path)?.from ?? 0;
}

function goToProblem(problem) {
  const { refs } = state.editor;
  if (state.editor.view === 'yaml') refs.codeEditor.reveal(offsetOf(problem));
  else if (problem.path) refs.form.focusPath(problem.path);
  else showEditorView('yaml'), refs.codeEditor.reveal(offsetOf(problem));
}

function renderValidation() {
  const { refs, validation } = state.editor;
  if (!refs.validation) return;
  if (!validation) return clear(refs.validation, h('p', { class: 'hint' }, 'Checking…'));
  if (!validation.ok) {
    const problems = validation.problems?.length ? validation.problems : [{ message: validation.error }];
    return clear(refs.validation, h('div', { class: 'notice error' },
      problems.length === 1 ? 'There is a problem with this configuration:' : `There are ${problems.length} problems with this configuration:`,
      h('ul', {}, problems.map((p) => h('li', {}, p.path || p.line
        ? h('button', { class: 'link', type: 'button', title: 'Go to it', onclick: () => goToProblem(p) }, p.message)
        : p.message)))));
  }

  const summary = validation.summary;
  clear(refs.validation,
    h('div', { class: 'notice ok' }, `Valid. ${summary.sites.length} release site${summary.sites.length === 1 ? '' : 's'}, flow period ${summary.periodStart.slice(0, 10)} to ${summary.periodEnd.slice(0, 10)}, ${summary.timeStep} steps.`),
    validation.warnings.length
      ? h('div', { class: 'notice warn' }, 'Worth a look:', h('ul', {}, validation.warnings.map((w) => h('li', {}, w))))
      : null);
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
  teardownEditor();
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
  // the log as it was written, and the part of the last line that has not all arrived
  state.run.rawLog = '';
  state.run.logPending = '';
  const full = h('input', { type: 'checkbox', onchange: (event) => { state.run.fullLog = event.target.checked; showLog(); } });
  clear($('#main'), refs.head, refs.progress, refs.metrics, refs.chart, refs.mapCard, refs.results,
    h('div', { class: 'card' }, h('div', { class: 'row-between' }, h('h3', {}, 'Log'),
      h('label', { class: 'hint' }, full, ' Show everything')), refs.log));
}

// What the log window shows. The log of a run is mostly a line for each day, which the chart above it already shows,
// each behind the name of the thread, the level and the class that wrote it, and the Java runtime's own notices: those
// are left out unless everything is asked for, which still has them.
function readableLog(raw) {
  return raw.split('\n').filter((line) => line && !/ has been completed in \d+ secs with /.test(line) && !line.startsWith('WARNING:'))
    .map((line) => line.replace(/^\[[^\]]*\] (INFO|DEBUG|TRACE) \S+ - /, '').replace(/^\[[^\]]*\] (WARN|ERROR) \S+ - /, '$1: '))
    .join('\n');
}

function showLog() {
  const box = state.run.refs.log;
  const stuck = box.scrollTop + box.clientHeight >= box.scrollHeight - 24;
  box.textContent = state.run.fullLog ? state.run.rawLog + state.run.logPending : readableLog(state.run.rawLog);
  if (stuck) box.scrollTop = box.scrollHeight;
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
      // only whole lines are kept: the rest of the last one comes with the next read
      const text = state.run.logPending + log.text;
      const end = text.lastIndexOf('\n') + 1;
      state.run.rawLog += text.slice(0, end);
      state.run.logPending = text.slice(end);
      showLog();
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

  // the reefs larvae were released from, outlined; the hover says which
  // the land, under everything else, so that what is not land is the water
  const land = map.land && map.land.length
    ? [svg('path', { class: 'land', d: map.land.map((ring) => path(ring) + 'Z').join('') })]
    : [];

  const reefs = map.reefs.map((reef) =>
    svg('path', { class: `reef ${reef.habitat === 'Reef' ? '' : 'other'}`, d: reef.rings.map((ring) => path(ring) + 'Z').join('') },
      svg('title', {}, document.createTextNode(`Patch ${reef.patch}${reef.habitat ? ` (${reef.habitat})` : ''}`))));

  const tracks = (set, extra) => set.tracks.map((t) =>
    svg('path', { class: `track ${extra}`, d: path(t.points), style: `stroke: var(--c${t.site < 0 ? 7 : t.site % 8})` }));

  const sites = map.sites.flatMap((site, i) => [
    svg('circle', { class: 'site', cx: x(site.longitude), cy: y(site.latitude), r: 5, style: `fill: var(--c${i % 8})` },
      svg('title', {}, document.createTextNode(`${site.name}, patch ${site.patch}`))),
    (() => { const label = textNode(Number(x(site.longitude)) + 9, Number(y(site.latitude)) + 4, site.name, 'start'); label.setAttribute('class', 'site-label'); return label; })(),
  ]);

  return svg('svg', { class: 'map', viewBox: `0 0 ${width.toFixed(0)} ${height.toFixed(0)}`, role: 'img', 'aria-label': 'Map of the land, the reefs larvae were released from, and larva paths' },
    ...land, ...grid, ...reefs, ...(options.sampled ? tracks(map.sampled, 'sampled') : []), ...(options.settled ? tracks(map.settled, '') : []), ...sites);
}

const metric = (value, label) => h('div', { class: 'metric' }, h('div', { class: 'value' }, value), h('div', { class: 'label' }, label));

// A round number to put gridlines at: the largest of 1, 2, 5 times a power of ten that gives at least three lines
function niceStep(max) {
  const raw = max / 4, power = Math.pow(10, Math.floor(Math.log10(raw)));
  return [1, 2, 5, 10].map((m) => m * power).find((step) => step >= raw);
}

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

const SITE_COLOURS = ['#3b6fb6', '#e07b39', '#3a9d6e', '#b5527f', '#8a6bd1', '#c9a227', '#2f9fb0', '#a65d3f'];

// The larvae in the water by day. When the log says how many are from each release site they are stacked, a band for
// each site; otherwise it is the one line for them all.
function lineChart(days) {
  const width = 640, height = 190, left = 46, bottom = 26, top = 10, right = 10;
  const names = [...new Set(days.flatMap((d) => d.sites.map((s) => s.site)))];
  const split = names.length > 1;
  const countOf = (d, name) => { const found = d.sites.find((s) => s.site === name); return found ? found.larvae : 0; };
  const max = Math.max(...days.map((d) => d.larvae), 1);
  const step = niceStep(max), ceiling = Math.ceil(max / step) * step;
  const x = (i) => left + (i / (days.length - 1)) * (width - left - right);
  const y = (v) => top + (1 - v / ceiling) * (height - top - bottom);
  const base = height - bottom;

  const grid = [];
  for (let v = 0; v <= ceiling; v += step) {
    grid.push(svg('line', { class: v === 0 ? 'axis' : 'gridline', x1: left, y1: y(v), x2: width - right, y2: y(v) }));
    grid.push(textNode(left - 6, y(v) + 4, formatNumber(v), 'end'));
  }
  // a tick and a label at the first day of each month; with many months, a label for every other one
  const firsts = days.map((d, i) => [d, i]).filter(([d]) => d.date.endsWith('-01'));
  const every = Math.ceil(firsts.length / 8);
  const months = firsts.flatMap(([d, i], k) => [
    svg('line', { class: 'axis', x1: x(i), y1: base, x2: x(i), y2: base + 4 }),
    ...(k % every === 0 ? [textNode(x(i), height - 8, `${MONTHS[Number(d.date.slice(5, 7)) - 1]}${d.date.slice(5, 7) === '01' ? ' ' + d.date.slice(0, 4) : ''}`, 'middle')] : [])
  ]);

  let shapes;
  if (split) {
    // each band lies on the ones below it: its top is the running total of the sites up to it
    const totals = days.map(() => 0);
    shapes = names.map((name, k) => {
      const lower = totals.slice();
      days.forEach((d, i) => { totals[i] += countOf(d, name); });
      const upper = days.map((d, i) => `${x(i).toFixed(1)},${y(totals[i]).toFixed(1)}`);
      const back = days.map((d, i) => `${x(i).toFixed(1)},${y(lower[i]).toFixed(1)}`).reverse();
      return svg('polygon', { class: 'band', points: [...upper, ...back].join(' '), fill: SITE_COLOURS[k % SITE_COLOURS.length] });
    });
  } else {
    const points = days.map((d, i) => `${x(i).toFixed(1)},${y(d.larvae).toFixed(1)}`);
    shapes = [
      svg('polygon', { class: 'area', points: `${x(0)},${base} ${points.join(' ')} ${x(days.length - 1)},${base}` }),
      svg('polyline', { class: 'line', points: points.join(' ') }),
      // the days the number in the water went up are days larvae were released: the others only lose larvae
      ...days.flatMap((d, i) => i > 0 && d.larvae > days[i - 1].larvae
        ? [svg('circle', { class: 'release', cx: x(i), cy: y(d.larvae), r: 2.5 })] : [])
    ];
  }

  const marker = svg('line', { class: 'cursor', x1: 0, y1: top, x2: 0, y2: base, visibility: 'hidden' });
  const readout = h('div', { class: 'chart-readout', hidden: true });
  const frame = svg('svg', { class: 'chart', viewBox: `0 0 ${width} ${height}`, role: 'img', 'aria-label': 'Larvae in the water, by day' },
    ...grid, ...shapes, ...months, marker);

  frame.addEventListener('pointermove', (event) => {
    const box = frame.getBoundingClientRect();
    const px = ((event.clientX - box.left) / box.width) * width;
    const i = Math.max(0, Math.min(days.length - 1, Math.round(((px - left) / (width - left - right)) * (days.length - 1))));
    const d = days[i];
    marker.setAttribute('x1', x(i)); marker.setAttribute('x2', x(i)); marker.setAttribute('visibility', 'visible');
    readout.hidden = false;
    const change = i > 0 ? d.larvae - days[i - 1].larvae : 0;
    readout.textContent = `${d.date}: ${formatNumber(d.larvae)} larvae` +
      (split ? ` (${names.filter((n) => countOf(d, n) > 0).map((n) => `${n} ${formatNumber(countOf(d, n))}`).join(', ')})`
        : i > 0 ? ` (${change >= 0 ? '+' : '−'}${formatNumber(Math.abs(change))} on the day before${change > 0 ? ', a release' : ''})` : '');
  });
  frame.addEventListener('pointerleave', () => { marker.setAttribute('visibility', 'hidden'); readout.hidden = true; });

  const legend = split ? h('div', { class: 'legend' }, names.map((name, k) =>
    h('span', {}, h('i', { style: `background:${SITE_COLOURS[k % SITE_COLOURS.length]}` }), name))) : null;
  return h('div', { class: 'chart-wrap' }, legend, frame, readout,
    h('p', { class: 'hint' }, 'Each rise is a release of larvae. Between releases the number falls as larvae die or settle on a reef.'));
}

function textNode(x, y, text, anchor) {
  const node = svg('text', { x, y, 'text-anchor': anchor });
  node.textContent = text;
  return node;
}

// Source to sink as a heatmap: every reef that larvae were released from or settled on, in the same order down
// the side and along the top (north to south), a cell for each pair that had larvae, coloured by how many (on a
// log scale, so that a few big flows do not hide the many small ones). The diagonal is larvae that settled where
// they were released.
function matrixHeatmap(matrix) {
  const n = matrix.patches.length;
  const left = 150, top = 120, size = 640;
  const cell = size / n;
  const x = (column) => left + column * cell;
  const y = (row) => top + row * cell;
  const shade = (count) => (0.2 + 0.8 * Math.log(1 + count) / Math.log(1 + Math.max(matrix.peak, 1))).toFixed(3);
  const named = matrix.patches.map((p, i) => ({ p, i })).filter(({ p }) => p.name);

  const cells = matrix.cells.map(([row, column, count]) => svg('rect', {
    class: 'cell', x: x(column).toFixed(2), y: y(row).toFixed(2),
    // a pair of reefs among thousands is less than a pixel: drawn no smaller than can be seen
    width: Math.max(cell, 1.8).toFixed(2), height: Math.max(cell, 1.8).toFixed(2), style: `opacity: ${shade(count)}`,
  }, svg('title', {}, document.createTextNode(
    `${count} larva${count === 1 ? '' : 'e'} from ${matrix.patches[row].name || `patch ${matrix.patches[row].patch}`} (patch ${matrix.patches[row].patch}) settled on patch ${matrix.patches[column].patch}${row === column ? ', where they were released' : ''}`))));

  const rowLabels = named.map(({ p, i }) => textNode(left - 8, y(i + 0.5) + 4, p.name, 'end'));
  const columnLabels = named.map(({ p, i }) => {
    const label = textNode(x(i + 0.5), top - 8, p.name, 'start');
    label.setAttribute('transform', `rotate(-50 ${x(i + 0.5).toFixed(1)} ${top - 8})`);
    return label;
  });
  const marks = named.flatMap(({ i }) => [
    svg('line', { class: 'mark', x1: left - 4, y1: y(i + 0.5), x2: left, y2: y(i + 0.5) }),
    svg('line', { class: 'mark', x1: x(i + 0.5), y1: top - 4, x2: x(i + 0.5), y2: top }),
  ]);

  return svg('svg', { class: 'matrix', viewBox: `0 0 ${left + size + 20} ${top + size + 20}`, role: 'img',
      'aria-label': 'Heatmap of larvae from each reef released from (down) to each reef settled on (across)' },
    svg('rect', { class: 'frame', x: left, y: top, width: size, height: size }),
    svg('line', { class: 'diagonal', x1: left, y1: top, x2: left + size, y2: top + size }),
    ...cells, ...marks, ...rowLabels, ...columnLabels);
}

function matrixView(matrix) {
  if (matrix.patches.length === 0) return h('p', { class: 'hint' }, 'No larvae settled in this run.');
  return h('div', {},
    h('p', { class: 'hint' }, `${matrix.patches.length} reefs, in the same order, north to south, down the side (released from) and along the top (settled on). The dashed diagonal is larvae that settled where they were released; point at a cell for its count.`),
    h('div', { class: 'legend heat-legend' },
      h('span', {}, '1 larva'), h('span', { class: 'ramp' }), h('span', {}, `${formatNumber(matrix.peak)} (log scale)`)),
    matrixHeatmap(matrix));
}

function resultsView(results) {
  const { matrix } = results;
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
      matrixView(matrix)),
    results.kernel ? kernelView(results.kernel) : null,
    h('div', { class: 'card' }, h('h3', {}, 'Files'),
      h('div', { class: 'files' }, results.files.map((f) =>
        h('a', { href: `/api/runs/${state.run.id}/files/${encodeURIComponent(f.name)}`, download: f.name }, `${f.name} (${formatBytes(f.bytes)})`)))));
}

const kmText = (km) => km >= 100 ? formatNumber(Math.round(km)) : km >= 10 ? km.toFixed(1) : km.toFixed(2).replace(/\.?0+$/, '') || '0';
const bandLabel = (b) => b.toKm === 0 ? 'stayed' : b.toKm == null ? `>${kmText(b.fromKm)}` : `${kmText(b.fromKm)}–${kmText(b.toKm)}`;

// How many larvae settled within each band of straight-line distance from where they were released (the bars, labelled
// with their share of all that settled), and what share had settled within that distance (the line, on the right). The
// first bar, larvae that never moved, is its own colour: a run where many do is telling you something.
function distanceChart(kernel) {
  const bands = kernel.bands;
  const width = 760, height = 300, left = 56, right = 52, top = 28, bottom = 52;
  const plotW = width - left - right, plotH = height - top - bottom;
  const peak = Math.max(1, ...bands.map((b) => b.count));
  const slot = plotW / bands.length;
  const barX = (i) => left + i * slot + slot * 0.15;
  const y = (count) => top + plotH - (count / peak) * plotH;
  let running = 0;
  const cumulative = bands.map((b) => (running += b.count) / kernel.count);
  const line = cumulative.map((c, i) => `${i ? 'L' : 'M'}${(left + (i + 0.5) * slot).toFixed(1)},${(top + plotH - c * plotH).toFixed(1)}`).join('');

  const bars = bands.flatMap((b, i) => [
    svg('rect', { class: `dbar${b.toKm === 0 ? ' stayed' : ''}`, x: barX(i).toFixed(1), y: y(b.count).toFixed(1), width: (slot * 0.7).toFixed(1), height: Math.max(0, top + plotH - y(b.count)).toFixed(1) },
      svg('title', {}, document.createTextNode(`${b.toKm === 0 ? 'Settled where released, without moving' : `${bandLabel(b)} km`}: ${formatNumber(b.count)} larvae (${((b.count / kernel.count) * 100).toFixed(1)}%), ${(cumulative[i] * 100).toFixed(1)}% by this distance`))),
    b.count > 0 ? textNode(left + (i + 0.5) * slot, y(b.count) - 5, `${((b.count / kernel.count) * 100).toFixed(b.count / kernel.count < 0.1 ? 1 : 0)}%`, 'middle') : null,
    textNode(left + (i + 0.5) * slot, top + plotH + 16, bandLabel(b), 'middle'),
  ]).filter(Boolean);

  const leftTicks = [0, 0.5, 1].map((f) => textNode(left - 6, top + plotH - f * plotH + 4, formatNumber(Math.round(peak * f)), 'end'));
  const rightTicks = [0, 0.5, 1].map((f) => textNode(width - right + 6, top + plotH - f * plotH + 4, `${f * 100}%`, 'start'));
  return svg('svg', { class: 'distance', viewBox: `0 0 ${width} ${height}`, role: 'img', 'aria-label': 'Larvae by distance from release to settlement, and the share settled within each distance' },
    svg('line', { class: 'axis', x1: left, y1: top + plotH, x2: width - right, y2: top + plotH }),
    ...bars, svg('path', { class: 'cumulative', d: line }),
    ...cumulative.map((c, i) => svg('circle', { class: 'cumulative-dot', cx: (left + (i + 0.5) * slot).toFixed(1), cy: (top + plotH - c * plotH).toFixed(1), r: 3 })),
    ...leftTicks, ...rightTicks,
    textNode(left, 12, 'larvae', 'start'), textNode(width - right, 12, 'settled within', 'end'),
    textNode(left + plotW / 2, height - 6, 'straight-line distance from release to settlement (km)', 'middle'));
}

function kernelView(kernel) {
  const share = (n) => `${((n / kernel.count) * 100).toFixed(n / kernel.count < 0.1 ? 1 : 0)}%`;
  const within = (p) => kernel.percentiles.find((x) => x.percent === p);
  const siteTable = kernel.bySite.length < 2 ? null : h('div', { class: 'scroll' }, h('table', {},
    h('thead', {}, h('tr', {}, ['Released from', 'Settled', 'Never moved', 'Median km', '90% within km', 'Furthest km'].map((t, i) => h('th', { class: i ? 'num' : '' }, t)))),
    h('tbody', {}, kernel.bySite.map((s) => h('tr', {},
      h('td', {}, s.name || `patch ${s.patch}`), h('td', { class: 'num' }, formatNumber(s.count)),
      h('td', { class: 'num' }, `${formatNumber(s.stayed)} (${Math.round((s.stayed / s.count) * 100)}%)`),
      h('td', { class: 'num' }, kmText(s.medianKm)), h('td', { class: 'num' }, kmText(s.p90Km)), h('td', { class: 'num' }, kmText(s.maxKm)))))));
  return h('div', { class: 'card' },
    h('h3', {}, 'How far larvae travelled'),
    h('div', { class: 'metrics' },
      metric(`${kmText(kernel.medianCrowFliesKm)} km`, 'median distance, release to settlement'),
      metric(`${kmText(within(90).km)} km`, '90% settled within'),
      metric(`${kmText(within(99).km)} km`, '99% settled within'),
      metric(`${kmText(kernel.maxCrowFliesKm)} km`, 'furthest'),
      metric(share(kernel.stayed), `never moved (${formatNumber(kernel.stayed)} of ${formatNumber(kernel.count)})`)),
    h('p', { class: 'hint' },
      `Straight-line distance from release to settlement, in bands that grow. Bars are the larvae in each band; the line is the share that had settled within that distance. ` +
      `Along the path they averaged ${kmText(kernel.meanDispersalKm)} km over ${kernel.meanAgeDays.toFixed(1)} days, against ${kmText(kernel.meanCrowFliesKm)} km in a straight line.`),
    h('div', { class: 'distance-chart' }, distanceChart(kernel)),
    siteTable);
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
  state.runningCount = state.runs.filter((r) => r.status === 'running').length;
  state.running = state.runningCount > 0;
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

$('#new-config').addEventListener('click', newConfigDialog);

document.addEventListener('keydown', (event) => {
  if ((event.metaKey || event.ctrlKey) && event.key === 's' && state.selected?.type === 'config') {
    event.preventDefault();
    if (!state.editor.refs.save.disabled) saveConfig();
  }
});

(async function start() {
  try {
    state.configs = await api('GET', '/api/configs');
    state.maxConcurrent = (await api('GET', '/api/info')).maxConcurrent || 1;
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
