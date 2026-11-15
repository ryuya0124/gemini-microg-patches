package app.geminipatch.retention;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.SystemClock;
import android.view.View;
import android.widget.ImageView;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.WeakHashMap;

/** Exact-version generated-media views only. Never logs or persists source URLs/auth headers. */
final class MediaCapture {
 private static volatile String bearer;
 private static volatile long expires;
 private final WeakHashMap<View,Object> seen=new WeakHashMap<View,Object>();
 private final String salt=java.util.UUID.randomUUID().toString();
 private long generation=-1;
 static void headers(Object raw){
  SavedArchive archive=SavedArchive.current();if(archive==null||!archive.enabled||!archive.videosEnabled)return;
  if(!(raw instanceof Map))return;Object value=((Map<?,?>)raw).get("Authorization");
  if(value instanceof String){String s=(String)value;if(s.startsWith("Bearer ")&&s.length()<=4096&&!s.contains("\n")&&!s.contains("\r")){bearer=s;expires=SystemClock.elapsedRealtime()+5*60*1000;}}
 }
 static void background(){bearer=null;expires=0;}
 private static String hash(String value)throws Exception{byte[] b=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(java.util.Locale.ROOT,"%02x",x&255));return s.toString();}
 void observe(View view,String name,SavedArchive archive){
  try{
   if(generation!=archive.generation){seen.clear();generation=archive.generation;}
   if(seen.size()>32)seen.clear();
   if(view instanceof ImageView&&(name.equals("assistant_robin_image_generation_image_view")||name.equals("assistant_robin_generated_image_lightbox_image"))){
    Drawable drawable=((ImageView)view).getDrawable();if(drawable==null||drawable==seen.get(view)||!archive.mediaCapacity()||drawable.getIntrinsicWidth()<32||drawable.getIntrinsicHeight()<32)return;
    int width=drawable.getIntrinsicWidth(),height=drawable.getIntrinsicHeight();double scale=Math.min(1.0,2048.0/Math.max(width,height));width=Math.max(1,(int)(width*scale));height=Math.max(1,(int)(height*scale));
    Bitmap copy;
    if(drawable instanceof BitmapDrawable){Bitmap source=((BitmapDrawable)drawable).getBitmap();if(source==null||source.isRecycled())return;
     Bitmap readable=source.getConfig()==Bitmap.Config.HARDWARE?source.copy(Bitmap.Config.ARGB_8888,false):source;
     if(readable==null)return;
     Bitmap scaled=Bitmap.createScaledBitmap(readable,width,height,true);copy=scaled==source||scaled==readable?scaled.copy(Bitmap.Config.ARGB_8888,false):scaled;
     if(readable!=source&&readable!=copy)readable.recycle();
    }else{copy=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);Rect before=new Rect(drawable.getBounds());try{drawable.setBounds(0,0,width,height);drawable.draw(new Canvas(copy));}finally{drawable.setBounds(before);}}
    if(copy==null)return;
    String source=hash("image:"+salt+":"+System.identityHashCode(drawable)+":"+width+":"+height);
    if(archive.image(copy,source))seen.put(view,drawable);else copy.recycle();
   }else if(name.equals("assistant_robin_toucan_native_player_view")&&view.getClass().getName().equals("androidx.media3.ui.PlayerView")&&archive.videosEnabled){
    Object player=view.getClass().getField("f").get(view);if(player==null)return;
    ClassLoader loader=view.getClass().getClassLoader();Object item=loader.loadClass("nfm").getMethod("h").invoke(player);if(item==null)return;
    Object configuration=item.getClass().getField("c").get(item);if(configuration==null)return;
    Object raw=configuration.getClass().getField("i").get(configuration);if(!(raw instanceof Uri))return;
    Uri uri=(Uri)raw;if(uri.equals(seen.get(view)))return;
    if(archive.video(uri,SystemClock.elapsedRealtime()<expires?bearer:null))seen.put(view,uri);
   }
  }catch(Throwable ignored){archive.mediaStatus="unavailable";if(view instanceof ImageView)seen.put(view,((ImageView)view).getDrawable());}
 }
}
