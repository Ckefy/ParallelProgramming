package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private AtomicRef<Node> head;
    private AtomicRef<Node> tail;

    public MSQueue() {
        Node dummy = new Node(0);
        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
    }

    //push
    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x);
        while (true) {
            Node T = tail.getValue();
            if (T.next.getValue() == null) {
                if (T.next.compareAndSet(null, newTail)) {
                    tail.compareAndSet(T, newTail);
                    break;
                }
            } else {
                tail.compareAndSet(T, T.next.getValue());
            }
        }
    }

    //pop
    @Override
    public int dequeue() {
        while (true){
            Node curHead = head.getValue();
            Node next = curHead.next.getValue();
            if (curHead == head.getValue()){
                if (next == null){
                    return Integer.MIN_VALUE;
                }
                if (curHead == tail.getValue()){
                    tail.setValue(next);
                } else {
                    if (head.compareAndSet(curHead, next)){
                        return next.x; //
                    }
                }
            }
        }
    }

    //back
    @Override
    public int peek() {
        while (true){
            Node curHead = head.getValue();
            Node next = curHead.next.getValue();
            if (curHead == head.getValue()){
                if (next == null){
                    return Integer.MIN_VALUE;
                }
                if (curHead == tail.getValue()){
                    tail.setValue(next);
                } else {
                    if (head.getValue() == curHead){
                        return next.x; //
                    }
                }
            }
        }
    }

    private class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x) {
            this.x = x;
            this.next = new AtomicRef<>(null);
        }
    }
}