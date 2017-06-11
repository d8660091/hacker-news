(ns hacker-news.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljsjs.firebase]
            [cljs.core.async :refer [<! >! chan close!]]
            [cljs.core.async.impl.timers :as timers :refer [timeout]]
            [hacker-news.firebase :as fire]
            [hacker-news.components :refer [root]]))

(enable-console-print!)

(defonce app-state (atom {:stories []
                          :comments {}
                          :focused-story-id nil
                          :hided-comment-ids #{}}))

(defn render! []
  (.render js/ReactDOM
           (root app-state)
           (.getElementById js/document "app")))

(add-watch app-state :on-change (fn [_ _ _ _] (render!)))

(render!)

(defonce sync-stories
  (go
    (let [story-ids (js->clj (<! (fire/get-data! (.limitToFirst (fire/get-ref "topstories") 20))))]
      (doall (for [story-id story-ids]
               (let [story-chan (fire/get-story! story-id)]
                 (go-loop [] (let [story (<! story-chan)
                                   stories (:stories @app-state)
                                   i (first (keep-indexed #(if (= (.-id %2) (.-id story)) %1) stories))]
                               (if i (swap! app-state assoc-in [:stories i] story)
                                   (swap! app-state update-in [:stories]
                                          #(conj %1 story)))
                               (recur)))))))))
