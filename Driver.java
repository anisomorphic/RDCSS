// Michael Harris
// COP4520 - pa2
// Driver for AtomicStackRDCSS.java

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

public class Driver<T> {
  public static final int MAX_NUM_NODES = 100000;
  public static final int THREADS = 32;

  public static void main(String[] args) throws Exception {
    Thread threads[] = new Thread[THREADS];
    AtomicStackRDCSS<Integer> AtomicStackRDCSS = new AtomicStackRDCSS<>();

    long begin = System.currentTimeMillis();

    for (int i = 0; i < THREADS; i++) {
      threads[i] = new Thread(new AtomicDriver(AtomicStackRDCSS, i));
      threads[i].start();
    }

    for (int i = 0; i < THREADS; i++)
      threads[i].join();

    long end = System.currentTimeMillis();

    System.out.println("took " + (end - begin) + "ms");
  }
}

// runnable interface
class AtomicDriver implements Runnable {
  private final int tid;

  public AtomicReference<AtomicStackRDCSS<Integer>> stack;

  public AtomicDriver(AtomicStackRDCSS<Integer> stack, int tid) {
    this.stack = new AtomicReference<>(stack);
    this.tid = tid;
  }

  // get a 'random' number and do a corresponding operation
  public void run() {
    int randomOp = (int)(Math.random() * 3);

    if (randomOp == 1) {
      int r = ThreadLocalRandom.current().nextInt(100, 10000);
      boolean push = stack.get().push(r);
    }
    else if (randomOp == 2) {
      Integer pop = stack.get().pop();
    }
    else {
      int size = stack.get().size();
    }
  }
}
