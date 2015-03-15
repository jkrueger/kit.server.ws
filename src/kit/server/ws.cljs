(ns kit.server.ws
  (:require
    [kit.app.component :as comp]
    [kit.async :as a])
  (:require-macros
    [kit.core :refer (? !)]))

(def WS  (js/require "ws"))
(def WSS (.-Server WS))

(defprotocol Socket
  (on [_ evt f])
  (send [_ msg]))

(extend-type WS
  Socket
  (on [this evt f]
    (.on this (name evt) f))
  (send [this msg]
    (.send @(:sock this) (clj->js msg))))

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
  Socket
  (on [_ evt f]
    (when @sock
      (.on @sock (name evt) f)))
  (send [this msg]
    (.send @(:sock this) (clj->js msg))))

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
  Socket
  (on [_ evt f]
    (when @sock
      (.on @sock (name evt) f)))
  (send [this msg]
    (.send @(:sock this) (clj->js msg))))

(defn server [opts]
  (let [opts (clj->js opts)]
    (Server. (atom nil) opts)))

(defn client [opts]
  (let [opts (clj->js opts)]
    (Client. (atom nil) opts)))

(def <on (partial a/lift on))
