import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.atomic.AtomicReference

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    val RETRY = "RETRY"
    private val dummy = Node()
    private var head: AtomicReference<Node> = AtomicReference(dummy)
    private var tail: AtomicReference<Node> = AtomicReference(dummy)

    open class Node {
        val next = AtomicReference<Node>(null)
    }

    class SendType<E>(
       val cont: Continuation<Unit>,
       val elem: E
    ) : Node()

    class RecType<E>(
        val cont: Continuation<E>
    ) : Node()

    override suspend fun send(element: E) {
        while (true) {
            val tail1 = this.tail.get()
            val head1 = this.head.get()
            if (this.tail.get() == tail1) {
                if (head1 == tail1 || checkSend(tail1)) {
                    val res = suspendCoroutine<Any> sc@{ cont ->
                        val new = SendType(cont, element)
                        if (tail1.next.compareAndSet(null, new)) {
                            this.tail.compareAndSet(tail1, new)
                        } else {
                            this.tail.compareAndSet(tail1, tail1.next.get())
                            cont.resume(RETRY)
                            return@sc
                        }
                    }
                    if (res != RETRY) {
                        return;
                    }
                } else if (this.tail.get() == tail1) {
                    val next1 = head1.next.get()
                    if (checkRec(next1) &&
                        this.head.compareAndSet(head1, next1)){
                        ((next1 as RecType<*>).cont as Continuation<E>).resume(element)
                        return
                    }
                }
            }
        }
    }

    fun checkRec(node: Node) : Boolean{
        return (node is RecType<*>)
    }

    fun checkSend(node: Node) : Boolean{
        return (node is SendType<*>)
    }

    override suspend fun receive(): E {
        while (true) {
            val tail1 = this.tail.get()
            val head1 = this.head.get()
            if (this.tail.get() == tail1){
                if (head1 == tail1 || checkRec(tail1)){
                    val res = suspendCoroutine<E?> sc@ { cont ->
                        val new = RecType(cont)
                        if (tail1.next.compareAndSet(null, new)){
                            this.tail.compareAndSet(tail1, new)
                        } else {
                            this.tail.compareAndSet(tail1, tail1.next.get())
                            cont.resume(null)
                            return@sc
                        }
                    }
                    if (res != null){
                        return res
                    }
                } else if (this.tail.get() == tail1) {
                    val next1 = head1.next.get()
                    if (this.tail.get() != head1 && checkSend(next1) &&
                        this.head.compareAndSet(head1, next1)){
                        (next1 as SendType<*>).cont.resume(Unit)
                        return (next1.elem as E)
                    }
                }
            }
        }
    }
}
