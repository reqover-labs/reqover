#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Run a reviewed scenario against an owned loopback JVM and publish CI evidence."""
import argparse
from collections import Counter
import json
import os
from pathlib import Path
import re
import shutil
import signal
import socket
import subprocess
import sys
import tempfile
import time
import urllib.error
import urllib.request


class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, *unused):
        return None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--cli', required=True)
    parser.add_argument('--app-jar', required=True)
    parser.add_argument('--scenario', required=True)
    parser.add_argument('--port', type=int, default=18082)
    parser.add_argument('--ready-path', default='/reqover/report')
    parser.add_argument('--repeat', type=int, default=1)
    parser.add_argument('--baseline')
    parser.add_argument('--revision', default=os.environ.get('GITHUB_SHA'))
    parser.add_argument('--out')
    parser.add_argument('--confirm-local', action='store_true')
    args = parser.parse_args()
    try:
        out = Path(args.out).resolve() if args.out else Path(tempfile.mkdtemp(prefix='reqover-retest-', dir=os.environ.get('RUNNER_TEMP')))
        if args.out:
            out.mkdir(parents=True, exist_ok=False)
    except OSError:
        print(json.dumps({'exitCode': 2, 'outcome': 'OUTPUT_ERROR', 'reason': 'A new writable output directory is required.'}))
        return 2
    summary = {'exitCode': 2, 'outcome': 'INPUT_ERROR', 'passed': 0, 'failed': 0, 'notRun': 0, 'outcomes': {}}
    java = str(Path(os.environ['JAVA_HOME']) / 'bin' / ('java.exe' if os.name == 'nt' else 'java')) if os.environ.get('JAVA_HOME') else shutil.which('java')
    app = None
    flags = subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0

    def cli(arguments, timeout=20):
        return subprocess.run([java, '-jar', str(Path(args.cli).resolve()), *arguments],
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True, timeout=timeout, creationflags=flags)

    try:
        if not args.confirm_local:
            raise ValueError('Explicit --confirm-local is required.')
        if not java or not 1 <= args.port <= 65535 or not 1 <= args.repeat <= 10:
            raise ValueError('Java, port 1-65535 and repeat 1-10 are required.')
        if not re.fullmatch(r'/(?!/)[^?#\x00-\x20\\]*', args.ready_path):
            raise ValueError('Readiness must be a local absolute path without query or fragment.')
        paths = [args.cli, args.app_jar, args.scenario] + ([args.baseline] if args.baseline else [])
        if any(not Path(p).is_file() or '\n' in p or '\r' in p for p in paths) or '\n' in str(out) or '\r' in str(out):
            raise ValueError('Input files must exist and paths cannot contain line breaks.')
        if Path(args.scenario).stat().st_size > 1048576:
            raise ValueError('Scenario exceeds 1 MiB.')
        help_result = cli(['help'])
        if help_result.returncode or 'run-compare' not in help_result.stdout:
            raise ValueError('A source-built CLI with scenario commands is required.')
        if args.baseline:
            checked = cli(['run-report', '--result', str(Path(args.baseline).resolve()), '--out', str(out / 'baseline.html')])
            if checked.returncode:
                raise ValueError('Baseline is not a valid run result.')
        scenario_path = Path(args.scenario).resolve()
        value = json.loads(scenario_path.read_text(encoding='utf-8'))
        if not isinstance(value, dict):
            raise ValueError('Scenario must be a JSON object.')
        if args.revision:
            value['revision'] = args.revision
            scenario_path = out / 'scenario.json'
            scenario_path.write_text(json.dumps(value), encoding='utf-8')
        with socket.socket() as probe:
            probe.bind(('127.0.0.1', args.port))
        summary['outcome'] = 'STARTUP_FAILED'
        with (out / 'app.log').open('wb') as app_log:
            app = subprocess.Popen([java, '-jar', str(Path(args.app_jar).resolve()),
                                    '--server.address=127.0.0.1', f'--server.port={args.port}'],
                                   stdout=app_log, stderr=subprocess.STDOUT, creationflags=flags)
            opener = urllib.request.build_opener(urllib.request.ProxyHandler({}), NoRedirect())
            deadline = time.monotonic() + 60
            while time.monotonic() < deadline:
                if app.poll() is not None:
                    raise RuntimeError('Owned application exited before readiness.')
                try:
                    with opener.open(f'http://127.0.0.1:{args.port}{args.ready_path}', timeout=1) as response:
                        if response.status == 200:
                            break
                except (OSError, urllib.error.URLError):
                    pass
                time.sleep(.25)
            else:
                raise RuntimeError('Application readiness timed out.')
            summary['outcome'] = 'EXECUTION_ERROR'
            result = cli(['run', '--scenario', str(scenario_path), '--base-url', f'http://127.0.0.1:{args.port}',
                          '--confirm-local', '--repeat', str(args.repeat), '--history-dir', str(out / 'runs')], timeout=80)
            if result.returncode not in (0, 1):
                summary['outcome'] = 'INPUT_ERROR'
                raise ValueError('CLI rejected the scenario or could not save its result.')
            files = list((out / 'runs').glob('*.run.json'))
            if len(files) != 1:
                raise RuntimeError('Exactly one saved run result is required.')
            shutil.copyfile(files[0], out / 'result.json')
            shutil.copyfile(files[0].with_name(files[0].name.removesuffix('.run.json') + '.html'), out / 'result.html')
            run = json.loads((out / 'result.json').read_text(encoding='utf-8'))
            outcomes = Counter(a['outcome'] for a in run['attempts'])
            summary.update(exitCode=result.returncode, outcome='PASS' if result.returncode == 0 else 'TEST_FAILED',
                           passed=outcomes['PASS'], failed=len(run['attempts']) - outcomes['PASS'],
                           notRun=run['plannedAttempts'] - len(run['attempts']), outcomes=dict(outcomes))
            if args.baseline:
                compared = cli(['run-compare', '--baseline', str(Path(args.baseline).resolve()),
                                '--current', str(out / 'result.json'), '--out', str(out / 'comparison.html')])
                if compared.returncode:
                    summary.update(exitCode=2, outcome='COMPARISON_ERROR')
    except (ValueError, OSError, RuntimeError, subprocess.TimeoutExpired) as error:
        summary['exitCode'] = 2
        if isinstance(error, ValueError):
            summary['outcome'] = 'INPUT_ERROR'
        summary['reason'] = str(error) if isinstance(error, (ValueError, RuntimeError)) else 'I/O failure or CLI execution timeout.'
    except KeyboardInterrupt:
        summary.update(exitCode=2, outcome='INTERRUPTED')
    finally:
        if app is not None and app.poll() is None:
            app.terminate()
            try:
                app.wait(timeout=10)
            except subprocess.TimeoutExpired:
                app.kill()
                app.wait(timeout=5)
        (out / 'summary.json').write_text(json.dumps(summary, indent=2) + '\n', encoding='utf-8')
        markdown = ('## Reqover scenario checks\n\n'
                    '| Result | Passed | Failed | Not run |\n| --- | ---: | ---: | ---: |\n'
                    f'| {summary["outcome"]} | {summary["passed"]} | {summary["failed"]} | {summary["notRun"]} |\n\n'
                    'Outcomes: ' + ', '.join(f'{k}: {v}' for k, v in summary['outcomes'].items()) + '\n')
        if summary.get('reason'):
            # Reasons above are fixed runner messages, never server output or exception traces.
            markdown += '\n' + summary['reason'] + '\n'
        (out / 'summary.md').write_text(markdown, encoding='utf-8')
        if os.environ.get('GITHUB_STEP_SUMMARY'):
            with open(os.environ['GITHUB_STEP_SUMMARY'], 'a', encoding='utf-8') as handle:
                handle.write(markdown)
        if os.environ.get('GITHUB_OUTPUT'):
            with open(os.environ['GITHUB_OUTPUT'], 'a', encoding='utf-8') as handle:
                handle.write(f'directory={out}\nexit-code={summary["exitCode"]}\noutcome={summary["outcome"]}\n')
        print(json.dumps({'directory': str(out), **summary}), flush=True)
    return summary['exitCode']


if __name__ == '__main__':
    def interrupted(*unused):
        raise KeyboardInterrupt()
    signal.signal(signal.SIGTERM, interrupted)
    sys.exit(main())
