package app.geminipatch.retentiontest;
public final class BlankActivity extends android.app.Activity {
 @Override public void onCreate(android.os.Bundle b){super.onCreate(b);android.widget.TextView t=new android.widget.TextView(this);t.setText("Retention lifecycle test");setContentView(t);}
}
