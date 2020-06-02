(ns swapi.datahike
  (:require [clojure.string :as string]
            [clojure.pprint :refer [pp pprint]]
            [datahike.api :as d]))

(defn reset []
  (require 'swapi.datahike :reload-all))

(def cfg {:store {:backend :file :path "/home/mwhitford/clojure/swapi/data"}})

;(d/create-database cfg)

;(def conn (d/connect cfg))

(comment
  (reset)
)
