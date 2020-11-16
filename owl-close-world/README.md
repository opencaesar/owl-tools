# OWL Close World

[ ![Download](https://api.bintray.com/packages/opencaesar/owl-tools/owl-close-world/images/download.svg) ](https://bintray.com/opencaesar/owl-tools/owl-close-world/_latestVersion)

A library of different algorithms to close the world on OWL ontologies

## Vocabulary Closure

### Introduction

One of the key features of the Semantic Web (and its formal foundations in Description Logic) is the so-called open-world semantics, which means simply that any unasserted claim may be true or false. The world of ordinary human interaction is open in just this way; if neither author of this paper is known to be a graduate of UCLA, then either might be. And in fact, one is and one is not.

Open-world semantics typically do not apply in traditional database applications. If my name does not appear in the table of customers, then an application is justified in concluding I am not (yet) a customer.

Neither open- nor closed-world semantics are “correct”, per se. Each has its appropriate uses, and it is important to be aware of the semantics in effect and draw proper conclusions.

One aspect of open-world semantics that is sometimes surprising to people familiar with object-oriented software development has to do with implicit disjointness. Suppose, for example, in Java, we declare classes as follows:
```
public class Vehicle {}
public class Person {}
```
We naturally expect that an object may be a `Vehicle` or it may be a `Person`, but it cannot be both. That is, the sets of objects of type `Vehicle` and `Person` are _disjoint_. Formally, two sets are disjoint if their intersection is empty.

If instead, however, we want to declare classes that are not disjoint because one is a subclass of the other, Java requires us to declare the relation explicitly:
```
public class Vehicle {}
```
The situation with OWL and the Semantic Web is somewhat different. Suppose we declare OWL classes as follows:
```
Declaration(Class(Vehicle))
```
If we assert nothing further, then any pair of classes may have a nonempty intersection. To assert the the situation from the Java example, we must add two axioms:
```
DisjointClasses(Vehicle Person)
SubClassOf(Car Vehicle)
```
Note that we do not need to assert
```
DisjointClasses(Car Person)
```
That fact follows from the definition of disjointness--a DL reasoner will infer it. Note also that, while it is probably not what we mean, it would not be logically inconsistent to assert
```
DisjointClasses(Car Vehicle)
```
A reasoner will include that the set of cars is empty, but the empty set is a valid set. A class that can have no members is said to be _unsatisfiable_.
## Desciption Closure
