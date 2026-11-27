import assert from "node:assert/strict";
import { createHash } from "node:crypto";

export const sha256 = data => createHash("sha256").update(data).digest("hex");
export const asciiJSON = (value, indent) => JSON.stringify(value, null, indent)
  .replace(/[^\x00-\x7f]/g, char => "\\u" + char.charCodeAt(0).toString(16).padStart(4, "0"));

export function buildCode(version) {
  assert.match(version, /^\d+\.\d+\.\d+$/);
  const alphabet = "abcdefghijklmnopqrstuvwxyz234567";
  const digest = createHash("sha256").update("geminipatch-diagnostics:" + version).digest();
  let code = "";
  let buffer = 0;
  let bits = 0;
  for (const byte of digest.subarray(0, 7)) {
    buffer = (buffer << 8) | byte;
    bits += 8;
    while (bits >= 5) {
      bits -= 5;
      code += alphabet[(buffer >>> bits) & 31];
    }
  }
  return code.slice(0, 10);
}
