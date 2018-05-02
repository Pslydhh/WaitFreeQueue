# WaitFreeQueue

Hello,gues! this queue based on the good jobs of http://ftp.cs.technion.ac.il/he/people/mad/online-publications/ppopp2013-x86queues.pdf

-Xmx1024m -Xmn256m -XX:+UseG1GC -XX:+TieredCompilation

# test_
<pre><code>
package main.java.com.psly.test;

import main.java.com.psly.concurrent.WaitFreeQueueV1;

public class benchmark1 {
	private static int rounds = 1024;
	public static void main(String[] args) throws InterruptedException {
			testWaitFreeQueue();
	}
	
	private static void testWaitFreeQueue() throws InterruptedException {
		final WaitFreeQueueV1<Integer> queues = new WaitFreeQueueV1<Integer>();
		System.out.println("\ntestWaitFreeQueue");
		for(int t = 0; t < rounds; ++t) {
			queues.initRingTail();
			int thread_num = MetaDATAs.thread_num;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\ntestWaitFreeQueue " + t + " " + (MetaDATAs.counts * 2) + " Ops Hello world! " + Thread.currentThread().getName());
			Thread.sleep(2000);
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threads[i] = new Thread() {
					public void run() {
						WaitFreeQueueV1.Handle<Integer> h = queues.register();
						for(int j = 0; j < counts; ++j) {
							queues.enqueue(new Integer(i_ * counts + j), h);
							Integer value;
							if((value = queues.dequeue(h)) == null)
								return ;
							ints[value] = 1;
						}

						queues.unregister(h);
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i)
				threads[i].join();
			queues.count();
			boolean verify = true;
			for(int i = 0; i < MetaDATAs.counts; ++i) {
				if(ints[i] != 1) {
					System.out.println("Error: ints[" + i + "]" + " = " + ints[i]);
					verify = false;
				}
			}
			if(verify)
				System.out.println("ints[0-" + (MetaDATAs.counts-1) + "] has been Verify through");
			
			System.out.println("cost times(seconds): " + (System.currentTimeMillis() - start) / 1000.0);
			Thread.sleep(2000);
		}
	}
}
</pre></code>
# output
testWaitFreeQueue

testWaitFreeQueue 0 20000000 Ops Hello world! main

rings: 4

ints[0-9999999] has been Verify through

cost times(seconds): 0.775
