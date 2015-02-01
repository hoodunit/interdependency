# interdependency
Generates a DOT graph file of a namespace's inter-function dependencies. This is done by looking up the source code for each function in a namespace, determining which symbols in the source reference another function or macro, and packaging this up into a directed graph.

Usage:
```
lein run clojure.core clojure_core_deps

# Or more generally
lein run <namespace> <output file>
```

The example file `clojure_core_deps.dot` has the inter-dependencies between `clojure.core` functions and was generated using the previous command. When pulled into a program like Gephi you can make cool charts like this:

![alt tag](https://nicholaskariniemi.github.io/img/clojure_core_deps.png)

Here for example the cluster of dependencies on the bottom left are functions in `clojure.core` that depend solely on the `defn` macro.

Note that this is a bit of a hack-job and may miss functions or have other oddities.
