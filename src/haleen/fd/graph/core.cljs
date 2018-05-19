(ns haleen.fd.graph.core
  (:require [cljsjs.d3]
            [clojure.string :as s]
            [cljs.core.async :refer [<! alt! chan close! go go-loop put! sliding-buffer]]
            [goog.dom :as dom]
            [goog.events :as events]
            [goog.events.EventType :as EventType]))


(enable-console-print!)

(def app-state
  (atom {:simulation nil}))

(def color (.scaleOrdinal js/d3 (.-schemeCategory20 js/d3)))

(defn append-svg []
  (-> js/d3
      (.select "#app")
      (.append "svg")
      (.classed "graph" true)))

(defn remove-svg []
  (-> js/d3
      (.selectAll "#app svg")
      (.remove)))

(defn ticked [link node]
  (fn tick []
    (-> link
        (.attr "x1" (fn [d] (.. d -source -x)))
        (.attr "y1" (fn [d] (.. d -source -y)))
        (.attr "x2" (fn [d] (.. d -target -x)))
        (.attr "y2" (fn [d] (.. d -target -y))))

    (-> node
        (.attr "cx" (fn [d] (.-x d)))
        (.attr "cy" (fn [d] (.-y d))))))

(defn simulation [height width graph links nodes]
  (let [sim
        (-> js/d3
            (.forceSimulation)
            (.force "link" (-> js/d3
                               (.forceLink)
                               (.id (fn [d] (.-id d)))))
            (.force "charge" (.forceManyBody js/d3))
            (.force "center" (.forceCenter js/d3
                                           (/ width 2)
                                           (/ height 2))))]
    (-> sim
        (.nodes (.-nodes graph))
        (.on "tick" (ticked links nodes)))

    (-> sim
        (.force "link")
        (.links (.-links graph)))

    sim))

(defmulti drag (fn [event d idx group] (.-type event)))

(defmethod drag "start" [event d idx group]
  (if (zero? (.-active event))
    (-> (:simulation @app-state)
        (.alphaTarget 0.3)
        (.restart))))

(defmethod drag "drag" [event d idx group]
  (set! (.-fx d) (.-x event))
  (set! (.-fy d) (.-y event)))

(defmethod drag "end" [event d idx group]
  (if (zero? (.-active event))
    (.alphaTarget (:simulation @app-state) 0))
  (set! (.-fx d) nil)
  (set! (.-fy d) nil))

(defmethod drag :default [event d idx group] nil)

(defn drag-dispatcher [d idx group]
  (drag (.-event js/d3) d idx group))

(defn draw-graph [svg graph]
  (let [links
        (-> svg
            (.append "g")
            (.attr "class" "links")
            (.selectAll "line")
            (.data (.-links graph))
            (.enter)
            (.append "line")
            (.attr "stroke-width" (fn [d] (Math/sqrt (.-value d)))))
        nodes (doto
                  (-> svg
                      (.append "g")
                      (.attr "class" "nodes")
                      (.selectAll "circle")
                      (.data (.-nodes graph))
                      (.enter)
                      (.append "circle")
                      (.attr "r" 5)
                      (.attr "fill" (fn [d] (color (.-group d))))
                      (.call (-> (.drag js/d3)
                                 (.on "start" drag-dispatcher)
                                 (.on "drag" drag-dispatcher)
                                 (.on "end" drag-dispatcher))))
                (.append "title")
                (.text (fn [d] (.-id d))))]
    [links nodes]))

(defn get-data [url ch-success ch-error]
  (go (.json js/d3 url
             (fn [err data]
               (if err
                 (put! ch-error err)
                 (put! ch-success data))))))

(defn refresh-simulation-force [event]
  (let [window-size (dom/getViewportSize (.-target event))
        width  (.-width window-size)
        height (.-height window-size)
        sim (:simulation @app-state)]

    (.force sim "center" (.forceCenter js/d3 (/ width 2) (/ height 2)))

    (-> sim
        (.alphaTarget 0.3)
        (.restart))))

(defn handle-window-resize [ch]
  ;;subscribe to window.resize
  (events/listen js/window
                 EventType/RESIZE
                 (fn [event]
                   (put! ch event)))

  ;; handle resize
  (go-loop [event (<! ch)]
    (when event
      (refresh-simulation-force event))
    (recur (<! ch))))

(defn ^:export main []
  (let [window-size (dom/getViewportSize (dom/getWindow))
        width  (.-width window-size)
        height (.-height window-size)
        svg (append-svg)
        data-channel (chan 1)
        data-error (chan 1)
        window-resized-channel (chan (sliding-buffer 5))]
    ;; handler
    (handle-window-resize window-resized-channel)
    ;; get data
    (go
      (get-data "miserables.json" data-channel data-error))
    (go
      (alt!
        data-error ([err] (throw err))
        data-channel ([data]
                      (let [graph (clj->js data)
                            [links nodes] (draw-graph svg graph)]
                        ;; init simulation
                        (swap! app-state
                               assoc :simulation (simulation height width graph links nodes)))))
      (close! data-error)
      (close! data-channel))))

(defn on-js-reload []
  (remove-svg)
  (main))

