package io.opencaesar.closeworld;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**protected
 * ClassExpression implements methods for constructing class expressions,
 * including singleton expressions encapsulating a single class, complements, intersections,
 * and unions. While the library does not perform any mathematical reasoning, it employs
 * these theorems to simplify expressions:
 * <ul>
 * <li>Theorem 1: For any class A, (A&prime;)&prime; = A.</li>
 * 
 * <li>Theorem 2: For any class A, A &cap; A = A.</li>
 * 
 * <li>Theorem 3: For any classes A and B, A &cap; B = B &cap; A.</li>
 * 
 * <li>Theorem 4: For any classes A, B, and C,
 * 				(A &cap; B) &cap; C = A &cap; (B &cap; C).</li>
 * 
 * <li>Theorem 5: For any class A, A &cup; A = A.</li>
 * 
 * <li>Theorem 6: For any classes A and B, A &cup; B = B &cup; A.</li>
 * 
 * <li>Theorem 7: For any classes A, B, and C,
 * 				(A &cup; B) &cup; C = A &cup; (B &cup; C).</li>
 * 
 * <li>Theorem 8: For any classes A, B, and C, (A\B)\C = A\(B &cup; C).</li>
 * 
 * <li>Theorem 9: For any class A and empty set &empty;, &empty; &cap; A = &empty;.</li>
 * 
 * <li>Theorem 10: For any class A, &empty; &cup; A = A.</li>
 * 
 * <li>Theorem 11: For any class A, A\&empty; = A.</li>
 * 
 * <li>Theorem 12: For any class A, &empty;\A = &empty;.</li>
 * 
 * <li>Theorem 13: For any class A, A\A = &empty;.</li>
 * 
 * <li>Theorem 14: For any class A and universal set U, U &cap; A = A.</li>
 * 
 * <li>Theorem 15: For any class A, U &cup; A = U.</li>
 * 
 * <li>Theorem 16: For any class A, A\U = &empty;.</li>
 * 
 * <li>Theorem 17: &empty;&prime; = U.</li>  
 * </ul>
 * 
 * @author		Steven Jenkins j.s.jenkins@jpl.nasa.gov
 * @version		0.0.1
 * @since		0.0.1
 */
 public abstract class ClassExpression {

	/**
	 * @return		ClassExpression The complement of this ClassExpression
	 */
	protected ClassExpression complement() { return new Complement(this); }
	
	/**
	 * @param		e ClassExpression 
	 * @return		ClassExpression The difference of this ClassExpression and another
	 * 				specified ClassExpression
	 */
	protected ClassExpression difference(ClassExpression e) {
		if (e instanceof Empty)
			// Theorem 11
			return this;
		else if (e instanceof Universal || this.equals(e))
			// Theorem 13, Theorem 16
			return new Empty();
		else
			return new Difference(this, e);
	}
	
	/**
	 * @param		e ClassExpression 
	 * @return		ClassExpression The intersection of this ClassExpression and another
	 * 				specified ClassExpression
	 */	
	protected ClassExpression intersection(ClassExpression e) {
		if (this.equals(e))
			// Theorem 2
			return this;
		else if ((e instanceof Intersection || e instanceof Empty || e instanceof Universal))
			// Theorem 3
			return e.intersection(this);
		else
			return new Intersection(new HashSet<>(Arrays.asList(this, e)));
	}
	
	/**
	 * @param		e ClassExpression 
	 * @return		ClassExpression The union of this ClassExpression and another
	 * 				specified ClassExpression
	 */
	protected ClassExpression union(ClassExpression e) {
		if (this.equals(e))
			// Theorem 5
			return this;
		else if ((e instanceof Union || e instanceof Empty || e instanceof Universal))
			// Theorem 6
			return e.union(this);
		else
			return new Union(new HashSet<>(Arrays.asList(this, e)));
	}
	
	/**
	 * @return		String A string representation of this ClassExpression as an atom
	 */
	protected String toAtom() { return "(" + toString() + ")"; }
	
	/**
	 * Universal implements methods for ClassExpressions that denote the universal class.
	
	 * @author		Steven Jenkins j.s.jenkins@jpl.nasa.gov
	 * @version		0.0.1
	 * @since		0.0.1
	 */
	public static class Universal extends ClassExpression {
		
		/**
		 * @param		o Object
		 * @return		boolean true if and only if o is a Universal
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof Universal;
		}
		
		/**
		 * @return		int hash code of Universal
		 */
		@Override
		public int hashCode() {
			return Universal.class.hashCode();
		}
		
		/**
		 * @return		String a string representation of the Universal
		 */
		@Override
		public String toString() { return "U"; }
		
		/**
		 * @return		String a string representation of the Universal as an atom
		 */
		@Override
		protected String toAtom() {
			return toString();
		}
		
		// Theorems 1, 17
		/**
		 * @return		Empty the complement of the Universal
		 */
		@Override
		protected ClassExpression complement() { return new Empty(); }
	
		// Theorem 14
		/**
		 * @param		e ClassExpression 
		 * @return		ClassExpression e
		 */	
		@Override
		protected ClassExpression intersection(ClassExpression e) { return e; }
	
		// Theorem 15
		/**
		 * @param		e ClassExpression 
		 * @return		Universal
		 */	
		@Override
		protected ClassExpression union(ClassExpression e) { return this; }
		
	}

	/**
	 * Empty implements methods for ClassExpressions that denote the empty class.
	
	 * @author		Steven Jenkins j.s.jenkins@jpl.nasa.gov
	 * @version		0.0.1
	 * @since		0.0.1
	 */
	public static class Empty extends ClassExpression {
		
		/**
		 * @param		o Object
		 * @return		boolean true if and only if o is an Empty
		 */
		@Override
		public boolean equals(Object o) {
			return o instanceof Empty;
		}
		
		/**
		 * @return		int hash code of Empty
		 */
		@Override
		public int hashCode() {
			return Empty.class.hashCode();
		}
		
		/**
		 * @return		String a string representation of the Empty
		 */
		@Override
		public String toString() {
			return "∅";
		}
		
		/**
		 * @return		String a string representation of the Empty as an atom
		 */
		@Override
		protected String toAtom() {
			return toString();
		}
		
		// Theorem 17
		/**
		 * @return		Universal the complement of the Empty
		 */
		@Override
		protected ClassExpression complement() { return new Universal(); }
		
		// Theorem 12
		/**
		 * @param		e ClassExpression
		 * @return		Empty
		 */
		@Override
		protected ClassExpression difference(ClassExpression e) { return this; }
	
		// Theorem 9
		/**
		 * @param		e ClassExpression
		 * @return		Empty
		 */
		@Override
		protected ClassExpression intersection(ClassExpression e) { return this; }
		
		// Theorem 10
		/**
		 * @param		e ClassExpression
		 * @return		ClassExpression e
		 */
		@Override
		protected ClassExpression union(ClassExpression e) { return e; }
			
	}

	/**
	 * Singleton implements methods for ClassExpressions that encapsulate an arbitrary
	 * object representing a single class.
	
	 * @author		Steven Jenkins j.s.jenkins@jpl.nasa.gov
	 * @version		0.0.1
	 * @since		0.0.1
	 */
	public static class Singleton extends ClassExpression {
		
		protected Object encapsulatedClass;

		/**
		 * A Singleton encapsulating the specified class
		 *
		 * @param		encapsulatedClass An arbitrary object representing a class
		 */
		public Singleton(Object encapsulatedClass) {
			this.encapsulatedClass = encapsulatedClass;
		}
		
		/**
		 * @param		o An arbitrary object
		 * @return		boolean true if and only if o denotes the same Singleton
		 */
		@Override
		public boolean equals(Object o) {
			return (o instanceof Singleton) &&
				((Singleton)o).encapsulatedClass.equals(encapsulatedClass);
		}
		
		/**
		 * @return		int hash code of the Singleton
		 */
		@Override
		public int hashCode() {
			return encapsulatedClass.hashCode();
		}
		
		/**
		 * @return		String a string representation of the encapsulated class
		 */
		@Override
		public String toString() {
			return encapsulatedClass.toString();
		}
		
		/**
		 * @return		String a string representation of the encapsulated class as an atom
		 */
		@Override
		protected String toAtom() {
			return toString();
		}
		
	}

	/**
	 * Unary implements methods for ClassExpressions denoting an operation on
	 * a single ClassExpression.
	
	 * @author		Steven Jenkins j.s.jenkins@jpl.nasa.gov
	 * @version		0.0.1
	 * @since		0.0.1
	 */
	protected static abstract class Unary extends ClassExpression {
		
		protected ClassExpression e;
		
		/**
		 * A Unary involving e
		 * 
		 * @param		e a ClassExpression
		 */
		protected Unary(ClassExpression e) {
			this.e = e;
		}
		
	}

	/**
	 * Complement implements methods for ClassExpressions denoting complements.
	
	 * @author		Steven Jenkins j.s.jenkins@jpl.nasa.gov
	 * @version		0.0.1
	 * @since		0.0.1
	 */
	protected static class Complement extends Unary {	
		
		/**
		 * The complement of e
		 * 
		 * @param		e a Class Expression
		 */
		protected Complement(ClassExpression e) {
			super(e);
		}
		
		/**
		 * @param		o An arbitrary object
		 * @return		boolean true if and only if o denotes the same Complement
		 */
		@Override
		public boolean equals(Object o) {
			return (o instanceof Complement) &&
				((Complement)o).e.equals(e);
		}
		
		/**
		 * @return		int hash code of the Complement
		 */
		@Override
		public int hashCode() {
			return Arrays.asList(Complement.class, e).hashCode();
		}
		
		/**
		 * @return		String string denoting this Complement
		 */
		@Override
		public String toString() {
			return e.toAtom() + "\u2032";
		}
		
		/**
		 * @return		String string denoting this Complement as an atom
		 */
		@Override
		protected String toAtom() {
			return toString();
		}
		
		/**
		 * @return		ClassExpression the complement of this Complement (simplified)
		 */
		@Override
		protected ClassExpression complement() {
			// Theorem 1
			return e;
		}
		
	}
	
	/**
	 * Binary implements methods for ClassExpressions denoting a operation on two
	 * 		ClassExpressions.
	 *
	 * @author		Steven Jenkins j.s.jenkins@jpl.nasa.gov
	 * @version		0.0.1
	 * @since		0.0.1
	 */
	protected static abstract class Binary extends ClassExpression {
		
		protected ClassExpression a;
		protected ClassExpression b;
		
		/**
		 * A Binary involving a and b
		 * 
		 * @param		a a ClassExpression
		 * @param		b a ClassExpression
		 */
		protected Binary(ClassExpression a, ClassExpression b) {
			this.a = a;
			this.b = b;
		}
		
		/**
		 * @param		op String denoting binary operator
		 * @return		String denoting this Binary
		 */
		protected String toString(String op) {
			return a.toAtom() + op + b.toAtom();
		}
			
	}
	
	/**
	 * Difference implements methods for ClassExpressions denoting set differences.
	 *
	 * @author		Steven Jenkins j.s.jenkins@jpl.nasa.gov
	 * @version		0.0.1
	 * @since		0.0.1
	 */
	protected static class Difference extends Binary {
		
		/**
		 * Difference denoting minuend minus subtrahend
		 * 
		 * @param		minuend a ClassExpression
		 * @param		subtrahend a ClassExpression
		 */
		protected Difference(ClassExpression minuend, ClassExpression subtrahend) {
			super(minuend, subtrahend);
		}
		
		/**
		 * @param		o An arbitrary object
		 * @return		boolean true if and only if o denotes the same Difference
		 */
		@Override
		public boolean equals(Object o) {
			return (o instanceof Difference) &&
				((Difference)o).a.equals(a) &&
				((Difference)o).b.equals(b);
		}
		
		/**
		 * @return		int hash code of the Difference
		 */
		@Override
		public int hashCode() {
			return Arrays.asList(Difference.class, a, b).hashCode();
		}
		
		/**
		 * @return		String denoting this Difference
		 */
		@Override
		public String toString() {
			return toString("\\");
		}
		
		/**
		 * @param		e ClassExpression 
		 * @return		ClassExpression The difference of this ClassExpression and another
		 * 				specified ClassExpression (simplified)
		 */
		@Override
		protected ClassExpression difference(ClassExpression e) {
			if (e instanceof Empty)
				// Theorem 11
				return this;
			else if (e instanceof Universal || this.equals(e))
				// Theorem 13, Theorem 16
				return new Empty();
			else
				return new Difference(a, b.union(e));
		}
		
	}
	
	/**
	 * Nary implements methods for ClassExpressions that denote an operation on a set of
	 * ClassExpressions.
	 *
	 * @author		Steven Jenkins j.s.jenkins@jpl.nasa.gov
	 * @version		0.0.1
	 * @since		0.0.1
	 */
	protected static abstract class Nary extends ClassExpression {
		
		protected Set<ClassExpression> s;
		
		/**
		 * Nary involving s
		 * 
		 * @param		s Set&lt;ClassExpression&gt;
		 */
		protected Nary(Set<ClassExpression> s) {
			this.s = s;
		}
		
		/**
		 * @param		c String denoting the operation
		 * @return		String denoting this Nary
		 */
		protected String toString(String c) {
			return s.stream().map(Object::toString).collect(Collectors.joining(c));
		}
		
		/**
		 * @return	String string denoting this Nary as an atom
		 */
		@Override
		protected String toAtom() {
		 	if (s.size() <= 1) return toString(); else return super.toAtom();
		 }
	}
	
	/**
	 * Intersection implements methods for ClassExpressions that denote the intersection of a set of
	 * ClassExpressions.
	 *
	 * @author		Steven Jenkins j.s.jenkins@jpl.nasa.gov
	 * @version		0.0.1
	 * @since		0.0.1
	 */
	protected static class Intersection extends Nary {
		
		/**
		 * Intersection of s
		 * @param		s Set&lt;ClassExpression&gt;
		 */
		protected Intersection(Set<ClassExpression> s) {
			super(s);
		}
		
		/**
		 * @param		o An arbitrary object
		 * @return		boolean true if and only if o denotes the same Intersection
		 */
		@Override
		public boolean equals(Object o) {
			return (o instanceof Intersection) &&
				((Intersection)o).s.equals(s);
		}
		
		/**
		 * @return		int hash code of the Intersection
		 */
		@Override
		public int hashCode() {
			return Arrays.asList(Intersection.class, s).hashCode();
		}
		
		/**
		 * @return		String denoting this Intersection
		 */
		@Override
		public String toString() {
			return super.toString("∩");
		}
		
		/**
		 * @param		e ClassExpression
		 * @return		Intersection denoting intersection of this Intersection with e (simplified)
		 */
		@Override
		protected ClassExpression intersection(ClassExpression e) {		
			Set<ClassExpression> newSet = new HashSet<>(s);
			// Theorem 4
			if (e instanceof Intersection)
				newSet.addAll(((Intersection)e).s);
			else
				newSet.add(e);						
			return new Intersection(newSet);
		}
	}
	
	/**
	 * Union implements methods for ClassExpressions that denote the union of a set of
	 * ClassExpressions.
	 *
	 * @author		Steven Jenkins j.s.jenkins@jpl.nasa.gov
	 * @version		0.0.1
	 * @since		0.0.1
	 */
	protected static class Union extends Nary {
		
		/**
		 * Union of s
		 * 
		 * @param		s Set&lt;ClassExpression&gt;
		 */
		protected Union(Set<ClassExpression> s) {
			super(s);
		}
		
		/**
		 * @param		o An arbitrary object
		 * @return		boolean true if and only if o denotes the same Union
		 */
		@Override
		public boolean equals(Object o) {
			return (o instanceof Union) &&
				((Union)o).s.equals(s); 
		}
		
		/**
		 * @return		int hash code of the Union
		 */
		@Override
		public int hashCode() {
			return Arrays.asList(Union.class, s).hashCode();
		}
		
		/**
		 * @return		String denoting this Union
		 */
		@Override
		public String toString() {
			return super.toString("∪");
		}
		
		/**
		 * @param		e ClassExpression
		 * @return		Union denoting union of this Union with e (simplified)
		 */
		@Override
		protected ClassExpression union(ClassExpression e) {
			Set<ClassExpression> newSet = new HashSet<>(s);
			// Theorem 7
			if (e instanceof Union)
				newSet.addAll(((Union)e).s);
			else
				newSet.add(e);	
			return new Union(newSet);
		}
		
	}

}