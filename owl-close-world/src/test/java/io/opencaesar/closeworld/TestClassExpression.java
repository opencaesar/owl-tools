package io.opencaesar.closeworld;

import io.opencaesar.closeworld.ClassExpression.*;
import org.junit.*;

import java.util.Arrays;
import java.util.HashSet;

import static org.junit.Assert.*;

public class TestClassExpression {
	private ClassExpression a, b, c;
	private ClassExpression dc, ec, fc;
	private ClassExpression gmh, imj, kml;
	private ClassExpression min, oip, qir;
	private ClassExpression sut, uuv, wux;
	private ClassExpression empty;
	private ClassExpression universal;

	@BeforeClass public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass public static void tearDownAfterClass() throws Exception {
	}

	@Before public void setUp() throws Exception {
		a = new Unitary("a");
		b = new Unitary("a");
		c = new Unitary("b");
		dc = new Unitary("d").complement();
		ec = new Unitary("e").complement();
		fc = new Unitary("f").complement();
		gmh = new Unitary("g").difference(new Unitary("h"));;
		imj = new Unitary("i").difference(new Unitary("j"));;
		kml = new Unitary("k").difference(new Unitary("l"));;
		min = new Unitary("m").intersection(new Unitary("n"));;
		oip = new Unitary("o").intersection(new Unitary("p"));;
		qir = new Unitary("q").intersection(new Unitary("r"));;
		sut = new Unitary("s").union(new Unitary("t"));;
		uuv = new Unitary("u").union(new Unitary("v"));;
		wux = new Unitary("w").union(new Unitary("x"));;
		empty = new Empty();
		universal = new Universal();
	}

	@After public void tearDown() throws Exception {
	}

	@Test public void testEqual() {
		assertEquals(a, a);
	    assertNotEquals(a, dc);
	    assertNotEquals(a, gmh);
	    assertNotEquals(a, min);
	    assertNotEquals(a, sut);
	    assertNotEquals(a, empty);
	    assertNotEquals(a, universal);
	    assertNotEquals(dc, a);
	    assertEquals(dc, dc);
	    assertNotEquals(dc, gmh);
	    assertNotEquals(dc, min);
	    assertNotEquals(dc, sut);
	    assertNotEquals(dc, empty);
	    assertNotEquals(dc, universal);
	    assertNotEquals(gmh, a);
	    assertNotEquals(gmh, dc);
	    assertEquals(gmh, gmh);
	    assertNotEquals(gmh, min);
	    assertNotEquals(gmh, sut);
	    assertNotEquals(gmh, empty);
	    assertNotEquals(gmh, universal);
	    assertNotEquals(min, a);
	    assertNotEquals(min, dc);
	    assertNotEquals(min, gmh);
	    assertEquals(min, min);
	    assertNotEquals(min, sut);
	    assertNotEquals(min, empty);
	    assertNotEquals(min, universal);
	    assertNotEquals(sut, a);
	    assertNotEquals(sut, dc);
	    assertNotEquals(sut, gmh);
	    assertNotEquals(sut, min);
	    assertEquals(sut, sut);
	    assertNotEquals(sut, empty);
	    assertNotEquals(sut, universal);
	    assertNotEquals(empty, a);
	    assertNotEquals(empty, dc);
	    assertNotEquals(empty, gmh);
	    assertNotEquals(empty, min);
	    assertNotEquals(empty, sut);
	    assertEquals(empty, empty);
	    assertNotEquals(empty, universal);
	    assertNotEquals(universal, a);
	    assertNotEquals(universal, dc);
	    assertNotEquals(universal, gmh);
	    assertNotEquals(universal, min);
	    assertNotEquals(universal, sut);
	    assertNotEquals(universal, empty);
	    assertEquals(universal, universal);
	}
	
	// Theorem 1: For any class A, (A′)′ = A.

	@Test public void testTheorem01() {
		assertEquals(a, a.complement().complement());;
		assertEquals(dc, dc.complement().complement());;
		assertEquals(gmh, gmh.complement().complement());;
		assertEquals(min, min.complement().complement());;
		assertEquals(sut, sut.complement().complement());;
		assertEquals(empty, empty.complement().complement());;
		assertEquals(universal, universal.complement().complement());;
	}

	// Theorem 2: For any class A, A ∩ A = A.
	
	@Test public void testTheorem02() {
		assertEquals(a, a.intersection(a));;
		assertEquals(dc, dc.intersection(dc));;
		assertEquals(gmh, gmh.intersection(gmh));;
		assertEquals(min, min.intersection(min));;
		assertEquals(sut, sut.intersection(sut));;
		assertEquals(empty, empty.intersection(empty));;
		assertEquals(universal, universal.intersection(universal));;
	}

	// Theorem 3: For any classes A and B, A ∩ B = B ∩ A.
	
	@Test public void testTheorem03() {
		assertEquals(b.intersection(a), a.intersection(b));
		assertEquals(ec.intersection(a), a.intersection(ec));
		assertEquals(imj.intersection(a), a.intersection(imj));
		assertEquals(oip.intersection(a), a.intersection(oip));
		assertEquals(uuv.intersection(a), a.intersection(uuv));
		assertEquals(empty.intersection(a), a.intersection(empty));
		assertEquals(universal.intersection(a), a.intersection(universal));
		assertEquals(b.intersection(dc), dc.intersection(b));
		assertEquals(ec.intersection(dc), dc.intersection(ec));
		assertEquals(imj.intersection(dc), dc.intersection(imj));
		assertEquals(oip.intersection(dc), dc.intersection(oip));
		assertEquals(uuv.intersection(dc), dc.intersection(uuv));
		assertEquals(empty.intersection(dc), dc.intersection(empty));
		assertEquals(universal.intersection(dc), dc.intersection(universal));
		assertEquals(b.intersection(gmh), gmh.intersection(b));
		assertEquals(ec.intersection(gmh), gmh.intersection(ec));
		assertEquals(imj.intersection(gmh), gmh.intersection(imj));
		assertEquals(oip.intersection(gmh), gmh.intersection(oip));
		assertEquals(uuv.intersection(gmh), gmh.intersection(uuv));
		assertEquals(empty.intersection(gmh), gmh.intersection(empty));
		assertEquals(universal.intersection(gmh), gmh.intersection(universal));
		assertEquals(b.intersection(min), min.intersection(b));
		assertEquals(ec.intersection(min), min.intersection(ec));
		assertEquals(imj.intersection(min), min.intersection(imj));
		assertEquals(oip.intersection(min), min.intersection(oip));
		assertEquals(uuv.intersection(min), min.intersection(uuv));
		assertEquals(empty.intersection(min), min.intersection(empty));
		assertEquals(universal.intersection(min), min.intersection(universal));
		assertEquals(b.intersection(sut), sut.intersection(b));
		assertEquals(ec.intersection(sut), sut.intersection(ec));
		assertEquals(imj.intersection(sut), sut.intersection(imj));
		assertEquals(oip.intersection(sut), sut.intersection(oip));
		assertEquals(uuv.intersection(sut), sut.intersection(uuv));
		assertEquals(empty.intersection(sut), sut.intersection(empty));
		assertEquals(universal.intersection(sut), sut.intersection(universal));
		assertEquals(b.intersection(empty), empty.intersection(b));
		assertEquals(ec.intersection(empty), empty.intersection(ec));
		assertEquals(imj.intersection(empty), empty.intersection(imj));
		assertEquals(oip.intersection(empty), empty.intersection(oip));
		assertEquals(uuv.intersection(empty), empty.intersection(uuv));
		assertEquals(empty.intersection(empty), empty.intersection(empty));
		assertEquals(universal.intersection(empty), empty.intersection(universal));
		assertEquals(b.intersection(universal), universal.intersection(b));
		assertEquals(ec.intersection(universal), universal.intersection(ec));
		assertEquals(imj.intersection(universal), universal.intersection(imj));
		assertEquals(oip.intersection(universal), universal.intersection(oip));
		assertEquals(uuv.intersection(universal), universal.intersection(uuv));
		assertEquals(empty.intersection(universal), universal.intersection(empty));
		assertEquals(universal.intersection(universal), universal.intersection(universal));
	}

}
