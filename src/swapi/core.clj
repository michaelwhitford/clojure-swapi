(ns swapi.core
  (:require [clojure.string :as string]
            [clojure.pprint :refer [pp pprint]]
            [cheshire.core :as json]
            [clj-http.client :as http]
            [manifold.deferred :as d]
            [manifold.executor :refer [fixed-thread-executor]]))

(defn reset []
  (shutdown-agents)
  (require 'swapi.core :reload-all))

(def config (atom {:url "https://swapi.dev/api"
                   :actions #{"people" "planets" "films"
                              "starships" "vehicles" "species"}))

(def options (atom {:as :json
                    ; :debug true
                    :accept :json}))
(defn query!
  "query to swapi.dev
    `action` required: config set
    `id` optional"
  [action & args]
  (if (contains? (:actions @config) action)
    (if-let [id (first args)]
      (:body (http/get (format "%s/%s/%s" (:url @config) action id) @options))
      (:results (:body (http/get (format "%s/%s" (:url @config) action) @options))))
    nil))

(defn query+
  "asynchronous query to swapi.dev
    `action` required: config set
    `id` optional"
  [pool action & args]
  (if (contains? (:actions @config) action)
    (let [id (first args)
          deferred (d/deferred pool)
          chain (d/chain deferred #(future (if id (query! action %) (query! %))))]
      (d/success! deferred (or id action))
      chain)
    nil)

(defmacro create-queries!
  "Create 3 new functions for one action of the api
  `action` required: config set"
  [action]
  (def (str 'action "!") (partial query! 'action))
  (def (str 'action "+") (partial query+ pool 'action))
  (def (str 'action "-schema!") (partial (str 'action "!") 'action)))

(def pool (fixed-thread-executor 4))

(create-queries! "people")
(create-queries! "planets")
(create-queries! "films")
(create-queries! "starships")
(create-queries! "vehicles")
(create-queries! "species")

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
