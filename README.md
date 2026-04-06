```
      ███                                    ███    ██████             ███  
     ░░░                                    ███    ███░░███           ░░███ 
     █████  ██████   █████ █████  ██████   ███    ░███ ░░░  ████████   ░░███
    ░░███  ░░░░░███ ░░███ ░░███  ░░░░░███ ░███   ███████   ░░███░░███   ░███
     ░███   ███████  ░███  ░███   ███████ ░███  ░░░███░     ░███ ░███   ░███
     ░███  ███░░███  ░░███ ███   ███░░███ ░░███   ░███      ░███ ░███   ███ 
     ░███ ░░████████  ░░█████   ░░████████ ░░███  █████     ████ █████ ██░  
     ░███  ░░░░░░░░    ░░░░░     ░░░░░░░░   ░░░  ░░░░░     ░░░░ ░░░░░ ░░░   
 ███ ░███                                                                   
░░██████                                                                    
 ░░░░░░                                                                     
```

`java(fn)` Tuples is a lightweight library with no dependencies, which adds a set of tuples allowing for better readability in streams.

Note that the javafn-tuples library was extracted from the core javafn library.  Neither depends on the other (at the moment), so it makes sense to have them be different dependencies, in case only one is needed.  In a future release of the core javafn library, the tuples package will be deprecated then removed; users are encouraged to migrate to this instead.

## Tuples

`java(fn)` offers tuples similar to the Pair, Triplet, and Quartet type in [javatuples](https://www.javatuples.org/) that supports better fluent functional usage.  These are named `Pair`, `Trio`, and `Quad`.  We did not add tuples of 5 or more because the utility in pipelines is negated as the number of elements grows.

Compare

```java
IntStream.range(0, 10).mapToObj(i -> org.javatuples.Pair.with(i, i*2))
    .filter(pair -> pair.getValue0() % 2 == 0)
    .map(pair -> pair.getValue0() + pair.getValue1())
    .forEach(System.out::println);
```

versus

```java
IntStream.range(0, 10).mapToObj(i -> Pair.of(i, i*2))
    .filter(pair -> pair.matches1(i -> i % 2 == 0)
    .map(pair -> pair.map( (l, r) -> l + r))
    .forEach(System.out::println);
```

and even better

```java
IntStream.range(0, 10).mapToObj(i -> Pair.of(i, i*2))
    .filter(Pairs.matches1(i -> i % 2 == 0)
    .map(Pairs.map( (l, r) -> l + r))
    .forEach(System.out::println);
```

Notice that you get to name the parameters you're interested in, similar to destructuring in languages that support pattern matching.  `java(fn)`'s tuples contain two versions of most functions, divided into object methods and static helpers in a companion class.  The object methods operate directly on the tuple, while the helper methods return a function that accepts a tuple and operates on that.  The latter are useful in streams to avoid creating a variable named `pair` just to call a function on it.

```java
stream.map(pair -> pair.map( (l, r) -> ...));
stream.map(Pairs.map( (l, r) -> ...));
```

The `Trio` and `Quad` classes are similar to `Pair` but are not documented so we don't run the risk of them getting out of sync.  Refer to the documentation for `Pair` for all three tuples.

Another feature of the tuples is the ability to create chunks and sliding windows over arrays.

## Index

Sometimes we need to enumerate the items in a stream, for example, if you have a list (with a known size) and you want to provide a completion percentage.  This tuple behaves similar to a Pair, except that the first element is a primative int, and each element in the stream receives an increasing value from 0.

Similar to [Rust's enumerate](https://doc.rust-lang.org/std/iter/trait.Iterator.html#method.enumerate) or [Python's enumerate](https://docs.python.org/3/library/functions.html#enumerate).

```java
someStream.map(Indexed.index())
     .peek(Indexed.peek( (i, item) -> println("Completing item " + i ))
     ...;
```

or equivalently,

```java
Indexed.index(someStream)
     .peek(idx -> idx.peek( (i, item) -> println("Completing item " + i ))
     ...;
```

The former is useful for inserting into an existing stream, for example, following some filter functions.

## Using

Simply include the dependency in your build tool, for example, using maven

```xml
<dependency>
    <groupId>org.javafn</groupId>
    <artifactId>javafn-tuples</artifactId>
    <version>1.0.x</version>
</dependency>
```

or gradle

```gradle
implementation 'org.javafn:javafn-tuples:1.0.x'
```

## Contact

Contact us by email `org.javafn`at`javafn.org` or create a ticket in this repo's issues.
