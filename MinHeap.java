import java.util.ArrayList;
// !DO NOT CHANGE THIS FILE USED to sort edges by weight for Kruskal's algorithm
public class MinHeap {

    public ArrayList<Edge> data; // Stores the edges in the min heap
    public int size; // Used to keep track of the number of edges in the min heap

    public MinHeap(Edge[] array) {
        // Initializes the min heap with the given array of edges
        data = new ArrayList<>();
        for (Edge item : array) {
            data.add(item);
        }
        size = data.size();

        for (int i = (size - 2) / 2; i >= 0; i--) {
            heapifyDown(i);
        }
    }


    private void swap(int i, int j) {
        // Helper method to swap two edges in the data list
        Edge temp = data.get(i);
        data.set(i, data.get(j));
        data.set(j, temp);
    }

    protected boolean compare(Edge child, Edge parent){
        // Compares two edges based on their weight for the min heap property
        return child.weight < parent.weight;
    }

    public Edge pop() {
        // Removes and returns the edge with the smallest weight from the min heap
        if (size == 0) {
            return null;
        }

        Edge root = data.get(0);
        Edge last = data.remove(size - 1);
        size--;

        if (size > 0) {
            data.set(0, last);
            heapifyDown(0);
        }

        return root;
    }

    private void heapifyDown(int index) {
        // Helper method used in constructor
        int pi = index;

        while (true) {
            int left = getLeftChildIndex(pi);
            int right = getRightChildIndex(pi);
            int best = pi;

            if (left < size && compare(data.get(left), data.get(best))) {
                best = left;
            }
            if (right < size && compare(data.get(right), data.get(best))) {
                best = right;
            }

            if (best == pi) {
                break;
            }

            swap(pi, best);
            pi = best;
        }
    }


    private int getLeftChildIndex(int parentIndex) {
        // Helper method to calculate the index of the left child of a given parent index
        return 2 * parentIndex + 1;
    }

    private int getRightChildIndex(int parentIndex) {
        // Helper method to calculate the index of the right child of a given parent index
        return 2 * parentIndex + 2;
    }

}
