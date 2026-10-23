#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Integration checks against owned JVM sample apps, without browser automation."""
import argparse
import json
import os
from pathlib import Path
import socket
import subprocess
import sys
import tempfile

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--cli', required=True)
parser.add_argument('--app', required=True)
args = parser.parse_args()
Path('tmp').mkdir(exist_ok=True)
root = Path(tempfile.mkdtemp(prefix='retest-ci-', dir='tmp'))
scenario = {'kind': 'reqover-test-scenario', 'schemaVersion': 1, 'name': 'CI fixture',
            'environment': 'local', 'revision': 'fixture', 'savedAt': '2026-10-23T00:00:00Z',
            'cases': [{'id': 'order', 'method': 'GET', 'path': '/auto/orders/42',
                       'expectedStatus': 200, 'maximumClientMillis': None, 'reviewed': True}]}
fixture = root / 'scenario.json'
fixture.write_text(json.dumps(scenario), encoding='utf-8')


def run(name, expected, options=(), value=None):
    if value is not None:
        fixture.write_text(json.dumps(value), encoding='utf-8')
    with socket.socket() as sock:
        sock.bind(('127.0.0.1', 0))
        port = sock.getsockname()[1]
    out = root / name
    env = dict(os.environ, GITHUB_OUTPUT=str(root / (name + '.outputs')),
               GITHUB_STEP_SUMMARY=str(root / (name + '.md')), GITHUB_SHA='a' * 40)
    result = subprocess.run([sys.executable, 'scripts/run-retest-ci.py', '--cli', args.cli,
                             '--app-jar', args.app, '--scenario', str(fixture), '--port', str(port),
                             '--out', str(out), *options], capture_output=True, text=True, timeout=100, env=env)
    assert result.returncode == expected, (name, result.returncode, result.stdout, result.stderr)
    summary = json.loads((out / 'summary.json').read_text(encoding='utf-8'))
    assert summary['exitCode'] == expected
    assert f'exit-code={expected}' in (root / (name + '.outputs')).read_text(encoding='utf-8')
    assert summary['outcome'] in (root / (name + '.md')).read_text(encoding='utf-8')
    with socket.socket() as sock:
        sock.settimeout(.5)
        assert sock.connect_ex(('127.0.0.1', port)) != 0, 'Owned application still listening'
    print('PASS', name, summary['outcome'], flush=True)
    return out, summary


_, denied = run('unconfirmed', 2)
assert denied['outcome'] == 'INPUT_ERROR'
assert not (root / 'unconfirmed/app.log').exists(), 'No application starts without confirmation'
before, passed = run('pass', 0, ['--confirm-local'])
assert passed['passed'] == 1 and (before / 'result.html').is_file()
assert json.loads((before / 'result.json').read_text(encoding='utf-8'))['scenario']['revision'] == 'a' * 40
bad = json.loads(json.dumps(scenario)); bad['cases'][0]['expectedStatus'] = 201
failed, mismatch = run('mismatch', 1, ['--confirm-local', '--baseline', str(before / 'result.json')], bad)
assert mismatch['outcomes']['STATUS_MISMATCH'] == 1 and (failed / 'comparison.html').is_file()
assert 'differ' in (failed / 'comparison.html').read_text(encoding='utf-8').lower()
slow = json.loads(json.dumps(scenario)); slow['cases'][0].update(path='/auto/diagnostics/delay/120', maximumClientMillis=1)
_, limited = run('client-limit', 1, ['--confirm-local'], slow)
assert limited['outcomes']['CLIENT_LIMIT_EXCEEDED'] == 1
invalid = root / 'invalid.jar'; invalid.write_text('not a jar', encoding='utf-8')
_, startup = run('startup-failure', 2, ['--confirm-local', '--app-jar', str(invalid)], scenario)
assert startup['outcome'] == 'STARTUP_FAILED'
_, baseline = run('invalid-baseline', 2, ['--confirm-local', '--baseline', str(fixture)])
assert baseline['outcome'] == 'INPUT_ERROR'
assert not (root / 'invalid-baseline/app.log').exists(), 'Validate baseline before starting application'
existing = root / 'existing'; existing.mkdir()
blocked = subprocess.run([sys.executable, 'scripts/run-retest-ci.py', '--cli', args.cli, '--app-jar', args.app,
                          '--scenario', str(fixture), '--out', str(existing), '--confirm-local'], capture_output=True, text=True)
assert blocked.returncode == 2 and not list(existing.iterdir()), 'Existing output directories must not be overwritten'
print('PASS existing-output OUTPUT_ERROR', flush=True)
print('PASS seven CI runner integration cases; owned sample processes stopped', flush=True)
