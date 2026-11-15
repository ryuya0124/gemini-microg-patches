"""Summarize a report without printing payloads; reject the wrong build before diagnosis."""
import argparse
import hashlib
import json
import re
from collections import Counter
from pathlib import Path


def build(value):
    return value if isinstance(value, str) and re.fullmatch(r"\d+\.\d+\.\d+(?:-(?:preview|diagnostics)\.\d+)?", value) else None


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", type=Path)
    parser.add_argument("--expected-build", required=True, type=build)
    args = parser.parse_args()
    if args.expected_build is None:
        parser.error("expected build must identify a preview or diagnostics version")
    try:
        raw = args.report.read_bytes()
        if len(raw) > 4 * 1024 * 1024:
            raise ValueError()
        report = json.loads(raw)
        if not isinstance(report, dict) or report.get("schema") != 2:
            raise ValueError()
        events = report["events"]
        if not isinstance(events, list) or not all(isinstance(e, dict) for e in events):
            raise ValueError()
        headers = [e for e in events if e.get("event") == "capture_started"]
        root_build = build(report.get("build"))
        header_builds = [build(e.get("diagnosticBuild")) for e in headers]
        matches = root_build == args.expected_build and header_builds == [args.expected_build]
        identity = report.get("buildIdentity", {})
        identity_state = identity.get("state") if isinstance(identity, dict) else None
        identity_state = identity_state if identity_state in {"consistent", "mismatch", "unknown", "no_capture"} else "unavailable"
        modern = tuple(map(int, args.expected_build.split('-')[0].split('.'))) >= (0, 3, 7)
        exporter = report.get("exporterIdentity", {})
        processes = report.get("processes", [])
        fingerprint = exporter.get("profileSha256") if isinstance(exporter, dict) else None
        valid_profile = isinstance(fingerprint, str) and re.fullmatch(r"[a-f0-9]{64}", fingerprint) is not None
        process_identities = [p.get("identity", {}) for p in processes if isinstance(p, dict)] if isinstance(processes, list) else []
        runtime_matches = bool(process_identities) and all(isinstance(i, dict) and i.get("runtimeBuild") == args.expected_build and i.get("profileSha256") == fingerprint for i in process_identities)
        identity_matches = not modern or (report.get("captureBuild") == args.expected_build and identity_state == "consistent" and isinstance(exporter, dict) and exporter.get("runtimeBuild") == args.expected_build and valid_profile and runtime_matches)
        compatibility = {}
        for kind in ("history_update_compatibility", "stream_update_compatibility"):
            counts = Counter(e.get("result") for e in events if e.get("event") == kind)
            compatibility[kind] = {key: counts[key] for key in ("no_punt", "retained", "matched", "removed", "failed") if counts[key]}
        loss = report.get("minimumObservedLoss")
        summary = {
            "bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(),
            "expectedBuild": args.expected_build, "reportBuild": root_build,
            "captureHeaderBuilds": header_builds, "buildMatches": matches,
            "buildIdentityState": identity_state, "identityMatches": identity_matches,
            "syntheticTest": report.get("syntheticTest") if type(report.get("syntheticTest")) is bool else None,
            "allProcessesAcknowledged": report.get("allStoppedProcessesAcknowledged") is True,
            "incomplete": report.get("incomplete") is not False,
            "minimumObservedLoss": loss if type(loss) is int and 0 <= loss <= 2**53 else None,
            "compatibilityOutcomes": compatibility,
        }
        print(json.dumps(summary, indent=2))
        return 2 if not matches else 0 if identity_matches else 3
    except (OSError, ValueError, KeyError, TypeError):
        print(json.dumps({"error": "unreadable_or_invalid_report"}))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
