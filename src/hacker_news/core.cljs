(ns hacker-news.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljsjs.firebase]
            [cljs.core.async :refer [<! >! chan close!]]
            [cljs.core.async.impl.timers :as timers :refer [timeout]]
            [reagent.core :as r]
            [hacker-news.firebase :as fire]
            [hacker-news.components :refer [root]]))

(enable-console-print!)

(defn get-viewed-story-ids []
  (take-last 30 (set (js->clj (js/JSON.parse (js/localStorage.getItem "viewed-story-ids"))))))

(defonce app-state (r/atom {:stories []
                            :comments {}
                            :focused-story-id nil
                            :viewed-story-ids (get-viewed-story-ids)
                            :hided-comment-ids #{}}))

(add-watch app-state :saveViewed
           (fn [key atom _ new-state]
             (js/localStorage.setItem "viewed-story-ids"
                                      (js/JSON.stringify (clj->js (:viewed-story-ids new-state))))))

(defn render! []
  (r/render [root app-state]
           (.getElementById js/document "app")))

(render!)

(defonce sync (fire/sync-stories app-state))
