package graph.tcm;

/**
 * Created by lukas on 16.04.18.
 */
public class HashFunction {

    private int a;
    private int b;
    private int prime;
    private int k;

    public HashFunction(int a, int b, int prime, int k) {
        this.a = a;
        this.b = b;
        this.prime = prime;
        this.k = k;
    }

    public int getHash(int input){
        return ((a * input + b) % prime % k + k) % k;
    }

    public int range(){
        return k;
    }
}
