package com.cloudbees.groovy.cps

import com.cloudbees.groovy.cps.impl.CpsFunction
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.customizers.ImportCustomizer
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 *
 *
 * @author Kohsuke Kawaguchi
 */
class CpsTransformerTest {
    /**
     * CPS-transforming shelll
     */
    GroovyShell csh;

    /**
     * Default groovy shell
     */
    GroovyShell sh;

    def binding = new Binding()

    @Before
    void setUp() {
        def imports = new ImportCustomizer()
                .addImports(CpsTransformerTest.class.name)
                .addImports(WorkflowMethod.class.name)

        def cc = new CompilerConfiguration()
        cc.addCompilationCustomizers(imports)
        cc.addCompilationCustomizers(new CpsTransformer())
        csh = new GroovyShell(binding,cc);

        cc = new CompilerConfiguration()
        cc.addCompilationCustomizers(imports)
        sh = new GroovyShell(binding,cc);
    }

    Object evalCPS(String script) {
        Object v = evalCPSonly(script)
        assert v==sh.evaluate(script); // make sure that regular non-CPS execution reports the same result
        return v;
    }

    Object evalCPSonly(String script) {
        Script s = csh.parse(script)
        CpsFunction f = s.run();
        def p = f.invoke(null, s, [], Continuation.HALT)

        def v = p.resume().yieldedValue()
        v
    }

    @Test
    void helloWorld() {
        assert evalCPS("'hello world'.length()")==11
    }

    @Test
    void comparison() {
        for(int i in [1,2,3]) {
            for (int j in [1,2,3]) {
                assert evalCPS("${i} < ${j}") == (i<j);
                assert evalCPS("${i} <= ${j}")== (i<=j);
                assert evalCPS("${i} > ${j}") == (i>j);
                assert evalCPS("${i} >= ${j}")== (i>=j);
            }
        }
    }

    @Test
    void forInLoop() {
        assert evalCPS("x=0; for (i in [1,2,3,4,5]) x+=i; return x;")==15;
    }

    @Test
    void variableAssignment() {
        assert evalCPS("x=3; x+=2; return x;")==5;
    }

    @Test
    void localVariable() {
        assert evalCPS("int x=3; x+=2; return x;")==5;
    }

    @Test
    void increment() {
        assert evalCPS("""
            x=0;
            y = x++;
            z = ++x;
            return x+"."+y+"."+z;
        """)=="2.0.2";
    }

    @Test
    void decrement() {
        assert evalCPS("""
            x=5;
            y = x--;
            z = --x;
            return x+"."+y+"."+z;
        """)=="3.5.3";
    }

    @Test
    void break_() {
        assert evalCPS("""
            x=0;
            int i=0;
            for (i=0; i<5; i+=1) {
                break;
                x+=1;
            }
            return i+x;
        """)==0;
    }

    @Test
    void globalBreak_() {
        assert evalCPS("""
            x=0;
            int i=0;
            int j=0;

            I:
            for (i=0; i<5; i+=1) {
                J:
                for (j=0; j<5; j+=1) {
                  break I;
                  x+=1;
                }
                x+=1;
            }
            return i+"."+j+"."+x;
        """)=="0.0.0";
    }

    @Test
    void functionCall() {
        assert evalCPS("""
            int i=1;
            i.plus(2)
        """)==3;
    }

    @Test
    void functionCall0arg() {
        assert evalCPS("""
            123.toString()
        """)=="123";
    }

    @Test
    void constructorCall() {
        assert evalCPS("""
            new String("abc"+"def")
        """)=="abcdef";
    }

    @Test
    void constructorCall0arg() {
        assert evalCPS("""
            new String()
        """)=="";
    }

    @Test
    void workflowCallingWorkflow() {
        assert evalCPS("""
            @WorkflowMethod
            def fib(int x) {
              if (x==0)     return 0;
              if (x==1)     return 1;
              x = fib(x-1)+fib(x-2);    // assignment to make sure x is treated as local variable
              return x;
            }
            fib(10);
        """)==55
    }

    /**
     *
     */
    @Test
    void exceptionFromNonCpsCodeShouldBeCaughtByCatchBlockInCpsCode() {
        assert evalCPS("""
            @WorkflowMethod
            def foo() {
              "abc".substring(5); // will caught exception
              return "fail";
            }

            try {
              return foo();
            } catch(StringIndexOutOfBoundsException e) {
              return e.message;
            }
        """)=="String index out of range: -2"
    }

    /**
     * while loop that evaluates to false and doesn't go through the body
     */
    @Test
    void whileLoop() {
        assert evalCPS("""
            int x=1;
            while (false) {
                x++;
            }
            return x;
        """)==1
    }

    /**
     * while loop that goes through several iterations.
     */
    @Test
    void whileLoop5() {
        assert evalCPS("""
            int x=1;
            while (x<5) {
                x++;
            }
            return x;
        """)==5
    }

    /**
     * do-while loop that evaluates to false immediately
     */
    @Test
    @Ignore
    void doWhileLoop() {
        assert evalCPS("""
            int x=1;
            do {
                x++;
            } while (false);
            return x;
        """)==2
    }

    /**
     * do/while loop that goes through several iterations.
     */
    @Test
    @Ignore
    void dowhileLoop5() {
        assert evalCPS("""
            int x=1;
            do {
                x++;
            } while (x<5);
            return x;
        """)==5
    }
}