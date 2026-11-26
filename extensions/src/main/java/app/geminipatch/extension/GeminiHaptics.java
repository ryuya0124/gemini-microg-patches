package app.geminipatch.extension;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.system.ErrnoException;
import android.system.Os;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/** Gemini view feedback and the waveform player share one app-private setting. */
public final class GeminiHaptics {
    private static final String KEY = "geminipatch_haptic_feedback";
    private static final String STATE_FILE = KEY + ".state";
    private static volatile Context application;

    private GeminiHaptics() {}

    public static void addPreference(Object screen, String names) {
        if (screen == null) return;
        try {
            String[] member = names.split(",", -1);
            ClassLoader loader = screen.getClass().getClassLoader();
            Class<?> preference = loader.loadClass("androidx.preference.Preference");
            Class<?> group = loader.loadClass("androidx.preference.PreferenceGroup");
            if (group.getMethod(member[0], String.class).invoke(screen, KEY) != null) return;

            Context context = (Context) readField(preference, "j").get(screen);
            Class<?> switchType = loader.loadClass("androidx.preference.SwitchPreferenceCompat");
            Object toggle = switchType.getConstructor(Context.class, AttributeSet.class)
                    .newInstance(context, null);
            preference.getMethod(member[1], String.class).invoke(toggle, KEY);
            preference.getMethod(member[2], CharSequence.class).invoke(toggle, title(context));
            readField(preference, member[3]).set(toggle, isEnabled(context));
            bindListener(preference, toggle, member[5], context);
            group.getMethod(member[4], preference).invoke(screen, toggle);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.w("GeminiPatch", "Cannot add the haptic feedback setting", e);
        }
    }

    /** Both settings builders consume AndroidX preferences, including the Compose list adapter. */
    public static void addListPreference(Object screen, String names) {
        if (screen == null) return;
        try {
            String[] member = names.split(",", -1);
            ClassLoader loader = screen.getClass().getClassLoader();
            Class<?> preference = loader.loadClass("androidx.preference.Preference");
            Class<?> group = loader.loadClass("androidx.preference.PreferenceGroup");
            if (member[3].equals(screen.getClass().getName())) {
                Field keyField = readField(preference, member[5]);
                List<?> rows = (List<?>) readField(screen.getClass(), member[4]).get(screen);
                for (Object row : rows) {
                    if (KEY.equals(keyField.get(row))) return;
                }
            } else if (group.getMethod(member[0], String.class).invoke(screen, KEY) != null) {
                return;
            }

            Context context = (Context) readField(preference, "j").get(screen);
            Class<?> switchType = loader.loadClass("androidx.preference.SwitchPreferenceCompat");
            Object toggle = switchType.getConstructor(Context.class, AttributeSet.class)
                    .newInstance(context, null);
            preference.getMethod(member[1], String.class).invoke(toggle, KEY);
            preference.getMethod(member[2], CharSequence.class).invoke(toggle, title(context));
            readField(preference, member[6]).setBoolean(toggle, false);
            switchType.getMethod(member[8], boolean.class).invoke(toggle, isEnabled(context));
            bindListener(preference, toggle, member[7], context);
            group.getMethod(member[9], preference).invoke(screen, toggle);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Log.w("GeminiPatch", "Cannot add the vibration setting to Gemini settings", e);
        }
    }

    private static void bindListener(Class<?> preference, Object toggle, String name, Context context)
            throws ReflectiveOperationException {
        Field listener = readField(preference, name);
        listener.set(toggle, Proxy.newProxyInstance(preference.getClassLoader(),
                new Class<?>[] { listener.getType() }, new SaveSwitchValue(context)));
    }

    private static final class SaveSwitchValue implements InvocationHandler {
        private final Context context;

        SaveSwitchValue(Context context) {
            this.context = context;
        }

        @Override public Object invoke(Object proxy, Method method, Object[] args) {
            if (method.getDeclaringClass() == Object.class) {
                switch (method.getName()) {
                    case "equals": return proxy == args[0];
                    case "hashCode": return System.identityHashCode(proxy);
                    default: return "Gemini vibration preference listener";
                }
            }
            if (args == null || args.length != 2 || !(args[1] instanceof Boolean)) return false;
            return saveEnabled(context, (Boolean) args[1]);
        }
    }

    /** Add a footer to the main Compose settings page. The original view retains its lifecycle. */
    public static View addComposePreference(View content, int titleResource) {
        if (content == null || content.getParent() != null) return content;
        Context context = content.getContext();
        String resourceName;
        try {
            resourceName = context.getResources().getResourceEntryName(titleResource);
        } catch (Resources.NotFoundException e) {
            return content;
        }
        if (!"assistant_ui_settings_settings".equals(resourceName)
                && !"assistant_ui_settings_title".equals(resourceName)) return content;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        ViewGroup.LayoutParams params = content.getLayoutParams();
        if (params != null) layout.setLayoutParams(params);
        layout.addView(content, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        Switch toggle = new Switch(context);
        toggle.setText(title(context));
        toggle.setChecked(isEnabled(context));
        float density = context.getResources().getDisplayMetrics().density;
        int horizontalPadding = Math.round(16 * density);
        toggle.setMinHeight(Math.round(48 * density));
        toggle.setPaddingRelative(horizontalPadding, 0, horizontalPadding, 0);
        ToggleBinding binding = new ToggleBinding(toggle, horizontalPadding);
        toggle.setOnCheckedChangeListener(binding);
        layout.setOnApplyWindowInsetsListener(binding);
        layout.addView(toggle, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        return layout;
    }

    private static final class ToggleBinding
            implements CompoundButton.OnCheckedChangeListener, View.OnApplyWindowInsetsListener {
        private final Switch toggle;
        private final int padding;

        ToggleBinding(Switch toggle, int padding) {
            this.toggle = toggle;
            this.padding = padding;
        }

        @Override public void onCheckedChanged(CompoundButton button, boolean enabled) {
            if (!saveEnabled(button.getContext(), enabled)) {
                toggle.setOnCheckedChangeListener(null);
                toggle.setChecked(isEnabled(button.getContext()));
                toggle.setOnCheckedChangeListener(this);
            }
        }

        @Override public WindowInsets onApplyWindowInsets(View layout, WindowInsets insets) {
            int bottom = insets.getStableInsetBottom();
            if (toggle.getPaddingBottom() != bottom) toggle.setPaddingRelative(padding, 0, padding, bottom);
            return insets;
        }
    }

    private static Field readField(Class<?> owner, String name) throws NoSuchFieldException {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    public static boolean performFeedback(View view, int effect) {
        return isEnabled(view.getContext()) && view.performHapticFeedback(effect);
    }

    public static void setApplicationContext(Context context) {
        application = context;
    }

    public static void performVibration(Vibrator vibrator, VibrationEffect effect) {
        Context context = application;
        if (context == null || isEnabled(context)) vibrator.vibrate(effect);
    }

    private static boolean isEnabled(Context context) {
        try (FileInputStream input = context.openFileInput(STATE_FILE)) {
            int state = input.read();
            if (state == 0 || state == 1) return state == 1;
        } catch (FileNotFoundException ignored) {
            // Preserve the user's setting from builds that used SharedPreferences alone.
        } catch (IOException e) {
            Log.w("GeminiPatch", "Cannot read the Gemini vibration setting", e);
        }
        return preferences(context).getBoolean(KEY, true);
    }

    private static boolean saveEnabled(Context context, boolean enabled) {
        File temporary = null;
        try {
            File destination = context.getFileStreamPath(STATE_FILE);
            temporary = File.createTempFile("gemini-haptics-", ".tmp", destination.getParentFile());
            try (FileOutputStream output = new FileOutputStream(temporary)) {
                output.write(enabled ? 1 : 0);
                output.getFD().sync();
            }
            // Other processes only open the final path, so they never cache or read a partial value.
            Os.rename(temporary.getAbsolutePath(), destination.getAbsolutePath());
            preferences(context).edit().putBoolean(KEY, enabled).apply();
            return true;
        } catch (IOException | ErrnoException e) {
            Log.w("GeminiPatch", "Cannot save the Gemini vibration setting", e);
            return false;
        } finally {
            if (temporary != null) temporary.delete();
        }
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(context.getPackageName() + "_preferences", Context.MODE_PRIVATE);
    }

    private static String title(Context context) {
        return isJapanese(context) ? "Geminiの振動" : "Gemini vibration";
    }

    private static boolean isJapanese(Context context) {
        return "ja".equals(context.getResources().getConfiguration().getLocales().get(0).getLanguage());
    }
}
