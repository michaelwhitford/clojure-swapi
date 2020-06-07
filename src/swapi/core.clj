(ns swapi.core
  (:require [clojure.string :as string]
            [clojure.pprint :refer [pp pprint]]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [manifold.deferred :as d]
            [manifold.executor :refer [fixed-thread-executor]]
            [medley.core :as m]))

(defn reset []
  (require 'swapi.core :reload-all))

(def config (atom {:url "https://swapi.dev/api"
                   :methods #{"people" "planets" "films"
                              "starships" "vehicles" "species"}
                   :options {:as :json
                             ; :debug true
                             :accept :json}
                   :pool (fixed-thread-executor 4)}))

(defn api!
  "get all api resources and urls"
  []
  (let [url (:url @config)
        options (:options @config)]
    (:body (http/get url options))))

(defn add-api!
  "add api resources to config"
  []
  (let [m (reduce-kv (fn [m k v] (assoc m k [:url v])) {} (api!))]
    (swap! config assoc :resources m))
  nil)

(add-api!)

(defn allowed?
  "check for method in config set
    `method` required: config set"
  [method]
  (contains? (:methods @config) method))

(defn query!
  "query to swapi.dev
    `method` required: config set
    `id` optional"
  [method & args]
  (if (allowed? method)
    (let [id (first args)
          url (:url @config)
          options (:options @config)]
      (if id
        (:body (http/get (format "%s/%s/%s" url method id) options))
        (:results (:body (http/get (format "%s/%s" url method) options)))))
    nil))

(defn query+
  "asynchronous query to swapi.dev
    `method` required: config set
    `id` optional"
  [method & args]
  (if (allowed? method)
    (let [id (first args)
          deferred (d/deferred (:pool @config))
          chain (d/chain deferred #(future (if id (query! method %) (query! %))))]
      (d/success! deferred (or id method))
      chain)
    nil))

(defmacro create-queries!
  "Create 3 new functions for one method of the api
    `method` required: config set"
  [method]
  (let [syn (format "%s!" method)
        asyn (format "%s+" method)
        schema (format "%s-schema!" method)]
    `(do
      (def ~(symbol syn) (partial query! ~method))
      (def ~(symbol asyn) (partial query+ ~method))
      (def ~(symbol schema) (partial query! ~method "schema")))))

(create-queries! "people")
(create-queries! "planets")
(create-queries! "films")
(create-queries! "species")
(create-queries! "vehicles")
(create-queries! "spaceships")

(defn add-schemas!
  "query for schemas and add to config"
  []
  (let [r (:resources @config)]
    (m/map-kv-vals (fn [k v] (assoc v :schema (query! (str (name k)) "schema"))) r)))
  ;(reduce #(assoc-in %1 [%2 :schema] (query! (str (name %2)) "schema")) r (keys r))))
  ;(m/map-kv-vals (fn [k v] (assoc v :schema (query! (str (name k)) "schema"))) r))
  ;(reduce-kv (fn [m k v] (assoc m k (assoc v :schema (query! (str (name k)) "schema")))) {} r))

(comment
  (people! 1)

  (def person (people+ 1))
  @person

  ; defaults to 10
  (def people (people+))
  @people

  (def first5 (apply d/zip (mapv people+ (range 1 6))))
  @first5
)
