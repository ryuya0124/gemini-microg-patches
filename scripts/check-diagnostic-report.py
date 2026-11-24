"""Summarize a report without printing payloads; reject the wrong build before diagnosis."""
import argparse
import base64
import hashlib
import json
import re
from collections import Counter
from pathlib import Path


def code(version):
    digest = hashlib.sha256(('geminipatch-diagnostics:' + version).encode('ascii')).digest()
    return base64.b32encode(digest).decode('ascii').lower()[:10]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("report", type=Path)
    parser.add_argument("--expected-build", required=True)
    args = parser.parse_args()
    if not re.fullmatch(r"\d+\.\d+\.\d+", args.expected_build):
        parser.error("expected build must be a version such as 0.3.9")
    expected = code(args.expected_build)
    try:
        raw = args.report.read_bytes()
        if len(raw) > 4 * 1024 * 1024:
            raise ValueError()
        report = json.loads(raw)
        if not isinstance(report, dict) or not isinstance(report.get("events"), list):
            raise ValueError()
        events = [e for e in report["events"] if isinstance(e, dict)]
        legacy = report.get("build") if isinstance(report.get("build"), str) else None
        headers = [e.get("r") for e in events if e.get("event") == "capture_started"]
        processes = [p for p in report.get("processes", []) if isinstance(p, dict)]
        process_codes = sorted({(p.get("identity") or {}).get("r") for p in processes})
        matches = report.get("r") == expected and report.get("cr") == expected and headers == [expected] and process_codes == [expected]
        handling = Counter((e.get("stage"), len(e.get("punts", []))) for e in events if e.get("event") == "warning_handling")
        located = Counter(json.dumps(e.get("punt", {}).get("path")) for e in events if e.get("event") == "conversation_punt_located")
        loss = report.get("minimumObservedLoss")
        summary = {
            "bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(),
            "expectedBuild": args.expected_build, "expectedCode": expected,
            "legacyBuildLabel": legacy, "reportCode": report.get("r"), "captureCode": report.get("cr"),
            "captureHeaderCodes": headers, "processCodes": process_codes, "buildMatches": matches,
            "buildIdentityState": (report.get("buildIdentity") or {}).get("state"),
            "allProcessesAcknowledged": report.get("allStoppedProcessesAcknowledged") is True,
            "incomplete": report.get("incomplete") is not False,
            "minimumObservedLoss": loss if type(loss) is int else None,
            "warningHandling": {f"{stage}:{count}": n for (stage, count), n in handling.items()},
            "locatedPunts": dict(located),
        }
        print(json.dumps(summary, indent=2))
        return 0 if matches else 2
    except (OSError, ValueError, KeyError, TypeError):
        print(json.dumps({"error": "unreadable_or_invalid_report"}))
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
