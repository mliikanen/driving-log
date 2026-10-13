// Tests of the release gate (release-only-app-changes, issue #22): scripts/ci/app-changed.sh, which decides whether a
// range changes the app; scripts/ci/release-skips.sh, which records the merges that didn't as dist-skip-v* tags; and
// scripts/release-notes.sh, which leaves out exactly the changes archived by a recorded skip. Each runs in a throwaway
// git repository with the scripts copied in. Run with: node --test 'scripts/*.test.mjs'
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
  for (const script of ['release-notes.sh', 'ci/app-changed.sh', 'ci/release-skips.sh', 'ci/prune-release-skips.sh']) {
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
  'androidApp/src/androidTest/kotlin/MainActivityTest.kt',
  'androidApp/src/androidTestProduction/kotlin/SignInTest.kt',
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

// ---- release-skips.sh: the recorded decisions

const tags = (root) => git(root, 'tag', '-l', 'dist-skip-v*').split('\n').filter(Boolean);
const skipped = (root, sha) => git(root, 'tag', '--points-at', sha, '-l', 'dist-skip-v*') !== '';

test('a merge that changed no app path is recorded as a skip; one that did is not', () => {
  const root = repo();
  git(root, 'tag', 'dist-v1-base');
  const docs = commit(root, 'docs', { 'docs/README.md': 'more docs' });
  const app = commit(root, 'app', { 'shared/src/commonMain/App.kt': 'more app' });
  const { status, stdout } = sh(root, 'ci/release-skips.sh', 'dist-v1-base', 'HEAD');
  assert.equal(status, 0);
  assert.equal(stdout.trim(), tags(root)[0]);
  assert.equal(skipped(root, docs), true);
  assert.equal(skipped(root, app), false);
  rmSync(root, { recursive: true, force: true });
});

test('a recorded decision is not made again', () => {
  const root = repo();
  git(root, 'tag', 'dist-v1-base');
  commit(root, 'docs', { 'docs/README.md': 'more docs' });
  sh(root, 'ci/release-skips.sh', 'dist-v1-base', 'HEAD');
  const second = sh(root, 'ci/release-skips.sh', 'dist-v1-base', 'HEAD');
  assert.equal(second.stdout.trim(), '');
  assert.equal(tags(root).length, 1);
  rmSync(root, { recursive: true, force: true });
});

test('only merges since the last distribution are judged', () => {
  const root = repo();
  commit(root, 'old docs', { 'docs/README.md': 'old docs' });
  git(root, 'tag', 'dist-v2-old');
  commit(root, 'new docs', { 'docs/README.md': 'new docs' });
  sh(root, 'ci/release-skips.sh', 'dist-v2-old', 'HEAD');
  assert.equal(tags(root).length, 1);
  rmSync(root, { recursive: true, force: true });
});

// ---- release-notes.sh: every archived change but those archived by a recorded skip

const archive = (name) => ({ [`openspec/changes/archive/${name}/proposal.md`]: '# Proposal' });

test('a change archived by a recorded skip is left out; the rest are listed', () => {
  const root = repo();
  git(root, 'tag', 'dist-v1-base');
  commit(root, 'tooling', { ...archive('2026-10-12-tooling'), 'docs/README.md': 'tooling docs' });
  commit(root, 'feature', { ...archive('2026-10-12-feature'), 'shared/src/commonMain/Widgets.kt': 'widgets' });
  sh(root, 'ci/release-skips.sh', 'dist-v1-base', 'HEAD');
  const { status, stdout } = sh(root, 'release-notes.sh', 'HEAD');
  rmSync(root, { recursive: true, force: true });
  assert.equal(status, 0);
  assert.equal(stdout.trim(), '2026-10-12-feature');
});

test('a change from a merge never judged is collected, not silently dropped', () => {
  // No release run recorded anything for it (its run failed, say): only an explicit skip leaves notes out.
  const root = repo();
  git(root, 'tag', 'dist-v1-base');
  commit(root, 'tooling', { ...archive('2026-10-12-tooling'), 'docs/README.md': 'tooling docs' });
  const { stdout } = sh(root, 'release-notes.sh', 'HEAD');
  rmSync(root, { recursive: true, force: true });
  assert.equal(stdout.trim(), '2026-10-12-tooling');
});

test('an app merge that was not released is collected into the next release', () => {
  // dist-v1 released; "missed" changed the app but its release failed (no tag); a docs merge was skipped; "next"
  // is released: its notes carry "missed" too, and not the skipped docs merge.
  const root = repo();
  git(root, 'tag', 'dist-v1-base');
  commit(root, 'missed', { ...archive('2026-10-11-missed'), 'androidApp/src/main/Missed.kt': 'missed' });
  commit(root, 'tooling', { ...archive('2026-10-11-tooling'), 'scripts/tool.sh': 'tool' });
  commit(root, 'next', { ...archive('2026-10-12-next'), 'shared/src/commonMain/Next.kt': 'next' });
  sh(root, 'ci/release-skips.sh', 'dist-v1-base', 'HEAD');
  const { stdout } = sh(root, 'release-notes.sh', 'HEAD');
  rmSync(root, { recursive: true, force: true });
  assert.deepEqual(stdout.trim().split('\n'), ['2026-10-11-missed', '2026-10-12-next']);
});

// ---- prune-release-skips.sh: a release deletes the skips it covers, and keeps every release tag

test('a release deletes the skip tags it covers, keeps later ones and every release tag', () => {
  const root = repo();
  git(root, 'tag', 'dist-v1-base');
  commit(root, 'docs', { 'docs/README.md': 'more docs' });
  commit(root, 'app', { 'shared/src/commonMain/App.kt': 'more app' });
  sh(root, 'ci/release-skips.sh', 'dist-v1-base', 'HEAD');
  const covered = tags(root);
  git(root, 'tag', 'dist-v2-release');
  commit(root, 'later docs', { 'docs/README.md': 'later docs' });
  sh(root, 'ci/release-skips.sh', 'dist-v2-release', 'HEAD');
  const later = tags(root).filter((t) => !covered.includes(t));

  const { status, stdout } = sh(root, 'ci/prune-release-skips.sh', 'dist-v2-release');
  assert.equal(status, 0);
  assert.deepEqual(stdout.trim().split('\n'), covered);
  assert.deepEqual(tags(root), later);
  assert.deepEqual(git(root, 'tag', '-l', 'dist-v*').split('\n'), ['dist-v1-base', 'dist-v2-release']);
  rmSync(root, { recursive: true, force: true });
});

test('pruning with nothing to prune changes nothing', () => {
  const root = repo();
  git(root, 'tag', 'dist-v1-base');
  const { status, stdout } = sh(root, 'ci/prune-release-skips.sh', 'dist-v1-base');
  assert.equal(status, 0);
  assert.equal(stdout.trim(), '');
  assert.equal(git(root, 'tag', '-l', 'dist-v*'), 'dist-v1-base');
  rmSync(root, { recursive: true, force: true });
});

test('pruning for a release that does not exist is an error', () => {
  const root = repo();
  assert.equal(sh(root, 'ci/prune-release-skips.sh', 'dist-v9-missing').status, 2);
  rmSync(root, { recursive: true, force: true });
});
