#!/usr/bin/env node
// The requirement-length ratchet (ratchet-spec-validation): no requirement over 500 characters gets in, and no long one
// grows. OpenSpec length-checks only a change's ADDED requirements, and the main specs are validated without --strict
// while their existing long requirements are split (issue #13), so this covers the rest, in two checks:
//
// 1. Active changes: a MODIFIED requirement over 500 characters must not be longer than the same requirement in the
//    main spec.
// 2. The main specs against the base (the target branch): a requirement over 500 characters must exist in the base's
//    main spec, no shorter. This is what holds once a change is archived (its deltas merged into the main specs, the
//    change no longer active) and for an edit to a main spec made without a change. A renamed long requirement counts
//    as new.
//
// Unchanged or shorter text passes, so a change can touch a long requirement (add a scenario, fix a word) without
// splitting it first. Issue #13's last change deletes this script, once `openspec validate --all --strict` passes again.
//
// It reads OpenSpec's own JSON (`openspec list`, `openspec show`), so a requirement's text is measured exactly as
// OpenSpec's 500-character check measures it. Run from the root of an OpenSpec project in a git repository
// (scripts/validate-specs.sh does). The base is $SPEC_BASE when set (any commit), else the merge base of HEAD and
// origin/main, or HEAD's first parent when HEAD is already on origin/main (a push to main).
//
// Usage: node scripts/check-requirement-length.mjs    (exit 0: nothing grew; 1: something did; 2: could not check)
import { execFileSync } from 'node:child_process';
import { existsSync, mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const LIMIT = 500;

function run(command, args, cwd) {
  return execFileSync(command, args, { cwd, encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
}

function openspec(cwd, ...args) {
  const out = run('openspec', args, cwd);
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

/** Requirement name -> text length of the main spec [spec] in [root], or null when that spec does not exist there. */
function specLengths(root, spec) {
  if (!existsSync(join(root, 'openspec', 'specs', spec, 'spec.md'))) return null;
  const shown = openspec(root, 'show', spec, '--type', 'spec', '--json');
  return new Map(field(shown, 'requirements', `spec ${spec}`).map((r) => [sameName(field(r, 'name', spec)), field(r, 'text', spec).length]));
}

function specNames(root) {
  const dir = join(root, 'openspec', 'specs');
  if (!existsSync(dir)) return [];
  return run('ls', [dir], root).split('\n').filter((name) => name && existsSync(join(dir, name, 'spec.md')));
}

/** Check 1: active changes' MODIFIED requirements against the main specs. */
function grownInChanges(root) {
  const grown = [];
  const mainSpecs = new Map();
  const mainLengths = (spec) => {
    if (!mainSpecs.has(spec)) mainSpecs.set(spec, specLengths(root, spec));
    return mainSpecs.get(spec);
  };
  for (const change of field(openspec(root, 'list', '--json'), 'changes', 'openspec list --json')) {
    const id = field(change, 'name', 'openspec list --json');
    for (const delta of field(openspec(root, 'show', id, '--json', '--deltas-only'), 'deltas', `change ${id}`)) {
      if (field(delta, 'operation', `change ${id}`) !== 'MODIFIED') continue;
      // A change without spec files (skip_specs) gets its proposal's "What Changes" bullets listed as deltas, with no
      // requirement; only a spec delta carries one.
      if (!('requirement' in delta)) continue;
      const spec = field(delta, 'spec', `change ${id}`);
      const requirement = field(delta, 'requirement', `change ${id}`);
      const name = sameName(field(requirement, 'name', `change ${id}`));
      const length = field(requirement, 'text', `change ${id}`).length;
      // No main spec, or no requirement of that name in it: OpenSpec already reports the MODIFIED delta as an error.
      const before = mainLengths(spec)?.get(name);
      if (before === undefined) continue;
      if (length > LIMIT && length > before) {
        grown.push(`${id}: ${spec} "${name}" is ${length} characters, longer than its ${before} in the main spec`);
      }
    }
  }
  return grown;
}

function baseCommit(root) {
  if (process.env.SPEC_BASE) return run('git', ['rev-parse', '--verify', `${process.env.SPEC_BASE}^{commit}`], root).trim();
  const head = run('git', ['rev-parse', 'HEAD'], root).trim();
  let main;
  try {
    main = run('git', ['rev-parse', '--verify', 'origin/main^{commit}'], root).trim();
  } catch {
    throw new Error('no origin/main to compare the main specs with: fetch it, or set SPEC_BASE to the base commit');
  }
  // On main's tip itself (a push to main), what this commit brought is relative to its first parent. Anywhere else,
  // relative to the merge base: for a branch, where it left main; for a checkout behind main, HEAD itself (nothing new),
  // so an already-accepted commit is not judged again.
  if (head === main) return run('git', ['rev-parse', 'HEAD^1'], root).trim();
  return run('git', ['merge-base', 'HEAD', main], root).trim();
}

/** Check 2: the main specs against the base's main specs. */
function grownSinceBase(root) {
  const base = baseCommit(root);
  const baseRoot = mkdtempSync(join(tmpdir(), 'spec-base-'));
  try {
    const archive = execFileSync('git', ['archive', base, 'openspec/specs'], { cwd: root, maxBuffer: 64 * 1024 * 1024 });
    execFileSync('tar', ['-x', '-C', baseRoot], { input: archive });
    const grown = [];
    for (const spec of specNames(root)) {
      const now = specLengths(root, spec);
      const before = specLengths(baseRoot, spec) ?? new Map();
      for (const [name, length] of now) {
        const was = before.get(name);
        if (length <= LIMIT || (was !== undefined && length <= was)) continue;
        grown.push(
          was === undefined
            ? `${spec} "${name}" is ${length} characters and new since ${base.slice(0, 7)}`
            : `${spec} "${name}" is ${length} characters, longer than its ${was} at ${base.slice(0, 7)}`,
        );
      }
    }
    return grown;
  } finally {
    rmSync(baseRoot, { recursive: true, force: true });
  }
}

function main() {
  const root = process.cwd();
  const grown = [...grownInChanges(root), ...grownSinceBase(root)];
  for (const g of grown) {
    console.error(`✗ ${g} (over ${LIMIT}, a requirement may only get shorter: move cases into scenarios or split it).`);
  }
  if (grown.length === 0) console.log(`No requirement over ${LIMIT} characters is new or grew.`);
  return grown.length === 0 ? 0 : 1;
}

try {
  process.exitCode = main();
} catch (error) {
  console.error(`check-requirement-length: ${error.message}`);
  process.exitCode = 2;
}
