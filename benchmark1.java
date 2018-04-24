package main.java.com.psly.test;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedTransferQueue;

import main.java.com.psly.concurrent.WaitFreeQueueV1;

public class benchmark1 {
	private static int rounds = 1024;
	private static final int kkk = MetaDATAs.counts;
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
		int kkk = MetaDATAs.counts;
		for(int t = 0; t < rounds; ++t) {
			queues.initRingTail();
			int thread_num = MetaDATAs.thread_num;
			if(t % 3 == 0)
				MetaDATAs.counts = kkk;
			else if(t % 3 == 1)
				MetaDATAs.counts = kkk;
			else 
				MetaDATAs.counts = kkk;
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
	
	private static void testConcurrentLinkedQueue() throws InterruptedException {
		final Queue<Integer> queues = new ConcurrentLinkedQueue<Integer>();
		System.out.println("\ntestConcurrentLinkedQueue");
		for(int t = 0; t < rounds; ++t) {
			int thread_num = MetaDATAs.thread_num;
			if(t % 3 == 0)
				MetaDATAs.counts = kkk;
			else if(t % 3 == 1)
				MetaDATAs.counts = kkk;
			else 
				MetaDATAs.counts = kkk;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\n" + t + " " + (MetaDATAs.counts * 2) + " Ops Hello world! " + Thread.currentThread().getName());
			Thread.sleep(2000);
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threads[i] = new Thread() {
					public void run() {
						for(int j = 0; j < counts; ++j) {
							queues.add(new Integer(i_ * counts + j));
							Integer value;
							if((value = queues.poll()) == null)
								return ;
							ints[value] = 1;
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
			
			System.out.println("cost times(seconds): " + (System.currentTimeMillis() - start) / 1000.0);
			Thread.sleep(2000);
		}
	}
	
	private static void testLinkedTransferQueue() throws InterruptedException {
		final Queue<Integer> queues = new LinkedTransferQueue<Integer>();
		System.out.println("\ntestLinkedTransferQueue");
		for(int t = 0; t < rounds; ++t) {
			int thread_num = MetaDATAs.thread_num;
			if(t % 3 == 0)
				MetaDATAs.counts = kkk;
			else if(t % 3 == 1)
				MetaDATAs.counts = kkk;
			else 
				MetaDATAs.counts = kkk;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\n" + t + " " + (MetaDATAs.counts * 2) + " Ops Hello world! " + Thread.currentThread().getName());
			Thread.sleep(2000);
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threads[i] = new Thread() {
					public void run() {
						for(int j = 0; j < counts; ++j) {
							queues.add(new Integer(i_ * counts + j));
							Integer value;
							if((value = queues.poll()) == null)
								return ;
							ints[value] = 1;
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
			
			System.out.println("cost times(seconds): " + (System.currentTimeMillis() - start) / 1000.0);
			Thread.sleep(2000);
		}
	}
}
