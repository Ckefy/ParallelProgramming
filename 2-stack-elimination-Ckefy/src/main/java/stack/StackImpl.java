package stack;

import kotlinx.atomicfu.AtomicRef;
import kotlinx.atomicfu.AtomicArray;

public class StackImpl implements Stack {
    private static class Node {
        final AtomicRef<Node> next;
        final int value;

        Node(int value, Node next) {
            this.next = new AtomicRef<>(next);
            this.value = value;
        }
    }

    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);
    private AtomicArray<Integer> elimination = new AtomicArray<>(size);

    static final int size = 30, spin = 5, neighbours = 5;

    private void traditional_push(int x){
        while (true) {
            Node H = head.getValue();
            Node Hnew = new Node(x, H);
            if (head.compareAndSet(H, Hnew)) {
                return;
            }
        }
    }

    private int getIndex(int i, int j){
        return (i + j) % size;
    }

    @Override
    public void push(int x) {
        final int i = (int) Math.random() * (size - 1);
        for (int j = 0; j < neighbours; j++) {
            final int ind = getIndex(i, j);
            final Integer need = x;
            if (elimination.get(ind).compareAndSet(null, need)) {
                for (int z = 0; z < spin; z++) {
                    if (elimination.get(ind).getValue() != null && elimination.get(ind).getValue() == need) {
                        continue;
                    }
                    return;
                }
                if (elimination.get(ind).compareAndSet(need, null)) {
                    break;
                } else {
                    return;
                }
            }
        }
        traditional_push(x);
    }

    private int traditional_pop(){
        while (true) {
            Node curHead = head.getValue();
            if (curHead == null) return Integer.MIN_VALUE;
            if (head.compareAndSet(curHead, curHead.next.getValue())){
                return curHead.value;
            }
        }
    }

    @Override
    public int pop() {
        final int i = (int) Math.random() * (size - 1);
        for (int j = 0; j < neighbours; j++) {
            final int ind = getIndex(i, j);
            final Integer need = elimination.get(ind).getValue();
            if (need == null){
                continue;
            }
            if (elimination.get(ind).compareAndSet(need, null)){
                return need;
            }
        }
        return traditional_pop();
    }
}
