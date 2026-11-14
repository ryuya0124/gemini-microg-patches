package app.geminipatch.diagnostics;

import java.util.*;
import java.util.concurrent.*;

/** Exercises real admission under a deliberately paused consumer, not implementation-shaped assertions. */
public final class CaptureInboxTest {
    private static void require(boolean v) { if(!v)throw new AssertionError(); }
    public static void main(String[] args)throws Exception {
        final Queue<Runnable> worker=new ConcurrentLinkedQueue<Runnable>();
        final List<String> observed=Collections.synchronizedList(new ArrayList<String>());
        final CaptureInbox inbox=new CaptureInbox(new CaptureInbox.Dispatch(){public void post(Runnable r){worker.add(r);}},
                new CaptureInbox.Execute(){public void run(long generation,long at,Runnable r){r.run();}});
        inbox.reset(1);
        final int[] latest={-1};
        ExecutorService producers=Executors.newFixedThreadPool(4);
        for(int t=0;t<4;t++) producers.submit(new Runnable(){public void run(){
            for(int i=0;i<5000;i++)inbox.offer(1,i,"metadata",false,true,new Runnable(){public void run(){observed.add("metadata");}});
        }});
        producers.shutdown();require(producers.awaitTermination(10,TimeUnit.SECONDS));
        inbox.offer(1,21000,"metadata",false,true,new Runnable(){public void run(){latest[0]=21000;}});
        for(final String key:new String[]{"turn","stream","capabilities","version","server_status","warning"}) {
            require(inbox.offer(1,21001,key,key.equals("server_status")||key.equals("warning"),false,
                    new Runnable(){public void run(){observed.add(key);}}));
        }
        require(worker.size()<=10);
        Runnable r;while((r=worker.poll())!=null)r.run();
        require(latest[0]==21000 && inbox.pending()==0 && inbox.lost()==0);
        require(observed.containsAll(Arrays.asList("turn","stream","capabilities","version","server_status","warning")));
        require(inbox.counters().get("merged:metadata")==19997L);
        inbox.offer(1,22000,"old",false,false,new Runnable(){public void run(){throw new AssertionError("stale generation executed");}});
        inbox.reset(2);while((r=worker.poll())!=null)r.run();require(inbox.pending()==0);
        // Overflow must stay bounded and remain visible, never masquerade as a lossless capture.
        for(int i=0;i<300;i++)inbox.offer(2,i,"important",true,false,new Runnable(){public void run(){}});
        require(worker.size()==128 && inbox.lost()==172);
        while((r=worker.poll())!=null)r.run();require(inbox.pending()==0);
        System.out.println("PASS: 20000-history burst, concurrent producers, all other event kinds, latest snapshot, generation reset, explicit bounded overflow");
    }
}
