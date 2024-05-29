package io.opencaesar.closeworld;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestAxiom {

	ClassExpression.Difference ciaubmd, buaicmd;
    Set<ClassExpression> ces1a, ces1b, ces2a, ces2b;
    Axiom.ClassExpressionSetAxiom.DisjointClassesAxiom djca1a, djca1b, djca2a, djca2b;
    Axiom.ClassExpressionSetAxiom.EquivalentClassesAxiom eqca1a, eqca1b, eqca2a, eqca2b;
    Axiom.ClassExpressionSetAxiom.DisjointUnionAxiom djua1a, djua1b, djua2a, djua2b;
    Axiom.SubClassOfAxiom scoa1, scoa2, scoa3, scoa4, scoa5;
    ClassExpression.Unitary a, b, e;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {

                                      a = new ClassExpression.Unitary("a");
                                      b = new ClassExpression.Unitary("b");
        final ClassExpression.Unitary c = new ClassExpression.Unitary("c");
        final ClassExpression.Unitary d = new ClassExpression.Unitary("d");
                                      e = new ClassExpression.Unitary("e");

        final ClassExpression.Union          aub = new ClassExpression.Union(new HashSet<>(Arrays.asList(a, b)));
        final ClassExpression.Union          bua = new ClassExpression.Union(new HashSet<>(Arrays.asList(b, a)));
        final ClassExpression.Intersection ciaub = new ClassExpression.Intersection(new HashSet<>(Arrays.asList(c, aub)));
        final ClassExpression.Intersection buaic = new ClassExpression.Intersection(new HashSet<>(Arrays.asList(bua, c)));
                                         ciaubmd = new ClassExpression.Difference(ciaub, d);
                                         buaicmd = new ClassExpression.Difference(buaic, d);

        ces1a = Stream.of(aub, ciaub).collect(Collectors.toSet());
        ces1b = Stream.of(bua, buaic).collect(Collectors.toSet());
        ces2a = Stream.of(aub, ciaub, ciaubmd, e).collect(Collectors.toSet());
        ces2b = Stream.of(bua, buaic, buaicmd, e).collect(Collectors.toSet());

        scoa1  = new Axiom.SubClassOfAxiom(a, b);
        scoa2  = new Axiom.SubClassOfAxiom(ciaubmd, b);
        scoa3  = new Axiom.SubClassOfAxiom(a, buaicmd);
        scoa4  = new Axiom.SubClassOfAxiom(ciaubmd, buaicmd);
        scoa5  = new Axiom.SubClassOfAxiom(buaicmd, ciaubmd);
        
        djca1a = new Axiom.ClassExpressionSetAxiom.DisjointClassesAxiom(ces1a);
        djca1b = new Axiom.ClassExpressionSetAxiom.DisjointClassesAxiom(ces1b);
        djca2a = new Axiom.ClassExpressionSetAxiom.DisjointClassesAxiom(ces2a);
        djca2b = new Axiom.ClassExpressionSetAxiom.DisjointClassesAxiom(ces2b);

        eqca1a = new Axiom.ClassExpressionSetAxiom.EquivalentClassesAxiom(ces1a);
        eqca1b = new Axiom.ClassExpressionSetAxiom.EquivalentClassesAxiom(ces1b);
        eqca2a = new Axiom.ClassExpressionSetAxiom.EquivalentClassesAxiom(ces2a);
        eqca2b = new Axiom.ClassExpressionSetAxiom.EquivalentClassesAxiom(ces2b);

        djua1a = new Axiom.ClassExpressionSetAxiom.DisjointUnionAxiom(e, ces1a);
        djua1b = new Axiom.ClassExpressionSetAxiom.DisjointUnionAxiom(e, ces1b);
        djua2a = new Axiom.ClassExpressionSetAxiom.DisjointUnionAxiom(e, ces2a);
        djua2b = new Axiom.ClassExpressionSetAxiom.DisjointUnionAxiom(e, ces2b);

    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testHashCode() {

        Assert.assertEquals(djca1a.hashCode(), djca1b.hashCode());
        Assert.assertEquals(djca2a.hashCode(), djca2b.hashCode());

        Assert.assertEquals(eqca1a.hashCode(), eqca1b.hashCode());
        Assert.assertEquals(eqca2a.hashCode(), eqca2b.hashCode());

        Assert.assertEquals(djua1a.hashCode(), djua1b.hashCode());
        Assert.assertEquals(djua2a.hashCode(), djua2b.hashCode());
    }

    @Test
    public void testEquals() {

        // equivalent class expressions
        Assert.assertEquals(djca1a, djca1b);
        Assert.assertEquals(djca2a, djca2b);

        // non-equivalent class expressions
        Assert.assertNotEquals(djca1a, djca2a);
        Assert.assertNotEquals(djca1a, djca2b);
        Assert.assertNotEquals(djca1b, djca2a);
        Assert.assertNotEquals(djca1b, djca2b);

        // equivalent class expressions
        Assert.assertEquals(eqca1a, eqca1b);
        Assert.assertEquals(eqca2a, eqca2b);

        // non-equivalent class expressions
        Assert.assertNotEquals(eqca1a, eqca2a);
        Assert.assertNotEquals(eqca1a, eqca2b);
        Assert.assertNotEquals(eqca1b, eqca2a);
        Assert.assertNotEquals(eqca1b, djca2b);

        // equivalent class expressions
        Assert.assertEquals(djua1a, djua1b);
        Assert.assertEquals(djua2a, djua2b);

        // non-equivalent class expressions
        Assert.assertNotEquals(djua1a, djua2a);
        Assert.assertNotEquals(djua1a, djua2b);
        Assert.assertNotEquals(djua1b, djua2a);
        Assert.assertNotEquals(djua1b, djca2b);

        // equivalent class expressions
        Assert.assertEquals(scoa4, scoa5);

        // non-equivalent class expressions
        Assert.assertNotEquals(scoa1, scoa2);
        Assert.assertNotEquals(scoa1, scoa3);
        Assert.assertNotEquals(scoa1, scoa4);
        Assert.assertNotEquals(scoa1, scoa5);
        
        // different axiom types
        Assert.assertNotEquals(djca1a, eqca1a);
        Assert.assertNotEquals(djca1a, eqca1b);
        Assert.assertNotEquals(djca1a, eqca2a);
        Assert.assertNotEquals(djca1a, eqca2b);

        Assert.assertNotEquals(djca1b, eqca1a);
        Assert.assertNotEquals(djca1b, eqca1b);
        Assert.assertNotEquals(djca1b, eqca2a);
        Assert.assertNotEquals(djca1b, eqca2b);

        Assert.assertNotEquals(djca2a, eqca1a);
        Assert.assertNotEquals(djca2a, eqca1b);
        Assert.assertNotEquals(djca2a, eqca2a);
        Assert.assertNotEquals(djca2a, eqca2b);

        Assert.assertNotEquals(djca2b, eqca1a);
        Assert.assertNotEquals(djca2b, eqca1b);
        Assert.assertNotEquals(djca2b, eqca2a);
        Assert.assertNotEquals(djca2b, eqca2b);

        Assert.assertNotEquals(djca1a, djua1a);
        Assert.assertNotEquals(djca1a, djua1b);
        Assert.assertNotEquals(djca1a, djua2a);
        Assert.assertNotEquals(djca1a, djua2b);

        Assert.assertNotEquals(djca1b, djua1a);
        Assert.assertNotEquals(djca1b, djua1b);
        Assert.assertNotEquals(djca1b, djua2a);
        Assert.assertNotEquals(djca1b, djua2b);

        Assert.assertNotEquals(djca2a, djua1a);
        Assert.assertNotEquals(djca2a, djua1b);
        Assert.assertNotEquals(djca2a, djua2a);
        Assert.assertNotEquals(djca2a, djua2b);

        Assert.assertNotEquals(djca2b, djua1a);
        Assert.assertNotEquals(djca2b, djua1b);
        Assert.assertNotEquals(djca2b, djua2a);
        Assert.assertNotEquals(djca2b, djua2b);

        Assert.assertNotEquals(eqca1a, djua1a);
        Assert.assertNotEquals(eqca1a, djua1b);
        Assert.assertNotEquals(eqca1a, djua2a);
        Assert.assertNotEquals(eqca1a, djua2b);

        Assert.assertNotEquals(eqca1b, djua1a);
        Assert.assertNotEquals(eqca1b, djua1b);
        Assert.assertNotEquals(eqca1b, djua2a);
        Assert.assertNotEquals(eqca1b, djua2b);

        Assert.assertNotEquals(eqca2a, djua1a);
        Assert.assertNotEquals(eqca2a, djua1b);
        Assert.assertNotEquals(eqca2a, djua2a);
        Assert.assertNotEquals(eqca2a, djua2b);

        Assert.assertNotEquals(eqca2b, djua1a);
        Assert.assertNotEquals(eqca2b, djua1b);
        Assert.assertNotEquals(eqca2b, djua2a);
        Assert.assertNotEquals(eqca2b, djua2b);

        Assert.assertNotEquals(djca1a, scoa1);
        Assert.assertNotEquals(eqca1a, scoa1);
        
    }

    @Test
    public void testToString() {

        Assert.assertEquals("DisjointClasses(" + ces1a.toString() + ")", djca1a.toString());
        Assert.assertEquals("DisjointClasses(" + ces1b.toString() + ")", djca1b.toString());
        Assert.assertEquals("DisjointClasses(" + ces2a.toString() + ")", djca2a.toString());
        Assert.assertEquals("DisjointClasses(" + ces2b.toString() + ")", djca2b.toString());

        Assert.assertEquals("EquivalentClasses(" + ces1a.toString() + ")", eqca1a.toString());
        Assert.assertEquals("EquivalentClasses(" + ces1b.toString() + ")", eqca1b.toString());
        Assert.assertEquals("EquivalentClasses(" + ces2a.toString() + ")", eqca2a.toString());
        Assert.assertEquals("EquivalentClasses(" + ces2b.toString() + ")", eqca2b.toString());

        Assert.assertEquals("DisjointUnion(" + e.toString() + ", " + ces1a.toString() + ")", djua1a.toString());
        Assert.assertEquals("DisjointUnion(" + e.toString() + ", " + ces1b.toString() + ")", djua1b.toString());
        Assert.assertEquals("DisjointUnion(" + e.toString() + ", " + ces2a.toString() + ")", djua2a.toString());
        Assert.assertEquals("DisjointUnion(" + e.toString() + ", " + ces2b.toString() + ")", djua2b.toString());
        
        Assert.assertEquals("SubClassOf(" + a.toString() + ", " + b.toString() + ")", scoa1.toString());
        Assert.assertEquals("SubClassOf(" + ciaubmd.toString() + ", " + b.toString() + ")", scoa2.toString());
        Assert.assertEquals("SubClassOf(" + a.toString() + ", " + buaicmd.toString() + ")", scoa3.toString());
        Assert.assertEquals("SubClassOf(" + ciaubmd.toString() + ", " + buaicmd.toString() + ")", scoa4.toString());
        Assert.assertEquals("SubClassOf(" + buaicmd.toString() + ", " + ciaubmd.toString() + ")", scoa5.toString());

    }

}
