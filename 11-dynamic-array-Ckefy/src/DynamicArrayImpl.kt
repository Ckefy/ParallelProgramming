import kotlinx.atomicfu.*
import java.util.concurrent.atomic.AtomicReference

class DynamicArrayImpl<E> : DynamicArray<E> {
    private val core = atomic(Core<E>(INITIAL_CAPACITY, -1))

    override fun get(index: Int): E {
        if (core.value.pos.value < index) {
            throw IllegalArgumentException()
        } else {
            return core.value.array[index].value!!.elem
        }
    }

    override fun put(index: Int, element: E) {
        while (true) {
            val nowList = core.value
            if (nowList.pos.value < index) {
                throw IllegalArgumentException()
            } else {
                val last = nowList.array[index].value
                if (last!! is Exist<*> &&
                    nowList.array[index].compareAndSet(last, Exist(element))) {
                    return
                }
            }
        }
    }

    override fun pushBack(element: E) {
        while (true) {
            val nowList = core.value
            val nowPos = nowList.pos.value
            if (nowPos + 1 >= nowList.capacity &&
                transfer.compareAndSet(false, true)){
                val newList = Core<E>(nowList.capacity * 2, nowPos)
                var ind = 0
                while (ind < nowPos + 1) {
                    val elem = nowList.array[ind].value
                    if (nowList.array[ind].compareAndSet(elem, Delete(elem!!.elem))) {
                        newList.array[ind].value = elem
                        ind++
                    }
                }
                core.compareAndSet(nowList, newList)
                transfer.compareAndSet(true, false)
            } else if (nowPos + 1 < nowList.capacity &&
                nowList.array[nowPos + 1].compareAndSet(null, Exist(element))) {
                nowList.pos.compareAndSet(nowPos, nowPos + 1)
                break
            }
        }
    }

    override val size: Int get() {
        return (core.value.pos.value + 1)
    }
}

interface Node<E> {
    val elem: E
}

class Exist<E>(override val elem: E) : Node<E> {
  //empty block
}

class Delete<E>(override val elem: E) : Node<E> {
   //empty block
}

class Moved<E>(override val elem: E) : Node<E> {
    //empty block
}

class Pre<E>(override val elem: E) : Node<E> {
    //empty block
}

private class Core<E>(
    capacity: Int,
    pos: Int
) {
    val array = atomicArrayOfNulls<Node<E>>(capacity)
    val pos = atomic(pos)
    val capacity = capacity
}

val transfer = atomic(false)
private const val INITIAL_CAPACITY = 1 // DO NOT CHANGE ME