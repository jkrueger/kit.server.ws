(ns kit.server.ws
  (:require
    [cljs.core.async :as async]
    [kit.app.component :as comp]
    [kit.async :as a])
  (:require-macros
    [kit.core :refer (? !)]))

(def WS  (js/require "ws"))
(def WSS (.-Server WS))

(defprotocol Socket
  (send [_ msg])
  (raw [_ msg]))

(defn parse [x]
  (try
    (js->clj (js/JSON.parse x) :keywordize-keys true)
    (catch js/Error e
      e)))

(extend-type WS
  a/EventSource
  (on [this evt f]
    (.on this (name evt) f))
  Socket
  (send [this msg]
    (.send this (js/JSON.stringify (clj->js msg))))
  (raw [this s]
    (.send this s)))

(defrecord Server [sock opts]
  comp/Lifecycle
  (up [_ next]
    (try
      (reset! sock (WSS. opts))
      (next)
      (catch js/Error e
        (next e))))
  (down [_ next]
    (try
      (.close @sock)
      (reset! sock nil)
      (next)
      (catch js/Error e
        (next e))))
  a/EventSource
  (on [_ evt f]
    (when @sock
      (.on @sock (name evt) f))))

(defrecord Client [sock opts]
  comp/Lifecycle
  (up [_ next]
    (try
      (reset! sock (WS. (? opts :address)))
      (.on @sock "open" next)
      (catch js/Error e
        (next e))))
  (down [_ next]
    (try
      (.close @sock)
      (reset! sock nil)
      (next)
      (catch js/Error e
        (next e))))
  a/EventSource
  (on [_ evt f]
    (when @sock
      (on @sock evt f)))
  Socket
  (send [this msg]
    (send @sock msg))
  (raw [this s]
    (raw @sock s)))

(defn server [opts]
  (let [opts (clj->js opts)]
    (Server. (atom nil) opts)))

(defn client [opts]
  (let [opts (clj->js opts)]
    (Client. (atom nil) opts)))

(defn <messages
  "Returns a new channel which will contain all
  the messages received from sock. Optionally a parse
  function can be supplied, which will be applied to
  each message received on the socket. If no parser is
  specified the messages are assumed to be json formatted
  strings"
  ([sock]
   (<messages sock parse))
  ([sock parse-fn]
   (let [ch (<on sock :message)]
     (.on sock "close"
       (fn []
         (async/close! ch)))
     (async/map parse-fn [ch]))))

(defn <accept
  "Returns a new "
  [server]
  (<on server :connection))

(defn <sink
  "Returns a channel that can be written to. Each value
   put on the channel will be send over the socket"
  [sock]
  (let [channel (chan)]
    (go
      (loop []
        (when-let [msg (<! channel)]
          (send sock msg)
          (recur))))))
