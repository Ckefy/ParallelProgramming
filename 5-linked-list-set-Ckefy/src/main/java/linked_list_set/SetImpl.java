package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private class Node {
        AtomicRef<Node> next;
        int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }

        private Node() { //super for Removed constructor
        }

        Node nextWithRemoves() {
            Node next1 = next.getValue();
            if (next1 instanceof Removed){
                return ((Removed) next1).next;
            }
            return next1;
        }
    }

    private class Removed extends Node {
        Node next;

        Removed(Node next) {
            this.next = next;
        }
    }

    private class Window {
        Node cur, next;
    }

    private final Node head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        hategoto:
        while (true) {
            Window w = new Window();
            w.cur = head;
            w.next = w.cur.nextWithRemoves();
            Node node;
            while (w.next.x < x) {
                node = w.next.next.getValue();
                if (node instanceof Removed) {
                    if (!w.cur.next.compareAndSet(w.next, ((Removed) node).next)) {
                        continue hategoto;
                    } else {
                        w.next = ((Removed) node).next;
                    }
                } else {
                    w.cur = w.next;
                    w.next = w.cur.nextWithRemoves();
                }
            }
            node = w.next.next.getValue();
            if (node instanceof Removed) {
                w.cur.next.compareAndSet(w.next, ((Removed) node).next);
            } else {
                return w;
            }
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            boolean res;
            if (w.next.x == x) {
                res = false;
            } else {
                Node newNode = new Node(x, w.next);
                if (w.cur.next.compareAndSet(w.next, newNode)) {
                    res = true;
                } else {
                    continue;
                }
            }
            return res;
        }
    }

    @Override
    public boolean remove(int x) {
        boolean res;
        while (true) {
            Window w = findWindow(x);
            if (w.next.x != x) {
                res = false;
                break;
            } else {
                Node node = w.next.nextWithRemoves();
                Node removeThis = new Removed(node);
                if (w.next.next.compareAndSet(node, removeThis)){
                    w.cur.next.compareAndSet(w.next, node);
                    res = true;
                    break;
                }
            }
        }
        return res;
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        boolean res = w.next.x == x;
        return res;
    }
}