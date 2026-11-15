package app.geminipatch.diagnostics;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.TreeSet;

/** Fixed version/configuration identities only; no account, content, APK path or signature data. */
final class BuildAudit {
 private static JSONObject installed;
 static String version(String text){return text!=null&&text.matches("[0-9]+\\.[0-9]+\\.[0-9]+(?:-(?:preview|diagnostics)\\.[0-9]+)?")?text:"unavailable";}
 private static String hash(String text){return text!=null&&text.matches("[a-f0-9]{64}")?text:"unavailable";}
 static synchronized JSONObject snapshot(Context context,int historyCalls,int streamCalls)throws Exception{
  if(installed==null){
   JSONObject value=new JSONObject().put("runtimeBuild",Schema.BUILD).put("profileSha256",Schema.PROFILE_SHA256);
   if(android.os.Build.VERSION.SDK_INT>=28){String process=android.app.Application.getProcessName();value.put("processRole",process!=null&&process.endsWith(":search")?"search":process!=null&&process.equals(context.getPackageName())?"main":"other");}
   try{
    PackageInfo pkg=context.getPackageManager().getPackageInfo(context.getPackageName(),0);
    value.put("packageLastUpdateTimeMs",pkg.lastUpdateTime).put("packageManagerReportedCode",android.os.Build.VERSION.SDK_INT>=28?pkg.getLongVersionCode():pkg.versionCode);
    String name=pkg.versionName;value.put("packageVersion",name!=null&&name.matches("[0-9A-Za-z._-]{1,90}")?name:"unavailable");
    ApplicationInfo app=context.getPackageManager().getApplicationInfo(context.getPackageName(),PackageManager.GET_META_DATA);
    String marker=app.metaData==null?null:app.metaData.getString("app.geminipatch.conversation_update_compatibility");
    value.put("compatibilityPatchBuild",marker==null?"not_marked":version(marker));
   }catch(Throwable ignored){value.put("compatibilityPatchBuild","unavailable");}
   try{
    Class<?> helper=context.getClassLoader().loadClass("app.geminipatch.compat.HistoryUpdateCompatibility");
    value.put("compatibilityRuntimeBuild",version((String)helper.getMethod("buildIdentity").invoke(null)));
    value.put("compatibilityProfileSha256",hash((String)helper.getMethod("profileIdentity").invoke(null)));
    value.put("historyMethodPresent",helper.getMethod("filter",Object.class)!=null).put("streamMethodPresent",helper.getMethod("filterStream",Object.class)!=null);
   }catch(Throwable ignored){value.put("compatibilityRuntimeBuild","unavailable");}
   String marker=value.optString("compatibilityPatchBuild"),helper=value.optString("compatibilityRuntimeBuild");
   String state="unavailable";
   if(marker.equals("not_marked"))state="not_marked";
   else if(!version(marker).equals("unavailable")&&!version(helper).equals("unavailable"))state=marker.equals(Schema.BUILD)&&helper.equals(Schema.BUILD)&&value.optString("compatibilityProfileSha256").equals(Schema.PROFILE_SHA256)?"ready":"mismatch";
   value.put("compatibilityState",state);installed=value;
  }
  return new JSONObject(installed.toString()).put("historyHookCallsSinceProcessStart",historyCalls).put("streamHookCallsSinceProcessStart",streamCalls);
 }
 private static boolean add(TreeSet<String> versions,String value){String safe=version(value);if(safe.equals("unavailable"))return false;versions.add(safe);return true;}
 static JSONObject evaluate(JSONObject report,JSONArray events)throws Exception{
  TreeSet<String> builds=new TreeSet<String>();boolean known=add(builds,report.optString("build"));boolean profileMismatch=false;
  if(!report.optBoolean("capturePresent"))return new JSONObject().put("state","no_capture").put("observedBuilds",new JSONArray(builds));
  known&=add(builds,report.optString("captureBuild"));
  JSONArray processes=report.optJSONArray("processes");
  if(processes==null||processes.length()==0)known=false;
  else for(int i=0;i<processes.length();i++){
   JSONObject identity=processes.getJSONObject(i).optJSONObject("identity");
   if(identity==null){known=false;continue;}
   known&=add(builds,identity.optString("runtimeBuild"));
   String profile=identity.optString("profileSha256");if(!hash(profile).equals("unavailable"))profileMismatch|=!profile.equals(Schema.PROFILE_SHA256);else known=false;
   String helper=identity.optString("compatibilityRuntimeBuild");if(!version(helper).equals("unavailable"))add(builds,helper);
   profileMismatch|=identity.optString("compatibilityState").equals("mismatch");
  }
  int headers=0;for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i);if(e.optString("event").equals("capture_started")){headers++;known&=add(builds,e.optString("diagnosticBuild"));}}
  known&=headers==1;
  return new JSONObject().put("state",builds.size()>1||profileMismatch?"mismatch":known?"consistent":"unknown").put("observedBuilds",new JSONArray(builds));
 }
}
