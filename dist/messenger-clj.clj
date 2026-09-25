(ns borkdude.dynaload
  #?(:cljs (:require-macros [borkdude.dynaload :refer [dynaload if-bb]])))

(defmacro if-bb
  [then else]
  (if #?(:clj (System/getProperty "babashka.version")
         :cljs false)
    then
    else))

(if-bb
    #?(:clj
       (defn ->LazyVar [f _]
         (let [cached (volatile! nil)]
           (reify
             clojure.lang.IDeref
             (deref [_this]
               (if-not (nil? @cached)
                 cached
                 (let [x (f)]
                   (when-not (nil? x)
                     (vreset! cached x))
                   x)))
             clojure.lang.IFn
             (invoke [this]
               (@this))
             (invoke [this a]
               (@this a))
             (invoke [this a b]
               (@this a b))
             (invoke [this a b c]
               (@this a b c))
             (invoke [this a b c d]
               (@this a b c d))
             (invoke [this a b c d e]
               (@this a b c d e))
             (invoke [this a b c d e f]
               (@this a b c d e f))
             (invoke [this a b c d e f g]
               (@this a b c d e f g))
             (invoke [this a b c d e f g h]
               (@this a b c d e f g h))
             (invoke [this a b c d e f g h i]
               (@this a b c d e f g h i))
             (invoke [this a b c d e f g h i j]
               (@this a b c d e f g h i j))
             (invoke [this a b c d e f g h i j k]
               (@this a b c d e f g h i j k))
             (invoke [this a b c d e f g h i j k l]
               (@this a b c d e f g h i j k l))
             (invoke [this a b c d e f g h i j k l m]
               (@this a b c d e f g h i j k l m))
             (invoke [this a b c d e f g h i j k l m n]
               (@this a b c d e f g h i j k l m n))
             (invoke [this a b c d e f g h i j k l m n o]
               (@this a b c d e f g h i j k l m n o))
             (invoke [this a b c d e f g h i j k l m n o p]
               (@this a b c d e f g h i j k l m n o p))
             (invoke [this a b c d e f g h i j k l m n o p q]
               (@this a b c d e f g h i j k l m n o p q))
             (invoke [this a b c d e f g h i j k l m n o p q r]
               (@this a b c d e f g h i j k l m n o p q r))
             (invoke [this a b c d e f g h i j k l m n o p q r s]
               (@this a b c d e f g h i j k l m n o p q r s))
             ;; for some reason not working yet in bb
             #_(invoke [this a b c d e f g h i j k l m n o p q r s t]
                 (@this a b c d e f g h i j k l m n o p q r s t))
             #_(invoke [this a b c d e f g h i j k l m n o p q r s t rest]
                 (apply @this a b c d e f g h i j k l m n o p q r s t rest))
             (applyTo [this args]
               (apply @this args)))))
       :cljs nil)
  #?(:org.babashka/nbb nil
     :default
     (deftype LazyVar #?(:clj [f ^:volatile-mutable cached] :cljs [f ^:mutable cached])
       #?(:clj clojure.lang.IDeref :cljs IDeref)
       (#?(:clj deref :cljs -deref) [_this]
         (if-not (nil? cached)
           cached
           (let [x (f)]
             (when-not (nil? x)
               (set! cached x))
             x)))
       #?(:clj clojure.lang.IFn :cljs IFn)
       (#?(:clj invoke :cljs -invoke) [this]
         (@this))
       (#?(:clj invoke :cljs -invoke) [this a]
         (@this a))
       (#?(:clj invoke :cljs -invoke) [this a b]
         (@this a b))
       (#?(:clj invoke :cljs -invoke) [this a b c]
         (@this a b c))
       (#?(:clj invoke :cljs -invoke) [this a b c d]
         (@this a b c d))
       (#?(:clj invoke :cljs -invoke) [this a b c d e]
         (@this a b c d e))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f]
         (@this a b c d e f))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g]
         (@this a b c d e f g))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h]
         (@this a b c d e f g h))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i]
         (@this a b c d e f g h i))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i j]
         (@this a b c d e f g h i j))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i j k]
         (@this a b c d e f g h i j k))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i j k l]
         (@this a b c d e f g h i j k l))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i j k l m]
         (@this a b c d e f g h i j k l m))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i j k l m n]
         (@this a b c d e f g h i j k l m n))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i j k l m n o]
         (@this a b c d e f g h i j k l m n o))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i j k l m n o p]
         (@this a b c d e f g h i j k l m n o p))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i j k l m n o p q]
         (@this a b c d e f g h i j k l m n o p q))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i j k l m n o p q r]
         (@this a b c d e f g h i j k l m n o p q r))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i j k l m n o p q r s]
         (@this a b c d e f g h i j k l m n o p q r s))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i j k l m n o p q r s t]
         (@this a b c d e f g h i j k l m n o p q r s t))
       (#?(:clj invoke :cljs -invoke) [this a b c d e f g h i j k l m n o p q r s t rest]
         (apply @this a b c d e f g h i j k l m n o p q r s t rest))
       #?(:clj
          (applyTo [this args]
                   (apply @this args))))))

(defmacro ? [& {:keys [cljs clj]}]
  (if (contains? &env '&env)
    `(if (:ns ~'&env) ~cljs ~clj)
    (if #?(:clj (:ns &env) :cljs true)
      cljs
      clj)))

#?(:clj
   (def resolve-at-compile-time? (= "true"
                                    (System/getProperty "borkdude.dynaload.aot"))))

#?(:clj (defonce ^:private dynalock (Object.)))

#?(:clj
   (defmacro ^:private locking2
     "Executes exprs in an implicit do, while holding the monitor of x.
  Will release the monitor of x in all circumstances."
     {:added "1.0"}
     [x & body]
     #?(:bb
        `(locking ~x ~body)
        :default
        `(let [lockee# ~x]
           (try
             (let [locklocal# lockee#]
               (monitor-enter locklocal#)
               (try
                 ~@body
                 (finally
                   (monitor-exit locklocal#)))))))))

#?(:clj (def resolve*
          (if resolve-at-compile-time?
            (constantly nil)
            (fn [sym]
              (let [ns (namespace sym)]
                (assert ns)
                (try (locking2 dynalock
                               (require (symbol ns)))
                     (catch Exception _ nil))
                (resolve sym))))))

(defmacro dynaload
  ([s] `(dynaload ~s {}))
  ([[_quote s] opts]
   #?(:org.babashka/nbb
      `(let [d# (delay (or (resolve '~s)
                           (if-let [e# (find ~opts :default)]
                             (val e#)
                             (throw
                              (ex-info
                               (str "Var " '~s " does not exist, "
                                    (namespace '~s) " never required")
                               {})))))]
         (fn
           ([]
            (@d#))
           ([a0]
            (@d# a0))
           ([a0 a1]
            (@d# a0 a1))
           ([a0 a1 a2]
            (@d# a0 a1 a2))
           ([a0 a1 a2 a3]
            (@d# a0 a1 a2 a3))
           ([a0 a1 a2 a3 a4]
            (@d# a0 a1 a2 a3 a4))
           ([a0 a1 a2 a3 a4 & args]
            (apply @d# a0 a1 a2 a3 a4 args))))
      :default
      #_{:clj-kondo/ignore[:redundant-let]}
      (let [#?@(:clj [resolved-at-compile-time (when resolve-at-compile-time?
                                                 (resolve s))])]
        `(->LazyVar
          (fn []
            (? :clj
               (if-let [v# (or #?(:clj ~resolved-at-compile-time)
                               (resolve* '~s))]
                 v#
                 (if-let [e# (find ~opts :default)]
                   (val e#)
                   (throw
                    (ex-info
                     (str "Var " '~s " does not exist, "
                          (namespace '~s) " never required")
                     {}))))
               :cljs
               (if (cljs.core/exists? ~s)
                 ~(vary-meta s assoc :cljs.analyzer/no-resolve true)
                 (if-let [e# (find ~opts :default)]
                   (val e#)
                   (throw
                    (js/Error.
                     (str "Var " '~s " does not exist, "
                          (namespace '~s) " never required")))))))
          nil)))))
(ns malli.sci
  (:require [borkdude.dynaload :as dynaload]))

(defn evaluator [options fail!]
  #?(:bb      (fn []
                (fn [form]
                  (load-string (str "(ns user (:require [malli.core :as m]))\n" form))))
     :default (let [eval-string* (dynaload/dynaload 'sci.core/eval-string* {:default nil})
                    init (dynaload/dynaload 'sci.core/init {:default nil})
                    fork (dynaload/dynaload 'sci.core/fork {:default nil})]
                (fn [] (if (and @eval-string* @init @fork)
                         (let [ctx (init options)]
                           (eval-string* ctx "(alias 'm 'malli.core)")
                           (fn eval [s]
                             (eval-string* (fork ctx) (str s))))
                         fail!)))))
(ns malli.registry
  (:refer-clojure :exclude [type])
  #?(:clj (:import (java.util HashMap Map))))

#?(:cljs (goog-define mode "default")
   :clj  (def mode (or (System/getProperty "malli.registry/mode") "default")))

#?(:cljs (goog-define type "default")
   :clj  (def type (or (System/getProperty "malli.registry/type") "default")))

(defprotocol Registry
  (-schema [this type] "returns the schema from a registry")
  (-schemas [this] "returns all schemas from a registry"))

(defn registry? [x] (#?(:clj instance?, :cljs implements?) malli.registry.Registry x))

(defn fast-registry [m]
  (let [fm #?(:clj (doto (HashMap. 1024 0.25) (.putAll ^Map m)), :cljs m)]
    (reify
      Registry
      (-schema [_ type] (.get fm type))
      (-schemas [_] m))))

(defn simple-registry [m]
  (reify
    Registry
    (-schema [_ type] (m type))
    (-schemas [_] m)))

(defn registry [?registry]
  (cond (nil? ?registry) nil
        (registry? ?registry) ?registry
        (map? ?registry) (simple-registry ?registry)
        (satisfies? Registry ?registry) ?registry))

;;
;; custom
;;

(def ^:private registry* (atom (simple-registry {})))

(defn set-default-registry! [?registry]
  (if-not #?(:cljs (identical? mode "strict")
             :default (= mode "strict"))
    (reset! registry* (registry ?registry))
    (throw (ex-info "can't set default registry, invalid mode" {:mode mode, :type type}))))

(defn ^:no-doc custom-default-registry []
  (reify
    Registry
    (-schema [_ type] (-schema @registry* type))
    (-schemas [_] (-schemas @registry*))))

(defn composite-registry [& ?registries]
  (let [registries (mapv registry ?registries)]
    (reify
      Registry
      (-schema [_ type] (some #(-schema % type) registries))
      (-schemas [_] (reduce merge (map -schemas (reverse registries)))))))

(defn mutable-registry [db]
  (reify
    Registry
    (-schema [_ type] (-schema (registry @db) type))
    (-schemas [_] (-schemas (registry @db)))))

(defn var-registry []
  (reify
    Registry
    (-schema [_ type] (if (var? type) @type))
    (-schemas [_])))

(def ^:dynamic *registry* {})

(defn dynamic-registry []
  (reify
    Registry
    (-schema [_ type] (-schema (registry *registry*) type))
    (-schemas [_] (-schemas (registry *registry*)))))

(defn lazy-registry [default-registry provider]
  (let [cache* (atom {})
        registry* (atom default-registry)]
    (reset!
     registry*
     (composite-registry
      default-registry
      (reify
        Registry
        (-schema [_ name]
          (or (@cache* name)
              (when-let [schema (provider name @registry*)]
                (swap! cache* assoc name schema)
                schema)))
        (-schemas [_] @cache*))))))

(defn schema
  "finds a schema from a registry"
  [registry type]
  (-schema registry type))

(defn schemas
  "finds all schemas from a registry"
  [registry]
  (-schemas registry))
(ns malli.impl.util
  #?(:clj (:import #?(:bb  (clojure.lang MapEntry)
                      :clj (clojure.lang MapEntry LazilyPersistentVector))
                   (java.util.concurrent TimeoutException TimeUnit FutureTask))))

(def ^:const +max-size+ #?(:clj Long/MAX_VALUE, :cljs (.-MAX_VALUE js/Number)))

(defn -entry [k v] #?(:clj (MapEntry. k v), :cljs (MapEntry. k v nil)))

(defn -invalid? [x] #?(:clj (identical? x :malli.core/invalid), :cljs (keyword-identical? x :malli.core/invalid)))
(defn -map-valid [f v] (if (-invalid? v) v (f v)))
(defn -map-invalid [f v] (if (-invalid? v) (f v) v))
(defn -reduce-kv-valid [f init coll] (reduce-kv (comp #(-map-invalid reduced %) f) init coll))

(defn -last [x] (if (vector? x) (peek x) (last x)))
(defn -some [pred coll] (reduce (fn [ret x] (if (pred x) (reduced true) ret)) nil coll))
(defn -merge [m1 m2] (if m1 (persistent! (reduce-kv assoc! (transient m1) m2)) m2))

(defn -error
  ([path in schema value] {:path path, :in in, :schema schema, :value value})
  ([path in schema value type] {:path path, :in in, :schema schema, :value value, :type type}))

(defn -vmap
  ([os] (-vmap identity os))
  ([f os] #?(:clj  (let [c (count os)]
                     (if-not (zero? c)
                       (let [oa (object-array c), iter (.iterator ^Iterable os)]
                         (loop [n 0] (when (.hasNext iter) (aset oa n (f (.next iter))) (recur (unchecked-inc n))))
                         #?(:bb  (vec oa)
                            :clj (LazilyPersistentVector/createOwning oa))) []))
             :cljs (into [] (map f) os))))

#?(:clj
   (defn ^:no-doc -run [^Runnable f ms]
     (let [task (FutureTask. f), t (Thread. task)]
       (try
         (.start t) (.get task ms TimeUnit/MILLISECONDS)
         (catch TimeoutException _ (.cancel task true) ::timeout)
         (catch Exception e (.cancel task true) (throw e))))))

#?(:clj
   (defmacro -combine-n
     [c n xs]
     (let [syms (repeatedly n gensym)
           g (gensym "preds__")
           bs (interleave syms (map (fn [n] `(nth ~g ~n)) (range n)))
           arg (gensym "arg__")
           body `(~c ~@(map (fn [sym] `(~sym ~arg)) syms))]
       `(let [~g (-vmap ~xs) ~@bs]
          (fn [~arg] ~body)))))

#?(:clj
   (defmacro -pred-composer
     [c n]
     (let [preds (gensym "preds__")
           f (gensym "f__")
           cases (mapcat (fn [i] [i `(-combine-n ~c ~i ~preds)]) (range 2 (inc n)))
           else `(let [p# (~f (take ~n ~preds)) q# (~f (drop ~n ~preds))]
                   (fn [x#] (~c (p# x#) (q# x#))))]
       `(fn ~f [~preds]
          (case (count ~preds)
            0 (constantly (boolean (~c)))
            1 (first ~preds)
            ~@cases
            ~else)))))

(def ^{:arglists '([[& preds]])} -every-pred
  #?(:clj  (-pred-composer and 16)
     :cljs (fn [preds] (fn [m] (boolean (reduce #(or (%2 m) (reduced false)) true preds))))))

(def ^{:arglists '([[& preds]])} -some-pred
  #?(:clj  (-pred-composer or 16)
     :cljs (fn [preds] (fn [x] (boolean (some #(% x) preds))))))
(ns malli.impl.regex
  "Regular expressions of sequences implementation namespace.

  The implementation is very similar to Packrat or GLL parser combinators.
  The parsing functions need to be written in CPS to support backtracking
  inside :*, :+ and :repeat. They also need to be trampolined because the
  (manually) CPS-converted code (for :*, :+ and :repeat) has to use tail
  calls instead of loops and Clojure does not have TCO.

  Because backtracking is used we need to memoize (parsing function, seq
  position, register stack) triples to avoid exponential behaviour. Discarding
  the memoization cache after traversing an input seq also requires trampolining.
  Because regular expressions don't use (nontail) recursion by definition, finding
  a memoization entry just means the parser already went 'here' and ultimately
  failed; much simpler than the graph-structured stacks of GLL. And the register
  stack is only there for and used by :repeat.

  NOTE: For the memoization to work correctly, every node in the schema tree
  must get its own validation/explanation/... function instance. So even every
  `(malli.impl.regex/cat)` call must return a new fn instance although it does not
  close over anything.

  https://epsil.github.io/gll/ is a nice explanation of GLL parser combinators
  and has links to papers etc. It also inspired Instaparse, which Engelberg
  had a presentation about at Clojure/West 2014.

  Despite the CPS and memoization, this implementation looks more like normal
  Clojure code than the 'Pike VM' in Seqexp. Hopefully JITs also see it that
  way and compile decent machine code for it. It is also much easier to extend
  for actual parsing (e.g. encode, decode [and parse?]) instead of just
  recognition for `validate`.

  For a more detailed explanation of this namespace see also
  https://www.metosin.fi/blog/malli-regex-schemas/."

  (:refer-clojure :exclude [+ * repeat cat])
  (:require [malli.impl.util :as miu])
  #?(:bb  (:import [java.util ArrayDeque])
     :clj (:import [java.util ArrayDeque]
                   [clojure.lang Util Murmur3]
                   [java.lang.reflect Array])))

;;;; # Driver Protocols

(defprotocol ^:private Driver
  (succeed! [self])
  (succeeded? [self])
  (pop-thunk! [self]))

(defprotocol ^:private IValidationDriver
  (noncaching-park-validator! [driver validator regs pos coll k])
  (park-validator! [driver validator regs pos coll k]))

(defprotocol ^:private IExplanationDriver
  (noncaching-park-explainer! [driver explainer regs pos coll k])
  (park-explainer! [driver explainer regs pos coll k])
  (value-path [self pos])
  (fail! [self pos errors*])
  (latest-errors [self]))

(defprotocol ^:private IParseDriver
  (noncaching-park-transformer! [driver transformer regs coll* pos coll k])
  (park-transformer! [driver transformer regs coll* pos coll k])
  (succeed-with! [self v])
  (success-result [self]))

;;;; # Primitives

;;;; ## Seq Item

(defn item-validator [valid?]
  (fn [_ _ pos coll k]
    (when (and (seq coll) (valid? (first coll)))
      (k (inc pos) (rest coll)))))

(defn item-explainer [path schema schema-explainer]
  (fn [driver _ pos coll k]
    (let [in (value-path driver pos)]
      (if (seq coll)
        (let [errors (schema-explainer (first coll) in [])]
          (if (seq errors)
            (fail! driver pos errors)
            (k (inc pos) (rest coll))))
        (fail! driver pos [(miu/-error path in schema nil :malli.core/end-of-input)])))))

(defn item-parser [parse]
  (fn [_ _ pos coll k]
    (when (seq coll)
      (let [v (parse (first coll))]
        (when-not (= v :malli.core/invalid)
          (k v (inc pos) (rest coll)))))))

(defn item-unparser [unparse] (fn [v] (miu/-map-valid vector (unparse v))))

(defn item-encoder [valid? encode]
  (fn [_ _ coll* pos coll k]
    (when (seq coll)
      (let [v (first coll)]
        (when (valid? v)
          (k (conj coll* (encode v)) (inc pos) (rest coll)))))))

(defn item-decoder [decode valid?]
  (fn [_ _ coll* pos coll k]
    (when (seq coll)
      (let [v (decode (first coll))]
        (when (valid? v)
          (k (conj coll* v) (inc pos) (rest coll)))))))

(defn item-transformer [method validator t]
  (case method
    :encode (item-encoder validator t)
    :decode (item-decoder t validator)))

;;;; ## End of Seq

(defn end-validator [] (fn [_ _ pos coll k] (when (empty? coll) (k pos coll))))

(defn end-explainer [schema path]
  (fn [driver _ pos coll k]
    (if (empty? coll)
      (k pos coll)
      (fail! driver pos (list (miu/-error path (value-path driver pos) schema (first coll) :malli.core/input-remaining))))))

(defn end-parser [] (fn [_ _ pos coll k] (when (empty? coll) (k nil pos coll))))

(defn end-transformer [] (fn [_ _ coll* pos coll k] (when (empty? coll) (k coll* pos coll))))

;;;; ## Unit

(defn pure-parser [v] (fn [_ _ pos coll k] (k v pos coll)))

(defn pure-unparser [_] [])

;;;; # Combinators

;;;; ## Functor

(defn fmap-parser [f p]
  (fn [driver regs pos coll k]
    (p driver regs pos coll (fn [v pos coll] (k (f v) pos coll)))))

;;;; ## Catenation

(defn- entry->regex [?kr] (if (vector? ?kr) (get ?kr 1) ?kr))

(defn cat-validator
  ([] (fn [_ _ pos coll k] (k pos coll)))
  ([?kr & ?krs]
   (reduce (fn [acc ?kr]
             (let [r* (entry->regex ?kr)]
               (fn [driver regs pos coll k]
                 (acc driver regs pos coll (fn [pos coll] (r* driver regs pos coll k))))))
           (entry->regex ?kr) ?krs)))

(defn cat-explainer
  ([] (fn [_ _ pos coll k] (k pos coll)))
  ([?kr & ?krs]
   (reduce (fn [acc ?kr]
             (let [r* (entry->regex ?kr)]
               (fn [driver regs pos coll k]
                 (acc driver regs pos coll (fn [pos coll] (r* driver regs pos coll k))))))
           (entry->regex ?kr) ?krs)))

(defn cat-parser
  ([] (fn [_ _ pos coll k] (k [] pos coll)))
  ([r & rs]
   (let [sp (reduce (fn [acc r]
                      (fn [driver regs coll* pos coll k]
                        (r driver regs pos coll
                           (fn [v pos coll] (acc driver regs (conj coll* v) pos coll k)))))
                    (fn [_ _ coll* pos coll k] (k coll* pos coll))
                    (reverse (cons r rs)))]
     (fn [driver regs pos coll k] (sp driver regs [] pos coll k)))))

;; we need to pass in the malli.core/tags function as an arg to avoid a cyclic reference
(defn catn-parser
  ([tags] (fn [_ _ pos coll k] (k (tags {}) pos coll)))
  ([tags kr & krs]
   (let [sp (reduce (fn [acc [tag r]]
                      (fn [driver regs m pos coll k]
                        (r driver regs pos coll
                           (fn [v pos coll] (acc driver regs (assoc m tag v) pos coll k)))))
                    (fn [_ _ m pos coll k] (k (tags m) pos coll))
                    (reverse (cons kr krs)))]
     (fn [driver regs pos coll k] (sp driver regs {} pos coll k)))))

(defn cat-unparser [& unparsers]
  (let [unparsers (vec unparsers)]
    (fn [tup]
      (if (and (vector? tup) (= (count tup) (count unparsers)))
        (miu/-reduce-kv-valid (fn [coll i unparser] (miu/-map-valid #(into coll %) (unparser (get tup i))))
                              [] unparsers)
        :malli.core/invalid))))

;; cyclic ref avoidance here as well for malli.core/tags?
(defn catn-unparser [tags? & unparsers]
  (let [unparsers (apply array-map (mapcat identity unparsers))]
    (fn [m]
      (if (and (tags? m) (= (count (:values m)) (count unparsers)))
        (miu/-reduce-kv-valid (fn [coll tag unparser]
                                (if-some [kv (find (:values m) tag)]
                                  (miu/-map-valid #(into coll %) (unparser (val kv)))
                                  :malli.core/invalid))
                              ;; `m` is in hash order, so have to iterate over `unparsers` to restore seq order:
                              [] unparsers)
        :malli.core/invalid))))

(defn cat-transformer
  ([] (fn [_ _ coll* pos coll k] (k coll* pos coll)))
  ([?kr & ?krs]
   (reduce (fn [acc ?kr]
             (let [r (entry->regex ?kr)]
               (fn [driver regs coll* pos coll k]
                 (acc driver regs coll* pos coll (fn [coll* pos coll] (r driver regs coll* pos coll k))))))
           (entry->regex ?kr) ?krs)))

;;;; ## Alternation

(defn alt-validator [?kr & ?krs]
  (reduce (fn [r ?kr]
            (let [r* (entry->regex ?kr)]
              (fn [driver regs pos coll k]
                (park-validator! driver r* regs pos coll k) ; remember fallback
                (park-validator! driver r regs pos coll k))))
          (entry->regex ?kr) ?krs))

(defn alt-explainer [?kr & ?krs]
  (reduce (fn [r ?kr]
            (let [r* (entry->regex ?kr)]
              (fn [driver regs pos coll k]
                (park-explainer! driver r* regs pos coll k) ; remember fallback
                (park-explainer! driver r regs pos coll k))))
          (entry->regex ?kr) ?krs))

(defn alt-parser [& rs]
  (reduce (fn [r r*]
            (fn [driver regs pos coll k]
              (park-validator! driver r* regs pos coll k) ; remember fallback
              (park-validator! driver r regs pos coll k)))
          rs))

;; cyclic ref avoidance for malli.core/tag
(defn altn-parser [tag kr & krs]
  (reduce (fn [r [t r*]]
            (let [r* (fmap-parser (fn [v] (tag t v)) r*)]
              (fn [driver regs pos coll k]
                (park-validator! driver r* regs pos coll k) ; remember fallback
                (park-validator! driver r regs pos coll k))))
          (let [[t r] kr]
            (fmap-parser (fn [v] (tag t v)) r))
          krs))

(defn alt-unparser [& unparsers]
  (fn [x]
    (reduce (fn [_ unparse] (miu/-map-valid reduced (unparse x)))
            :malli.core/invalid unparsers)))

;; cyclic ref avoidance for malli.core/tag?
(defn altn-unparser [tag? & unparsers]
  (let [unparsers (into {} unparsers)]
    (fn [x]
      (if (tag? x)
        (if-some [kv (find unparsers (:key x))]
          ((val kv) (:value x))
          :malli.core/invalid)
        :malli.core/invalid))))

(defn alt-transformer [?kr & ?krs]
  (reduce (fn [r ?kr]
            (let [r* (entry->regex ?kr)]
              (fn [driver regs coll* pos coll k]
                (park-transformer! driver r* regs coll* pos coll k) ; remember fallback
                (park-transformer! driver r regs coll* pos coll k))))
          (entry->regex ?kr) ?krs))

;;;; ## Option

(defn ?-validator [p] (alt-validator p (cat-validator)))
(defn ?-explainer [p] (alt-explainer p (cat-explainer)))
(defn ?-parser [p] (alt-parser p (pure-parser nil)))
(defn ?-unparser [p] (alt-unparser p pure-unparser))
(defn ?-transformer [p] (alt-transformer p (cat-transformer)))

;;;; ## Kleene Star

(defn *-validator [p]
  (let [*p-epsilon (cat-validator)]
    (fn *p [driver regs pos coll k]
      (park-validator! driver *p-epsilon regs pos coll k) ; remember fallback
      (p driver regs pos coll (fn [pos coll] (park-validator! driver *p regs pos coll k)))))) ; TCO

(defn *-explainer [p]
  (let [*p-epsilon (cat-explainer)]
    (fn *p [driver regs pos coll k]
      (park-explainer! driver *p-epsilon regs pos coll k) ; remember fallback
      (p driver regs pos coll (fn [pos coll] (park-explainer! driver *p regs pos coll k)))))) ; TCO

(defn *-parser [p]
  (let [*p-epsilon (fn [_ _ coll* pos coll k] (k coll* pos coll))] ; TCO
    (fn *p
      ([driver regs pos coll k] (*p driver regs [] pos coll k))
      ([driver regs coll* pos coll k]
       (park-transformer! driver *p-epsilon regs coll* pos coll k) ; remember fallback
       (p driver regs pos coll
          (fn [v pos coll] (park-transformer! driver *p regs (conj coll* v) pos coll k))))))) ; TCO

(defn *-unparser [up]
  (fn [v]
    (reduce (fn [acc v]
              (let [result (up v)]
                (if (miu/-invalid? result)
                  (reduced result)
                  (into acc result))))
            [] v)))

(defn *-transformer [p]
  (let [*p-epsilon (cat-transformer)]
    (fn *p [driver regs coll* pos coll k]
      (park-transformer! driver *p-epsilon regs coll* pos coll k) ; remember fallback
      (p driver regs coll* pos coll
         (fn [coll* pos coll] (park-transformer! driver *p regs coll* pos coll k)))))) ; TCO

;;;; ## Non-Kleene Plus

(defn +-validator [p] (cat-validator p (*-validator p)))
(defn +-explainer [p] (cat-explainer p (*-explainer p)))
(defn +-parser [p] (fmap-parser (fn [[v vs]] (into [v] vs)) (cat-parser p (*-parser p))))

(defn +-unparser [up]
  (let [up* (*-unparser up)]
    (fn [x]
      (if (and (vector? x) (<= 1 (count x)))
        (up* x)
        :malli.core/invalid))))

(defn +-transformer [p] (cat-transformer p (*-transformer p)))

;;;; ## Repeat

;; eagerly repeat a child until either:
;; - the child consumes no elements
;;   - then bail to check for remaining elements
;; - we run out of repetitions via :max
;;   - then bail to check for remaining elements
;; - we have repeated at least :min times and the coll is empty
;;   - success case

(defn repeat-validator [min max p]
  (let [rep-epsilon (cat-validator)]
    (letfn [(compulsories [driver regs pos coll k]
              (if (< (peek regs) min)
                (p driver regs pos coll
                   (fn [pos coll]
                     (noncaching-park-validator! driver
                                                 (fn [driver stack pos coll k]
                                                   (compulsories driver (conj (pop stack) (inc (peek stack))) pos coll k))
                                                 regs pos coll k))) ; TCO
                (optionals driver regs pos coll k)))
            (optionals [driver regs pos coll k]
              (if (and (< (peek regs) max)
                       (<= (peek regs) pos)
                       (seq coll))
                (do
                  (park-validator! driver rep-epsilon regs pos coll k) ; remember fallback
                  (p driver regs pos coll
                     (fn [pos coll]
                       (park-validator! driver
                                        (fn [driver regs pos coll k]
                                          (optionals driver (conj (pop regs) (inc (peek regs))) pos coll k))
                                        regs pos coll k)))) ; TCO
                (k pos coll)))]
      (fn [driver regs pos coll k] (compulsories driver (conj regs 0) pos coll k)))))

(defn repeat-explainer [min max p]
  (let [rep-epsilon (cat-explainer)]
    (letfn [(compulsories [driver regs pos coll k]
              (if (< (peek regs) min)
                (p driver regs pos coll
                   (fn [pos coll]
                     (noncaching-park-explainer! driver
                                                 (fn [driver regs pos coll k]
                                                   (compulsories driver (conj (pop regs) (inc (peek regs))) pos coll k))
                                                 regs pos coll k))) ; TCO
                (optionals driver regs pos coll k)))
            (optionals [driver regs pos coll k]
              (if (and (< (peek regs) max)
                       (<= (peek regs) pos)
                       (seq coll))
                (do
                  (park-explainer! driver rep-epsilon regs pos coll k) ; remember fallback
                  (p driver regs pos coll
                     (fn [pos coll]
                       (park-explainer! driver
                                        (fn [driver regs pos coll k]
                                          (optionals driver (conj (pop regs) (inc (peek regs))) pos coll k))
                                        regs pos coll k)))) ; TCO
                (k pos coll)))]
      (fn [driver regs pos coll k] (compulsories driver (conj regs 0) pos coll k)))))

(defn repeat-parser [min max p]
  (let [rep-epsilon (fn [_ _ coll* pos coll k] (k coll* pos coll))]
    (letfn [(compulsories [driver regs coll* pos coll k]
              (if (< (peek regs) min)
                (p driver regs pos coll
                   (fn [v pos coll]
                     (noncaching-park-transformer! driver
                                                   (fn [driver regs coll* pos coll k]
                                                     (compulsories driver (conj (pop regs) (inc (peek regs))) (conj coll* v) pos coll k))
                                                   regs coll* pos coll k))) ; TCO
                (optionals driver regs coll* pos coll k)))
            (optionals [driver regs coll* pos coll k]
              (if (and (< (peek regs) max)
                       (<= (peek regs) pos)
                       (seq coll))
                (do
                  (park-transformer! driver rep-epsilon regs coll* pos coll k) ; remember fallback
                  (p driver regs pos coll
                     (fn [v pos coll]
                       (park-transformer!
                        driver
                        (fn [driver regs coll* pos coll k]
                          (optionals driver (conj (pop regs) (inc (peek regs))) (conj coll* v) pos coll k))
                        regs coll* pos coll k)))) ; TCO
                (k coll* pos coll)))]
      (fn [driver regs pos coll k] (compulsories driver (conj regs 0) [] pos coll k)))))

(defn repeat-unparser [min max up]
  (let [up* (*-unparser up)]
    (fn [v]
      (if (and (vector? v) (<= min (count v) max))
        (up* v)
        :malli.core/invalid))))

(defn repeat-transformer [min max p]
  (let [rep-epsilon (cat-transformer)]
    (letfn [(compulsories [driver regs coll* pos coll k]
              (if (< (peek regs) min)
                (p driver regs coll* pos coll
                   (fn [coll* pos coll]
                     (noncaching-park-transformer! driver
                                                   (fn [driver regs coll* pos coll k]
                                                     (compulsories driver (conj (pop regs) (inc (peek regs))) coll* pos coll k))
                                                   regs coll* pos coll k))) ; TCO
                (optionals driver regs coll* pos coll k)))
            (optionals [driver regs coll* pos coll k]
              (if (and (< (peek regs) max)
                       (<= (peek regs) pos)
                       (seq coll))
                (do
                  (park-transformer! driver rep-epsilon regs coll* pos coll k) ; remember fallback
                  (p driver regs coll* pos coll
                     (fn [coll* pos coll]
                       (park-transformer! driver
                                          (fn [driver regs coll* pos coll k]
                                            (optionals driver (conj (pop regs) (inc (peek regs))) coll* pos coll k))
                                          regs coll* pos coll k)))) ; TCO
                (k coll* pos coll)))]
      (fn [driver regs coll* pos coll k] (compulsories driver (conj regs 0) coll* pos coll k)))))

;;;; # Shared Drivers

(defn- make-stack [] #?(:clj (ArrayDeque.), :cljs #js []))

(defn- empty-stack? [^ArrayDeque stack] #?(:clj (.isEmpty stack), :cljs (zero? (alength stack))))

(defprotocol ^:private ICache
  (ensure-cached! [cache f pos regs]))

(deftype ^:private CacheEntry [^long hash f ^long pos regs])

#?(:clj (set! *unchecked-math* true))

;; Custom hash set so that Cljs Malli users can have decent perf without having to to set up Closure ES6 Set polyfill.
;; Uses quadratic probing with power-of-two sizes and triangular numbers, what a nice trick!
(deftype Cache
  #?(:clj  [^:unsynchronized-mutable ^"[Ljava.lang.Object;" values, ^:unsynchronized-mutable ^long size]
     :cljs [^:mutable values, ^:mutable size])
  ICache
  (ensure-cached! [_ f pos regs]
    (when (> (unchecked-inc size) (bit-shift-right (alength values) 1)) ; potential new load factor > 0.5
      ;; Rehash:
      (let [capacity* (bit-shift-left (alength values) 1)
            ^objects values* #?(:bb   (object-array capacity*)
                                :clj (Array/newInstance Object capacity*)
                                :cljs (object-array capacity*))
            max-index (unchecked-dec capacity*)]

        (let [len (alength values)]
          (loop [i 0]
            (when (< i len)
              (when-some [^CacheEntry v (aget values i)]
                (loop [i* (bit-and (.-hash v) max-index)
                       collisions 0]
                  (if (aget values* i*)
                    (let [collisions (unchecked-inc collisions)]
                      (recur
                       (bit-and (unchecked-add i* collisions) max-index)
                       collisions))
                    (aset values* i* v))))
              (recur (unchecked-inc i)))))

        (set! values values*)))

    (let [capacity (alength values)
          max-index (unchecked-dec capacity)
          #?@(:clj [pos (.longValue ^Long pos)])
          ;; Unfortunately `hash-combine` hashes its second argument on clj and neither argument on cljs:
          h #?(:bb   (-> (hash f) (hash-combine pos) (hash-combine regs))
               :clj (-> (.hashCode ^Object f) (Util/hashCombine (Murmur3/hashLong pos)) (Util/hashCombine (Util/hash regs)))
               :cljs (-> (hash f) (hash-combine (hash pos)) (hash-combine (hash regs))))]
      (loop [i (bit-and h max-index), collisions 0]
        (if-some [^CacheEntry entry (aget values i)]
          (or (and (= (.-hash entry) h)
                   (= (.-f entry) f)
                   (= (.-pos entry) pos)
                   (= (.-regs entry) regs))
              (let [collisions (unchecked-inc collisions)]
                (recur (bit-and (unchecked-add i collisions) max-index) ; i = (i + collisions) % capacity
                       collisions)))
          (do
            (aset values i (CacheEntry. h f pos regs))
            (set! size (unchecked-inc size))
            false))))))

(defn- make-cache [] (Cache. (object-array 2) 0))

#?(:clj (set! *unchecked-math* false))

(deftype ^:private CheckDriver
  #?(:clj  [^:unsynchronized-mutable ^boolean success, ^ArrayDeque stack, cache]
     :cljs [^:mutable success, stack, cache])

  Driver
  (succeed! [_] (set! success (boolean true)))
  (succeeded? [_] success)
  (pop-thunk! [_] (when-not (empty-stack? stack) (.pop stack)))

  IValidationDriver
  (noncaching-park-validator! [self validator regs pos coll k] (.push stack #(validator self regs pos coll k)))
  (park-validator! [self validator regs pos coll k]
    (when-not (ensure-cached! cache validator pos regs)
      (noncaching-park-validator! self validator regs pos coll k))))

(deftype ^:private ParseDriver
  #?(:clj  [^:unsynchronized-mutable ^boolean success, ^ArrayDeque stack, cache
            ^:unsynchronized-mutable result]
     :cljs [^:mutable success, stack, cache, ^:mutable result])

  Driver
  (succeed! [_] (set! success (boolean true)))
  (succeeded? [_] success)
  (pop-thunk! [_] (when-not (empty-stack? stack) (.pop stack)))

  IValidationDriver
  (noncaching-park-validator! [self validator regs pos coll k] (.push stack #(validator self regs pos coll k)))
  (park-validator! [self validator regs pos coll k]
    (when-not (ensure-cached! cache validator pos regs)
      (noncaching-park-validator! self validator regs pos coll k)))

  IParseDriver
  (noncaching-park-transformer! [driver transformer regs coll* pos coll k]
    (.push stack #(transformer driver regs coll* pos coll k)))
  (park-transformer! [driver transformer regs coll* pos coll k]
    (when-not (ensure-cached! cache transformer pos regs)
      (noncaching-park-transformer! driver transformer regs coll* pos coll k)))
  (succeed-with! [self v] (succeed! self) (set! result v))
  (success-result [_] result))

;;;; # Validator

(defn validator [p]
  (let [p (cat-validator p (end-validator))]
    (fn [coll]
      (and (sequential? coll)
           (let [driver (CheckDriver. false (make-stack) (make-cache))]
             (p driver () 0 coll (fn [_ _] (succeed! driver)))
             (or (succeeded? driver)
                 (loop []
                   (if-some [thunk (pop-thunk! driver)]
                     (do
                       (thunk)
                       (or (succeeded? driver) (recur)))
                     false))))))))

;;;; # Explainer

(deftype ^:private ExplanationDriver
  #?(:clj  [^:unsynchronized-mutable ^boolean success, ^ArrayDeque stack, cache
            in, ^:unsynchronized-mutable errors-max-pos, ^:unsynchronized-mutable errors]
     :cljs [^:mutable success, stack, cache, in, ^:mutable errors-max-pos, ^:mutable errors])

  Driver
  (succeed! [_] (set! success (boolean true)))
  (succeeded? [_] success)
  (pop-thunk! [_] (when-not (empty-stack? stack) (.pop stack)))

  IExplanationDriver
  (noncaching-park-explainer! [self validator regs pos coll k] (.push stack #(validator self regs pos coll k)))
  (park-explainer! [self validator regs pos coll k]
    (when-not (ensure-cached! cache validator pos regs)
      (noncaching-park-explainer! self validator regs pos coll k)))
  (value-path [_ pos] (conj in pos))
  (fail! [_ pos errors*]
    (cond
      (> pos errors-max-pos) (do
                               (set! errors-max-pos pos)
                               (set! errors errors*))
      (= pos errors-max-pos) (set! errors (into errors errors*))))
  (latest-errors [_] errors))

(defn explainer [schema path p]
  (let [p (cat-explainer p (end-explainer schema path))]
    (fn [coll in errors]
      (if (sequential? coll)
        (let [pos 0
              driver (ExplanationDriver. false (make-stack) (make-cache) in pos [])]
          (p driver () pos coll (fn [_ _] (succeed! driver)))
          (if (succeeded? driver)
            errors
            (loop []
              (if-some [thunk (pop-thunk! driver)]
                (do
                  (thunk)
                  (if (succeeded? driver) errors (recur)))
                (into errors (latest-errors driver))))))
        (conj errors (miu/-error path in schema coll :malli.core/invalid-type))))))

;;;; # Parser

(defn parser [p]
  (let [p (cat-parser p (end-parser))]
    (fn [coll]
      (if (sequential? coll)
        (let [driver (ParseDriver. false (make-stack) (make-cache) nil)]
          (p driver () 0 coll (fn [v _ _] (succeed-with! driver v)))
          (if (succeeded? driver)
            (first (success-result driver))
            (loop []
              (if-some [thunk (pop-thunk! driver)]
                (do
                  (thunk)
                  (if (succeeded? driver) (first (success-result driver)) (recur)))
                :malli.core/invalid))))
        :malli.core/invalid))))

;;;; # Transformer

(defn transformer [p]
  (let [p (cat-transformer p (end-transformer))]
    (fn [coll]
      (if (sequential? coll)
        (let [driver (ParseDriver. false (make-stack) (make-cache) nil)]
          (p driver () [] 0 coll (fn [coll* _ _] (succeed-with! driver coll*)))
          (if (succeeded? driver)
            (success-result driver)
            (loop []
              (if-some [thunk (pop-thunk! driver)]
                (do
                  (thunk)
                  (if (succeeded? driver) (success-result driver) (recur)))
                coll))))
        coll))))
(ns malli.core
  (:refer-clojure :exclude [eval type -deref deref -lookup -key assert])
  #?(:cljs (:require-macros malli.core))
  (:require #?(:clj [clojure.walk :as walk])
            [clojure.core :as c]
            [malli.impl.regex :as re]
            [malli.impl.util :as miu]
            [malli.registry :as mr]
            [malli.sci :as ms])
  #?(:clj (:import #?(:bb  (clojure.lang Associative IPersistentCollection MapEntry IPersistentVector PersistentArrayMap)
                      :clj (clojure.lang Associative IPersistentCollection MapEntry IPersistentVector LazilyPersistentVector PersistentArrayMap))
                   (java.util.concurrent.atomic AtomicReference)
                   (java.util.regex Pattern))))

(declare schema schema? into-schema into-schema? type eval default-registry
         -simple-schema -val-schema -ref-schema -schema-schema -registry
         parser unparser ast from-ast -instrument ^:private -safely-countable?)

;;
;; protocols and records
;;

(defprotocol IntoSchema
  (-type [this] "returns type of the schema")
  (-type-properties [this] "returns schema type properties")
  (-properties-schema [this options] "maybe returns :map schema describing schema properties")
  (-children-schema [this options] "maybe returns sequence schema describing schema children")
  (-into-schema [this properties children options] "creates a new schema instance"))

(defprotocol Schema
  (-validator [this] "returns a predicate function that checks if the schema is valid")
  (-explainer [this path] "returns a function of `x in acc -> maybe errors` to explain the errors for invalid values")
  (-parser [this] "return a function of `x -> parsed-x | ::m/invalid` to explain how schema is valid.")
  (-unparser [this] "return the inverse (partial) function wrt. `-parser`; `parsed-x -> x | ::m/invalid`")
  (-transformer [this transformer method options]
    "returns a function to transform the value for the given schema and method.
    Can also return nil instead of `identity` so that more no-op transforms can be elided.")
  (-walk [this walker path options] "walks the schema and it's children, ::m/walk-entry-vals, ::m/walk-refs, ::m/walk-schema-refs options effect how walking is done.")
  (-properties [this] "returns original schema properties")
  (-options [this] "returns original options")
  (-children [this] "returns schema children")
  (-parent [this] "returns the IntoSchema instance")
  (-form [this] "returns original form of the schema"))

(defprotocol AST
  (-to-ast [this options] "schema to ast")
  (-from-ast [this ast options] "ast to schema"))

(defprotocol EntryParser
  (-entry-keyset [this])
  (-entry-children [this])
  (-entry-entries [this])
  (-entry-forms [this]))

(defprotocol EntrySchema
  (-entries [this] "returns sequence of `key -val-schema` entries")
  (-entry-parser [this]))

(defprotocol Cached
  (-cache [this]))

(defprotocol LensSchema
  (-keep [this] "returns truthy if schema contributes to value path")
  (-get [this key default] "returns schema at key")
  (-set [this key value] "returns a copy with key having new value"))

(defprotocol RefSchema
  (-ref [this] "returns the reference name")
  (-deref [this] "returns the referenced schema"))

(defprotocol Walker
  (-accept [this schema path options])
  (-inner [this schema path options])
  (-outer [this schema path children options]))

(defprotocol Transformer
  (-transformer-chain [this] "returns transformer chain as a vector of maps with :name, :encoders, :decoders and :options")
  (-value-transformer [this schema method options] "returns a value transforming interceptor for the given schema and method"))

(defprotocol RegexSchema
  (-regex-op? [this] "is this a regex operator (e.g. :cat, :*...)")
  (-regex-validator [this] "returns the raw internal regex validator implementation")
  (-regex-explainer [this path] "returns the raw internal regex explainer implementation")
  (-regex-unparser [this] "returns the raw internal regex unparser implementation")
  (-regex-parser [this] "returns the raw internal regex parser implementation")
  (-regex-transformer [this transformer method options] "returns the raw internal regex transformer implementation")
  (-regex-min-max [this nested?] "returns size of the sequence as {:min min :max max}. nil max means unbounded. nested? is true when this schema is nested inside an outer regex schema."))

(defprotocol FunctionSchema
  (-function-schema? [this])
  (-function-schema-arities [this])
  (-function-info [this])
  (-instrument-f [schema props f options]))

(defprotocol DistributiveSchema
  (-distributive-schema? [this])
  (-distribute-to-children [this f options]))

(defn -ref-schema? [x] (#?(:clj instance?, :cljs implements?) malli.core.RefSchema x))
(defn -entry-parser? [x] (#?(:clj instance?, :cljs implements?) malli.core.EntryParser x))
(defn -entry-schema? [x] (#?(:clj instance?, :cljs implements?) malli.core.EntrySchema x))
(defn -cached? [x] (#?(:clj instance?, :cljs implements?) malli.core.Cached x))
(defn -ast? [x] (#?(:clj instance?, :cljs implements?) malli.core.AST x))
(defn -transformer? [x] (#?(:clj instance?, :cljs implements?) malli.core.Transformer x))

(extend-type #?(:clj Object, :cljs default)
  FunctionSchema
  (-function-schema? [_] false)
  (-function-info [_])
  (-function-schema-arities [_])
  (-instrument-f [_ _ _ _])

  DistributiveSchema
  (-distributive-schema? [_] false)
  (-distribute-to-children [this _ _]
    (throw (ex-info "Not distributive" {:schema this})))

  RegexSchema
  (-regex-op? [_] false)

  (-regex-validator [this]
    (if (-ref-schema? this)
      (-regex-validator (-deref this))
      (re/item-validator (-validator this))))

  (-regex-explainer [this path]
    (if (-ref-schema? this)
      (-regex-explainer (-deref this) path)
      (re/item-explainer path this (-explainer this path))))

  (-regex-parser [this]
    (if (-ref-schema? this)
      (-regex-parser (-deref this))
      (re/item-parser (parser this))))

  (-regex-unparser [this]
    (if (-ref-schema? this)
      (-regex-unparser (-deref this))
      (re/item-unparser (unparser this))))

  (-regex-transformer [this transformer method options]
    (if (-ref-schema? this)
      (-regex-transformer (-deref this) transformer method options)
      (re/item-transformer method (-validator this) (or (-transformer this transformer method options) identity))))

  (-regex-min-max [_ _] {:min 1, :max 1}))

#?(:clj (defmethod print-method ::into-schema [v ^java.io.Writer w] (.write w (str "#IntoSchema {:type " (pr-str (-type ^IntoSchema v)) "}"))))
#?(:clj (defmethod print-method ::schema [v ^java.io.Writer w] (.write w (pr-str (-form ^Schema v)))))
#?(:cljs (defn- pr-writer-into-schema [obj writer opts]
           (-write writer "#IntoSchema ")
           (-pr-writer {:type (-type ^IntoSchema obj)} writer opts)))
#?(:cljs (defn- pr-writer-schema [obj writer opts]
           (-pr-writer (-form ^Schema obj) writer opts)))

(defrecord Tag [key value])

(defn tag
  "A tagged value, used eg. for results of `parse` for `:orn` schemas."
  [key value] (->Tag key value))

(defn tag?
  "Is this a value constructed with `tag`?"
  [x] (instance? Tag x))

(defrecord Tags [values])

(defn tags
  "A collection of tagged values. `values` should be a map from tag to value.
   Used eg. for results of `parse` for `:catn` schemas."
  [values] (->Tags values))

(defn tags?
  "Is this a value constructed with `tags`?"
  [x] (instance? Tags x))

;;
;; impl
;;

(defn -deprecated! [x] (println "DEPRECATED:" x))

(defn -exception [type data] (ex-info (str type) {:type type, :message type, :data data}))

(defn -fail!
  ([type] (-fail! type nil))
  ([type data] (throw (-exception type data))))

(defn -safe-pred [f] #(try (boolean (f %)) (catch #?(:clj Exception, :cljs js/Error) _ false)))

(defn -keyword->string [x]
  (if (keyword? x)
    (if-let [nn (namespace x)]
      (str nn "/" (name x))
      (name x))
    x))

(defn -guard [pred tf] (when tf (fn [x] (if (pred x) (tf x) x))))

(defn -unlift-keys [m prefix]
  (reduce-kv #(if (= (name prefix) (namespace %2)) (assoc %1 (keyword (name %2)) %3) %1) {} m))

(defn ^:no-doc -check-children? [] true)

(defn -check-children!
  ([type properties children props]
   (-deprecated! "use (m/-check-children! type properties children min max) instead.")
   (-check-children! type properties children (:min props) (:max props)))
  ([type properties children min max]
   (when (-check-children?)
     (when-let [size (and (or (sequential? children) (nil? children)) (count children))]
       (when (or (and min (< size ^long min)) (and max (> size ^long max)))
         (-fail! ::child-error {:type type, :properties properties, :children children, :min min, :max max}))))))

(defn -pointer [id schema options] (-into-schema (-schema-schema {:id id}) nil [schema] options))

(defn -reference? [?schema] (or (string? ?schema) (qualified-ident? ?schema) (var? ?schema)))

(defn -lazy [ref options] (-into-schema (-ref-schema {:lazy true}) nil [ref] options))

(defn -boolean-fn [x] (cond (boolean? x) (constantly x) (ifn? x) x :else (constantly false)))

(defn -infer [children]
  (loop [[[s f] & fs] [[:string string?] [:keyword keyword?] [:symbol symbol?] [:int int?] [:double float?]]]
    (if (every? f children) s (when fs (recur fs)))))

(defn -comp
  ([] identity)
  ([f] f)
  ([f g] (fn [x] (f (g x))))
  ([f g h] (fn [x] (f (g (h x)))))
  #?@(:clj  [([f1 f2 f3 f4] (fn [x] (-> x f4 f3 f2 f1)))
             ([f1 f2 f3 f4 f5] (fn [x] (-> x f5 f4 f3 f2 f1)))
             ([f1 f2 f3 f4 f5 f6] (fn [x] (-> x f6 f5 f4 f3 f2 f1)))
             ([f1 f2 f3 f4 f5 f6 f7] (fn [x] (-> x f7 f6 f5 f4 f3 f2 f1)))
             ([f1 f2 f3 f4 f5 f6 f7 f8] (fn [x] (-> x f8 f7 f6 f5 f4 f3 f2 f1)))
             ([f1 f2 f3 f4 f5 f6 f7 f8 & fs] (let [f9 (apply -comp fs)]
                                               (fn [x] (-> x f9 f8 f7 f6 f5 f4 f3 f2 f1))))]
      :cljs [([f1 f2 f3 & fs] (let [f4 (apply -comp fs)]
                                (fn [x] (-> x f4 f3 f2 f1))))]))

(defn -update [x k f] (assoc x k (f (get x k))))

(defn -equals [x y] (or (identical? x y) (= x y)))

(defn -vmap ([os] (miu/-vmap identity os)) ([f os] (miu/-vmap f os)))

(defn -memoize [f]
  (let [value #?(:clj (AtomicReference. nil), :cljs (atom nil))]
    (fn [] #?(:clj (or (.get value) (do (.set value (f)) (.get value))), :cljs (or @value (reset! value (f)))))))

(defn -group-by-arity! [infos]
  (let [aritys (atom #{})]
    (reduce
     (fn [acc {:keys [min arity] :as info}]
       (let [vararg (= :varargs arity)
             min (if (and vararg (@aritys min)) (inc (apply max (filter int? @aritys))) min)]
         (cond (and vararg (@aritys arity))
               (-fail! ::multiple-varargs {:infos infos})

               (@aritys min)
               (-fail! ::duplicate-arities {:infos infos})

               :else
               (do (swap! aritys conj arity)
                   (assoc acc arity (assoc info :min min)))))) {} infos)))

(defn- -re-min-max [f {min' :min, max' :max} child]
  (let [{min'' :min max'' :max} (-regex-min-max child true)]
    (cond-> {:min (f (or min' 0) min'')} (and max' max'') (assoc :max (f max' max'')))))

(defn- -re-alt-min-max [{min' :min, max' :max} child]
  (let [{min'' :min max'' :max} (-regex-min-max child true)]
    (cond-> {:min (min (or min' miu/+max-size+) min'')} (and max' max'') (assoc :max (max max' max'')))))

;;
;; registry
;;

(defn- -register-var [registry ?v]
  (let [[v pred] (if (vector? ?v) ?v [?v @?v])
        name (-> v meta :name)
        schema (-simple-schema {:type name, :pred pred})]
    (-> registry
        (assoc name schema)
        (assoc @v schema))))

(defn -registry {:arglists '([] [{:keys [registry]}])}
  ([] default-registry)
  ([opts] (or (when opts (mr/registry (opts :registry))) default-registry)))

(defn -property-registry [m options f]
  (let [options (assoc options ::allow-invalid-refs true)]
    (reduce-kv (fn [acc k v] (assoc acc k (f (schema v options)))) {} m)))

(defn -delayed-registry [m f]
  (reduce-kv (fn [acc k v] (assoc acc k (reify IntoSchema (-into-schema [_ _ _ options] (f v options))))) {} m))

(defn- -lookup [?schema options]
  (let [registry (-registry options)]
    (or (mr/-schema registry ?schema)
        (when-some [p (some-> registry (mr/-schema (c/type ?schema)))]
          (when (schema? ?schema)
            (when (= p (-parent ?schema))
              (-fail! ::infinitely-expanding-schema {:schema ?schema})))
          (-into-schema p nil [?schema] options)))))

(defn- -lookup! [?schema ?form f rec options]
  (or (and f (f ?schema) ?schema)
      (if-let [?schema (-lookup ?schema options)]
        (cond-> ?schema rec (recur ?form f rec options))
        (-fail! ::invalid-schema {:schema ?schema, :form ?form}))))

(defn -properties-and-options [properties options f]
  (if-let [r (:registry properties)]
    (let [options (-update options :registry #(mr/composite-registry r (or % (-registry options))))]
      [(assoc properties :registry (-property-registry r options f)) options])
    [properties options]))

;;
;; cache
;;

(defn -create-cache [_options] (atom {}))

(defn -cached [s k f]
  (if (-cached? s)
    (let [c (-cache s)]
      (or (@c k) ((swap! c assoc k (f s)) k)))
    (f s)))

;;
;; forms
;;

(defn -raw-form [type properties children]
  (let [has-children (seq children), has-properties (seq properties)]
    (cond (and has-properties has-children) (reduce conj [type properties] children)
          has-properties [type properties]
          has-children (let [fchild (nth children 0)]
                         (reduce conj
                                 (cond-> [type]
                                   (or (map? fchild)
                                       (nil? fchild)) (conj nil))
                                 children))
          :else type)))

(defn -create-form [type properties children options]
  (let [properties (when (seq properties)
                     (let [registry (:registry properties)]
                       (cond-> properties registry (assoc :registry (-property-registry registry options -form)))))]
    (-raw-form type properties children)))

(defn -simple-form [parent properties children f options]
  (-create-form (-type parent) properties (-vmap f children) options))

(defn -create-entry-form [parent properties entry-parser options]
  (-create-form (-type parent) properties (-entry-forms entry-parser) options))

;;
;; walkers
;;

(defn -inner-indexed [walker path children options]
  (-vmap (fn [[i c]] (-inner walker c (conj path i) options)) (map-indexed vector children)))

(defn -inner-entries [walker path entries options]
  (-vmap (fn [[k s]] [k (-properties s) (-inner walker s (conj path k) options)]) entries))

(defn -walk-entries [schema walker path options]
  (when (-accept walker schema path options)
    (-outer walker schema path (-inner-entries walker path (-entries schema) options) options)))

(defn -walk-indexed [schema walker path options]
  (when (-accept walker schema path options)
    (-outer walker schema path (-inner-indexed walker path (-children schema) options) options)))

(defn -walk-leaf [schema walker path options]
  (when (-accept walker schema path options)
    (-outer walker schema path (-children schema) options)))

;;
;; lenses
;;

(defn -set-children [schema children]
  (if (-equals children (-children schema))
    schema (-into-schema (-parent schema) (-properties schema) children (-options schema))))

(defn -set-properties [schema properties]
  (if (-equals properties (-properties schema))
    schema (-into-schema (-parent schema) properties (or (and (-entry-schema? schema) (-entry-parser schema)) (-children schema)) (-options schema))))

(defn -update-properties [schema f & args]
  (-set-properties schema (not-empty (apply f (-properties schema) args))))

(defn -update-options [schema f]
  (-into-schema (-parent schema) (-properties schema) (-children schema) (f (-options schema))))

(defn -set-assoc-children [schema key value]
  (-set-children schema (assoc (-children schema) key value)))

(defn -get-entries [schema key default]
  (or (some (if (and (vector? key) (= ::find (nth key 0)))
              (fn [e] (when (= (nth e 0) (nth key 1)) e))
              (fn [e] (when (= (nth e 0) key) (nth e 2))))
            (-children schema)) default))

;;
;; entries
;;

(defn -simple-entry-parser [keyset children forms]
  (let [entries (map (fn [[k p s]] (miu/-entry k (-val-schema s p))) children)]
    (reify EntryParser
      (-entry-keyset [_] keyset)
      (-entry-children [_] children)
      (-entry-entries [_] entries)
      (-entry-forms [_] forms))))

(defn- -update-parsed [entry-parser ?key value options]
  (let [[override k p] (if (and (vector? ?key) (nth ?key 0)) (cons true ?key) [false ?key])
        keyset (-entry-keyset entry-parser)
        children (-entry-children entry-parser)
        forms (-entry-forms entry-parser)
        s (when value (schema value options))
        i (:order (keyset k))]
    (if (nil? s)
      ;; remove
      (letfn [(cut [v] (into (subvec v 0 i) (subvec v (inc i))))]
        (-simple-entry-parser (dissoc keyset k) (cut children) (cut forms)))
      (let [p (if i (if override p (nth (children i) 1)) p)
            c [k p s]
            f (if (seq p) [k p (-form s)] [k (-form s)])]
        (if i
          ;; update
          (-simple-entry-parser keyset (assoc children i c) (assoc forms i f))
          ;; assoc
          (-simple-entry-parser (assoc keyset k {:order (count keyset)}) (conj children c) (conj forms f)))))))

(defn -set-entries
  ([schema ?key value]
   (if-let [entry-parser (-entry-parser schema)]
     (-set-children schema (-update-parsed entry-parser ?key value (-options schema)))
     (let [found (atom nil)
           [key props override] (if (vector? ?key) [(nth ?key 0) (second ?key) true] [?key])
           children (cond-> (-vmap (fn [[k p :as entry]]
                                     (if (= key k)
                                       (do (reset! found true) [key (if override props p) value])
                                       entry))
                                   (-children schema))
                      (not @found) (conj (if key [key props value] (-fail! ::key-missing)))
                      :always (->> (filter (fn [e] (-> e last some?)))))]
       (-set-children schema children)))))

(defn- -parse-entry [e naked-keys lazy-refs options i ^objects -children ^objects -forms ^objects -keyset]
  (letfn [(-collect [k c f i]
            (let [i (int i)]
              (aset -keyset (* 2 i) k)
              (aset -keyset (inc (* 2 i)) {:order i})
              (aset -children i c)
              (aset -forms i f)
              (unchecked-inc-int i)))
          (-schema [e] (schema (cond-> e (and (-reference? e) lazy-refs) (-lazy options)) options))
          (-parse-ref-entry [e]
            (let [s (-schema e)
                  c [e nil s]]
              (-collect e c e i)))
          (-parse-ref-vector1 [e e0]
            (let [s (-schema e0)
                  c [e0 nil s]]
              (-collect e0 c e i)))
          (-parse-ref-vector2 [e e0 e1]
            (let [s (-schema e0)
                  c [e0 e1 s]]
              (-collect e0 c e i)))
          (-parse-entry-else2 [e0 e1]
            (let [s (-schema e1)
                  f [e0 (-form s)]
                  c [e0 nil s]]
              (-collect e0 c f i)))
          (-parse-entry-else3 [e0 e1 e2]
            (let [s (-schema e2)
                  f' (-form s)
                  f (if e1 [e0 e1 f'] [e0 f'])
                  c [e0 e1 s]]
              (-collect e0 c f i)))]
    (if (vector? e)
      (let [ea (object-array e)
            n (alength ea)
            e0 (aget ea 0)]
        (if (== n 1)
          (if (and (-reference? e0) naked-keys)
            (-parse-ref-vector1 e e0)
            (-fail! ::invalid-entry {:entry e}))
          (let [e1 (aget ea 1)]
            (if (== n 2)
              (if (and (-reference? e0) (map? e1))
                (if naked-keys (-parse-ref-vector2 e e0 e1) i)
                (-parse-entry-else2 e0 e1))
              (let [e2 (aget ea 2)]
                (-parse-entry-else3 e0 e1 e2))))))
      (if (and naked-keys (-reference? e))
        (-parse-ref-entry e)
        (-fail! ::invalid-entry {:entry e})))))

(defn -eager-entry-parser [children props options]
  (letfn [(-vec [^objects arr] #?(:bb (vec arr) :clj (LazilyPersistentVector/createOwning arr), :cljs (vec arr)))
          (-map [^objects arr] #?(:bb   (let [m (apply array-map arr)]
                                          (when-not (= (* 2 (count m)) (count arr))
                                            (-fail! ::duplicate-keys {:arr arr})) m)
                                  :clj (try (PersistentArrayMap/createWithCheck arr)
                                            (catch Exception _ (-fail! ::duplicate-keys {:arr arr})))
                                  :cljs (let [m (apply array-map arr)]
                                          (when-not (= (* 2 (count m)) (count arr))
                                            (-fail! ::duplicate-keys {:arr arr})) m)))
          (-arange [^objects arr to]
           #?(:clj (let [-arr (object-array to)] (System/arraycopy arr 0 -arr 0 to) -arr)
              :cljs (.slice arr 0 to)))]
    (let [{:keys [naked-keys lazy-refs]} props
          ca (object-array children)
          n (alength ca)
          -children (object-array n)
          -forms (object-array n)
          -keyset (object-array (* 2 n))]
      (loop [i (int 0), ci (int 0)]
        (if (== ci n)
          (let [f (if (== ci i) -vec #(-vec (-arange % i)))]
            (-simple-entry-parser (-map -keyset) (f -children) (f -forms)))
          (recur (int (-parse-entry (aget ca i) naked-keys lazy-refs options i -children -forms -keyset))
                 (unchecked-inc-int ci)))))))

(defn -lazy-entry-parser [?children props options]
  (let [parser (delay (-eager-entry-parser ?children props options))]
    (reify EntryParser
      (-entry-keyset [_] (-entry-keyset @parser))
      (-entry-children [_] (-entry-children @parser))
      (-entry-entries [_] (-entry-entries @parser))
      (-entry-forms [_] (-entry-forms @parser)))))

(defn -create-entry-parser [?children props options]
  (cond (-entry-parser? ?children) ?children
        (or (:lazy props) (::lazy-entries options)) (-lazy-entry-parser ?children props options)
        :else (-eager-entry-parser ?children props options)))

(defn -default-entry [e] (-equals (nth e 0) ::default))
(defn -default-entry-schema [children] (some (fn [e] (when (-default-entry e) (nth e 2))) children))

;;
;; transformers
;;

(defn -no-op-transformer []
  (reify Transformer
    (-transformer-chain [_])
    (-value-transformer [_ _ _ _])))

(defn -intercepting
  ([interceptor] (-intercepting interceptor nil))
  ([{:keys [enter leave]} f] (some->> [leave f enter] (keep identity) (seq) (apply -comp))))

(defn -into-transformer [x]
  (cond
    (-transformer? x) x
    (fn? x) (-into-transformer (x))
    (nil? x) (-no-op-transformer)
    :else (-fail! ::invalid-transformer {:value x})))

(defn -parent-children-transformer [parent children transformer method options]
  (let [parent-transformer (-value-transformer transformer parent method options)
        child-transformers (into [] (keep #(-transformer % transformer method options)) children)
        child-transformer (when (seq child-transformers) (apply -comp (rseq child-transformers)))]
    (-intercepting parent-transformer child-transformer)))

(defn -map-transformer [ts]
  #?(:bb   (fn [x] (reduce (fn child-transformer [m [k t]]
                             (if-let [entry (find m k)]
                               (assoc m k (t (val entry)))
                               m)) x ts))
     :clj  (let [not-found (Object.)]
             (apply -comp (map (fn child-transformer [[k t]]
                                 (fn [^Associative x]
                                   (let [val (.valAt x k not-found)]
                                     (if (identical? val not-found)
                                       x (.assoc x k (t val)))))) (rseq ts))))
     :cljs (fn [x] (reduce (fn child-transformer [m [k t]]
                             (if-let [entry (find m k)]
                               (assoc m k (t (val entry)))
                               m)) x ts))))

(defn -tuple-transformer [ts] (fn [x] (reduce-kv -update x ts)))

(defn -collection-transformer [t empty]
  #?(:bb   (fn [x] (into (when x empty) (map t) x))
     :clj  (fn [x] (let [i (.iterator ^Iterable x)]
                     (loop [x ^IPersistentCollection empty]
                       (if (.hasNext i)
                         (recur (.cons x (t (.next i))))
                         x))))
     :cljs (fn [x] (into (when x empty) (map t) x))))

(defn -or-transformer [this transformer child-schemas method options]
  (let [this-transformer (-value-transformer transformer this method options)]
    (if (seq child-schemas)
      (let [transformers (-vmap #(or (-transformer % transformer method options) identity) child-schemas)
            validators (-vmap -validator child-schemas)]
        (-intercepting this-transformer
                       (if (= :decode method)
                         (fn [x]
                           (reduce-kv
                            (fn [acc i transformer]
                              (let [x* (transformer x)]
                                (if ((nth validators i) x*)
                                  (reduced x*)
                                  (if (-equals acc ::nil) x* acc))))
                            ::nil transformers))
                         (fn [x]
                           (reduce-kv
                            (fn [x i validator] (if (validator x) (reduced ((nth transformers i) x)) x))
                            x validators)))))
      (-intercepting this-transformer))))

;;
;; ast
;;

(defn -parse-entry-ast [ast options]
  (let [ast-entry-order (::ast-entry-order options)
        keyset (:keys ast)
        ->child (fn [[k v]] [k (:properties v) (from-ast (:value v) options)])
        children (delay (-vmap ->child (cond->> keyset ast-entry-order (sort-by #(:order (val %)) keyset))))]
    (reify EntryParser
      (-entry-keyset [_] keyset)
      (-entry-children [_] @children)
      (-entry-entries [_] (-vmap (fn [[k p s]] (miu/-entry k (-val-schema s p))) @children))
      (-entry-forms [_] (->> @children (-vmap (fn [[k p v]] (if p [k p (-form v)] [k (-form v)]))))))))

(defn -from-entry-ast [parent ast options]
  (-into-schema parent (:properties ast) (-parse-entry-ast ast options) options))

(defn -ast [acc properties options]
  (let [registry (when-let [registry (:registry properties)]
                   (into {} (map (fn [[k v]] [k (ast v options)])) registry))
        properties (not-empty (cond-> properties registry (dissoc :registry)))]
    (cond-> acc properties (assoc :properties properties) registry (assoc :registry registry))))

(defn -entry-ast [schema keyset]
  (-ast {:type (type schema)
         :keys (reduce (fn [acc [k p s]] (assoc acc k (cond-> {:order (-> keyset (get k) :order),
                                                               :value (ast s)} p (assoc :properties p))))
                       {} (-children schema))}
        (-properties schema)
        (-options schema)))

(defn -from-child-ast [parent ast options]
  (-into-schema parent (:properties ast) [(from-ast (:child ast) options)] options))

(defn -to-child-ast [schema]
  (-ast {:type (type schema), :child (ast (nth (-children schema) 0))} (-properties schema) (-options schema)))

(defn -from-value-ast [parent ast options]
  (-into-schema parent (:properties ast) (when-let [value (:value ast)] [value]) options))

(defn -to-value-ast [schema]
  (-ast {:type (type schema), :value (nth (-children schema) 0)} (-properties schema) (-options schema)))

(defn -from-type-ast [parent ast options]
  (-into-schema parent (:properties ast) nil options))

(defn -to-type-ast [schema]
  (-ast {:type (type schema)} (-properties schema) (-options schema)))

;;
;; simple schema helpers
;;

(defn -min-max-pred [f]
  (fn [{:keys [min max]}]
    (cond
      (not (or min max)) nil
      (and (and min max) f) (fn [x] (let [size (f x)]
                                      (and (<= min size) (<= size max))))
      (and min max) (fn [x] (and (<= min x) (<= x max)))
      (and min f) (fn [x] (<= min (f x)))
      min (fn [x] (<= min x))
      (and max f) (fn [x] (<= (f x) max))
      max (fn [x] (<= x max)))))

(defn- -safe-count [x]
  (if (-safely-countable? x)
    (count x)
    (reduce (fn [cnt _] (inc cnt)) 0 x)))

(defn -validate-limits [min max] (or ((-min-max-pred -safe-count) {:min min :max max}) (constantly true)))

(defn -needed-bounded-checks [min max options]
  (c/max (or (some-> max inc) 0)
         (or min 0)
         (::coll-check-limit options 101)))

(defn -validate-bounded-limits [needed min max]
  (or ((-min-max-pred #(bounded-count needed %)) {:min min :max max}) (constantly true)))

(defn -qualified-keyword-pred [properties]
  (when-let [ns-name (some-> properties :namespace name)]
    (fn [x] (= (namespace x) ns-name))))

;;
;; Schemas
;;

(defn -simple-schema [props]
  (let [{:keys [type type-properties pred property-pred min max from-ast to-ast compile]
         :or {min 0, max 0, from-ast -from-value-ast, to-ast -to-type-ast}} props]
    (if (fn? props)
      (do
        (-deprecated! "-simple-schema doesn't take fn-props, use :compile property instead")
        (-simple-schema {:compile (fn [c p _] (props c p))}))
      ^{:type ::into-schema}
      (reify
        AST
        (-from-ast [parent ast options] (from-ast parent ast options))
        IntoSchema
        (-type [_] type)
        (-type-properties [_] type-properties)
        (-properties-schema [_ _])
        (-children-schema [_ _])
        (-into-schema [parent properties children options]
          (if compile
            (-into-schema (-simple-schema (merge (dissoc props :compile) (compile properties children options))) properties children options)
            (let [form (delay (-simple-form parent properties children identity options))
                  cache (-create-cache options)]
              (-check-children! type properties children min max)
              ^{:type ::schema}
              (reify
                AST
                (-to-ast [this _] (to-ast this))
                Schema
                (-validator [_]
                  (if-let [pvalidator (when property-pred (property-pred properties))]
                    (fn [x] (and (pred x) (pvalidator x))) pred))
                (-explainer [this path]
                  (let [validator (-validator this)]
                    (fn explain [x in acc]
                      (if-not (validator x) (conj acc (miu/-error path in this x)) acc))))
                (-parser [this]
                  (let [validator (-validator this)]
                    (fn [x] (if (validator x) x ::invalid))))
                (-unparser [this] (-parser this))
                (-transformer [this transformer method options]
                  (-intercepting (-value-transformer transformer this method options)))
                (-walk [this walker path options] (-walk-leaf this walker path options))
                (-properties [_] properties)
                (-options [_] options)
                (-children [_] children)
                (-parent [_] parent)
                (-form [_] @form)
                Cached
                (-cache [_] cache)
                LensSchema
                (-keep [_])
                (-get [_ _ default] default)
                (-set [this key _] (-fail! ::non-associative-schema {:schema this, :key key}))
                #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))])))))
        #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))))

(defn -nil-schema [] (-simple-schema {:type :nil, :pred nil?}))
(defn -any-schema [] (-simple-schema {:type :any, :pred any?}))
(defn -some-schema [] (-simple-schema {:type :some, :pred some?}))
(defn -string-schema [] (-simple-schema {:type :string, :pred string?, :property-pred (-min-max-pred count)}))
(defn -int-schema [] (-simple-schema {:type :int, :pred int?, :property-pred (-min-max-pred nil)}))
(defn -float-schema [] (-simple-schema {:type :float, :pred float?, :property-pred (-min-max-pred nil)}))
(defn -double-schema [] (-simple-schema {:type :double, :pred double?, :property-pred (-min-max-pred nil)}))
(defn -boolean-schema [] (-simple-schema {:type :boolean, :pred boolean?}))
(defn -keyword-schema [] (-simple-schema {:type :keyword, :pred keyword?}))
(defn -symbol-schema [] (-simple-schema {:type :symbol, :pred symbol?}))
(defn -qualified-keyword-schema [] (-simple-schema {:type :qualified-keyword, :pred qualified-keyword?, :property-pred -qualified-keyword-pred}))
(defn -qualified-symbol-schema [] (-simple-schema {:type :qualified-symbol, :pred qualified-symbol?}))
(defn -uuid-schema [] (-simple-schema {:type :uuid, :pred uuid?}))

(defn -and-schema []
  ^{:type ::into-schema}
  (reify IntoSchema
    (-type [_] :and)
    (-type-properties [_])
    (-properties-schema [_ _])
    (-children-schema [_ _])
    (-into-schema [parent properties children options]
      (-check-children! :and properties children 1 nil)
      (let [children (-vmap #(schema % options) children)
            form (delay (-simple-form parent properties children -form options))
            cache (-create-cache options)
            ->parser (fn [f m] (let [parsers (m (-vmap f children))]
                                 #(reduce (fn [x parser] (miu/-map-invalid reduced (parser x))) % parsers)))]
        ^{:type ::schema}
        (reify
          Schema
          (-validator [_]
            (let [validators (-vmap -validator children)] (miu/-every-pred validators)))
          (-explainer [_ path]
            (let [explainers (-vmap (fn [[i c]] (-explainer c (conj path i))) (map-indexed vector children))]
              (fn explain [x in acc] (reduce (fn [acc' explainer] (explainer x in acc')) acc explainers))))
          (-parser [_] (->parser -parser seq))
          (-unparser [_] (->parser -unparser rseq))
          (-transformer [this transformer method options]
            (-parent-children-transformer this children transformer method options))
          (-walk [this walker path options] (-walk-indexed this walker path options))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] children)
          (-parent [_] parent)
          (-form [_] @form)
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_])
          (-get [_ key default] (get children key default))
          (-set [this key value] (-set-assoc-children this key value))
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

(defn -or-schema []
  ^{:type ::into-schema}
  (reify IntoSchema
    (-type [_] :or)
    (-type-properties [_])
    (-properties-schema [_ _])
    (-children-schema [_ _])
    (-into-schema [parent properties children options]
      (-check-children! :or properties children 1 nil)
      (let [children (-vmap #(schema % options) children)
            form (delay (-simple-form parent properties children -form options))
            cache (-create-cache options)
            ->parser (fn [f] (let [parsers (-vmap f children)]
                               #(reduce (fn [_ parser] (miu/-map-valid reduced (parser %))) ::invalid parsers)))]
        ^{:type ::schema}
        (reify
          Schema
          (-validator [_]
            (let [validators (-vmap -validator children)] (miu/-some-pred validators)))
          (-explainer [_ path]
            (let [explainers (-vmap (fn [[i c]] (-explainer c (conj path i))) (map-indexed vector children))]
              (fn explain [x in acc]
                (reduce
                 (fn [acc' explainer]
                   (let [acc'' (explainer x in acc')]
                     (if (identical? acc' acc'') (reduced acc) acc'')))
                 acc explainers))))
          (-parser [_] (->parser -parser))
          (-unparser [_] (->parser -unparser))
          (-transformer [this transformer method options]
            (-or-transformer this transformer children method options))
          (-walk [this walker path options] (-walk-indexed this walker path options))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] children)
          (-parent [_] parent)
          (-form [_] @form)
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_])
          (-get [_ key default] (get children key default))
          (-set [this key value] (-set-assoc-children this key value))
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

(defn -orn-schema []
  ^{:type ::into-schema}
  (reify
    AST
    (-from-ast [parent ast options] (-from-entry-ast parent ast options))
    IntoSchema
    (-type [_] :orn)
    (-type-properties [_])
    (-properties-schema [_ _])
    (-children-schema [_ _])
    (-into-schema [parent properties children options]
      (-check-children! :orn properties children 1 nil)
      (let [entry-parser (-create-entry-parser children {:naked-keys true} options)
            form (delay (-create-entry-form parent properties entry-parser options))
            cache (-create-cache options)]
        ^{:type ::schema}
        (reify
          AST
          (-to-ast [this _] (-entry-ast this (-entry-keyset entry-parser)))
          Schema
          (-validator [this] (miu/-some-pred (-vmap (fn [[_ _ c]] (-validator c)) (-children this))))
          (-explainer [this path]
            (let [explainers (-vmap (fn [[k _ c]] (-explainer c (conj path k))) (-children this))]
              (fn explain [x in acc]
                (reduce
                 (fn [acc' explainer]
                   (let [acc'' (explainer x in acc')]
                     (if (identical? acc' acc'') (reduced acc) acc'')))
                 acc explainers))))
          (-parser [this]
            (let [parsers (-vmap (fn [[k _ c]]
                                   (let [c (-parser c)]
                                     (fn [x] (miu/-map-valid #(reduced (tag k %)) (c x)))))
                                 (-children this))]
              (fn [x] (reduce (fn [_ parser] (parser x)) x parsers))))
          (-unparser [this]
            (let [unparsers (into {} (map (fn [[k _ c]] [k (-unparser c)])) (-children this))]
              (fn [x]
                (if (tag? x)
                  (if-some [unparse (get unparsers (:key x))]
                    (unparse (:value x))
                    ::invalid)
                  ::invalid))))
          (-transformer [this transformer method options]
            (-or-transformer this transformer (-vmap #(nth % 2) (-children this)) method options))
          (-walk [this walker path options] (-walk-entries this walker path options))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] (-entry-children entry-parser))
          (-parent [_] parent)
          (-form [_] @form)
          EntrySchema
          (-entries [_] (-entry-entries entry-parser))
          (-entry-parser [_] entry-parser)
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_])
          (-get [this key default] (-get-entries this key default))
          (-set [this key value] (-set-entries this key value))
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

(defn -not-schema []
  ^{:type ::into-schema}
  (reify
    AST
    (-from-ast [parent ast options] (-from-child-ast parent ast options))
    IntoSchema
    (-type [_] :not)
    (-type-properties [_])
    (-properties-schema [_ _])
    (-children-schema [_ _])
    (-into-schema [parent properties children options]
      (-check-children! :not properties children 1 1)
      (let [[schema :as children] (-vmap #(schema % options) children)
            form (delay (-simple-form parent properties children -form options))
            cache (-create-cache options)]
        ^{:type ::schema}
        (reify
          AST
          (-to-ast [this _] (-to-child-ast this))
          Schema
          (-validator [_] (complement (-validator schema)))
          (-explainer [this path]
            (let [validator (-validator this)]
              (fn explain [x in acc]
                (if-not (validator x) (conj acc (miu/-error (conj path 0) in this x)) acc))))
          (-parser [this]
            (let [validator (-validator this)]
              (fn [x] (if (validator x) x ::invalid))))
          (-unparser [this] (-parser this))
          (-transformer [this transformer method options]
            (-parent-children-transformer this children transformer method options))
          (-walk [this walker path options] (-walk-indexed this walker path options))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] children)
          (-parent [_] parent)
          (-form [_] @form)
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_])
          (-get [_ key default] (get children key default))
          (-set [this key value] (-set-assoc-children this key value))
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

(defn -val-schema
  ([schema properties]
   (-into-schema (-val-schema) properties (list schema) (-options schema)))
  ([]
   ^{:type ::into-schema}
   (reify
     AST
     (-from-ast [parent ast options] (-from-child-ast parent ast options))
     IntoSchema
     (-type [_] ::val)
     (-type-properties [_])
     (-properties-schema [_ _])
     (-children-schema [_ _])
     (-into-schema [parent properties children options]
      #_(-check-children! ::val properties children 1 1)
       (let [children (-vmap #(schema % options) children)
             form (delay (-simple-form parent properties children -form options))
             schema (first children)
             cache (-create-cache options)]
         ^{:type ::schema}
         (reify
           AST
           (-to-ast [this _] (-to-child-ast this))
           Schema
           (-validator [_] (-validator schema))
           (-explainer [_ path] (-explainer schema path))
           (-parser [_] (-parser schema))
           (-unparser [_] (-unparser schema))
           (-transformer [this transformer method options]
             (-parent-children-transformer this (list schema) transformer method options))
           (-walk [this walker path options]
             (if (::walk-entry-vals options)
               (when (-accept walker this path options)
                 (-outer walker this path (list (-inner walker schema path options)) options))
               (-walk schema walker path options)))
           (-properties [_] properties)
           (-options [_] (-options schema))
           (-children [_] [schema])
           (-parent [_] parent)
           (-form [_] @form)
           Cached
           (-cache [_] cache)
           LensSchema
           (-keep [_])
           (-get [_ key default] (if (= 0 key) schema default))
           (-set [_ key value] (when (= 0 key) (-val-schema value properties)))
           RefSchema
           (-ref [_])
           (-deref [_] schema)
           #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
     #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))]))))

(defn -map-schema
  ([]
   (-map-schema {:naked-keys true}))
  ([opts] ;; :naked-keys, :lazy, :pred
   ^{:type ::into-schema}
   (reify
     AST
     (-from-ast [parent ast options] (-from-entry-ast parent ast options))
     IntoSchema
     (-type [_] (:type opts :map))
     (-type-properties [_] (:type-properties opts))
     (-properties-schema [_ _])
     (-children-schema [_ _])
     (-into-schema [parent {:keys [closed] :as properties} children options]
       (let [pred? (:pred opts map?)
             entry-parser (-create-entry-parser children opts options)
             form (delay (-create-entry-form parent properties entry-parser options))
             cache (-create-cache options)
             default-schema (delay (some-> entry-parser (-entry-children) (-default-entry-schema) (schema options)))
             explicit-children (delay (cond->> (-entry-children entry-parser) @default-schema (remove -default-entry)))
             ->parser (fn [this f]
                        (let [keyset (-entry-keyset (-entry-parser this))
                              default-parser (some-> @default-schema (f))
                              ;; prevent unparsing :catn/:orn/etc parse results as maps
                              ok? #(and (pred? %) (not (tag? %)) (not (tags? %)))
                              parsers (cond->> (-vmap
                                                (fn [[key {:keys [optional]} schema]]
                                                  (let [parser (f schema)]
                                                    (fn [m]
                                                      (if-let [e (find m key)]
                                                        (let [v (val e)
                                                              v* (parser v)]
                                                          (cond (miu/-invalid? v*) (reduced v*)
                                                                (identical? v* v) m
                                                                :else (assoc m key v*)))
                                                        (if optional m (reduced ::invalid))))))
                                                @explicit-children)
                                        default-parser
                                        (cons (fn [m]
                                                (let [m' (default-parser
                                                          (reduce (fn [acc k] (dissoc acc k)) m (keys keyset)))]
                                                  (if (miu/-invalid? m')
                                                    (reduced m')
                                                    (merge (select-keys m (keys keyset)) m')))))
                                        closed
                                        (cons (fn [m]
                                                (reduce
                                                 (fn [m k] (if (contains? keyset k) m (reduced (reduced ::invalid))))
                                                 m (keys m)))))]
                          (fn [x] (if (ok? x) (reduce (fn [m parser] (parser m)) x parsers) ::invalid))))]
         ^{:type ::schema}
         (reify
           AST
           (-to-ast [this _] (-entry-ast this (-entry-keyset entry-parser)))
           Schema
           (-validator [this]
             (let [keyset (-entry-keyset (-entry-parser this))
                   default-validator (some-> @default-schema (-validator))
                   validators (cond-> (-vmap
                                       (fn [[key {:keys [optional]} value]]
                                         (let [valid? (-validator value)
                                               default (boolean optional)]
                                           #?(:bb   (fn [m] (if-let [map-entry (find m key)] (valid? (val map-entry)) default))
                                              :clj  (let [not-found (Object.)]
                                                      (fn [^Associative m]
                                                        (let [val (.valAt m key not-found)]
                                                          (if (identical? val not-found)
                                                            default
                                                            (valid? val)))))
                                              :cljs (fn [m] (if-let [map-entry (find m key)] (valid? (val map-entry)) default)))))
                                       @explicit-children)
                                default-validator
                                (conj (fn [m] (default-validator (reduce (fn [acc k] (dissoc acc k)) m (keys keyset)))))
                                (and closed (not default-validator))
                                (conj (fn [m] (reduce (fn [acc k] (if (contains? keyset k) acc (reduced false))) true (keys m)))))
                   validate (miu/-every-pred validators)]
               (fn [m] (and (pred? m) (validate m)))))
           (-explainer [this path]
             (let [keyset (-entry-keyset (-entry-parser this))
                   default-explainer (some-> @default-schema (-explainer (conj path ::default)))
                   explainers (cond-> (-vmap
                                       (fn [[key {:keys [optional]} schema]]
                                         (let [explainer (-explainer schema (conj path key))]
                                           (fn [x in acc]
                                             (if-let [e (find x key)]
                                               (explainer (val e) (conj in key) acc)
                                               (if-not optional
                                                 (conj acc (miu/-error (conj path key) (conj in key) this nil ::missing-key))
                                                 acc)))))
                                       @explicit-children)
                                default-explainer
                                (conj (fn [x in acc]
                                        (default-explainer
                                         (reduce (fn [acc k] (dissoc acc k)) x (keys keyset))
                                         in acc)))
                                (and closed (not default-explainer))
                                (conj (fn [x in acc]
                                        (reduce-kv
                                         (fn [acc k v]
                                           (if (contains? keyset k)
                                             acc
                                             (conj acc (miu/-error (conj path k) (conj in k) this v ::extra-key))))
                                         acc x))))]
               (fn [x in acc]
                 (if-not (pred? x)
                   (conj acc (miu/-error path in this x ::invalid-type))
                   (reduce
                    (fn [acc explainer]
                      (explainer x in acc))
                    acc explainers)))))
           (-parser [this] (->parser this -parser))
           (-unparser [this] (->parser this -unparser))
           (-transformer [this transformer method options]
             (let [keyset (-entry-keyset (-entry-parser this))
                   this-transformer (-value-transformer transformer this method options)
                   ->children (reduce (fn [acc [k s]]
                                        (let [t (-transformer s transformer method options)]
                                          (cond-> acc t (conj [k t]))))
                                      [] (cond->> (-entries this) @default-schema (remove -default-entry)))
                   apply->children (when (seq ->children) (-map-transformer ->children))
                   apply->default (when-let [dt (some-> @default-schema (-transformer transformer method options))]
                                    (fn [x] (merge (dt (reduce (fn [acc k] (dissoc acc k)) x (keys keyset))) (select-keys x (keys keyset)))))
                   apply->children (some->> [apply->default apply->children] (keep identity) (seq) (apply -comp))
                   apply->children (-guard pred? apply->children)]
               (-intercepting this-transformer apply->children)))
           (-walk [this walker path options] (-walk-entries this walker path options))
           (-properties [_] properties)
           (-options [_] options)
           (-children [_] (-entry-children entry-parser))
           (-parent [_] parent)
           (-form [_] @form)
           EntrySchema
           (-entries [_] (-entry-entries entry-parser))
           (-entry-parser [_] entry-parser)
           Cached
           (-cache [_] cache)
           LensSchema
           (-keep [_] true)
           (-get [this key default] (-get-entries this key default))
           (-set [this key value] (-set-entries this key value))
           #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
     #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))]))))

(defn -map-of-schema
  ([]
   (-map-of-schema {}))
  ([opts]
   ^{:type ::into-schema}
   (reify
     AST
     (-from-ast [parent ast options]
       (-into-schema parent (:properties ast) [(from-ast (:key ast) options) (from-ast (:value ast) options)] options))
     IntoSchema
     (-type [_] (:type opts :map-of))
     (-type-properties [_] (:type-properties opts))
     (-properties-schema [_ _])
     (-children-schema [_ _])
     (-into-schema [parent {:keys [min max] :as properties} children options]
       (-check-children! :map-of properties children 2 2)
       (let [[key-schema value-schema :as children] (-vmap #(schema % options) children)
             form (delay (-simple-form parent properties children -form options))
             cache (-create-cache options)
             validate-limits (-validate-limits min max)
             ->parser (fn [f] (let [key-parser (f key-schema)
                                    value-parser (f value-schema)]
                                (fn [x]
                                  (if (map? x)
                                    (reduce-kv (fn [acc k v]
                                                 (let [k* (key-parser k)
                                                       v* (value-parser v)]
                                                   ;; OPTIMIZE: Restore `identical?` check + NOOP
                                                   (if (or (miu/-invalid? k*) (miu/-invalid? v*))
                                                     (reduced ::invalid)
                                                     (assoc acc k* v*))))
                                               (empty x) x)
                                    ::invalid))))]
         ^{:type ::schema}
         (reify
           AST
           (-to-ast [_ _]
             (-ast {:type :map-of, :key (ast key-schema), :value (ast value-schema)} properties options))
           Schema
           (-validator [_]
             (let [key-valid? (-validator key-schema)
                   value-valid? (-validator value-schema)]
               (fn [m]
                 (and (map? m)
                      (validate-limits m)
                      (reduce-kv
                       (fn [_ key value]
                         (or (and (key-valid? key) (value-valid? value)) (reduced false)))
                       true m)))))
           (-explainer [this path]
             (let [key-explainer (-explainer key-schema (conj path 0))
                   value-explainer (-explainer value-schema (conj path 1))]
               (fn explain [m in acc]
                 (if-not (map? m)
                   (conj acc (miu/-error path in this m ::invalid-type))
                   (if-not (validate-limits m)
                     (conj acc (miu/-error path in this m ::limits))
                     (reduce-kv
                      (fn [acc key value]
                        (let [in (conj in key)]
                          (->> acc
                               (key-explainer key in)
                               (value-explainer value in))))
                      acc m))))))
           (-parser [_] (->parser -parser))
           (-unparser [_] (->parser -unparser))
           (-transformer [this transformer method options]
             (let [this-transformer (-value-transformer transformer this method options)
                   ->key (-transformer key-schema transformer method options)
                   ->child (-transformer value-schema transformer method options)
                   ->key-child (cond
                                 (and ->key ->child) #(assoc %1 (->key %2) (->child %3))
                                 ->key #(assoc %1 (->key %2) %3)
                                 ->child #(assoc %1 %2 (->child %3)))
                   apply->key-child (when ->key-child #(reduce-kv ->key-child (empty %) %))
                   apply->key-child (-guard map? apply->key-child)]
               (-intercepting this-transformer apply->key-child)))
           (-walk [this walker path options] (-walk-indexed this walker path options))
           (-properties [_] properties)
           (-options [_] options)
           (-children [_] children)
           (-parent [_] parent)
           (-form [_] @form)
           Cached
           (-cache [_] cache)
           LensSchema
           (-keep [_])
           (-get [_ key default] (get children key default))
           (-set [this key value] (-set-assoc-children this key value))
           #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
     #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))]))))

;; also doubles as a predicate for the :every schema to bound the number
;; of elements to check, so don't add potentially-infinite countable things like seq's.
(defn- -safely-countable? [x]
  (or (nil? x)
      (counted? x)
      (indexed? x)
      ;; note: js/Object not ISeqable
      #?(:clj (instance? java.util.Map x))
      ;; many Seq's are List's, so just pick some popular classes
      #?@(:bb  []
          :clj [(instance? java.util.AbstractList x)
                (instance? java.util.Vector x)])
      #?(:clj  (instance? CharSequence x)
         :cljs (string? x))
      #?(:clj  (.isArray (class x))
         :cljs (identical? js/Array (c/type x)))))

(defn -collection-schema [props]
  (if (fn? props)
    (do (-deprecated! "-collection-schema doesn't take fn-props, use :compiled property instead")
        (-collection-schema {:compile (fn [c p _] (props c p))}))
    ^{:type ::into-schema}
    (reify
      AST
      (-from-ast [parent ast options] (-from-child-ast parent ast options))
      IntoSchema
      (-type [_] (:type props))
      (-type-properties [_] (:type-properties props))
      (-properties-schema [_ _])
      (-children-schema [_ _])
      (-into-schema [parent {:keys [min max] :as properties} children options]
        (if-let [compile (:compile props)]
          (-into-schema (-collection-schema (merge (dissoc props :compile) (compile properties children options))) properties children options)
          (let [{:keys [type parse unparse], fpred :pred, fempty :empty, fin :in :or {fin (fn [i _] i)}} props]
            (-check-children! type properties children 1 1)
            (let [[schema :as children] (-vmap #(schema % options) children)
                  form (delay (-simple-form parent properties children -form options))
                  cache (-create-cache options)
                  bounded (when (:bounded props)
                            (when fempty
                              (-fail! ::cannot-provide-empty-and-bounded-props))
                            (-needed-bounded-checks min max options))
                  validate-limits (if bounded
                                    (-validate-bounded-limits (c/min bounded (or max bounded)) min max)
                                    (-validate-limits min max))
                  ->parser (fn [f g] (let [child-parser (f schema)]
                                       (fn [x]
                                         (cond
                                           (not (fpred x)) ::invalid
                                           (not (validate-limits x)) ::invalid
                                           :else (if bounded
                                                   (let [child-validator child-parser]
                                                     (reduce
                                                      (fn [x v]
                                                        (if (child-validator v) x (reduced ::invalid)))
                                                      x (cond->> x
                                                          (not (-safely-countable? x))
                                                          (eduction (take bounded)))))
                                                   (let [x' (reduce
                                                             (fn [acc v]
                                                               (let [v' (child-parser v)]
                                                                 (if (miu/-invalid? v') (reduced ::invalid) (conj acc v'))))
                                                             [] x)]
                                                     (cond
                                                       (miu/-invalid? x') x'
                                                       g (g x')
                                                       fempty (into fempty x')
                                                       :else x')))))))]
              ^{:type ::schema}
              (reify
                AST
                (-to-ast [this _] (-to-child-ast this))
                Schema
                (-validator [_]
                  (let [validator (-validator schema)]
                    (fn [x] (and (fpred x)
                                 (validate-limits x)
                                 (reduce (fn [acc v] (if (validator v) acc (reduced false))) true
                                         (cond->> x
                                           (and bounded (not (-safely-countable? x)))
                                           (eduction (take bounded))))))))
                (-explainer [this path]
                  (let [explainer (-explainer schema (conj path 0))]
                    (fn [x in acc]
                      (cond
                        (not (fpred x)) (conj acc (miu/-error path in this x ::invalid-type))
                        (not (validate-limits x)) (conj acc (miu/-error path in this x ::limits))
                        :else (let [size (when (and bounded (not (-safely-countable? x)))
                                           bounded)]
                                (loop [acc acc, i 0, [x & xs :as ne] (seq x)]
                                  (if (and ne (or (not size) (< i #?(:cljs    ^number size
                                                                     :default size))))
                                    (cond-> (or (explainer x (conj in (fin i x)) acc) acc) xs (recur (inc i) xs))
                                    acc)))))))
                (-parser [_] (->parser (if bounded -validator -parser) (if bounded identity parse)))
                (-unparser [_] (->parser (if bounded -validator -unparser) (if bounded identity unparse)))
                (-transformer [this transformer method options]
                  (let [collection? #(or (sequential? %) (set? %))
                        this-transformer (-value-transformer transformer this method options)
                        child-transformer (-transformer schema transformer method options)
                        ->child (when child-transformer
                                  (if fempty
                                    (-collection-transformer child-transformer fempty)
                                    #(-vmap child-transformer %)))
                        ->child (-guard collection? ->child)]
                    (-intercepting this-transformer ->child)))
                (-walk [this walker path options]
                  (when (-accept walker this path options)
                    (-outer walker this path [(-inner walker schema (conj path ::in) options)] options)))
                (-properties [_] properties)
                (-options [_] options)
                (-children [_] children)
                (-parent [_] parent)
                (-form [_] @form)
                Cached
                (-cache [_] cache)
                LensSchema
                (-keep [_] true)
                (-get [_ _ _] schema)
                (-set [this _ value] (-set-children this [value]))
               #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))))
      #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))]))))

(defn -tuple-schema
  ([]
   (-tuple-schema {}))
  ([opts]
   ^{:type ::into-schema}
   (reify IntoSchema
     (-type [_] :tuple)
     (-type-properties [_] (:type-properties opts))
     (-properties-schema [_ _])
     (-children-schema [_ _])
     (-into-schema [parent properties children options]
       (let [children (-vmap #(schema % options) children)
             form (delay (-simple-form parent properties children -form options))
             size (count children)
             cache (-create-cache options)
             ->parser (fn [f] (let [parsers (into {} (comp (map f) (map-indexed vector)) children)]
                                (fn [x]
                                  (cond
                                    (not (vector? x)) ::invalid
                                    (not= (count x) size) ::invalid
                                    :else (reduce-kv (fn [x i c]
                                                       (let [v (get x i)
                                                             v* (c v)]
                                                         (cond
                                                           (miu/-invalid? v*) (reduced v*)
                                                           (identical? v* v) x
                                                           :else (assoc x i v*))))
                                                     x parsers)))))]
         ^{:type ::schema}
         (reify
           Schema
           (-validator [_]
             (let [validators (into (array-map) (map-indexed vector (mapv -validator children)))]
               (fn [x] (and (vector? x)
                            (= (count x) size)
                            (reduce-kv
                             (fn [acc i validator]
                               (if (validator (nth x i)) acc (reduced false))) true validators)))))
           (-explainer [this path]
             (let [explainers (-vmap (fn [[i s]] (-explainer s (conj path i))) (map-indexed vector children))]
               (fn [x in acc]
                 (cond
                   (not (vector? x)) (conj acc (miu/-error path in this x ::invalid-type))
                   (not= (count x) size) (conj acc (miu/-error path in this x ::tuple-size))
                   :else (if (zero? size)
                           acc
                           (loop [acc acc, i 0, [x & xs] x, [e & es] explainers]
                             (cond-> (e x (conj in i) acc) xs (recur (inc i) xs es))))))))
           (-parser [_] (->parser -parser))
           (-unparser [_] (->parser -unparser))
           (-transformer [this transformer method options]
             (let [this-transformer (-value-transformer transformer this method options)
                   ->children (into {} (comp (map-indexed vector)
                                             (keep (fn [[k c]]
                                                     (when-some [t (-transformer c transformer method options)]
                                                       [k t])))) children)
                   apply->children (when (seq ->children) (-tuple-transformer ->children))
                   apply->children (-guard vector? apply->children)]
               (-intercepting this-transformer apply->children)))
           (-walk [this walker path options] (-walk-indexed this walker path options))
           (-properties [_] properties)
           (-options [_] options)
           (-children [_] children)
           (-parent [_] parent)
           (-form [_] @form)
           Cached
           (-cache [_] cache)
           LensSchema
           (-keep [_] true)
           (-get [_ key default] (get children key default))
           (-set [this key value] (-set-assoc-children this key value))
           #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
     #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))]))))

(defn -enum-schema []
  ^{:type ::into-schema}
  (reify
    AST
    (-from-ast [parent ast options] (-into-schema parent (:properties ast) (:values ast) options))
    IntoSchema
    (-type [_] :enum)
    (-type-properties [_])
    (-into-schema [parent properties children options]
      (-check-children! :enum properties children 1 nil)
      (let [children (vec children)
            schema (set children)
            form (delay (-simple-form parent properties children identity options))
            cache (-create-cache options)]
        ^{:type ::schema}
        (reify
          AST
          (-to-ast [_ _] (-ast {:type :enum :values children} properties options))
          Schema
          (-validator [_]
            (fn [x] (contains? schema x)))
          (-explainer [this path]
            (let [validator (-validator this)]
              (fn explain [x in acc]
                (if-not (validator x) (conj acc (miu/-error path in this x)) acc))))
          (-parser [_] (fn [x] (if (contains? schema x) x ::invalid)))
          (-unparser [this] (-parser this))
          ;; TODO: should we try to derive the type from values? e.g. [:enum 1 2] ~> int?
          (-transformer [this transformer method options]
            (-intercepting (-value-transformer transformer this method options)))
          (-walk [this walker path options] (-walk-leaf this walker path options))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] children)
          (-parent [_] parent)
          (-form [_] @form)
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_])
          (-get [_ key default] (get children key default))
          (-set [this key value] (-set-assoc-children this key value))
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

(defn -re-schema [class?]
  ^{:type ::into-schema}
  (reify
    AST
    (-from-ast [parent ast options] (-from-value-ast parent ast options))
    IntoSchema
    (-type [_] :re)
    (-type-properties [_])
    (-properties-schema [_ _])
    (-children-schema [_ _])
    (-into-schema [parent properties [child :as children] options]
      (-check-children! :re properties children 1 1)
      (let [children (vec children)
            re (re-pattern child)
            matches? #(and #?(:clj (instance? CharSequence %), :cljs (string? %))
                           (re-find re %))
            form (delay (if class? re (-simple-form parent properties children identity options)))
            cache (-create-cache options)]
        ^{:type ::schema}
        (reify
          AST
          (-to-ast [this _] (-to-value-ast this))
          Schema
          (-validator [_]
            (-safe-pred matches?))
          (-explainer [this path]
            (fn explain [x in acc]
              (try
                (if-not (matches? x)
                  (conj acc (miu/-error path in this x))
                  acc)
                (catch #?(:clj Exception, :cljs js/Error) e
                  (conj acc (miu/-error path in this x (:type (ex-data e))))))))
          (-transformer [this transformer method options]
            (-intercepting (-value-transformer transformer this method options)))
          (-parser [this]
            (let [valid? (-validator this)]
              (fn [x] (if (valid? x) x ::invalid))))
          (-unparser [this] (-parser this))
          (-walk [this walker path options] (-walk-leaf this walker path options))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] children)
          (-parent [_] parent)
          (-form [_] @form)
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_])
          (-get [_ key default] (get children key default))
          (-set [this key value] (-set-assoc-children this key value))
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

(defn -fn-schema []
  ^{:type ::into-schema}
  (reify
    AST
    (-from-ast [parent ast options] (-from-value-ast parent ast options))
    IntoSchema
    (-type [_] :fn)
    (-type-properties [_])
    (-into-schema [parent properties children options]
      (-check-children! :fn properties children 1 1)
      (let [children (vec children)
            f (eval (first children) options)
            form (delay (-simple-form parent properties children identity options))
            cache (-create-cache options)]
        ^{:type ::schema}
        (reify
          AST
          (-to-ast [this _] (-to-value-ast this))
          Schema
          (-validator [_] (-safe-pred f))
          (-explainer [this path]
            (fn explain [x in acc]
              (try
                (if-not (f x)
                  (conj acc (miu/-error path in this x))
                  acc)
                (catch #?(:clj Exception, :cljs js/Error) e
                  (conj acc (miu/-error path in this x (:type (ex-data e))))))))
          (-parser [this]
            (let [validator (-validator this)]
              (fn [x] (if (validator x) x ::invalid))))
          (-unparser [this] (-parser this))
          (-transformer [this transformer method options]
            (-intercepting (-value-transformer transformer this method options)))
          (-walk [this walker path options] (-walk-leaf this walker path options))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] children)
          (-parent [_] parent)
          (-form [_] @form)
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_])
          (-get [_ key default] (get children key default))
          (-set [this key value] (-set-assoc-children this key value))
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

(defn -maybe-schema []
  ^{:type ::into-schema}
  (reify
    AST
    (-from-ast [parent ast options] (-from-child-ast parent ast options))
    IntoSchema
    (-type [_] :maybe)
    (-type-properties [_])
    (-properties-schema [_ _])
    (-children-schema [_ _])
    (-into-schema [parent properties children options]
      (-check-children! :maybe properties children 1 1)
      (let [[schema :as children] (-vmap #(schema % options) children)
            form (delay (-simple-form parent properties children -form options))
            cache (-create-cache options)
            ->parser (fn [f] (let [parser (f schema)] (fn [x] (if (nil? x) x (parser x)))))]
        ^{:type ::schema}
        (reify
          AST
          (-to-ast [this _] (-to-child-ast this))
          Schema
          (-validator [_]
            (let [validator (-validator schema)]
              (fn [x] (or (nil? x) (validator x)))))
          (-explainer [_ path]
            (let [explainer (-explainer schema (conj path 0))]
              (fn explain [x in acc]
                (if (nil? x) acc (explainer x in acc)))))
          (-parser [_] (->parser -parser))
          (-unparser [_] (->parser -unparser))
          (-transformer [this transformer method options]
            (-parent-children-transformer this children transformer method options))
          (-walk [this walker path options] (-walk-indexed this walker path options))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] children)
          (-parent [_] parent)
          (-form [_] @form)
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_])
          (-get [_ key default] (if (= 0 key) schema default))
          (-set [this key value] (if (= 0 key)
                                   (-set-children this [value])
                                   (-fail! ::index-out-of-bounds {:schema this, :key key})))
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

(defn -multi-schema
  ([]
   (-multi-schema {:naked-keys true}))
  ([opts]
   ^{:type ::into-schema}
   (reify
     AST
     (-from-ast [parent ast options] (-from-entry-ast parent ast options))
     IntoSchema
     (-type [_] (or (:type opts) :multi))
     (-type-properties [_] (:type-properties opts))
     (-properties-schema [_ _])
     (-children-schema [_ _])
     (-into-schema [parent properties children options]
       (let [opts' (merge opts (select-keys properties [:lazy-refs]))
             entry-parser (-create-entry-parser children opts' options)
             form (delay (-create-entry-form parent properties entry-parser options))
             cache (-create-cache options)
             dispatch (eval (:dispatch properties) options)
             dispatch-map (delay (into {} (-entry-entries entry-parser)))
             finder (fn [{:keys [::default] :as m}] (fn [x] (m x default)))]
         (when-not dispatch
           (-fail! ::missing-property {:key :dispatch}))
         ^{:type ::schema}
         (reify
           AST
           (-to-ast [this _] (-entry-ast this (-entry-keyset entry-parser)))
           DistributiveSchema
           (-distributive-schema? [_] true)
           (-distribute-to-children [this f _]
             (-into-schema parent
                           properties
                           (mapv (fn [c] (update c 2 f options)) (-children this))
                           options))
           Schema
           (-validator [_]
             (let [find (finder (reduce-kv (fn [acc k s] (assoc acc k (-validator s))) {} @dispatch-map))]
               (fn [x] (if-let [validator (find (dispatch x))] (validator x) false))))
           (-explainer [this path]
             (let [find (finder (reduce (fn [acc [k s]] (assoc acc k (-explainer s (conj path k)))) {} (-entries this)))]
               (fn [x in acc]
                 (if-let [explainer (find (dispatch x))]
                   (explainer x in acc)
                   (let [->path (if (and (map? x) (keyword? dispatch)) #(conj % dispatch) identity)]
                     (conj acc (miu/-error (->path path) (->path in) this x ::invalid-dispatch-value)))))))
           (-parser [_]
             (let [parse (fn [k s] (let [p (-parser s)] (fn [x] (miu/-map-valid #(tag k %) (p x)))))
                   find (finder (reduce-kv (fn [acc k s] (assoc acc k (parse k s))) {} @dispatch-map))]
               (fn [x] (if-some [parser (find (dispatch x))] (parser x) ::invalid))))
           (-unparser [_]
             (let [unparsers (reduce-kv (fn [acc k s] (assoc acc k (-unparser s))) {} @dispatch-map)]
               (fn [x] (if (tag? x) (if-some [f (unparsers (:key x))] (f (:value x)) ::invalid) ::invalid))))
           (-transformer [this transformer method options]
            ;; FIXME: Probably should not use `dispatch`
            ;; Can't use `dispatch` as `x` might not be valid before it has been unparsed:
             (let [this-transformer (-value-transformer transformer this method options)
                   ->children (reduce-kv (fn [acc k s] (let [t (-transformer s transformer method options)]
                                                         (cond-> acc t (assoc k t)))) {} @dispatch-map)
                   find (finder ->children)
                   child-transformer (when (seq ->children) (fn [x] (if-some [t (find (dispatch x))] (t x) x)))]
               (-intercepting this-transformer child-transformer)))
           (-walk [this walker path options] (-walk-entries this walker path options))
           (-properties [_] properties)
           (-options [_] options)
           (-children [_] (-entry-children entry-parser))
           (-parent [_] parent)
           (-form [_] @form)
           EntrySchema
           (-entries [_] (-entry-entries entry-parser))
           (-entry-parser [_] entry-parser)
           Cached
           (-cache [_] cache)
           LensSchema
           (-keep [_])
           (-get [this key default] (-get-entries this key default))
           (-set [this key value] (-set-entries this key value))
           #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
     #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))]))))

(defn -ref-schema
  ([]
   (-ref-schema nil))
  ([{:keys [lazy type-properties]}]
   ^{:type ::into-schema}
   (reify
     AST
     (-from-ast [parent ast options] (-from-value-ast parent ast options))
     IntoSchema
     (-type [_] :ref)
     (-type-properties [_] type-properties)
     (-into-schema [parent properties [ref :as children] {::keys [allow-invalid-refs] :as options}]
       (-check-children! :ref properties children 1 1)
       (when-not (-reference? ref)
         (-fail! ::invalid-ref {:ref ref}))
       (let [rf (or (and lazy (-memoize (fn [] (schema (mr/-schema (-registry options) ref) options))))
                    (when-let [s (mr/-schema (-registry options) ref)] (-memoize (fn [] (schema s options))))
                    (when-not allow-invalid-refs
                      (-fail! ::invalid-ref {:type :ref, :ref ref})))
             children (vec children)
             form (delay (-simple-form parent properties children identity options))
             cache (-create-cache options)
             ->parser (fn [f] (let [parser (-memoize (fn [] (f (rf))))]
                                (fn [x] ((parser) x))))]
         ^{:type ::schema}
         (reify
           AST
           (-to-ast [this _] (-to-value-ast this))
           Schema
           (-validator [_]
             (let [validator (-memoize (fn [] (-validator (rf))))]
               (fn [x] ((validator) x))))
           (-explainer [_ path]
             (let [explainer (-memoize (fn [] (-explainer (rf) (into path [0 0]))))]
               (fn [x in acc] ((explainer) x in acc))))
           (-parser [_] (->parser -parser))
           (-unparser [_] (->parser -unparser))
           (-transformer [this transformer method options]
             (let [this-transformer (-value-transformer transformer this method options)
                   deref-transformer (-memoize (fn [] (-transformer (rf) transformer method options)))]
               (-intercepting this-transformer (fn [x] (if-some [t (deref-transformer)] (t x) x)))))
           (-walk [this walker path options]
             (let [accept (fn [] (-inner walker (rf) (into path [0 0])
                                         (-update options ::walked-refs #(conj (or % #{}) ref))))]
               (when (-accept walker this path options)
                 (if (or (not ((-boolean-fn (::walk-refs options false)) ref))
                         (contains? (::walked-refs options) ref))
                   (-outer walker this path [ref] options)
                   (-outer walker this path [(accept)] options)))))
           (-properties [_] properties)
           (-options [_] options)
           (-children [_] children)
           (-parent [_] parent)
           (-form [_] @form)
           Cached
           (-cache [_] cache)
           LensSchema
           (-get [_ key default] (if (= key 0) (-pointer ref (rf) options) default))
           (-keep [_])
           (-set [this key value] (if (= key 0) (-set-children this [value])
                                                (-fail! ::index-out-of-bounds {:schema this, :key key})))
           RefSchema
           (-ref [_] ref)
           (-deref [_] (rf))
           RegexSchema
           (-regex-op? [_] false)
           (-regex-validator [this] (-fail! ::potentially-recursive-seqex this))
           (-regex-explainer [this _] (-fail! ::potentially-recursive-seqex this))
           (-regex-parser [this] (-fail! ::potentially-recursive-seqex this))
           (-regex-unparser [this] (-fail! ::potentially-recursive-seqex this))
           (-regex-transformer [this _ _ _] (-fail! ::potentially-recursive-seqex this))
           (-regex-min-max [this _] (-fail! ::potentially-recursive-seqex this))
           #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
     #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))]))))

(defn -schema-schema [{:keys [id raw]}]
  ^{:type ::into-schema}
  (let [internal (or id raw)
        type (if internal ::schema :schema)]
    ^{:type ::into-schema}
    (reify
      AST
      (-from-ast [parent ast options] ((if internal -from-value-ast -from-child-ast) parent ast options))
      IntoSchema
      (-type [_] type)
      (-type-properties [_])
      (-properties-schema [_ _])
      (-children-schema [_ _])
      (-into-schema [parent properties children options]
        (-check-children! type properties children 1 1)
        (let [children (-vmap #(schema % options) children)
              child (nth children 0)
              form (delay (or (and (empty? properties) (or id (and raw (-form child))))
                              (-simple-form parent properties children -form options)))
              cache (-create-cache options)]
          ^{:type ::schema}
          (reify
            AST
            (-to-ast [this _]
              (cond
                id (-ast {:type type, :value id} (-properties this) (-options this))
                raw (-to-value-ast this)
                :else (-to-child-ast this)))
            Schema
            (-validator [_] (-validator child))
            (-explainer [_ path] (-explainer child (conj path 0)))
            (-parser [_] (-parser child))
            (-unparser [_] (-unparser child))
            (-transformer [this transformer method options]
              (-parent-children-transformer this children transformer method options))
            (-walk [this walker path options]
              (when (-accept walker this path options)
                (if (or (not id) ((-boolean-fn (::walk-schema-refs options false)) id))
                  (-outer walker this path (-inner-indexed walker path children options) options)
                  (-outer walker this path children options))))
            (-properties [_] properties)
            (-options [_] options)
            (-children [_] children)
            (-parent [_] parent)
            (-form [_] @form)
            Cached
            (-cache [_] cache)
            LensSchema
            (-keep [_])
            (-get [_ key default] (if (= key 0) child default))
            (-set [this key value] (if (= key 0) (-set-children this [value])
                                                 (-fail! ::index-out-of-bounds {:schema this, :key key})))
            RefSchema
            (-ref [_] id)
            (-deref [_] child)
            RegexSchema
            (-regex-op? [_]
              (if internal
                (-regex-op? child)
                false))
            (-regex-validator [_]
              (if internal
                (-regex-validator child)
                (re/item-validator (-validator child))))
            (-regex-explainer [_ path]
              (if internal
                (-regex-explainer child path)
                (re/item-explainer path child (-explainer child path))))
            (-regex-parser [_]
              (if internal
                (-regex-parser child)
                (re/item-parser (parser child))))
            (-regex-unparser [_]
              (if internal
                (-regex-unparser child)
                (re/item-unparser (unparser child))))
            (-regex-transformer [_ transformer method options]
              (if internal
                (-regex-transformer child transformer method options)
                (re/item-transformer method (-validator child)
                                     (or (-transformer child transformer method options) identity))))
            (-regex-min-max [_ nested?]
              (if (and nested? (not internal))
                {:min 1 :max 1}
                (-regex-min-max child nested?)))
            #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
      #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))]))))

(defn -=>-schema []
  ^{:type ::into-schema}
  (reify
    AST
    (-from-ast [parent {:keys [input output guard properties]} options]
      (-into-schema parent properties (cond-> [(from-ast input options) (from-ast output options)]
                                        guard (conj (from-ast guard))) options))
    IntoSchema
    (-type [_] :=>)
    (-type-properties [_])
    (-into-schema [parent properties children {::keys [function-checker] :as options}]
      (-check-children! :=> properties children 2 3)
      (let [[input output guard :as children] (-vmap #(schema % options) children)
            form (delay (-create-form (-type parent) properties (-vmap -form children) options))
            cache (-create-cache options)
            ->checker (if function-checker #(function-checker % options) (constantly nil))]
        (when-not (#{:cat :catn} (type input))
          (-fail! ::invalid-input-schema {:input input}))
        ^{:type ::schema}
        (reify
          AST
          (-to-ast [_ _]
            (cond-> {:type :=>, :input (ast input), :output (ast output)}
              guard (assoc :guard (ast guard)), properties (assoc :properties properties)))
          Schema
          (-validator [this]
            (if-let [checker (->checker this)]
              (let [validator (fn [x] (nil? (checker x)))]
                (fn [x] (and (ifn? x) (validator x)))) ifn?))
          (-explainer [this path]
            (if-let [checker (->checker this)]
              (fn explain [x in acc]
                (if (not (fn? x))
                  (conj acc (miu/-error path in this x))
                  (if-let [res (checker x)]
                    (let [{::keys [explain-input explain-output explain-guard]} res
                          res (dissoc res ::explain-input ::explain-output ::explain-guard)
                          {:keys [path in] :as error} (assoc (miu/-error path in this x) :check res)
                          -push (fn [acc i e]
                                  (cond-> acc e (into (map #(assoc % :path (conj path i), :in in) (:errors e)))))]
                      (-> (conj acc error) (-push 0 explain-input) (-push 1 explain-output) (-push 2 explain-guard)))
                    acc)))
              (let [validator (-validator this)]
                (fn explain [x in acc]
                  (if-not (validator x) (conj acc (miu/-error path in this x)) acc)))))
          (-parser [this]
            (let [validator (-validator this)]
              (fn [x] (if (validator x) x ::invalid))))
          (-unparser [this] (-parser this))
          (-transformer [_ _ _ _])
          (-walk [this walker path options] (-walk-indexed this walker path options))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] children)
          (-parent [_] parent)
          (-form [_] @form)
          FunctionSchema
          (-function-schema? [_] true)
          (-function-schema-arities [this] [this])
          (-function-info [_]
            (let [{:keys [min max]} (-regex-min-max input false)]
              (cond-> {:min min
                       :arity (if (= min max) min :varargs)
                       :input input
                       :output output}
                guard (assoc :guard guard)
                max (assoc :max max))))
          (-instrument-f [schema {:keys [scope report gen] :as props} f _options]
            (let [{:keys [min max input output guard]} (-function-info schema)
                  [validate-input validate-output] (-vmap -validator [input output])
                  validate-guard (or (some-> guard -validator) any?)
                  [wrap-input wrap-output wrap-guard] (-vmap #(contains? scope %) [:input :output :guard])
                  f (or (if gen (gen schema) f) (-fail! ::missing-function {:props props}))]
              (fn [& args]
                (let [args (vec args), arity (count args)]
                  (when wrap-input
                    (when-not (<= min arity (or max miu/+max-size+))
                      (report ::invalid-arity {:arity arity, :arities #{{:min min :max max}}, :args args, :input input, :schema schema}))
                    (when-not (validate-input args)
                      (report ::invalid-input {:input input, :args args, :schema schema})))
                  (let [value (apply f args)]
                    (when (and wrap-output (not (validate-output value)))
                      (report ::invalid-output {:output output, :value value, :args args, :schema schema}))
                    (when (and wrap-guard (not (validate-guard [args value])))
                      (report ::invalid-guard {:guard guard, :value value, :args args, :schema schema}))
                    value)))))
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_])
          (-get [_ key default] (get children key default))
          (-set [this key value] (-set-assoc-children this key value))
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

(defn -function-schema [_]
  ^{:type ::into-schema}
  (reify IntoSchema
    (-type [_] :function)
    (-type-properties [_])
    (-properties-schema [_ _])
    (-children-schema [_ _])
    (-into-schema [parent properties children {::keys [function-checker] :as options}]
      (-check-children! :function properties children 1 nil)
      (let [children (-vmap #(schema % options) children)
            form (delay (-simple-form parent properties children -form options))
            cache (-create-cache options)
            ->checker (if function-checker #(function-checker % options) (constantly nil))]
        (when-not (every? (every-pred -function-schema? -function-info) children)
          (-fail! ::non-function-childs {:children children}))
        (-group-by-arity! (-vmap -function-info children))
        ^{:type ::schema}
        (reify
          Schema
          (-validator [this]
            (if-let [checker (->checker this)]
              (let [validator (fn [x] (nil? (checker x)))]
                (fn [x] (and (ifn? x) (validator x)))) ifn?))
          (-explainer [this path]
            (if-let [checker (->checker this)]
              (fn explain [x in acc]
                (if (not (fn? x))
                  (conj acc (miu/-error path in this x))
                  (if-let [res (checker x)]
                    (conj acc (assoc (miu/-error path in this x) :check res))
                    acc)))
              (let [validator (-validator this)]
                (fn explain [x in acc]
                  (if-not (validator x) (conj acc (miu/-error path in this x)) acc)))))
          (-parser [this]
            (let [validator (-validator this)]
              (fn [x] (if (validator x) x ::invalid))))
          (-unparser [this] (-parser this))
          (-transformer [_ _ _ _])
          (-walk [this walker path options] (-walk-indexed this walker path options))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] children)
          (-parent [_] parent)
          (-form [_] @form)
          FunctionSchema
          (-function-schema? [_] true)
          (-function-schema-arities [_] children)
          (-function-info [_])
          (-instrument-f [this {:keys [_scope report] :as props} f options]
            (let [arity->info (->> children
                                   (map (fn [s] (assoc (-function-info s) :f (-instrument (assoc props :schema s) f options))))
                                   (-group-by-arity!))
                  arities (-> arity->info keys set)
                  varargs-info (arity->info :varargs)]
              (if (= 1 (count arities))
                (-> arity->info first val :f)
                (fn [& args]
                  (let [arity (count args)
                        {:keys [input] :as info} (arity->info arity)
                        report-arity #(report ::invalid-arity {:arity arity, :arities arities, :args args, :input input, :schema this})]
                    (cond
                      info (apply (:f info) args)
                      varargs-info (if (< arity (:min varargs-info)) (report-arity) (apply (:f varargs-info) args))
                      :else (report-arity)))))))
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_])
          (-get [_ key default] (get children key default))
          (-set [this key value] (-set-assoc-children this key value))
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

(defn -proxy-schema [{:keys [type min max childs type-properties fn]}]
  ^{:type ::into-schema}
  (reify IntoSchema
    (-type [_] type)
    (-type-properties [_] type-properties)
    (-properties-schema [_ _])
    (-children-schema [_ _])
    (-into-schema [parent properties children options]
      (-check-children! type properties children min max)
      (let [[children forms schema] (fn properties (vec children) options)
            schema (delay (force schema))
            form (delay (-create-form type properties forms options))
            cache (-create-cache options)]
        ^{:type ::schema}
        (reify
          Schema
          (-validator [_] (-validator @schema))
          (-explainer [_ path] (-explainer @schema (conj path ::in)))
          (-parser [_] (-parser @schema))
          (-unparser [_] (-unparser @schema))
          (-transformer [this transformer method options]
            (-parent-children-transformer this [@schema] transformer method options))
          (-walk [this walker path options]
            (let [children (if childs (subvec children 0 childs) children)]
              (when (-accept walker this path options)
                (-outer walker this path (-inner-indexed walker path children options) options))))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] children)
          (-parent [_] parent)
          (-form [_] @form)
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_])
          (-get [_ key default] (if (= ::in key) @schema (get children key default)))
          (-set [_ key value] (into-schema type properties (assoc children key value)))
          DistributiveSchema
          (-distributive-schema? [_] (-distributive-schema? schema))
          (-distribute-to-children [_ f options] (-distribute-to-children schema f options))
          FunctionSchema
          (-function-schema? [_] (-function-schema? @schema))
          (-function-info [_] (-function-info @schema))
          (-function-schema-arities [_] (-function-schema-arities @schema))
          (-instrument-f [_ props f options] (-instrument-f @schema props f options))
          RegexSchema
          (-regex-op? [_] (-regex-op? @schema))
          (-regex-validator [_] (-regex-validator @schema))
          (-regex-explainer [_ path] (-regex-explainer @schema path))
          (-regex-unparser [_] (-regex-unparser @schema))
          (-regex-parser [_] (-regex-parser @schema))
          (-regex-transformer [_ transformer method options] (-regex-transformer @schema transformer method options))
          (-regex-min-max [_ nested?] (-regex-min-max @schema nested?))
          RefSchema
          (-ref [_])
          (-deref [_] @schema)
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

(defn -->-schema
  "Experimental simple schema for :=> schema. AST and explain results subject to change."
  [_]
  (-proxy-schema {:type :->
                  :fn (fn [{:keys [guard] :as p} c o]
                        (-check-children! :-> p c 1 nil)
                        (let [c (mapv #(schema % o) c)]
                          [c (map -form c) (delay (let [cc (cond-> [(into [:cat] (pop c)) (peek c)]
                                                             guard (conj [:fn guard]))]
                                                    (into-schema :=> (dissoc p :guard) cc o)))]))}))

(defn- regex-validator [schema] (re/validator (-regex-validator schema)))

(defn- regex-explainer [schema path] (re/explainer schema path (-regex-explainer schema path)))

(defn- regex-parser [schema] (re/parser (-regex-parser schema)))

(defn- regex-transformer [schema transformer method options]
  (let [this-transformer (-value-transformer transformer schema method options)
        ->children (re/transformer (-regex-transformer schema transformer method options))]
    (-intercepting this-transformer ->children)))

(defn -sequence-schema
  [{:keys [type re-validator re-explainer re-parser re-unparser re-transformer re-min-max] {:keys [min max]} :child-bounds}]
  ^{:type ::into-schema}
  (reify IntoSchema
    (-type [_] type)
    (-type-properties [_])
    (-properties-schema [_ _])
    (-children-schema [_ _])
    (-into-schema [parent properties children options]
      (-check-children! type properties children min max)
      (let [children (-vmap #(schema % options) children)
            form (delay (-simple-form parent properties children -form options))
            cache (-create-cache options)]
        ^{:type ::schema}
        (reify
          Schema
          (-validator [this] (regex-validator this))
          (-explainer [this path] (regex-explainer this path))
          (-parser [this] (regex-parser this))
          (-unparser [this] (-regex-unparser this))
          (-transformer [this transformer method options] (regex-transformer this transformer method options))
          (-walk [this walker path options] (-walk-indexed this walker path options))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] children)
          (-parent [_] parent)
          (-form [_] @form)
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_] true)
          (-get [_ key default] (get children key default))
          (-set [this key value] (-set-assoc-children this key value))
          RegexSchema
          (-regex-op? [_] true)
          (-regex-validator [_] (re-validator properties (-vmap -regex-validator children)))
          (-regex-explainer [_ path]
            (re-explainer properties (map-indexed (fn [i child] (-regex-explainer child (conj path i))) children)))
          (-regex-parser [_] (re-parser properties (-vmap -regex-parser children)))
          (-regex-unparser [_] (re-unparser properties (-vmap -regex-unparser children)))
          (-regex-transformer [_ transformer method options]
            (re-transformer properties (-vmap #(-regex-transformer % transformer method options) children)))
          (-regex-min-max [_ _] (re-min-max properties children))
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

(defn -sequence-entry-schema
  [{:keys [type re-validator re-explainer re-parser re-unparser re-transformer re-min-max] {:keys [min max keep]} :child-bounds :as opts}]
  ^{:type ::into-schema}
  (reify
    AST
    (-from-ast [parent ast options] (-from-entry-ast parent ast options))
    IntoSchema
    (-type [_] type)
    (-type-properties [_])
    (-properties-schema [_ _])
    (-children-schema [_ _])
    (-into-schema [parent properties children options]
      (-check-children! type properties children min max)
      (let [entry-parser (-create-entry-parser children opts options)
            form (delay (-create-entry-form parent properties entry-parser options))
            cache (-create-cache options)]
        ^{:type ::schema}
        (reify
          AST
          (-to-ast [this _] (-entry-ast this (-entry-keyset entry-parser)))
          Schema
          (-validator [this] (regex-validator this))
          (-explainer [this path] (regex-explainer this path))
          (-parser [this] (regex-parser this))
          (-unparser [this] (-regex-unparser this))
          (-transformer [this transformer method options] (regex-transformer this transformer method options))
          (-walk [this walker path options] (-walk-entries this walker path options))
          (-properties [_] properties)
          (-options [_] options)
          (-children [_] (-entry-children entry-parser))
          (-parent [_] parent)
          (-form [_] @form)
          Cached
          (-cache [_] cache)
          LensSchema
          (-keep [_] keep)
          (-get [this key default] (-get-entries this key default))
          (-set [this key value] (-set-entries this key value))
          EntrySchema
          (-entries [_] (-entry-entries entry-parser))
          (-entry-parser [_] entry-parser)
          RegexSchema
          (-regex-op? [_] true)
          (-regex-validator [this] (re-validator properties (-vmap (fn [[k _ s]] [k (-regex-validator s)]) (-children this))))
          (-regex-explainer [this path]
            (re-explainer properties (-vmap (fn [[k _ s]] [k (-regex-explainer s (conj path k))]) (-children this))))
          (-regex-parser [this] (re-parser properties (-vmap (fn [[k _ s]] [k (-regex-parser s)]) (-children this))))
          (-regex-unparser [this] (re-unparser properties (-vmap (fn [[k _ s]] [k (-regex-unparser s)]) (-children this))))
          (-regex-transformer [this transformer method options]
            (re-transformer properties (-vmap (fn [[k _ s]] [k (-regex-transformer s transformer method options)]) (-children this))))
          (-regex-min-max [this _] (re-min-max properties (-children this)))
          #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-schema this writer opts))]))))
    #?@(:cljs [IPrintWithWriter (-pr-writer [this writer opts] (pr-writer-into-schema this writer opts))])))

;;
;; public api
;;

;;
;; into-schema
;;

(defn into-schema?
  "Checks if x is a IntoSchema instance"
  [x] (#?(:clj instance?, :cljs implements?) malli.core.IntoSchema x))

(defn into-schema
  "Creates a Schema instance out of type, optional properties map and children"
  ([type properties children]
   (into-schema type properties children nil))
  ([type properties children options]
   (let [properties' (when properties (when (pos? (count properties)) properties))
         r (when properties' (properties' :registry))
         options (if r (-update options :registry #(mr/composite-registry r (or % (-registry options)))) options)
         properties (if r (assoc properties' :registry (-property-registry r options identity)) properties')]
     (-into-schema (-lookup! type [type properties children] into-schema? false options) properties children options))))

(defn type
  "Returns the Schema type."
  ([?schema]
   (type ?schema nil))
  ([?schema options]
   (-type (-parent (schema ?schema options)))))

(defn type-properties
  "Returns the Schema type properties"
  ([?schema]
   (type-properties ?schema nil))
  ([?schema options]
   (-type-properties (-parent (schema ?schema options)))))

(defn properties-schema
  "Returns properties schema for Schema or IntoSchema."
  ([?schema]
   (properties-schema ?schema nil))
  ([?schema options]
   (if (into-schema? ?schema)
     (some-> ?schema (-properties-schema options) schema)
     (some-> (schema ?schema options) -parent (-properties-schema options)))))

(defn children-schema
  "Returns children schema for Schema or IntoSchema."
  ([?schema]
   (children-schema ?schema nil))
  ([?schema options]
   (if (into-schema? ?schema)
     (some-> ?schema (-children-schema options) schema)
     (some-> (schema ?schema options) -parent (-children-schema options)))))

;;
;; schema
;;

(defn schema?
  "Checks if x is a Schema instance"
  [x] (#?(:clj instance?, :cljs implements?) malli.core.Schema x))

(defn schema
  "Creates a Schema object from any of the following:

   - Schema instance (just returns it)
   - IntoSchema instance
   - Schema vector syntax, e.g. [:string {:min 1}]
   - Qualified Keyword or String, using a registry lookup"
  ([?schema]
   (schema ?schema nil))
  ([?schema options]
   (cond
     (schema? ?schema) ?schema
     (into-schema? ?schema) (-into-schema ?schema nil nil options)
     (vector? ?schema) (let [v #?(:clj ^IPersistentVector ?schema, :cljs ?schema)
                             t (-lookup! #?(:clj (.nth v 0), :cljs (nth v 0)) v into-schema? true options)
                             n #?(:bb (count v) :clj (.count v), :cljs (count v))
                             ?p (when (> n 1) #?(:clj (.nth v 1), :cljs (nth v 1)))]
                         (if (or (nil? ?p) (map? ?p))
                           (into-schema t ?p (when (< 2 n) (subvec ?schema 2 n)) options)
                           (into-schema t nil (when (< 1 n) (subvec ?schema 1 n)) options)))
     :else (if-let [?schema' (and (-reference? ?schema) (-lookup ?schema options))]
             (-pointer ?schema (schema ?schema' options) options)
             (-> ?schema (-lookup! ?schema nil false options) (recur options))))))

(defn form
  "Returns the Schema form"
  ([?schema]
   (form ?schema nil))
  ([?schema options]
   (-form (schema ?schema options))))

(defn properties
  "Returns the Schema properties"
  ([?schema]
   (properties ?schema nil))
  ([?schema options]
   (-properties (schema ?schema options))))

(defn options
  "Returns options used in creating the Schema"
  ([?schema]
   (options ?schema nil))
  ([?schema options]
   (-options (schema ?schema options))))

(defn children
  "Returns the Schema children with all Child Schemas resolved. For
  `MapEntry` Schemas, returns a always tuple3 of `key ?properties child`"
  ([?schema]
   (children ?schema nil))
  ([?schema options]
   (let [schema (schema ?schema options)]
     (-children schema))))

(defn parent
  "Returns the IntoSchema instance that created the Schema"
  ([?schema]
   (parent ?schema nil))
  ([?schema options]
   (-parent (schema ?schema options))))

(defn walk
  "Postwalks recursively over the Schema and it's children.
   The walker callback is a arity4 function with the following
   arguments: schema, path, (walked) children and options."
  ([?schema f]
   (walk ?schema f nil))
  ([?schema f options]
   (-walk
    (schema ?schema options)
    (reify Walker
      (-accept [_ s _ _] s)
      (-inner [this s p options] (-walk s this p options))
      (-outer [_ s p c options] (f s p c options)))
    [] options)))

(defn validator
  "Returns an pure validation function of type `x -> boolean` for a given Schema.
   Caches the result for [[Cached]] Schemas with key `:validator`."
  ([?schema]
   (validator ?schema nil))
  ([?schema options]
   (-cached (schema ?schema options) :validator -validator)))

(defn validate
  "Returns true if value is valid according to given schema. Creates the `validator`
   for every call. When performance matters, (re-)use `validator` instead."
  ([?schema value]
   (validate ?schema value nil))
  ([?schema value options]
   ((validator ?schema options) value)))

(defn explainer
  "Returns an pure explainer function of type `x -> explanation` for a given Schema.
   Caches the result for [[Cached]] Schemas with key `:explainer`."
  ([?schema]
   (explainer ?schema nil))
  ([?schema options]
   (let [schema' (schema ?schema options)
         explainer' (-cached schema' :explainer #(-explainer % []))]
     (fn explainer
       ([value]
        (explainer value [] []))
       ([value in acc]
        (when-let [errors (seq (explainer' value in acc))]
          {:schema schema'
           :value value
           :errors errors}))))))

(defn explain
  "Explains a value against a given schema. Creates the `explainer` for every call.
   When performance matters, (re-)use `explainer` instead."
  ([?schema value]
   (explain ?schema value nil))
  ([?schema value options]
   ((explainer ?schema options) value [] [])))

(defn parser
  "Returns an pure parser function of type `x -> either parsed-x ::invalid` for a given Schema.
   Caches the result for [[Cached]] Schemas with key `:parser`."
  ([?schema]
   (parser ?schema nil))
  ([?schema options]
   (-cached (schema ?schema options) :parser -parser)))

(defn parse
  "parses a value against a given schema. Creates the `parser` for every call.
   When performance matters, (re-)use `parser` instead."
  ([?schema value]
   (parse ?schema value nil))
  ([?schema value options]
   ((parser ?schema options) value)))

(defn unparser
  "Returns an pure unparser function of type `parsed-x -> either x ::invalid` for a given Schema.
   Caches the result for [[Cached]] Schemas with key `:unparser`."
  ([?schema]
   (unparser ?schema nil))
  ([?schema options]
   (-cached (schema ?schema options) :unparser -unparser)))

(defn unparse
  "Unparses a value against a given schema. Creates the `unparser` for every call.
   When performance matters, (re-)use `unparser` instead."
  ([?schema value]
   (unparse ?schema value nil))
  ([?schema value options]
   ((unparser ?schema options) value)))

(defn decoder
  "Creates a value decoding function given a transformer and a schema."
  ([?schema t]
   (decoder ?schema nil t))
  ([?schema options t]
   (or (-transformer (schema ?schema options) (-into-transformer t) :decode options)
       identity)))

(defn decode
  "Transforms a value with a given decoding transformer against a schema."
  ([?schema value t]
   (decode ?schema value nil t))
  ([?schema value options t]
   (if-let [transform (decoder ?schema options t)]
     (transform value)
     value)))

(defn encoder
  "Creates a value encoding transformer given a transformer and a schema."
  ([?schema t]
   (encoder ?schema nil t))
  ([?schema options t]
   (or (-transformer (schema ?schema options) (-into-transformer t) :encode options)
       identity)))

(defn encode
  "Transforms a value with a given encoding transformer against a schema."
  ([?schema value t]
   (encode ?schema value nil t))
  ([?schema value options t]
   (if-let [transform (encoder ?schema options t)]
     (transform value)
     value)))

(defn coercer
  "Creates a function to decode and validate a value, throws on validation error."
  ([?schema] (coercer ?schema nil nil))
  ([?schema transformer] (coercer ?schema transformer nil))
  ([?schema transformer options] (coercer ?schema transformer nil nil options))
  ([?schema transformer respond raise] (coercer ?schema transformer respond raise nil))
  ([?schema transformer respond raise options]
   (let [s (schema ?schema options)
         valid? (validator s)
         decode (decoder s transformer)
         explain (explainer s)
         respond (or respond identity)
         raise (or raise #(-fail! ::coercion %))]
     (fn -coercer [x] (let [value (decode x)]
                        (if (valid? value)
                          (respond value)
                          (raise {:value value, :schema s, :explain (explain value)})))))))

(defn coerce
  "Decode and validate a value, throws on validation error."
  ([?schema value] (coerce ?schema value nil nil))
  ([?schema value transformer] (coerce ?schema value transformer nil))
  ([?schema value transformer options] (coerce ?schema value transformer nil nil options))
  ([?schema value transformer respond raise] (coerce ?schema value transformer respond raise nil))
  ([?schema value transformer respond raise options] ((coercer ?schema transformer respond raise options) value)))

(defmacro assert
  "Assert that `value` validates against schema `?schema`, or throws ExceptionInfo.
   The var clojure.core/*assert* determines whether assertion are checked."

  ([?schema value]
   `(assert ~?schema ~value nil))

  ([?schema value options]
   (if *assert*
     `(coerce ~?schema ~value nil ~options)
     value)))

(defn entries
  "Returns `EntrySchema` children as a sequence of `clojure.lang/MapEntry`s
   where the values child schemas wrapped in `:malli.core/val` Schemas,
   with the entry properties as properties.

   Using `entries` enable usage of entry properties in walking and value
   transformation.

      (def schema
        [:map
         [:x int?]
         [:y {:optional true} int?]])

      (m/children schema)
      ; [[:x nil int?]
      ;  [:y {:optional true} int?]]

      (m/entries schema)
      ; [[:x [:malli.core/val int?]]
      ;  [:y [:malli.core/val {:optional true} int?]]]

      (map key (m/entries schema))
      ; (:x :y)"
  ([?schema]
   (entries ?schema nil))
  ([?schema options]
   (when-let [schema (schema ?schema options)]
     (when (-entry-schema? schema) (-entries schema)))))

(defn explicit-keys
  "Returns a vector of explicit (not ::m/default) keys from EntrySchema"
  ([?schema] (explicit-keys ?schema nil))
  ([?schema options]
   (let [schema (schema ?schema options)]
     (when (-entry-schema? schema)
       (reduce
        (fn [acc [k :as e]] (cond-> acc (not (-default-entry e)) (conj k)))
        [] (-entries schema))))))

(defn default-schema
  "Returns the default (::m/default) schema from EntrySchema"
  ([?schema] (default-schema ?schema nil))
  ([?schema options]
   (let [schema (schema ?schema options)]
     (when (-entry-schema? schema)
       (-default-entry-schema (-children schema))))))

(defn deref
  "Derefs top-level `RefSchema`s or returns original Schema."
  ([?schema]
   (deref ?schema nil))
  ([?schema options]
   (let [schema (schema ?schema options)]
     (cond-> schema (-ref-schema? schema) (-deref)))))

(defn deref-all
  "Derefs top-level `RefSchema`s recursively or returns original Schema."
  ([?schema]
   (deref-all ?schema nil))
  ([?schema options]
   (let [schema (deref ?schema options)]
     (cond-> schema (-ref-schema? schema) (recur options)))))

(defn deref-recursive
  "Derefs all schemas at all levels. Does not walk over `:ref`s."
  ([?schema]
   (deref-recursive ?schema nil))
  ([?schema {::keys [ref-key] :as options}]
   (let [schema (schema ?schema options)
         maybe-set-ref (fn [s r] (if (and ref-key r) (-update-properties s assoc ref-key r) s))]
     (-> (walk schema (fn [schema _ children _]
                        (cond (= :ref (type schema)) schema
                              (-ref-schema? schema) (maybe-set-ref (deref (-set-children schema children)) (-ref schema))
                              :else (-set-children schema children)))
               {::walk-schema-refs true})
         (deref-all)))))

(defn from-ast
  "Creates a Schema from AST"
  ([?ast] (from-ast ?ast nil))
  ([?ast options]
   (cond
     (schema? ?ast) ?ast
     (map? ?ast) (if-let [s (-lookup (:type ?ast) options)]
                   (let [r (when-let [r (:registry ?ast)] (-delayed-registry r from-ast))
                         options (cond-> options r (-update :registry #(mr/composite-registry r (or % (-registry options)))))
                         ast (cond-> ?ast r (-update :properties #(assoc % :registry (-property-registry r options identity))))]
                     (cond (and (into-schema? s) (-ast? s)) (-from-ast s ast options)
                           (into-schema? s) (-into-schema s (:properties ast) (-vmap #(from-ast % options) (:children ast)) options)
                           :else s))
                   (-fail! ::invalid-ast {:ast ?ast}))
     :else (-fail! ::invalid-ast {:ast ?ast}))))

(defn ast
  "Returns the Schema AST"
  ([?schema] (ast ?schema nil))
  ([?schema options]
   (let [s (schema ?schema options)]
     (if (-ast? s)
       (-to-ast s options)
       (let [c (-children s)]
         (-ast (cond-> {:type (type s)}
                 c (assoc :children (-vmap #(ast % options) c)))
               (-properties s)
               (-options s)))))))
;;
;; eval
;;

(defn -default-sci-options []
  {:preset :termination-safe
   :aliases {'str 'clojure.string
             'm 'malli.core}
   :namespaces {'malli.core {'properties properties
                             'type type
                             'children children
                             'entries entries}}})

(let [-fail! #(-fail! ::sci-not-available {:code %})
      -eval? #(or (symbol? %) (string? %) (sequential? %))
      -evaluator (memoize ms/evaluator)]
  (defn eval
    ([?code] (eval ?code nil))
    ([?code options]
     (cond (vector? ?code) ?code
           (-eval? ?code) (if (::disable-sci options)
                            (-fail! ?code)
                            (((-evaluator (or (::sci-options options) (-default-sci-options)) -fail!)) ?code))
           :else ?code))))

;;
;; schema walker
;;

(defn schema-walker [f]
  (fn [schema _ children _]
    (f (-set-children schema children))))

;;
;; registry
;;

(defn predicate-schemas []
  (let [-safe-empty? (fn [x] (and (seqable? x) (empty? x)))]
    (->> [#'any? #'some? #'number? #'integer? #'int? #'pos-int? #'neg-int? #'nat-int? #'pos? #'neg? #'float? #'double?
          #'boolean? #'string? #'ident? #'simple-ident? #'qualified-ident? #'keyword? #'simple-keyword?
          #'qualified-keyword? #'symbol? #'simple-symbol? #'qualified-symbol? #'uuid? #'uri? #'inst? #'seqable?
          #'indexed? #'map? #'vector? #'list? #'seq? #'char? #'set? #'nil? #'false? #'true?
          #'zero? #'coll? [#'empty? -safe-empty?] #'associative? #'sequential? #'ifn? #'fn?
          #?@(:clj [#'rational? #'ratio? #'bytes? #'decimal?])]
         (reduce -register-var {}))))

(defn class-schemas []
  {#?(:clj  Pattern,
      ;; closure will complain if you reference the global RegExp object.
      :cljs (c/type #"")) (-re-schema true)})

(defn comparator-schemas []
  (->> {:> >, :>= >=, :< <, :<= <=, := =, :not= not=}
       (-vmap (fn [[k v]] [k (-simple-schema {:type k :from-ast -from-value-ast :to-ast -to-value-ast :min 1 :max 1
                                              :compile (fn [_ [child] _] {:pred (-safe-pred #(v % child))})})]))
       (into {}) (reduce-kv assoc nil)))

(defn type-schemas []
  {:any (-any-schema)
   :some (-some-schema)
   :nil (-nil-schema)
   :string (-string-schema)
   :int (-int-schema)
   :float (-float-schema)
   :double (-double-schema)
   :boolean (-boolean-schema)
   :keyword (-keyword-schema)
   :symbol (-symbol-schema)
   :qualified-keyword (-qualified-keyword-schema)
   :qualified-symbol (-qualified-symbol-schema)
   :uuid (-uuid-schema)})

(defn sequence-schemas []
  {:+ (-sequence-schema {:type :+, :child-bounds {:min 1, :max 1}, :keep true
                         :re-validator (fn [_ [child]] (re/+-validator child))
                         :re-explainer (fn [_ [child]] (re/+-explainer child))
                         :re-parser (fn [_ [child]] (re/+-parser child))
                         :re-unparser (fn [_ [child]] (re/+-unparser child))
                         :re-transformer (fn [_ [child]] (re/+-transformer child))
                         :re-min-max (fn [_ [child]] {:min (:min (-regex-min-max child true))})})
   :* (-sequence-schema {:type :*, :child-bounds {:min 1, :max 1}, :keep true
                         :re-validator (fn [_ [child]] (re/*-validator child))
                         :re-explainer (fn [_ [child]] (re/*-explainer child))
                         :re-parser (fn [_ [child]] (re/*-parser child))
                         :re-unparser (fn [_ [child]] (re/*-unparser child))
                         :re-transformer (fn [_ [child]] (re/*-transformer child))
                         :re-min-max (fn [_ _] {:min 0})})
   :? (-sequence-schema {:type :?, :child-bounds {:min 1, :max 1}, :keep true
                         :re-validator (fn [_ [child]] (re/?-validator child))
                         :re-explainer (fn [_ [child]] (re/?-explainer child))
                         :re-parser (fn [_ [child]] (re/?-parser child))
                         :re-unparser (fn [_ [child]] (re/?-unparser child))
                         :re-transformer (fn [_ [child]] (re/?-transformer child))
                         :re-min-max (fn [_ [child]] {:min 0, :max (:max (-regex-min-max child true))})})
   :repeat (-sequence-schema {:type :repeat, :child-bounds {:min 1, :max 1}, :keep true
                              :re-validator (fn [{:keys [min max] :or {min 0, max ##Inf}} [child]] (re/repeat-validator min max child))
                              :re-explainer (fn [{:keys [min max] :or {min 0, max ##Inf}} [child]] (re/repeat-explainer min max child))
                              :re-parser (fn [{:keys [min max] :or {min 0, max ##Inf}} [child]] (re/repeat-parser min max child))
                              :re-unparser (fn [{:keys [min max] :or {min 0, max ##Inf}} [child]] (re/repeat-unparser min max child))
                              :re-transformer (fn [{:keys [min max] :or {min 0, max ##Inf}} [child]] (re/repeat-transformer min max child))
                              :re-min-max (fn [props [child]] (-re-min-max * props child))})
   :cat (-sequence-schema {:type :cat, :child-bounds {}, :keep true
                           :re-validator (fn [_ children] (apply re/cat-validator children))
                           :re-explainer (fn [_ children] (apply re/cat-explainer children))
                           :re-parser (fn [_ children] (apply re/cat-parser children))
                           :re-unparser (fn [_ children] (apply re/cat-unparser children))
                           :re-transformer (fn [_ children] (apply re/cat-transformer children))
                           :re-min-max (fn [_ children] (reduce (partial -re-min-max +) {:min 0, :max 0} children))})
   :alt (-sequence-schema {:type :alt, :child-bounds {:min 1}, :keep true
                           :re-validator (fn [_ children] (apply re/alt-validator children))
                           :re-explainer (fn [_ children] (apply re/alt-explainer children))
                           :re-parser (fn [_ children] (apply re/alt-parser children))
                           :re-unparser (fn [_ children] (apply re/alt-unparser children))
                           :re-transformer (fn [_ children] (apply re/alt-transformer children))
                           :re-min-max (fn [_ children] (reduce -re-alt-min-max {:max 0} children))})
   :catn (-sequence-entry-schema {:type :catn, :child-bounds {}, :keep false
                                  :re-validator (fn [_ children] (apply re/cat-validator children))
                                  :re-explainer (fn [_ children] (apply re/cat-explainer children))
                                  :re-parser (fn [_ children] (apply re/catn-parser tags children))
                                  :re-unparser (fn [_ children] (apply re/catn-unparser tags? children))
                                  :re-transformer (fn [_ children] (apply re/cat-transformer children))
                                  :re-min-max (fn [_ children] (reduce (partial -re-min-max +) {:min 0, :max 0} (-vmap last children)))})
   :altn (-sequence-entry-schema {:type :altn, :child-bounds {:min 1}, :keep false
                                  :re-validator (fn [_ children] (apply re/alt-validator children))
                                  :re-explainer (fn [_ children] (apply re/alt-explainer children))
                                  :re-parser (fn [_ children] (apply re/altn-parser tag children))
                                  :re-unparser (fn [_ children] (apply re/altn-unparser tag? children))
                                  :re-transformer (fn [_ children] (apply re/alt-transformer children))
                                  :re-min-max (fn [_ children] (reduce -re-alt-min-max {:max 0} (-vmap last children)))})})

(defn base-schemas []
  {:and (-and-schema)
   :or (-or-schema)
   :orn (-orn-schema)
   :not (-not-schema)
   :map (-map-schema)
   :map-of (-map-of-schema)
   :vector (-collection-schema {:type :vector, :pred vector?, :empty []})
   :sequential (-collection-schema {:type :sequential, :pred sequential?})
   :seqable (-collection-schema {:type :seqable, :pred seqable?})
   :every (-collection-schema {:type :every, :pred seqable?, :bounded true})
   :set (-collection-schema {:type :set, :pred set?, :empty #{}, :in (fn [_ x] x)})
   :enum (-enum-schema)
   :maybe (-maybe-schema)
   :tuple (-tuple-schema)
   :multi (-multi-schema)
   :re (-re-schema false)
   :fn (-fn-schema)
   :ref (-ref-schema)
   :=> (-=>-schema)
   :-> (-->-schema nil)
   :function (-function-schema nil)
   :schema (-schema-schema nil)
   ::schema (-schema-schema {:raw true})})

(defn default-schemas []
  (merge (predicate-schemas) (class-schemas) (comparator-schemas) (type-schemas) (sequence-schemas) (base-schemas)))

(def default-registry
  (let [strict #?(:cljs (identical? mr/mode "strict")
                  :default (= mr/mode "strict"))
        custom #?(:cljs (identical? mr/type "custom")
                  :default (= mr/type "custom"))
        registry (if custom (mr/fast-registry {}) (mr/composite-registry (mr/fast-registry (default-schemas)) (mr/var-registry)))]
    (when-not strict (mr/set-default-registry! registry))
    (mr/registry (if strict registry (mr/custom-default-registry)))))

;;
;; function schemas
;;

(defonce ^:private -function-schemas* (atom {}))
(defn function-schemas ([] (function-schemas :clj)) ([key] (@-function-schemas* key)))

(defn -deregister-function-schemas! [key] (swap! -function-schemas* assoc key {}))

(defn -deregister-metadata-function-schemas!
  [key]
  (swap! -function-schemas* update key
         (fn [fn-schemas-map]
           (reduce-kv (fn [acc ns-sym fn-map]
                        (assoc acc ns-sym
                               (reduce-kv
                                (fn [acc2 fn-sym fn-map]
                                  ;; rm metadata schemas
                                  (if (:metadata-schema? fn-map)
                                    acc2
                                    (assoc acc2 fn-sym fn-map)))
                                {}
                                fn-map)))
                      {}
                      fn-schemas-map))))

(defn function-schema
  ([?schema] (function-schema ?schema nil))
  ([?schema options]
   (let [s (schema ?schema options)]
     (if (-function-schema? s) s (-fail! ::invalid-=>schema {:type (type s), :schema s})))))

;; for cljs we cannot invoke `function-schema` at macroexpansion-time
;; - `?schema` could contain cljs vars that will only resolve at runtime.
(defn -register-function-schema!
  ([ns name ?schema data] (-register-function-schema! ns name ?schema data :clj function-schema))
  ([ns name ?schema data key f]
   (try
     (swap! -function-schemas* assoc-in [key ns name] (merge data {:schema (f ?schema), :ns ns, :name name}))
     (catch #?(:clj Throwable :cljs :default) ex
       (-fail! ::register-function-schema {:ns ns, :name name, :schema ?schema, :data data, :key key, :exception ex})))))

#?(:clj
   (defmacro => [given-sym value]
     (let [cljs-resolve (when (:ns &env) (ns-resolve 'cljs.analyzer.api 'resolve))
           cljs-resolve-symbols (fn [env d]
                                  (walk/postwalk (fn [x] (cond->> x (symbol? x) (or (:name (cljs-resolve env x)))))
                                                 d))
           name-str (name given-sym)
           ns-str (str (or (not-empty (namespace given-sym)) *ns*))
           name' `'~(symbol name-str)
           ns' `'~(symbol ns-str)
           sym `'~(symbol ns-str name-str)
           value' (cond->> value (:ns &env) (cljs-resolve-symbols &env))]
       ;; in cljs we need to register the schema in clojure (the cljs compiler)
       ;; so it is visible in the (function-schemas :cljs) map at macroexpansion time.
       (if (:ns &env)
         (do
           (-register-function-schema! (symbol ns-str) (symbol name-str) value' (meta given-sym) :cljs identity)
           `(do (-register-function-schema! ~ns' ~name' ~value' ~(meta given-sym) :cljs identity) ~sym))
         `(do (-register-function-schema! ~ns' ~name' ~value' ~(meta given-sym)) ~sym)))))

(defn -instrument
  "Takes an instrumentation properties map and a function and returns a wrapped function,
   which will validate function arguments and return values based on the function schema
   definition. The following properties are used:

   | key       | description |
   | ----------|-------------|
   | `:schema` | function schema
   | `:scope`  | optional set of scope definitions, defaults to `#{:input :output :guard}`
   | `:report` | optional side-effecting function of `key data -> any` to report problems, defaults to `m/-fail!`
   | `:gen`    | optional function of `schema -> schema -> value` to be invoked on the args to get the return value"
  ([props]
   (-instrument props nil nil))
  ([props f]
   (-instrument props f nil))
  ([props f options]
   (let [props (-> props
                   (update :scope #(or % #{:input :output :guard}))
                   (update :report #(or % -fail!)))
         s (-> props :schema (schema options))]
     (or (-instrument-f s props f options)
         (-fail! ::instrument-requires-function-schema {:schema s})))))
(ns messenger-clj.typed-store
  "Typed Datalevin authority for the Clojure messenger. EDN is exposed only as
  an explicit import/export representation."
  (:require [babashka.fs :as fs]
            [babashka.pods :as pods]
            [clojure.edn :as edn]
            [malli.core :as m]))

(def pod-version "0.8.25")
(if-let [executable (System/getenv "MESSENGER_CLJ_DATALEVIN_POD")]
  (pods/load-pod executable)
  (pods/load-pod 'huahaiy/datalevin pod-version))
(require 'pod.huahaiy.datalevin)

(defn- call [symbol & args]
  (apply (or (resolve symbol)
             (throw (ex-info (str "Datalevin pod lacks " symbol) {})))
         args))

(def ReadinessProof
  [:map {:closed true}
   [:thread_id :string] [:rollout :string] [:marker :string]
   [:evidence_kind {:optional true} :string]])
(def Route
  [:map {:closed true}
   [:session :string] [:name :string] [:pane_id :string] [:terminal_id :string]
   [:agent :string] [:native_thread {:optional true} :string]
   [:readiness_proof {:optional true} ReadinessProof]
   [:route_hold {:optional true} :string]
   [:transition {:optional true} :boolean]
   [:state :string]])
(def AttemptBinding
  [:map {:closed true}
   [:session {:optional true} :string] [:name {:optional true} :string]
   [:pane_id {:optional true} :string] [:terminal_id {:optional true} :string]
   [:agent {:optional true} :string] [:native_thread {:optional true} :string]
   [:readiness_proof {:optional true} ReadinessProof]
   [:route_hold {:optional true} :string] [:transition {:optional true} :boolean]
   [:state {:optional true} :string]])
(def Attempt
  [:map {:closed true}
   [:id :string] [:flow :string] [:at :string] [:grade {:optional true} :keyword] [:reason :keyword]
   [:variant {:optional true} [:enum :msg :psyche]]
   [:context {:optional true} :string] [:body {:optional true} :string]
   [:submitted {:optional true} :string] [:binding {:optional true} AttemptBinding]])
(def Pending
  [:map {:closed true}
   [:attempt Attempt] [:message :string] [:variant {:optional true} [:enum :msg :psyche]]
   [:context {:optional true} :string] [:state [:= "held"]]])
(def RouteIdentity
  [:map {:closed true}
   [:session :string] [:name :string] [:pane_id :string] [:terminal_id :string] [:agent :string]])
(def Evidence
  [:map {:closed true} [:path :string] [:sha256 :string]])
(def Retirement
  [:map {:closed true}
   [:version [:= 1]] [:state [:= "retired"]] [:flow :string]
   [:record RouteIdentity] [:native_thread :string] [:evidence Evidence]
   [:retired_by :string] [:retired_at :string]])

(def schema
  {:flow/id {:db/unique :db.unique/identity}
   :route/flow {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one
                :db/unique :db.unique/identity}
   :route/session {} :route/name {} :route/pane {} :route/terminal {}
   :route/agent {} :route/thread {} :route/hold {} :route/transition {} :route/state {}
   :route/readiness-thread {} :route/readiness-rollout {} :route/readiness-marker {}
   :route/readiness-kind {}
   :attempt/id {:db/unique :db.unique/identity}
   :attempt/flow {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   :attempt/at {} :attempt/grade {} :attempt/reason {} :attempt/variant {}
   :attempt/context {} :attempt/body {} :attempt/submitted {}
   :attempt.binding/session {} :attempt.binding/name {} :attempt.binding/pane {}
   :attempt.binding/terminal {} :attempt.binding/agent {} :attempt.binding/thread {}
   :attempt.binding/hold {} :attempt.binding/transition {} :attempt.binding/state {}
   :attempt.binding/readiness-thread {} :attempt.binding/readiness-rollout {}
   :attempt.binding/readiness-marker {} :attempt.binding/readiness-kind {}
   :pending/id {:db/unique :db.unique/identity}
   :pending/attempt {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one}
   :pending/message {} :pending/variant {} :pending/context {} :pending/state {}
   :retirement/flow {:db/valueType :db.type/ref :db/cardinality :db.cardinality/one
                     :db/unique :db.unique/identity}
   :retirement/version {} :retirement/state {} :retirement/session {}
   :retirement/name {} :retirement/pane {} :retirement/terminal {} :retirement/agent {}
   :retirement/thread {} :retirement/evidence-path {} :retirement/evidence-sha256 {}
   :retirement/retired-by {} :retirement/at {}})

(defn valid! [entity-schema value]
  (if (m/validate entity-schema value)
    value
    (throw (ex-info "Invalid typed store entity"
                    {:value value :explanation (m/explain entity-schema value)}))))

(defn route! [route]
  (valid! Route
          (assoc route :state
                 (or (:state route)
                     (if (:native_thread route) "Bound" "NeedsBinding")))))
(defn attempt! [attempt] (valid! Attempt attempt))
(defn pending! [pending] (valid! Pending (update pending :attempt attempt!)))
(defn retirement! [retirement] (valid! Retirement retirement))

(defn database-path [root] (str (fs/path root "typed-datalevin")))
(defn with-db [root f]
  (let [conn (call 'pod.huahaiy.datalevin/get-conn (database-path root) schema)]
    (try (f conn) (finally (call 'pod.huahaiy.datalevin/close conn)))))
(defn transact! [root tx] (with-db root #(call 'pod.huahaiy.datalevin/transact! % tx)))
(defn query [root form & inputs]
  (with-db root
    #(apply call 'pod.huahaiy.datalevin/q
            form (call 'pod.huahaiy.datalevin/db %) inputs)))

(defn checked-rows! [rows width]
  (when-not (and (or (set? rows) (sequential? rows))
                 (every? #(and (vector? %) (= width (count %))) rows))
    (throw (ex-info "Malformed typed Datalevin query row" {:rows rows :width width})))
  rows)
(defn- one! [rows kind identity]
  (let [rows (checked-rows! rows 1)]
    (when (> (count rows) 1)
      (throw (ex-info (str "Typed " kind " is not unique") {:identity identity :rows rows})))
    (ffirst rows)))
(defn- assoc-present [m k v] (if (nil? v) m (assoc m k v)))

(defn- route-attrs [prefix route]
  (let [route (route! route)
        proof (:readiness_proof route)]
    (-> {}
        (assoc (keyword prefix "session") (:session route)
               (keyword prefix "name") (:name route)
               (keyword prefix "pane") (:pane_id route)
               (keyword prefix "terminal") (:terminal_id route)
               (keyword prefix "agent") (:agent route)
               (keyword prefix "state") (:state route))
        (assoc-present (keyword prefix "thread") (:native_thread route))
        (assoc-present (keyword prefix "hold") (:route_hold route))
        (assoc-present (keyword prefix "transition") (:transition route))
        (assoc-present (keyword prefix "readiness-thread") (:thread_id proof))
        (assoc-present (keyword prefix "readiness-rollout") (:rollout proof))
        (assoc-present (keyword prefix "readiness-marker") (:marker proof))
        (assoc-present (keyword prefix "readiness-kind") (:evidence_kind proof)))))

(defn- attrs-route! [prefix entity]
  (let [getv #(get entity (keyword prefix %))
        proof (when-let [thread (getv "readiness-thread")]
                (cond-> {:thread_id thread
                         :rollout (getv "readiness-rollout")
                         :marker (getv "readiness-marker")}
                  (getv "readiness-kind") (assoc :evidence_kind (getv "readiness-kind"))))]
    (route!
     (cond-> {:session (getv "session") :name (getv "name")
              :pane_id (getv "pane") :terminal_id (getv "terminal")
              :agent (getv "agent") :state (getv "state")}
       (getv "thread") (assoc :native_thread (getv "thread"))
       (getv "hold") (assoc :route_hold (getv "hold"))
       (some? (getv "transition")) (assoc :transition (getv "transition"))
       proof (assoc :readiness_proof proof)))))

(defn- binding-attrs [binding]
  (let [proof (:readiness_proof binding)]
    (-> {}
        (assoc-present :attempt.binding/session (:session binding))
        (assoc-present :attempt.binding/name (:name binding))
        (assoc-present :attempt.binding/pane (:pane_id binding))
        (assoc-present :attempt.binding/terminal (:terminal_id binding))
        (assoc-present :attempt.binding/agent (:agent binding))
        (assoc-present :attempt.binding/thread (:native_thread binding))
        (assoc-present :attempt.binding/hold (:route_hold binding))
        (assoc-present :attempt.binding/transition (:transition binding))
        (assoc-present :attempt.binding/state (:state binding))
        (assoc-present :attempt.binding/readiness-thread (:thread_id proof))
        (assoc-present :attempt.binding/readiness-rollout (:rollout proof))
        (assoc-present :attempt.binding/readiness-marker (:marker proof))
        (assoc-present :attempt.binding/readiness-kind (:evidence_kind proof)))))
(defn- attrs-binding! [entity]
  (let [proof (when-let [thread (:attempt.binding/readiness-thread entity)]
                (cond-> {:thread_id thread
                         :rollout (:attempt.binding/readiness-rollout entity)
                         :marker (:attempt.binding/readiness-marker entity)}
                  (:attempt.binding/readiness-kind entity)
                  (assoc :evidence_kind (:attempt.binding/readiness-kind entity))))]
    (valid! AttemptBinding
            (cond-> {}
              (:attempt.binding/session entity) (assoc :session (:attempt.binding/session entity))
              (:attempt.binding/name entity) (assoc :name (:attempt.binding/name entity))
              (:attempt.binding/pane entity) (assoc :pane_id (:attempt.binding/pane entity))
              (:attempt.binding/terminal entity) (assoc :terminal_id (:attempt.binding/terminal entity))
              (:attempt.binding/agent entity) (assoc :agent (:attempt.binding/agent entity))
              (:attempt.binding/thread entity) (assoc :native_thread (:attempt.binding/thread entity))
              (:attempt.binding/hold entity) (assoc :route_hold (:attempt.binding/hold entity))
              (some? (:attempt.binding/transition entity))
              (assoc :transition (:attempt.binding/transition entity))
              (:attempt.binding/state entity) (assoc :state (:attempt.binding/state entity))
              proof (assoc :readiness_proof proof)))))

(def route-pull
  [:route/session :route/name :route/pane :route/terminal :route/agent :route/thread
   :route/hold :route/transition :route/state :route/readiness-thread
   :route/readiness-rollout :route/readiness-marker :route/readiness-kind
   {:route/flow [:flow/id]}])
(def attempt-pull
  [:attempt/id :attempt/at :attempt/grade :attempt/reason :attempt/variant
   :attempt/context :attempt/body :attempt/submitted
   :attempt.binding/session :attempt.binding/name :attempt.binding/pane
   :attempt.binding/terminal :attempt.binding/agent :attempt.binding/thread
   :attempt.binding/hold :attempt.binding/transition :attempt.binding/state
   :attempt.binding/readiness-thread :attempt.binding/readiness-rollout
   :attempt.binding/readiness-marker :attempt.binding/readiness-kind
   {:attempt/flow [:flow/id]}])
(def pending-pull
  [:pending/message :pending/variant :pending/context :pending/state
   {:pending/attempt attempt-pull}])
(def retirement-pull
  [:retirement/version :retirement/state :retirement/session :retirement/name
   :retirement/pane :retirement/terminal :retirement/agent :retirement/thread
   :retirement/evidence-path :retirement/evidence-sha256
   :retirement/retired-by :retirement/at {:retirement/flow [:flow/id]}])

(defn put-route! [root flow route]
  (let [route (route! route)
        current (some-> (query root '[:find (pull ?route [*])
                                      :in $ ?flow
                                      :where [?f :flow/id ?flow] [?route :route/flow ?f]]
                               flow)
                        (one! "route" flow))
        entity-id (:db/id current)
        replacement (cond-> (assoc (route-attrs "route" route)
                                   :route/flow [:flow/id flow])
                      entity-id (assoc :db/id entity-id))
        retractions (for [[attribute value] current
                          :when (and (keyword? attribute)
                                     (= "route" (namespace attribute))
                                     (not= attribute :route/flow)
                                     (not (contains? replacement attribute)))]
                      [:db/retract entity-id attribute value])]
    (transact! root (vec (concat retractions [{:flow/id flow} replacement])))
    route))
(defn- pulled-route! [entity]
  (when-not (string? (get-in entity [:route/flow :flow/id]))
    (throw (ex-info "Malformed route flow reference" {:entity entity})))
  (attrs-route! "route" entity))
(defn stored-route-for [root flow]
  (some-> (query root '[:find (pull ?route ?pattern)
                        :in $ ?flow ?pattern
                        :where [?f :flow/id ?flow] [?route :route/flow ?f]]
                 flow route-pull)
          (one! "route" flow) pulled-route!))

(declare retirement-for)
(defn route-for [root flow]
  (when-not (retirement-for root flow) (stored-route-for root flow)))
(defn routes [root]
  (let [rows (checked-rows!
              (query root '[:find ?flow (pull ?route ?pattern)
                            :in $ ?pattern
                            :where [?f :flow/id ?flow] [?route :route/flow ?f]]
                     route-pull)
              2)]
    (into {} (map (fn [[flow entity]] [flow (pulled-route! entity)]) rows))))
(defn remove-route! [root flow]
  (when-let [entity-id (query root '[:find ?route . :in $ ?flow
                                     :where [?f :flow/id ?flow] [?route :route/flow ?f]]
                              flow)]
    (transact! root [[:db/retractEntity entity-id]]))
  nil)

(defn- attempt-tx [attempt]
  (let [attempt (attempt! attempt)]
    [{:flow/id (:flow attempt)}
     (cond-> {:attempt/id (:id attempt) :attempt/flow [:flow/id (:flow attempt)]
              :attempt/at (:at attempt) :attempt/reason (:reason attempt)}
       (:grade attempt) (assoc :attempt/grade (:grade attempt))
       (:variant attempt) (assoc :attempt/variant (:variant attempt))
       (:context attempt) (assoc :attempt/context (:context attempt))
       (:body attempt) (assoc :attempt/body (:body attempt))
       (:submitted attempt) (assoc :attempt/submitted (:submitted attempt))
       (:binding attempt) (merge (binding-attrs (:binding attempt))))]))
(defn put-attempt! [root attempt]
  (let [attempt (attempt! attempt)] (transact! root (attempt-tx attempt)) attempt))
(defn- pulled-attempt! [entity]
  (let [flow (get-in entity [:attempt/flow :flow/id])]
    (when-not (string? flow)
      (throw (ex-info "Malformed attempt flow reference" {:entity entity})))
    (attempt!
     (cond-> {:id (:attempt/id entity) :flow flow :at (:attempt/at entity)
              :reason (:attempt/reason entity)}
       (:attempt/grade entity) (assoc :grade (:attempt/grade entity))
       (:attempt/variant entity) (assoc :variant (:attempt/variant entity))
       (:attempt/context entity) (assoc :context (:attempt/context entity))
       (:attempt/body entity) (assoc :body (:attempt/body entity))
       (:attempt/submitted entity) (assoc :submitted (:attempt/submitted entity))
       (some #(contains? entity %)
             [:attempt.binding/session :attempt.binding/name :attempt.binding/pane
              :attempt.binding/terminal :attempt.binding/agent :attempt.binding/thread])
       (assoc :binding (attrs-binding! entity))))))
(defn attempt-by-id [root id]
  (some-> (query root '[:find (pull ?attempt ?pattern)
                        :in $ ?id ?pattern :where [?attempt :attempt/id ?id]]
                 id attempt-pull)
          (one! "attempt" id) pulled-attempt!))
(defn attempts-for [root flow]
  (->> (checked-rows!
        (query root '[:find (pull ?attempt ?pattern)
                      :in $ ?flow ?pattern
                      :where [?f :flow/id ?flow] [?attempt :attempt/flow ?f]]
               flow attempt-pull)
        1)
       (map (comp pulled-attempt! first)) (sort-by :id) vec))

(defn put-pending! [root pending]
  (let [pending (pending! pending) attempt (:attempt pending)]
    (when-not (attempt-by-id root (:id attempt))
      (throw (ex-info "Pending intent requires a persisted attempt" {:attempt (:id attempt)})))
    (transact! root [(cond-> {:pending/id (:id attempt)
                              :pending/attempt [:attempt/id (:id attempt)]
                              :pending/message (:message pending)
                              :pending/state (:state pending)}
                       (:variant pending) (assoc :pending/variant (:variant pending))
                       (:context pending) (assoc :pending/context (:context pending)))])
    pending))
(defn- pulled-pending! [entity]
  (when-not (map? (:pending/attempt entity))
    (throw (ex-info "Malformed pending attempt reference" {:entity entity})))
  (pending! (cond-> {:attempt (pulled-attempt! (:pending/attempt entity))
                     :message (:pending/message entity) :state (:pending/state entity)}
              (:pending/variant entity) (assoc :variant (:pending/variant entity))
              (:pending/context entity) (assoc :context (:pending/context entity)))))
(defn pending-by-id [root id]
  (some-> (query root '[:find (pull ?pending ?pattern)
                        :in $ ?id ?pattern :where [?pending :pending/id ?id]]
                 id pending-pull)
          (one! "pending intent" id) pulled-pending!))
(defn pending-for [root flow]
  (->> (checked-rows!
        (query root '[:find (pull ?pending ?pattern)
                      :in $ ?flow ?pattern
                      :where [?f :flow/id ?flow] [?attempt :attempt/flow ?f]
                      [?pending :pending/attempt ?attempt]]
               flow pending-pull)
        1)
       (map (comp pulled-pending! first)) (sort-by #(get-in % [:attempt :id])) vec))

(defn put-retirement! [root retirement]
  (let [retirement (retirement! retirement) record (:record retirement)
        evidence (:evidence retirement)]
    (transact! root
               [{:flow/id (:flow retirement)}
                {:retirement/flow [:flow/id (:flow retirement)]
                 :retirement/version (:version retirement) :retirement/state (:state retirement)
                 :retirement/session (:session record) :retirement/name (:name record)
                 :retirement/pane (:pane_id record) :retirement/terminal (:terminal_id record)
                 :retirement/agent (:agent record) :retirement/thread (:native_thread retirement)
                 :retirement/evidence-path (:path evidence)
                 :retirement/evidence-sha256 (:sha256 evidence)
                 :retirement/retired-by (:retired_by retirement)
                 :retirement/at (:retired_at retirement)}])
    retirement))
(defn- pulled-retirement! [entity]
  (let [flow (get-in entity [:retirement/flow :flow/id])]
    (when-not (string? flow)
      (throw (ex-info "Malformed retirement flow reference" {:entity entity})))
    (retirement!
     {:version (:retirement/version entity) :state (:retirement/state entity) :flow flow
      :record {:session (:retirement/session entity) :name (:retirement/name entity)
               :pane_id (:retirement/pane entity) :terminal_id (:retirement/terminal entity)
               :agent (:retirement/agent entity)}
      :native_thread (:retirement/thread entity)
      :evidence {:path (:retirement/evidence-path entity)
                 :sha256 (:retirement/evidence-sha256 entity)}
      :retired_by (:retirement/retired-by entity) :retired_at (:retirement/at entity)})))
(defn retirement-for [root flow]
  (some-> (query root '[:find (pull ?retirement ?pattern)
                        :in $ ?flow ?pattern
                        :where [?f :flow/id ?flow] [?retirement :retirement/flow ?f]]
                 flow retirement-pull)
          (one! "retirement" flow) pulled-retirement!))
(defn retirements [root]
  (->> (checked-rows!
        (query root '[:find (pull ?retirement ?pattern)
                      :in $ ?pattern :where [?retirement :retirement/flow]]
               retirement-pull)
        1)
       (map (comp pulled-retirement! first)) (sort-by :flow) vec))

(defn- boundary-entity! [entity]
  (when-not (map? entity)
    (throw (ex-info "Typed boundary entity must be a map" {:value entity})))
  (case (:entity/type entity)
    :route (do
             (when-not (string? (:entity/flow entity))
               (throw (ex-info "Route export requires its Flow ID" {:value entity})))
             (assoc entity :entity/value (route! (:entity/value entity))))
    :attempt (assoc entity :entity/value (attempt! (:entity/value entity)))
    :pending (assoc entity :entity/value (pending! (:entity/value entity)))
    :retirement (assoc entity :entity/value (retirement! (:entity/value entity)))
    (throw (ex-info "Unknown typed boundary entity" {:value entity}))))
(defn export-edn [entities]
  (when-not (sequential? entities)
    (throw (ex-info "Typed export must be a sequence" {:value entities})))
  (pr-str (mapv boundary-entity! entities)))
(defn import-edn [text]
  (when-not (string? text)
    (throw (ex-info "Typed import must be EDN text" {:value text})))
  (let [entities (edn/read-string text)]
    (when-not (sequential? entities)
      (throw (ex-info "Typed import must contain a sequential EDN value" {:value entities})))
    (mapv boundary-entity! entities)))
(ns messenger-clj.legacy-import
  "Offline, one-way conversion of the legacy Python JSON registry into the
  typed Datalevin registry. Source bytes are read once, validated completely,
  and represented by a digest manifest before an optional apply."
  (:import [java.nio.file Files]
           [java.nio.file.attribute PosixFilePermissions])
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.set :as set]
            [clojure.string :as str]
            [messenger-clj.typed-store :as store]))

(def flow-pattern #"[A-Za-z0-9][A-Za-z0-9_-]{0,95}")
(def attempt-pattern #"[0-9a-f]{32}")
(def thread-pattern #"[A-Za-z0-9-]{16,96}")
(def digest-pattern #"[0-9a-f]{64}")
(def reasons #{"NotRegistered" "NeedsBinding" "InTransition" "RouteHold"
               "IdentityChanged" "PaneMissing" "NotReady" "Blocked"
               "ProcessMismatch" "Stalled" "Uncertain" "RelayOverflow"
               "Submitting" "sent"})
(def grades #{"Transported" "Presented" "Fallback-Presented" "Held" "Uncertain"})

(defn fail! [message path]
  (throw (ex-info (str message (when path (str " in " path)))
                  {:hm/import true :path path})))
(defn exact-map! [value required optional label path]
  (when-not (map? value) (fail! (str label " must be a JSON object") path))
  (let [actual (set (keys value))
        allowed (set/union required optional)]
    (when-let [missing (seq (set/difference required actual))]
      (fail! (str label " is missing fields " (pr-str (sort missing))) path))
    (when-let [unknown (seq (set/difference actual allowed))]
      (fail! (str label " has unknown fields " (pr-str (sort unknown))) path)))
  value)
(defn string! [value label path]
  (when-not (and (string? value) (not (str/blank? value)))
    (fail! (str label " must be a nonempty string") path))
  value)
(defn string-value! [value label path]
  (when-not (string? value) (fail! (str label " must be a string") path))
  value)
(defn pattern! [value pattern label path]
  (string! value label path)
  (when-not (re-matches pattern value) (fail! (str "Malformed " label) path))
  value)
(defn flow! [value path] (pattern! value flow-pattern "Flow ID" path))
(defn attempt-id! [value path] (pattern! value attempt-pattern "attempt ID" path))
(defn thread! [value path] (pattern! value thread-pattern "native thread" path))
(defn timestamp! [value path]
  (string! value "UTC timestamp" path)
  (when-not (str/ends-with? value "Z") (fail! "Timestamp must be UTC" path))
  (try (java.time.Instant/parse value)
       (catch Exception _ (fail! "Malformed UTC timestamp" path)))
  value)
(defn sha256-bytes [bytes]
  (apply str (map #(format "%02x" (bit-and % 0xff))
                  (.digest (doto (java.security.MessageDigest/getInstance "SHA-256")
                             (.update bytes))))))
(defn sha256-file [path] (sha256-bytes (fs/read-all-bytes path)))

(defn json! [bytes path]
  (try
    (json/parse-string (String. bytes java.nio.charset.StandardCharsets/UTF_8) true)
    (catch Exception _ (fail! "Malformed JSON" path))))

(defn readiness! [value path]
  (exact-map! value #{:thread_id :rollout :marker} #{:evidence_kind} "readiness proof" path)
  (let [result {:thread_id (thread! (:thread_id value) path)
                :rollout (string! (:rollout value) "readiness rollout" path)
                :marker (string! (:marker value) "readiness marker" path)}]
    (cond-> result
      (contains? value :evidence_kind)
      (assoc :evidence_kind (string! (:evidence_kind value) "readiness evidence kind" path)))))

(defn route! [flow value path]
  (flow! flow path)
  (exact-map! value #{:session :name :pane_id :terminal_id :agent}
              #{:native_thread :readiness_proof :route_hold :transition :state}
              "route" path)
  (let [route {:session (string! (:session value) "route session" path)
               :name (string! (:name value) "route name" path)
               :pane_id (string! (:pane_id value) "route pane" path)
               :terminal_id (string! (:terminal_id value) "route terminal" path)
               :agent (string! (:agent value) "route agent" path)}]
    (store/route!
     (let [result (cond-> route
                    (contains? value :native_thread) (assoc :native_thread (thread! (:native_thread value) path))
                    (contains? value :readiness_proof) (assoc :readiness_proof (readiness! (:readiness_proof value) path))
                    (contains? value :route_hold) (assoc :route_hold (string! (:route_hold value) "route hold" path))
                    (contains? value :transition) (assoc :transition (if (boolean? (:transition value))
                                                                       (:transition value)
                                                                       (fail! "Route transition must be boolean" path)))
                    (contains? value :state) (assoc :state (string! (:state value) "route state" path)))]
       (when (and (:readiness_proof result)
                  (not= (:native_thread result) (get-in result [:readiness_proof :thread_id])))
         (fail! "Readiness proof conflicts with route native thread" path))
       result))))

(defn binding! [value path]
  (exact-map! value #{} #{:pane_id :terminal_id :native_thread} "attempt binding" path)
  (cond-> {}
    (contains? value :pane_id) (assoc :pane_id (string! (:pane_id value) "binding pane" path))
    (contains? value :terminal_id) (assoc :terminal_id (string! (:terminal_id value) "binding terminal" path))
    (contains? value :native_thread) (assoc :native_thread (thread! (:native_thread value) path))))

(def attempt-fields #{:id :at :flow :reason :grade :binding})
(defn attempt! [value path]
  (exact-map! value attempt-fields #{} "attempt" path)
  (when-not (contains? reasons (:reason value)) (fail! "Unknown attempt reason or case" path))
  (when-not (or (nil? (:grade value)) (contains? grades (:grade value)))
    (fail! "Unknown attempt grade or case" path))
  (when-not (or (nil? (:binding value)) (map? (:binding value)))
    (fail! "Attempt binding must be null or an object" path))
  (store/attempt!
   (cond-> {:id (attempt-id! (:id value) path)
            :at (timestamp! (:at value) path)
            :flow (flow! (:flow value) path)
            :reason (keyword (:reason value))}
     (:grade value) (assoc :grade (keyword (:grade value)))
     (:binding value) (assoc :binding (binding! (:binding value) path)))))

(defn retirement! [flow value path]
  (flow! flow path)
  (exact-map! value #{:version :state :flow :record :native_thread :evidence :retired_by :retired_at}
              #{} "retirement" path)
  (when-not (and (= 1 (:version value)) (= "retired" (:state value)) (= flow (:flow value)))
    (fail! "Retirement header does not match its file" path))
  (exact-map! (:record value) #{:session :name :pane_id :terminal_id :agent} #{} "retirement record" path)
  (exact-map! (:evidence value) #{:path :sha256} #{} "retirement evidence" path)
  (let [evidence-path (string! (get-in value [:evidence :path]) "evidence path" path)
        expected (pattern! (get-in value [:evidence :sha256]) digest-pattern "evidence SHA-256" path)]
    (when-not (fs/absolute? evidence-path) (fail! "Retirement evidence path must be absolute" path))
    (when-not (fs/regular-file? evidence-path) (fail! "Retirement evidence file is unavailable" path))
    (when-not (= expected (sha256-file evidence-path)) (fail! "Retirement evidence SHA-256 mismatch" path))
    (store/retirement!
     {:version 1 :state "retired" :flow flow
      :record (into {} (map (fn [[key label]] [key (string! (get-in value [:record key]) label path)]))
                    [[:session "retirement session"] [:name "retirement name"]
                     [:pane_id "retirement pane"] [:terminal_id "retirement terminal"]
                     [:agent "retirement agent"]])
      :native_thread (thread! (:native_thread value) path)
      :evidence {:path evidence-path :sha256 expected}
      :retired_by (string-value! (:retired_by value) "retired_by" path)
      :retired_at (timestamp! (:retired_at value) path)})))

(defn source-paths [root]
  (let [root (fs/absolutize root)]
    (when-not (fs/directory? root) (fail! "Source registry directory is unavailable" (str root)))
    (->> (concat (fs/glob root "*.json")
                 (let [attempts (fs/path root "attempts.jsonl")]
                   (when (fs/regular-file? attempts) [attempts]))
                 (when (fs/directory? (fs/path root "pending")) (fs/glob root "pending/*.json"))
                 (when (fs/directory? (fs/path root "retired")) (fs/glob root "retired/*.json")))
         (filter fs/regular-file?)
         (sort-by str)
         vec)))
(defn read-snapshot [root]
  (let [root (fs/absolutize root)
        files (mapv (fn [path]
                      (let [bytes (fs/read-all-bytes path)]
                        {:path path :relative (str (fs/relativize root path))
                         :bytes bytes :size (alength bytes) :sha256 (sha256-bytes bytes)}))
                    (source-paths root))
        digest (java.security.MessageDigest/getInstance "SHA-256")]
    (doseq [{:keys [relative bytes]} files]
      (.update digest (.getBytes relative java.nio.charset.StandardCharsets/UTF_8))
      (.update digest (byte-array [0]))
      (.update digest bytes)
      (.update digest (byte-array [0])))
    {:root (str root) :digest (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest digest)))
     :files files}))

(defn duplicate! [kind ids]
  (when-let [id (->> ids frequencies (filter #(> (val %) 1)) ffirst)]
    (fail! (str "Duplicate " kind " " id) nil)))
(defn route-conflicts! [routes]
  (doseq [[label key-fn] [["session/name" #(select-keys (val %) [:session :name])]
                          ["session/pane" #(select-keys (val %) [:session :pane_id])]
                          ["session/terminal" #(select-keys (val %) [:session :terminal_id])]]]
    (when-let [[identity rows] (first (filter #(> (count (val %)) 1) (group-by key-fn routes)))]
      (fail! (str "Conflicting route " label " " (pr-str identity)
                  " for flows " (pr-str (sort (map key rows)))) nil))))
(defn native-conflicts! [routes retirements]
  (let [bindings (concat (keep (fn [[flow route]]
                                 (when-let [thread (:native_thread route)] [flow thread]))
                               routes)
                         (map (fn [[flow retirement]] [flow (:native_thread retirement)]) retirements))]
    (when-let [[thread rows] (first (filter #(> (count (set (map first (val %)))) 1)
                                            (group-by second bindings)))]
      (fail! (str "Conflicting native thread " thread " for flows "
                  (pr-str (sort (set (map first rows))))) nil))))

(defn parse-snapshot [snapshot]
  (let [files (:files snapshot)
        route-files (filter #(re-matches #"[^/]+\.json" (:relative %)) files)
        attempt-file (first (filter #(= "attempts.jsonl" (:relative %)) files))
        pending-files (filter #(str/starts-with? (:relative %) "pending/") files)
        retirement-files (filter #(str/starts-with? (:relative %) "retired/") files)
        routes (into (sorted-map)
                     (map (fn [{:keys [relative bytes]}]
                            (let [flow (subs relative 0 (- (count relative) 5))]
                              [flow (route! flow (json! bytes relative) relative)])))
                     route-files)
        raw-attempts (if attempt-file
                       (->> (str/split-lines (String. (:bytes attempt-file) java.nio.charset.StandardCharsets/UTF_8))
                            (remove str/blank?)
                            (map-indexed (fn [index line]
                                           (attempt! (json! (.getBytes line java.nio.charset.StandardCharsets/UTF_8)
                                                            (str "attempts.jsonl:" (inc index)))
                                                     (str "attempts.jsonl:" (inc index)))))
                            vec)
                       [])
        _ (duplicate! "attempt ID" (map :id raw-attempts))
        attempt-map (into {} (map (juxt :id identity) raw-attempts))
        pending-rows (mapv (fn [{:keys [relative bytes]}]
                             (let [value (json! bytes relative)]
                               (exact-map! value (conj attempt-fields :message :state) #{} "pending intent" relative)
                               (let [embedded (attempt! (select-keys value attempt-fields) relative)
                                     id (:id embedded)
                                     filename (fs/file-name relative)
                                     source (get attempt-map id)]
                                 (when-not (= filename (str id ".json")) (fail! "Pending filename does not match attempt ID" relative))
                                 (when-not source (fail! (str "Dangling pending attempt " id) relative))
                                 (when-not (= source embedded) (fail! (str "Pending attempt " id " conflicts with attempts.jsonl") relative))
                                 (when-not (= "held" (:state value)) (fail! "Pending state must be held" relative))
                                 (let [body (string! (:message value) "pending message" relative)
                                       enriched (store/attempt! (assoc source :body body))]
                                   {:id id :attempt enriched
                                    :pending (store/pending! {:attempt enriched :message body :state "held"})}))))
                           pending-files)
        _ (duplicate! "pending attempt ID" (map :id pending-rows))
        pending-by-id (into {} (map (juxt :id identity) pending-rows))
        attempts (mapv #(or (get-in pending-by-id [(:id %) :attempt]) %) raw-attempts)
        retirements (into (sorted-map)
                          (map (fn [{:keys [relative bytes]}]
                                 (let [filename (fs/file-name relative)
                                       flow (subs filename 0 (- (count filename) 5))]
                                   [flow (retirement! flow (json! bytes relative) relative)])))
                          retirement-files)
        _ (duplicate! "retirement Flow" (keys retirements))
        overlaps (vec (sort (set/intersection (set (keys routes)) (set (keys retirements)))))
        effective-routes (apply dissoc routes overlaps)
        _ (route-conflicts! effective-routes)
        _ (native-conflicts! effective-routes retirements)
        referenced-flows (set (concat (map :flow attempts) (keys routes) (keys retirements)))
        anchored-flows (set (concat (keys effective-routes) (keys retirements)))
        orphan-flows (vec (sort (set/difference referenced-flows anchored-flows)))
        empty-retired-by (vec (sort (keep (fn [[flow retirement]]
                                            (when (empty? (:retired_by retirement)) flow))
                                          retirements)))
        entities (vec (concat (map (fn [[flow route]] {:entity/type :route :entity/flow flow :entity/value route}) effective-routes)
                              (map (fn [attempt] {:entity/type :attempt :entity/value attempt}) attempts)
                              (map (fn [{:keys [pending]}] {:entity/type :pending :entity/value pending}) pending-rows)
                              (map (fn [[_ retirement]] {:entity/type :retirement :entity/value retirement}) retirements)))]
    {:routes routes :effective-routes effective-routes :attempts attempts
     :pending (mapv :pending pending-rows) :retirements retirements
     :overlaps overlaps :orphan-flows orphan-flows :empty-retired-by empty-retired-by
     :entities (store/import-edn (store/export-edn entities))}))

(defn receipt! [value]
  (exact-map! value #{:receipt/version :source/snapshot :import/report :export/entities} #{} "receipt" nil)
  (when-not (= 1 (:receipt/version value)) (fail! "Unsupported receipt version" nil))
  (let [snapshot (:source/snapshot value)
        report (:import/report value)]
    (exact-map! snapshot #{:root :sha256 :files} #{} "source snapshot" nil)
    (when-not (fs/absolute? (:root snapshot)) (fail! "Snapshot root must be absolute" nil))
    (pattern! (:sha256 snapshot) digest-pattern "snapshot SHA-256" nil)
    (when-not (vector? (:files snapshot)) (fail! "Snapshot files must be a vector" nil))
    (doseq [file (:files snapshot)]
      (exact-map! file #{:relative :size :sha256} #{} "snapshot file" nil)
      (when-not (and (string? (:relative file)) (not (str/blank? (:relative file)))
                     (not (fs/absolute? (:relative file)))
                     (not-any? #{".."} (fs/components (:relative file))))
        (fail! "Snapshot relative path is unsafe" nil))
      (when-not (and (integer? (:size file)) (not (neg? (:size file))))
        (fail! "Snapshot file size is invalid" nil))
      (pattern! (:sha256 file) digest-pattern "snapshot file SHA-256" nil))
    (exact-map! report #{:mode :counts :inserted :unchanged :route-removals
                         :retirement-route-overlaps :orphan-flow-refs :empty-retired-by}
                #{} "import report" nil)
    (when-not (contains? #{:dry-run :applied} (:mode report)) (fail! "Receipt mode is invalid" nil))
    (exact-map! (:counts report)
                #{:source-routes :source-bound-routes :source-needs-binding-routes
                  :effective-routes :attempts :attempt-bodies :pending :retirements :flow-ids}
                #{} "receipt counts" nil)
    (doseq [[key amount] (:counts report)]
      (when-not (and (integer? amount) (not (neg? amount)))
        (fail! (str "Receipt count " (name key) " is invalid") nil)))
    (doseq [key [:inserted :unchanged :route-removals]]
      (when-not (and (integer? (get report key)) (not (neg? (get report key))))
        (fail! (str "Receipt " (name key) " is invalid") nil)))
    (doseq [key [:retirement-route-overlaps :orphan-flow-refs :empty-retired-by]]
      (when-not (and (vector? (get report key)) (every? string? (get report key)))
        (fail! (str "Receipt " (name key) " must be a string vector") nil))))
  (let [entities (store/import-edn (pr-str (:export/entities value)))
        counts (get-in value [:import/report :counts])
        entity-counts (frequencies (map :entity/type entities))]
    (when-not (and (= (:effective-routes counts) (get entity-counts :route 0))
                   (= (:attempts counts) (get entity-counts :attempt 0))
                   (= (:pending counts) (get entity-counts :pending 0))
                   (= (:retirements counts) (get entity-counts :retirement 0))
                   (= (:attempt-bodies counts)
                      (count (filter #(and (= :attempt (:entity/type %))
                                           (contains? (:entity/value %) :body)) entities))))
      (fail! "Receipt counts do not match its typed entities" nil))
    (assoc value :export/entities entities)))
(defn read-receipt [text] (receipt! (edn/read-string text)))

(defn snapshot-manifest [snapshot]
  {:root (:root snapshot) :sha256 (:digest snapshot)
   :files (mapv #(select-keys % [:relative :size :sha256]) (:files snapshot))})
(defn counts [parsed]
  {:source-routes (count (:routes parsed))
   :source-bound-routes (count (filter #(= "Bound" (:state %)) (vals (:routes parsed))))
   :source-needs-binding-routes (count (filter #(= "NeedsBinding" (:state %)) (vals (:routes parsed))))
   :effective-routes (count (:effective-routes parsed))
   :attempts (count (:attempts parsed))
   :attempt-bodies (count (filter :body (:attempts parsed)))
   :pending (count (:pending parsed))
   :retirements (count (:retirements parsed))
   :flow-ids (count (set (concat (keys (:routes parsed)) (keys (:retirements parsed))
                                 (map :flow (:attempts parsed)))))})

(defn target-value [target entity]
  (case (:entity/type entity)
    :route (store/stored-route-for target (:entity/flow entity))
    :attempt (store/attempt-by-id target (get-in entity [:entity/value :id]))
    :pending (store/pending-by-id target (get-in entity [:entity/value :attempt :id]))
    :retirement (store/retirement-for target (get-in entity [:entity/value :flow]))))
(defn preflight-target [target parsed]
  (if-not (fs/exists? (store/database-path target))
    {:inserted (count (:entities parsed)) :unchanged 0 :route-removals 0}
    (let [states (mapv (fn [entity]
                         (let [existing (target-value target entity)]
                           (when (and (= :route (:entity/type entity))
                                      (store/retirement-for target (:entity/flow entity)))
                             (fail! (str "Target retirement conflicts with route " (:entity/flow entity)) nil))
                           (when (and existing (not= existing (:entity/value entity)))
                             (fail! (str "Target conflicts with " (name (:entity/type entity)) " identity") nil))
                           (if existing :unchanged :inserted)))
                       (:entities parsed))
          removals (count (keep (fn [flow]
                                  (when-let [existing (store/stored-route-for target flow)]
                                    (when-not (= existing (get-in parsed [:routes flow]))
                                      (fail! (str "Target route conflicts with retirement " flow) nil))
                                    flow))
                                (:overlaps parsed)))
          retained (merge (store/routes target) (:effective-routes parsed))]
      (route-conflicts! retained)
      (native-conflicts! retained
                         (into {} (map (juxt :flow identity)
                                       (concat (store/retirements target)
                                               (vals (:retirements parsed))))))
      {:inserted (count (filter #{:inserted} states))
       :unchanged (count (filter #{:unchanged} states))
       :route-removals removals})))

(defn apply-entities! [target parsed]
  (doseq [attempt (:attempts parsed)] (store/put-attempt! target attempt))
  (doseq [pending (:pending parsed)] (store/put-pending! target pending))
  (doseq [[flow route] (:effective-routes parsed)] (store/put-route! target flow route))
  (doseq [[flow retirement] (:retirements parsed)]
    (store/put-retirement! target retirement)
    (store/remove-route! target flow)))

(defn unchanged-snapshot! [snapshot]
  (let [again (read-snapshot (:root snapshot))]
    (when-not (= (snapshot-manifest snapshot) (snapshot-manifest again))
      (fail! "Source registry changed during import" (:root snapshot)))))
(defn verify-retirement-evidence! [parsed]
  (doseq [[flow retirement] (:retirements parsed)
          :let [{:keys [path sha256]} (:evidence retirement)]]
    (when-not (and (fs/regular-file? path) (= sha256 (sha256-file path)))
      (fail! (str "Retirement evidence changed for " flow) path))))
(defn write-receipt! [path receipt]
  (let [path (fs/absolutize path)
        temporary (fs/path (fs/parent path) (str "." (fs/file-name path) ".tmp"))
        text (str (pr-str (receipt! receipt)) "\n")]
    (fs/create-dirs (fs/parent path))
    (spit (str temporary) text)
    (Files/setPosixFilePermissions (fs/path temporary) (PosixFilePermissions/fromString "rw-------"))
    (fs/move temporary path {:replace-existing true})
    (Files/setPosixFilePermissions (fs/path path) (PosixFilePermissions/fromString "rw-------"))
    (read-receipt (slurp (str path)))
    (str path)))

(defn import-json! [source target receipt-path apply?]
  (let [source (str (fs/absolutize source))
        target (str (fs/absolutize target))]
    (when (= source target) (fail! "Source and target must be different directories" source))
    (when (.startsWith (.normalize (fs/path target)) (.normalize (fs/path source)))
      (fail! "Target must be outside the source registry" target))
    (let [snapshot (read-snapshot source)
          parsed (parse-snapshot snapshot)
          target-report (preflight-target target parsed)
          report (merge {:mode (if apply? :applied :dry-run)
                         :counts (counts parsed)
                         :retirement-route-overlaps (:overlaps parsed)
                         :orphan-flow-refs (:orphan-flows parsed)
                         :empty-retired-by (:empty-retired-by parsed)}
                        target-report)
          receipt {:receipt/version 1 :source/snapshot (snapshot-manifest snapshot)
                   :import/report report :export/entities (:entities parsed)}]
      (when apply?
        (unchanged-snapshot! snapshot)
        (verify-retirement-evidence! parsed)
        (apply-entities! target parsed)
        (let [verification (preflight-target target parsed)]
          (when-not (and (zero? (:inserted verification))
                         (= (count (:entities parsed)) (:unchanged verification))
                         (zero? (:route-removals verification)))
            (fail! "Target read-after-write verification failed" target))))
      (let [receipt-path (write-receipt! receipt-path receipt)
            output {:mode (:mode report) :source-sha256 (:digest snapshot)
                    :target target :receipt receipt-path
                    :counts (:counts report) :inserted (:inserted report)
                    :unchanged (:unchanged report) :route-removals (:route-removals report)
                    :retirement-route-overlaps (:retirement-route-overlaps report)
                    :orphan-flow-refs (:orphan-flow-refs report)
                    :empty-retired-by (:empty-retired-by report)}]
        (pr-str output)))))
(ns messenger-clj.core
  (:import [java.io PushbackReader StringReader])
  (:require [babashka.fs :as fs]
            [cheshire.core :as json]
            [babashka.process :refer [shell]]
            [clojure.edn :as edn]
            [clojure.string :as str]
            [messenger-clj.typed-store :as store]
            [malli.core :as m]))

(def skill-note "Documented by the authored messaging skills in Curriculum. Update those sources with any change to this tool.")
(def failure-reasons #{:NotRegistered :NeedsBinding :InTransition :RouteHold :IdentityChanged :PaneMissing :NotReady :Blocked :ProcessMismatch :Stalled :Uncertain :RelayOverflow :Submitting :sent})
(def delivery-grades #{:Transported :Presented :Fallback-Presented :Held :Uncertain})
(def FlowId [:and [:string {:min 1 :max 96}] [:re #"^[A-Za-z0-9][A-Za-z0-9_-]*$"]])
(def NativeThread [:and [:string {:min 16 :max 96}] [:re #"^[A-Za-z0-9-]+$"]])
(def MessageBody [:string {:min 1}])
(def MessageVariant [:enum :msg :psyche])
(def ReadinessProof [:map {:closed true} [:thread_id NativeThread] [:rollout :string] [:marker :string] [:evidence_kind {:optional true} :string]])
(def RouteBinding [:map {:closed true} [:session :string] [:name :string] [:pane_id :string] [:terminal_id :string] [:agent :string] [:native_thread {:optional true} NativeThread] [:readiness_proof {:optional true} ReadinessProof] [:route_hold {:optional true} :string] [:transition {:optional true} :boolean] [:state {:optional true} :string]])
(def DeliveryAttempt [:map {:closed true} [:id :string] [:at :string] [:flow FlowId] [:reason :keyword] [:grade {:optional true} :keyword] [:variant {:optional true} MessageVariant] [:context {:optional true} MessageBody] [:body {:optional true} MessageBody] [:submitted {:optional true} :string] [:binding {:optional true} RouteBinding]])
(def PendingIntent [:map {:closed true} [:attempt DeliveryAttempt] [:message MessageBody] [:variant {:optional true} MessageVariant] [:context {:optional true} MessageBody] [:state [:= "held"]]])
(def RouteIdentity [:map {:closed true} [:session :string] [:name :string] [:pane_id :string] [:terminal_id :string] [:agent :string]])
(def RetirementEvidence [:map {:closed true} [:path :string] [:sha256 [:re #"^[0-9a-f]{64}$"]]])
(def RetirementMarker [:map {:closed true} [:version [:= 1]] [:state [:= "retired"]] [:flow FlowId] [:record RouteIdentity] [:native_thread NativeThread] [:evidence RetirementEvidence] [:retired_by :string] [:retired_at :string]])
(def Reservation [:map {:closed true} [:id :int] [:flow FlowId] [:root :string]])
(def PaneMessage [:tuple FlowId MessageBody])
(def PsycheMessage [:tuple FlowId MessageBody MessageBody])
(def MessageRequest [:map {:closed true} [:variant MessageVariant] [:body MessageBody] [:context {:optional true} MessageBody]])
(doseq [schema [FlowId NativeThread MessageBody MessageVariant ReadinessProof RouteBinding DeliveryAttempt PendingIntent RetirementMarker Reservation PaneMessage PsycheMessage MessageRequest]] (m/validator schema))

(defn fail [s] (throw (ex-info s {:hm/failure true})))
(defn valid! [schema value label] (if (m/validate schema value) value (fail (str "Invalid " label ": " (pr-str (m/explain schema value))))))
(declare ->DatalevinLedger record-attempt! record-pending! route-records nonempty-strings!)
(defn flow-id! [value]
  (when-not (and (string? value) (re-matches #"[A-Za-z0-9][A-Za-z0-9_-]{0,95}" value))
    (fail "Flow ID must contain only letters, digits, underscores, or hyphens"))
  (valid! FlowId value "FlowId"))
(defn native-thread! [value]
  (when-not (and (string? value) (re-matches #"[A-Za-z0-9-]{16,96}" value)) (fail "Invalid NativeThread"))
  (valid! NativeThread value "NativeThread"))
(defn route-binding! [value] (valid! RouteBinding (store/route! value) "RouteBinding"))
(defn delivery-attempt! [value]
  (when-not (and (contains? failure-reasons (:reason value)) (contains? delivery-grades (:grade value)))
    (fail "Invalid DeliveryAttempt grade or reason"))
  (valid! DeliveryAttempt value "DeliveryAttempt"))
(defprotocol Registry (load-route [this flow]) (save-route! [this flow route]))
(defprotocol HerdrTransport
  (live-agents* [this])
  (target-agent* [this route])
  (process-info* [this route])
  (pane* [this route])
  (move-pane* [this route workspace label])
  (send-keys* [this route key])
  (prompt!* [this route envelope wait?]))
(defprotocol Ledger (record-attempt! [this attempt]) (record-pending! [this attempt body]))
(defprotocol Clock (current-time [this]))
(defrecord SystemClock [] Clock (current-time [_] (.toString (java.time.Instant/now))))
(defrecord DatalevinRegistry [state-root]
  Registry
  (load-route [_ flow] (store/route-for state-root (flow-id! flow)))
  (save-route! [_ flow route] (store/put-route! state-root (flow-id! flow) (route-binding! route))))
(def ^:dynamic *root* nil)
(def ^:dynamic *flow-id* nil)
(def ^:dynamic *ledger* nil)
(def ^:dynamic *registry* nil)
(def ^:dynamic *clock* nil)
(def ^:dynamic *transport* nil)
(def ^:dynamic *shell* shell)
(def ^:dynamic *readiness-attempts* 50)
(def ^:dynamic *reservation-wait-ms* 15000)
(def ^:dynamic *reservation-retry-ms* 50)
;; A test seam around the Orchestrate boundary.  Production always uses
;; `with-reservation` below; tests supply a short-lived in-memory lease.
(def ^:dynamic *with-reservation* nil)
(defn root []
  ;; The deployment migrates the typed database here while holding both state
  ;; roots and replacing every launcher in the same activation.
  (fs/absolutize (or *root* (System/getenv "HM_REGISTRY")
                     (str (fs/path (System/getProperty "user.home") ".local/state/messenger-clj")))))
(defn registry [] (or *registry* (->DatalevinRegistry (root))))
(defn now [] (current-time (or *clock* (->SystemClock))))
(defn quote-datom [s] (str "«" (str/replace (str s) #"[\\»]" {\\ "\\\\" \» "\\»"}) "»"))
(defn read-msg [value]
  ;; `data_readers.clj` binds #msg to this function for Clojure readers.  The
  ;; tagged value is deliberately just the two pane-visible fields.
  (valid! PaneMessage value "#msg"))
(defn read-psyche [value]
  (valid! PsycheMessage value "#psyche"))
(defn- read-complete [readers line]
  (with-open [reader (PushbackReader. (StringReader. line))]
    (let [eof (Object.)
          value (edn/read {:readers readers :eof eof} reader)]
      (when (or (identical? eof value)
                (not (identical? eof (edn/read {:readers readers :eof eof} reader))))
        (fail "Expected exactly one complete EDN form"))
      value)))
(defn read-pane-message [line]
  (let [tagged ::tagged
        value (read-complete {'msg #(hash-map tagged (read-msg %))} line)]
    (or (get value tagged) (fail "Expected one complete #msg form"))))
(defn read-psyche-message [line]
  (let [tagged ::tagged
        value (read-complete {'psyche #(hash-map tagged (read-psyche %))} line)]
    (or (get value tagged) (fail "Expected one complete #psyche form"))))
(defn request! [request]
  (let [request (valid! MessageRequest request "MessageRequest")]
    (when (and (= :psyche (:variant request)) (not (contains? request :context)))
      (fail "Psyche messages require context"))
    (when (and (= :msg (:variant request)) (contains? request :context))
      (fail "Machine messages do not carry psyche context"))
    request))
(defn message-envelope [sender request]
  (flow-id! sender)
  (let [{:keys [variant context body]} (request! request)
        [tag value reader] (case variant
                             :msg ["#msg" (read-msg [sender body]) read-pane-message]
                             :psyche ["#psyche" (read-psyche [sender context body]) read-psyche-message])
        envelope (str tag " " (pr-str value))]
    (when-not (= value (reader envelope))
      (fail (str tag " EDN round trip failed; message held")))
    envelope))
(defn relay-line [sender _recipient body] (message-envelope sender {:variant :msg :body body}))
(defn relay [sender recipient body] (relay-line sender recipient body))
(defn framed-text [sender _recipient body] (message-envelope sender {:variant :msg :body body}))
(defn nested-relay? [body]
  (or (try (read-pane-message body) true (catch Exception _ false))
      (try (read-psyche-message body) true (catch Exception _ false))))
(defn read-route [flow]
  (try
    (if-let [route (load-route (registry) flow)]
      (route-binding! route)
      (fail (str "No valid registration for " flow)))
    (catch Exception e (fail (str "No valid registration for " flow ": " (.getMessage e))))))
(defn herdr! [& args]
  (let [{:keys [exit out err]} (apply shell {:out :string :err :string :continue true :timeout 15000} "herdr" args)]
    (when-not (zero? exit) (fail (or (not-empty (str/trim err)) (str "herdr failed: " exit))))
    (try (let [reply (json/parse-string out true)]
           (when-not (map? reply) (fail "Herdr returned malformed JSON object; do not blindly retry a send"))
           (when (:error reply) (fail (str "Herdr: " (:error reply))))
           (let [result (or (:result reply) reply)]
             (when-not (map? result) (fail "Herdr returned malformed result object; do not blindly retry a send"))
             result))
         (catch Exception _ (fail "Herdr returned invalid JSON; do not blindly retry a send")))))
(declare shell-live-agents)
(defn direct-prompt! [route envelope wait-presented]
  (let [args (cond-> ["--session" (:session route) "agent" "prompt" (:pane_id route) envelope]
               wait-presented (into ["--wait" "--timeout" "5000"]))]
    (apply herdr! args)))
(defn presented! [reply]
  ;; Herdr's supported `--wait` response is the observation boundary.  A
  ;; successful submission reply alone cannot establish pane presentation.
  (when-not (true? (:presented reply))
    (fail "Presentation was not observed; do not retry blindly"))
  reply)
(defrecord ShellHerdr []
  HerdrTransport
  (live-agents* [_] (shell-live-agents))
  (target-agent* [_ route] (herdr! "--session" (:session route) "agent" "get" (:pane_id route)))
  (process-info* [_ route] (herdr! "--session" (:session route) "pane" "process-info" "--pane" (:pane_id route)))
  (pane* [_ route] (herdr! "--session" (:session route) "pane" "get" (:pane_id route)))
  (move-pane* [_ route workspace label] (herdr! "--session" (:session route) "pane" "move" (:pane_id route) "--new-tab" "--workspace" workspace "--label" label "--no-focus"))
  (send-keys* [_ route key] (herdr! "--session" (:session route) "agent" "send-keys" (:pane_id route) key))
  (prompt!* [_ route envelope wait?] (direct-prompt! route envelope wait?)))
(defn transport [] (or *transport* (->ShellHerdr)))
(defn sha256 [file]
  (apply str (map #(format "%02x" (bit-and % 0xff)) (.digest (doto (java.security.MessageDigest/getInstance "SHA-256") (.update (fs/read-all-bytes file)))))))
(defn retirement! [flow]
  (when-let [stored (store/retirement-for (root) flow)]
    (try
      (let [value (valid! RetirementMarker stored "RetirementMarker")
            evidence (:evidence value)]
        (when-not (and (= flow (:flow value)) (fs/absolute? (:path evidence))
                       (fs/regular-file? (:path evidence))
                       (= (:sha256 evidence) (sha256 (:path evidence))))
          (fail "bad marker"))
        value)
      (catch Exception _ (fail (str "Retirement marker for " flow " is unavailable or malformed"))))))
(defn assert-not-retired! [flow]
  (when-let [marker (retirement! flow)]
    (fail (str "Retired: " flow " by " (get-in marker [:evidence :path])))))
(defn assert-native-not-retired! [native-thread flow]
  (doseq [marker (store/retirements (root))
          :let [other (:flow marker)]]
    (when (and (not= flow other) (= native-thread (:native_thread (retirement! other))))
      (fail (str "Native thread " native-thread " is retired as Flow " other "; use a fresh native session")))))
(defn claude-session-matches? [process native]
  (and (int? (:pid process))
       (try
         (let [file (fs/path (System/getProperty "user.home") ".claude" "sessions" (str (:pid process) ".json"))
               value (json/parse-string (slurp (str file)) true)]
           (= native (:sessionId value)))
         (catch Exception _ false))))
(defn process-matches! [route]
  (let [reply (process-info* (transport) route)
        info (or (:process_info reply) reply)
        processes (:foreground_processes info)
        native (native-thread! (:native_thread route))]
    (when-not (some #(or (str/includes? (str/join " " (map str (or (:argv %) []))) native)
                         (and (= "claude" (:agent route)) (claude-session-matches? % native)))
                    processes)
      (fail "ProcessMismatch"))))
(defn content-text [content]
  (cond
    (string? content) content
    (sequential? content) (str/join "" (keep #(when (map? %) (:text %)) content))
    :else ""))
(defn readiness-witness [rows marker native-thread rollout]
  (let [user-index (first (keep-indexed (fn [index row]
                                          (let [payload (:payload row) item (:item payload)]
                                            (when (and (= "event_msg" (:type row))
                                                       (= native-thread (:thread_id payload))
                                                       (= "UserMessage" (:type item))
                                                       (str/includes? (content-text (:content item)) marker))
                                              index))) rows))]
    (when user-index
      (some (fn [row]
              (let [payload (:payload row) item (:item payload)]
                (when (and (= native-thread (:thread_id payload))
                           (= "AgentMessage" (:type item))
                           (= marker (str/trim (content-text (:content item)))))
                  {:thread_id native-thread :rollout (str (fs/absolutize rollout)) :marker marker})))
            (drop (inc user-index) rows)))))
(defn claude-readiness-witness [rows marker native-thread rollout]
  (let [session-id #(or (:sessionId %) (:session_id %))
        user-index (first (keep-indexed (fn [index row]
                                          (when (and (= "user" (:type row))
                                                     (= native-thread (session-id row))
                                                     (str/includes? (content-text (get-in row [:message :content])) marker))
                                            index)) rows))]
    (when user-index
      (some (fn [row]
              (when (and (= "assistant" (:type row))
                         (= native-thread (session-id row))
                         (= marker (str/trim (content-text (get-in row [:message :content])))))
                {:thread_id native-thread :rollout (str (fs/absolutize rollout)) :marker marker
                 :evidence_kind "claude-transcript"}))
            (drop (inc user-index) rows)))))
(defn readiness-probe! [agent marker native-thread rollout]
  (when-not (and (string? marker) (re-matches #"HM_READY_[A-Za-z0-9_-]{8,96}" marker))
    (fail "Readiness probe marker must be a unique HM_READY token"))
  (native-thread! native-thread)
  (try
    (prompt!* (transport) agent (str "Reply exactly " marker " to confirm this explicit HM readiness probe.") false)
    (catch Exception error
      ;; Herdr can report this after injecting into a resumed Codex pane.  Only
      ;; the exact native transcript witness below can turn it into readiness.
      (when-not (str/includes? (or (.getMessage error) "") "agent_prompt_stalled")
        (throw error))))
  (when-not rollout (fail "Readiness probe requires a native Codex rollout or Claude transcript"))
  (loop [remaining *readiness-attempts*]
    (let [rows (try (mapv #(json/parse-string % true)
                          (remove str/blank? (str/split-lines (slurp (str rollout)))))
                    (catch Exception _ (fail "Readiness probe rollout is unavailable or invalid")))]
      (or (readiness-witness rows marker native-thread rollout)
          (claude-readiness-witness rows marker native-thread rollout)
          (if (pos? (dec remaining))
            (do (Thread/sleep 100) (recur (dec remaining)))
            (fail "Readiness probe marker was not observed in an exact native assistant reply"))))))
(defn verify-target! [route]
  (let [reply (target-agent* (transport) route)
        agent (or (:agent reply) reply)]
    (when-not (and (= (:name route) (:name agent))
                   (= (:pane_id route) (:pane_id agent))
                   (= (:terminal_id route) (:terminal_id agent))
                   (= (:agent route) (:agent agent)))
      (fail "IdentityChanged"))
    (when (and (not (:interactive_ready agent))
               (not= (:native_thread route) (get-in route [:readiness_proof :thread_id])))
      (fail "NotReady"))
    (when (= "blocked" (:agent_status agent)) (fail "Blocked"))
    (when-not (contains? #{"idle" "working" "done"} (:agent_status agent)) (fail "Uncertain"))
    (process-matches! route)
    agent))
(defn shell-live-agents []
  (mapcat (fn [session]
            (map #(assoc % :session (:name session))
                 (:agents (herdr! "--session" (:name session) "agent" "list"))))
          (filter :running (:sessions (herdr! "session" "list" "--json")))))
(defn live-agents [] (live-agents* (transport)))
(defn parse-pane [value]
  (let [[session pane] (str/split (or value "") #":" 2)]
    (when (or (str/blank? session) (str/blank? pane)) (fail "--pane requires <session>:<pane>"))
    {:session session :pane_id pane}))
(defn fallback-route [flow stored pane]
  (cond
    pane (let [{:keys [session pane_id]} (parse-pane pane)
               hits (filter #(and (= session (:session %)) (= pane_id (:pane_id %))) (live-agents))]
           (when-not (= 1 (count hits)) (fail "Held: --pane does not name exactly one live Herdr agent"))
           (route-binding! (assoc (select-keys (first hits) [:name :pane_id :terminal_id :agent]) :session session :native_thread (or (:native_thread stored) "00000000-0000-0000-0000-000000000000"))))
    stored (let [hits (filter #(and (= (:session stored) (:session %)) (= (:name stored) (:name %))) (live-agents))]
             (when-not (= 1 (count hits)) (fail "Held: stored route has no unique live Herdr agent"))
             (assoc (first hits) :native_thread (:native_thread stored)))
    :else (let [suffix (re-pattern (str "\\b" (java.util.regex.Pattern/quote flow) "$"))
                hits (filter #(re-find suffix (or (:name %) "")) (live-agents))]
            (when-not (= 1 (count hits)) (fail "Held: Flow title has no unique live Herdr agent"))
            (let [a (first hits)] (route-binding! (assoc (select-keys a [:session :name :pane_id :terminal_id :agent]) :native_thread "00000000-0000-0000-0000-000000000000"))))))
(defn exact-live-route? [stored]
  (some #(and (= (:session stored) (:session %))
              (= (:name stored) (:name %))
              (= (:pane_id stored) (:pane_id %))
              (= (:terminal_id stored) (:terminal_id %))
              (= (:agent stored) (:agent %)))
        (live-agents)))
(defn resolve-send-route [flow stored pane]
  (cond
    pane [(fallback-route flow stored pane) true]
    (and stored (exact-live-route? stored)) [stored false]
    stored [(fallback-route flow stored nil) true]
    :else [(fallback-route flow nil nil) true]))
(defn in-transition? [route] (or (:transition route) (= "transition" (:state route))))
(defn needs-binding? [route] (= "NeedsBinding" (:state route)))
(defn attempt-fields [request submitted]
  (cond-> (select-keys (request! request) [:variant :context :body])
    submitted (assoc :submitted submitted)))
(defn append-attempt! [flow reason grade route request submitted]
  (let [attempt (cond-> {:id (str (java.util.UUID/randomUUID)) :at (now) :flow flow :reason reason :grade grade}
                  request (merge (attempt-fields request submitted))
                  route (assoc :binding route))]
    (delivery-attempt! attempt)
    (record-attempt! (or *ledger* (->DatalevinLedger (root))) attempt)
    ;; The production ledger must be queryable before a prompt may rely on its
    ;; pre-prompt attempt.  Injected test ledgers own their own persistence.
    (when-not *ledger*
      (when-not (= attempt (store/attempt-by-id (root) (:id attempt)))
        (fail "Attempt ledger index did not confirm persistence")))
    attempt))
(defn held-reason [error]
  (let [message (.getMessage error)
        candidate (keyword (or message ""))]
    (if (contains? failure-reasons candidate) candidate :PaneMissing)))
(defn record-uncertain! [flow route request submitted]
  ;; The pre-prompt record is already durable.  Keep the original uncertainty
  ;; if storage is unavailable while recording this post-submit observation.
  (try (append-attempt! flow :Uncertain :Uncertain route request submitted) (catch Exception _ nil)))
(defn contention? [reply]
  (boolean (re-find #"LockRejected\.(?:DuplicateName|PathConflict|PathOverlap)|(?:DuplicateName|PathConflict|PathOverlap)"
                    (str (:out reply) "\n" (:err reply)))))
(defn reserve! [flow]
  (let [owner (flow-id! (or *flow-id* (System/getenv "FLOW_ID") flow))
        operation (str "MessengerCljDelivery-"
                       (str/replace (str (java.util.UUID/randomUUID)) "-" ""))
        query (str "Lock.{ " operation " " owner " [ " (quote-datom (root))
                   " ] «Register or submit through Herdr» }")
        deadline (+ (System/nanoTime) (* 1000000 (long *reservation-wait-ms*)))]
    (loop []
      (let [reply (*shell* {:out :string :err :string :continue true :timeout 15000}
                           "orchestrate" query)
            match (re-find #"Locked\.\{\s+(\d+)\b" (:out reply))]
        (cond
          (and (zero? (:exit reply)) match)
          (valid! Reservation {:id (parse-long (second match)) :flow flow :root (str (root))}
                  "Reservation")

          (and (contention? reply) (< (System/nanoTime) deadline))
          (do (Thread/sleep (long *reservation-retry-ms*)) (recur))

          (contention? reply)
          (fail (str "Reservation timed out after " *reservation-wait-ms* "ms: "
                     (str/trim (or (not-empty (:out reply)) (:err reply) ""))))

          :else
          (fail (str "Reservation refused: "
                     (str/trim (or (not-empty (:out reply)) (:err reply) "")))))))))
(defn release! [reservation]
  (let [reply (*shell* {:out :string :err :string :continue true :timeout 15000} "orchestrate" (str "Release." (:id reservation)))]
    (when-not (and (zero? (:exit reply)) (str/starts-with? (:out reply) "Released."))
      (fail (str "Reservation release failed: " (str/trim (or (not-empty (:out reply)) (:err reply) "")))))))
(defn with-reservation [flow f]
  (if *with-reservation*
    (*with-reservation* flow f)
    (let [reservation (reserve! flow)]
      (try (f) (finally (release! reservation))))))
(defrecord DatalevinLedger [state-root]
  Ledger
  (record-attempt! [_ attempt]
    (store/put-attempt! state-root attempt)
    attempt)
  (record-pending! [_ attempt request]
    (let [{:keys [variant context body]} (request! request)]
      (store/put-pending! state-root
                          (valid! PendingIntent
                                  (cond-> {:attempt attempt :message body :variant variant :state "held"}
                                    context (assoc :context context))
                                  "PendingIntent")))
    attempt))
(defn held! [flow reason request route]
  (let [{:keys [variant context body]} (request! request)
        attempt (append-attempt! flow reason :Held route request nil)
        pending (valid! PendingIntent
                        (cond-> {:attempt attempt :message body :variant variant :state "held"}
                          context (assoc :context context))
                        "PendingIntent")]
    (record-pending! (or *ledger* (->DatalevinLedger (root))) attempt request)
    (when-not *ledger*
      (when-not (= pending (store/pending-by-id (root) (:id attempt)))
        (fail "Pending ledger index did not confirm persistence")))
    (throw (ex-info (str "Held.{ " flow " " (name reason) " attempt-" (subs (:id attempt) 0 12) " }")
                    {:hm/failure true :hm/held true}))))
(defn register! [flow name session native-thread readiness-marker rollout]
  (flow-id! flow)
  (with-reservation flow
    (fn []
      (assert-not-retired! flow)
      (let [existing (load-route (registry) flow)]
        (when (:route_hold existing) (fail "Registration is held for route repair"))
        (let [agents (if session (:agents (herdr! "--session" session "agent" "list")) (live-agents))
              found (filter #(= name (:name %)) agents)]
          (when-not (= 1 (count found)) (fail (str "Expected one live agent named " name "; found " (count found) ". Use --session.")))
          (let [a (assoc (first found) :session (or session (:session (first found))))
                session (:session a)
                observed (herdr! "--session" session "pane" "process-info" "--pane" (:pane_id a))
                native-thread (or native-thread (:native_thread existing)
                                  (second (re-find #"([A-Za-z0-9-]{16,96})" (pr-str observed))))
                _ (native-thread! native-thread)
                _ (assert-native-not-retired! native-thread flow)
                _ (nonempty-strings! "Herdr registration has no agent kind" [(:agent a)])
                proof (when-not (:interactive_ready a)
                        (if readiness-marker
                          (readiness-probe! a readiness-marker native-thread rollout)
                          (fail "Agent is not interactively ready")))
                route (valid! RouteBinding (cond-> (assoc (select-keys a [:session :name :pane_id :terminal_id :agent]) :native_thread native-thread)
                                             proof (assoc :readiness_proof proof)) "RouteBinding")]
            (when (and existing (not= (:terminal_id existing) (:terminal_id route)))
              (fail "Flow is already registered to a different terminal"))
            (save-route! (registry) flow route)
            (str "Registered " flow ": " name " (" session ")")))))))
(defn nonempty-strings! [label fields]
  (when-not (every? #(and (string? %) (not (str/blank? %))) fields)
    (fail label)))
(defn deregister! [flow session pane-id terminal-id name]
  (flow-id! flow)
  (nonempty-strings! "Deregister requires every exact stale route identity field" [session pane-id terminal-id name])
  (with-reservation flow
    (fn []
      (let [actual (read-route flow)]
        (when-not (= {:session session :pane_id pane-id :terminal_id terminal-id :name name}
                     (select-keys actual [:session :pane_id :terminal_id :name]))
          (fail "Registration differs from the explicitly revalidated stale route"))
        (store/remove-route! (root) flow)
        (str "Deregistered stale " flow ": " name " (" session "/" pane-id "/" terminal-id ")")))))
(defn retirement-evidence! [evidence-path evidence-sha256]
  (let [path (fs/absolutize evidence-path)]
    (when-not (and (fs/absolute? path) (fs/regular-file? path) (string? evidence-sha256) (re-matches #"[0-9a-f]{64}" evidence-sha256))
      (fail "Retirement evidence requires an existing absolute file and SHA-256 digest"))
    (let [actual (sha256 path)]
      (when-not (= evidence-sha256 actual) (fail "Retirement evidence SHA-256 does not match; nothing changed"))
      {:path (str path) :sha256 actual})))
(defn retire! [flow session pane-id terminal-id name agent native-thread evidence-path evidence-sha256 allow-absent?]
  (flow-id! flow)
  (nonempty-strings! "Retirement requires every exact route identity field" [session pane-id terminal-id name agent])
  (native-thread! native-thread)
  (let [evidence (retirement-evidence! evidence-path evidence-sha256)
        expected {:session session :pane_id pane-id :terminal_id terminal-id :name name :agent agent}]
    (with-reservation flow
      (fn []
        (if-let [existing (retirement! flow)]
          (if (and (= expected (:record existing)) (= native-thread (:native_thread existing)))
            (str "Already retired " flow ": marker retained")
            (fail (str "Flow " flow " already has a different retirement marker")))
          (do
            (if-let [route (store/stored-route-for (root) flow)]
              (when-not (= expected (select-keys route (keys expected)))
                (fail "Registration differs from the explicitly revalidated retirement route"))
              (when-not allow-absent?
                (fail "No current registration; use import-retirement only with retained exact evidence")))
            (store/put-retirement!
             (root)
             (valid! RetirementMarker
                     {:version 1 :state "retired" :flow flow :record expected
                      :native_thread native-thread :evidence evidence
                      :retired_by (or *flow-id* (System/getenv "FLOW_ID") "")
                      :retired_at (now)}
                     "RetirementMarker"))
            (store/remove-route! (root) flow)
            (str "Retired " flow ": delivery is blocked before Herdr routing")))))))
(defn rebind! [flow old-name new-name session pane-id terminal-id agent native-thread]
  (flow-id! flow)
  (nonempty-strings! "Rebind requires every exact old route identity field" [old-name session pane-id terminal-id agent])
  (when (or (not (string? new-name)) (str/blank? new-name) (= old-name new-name))
    (fail "Rebind requires a distinct nonempty new agent name"))
  (native-thread! native-thread)
  (with-reservation flow
    (fn []
      (assert-not-retired! flow)
      (let [actual (read-route flow)
            expected {:session session :name old-name :pane_id pane-id :terminal_id terminal-id :agent agent}]
        (when (:route_hold actual) (fail "Registration is held for route repair"))
        (when-not (= expected (select-keys actual (keys expected)))
          (fail "Registration differs from the explicitly revalidated old binding"))
        (when-not (= native-thread (:native_thread actual))
          (fail "Registration differs from the explicitly revalidated native thread"))
        (when (some #(and (not= flow (key %)) (= session (get-in % [1 :session])) (= new-name (get-in % [1 :name])))
                    (route-records))
          (fail (str "New agent name " new-name " is already registered in " session)))
        (let [replacement (route-binding! (assoc actual :name new-name))]
          ;; `verify-target!` checks exact live identity, readiness, blocked
          ;; state, and native process before the registry name is changed.
          (verify-target! replacement)
          (save-route! (registry) flow replacement)
          (str "Rebound " flow ": " old-name " -> " new-name " (" session "/" pane-id "/" terminal-id ")"))))))
(defn move-route! [flow record pane-id hold?]
  (let [replacement (cond-> (assoc record :pane_id pane-id)
                      hold? (assoc :route_hold "pane_move_in_progress")
                      (not hold?) (dissoc :route_hold))]
    (save-route! (registry) flow replacement)
    replacement))
(defn pane-value [reply] (or (:pane reply) reply))
(defn move-result-value [reply] (or (:move_result reply) reply))
(defn verify-move-target! [expected pane process-pid native-thread]
  (let [route (assoc expected :pane_id (:pane_id pane))
        live-pane (pane-value (pane* (transport) route))
        process-reply (process-info* (transport) route)
        info (or (:process_info process-reply) process-reply)
        processes (:foreground_processes info)
        matches (filter #(and (= (:session expected) (:session %))
                              (= (:name expected) (:name %))
                              (= (:pane_id route) (:pane_id %))
                              (= (:terminal_id expected) (:terminal_id %))
                              (= (:agent expected) (:agent %)))
                        (live-agents))]
    (when-not (and (= (:terminal_id expected) (:terminal_id pane))
                   (= (:terminal_id expected) (:terminal_id live-pane))
                   (= (:agent expected) (:agent live-pane)))
      (fail "Moved pane terminal or harness identity changed"))
    (when-not (some #(= process-pid (:pid %)) processes)
      (fail "Moved pane foreground process identity changed"))
    (when-not (or (= "codex" (:agent expected))
                  (some #(str/includes? (str/join " " (map str (or (:argv %) []))) native-thread) processes))
      (fail "Moved pane native session identity changed"))
    (when-not (= 1 (count matches))
      (fail "Moved pane has no unique matching Herdr agent"))
    route))
(defn move! [flow session pane-id terminal-id name agent native-thread process-pid workspace]
  (flow-id! flow)
  (nonempty-strings! "Move requires the complete old route" [session pane-id terminal-id name agent])
  (native-thread! native-thread)
  (when-not (and (integer? process-pid) (pos? process-pid))
    (fail "Move requires a witnessed positive foreground process PID"))
  (when-not (and (string? workspace) (re-matches #"w[A-Za-z0-9]+" workspace))
    (fail "Move requires an exact Herdr workspace ID"))
  (with-reservation flow
    (fn []
      (assert-not-retired! flow)
      (let [record (read-route flow)
            expected {:session session :name name :pane_id pane-id :terminal_id terminal-id :agent agent}]
        (when (:route_hold record) (fail "Registration is held for route repair; move refused"))
        (when-not (and (= expected (select-keys record (keys expected))) (= native-thread (:native_thread record)))
          (fail "Move old route or native thread differs from registration"))
        (let [source (pane-value (pane* (transport) record))
              old-workspace (:workspace_id source)]
          (verify-move-target! expected source process-pid native-thread)
          (when (some #(and (not= flow (key %)) (= terminal-id (get-in % [1 :terminal_id]))) (route-records))
            (fail "Terminal is registered to another Flow"))
          ;; The persisted hold is the boundary before an irreversible pane mutation.
          (move-route! flow record pane-id true)
          (let [moved (atom nil)]
            (try
              (let [result (move-result-value (move-pane* (transport) record workspace (or (:label source) name)))
                    pane (:pane result)]
                (reset! moved pane)
                (when-not (and (= pane-id (:previous_pane_id result))
                               (= old-workspace (:previous_workspace_id result))
                               (= workspace (:workspace_id pane)))
                  (fail "Herdr move result differs from requested route"))
                (let [verified (verify-move-target! expected pane process-pid native-thread)]
                  (move-route! flow record (:pane_id verified) false)
                  (str "Moved " flow ": " old-workspace "/" pane-id " -> " workspace "/" (:pane_id verified) " (" terminal-id ")")))
              (catch Exception error
                (if-not @moved
                  (fail (str "Move failed or is uncertain; inspect exact terminal before routing: " (.getMessage error)))
                  (let [rollback-error (try
                                         (let [reverse-result (move-result-value (move-pane* (transport) (assoc record :pane_id (:pane_id @moved)) old-workspace (or (:label source) name)))
                                               reverse (:pane reverse-result)
                                               verified (verify-move-target! expected reverse process-pid native-thread)]
                                           (move-route! flow record (:pane_id verified) false)
                                           nil)
                                         (catch Exception rollback-error rollback-error))]
                    (if rollback-error
                      (fail (str "Move and compensation failed; delivery held for manual route repair: " (.getMessage rollback-error)))
                      (fail (str "Move failed; terminal was returned to original workspace with new pane ID: " (.getMessage error))))))))))))))
(def abrupt-keys {"codex" {:interrupt ["esc"] :submit []}
                  "claude" {:interrupt ["esc" "esc"] :submit ["enter"]}})
(defn validate-request! [request]
  (let [{:keys [context body] :as request} (request! request)
        values (cond-> [body] context (conj context))]
    (when (some #(or (str/blank? %) (re-find #"[\p{Cc}&&[^\n\t]]" %)) values)
      (fail "Message fields must be nonempty and contain no terminal control characters"))
    (when (some nested-relay? values)
      (fail "Nested complete #msg or #psyche form is not a message field"))
    request))
(defn send-abrupt-request! [flow request wait-presented]
  (flow-id! flow)
  (let [request (validate-request! request)]
  (let [sender (or *flow-id* (System/getenv "FLOW_ID") (fail "Set FLOW_ID to your own flow ID before sending"))]
    (with-reservation flow
      (fn []
        (assert-not-retired! flow)
        (let [route (read-route flow)]
          (when (needs-binding? route) (held! flow :NeedsBinding request route))
          (when (in-transition? route) (held! flow :InTransition request route))
          (when (:route_hold route) (held! flow :RouteHold request route))
          (let [live (try (verify-target! route) (catch Exception error (held! flow (held-reason error) request route)))
                keys (get abrupt-keys (:agent route))]
            (when-not keys (fail (str "Hard-abrupt is not supported for " (:agent route) "; nothing sent")))
            (let [envelope (message-envelope sender request)
                  submission (append-attempt! flow :Submitting :Uncertain route request envelope)]
              (try
                (doseq [key (:interrupt keys)] (send-keys* (transport) route key))
                (let [reply (prompt!* (transport) route envelope wait-presented)]
                  (when wait-presented (presented! reply)))
                (doseq [key (:submit keys)] (send-keys* (transport) route key))
              ;; A successful prompt does not prove the terminal stayed bound.
              ;; Recheck before reporting any delivery grade.
                (verify-target! route)
                (try
                  (append-attempt! flow :sent (if wait-presented :Presented :Transported) route request envelope)
                  (str (if wait-presented "Presented" "Transported") ".{ " flow " " (or (:agent_status live) "unknown") " }")
                  (catch Exception error
                    (record-uncertain! flow route request envelope)
                    (throw (ex-info (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12) " } prompt was delivered but ledger confirmation failed; do not retry: " (.getMessage error))
                                    {:hm/failure true :hm/post-ledger true}))))
                (catch Exception error
                  (if (:hm/post-ledger (ex-data error))
                    (throw error)
                    (do (record-uncertain! flow route request envelope)
                        (fail (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12) " } Escape was sent; prompt failed or is uncertain: " (.getMessage error)))))))))))))))
(defn send-abrupt! [flow body wait-presented]
  (send-abrupt-request! flow {:variant :msg :body body} wait-presented))
(defn send-abrupt-psyche! [flow context verbatim wait-presented]
  (send-abrupt-request! flow {:variant :psyche :context context :body verbatim} wait-presented))
(defn record-sent! [flow grade route submission live request envelope]
  (try
    (append-attempt! flow :sent grade route request envelope)
    (str (name grade) ".{ " flow " " (or (:agent_status live) "unknown") " }")
    (catch Exception error
      (record-uncertain! flow route request envelope)
      (throw (ex-info (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12)
                           " } prompt was delivered but ledger confirmation failed; do not retry: " (.getMessage error))
                      {:hm/failure true :hm/post-ledger true})))))
(defn send-request!
  ([flow request wait-presented pane] (send-request! flow request wait-presented pane 10))
  ([flow request wait-presented pane hold-seconds]
   (flow-id! flow)
   (let [request (validate-request! request)]
   (let [sender (or *flow-id* (System/getenv "FLOW_ID") (fail "Set FLOW_ID to your own flow ID before sending"))]
     (with-reservation flow
       (fn []
         (assert-not-retired! flow)
         (let [stored (try (read-route flow) (catch Exception _ nil))
               stored (if (and (nil? stored) (pos? hold-seconds))
                        (do (Thread/sleep (long (* 1000 hold-seconds)))
                            (try (read-route flow) (catch Exception _ nil)))
                        stored)]
           (when (in-transition? stored) (held! flow :InTransition request stored))
           (when (needs-binding? stored) (held! flow :NeedsBinding request stored))
           (when (:route_hold stored) (held! flow :RouteHold request stored))
           (let [[route fallback?] (try (resolve-send-route flow stored pane)
                                        (catch Exception _ (held! flow (if stored :PaneMissing :NotRegistered) request stored)))
                 live (try (verify-target! route) (catch Exception error (held! flow (held-reason error) request route)))
                 envelope (message-envelope sender request)
                 submission (append-attempt! flow :Submitting :Uncertain route request envelope)
                 grade (if fallback? :Fallback-Presented (if wait-presented :Presented :Transported))]
             (try
               (let [waited? (or fallback? wait-presented)
                     reply (prompt!* (transport) route envelope waited?)]
                 (when waited? (presented! reply)))
               (verify-target! route)
               (record-sent! flow grade route submission live request envelope)
               (catch Exception error
                 (if (:hm/post-ledger (ex-data error))
                   (throw error)
                   (do (record-uncertain! flow route request envelope)
                       (fail (str "Uncertain.{ " flow " attempt-" (subs (:id submission) 0 12)
                                  " } prompt failed or is uncertain: " (.getMessage error)))))))))))))))
(defn send!
  ([flow body wait-presented pane] (send-request! flow {:variant :msg :body body} wait-presented pane))
  ([flow body wait-presented pane hold-seconds]
   (send-request! flow {:variant :msg :body body} wait-presented pane hold-seconds)))
(defn send-psyche!
  ([flow context verbatim wait-presented pane]
   (send-request! flow {:variant :psyche :context context :body verbatim} wait-presented pane))
  ([flow context verbatim wait-presented pane hold-seconds]
   (send-request! flow {:variant :psyche :context context :body verbatim} wait-presented pane hold-seconds)))
(defn route-records []
  (into {} (remove (fn [[flow _]] (store/retirement-for (root) flow))
                   (store/routes (root)))))
(defn heartbeat-state! []
  (json/generate-string
   {:version 1
    :routes (mapv (fn [[flow route]] {:flow flow :route route})
                  (sort-by key (route-records)))
    :retirements (store/retirements (root))}))
(defn route-matches-agent? [route agent]
  (and (= (:session route) (:session agent))
       (= (:name route) (:name agent))
       (= (:pane_id route) (:pane_id agent))
       (= (:terminal_id route) (:terminal_id agent))
       (= (:agent route) (:agent agent))))
(defn listing! []
  (let [records (route-records)
        agents (live-agents)
        live-rows (for [agent agents
                        :let [flows (->> records
                                         (keep (fn [[flow route]] (when (route-matches-agent? route agent) flow)))
                                         sort)]]
                    (str (if (seq flows) (str/join "," flows) "-") "\t"
                         (or (:name agent) "-") "\t" (:session agent) "\t"
                         (or (:agent_status agent) "unknown")))
        matched (set (mapcat (fn [agent]
                               (keep (fn [[flow route]] (when (route-matches-agent? route agent) flow)) records))
                             agents))
        stale-rows (for [[flow route] (sort-by key (remove (fn [[flow _]] (contains? matched flow)) records))]
                     (str flow "\t" (:name route) "\t" (:session route) "\tSTALE"))]
    (str/join "\n" (concat ["FLOW\tAGENT\tSESSION\tSTATE"] live-rows stale-rows))))
(ns messenger-clj.main
  (:require [messenger-clj.core :as hm]
            [messenger-clj.legacy-import :as legacy]))
(defn usage [] (str "Usage: messenger-clj <send|send-abrupt|register|deregister|rebind|move|retire|import-retirement|import-json|heartbeat-state|list> ...\n"
                    "  messenger-clj send TARGET BODY [--wait-presented] [--hold-seconds N] [--pane SESSION:PANE]\n"
                    "  messenger-clj send TARGET --psyche CONTEXT VERBATIM [--wait-presented] [--hold-seconds N] [--pane SESSION:PANE]\n"
                    hm/skill-note))
(defn arg [xs option] (second (drop-while #(not= option %) xs)))
(defn parse-error [message] (throw (ex-info message {:hm/parse true})))
(def value-options #{"--session" "--native-thread" "--readiness-probe" "--rollout" "--old-name" "--pane-id" "--terminal-id" "--name" "--agent" "--process-pid" "--evidence" "--evidence-sha256" "--hold-seconds" "--pane" "--target" "--receipt"})
(defn expand-equals [xs]
  (mapcat #(if-let [[_ option value] (re-matches #"(--[^=]+)=(.*)" %)] [option value] [%]) xs))
(defn normalize-options [xs]
  (let [xs (vec (expand-equals xs))]
    (loop [remaining xs positional [] options []]
      (if-let [value (first remaining)]
        (cond
          (contains? #{"--wait-presented" "--apply" "--psyche"} value) (recur (next remaining) positional (conj options value))
          (contains? value-options value) (if-let [argument (second remaining)]
                                            (recur (nnext remaining) positional (into options [value argument]))
                                            (parse-error (str "argument " value ": expected one argument")))
          (.startsWith value "--") (recur (next remaining) positional (conj options value))
          :else (recur (next remaining) (conj positional value) options))
        (into positional options)))))
(defn extra-values! [xs allowed]
  (loop [remaining xs]
    (when-let [value (first remaining)]
      (cond
        (contains? #{"--wait-presented" "--apply" "--psyche"} value) (recur (next remaining))
        (contains? value-options value) (recur (nnext remaining))
        (contains? allowed value) (recur (next remaining))
        :else (parse-error (str "unrecognized arguments: " value))))))
(defn unknown-flags! [xs allowed]
  (doseq [value xs :when (and (.startsWith value "--") (not (contains? allowed value)))]
    (parse-error (str "unrecognized arguments: " value))))
(defn -main [& argv]
  (try
    (let [[op & raw-xs] argv
          xs (normalize-options raw-xs)]
      (if (or (= op "--help") (= op "-h") (some #{"--help" "-h"} xs))
        (println (usage))
        (case op
          "send" (let [psyche? (boolean (some #{"--psyche"} xs))
                       [flow first-field second-field & tail] xs
                       [context body rest] (if psyche?
                                             [first-field second-field tail]
                                             [nil first-field (if (nil? second-field) tail (cons second-field tail))])]
                   (when-not (and flow body (or (not psyche?) context))
                     (parse-error (if psyche?
                                    "the following arguments are required: flow, --psyche, context, verbatim"
                                    "the following arguments are required: flow, message")))
                   (unknown-flags! rest #{"--psyche" "--wait-presented" "--hold-seconds" "--pane"})
                   (extra-values! rest #{"--psyche" "--wait-presented" "--hold-seconds" "--pane"})
                   (let [hold (try (Double/parseDouble (or (arg rest "--hold-seconds") "10"))
                                   (catch Exception _ (parse-error "argument --hold-seconds: invalid float value")))]
                     (when-not (<= 0 hold 60) (hm/fail "--hold-seconds must be between 0 and 60"))
                     (println (if psyche?
                                (hm/send-psyche! flow context body (boolean (some #{"--wait-presented"} rest)) (arg rest "--pane") hold)
                                (hm/send! flow body (boolean (some #{"--wait-presented"} rest)) (arg rest "--pane") hold)))))
          "send-abrupt" (let [psyche? (boolean (some #{"--psyche"} xs))
                              [flow first-field second-field & tail] xs
                              [context body rest] (if psyche?
                                                    [first-field second-field tail]
                                                    [nil first-field (if (nil? second-field) tail (cons second-field tail))])]
                          (when-not (and flow body (or (not psyche?) context))
                            (parse-error (if psyche?
                                           "the following arguments are required: flow, --psyche, context, verbatim"
                                           "the following arguments are required: flow, message")))
                          (unknown-flags! rest #{"--psyche" "--wait-presented" "--hold-seconds"})
                          (extra-values! rest #{"--psyche" "--wait-presented" "--hold-seconds"})
                          (let [hold (try (Double/parseDouble (or (arg rest "--hold-seconds") "10"))
                                          (catch Exception _ (parse-error "argument --hold-seconds: invalid float value")))]
                            (when-not (<= 0 hold 60) (hm/fail "--hold-seconds must be between 0 and 60")))
                          (println (if psyche?
                                     (hm/send-abrupt-psyche! flow context body (boolean (some #{"--wait-presented"} rest)))
                                     (hm/send-abrupt! flow body (boolean (some #{"--wait-presented"} rest))))))
          "register" (let [[flow name & rest] xs session (arg rest "--session") thread (arg rest "--native-thread")
                           marker (arg rest "--readiness-probe") rollout (arg rest "--rollout")]
                       (when-not (and flow name) (parse-error "the following arguments are required: flow, name"))
                       (unknown-flags! rest #{"--session" "--native-thread" "--readiness-probe" "--rollout"})
                       (extra-values! rest #{"--session" "--native-thread" "--readiness-probe" "--rollout"})
                       (println (hm/register! flow name session thread marker rollout)))
          "deregister" (let [[flow & rest] xs]
                         (when-not flow (parse-error "the following arguments are required: flow"))
                         (unknown-flags! rest #{"--session" "--pane-id" "--terminal-id" "--name"})
                         (extra-values! rest #{"--session" "--pane-id" "--terminal-id" "--name"})
                         (let [session (arg rest "--session") pane-id (arg rest "--pane-id") terminal-id (arg rest "--terminal-id") name (arg rest "--name")]
                           (when-not (every? some? [session pane-id terminal-id name])
                             (parse-error "the following arguments are required: --session, --pane-id, --terminal-id, --name"))
                           (println (hm/deregister! flow session pane-id terminal-id name))))
          "rebind" (let [[flow new-name & rest] xs]
                     (when-not (and flow new-name) (parse-error "the following arguments are required: flow, new_name"))
                     (unknown-flags! rest #{"--old-name" "--session" "--pane-id" "--terminal-id" "--agent" "--native-thread"})
                     (extra-values! rest #{"--old-name" "--session" "--pane-id" "--terminal-id" "--agent" "--native-thread"})
                     (let [old-name (arg rest "--old-name") session (arg rest "--session") pane-id (arg rest "--pane-id")
                           terminal-id (arg rest "--terminal-id") agent (arg rest "--agent") native-thread (arg rest "--native-thread")]
                       (when-not (every? some? [old-name session pane-id terminal-id agent native-thread])
                         (parse-error "the following arguments are required: --old-name, --session, --pane-id, --terminal-id, --agent, --native-thread"))
                       (println (hm/rebind! flow old-name new-name session pane-id terminal-id agent native-thread))))
          "move" (let [[flow workspace & rest] xs]
                   (when-not (and flow workspace) (parse-error "the following arguments are required: flow, workspace"))
                   (unknown-flags! rest #{"--session" "--pane-id" "--terminal-id" "--name" "--agent" "--native-thread" "--process-pid"})
                   (extra-values! rest #{"--session" "--pane-id" "--terminal-id" "--name" "--agent" "--native-thread" "--process-pid"})
                   (let [session (arg rest "--session") pane-id (arg rest "--pane-id") terminal-id (arg rest "--terminal-id")
                         name (arg rest "--name") agent (arg rest "--agent") native-thread (arg rest "--native-thread") pid-text (arg rest "--process-pid")]
                     (when-not (every? some? [session pane-id terminal-id name agent native-thread pid-text])
                       (parse-error "the following arguments are required: --session, --pane-id, --terminal-id, --name, --agent, --native-thread, --process-pid"))
                     (let [pid (or (try (parse-long pid-text) (catch Exception _ nil))
                                   (parse-error "argument --process-pid: invalid int value"))]
                       (println (hm/move! flow session pane-id terminal-id name agent native-thread pid workspace)))))
          ("retire" "import-retirement") (let [[flow & rest] xs]
                                           (when-not flow (parse-error "the following arguments are required: flow"))
                                           (unknown-flags! rest #{"--session" "--pane-id" "--terminal-id" "--name" "--agent" "--native-thread" "--evidence" "--evidence-sha256"})
                                           (extra-values! rest #{"--session" "--pane-id" "--terminal-id" "--name" "--agent" "--native-thread" "--evidence" "--evidence-sha256"})
                                           (let [session (arg rest "--session") pane-id (arg rest "--pane-id") terminal-id (arg rest "--terminal-id") name (arg rest "--name")
                                                 agent (arg rest "--agent") native-thread (arg rest "--native-thread") evidence (arg rest "--evidence") digest (arg rest "--evidence-sha256")]
                                             (when-not (every? some? [session pane-id terminal-id name agent native-thread evidence digest])
                                               (parse-error "the following arguments are required: --session, --pane-id, --terminal-id, --name, --agent, --native-thread, --evidence, --evidence-sha256"))
                                             (println (hm/retire! flow session pane-id terminal-id name agent native-thread evidence digest (= op "import-retirement")))))
          "import-json" (let [[source & rest] xs]
                          (when-not source (parse-error "the following arguments are required: source"))
                          (unknown-flags! rest #{"--target" "--receipt" "--apply"})
                          (extra-values! rest #{"--target" "--receipt" "--apply"})
                          (let [target (arg rest "--target") receipt (arg rest "--receipt")]
                            (when-not (and target receipt)
                              (parse-error "the following arguments are required: --target, --receipt"))
                            (println (legacy/import-json! source target receipt
                                                          (boolean (some #{"--apply"} rest))))))
          "heartbeat-state" (do (when (seq xs) (parse-error "unrecognized arguments"))
                                (println (hm/heartbeat-state!)))
          "list" (do (when (seq xs) (parse-error "unrecognized arguments")) (println (hm/listing!)))
          (parse-error (str "invalid choice: " op)))))
    (catch clojure.lang.ExceptionInfo e
      (binding [*out* *err*]
        (println (if (:hm/parse (ex-data e))
                   (str "usage: " (usage) "messenger-clj: error: " (.getMessage e))
                   (str (when-not (:hm/held (ex-data e)) "messenger-clj: ") (.getMessage e))))
        (System/exit (if (:hm/parse (ex-data e)) 2 1))))
    (catch Exception e
      (binding [*out* *err*]
        (println "messenger-clj:" (.getMessage e))
        (System/exit 1)))))
(ns user (:require [messenger-clj.main])) (apply messenger-clj.main/-main *command-line-args*)