(ns guestbook.core
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [ajax.core :refer [GET POST]]
            [clojure.string :as string]
            [guestbook.validation :refer [validate-message]]))

(defn get-messages [messages]
  (GET "/messages"
       {:headers {"Accept" "application/transit+json"}
        :handler #(reset! messages (:messages %))}))

(defn send-message! [fields errors messages]
  (if-let [validation-errors (validate-message @fields)]
    (reset! errors validation-errors)
    (POST "/message"
      {:params @fields
       :format :json
       :headers
       {"Accept" "application/transit+json"
        "x-csrf-token" (.-value (.getElementById js/document "token"))}
       :handler (fn [_]
                  (swap! messages conj (assoc @fields
                                              :timestamp (js/Date.)))
                  (reset! fields nil)
                  (reset! errors nil))
       :error-handler (fn [e]
                        (.error js/console (str e))
                        (reset! errors (-> e :response :errors)))})))

(defn message-list [messages]
  (println messages)
  [:ul.messages
   (for [{:keys [timestamp message name]} @messages]
     ^{:key timestamp}
     [:li
      [:time (.toLocaleString timestamp)]
      [:p message]
      [:p " - " name]])])

(defn error-component [errors id]
  (when-let [error (id @errors)]
    [:div.notification.is-danger (string/join error)]))

(defn message-form [messages]
  (let [fields (r/atom {})
        errors (r/atom nil)]
    (fn []
      [:div
       [error-component errors :server-error]
       [:div.field
        [:label.label {:for :name} "Name"]
        [error-component errors :name]
        [:input.input {:type :text
                       :name :name
                       :on-change #(swap! fields
                                          assoc :name (-> % .-target .-value))
                       :value (:name @fields)}]]
       [:div.field
        [:label.label {:for :message} "Message"]
        [error-component errors :message]
        [:textarea.textarea {:name :message
                             :value (:message @fields)
                             :on-change #(swap! fields
                                                assoc :message (-> % .-target .-value))}]]
       [:input.button.is-primary {:type :submit
                                  :value "comment"
                                  :on-click #(send-message! fields errors messages)}]])))

(defn home []
  (let [messages (r/atom nil)]
    (get-messages messages)
    (fn []
      [:div.content>div.columns.is-centered>div.column.is-two-thirds
       [:div.columns>div.column
        [:h3 "Messages"]
        [message-list messages]]
       [:div.columns>div.column
        [message-form messages]]])))

(dom/render
 [home]
 (.getElementById js/document "content"))
