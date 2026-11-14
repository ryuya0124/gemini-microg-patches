package app.geminipatch.updatecompattest;
import android.app.*;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

public final class UpdateCompatibilityProbe extends Instrumentation {
 private Bundle args;private Context target;private ClassLoader loader;private Class<?> capture,reply;private String stage="initialize";
 @Override public void onCreate(Bundle a){super.onCreate(a);args=a;start();}
 private static void require(boolean value){if(!value)throw new AssertionError();}
 private static byte[] varint(int n){ByteArrayOutputStream b=new ByteArrayOutputStream();while((n&~127)!=0){b.write((n&127)|128);n>>>=7;}b.write(n);return b.toByteArray();}
 private static byte[] concat(byte[]...parts){ByteArrayOutputStream b=new ByteArrayOutputStream();for(byte[] p:parts)b.write(p,0,p.length);return b.toByteArray();}
 private static byte[] bytes(int f,byte[] value){return concat(varint((f<<3)|2),varint(value.length),value);}
 private static byte[] text(int f,String value){return bytes(f,value.getBytes(StandardCharsets.UTF_8));}
 private static byte[] number(int f,int value){return concat(varint(f<<3),varint(value));}
 private Object parse(String name,byte[] wire)throws Exception{Class<?> lite=loader.loadClass("gtpr"),type=loader.loadClass(name);return lite.getMethod("parseFrom",lite,byte[].class).invoke(null,type.getField("a").get(null),wire);}
 private byte[] wire(Object value)throws Exception{return(byte[])loader.loadClass("com.google.protobuf.MessageLite").getMethod("toByteArray").invoke(value);}
 private void action(final String name,File destination)throws Exception{
  final CountDownLatch latch=new CountDownLatch(1);final boolean[] ok={false};
  Object callback=Proxy.newProxyInstance(reply.getClassLoader(),new Class<?>[]{reply},new InvocationHandler(){public Object invoke(Object p,Method m,Object[] a){if(m.getName().equals("done")){ok[0]=(Boolean)a[0];latch.countDown();}return null;}});
  if(name.equals("start"))capture.getMethod(name,Context.class,boolean.class,reply).invoke(null,target,true,callback);
  else if(name.equals("save"))capture.getMethod(name,Context.class,Uri.class,reply).invoke(null,target,Uri.fromFile(destination),callback);
  else capture.getMethod(name,Context.class,reply).invoke(null,target,callback);
  require(latch.await(25,TimeUnit.SECONDS)&&ok[0]);
 }
 private void unit(Bundle result)throws Exception{
  Method filter=loader.loadClass("app.geminipatch.compat.HistoryUpdateCompatibility").getMethod("filter",Object.class);
  String canary="PRIVATE_CANARY user@example.invalid https://private.invalid/SECRET_CONVERSATION_ID";
  byte[] rest=concat(bytes(1,text(8,canary)),text(2,canary),number(99,7));
  byte[] punt=concat(text(1,"Conversation requires an update"),text(2,"This conversation was created with a newer version of Gemini. "+canary),number(4,1));
  Object original=parse("gffq",concat(rest,bytes(3,punt))),expected=parse("gffq",rest);byte[] before=wire(original);
  Object fixed=filter.invoke(null,original);require(fixed!=original&&Arrays.equals(wire(fixed),wire(expected))&&Arrays.equals(wire(original),before));
  require(fixed.getClass().getField("d").get(fixed)==null&&(fixed.getClass().getField("b").getInt(fixed)&1)==0);
  require(((List<?>)fixed.getClass().getField("c").get(fixed)).size()==1);
  result.putBoolean("historyPaginationAndUnknownFieldsPreserved",true);result.putBoolean("originalResponseUnchanged",true);
  for(int behavior:new int[]{0,2,3,4,99}){Object other=parse("gffq",concat(rest,bytes(3,concat(text(1,"Conversation requires an update"),text(2,"This conversation was created with a newer version of Gemini."),number(4,behavior)))));require(filter.invoke(null,other)==other);}
  Object unrelated=parse("gffq",concat(rest,bytes(3,concat(text(1,"Temporarily unavailable"),text(2,canary),number(4,1)))));require(filter.invoke(null,unrelated)==unrelated);
  Object titleOnly=parse("gffq",concat(rest,bytes(3,concat(text(1,"Conversation requires an update"),text(2,"Different reason"),number(4,1)))));require(filter.invoke(null,titleOnly)==titleOnly);
  Object noPunt=parse("gffq",concat(rest,bytes(1,text(8,"Conversation requires an update. This conversation was created with a newer version of Gemini."))));require(filter.invoke(null,noPunt)==noPunt);
  require(filter.invoke(null,"unrelated object").equals("unrelated object"));
  result.putBoolean("otherPuntsBehaviorsAndQuotedTextUnchanged",true);result.putBoolean("nativeProtobufSelectiveRemovalPassed",true);
 }
 private void export(int mode,Bundle result)throws Exception{
  action("stop",null);File f=target.getFileStreamPath("geminipatch-update-fixture-export.json");action("save",f);
  ByteArrayOutputStream out=new ByteArrayOutputStream();try(InputStream in=new FileInputStream(f)){byte[] b=new byte[4096];int n;while((n=in.read(b))>=0)out.write(b,0,n);}
  String raw=new String(out.toByteArray(),StandardCharsets.UTF_8);JSONObject j=new JSONObject(raw);
  require(j.getBoolean("syntheticTest")&&!raw.contains("PRIVATE_CANARY")&&!raw.contains("SECRET_CONVERSATION_ID")&&!raw.contains("user@example.invalid"));
  boolean before=false,after=false,banner=false,setter=false;JSONArray events=j.getJSONArray("events");Map<String,Integer> counts=new TreeMap<String,Integer>();
  for(int i=0;i<events.length();i++){JSONObject e=events.getJSONObject(i);require(e.getBoolean("syntheticTest"));String name=e.getString("event");counts.put(name,counts.containsKey(name)?counts.get(name)+1:1);
   require(!name.equals("observer_unavailable"));
   if(name.equals("conversation_punt")){String source=e.getString("source");boolean hit=e.optBoolean("knownWarningTitle")&&e.optBoolean("knownWarningBody");before|=source.equals("history_response")&&hit;after|=source.equals("history_response")&&!e.getBoolean("present");banner|=source.equals("banner")&&hit;}
   setter|=name.equals("known_warning_setter");
  }
  require(before&&j.getLong("minimumObservedLoss")==0);
  if(mode==1)require(banner&&setter);else if(mode==2)require(after&&!banner&&!setter);else throw new AssertionError();
  JSONObject summary=new JSONObject();summary.put("mode",mode).put("syntheticTest",true).put("originalWarningObserved",before).put("filteredHistoryObserved",after).put("nativeWarningBanner",banner).put("nativeWarningSetter",setter).put("canaryExcluded",true).put("minimumObservedLoss",j.getLong("minimumObservedLoss")).put("allStoppedProcessesAcknowledged",j.getBoolean("allStoppedProcessesAcknowledged")).put("incomplete",j.getBoolean("incomplete")).put("events",new JSONObject(counts));
  result.putString("summary",summary.toString());result.putBoolean("syntheticNativeUiCaptureValidated",true);
 }
 @Override public void onStart(){Bundle result=new Bundle();try{
  target=getTargetContext();require(target.getPackageName().equals("com.google.android.googlequicksearchbox.morphe"));loader=target.getClassLoader();capture=loader.loadClass("app.geminipatch.diagnostics.Capture");reply=loader.loadClass("app.geminipatch.diagnostics.Capture$Reply");capture.getMethod("init",Context.class).invoke(null,target);
  String command=args.getString("command","unit");int mode=Integer.parseInt(args.getString("mode","0"));stage=command;
  if(command.equals("unit"))unit(result);
  else if(command.equals("start")){require(mode==1||mode==2);try(OutputStream out=new FileOutputStream(target.getFileStreamPath("geminipatch-update-fixture-mode"))){out.write(mode);}action("start",null);result.putInt("syntheticMode",mode);}
  else if(command.equals("export"))export(mode,result);
  else if(command.equals("cleanup")){action("stop",null);action("delete",null);File control=target.getFileStreamPath("geminipatch-update-fixture-mode");require(!control.exists()||control.delete());File exported=target.getFileStreamPath("geminipatch-update-fixture-export.json");require(!exported.exists()||exported.delete());result.putBoolean("fixtureAndCaptureRemoved",true);}
  else throw new AssertionError();result.putString("result","PASS");
 }catch(Throwable error){result.putString("result","FAIL");result.putString("stage",stage);result.putString("errorType",error.getClass().getSimpleName());if(error.getCause()!=null)result.putString("causeType",error.getCause().getClass().getSimpleName());}finish("PASS".equals(result.getString("result"))?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);}
}
