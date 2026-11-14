package app.geminipatch.compat;
import java.lang.reflect.*;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import android.util.Log;

/** Exact Google17.72.17 prototype: remove only the recognized thread-update punt. */
public final class HistoryUpdateCompatibility {
 private static final AtomicInteger CALLS=new AtomicInteger();
 private static void audit(ClassLoader loader,int source,int call,int result,int stage,int behavior,boolean present,boolean compared,boolean title,boolean body){
  try{loader.loadClass("app.geminipatch.diagnostics.Capture").getMethod("updateCompatibility",int.class,int.class,int.class,int.class,int.class,boolean.class,boolean.class,boolean.class,boolean.class)
   .invoke(null,source,call,result,stage,behavior,present,compared,title,body);}catch(Throwable ignored){}
 }
 public static Object filter(Object original){
  if(original==null||!original.getClass().getName().equals("gffq"))return original;
  return filterPunt(original,"d",1,0);
 }
 public static Object filterStream(Object original){
  if(original==null||!original.getClass().getName().equals("gfeh"))return original;
  try{
   Object value=original;boolean present=false;Class<?> type=original.getClass();
   for(int source=1;source<=2;source++){
    String field=source==1?"p":"q";int bit=source==1?512:1024;
    if((type.getField("b").getInt(value)&bit)!=0&&type.getField(field).get(value)!=null){present=true;value=filterPunt(value,field,bit,source);}
   }
   if(!present)audit(type.getClassLoader(),3,CALLS.incrementAndGet(),0,1,-1,false,false,false,false);
   return value;
  }catch(Throwable ignored){audit(original.getClass().getClassLoader(),3,CALLS.incrementAndGet(),4,1,-1,false,false,false,false);return original;}
 }
 private static Object filterPunt(Object original,String field,int bit,int source){
  ClassLoader loader=original.getClass().getClassLoader();int call=CALLS.incrementAndGet(),stage=1,behavior=-1;
  boolean present=false,compared=false,titleHit=false,bodyHit=false;
  try{
   Class<?> type=original.getClass();Object punt=type.getField(field).get(original);
   int mask=type.getField("b").getInt(original);
   present=(mask&bit)!=0&&punt!=null;
   if(!present){audit(loader,source,call,0,stage,behavior,false,false,false,false);return original;}
   if(!punt.getClass().getName().equals("ghto"))throw new IllegalStateException();
   behavior=punt.getClass().getField("f").getInt(punt);
   if(behavior!=1){audit(loader,source,call,1,stage,behavior,true,false,false,false);return original;}
   stage=2;
   String title=(String)punt.getClass().getField("b").get(punt),body=(String)punt.getClass().getField("c").get(punt);
   if(title==null||body==null||title.length()>4096||body.length()>4096){audit(loader,source,call,1,stage,behavior,true,false,false,false);return original;}
   compared=true;titleHit=title.trim().toLowerCase(Locale.ROOT).equals("conversation requires an update");
   bodyHit=body.toLowerCase(Locale.ROOT).contains("this conversation was created with a newer version of gemini");
   if(!titleHit||!bodyHit){audit(loader,source,call,1,stage,behavior,true,true,titleHit,bodyHit);return original;}
   audit(loader,source,call,2,stage,behavior,true,true,true,true);
   // Preserve before/after evidence when this prototype is used with diagnostics3.
   try{Class<?> capture=type.getClassLoader().loadClass("app.geminipatch.diagnostics.Capture");capture.getMethod("incoming",Object.class).invoke(null,original);}catch(Throwable ignored){}
   stage=3;Class<?> lite=loader.loadClass("gtpr"),builderType=loader.loadClass("gtpj");
   Object builder=lite.getMethod("toBuilder").invoke(original);builderType.getMethod("copyOnWrite").invoke(builder);
   Field instance=builderType.getDeclaredField("instance");instance.setAccessible(true);Object mutable=instance.get(builder);
   stage=4;type.getField("b").setInt(mutable,mask&~bit);type.getField(field).set(mutable,null);
   Object result=builderType.getMethod("build").invoke(builder);
   stage=5;
   if(type.getField("b").getInt(original)!=mask||type.getField(field).get(original)!=punt||
      type.getField("b").getInt(result)!=(mask&~bit)||type.getField(field).get(result)!=null)throw new AssertionError();
   for(Field f:type.getFields())if(!Modifier.isStatic(f.getModifiers())&&!f.getName().equals("b")&&!f.getName().equals(field))
    if(!Objects.equals(f.get(original),f.get(result)))throw new AssertionError();
   audit(loader,source,call,3,6,behavior,true,true,true,true);
   Log.i("GP8Update","recognizedHistoryUpdatePuntRemoved=true behavior=1");return result;
  }catch(Throwable ignored){audit(loader,source,call,4,stage,behavior,present,compared,titleHit,bodyHit);Log.i("GP8Update","historyUpdateFilterUnavailable=true");return original;}
 }
}
