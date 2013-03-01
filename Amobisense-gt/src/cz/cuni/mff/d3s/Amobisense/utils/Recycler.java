package cz.cuni.mff.d3s.Amobisense.utils;

import java.util.Vector;

/* The aim of this class is to reduce the amount of objects that need to be
 * created and destroyed every iteration.  If we can avoid having to allocate
 * objects on the heap we can ease the job of the garbage collector and be
 * more efficient.
 */
public class Recycler<T> {
  private Vector<T> list;
  private int avail;

  public Recycler() {
    list = new Vector<T>();
    avail = 0;
  }

  public synchronized T obtain() {
    if(avail == 0) {
      return null;
    }
    return list.get(--avail);
  }

  public synchronized void recycle(T a) {
    if(avail < list.size()) {
      list.set(avail++, a);
    } else {
      list.add(a);
    }
  }
}
