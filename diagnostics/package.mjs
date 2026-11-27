import assert from "node:assert/strict";
import { execFileSync } from "node:child_process";
import { chmodSync, cpSync, existsSync, mkdirSync, mkdtempSync, readdirSync, readFileSync,
  renameSync, rmSync, utimesSync, writeFileSync } from "node:fs";
import { basename, dirname, join, relative, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { asciiJSON, buildCode, sha256 } from "./format.mjs";

const root = fileURLToPath(new URL("..", import.meta.url));
const [mode, directory, override] = process.argv.slice(2);
const out = resolve(directory);
assert.equal(out, join(root, "build", "diagnostics"));
const readJSON = file => JSON.parse(readFileSync(file, "utf8"));
const metadata = readJSON(join(root, "diagnostics", "metadata.json"));
const { version, description, created_at: created } = metadata;
assert.match(version, /^\d+\.\d+\.\d+$/);
assert.match(metadata.profile, /^[A-Za-z0-9._-]+\.json$/);
const profile = readJSON(join(root, "diagnostics", "profiles", metadata.profile));
assert.equal(profile.build, version);
const profileText = asciiJSON(profile, 2) + "\n";
const resources = join(out, "resources");

function schema() {
  for (const folder of ["runtime/classes", "runtime/dex", "patch-dex"]) {
    const path = join(out, folder);
    rmSync(path, { recursive: true, force: true });
    mkdirSync(path, { recursive: true });
  }
  const lines = ["package app.geminipatch.diagnostics;", "import java.util.*;", "public final class Schema {"];
  const string = (key, value, access = "") => lines.push(access + "static final String " + key + " = " + asciiJSON(value) + ";");
  string("R", buildCode(version), "public ");
  string("PROFILE_SHA256", sha256(profileText), "public ");
  for (const [constant, field] of Object.entries({
    VERSION: "versionName", STATUS: "statusType", ERROR_ENUM: "errorEnum", CAPABILITIES: "capabilitiesType",
    PUNT: "puntType", PUNT_TITLE: "puntTitleField", PUNT_BODY: "puntBodyField", PUNT_BEHAVIOR: "puntBehaviorField",
  })) string(constant, profile[field]);
  assert.match(profile.versionCode, /^\d+$/);
  lines.push("static final long CODE = " + profile.versionCode + "L;");
  for (const [name, section] of [["ROOTS", "incomingTypes"], ["PUNT_FIELDS", "puntFields"]]) {
    lines.push("static final Map<String,String> " + name + "; static { Map<String,String> m=new HashMap<String,String>();");
    for (const [key, value] of Object.entries(profile[section])) {
      lines.push("m.put(" + asciiJSON(key) + "," + asciiJSON(value) + ");");
    }
    lines.push(name + "=Collections.unmodifiableMap(m);", "}");
  }
  writeFileSync(join(out, "runtime", "Schema.java"), [...lines, "}"].join("\n") + "\n");
  const folder = join(resources, "diagnostics");
  mkdirSync(folder, { recursive: true });
  writeFileSync(join(folder, "profile.json"), profileText);
  const properties = Object.fromEntries(Object.entries(profile).filter(([, value]) => typeof value === "string"));
  for (const section of ["methods", "incomingTypes", "expectedSites"]) {
    for (const [key, value] of Object.entries(profile[section])) properties[section + "." + key] = String(value);
  }
  const fields = Object.entries(profile.versionFields);
  properties["versionFields.count"] = String(fields.length);
  for (const [i, [field, observer]] of fields.entries()) {
    properties["versionFields." + i + ".field"] = field;
    properties["versionFields." + i + ".observer"] = observer;
  }
  properties["capabilityFields.count"] = String(profile.capabilityFields.length);
  for (const [i, field] of profile.capabilityFields.entries()) properties["capabilityFields." + i] = field;
  const sorted = Object.keys(properties).sort().map(key => {
    assert.match(key, /^[A-Za-z0-9_.]+$/);
    assert.match(properties[key], /^[\x20-\x5b\x5d-\x7e]*$/);
    return key + "=" + properties[key] + "\n";
  });
  writeFileSync(join(folder, "profile.properties"), sorted.join(""));
}

const files = folder => readdirSync(folder, { recursive: true, withFileTypes: true })
  .filter(entry => entry.isFile()).map(entry => join(entry.parentPath, entry.name));

function collect(source, target) {
  for (const file of files(source)) {
    const name = relative(source, file);
    if (name === "META-INF/MANIFEST.MF") continue;
    const dest = join(target, name);
    assert.ok(!existsSync(dest), "Duplicate archive entry: " + name);
    mkdirSync(dirname(dest), { recursive: true });
    cpSync(file, dest);
  }
}

function compress(tree, archive) {
  const epoch = new Date("2000-01-01T00:00:00Z");
  const entries = files(tree).map(file => {
    chmodSync(file, 0o644);
    utimesSync(file, epoch, epoch);
    return relative(tree, file);
  }).sort();
  execFileSync("zip", ["-q", "-X", archive, "-@"], {
    cwd: tree, input: entries.join("\n") + "\n", env: { ...process.env, LC_ALL: "C", TZ: "UTC" },
  });
}

function archives(action) {
  const scratch = mkdtempSync(join(out, ".pack-"));
  try {
    const tree = join(scratch, "tree");
    mkdirSync(tree);
    action(tree, scratch);
  } finally {
    rmSync(scratch, { recursive: true, force: true });
  }
}

const merged = join(out, "diagnostics-patches.jar");

function merge(tree, scratch) {
  for (const [i, source] of [join(root, "build", "gemini-patches.jar"), join(out, "addon.jar")].entries()) {
    const extracted = join(scratch, "extracted-" + i);
    execFileSync("unzip", ["-q", source, "-d", extracted]);
    collect(extracted, tree);
  }
  collect(resources, tree);
  const archive = join(scratch, "diagnostics-patches.jar");
  compress(tree, archive);
  renameSync(archive, merged);
}

function pack(tree, scratch) {
  const extracted = join(scratch, "merged");
  execFileSync("unzip", ["-q", merged, "-d", extracted]);
  collect(extracted, tree);
  const timestamp = Date.parse(created.endsWith("Z") ? created : created + "Z");
  assert.ok(Number.isFinite(timestamp));
  const repo = "https://github.com/ryuya0124/gemini-microg-patches";
  const attributes = {
    "Manifest-Version": "1.0", Name: "Gemini microG patches", Description: description, Version: version,
    Timestamp: timestamp, Source: repo, Author: "ryuya0124", Website: repo, "Patcher-Version": "2.5.0",
  };
  const lines = [];
  for (const [key, value] of Object.entries(attributes)) {
    let line = key + ": " + value;
    assert.match(line, /^[\x20-\x7e]+$/);
    while (line.length > 70) {
      lines.push(line.slice(0, 70));
      line = " " + line.slice(70);
    }
    lines.push(line);
  }
  mkdirSync(join(tree, "META-INF"), { recursive: true });
  writeFileSync(join(tree, "META-INF", "MANIFEST.MF"), lines.join("\r\n") + "\r\n\r\n");
  const dex = readdirSync(join(out, "patch-dex")).filter(name => /^classes\d*\.dex$/.test(name));
  assert.ok(dex.includes("classes.dex"));
  for (const name of dex) cpSync(join(out, "patch-dex", name), join(tree, name));
  const name = "gemini-microg-patches-" + version + ".mpp";
  const feed = "diagnostics-bundle.json";
  compress(tree, join(scratch, name));
  writeFileSync(join(scratch, feed), JSON.stringify({ version, created_at: created, description,
    download_url: repo + "/releases/download/v" + version + "/" + name,
    page_url: repo + "/releases/tag/v" + version }, null, 2) + "\n");
  writeFileSync(join(scratch, "SHA256SUMS"), [name, feed].map(file => sha256(readFileSync(join(scratch, file))) + "  " + basename(file) + "\n").join(""));
  const release = join(out, "release");
  mkdirSync(release, { recursive: true });
  for (const file of [name, feed, "SHA256SUMS"]) renameSync(join(scratch, file), join(release, file));
  console.log("Packaged " + name + " build code " + buildCode(version));
}

switch (mode) {
  case "token": console.log(buildCode(override ?? version)); break;
  case "schema": schema(); break;
  case "merge": archives(merge); break;
  case "package": archives(pack); break;
  default: assert.fail("Unknown packaging stage");
}
