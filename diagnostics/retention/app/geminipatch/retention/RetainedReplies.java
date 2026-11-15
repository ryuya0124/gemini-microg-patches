package app.geminipatch.retention;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;

/** Opt-in patch: retain displayed assistant output in a private encrypted local archive. */
public final class RetainedReplies {
    private static final Handler MAIN=new Handler(Looper.getMainLooper());
    private static final WeakHashMap<Activity,WeakReference<Session>> SESSIONS=new WeakHashMap<Activity,WeakReference<Session>>();
    private RetainedReplies(){}
    public static void rememberVideoHeaders(Object headers){MediaCapture.headers(headers);}
    public static void attach(final Activity activity){
        if(activity==null)return;
        MAIN.post(new Runnable(){public void run(){try{
            if(activity.isFinishing()||activity.isDestroyed())return;
            synchronized(SESSIONS){WeakReference<Session> old=SESSIONS.get(activity);if(old!=null&&old.get()!=null)return;
                Session session=new Session(activity);SESSIONS.put(activity,new WeakReference<Session>(session));session.start();}
        }catch(Throwable ignored){/* An unavailable viewer must not change the native chat. */}}});
    }
    static String label(String en,String ja){return "ja".equals(Locale.getDefault().getLanguage())?ja:en;}
    private static final class Session implements ViewTreeObserver.OnDrawListener,Application.ActivityLifecycleCallbacks {
        private final WeakReference<Activity> activity;
        private final Application application;
        private final SavedArchive archive;
        private final ReplyStore store;
        private final MediaCapture mediaCapture=new MediaCapture();
        private final MediaViewer mediaViewer;
        private View root;
        private Button button;
        private android.app.Dialog archiveDialog;
        private boolean foreground=true, disposed;
        Session(Activity activity){this.activity=new WeakReference<Activity>(activity);application=activity.getApplication();archive=SavedArchive.get(activity);store=archive.store;mediaViewer=new MediaViewer(activity);}
        void start(){
            Activity a=activity.get();if(a==null)return;
            View content=a.findViewById(android.R.id.content);if(!(content instanceof FrameLayout))return;
            root=a.getWindow().getDecorView();
            button=new Button(a);button.setText(label("Saved replies","保持した回答"));button.setTextSize(12);
            button.setContentDescription("GeminiPatch saved replies");button.setAllCaps(false);
            button.setEnabled(archive.ready);
            button.setMinHeight(0);button.setMinimumHeight(0);button.setMinWidth(0);button.setMinimumWidth(0);
            float density=a.getResources().getDisplayMetrics().density;
            button.setPadding((int)(8*density),0,(int)(8*density),0);
            FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,(int)(36*density),Gravity.TOP|Gravity.END);
            p.topMargin=(int)(64*density);p.rightMargin=(int)(8*density);
            ((FrameLayout)content).addView(button,p);
            button.setOnClickListener(new View.OnClickListener(){public void onClick(View v){show();}});
            application.registerActivityLifecycleCallbacks(this);root.getViewTreeObserver().addOnDrawListener(this);
            MAIN.post(new Runnable(){public void run(){if(disposed)return;if(archive.ready){button.setEnabled(true);root.invalidate();}else MAIN.postDelayed(this,50);}});
        }
        @Override public void onDraw(){
            Activity a=activity.get();if(disposed||!archive.ready||!archive.enabled||!foreground||a==null||!a.hasWindowFocus()||a.isFinishing()||a.isDestroyed())return;
            try{scan(root,new int[]{1200},new Rect());}catch(Throwable ignored){}
        }
        private void scan(View view,int[] budget,Rect visible){
            if(view==null||budget[0]--<=0||view==button||view.getVisibility()!=View.VISIBLE||view.getAlpha()<=0)return;
            if(view.getId()!=View.NO_ID&&(view instanceof android.widget.ImageView||view.getClass().getName().equals("androidx.media3.ui.PlayerView"))&&view.isShown()&&view.getWidth()>0&&view.getHeight()>0&&view.getGlobalVisibleRect(visible))try{
                mediaCapture.observe(view,view.getResources().getResourceEntryName(view.getId()),archive);
            }catch(android.content.res.Resources.NotFoundException ignored){}
            if(view.getId()!=View.NO_ID&&view instanceof TextView && !(view instanceof android.widget.EditText)){
                try{
                    if("assistant_robin_text".equals(view.getResources().getResourceEntryName(view.getId()))
                            &&view.isShown()&&view.getWidth()>0&&view.getHeight()>0&&view.getGlobalVisibleRect(visible)){
                        CharSequence text=((TextView)view).getText();if(text!=null&&store.observe(view,text.toString()))archive.changed();
                    }
                }catch(android.content.res.Resources.NotFoundException ignored){}
            }
            if(view instanceof ViewGroup){ViewGroup group=(ViewGroup)view;for(int i=0;i<group.getChildCount()&&budget[0]>0;i++)scan(group.getChildAt(i),budget,visible);}
        }
        private void show(){
            final Activity a=activity.get();if(disposed||a==null||a.isFinishing())return;
            if(archiveDialog!=null)archiveDialog.dismiss();
            List<String> saved=store.snapshot();StringBuilder text=new StringBuilder();
            text.append(label("Previously displayed replies, including chats you switched between. Encrypted in this app's private storage and kept across restarts. Nothing uploads automatically. May include incomplete or replaced text.\n\n","表示された回答です（切り替えた会話も含みます）。アプリ専用領域に暗号化して保存し、再起動後も保持します。自動送信はしません。表示途中・置換前の文章を含む場合があります。\n\n"));
            if(!"saved".equals(archive.status))text.append(storageStatus()).append("\n\n");
            if("saving".equals(archive.mediaStatus))text.append(label("Saving media…\n\n","画像・動画を保存しています…\n\n"));
            if("unavailable".equals(archive.mediaStatus)||"save_failed".equals(archive.mediaStatus))text.append(label("Some media could not be saved.\n\n","保存できなかった画像・動画があります。\n\n"));
            text.append(label("Limit: latest 8 versions, 32,768 characters each.\n\n","上限：最新8件、各32,768文字。\n\n"));
            text.append(label("Images/videos: up to 8 files / 128 MB total. This local archive is separate from Google history and remains after signing out; clear it here when no longer needed.\n\n","画像・動画は合計8件・128 MBまでです。Googleの会話履歴とは別の保存で、ログアウト後も残ります。不要になったらここで消去してください。\n\n"));
            if(saved.isEmpty())text.append(label("No displayed replies saved yet.","保持した回答はまだありません。"));
            for(int i=saved.size()-1;i>=0;i--){text.append("— ").append(i+1).append(" —\n").append(saved.get(i)).append("\n\n");}
            TextView body=new TextView(a);body.setText(text);body.setTextSize(16);body.setTextIsSelectable(true);
            int pad=(int)(16*a.getResources().getDisplayMetrics().density);body.setPadding(pad,pad,pad,pad);
            android.widget.LinearLayout column=new android.widget.LinearLayout(a);column.setOrientation(1);
            android.widget.Switch videos=new android.widget.Switch(a);videos.setText(label("Save videos too (may use network data; up to 32 MB each)","動画も保存する（通信が発生する場合あり・1件32 MBまで）"));videos.setChecked(archive.videosEnabled);
            videos.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener(){public void onCheckedChanged(android.widget.CompoundButton b,boolean checked){archive.setVideosEnabled(checked);}});column.addView(videos);
            for(final SavedArchive.Media item:archive.media()){
                Button open=new Button(a);open.setAllCaps(false);open.setText((item.kind==1?label("Saved image","保存した画像"):label("Saved video","保存した動画"))+" · "+(item.size/1024)+" KB");
                open.setOnClickListener(new View.OnClickListener(){public void onClick(View v){mediaViewer.open(archive,item);}});column.addView(open);
            }
            column.addView(body);ScrollView scroll=new ScrollView(a);scroll.addView(column);
            archiveDialog=new AlertDialog.Builder(a).setTitle(label("Saved displayed replies","表示済みの回答"))
                    .setView(scroll).setPositiveButton(label("Close","閉じる"),null)
                    .setNegativeButton(label("Clear & pause","消去して一時停止"),new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface dialog,int which){archive.clearAndPause();}})
                    .setNeutralButton(archive.enabled?label("Pause saving","保持を一時停止"):label("Resume saving","保持を再開"),new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface dialog,int which){archive.setEnabled(!archive.enabled);}})
                    .show();
        }
        private String storageStatus(){
            if("load_failed".equals(archive.status))return label("Could not read the saved archive. New text is in memory only; clear and resume to reset storage.","保存済みデータを読み込めません。新しい本文はメモリ内のみです。消去して再開すると保存先をリセットします。");
            if("save_failed".equals(archive.status))return label("Saving failed. New text is in memory only for now.","保存に失敗しました。新しい本文は現在メモリ内のみです。");
            if("clearing".equals(archive.status))return label("Clearing saved data…","保持データを消去しています…");
            return label("Saving / loading…","保存・読み込み中…");
        }
        private void dispose(){
            if(disposed)return;disposed=true;mediaViewer.close();if(archiveDialog!=null)archiveDialog.dismiss();archiveDialog=null;store.detachViews();archive.flush();application.unregisterActivityLifecycleCallbacks(this);
            if(root!=null&&root.getViewTreeObserver().isAlive())root.getViewTreeObserver().removeOnDrawListener(this);
            if(button!=null&&button.getParent()instanceof ViewGroup)((ViewGroup)button.getParent()).removeView(button);
            Activity a=activity.get();synchronized(SESSIONS){if(a!=null)SESSIONS.remove(a);}root=null;button=null;
        }
        @Override public void onActivityStopped(Activity a){if(a==activity.get()){foreground=false;MediaCapture.background();store.detachViews();archive.flush();}}
        @Override public void onActivityDestroyed(Activity a){if(a==activity.get()||activity.get()==null)dispose();}
        @Override public void onActivityCreated(Activity a,Bundle b){}
        @Override public void onActivityStarted(Activity a){}
        @Override public void onActivityResumed(Activity a){if(a==activity.get())foreground=true;}
        @Override public void onActivityPaused(Activity a){if(a==activity.get()){foreground=false;mediaViewer.close();if(archiveDialog!=null)archiveDialog.dismiss();archiveDialog=null;}}
        @Override public void onActivitySaveInstanceState(Activity a,Bundle b){}
    }
}
