(ns rob.core
  (:use compojure.core)
  (:use ring.middleware.json-params)
  (:require [clj-json.core :as json])
  (:use ring.adapter.jetty)
  (:import java.util.Random)
  (:use clojure.contrib.generic.functor))

; Game state and changer

(def random (Random.))
(defn sample [coll] (nth coll (.nextInt random (count coll))))
(defn make-card [type] {:type type, :time 20})
(def cards (map make-card [:forward :backward :turn-left :turn-right :fast-forward :fire]))
(defn random-deck [] (for [i (range 5)] (sample cards)))
(defn initial-player [x y]
  {:x x, :y y, :direction :north, :queue [], :deck (random-deck)})
(defn random-game-state []
  {:players {0 (initial-player 0 3), 1 (initial-player 0 5)}
	    :goal [6 4]})
(def game-state (atom (random-game-state)))

; Playing cards

(defn remove-nth [n coll] (concat (take n coll) (drop (inc n) coll)))
(defn play-card [current-game-state player-id card-number]
  (let [player-state (get-in current-game-state [:players player-id])
	card (nth (:deck player-state) card-number)
	remaining-cards (remove-nth card-number (:deck player-state))
	new-queue (conj (:queue player-state) card)]
    (assoc-in
     current-game-state [:players player-id]
     (assoc player-state
       :deck (conj remaining-cards (sample cards))
       :queue new-queue))))

; Advancing time

(defn move [steps]
  (fn [player-state]
    (case (:direction player-state)
	  :north (assoc player-state :y (+ (:y player-state) steps))
	  :south (assoc player-state :y (- (:y player-state) steps))
	  :east (assoc player-state :x (+ (:x player-state) steps))
	  :west (assoc player-state :x (- (:x player-state) steps)))))

(def clockwise-to {:north :east, :east :south, :south :west, :west :north})
(defn turn-clockwise [player-state]
  (assoc player-state :direction (clockwise-to (:direction player-state))))
(defn turn [times]
  (fn [player-state] (nth (iterate turn-clockwise player-state) times)))

(def player-actions
     {:forward (move 1)
      :backward (move -1)
      :turn-left (turn 3)
      :turn-right (turn 1)
      :fast-forward (move 2)
      :fire identity})

(defn advance-time-for-player [player-state]
  (let [queue (:queue player-state)]
    (if (empty? queue)
      player-state
      (let [top-card (first queue)
	    top-card-time (:time top-card)]
	(if (= 0 top-card-time)
	  (assoc
	      ((player-actions (:type top-card)) player-state)
	    :queue (rest queue))
	  (assoc player-state
	    :queue (cons (assoc top-card :time (dec top-card-time)) (rest queue))))))))
    
(defn advance-time [current-game-state]
  (assoc current-game-state :players (fmap advance-time-for-player (:players current-game-state))))

; Web-server magic

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

(defn play-card-on-game-state [player-id card-number]
  (swap! game-state play-card player-id card-number)
  {:status "success"})

(defroutes handler
  (GET "/" [] (slurp "index.html"))
  (GET "/js/core.js" [] (slurp "js/core.js"))
  (GET "/game-state" [] (json-response @game-state))
  (GET "/advance-time" [] (swap! game-state advance-time))
  (POST "/play-card" [player-id card-number] (json-response (play-card-on-game-state player-id card-number))))

(def app
     (-> handler
	 wrap-json-params))

(future (run-jetty (var app) {:port 8080}))