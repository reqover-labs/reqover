/*
 * Copyright 2026 Reqover contributors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

/* Run against synthetic sample reports only. Requires the Playwright package. */
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { chromium } = require('playwright');

(async () => {
  const [report, legacy, out = 'tmp/dashboard-check'] = process.argv.slice(2);
  assert(report && legacy, 'Usage: node scripts/verify-dashboard.cjs report.html legacy.html [screenshot-directory]');
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch({ headless: true });
  const errors = [];
  const external = [];
  try {
    const page = await browser.newPage({ viewport: { width: 1440, height: 1000 }, colorScheme: 'light' });
    page.on('pageerror', e => errors.push(e.message));
    page.on('request', r => { if (/^https?:/.test(r.url())) external.push(r.url()); });
    await page.goto(pathToFileURL(path.resolve(report)).href);
    await page.locator('#reqover-workspace:not([hidden])').waitFor();
    assert(await page.locator('.graph-node').count() > 0, 'Graph must not be blank');
    const requestCount = await page.locator('details.request-detail').count();
    assert(requestCount >= 5, 'Record success, 400, 503, delay, and shared code for this check');
    await page.screenshot({ path: path.join(out, 'reqover-request-diagnostics.png') });

    await page.getByRole('button', { name: 'Create test draft', exact: true }).filter({ visible: true }).first().click();
    await page.locator('#test-case-drafts').waitFor({ state: 'visible' });
    assert.equal(await page.locator('#reqover-case-expected-status').inputValue(), '', 'Observed failure must not become the expectation');
    assert.equal(await page.locator('#reqover-case-path').inputValue(), '');
    assert.equal(await page.locator('#reqover-case-max-ms').inputValue(), '');
    assert(await page.locator('#reqover-case-junit').isDisabled());
    await page.locator('#reqover-case-path').fill('/orders/{id}');
    await page.locator('#reqover-case-expected-status').fill('200');
    await page.locator('#reqover-case-reviewed').check();
    assert(await page.locator('#reqover-case-junit').isDisabled(), 'Unresolved route cannot generate an executable draft');
    await page.locator('#reqover-case-path').fill('/auto/diagnostics/failure');
    assert(!await page.locator('#reqover-case-reviewed').isChecked(), 'Editing resets prior review');
    await page.locator('#reqover-case-max-ms').fill('1500');
    await page.locator('#reqover-case-reviewed').check();
    assert(!await page.locator('#reqover-case-junit').isDisabled());
    let exportPromise = page.waitForEvent('download');
    await page.locator('#reqover-case-junit').click();
    let exported = await exportPromise;
    assert.equal(exported.suggestedFilename(), 'ReqoverCase1Test.java');
    await exported.saveAs(path.join(out, 'ReqoverCase1Test.java'));
    const java = fs.readFileSync(path.join(out, 'ReqoverCase1Test.java'), 'utf8');
    assert(java.includes('@Disabled(') && java.includes('assertEquals(200, response.statusCode())'));
    assert(!java.includes('assertEquals(503'));
    exportPromise = page.waitForEvent('download');
    await page.locator('#reqover-case-json').click();
    exported = await exportPromise;
    await exported.saveAs(path.join(out, 'case.json'));
    const draft = JSON.parse(fs.readFileSync(path.join(out, 'case.json'), 'utf8'));
    assert.equal(draft.source.observedStatus, 503);
    assert.equal(draft.expected.statusCode, 200);
    assert.equal(draft.replayable, false);
    assert.equal(draft.kind, 'reqover-http-test-draft');
    await page.locator('.case-code summary').click();
    await page.screenshot({ path: path.join(out, 'reqover-test-case-draft.png') });
    await page.setViewportSize({ width: 390, height: 900 });
    const caseWidth = await page.evaluate(() => ({ scroll: document.documentElement.scrollWidth, client: document.documentElement.clientWidth }));
    assert(caseWidth.scroll <= caseWidth.client, 'Draft editor must not overflow mobile');
    await page.screenshot({ path: path.join(out, 'reqover-test-case-mobile.png') });
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.locator('.dashboard-link[href="#request-overview"]').click();
    await page.locator('#request-overview').waitFor({ state: 'visible' });

    const currentSummary = await page.locator('#reqover-comparison-data').textContent().then(JSON.parse);
    const syntheticBaseline = structuredClone(currentSummary);
    syntheticBaseline.endpoints.forEach(row => {
      if (row.timedRequestCount) {
        row.averageMillis *= 2; row.p95Millis *= 2; row.maximumMillis *= 2; row.cumulativeMillis *= 2;
      }
    });
    await page.locator('.dashboard-link[href="#recording-comparison"]').click();
    await page.locator('#recording-comparison').waitFor({ state: 'visible' });
    await page.locator('#reqover-baseline-file').setInputFiles({ name: 'synthetic-baseline.json',
      mimeType: 'application/json', buffer: Buffer.from(JSON.stringify(syntheticBaseline)) });
    await page.locator('#reqover-comparison-results:not([hidden])').waitFor();
    assert.equal(await page.locator('#reqover-comparison-rows tr').count(), currentSummary.endpoints.length);
    assert(await page.locator('#reqover-comparison-context').innerText().then(t => t.includes('No automatic performance verdict')));
    await page.locator('#reqover-comparison-confirmed').check();
    assert(await page.locator('#reqover-comparison-context').innerText().then(t => t.includes('Still no automatic')));
    await page.locator('#reqover-comparison-confirmed').uncheck();
    await page.locator('#reqover-comparison-filter').fill('/payments');
    assert.equal(await page.locator('#reqover-comparison-rows tr').count(), 1);
    await page.locator('#reqover-comparison-filter').fill('no-such-endpoint');
    assert(await page.locator('#reqover-comparison-no-match').isVisible());
    await page.locator('#reqover-comparison-filter').fill('');
    await page.screenshot({ path: path.join(out, 'reqover-recording-comparison.png') });
    await page.setViewportSize({ width: 390, height: 900 });
    const comparisonWidth = await page.evaluate(() => ({ scroll: document.documentElement.scrollWidth, client: document.documentElement.clientWidth }));
    assert(comparisonWidth.scroll <= comparisonWidth.client, 'Comparison view must not overflow mobile');
    await page.screenshot({ path: path.join(out, 'reqover-comparison-mobile.png') });
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.locator('#reqover-baseline-file').setInputFiles({ name: 'wrong.json', mimeType: 'application/json', buffer: Buffer.from('{"kind":"reqover-http-test-draft"}') });
    await page.locator('#reqover-comparison-error:not([hidden])').waitFor();
    assert.equal(await page.locator('#reqover-baseline-name').innerText(), 'synthetic-baseline.json', 'Bad imports must preserve the last valid baseline');
    const summaryDownload = page.waitForEvent('download');
    await page.locator('#reqover-comparison-export').click();
    const summaryFile = await summaryDownload;
    await summaryFile.saveAs(path.join(out, 'current-summary.json'));
    const exportedSummary = JSON.parse(fs.readFileSync(path.join(out, 'current-summary.json'), 'utf8'));
    assert.equal(exportedSummary.kind, 'reqover-recorded-summary');
    assert.equal(exportedSummary.httpRequestCount, requestCount);
    assert(!Object.hasOwn(exportedSummary, 'requests'), 'Summary export must not contain raw requests');
    await page.getByRole('button', { name: 'Clear baseline', exact: true }).click();
    assert(await page.locator('#reqover-comparison-empty').isVisible());
    assert(!await page.locator('#reqover-comparison-confirmed').isChecked());
    await page.locator('#reqover-baseline-file').setInputFiles({ name: 'legacy.json', mimeType: 'application/json',
      buffer: Buffer.from(JSON.stringify({ schemaVersion: 1, generatedAt: currentSummary.generatedAt,
        completedRequestCount: 0, endpoints: [], reverseIndex: [] })) });
    await page.locator('#reqover-comparison-results:not([hidden])').waitFor();
    assert(await page.locator('#reqover-comparison-notices').innerText().then(t => t.includes('no retained HTTP diagnostics')));
    assert(await page.locator('#reqover-comparison-rows').innerText().then(t => t.includes('Not comparable')));
    await page.locator('.dashboard-link[href="#request-overview"]').click();
    await page.locator('#request-overview').waitFor({ state: 'visible' });

    await page.getByRole('button', { name: 'Retest map', exact: true }).click();
    const methodOption = await page.locator('#reqover-map-selection option').evaluateAll(options =>
      options.find(o => o.textContent.includes('SharedValidator#validate'))?.value);
    assert.notEqual(methodOption, undefined, 'The synthetic report must contain shared code');
    await page.locator('#reqover-map-selection').selectOption(methodOption);
    assert.equal(await page.locator('[data-node-kind="endpoint"]').count(), 2);
    await page.locator('[data-node-kind="method"]').click();
    assert.equal(await page.locator('#reqover-inspector .inspector-endpoint').count(), 2);
    assert(await page.locator('#reqover-inspector').innerText().then(t => t.includes('Unobserved callers')));
    const animated = page.locator('.graph-edge.association').first();
    const first = await animated.evaluate(e => getComputedStyle(e).strokeDashoffset);
    await page.waitForTimeout(220);
    const second = await animated.evaluate(e => getComputedStyle(e).strokeDashoffset);
    assert.notEqual(first, second, 'Association animation must move');
    const frame1 = await page.locator('#reqover-graph-stage').screenshot();
    await page.waitForTimeout(220);
    const frame2 = await page.locator('#reqover-graph-stage').screenshot();
    assert(!frame1.equals(frame2), 'Animated graph pixels must change');
    await page.getByRole('button', { name: 'Pause animation', exact: true }).click();
    const paused = await animated.evaluate(e => getComputedStyle(e).strokeDashoffset);
    await page.waitForTimeout(220);
    assert.equal(paused, await animated.evaluate(e => getComputedStyle(e).strokeDashoffset));
    await page.getByRole('button', { name: 'Zoom in', exact: true }).click();
    const transform = await page.locator('#reqover-graph-plane').evaluate(e => e.style.transform);
    await page.getByRole('button', { name: 'Zoom out', exact: true }).click();
    assert.notEqual(transform, await page.locator('#reqover-graph-plane').evaluate(e => e.style.transform));
    await page.getByRole('button', { name: 'Fit graph', exact: true }).click();
    await page.screenshot({ path: path.join(out, 'reqover-retest-map.png') });

    await page.locator('[data-node-kind="endpoint"]').first().click();
    assert(await page.locator('#reqover-inspector .inspector-label').innerText().then(t => t.includes('RETEST')));
    await page.locator('#reqover-inspector .inspector-endpoint').click();
    assert(await page.locator('#endpoint-code').isVisible());
    assert.equal(await page.locator('article.endpoint:visible').count(), 1);
    await page.locator('#reqover-filter').fill('');

    await page.locator('.dashboard-link[href="#request-list"]').click();
    await page.locator('#request-list').waitFor({ state: 'visible' });
    await page.locator('#reqover-request-mode').selectOption('failure');
    const statuses = await page.locator('details.request-detail:visible').evaluateAll(ds => ds.map(d => Number(d.dataset.requestStatus)));
    assert(statuses.length >= 2 && statuses.every(s => s >= 400 && s <= 599));
    await page.locator('details.request-detail:visible summary').first().click();
    assert.equal(await page.locator('details.request-detail[open]:visible').count(), 1);
    await page.screenshot({ path: path.join(out, 'reqover-request-detail.png') });
    await page.locator('details.request-detail[open]:visible').getByRole('button', { name: 'Create test draft', exact: true }).click();
    await page.locator('#test-case-drafts').waitFor({ state: 'visible' });
    assert.equal(await page.locator('#reqover-case-list button').count(), 2);
    assert.equal(await page.locator('#reqover-case-expected-status').inputValue(), '', 'A new draft must not inherit expectations');
    await page.locator('#reqover-case-list button').first().click();
    assert.equal(await page.locator('#reqover-case-expected-status').inputValue(), '200');
    await page.locator('.dashboard-link[href="#request-list"]').click();
    await page.locator('#request-list').waitFor({ state: 'visible' });
    await page.locator('#reqover-request-mode').selectOption('slow');
    assert(await page.locator('details.request-detail:visible').count() >= 1);
    await page.locator('#reqover-slow-ms').fill('100000');
    assert.equal(await page.locator('details.request-detail:visible').count(), 0);
    assert(await page.locator('#reqover-no-request').isVisible());
    await page.locator('#reqover-slow-ms').fill('1000');
    await page.locator('#reqover-request-mode').selectOption('all');
    await page.locator('#reqover-filter').fill('failure');
    assert.equal(await page.locator('details.request-detail:visible').count(), 1);
    await page.locator('#reqover-filter').fill('');

    await page.locator('.dashboard-link[href="#ci-report"]').click();
    await page.locator('#ci-report').waitFor({ state: 'visible' });
    assert(await page.getByText('Not determined by recorded coverage', { exact: true }).isVisible());
    const downloadPromise = page.waitForEvent('download');
    await page.getByRole('button', { name: 'Download HTML report', exact: true }).click();
    const download = await downloadPromise;
    assert.equal(download.suggestedFilename(), 'reqover-report.html');
    await download.saveAs(path.join(out, 'downloaded-report.html'));
    const downloaded = fs.readFileSync(path.join(out, 'downloaded-report.html'), 'utf8');
    const iconLicense = fs.readFileSync(path.resolve(__dirname, '../reqover-report/src/main/resources/io/reqover/report/dashboard/icons/LICENSE'), 'utf8').replace(/--/g, '- -');
    assert(downloaded.includes(iconLicense), 'Downloaded HTML must include the full Lucide/Feather notices');

    await page.locator('.dashboard-link[href="#request-overview"]').click();
    await page.locator('#request-overview').waitFor({ state: 'visible' });
    for (const width of [1920, 1280, 760, 390]) {
      await page.setViewportSize({ width, height: 900 });
      await page.evaluate(() => window.scrollTo(0, 0));
      const geometry = await page.evaluate(() => ({ scroll: document.documentElement.scrollWidth, client: document.documentElement.clientWidth }));
      assert(geometry.scroll <= geometry.client, 'Page overflow at ' + width + ': ' + JSON.stringify(geometry));
    }
    await page.screenshot({ path: path.join(out, 'reqover-dashboard-mobile.png') });
    await page.emulateMedia({ colorScheme: 'dark' });
    await page.screenshot({ path: path.join(out, 'reqover-dashboard-mobile-dark.png') });
    await page.setViewportSize({ width: 1440, height: 1000 });
    await page.screenshot({ path: path.join(out, 'reqover-dashboard-dark.png') });
    await page.emulateMedia({ reducedMotion: 'reduce' });
    await page.waitForFunction(() => document.getElementById('reqover-animation').disabled);
    assert(await page.locator('#reqover-animation').isDisabled());
    assert.equal(await animated.evaluate(e => getComputedStyle(e).animationName), 'none');

    await page.goto(pathToFileURL(path.resolve(out, 'downloaded-report.html')).href);
    assert(await page.locator('#reqover-workspace').isVisible(), 'Downloaded HTML must initialize again');
    await page.goto(pathToFileURL(path.resolve(legacy)).href);
    assert(await page.getByText('No per-request diagnostics.', { exact: false }).isVisible());
    assert.equal(await page.locator('.dashboard-link[href="#request-list"]').count(), 0);
    assert.equal(await page.locator('.dashboard-link[href="#test-case-drafts"]').count(), 0);
    assert(await page.locator('[data-map-mode="request"]').isDisabled());
    assert.equal(await page.locator('[data-map-mode="retest"]').getAttribute('aria-pressed'), 'true');
    assert.equal(external.length, 0, 'Standalone reports must not request external assets');
    assert.deepEqual(errors, [], 'Browser errors');

    const fallback = await browser.newPage({ javaScriptEnabled: false });
    await fallback.goto(pathToFileURL(path.resolve(report)).href);
    assert(await fallback.locator('#endpoint-code').isVisible());
    assert(await fallback.locator('#request-list').isVisible());
    assert(await fallback.locator('#reqover-workspace').isHidden());
    assert(await fallback.locator('.dashboard-link[href="#test-case-drafts"]').isHidden());
    assert(await fallback.locator('.dashboard-link[href="#recording-comparison"]').isHidden());
    console.log(JSON.stringify({ requestCount, failureStatuses: statuses, graphSelection: true,
      animationPixels: true, zoom: true, filters: true, download: true,
      viewports: [1920, 1280, 760, 390], reducedMotion: true, legacy: true,
      noScriptFallback: true, reviewedTestDrafts: true, recordingComparison: true, externalRequests: external.length, pageErrors: errors }));
  } finally {
    await browser.close();
  }
})().catch(e => { console.error(e); process.exit(1); });
