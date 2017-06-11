(ns hacker-news.components
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljsjs.moment]
            [cljs.core.async :as a :refer [<! >! chan close!]]
            [hacker-news.firebase :as fire]
            [sablono.core :as sab]))

(defn comment-list [comment-ids comments data]
  [:div {:class "comment-list"}
   (for [comment-id comment-ids]
     (let [comment (get comments (keyword (str comment-id)))]
       (when comment
         [:div {:key (:id comment)
                :class ["comment"
                        (if (and (contains? (:hided-comment-ids @data) comment-id)
                                 (:kids comment))
                          "comment--hide-kids")]}
          [:div {:class "comment-meta"}
           [:span {:class "comment-meta__by"}
            (aget comment "by")]
           [:span {:class "comment-meta__time"}
            (.fromNow (js/moment (* (:time comment) 1000)))]]
          [:a {:class "comment-content"
               :on-click
               (fn []
                 (if (contains? (:hided-comment-ids @data) comment-id)
                   (swap! data update-in [:hided-comment-ids] #(disj %1 comment-id))
                   (swap! data update-in [:hided-comment-ids] #(conj %1 comment-id))))}
           [:div {:dangerouslySetInnerHTML #js {:__html (:text comment)}}]]
          (when (:kids comment)
            [:div (comment-list (:kids comment) comments data)])
          ])))])

(defn story [story comments data]
  (let [comment-ids (:kids story)]
    [:div {:key (str (:id story) (.getTime (js/Date.)))
           :data-id (:id story)
           :class ["story-item"
                   (when (= (:id story) (:focused-story-id @data))
                     "story-item--show-comments")]}
     [:a {:class "story-item__title"
          :on-click (fn []
                      (if-not (:focused-story-id @data)
                        (do
                          (go
                            (let [tmp (atom {:comments {}})]
                              (<! (fire/get-comments (:id story) tmp))
                              (swap! data assoc :comments (:comments @tmp))))
                          (swap! data assoc :focused-story-id (:id story)))
                        (swap! data assoc :focused-story-id nil)))}
      (+ 1 (first (fire/index-by-id (:id story) (:stories @data))))
      ". "
      (:title story)]
     [:div {:class "story-item__url"}
      [:a {:href (:url story)
           :target "_blank"}
       (:url story)]]
     [:div {:class "story-meta"}
      [:div {:class "story-meta__comment-count"}
       (:descendants story) " comments"]
      [:div {:class "story-meta__score"}
       (:score story) " points"]]
     (when (= (:id story) (:focused-story-id @data))
       (comment-list comment-ids comments data))]
    ))

(defn more [data]
  [:button {:class "load-more"
            :on-click (fn []
                        (fire/sync-stories data))}
   "More"])

(defn close [data]
  [:a {:class "close-comment"
       :on-click
       (fn []
         (let [query (str "[data-id='" (:focused-story-id @data) "']")]
           (.scrollIntoView (.querySelector js/document query)))
         (swap! data assoc :focused-story-id nil))}
   "Close"]
  )

(defn root [data]
  (sab/html
   [:div
    (if (:focused-story-id @data)
      (close data))
    [:div {:class "story-list"}
     (for [story-obj (:stories @data)] (story story-obj (:comments @data) data))]
    (more data)]))
