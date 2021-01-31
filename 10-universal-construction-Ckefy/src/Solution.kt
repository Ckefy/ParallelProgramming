/**
 * @author Arseny Lukonin
 */
class Solution : AtomicCounter {
    // объявите здесь нужные вам поля
    private val first: Node = Node(0)
    private val last: ThreadLocal<Node> = ThreadLocal.withInitial { first }

    override fun getAndAdd(x: Int): Int {
        // напишите здесь код
        var flag = false
        var oldValue = last.get().value
        while (!flag) {
            oldValue = last.get().value
            val curNode = Node(oldValue + x)
            last.set(last.get().next.decide(curNode))
            if (last.get() == curNode){
                flag = true
            }
        }
        return oldValue
    }

    // вам наверняка потребуется дополнительный класс
    private class Node(val value: Int, val next: Consensus<Node> = Consensus())
}
