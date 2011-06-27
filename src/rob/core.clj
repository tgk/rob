(ns rob.core
  (:use rob.game-controllers)
  (:use compojure.core)
  (:use ring.util.response
	ring.middleware.json-params
	ring.middleware.keyword-params)
  (:require [clj-json.core :as json])
  (:use ring.adapter.jetty)
  (:import
   java.util.UUID
   [java.util.concurrent Executors TimeUnit]))

(def game-state (atom (random-game-state)))

(defn add-uuid-player-to-game-state []
  (let [uuid (.toString (UUID/randomUUID))]
    (swap! game-state add-player uuid)
    uuid))

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
	 :me me, :others others, :winnerinfo winner-info,
	 :walls (-> current-game-state :walls seq))))
    nil))

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