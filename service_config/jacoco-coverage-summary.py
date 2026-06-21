#!/usr/bin/env python3
"""
Aggregate JaCoCo per-class line coverage across all Qanary modules and print a
human-readable summary.

Reads every ``**/target/site/jacoco/jacoco.csv`` produced by the ``jacoco:report``
goal (run as part of ``mvn test``) and reports, per module and overall:

  * line coverage percentage
  * the list of classes whose line coverage is below the threshold (default 80%)

The summary is written to ``$GITHUB_STEP_SUMMARY`` (GitHub Actions job summary)
when that environment variable is set, and always to stdout.

Exit code:
  * 0 by default (report-only).
  * If ``COVERAGE_ENFORCE=true``, exits 1 when any class is below the threshold,
    so the coverage gate can fail the build once all classes meet it.

The per-class gate itself is also enforced by the ``jacoco:check`` goal; enable it
to fail the Maven build with ``-Djacoco.haltOnFailure=true``.
"""
import csv
import glob
import os
import sys

THRESHOLD = float(os.environ.get("COVERAGE_THRESHOLD", "80"))
ENFORCE = os.environ.get("COVERAGE_ENFORCE", "false").lower() == "true"

# Regression ratchet: enforced minimum *overall* line coverage (percent). The
# build fails if overall coverage drops below this, so coverage cannot silently
# regress even though the per-class 80% rule stays advisory (report-only).
MIN_OVERALL = float(os.environ.get("COVERAGE_MIN_OVERALL", "0") or "0")

# Enforced per-module floors, e.g. "qanary_commons=55,qanary_pipeline-template=48".
MIN_MODULE = {}
for _part in os.environ.get("COVERAGE_MIN_MODULE", "").split(","):
    if "=" in _part:
        _k, _v = _part.split("=", 1)
        MIN_MODULE[_k.strip()] = float(_v)


def load_csv(path):
    classes = []
    with open(path, newline="") as f:
        for row in csv.DictReader(f):
            line_missed = int(row["LINE_MISSED"])
            line_covered = int(row["LINE_COVERED"])
            total = line_missed + line_covered
            if total == 0:
                continue  # interfaces / marker classes have no lines to cover
            classes.append({
                "name": (row["PACKAGE"] + "." + row["CLASS"]),
                "covered": line_covered,
                "total": total,
                "pct": 100.0 * line_covered / total,
            })
    return classes


def main():
    csv_paths = sorted(glob.glob("**/target/site/jacoco/jacoco.csv", recursive=True))
    if not csv_paths:
        print("No JaCoCo reports found (expected **/target/site/jacoco/jacoco.csv). "
              "Did 'mvn test' run?", file=sys.stderr)
        return 0

    lines = []
    lines.append("## Test coverage (JaCoCo)\n")
    lines.append(f"Per-class line-coverage threshold: **{THRESHOLD:.0f}%**\n")

    grand_covered = grand_total = 0
    below = []
    module_pct = {}

    for path in csv_paths:
        module = path.split("/target/")[0]
        classes = load_csv(path)
        if not classes:
            continue
        mod_covered = sum(c["covered"] for c in classes)
        mod_total = sum(c["total"] for c in classes)
        grand_covered += mod_covered
        grand_total += mod_total
        mod_pct = 100.0 * mod_covered / mod_total if mod_total else 0.0
        module_pct[module] = mod_pct
        mod_below = [c for c in classes if c["pct"] < THRESHOLD]
        below.extend((module, c) for c in mod_below)
        floor = MIN_MODULE.get(module)
        floor_note = f" (enforced floor {floor:.0f}%{' ✗' if mod_pct + 1e-9 < floor else ' ✓'})" if floor else ""
        lines.append(f"### `{module}` — {mod_pct:.1f}% "
                     f"({mod_covered}/{mod_total} lines, "
                     f"{len(mod_below)}/{len(classes)} classes below threshold){floor_note}\n")

    overall = 100.0 * grand_covered / grand_total if grand_total else 0.0
    summary = (f"### Overall: **{overall:.1f}%** "
               f"({grand_covered}/{grand_total} lines covered); "
               f"{len(below)} class(es) below {THRESHOLD:.0f}%\n")
    lines.insert(1, summary)

    if below:
        lines.append("\n<details><summary>Classes below threshold</summary>\n")
        lines.append("\n| Module | Class | Lines | Coverage |")
        lines.append("| --- | --- | ---: | ---: |")
        for module, c in sorted(below, key=lambda x: (x[0], x[1]["pct"])):
            lines.append(f"| {module} | `{c['name']}` | {c['total']} | {c['pct']:.1f}% |")
        lines.append("\n</details>\n")

    report = "\n".join(lines)
    print(report)

    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a") as f:
            f.write(report + "\n")

    # ---- enforced regression gate (ratchet) --------------------------------
    failures = []
    if MIN_OVERALL and overall + 1e-9 < MIN_OVERALL:
        failures.append(f"overall coverage {overall:.1f}% < required {MIN_OVERALL:.0f}%")
    for module, floor in MIN_MODULE.items():
        actual = module_pct.get(module)
        if actual is not None and actual + 1e-9 < floor:
            failures.append(f"module {module} coverage {actual:.1f}% < required {floor:.0f}%")
    if failures:
        print("\nCOVERAGE GATE FAILED (regression):", file=sys.stderr)
        for f in failures:
            print("  - " + f, file=sys.stderr)
        return 1

    # ---- advisory per-class enforcement (off by default) -------------------
    if ENFORCE and below:
        print(f"\nCOVERAGE GATE FAILED: {len(below)} class(es) below "
              f"{THRESHOLD:.0f}% line coverage.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
