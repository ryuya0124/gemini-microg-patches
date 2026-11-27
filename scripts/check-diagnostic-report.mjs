import { readFileSync, statSync } from "node:fs";
import { parseArgs } from "node:util";
import { buildCode, sha256 } from "../diagnostics/format.mjs";

const { values, positionals } = parseArgs({
  options: { "expected-build": { type: "string" } }, allowPositionals: true,
});
const [file] = positionals;
const version = values["expected-build"];
if (positionals.length !== 1 || !/^\d+\.\d+\.\d+$/.test(version ?? "")) {
  console.error("Usage: node scripts/check-diagnostic-report.mjs REPORT --expected-build VERSION");
  process.exit(1);
}

const isObject = value => value && typeof value === "object" && !Array.isArray(value);
const counter = (items, key) => {
  const counts = new Map();
  for (const item of items) {
    const id = key(item);
    counts.set(id, (counts.get(id) ?? 0) + 1);
  }
  return Object.fromEntries(counts);
};
const identity = value => typeof value === "string" && /^[a-z2-7]{10}$/.test(value) ? value : null;
const safeLabel = value => typeof value === "string" && /^[A-Za-z0-9_.-]{1,64}$/.test(value) ? value : null;
const cleanPath = value => typeof value === "string" && value.length <= 200 &&
  /^[A-Za-z_$][\w$]*(?:\.[A-Za-z_$][\w$]*(?:\[\]|\{\})?)*$/.test(value) ? value : null;

try {
  const maxBytes = 4 * 1024 * 1024;
  if (statSync(file).size > maxBytes) throw new Error();
  const bytes = readFileSync(file);
  if (bytes.length > maxBytes) throw new Error();
  const report = JSON.parse(bytes.toString("utf8"));
  if (!isObject(report) || !Array.isArray(report.events)) throw new Error();
  const expected = buildCode(version);
  const events = report.events.filter(isObject);
  const headers = events.filter(event => event.event === "capture_started").map(event => identity(event.r));
  const codes = [...new Set((report.processes ?? []).filter(isObject).map(p => identity(p.identity?.r)))].sort();
  const isExpected = codes => codes.length === 1 && codes[0] === expected;
  const matches = report.r === expected && report.cr === expected && isExpected(headers) && isExpected(codes);
  const summary = {
    bytes: bytes.length, sha256: sha256(bytes),
    expectedBuild: version, expectedCode: expected, legacyBuildLabel: safeLabel(report.build),
    reportCode: identity(report.r), captureCode: identity(report.cr), captureHeaderCodes: headers,
    processCodes: codes, buildMatches: matches, buildIdentityState: safeLabel(report.buildIdentity?.state),
    allProcessesAcknowledged: report.allStoppedProcessesAcknowledged === true, incomplete: report.incomplete !== false,
    minimumObservedLoss: Number.isSafeInteger(report.minimumObservedLoss) ? report.minimumObservedLoss : null,
    warningHandling: counter(events.filter(event => event.event === "warning_handling"),
      event => (safeLabel(event.stage) ?? "unknown") + ":" + (Array.isArray(event.punts) ? event.punts.length : 0)),
    locatedPunts: counter(events.filter(event => event.event === "conversation_punt_located"), event => JSON.stringify(cleanPath(event.punt?.path))),
  };
  console.log(JSON.stringify(summary, null, 2));
  process.exitCode = matches ? 0 : 2;
} catch {
  console.log(JSON.stringify({ error: "unreadable_or_invalid_report" }));
  process.exitCode = 1;
}
