package io.opencaesar.owl.reason_incrementally;

import openllet.aterm.ATerm;
import openllet.aterm.ATermAppl;
import openllet.aterm.ATermList;
import openllet.core.utils.ATermUtils;
import openllet.jena.graph.converter.TripleAdder;
import openllet.jena.vocabulary.OWL2;
import openllet.jena.vocabulary.SWRL;
import org.apache.jena.graph.BlankNodeId;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class ATermApplVisitor {


    public static String convertNary(final ATermAppl axiom, final Resource kind, final Property property) {
        String buff = kind.getLocalName();
        for (int i = 0; i < axiom.getArity(); i++) {
            if (0 == i)
                buff = buff + "(";
            else
                buff = buff + ",";
            final ATerm term = axiom.getArgument(i);
            buff = buff + term.toString();
        }
        buff = buff + ")";
        return buff;
    }
    public static String convertBinary(final ATermAppl axiom, final Resource kind) {
        assert 2 == axiom.getArity();
        final ATerm term1 = axiom.getArgument(0);
        final ATerm term2 = axiom.getArgument(1);
        return term1.toString() + " " + kind.getLocalName() + " " + term2.toString();
    }

    public static String convertUnary(final ATermAppl axiom, final Resource kind) {
        assert 1 == axiom.getArity();
        final ATerm term = axiom.getArgument(0);
        return kind.getLocalName() + " " + term.toString();
    }

    public static String convert(final ATermAppl axiom) {

        if (axiom.getAFun().equals(ATermUtils.EQCLASSFUN))
            return convertBinary(axiom, OWL.equivalentClass);
        else if (axiom.getAFun().equals(ATermUtils.SUBFUN))
            return convertBinary(axiom, RDFS.subClassOf);
        else if (axiom.getAFun().equals(ATermUtils.DISJOINTFUN))
            return convertBinary(axiom, OWL.disjointWith);
        else if (axiom.getAFun().equals(ATermUtils.DISJOINTSFUN))
            return convertNary(axiom, OWL2.AllDisjointClasses, OWL2.members);
        else if (axiom.getAFun().equals(ATermUtils.EQPROPFUN))
            return convertBinary(axiom, OWL.equivalentProperty);
        else if (axiom.getAFun().equals(ATermUtils.SUBPROPFUN)) {
            if (axiom.getArgument(0) instanceof ATermList) {
                // https://www.w3.org/TR/2012/REC-owl2-mapping-to-rdf-20121211/
                // SubObjectPropertyOf( ObjectPropertyChain( OPE1 ... OPEn ) OPE )
                // T(OPE) owl:propertyChainAxiom T(SEQ OPE1 ... OPEn) .

//                final Node s = _converter.convert(axiom.getArgument(1));
//                final Node o = _converter.convert(axiom.getArgument(0));
//
//                TripleAdder.add(_graph, s, OWL2.propertyChainAxiom, o);
                return "";
            } else
                return convertBinary(axiom, RDFS.subPropertyOf);
        } else if (axiom.getAFun().equals(ATermUtils.DISJOINTPROPFUN))
            return convertBinary(axiom, OWL2.propertyDisjointWith);
        else if (axiom.getAFun().equals(ATermUtils.DISJOINTPROPSFUN))
            return convertNary(axiom, OWL2.AllDisjointProperties, OWL2.members);
        else if (axiom.getAFun().equals(ATermUtils.DOMAINFUN))
            return convertBinary(axiom, RDFS.domain);
        else if (axiom.getAFun().equals(ATermUtils.RANGEFUN))
            return convertBinary(axiom, RDFS.range);
        else if (axiom.getAFun().equals(ATermUtils.INVPROPFUN))
            return convertBinary(axiom, OWL.inverseOf);
        else if (axiom.getAFun().equals(ATermUtils.TRANSITIVEFUN))
            return convertUnary(axiom, OWL.TransitiveProperty);
        else if (axiom.getAFun().equals(ATermUtils.FUNCTIONALFUN))
            return convertUnary(axiom, OWL.FunctionalProperty);
        else if (axiom.getAFun().equals(ATermUtils.INVFUNCTIONALFUN))
            return convertUnary(axiom, OWL.InverseFunctionalProperty);
        else if (axiom.getAFun().equals(ATermUtils.SYMMETRICFUN))
            return convertUnary(axiom, OWL.SymmetricProperty);
        else if (axiom.getAFun().equals(ATermUtils.ASYMMETRICFUN))
            return convertUnary(axiom, OWL2.AsymmetricProperty);
        else if (axiom.getAFun().equals(ATermUtils.REFLEXIVEFUN))
            return convertUnary(axiom, OWL2.ReflexiveProperty);
        else if (axiom.getAFun().equals(ATermUtils.IRREFLEXIVEFUN))
            return convertUnary(axiom, OWL2.IrreflexiveProperty);
        else if (axiom.getAFun().equals(ATermUtils.TYPEFUN))
            return convertBinary(axiom, RDF.type);
        else if (axiom.getAFun().equals(ATermUtils.SAMEASFUN))
            return convertBinary(axiom, OWL.sameAs);
        else if (axiom.getAFun().equals(ATermUtils.DIFFERENTFUN))
            return convertBinary(axiom, OWL.differentFrom);
        else if (axiom.getAFun().equals(ATermUtils.ALLDIFFERENTFUN))
            return convertNary(axiom, OWL.AllDifferent, OWL2.members);
        else if (axiom.getAFun().equals(ATermUtils.NOTFUN)) {
//            axiom = (ATermAppl) axiom.getArgument(0);
//
//            final Node p = _converter.convert(axiom.getArgument(0));
//            final Node s = _converter.convert(axiom.getArgument(1));
//            final Node o = _converter.convert(axiom.getArgument(2));
//
//            final Node n = NodeFactory.createBlankNode();
//            TripleAdder.add(_graph, n, RDF.type, OWL2.NegativePropertyAssertion);
//            TripleAdder.add(_graph, n, RDF.subject, s);
//            TripleAdder.add(_graph, n, RDF.predicate, p);
//            TripleAdder.add(_graph, n, RDF.object, o);
            return "";
        } else if (axiom.getAFun().equals(ATermUtils.PROPFUN)) {
//            final Node p = _converter.convert(axiom.getArgument(0));
//            final Node s = _converter.convert(axiom.getArgument(1));
//            final Node o = _converter.convert(axiom.getArgument(2));
//
//            TripleAdder.add(_graph, s, p, o);
            return "";
        } else if (axiom.getAFun().equals(ATermUtils.RULEFUN)) {
//            Node node = null;
//
//            final ATermAppl name = (ATermAppl) axiom.getArgument(0);
//            if (name == ATermUtils.EMPTY)
//                node = NodeFactory.createBlankNode();
//            else if (ATermUtils.isBnode(name))
//                node = NodeFactory.createBlankNode(new BlankNodeId(((ATermAppl) name.getArgument(0)).getName()));
//            else
//                node = NodeFactory.createURI(name.getName());
//
//            TripleAdder.add(_graph, node, RDF.type, SWRL.Imp);
//
//            ATermList head = (ATermList) axiom.getArgument(1);
//            if (head.isEmpty())
//                TripleAdder.add(_graph, node, SWRL.head, RDF.nil);
//            else {
//                Node list = null;
//                for (; !head.isEmpty(); head = head.getNext()) {
//                    final Node atomNode = convertAtom((ATermAppl) head.getFirst());
//                    final Node newList = NodeFactory.createBlankNode();
//                    TripleAdder.add(_graph, newList, RDF.type, SWRL.AtomList);
//                    TripleAdder.add(_graph, newList, RDF.first, atomNode);
//                    if (list != null)
//                        TripleAdder.add(_graph, list, RDF.rest, newList);
//                    else
//                        TripleAdder.add(_graph, node, SWRL.head, newList);
//                    list = newList;
//                }
//                TripleAdder.add(_graph, list, RDF.rest, RDF.nil);
//            }
//
//            ATermList body = (ATermList) axiom.getArgument(2);
//            if (body.isEmpty())
//                TripleAdder.add(_graph, node, SWRL.body, RDF.nil);
//            else {
//                Node list = null;
//                for (; !body.isEmpty(); body = body.getNext()) {
//                    final Node atomNode = convertAtom((ATermAppl) body.getFirst());
//                    final Node newList = NodeFactory.createBlankNode();
//                    TripleAdder.add(_graph, newList, RDF.type, SWRL.AtomList);
//                    TripleAdder.add(_graph, newList, RDF.first, atomNode);
//                    if (list != null)
//                        TripleAdder.add(_graph, list, RDF.rest, newList);
//                    else
//                        TripleAdder.add(_graph, node, SWRL.body, newList);
//                    list = newList;
//                }
//                TripleAdder.add(_graph, list, RDF.rest, RDF.nil);
//            }
            return "";
        } else {
            return "";
        }
    }
}
