(ns rob.core
  (:use clojure.set)
  (:use compojure.core)
  (:use ring.util.response
	ring.middleware.json-params
	ring.middleware.keyword-params)
  (:require [clj-json.core :as json])
  (:use ring.adapter.jetty)
  (:import [java.util Random UUID]
	   [java.util.concurrent Executors TimeUnit])
  (:use clojure.contrib.generic.functor))

; Game state and changer

(def default-board-width 25)
(def default-board-height 25)

(def random (Random.))
(defn sample [coll] (nth coll (.nextInt random (count coll))))
(defn make-card [type] {:type type, :time 1})
(def cards (map make-card [:forward :backward :turn-left :turn-right :fast-forward :fire]))
(defn random-deck [] (for [i (range 5)] (sample cards)))
(defn initial-player [x y]
  {:x x, :y y, :direction :north, :queue [], :deck (random-deck)})
(defn random-wall [] {:x (.nextInt random default-board-width) :y (.nextInt random default-board-height)})
(defn random-walls [max-n goal]
  (difference (set (for [i (range max-n)] (random-wall))) #{goal}))
(defn random-game-state []
  {:players {}
   :goal {:x 12, :y 12}
   :walls (random-walls 50 {:x 12, :y 12})
   :board-width default-board-width
   :board-height default-board-height})

(def game-state (atom (random-game-state)))

(defn add-player [current-game-state player-id]
  (assoc-in current-game-state [:players player-id] (initial-player 0 0)))

(defn add-uuid-player-to-game-state []
  (let [uuid (.toString (UUID/randomUUID))]
    (swap! game-state add-player uuid)
    uuid))

; Playing cards

(defn switch-nth [n coll val]
  (concat (take n coll) [val] (drop (inc n) coll))) 
(defn play-card [current-game-state player-id card-number]
  (let [player-state (get-in current-game-state [:players player-id])
	card (nth (:deck player-state) card-number)
	new-deck (switch-nth card-number (:deck player-state) (sample cards))
	new-queue (concat (:queue player-state) [card])]
    (assoc-in
     current-game-state [:players player-id]
     (assoc player-state
       :deck new-deck
       :queue new-queue))))

; Advancing time

(defn clamp [val low high]
  (-> val (max low) (min high)))

(defn cap-player-to-board [player-state]
  (-> player-state
      (update-in [:x] clamp 0 default-board-width)
      (update-in [:y] clamp 0 default-board-height)))
	
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
	(cap-player-to-board
	 (if (= 0 top-card-time)
	   (assoc
	       ((player-actions (:type top-card)) player-state)
	     :queue (rest queue))
	   (assoc player-state
	     :queue
	     (cons (assoc top-card :time (dec top-card-time))
		   (rest queue)))))))))

(defn probability-lower-than [p]
  (< (.nextFloat random) p))
(defn increment-card-time [card] (assoc card :time (inc (:time card))))
(defn stocastically-increment-card-time [card]
  (if (probability-lower-than (/ 1 (+ 5 (:time card))))
    (increment-card-time card)
    card))
(defn increment-deck-time [player]
  (assoc player :deck
	 (map stocastically-increment-card-time (:deck player))))

(defn player-in-goal [{goal-x :x, goal-y :y} [name {x :x, y :y}]]
  (and (= goal-x x) (= goal-y y)))
(defn players-in-goal [current-game-state]
  (filter (partial player-in-goal (:goal current-game-state))
	  (:players current-game-state)))

(defn advance-time [current-game-state]
  (if (contains? current-game-state :winners)
    current-game-state
    (let [players (:players current-game-state)
	  players (fmap advance-time-for-player players)
	  players (fmap increment-deck-time players)
	  players-who-won (players-in-goal current-game-state)
	  new-state (assoc current-game-state
		      :players players
		      :last-updated (System/currentTimeMillis))]
      (if (seq players-who-won)
	(assoc new-state :winners (map first players-who-won))
	new-state))))

(def thread-pool (Executors/newScheduledThreadPool 4))
(def scheduler
  (do (println "Defining scheduler")
      (.scheduleAtFixedRate
       thread-pool
       (fn [] (swap! game-state advance-time))
       0 200
       TimeUnit/MILLISECONDS)))

(defn json-response [data & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string data)})

(defn tailored-game-state [current-game-state player-id]
  (if (contains? (:players current-game-state) player-id)
    (let [players (:players current-game-state)
	  me (get players player-id)
	  others (vals (dissoc players player-id))
	  winner-info (if (contains? current-game-state :winners)
			(if (contains?
			     (set (:winners current-game-state)) player-id)
			  :you-won, :you-lost)
			:undecided)]
      (json-response
       (assoc
	   (dissoc current-game-state :players)
	 :me me, :others others, :winnerinfo winner-info)))
    nil))

; Web-server magic

(defn play-card-on-game-state [current-game-state player-id card-number]
  (if (and (contains? (:players current-game-state) player-id)
	   (<= 0 card-number 4))
      (play-card current-game-state player-id card-number)
      current-game-state))

(defroutes handler
  (GET "/" [] (redirect "/add-player"))
  (GET "/js/core.js" [] (slurp "js/core.js"))
  (GET "/restart" []
       (do (reset! game-state (random-game-state))
	   (redirect "/")))
  (GET "/add-player" []
       (redirect (format "/game/%s/" (add-uuid-player-to-game-state))))
  (GET "/game/:player-id/" [] (slurp "index.html"))
  (GET "/game/:player-id/game-state" [player-id]
       (tailored-game-state @game-state player-id))
  (POST "/game/:player-id/play-card/:card-number" [player-id card-number]
	(do
	  (swap! game-state
		 play-card-on-game-state
		 player-id
		 (Integer/parseInt card-number))
	  (json-response {:status :ok}))))

(def app
     (-> handler
	 wrap-json-params
	 wrap-keyword-params))

(future (run-jetty (var app) {:port 8080}))
