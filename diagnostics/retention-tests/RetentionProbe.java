package app.geminipatch.retentiontest;
import android.app.*;
import android.content.Intent;
import android.os.*;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.*;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaPlayer;
import java.io.*;
import java.lang.ref.WeakReference;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.*;
public final class RetentionProbe extends Instrumentation {
 private String stage="start",phase;private Activity activity;private Object session,archive,store;private Class<?> helper;private Bundle args;
 @Override public void onCreate(Bundle b){super.onCreate(b);args=b;start();}
 private static void require(boolean b){if(!b)throw new AssertionError();}
 private void idle(){waitForIdleSync();SystemClock.sleep(80);}
 private Object field(Object owner,String name)throws Exception{Field f=owner.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
 private Object call(Object owner,String name,Class<?>[] types,Object...values)throws Exception{Method m=owner.getClass().getDeclaredMethod(name,types);m.setAccessible(true);return m.invoke(owner,values);}
 private Object call(Object owner,String name)throws Exception{return call(owner,name,new Class<?>[0]);}
 @SuppressWarnings("unchecked") private List<String> saved()throws Exception{return(List<String>)call(store,"snapshot");}
 @SuppressWarnings("unchecked") private List<Object> media()throws Exception{return(List<Object>)call(archive,"media");}
 private void awaitText(String text)throws Exception{long until=SystemClock.elapsedRealtime()+8000;while(SystemClock.elapsedRealtime()<until){idle();if(saved().contains(text))return;}throw new AssertionError();}
 private void durable()throws Exception{call(archive,"flush");((ScheduledExecutorService)field(archive,"worker")).submit(new Runnable(){public void run(){}}).get(15,TimeUnit.SECONDS);require("saved".equals(field(archive,"status")));}
 private void awaitMedia()throws Exception{long until=SystemClock.elapsedRealtime()+15000;while(SystemClock.elapsedRealtime()<until){idle();if(media().size()==2&&((Set<?>)field(archive,"pendingMedia")).isEmpty()){durable();return;}}throw new AssertionError();}
 private View button(View root){if("GeminiPatch saved replies".contentEquals(root.getContentDescription()==null?"":root.getContentDescription()))return root;if(root instanceof ViewGroup){ViewGroup g=(ViewGroup)root;for(int i=0;i<g.getChildCount();i++){View v=button(g.getChildAt(i));if(v!=null)return v;}}return null;}
 private void open(){runOnMainSync(new Runnable(){public void run(){View v=button(activity.getWindow().getDecorView());require(v!=null);v.performClick();}});idle();}
 private View textView(View root,String label,boolean prefix,boolean clickable){if(root instanceof TextView){String text=((TextView)root).getText().toString();if((prefix?text.startsWith(label):text.contains(label))&&(!clickable||root.isClickable()))return root;}if(root instanceof ViewGroup){ViewGroup group=(ViewGroup)root;for(int i=0;i<group.getChildCount();i++){View found=textView(group.getChildAt(i),label,prefix,clickable);if(found!=null)return found;}}return null;}
 private void click(final String label,final boolean prefix)throws Exception{long until=SystemClock.elapsedRealtime()+6000;while(SystemClock.elapsedRealtime()<until){final boolean[] clicked={false};runOnMainSync(new Runnable(){public void run(){try{Object viewer=field(session,"mediaViewer");Dialog d=(Dialog)field(viewer,"dialog");if(d==null||!d.isShowing())d=(Dialog)field(session,"archiveDialog");if(d!=null&&d.isShowing()){View v=textView(d.getWindow().getDecorView(),label,prefix,true);if(v!=null)clicked[0]=v.performClick();}}catch(Exception e){throw new RuntimeException(e);}}});idle();if(clicked[0])return;}throw new AssertionError();}
 private static byte[] read(File file)throws Exception{try(InputStream in=new FileInputStream(file)){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] v=new byte[8192];int n;while((n=in.read(v))!=-1)b.write(v,0,n);return b.toByteArray();}}
 private byte[] asset()throws Exception{try(InputStream in=getContext().getAssets().open("retention-test.mp4")){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] v=new byte[8192];int n;while((n=in.read(v))!=-1)b.write(v,0,n);return b.toByteArray();}}
 private void verifyEncryptedMedia(Bundle result)throws Exception{
  Object files=field(archive,"files");int images=0,videos=0;byte[] original=asset();
  for(Object item:media()){
   String id=(String)field(item,"id");byte[] bytes=(byte[])call(files,"read",new Class<?>[]{String.class,int.class},id+".enc",32*1024*1024);
   if((Integer)field(item,"kind")==1){Bitmap image=BitmapFactory.decodeByteArray(bytes,0,bytes.length);require(image!=null&&image.getPixel(image.getWidth()/2,image.getHeight()/2)==Color.RED);image.recycle();images++;}
   else{require(Arrays.equals(MessageDigest.getInstance("SHA-256").digest(bytes),MessageDigest.getInstance("SHA-256").digest(original)));videos++;}
  }
  require(images==1&&videos==1);result.putBoolean("redImageAndCompleteVideoRetained",true);
  File directory=(File)field(files,"directory");for(File file:directory.listFiles())if(file.getName().endsWith(".enc")){
   byte[] encrypted=read(file);require(encrypted.length>32&&encrypted[0]=='G'&&encrypted[1]=='P'&&encrypted[2]=='R'&&encrypted[3]=='1');
   String raw=new String(encrypted,StandardCharsets.ISO_8859_1);require(!raw.contains("VISIBLE_REPLY_ALPHA")&&!raw.contains("PRIVATE_CANARY")&&!raw.contains("PRIVATE_TOKEN"));
  }
  result.putBoolean("archiveFilesContainCiphertextOnly",true);
 }
 private void verifyViewer(Bundle result)throws Exception{
  stage="archive_viewer_text";open();Dialog archiveDialog=(Dialog)field(session,"archiveDialog");require(archiveDialog!=null&&archiveDialog.isShowing()&&textView(archiveDialog.getWindow().getDecorView(),"VISIBLE_REPLY_ALPHA extended",false,false)!=null);
  stage="archive_image_button";click("Saved image",true);Object viewer=field(session,"mediaViewer");long until=SystemClock.elapsedRealtime()+6000;boolean imageShown=false;
  while(SystemClock.elapsedRealtime()<until){idle();Dialog d=(Dialog)field(viewer,"dialog");if(d!=null&&image(d.getWindow().getDecorView())){imageShown=true;break;}}
  stage="archive_image_render";require(imageShown);click("Close",false);result.putBoolean("savedImageDisplayedAfterRemoval",true);
  stage="archive_video_button";click("Saved video",true);until=SystemClock.elapsedRealtime()+10000;boolean played=false;
  while(SystemClock.elapsedRealtime()<until){idle();MediaPlayer p=(MediaPlayer)field(viewer,"player");try{if(p!=null&&p.getVideoWidth()>0&&p.getDuration()>0){played=true;break;}}catch(IllegalStateException ignored){}}
  stage="archive_video_decode";require(played);click("Close",false);result.putBoolean("encryptedVideoDecodedWithoutPlaintextTempFile",true);
 }
 private boolean image(View view){if(view instanceof ImageView&&((ImageView)view).getDrawable() instanceof BitmapDrawable){Bitmap b=((BitmapDrawable)((ImageView)view).getDrawable()).getBitmap();if(b!=null&&b.getWidth()>=32&&b.getPixel(b.getWidth()/2,b.getHeight()/2)==Color.RED)return true;}if(view instanceof ViewGroup){ViewGroup g=(ViewGroup)view;for(int i=0;i<g.getChildCount();i++)if(image(g.getChildAt(i)))return true;}return false;}
 @Override public void onStart(){Bundle result=new Bundle();try{
  phase=args.getString("phase","setup");String pkg=getTargetContext().getPackageName();require(pkg.equals("com.google.android.googlequicksearchbox.morphe"));
  helper=getTargetContext().getClassLoader().loadClass("app.geminipatch.retention.RetainedReplies");
  stage="launch_activity";activity=startActivitySync(new Intent().setClassName(pkg,phase.equals("native")?"com.google.android.apps.search.assistant.surfaces.voice.robin.main.MainActivity":"app.geminipatch.retentiontest.FixtureActivity").putExtra("phase",phase).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));idle();
  Field sessions=helper.getDeclaredField("SESSIONS");sessions.setAccessible(true);
  for(int i=0;i<80;i++){Object ref=((Map<?,?>)sessions.get(null)).get(activity);if(ref!=null){session=((WeakReference<?>)ref).get();if(session!=null)break;}idle();}
  require(session!=null);archive=field(session,"archive");store=field(session,"store");for(int i=0;i<80&&!Boolean.TRUE.equals(field(archive,"ready"));i++)idle();require(Boolean.TRUE.equals(field(archive,"ready")));
  if(phase.equals("native")){
   stage="native_activity_attachment";require(button(activity.getWindow().getDecorView())!=null);result.putBoolean("nativeMainActivityHasArchiveViewer",true);
  }else if(phase.equals("cleared")){
   stage="clear_survives_process_restart";require(saved().isEmpty()&&media().isEmpty()&&Boolean.FALSE.equals(field(archive,"enabled")));result.putBoolean("clearAndPausePersistAcrossRestart",true);
  }else if(phase.equals("setup")){
   stage="visible_assistant_only";awaitText("VISIBLE_REPLY_ALPHA");for(String text:saved())require(!text.contains("PRIVATE_CANARY"));require(Boolean.FALSE.equals(field(archive,"videosEnabled")));result.putBoolean("userHiddenAndProgressExcluded",true);result.putBoolean("videoNetworkSavingOptIn",true);
   stage="append_then_late_replacement";final TextView reply=(TextView)activity.getClass().getField("reply").get(activity);
   runOnMainSync(new Runnable(){public void run(){reply.setText("VISIBLE_REPLY_ALPHA extended");}});awaitText("VISIBLE_REPLY_ALPHA extended");
   runOnMainSync(new Runnable(){public void run(){reply.setText("Replacement notice");}});awaitText("Replacement notice");require(saved().contains("VISIBLE_REPLY_ALPHA extended"));
   stage="visible_video_source";File source=new File(getTargetContext().getCacheDir(),"retention-test-source.mp4");try(FileOutputStream out=new FileOutputStream(source)){out.write(asset());}
   call(archive,"setVideosEnabled",new Class<?>[]{boolean.class},true);final String uri=android.net.Uri.fromFile(source).toString();final boolean[] installed={false};
   runOnMainSync(new Runnable(){public void run(){try{activity.getClass().getMethod("addVideo",String.class).invoke(activity,uri);installed[0]=true;}catch(Exception ignored){}}});require(installed[0]);awaitMedia();verifyEncryptedMedia(result);
   stage="remove_native_output";runOnMainSync(new Runnable(){public void run(){reply.setText("");((ViewGroup)reply.getParent()).removeView(reply);try{for(String name:new String[]{"picture","video"}){View v=(View)activity.getClass().getField(name).get(activity);((ViewGroup)v.getParent()).removeView(v);}}catch(Exception e){throw new RuntimeException(e);}}});idle();require(saved().contains("VISIBLE_REPLY_ALPHA extended"));require(source.delete());result.putBoolean("textAndMediaSurviveNativeViewRemoval",true);
   stage="native_viewer";verifyViewer(result);click("Close",false);
   stage="authenticated_ciphertext";Object files=field(archive,"files");File index=new File((File)field(files,"directory"),"index.enc");durable();byte[] encrypted=read(index);byte[] damaged=encrypted.clone();damaged[damaged.length-1]^=1;boolean rejected=false;
   try{try(FileOutputStream out=new FileOutputStream(index)){out.write(damaged);}try{call(files,"read",new Class<?>[]{String.class,int.class},"index.enc",1024*1024);}catch(InvocationTargetException e){rejected=e.getCause() instanceof java.security.GeneralSecurityException;}}
   finally{try(FileOutputStream out=new FileOutputStream(index)){out.write(encrypted);out.getFD().sync();}}
   require(rejected);result.putBoolean("ciphertextTamperRejected",true);durable();
  }else if(phase.equals("reload")){
   stage="restore_after_process_restart";awaitText("VISIBLE_REPLY_ALPHA extended");verifyEncryptedMedia(result);verifyViewer(result);result.putBoolean("textImageAndVideoRestoredAfterProcessRestart",true);
   stage="cancel_delayed_media_viewer";final CountDownLatch loadEntered=new CountDownLatch(1),loadRelease=new CountDownLatch(1);final ScheduledExecutorService writer=(ScheduledExecutorService)field(archive,"worker");
   writer.execute(new Runnable(){public void run(){loadEntered.countDown();try{loadRelease.await(10,TimeUnit.SECONDS);}catch(InterruptedException ignored){}}});require(loadEntered.await(5,TimeUnit.SECONDS));
   try{click("Saved image",true);click("Cancel",false);}finally{loadRelease.countDown();}writer.submit(new Runnable(){public void run(){}}).get(15,TimeUnit.SECONDS);idle();Dialog cancelled=(Dialog)field(field(session,"mediaViewer"),"dialog");require(cancelled==null||!cancelled.isShowing());result.putBoolean("cancelDoesNotReopenDelayedViewer",true);
   stage="manual_clear_with_media_pending";final CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);ExecutorService mediaWorker=(ExecutorService)field(archive,"mediaWorker");
   mediaWorker.execute(new Runnable(){public void run(){entered.countDown();try{release.await(10,TimeUnit.SECONDS);}catch(InterruptedException ignored){}}});require(entered.await(5,TimeUnit.SECONDS));
   try{Bitmap pending=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);pending.eraseColor(Color.YELLOW);require((Boolean)call(archive,"image",new Class<?>[]{Bitmap.class,String.class},pending,"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));click("Clear & pause",false);}finally{release.countDown();}
   mediaWorker.submit(new Runnable(){public void run(){}}).get(15,TimeUnit.SECONDS);writer.submit(new Runnable(){public void run(){}}).get(15,TimeUnit.SECONDS);require(saved().isEmpty()&&media().isEmpty()&&Boolean.FALSE.equals(field(archive,"enabled")));result.putBoolean("queuedMediaCannotReappearAfterClear",true);
   File directory=(File)field(field(archive,"files"),"directory");File[] remaining=directory.listFiles();require(remaining==null||remaining.length==0);result.putBoolean("manualClearRemovesEncryptedTextAndMedia",true);
  }else throw new AssertionError();
  result.putString("phase",phase);result.putString("result","PASS");
 }catch(Throwable e){result.putString("result","FAIL");result.putString("stage",stage);result.putString("errorType",e.getClass().getSimpleName());if(e.getStackTrace().length>0)result.putInt("sourceLine",e.getStackTrace()[0].getLineNumber());if(e.getCause()!=null)result.putString("causeType",e.getCause().getClass().getSimpleName());}
 finally{runOnMainSync(new Runnable(){public void run(){if(activity!=null)activity.finish();}});}
 finish("PASS".equals(result.getString("result"))?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
}
