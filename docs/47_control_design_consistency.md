**English** | [한국어](47_control_design_consistency.ko.md) | [Documentation](README.md)

# Consistent file pickers and dashboard controls

October 23, 2026 (KST), PR #53. Started at `0455553`, synchronized with origin and
without user changes. This is unreleased source work, not a change to published 0.4.2.
It preserves the behavior covered by the [rendered review](46_rendered_browser_review.md).

## Design decisions and plan

Native file UI was nested inside styled buttons, producing mismatched heights and
mixed browser/application presentation. Control sizes and action placement also
varied across scenario, history, comparison and draft views.

- Preserve the green accent and existing light/dark palette.
- Share 44px ordinary controls, 8px corners and 13px control text; use a deliberate
  36px compact action size rather than unrelated per-screen sizes.
- Group scenario metadata/save/import/review together, with a separate panel for
  exporting reviewed cases. Make primary and secondary actions visually distinct.
- Align history actions and right-aligned desktop search. Stack fields/actions at
  narrow widths without fixed content heights.
- Give all four imports one upload-button presentation, native keyboard behavior
  and a visible focus ring.

Work sequence: inspect the actual baseline and prior troubleshooting; add a failing
native-input contract regression; introduce the shared picker and final control
stylesheet; group actions; separately reproduce/fix the icon defect; rebuild and
verify generated HTML in the browser, then update README/docs.

## Implementation boundaries

`DiagnosticDashboard.filePicker` supplies fixed UI labels and IDs. The native input
covers its visible label with transparent presentation; it is not removed or taken
out of the Tab order. `:focus-within` outlines the control. The OS file chooser
itself is unchanged. No upload service or dependency was added.

| Existing input ID | Accessible name | Multiple |
| --- | --- | --- |
| `scenario-import` | Import scenario | No |
| `scenario-runs-import` | Import runs / history | Yes |
| `recording-import` | Import recording / catalog | Yes |
| `reqover-baseline-file` | Import baseline JSON | No |

`controls.css` is included after component styles and owns shared dimensions/states.
Heatmap cells, graph nodes and table sort links retain their data-specific designs.
Models, controllers, validation rules, file schemas, runners, collectors and CLI
commands are unchanged, including existing selection and feedback-lifetime behavior.

## Troubleshooting: multiline SVG roots

The icon helper replaced only a literal `<svg `, while bundled SVG roots put a
newline after `<svg`. Their shared class and `aria-hidden` were therefore missing,
leaving the default 24px dimensions. The browser review measured a 32px-wide baseline
clear button with its icon compressed to 2px.

Handle whitespace after the SVG root and size the baseline clear control at 44px.
After rebuilding/reloading, the button measured 44×44px and the icon 18px with
`aria-hidden=true`. The upload/download/clear/network regression failed before
the fix and passed afterward.

## Rendered verification

- Direct in-app browser access to synthetic HTML on a dedicated loopback server;
  no access-denial error or workaround.
- Exercised all four imports, including multi-file runs/recordings and single-file
  baseline comparison. Enter opened run/baseline choosers with visible focus.
- Imported, reviewed and saved a scenario. Imported cases still required review;
  executable exports remained disabled until reviewed.
- Checked heatmap/API list document width at 320×568, 390×844, 1024×480 and 1440×900;
  no document horizontal overflow. Inspected stacked forms/actions and light/dark themes.
- An offscreen file input and checkbox each caused an automation wait/check error.
  Inspecting their positions, scrolling normally and interacting with the same
  control succeeded. Do not assume this tool automatically scrolls offscreen inputs.
- An actual downloaded HTML retained the shared CSS, four file pickers, normalized
  icons and Lucide/Feather notices. Captured browser warning/error logs were empty.
  Before/after evidence is kept in local task outputs.

## Automated evidence and cleanup

Java 21 report 81 + CLI 25 tests passed: 106 total, failures/errors/skips 0, including
the two new native-input/icon regressions. All 48 Node tests passed, failures/skips 0.
Latest-head GitHub checks are recorded separately in the PR body/checks.

The dedicated server was stopped and port 18753 verified closed; temporary theme
and viewport overrides were restored. This does not establish real-phone,
other-browser or exhaustive accessibility coverage. No personal recordings, keys,
.env or downloaded verification artifacts are committed. No merge or release.
