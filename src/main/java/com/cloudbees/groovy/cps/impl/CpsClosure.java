package com.cloudbees.groovy.cps.impl;

import groovy.lang.Closure;

/**
 * {@link Closure} whose code is CPS-transformed.
 *
 * @author Kohsuke Kawaguchi
 */
public class CpsClosure extends Closure {
    private final CpsClosureDef def;

    public CpsClosure(Object owner, Object thisObject, CpsClosureDef def) {
        super(owner, thisObject);
        this.def = def;
        // TODO: parameterTypes and maximumNumberOfParameters
    }

    // returning CpsCallable lets the caller know that it needs to do CPS evaluation of this closure.
    @Override
    public Object call() {
        return def;
    }

    @Override
    public Object call(Object... args) {
        return def;
    }

    @Override
    public Object call(Object arguments) {
        return def;
    }
}