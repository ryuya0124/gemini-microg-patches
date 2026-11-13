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
    // Observations are bounded separately. FIFO keeps Stop/Save behind prior error events.
    private static final LinkedBlockingDeque<Runnable> TASKS = new LinkedBlockingDeque<Runnable>();
    private static final AtomicInteger DROPPED = new AtomicInteger();
    private static final AtomicInteger NORMAL_PENDING = new AtomicInteger(), IMPORTANT_PENDING = new AtomicInteger();
    private static final Map<String, Integer> COUNTERS = new HashMap<String, Integer>();
    private static final Set<String> SEEN = new HashSet<String>();
    private static volatile Context context;
    private static volatile File folder;
    private static volatile boolean active;
    private static volatile String state = "Capture is off.";
    private static Thread worker;
    private static volatile long started;
    private static long until, refreshed;
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
                Runnable task = TASKS.poll(500, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (task != null) task.run();
            } catch (Throwable ignored) { state = "A diagnostic operation could not complete."; }
        }}, "GeminiPatch-diagnostics");
        worker.setDaemon(true); worker.start();
    }
    private static void enqueue(Runnable task, boolean control) {
        if (worker == null) return;
        TASKS.offerLast(task);
    }
    private static void observed(final Runnable task, boolean important) {
        final long generation = started;
        final AtomicInteger pending = important ? IMPORTANT_PENDING : NORMAL_PENDING;
        if (pending.incrementAndGet() > (important ? 16 : 24)) { pending.decrementAndGet(); DROPPED.incrementAndGet(); return; }
        enqueue(new Runnable() { public void run() {
            try { if (active && generation == started) task.run(); } finally { pending.decrementAndGet(); }
        }}, false);
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
        if (now - refreshed < 500 || folder == null) return;
        refreshed = now;
        JSONObject control = read(file("control.json"));
        long begin = control.optLong("started", 0);
        if (begin != started) { started = begin; COUNTERS.clear(); SEEN.clear(); DROPPED.set(0); }
        until = control.optLong("until", 0); synthetic = control.optBoolean("synthetic", false);
        active = started > 0 && System.currentTimeMillis() < until;
        state = active ? "Recording technical diagnostics (up to 15 minutes)." : (started > 0 ? "Capture stopped. You can save the report." : "Capture is off.");
    }
    private static JSONObject event(String name) throws Exception {
        return new JSONObject().put("event", name).put("milliseconds", Math.max(0, System.currentTimeMillis() - started))
                .put("process", Process.myPid()).put("syntheticTest", synthetic);
    }
    private static void append(JSONObject value, boolean important) {
        if (!active) return;
        try {
            folder.mkdirs();
            try (RandomAccessFile lockFile = new RandomAccessFile(file("capture.lock"), "rw"); FileChannel channel = lockFile.getChannel(); FileLock lock = channel.tryLock()) {
                if (lock == null) { DROPPED.incrementAndGet(); return; }
                JSONObject control = read(file("control.json"));
                if (control.optLong("started") != started || System.currentTimeMillis() >= control.optLong("until")) return;
                File events = file("events.jsonl");
                value.put("queueDropped", DROPPED.get());
                byte[] data = (value.toString() + "\n").getBytes(StandardCharsets.UTF_8);
                if (events.length() + data.length > (important ? HARD_LIMIT : NORMAL_LIMIT)) { DROPPED.incrementAndGet(); return; }
                try (OutputStream o = new FileOutputStream(events, true)) { o.write(data); }
            }
        } catch (Throwable ignored) { DROPPED.incrementAndGet(); }
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
                }
                refreshed = 0; refresh();
                JSONObject header = event("capture_started").put("reportSchema", 1).put("diagnosticBuild", Schema.BUILD)
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
        init(c); enqueue(new Runnable() { public void run() {
            try {
                folder.mkdirs();
                append(event("capture_stopped").put("observedCounters", new JSONObject(COUNTERS)), true);
                try (RandomAccessFile l = new RandomAccessFile(file("capture.lock"), "rw"); FileChannel ch = l.getChannel(); FileLock lock = ch.lock()) {
                    JSONObject control = read(file("control.json")); control.put("until", 0); write(file("control.json"), control);
                }
                refreshed = 0; refresh(); reply(callback, true, "Capture stopped.");
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
                }
                refreshed = 0; refresh(); reply(callback, true, "Report deleted.");
            } catch (Throwable ignored) { reply(callback, false, "Could not delete the report. Try again."); }
        }}, true);
    }
    public static void save(Context c, final android.net.Uri destination, final Reply callback) {
        init(c); enqueue(new Runnable() { public void run() {
            try {
                folder.mkdirs();
                JSONObject report = new JSONObject().put("schema", 1).put("build", Schema.BUILD);
                JSONArray events = new JSONArray();
                try (RandomAccessFile l = new RandomAccessFile(file("capture.lock"), "rw"); FileChannel ch = l.getChannel(); FileLock lock = ch.lock()) {
                    JSONObject control = read(file("control.json")); report.put("syntheticTest", control.optBoolean("synthetic", false));
                    report.put("capturePresent", control.optLong("started", 0) > 0);
                    if (file("events.jsonl").length() > HARD_LIMIT) throw new IOException();
                    if (file("events.jsonl").isFile()) try (BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file("events.jsonl")), StandardCharsets.UTF_8))) {
                        String line; while ((line = in.readLine()) != null) events.put(new JSONObject(line));
                    }
                }
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
        observed(new Runnable() { public void run() {
            if (!active) return;
            try {
                String kind = Schema.ROOTS.get(value.getClass().getName());
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
        }}, false);
    }
    public static void stream(Object value) {
        if (!active) return;
        observed(new Runnable() { public void run() { try { if (sample("stream")) append(event("incoming_stream").put("observedCount", COUNTERS.get("stream")), false); } catch (Throwable ignored) {} }}, false);
        incoming(value);
    }
    public static void status(final Object value) {
        if (!active) return;
        observed(new Runnable() { public void run() {
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
        }}, true);
    }
    public static void capabilities(final Object value) {
        if (!active || value == null) return;
        observed(new Runnable() { public void run() {
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
        }}, false);
    }
    public static void version(final String value) {
        if (!active) return;
        observed(new Runnable() { public void run() { try {
            if (value != null && value.matches("[0-9][0-9A-Za-z._-]{0,89}") && SEEN.add("version:" + value))
                append(event("constructed_client_version").put("version", value), false);
        } catch (Throwable ignored) { problem("constructed_client_version"); } }}, false);
    }
    public static void requestVersion(final String value) {
        if (!active) return;
        observed(new Runnable() { public void run() { try {
            if (value != null && value.matches("[0-9][0-9A-Za-z._-]{0,89}") && SEEN.add("request_version:" + value))
                append(event("request_client_version").put("version", value), false);
        } catch (Throwable ignored) { problem("request_client_version"); } }}, false);
    }
    public static void readOnly(final Object value) {
        if (!active) return;
        observed(new Runnable() { public void run() { try { if (sample("readonly:" + (value != null))) append(event("read_only_input").put("present", value != null), value != null); } catch (Throwable ignored) {} }}, value != null);
    }
    public static void classified(final String value) {
        if (!active) return;
        observed(new Runnable() { public void run() { try {
            append(event("client_error_classified").put("tooOld", "UNSUPPORTED_CLIENT_TOO_OLD".equals(value)), true);
        } catch (Throwable ignored) {} }}, true);
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
                if (!role.equals("user_message")) observed(new Runnable() { public void run() { try {
                    if (SEEN.add("visible_warning:" + fixedRole)) append(event("known_warning_text_rendered").put("role", fixedRole), true);
                } catch (Throwable ignored) {} }}, true);
            }
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount() && budget[0] > 0; i++) scan(group.getChildAt(i), budget);
        }
    }
}
