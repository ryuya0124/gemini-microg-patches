package app.geminipatch.diagnostics;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.TreeSet;

/**
 * Fixed installation identities only; no account, content, APK path or signature data.
 * The diagnostic build is recorded as the opaque code {@link Schema#R}, never as a version number.
 */
final class BuildAudit {
    private static JSONObject installed;

    static String code(String text) { return text != null && text.matches("[a-z2-7]{10}") ? text : "unavailable"; }
    private static String hash(String text) { return text != null && text.matches("[a-f0-9]{64}") ? text : "unavailable"; }
    private static String safe(String text) { return text != null && text.matches("[0-9A-Za-z._ -]{1,90}") ? text : "unavailable"; }

    static JSONObject packageInfo(Context context, String pkg) throws Exception {
        JSONObject v = new JSONObject();
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(pkg, 0);
            v.put("present", true).put("versionName", safe(info.versionName))
                    .put("code", Build.VERSION.SDK_INT >= 28 ? info.getLongVersionCode() : info.versionCode)
                    .put("updatedAtMs", info.lastUpdateTime).put("installedAtMs", info.firstInstallTime);
        } catch (Throwable ignored) { v.put("present", false); }
        return v;
    }

    static synchronized JSONObject snapshot(Context context, int[] calls) throws Exception {
        if (installed == null) {
            JSONObject value = new JSONObject().put("r", Schema.R).put("p", Schema.PROFILE_SHA256);
            if (Build.VERSION.SDK_INT >= 28) {
                String process = android.app.Application.getProcessName();
                value.put("processRole", process == null ? "other" : process.equals(context.getPackageName()) ? "main"
                        : process.substring(process.indexOf(':') + 1).matches("[a-z_]{1,40}") ? process.substring(process.indexOf(':') + 1) : "other");
            }
            value.put("self", packageInfo(context, context.getPackageName()));
            try {
                Class<?> helper = context.getClassLoader().loadClass("app.geminipatch.extension.ConversationUpdateCompatibility");
                value.put("warningHandlingLoaded", helper.getMethod("filter", Object.class) != null && helper.getMethod("filterStream", Object.class) != null);
            } catch (Throwable ignored) { value.put("warningHandlingLoaded", false); }
            installed = value;
        }
        JSONObject result = new JSONObject(installed.toString());
        JSONObject hooks = new JSONObject();
        String[] names = Capture.HOOKS;
        for (int i = 0; i < names.length; i++) hooks.put(names[i], calls[i]);
        return result.put("hookCallsSinceProcessStart", hooks);
    }

    /** Whether every part of a report came from this exact installed diagnostic build. */
    static JSONObject evaluate(JSONObject report, JSONArray events) throws Exception {
        TreeSet<String> codes = new TreeSet<String>();
        boolean known = true, profileMismatch = false;
        if (!report.optBoolean("capturePresent")) return new JSONObject().put("state", "no_capture");
        for (String key : new String[] {"r", "cr"}) {
            String c = code(report.optString(key));
            if (c.equals("unavailable")) known = false; else codes.add(c);
        }
        JSONArray processes = report.optJSONArray("processes");
        if (processes == null || processes.length() == 0) known = false;
        else for (int i = 0; i < processes.length(); i++) {
            JSONObject identity = processes.getJSONObject(i).optJSONObject("identity");
            if (identity == null) { known = false; continue; }
            String c = code(identity.optString("r"));
            if (c.equals("unavailable")) known = false; else codes.add(c);
            String profile = hash(identity.optString("p"));
            if (profile.equals("unavailable")) known = false; else profileMismatch |= !profile.equals(Schema.PROFILE_SHA256);
        }
        int headers = 0;
        for (int i = 0; i < events.length(); i++) {
            JSONObject e = events.getJSONObject(i);
            if (!e.optString("event").equals("capture_started")) continue;
            headers++;
            String c = code(e.optString("r"));
            if (c.equals("unavailable")) known = false; else codes.add(c);
        }
        known &= headers == 1;
        return new JSONObject().put("state", codes.size() > 1 || profileMismatch ? "mismatch" : known ? "consistent" : "unknown")
                .put("codes", new JSONArray(codes));
    }
}
