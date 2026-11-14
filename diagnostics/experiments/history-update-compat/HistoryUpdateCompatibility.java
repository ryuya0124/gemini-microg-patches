package app.geminipatch.compat;
import java.lang.reflect.*;
import java.util.Locale;
import java.util.Objects;
import android.util.Log;

/** Exact Google17.72.17 prototype: remove only the recognized thread-update punt. */
public final class HistoryUpdateCompatibility {
 public static Object filter(Object original){
  if(original==null||!original.getClass().getName().equals("gffq"))return original;
  try{
   Class<?> type=original.getClass();Object punt=type.getField("d").get(original);
   if(punt==null||!punt.getClass().getName().equals("ghto"))return original;
   int mask=type.getField("b").getInt(original);
   if((mask&1)==0||punt.getClass().getField("f").getInt(punt)!=1)return original;
   String title=(String)punt.getClass().getField("b").get(punt),body=(String)punt.getClass().getField("c").get(punt);
   if(title==null||body==null||title.length()>4096||body.length()>4096)return original;
   if(!title.trim().toLowerCase(Locale.ROOT).equals("conversation requires an update")||
      !body.toLowerCase(Locale.ROOT).contains("this conversation was created with a newer version of gemini"))return original;
   // Preserve before/after evidence when this prototype is used with diagnostics3.
   try{Class<?> capture=type.getClassLoader().loadClass("app.geminipatch.diagnostics.Capture");capture.getMethod("incoming",Object.class).invoke(null,original);}catch(Throwable ignored){}
   ClassLoader loader=type.getClassLoader();Class<?> lite=loader.loadClass("gtpr"),builderType=loader.loadClass("gtpj");
   Object builder=lite.getMethod("toBuilder").invoke(original);builderType.getMethod("copyOnWrite").invoke(builder);
   Field instance=builderType.getDeclaredField("instance");instance.setAccessible(true);Object mutable=instance.get(builder);
   type.getField("b").setInt(mutable,mask&~1);type.getField("d").set(mutable,null);
   Object result=builderType.getMethod("build").invoke(builder);
   if(type.getField("b").getInt(original)!=mask||type.getField("d").get(original)!=punt||
      type.getField("b").getInt(result)!=(mask&~1)||type.getField("d").get(result)!=null)throw new AssertionError();
   for(Field f:type.getFields())if(!Modifier.isStatic(f.getModifiers())&&!f.getName().equals("b")&&!f.getName().equals("d"))
    if(!Objects.equals(f.get(original),f.get(result)))throw new AssertionError();
   Log.i("GP8Update","recognizedHistoryUpdatePuntRemoved=true behavior=1");return result;
  }catch(Throwable ignored){Log.i("GP8Update","historyUpdateFilterUnavailable=true");return original;}
 }
}
