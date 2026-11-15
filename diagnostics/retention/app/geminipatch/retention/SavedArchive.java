package app.geminipatch.retention;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.security.MessageDigest;
import java.net.HttpURLConnection;
import java.net.URL;

/** Shared local archive. User text is never written outside the encrypted index. */
final class SavedArchive {
 private static SavedArchive instance;
 static synchronized SavedArchive current(){return instance;}
 static synchronized SavedArchive get(Context context){if(instance==null)instance=new SavedArchive(context.getApplicationContext());return instance;}
 final ReplyStore store=new ReplyStore();
 final EncryptedFiles files;
 private final Context context;
 static final int MAX_MEDIA=8,MAX_MEDIA_BYTES=32*1024*1024,MAX_MEDIA_TOTAL=128*1024*1024;
 static final class Media {
  final String id,digest,source,mime;final int kind,size;final long time;
  Media(String id,String digest,String source,String mime,int kind,int size,long time){this.id=id;this.digest=digest;this.source=source;this.mime=mime;this.kind=kind;this.size=size;this.time=time;}
 }
 private final ArrayList<Media> media=new ArrayList<Media>();
 private final Set<String> pendingMedia=new HashSet<String>();
 private final ExecutorService mediaWorker=Executors.newFixedThreadPool(1,new ThreadFactory(){public Thread newThread(Runnable r){Thread t=new Thread(r,"GeminiPatch-save-media");t.setDaemon(true);return t;}});
 private final SharedPreferences preferences;
 final ScheduledExecutorService worker=Executors.newSingleThreadScheduledExecutor(new ThreadFactory(){public Thread newThread(Runnable r){Thread t=new Thread(r,"GeminiPatch-saved-replies");t.setDaemon(true);return t;}});
 private ScheduledFuture<?> pending;
 volatile boolean ready,enabled=true,videosEnabled,loadFailed;
 volatile String status="loading";
 volatile String mediaStatus="idle";
 volatile long generation;
 SavedArchive(Context context){
  this.context=context;files=new EncryptedFiles(context);preferences=context.getSharedPreferences("geminipatch-retention",Context.MODE_PRIVATE);
  worker.execute(new Runnable(){public void run(){
   enabled=preferences.getBoolean("enabled",true);
   videosEnabled=preferences.getBoolean("videos",false);
   try{if(files.exists("index.enc"))restore(files.read("index.enc",1024*1024));status="saved";}
   catch(Throwable ignored){loadFailed=true;status="load_failed";}
   ready=true;
  }});
 }
 private static void text(DataOutputStream out,String value)throws IOException{byte[] b=value.getBytes(StandardCharsets.UTF_8);out.writeInt(b.length);out.write(b);}
 private static String text(DataInputStream in,int maximum)throws IOException{int n=in.readInt();if(n<0||n>maximum)throw new IOException();byte[] b=new byte[n];in.readFully(b);return new String(b,StandardCharsets.UTF_8);}
 private byte[] encode()throws IOException{
  ByteArrayOutputStream b=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(b);out.writeInt(2);
  List<String> saved=store.snapshot();out.writeInt(saved.size());for(String value:saved)text(out,value);
  List<Media> items=media();out.writeInt(items.size());for(Media item:items){text(out,item.id);text(out,item.digest);text(out,item.source);text(out,item.mime);out.writeInt(item.kind);out.writeInt(item.size);out.writeLong(item.time);}
  out.flush();return b.toByteArray();
 }
 private void restore(byte[] data)throws IOException{
  DataInputStream in=new DataInputStream(new ByteArrayInputStream(data));int version=in.readInt();if(version!=1&&version!=2)throw new IOException();int count=in.readInt();if(count<0||count>ReplyStore.MAX_ENTRIES)throw new IOException();
  ArrayList<String> saved=new ArrayList<String>();int total=0;
  for(int i=0;i<count;i++){String value=text(in,ReplyStore.MAX_TEXT*4);total+=value.length();if(value.length()>ReplyStore.MAX_TEXT||total>ReplyStore.MAX_TOTAL)throw new IOException();saved.add(value);}
  ArrayList<Media> items=new ArrayList<Media>();int mediaTotal=0;
  if(version==2){int n=in.readInt();if(n<0||n>MAX_MEDIA)throw new IOException();for(int i=0;i<n;i++){
   String id=text(in,32),digest=text(in,64),source=text(in,64),mime=text(in,32);int kind=in.readInt(),size=in.readInt();long time=in.readLong();
   if(!id.matches("[a-f0-9]{32}")||!digest.matches("[a-f0-9]{64}")||!source.matches("[a-f0-9]{64}")||kind<1||kind>2||size<1||size>MAX_MEDIA_BYTES||!Arrays.asList("image/png","video/mp4","video/webm").contains(mime))throw new IOException();
   mediaTotal+=size;if(mediaTotal>MAX_MEDIA_TOTAL)throw new IOException();if(files.exists(id+".enc"))items.add(new Media(id,digest,source,mime,kind,size,time));
  }}
  if(in.available()!=0)throw new IOException();store.restore(saved);synchronized(this){media.clear();media.addAll(items);}
 }
 synchronized void changed(){
  if(!ready||loadFailed||!enabled)return;
  if(pending!=null&&!pending.isDone())return;
  final long expected=generation;status="saving";
  pending=worker.schedule(new Runnable(){public void run(){synchronized(SavedArchive.this){pending=null;}save(expected);}},150,TimeUnit.MILLISECONDS);
 }
 private void save(long expected){
  if(!ready||loadFailed||expected!=generation||(!enabled&&store.size()==0&&media().isEmpty()))return;
  try{byte[] bytes=encode();if(expected!=generation)return;files.write("index.enc",bytes);if(expected==generation)status="saved";}
  catch(Throwable ignored){if(expected==generation)status="save_failed";}
 }
 void flush(){final long expected=generation;worker.execute(new Runnable(){public void run(){save(expected);}});}
 void setEnabled(final boolean value){enabled=value;if(!value)MediaCapture.background();worker.execute(new Runnable(){public void run(){if(!preferences.edit().putBoolean("enabled",value).commit())status="save_failed";}});if(!value)flush();}
 void setVideosEnabled(final boolean value){videosEnabled=value;if(!value)MediaCapture.background();worker.execute(new Runnable(){public void run(){if(!preferences.edit().putBoolean("videos",value).commit())status="save_failed";}});}
 void clearAndPause(){
  enabled=false;videosEnabled=false;MediaCapture.background();status="clearing";
  synchronized(this){generation++;store.clear();if(pending!=null)pending.cancel(false);pending=null;media.clear();pendingMedia.clear();}
  worker.execute(new Runnable(){public void run(){try{files.clear();if(!preferences.edit().putBoolean("enabled",false).putBoolean("videos",false).commit())throw new IOException();loadFailed=false;status="saved";mediaStatus="idle";}catch(Throwable ignored){status="save_failed";}}});
 }
 synchronized List<Media> media(){return new ArrayList<Media>(media);}
 synchronized boolean mediaCapacity(){return ready&&enabled&&!loadFailed&&pendingMedia.size()<2;}
 private static String hash(byte[] value)throws Exception{byte[] hash=MessageDigest.getInstance("SHA-256").digest(value);StringBuilder s=new StringBuilder();for(byte b:hash)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
 private synchronized boolean claim(String source){if(!ready||!enabled||loadFailed||pendingMedia.size()>=2||pendingMedia.contains(source))return false;for(Media item:media)if(item.source.equals(source))return false;pendingMedia.add(source);return true;}
 private void commitMedia(final byte[] bytes,final String source,final String mime,final int kind,final long expected){
  worker.execute(new Runnable(){public void run(){try{
   if(expected!=generation||!enabled||(kind==2&&!videosEnabled))return;
   String digest=hash(bytes);synchronized(SavedArchive.this){for(Media item:media)if(item.digest.equals(digest)&&item.kind==kind){mediaStatus="saved";return;}}
   String id=UUID.randomUUID().toString().replace("-","");files.write(id+".enc",bytes);
   if(expected!=generation){files.delete(id+".enc");return;}
   ArrayList<Media> removed=new ArrayList<Media>();
   synchronized(SavedArchive.this){if(expected!=generation){files.delete(id+".enc");return;}media.add(new Media(id,digest,source,mime,kind,bytes.length,System.currentTimeMillis()));int total=0;for(Media m:media)total+=m.size;while(media.size()>MAX_MEDIA||total>MAX_MEDIA_TOTAL){Media old=media.remove(0);total-=old.size;removed.add(old);}}
   save(expected);if(!"saved".equals(status))throw new IOException();for(Media old:removed)files.delete(old.id+".enc");mediaStatus="saved";
  }catch(Throwable ignored){if(expected==generation)mediaStatus="save_failed";}finally{synchronized(SavedArchive.this){if(expected==generation)pendingMedia.remove(source);}Arrays.fill(bytes,(byte)0);}}});
 }
 boolean image(final Bitmap bitmap,final String source){
  if(bitmap==null||!claim(source))return false;final long expected=generation;mediaStatus="saving";
  mediaWorker.execute(new Runnable(){public void run(){try{ByteArrayOutputStream out=new ByteArrayOutputStream();if(!bitmap.compress(Bitmap.CompressFormat.PNG,100,out)||out.size()>8*1024*1024)throw new IOException();commitMedia(out.toByteArray(),source,"image/png",1,expected);}catch(Throwable ignored){if(expected==generation)mediaStatus="save_failed";synchronized(SavedArchive.this){if(expected==generation)pendingMedia.remove(source);}}finally{bitmap.recycle();}}});return true;
 }
 boolean video(final Uri uri,final String bearer){
  if(!videosEnabled||uri==null)return false;
  final String source;try{source=hash(uri.toString().getBytes(StandardCharsets.UTF_8));}catch(Exception ignored){return false;}
  if(!claim(source)){synchronized(this){if(pendingMedia.contains(source))return true;for(Media item:media)if(item.source.equals(source))return true;}return false;}final long expected=generation;mediaStatus="saving";
  mediaWorker.execute(new Runnable(){public void run(){try{
   byte[] bytes=readVideo(uri,bearer,expected);String mime;
   if(bytes.length>=12&&bytes[4]=='f'&&bytes[5]=='t'&&bytes[6]=='y'&&bytes[7]=='p')mime="video/mp4";
   else if(bytes.length>=4&&(bytes[0]&255)==0x1a&&(bytes[1]&255)==0x45&&(bytes[2]&255)==0xdf&&(bytes[3]&255)==0xa3)mime="video/webm";else throw new IOException();
   commitMedia(bytes,source,mime,2,expected);
  }catch(Throwable ignored){if(expected==generation)mediaStatus="unavailable";synchronized(SavedArchive.this){if(expected==generation)pendingMedia.remove(source);}}}});return true;
 }
 private boolean localFile(File file)throws IOException{String path=file.getCanonicalPath();for(File root:new File[]{context.getFilesDir(),context.getCacheDir(),context.getNoBackupFilesDir(),context.getExternalCacheDir(),context.getExternalFilesDir(null)})if(root!=null&&path.startsWith(root.getCanonicalPath()+File.separator))return true;return false;}
 private static boolean google(URL url){String host=url.getHost().toLowerCase(java.util.Locale.ROOT);return url.getProtocol().equals("https")&&(url.getPort()==-1||url.getPort()==443)&&url.getUserInfo()==null&&(host.endsWith(".googleusercontent.com")||host.endsWith(".googleapis.com")||host.endsWith(".googlevideo.com")||host.equals("googleusercontent.com")||host.equals("googlevideo.com"));}
 private byte[] readVideo(Uri uri,String bearer,long expected)throws Exception{
  String scheme=uri.getScheme();InputStream input=null;HttpURLConnection connection=null;long length=-1;
  try{
   if("file".equals(scheme)){File file=new File(uri.getPath());if(!localFile(file))throw new IOException();length=file.length();input=new FileInputStream(file);}
   else if("content".equals(scheme))input=context.getContentResolver().openInputStream(uri);
   else if("https".equals(scheme)){
    URL url=new URL(uri.toString());String origin=url.getHost();
    for(int redirect=0;redirect<=3;redirect++){
     if(!google(url))throw new IOException();connection=(HttpURLConnection)url.openConnection();connection.setInstanceFollowRedirects(false);connection.setConnectTimeout(5000);connection.setReadTimeout(5000);
     if(bearer!=null&&url.getHost().equals(origin))connection.setRequestProperty("Authorization",bearer);
     int status=connection.getResponseCode();if(status==301||status==302||status==303||status==307||status==308){String location=connection.getHeaderField("Location");connection.disconnect();connection=null;if(location==null)throw new IOException();url=new URL(url,location);continue;}
     if(status!=200)throw new IOException();length=connection.getContentLengthLong();input=connection.getInputStream();break;
    }
   }
   if(input==null||length>MAX_MEDIA_BYTES)throw new IOException();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[32768];int n;long until=android.os.SystemClock.elapsedRealtime()+30000;
   while((n=input.read(buffer))!=-1){if(expected!=generation||!enabled||!videosEnabled||android.os.SystemClock.elapsedRealtime()>until||out.size()+n>MAX_MEDIA_BYTES)throw new IOException();out.write(buffer,0,n);}
   if(length>=0&&length!=out.size())throw new IOException();return out.toByteArray();
  }finally{if(input!=null)input.close();if(connection!=null)connection.disconnect();}
 }
 interface MediaResult {void done(byte[] data,String mime);}
 void load(final Media item,final MediaResult callback){worker.execute(new Runnable(){public void run(){try{callback.done(files.read(item.id+".enc",MAX_MEDIA_BYTES),item.mime);}catch(Throwable ignored){callback.done(null,item.mime);}}});}
}
