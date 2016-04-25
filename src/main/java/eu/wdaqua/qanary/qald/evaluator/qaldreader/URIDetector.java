package eu.wdaqua.qanary.qald.evaluator.qaldreader;

import org.apache.jena.sparql.syntax.*;
import org.apache.xerces.util.URI;

import java.util.List;

/**
 * Created by didier on 25.04.16.
 */
public class URIDetector implements ElementVisitor {

    private List<String> subjects;
    private List<String> predicates;

    public List<String> getSubjects() {
        return subjects;
    }

    public List<String> getPredicates() {
        return predicates;
    }

    public List<String> getObjects() {
        return objects;
    }

    private List<String> objects;


    @Override
    public void visit(ElementTriplesBlock el) {

    }

    @Override
    public void visit(ElementPathBlock el) {
        el.getPattern().forEach(triplePath -> {
            if (triplePath.getSubject().isURI()) {
                subjects.add(triplePath.getSubject().getURI());
            }
            if (triplePath.getPredicate().isURI()) {
                predicates.add(triplePath.getPredicate().getURI());
            }
            if (triplePath.getObject().isURI()) {
                objects.add(triplePath.getObject().getURI());
            }
        });
    }

    @Override
    public void visit(ElementFilter el) {
    }

    @Override
    public void visit(ElementAssign el) {

    }

    @Override
    public void visit(ElementBind el) {

    }

    @Override
    public void visit(ElementData el) {

    }

    @Override
    public void visit(ElementUnion el) {
        for (Element element : el.getElements()) {
            element.visit(this);
        }
    }

    @Override
    public void visit(ElementOptional el) {
        el.getOptionalElement().visit(this);
    }

    @Override
    public void visit(ElementGroup el) {
        for (Element element : el.getElements()) {
            element.visit(this);
        }
    }

    @Override
    public void visit(ElementDataset el) {

    }

    @Override
    public void visit(ElementNamedGraph el) {

    }

    @Override
    public void visit(ElementExists el) {
    }

    @Override
    public void visit(ElementNotExists el) {

    }

    @Override
    public void visit(ElementMinus el) {
        el.getMinusElement().visit(this);
    }

    @Override
    public void visit(ElementService el) {

    }

    @Override
    public void visit(ElementSubQuery el) {
        el.getQuery().getQueryPattern().visit(this);
    }
}
