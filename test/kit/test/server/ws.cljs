(ns kit.test.server.ws
  (:require
    [clojure.string :as str]
    [cljs.core.async :as async :refer (<! >!)]
    [kit.app.component :refer (<up <down Lifecycle system with)]
    [kit.server.ws :as ws]
    [latte.chai :refer (expect)])
  (:require-macros
    [cljs.core.async.macros :refer (go)]
    [kit.async :refer (<?)]
    [kit.core :refer (?)]
    [latte.core :refer (describe it)]))

(def chai (js/require "chai"))

(def port    {:port 8080})
(def address {:address (str "ws://localhost:" (:port port))})

(describe "WebSockets"

  (let [server    (atom (ws/server port))
        client    (atom (ws/client address))
        backlog   (atom nil)
        remote    (atom nil)
        remote-ch (atom nil)
        client-ch (atom nil)]

    (it "open a websocket server when bringing the component up" [done]
      (go
        (try
          (reset! server (<? (<up @server)))
          (reset! backlog (ws/<accept @server))
          (done)
          (catch js/Error e
            (done e)))))

    (it "open a websocket client when bringing the component up" [done]
      (go
        (try
          (reset! client (<? (<up @client)))
          (reset! remote (<! @backlog))
          (reset! remote-ch (ws/<messages @remote))
          (reset! client-ch (ws/<messages @client))
          (done)
          (catch js/Error e
            (done e)))))

    (it "allow a connected client and server to send messages to each other" [done]
      (go
        (try
          (ws/send @client {:msg "foo"})
          (let [msg (<! @remote-ch)]
            (expect (:msg msg) :to.equal "foo"))
          (ws/send @remote {:msg "bar"})
          (let [msg (<! @client-ch)]
            (expect (:msg msg) :to.equal "bar"))
          (done)
          (catch js/Error e
            (done e)))))

    (it "propagates errors through async channels" [done]
      (go
        (try
          (ws/raw @client "[1, 2, 3")
          (let [err (<! @remote-ch)]
            (expect err :to.be.an.instanceof js/Error))
          (done)
          (catch js/Error e
            (done e)))))

    (it "allows custom parsers when reading messages" [done]
      (go
        (try
          (let [client    (<! (<up (ws/client address)))
                remote    (<! @backlog)
                parser    #(mapv js/parseInt (str/split % #","))
                remote-ch (ws/<messages remote parser)]
            (ws/raw client "1,2,3")
            (let [msg (<! remote-ch)]
              (expect msg :to.eql [1,2,3]))
            (done))
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
