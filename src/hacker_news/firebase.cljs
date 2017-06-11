(ns hacker-news.firebase
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljsjs.firebase]
            [cljs.core.async :as a :refer [<! >! chan close!]]
            ))

(defonce firebase-app
  (js/firebase.initializeApp
   #js{:databaseURL "https://hacker-news.firebaseio.com/"}))

(defn get-ref [path]
  "get firebase ref for a path in hacker news database"
  (.ref (js/firebase.database firebase-app)
        (str "v0/" path)))

;; (defonce topstories-ref (get-ref "topstories"))

(defn get-data! [data-ref]
  "get hacker news story from a story id"
  (let [c (chan 1)]
    (.on data-ref "value"
         (fn [data-snapshot]
           (go (>! c (.val data-snapshot)))))
    c))

(defn get-comments [item-id result]
  (let [item-ref (get-ref (str "item/" item-id))
        c (chan)]
    (do
      (.once item-ref "value"
             (fn [data-snapshot]
               (let [comment (.val data-snapshot)
                     comment-id (.-id comment)
                     key (keyword (str comment-id))]
                 (when-not (get-in result [:comments key])
                   (swap! result assoc-in [:comments key] comment)
                   (go
                     (<! (a/map identity
                                (let [kids (js->clj (.-kids comment))]
                                  (if kids
                                    (for [item-id-2 kids]
                                      (get-comments item-id-2 result))
                                    [(go "complete")]))))
                     (>! c "complete"))))))
      c)))

(defn get-story! [story-id]
  "get a story chan from a story id"
  (let [story-ref (get-ref (str "item/" story-id))]
    (get-data! story-ref)))
