(ns hacker-news.components
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljsjs.moment]
            [cljs.core.async :as a :refer [<! >! chan close!]]
            [hacker-news.firebase :as fire]
            [sablono.core :as sab]))

(defn comment-list [comment-ids comments data]
  (sab/html [:div {:class "comment-list"}
             (for [comment-id comment-ids]
               (let [comment (get comments (keyword (str comment-id)))]
                 (when comment
                   (sab/html [:div {:key (.-id comment)
                                    :class ["comment"
                                            (if (and (contains? (:hided-comment-ids @data) comment-id)
                                                     (.-kids comment))
                                              "comment--hide-kids")]}
                              [:div {:class "comment-meta"}
                               [:span {:class "comment-meta__by"}
                                (.-by comment)]
                               [:span {:class "comment-meta__time"}
                                (.fromNow (js/moment (* (.-time comment) 1000)))]]
                              [:a {:on-click (fn []
                                             (if (contains? (:hided-comment-ids @data) comment-id)
                                               (swap! data update-in [:hided-comment-ids] #(disj %1 comment-id))
                                               (swap! data update-in [:hided-comment-ids] #(conj %1 comment-id))))}
                               [:div {:dangerouslySetInnerHTML #js {:__html (.-text comment)}}]]
                              [:div (comment-list (js->clj (.-kids comment)) comments data)]
                              ]))))]))

(defn story [story comments data]
  (let [comment-ids (js->clj (.-kids story))]
    (sab/html [:div {:key (str (.-id story) (.getTime (js/Date.)))
                     :class ["story-item"
                             (when (= (.-id story) (:focused-story-id @data))
                               "story-item--show-comments")]}
               [:a {:class "story-item__title"
                     :on-click (fn []
                                 (if-not (:focused-story-id @data)
                                   (do
                                     (go
                                       (let [tmp (atom {:comments {}})]
                                         (<! (fire/get-comments (.-id story) tmp))
                                         (swap! data assoc :comments (:comments @tmp))))
                                     (swap! data assoc :focused-story-id (.-id story)))
                                   (swap! data assoc :focused-story-id nil)))}
                (.-title story)]
               [:div {:class "story-item__url"}
                [:a {:href (.-url story)
                     :target "_blank"}
                 (.-url story)]]
               [:div {:class "story-meta"}
                [:div {:class "story-meta__comment-count"}
                 (str (.-descendants story) " comments")]
                [:div {:class "story-meta__score"}
                 (str (.-score story) " points")]]
               (when (= (.-id story) (:focused-story-id @data))
                 (comment-list comment-ids comments data)
                 )
               ])))

(defn root [data]
  (sab/html [:div
             (if (:focused-story-id @data)
               [:a {:class "close-comment"
                    :on-click (fn [] (swap! data assoc :focused-story-id nil))}
                "Close"])
             [:div {:class "story-list"}
              (for [story-obj (:stories @data)] (story story-obj (:comments @data) data))]]))
