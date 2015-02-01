(ns interdependency.core
  (:gen-class)
  (:require [clojure.set :as set]
            [clique.core :as c]
            [clojure.repl :as repl]
            [lacij.view.graphview :as lgv]
            [lacij.edit.graph :as leg]
            [lacij.layouts.layout :as lll]
            [loom.graph :as graph]
            [loom.alg :as alg]))

;; (def core-deps (c/dependencies 'clojure.core))
;; (def println-deps ('clojure.core/println (c/dependencies 'clojure.core)))
;; (def println-existing-deps (filter #(not (nil? %)) (map #(ns-resolve 'clojure.core %) println-deps)))

(defn get-qualified-name [sym sym-ns]
  (let [resolved (ns-resolve sym-ns sym)]
    (when resolved
      (let [metadata (meta resolved)
            fn-name (name (:name metadata))
            ns-name (name (.. (:ns metadata) name))]
        (symbol ns-name fn-name)))))

(defn is-symbol-fn? [sym sym-ns]
  (when sym
    (try
      (let [resolved (ns-resolve sym-ns sym)]
        (when (var? resolved)
          (fn? (var-get resolved))))
      (catch java.lang.ClassNotFoundException e
        nil))))

(defn get-fn-deps [fun ns-deps fun-ns]
  (let [deps (ns-deps fun)
        fn-deps (filter #(is-symbol-fn? % fun-ns) deps)
        namespaced-fn-deps (map #(get-qualified-name % fun-ns) fn-deps)
        unique-deps (set namespaced-fn-deps)]
    unique-deps))

(defn get-all-deps-in-ns [fun fun-ns]
  (let [core-deps (c/dependencies fun-ns)
        nsed-fun (get-qualified-name fun fun-ns)]
    (loop [deps {}
           rem-deps #{nsed-fun}]
      (if (not (empty? rem-deps))
        (if (deps (first rem-deps))
          (recur deps (rest rem-deps))
          (let [fn-deps (get-fn-deps (first rem-deps) core-deps fun-ns)
                not-checked (set/difference fn-deps (set (keys deps)) #{(first rem-deps)})
                new-deps (assoc deps (first rem-deps) (into [] fn-deps))
                new-rem-deps (into #{} (set/union not-checked (rest rem-deps)))]
            (recur new-deps new-rem-deps)))
        deps))))

(defn get-unused-core-fns [fun]
  (let [core-deps (keys (c/dependencies 'clojure.core))
        deps (keys (get-all-deps-in-ns fun 'clojure.core))
        core-set (set core-deps)
        deps-set (set deps)
        unused (set/difference core-set deps-set)]
    unused))

;; (defn get-unused-core-fns [funs]
;;   (let [core-deps (keys (c/dependencies 'clojure.core))
;;         core-set (set core-deps)
;;         all-deps (reduce (fn [a b]
;;                            (let [deps (set (keys (get-all-deps-in-ns b 'clojure.core)))]
;;                              (set/union a b))) #{} funs)
;;         unused (set/difference core-set all-deps)]
;;     unused))

;; (def core-functions (map :name (c/functions 'clojure.core)))

(defn functions [ns]
  (ns-interns ns))

(defn resolve-fn-or-class [sym]
  (try (when-let [resolved (ns-resolve 'clojure.core sym)]
         (if-let [m (meta resolved)]
           (:name m)
           resolved))
       (catch Exception e nil)))

(defn resolve-fn [sym]
  (try (let [resolved (ns-resolve 'clojure.core sym)
             m (meta resolved)]
         (:name m))
       (catch Exception e nil)))

(defn get-string-symbols [string]
  (let [str-list (read-string string)
        flattened (flatten str-list)
        symbols (filter symbol? flattened)]
    symbols))

(defn parse-functions [fns-str]
  (when fns-str
    (let [symbols (get-string-symbols fns-str)
          resolved (->> symbols
                        ;(map resolve-fn-or-class)
                        (map resolve-fn)
                        (filter #(not (nil? %))))
          unique (set resolved)]
      unique)))

(defn function-deps [ns]
  (let [fns (functions ns)
        fn-symbols (keys fns)
        fn-full-names (map #(symbol (str ns) (str %)) fn-symbols)
        fn-source-strings (map repl/source-fn fn-full-names)
        fn-deps (map parse-functions fn-source-strings)
        fn-with-deps (zipmap fn-symbols fn-deps)]
    fn-with-deps))

;; (def core-functions (functions 'clojure.core))
;; (def core-deps (function-deps 'clojure.core))
;; (def core-deps-as-list (into {} (map (fn [[k v]] [k (into '() v)]) core-deps)))

;; (def core-graph (c/graph core-deps-as-list))

(defn export-graph [graph path]
  (let [layout (lll/layout graph :naive)
        build (leg/build layout)]
    (lgv/export build path :indent "yes")))

;; (def core-graph (graph/digraph core-deps))
;; (def println-deps (alg/bf-traverse g 'println))
        
  ;; (lgv/export (leg/build (lll/layout graph :naive)) path :indent "yes"))

(defn export-core-deps-as-graph [ns filename]
  (let [deps (function-deps (symbol ns))
        deps-as-list (into {} (map (fn [[k v]] [k (into '() v)]) deps))
        nodes (c/nodes deps-as-list)
        edges (c/edges deps-as-list)]
    (c/export-graphviz nodes edges filename)))

(defn print-usage []
  (println "Usage: lein run <namespace> <output filename>")
  (println "Calculates and exports clojure.core dependencies as a DOT graph file (.dot).")
  (println "Example: lein run clojure.core core_deps"))

(defn -main [& args]
  (if (not= (count args) 2)
    (print-usage)
    (let [[ns filename _] args]
      (export-core-deps-as-graph ns filename))))

