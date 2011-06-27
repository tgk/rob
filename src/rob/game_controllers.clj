(ns rob.game-controllers
  (:import java.util.Random)
  (:use clojure.set)
  (:use compojure.core)
  (:use clojure.contrib.generic.functor))

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

(defn add-player [current-game-state player-id]
  (assoc-in current-game-state [:players player-id] (initial-player 0 0)))

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

(defn guarded-move [player-state walls delta-x delta-y]
  (let [new-x (+ (:x player-state) delta-x)
	new-y (+ (:y player-state) delta-y)]
    (if (contains? {:x new-x, :y new-y} walls)
      player-state
      (assoc player-state :x new-x :y new-y))))

(defn move [steps]
  (fn [player-state walls]
    (let [backing? (< steps 0)
	  sign (if backing? - identity)
	  steps (if backing? (- steps) steps)]
      (loop [player-state player-state
	     steps steps]
	(if (= 0 steps)
	  player-state
	  (recur 
	   (case (:direction player-state)
		 :north (guarded-move player-state walls       0 (sign  1))
		 :south (guarded-move player-state walls       0 (sign -1))
		 :east  (guarded-move player-state walls (sign  1)      0)
		 :west  (guarded-move player-state walls (sign -1)      0))
	   (dec steps)))))))

(def clockwise-to {:north :east, :east :south, :south :west, :west :north})
(defn turn-clockwise [player-state]
  (assoc player-state :direction (clockwise-to (:direction player-state))))
(defn turn [times]
  (fn [player-state _] (nth (iterate turn-clockwise player-state) times)))

(def player-actions
     {:forward (move 1)
      :backward (move -1)
      :turn-left (turn 3)
      :turn-right (turn 1)
      :fast-forward (move 2)
      :fire (fn [player-state _] player-state)})

; Advancing time

(defn advance-time-for-player [player-state walls]
  (let [queue (:queue player-state)]
    (if (empty? queue)
      player-state
      (let [top-card (first queue)
	    top-card-time (:time top-card)]
	(cap-player-to-board
	 (if (= 0 top-card-time)
	   (assoc
	       ((player-actions (:type top-card)) player-state walls)
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
	  players (fmap (fn [player-state]
			  (advance-time-for-player player-state (:walls current-game-state)))
			players)
	  players (fmap increment-deck-time players)
	  players-who-won (players-in-goal current-game-state)
	  new-state (assoc current-game-state
		      :players players
		      :last-updated (System/currentTimeMillis))]
      (if (seq players-who-won)
	(assoc new-state :winners (map first players-who-won))
	new-state))))

