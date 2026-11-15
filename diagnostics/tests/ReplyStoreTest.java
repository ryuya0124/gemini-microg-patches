package app.geminipatch.retention;
import java.util.List;
public final class ReplyStoreTest {
 private static void require(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){
  ReplyStore store=new ReplyStore();Object view=new Object();
  store.observe(view,"A partial");store.observe(view,"A partial answer");require(store.size()==1&&store.snapshot().get(0).equals("A partial answer"));
  store.observe(view,"");store.observe(view,"A");require(store.snapshot().get(0).equals("A partial answer"));
  store.observe(view,"Unable to respond");require(store.size()==2&&store.snapshot().contains("A partial answer"));
  store.observe(new Object(),"A partial answer");require(store.size()==2);
  List<String> snapshot=store.snapshot();store.clear();require(store.size()==0&&store.characters()==0&&snapshot.size()==2);
  for(int i=0;i<40;i++)store.observe(new Object(),"reply "+i);require(store.size()==8&&store.snapshot().get(0).equals("reply 32"));
  store.clear();StringBuilder large=new StringBuilder();for(int i=0;i<40000;i++)large.append('x');
  for(int i=0;i<10;i++)store.observe(new Object(),i+large.toString());require(store.characters()<=ReplyStore.MAX_TOTAL&&store.size()<=ReplyStore.MAX_ENTRIES);
  for(String text:store.snapshot())require(text.length()<=ReplyStore.MAX_TEXT);
  store.clear();store.observe(view,"new session");require(store.size()==1&&!store.snapshot().contains("A partial answer"));
  System.out.println("Reply retention: append/shrink/replacement, bounded memory, dedup and clear passed");
 }
}
