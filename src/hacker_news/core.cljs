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

(defonce sync (fire/sync-stories app-state))
