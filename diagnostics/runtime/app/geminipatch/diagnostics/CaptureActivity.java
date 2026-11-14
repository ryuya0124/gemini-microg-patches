package app.geminipatch.diagnostics;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** User-controlled capture/export UI; no automatic sharing and no account/content access. */
public final class CaptureActivity extends Activity {
    private static final int SAVE = 901;
    private TextView status;
    private final Handler handler = new Handler();
    private final Runnable refresh = new Runnable() { public void run() {
        if (status != null) status.setText(Capture.state()); handler.postDelayed(this, 1000);
    }};
    private final Capture.Reply reply = new Capture.Reply() { public void done(boolean ok, String message) {
        if (!isFinishing()) new AlertDialog.Builder(CaptureActivity.this).setMessage(message).setPositiveButton("OK", null).show();
    }};
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved); Capture.init(this); setTitle("Gemini diagnostics");
        LinearLayout column = new LinearLayout(this); column.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (20 * getResources().getDisplayMetrics().density); column.setPadding(pad, pad, pad, pad);
        TextView title = new TextView(this); title.setText("Gemini diagnostics"); title.setTextSize(24); column.addView(title);
        TextView info = new TextView(this);
        info.setText("Records technical app versions, feature flags, response/error codes, app code/view identifiers and whether the update-warning text was detected.\n\nChat text, email addresses, conversation IDs, credentials and URLs are not saved. Nothing is uploaded automatically.\n\nStart capture, open the affected chat, then return here and save the JSON report. Recording stops after 15 minutes. Starting again replaces the previous report.");
        info.setTextSize(16); info.setPadding(0, pad, 0, pad); column.addView(info);
        status = new TextView(this); status.setTextSize(16); column.addView(status);
        button(column, "Start capture", new android.view.View.OnClickListener() { public void onClick(android.view.View v) { Capture.start(CaptureActivity.this, false, reply); }});
        button(column, "Open Gemini", new android.view.View.OnClickListener() { public void onClick(android.view.View v) {
            Intent i = new Intent(); i.setClassName("com.google.android.apps.bard.morphe", "com.google.android.apps.bard.shellapp.BardEntryPointActivity");
            try { startActivity(i); } catch (Throwable ignored) {
                try { i.setClassName(getPackageName(), "com.google.android.apps.search.assistant.surfaces.voice.robin.main.MainActivity"); startActivity(i); }
                catch (Throwable unavailable) { reply.done(false, "Open your patched Gemini app from its usual icon."); }
            }
        }});
        button(column, "Mark warning shown", new android.view.View.OnClickListener() { public void onClick(android.view.View v) {
            if (!Capture.active()) { reply.done(false, "Start capture before marking the warning."); return; }
            Capture.mark(CaptureActivity.this); reply.done(true, "Marked the warning. Stop capture and save the report next.");
        }});
        button(column, "Stop capture", new android.view.View.OnClickListener() { public void onClick(android.view.View v) { Capture.stop(CaptureActivity.this, reply); }});
        button(column, "Save report", new android.view.View.OnClickListener() { public void onClick(android.view.View v) {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE, "GeminiPatch-diagnostics.json");
            try { startActivityForResult(i, SAVE); } catch (Throwable ignored) { reply.done(false, "Android's document picker is unavailable."); }
        }});
        button(column, "Delete report", new android.view.View.OnClickListener() { public void onClick(android.view.View v) {
            new AlertDialog.Builder(CaptureActivity.this).setMessage("Delete the report stored in this app?").setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface dialog, int which) { Capture.delete(CaptureActivity.this, reply); }
                }).show();
        }});
        ScrollView scroll = new ScrollView(this); scroll.addView(column); setContentView(scroll);
    }
    private void button(LinearLayout parent, String text, android.view.View.OnClickListener listener) {
        Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setOnClickListener(listener); parent.addView(b);
    }
    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == SAVE && result == RESULT_OK && data != null && data.getData() != null) Capture.save(this, data.getData(), reply);
    }
}
