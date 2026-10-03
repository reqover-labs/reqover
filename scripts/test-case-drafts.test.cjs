const assert = require('node:assert/strict');
const test = require('node:test');
const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');
const { spawnSync } = require('node:child_process');
const drafts = require('../reqover-report/src/main/resources/io/reqover/report/dashboard/test-case-drafts.js');

const request = overrides => ({ requestId: 'req-1', unitType: 'http-request', endpoint: 'GET /orders/{id}',
  startedAt: '2026-10-03T00:00:00Z', endedAt: '2026-10-03T00:00:01Z', statusCode: 503, duration: 1200.25,
  classes: [{ className: 'example.OrderService', methods: [{ methodName: 'find', descriptor: '(J)V' }] }], ...overrides });
const reviewed = overrides => drafts.update(drafts.create(request()), {
  path: '/orders/42', expectedStatus: 200, maximumClientMillis: 1500, reviewed: true, ...overrides });

test('keeps source observations separate from unset expectations and uncollected inputs', () => {
  const value = drafts.create(request({ authorization: 'do-not-copy', body: 'do-not-copy' }));
  assert.equal(value.kind, 'reqover-http-test-draft');
  assert.equal(value.replayable, false);
  assert.equal(value.source.observedStatus, 503);
  assert.equal(value.source.recordedProcessingMillis, 1200.25);
  assert.equal(value.request.path, '');
  assert.equal(value.expected.statusCode, null);
  assert.equal(value.expected.maximumClientMillis, null);
  assert.equal(value.observedCode[0], 'example.OrderService#find(J)V');
  assert(!JSON.stringify(value).includes('do-not-copy'));
});

test('rejects non-HTTP observations and preserves unknown final status', () => {
  assert.throws(() => drafts.create(request({ unitType: 'scheduled-job' })), /HTTP/);
  assert.equal(drafts.create(request({ endedAt: null, statusCode: 200 })).source.observedStatus, null);
});

test('requires explicit path, expected status, and review before generating JUnit', () => {
  const original = drafts.create(request());
  assert(drafts.problems(original).length >= 3);
  assert.throws(() => drafts.junit(original), /review/i);
  const complete = reviewed();
  assert.deepEqual(drafts.problems(complete), []);
  assert.equal(complete.state, 'reviewed-draft');
  assert.equal(original.expected.statusCode, null, 'editing must not mutate the source draft');
  assert.equal(complete.replayable, false, 'manual review is not proof of faithful replay');
});

test('blocks authority changes, fragments, queries, unresolved patterns, and malformed paths', () => {
  for (const path of ['https://example.invalid', '//evil.invalid/path', '/\\evil', '/orders/{id}',
    '/**', '/items?q=private', '/items#part', '/space here', '/bad\npath', '/bad%0a', '/bad%', '/%7Bid%7D']) {
    assert(drafts.problems(reviewed({ path })).length > 0, 'must reject ' + JSON.stringify(path));
  }
});

test('does not export mutating requests as executable tests', () => {
  const post = drafts.update(drafts.create(request({ endpoint: 'POST /payments' })),
    { path: '/payments', expectedStatus: 200, reviewed: true });
  assert.equal(post.request.method, 'POST');
  assert.throws(() => drafts.junit(post), /GET.*HEAD/);
});

test('validates final status and client-time limits without borrowing server timing', () => {
  for (const expectedStatus of [0, 100, 600, 200.5, NaN]) assert(drafts.problems(reviewed({ expectedStatus })).length);
  for (const maximumClientMillis of [0, -1, 1.5, 600001, Infinity]) assert(drafts.problems(reviewed({ maximumClientMillis })).length);
  assert.deepEqual(drafts.problems(reviewed({ maximumClientMillis: '' })), []);
});

test('exports disabled Java with explicit target, bounded timeouts and independent assertions', () => {
  const source = drafts.junit(reviewed());
  assert(source.includes('@Disabled('));
  assert(source.includes('System.getProperty("reqover.test.baseUrl")'));
  assert(source.includes('HttpClient.Redirect.NEVER'));
  assert(source.includes('System.nanoTime()'));
  assert(source.includes('assertEquals(200, response.statusCode())'));
  assert(source.includes('Duration.ofMillis(1500L)'));
  assert(!source.includes('assertEquals(503'));
  assert(!source.includes('.header('));
});

test('supports HEAD and omits the optional timing assertion when unset', () => {
  const value = drafts.update(drafts.create(request({ endpoint: 'HEAD /health' })),
    { path: '/health', expectedStatus: 200, reviewed: true });
  const source = drafts.junit(value, 'ReqoverCase2Test');
  assert(source.includes('class ReqoverCase2Test'));
  assert(source.includes('.method("HEAD",'));
  assert(!source.includes('Duration.ofMillis('));
  assert.throws(() => drafts.junit(value, 'Bad";Injected'), /class name/i);
});

test('escapes hostile recorded text without placing it in Java comments', () => {
  const value = drafts.update(drafts.create(request({ requestId: '*/\n";throw new Error(); //\\u000a\0' })),
    { path: '/orders/42', expectedStatus: 200, reviewed: true });
  const source = drafts.junit(value);
  assert(source.includes('\\n\\";throw new Error();'));
  assert(!source.includes('*/\n";throw'));
  assert(source.includes('\\000'));
});

test('generated Java compiles with real JUnit APIs, including hostile Unicode metadata', () => {
  const cache = path.join(process.env.GRADLE_USER_HOME || path.join(os.homedir(), '.gradle'), 'caches/modules-2/files-2.1');
  function jar(group, artifact, version) {
    const root = path.join(cache, group, artifact, version);
    assert(fs.existsSync(root), 'Run the Gradle tests first to resolve ' + artifact);
    for (const hash of fs.readdirSync(root)) {
      const dir = path.join(root, hash);
      for (const file of fs.readdirSync(dir)) {
        if (file === artifact + '-' + version + '.jar') return path.join(dir, file);
      }
    }
    throw Error('Missing JAR: ' + artifact);
  }
  const classpath = [jar('org.junit.jupiter', 'junit-jupiter-api', '5.12.2'),
    jar('org.apiguardian', 'apiguardian-api', '1.1.2'), jar('org.opentest4j', 'opentest4j', '1.3.0'),
    jar('org.junit.platform', 'junit-platform-commons', '1.12.2')].join(path.delimiter);
  const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'reqover-case-compile-'));
  try {
    const normal = reviewed();
    const hostile = drafts.update(drafts.create(request({ requestId: '*/\r\n"; throw new Error(); //\\u000a\0한글',
      classes: [{ className: '</script>Bad"', methods: [{ methodName: '*/unsafe\n', descriptor: '()V' }] }] })),
      { path: '/orders/42', expectedStatus: 200, reviewed: true });
    const files = [normal, hostile].map((value, i) => {
      const name = 'ReqoverCase' + (i + 1) + 'Test';
      const file = path.join(temp, name + '.java');
      fs.writeFileSync(file, drafts.junit(value, name), 'utf8');
      return file;
    });
    const compiled = spawnSync('javac', ['--release', '17', '-encoding', 'UTF-8', '-proc:none',
      '-classpath', classpath, '-d', path.join(temp, 'classes'), ...files], { encoding: 'utf8', timeout: 20000 });
    assert.equal(compiled.status, 0, compiled.stderr || String(compiled.error));
  } finally {
    assert.equal(path.dirname(path.resolve(temp)), path.resolve(os.tmpdir()));
    assert(path.basename(temp).startsWith('reqover-case-compile-'));
    fs.rmSync(temp, { recursive: true, force: true });
  }
});
