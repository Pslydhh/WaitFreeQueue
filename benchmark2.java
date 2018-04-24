package main.java.com.psly.test;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedTransferQueue;

import main.java.com.psly.concurrent.WaitFreeQueue;

public class benchmark2 {
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
		final WaitFreeQueue<Integer> queues = new WaitFreeQueue<Integer>();
		System.out.println("\ntestWaitFreeQueue");
		for(int t = 0; t < rounds; ++t) {
			queues.initRingTail();
			int thread_num = MetaDATAs.thread_num;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\n" + t + " " + (MetaDATAs.counts) + " Ops times Hello world! " + Thread.currentThread().getName());
			Thread.sleep(2000);
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts / 2];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threads[i] = new Thread() {
					public void run() {
						WaitFreeQueue.Handle<Integer> h = queues.register();
						for(int j = 0; j < counts; ++j) {
							if( j % 2 == 0)
								queues.enqueue((i_ * counts + j) / 2, h);
							else
								ints[queues.dequeue(h)] = 1;
						}

						queues.unregister(h);
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i) 
				threads[i].join();
			
			boolean verify = true;
			for(int i = 0; i < MetaDATAs.counts / 2; ++i) {
				if(ints[i] != 1) {
					System.out.println("Error: ints[" + i + "]");
					verify = false;
				}
			}
			if(verify)
				System.out.println("ints[0-" + (MetaDATAs.counts / 2-1) + "] has been Verify through");
			
			System.out.println((MetaDATAs.counts / 2) + " put & pop " + "cost times(seconds): " + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
			Thread.sleep(2000);
		}
	}
	
	private static void testConcurrentLinkedQueue() throws InterruptedException {
		final Queue<Integer> queues = new ConcurrentLinkedQueue<Integer>();
		System.out.println("\ntestConcurrentLinkedQueue");
		for(int t = 0; t < rounds; ++t) {
			int thread_num = MetaDATAs.thread_num;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\n" + t + " " + (MetaDATAs.counts) + " Ops times Hello world! " + Thread.currentThread().getName());
			Thread.sleep(2000);
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts / 2];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threads[i] = new Thread() {
					public void run() {
						for(int j = 0; j < counts; ++j) {
							if( j % 2 == 0)
								queues.add((i_ * counts + j) / 2);
							else
								ints[queues.poll()] = 1;
						}
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i) 
				threads[i].join();
			
			boolean verify = true;
			for(int i = 0; i < MetaDATAs.counts / 2; ++i) {
				if(ints[i] != 1) {
					System.out.println("Error: ints[" + i + "]");
					verify = false;
				}
			}
			if(verify)
				System.out.println("ints[0-" + (MetaDATAs.counts / 2-1) + "] has been Verify through");
			
			System.out.println((MetaDATAs.counts / 2) + " put & pop " + "cost times(seconds): " + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
			Thread.sleep(2000);
		}
	}
	
	private static void testLinkedTransferQueue() throws InterruptedException {
		final Queue<Integer> queues = new LinkedTransferQueue<Integer>();
		System.out.println("\ntestLinkedTransferQueue");
		for(int t = 0; t < rounds; ++t) {
			int thread_num = MetaDATAs.thread_num;
			final int counts = MetaDATAs.counts / thread_num;
			System.out.println("\n" + t + " " + (MetaDATAs.counts) + " Ops times Hello world! " + Thread.currentThread().getName());
			Thread.sleep(2000);
			long start = System.currentTimeMillis();
			Thread[] threads = new Thread[thread_num];
			final int[] ints = new int[MetaDATAs.counts / 2];
			for(int i = 0; i < thread_num; ++i) {
				final int i_ = i;
				(threads[i] = new Thread() {
					public void run() {
						for(int j = 0; j < counts; ++j) {
							if( j % 2 == 0)
								queues.add((i_ * counts + j) / 2);
							else
								ints[queues.poll()] = 1;
						}
					}
				}).start();
			}
			
			for(int i = 0; i < thread_num; ++i) 
				threads[i].join();
			
			boolean verify = true;
			for(int i = 0; i < MetaDATAs.counts / 2; ++i) {
				if(ints[i] != 1) {
					System.out.println("Error: ints[" + i + "]");
					verify = false;
				}
			}
			if(verify)
				System.out.println("ints[0-" + (MetaDATAs.counts / 2-1) + "] has been Verify through");
			
			System.out.println((MetaDATAs.counts / 2) + " put & pop " + "cost times(seconds): " + ((System.currentTimeMillis() - start) / 1000.0) + " seconds");
			Thread.sleep(2000);
		}
	}
}
