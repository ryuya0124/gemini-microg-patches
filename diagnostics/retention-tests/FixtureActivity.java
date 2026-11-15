package app.geminipatch.retentiontest;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.content.Context;
import android.util.AttributeSet;
import java.lang.reflect.*;
import app.geminipatch.retention.RetainedReplies;
public final class FixtureActivity extends Activity {
 public TextView reply;public LinearLayout column;public View picture,video;
 private int resource(String name){int id=getResources().getIdentifier(name,"id",getPackageName());if(id==0)id=getResources().getIdentifier(name,"id",new StringBuilder("com.google.android.").append("googlequicksearchbox").toString());if(id==0)throw new IllegalStateException();return id;}
 @Override public void onCreate(Bundle b){super.onCreate(b);column=new LinearLayout(this);column.setOrientation(1);column.setPadding(24,200,24,24);
  if(!"setup".equals(getIntent().getStringExtra("phase"))){TextView label=new TextView(this);label.setText("Encrypted archive restart fixture");column.addView(label);setContentView(column);RetainedReplies.attach(this);return;}
  reply=new TextView(this);reply.setTextSize(20);reply.setId(resource("assistant_robin_text"));reply.setText("VISIBLE_REPLY_ALPHA");column.addView(reply);
  TextView user=new TextView(this);user.setId(resource("assistant_robin_user_message_text"));user.setText("USER_INPUT_PRIVATE_CANARY");column.addView(user);
  TextView hidden=new TextView(this);hidden.setId(resource("assistant_robin_text"));hidden.setText("HIDDEN_REPLY_PRIVATE_CANARY");hidden.setVisibility(View.GONE);column.addView(hidden);
  TextView progress=new TextView(this);progress.setText("PROGRESS_PRIVATE_CANARY");column.addView(progress);
  ImageView image=new ImageView(this);image.setId(resource("assistant_robin_image_generation_image_view"));Bitmap red=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);red.eraseColor(Color.RED);image.setImageBitmap(red);column.addView(image,new LinearLayout.LayoutParams(192,192));picture=image;
  ImageView userImage=new ImageView(this);userImage.setId(android.R.id.icon);Bitmap blue=Bitmap.createBitmap(64,64,Bitmap.Config.ARGB_8888);blue.eraseColor(Color.BLUE);userImage.setImageBitmap(blue);column.addView(userImage,new LinearLayout.LayoutParams(96,96));
  ImageView hiddenImage=new ImageView(this);hiddenImage.setId(resource("assistant_robin_image_generation_image_view"));hiddenImage.setImageBitmap(blue);hiddenImage.setVisibility(View.GONE);column.addView(hiddenImage);
  setContentView(column);RetainedReplies.attach(this);
 }
 public void addVideo(String uri)throws Exception{
  ClassLoader loader=getClassLoader();final Class<?> playerType=loader.loadClass("nfm");final Object item=loader.loadClass("neu").getMethod("d",String.class).invoke(null,uri);
  Object player=Proxy.newProxyInstance(loader,new Class<?>[]{playerType},new InvocationHandler(){public Object invoke(Object proxy,Method method,Object[] args){
   if(method.getName().equals("h")&&method.getParameterTypes().length==0)return item;
   Class<?> type=method.getReturnType();if(type==boolean.class)return false;if(type==int.class)return 0;if(type==long.class)return 0L;if(type==float.class)return 0F;if(type==double.class)return 0D;return null;
  }});
  video=(View)loader.loadClass("androidx.media3.ui.PlayerView").getConstructor(Context.class,AttributeSet.class).newInstance(this,null);video.setId(resource("assistant_robin_toucan_native_player_view"));
  column.addView(video,new LinearLayout.LayoutParams(480,270));video.getClass().getField("f").set(video,player);video.invalidate();
 }
}
