package dijkstra

import kotlinx.atomicfu.atomic
import java.util.*
import java.util.concurrent.Phaser
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val q = MultiPriorityQueue(2 * workers, NODE_DISTANCE_COMPARATOR) // TODO replace me with a multi-queue based PQ!
    q.add(start)
    q.update(true)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (true) {
                //if cur == null then maybe need new Random, maybe pool is empty
                val cur: Node = q.remove() ?: if (q.curWorking.compareAndSet(0, 0)) break else continue
                for (e in cur.outgoingEdges) {
                    while (true) {
                        val old = e.to.distance
                        val new = e.weight + cur.distance
                        if (new >= old) {
                            break
                        }
                        if (e.to.casDistance(old, new)) {
                            q.add(e.to)
                            q.update(true)
                            break
                        }
                    }
                }
                q.update(false)
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}

class MultiPriorityQueue(val workers: Int, comp: java.util.Comparator<Node>) {
    val curWorking = atomic(0)
    val pool: MutableList<PriorityQueue<Node>> = Collections.nCopies(workers, PriorityQueue(comp))
    val rand = Random(0)

    fun getRand(): Int {
        return rand.nextInt(workers)
    }

    fun add(now: Node) {
        val ind = getRand()
        synchronized(pool[ind]) {
            pool[ind].add(now)
        }
    }

    fun remove(): Node? {
        val ind1 = getRand()
        var ind2 = getRand()
        while (ind1 == ind2) {
            ind2 = getRand()
        }
        synchronized(pool[ind1]) {
            synchronized(pool[ind2]) {
                if (pool[ind1].isEmpty() && pool[ind2].isEmpty()) {
                    return null
                } else if (!pool[ind1].isEmpty() && pool[ind2].isEmpty()) {
                    return pool[ind1].poll()
                } else if (pool[ind1].isEmpty() && !pool[ind2].isEmpty()) {
                    return pool[ind2].poll()
                } else {
                    val maxi = Math.max(pool[ind1].peek().distance, pool[ind2].peek().distance)
                    if (pool[ind1].peek().distance == maxi) {
                        return pool[ind2].poll()
                    } else {
                        return pool[ind1].poll()
                    }
                }
            }
        }
    }

    fun update(flag: Boolean) {
        if (flag) {
            curWorking.incrementAndGet()
        } else {
            curWorking.decrementAndGet()
        }
    }

}
