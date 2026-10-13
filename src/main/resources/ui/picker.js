'use strict';

// A map in a dialog, to choose a place or an area on instead of typing coordinates.
//
//   MapPicker.open({ mode: 'point' | 'region', title, view, point, region, sites, regions, onChoose })
//
// view and region are [west, south, east, north]; point is [longitude, latitude]; sites are
// { name, longitude, latitude }; regions are drawn as outlines for reference. onChoose gets
// { longitude, latitude } or { west, south, east, north }.

const MapPicker = (() => {
  const WIDTH = 900;
  const DEFAULT_VIEW = [140, -50, 180, -8];
  const round = (n, places) => Math.round(n * 10 ** places) / 10 ** places;

  function fit(boxes, pad = 0.2) {
    const live = boxes.filter(Boolean);
    if (!live.length) return DEFAULT_VIEW.slice();
    let [west, south, east, north] = [Infinity, Infinity, -Infinity, -Infinity];
    for (const b of live) { west = Math.min(west, b[0]); south = Math.min(south, b[1]); east = Math.max(east, b[2]); north = Math.max(north, b[3]); }
    const w = Math.max(east - west, 2), hgt = Math.max(north - south, 2);
    return [west - w * pad, south - hgt * pad, east + w * pad, north + hgt * pad].map((v, i) => (i % 2 ? Math.max(-85, Math.min(85, v)) : v));
  }

  // a view much taller than wide would be a thin strip in a wide dialog, so widen it
  function widen(box) {
    const [west, south, east, north] = box;
    const k = Math.cos((((south + north) / 2) * Math.PI) / 180);
    const wanted = (1.4 * (north - south)) / k;
    if (east - west >= wanted) return box;
    const cx = (west + east) / 2;
    return [cx - wanted / 2, south, cx + wanted / 2, north];
  }

  function open(options) {
    const { mode, title, point, region, sites = [], regions = [], onChoose } = options;
    let view = widen(options.view ? options.view.slice() : fit([region, point && [point[0], point[1], point[0], point[1]], ...sites.map((s) => [s.longitude, s.latitude, s.longitude, s.latitude]), ...regions]));
    let chosen = mode === 'point' ? (point ? { longitude: point[0], latitude: point[1] } : null) : (region ? { west: region[0], south: region[1], east: region[2], north: region[3] } : null);
    let land = [];
    let ticket = 0;

    const readout = h('span', { class: 'picker-readout' }, mode === 'point' ? 'Click the map to place it.' : 'Drag on the map to draw the area.');
    const holder = h('div', { class: 'picker-map' });
    const useButton = h('button', { class: 'btn primary', type: 'button', onclick: () => { dialog.close('use'); onChoose(chosen); } }, 'Use this');
    const dialog = h('dialog', { class: 'picker', 'aria-label': title },
      h('div', { class: 'picker-head' }, h('h3', {}, title),
        h('div', { class: 'actions' },
          h('button', { class: 'btn small', type: 'button', onclick: () => zoom(1.5) }, 'Zoom out'),
          h('button', { class: 'btn small', type: 'button', onclick: () => zoom(1 / 1.5) }, 'Zoom in'))),
      holder,
      h('div', { class: 'picker-foot' }, readout,
        h('div', { class: 'actions' }, h('button', { class: 'btn', type: 'button', onclick: () => dialog.close('cancel') }, 'Cancel'), useButton)));
    dialog.addEventListener('close', () => dialog.remove());
    document.body.append(dialog);
    dialog.showModal();

    function zoom(factor) {
      const cx = (view[0] + view[2]) / 2, cy = (view[1] + view[3]) / 2;
      const w = Math.min((view[2] - view[0]) * factor, 360), hgt = Math.min((view[3] - view[1]) * factor, 170);
      view = widen([cx - w / 2, Math.max(-85, cy - hgt / 2), cx + w / 2, Math.min(85, cy + hgt / 2)]);
      load();
    }

    async function load() {
      const mine = ++ticket;
      draw();
      try {
        const reply = await api('GET', `/api/land?west=${view[0]}&south=${view[1]}&east=${view[2]}&north=${view[3]}`);
        if (mine === ticket) { land = reply.land; draw(); }
      } catch (e) { /* the map is still usable without the coastline */ }
    }

    function draw() {
      const [west, south, east, north] = view;
      const k = Math.cos((((south + north) / 2) * Math.PI) / 180);
      const scale = WIDTH / (east - west);
      const height = ((north - south) / k) * scale;
      const x = (lon) => (lon - west) * scale;
      const y = (lat) => ((north - lat) / k) * scale;
      const lonOf = (px) => west + px / scale;
      const latOf = (py) => north - (py * k) / scale;
      const path = (ring) => ring.map((p, i) => `${i ? 'L' : 'M'}${x(p[0]).toFixed(1)},${y(p[1]).toFixed(1)}`).join('') + 'Z';

      const parts = [];
      const lonStep = niceStep(east - west), latStep = niceStep(north - south);
      for (let lon = Math.ceil(west / lonStep) * lonStep; lon < east; lon += lonStep) {
        parts.push(svg('line', { class: 'grid', x1: x(lon), y1: 0, x2: x(lon), y2: height }), textNode(x(lon) + 4, height - 6, `${round(lon, 2)}°`, 'start'));
      }
      for (let lat = Math.ceil(south / latStep) * latStep; lat < north; lat += latStep) {
        parts.push(svg('line', { class: 'grid', x1: 0, y1: y(lat), x2: WIDTH, y2: y(lat) }), textNode(4, y(lat) - 4, `${round(lat, 2)}°`, 'start'));
      }
      if (land.length) parts.push(svg('path', { class: 'land', d: land.map(path).join('') }));
      const box = (b, cls) => svg('rect', { class: cls, x: x(b[0]), y: y(b[3]), width: Math.max(0, x(b[2]) - x(b[0])), height: Math.max(0, y(b[1]) - y(b[3])) });
      regions.forEach((r) => parts.push(box(r, 'area')));
      sites.forEach((s) => parts.push(
        svg('circle', { class: 'site', cx: x(s.longitude), cy: y(s.latitude), r: 4, style: 'fill: var(--muted)' }),
        (() => { const t = textNode(x(s.longitude) + 8, y(s.latitude) + 4, s.name, 'start'); t.setAttribute('class', 'site-label'); return t; })()));
      const chosenLayer = svg('g', { class: 'chosen' });
      parts.push(chosenLayer);

      const el = svg('svg', { class: 'map', viewBox: `0 0 ${WIDTH} ${height.toFixed(0)}`, role: 'img', 'aria-label': 'Map to choose on' }, ...parts);
      el.style.cursor = 'crosshair';

      const show = () => {
        chosenLayer.replaceChildren();
        if (chosen && mode === 'point') {
          chosenLayer.append(svg('circle', { class: 'pick', cx: x(chosen.longitude), cy: y(chosen.latitude), r: 7 }));
          readout.textContent = `${round(chosen.latitude, 4)}° latitude, ${round(chosen.longitude, 4)}° longitude`;
        } else if (chosen) {
          chosenLayer.append(box([chosen.west, chosen.south, chosen.east, chosen.north], 'pick-area'));
          readout.textContent = `South ${chosen.south}°, north ${chosen.north}°, west ${chosen.west}°, east ${chosen.east}°`;
        }
        useButton.disabled = !chosen;
      };
      show();

      const at = (event) => {
        const rect = el.getBoundingClientRect();
        const px = ((event.clientX - rect.left) / rect.width) * WIDTH;
        const py = ((event.clientY - rect.top) / rect.height) * height;
        return { lon: lonOf(px), lat: latOf(py) };
      };
      let anchor = null;
      el.addEventListener('pointerdown', (event) => {
        const p = at(event);
        if (mode === 'point') { chosen = { longitude: round(p.lon, 4), latitude: round(p.lat, 4) }; show(); return; }
        anchor = p;
        el.setPointerCapture(event.pointerId);
      });
      el.addEventListener('pointermove', (event) => {
        const p = at(event);
        if (mode === 'region' && anchor) {
          chosen = {
            west: round(Math.min(anchor.lon, p.lon), 2), east: round(Math.max(anchor.lon, p.lon), 2),
            south: round(Math.min(anchor.lat, p.lat), 2), north: round(Math.max(anchor.lat, p.lat), 2),
          };
          show();
        } else if (!chosen) {
          readout.textContent = `${round(p.lat, 3)}°, ${round(p.lon, 3)}°`;
        }
      });
      el.addEventListener('pointerup', () => {
        anchor = null;
        if (chosen && mode === 'region' && (chosen.west === chosen.east || chosen.south === chosen.north)) { chosen = null; show(); }
      });
      holder.replaceChildren(el);
    }

    load();
  }

  return { open, fit };
})();
