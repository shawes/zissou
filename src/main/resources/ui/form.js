'use strict';

// The form view of a configuration. The YAML text stays the one source of truth: every edit here
// goes through ZissouEditor.edit, which changes the one value in the YAML document, so comments,
// ordering and any settings the form does not know about are left as they were.
//
//   const form = ConfigForm.create(container, { text, onText, pick });
//   form.setText(text)           the text changed somewhere else; draw the form again
//   form.setProblems(problems)   [{ message, path }] from the server, shown against their fields

const ConfigForm = (() => {
  const Z = () => window.ZissouEditor;

  const pathKey = (parts) => parts.reduce((s, p) => (typeof p === 'number' ? `${s}[${p}]` : s ? `${s}.${p}` : String(p)), '');
  const getIn = (data, parts) => parts.reduce((v, p) => (v == null ? undefined : v[p]), data);
  const isDay = (v) => typeof v === 'string' && /^\d{4}-\d\d-\d\d$/.test(v);

  // ---- what the form knows about -----------------------------------------------------------

  const SITE_FIELDS = [
    { key: 'name', label: 'Name', kind: 'text', wide: true },
    { key: 'patchNumber', label: 'Patch number', kind: 'int', hint: 'The reef patch the larvae are spawned from.' },
    { key: ['site', 'latitude'], label: 'Latitude', kind: 'number', unit: '°', hint: 'Negative is south.', pick: 'site' },
    { key: ['site', 'longitude'], label: 'Longitude', kind: 'number', unit: '°E' },
    { key: ['site', 'depth'], label: 'Depth', kind: 'number', unit: 'm', hint: 'The depth larvae are released at.' },
    { key: 'numberOfLarvae', label: 'Larvae per release', kind: 'int' },
    { key: ['releasePeriod', 'start'], label: 'Releases start', kind: 'date' },
    { key: ['releasePeriod', 'end'], label: 'Releases end', kind: 'date' },
    { key: 'interval', label: 'Interval', kind: 'int', unit: 'days', hint: '1 is daily, 7 is weekly.' },
    { key: ['site', 'flowId'], label: 'Flow id', kind: 'int', optional: true, hint: 'Only for configurations that name which nested flow a site uses.' },
  ];

  const NEW_SITE = {
    name: 'New site', patchNumber: 0, site: { longitude: 0, latitude: 0, depth: 5 }, numberOfLarvae: 1000,
    releasePeriod: { start: '2020-01-01', end: '2020-01-31' }, interval: 7,
  };

  const LEVELS = ['trace', 'debug', 'info', 'error', 'off', 'all'];
  const STAGES = ['hatching', 'preflexion', 'flexion', 'postflexion'];

  const SWIMMING_DEFAULT = {
    strategy: 'one', ability: 'directed', criticalSwimmingSpeed: 0.4, inSituSwimmingPotential: 0.25,
    endurance: 0.5, reynoldsEffect: false, hatchSwimmingSpeed: 1,
  };

  // A section is a list of fields drawn from `fields`; `show` hides a field that does not apply.
  const SECTIONS = [
    { id: 'run', title: 'Run', fields: [
      { path: ['settings', 'randomSeed'], label: 'Random seed', kind: 'int', hint: 'The same seed gives the same run.' },
    ] },
    { id: 'sites', title: 'Release sites', custom: 'sites' },
    { id: 'larva', title: 'Larva', blocks: [
      { title: 'Species and life stages', fields: [
        { path: ['larva', 'species'], label: 'Species', kind: 'text' },
        { path: ['larva', 'ontogeny', 'preflexion'], label: 'Preflexion at', kind: 'int', unit: 'days', hint: 'Age the larva reaches preflexion. Above 0, it is a pelagic egg until then.' },
        { path: ['larva', 'ontogeny', 'flexion'], label: 'Flexion at', kind: 'int', unit: 'days' },
        { path: ['larva', 'ontogeny', 'postflexion'], label: 'Postflexion at', kind: 'int', unit: 'days' },
      ] },
      { title: 'Pelagic larval duration', fields: [
        { path: ['larva', 'pelagicLarvalDuration', 'pldType'], label: 'Kind', kind: 'select', options: ['fixed', 'normal'], hint: 'Fixed uses the mean for every larva; normal samples each from a normal distribution.' },
        { path: ['larva', 'pelagicLarvalDuration', 'mean'], label: 'Mean', kind: 'number', unit: 'days' },
        { path: ['larva', 'pelagicLarvalDuration', 'stdev'], label: 'Standard deviation', kind: 'number', unit: 'days' },
        { path: ['larva', 'pelagicLarvalDuration', 'nonSettlementPeriod'], label: 'Competency starts at', kind: 'number', unit: 'days', hint: 'Larvae cannot settle before this age.' },
      ] },
      { title: 'Mortality', fields: [
        { path: ['larva', 'isMortal'], label: 'Larvae can die', kind: 'bool', rerender: true },
        { path: ['larva', 'mortalityRate'], label: 'Mortality rate', kind: 'number', unit: 'per day', hint: 'A share from 0 to 1: 0.26 kills 26% of larvae a day.',
          show: (d) => d.larva && d.larva.isMortal === true, optional: true },
      ] },
      { title: 'Horizontal swimming', optionalPath: ['larva', 'swimming'], optionalDefault: SWIMMING_DEFAULT, fields: [
        { path: ['larva', 'swimming', 'strategy'], label: 'Strategy', kind: 'text' },
        { path: ['larva', 'swimming', 'ability'], label: 'Ability', kind: 'select', options: ['directed', 'undirected', 'passive'] },
        { path: ['larva', 'swimming', 'criticalSwimmingSpeed'], label: 'Critical swimming speed', kind: 'number', unit: 'm/s', hint: 'Speed at which the fish tires, measured in a laboratory.' },
        { path: ['larva', 'swimming', 'inSituSwimmingPotential'], label: 'In situ speed', kind: 'number', unit: 'of critical', hint: 'Proportion of the critical speed seen in the field.' },
        { path: ['larva', 'swimming', 'endurance'], label: 'Endurance', kind: 'number', unit: 'proportion', hint: 'The share of time the fish can spend swimming.' },
        { path: ['larva', 'swimming', 'reynoldsEffect'], label: 'Reynolds effect', kind: 'bool' },
        { path: ['larva', 'swimming', 'hatchSwimmingSpeed'], label: 'Swimming speed at hatching', kind: 'number', unit: 'cm/s' },
      ] },
      { title: 'Diel vertical migration', optionalPath: ['larva', 'dielProbabilities'], profile: 'diel' },
      { title: 'Ontogenetic vertical migration', optionalPath: ['larva', 'ovmProbabilities'], profile: 'ovm' },
    ] },
    { id: 'flow', title: 'Ocean flow', blocks: [
      { title: 'Data', fields: [
        { path: ['flow', 'netcdfFilePath'], label: 'Flow folder or address', kind: 'text', wide: true, hint: 'A folder of monthly NetCDF files, or the address of a directory on an OPeNDAP server.' },
        { path: ['flow', 'filePrefix'], label: 'File prefix', kind: 'text', optional: true },
        { path: ['flow', 'period', 'start'], label: 'Period starts', kind: 'date', hint: 'Start on the first release day, or the run ends at once.' },
        { path: ['flow', 'period', 'end'], label: 'Period ends', kind: 'date' },
        { path: ['flow', 'timeStep', 'duration'], label: 'Time step', kind: 'int' },
        { path: ['flow', 'timeStep', 'unit'], label: 'Time step unit', kind: 'select', options: ['Second', 'Hour', 'Day'] },
        { path: ['flow', 'includeVerticalVelocity'], label: 'Include vertical velocity', kind: 'bool' },
      ] },
      { title: 'Region that is read', optionalPath: ['flow', 'bounds'], optionalDefault: { south: -45, north: -10, west: 142, east: 180 }, region: ['flow', 'bounds'],
        note: 'Larvae that leave the region are killed, so leave room round the area you are studying.',
        fields: [
          { path: ['flow', 'bounds', 'south'], label: 'South', kind: 'number', unit: '°' },
          { path: ['flow', 'bounds', 'north'], label: 'North', kind: 'number', unit: '°' },
          { path: ['flow', 'bounds', 'west'], label: 'West', kind: 'number', unit: '°E' },
          { path: ['flow', 'bounds', 'east'], label: 'East', kind: 'number', unit: '°E' },
        ] },
      { title: 'Depth levels', fields: [
        { path: ['flow', 'depthLevels'], label: 'Levels from the surface', kind: 'int', optional: true, hint: 'Left empty, 15 are read.' },
      ] },
      { title: 'Nested models', custom: 'nested' },
    ] },
    { id: 'turbulence', title: 'Turbulence', fields: [
      { path: ['turbulence', 'applyTurbulence'], label: 'Apply turbulence', kind: 'bool' },
      { path: ['turbulence', 'horizontalDiffusionCoefficient'], label: 'Horizontal diffusion', kind: 'number', unit: 'm²/s' },
      { path: ['turbulence', 'verticalDiffusionCoefficient'], label: 'Vertical diffusion', kind: 'number', unit: 'm²/s' },
      { path: ['turbulence', 'interval'], label: 'Interval', kind: 'int', unit: 'steps' },
    ] },
    { id: 'habitat', title: 'Habitat', fields: [
      { path: ['habitat', 'shapeFilePath'], label: 'Reef shapefile', kind: 'text', wide: true, hint: 'Polygons that larvae settle on.' },
      { path: ['habitat', 'buffer', 'settlement'], label: 'Settlement buffer', kind: 'number', unit: 'km', hint: 'How close to a reef a larva can settle.' },
      { path: ['habitat', 'buffer', 'olfactory'], label: 'Sensory distance', kind: 'number', unit: 'km', hint: 'How far away a larva can sense a reef.' },
    ] },
    { id: 'output', title: 'Output', fields: [
      { path: ['output', 'saveOutputFilePath'], label: 'Output folder', kind: 'text', wide: true },
      { path: ['output', 'prefix'], label: 'File prefix', kind: 'text' },
      { path: ['output', 'percentage'], label: 'Larvae sampled', kind: 'int', unit: '%', hint: 'The share of larvae whose tracks are written.' },
      { path: ['output', 'includeLarvaeHistory'], label: 'Keep each larva’s history', kind: 'bool' },
      { path: ['output', 'logLevel'], label: 'Log level', kind: 'select', options: LEVELS },
      { path: ['output', 'logFile'], label: 'Log file', kind: 'text', wide: true },
    ] },
  ];

  // ---- the form ------------------------------------------------------------------------------

  function create(container, { text, onText, pick }) {
    const ctx = { text, data: Z().parse(text), errors: new Map(), problems: [], onText, pick };

    function commit(change) {
      ctx.text = Z().edit(ctx.text, change);
      ctx.data = Z().parse(ctx.text);
      ctx.onText(ctx.text);
    }

    const setValue = (path, value) => commit((doc) => (value === undefined ? doc.deleteIn(path) : doc.setIn(path, value)));
    const setFlow = (path, array) => commit((doc) => {
      const seq = doc.createNode(array);
      seq.flow = true;
      doc.setIn(path, seq);
    });

    // draws again after a change of structure, keeping where the user was
    function rerender() {
      const scroll = window.scrollY;
      const focused = document.activeElement && document.activeElement.dataset ? document.activeElement.dataset.path : null;
      const open = [...container.querySelectorAll('details[data-key]')].filter((d) => d.open).map((d) => d.dataset.key);
      draw(open);
      if (focused) { const again = container.querySelector(`[data-path="${CSS.escape(focused)}"]`); if (again) again.focus(); }
      window.scrollTo(0, scroll);
    }

    // ---- fields ----

    function field(path, spec) {
      const key = pathKey(path);
      const value = getIn(ctx.data, path);
      const id = `f-${key.replace(/\W+/g, '-')}`;
      const error = h('div', { class: 'field-error', role: 'alert' });
      ctx.errors.set(key, error);
      const set = (v) => { setValue(path, v); if (spec.rerender) rerender(); };

      let input;
      if (spec.kind === 'bool') {
        input = h('input', { type: 'checkbox', id, 'data-path': key, checked: value === true, onchange: (e) => set(e.target.checked) });
        return h('div', { class: 'field bool' + (spec.wide ? ' wide' : '') }, h('label', { for: id }, input, ' ', spec.label),
          spec.hint ? h('div', { class: 'field-hint' }, spec.hint) : null, error);
      }
      if (spec.kind === 'select') {
        const options = spec.options.includes(value) || value == null ? spec.options : [...spec.options, value];
        input = h('select', { id, 'data-path': key, onchange: (e) => set(e.target.value) },
          value == null ? h('option', { value: '' }, '') : null, options.map((o) => h('option', { value: o, selected: o === value }, o)));
      } else if (spec.kind === 'int' || spec.kind === 'number') {
        input = h('input', {
          type: 'number', id, 'data-path': key, step: spec.kind === 'int' ? '1' : 'any', inputmode: 'decimal',
          value: value == null ? '' : String(value),
          oninput: (e) => {
            const raw = e.target.value.trim();
            if (raw === '') set(spec.optional ? undefined : '');
            else if (Number.isFinite(Number(raw))) set(Number(raw));
          },
        });
      } else if (spec.kind === 'date' && (value == null || isDay(value))) {
        input = h('input', { type: 'date', id, 'data-path': key, value: value || '', oninput: (e) => e.target.value && set(e.target.value) });
      } else {
        input = h('input', {
          type: 'text', id, 'data-path': key, spellcheck: 'false', value: value == null ? '' : String(value),
          oninput: (e) => set(e.target.value === '' && spec.optional ? undefined : e.target.value),
        });
      }
      return h('div', { class: 'field' + (spec.wide ? ' wide' : '') },
        h('label', { for: id }, spec.label, spec.unit ? h('span', { class: 'unit' }, ` (${spec.unit})`) : null, spec.optional ? h('span', { class: 'opt' }, ' optional') : null),
        h('div', { class: 'input-row' }, input,
          spec.pick && ctx.pick ? h('button', { class: 'btn small', type: 'button', title: 'Choose on a map', onclick: () => ctx.pick(spec.pick, pathOwner(path)) }, 'Map…') : null),
        spec.hint ? h('div', { class: 'field-hint' }, spec.hint) : null, error);
    }

    // the site that a field belongs to, for the map picker
    const pathOwner = (path) => path.slice(0, 3);

    const fieldsOf = (fields) => h('div', { class: 'fields' },
      fields.filter((f) => !f.show || f.show(ctx.data || {})).map((f) => field(f.path, f)));

    // ---- release sites ----

    function sites() {
      const list = getIn(ctx.data, ['spawn', 'spawningLocation']) || [];
      const base = ['spawn', 'spawningLocation'];
      const cards = list.map((site, i) => {
        const at = [...base, i];
        const key = pathKey(at);
        const summary = h('summary', {},
          h('span', { class: 'site-name' }, site.name || `Site ${i + 1}`),
          h('span', { class: 'site-meta' }, [
            site.patchNumber != null ? `patch ${site.patchNumber}` : null,
            site.site ? `${site.site.latitude}, ${site.site.longitude}` : null,
            site.numberOfLarvae != null ? `${Number(site.numberOfLarvae).toLocaleString()} larvae` : null,
          ].filter(Boolean).join(' · ')));
        const err = h('span', { class: 'site-error' });
        ctx.errors.set(key, err);
        summary.append(err);
        return h('details', { class: 'site', 'data-key': key },
          summary,
          h('div', { class: 'fields' }, SITE_FIELDS.map((f) => field([...at, ...[].concat(f.key)], f))),
          h('div', { class: 'row-actions' },
            h('button', { class: 'btn small', type: 'button', onclick: () => duplicate(i) }, 'Duplicate'),
            h('button', { class: 'btn small danger', type: 'button', disabled: list.length < 2, onclick: () => remove(i) }, 'Remove')));
      });

      const shared = { numberOfLarvae: '', start: '', end: '', interval: '', depth: '' };
      const sharedInput = (label, name, type, extra = {}) => h('label', { class: 'shared-field' }, label,
        h('input', { type, ...extra, oninput: (e) => { shared[name] = e.target.value; } }));

      return h('div', {},
        h('p', { class: 'hint' }, `${list.length} release site${list.length === 1 ? '' : 's'}. Open one to edit it.`),
        h('div', { class: 'site-list' }, cards),
        h('div', { class: 'row-actions' },
          h('button', { class: 'btn small', type: 'button', onclick: add }, 'Add site'),
          ctx.pick ? h('button', { class: 'btn small', type: 'button', onclick: () => ctx.pick('new-site', null) }, 'Add site on map…') : null),
        list.length > 1 ? h('details', { class: 'bulk' },
          h('summary', {}, 'Set the same value for every site'),
          h('div', { class: 'shared' },
            sharedInput('Larvae per release', 'numberOfLarvae', 'number', { step: '1' }),
            sharedInput('Releases start', 'start', 'date'),
            sharedInput('Releases end', 'end', 'date'),
            sharedInput('Interval (days)', 'interval', 'number', { step: '1' }),
            sharedInput('Depth (m)', 'depth', 'number', { step: 'any' }),
            h('button', { class: 'btn small', type: 'button', onclick: () => applyAll(shared) }, 'Apply to all sites'))) : null);
    }

    function add() {
      const list = getIn(ctx.data, ['spawn', 'spawningLocation']) || [];
      addSite(list.length ? { ...structuredClone(list[list.length - 1]), name: `Site ${list.length + 1}` } : structuredClone(NEW_SITE));
    }
    function addSite(site) {
      commit((doc) => {
        if (!doc.hasIn(['spawn', 'spawningLocation'])) doc.setIn(['spawn', 'spawningLocation'], doc.createNode([]));
        doc.addIn(['spawn', 'spawningLocation'], doc.createNode(site));
      });
      rerender();
      const items = container.querySelectorAll('details.site');
      const last = items[items.length - 1];
      if (last) { last.open = true; last.scrollIntoView({ block: 'nearest' }); }
    }
    function duplicate(i) {
      const site = structuredClone(getIn(ctx.data, ['spawn', 'spawningLocation', i]));
      site.name = `${site.name} copy`;
      addSite(site);
    }
    function remove(i) {
      commit((doc) => doc.deleteIn(['spawn', 'spawningLocation', i]));
      rerender();
    }
    function applyAll(shared) {
      const list = getIn(ctx.data, ['spawn', 'spawningLocation']) || [];
      commit((doc) => list.forEach((_, i) => {
        const at = ['spawn', 'spawningLocation', i];
        if (shared.numberOfLarvae !== '') doc.setIn([...at, 'numberOfLarvae'], Number(shared.numberOfLarvae));
        if (shared.start) doc.setIn([...at, 'releasePeriod', 'start'], shared.start);
        if (shared.end) doc.setIn([...at, 'releasePeriod', 'end'], shared.end);
        if (shared.interval !== '') doc.setIn([...at, 'interval'], Number(shared.interval));
        if (shared.depth !== '') doc.setIn([...at, 'site', 'depth'], Number(shared.depth));
      }));
      rerender();
    }

    // ---- nested flow models ----

    function nested() {
      const list = getIn(ctx.data, ['flow', 'nested']) || [];
      const rows = list.map((_, i) => {
        const at = ['flow', 'nested', i];
        const err = h('span', { class: 'site-error' });
        ctx.errors.set(pathKey(at), err);
        return h('details', { class: 'site', 'data-key': pathKey(at) },
          h('summary', {}, h('span', { class: 'site-name' }, `Nested model ${i + 1}`), err),
          h('div', { class: 'fields' }, [
            field([...at, 'netcdfFilePath'], { label: 'Flow folder or address', kind: 'text', wide: true }),
            field([...at, 'bounds', 'south'], { label: 'South', kind: 'number', unit: '°' }),
            field([...at, 'bounds', 'north'], { label: 'North', kind: 'number', unit: '°' }),
            field([...at, 'bounds', 'west'], { label: 'West', kind: 'number', unit: '°E' }),
            field([...at, 'bounds', 'east'], { label: 'East', kind: 'number', unit: '°E' }),
            field([...at, 'depthLevels'], { label: 'Depth levels', kind: 'int', optional: true }),
            field([...at, 'filePrefix'], { label: 'File prefix', kind: 'text', optional: true }),
          ]),
          h('div', { class: 'row-actions' }, ctx.pick ? h('button', { class: 'btn small', type: 'button', onclick: () => ctx.pick('region', [...at, 'bounds']) }, 'Choose area on map…') : null,
            h('button', { class: 'btn small danger', type: 'button', onclick: () => { commit((doc) => doc.deleteIn(at)); if (list.length === 1) commit((doc) => doc.deleteIn(['flow', 'nested'])); rerender(); } }, 'Remove')));
      });
      return h('div', {},
        h('p', { class: 'hint' }, list.length ? 'A finer model inside the main one. Its period and time step are the main flow’s.' : 'None. A nested model is a finer ocean model used inside part of the main one.'),
        h('div', { class: 'site-list' }, rows),
        h('div', { class: 'row-actions' }, h('button', { class: 'btn small', type: 'button', onclick: () => {
          commit((doc) => {
            if (!doc.hasIn(['flow', 'nested'])) doc.setIn(['flow', 'nested'], doc.createNode([]));
            doc.addIn(['flow', 'nested'], doc.createNode({ netcdfFilePath: '', bounds: { south: -35, north: -30, west: 150, east: 155 } }));
          });
          rerender();
        } }, 'Add nested model')));
    }

    // ---- depth profiles (diel and ontogenetic migration) ----

    // Rows of probabilities, one number for each depth band. A row adds up to 1, or is all 0.
    function profile(kind) {
      const root = kind === 'diel' ? ['larva', 'dielProbabilities'] : ['larva', 'ovmProbabilities'];
      const rows = kind === 'diel' ? ['day', 'night'] : STAGES;
      const data = getIn(ctx.data, root) || {};
      const depths = data.depths || [];
      const err = h('div', { class: 'field-error', role: 'alert' });
      ctx.errors.set(pathKey(root), err);

      const number = (path, value, onValue, label) => h('input', {
        type: 'number', step: 'any', value: value == null ? '' : String(value), 'aria-label': label, 'data-path': pathKey(path),
        oninput: (e) => { const n = Number(e.target.value); if (e.target.value !== '' && Number.isFinite(n)) onValue(n); },
      });
      const setAt = (path, index, n) => {
        const current = [...(getIn(ctx.data, path) || [])];
        current[index] = n;
        setFlow(path, current);
        refreshSums();
      };
      const sums = [];
      const refreshSums = () => sums.forEach(({ el, path, bars }) => {
        const values = getIn(ctx.data, path) || [];
        const total = values.reduce((a, b) => a + (Number(b) || 0), 0);
        const ok = Math.abs(total - 1) <= 1e-6 || total === 0;
        el.textContent = `= ${Math.round(total * 1e6) / 1e6}`;
        el.className = 'sum ' + (ok ? 'ok' : 'bad');
        bars.forEach((bar, i) => { bar.style.width = `${Math.min(100, Math.max(0, (Number(values[i]) || 0) * 100))}%`; });
      });

      const head = h('tr', {}, h('th', {}, 'Depth (m)'),
        depths.map((d, i) => h('th', {}, number([...root, 'depths'], d, (n) => setAt([...root, 'depths'], i, n), `Depth ${i + 1}`),
          depths.length > 1 ? h('button', { class: 'icon', type: 'button', title: 'Remove this depth', 'aria-label': `Remove depth ${d}`, onclick: () => removeBand(root, rows, i) }, '×') : null)),
        h('th', {}, ''));
      const body = rows.map((row) => {
        const path = [...root, row];
        const values = data[row] || [];
        const sumEl = h('td', { class: 'sum' });
        const bars = depths.map(() => h('span', { class: 'bar-fill' }));
        sums.push({ el: sumEl, path, bars });
        return h('tr', {}, h('th', { scope: 'row' }, row),
          depths.map((_, i) => h('td', {}, number(path, values[i], (n) => setAt(path, i, n), `${row} at depth ${i + 1}`), h('span', { class: 'bar' }, bars[i]))),
          sumEl);
      });
      refreshSums();

      const extra = kind === 'ovm' ? h('div', { class: 'fields' },
        field([...root, 'implementation'], { label: 'Migrates', kind: 'select', options: ['stage', 'daily', 'timestep'],
          hint: 'stage: when a larva changes stage; daily: on the first step of a day; timestep: every step.' })) : null;
      return h('div', {}, extra,
        h('div', { class: 'scroll' }, h('table', { class: 'profile' }, h('thead', {}, head), h('tbody', {}, body))),
        h('p', { class: 'hint' }, 'Each row is the probability of being in each depth band. It adds up to 1, or is all 0 to leave larvae where they are.'),
        h('div', { class: 'row-actions' }, h('button', { class: 'btn small', type: 'button', onclick: () => addBand(root, rows) }, 'Add depth')), err);
    }

    function addBand(root, rows) {
      const depths = getIn(ctx.data, [...root, 'depths']) || [];
      const last = depths.length ? depths[depths.length - 1] : 0;
      commit((doc) => {
        const put = (path, array) => { const s = doc.createNode(array); s.flow = true; doc.setIn(path, s); };
        put([...root, 'depths'], [...depths, last + 25]);
        rows.forEach((r) => put([...root, r], [...(getIn(ctx.data, [...root, r]) || []), 0]));
      });
      rerender();
    }
    function removeBand(root, rows, index) {
      commit((doc) => {
        const put = (path, array) => { const s = doc.createNode(array); s.flow = true; doc.setIn(path, s); };
        put([...root, 'depths'], (getIn(ctx.data, [...root, 'depths']) || []).filter((_, i) => i !== index));
        rows.forEach((r) => put([...root, r], (getIn(ctx.data, [...root, r]) || []).filter((_, i) => i !== index)));
      });
      rerender();
    }

    const PROFILE_DEFAULT = {
      diel: { depths: [25, 50, 75, 100], day: [0.25, 0.25, 0.25, 0.25], night: [0.25, 0.25, 0.25, 0.25] },
      ovm: { implementation: 'stage', depths: [5, 50, 100], hatching: [1, 0, 0], preflexion: [0.4, 0.5, 0.1], flexion: [0.35, 0.5, 0.15], postflexion: [0.05, 0.85, 0.1] },
    };

    // ---- layout ----

    function block(spec) {
      const body = spec.custom === 'nested' ? nested()
        : spec.profile ? profile(spec.profile)
        : h('div', {}, spec.note ? h('p', { class: 'hint' }, spec.note) : null, fieldsOf(spec.fields),
          spec.region && ctx.pick ? h('div', { class: 'row-actions' }, h('button', { class: 'btn small', type: 'button', onclick: () => ctx.pick('region', spec.region) }, 'Choose area on map…')) : null);

      if (!spec.optionalPath) return h('div', { class: 'block' }, h('h4', {}, spec.title), body);

      const present = getIn(ctx.data, spec.optionalPath) != null;
      const toggle = h('input', {
        type: 'checkbox', checked: present, 'data-path': `use:${pathKey(spec.optionalPath)}`,
        onchange: (e) => {
          const fallback = spec.optionalDefault || PROFILE_DEFAULT[spec.profile];
          if (e.target.checked) commit((doc) => doc.setIn(spec.optionalPath, doc.createNode(structuredClone(fallback))));
          else commit((doc) => doc.deleteIn(spec.optionalPath));
          rerender();
        },
      });
      return h('div', { class: 'block' + (present ? '' : ' off') },
        h('h4', {}, h('label', {}, toggle, ' ', spec.title), present ? null : h('span', { class: 'opt' }, ' not used')),
        present ? body : null);
    }

    function section(spec, open) {
      const err = h('span', { class: 'section-error' });
      ctx.errors.set(`section:${spec.id}`, err);
      const content = spec.custom === 'sites' ? sites()
        : spec.blocks ? spec.blocks.map(block) : fieldsOf(spec.fields);
      return h('section', { class: 'card form-section', id: `section-${spec.id}` }, h('h3', {}, spec.title, err), h('div', { class: 'section-problems' }), content);
    }

    function draw(open = []) {
      ctx.errors = new Map();
      if (!ctx.data || typeof ctx.data !== 'object') {
        clear(container, h('div', { class: 'notice error' }, 'The YAML cannot be read, so there is no form to show. Fix it in the YAML view first.'));
        return;
      }
      const nav = h('nav', { class: 'section-nav', 'aria-label': 'Sections' }, SECTIONS.map((s) =>
        h('a', { href: `#section-${s.id}`, onclick: (e) => { e.preventDefault(); const el = $(`#section-${s.id}`); el && el.scrollIntoView({ behavior: 'smooth', block: 'start' }); } }, s.title)));
      clear(container, nav, SECTIONS.map((s) => section(s)));
      container.querySelectorAll('details[data-key]').forEach((d) => { if (open.includes(d.dataset.key)) d.open = true; });
      showProblems();
    }

    // ---- problems ----

    // A problem is shown against its field, or the nearest thing above it that is on the form
    function showProblems() {
      const unmatched = new Map();
      for (const el of ctx.errors.values()) { el.textContent = ''; el.removeAttribute('title'); }
      container.querySelectorAll('.invalid').forEach((el) => el.classList.remove('invalid'));
      container.querySelectorAll('.section-problems').forEach((el) => el.replaceChildren());
      const sectionIds = { settings: 'run', spawn: 'sites', larva: 'larva', flow: 'flow', turbulence: 'turbulence', habitat: 'habitat', output: 'output' };
      const counts = new Map();
      for (const problem of ctx.problems) {
        const parts = Z().parsePath(problem.path || '');
        const section = sectionIds[parts[0]];
        if (section) counts.set(section, (counts.get(section) || 0) + 1);
        let el = null;
        for (let n = parts.length; n > 0 && !el; n--) el = ctx.errors.get(pathKey(parts.slice(0, n)));
        if (!el) {
          const list = container.querySelector(`#section-${section || 'run'} .section-problems`);
          if (list) list.append(h('div', { class: 'notice error' }, problem.message));
        } else if (el.className === 'field-error') {
          el.textContent = el.textContent ? `${el.textContent} ${problem.message}` : problem.message;
          const input = el.parentElement && el.parentElement.querySelector('input, select');
          if (input) input.classList.add('invalid');
        }
        // a card that holds the problem says so, closed or open
        for (let n = parts.length; n > 0; n--) {
          const mark = ctx.errors.get(pathKey(parts.slice(0, n)));
          if (mark && mark.className === 'site-error') {
            mark.textContent = '●';
            mark.title = mark.title ? `${mark.title}\n${problem.message}` : problem.message;
            const card = mark.closest('details');
            if (card) card.classList.add('invalid');
          }
        }
      }
      for (const [id, n] of counts) {
        const el = ctx.errors.get(`section:${id}`);
        if (el) el.textContent = `${n} problem${n === 1 ? '' : 's'}`;
      }
    }

    draw();
    return {
      setText(next) { ctx.text = next; ctx.data = Z().parse(next); draw(); },
      setProblems(problems) { ctx.problems = problems || []; showProblems(); },
      // apply a change made outside the form, for example a point chosen on the map
      change(fn) { commit(fn); rerender(); },
      data: () => ctx.data,
      focusPath(path) {
        const parts = Z().parsePath(path);
        for (let n = parts.length; n > 0; n--) {
          const el = container.querySelector(`[data-path="${CSS.escape(pathKey(parts.slice(0, n)))}"]`);
          if (el) { const d = el.closest('details'); if (d) d.open = true; el.scrollIntoView({ block: 'center' }); el.focus(); return; }
        }
      },
    };
  }

  return { create, SECTIONS, pathKey };
})();
