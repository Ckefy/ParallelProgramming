import java.util.*
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.atomicArrayOfNulls


class FCPriorityQueue<E : Comparable<E>> {
    private val q = PriorityQueue<E>()

    private val locked = atomic(false)

    private fun tryLock() : Boolean {
        return locked.compareAndSet(false, true)
    }

    private fun unlock() {
        locked.compareAndSet(true, false)
    }

    interface Node<E> {
        val value: E?
    }

    class ResultNode<E>(override val value: E?) : Node<E> {}

    class AddNode<E>(override val value: E?) : Node<E> {}

    class PeekNode<E>(override val value: E?) : Node<E> {}

    class PollNode<E>(override val value: E?) : Node<E> {}


    private val ran = Random()
    private val size = 4 * Runtime.getRuntime().availableProcessors()
    private val lst = atomicArrayOfNulls<Node<E>>(size)

    private fun combine() {
        for (i in 0 until size) {
            val now = lst[i].value ?: continue
            if (now is AddNode<*>) {
                q.add(now.value)
                lst[i].value = ResultNode(null)
            } else if (now is PollNode<*>) {
                val elem = q.poll()
                lst[i].value = ResultNode(elem)
            } else {
                continue
            }
        }
    }

    private fun combPoll(): Pair<E?, Boolean> {
        if (!locked.value) {
            if (tryLock()){
                val elem = q.poll()
                combine()
                unlock()
                return Pair(elem, true)
            }
        }
        return Pair(null, false)
    }
    /**
     * Retrieves the element with the highest priority
     * and returns it as the result of this function;
     * returns `null` if the queue is empty.
     */
    fun poll(): E? {
        var isComb = combPoll()
        if (!isComb.second){
            var ind = ran.nextInt(size)
            var flag = false
            while (true) {
                while (ind < size){
                    if (lst[ind].compareAndSet(null, PollNode(null as E?))) {
                        flag = true
                        break
                    } else {
                        isComb = combPoll()
                        if (isComb.second) return isComb.first
                    }
                    ind++
                }
                if (flag) break
                ind = ran.nextInt(size)
            }
            while (true) {
                if (!locked.value){
                    if (tryLock()){
                        val res = lst[ind].value
                        val curElem: E?
                        if (res!! is ResultNode<*>){
                            curElem = res.value
                        } else {
                            curElem = q.poll()
                        }
                        lst[ind].value = null
                        combine()
                        unlock()
                        return curElem
                    }
                }
                val res = lst[ind].value
                if (res!! is ResultNode<*>) {
                    lst[ind].value = null
                    return res.value
                }
            }
        } else {
            return isComb.first
        }
    }

    private fun combPeek(): Pair<E?, Boolean> {
        if (!locked.value) {
            if (tryLock()){
                val elem = q.peek()
                combine()
                unlock()
                return Pair(elem, true)
            }
        }
        return Pair(null, false)
    }
    /**
     * Returns the element with the highest priority
     * or `null` if the queue is empty.
     */
    fun peek(): E? {
        var isComb = combPeek()
        if (!isComb.second){
            var ind = ran.nextInt(size)
            var flag = false
            while (true) {
                while (ind < size){
                    if (lst[ind].compareAndSet(null, PeekNode(null as E?))) {
                        flag = true
                        break
                    } else {
                        isComb = combPeek()
                        if (isComb.second) return isComb.first
                    }
                    ind++
                }
                if (flag) break
                ind = ran.nextInt(size)
            }
            while (true) {
                if (!locked.value){
                    if (tryLock()){
                        val res = lst[ind].value
                        val curElem: E?
                        if (res!! is ResultNode<*>){
                            curElem = res.value
                        } else {
                            curElem = q.peek()
                        }
                        lst[ind].value = null
                        combine()
                        unlock()
                        return curElem
                    }
                }
                val res = lst[ind].value
                if (res!! is ResultNode<*>) {
                    lst[ind].value = null
                    return res.value
                }
            }
        } else {
            return isComb.first
        }
    }


    private fun combAdd(element: E): Boolean {
        if (!locked.value) {
            if (tryLock()){
                q.add(element)
                combine()
                unlock()
                return true
            }
        }
        return false
    }
    /**
     * Adds the specified element to the queue.
     */
    fun add(element: E) {
        if (!combAdd(element)){
            var ind = ran.nextInt(size)
            var flag = false
            while (true) {
                while (ind < size){
                    if (lst[ind].compareAndSet(null, AddNode(element))) {
                        flag = true
                        break;
                    } else {
                        if (combAdd(element)) return
                    }
                    ind++
                }
                if (flag) break
                ind = ran.nextInt(size)
            }
            while (true) {
                if (lst[ind].value!! is ResultNode<*>) {
                    lst[ind].value = null
                    return
                }
                if (!locked.value){
                    if (tryLock()){
                        if (lst[ind].value!! is AddNode<*>){
                            q.add(element)
                        }
                        lst[ind].value = null
                        combine()
                        unlock()
                        return
                    }
                }
            }
        }
    }
}