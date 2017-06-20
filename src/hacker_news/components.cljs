(ns hacker-news.components
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljsjs.moment]
            [cljsjs.react-transition-group]
            [cljs.core.async :as a :refer [<! >! chan close!]]
            [hacker-news.firebase :as fire]
            [sablono.core :as sab]))

(defn loading []
  [:img {:class "loading"
         :src "images/loader.svg"}])

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
            (:by comment)]
           [:span {:class "comment-meta__time"}
            (.fromNow (js/moment (* (:time comment) 1000)))]]
          [:a {:class "comment-content"
               :on-click
               (fn [e]
                 (when-not
                     (and (= (.-tagName (.-target e)) "A")
                          (not (.contains (.-classList (.-target e)) "comment-content")))
                   (if (contains? (:hided-comment-ids @data) comment-id)
                     (swap! data update-in [:hided-comment-ids] #(disj %1 comment-id))
                     (swap! data update-in [:hided-comment-ids] #(conj %1 comment-id)))))}
           [:div {:dangerouslySetInnerHTML #js {:__html (or (:text comment) "[deleted]")}}]]
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
                      (if-not (= (:focused-story-id @data) (:id story))
                        (do
                          (js/setTimeout #(.scrollIntoView
                            (.querySelector
                             js/document
                             (str "[data-id='" (:id story) "']"))) 100)
                          (go
                            (let [tmp (atom {:comments {}})]
                              (<! (fire/get-comments (:id story) tmp))
                              (swap! data assoc :comments (:comments @tmp))))
                          (swap! data assoc :focused-story-id (:id story)))
                        (swap! data assoc :focused-story-id nil)))}
      [:span {:class "story-item__number"}
       (+ 1 (first (fire/index-by-id (:id story) (:stories @data))))
       ". "]
      (:title story)]
     [:div {:class "story-item__url"}
      [:a {:href (:url story)
           :target "_blank"}
       (:url story)]]
     [:div {:class "story-meta"}
      [:div {:class "story-meta__comment-count"}
       (or (:descendants story) "0") " comments"]
      [:div {:class "story-meta__score"}
       (:score story) " points"]]
     (when (= (:id story) (:focused-story-id @data))
       (comment-list comment-ids comments data))]))

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
   "Close"])

(defn root [data]
  (sab/html
   [:div
    (when (empty? (:stories @data))
      (loading))
    (if (:focused-story-id @data)
      (close data))
    [:div {:class "story-list"}
     [:css-trans-group {:transition-name "fade"
                        :transitionEnterTimeout 5000}
      (for [story-obj (:stories @data)] (story story-obj (:comments @data) data))]]
    (when-not (empty? (:stories @data)) (more data))]))
