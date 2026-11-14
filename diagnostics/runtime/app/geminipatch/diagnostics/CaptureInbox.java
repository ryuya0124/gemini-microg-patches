package app.geminipatch.diagnostics;

import java.util.*;

/** Bounded, fair admission. Repeated snapshots cannot occupy another kind's slots. */
final class CaptureInbox {
    interface Dispatch { void post(Runnable task); }
    interface Execute { void run(long generation, long observedAt, Runnable task); }
    private final Dispatch dispatch;
    private final Execute execute;
    private long generation, revision;
    private int importantPending, ordinaryPending;
    private final Map<String, ArrayList<Entry>> waiting = new HashMap<String, ArrayList<Entry>>();
    private final Map<String, Long> counts = new TreeMap<String, Long>();
    private static final class Entry {
        long generation, time;
        String kind;
        boolean important;
        Runnable task;
    }
    CaptureInbox(Dispatch dispatch, Execute execute) { this.dispatch=dispatch; this.execute=execute; }
    synchronized void reset(long next) {
        generation=next; revision++; waiting.clear(); counts.clear(); importantPending=ordinaryPending=0;
    }
    private void count(String key) { counts.put(key, counts.containsKey(key)?counts.get(key)+1:1L); revision++; }
    synchronized void loss(String reason) { count("lost:"+reason); }
    synchronized void duplicate(String kind) { count("duplicate:"+kind); }
    synchronized boolean offer(long gen, long time, String kind, boolean important, boolean merge, Runnable job) {
        if(gen!=generation) return false;
        count("seen:"+kind);
        ArrayList<Entry> entries=waiting.get(kind);
        if(entries==null) { entries=new ArrayList<Entry>(); waiting.put(kind,entries); }
        // Keep the first three pending snapshots plus the most recent one.
        // Report coalescing explicitly; it is not a lossless protobuf recorder.
        if(merge && entries.size()>=4) {
            Entry latest=entries.get(entries.size()-1); latest.task=job;latest.time=time;
            count("merged:"+kind); return true;
        }
        if((important?importantPending>=128:ordinaryPending>=128) || (!important && entries.size()>=16)) {
            count("lost:queue:"+kind); return false;
        }
        final Entry entry=new Entry();entry.generation=gen;entry.time=time;entry.kind=kind;entry.important=important;entry.task=job;
        entries.add(entry);if(important)importantPending++;else ordinaryPending++;
        count("queued:"+kind);
        dispatch.post(new Runnable() { public void run() {
            Runnable task;long at;
            synchronized(CaptureInbox.this) {
                if(entry.generation!=generation)return;
                ArrayList<Entry> list=waiting.get(entry.kind);if(list!=null)list.remove(entry);
                if(entry.important)importantPending--;else ordinaryPending--;
                task=entry.task;at=entry.time;revision++;
            }
            try { execute.run(entry.generation,at,task); }
            finally { synchronized(CaptureInbox.this) { if(entry.generation==generation)count("processed:"+entry.kind); } }
        }});
        return true;
    }
    synchronized Map<String, Long> counters() { return new TreeMap<String, Long>(counts); }
    synchronized int pending() { return importantPending+ordinaryPending; }
    synchronized long revision() { return revision; }
    synchronized long lost() {
        long value=0;for(Map.Entry<String,Long> e:counts.entrySet())if(e.getKey().startsWith("lost:"))value+=e.getValue();return value;
    }
}
