(ns swapi.core
  (:require [clojure.string :as string]
            [clojure.pprint :refer [pp pprint]]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [manifold.deferred :as d]
            [manifold.executor :refer [fixed-thread-executor]]))

(defn reset []
  (require 'swapi.core :reload-all))

(def url (atom "https://swapi.dev/api"))

(def options (atom {:as :json
                    ; :debug true
                    :accept :json}))

(defn query!
  "query to swapi.dev
    `action` required: people planets films starships vehicles species
    `id` optional"
  [action & args]
  (if-let [id (first args)]
    (:body (http/get (format "%s/%s/%s" @url action id) @options))
    (:results (:body (http/get (format "%s/%s" @url action) @options)))))

(defn query+
  "asynchronous query to swapi.dev
    `action` required: people planets films starships vehicles species
    `id` optional"
  [pool action & args]
  (let [deferred (d/deferred pool)]
    (if-let [id (first args)]
      (let [chain (d/chain deferred #(future (query! action %)))]
        (d/success! deferred id)
        chain)
      (let [chain (d/chain deferred #(future (query! %)))]
        (d/success! deferred action)
        chain))))

(def pool (fixed-thread-executor 4))

(def people! (partial query! "people"))
(def people+ (partial query+ pool "people"))
(def people-schema! (partial people! "schema"))

(def planets! (partial query! "planets"))
(def planets+ (partial query+ pool "planets"))
(def planets-schema! (partial planets! "schema"))

(def films! (partial query! "films"))
(def films+ (partial query+ pool "films"))
(def films-schema! (partial films! "schema"))

(def starships! (partial query! "starships"))
(def starships+ (partial query+ pool "starships"))
(def starships-schema! (partial starships! "schema"))

(def vehicles! (partial query! "vehicles"))
(def vehicles+ (partial query+ pool "vehicles"))
(def vehicles-schema! (partial vehicles! "schema"))

(def species! (partial query! "species"))
(def species+ (partial query+ pool "species"))
(def species-schema (partial species! "schema"))

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
