import kotlinx.atomicfu.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference



class FAAQueue<T> {
    private val head: AtomicReference<Segment> // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private val tail: AtomicReference<Segment> // Tail pointer, similarly to the Michael-Scott queue

    init {
        val firstNode = Segment()
        head = AtomicReference<Segment>(firstNode)
        tail = AtomicReference<Segment>(firstNode)
    }

    /**
     * Adds the specified element [x] to the queue.
     */
    fun enqueue(x: T) {
        while (true) {
            val tail = this.tail.get()
            val enqIdx = tail.enqIdx.getAndAdd(1)
            if (enqIdx >= SEGMENT_SIZE) {
                val newTail = Segment(x)
                if (tail.next.compareAndSet(null, newTail)) {
                    this.tail.compareAndSet(tail, newTail)
                    return
                } else {
                    this.tail.compareAndSet(tail, tail.next.get())
                }
            } else {
                if (tail.elements[enqIdx].compareAndSet(null, x)) {
                    return
                }
            }
        }
    }

    /**
     * Retrieves the first element from the queue
     * and returns it; returns `null` if the queue
     * is empty.
     */
    fun dequeue(): T? {
        while (true) {
            val head = head.get()
            if (head.isEmpty) {
                if (head.next.get() != null) {
                    this.head.compareAndSet(head, head.next.get())
                } else {
                    return null
                }
            } else {
                val deqIdx = head.deqIdx.getAndAdd(1)
                if (deqIdx >= SEGMENT_SIZE) {
                    continue
                }
                val res = head.elements[deqIdx].getAndSet(DONE) ?: continue
                return res as T
            }
        }
    }

    /**
     * Returns `true` if this queue is empty;
     * `false` otherwise.
     */
    val isEmpty: Boolean get() {
        while (true) {
            val head = head.get()
            if (head.isEmpty) {
                if (head.next.get() != null) {
                    this.head.compareAndSet(head, head.next.get())
                } else {
                    return true
                }
            } else {
                return false
            }
        }
    }
}


internal class Segment {
    internal val next = AtomicReference<Segment>(null)
    internal val enqIdx = AtomicInteger(0)// index for the next enqueue operation
    internal val deqIdx = AtomicInteger(0) // index for the next dequeue operation
    internal val elements = Array<AtomicReference<Any?>>(SEGMENT_SIZE) { AtomicReference(null) }

    constructor() // for the first segment creation

    constructor(x: Any?) { // each next new segment should be constructed with an element
        enqIdx.set(1)
        elements[0].set(x)
    }

    internal val isEmpty: Boolean get() = deqIdx.get() >= SEGMENT_SIZE
}

private val DONE = Any() // Marker for the "DONE" slot state; to avoid memory leaks
const val SEGMENT_SIZE = 2 // DO NOT CHANGE, IMPORTANT FOR TESTS
