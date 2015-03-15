(ns kit.server.ws
  (:require
    [kit.app.component :as comp]
    [kit.app.log :as log])
  (:require-macros
    [kit.core :refer (? !)]))

(def WS  (js/require "ws"))
(def WSS (.-Server WS))

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
      (.close sock)
      (reset! sock nil)
      (next)
      (catch js/Error e
        (next e)))))

(defrecord Client [sock opts]
  comp/Lifecycle
  (up [_ next]
    (try
      (reset! sock (Server. opts))
      (.on @sock "open" next)
      (catch js/Error e
        (next e))))
  (down [_ next]
    (try
      (.close sock)
      (reset! sock nil)
      (next)
      (catch js/Error e
        (next e)))))

(defn server [opts]
  (let [opts (clj->js opts)]
    (Server. (atom nil) opts)))

(defn client [opts]
  (Client. (WS. address)))

(defn send [this]
  (.send @(:sock this)))

(defn on [this evt f]
  (.on @(:sock this) (name evt) f))
