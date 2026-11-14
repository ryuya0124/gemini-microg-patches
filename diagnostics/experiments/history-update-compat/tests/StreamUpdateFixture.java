package app.geminipatch.fixture;
import android.content.Context;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import app.geminipatch.compat.HistoryUpdateCompatibility;

/** Explicitly armed synthetic reply warnings on each response. Never distribute. */
public final class StreamUpdateFixture {
 private static byte[] varint(int n){ByteArrayOutputStream b=new ByteArrayOutputStream();while((n&~127)!=0){b.write((n&127)|128);n>>>=7;}b.write(n);return b.toByteArray();}
 private static void text(ByteArrayOutputStream b,int f,String s)throws Exception{byte[] v=s.getBytes(StandardCharsets.UTF_8);b.write(varint((f<<3)|2));b.write(varint(v.length));b.write(v);}
 public static Object receive(Object original){
  if(original==null||!original.getClass().getName().equals("gfeh"))return original;
  int mode=0;Object value=original;
  try{
   ClassLoader l=original.getClass().getClassLoader();Class<?> capture=l.loadClass("app.geminipatch.diagnostics.Capture");Field context=capture.getDeclaredField("context");context.setAccessible(true);
   Context c=(Context)context.get(null);if(c==null)return HistoryUpdateCompatibility.filterStream(original);
   File arm=c.getFileStreamPath("geminipatch-stream-fixture-mode");
   if(arm.isFile()){
    try(InputStream in=new FileInputStream(arm)){mode=in.read();}
    if(mode!=1&&mode!=2)throw new IOException();
    if(original.getClass().getField("p").get(original)!=null)throw new IllegalStateException();
    ByteArrayOutputStream bytes=new ByteArrayOutputStream();text(bytes,1,"Conversation requires an update");text(bytes,2,"This conversation was created with a newer version of Gemini. PRIVATE_CANARY user@example.invalid");bytes.write(32);bytes.write(1);
    Class<?> lite=l.loadClass("gtpr"),builder=l.loadClass("gtpj"),punt=l.loadClass("ghto");
    Object warning=lite.getMethod("parseFrom",lite,byte[].class).invoke(null,punt.getField("a").get(null),bytes.toByteArray());
    Object b=lite.getMethod("toBuilder").invoke(original);builder.getMethod("copyOnWrite").invoke(b);Field instance=builder.getDeclaredField("instance");instance.setAccessible(true);Object mutable=instance.get(b);
    original.getClass().getField("p").set(mutable,warning);original.getClass().getField("b").setInt(mutable,original.getClass().getField("b").getInt(original)|512);
    value=builder.getMethod("build").invoke(b);capture.getMethod("incoming",Object.class).invoke(null,value);
   }
  }catch(Throwable ignored){throw new IllegalStateException("Synthetic stream fixture failed");}
  return mode==1?value:HistoryUpdateCompatibility.filterStream(value);
 }
}
