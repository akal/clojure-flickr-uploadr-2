(ns devtools)

(use 'clojure.reflect 'clojure.pprint)

;; cider keyboard shortcuts
;; http://cider.readthedocs.io/en/latest/interactive_programming/
;; https://www.cheatography.com/bilus/cheat-sheets/emacs-cider/


;; http://brownsofa.org/blog/2014/08/03/debugging-in-clojure-tools/


(defn -info [x]
  (pprint (reflect x)))

(defn -all-methods [x]
    (->> x reflect
           :members
           (filter :return-type)
           (map :name)
           sort
           (map #(str "." %) )
           distinct
           println))

(defn -classpath []
  (println (seq (.getURLs (java.lang.ClassLoader/getSystemClassLoader)))))
