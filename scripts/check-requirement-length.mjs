#!/usr/bin/env node
// Fails when an active change makes a long requirement longer (ratchet-spec-validation): a MODIFIED requirement whose
// text is over 500 characters and longer than the same requirement's text in the main spec. OpenSpec length-checks only
// ADDED requirements, so without this a change could grow one of the main specs' long requirements unnoticed. Unchanged
// or shorter text passes, so a change can still touch a long requirement (add a scenario, fix a word) without splitting
// it first. Issue #13's last change deletes this script, once `openspec validate --all --strict` passes again.
//
// It reads OpenSpec's own JSON (`openspec list`, `openspec show`), so a requirement's text is measured exactly as
// OpenSpec's 500-character check measures it. Run from the root of an OpenSpec project (scripts/validate-specs.sh does).
//
// Usage: node scripts/check-requirement-length.mjs    (exit 0: nothing grew; 1: something did; 2: unexpected output)
import { execFileSync } from 'node:child_process';

const LIMIT = 500;

function openspec(...args) {
  const out = execFileSync('openspec', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
  try {
    return JSON.parse(out);
  } catch {
    throw new Error(`openspec ${args.join(' ')} did not print JSON: ${out.slice(0, 200)}`);
  }
}

/** The field, or a loud failure: a changed JSON shape must not pass silently. */
function field(object, name, what) {
  if (object == null || !(name in object)) throw new Error(`openspec output has no "${name}" in ${what}`);
  return object[name];
}

const sameName = (name) => name.replace(/\s+/g, ' ').trim();

function main() {
  const changes = field(openspec('list', '--json'), 'changes', 'openspec list --json');
  const mainSpecs = new Map(); // spec name -> Map(requirement name -> text length), read once each
  const mainLengths = (spec) => {
    if (!mainSpecs.has(spec)) {
      let lengths = null;
      try {
        const shown = openspec('show', spec, '--type', 'spec', '--json');
        lengths = new Map(field(shown, 'requirements', `spec ${spec}`).map((r) => [sameName(field(r, 'name', spec)), field(r, 'text', spec).length]));
      } catch {
        lengths = null; // no main spec: a MODIFIED delta against it is already an OpenSpec error
      }
      mainSpecs.set(spec, lengths);
    }
    return mainSpecs.get(spec);
  };

  const grown = [];
  for (const change of changes) {
    const id = field(change, 'name', 'openspec list --json');
    const deltas = field(openspec('show', id, '--json', '--deltas-only'), 'deltas', `change ${id}`);
    for (const delta of deltas) {
      if (field(delta, 'operation', `change ${id}`) !== 'MODIFIED') continue;
      // A change without spec files (skip_specs) gets its proposal's "What Changes" bullets listed as deltas, with no
      // requirement; only a spec delta carries one.
      if (!('requirement' in delta)) continue;
      const spec = field(delta, 'spec', `change ${id}`);
      const requirement = field(delta, 'requirement', `change ${id}`);
      const name = sameName(field(requirement, 'name', `change ${id}`));
      const length = field(requirement, 'text', `change ${id}`).length;
      const before = mainLengths(spec)?.get(name);
      // No main requirement of that name: OpenSpec already reports the MODIFIED delta as an error.
      if (before === undefined) continue;
      if (length > LIMIT && length > before) grown.push({ id, spec, name, length, before });
    }
  }

  for (const g of grown) {
    console.error(
      `✗ ${g.id}: ${g.spec} "${g.name}" is ${g.length} characters, longer than its ${g.before} in the main spec ` +
        `(over ${LIMIT}, a requirement may only get shorter: move cases into scenarios or split it).`,
    );
  }
  if (grown.length === 0) console.log(`No modified requirement over ${LIMIT} characters grew.`);
  return grown.length === 0 ? 0 : 1;
}

try {
  process.exitCode = main();
} catch (error) {
  console.error(`check-requirement-length: ${error.message}`);
  process.exitCode = 2;
}
