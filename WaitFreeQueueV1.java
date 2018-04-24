package main.java.com.psly.concurrent;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import sun.misc.Contended;
import sun.misc.Unsafe;

/**
 *
 * @since 1.6
 * @author Pslydhh 2018.04.19
 * @param <E> the type of elements held in this collection
 */
public class WaitFreeQueueV1<T> {
	public WaitFreeQueueV1() {
		this.putNode = new Node<T>();
		this.popNode = this.putNode;
		this.putIndex = new AtomicLong();
		this.popIndex = new AtomicLong();
	}
	
	private Node<T> putNode;
	private Node<T> popNode;
	private AtomicLong putIndex;
	private AtomicLong popIndex;
	// ring for handles
	Handle<T> ringTail;
	
	public void initRingTail() {
		this.ringTail = null;
	}
	
	public void count() {
		Handle<T> local = ringTail;
		Handle<T> ele = local;
		int counts = 1;
		while(ele.next != local) {
			++counts;
			ele = ele.next;
		}
		System.out.println("rings: " + counts);
	}
	
	public long getPutIndex() {
		return putIndex.get();
	}
	
	public long getPopIndex() {
		return popIndex.get();
	}
	
	private boolean casPutNode(Node<T> cmp, Node<T> val) {
		return _unsafe.compareAndSwapObject(this, putNode_offset, cmp, val);
	}
	
	private boolean casPopNode(Node<T> cmp, Node<T> val) {
		return _unsafe.compareAndSwapObject(this, popNode_offset, cmp, val);
	}
	
	private boolean casRingTail(Handle<T> cmp, Handle<T> val) {
		return _unsafe.compareAndSwapObject(this, ringTail_offset, cmp, val);
	}
	
	private static final long putNode_offset;
	private static final long popNode_offset;
	private static final long ringTail_offset;
	private static final Unsafe _unsafe = UtilUnsafe.getUnsafe();
	
	private static class UtilUnsafe {
		private UtilUnsafe() {
		}

		public static Unsafe getUnsafe() {
			if (UtilUnsafe.class.getClassLoader() == null)
				return Unsafe.getUnsafe();
			try {
				final Field fld = Unsafe.class.getDeclaredField("theUnsafe");
				fld.setAccessible(true);
				return (Unsafe) fld.get(UtilUnsafe.class);
			} catch (Exception e) {
				throw new RuntimeException("Could not obtain access to sun.misc.Unsafe", e);
			}
		}
	}
	
	static {
		try {
			putNode_offset = _unsafe.objectFieldOffset(WaitFreeQueueV1.class.getDeclaredField("putNode"));
			popNode_offset = _unsafe.objectFieldOffset(WaitFreeQueueV1.class.getDeclaredField("popNode"));
			ringTail_offset = _unsafe.objectFieldOffset(WaitFreeQueueV1.class.getDeclaredField("ringTail"));
		} catch (Exception e) {
			throw new Error(e);
		}
	}
	
  /**
   * Throws NullPointerException if argument is null.
   *
   * @param v the element
   */
  private static void checkNotNull(Object v) {
      if (v == null)
          throw new NullPointerException();
  }

  /**
   * Inserts the specified element to this queue.
   */
	public void enqueue(T item, Handle<T> handle) {
		checkNotNull(item);
		long index = -1, times = Node.PUT_TIMES;
		Node<T> node;
		
		Cell<T> cell = new Cell<T>(item), local;
		for(; times-- > 0;) {
			index = putIndex.getAndIncrement();
			node = findCell(handle.putNode, index, handle);
			if(handle.putNode.id < node.id) {
				handle.putNode = node;
				chasePut(node);
			}
			if((local = node.getCells(index & Node.CELLS_BIT)) == null) {
				if(node.casCells(index & Node.CELLS_BIT, null, cell)) {
					return ;
				}
				local = node.getCells(index & Node.CELLS_BIT);
			}
			if(local.val == null && local.casVal(null, item)) {
				return ;
			}
		}

		long enqId;
		Node<T> reserveNode = handle.putNode;
		Enq<T> enq = handle.er;
		enq.val = item;
		enq.setId(enqId = index);
		
		cell.val = Cell.TOP_VAL;
		cell.setEnq(enq);
		for(; enq.id > 0; ){
			index = putIndex.getAndIncrement();
			node = findCell(handle.putNode, index, handle);
			if(handle.putNode.id < node.id) {
				handle.putNode = node;
				chasePut(node);
			}
			if((local = node.getCells(index & Node.CELLS_BIT)) == null) {
				if(node.casCells(index & Node.CELLS_BIT, null, cell)) {
					enq.casId(enqId, -index);
					break;
				}
				local = node.getCells(index & Node.CELLS_BIT);
			} 
			if((local.enq == null && local.casEnq(null, enq)) || local.enq == enq) {
				enq.casId(enqId, -index);
				break;
			}
		}
		
		enqId = -enq.id;
		node = findCell(reserveNode, enqId, handle);
		
		if(enqId > index) {
			do {
				index = putIndex.get();
			} while(index <= enqId && !putIndex.compareAndSet(index, enqId + 1));
		}
		
		cell = node.getCells(enqId & Node.CELLS_BIT);
		cell.setVal(item);
	}
	
  /**
   * try to chase the newest putNode. As the unused Memory can be reclaimed
   */
	private void chasePut(Node<T> node) {
		Node<T> putNode = this.putNode;
		while(putNode.id < node.id && !this.casPutNode(putNode, node))
			putNode = this.putNode;
	}
	
	private Object help_enq(Handle<T> th, Cell<T> c, long i) {
		// get the cell's val
		Object val = null;
		int times = Node.POP_PROBES;
		for(; times-- > 0; ){
			val = c.val;
			if(val != null)
				break;
		}
		
		if(val != null) {
			if(val != Cell.TOP_VAL)
				return val;
		} else {
			if(!c.casVal(null, Cell.TOP_VAL)){
				if((val = c.val) != Cell.TOP_VAL)
					return val;
			}
		}
		
		// val must be Cell.TOP_VAL
		Enq<T> e = c.enq;
		boolean helpEnqIsOuter = false;
		Enq<T> pe = null;
		long id = 0;
		if(e == null) {
			pe = th.eh.er;
			id = pe.id;
			if(id > 0 && id <= i) {
				if(!c.casEnq(null, pe)) {
					if((e = c.enq) == Cell.TOP_ENQ)
						return this.putIndex.get() <= i ? Cell.BOT_VAL: Cell.TOP_VAL;
				} else {
					e = pe;
				}
			} else {
				helpEnqIsOuter = true;
				if(c.casEnq(null, Cell.TOP_ENQ) || (e = c.enq) == Cell.TOP_ENQ) {
				// transfer the helped enq to the next.
					th.eh = th.eh.next;
					return this.putIndex.get() <= i ? Cell.BOT_VAL: Cell.TOP_VAL;
				}
			}
		} else if(e == Cell.TOP_ENQ) {
				return this.putIndex.get() <= i ? Cell.BOT_VAL: Cell.TOP_VAL;
		}
		
		long ei = e.id;
		T ev = e.val;
		
		if((ei > 0 && e.casId(ei, -i)) || ((ei = e.id) == -i && c.val == Cell.TOP_VAL)){
			long index;
			do {
				index = putIndex.get();
			} while(index <= i && !putIndex.compareAndSet(index, i + 1));
			c.val = ev;
		}
		// transfer the helped enq to the next.
		if(helpEnqIsOuter || (id > 0 && id != pe.id))
			th.eh = th.eh.next;
		
		return c.val;
	}
	
  /**
   * get a element from this queue.
   */
	public T dequeue(Handle<T> handle) {
		long index = 0, times = Node.POP_TIMES;
		Node<T> node;
		Object val = null;
		
		for(; times-- > 0;) {
			index = popIndex.getAndIncrement();
			node = findCell(handle.popNode, index, handle);
			Cell<T> cell = node.getCells(index & Node.CELLS_BIT);

			if(cell == null) { 
				cell = new Cell<T>(null);
				if(!node.casCells(index & Node.CELLS_BIT, null, cell))
					cell = node.getCells(index & Node.CELLS_BIT);
			}
			
			if(handle.popNode.id < node.id) {
				handle.popNode = node;
				chasePop(node);
			}
			
			val = help_enq(handle, cell, index);
			if(val == Cell.BOT_VAL)
				break;
			if(val != Cell.TOP_VAL){
				if(cell.casDeq(null, Cell.TOP_DEQ))
					break;
				val = Cell.TOP_VAL;
			}
		}
		
		if(val == Cell.TOP_VAL)
			val = deq_slow(handle, index);
		
		// has got the elements, val is the val~~
		if(val != Cell.BOT_VAL) {
			help_deq(handle, handle.dh);
			handle.dh = handle.dh.next;
			return (T) val;
		}
		
		// there is no elements in the queues.
		return null;
	}
	
	Object deq_slow(Handle<T> th, long id) {
		Deq<T> deq = th.dr;
		deq.id = id;
		deq.setIdx(id);

		help_deq(th, th);
		long i = -deq.idx;
		Node<T> node = findCell(th.popNode, i, th);
		Cell<T> c = node.getCells(i & Node.CELLS_BIT);
		if(th.popNode.id < node.id) {
			th.popNode = node;
			chasePop(node);
		}
		Object val = c.val;
		return val == Cell.TOP_VAL? Cell.BOT_VAL: val;
	}
	
	void help_deq(Handle<T> th, Handle<T> ph) {
		Deq<T> deq = ph.dr;
		long idx = deq.idx;
		long id = deq.id;
		
		if(idx < id) 
			return ;
		
		Node<T> dp = ph.popNode;
		_unsafe.loadFence();
		idx = deq.idx;
		
		long old = id, i = id + 1, new_ = 0;
		for(;;) {
			for(; idx == old; ++i) {
				Node<T> node = dp = findCell(dp, i, th);
				Cell<T> c = node.getCells(i & Node.CELLS_BIT);
				
				if(c == null) { 
					c = new Cell<T>(null);
					if(!node.casCells(i & Node.CELLS_BIT, null, c))
						c = node.getCells(i & Node.CELLS_BIT);
				}
				
				long di;
				do {
					di = this.getPopIndex();
				} while(di <= i && !this.popIndex.compareAndSet(di, i + 1));
				
				Object v = help_enq(th, c, i);
				if(v == Cell.BOT_VAL) {
					if(deq.casIdx(idx, -i)) {
						return;
					} else {
						idx = deq.idx;
					}
				} else if(v != Cell.TOP_VAL && (c.deq == null || c.deq == deq)) {
					new_ = i;
					break;
				} else {
					idx = deq.idx;
				}
			}
			
			if(new_ != 0) {
				for(;;) {
					long idxLocal = deq.idx;
					if(idxLocal < 0 || deq.id != id)
						return;
					if(idxLocal >= new_) {
						idx = idxLocal;
						break;
					}
					if(deq.casIdx(idxLocal, new_)) {
						idx = new_;
						break;
					}
				}
				new_ = 0;
			} else {
				if(idx < 0 || deq.id != id)
					return;
				if(idx < i) {
					old = idx;
					continue;
				}
			}
			
			Node<T> node = dp = findCell(dp, idx, th);
			Cell<T> c = node.getCells(idx & Node.CELLS_BIT);
			
			if(c.casDeq(null, deq) || c.deq == deq) {
				deq.casIdx(idx, -idx);
				return;
			}
			
			old = idx;
			i = idx + 1;
		}
	}
	
	/*
	 * try to chase the newest popNode. As the unused Memory can be reclaimed
	 */
	private void chasePop(Node<T> node) {
		Node<T> popNode = this.popNode;
		while(popNode.id < node.id && !this.casPopNode(popNode, node))
			popNode = this.popNode;
	}
	
	/*
	 * find the node as index:i, that is the node such that:
	 *       node.id == (i >>>Node.RIght_shift)
	 * return node.      
	 */
	public Node<T> findCell(Node<T> node, long i, Handle<T> handle) {
		Node<T> curr = node;
		long j, index = (i >>> Node.RIght_shift);
		for(j = curr.id; j < index; ++j) {
			Node<T> next = curr.next;
			// next is null
			if(next == null) {
				// construct a new Node
				Node<T> temp = handle.spare;
				if(temp == null) {
					temp = new Node<T>();
					handle.spare = temp;
				}
				temp.id = j + 1;
				// link to the prev's next;
				if( curr.casNext(null, temp)) {
					next = temp;
					handle.spare = null;
				} else {
					next = curr.next;
				}
			}
			curr = next;
		}
		return curr;
	}
	
	/*
	 * Threads must first registered for the queue as get the useful Handle.
	 */
	public Handle<T> register() {
		// get the putNode and popNode
		Handle<T> handle = new Handle<T>(null, null);
		
		// add this handle to the handles_ring
		Handle<T> tail;
		if((tail = this.ringTail) == null){
			handle.next = handle;
			if(this.casRingTail(null, handle)) {
				handle.eh = handle.next;
				handle.dh = handle.next;
				handle.putNode = this.putNode;
				handle.popNode = this.popNode;
				_unsafe.loadFence();
				return handle;
			}
			tail = this.ringTail;
		}
		
		// add this handle to the handles_ring
		for(;;){
			Handle<T> next = tail.next;
			handle.next = next;
			if(tail.casNext(next, handle)) {
				handle.eh = handle.next;
				handle.dh = handle.next;
				handle.putNode = this.putNode;
				handle.popNode = this.popNode;
				_unsafe.loadFence();
				return handle;
			}
		}
	}
	
	/*
	 * Threads never use the queue again.
	 */
	public void unregister(Handle<T> handle) {
		handle.popNode = handle.putNode = handle.spare = null;
	}
	
	public static class Handle<T>{
		
		Node<T> putNode;
		Node<T> popNode;
		Node<T> spare;
		public Handle(Node<T> putNode, Node<T> popNode) {
			super();
			this.putNode = putNode;
			this.popNode = popNode;
			this.spare = null;
			this.er = new Enq<T>(0, null);
			this.dr = new Deq<T>(0, -1);
		}
		
		boolean casNext(Handle<T> cmp, Handle<T> val) {
			return _unsafe.compareAndSwapObject(this, next_offset, cmp, val);
		}
		
		Handle<T> next;
		
		final Enq<T> er;
		final	Deq<T> dr;
		
		@Contended
		Handle<T> eh;
		@Contended
		Handle<T> dh;
		
		private static final long next_offset;
		static {
			try {
				next_offset = _unsafe.objectFieldOffset(Handle.class.getDeclaredField("next"));
			} catch (Exception e) {
				throw new Error(e);
			}
		}
	}

	static class Node<T> {
		private static final int RIght_shift = 10;
		private static final int CELLS_SIZE = 1 << RIght_shift;
		private static final int CELLS_BIT = CELLS_SIZE - 1;
		private static final int PUT_TIMES = 16;
		private static final int POP_TIMES = 16;
		private static final int POP_PROBES = 64;
		
		@Contended
		private Node<T> next;
		@Contended
		private long id;
		@Contended
		private volatile Object[] cells;
		
		public Node() {
			super();
			this.cells = new Object[CELLS_SIZE];
			this.next = null;
		}

		private static long rawIndex(final long idx) {
			return cells_entry_base + idx * cells_entry_scale;
		}
		  
		public boolean casCells(long idx, Object cmp, Object val) {
			return _unsafe.compareAndSwapObject(cells, rawIndex(idx), cmp, val);
		}
		
		public Cell<T> getCells(long idx) {
		//	return (T) cells[(int) idx];
			return (Cell<T>) _unsafe.getObjectVolatile(cells, rawIndex(idx));
		}
		
		public boolean casNext(Node<T> cmp, Node<T> val) {
			return _unsafe.compareAndSwapObject(this, next_offset, cmp, val);
		}
		
		private static final long cells_entry_base;
		private static final long cells_entry_scale;
		private static final long next_offset;
		static {
			try {
				cells_entry_base = _unsafe.arrayBaseOffset(Object[].class);
				cells_entry_scale = _unsafe.arrayIndexScale(Object[].class);
				next_offset = _unsafe.objectFieldOffset(Node.class.getDeclaredField("next"));
			} catch (Exception e) {
				throw new Error(e);
			}
		}
	}
	
	@Contended
	static class Cell<T> {
		static final Enq<?> TOP_ENQ = new Enq<>(0, null);
		static final Deq<?> TOP_DEQ = new Deq<>(0, 0);
		static final Object TOP_VAL = new Object();
		static final Object BOT_VAL = new Object();
		
		Object val;
		Enq<T> enq;
		Deq<T> deq;	
		
		public Cell(T val) {
			super();
			this.val = val;
		}
		
		public void setVal(Object val) {
			_unsafe.putObjectVolatile(this, val_offset, val);
		}
		  
		public boolean casVal(Object cmp, Object val) {
			return _unsafe.compareAndSwapObject(this, val_offset, cmp, val);
		}
		
		public void setEnq(Enq<T> val) {
			_unsafe.putObjectVolatile(this, enq_offset, val);
		}
		
		public boolean casEnq(Object cmp, Object val) {
			return _unsafe.compareAndSwapObject(this, enq_offset, cmp, val);
		}
		
		public boolean casDeq(Object cmp, Object val) {
			return _unsafe.compareAndSwapObject(this, deq_offset, cmp, val);
		}
		
		private static final long val_offset;
		private static final long enq_offset;
		private static final long deq_offset;
		static {
			try {
				val_offset = _unsafe.objectFieldOffset(Cell.class.getDeclaredField("val"));
				enq_offset = _unsafe.objectFieldOffset(Cell.class.getDeclaredField("enq"));
				deq_offset = _unsafe.objectFieldOffset(Cell.class.getDeclaredField("deq"));
			} catch (Exception e) {
				throw new Error(e);
			}
		}
	}
	
	static class Enq<T> {
		
		@Contended
		long id;
		@Contended
		T val;
		
		public Enq(long id, T val) {
			super();
			this.id = id;
			this.val = val;
		}

		public void setId(long val) {
			_unsafe.putLongVolatile(this, id_offset, val);
		}
		
		public boolean casId(long cmp, long val) {
			return _unsafe.compareAndSwapLong(this, id_offset, cmp, val);
		}
		
		private static final long id_offset;
		static {
			try {
				id_offset = _unsafe.objectFieldOffset(Enq.class.getDeclaredField("id"));
			} catch (Exception e) {
				throw new Error(e);
			}
		}
	}
	
	static class Deq<T> {
		@Contended
		long id;
		@Contended
		long idx;
		
		public Deq(long id, long idx) {
			super();
			this.id = id;
			this.idx = idx;
		}
		
		void setIdx(long val) {
			_unsafe.putLongVolatile(this, idx_offset, val);
		}
		
		boolean casIdx(long cmp, long val) {
			return _unsafe.compareAndSwapLong(this, idx_offset, cmp, val);
		}
		
		private static final long idx_offset;
		static {
			try {
				idx_offset = _unsafe.objectFieldOffset(Deq.class.getDeclaredField("idx"));
			} catch (Exception e) {
				throw new Error(e);
			}
		}
	}
}
	
