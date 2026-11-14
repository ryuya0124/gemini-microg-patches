package app.geminipatch.diagnosticstest;

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import org.json.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/** Same-key emulator instrumentation. Synthetic status stays local; no server request is forged. */
public final class NativeCaptureProbe extends Instrumentation {
    private Class<?> capture, replyType;
    private Context target;
    private String stage="initialize";
    private boolean stress;
    @Override public void onCreate(Bundle args) { super.onCreate(args); stress=args!=null&&"true".equals(args.getString("stress")); start(); }
    private static void require(boolean b) { if (!b) throw new AssertionError(); }
    private Object callback(final CountDownLatch latch, final boolean[] result) {
        return Proxy.newProxyInstance(replyType.getClassLoader(),new Class<?>[]{replyType},new InvocationHandler(){
            public Object invoke(Object proxy,Method m,Object[] args){
                if(m.getName().equals("done")){result[0]=(Boolean)args[0];latch.countDown();}
                return null;
            }
        });
    }
    private void action(String name,File destination,boolean synthetic) throws Exception {
        CountDownLatch latch=new CountDownLatch(1);boolean[] ok={false};Object callback=callback(latch,ok);
        if(name.equals("start"))capture.getMethod(name,Context.class,boolean.class,replyType).invoke(null,target,synthetic,callback);
        else if(name.equals("save"))capture.getMethod(name,Context.class,Uri.class,replyType).invoke(null,target,Uri.fromFile(destination),callback);
        else capture.getMethod(name,Context.class,replyType).invoke(null,target,callback);
        require(latch.await(20,TimeUnit.SECONDS)&&ok[0]);
    }
    private static byte[] varint(int n){ByteArrayOutputStream b=new ByteArrayOutputStream();while((n&~127)!=0){b.write((n&127)|128);n>>>=7;}b.write(n);return b.toByteArray();}
    private static byte[] concat(byte[]...parts){ByteArrayOutputStream b=new ByteArrayOutputStream();for(byte[] p:parts)b.write(p,0,p.length);return b.toByteArray();}
    private static byte[] number(int field,int n){return concat(varint(field<<3),varint(n));}
    private static byte[] bytes(int field,byte[] value){return concat(varint((field<<3)|2),varint(value.length),value);}
    private static byte[] text(int field,String value){return bytes(field,value.getBytes(StandardCharsets.UTF_8));}
    private static String read(File f) throws Exception {
        ByteArrayOutputStream b=new ByteArrayOutputStream();try(InputStream in=new FileInputStream(f)){byte[] data=new byte[4096];int n;while((n=in.read(data))>=0)b.write(data,0,n);}return new String(b.toByteArray(),StandardCharsets.UTF_8);
    }
    @Override public void onStart(){
        Bundle result=new Bundle();File exported=null;
        try{
            target=getTargetContext();require(target.getPackageName().equals("com.google.android.googlequicksearchbox.morphe"));
            ClassLoader l=target.getClassLoader();capture=l.loadClass("app.geminipatch.diagnostics.Capture");replyType=l.loadClass("app.geminipatch.diagnostics.Capture$Reply");
            capture.getMethod("init",Context.class).invoke(null,target);
            action("delete",null,false);
            require(!((Boolean)capture.getMethod("active").invoke(null)));
            exported=new File(target.getFilesDir(),"geminipatch-synthetic-export.json");
            action("save",exported,false);JSONObject empty=new JSONObject(read(exported));
            require(!empty.getBoolean("capturePresent")&&empty.getJSONArray("events").length()==0);
            result.putBoolean("captureOffByDefault",true);
            stage="start_synthetic_capture";action("start",null,true);
            Class<?> lite=l.loadClass("gtpr");Method parse=lite.getMethod("parseFrom",lite,byte[].class);
            String canary="PRIVATE_CANARY user@example.invalid https://private.invalid/chat/SECRET_CONVERSATION_ID";
            Class<?> metadataProto=l.loadClass("gfnf");
            Object metadata=parse.invoke(null,metadataProto.getField("a").get(null),concat(text(1,canary),text(2,canary)));
            final CountDownLatch entered=new CountDownLatch(1), release=new CountDownLatch(1);
            if(stress){
                Field queue=capture.getDeclaredField("TASKS");queue.setAccessible(true);
                @SuppressWarnings("unchecked") java.util.concurrent.BlockingDeque<Runnable> tasks=(java.util.concurrent.BlockingDeque<Runnable>)queue.get(null);
                tasks.offerLast(new Runnable(){public void run(){entered.countDown();try{release.await(15,TimeUnit.SECONDS);}catch(InterruptedException ignored){}}});
                require(entered.await(5,TimeUnit.SECONDS));
            }
            try {
                for(int i=0;i<(stress?5000:1);i++)capture.getMethod("incoming",Object.class).invoke(null,metadata);
                if(stress){
                    stage="native_punt_submessages";
                    byte[] puntBytes=concat(text(1,"Conversation requires an update "+canary),text(2,"This conversation was created with a newer version of Gemini. "+canary),number(4,1));
                    Class<?> punt=l.loadClass("ghto"),history=l.loadClass("gffq"),generate=l.loadClass("gfeh");
                    Object nativePunt=parse.invoke(null,punt.getField("a").get(null),puntBytes);
                    Object nativeHistory=parse.invoke(null,history.getField("a").get(null),bytes(3,puntBytes));
                    Object nativeGenerate=parse.invoke(null,generate.getField("a").get(null),bytes(13,puntBytes));
                    require(nativeHistory.getClass().getField("d").get(nativeHistory)!=null);
                    require(nativeGenerate.getClass().getField("p").get(nativeGenerate)!=null);
                    capture.getMethod("incoming",Object.class).invoke(null,nativeHistory);
                    capture.getMethod("history",Object.class).invoke(null,nativeHistory);
                    capture.getMethod("incoming",Object.class).invoke(null,nativeGenerate);
                    capture.getMethod("puntRendered",Object.class).invoke(null,nativePunt);
                    // A normal response must explicitly report an absent thread punt.
                    capture.getMethod("incoming",Object.class).invoke(null,history.getField("a").get(null));
                    Class<?> turn=l.loadClass("gfnl");Object value=parse.invoke(null,turn.getField("a").get(null),text(8,canary));
                    capture.getMethod("incoming",Object.class).invoke(null,value);
                    Class<?> frame=l.loadClass("gigv");capture.getMethod("stream",Object.class).invoke(null,frame.getField("a").get(null));
                    capture.getMethod("requestVersion",String.class).invoke(null,"17.72.17.ve.arm64");
                }
            Class<?> caps=l.loadClass("gfcc");capture.getMethod("capabilities",Object.class).invoke(null,caps.getField("a").get(null));
            capture.getMethod("version",String.class).invoke(null,"17.72.17.ve.arm64");
            stage="native_status_parser";
            byte[] robin=concat(number(1,1),text(2,"Conversation requires an update. "+canary),number(3,1086));
            byte[] any=concat(text(1,"type.googleapis.com/assistant.mobile.robin.RobinStatus"),bytes(2,robin));
            Class<?> rpc=l.loadClass("gzvs");Object envelope=parse.invoke(null,rpc.getField("a").get(null),concat(number(1,9),bytes(3,any)));
            Class<?> parser=l.loadClass("atuc");Field key=parser.getDeclaredField("b");key.setAccessible(true);
            Class<?> trailersClass=l.loadClass("hgfy");Object trailers=trailersClass.getConstructor().newInstance();
            Method put=null;
            for(Method m:trailersClass.getDeclaredMethods())if(m.getReturnType()==void.class&&m.getParameterTypes().length==2&&m.getParameterTypes()[0].isInstance(key.get(null))&&m.getParameterTypes()[1]==Object.class){require(put==null);put=m;}
            require(put!=null);put.invoke(trailers,key.get(null),envelope);
            Class<?> status=l.loadClass("io.grpc.Status"),exception=l.loadClass("io.grpc.StatusException");
            Throwable error=(Throwable)exception.getConstructor(status,trailersClass).newInstance(status.getMethod("fromCodeValue",int.class).invoke(null,9),trailers);
            Object detail=parser.getMethod("a",Throwable.class).invoke(null,error);
            require(detail!=null&&detail.getClass().getField("c").getInt(detail)==1086);
            require("UNSUPPORTED_CLIENT_TOO_OLD".equals(l.loadClass("atuf").getMethod("b",Throwable.class).invoke(null,error)));
            } finally { release.countDown(); }
            if(stress){
                stage="native_text_setter";
                final Method setter=l.loadClass("tm").getDeclaredMethod("o",android.widget.TextView.class,CharSequence.class);
                setter.setAccessible(true);
                final String warningText="Conversation requires an update. "+canary;
                final boolean[] unchanged={false};
                final String[] rendererError={null};
                runOnMainSync(new Runnable(){public void run(){try{
                    android.widget.TextView view=new android.widget.TextView(target);setter.invoke(null,view,warningText);
                    unchanged[0]=warningText.contentEquals(view.getText());
                }catch(Exception e){rendererError[0]=e.getClass().getSimpleName();}}});
                if(rendererError[0]!=null)result.putString("rendererErrorType",rendererError[0]);
                require(unchanged[0]);result.putBoolean("nativeTextSetterResultPreserved",true);
            }
            // Stop must drain earlier observations; it must not overtake the error event.
            stage="stop_and_export";action("stop",null,false);action("save",exported,false);
            String serialized=read(exported);require(serialized.length()<1024*1024);
            stage="validate_redaction";
            require(!serialized.contains("PRIVATE_CANARY")&&!serialized.contains("SECRET_CONVERSATION_ID")&&!serialized.contains("@")&&!serialized.contains("https://"));
            JSONObject report=new JSONObject(serialized);require(report.getBoolean("syntheticTest"));
            result.putBoolean("canaryTextAndIdentifiersExcluded",true);
            stage="validate_events";
            JSONArray events=report.getJSONArray("events");boolean wire=false,meta=false,cap=false,stopped=false,header=false,available=true,turn=false,stream=false,request=false,setter=false,historyPunt=false,streamPunt=false,bannerPunt=false,absentPunt=false;
            java.util.TreeSet<String> kinds=new java.util.TreeSet<String>(),missing=new java.util.TreeSet<String>();
            for(int i=0;i<events.length();i++){
                JSONObject e=events.getJSONObject(i);require(e.getBoolean("syntheticTest"));String kind=e.getString("event");
                kinds.add(kind);
                if(kind.equals("observer_unavailable")){available=false;missing.add(e.optString("stage"));}
                if(kind.equals("server_status"))wire|=e.getInt("wireCode")==1086&&e.getString("error").equals("UNSUPPORTED_CLIENT_TOO_OLD")&&e.getBoolean("knownWarningText");
                if(kind.equals("parsed_message"))meta|=e.getString("schema").equals("parsed_conversation_metadata")&&e.getBoolean("textScanPerformed");
                if(kind.equals("parsed_message"))turn|=e.getString("schema").equals("parsed_conversation_turn")&&e.getBoolean("textScanPerformed");
                if(kind.equals("incoming_stream"))stream=true;
                if(kind.equals("request_client_version"))request=true;
                if(kind.equals("known_warning_setter"))setter|=e.getJSONArray("callers").toString().contains("tm#o");
                if(kind.equals("conversation_punt")){
                    String source=e.getString("source");boolean hit=e.optBoolean("knownWarningTitle")&&e.optBoolean("knownWarningBody")&&e.optInt("behavior")==1;
                    historyPunt|=source.equals("history_response")&&hit;
                    streamPunt|=source.equals("stream_thread")&&hit;
                    bannerPunt|=source.equals("banner")&&hit;
                    absentPunt|=source.equals("history_response")&&!e.getBoolean("present");
                }
                if(kind.equals("capabilities"))cap=true;
                if(kind.equals("capture_stopped"))stopped=true;
                if(kind.equals("capture_started")) {
                    JSONObject packages=e.getJSONObject("relevantPackages");
                    header=packages.length()==4&&packages.has("com.google.android.googlequicksearchbox")&&packages.has("com.google.android.googlequicksearchbox.morphe")
                            &&packages.has("com.google.android.apps.bard")&&packages.has("com.google.android.apps.bard.morphe");
                }
            }
            result.putString("observedEventKinds",kinds.toString());result.putString("unavailableStages",missing.toString());
            result.putBoolean("wireStatusObserved",wire);result.putBoolean("metadataObserved",meta);result.putBoolean("capabilitiesObserved",cap);result.putBoolean("stopObserved",stopped);result.putBoolean("headerObserved",header);
            if(stress){
                result.putBoolean("burstTurnObserved",turn);result.putBoolean("burstStreamObserved",stream);result.putBoolean("burstRequestVersionObserved",request);
                stage="validate_burst_retention";require(turn&&stream&&request&&cap&&wire);
                require(report.getBoolean("allStoppedProcessesAcknowledged")&&!report.getBoolean("incomplete")&&report.getLong("minimumObservedLoss")==0);
                JSONArray processes=report.getJSONArray("processes");boolean merged=false;
                for(int i=0;i<processes.length();i++)merged|=processes.getJSONObject(i).getJSONObject("admission").optLong("merged:incoming:parsed_conversation_metadata")>4000;
                require(merged);result.putBoolean("historyBurst5000AllOtherKindsRetained",true);result.putBoolean("processLossSummaryVerified",true);
                require(setter);result.putBoolean("nativeWarningSetterCallsiteCaptured",true);
                stage="validate_punt_submessages";require(historyPunt&&streamPunt&&bannerPunt&&absentPunt);
                result.putBoolean("nativePuntSubmessagesCapturedWithoutText",true);
            }
            require(available);
            require(wire&&meta&&cap&&stopped&&header);
            result.putBoolean("nativeWire1086Captured",true);result.putBoolean("incomingMetadataScanned",true);
            result.putBoolean("canaryTextAndIdentifiersExcluded",true);result.putBoolean("stopPreservesPendingErrors",true);
            result.putBoolean("syntheticReportExplicitlyMarked",true);result.putString("result","PASS");
        }catch(Throwable e){result.putString("result","FAIL");result.putString("stage",stage);result.putString("errorType",e.getClass().getSimpleName());}
        finally{
            try{if(capture!=null){action("stop",null,false);action("delete",null,false);result.putBoolean("diagnosticFilesCleared",true);}}catch(Throwable ignored){result.putBoolean("diagnosticFilesCleared",false);}
            if(exported!=null&&exported.exists())result.putBoolean("syntheticExportDeleted",exported.delete());
        }
        finish("PASS".equals(result.getString("result"))?Activity.RESULT_OK:Activity.RESULT_CANCELED,result);
    }
}
