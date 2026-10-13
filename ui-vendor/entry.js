// Bundled by `npm run build` into ../src/main/resources/ui/vendor/editor.js.
// Exposes window.ZissouEditor: a YAML editor that shows problems in the text, and helpers
// the form view uses to edit the YAML text without losing its comments or layout.
import { EditorState } from '@codemirror/state';
import { EditorView, keymap, lineNumbers, highlightActiveLine, highlightActiveLineGutter, drawSelection } from '@codemirror/view';
import { defaultKeymap, history, historyKeymap, indentWithTab } from '@codemirror/commands';
import { yaml } from '@codemirror/lang-yaml';
import { HighlightStyle, syntaxHighlighting, indentOnInput, foldGutter, foldKeymap, bracketMatching } from '@codemirror/language';
import { setDiagnostics, lintGutter } from '@codemirror/lint';
import { tags as t } from '@lezer/highlight';
import YAML from 'yaml';

// colours come from the page's own variables, so light and dark mode need nothing here
const theme = EditorView.theme({
  '&': { color: 'var(--ink)', backgroundColor: 'var(--code-bg)', border: '1px solid var(--line)', borderRadius: 'var(--radius)', fontSize: '13px' },
  '&.cm-focused': { outline: '2px solid var(--accent)', outlineOffset: '2px' },
  '.cm-scroller': { fontFamily: 'var(--mono)', lineHeight: '1.5', minHeight: '360px', maxHeight: '70vh' },
  '.cm-content': { caretColor: 'var(--ink)', padding: '8px 0' },
  '&.cm-focused .cm-cursor': { borderLeftColor: 'var(--ink)' },
  '.cm-gutters': { backgroundColor: 'transparent', color: 'var(--muted)', border: 'none' },
  '.cm-activeLine': { backgroundColor: 'color-mix(in srgb, var(--accent) 7%, transparent)' },
  '.cm-activeLineGutter': { backgroundColor: 'transparent', color: 'var(--ink)' },
  '&.cm-focused .cm-selectionBackground, .cm-selectionBackground, ::selection': { backgroundColor: 'color-mix(in srgb, var(--accent) 28%, transparent)' },
  '.cm-tooltip': { backgroundColor: 'var(--panel)', color: 'var(--ink)', border: '1px solid var(--line)', borderRadius: '6px' },
  '.cm-diagnostic': { padding: '4px 8px', fontFamily: 'system-ui, sans-serif' },
  '.cm-diagnostic-error': { borderLeft: '3px solid var(--bad)' },
  '.cm-lintRange-error': { backgroundImage: 'none', textDecoration: 'underline wavy var(--bad)', textUnderlineOffset: '3px' },
  '.cm-lint-marker-error': { content: '"●"', color: 'var(--bad)' },
});

const highlight = HighlightStyle.define([
  { tag: [t.propertyName, t.definition(t.propertyName)], color: 'var(--c0)' },
  { tag: [t.string, t.special(t.string)], color: 'var(--c2)' },
  { tag: [t.number, t.bool, t.null], color: 'var(--c1)' },
  { tag: [t.comment, t.lineComment], color: 'var(--muted)', fontStyle: 'italic' },
  { tag: [t.separator, t.punctuation, t.brace, t.squareBracket], color: 'var(--muted)' },
  { tag: [t.meta, t.labelName, t.typeName], color: 'var(--c4)' },
]);

// "spawn.spawningLocation[0].site" -> ['spawn', 'spawningLocation', 0, 'site']
function parsePath(path) {
  if (Array.isArray(path)) return path;
  const parts = [];
  for (const piece of String(path).split('.')) {
    if (!piece) continue;
    const m = piece.match(/^([^[]*)((?:\[\d+\])*)$/);
    if (!m) { parts.push(piece); continue; }
    if (m[1]) parts.push(m[1]);
    for (const i of m[2].matchAll(/\[(\d+)\]/g)) parts.push(Number(i[1]));
  }
  return parts;
}

// The character range of the value at `path`, or of the nearest enclosing thing that exists
// (a missing field is reported against its section). Null if the text cannot be parsed.
function locate(text, path) {
  const doc = YAML.parseDocument(text);
  if (doc.errors.length) return null;
  const parts = parsePath(path);
  for (let n = parts.length; n > 0; n--) {
    const prefix = parts.slice(0, n);
    const node = doc.getIn(prefix, true);
    if (node && node.range) {
      // point at the key when there is one: it is what the message names
      const parent = n > 1 ? doc.getIn(parts.slice(0, n - 1), true) : doc.contents;
      const pair = parent && parent.items && parent.items.find((p) => p && p.value === node);
      const from = pair && pair.key && pair.key.range ? pair.key.range[0] : node.range[0];
      return { from, to: Math.max(node.range[1], from + 1), exact: n === parts.length };
    }
  }
  return null;
}

function createEditor(parent, { text, onChange }) {
  let applying = false;
  const view = new EditorView({
    parent,
    state: EditorState.create({
      doc: text,
      extensions: [
        lineNumbers(), foldGutter(), lintGutter(), history(), drawSelection(), indentOnInput(), bracketMatching(),
        highlightActiveLine(), highlightActiveLineGutter(), yaml(), syntaxHighlighting(highlight), theme,
        EditorState.tabSize.of(2),
        keymap.of([indentWithTab, ...foldKeymap, ...historyKeymap, ...defaultKeymap]),
        EditorView.contentAttributes.of({ 'aria-label': 'Configuration YAML', spellcheck: 'false' }),
        EditorView.updateListener.of((update) => { if (update.docChanged && !applying) onChange(update.state.doc.toString()); }),
      ],
    }),
  });
  return {
    getText: () => view.state.doc.toString(),
    setText(next) {
      if (next === view.state.doc.toString()) return;
      applying = true;
      view.dispatch({ changes: { from: 0, to: view.state.doc.length, insert: next } });
      applying = false;
    },
    // problems: [{ message, path, line, column }]
    setProblems(problems) {
      const doc = view.state.doc;
      const diagnostics = [];
      for (const p of problems) {
        let from = 0, to = 0;
        if (p.line) {
          const line = doc.line(Math.min(Math.max(p.line, 1), doc.lines));
          from = Math.min(line.from + Math.max((p.column || 1) - 1, 0), line.to);
          to = Math.max(line.to, from);
        } else if (p.path) {
          const at = locate(doc.toString(), p.path);
          if (at) { from = at.from; to = at.to; }
        }
        diagnostics.push({ from, to: Math.min(to, doc.length), severity: 'error', message: p.message });
      }
      view.dispatch(setDiagnostics(view.state, diagnostics));
    },
    reveal(offset) {
      view.dispatch({ selection: { anchor: Math.min(offset, view.state.doc.length) }, effects: EditorView.scrollIntoView(offset, { y: 'center' }) });
      view.focus();
    },
    focus: () => view.focus(),
    destroy: () => view.destroy(),
  };
}

// Edits the YAML text through its document, so comments and layout survive. `change`
// receives the Document and edits it; returns the new text, or the old if it cannot parse.
function edit(text, change) {
  const doc = YAML.parseDocument(text);
  if (doc.errors.length) return text;
  change(doc);
  return doc.toString({ lineWidth: 0 });
}

function parse(text) {
  const doc = YAML.parseDocument(text);
  return doc.errors.length ? null : doc.toJS();
}

window.ZissouEditor = { createEditor, edit, parse, locate, parsePath, YAML };
