package app.geminipatch.fixture;
import android.content.Context;
import android.util.Log;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import app.geminipatch.compat.HistoryUpdateCompatibility;

/** Test-only response fixture; never included in a production patch. */
public final class HistoryUpdateFixture {
 public static final String CONTROL="geminipatch-update-fixture-mode";
 private static byte[] varint(int n){ByteArrayOutputStream out=new ByteArrayOutputStream();while((n&~127)!=0){out.write((n&127)|128);n>>>=7;}out.write(n);return out.toByteArray();}
 private static void bytes(ByteArrayOutputStream out,int field,byte[] value)throws IOException{out.write(varint((field<<3)|2));out.write(varint(value.length));out.write(value);}
 public static Object receive(Object original){
  if(original==null||!original.getClass().getName().equals("gffq"))return original;
  int mode=3;Object value=original;
  try{
   ClassLoader l=original.getClass().getClassLoader();Class<?> capture=l.loadClass("app.geminipatch.diagnostics.Capture");
   Field contextField=capture.getDeclaredField("context");contextField.setAccessible(true);Context context=(Context)contextField.get(null);
   if(context!=null){File file=context.getFileStreamPath(CONTROL);if(file.isFile())try(InputStream in=new FileInputStream(file)){mode=in.read();}}
   if(mode==1||mode==2){
    if(original.getClass().getField("d").get(original)!=null)throw new AssertionError("Existing punt must not be replaced by a fixture");
    ByteArrayOutputStream wire=new ByteArrayOutputStream();
    bytes(wire,1,"Conversation requires an update".getBytes(StandardCharsets.UTF_8));
    bytes(wire,2,"This conversation was created with a newer version of Gemini. PRIVATE_CANARY user@example.invalid https://private.invalid/SECRET_CONVERSATION_ID".getBytes(StandardCharsets.UTF_8));
    wire.write(32);wire.write(1);
    Class<?> lite=l.loadClass("gtpr"),builderType=l.loadClass("gtpj"),punt=l.loadClass("ghto");
    Object p=lite.getMethod("parseFrom",lite,byte[].class).invoke(null,punt.getField("a").get(null),wire.toByteArray());
    Object builder=lite.getMethod("toBuilder").invoke(original);builderType.getMethod("copyOnWrite").invoke(builder);
    Field instance=builderType.getDeclaredField("instance");instance.setAccessible(true);Object mutable=instance.get(builder);
    original.getClass().getField("d").set(mutable,p);original.getClass().getField("b").setInt(mutable,original.getClass().getField("b").getInt(original)|1);
    value=builderType.getMethod("build").invoke(builder);
    Log.i("GP8Update","syntheticHistoryUpdateInjected=true filterEnabled="+(mode==2));
   }
  }catch(Throwable ignored){Log.i("GP8Update","syntheticHistoryFixtureUnavailable=true");return original;}
  return mode==1?value:HistoryUpdateCompatibility.filter(value);
 }
}
