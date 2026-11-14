package app.geminipatch.diagnostics;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.EditText;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicInteger;

/** Opt-in, local-only observer. Never serializes user text into the report. */
public final class Capture {
    public interface Reply { void done(boolean ok, String message); }
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    // FIFO preserves per-process ordering; admission is fair between observation kinds.
    private static final LinkedBlockingDeque<Runnable> TASKS = new LinkedBlockingDeque<Runnable>();
    private static final AtomicInteger DROPPED = new AtomicInteger();
    private static final ThreadLocal<Long> OBSERVED_AT = new ThreadLocal<Long>();
    private static final CaptureInbox INBOX = new CaptureInbox(new CaptureInbox.Dispatch() {
        public void post(Runnable r) { TASKS.offerLast(r); }
    }, new CaptureInbox.Execute() { public void run(long generation, long at, Runnable r) {
        if (generation != started) return;
        OBSERVED_AT.set(at); try { r.run(); } finally { OBSERVED_AT.remove(); }
    }});
    private static final Map<String, Integer> COUNTERS = new HashMap<String, Integer>();
    private static final Set<String> SEEN = new HashSet<String>();
    private static volatile Context context;
    private static volatile File folder;
    private static volatile boolean active;
    private static volatile String state = "Capture is off.";
    private static Thread worker;
    private static volatile long started;
    private static volatile long localStopRequest;
    private static long until, refreshed;
    private static long stoppedAt, summaryRevision = -1, summaryStoppedAt = -1;
    private static int summaryDropped = -1;
    private static boolean synthetic;
    private static final int NORMAL_LIMIT = 192 * 1024, HARD_LIMIT = 256 * 1024;
    private static final long DURATION = 15 * 60 * 1000;

    private Capture() {}
    public static synchronized void init(Context supplied) {
        if (context == null) {
            context = supplied.getApplicationContext();
            folder = new File(context.getFilesDir(), "geminipatch-conversation-diagnostics");
        }
        if (worker != null) return;
        worker = new Thread(new Runnable() { public void run() {
            for (;;) try {
                refresh();
                Runnable task = TASKS.poll(100, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (task != null) task.run();
                checkpoint(false);
            } catch (Throwable ignored) { state = "A diagnostic operation could not complete."; }
        }}, "GeminiPatch-diagnostics");
        worker.setDaemon(true); worker.start();
    }
    private static void enqueue(Runnable task, boolean control) {
        if (worker == null) return;
        TASKS.offerLast(task);
    }
    private static void observed(String kind, final Runnable task, boolean important, boolean merge) {
        if (worker == null || !active) return;
        INBOX.offer(started, System.currentTimeMillis(), kind, important, merge, task);
    }
    private static void reply(final Reply r, final boolean ok, final String message) {
        if (r != null) MAIN.post(new Runnable() { public void run() { r.done(ok, message); }});
    }
    public static String state() { return state; }
    public static boolean active() { return active; }
    private static File file(String name) { return new File(folder, name); }
    private static JSONObject read(File f) throws Exception {
        if (!f.isFile() || f.length() > HARD_LIMIT) return new JSONObject();
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        try (InputStream in = new FileInputStream(f)) { byte[] buf = new byte[4096]; int n; while ((n = in.read(buf)) >= 0) b.write(buf, 0, n); }
        return new JSONObject(new String(b.toByteArray(), StandardCharsets.UTF_8));
    }
    private static void write(File f, JSONObject j) throws Exception {
        File pending = new File(f.getParentFile(), f.getName() + ".pending");
        try (FileOutputStream o = new FileOutputStream(pending)) { o.write(j.toString().getBytes(StandardCharsets.UTF_8)); o.getFD().sync(); }
        if (!pending.renameTo(f)) throw new IOException(); // Same-directory atomic rename on Android.
    }
    private static void refresh() throws Exception {
        long now = SystemClock.elapsedRealtime();
        if (now - refreshed < 100 || folder == null) return;
        refreshed = now;
        JSONObject control = read(file("control.json"));
        long begin = control.optLong("started", 0);
        if (begin != started) {
            started = begin; localStopRequest = 0; COUNTERS.clear(); SEEN.clear(); DROPPED.set(0); INBOX.reset(begin);
            summaryRevision = summaryStoppedAt = -1; summaryDropped = -1;
        }
        until = control.optLong("until", 0); stoppedAt = control.optLong("stoppedAt", 0); synthetic = control.optBoolean("synthetic", false);
        active = started > 0 && localStopRequest == 0 && System.currentTimeMillis() < until;
        state = active ? "Recording technical diagnostics (up to 15 minutes)." : (started > 0 ? "Capture stopped. You can save the report." : "Capture is off.");
    }
    private static void checkpoint(boolean force) throws Exception {
        if (started <= 0 || folder == null) return;
        long revision = INBOX.revision(); int dropped = DROPPED.get();
        if (!force && revision == summaryRevision && dropped == summaryDropped && stoppedAt == summaryStoppedAt) return;
        JSONObject summary = new JSONObject().put("process", Process.myPid()).put("generation", started)
                .put("pending", INBOX.pending()).put("stopAcknowledged", stoppedAt > 0 && INBOX.pending() == 0)
                .put("observerLost", INBOX.lost() + dropped).put("otherDropped", dropped)
                .put("admission", new JSONObject(INBOX.counters())).put("processedCounters", new JSONObject(COUNTERS));
        folder.mkdirs();
        try (RandomAccessFile l = new RandomAccessFile(file("capture.lock"), "rw"); FileChannel ch = l.getChannel(); FileLock lock = ch.lock()) {
            if (read(file("control.json")).optLong("started") != started) return;
            write(file("process-" + Process.myPid() + ".json"), summary);
        }
        summaryRevision = revision; summaryDropped = dropped; summaryStoppedAt = stoppedAt;
    }
    private static JSONArray processSummaries(long generation) throws Exception {
        JSONArray records = new JSONArray(); File[] files = folder.listFiles(); if (files == null) return records;
        Arrays.sort(files);
        for (File f : files) if (f.getName().matches("process-[0-9]+\\.json")) {
            if (records.length() >= 64) throw new IOException();
            JSONObject value = read(f); if (value.optLong("generation") == generation) { value.remove("generation"); records.put(value); }
        }
        return records;
    }
    private static boolean acknowledged(JSONArray records) throws Exception {
        if (records.length() == 0) return false;
        for (int i = 0; i < records.length(); i++) if (!records.getJSONObject(i).optBoolean("stopAcknowledged")) return false;
        return true;
    }
    private static void awaitProcesses() throws Exception {
        long end = SystemClock.elapsedRealtime() + 3000;
        do {
            if (acknowledged(processSummaries(started))) return;
            Thread.sleep(50);
        } while (SystemClock.elapsedRealtime() < end);
    }
    private static JSONObject event(String name) throws Exception {
        Long observed = OBSERVED_AT.get();
        return new JSONObject().put("event", name).put("milliseconds", Math.max(0, (observed == null ? System.currentTimeMillis() : observed) - started))
                .put("process", Process.myPid()).put("syntheticTest", synthetic);
    }
    private static void append(JSONObject value, boolean important) {
        if (started <= 0) return;
        try {
            folder.mkdirs();
            // Wait only on the background worker. A competing process must not silently lose a record.
            try (RandomAccessFile lockFile = new RandomAccessFile(file("capture.lock"), "rw"); FileChannel channel = lockFile.getChannel(); FileLock lock = channel.lock()) {
                JSONObject control = read(file("control.json"));
                long cutoff = control.has("stoppedAt") ? control.optLong("stoppedAt") : control.optLong("until");
                if (control.optLong("started") != started || started + value.optLong("milliseconds") > cutoff) return;
                File events = file("events.jsonl");
                value.put("queueDropped", INBOX.lost()).put("otherDropped", DROPPED.get());
                byte[] data = (value.toString() + "\n").getBytes(StandardCharsets.UTF_8);
                if (events.length() + data.length > (important ? HARD_LIMIT : NORMAL_LIMIT)) { INBOX.loss("event_budget"); return; }
                try (OutputStream o = new FileOutputStream(events, true)) { o.write(data); }
            }
        } catch (Throwable ignored) { INBOX.loss("file_io"); }
    }
    private static boolean sample(String kind) {
        int count = COUNTERS.containsKey(kind) ? COUNTERS.get(kind) + 1 : 1;
        COUNTERS.put(kind, count);
        return count <= 4 || count % 128 == 0;
    }
    private static void problem(String stage) {
        try { if (SEEN.add("unavailable:" + stage)) append(event("observer_unavailable").put("stage", stage), true); } catch (Throwable ignored) {}
    }
    public static void start(Context c, final boolean test, final Reply callback) {
        init(c);
        enqueue(new Runnable() { public void run() {
            try {
                folder.mkdirs();
                try (RandomAccessFile l = new RandomAccessFile(file("capture.lock"), "rw"); FileChannel ch = l.getChannel(); FileLock lock = ch.lock()) {
                    long now = System.currentTimeMillis();
                    JSONObject control = new JSONObject().put("started", now).put("until", now + DURATION).put("synthetic", test);
                    write(file("control.json"), control);
                    try (OutputStream clear = new FileOutputStream(file("events.jsonl"))) { /* New opt-in capture replaces the previous report. */ }
                    File[] old = folder.listFiles(); if (old != null) for (File f : old)
                        if (f.getName().matches("process-[0-9]+\\.json(?:\\.pending)?")) f.delete();
                }
                refreshed = 0; refresh();
                JSONObject header = event("capture_started").put("reportSchema", 2).put("diagnosticBuild", Schema.BUILD)
                        .put("basePatchVersion", "0.3.5").put("sourceGoogleVersion", Schema.VERSION).put("sourceGoogleCode", Schema.CODE)
                        .put("androidApi", Build.VERSION.SDK_INT).put("durationLimitSeconds", DURATION / 1000)
                        .put("maximumEventBytes", HARD_LIMIT);
                JSONObject versions = new JSONObject();
                // The base clone patch rewrites exact package literals. Keep inventory identities intact.
                String google = new StringBuilder("com.google.android.").append("googlequicksearchbox").toString();
                String gemini = new StringBuilder("com.google.android.apps.").append("bard").toString();
                for (String pkg : new String[] {google, google + ".morphe", gemini, gemini + ".morphe"}) {
                    JSONObject v = new JSONObject();
                    try {
                        PackageInfo info = context.getPackageManager().getPackageInfo(pkg, 0);
                        String name = info.versionName;
                        v.put("present", true).put("versionName", name != null && name.matches("[0-9A-Za-z._-]{1,90}") ? name : "unavailable")
                                .put("packageManagerReportedCode", Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode);
                    } catch (Throwable ignored) { v.put("present", false); }
                    versions.put(pkg, v);
                }
                header.put("relevantPackages", versions); append(header, true);
                reply(callback, true, "Capture started. Open the affected chat, then return here and save the report.");
            } catch (Throwable ignored) { reply(callback, false, "Could not start capture. Try again."); }
        }}, true);
    }
    public static void stop(Context c, final Reply callback) {
        init(c); final long requestedAt = System.currentTimeMillis(); localStopRequest = requestedAt; active = false;
        enqueue(new Runnable() { public void run() {
            try {
                folder.mkdirs();
                try (RandomAccessFile l = new RandomAccessFile(file("capture.lock"), "rw"); FileChannel ch = l.getChannel(); FileLock lock = ch.lock()) {
                    JSONObject control = read(file("control.json"));
                    if (!control.has("stoppedAt")) {
                        long deadline = control.optLong("until", requestedAt);
                        control.put("stoppedAt", deadline > 0 ? Math.min(requestedAt, deadline) : requestedAt);
                    }
                    control.put("until", 0); write(file("control.json"), control);
                }
                refreshed = 0; refresh();
                append(event("capture_stopped").put("milliseconds", Math.max(0, stoppedAt - started)), true);
                checkpoint(true); awaitProcesses();
                reply(callback, true, "Capture stopped.");
            } catch (Throwable ignored) { reply(callback, false, "Could not stop capture. It expires after 15 minutes."); }
        }}, true);
    }
    public static void mark(Context c) {
        init(c); enqueue(new Runnable() { public void run() { try { append(event("user_marked_warning"), true); } catch (Throwable ignored) {} }}, true);
    }
    public static void delete(Context c, final Reply callback) {
        init(c); enqueue(new Runnable() { public void run() {
            try {
                folder.mkdirs();
                try (RandomAccessFile l = new RandomAccessFile(file("capture.lock"), "rw"); FileChannel ch = l.getChannel(); FileLock lock = ch.lock()) {
                    boolean ok = (!file("control.json").exists() || file("control.json").delete()) && (!file("events.jsonl").exists() || file("events.jsonl").delete())
                            && (!file("control.json.pending").exists() || file("control.json.pending").delete());
                    if (!ok) throw new IOException();
                    File[] old = folder.listFiles(); if (old != null) for (File f : old)
                        if (f.getName().matches("process-[0-9]+\\.json(?:\\.pending)?")) if (!f.delete() && f.exists()) throw new IOException();
                }
                refreshed = 0; refresh(); reply(callback, true, "Report deleted.");
            } catch (Throwable ignored) { reply(callback, false, "Could not delete the report. Try again."); }
        }}, true);
    }
    public static void save(Context c, final android.net.Uri destination, final Reply callback) {
        init(c); enqueue(new Runnable() { public void run() {
            try {
                folder.mkdirs();
                checkpoint(true);
                if (stoppedAt > 0) awaitProcesses();
                JSONObject report = new JSONObject().put("schema", 2).put("build", Schema.BUILD);
                JSONArray events = new JSONArray();
                try (RandomAccessFile l = new RandomAccessFile(file("capture.lock"), "rw"); FileChannel ch = l.getChannel(); FileLock lock = ch.lock()) {
                    JSONObject control = read(file("control.json")); report.put("syntheticTest", control.optBoolean("synthetic", false));
                    report.put("capturePresent", control.optLong("started", 0) > 0);
                    JSONArray processes = processSummaries(control.optLong("started"));
                    long lost = 0; for (int i = 0; i < processes.length(); i++) lost += processes.getJSONObject(i).optLong("observerLost");
                    boolean complete = control.has("stoppedAt") && acknowledged(processes);
                    report.put("processes", processes).put("allStoppedProcessesAcknowledged", complete)
                            .put("minimumObservedLoss", lost).put("incomplete", !complete || lost > 0);
                    if (file("events.jsonl").length() > HARD_LIMIT) throw new IOException();
                    if (file("events.jsonl").isFile()) try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file("events.jsonl")), StandardCharsets.UTF_8))) {
                        String line; while ((line = in.readLine()) != null) events.put(new JSONObject(line));
                    }
                }
                JSONArray processes = report.getJSONArray("processes"); Set<Integer> registered = new HashSet<Integer>();
                for (int i = 0; i < processes.length(); i++) registered.add(processes.getJSONObject(i).getInt("process"));
                for (int i = 0; i < events.length(); i++) {
                    int pid = events.getJSONObject(i).getInt("process");
                    if (registered.add(pid)) processes.put(new JSONObject().put("process", pid).put("summaryMissing", true).put("stopAcknowledged", false));
                }
                if (!acknowledged(processes)) report.put("allStoppedProcessesAcknowledged", false).put("incomplete", true);
                report.put("events", events);
                try (OutputStream o = context.getContentResolver().openOutputStream(destination, "wt")) {
                    if (o == null) throw new IOException(); o.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
                }
                reply(callback, true, "Report saved. Nothing was uploaded automatically.");
            } catch (Throwable ignored) { reply(callback, false, "Could not save the report. Try another location."); }
        }}, true);
    }
    private static boolean warning(String text) {
        if (text == null) return false;
        String t = text.toLowerCase(Locale.ROOT);
        return t.contains("conversation requires an update") || t.contains("created with a newer version of gemini")
                || ((t.contains("この会話") || t.contains("会話の継続")) && (t.contains("更新") || t.contains("アップデート")));
    }
    public static void incoming(final Object value) {
        if (!active || value == null || !Schema.ROOTS.containsKey(value.getClass().getName())) return;
        final String kind = Schema.ROOTS.get(value.getClass().getName());
        final boolean response = kind.equals("parsed_history_response") || kind.equals("parsed_generate_response");
        final ArrayList<String> callers = response || kind.equals("parsed_conversation_turn") ? fixedCallers() : null;
        observed("incoming:" + kind, new Runnable() { public void run() {
            try {
                String prefix = value.getClass().getName() + ".";
                for (Map.Entry<String,String> field : Schema.PUNT_FIELDS.entrySet()) if (field.getKey().startsWith(prefix))
                    recordPunt(value.getClass().getField(field.getKey().substring(prefix.length())).get(value), field.getValue(), callers);
                // Inspect only the warning submessage on large history/generate envelopes.
                // Never serialize those envelopes or retain conversation IDs/turn contents.
                if (response) return;
                Class<?> lite = value.getClass().getClassLoader().loadClass("com.google.protobuf.MessageLite");
                int size = ((Integer) lite.getMethod("getSerializedSize").invoke(value)).intValue();
                boolean scanned = size >= 0 && size <= 1024 * 1024, hit = false;
                if (scanned) {
                    byte[] bytes = (byte[]) lite.getMethod("toByteArray").invoke(value);
                    hit = warning(new String(bytes, StandardCharsets.UTF_8)); // Includes extensions; bytes/text are never written.
                }
                boolean emit = sample("parsed:" + kind);
                if (hit || emit) append(event("parsed_message").put("schema", kind).put("observedCount", COUNTERS.get("parsed:" + kind))
                        .put("knownWarningText", hit).put("textScanPerformed", scanned), hit);
            } catch (Throwable ignored) { problem("parsed_message"); }
        }}, response || kind.equals("parsed_conversation_turn"), !response && !kind.equals("parsed_conversation_turn"));
    }
    public static void history(Object value) { incoming(value); }
    public static void puntRendered(final Object value) {
        if (!active) return;
        final ArrayList<String> callers = fixedCallers();
        observed("punt_rendered", new Runnable() { public void run() {
            try { recordPunt(value, "banner", callers); } catch (Throwable ignored) { problem("punt_rendered"); }
        }}, true, false);
    }
    private static void recordPunt(Object value, String source, ArrayList<String> callers) throws Exception {
        if (value != null && !value.getClass().getName().equals(Schema.PUNT)) { problem("punt_type"); return; }
        JSONObject data = event("conversation_punt").put("source", source).put("present", value != null).put("callers", new JSONArray(callers));
        String key = "punt:" + source + ":" + callers.toString() + ":" + (value != null);
        boolean titleHit = false, bodyHit = false;
        if (value != null) {
            String title = (String) value.getClass().getField(Schema.PUNT_TITLE).get(value);
            String body = (String) value.getClass().getField(Schema.PUNT_BODY).get(value);
            int rawBehavior = value.getClass().getField(Schema.PUNT_BEHAVIOR).getInt(value);
            int behavior = rawBehavior >= 0 && rawBehavior <= 4 ? rawBehavior : -1;
            boolean titlePresent = title != null && !title.isEmpty(), bodyPresent = body != null && !body.isEmpty();
            titleHit = warning(title); bodyHit = warning(body);
            data.put("behavior", behavior).put("titlePresent", titlePresent).put("bodyPresent", bodyPresent)
                    .put("knownWarningTitle", titleHit).put("knownWarningBody", bodyHit);
            key += ":" + behavior + ":" + titlePresent + ":" + bodyPresent + ":" + titleHit + ":" + bodyHit;
        }
        // Only safe flags and static locations participate in de-duplication.
        if (SEEN.add(key)) append(data, titleHit || bodyHit || source.equals("banner"));
    }
    /** Fixed compatibility outcomes only; never retains a response, text or exception. */
    public static void historyUpdateCompatibility(final int call, final int result, final int stage, final int behavior,
            final boolean present, final boolean compared, final boolean title, final boolean body) {
        if (!active || call <= 0 || result < 0 || result > 4 || stage < 1 || stage > 6) return;
        observed("history_update_compatibility", new Runnable() { public void run() { try {
            String[] outcomes = {"no_punt", "retained", "matched", "removed", "failed"};
            String[] stages = {"schema", "match", "copy", "clear", "validate", "complete"};
            String key = "compatibility:" + outcomes[result];
            COUNTERS.put(key, COUNTERS.containsKey(key) ? COUNTERS.get(key) + 1 : 1);
            append(event("history_update_compatibility").put("call", call).put("result", outcomes[result])
                    .put("stage", stages[stage - 1]).put("behavior", behavior >= 0 && behavior <= 4 ? behavior : -1)
                    .put("originalPuntPresent", present).put("filteredPuntPresent", result != 3 && present)
                    .put("textCompared", compared).put("knownWarningTitle", title).put("knownWarningBody", body), true);
        } catch (Throwable ignored) { problem("history_update_compatibility"); } }}, true, false);
    }
    public static void stream(Object value) {
        if (!active) return;
        observed("stream", new Runnable() { public void run() { try { if (sample("stream")) append(event("incoming_stream").put("observedCount", COUNTERS.get("stream")), false); } catch (Throwable ignored) {} }}, true, true);
        incoming(value);
    }
    public static void status(final Object value) {
        if (!active) return;
        observed(value == null ? "status_absent" : "server_status", new Runnable() { public void run() {
            try {
                if (value == null) { if (sample("status_absent")) append(event("status_detail_absent"), false); return; }
                if (!value.getClass().getName().equals(Schema.STATUS)) { problem("status_type"); return; }
                int category = value.getClass().getField("b").getInt(value), code = value.getClass().getField("c").getInt(value);
                String message = (String) value.getClass().getField("d").get(value);
                Object en = value.getClass().getClassLoader().loadClass(Schema.ERROR_ENUM).getMethod("a", int.class).invoke(null, code);
                String name = en instanceof Enum ? ((Enum<?>) en).name() : "UNRECOGNIZED";
                if (!name.matches("[A-Z0-9_]{1,120}")) name = "UNRECOGNIZED";
                append(event("server_status").put("category", category).put("wireCode", code).put("error", name)
                        .put("serverTextPresent", message != null && !message.isEmpty()).put("knownWarningText", warning(message)), true);
            } catch (Throwable ignored) { problem("server_status"); }
        }}, value != null, value == null);
    }
    public static void capabilities(final Object value) {
        if (!active || value == null) return;
        observed("capabilities", new Runnable() { public void run() {
            try {
                if (!value.getClass().getName().equals(Schema.CAPABILITIES)) { problem("capabilities_type"); return; }
                Object raw = value.getClass().getField("c").get(value);
                if (!(raw instanceof List)) { problem("capabilities_list"); return; }
                if (((List<?>) raw).size() > 1024) { problem("capabilities_size"); return; }
                TreeSet<Integer> ids = new TreeSet<Integer>();
                for (Object id : (List<?>) raw) {
                    if (!(id instanceof Integer) || (Integer) id < 0 || (Integer) id > 10000) { problem("capabilities_value"); return; }
                    ids.add((Integer) id);
                }
                String data = ids.toString();
                if (!SEEN.contains("caps:" + data)) {
                    int variants = COUNTERS.containsKey("capability_variants") ? COUNTERS.get("capability_variants") : 0;
                    if (variants >= 64) { DROPPED.incrementAndGet(); return; }
                    COUNTERS.put("capability_variants", variants + 1); SEEN.add("caps:" + data);
                    append(event("capabilities").put("ids", new JSONArray(ids)).put("count", ids.size()), false);
                }
            } catch (Throwable ignored) { problem("capabilities"); }
        }}, false, false);
    }
    public static void version(final String value) {
        if (!active) return;
        observed("version", new Runnable() { public void run() { try {
            if (value != null && value.matches("[0-9][0-9A-Za-z._-]{0,89}") && SEEN.add("version:" + value))
                append(event("constructed_client_version").put("version", value), false);
        } catch (Throwable ignored) { problem("constructed_client_version"); } }}, false, true);
    }
    public static void requestVersion(final String value) {
        if (!active) return;
        observed("request_version", new Runnable() { public void run() { try {
            if (value != null && value.matches("[0-9][0-9A-Za-z._-]{0,89}") && SEEN.add("request_version:" + value))
                append(event("request_client_version").put("version", value), false);
        } catch (Throwable ignored) { problem("request_client_version"); } }}, false, true);
    }
    public static void readOnly(final Object value) {
        if (!active) return;
        observed("readonly:" + (value != null), new Runnable() { public void run() { try { if (sample("readonly:" + (value != null))) append(event("read_only_input").put("present", value != null), value != null); } catch (Throwable ignored) {} }}, value != null, true);
    }
    public static void classified(final String value) {
        if (!active) return;
        observed("classified:" + "UNSUPPORTED_CLIENT_TOO_OLD".equals(value), new Runnable() { public void run() { try {
            append(event("client_error_classified").put("tooOld", "UNSUPPORTED_CLIENT_TOO_OLD".equals(value)), true);
        } catch (Throwable ignored) {} }}, true, true);
    }
    private static String codeName(String value) {
        return value != null && value.matches("[A-Za-z0-9_.$<>-]{1,180}") ? value : "unavailable";
    }
    private static ArrayList<String> fixedCallers() {
        ArrayList<String> callers = new ArrayList<String>();
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            String cls = frame.getClassName();
            if (cls.startsWith("java.") || cls.startsWith("android.") || cls.startsWith("dalvik.") ||
                    cls.startsWith("kotlin.") || cls.startsWith("j$.") || cls.startsWith("app.geminipatch.")) continue;
            callers.add(codeName(cls) + "#" + codeName(frame.getMethodName()));
            if (callers.size() == 6) break;
        }
        return callers;
    }
    /** Native setter call sites only. No text, filenames, line numbers or exception trace is saved. */
    public static void textSet(CharSequence value) {
        if (!active || value == null) return;
        try {
            if (value.length() > 4096 || !warning(value.toString())) return;
            final ArrayList<String> callers = fixedCallers();
            observed("warning_setter", new Runnable() { public void run() { try {
                String key = "setter:" + callers.toString();
                if (SEEN.add(key)) append(event("known_warning_setter").put("callers", new JSONArray(callers)), true);
            } catch (Throwable ignored) {} }}, true, true);
        } catch (Throwable ignored) {}
    }
    private static String resourceName(View view) {
        try { return codeName(view.getResources().getResourceEntryName(view.getId())); }
        catch (Throwable ignored) { return "unavailable"; }
    }
    public static void attach(final Activity activity) {
        try {
            init(activity);
            final WeakReference<Activity> reference = new WeakReference<Activity>(activity);
            MAIN.postDelayed(new Runnable() { public void run() {
                Activity a = reference.get(); if (a == null || a.isFinishing() || (Build.VERSION.SDK_INT >= 17 && a.isDestroyed())) return;
                if (active) try { scan(a.getWindow().getDecorView(), new int[] {1500}); } catch (Throwable ignored) {}
                MAIN.postDelayed(this, 1200);
            }}, 1200);
        } catch (Throwable ignored) {}
    }
    private static void scan(View view, int[] budget) {
        if (view == null || budget[0]-- <= 0 || view.getVisibility() != View.VISIBLE) return;
        if (view instanceof TextView && !(view instanceof EditText)) {
            CharSequence text = ((TextView) view).getText();
            if (text != null && warning(text.toString())) {
                String role = "other";
                try {
                    String id = view.getResources().getResourceEntryName(view.getId());
                    if (id.equals("assistant_robin_user_message_text")) role = "user_message";
                    else if (id.equals("assistant_robin_text")) role = "assistant_message";
                } catch (Throwable ignored) {}
                final String fixedRole = role;
                final String resource = resourceName(view), viewClass = codeName(view.getClass().getName());
                final ArrayList<String> ancestors = new ArrayList<String>();
                android.view.ViewParent parent = view.getParent();
                for (int i = 0; i < 5 && parent instanceof View; i++) {
                    View ancestor = (View) parent;
                    ancestors.add(codeName(ancestor.getClass().getName()) + "#" + resourceName(ancestor));
                    parent = ancestor.getParent();
                }
                if (!role.equals("user_message")) observed("visible_warning:" + fixedRole, new Runnable() { public void run() { try {
                    if (SEEN.add("visible_warning:" + fixedRole + ":" + resource + ":" + viewClass))
                        append(event("known_warning_text_rendered").put("role", fixedRole).put("resource", resource)
                                .put("viewClass", viewClass).put("ancestors", new JSONArray(ancestors)), true);
                } catch (Throwable ignored) {} }}, true, true);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount() && budget[0] > 0; i++) scan(group.getChildAt(i), budget);
        }
    }
}
