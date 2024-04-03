package io.opencaesar.closeworld;

import java.util.Arrays;
import java.util.Set;

/**
 * Axiom is an internal representation for the subset of OWL2-DL axioms that the bundle closure algorithm can generate.
 */
public abstract class Axiom {

	/**
	 * Creates a new Axiom object
	 */
	public Axiom() {
	}
	
    /**
     * The different options for closure strength that the bundle closure algorithm can generate.
     */
    public enum AxiomType {
        /**
         * Corresponds to OWL2-DL 9.1.3
         */
        DISJOINT_CLASSES,
        /**
         * Corresponds to OWL2-DL 9.1.2
         */
        EQUIVALENT_CLASSES,
        /**
         * Corresponds to OWL2-DL 9.1.4
         */
        DISJOINT_UNION}


    /**
     * SubClassOfAxiom corresponds to OWL2-DL 9.1.1
     * https://www.w3.org/TR/owl2-syntax/#Subclass_Axioms
     */
    public static class SubClassOfAxiom extends Axiom {

    	private final ClassExpression subclass;
    	private final ClassExpression superclass;
    	
        /**
         * Constructs an OWL2-DL SubClassOf axiom.
         * 
         * @param subclass class expression for the subclass
         * @param superclass class expression for the superclass
         */
        protected SubClassOfAxiom(ClassExpression subclass, ClassExpression superclass) {
            this.subclass = subclass;
            this.superclass = superclass;
        }

        /**
         * The subclass expression.
         * 
         * @return Class expression.
         */
        protected ClassExpression getSubclass() {
            return subclass;
        }
        
        /**
         * The superclass expression.
         * 
         * @return Class expression.
         */
        protected ClassExpression getSuperclass() {
            return superclass;
        }
        
       @Override
        public boolean equals(Object o) {
            return (o instanceof SubClassOfAxiom) &&
            		this.subclass.equals(((SubClassOfAxiom) o).getSubclass()) &&
            		this.superclass.equals(((SubClassOfAxiom) o).getSuperclass());
        }

        @Override
        public String toString() {
            return "SubClassOf(" + this.subclass.toString() + ", " + this.superclass.toString() + ")";
        }
    }

    /**
     * Some generated Axioms involve a set of class expressions.
     */
    public abstract static class ClassExpressionSetAxiom extends Axiom {

        private final Set<ClassExpression> set;

        /**
         * Constructs a ClassExpressionSetAxiom
         * 
         * @param set The set of class expressions for this axiom.
         */
        protected ClassExpressionSetAxiom(Set<ClassExpression> set) {
            super();
            this.set = set;
        }

        /**
         * The class expressions in scope of the axiom.
         * 
         * @return Class expressions.
         */
        protected Set<ClassExpression> getSet() {
            return set;
        }

        public int hashCode() {
            return set.hashCode();
        }

        public boolean equals(Object o) {
            return (o instanceof ClassExpressionSetAxiom) && (((ClassExpressionSetAxiom) o).getSet().equals(getSet()));

        }

        /**
         * Produces a readable description of the axiom.
         * 
         * @param type Axiom type
         * @return A human-readable descrition of the axiom.
         */
        public String toString(String type) {
            return type + "(" + set.toString() + ")";
        }

        /**
         * DisjointClassesAxiom corresponds to OWL2-DL 9.1.3
         * https://www.w3.org/TR/2012/REC-owl2-syntax-20121211/#Disjoint_Classes
         */
        protected static class DisjointClassesAxiom extends ClassExpressionSetAxiom {

            /**
             * Constructs an OWL2-DL Disjoint Classes axiom.
             * 
             * @param set The set of class expressions asserted to be pairwise disjoint.
             */
            protected DisjointClassesAxiom(Set<ClassExpression> set) {
                super(set);
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof DisjointClassesAxiom) && super.equals(o);
            }

            @Override
            public String toString() {
                return super.toString("DisjointClasses");
            }
        }

        /**
         * EquivalentClassesAxiom corresponds to OWL2-DL 9.1.2
         * https://www.w3.org/TR/2012/REC-owl2-syntax-20121211/#Equivalent_Classes
         */
        protected static class EquivalentClassesAxiom extends ClassExpressionSetAxiom {

            /**
             * Constructs an OWL2-DL Equivalent Classes axiom.
             * @param set The set of class expressions asserted to be semantically equivalent to each other.
             */
            protected EquivalentClassesAxiom(Set<ClassExpression> set) {
                super(set);
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof EquivalentClassesAxiom) && super.equals(o);
            }

            @Override
            public String toString() {
                return super.toString("EquivalentClasses");
            }
        }

        /**
         * DisjointUnionAxiom corresponds to OWL2-DL 9.1.4
         * https://www.w3.org/TR/2012/REC-owl2-syntax-20121211/#Disjoint_Union_of_Class_Expressions
         */
        protected static class DisjointUnionAxiom extends ClassExpressionSetAxiom {

            /**
             * The single class that is the disjoint union of class expressions.
             */
            protected ClassExpression.Unitary c;

            /**
             * Constructs an OWL2-DL Disjoint Union of Class Expressions actions.
             * 
             * @param c The single class that is the disjoint union of class expressions.
             * @param set The class expressions asserted to be pairwise disjoint.
             */
            protected DisjointUnionAxiom(ClassExpression.Unitary c, Set<ClassExpression> set) {
                super(set);
                this.c = c;
            }

            /**
             * The single class that is the disjoint union of class expressions.
             * 
             * @return a ClassExpression.Singleton
             */
            protected ClassExpression.Unitary getC() {
                return c;
            }

            @Override
            public int hashCode() {
                return Arrays.asList(c, getSet()).hashCode();
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof DisjointUnionAxiom) &&
                        c.equals(((DisjointUnionAxiom) o).getC()) && super.equals(o);
            }

            @Override
            public String toString() {
                return "DisjointUnion(" + c.toString() + ", " + getSet().toString() + ")";
            }
        }
    }

}
