(ns hacker-news.components
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljsjs.moment]
            [cljs.core.async :as a :refer [<! >! chan close!]]
            [hacker-news.firebase :as fire]
            [reagent.core :as r]))

(def css-transition-group
  (r/adapt-react-class js/React.addons.CSSTransitionGroup))

(defn loading []
  [:img {:class "loading"
         :src "images/loader.svg"}])

(defn comment-list [comment-ids comments data]
  [:div {:class "comment-list"}
   [css-transition-group {:transition-name "fade"
                          :transitionEnterTimeout 300
                          :transitionLeaveTimeout 0}
    (doall
     (for [comment-id comment-ids]
       (let [comment (get comments (keyword (str comment-id)))]
         (if comment
           [:div {:key (:id comment)
                  :class (str "comment "
                          (if (and (contains? (:hided-comment-ids @data) comment-id)
                                   (:kids comment))
                            "comment--hide-kids"))}
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
            ]))))]])

(defn story [story comments data]
  (let [comment-ids (:kids story)]
    [:div {:key (str (:id story))
           :data-id (:id story)
           :class (str "story-item "
                   (when (= (:id story) (:focused-story-id @data))
                     "story-item--show-comments ")
                   (when (some #{(:id story)} (:viewed-story-ids @data))
                     "story-item--viewed"))}
     [:a {:class "story-item__title"
          :on-click (fn []
                      (if-not (= (:focused-story-id @data) (:id story))
                        (do
                          (js/setTimeout #(.scrollIntoView
                            (.querySelector
                             js/document
                             (str "[data-id='" (:id story) "']"))) 100)
                          (swap! data update-in [:viewed-story-ids] #(conj %1 (:id story)))
                          (go
                            (let [tmp (atom {:comments {}})]
                              (<! (fire/get-comments (:id story) tmp))
                              (swap! data assoc :comments (:comments @tmp))))
                          (swap! data assoc :focused-story-id (:id story)))
                        (swap! data assoc :focused-story-id nil)))}
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
       (if (contains? comments (keyword (str (first comment-ids))))
         (comment-list comment-ids comments data)
         (when (not (empty? comment-ids))
           (loading))))]))

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
  [:div
   (when (empty? (:stories @data))
     (loading))
   (if (:focused-story-id @data)
     (close data))
   [:div {:class "story-list"}
    [css-transition-group {:transition-name "fade"
                           :transitionEnterTimeout 300
                           :transitionLeaveTimeout 0}
     (doall (for [story-obj (:stories @data)] (story story-obj (:comments @data) data)))]]
   (when-not (empty? (:stories @data)) (more data))])
