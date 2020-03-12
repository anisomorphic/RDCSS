// Michael Harris
// COP4520 - pa2
// Modified AtomicStack to include RDCSS

import java.util.*;
import java.util.concurrent.atomic.*;



// node class
class Node<T> {
  public T val;
  public Node<T> next;

  // custom node constructor, sets node value
  public Node(T _val) {
    this.val = _val;
    this.next = null;
  }

  // accessor/setters
  public T getVal() {
      return val;
  }

  public void setVal(T e) {
      val = e;
  }

  public Node<T> getNext() {
      return next;
  }
}

// descriptor for rdcss object:
class RDCSSDescriptor<T> {
  AtomicInteger a1;             //ctrl address
  Integer o1;                   //expected value at a1
  AtomicReference<Node<T>> a2;  //data address
  Node<T> o2;                   //expected value at a2
  Node<T> n2;                   //new value
  boolean pending;              //pending op in this descriptor

  // initialize
  public RDCSSDescriptor(AtomicInteger a1, Integer o1, AtomicReference<Node<T>> a2, Node<T> o2, Node<T> n2) {
    this.a1 = a1; this.o1 = o1; this.a2 = a2; this.o2 = o2; this.n2 = n2; this.pending = true;
  }
}


// RDCSS stack class
public class AtomicStackRDCSS<T> {
  AtomicReference<Node<T>> head;
  AtomicInteger size;
  AtomicInteger numOps;

  // for pre-allocating nodes
  ArrayList<Node<T>> Nodes;
  AtomicInteger node_Idx;


  public AtomicStackRDCSS() {
    head = new AtomicReference<>();
    numOps = new AtomicInteger(0);
    size = new AtomicInteger(0);

    // pre-allocate 100000 nodes and set index to 0
    node_Idx = new AtomicInteger(0);
    Nodes = new ArrayList<>(Driver.MAX_NUM_NODES);

    // do the allocation, constant is in Driver.java
    for (int i = 0; i < Driver.MAX_NUM_NODES; i++)
      Nodes.add(new Node<>(null));
  }


  // restricted form of CAS2 where only data section is subject to update
  public RDCSSDescriptor<T> RDCSS(RDCSSDescriptor<T> d) {
    RDCSSDescriptor<T> r;

    do {
      r = CAS(d, d.a2, d.o2, d.n2); //C1

      // try and finish
      if (!r.pending)
        Complete(r); //H1

    } while(!r.pending); //B1

    if (r.o2 == d.o2)
      Complete(d);

    numOps.getAndIncrement();

    return r;
  }


  // if a2 is an in-progress descriptor, run complete on it, otherwise kick it back
  public RDCSSDescriptor<T> RDCSSRead(RDCSSDescriptor<T> d) {

    do {
      if (!d.pending) //R1
        Complete(d); //H2
    } while(!d.pending); //B2

    return d;
  }


  // finish the operation
  public void Complete(RDCSSDescriptor<T> currDesc) {
    //if the control address holds the expected value,
    if (currDesc.a1.compareAndSet(currDesc.o1, currDesc.a1.get())) { //R2

      //pointer is changed to the new value
      CAS(currDesc, currDesc.a2, currDesc.o2, currDesc.n2); //C2
      currDesc.pending = true;
    }
    //otherwise the old value is re-instated
    else
      CAS(currDesc, currDesc.a2, currDesc.o2, currDesc.o2); //C3
  }


  // update the node and the descriptor if it was successful. return value used in RDCSS(d)
  public RDCSSDescriptor<T> CAS(RDCSSDescriptor<T> desc, AtomicReference<Node<T>> addr, Node<T> oldVal, Node<T> newVal) {
    if (addr.compareAndSet(oldVal, newVal)) {
      desc.pending = false;
    }

    return desc;
  }


  // push function, modified from part 1 to use RDCSS
  public boolean push(T e) {
    if (size.get() >= Driver.MAX_NUM_NODES) {
      System.out.println("\nMaximum nodes inserted, ABA hazard in play. Expand Driver.MAX_NUM_NODES!");
      java.lang.System.exit(0);
    }

    Node<T> currHead;
    Integer currSize;

    RDCSSDescriptor<T> newDesc;

    // grab node from pool and set value
    Node<T> newNode = Nodes.get(node_Idx.getAndIncrement());
    newNode.setVal(e);

    // much like the do loop from part 1..
    do {
      currSize = size.get();

      // link current head in as new head's.next
      currHead = head.get();
      newNode.next = currHead;

      // create new descriptor for RDCSS
      newDesc = new RDCSSDescriptor<T>(size, currSize, this.head, currHead, newNode);

      // reinit and try again until a2 is enqueued
    } while((RDCSS(newDesc)).a2.get() != this.head.get());

    return true;
  }


  // pop function, modified from part 1 to use RDCSS
  public T pop() {
    Node<T> currHead;
    Integer currSize;
    Node<T> newHead;
    RDCSSDescriptor<T> newDesc;

    do {
      currSize = size.get();
      currHead = head.get();

      // can't pop empty stack
      if (currHead == null)
        return null;

      // setup new head after pop
      newHead = currHead.next;

      // setup descriptor to do write op
      newDesc = new RDCSSDescriptor<T>(size, currSize, this.head, currHead, newHead);

      // reinit and try again until head is expected value
    } while((RDCSS(newDesc)).a2.get() != this.head.get());

    // return pop value
    return currHead.getVal();
  }


  // from hw1
  public int getNumOps() {
    return numOps.get();
  }


  // elements in data structure
  public int size() {
    return size.get();
  }
}
