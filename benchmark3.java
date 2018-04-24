package main.java.com.psly.test;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedTransferQueue;
import main.java.com.psly.concurrent.WaitFreeQueueV1;

public class benchmark3 {
	private static final int rounds = 1024;
	public static void main(String[] args) throws InterruptedException {
		if(args.length == 0 || args[0].trim().equals("WaitFreeQueue"))
			testWaitFreeQueue();
		else if(args[0].trim().equals("ConcurrentLinkedQueue")) {
			testConcurrentLinkedQueue();
		}
		else if(args[0].trim().equals("LinkedTransferQueue")) {
			testLinkedTransferQueue();
		}
	}
	
	private static void testWaitFreeQueue() throws InterruptedException {
		final WaitFreeQueueV1<Integer> queues = new WaitFreeQueueV1<Integer>();
		System.out.println("\ntestWaitFreeQueue");
		for(int t = 0; ; ++t) {
			queues.initRingTail();
			int thread_num = MetaDATAs.thread_num;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\n" + t + " " + (MetaDATAs.counts * 2) + " Ops Hello world! " + Thread.currentThread().getName());
			Thread.sleep(1000);
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threads[i] = new Thread() {
					public void run() {
						WaitFreeQueueV1.Handle<Integer> h = queues.register();
						for(int j = 0; j < counts; ++j) {
							queues.enqueue(i_ * counts + j, h);
						}
						queues.unregister(h);
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i)
				threads[i].join();

			long now = System.currentTimeMillis();
			System.out.println(MetaDATAs.counts + " puts times(seconds): " + (now - start) / 1000.0);
			for(int i = 0; i < thread_num; ++i) {
				(threads[i] = new Thread() {
					public void run() {
						WaitFreeQueueV1.Handle<Integer> h = queues.register();
						for(;;) {
							Integer val = queues.dequeue(h);
							if(val != null)
								ints[val.intValue()] = 1;
							else
								break;
						}
						queues.unregister(h);
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i) 
				threads[i].join();
			
			boolean verify = true;
			for(int i = 0; i < MetaDATAs.counts; ++i) {
				if(ints[i] != 1) {
					System.out.println("Error: ints[" + i + "]");
					verify = false;
				}
			}
			if(verify)
				System.out.println("ints[0-" + (MetaDATAs.counts-1) + "] has been Verify through");
			
			System.out.println(MetaDATAs.counts + " pops times(seconds): " + (System.currentTimeMillis() - now) / 1000.0);
			Thread.sleep(1000);
		}
	}
	
	private static void testConcurrentLinkedQueue() throws InterruptedException {
		final Queue<Integer> queues = new ConcurrentLinkedQueue<Integer>();
		System.out.println("\ntestConcurrentLinkedQueue");
		for(int t = 0; t < rounds; ++t) {
			int thread_num = MetaDATAs.thread_num;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\n" + t + " " + (MetaDATAs.counts * 2) + " Ops Hello world! " + Thread.currentThread().getName());
			Thread.sleep(1000);
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threads[i] = new Thread() {
					public void run() {
						for(int j = 0; j < counts; ++j) {
							queues.add(i_ * counts + j);
						}
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i)
				threads[i].join();
			
			long now = System.currentTimeMillis();
			System.out.println(MetaDATAs.counts + " puts times(seconds): " + (now - start) / 1000.0);
			for(int i = 0; i < thread_num; ++i) {
				(threads[i] = new Thread() {
					public void run() {
						for(;;) {
							Integer val = queues.poll();
							if(val != null)
								ints[val.intValue()] = 1;
							else
								break;
						}
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i) 
				threads[i].join();
			
			boolean verify = true;
			for(int i = 0; i < MetaDATAs.counts; ++i) {
				if(ints[i] != 1) {
					System.out.println("Error: ints[" + i + "]");
					verify = false;
				}
			}
			if(verify)
				System.out.println("ints[0-" + (MetaDATAs.counts-1) + "] has been Verify through");
			
			System.out.println(MetaDATAs.counts + " pops times(seconds): " + (System.currentTimeMillis() - now) / 1000.0);
			Thread.sleep(1000);
		}
	}
	
	private static void testLinkedTransferQueue() throws InterruptedException {
		final Queue<Integer> queues = new LinkedTransferQueue<Integer>();
		System.out.println("\ntestLinkedTransferQueue");
		for(int t = 0; t < rounds; ++t) {
			int thread_num = MetaDATAs.thread_num;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\n" + t + " " + (MetaDATAs.counts * 2) + " Ops Hello world! " + Thread.currentThread().getName());
			Thread.sleep(1000);
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threads[i] = new Thread() {
					public void run() {
						for(int j = 0; j < counts; ++j) {
							queues.add(i_ * counts + j);
						}
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i)
				threads[i].join();
			
			long now = System.currentTimeMillis();
			System.out.println(MetaDATAs.counts + " puts times(seconds): " + (now - start) / 1000.0);
			for(int i = 0; i < thread_num; ++i) {
				(threads[i] = new Thread() {
					public void run() {
						for(;;) {
							Integer val = queues.poll();
							if(val != null)
								ints[val.intValue()] = 1;
							else
								break;
						}
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i) 
				threads[i].join();
			
			boolean verify = true;
			for(int i = 0; i < MetaDATAs.counts; ++i) {
				if(ints[i] != 1) {
					System.out.println("Error: ints[" + i + "]");
					verify = false;
				}
			}
			if(verify)
				System.out.println("ints[0-" + (MetaDATAs.counts-1) + "] has been Verify through");
			
			System.out.println(MetaDATAs.counts + " pops times(seconds): " + (System.currentTimeMillis() - now) / 1000.0);
			Thread.sleep(1000);
		}
	}
}
