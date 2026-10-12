// Tests of the spec validation (ratchet-spec-validation): scripts/check-requirement-length.mjs and
// scripts/validate-specs.sh, each run in a throwaway OpenSpec tree with one main spec and at most one change.
// Run with: node --test scripts/
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const scripts = dirname(fileURLToPath(import.meta.url));

/** Requirement text of exactly [length] characters, with the SHALL OpenSpec requires. */
function text(length) {
  const head = 'The system SHALL ';
  return head + 'x'.repeat(length - head.length);
}

function requirement(name, length, scenarios = ['Works']) {
  return [
    `### Requirement: ${name}`,
    text(length),
    '',
    ...scenarios.flatMap((s) => [`#### Scenario: ${s}`, '- **WHEN** it is used', '- **THEN** it works', '']),
  ].join('\n');
}

/**
 * A throwaway OpenSpec project: a main spec "widgets" with one requirement "Shows widgets" of [mainLength] characters,
 * and, unless [change] is null, a change "c" with the given delta sections and `.openspec.yaml` lines.
 */
function tree({ mainLength, change = null }) {
  const root = mkdtempSync(join(tmpdir(), 'spec-validation-'));
  const write = (path, content) => {
    mkdirSync(dirname(join(root, path)), { recursive: true });
    writeFileSync(join(root, path), content);
  };
  write('openspec/config.yaml', 'schema: spec-driven\n');
  write(
    'openspec/specs/widgets/spec.md',
    `# widgets Specification\n\n## Purpose\nShows the widgets the user has, so that they can find one quickly.\n\n## Requirements\n\n${requirement('Shows widgets', mainLength)}`,
  );
  if (change) {
    write('openspec/changes/c/.openspec.yaml', ['schema: spec-driven', 'created: 2026-10-12', ...(change.yaml ?? [])].join('\n') + '\n');
    write('openspec/changes/c/proposal.md', '# Proposal\n\n## Why\n\nA test change for the spec validation.\n\n## What Changes\n\n- Changes widgets.\n');
    write('openspec/changes/c/specs/widgets/spec.md', `# Spec Delta\n\n${change.delta}\n`);
  }
  return root;
}

function run(root, command, args) {
  const result = spawnSync(command, args, { cwd: root, encoding: 'utf8' });
  rmSync(root, { recursive: true, force: true });
  return { status: result.status, output: result.stdout + result.stderr };
}

const ratchet = (root) => run(root, 'node', [join(scripts, 'check-requirement-length.mjs')]);
const validation = (root) => run(root, 'bash', [join(scripts, 'validate-specs.sh'), root]);

const modified = (length, scenarios) => `## MODIFIED Requirements\n\n${requirement('Shows widgets', length, scenarios)}`;

// ---- The ratchet: a long requirement can only get shorter

test('a long requirement that grows fails, naming both lengths', () => {
  const { status, output } = ratchet(tree({ mainLength: 600, change: { delta: modified(650) } }));
  assert.equal(status, 1);
  assert.match(output, /widgets "Shows widgets" is 650 characters, longer than its 600/);
});

test('a short requirement that grows past the limit fails', () => {
  assert.equal(ratchet(tree({ mainLength: 450, change: { delta: modified(520) } })).status, 1);
});

test('a long requirement that shrinks passes', () => {
  assert.equal(ratchet(tree({ mainLength: 900, change: { delta: modified(700) } })).status, 0);
});

test('a long requirement with only a scenario added passes', () => {
  assert.equal(ratchet(tree({ mainLength: 900, change: { delta: modified(900, ['Works', 'Also works']) } })).status, 0);
});

test('a short requirement that stays short passes', () => {
  assert.equal(ratchet(tree({ mainLength: 300, change: { delta: modified(480) } })).status, 0);
});

test('no active change passes', () => {
  assert.equal(ratchet(tree({ mainLength: 900 })).status, 0);
});

// ---- The whole validation: strict for changes, lenient for the main specs

test('a change adding a requirement over 500 characters fails', () => {
  const delta = `## ADDED Requirements\n\n${requirement('Sorts widgets', 520)}`;
  const { status, output } = validation(tree({ mainLength: 300, change: { delta } }));
  assert.notEqual(status, 0);
  assert.match(output, /Sorts widgets.*very long/);
});

test("a change whose .openspec.yaml has an unknown key fails", () => {
  const delta = `## ADDED Requirements\n\n${requirement('Sorts widgets', 200)}`;
  const { status, output } = validation(tree({ mainLength: 300, change: { delta, yaml: ['colour: blue'] } }));
  assert.notEqual(status, 0);
  assert.match(output, /Unrecognized key.*colour/);
});

test('a main spec with a long requirement and no change passes, showing the warning', () => {
  const { status, output } = validation(tree({ mainLength: 600 }));
  assert.equal(status, 0);
  assert.match(output, /very long/);
});

test('a long requirement that grows fails the whole validation too', () => {
  const { status, output } = validation(tree({ mainLength: 600, change: { delta: modified(650) } }));
  assert.notEqual(status, 0);
  assert.match(output, /longer than its 600/);
});

test('a valid change passes the whole validation', () => {
  const delta = `## ADDED Requirements\n\n${requirement('Sorts widgets', 200)}`;
  assert.equal(validation(tree({ mainLength: 600, change: { delta } })).status, 0);
});
