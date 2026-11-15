package app.geminipatch.retention;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.MediaDataSource;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import java.io.IOException;
import java.util.Arrays;

/** Playback decrypts into bounded memory; never creates a plaintext media file. */
final class MediaViewer {
 private final Activity activity;private Dialog dialog;private MediaPlayer player;private MemorySource source;private int generation;
 MediaViewer(Activity activity){this.activity=activity;}
 void open(SavedArchive archive,final SavedArchive.Media item){
  close();final int expected=generation;
  dialog=new AlertDialog.Builder(activity).setMessage(RetainedReplies.label("Loading saved media…","保存データを読み込んでいます…")).setNegativeButton(RetainedReplies.label("Cancel","キャンセル"),new android.content.DialogInterface.OnClickListener(){public void onClick(android.content.DialogInterface d,int w){close();}}).create();
  dialog.setOnCancelListener(new android.content.DialogInterface.OnCancelListener(){public void onCancel(android.content.DialogInterface d){close();}});dialog.show();
  archive.load(item,new SavedArchive.MediaResult(){public void done(final byte[] data,String mime){new Handler(Looper.getMainLooper()).post(new Runnable(){public void run(){
   if(expected!=generation||activity.isFinishing()||activity.isDestroyed()){if(data!=null)Arrays.fill(data,(byte)0);return;}
   if(dialog!=null)dialog.dismiss();
   if(data==null){Toast.makeText(activity,RetainedReplies.label("Saved media unavailable","保存データを読み込めません"),Toast.LENGTH_SHORT).show();return;}
   if(item.kind==1)image(data);else video(data);
  }});}});
 }
 private void image(byte[] data){
  Bitmap image=BitmapFactory.decodeByteArray(data,0,data.length);Arrays.fill(data,(byte)0);if(image==null)return;
  final ImageView view=new ImageView(activity);view.setAdjustViewBounds(true);view.setScaleType(ImageView.ScaleType.FIT_CENTER);view.setImageBitmap(image);
  dialog=new AlertDialog.Builder(activity).setTitle(RetainedReplies.label("Saved image","保存した画像")).setView(view).setPositiveButton(RetainedReplies.label("Close","閉じる"),null).create();
  dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener(){public void onDismiss(android.content.DialogInterface d){view.setImageDrawable(null);}});dialog.show();
 }
 private void video(final byte[] data){
  final int expected=generation;source=new MemorySource(data);final LinearLayout column=new LinearLayout(activity);column.setOrientation(1);
  TextureView texture=new TextureView(activity);column.addView(texture,new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,(int)(240*activity.getResources().getDisplayMetrics().density)));
  final Button control=new Button(activity);control.setText(RetainedReplies.label("Pause / play","一時停止・再生"));column.addView(control);
  control.setOnClickListener(new View.OnClickListener(){public void onClick(View v){try{if(player!=null){if(player.isPlaying())player.pause();else player.start();}}catch(Throwable ignored){}}});
  texture.setSurfaceTextureListener(new TextureView.SurfaceTextureListener(){public void onSurfaceTextureAvailable(SurfaceTexture texture,int width,int height){
   if(expected!=generation)return;
   try{player=new MediaPlayer();player.setDataSource(source);Surface surface=new Surface(texture);player.setSurface(surface);surface.release();
    player.setOnPreparedListener(new MediaPlayer.OnPreparedListener(){public void onPrepared(MediaPlayer p){p.start();}});
    player.setOnErrorListener(new MediaPlayer.OnErrorListener(){public boolean onError(MediaPlayer p,int what,int extra){Toast.makeText(activity,RetainedReplies.label("Playback unavailable","再生できません"),Toast.LENGTH_SHORT).show();return true;}});player.prepareAsync();
   }catch(Throwable ignored){Toast.makeText(activity,RetainedReplies.label("Playback unavailable","再生できません"),Toast.LENGTH_SHORT).show();}
  }public void onSurfaceTextureSizeChanged(SurfaceTexture t,int w,int h){}public boolean onSurfaceTextureDestroyed(SurfaceTexture t){if(expected==generation)release();return true;}public void onSurfaceTextureUpdated(SurfaceTexture t){}});
  dialog=new AlertDialog.Builder(activity).setTitle(RetainedReplies.label("Saved video","保存した動画")).setView(column).setPositiveButton(RetainedReplies.label("Close","閉じる"),null).create();
  dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener(){public void onDismiss(android.content.DialogInterface d){if(expected==generation)release();}});dialog.show();
 }
 private void release(){if(player!=null){player.release();player=null;}if(source!=null){source.close();source=null;}}
 void close(){generation++;if(dialog!=null){dialog.dismiss();dialog=null;}release();}
 private static final class MemorySource extends MediaDataSource {
  private byte[] data;MemorySource(byte[] data){this.data=data;}
  @Override public synchronized int readAt(long position,byte[] buffer,int offset,int size)throws IOException{if(data==null||position>=data.length)return-1;if(position<0)throw new IOException();int n=(int)Math.min(size,data.length-position);System.arraycopy(data,(int)position,buffer,offset,n);return n;}
  @Override public synchronized long getSize(){return data==null?0:data.length;}
  @Override public synchronized void close(){if(data!=null){Arrays.fill(data,(byte)0);data=null;}}
 }
}
