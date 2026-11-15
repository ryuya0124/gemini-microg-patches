package app.geminipatch.retention;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;

/** Bounded in-memory model; persistence is handled separately. No conversation identifiers. */
public final class ReplyStore {
    public static final int MAX_ENTRIES = 8, MAX_TEXT = 32768, MAX_TOTAL = 131072;
    private final WeakHashMap<Object, State> owners = new WeakHashMap<Object, State>();
    private final ArrayList<Entry> entries = new ArrayList<Entry>();
    private int total;
    private long next;
    private static final class Entry { long id; String text; Entry(long id, String text) { this.id=id; this.text=text; } }
    private static final class State { String last; Entry entry; }

    public synchronized boolean observe(Object owner, String text) {
        if (owner == null || text == null || text.trim().isEmpty()) return false;
        if (text.length() > MAX_TEXT) text = text.substring(0, MAX_TEXT);
        State state=owners.get(owner);
        if(state==null){if(owners.size()>=32)owners.remove(owners.keySet().iterator().next());state=new State();owners.put(owner,state);}
        if(text.equals(state.last))return false;
        state.last=text;
        Entry old=state.entry;
        if(old!=null && entries.contains(old)){
            if(old.text.startsWith(text))return false; // A shorter rewrite must not discard an already seen prefix.
            if(text.startsWith(old.text)){
                total+=text.length()-old.text.length();old.text=text;trim();return true;
            }
        }
        // Rebinding an existing answer must not continually duplicate it in the bounded viewer.
        for(Entry entry:entries)if(entry.text.startsWith(text)){state.entry=entry;return false;}
        Entry entry=new Entry(++next,text);state.entry=entry;entries.add(entry);total+=text.length();trim();return true;
    }
    private void trim(){while(entries.size()>MAX_ENTRIES||total>MAX_TOTAL){Entry old=entries.remove(0);total-=old.text.length();}}
    public synchronized List<String> snapshot(){
        ArrayList<String> result=new ArrayList<String>();for(Entry entry:entries)result.add(entry.text);
        return Collections.unmodifiableList(result);
    }
    public synchronized int size(){return entries.size();}
    public synchronized int characters(){return total;}
    public synchronized void clear(){entries.clear();owners.clear();total=0;}
    public synchronized void detachViews(){owners.clear();}
    public synchronized void restore(List<String> saved){clear();for(String text:saved)observe(new Object(),text);detachViews();}
}
