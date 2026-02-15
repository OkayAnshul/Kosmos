# Archive Reference Map

This file maps archived document clusters to their current location for forensic/history use.

## Master Archive Root
- `cleanup_nonprod_2026-03-06/`

## Key Buckets
- Legacy root docs: `root_legacy_docs/`
- SQL and historical logs: `root_sql_and_logs/`
- Production analysis set: `analysis_docs/analysis/`
- Historical project archives: `archived_material/archive/`
- Legacy documents workspace: `archived_material/documents/`
- Prior testing docs: `testing_docs/testing/`
- Internal debug runtime code moved from main: `internal_debug_runtime/`

## Final Cleanup Pass Moves
- Previous `docs/` set: `archived_material/final_cleanup_pass/docs/`
- Previous `fixes/` set: `archived_material/final_cleanup_pass/fixes/`

## How to Use
1. Read production docs in `docs/` first.
2. Use archive paths only when deep historical context is needed.
3. Do not reintroduce archived docs into root without explicit curation.
