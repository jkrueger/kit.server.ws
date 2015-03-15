(ns kit.test
  (:require
    [kit.core :as core]
    [kit.test.server.ws :as rest]))

(core/enable-source-maps)

(set! *main-cli-fn* (fn []))
