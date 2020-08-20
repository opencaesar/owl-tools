package io.opencaesar.closetaxonomy;

import java.util.Arrays;
import java.util.Set;

public abstract class Axiom {

    public enum AxiomType {DISJOINT_CLASSES, EQUIVALENT_CLASSES, DISJOINT_UNION}

    public abstract static class ClassExpressionSetAxiom extends Axiom {

        private final Set<ClassExpression> set;

        protected ClassExpressionSetAxiom(Set<ClassExpression> set) {
            super();
            this.set = set;
        }

        protected Set<ClassExpression> getSet() {
            return set;
        }

        public int hashCode() {
            return set.hashCode();
        }

        public boolean equals(Object o) {
            return (o instanceof ClassExpressionSetAxiom) && (((ClassExpressionSetAxiom) o).getSet().equals(getSet()));

        }
        public String toString(String type) {
            return type + "(" + set.toString() + ")";
        }

        protected static class DisjointClassesAxiom extends ClassExpressionSetAxiom {

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

        protected static class EquivalentClassesAxiom extends ClassExpressionSetAxiom {

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

        protected static class DisjointUnionAxiom extends ClassExpressionSetAxiom {

            protected ClassExpression.Singleton c;

            protected DisjointUnionAxiom(ClassExpression.Singleton c, Set<ClassExpression> set) {
                super(set);
                this.c = c;
            }

            protected ClassExpression.Singleton getC() {
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
