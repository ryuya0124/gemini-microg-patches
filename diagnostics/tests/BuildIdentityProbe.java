package app.geminipatch.diagnosticstest;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Bundle;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Private account-free emulator checks for installed/capture/helper identity and stale exports. */
public final class BuildIdentityProbe extends Instrumentation {
 private Context target;private Class<?> capture,reply,audit;private String stage="initialize";private Activity screen;private File exported;
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 private static void require(boolean value){if(!value)throw new AssertionError();}
 private void action(String name,File file)throws Exception{
  final CountDownLatch latch=new CountDownLatch(1);final boolean[] ok={false};
  Object callback=Proxy.newProxyInstance(reply.getClassLoader(),new Class<?>[]{reply},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){if(m.getName().equals("done")){ok[0]=(Boolean)a[0];latch.countDown();}return null;}});
  if(name.equals("start"))capture.getMethod(name,Context.class,boolean.class,reply).invoke(null,target,true,callback);
  else if(name.equals("save"))capture.getMethod(name,Context.class,Uri.class,reply).invoke(null,target,Uri.fromFile(file),callback);
  else capture.getMethod(name,Context.class,reply).invoke(null,target,callback);
  require(latch.await(25,TimeUnit.SECONDS)&&ok[0]);
 }
 private JSONObject read(File file)throws Exception{try(InputStream in=new FileInputStream(file)){ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] data=new byte[8192];int n;while((n=in.read(data))!=-1)b.write(data,0,n);return new JSONObject(new String(b.toByteArray(),StandardCharsets.UTF_8));}}
 private String evaluate(JSONObject report)throws Exception{Method m=audit.getDeclaredMethod("evaluate",JSONObject.class,JSONArray.class);m.setAccessible(true);return((JSONObject)m.invoke(null,report,report.getJSONArray("events"))).getString("state");}
 @Override public void onStart(){Bundle result=new Bundle();try{
  target=getTargetContext();require(target.getPackageName().equals("com.google.android.googlequicksearchbox.morphe"));ClassLoader l=target.getClassLoader();
  capture=l.loadClass("app.geminipatch.diagnostics.Capture");reply=l.loadClass("app.geminipatch.diagnostics.Capture$Reply");audit=l.loadClass("app.geminipatch.diagnostics.BuildAudit");capture.getMethod("init",Context.class).invoke(null,target);
  action("delete",null);require("0.3.7".equals(capture.getMethod("build").invoke(null)));require(((String)capture.getMethod("reportFileName").invoke(null)).matches("GeminiPatch-diagnostics-0\\.3\\.7-[0-9]+\\.json"));
  stage="installed_patch_and_helper_identity";JSONObject identity=(JSONObject)capture.getMethod("identity").invoke(null);
  require(identity.getString("runtimeBuild").equals("0.3.7")&&identity.getString("compatibilityPatchBuild").equals("0.3.7")&&identity.getString("compatibilityRuntimeBuild").equals("0.3.7")&&identity.getString("compatibilityState").equals("ready"));
  require(identity.getString("profileSha256").matches("[a-f0-9]{64}")&&identity.getString("profileSha256").equals(identity.getString("compatibilityProfileSha256")));result.putBoolean("installedMarkerHelperAndProfileMatch",true);
  stage="versioned_diagnostic_screen";screen=startActivitySync(new Intent().setClassName(target.getPackageName(),"app.geminipatch.diagnostics.CaptureActivity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));waitForIdleSync();require(screen.getTitle().toString().equals("Gemini diagnostics 0.3.7"));result.putBoolean("diagnosticScreenAndFilenameShow037",true);
  stage="fresh_capture_identity";action("start",null);
  Class<?> helper=l.loadClass("app.geminipatch.compat.HistoryUpdateCompatibility");helper.getMethod("filter",Object.class).invoke(null,new Object[]{null});helper.getMethod("filterStream",Object.class).invoke(null,new Object[]{null});
  action("stop",null);exported=target.getFileStreamPath("geminipatch-identity-probe.json");action("save",exported);JSONObject report=read(exported);
  require(report.getString("build").equals("0.3.7")&&report.getString("captureBuild").equals("0.3.7")&&report.getJSONObject("buildIdentity").getString("state").equals("consistent"));
  require(report.getLong("captureStartedAtMs")>0&&report.getLong("exportedAtMs")>=report.getLong("captureStartedAtMs"));
  JSONArray processes=report.getJSONArray("processes");boolean entered=false;for(int i=0;i<processes.length();i++){JSONObject id=processes.getJSONObject(i).getJSONObject("identity");require(id.getString("runtimeBuild").equals("0.3.7"));entered|=id.getInt("historyHookCallsSinceProcessStart")>0&&id.getInt("streamHookCallsSinceProcessStart")>0;}require(entered);result.putBoolean("perProcessBuildAndBothHookEntriesRecorded",true);
  stage="mixed_build_detection";JSONObject changed=new JSONObject(report.toString());changed.put("captureBuild","0.3.6-preview.2");require(evaluate(changed).equals("mismatch"));
  changed=new JSONObject(report.toString());changed.getJSONArray("processes").getJSONObject(0).getJSONObject("identity").put("runtimeBuild","0.3.6-preview.2");require(evaluate(changed).equals("mismatch"));
  changed=new JSONObject(report.toString());changed.getJSONArray("processes").getJSONObject(0).remove("identity");require(evaluate(changed).equals("unknown"));
  changed=new JSONObject(report.toString());changed.getJSONArray("processes").getJSONObject(0).getJSONObject("identity").put("profileSha256","aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");require(evaluate(changed).equals("mismatch"));result.putBoolean("mixedLegacyAndWrongProfileIdentitiesDetected",true);
  stage="old_capture_real_export";File folder=new File(target.getFilesDir(),"geminipatch-conversation-diagnostics"),control=new File(folder,"control.json");JSONObject old=read(control);old.put("captureBuild","0.3.6-preview.2");File pending=new File(folder,"identity-probe.pending");try(FileOutputStream out=new FileOutputStream(pending)){out.write(old.toString().getBytes(StandardCharsets.UTF_8));out.getFD().sync();}require(pending.renameTo(control));
  action("save",exported);JSONObject stale=read(exported);require(stale.getString("build").equals("0.3.7")&&stale.getString("captureBuild").equals("0.3.6-preview.2")&&stale.getJSONObject("buildIdentity").getString("state").equals("mismatch"));result.putBoolean("oldCaptureCannotMasqueradeAsFresh037Export",true);
  stage="fresh_capture_reset";action("start",null);action("stop",null);action("save",exported);require(read(exported).getJSONObject("buildIdentity").getString("state").equals("consistent"));result.putBoolean("newCaptureResetsStaleIdentity",true);result.putString("result","PASS");
 }catch(Throwable error){result.putString("result","FAIL");result.putString("stage",stage);result.putString("errorType",error.getClass().getSimpleName());if(error.getCause()!=null)result.putString("causeType",error.getCause().getClass().getSimpleName());}
 finally{try{if(capture!=null){action("stop",null);action("delete",null);}if(exported!=null)exported.delete();if(screen!=null)runOnMainSync(new Runnable(){public void run(){screen.finish();}});}catch(Throwable ignored){}}
 finish("PASS".equals(result.getString("result"))?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
 }
}
