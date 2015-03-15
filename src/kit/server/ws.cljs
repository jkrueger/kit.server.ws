(ns kit.server.ws
  (:require
    [kit.app.component :as comp]
    [kit.async :as a])
  (:require-macros
    [kit.core :refer (? !)]))

(def WS  (js/require "ws"))
(def WSS (.-Server WS))

(defprotocol EventSource
  (on [_ evt f]))

(defprotocol Socket
  (send [_ msg])
  (raw [_ msg]))

(defn parse [x]
  (js->clj x :keywordize-keys true))

(extend-type WS
  EventSource
  (on [this evt f]
    (.on this (name evt)
      (fn [x]
        (try
          (f (parse (js/JSON.parse x)))
          (catch js/Error e
            (f e))))))
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
  EventSource
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
  EventSource
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

(def <on (partial a/lift on))

(defn <messages [sock]
  (<on sock :message))

(defn <accept [server]
  (<on server :connection))
