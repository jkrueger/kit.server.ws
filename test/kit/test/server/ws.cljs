(ns kit.test.server.ws
  (:require
    [cljs.core.async :as async :refer (<!)]
    [kit.app.component :refer (<up <down Lifecycle system with)]
    [kit.server.ws :as ws]
    [latte.chai :refer (expect)])
  (:require-macros
    [cljs.core.async.macros :refer (go)]
    [kit.async :refer (<?)]
    [kit.core :refer (?)]
    [latte.core :refer (describe it)]))

(def chai (js/require "chai"))

(describe "WebSockets"

  (let [server (atom (ws/server {:port 8080}))
        client (atom (ws/client {:address "ws://localhost:8080/"}))]

    (it "open a websocket server when bringing the component up" [done]
      (go
        (try
          (reset! server (<? (<up @server)))
          (done)
          (catch js/Error e
            (done e)))))

    (it "open a websocket client when bringing the component up" [done]
      (go
        (try
          (reset! client (<? (<up @client)))
          (done)
          (catch js/Error e
            (done e)))))

    (it "closes the websocket client when bringing the component down" [done]
      (go
        (try
          (reset! client (<? (<down @client)))
          (done)
          (catch js/Error e
            (done e)))))

    (it "closes the websocket serverwhen bringing the component down" [done]
      (go
        (try
          (reset! server (<? (<down @server)))
          (done)
          (catch js/Error e
            (done e)))))))