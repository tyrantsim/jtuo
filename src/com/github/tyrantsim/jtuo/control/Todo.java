package com.github.tyrantsim.jtuo.control;

public class Todo {
    public Operation operation;
    public int iterations;
    public int iterationsFine;

    public Todo(Operation operation, int iterations, int iterationsFine) {
        this.iterations = iterations;
        this.iterationsFine = iterationsFine;
        this.operation = operation;
    }
}
