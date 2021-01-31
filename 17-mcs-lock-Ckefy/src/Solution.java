import java.util.concurrent.atomic.*;

public class Solution implements Lock<Solution.Node> {
    private final Environment env;

    // todo: необходимые поля (final, используем AtomicReference)
    private final AtomicReference<Node> last;

    public Solution(Environment env) {
        this.last = new AtomicReference<>();
        this.env = env;
    }

    @Override
    public Node lock() {
        Node my = new Node(); // сделали узел
        // todo: алгоритм
        my.locked.set(true);
        Node cur = last.getAndSet(my);
        if (cur == null) {
            return my; // вернули узел
        }
        cur.next.set(my);
        while (my.locked.get()){
            env.park();
        }
        return my;
    }

    @Override
    public void unlock(Node node) {
        // todo: алгоритм
        if (node.next.get() != null){
            node.next.get().locked.set(false);
            env.unpark(node.next.get().thread);
        } else {
            if (last.compareAndSet(node, null)) return;
            while (node.next.get() == null) {

            }
            node.next.get().locked.set(false);
            env.unpark(node.next.get().thread);
        }
    }

    static class Node {
        final Thread thread = Thread.currentThread(); // запоминаем поток, которые создал узел
        // todo: необходимые поля (final, используем AtomicReference)
        final AtomicReference<Node> next = new AtomicReference<>();
        final AtomicReference<Boolean> locked = new AtomicReference<>(false);
    }
}
