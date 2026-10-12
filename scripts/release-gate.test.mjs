// Tests of the release gate (release-only-app-changes, issue #22): scripts/ci/app-changed.sh, which decides whether a
// range changes the app, and scripts/release-notes.sh, which leaves out changes archived without an app change. Each
// runs in a throwaway git repository with the scripts copied in. Run with: node --test 'scripts/*.test.mjs'
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { mkdtempSync, mkdirSync, writeFileSync, copyFileSync, rmSync, chmodSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join, dirname } from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const scripts = dirname(fileURLToPath(import.meta.url));

/** A throwaway repository with the release scripts and a first commit holding a little of everything. */
function repo() {
  const root = mkdtempSync(join(tmpdir(), 'release-gate-'));
  for (const script of ['release-notes.sh', 'ci/app-changed.sh']) {
    mkdirSync(dirname(join(root, 'scripts', script)), { recursive: true });
    copyFileSync(join(scripts, script), join(root, 'scripts', script));
    chmodSync(join(root, 'scripts', script), 0o755);
  }
  git(root, 'init', '-q');
  commit(root, 'initial', { 'shared/src/commonMain/App.kt': 'app', 'docs/README.md': 'docs', 'README.md': 'readme' });
  return root;
}

function git(root, ...args) {
  const result = spawnSync('git', ['-c', 'user.name=t', '-c', 'user.email=t@t', ...args], { cwd: root, encoding: 'utf8' });
  assert.equal(result.status, 0, `git ${args.join(' ')}: ${result.stderr}`);
  return result.stdout.trim();
}

/** Writes [files] (path -> content) and commits them; returns the commit. */
function commit(root, message, files) {
  for (const [path, content] of Object.entries(files)) {
    mkdirSync(dirname(join(root, path)), { recursive: true });
    writeFileSync(join(root, path), content);
  }
  git(root, 'add', '-A');
  git(root, 'commit', '-q', '-m', message);
  return git(root, 'rev-parse', 'HEAD');
}

function sh(root, script, ...args) {
  const result = spawnSync('bash', [join(root, 'scripts', script), ...args], { cwd: root, encoding: 'utf8' });
  return { status: result.status, output: result.stdout + result.stderr, stdout: result.stdout };
}

/** Whether one commit changing [path] changes the app, by app-changed.sh's exit status. */
function changesApp(path) {
  const root = repo();
  const sha = commit(root, `change ${path}`, { [path]: 'changed' });
  const { status } = sh(root, 'ci/app-changed.sh', `${sha}^1`, sha);
  rmSync(root, { recursive: true, force: true });
  return status;
}

// ---- app-changed.sh: what counts as the app

for (const path of [
  'shared/src/commonMain/App.kt',
  'shared/src/androidMain/Platform.kt',
  'shared/src/commonMain/sqldelight/migrations/13.sqm',
  'shared/build.gradle.kts',
  'androidApp/src/main/AndroidManifest.xml',
  'androidApp/src/production/res/values/strings.xml',
  'gradle/libs.versions.toml',
  'gradle/wrapper/gradle-wrapper.properties',
  'build.gradle.kts',
  'settings.gradle.kts',
  'gradle.properties',
]) {
  test(`a change to ${path} changes the app`, () => assert.equal(changesApp(path), 0));
}

for (const path of [
  'shared/src/commonTest/AppTest.kt',
  'shared/src/androidHostTest/StorageTest.kt',
  'shared/src/iosMain/Platform.kt',
  'shared/src/iosTest/PlatformTest.kt',
  'androidApp/src/fake/FakeAuth.kt',
  'androidApp/src/testFake/FakeAuthTest.kt',
  'docs/README.md',
  'openspec/specs/widgets/spec.md',
  '.github/workflows/pr-check.yml',
  'scripts/validate-specs.sh',
  'maestro/distance/log-distance.yaml',
  'config/detekt/detekt.yml',
  'iosApp/README.md',
  'README.md',
]) {
  test(`a change only to ${path} does not change the app`, () => assert.equal(changesApp(path), 1));
}

test('a range changes the app when any commit in it does', () => {
  const root = repo();
  const from = git(root, 'rev-parse', 'HEAD');
  commit(root, 'docs', { 'docs/README.md': 'more docs' });
  commit(root, 'app', { 'shared/src/commonMain/App.kt': 'more app' });
  const to = commit(root, 'docs again', { 'docs/README.md': 'even more docs' });
  assert.equal(sh(root, 'ci/app-changed.sh', from, to).status, 0);
  rmSync(root, { recursive: true, force: true });
});

test('with no previous distribution, the whole history counts', () => {
  const root = repo();
  assert.equal(sh(root, 'ci/app-changed.sh', '', 'HEAD').status, 0);
  rmSync(root, { recursive: true, force: true });
});

test('a commit that does not exist is an error, not "unchanged"', () => {
  const root = repo();
  assert.equal(sh(root, 'ci/app-changed.sh', 'no-such-commit', 'HEAD').status, 2);
  rmSync(root, { recursive: true, force: true });
});

// ---- release-notes.sh: archived changes, only when their archiving commit changed the app

const archive = (name) => ({ [`openspec/changes/archive/${name}/proposal.md`]: '# Proposal' });

test('a change archived together with app code is listed', () => {
  const root = repo();
  commit(root, 'feature', { ...archive('2026-10-12-add-widgets'), 'shared/src/commonMain/Widgets.kt': 'widgets' });
  const { status, stdout } = sh(root, 'release-notes.sh', 'HEAD');
  rmSync(root, { recursive: true, force: true });
  assert.equal(status, 0);
  assert.equal(stdout.trim(), '2026-10-12-add-widgets');
});

test('a change archived without app code is left out', () => {
  const root = repo();
  commit(root, 'tooling', { ...archive('2026-10-12-ratchet-docs'), 'docs/README.md': 'tooling docs' });
  const { status, stdout } = sh(root, 'release-notes.sh', 'HEAD');
  rmSync(root, { recursive: true, force: true });
  assert.equal(status, 0);
  assert.equal(stdout.trim(), '');
});

test('only changes archived since the last distribution count, and only the ones that changed the app', () => {
  const root = repo();
  commit(root, 'old feature', { ...archive('2026-10-01-old'), 'shared/src/commonMain/Old.kt': 'old' });
  git(root, 'tag', 'dist-v3-old');
  commit(root, 'tooling', archive('2026-10-12-tooling'));
  commit(root, 'feature', { ...archive('2026-10-12-feature'), 'androidApp/src/main/Feature.kt': 'feature' });
  const { stdout } = sh(root, 'release-notes.sh', 'HEAD');
  rmSync(root, { recursive: true, force: true });
  assert.equal(stdout.trim(), '2026-10-12-feature');
});
