(ns geo-dispersion.core
  (:use [clojure-csv.core :only (parse-csv write-csv)])
  (:use [clojure.contrib.seq :only (positions)])
  (:use [clojure.contrib.def :only (defn-memo)])
  (:use [clojure.contrib.swing-utils :only (do-swing*)])
  (:use [clojure.java.io :as io])
  (:use [geo-dispersion.location :as location])
  (:import (au.com.bytecode.opencsv CSVReader))
  (:import (javax.swing JFileChooser JOptionPane))
  (:import (java.awt Dimension))
  (:gen-class))

(defn read-csv [path]
  (let [buffer (CSVReader. (io/reader path))]
    (lazy-seq
     (loop [res []]
       (if-let [nxt (.readNext buffer)]
         (recur (conj res (seq nxt)))
         res)))))

(defn progress-map
  "Like map, but shows a progress bar.
   Example: (progress-map \"Cleaning dishes\"
                          (fn [dish] (Thread/sleep 500) dish)
                          (range 100))"
  [label f & xs]
  (let [len (apply min (map count xs))
        skip (max 1 (int (/ len 100)))
        progress-bar (javax.swing.JProgressBar. 0 len)
        _ (.setPreferredSize progress-bar (Dimension. 300 50))
        panel (javax.swing.JPanel.)
        _ (do-swing* :now #(doto panel
                             (.add progress-bar)
                             (.setOpaque true)))
        frame (javax.swing.JFrame. label)
        _ (do-swing* :now #(doto frame
                             (.setContentPane panel)
                             (.pack)
                             (.setVisible true)))
        results (doall (apply map
                              (fn [i & xs]
                                (when (= 0 (rem i skip))
                                  (do-swing* :now #(.setValue progress-bar i)))
                                (apply f xs))
                              (cons (range len) xs)))]
    (do-swing* :now #(.dispose frame))
    results))

(defn manual-select-csv [show-dialog-method dialog-title]
  (let [fc (doto (JFileChooser.)
             (.setCurrentDirectory (java.io.File. "."))
             (.setDialogTitle dialog-title))
        fcret (show-dialog-method fc)
        ok JFileChooser/APPROVE_OPTION]
    (when (= ok fcret)
      (.getSelectedFile fc))))

; To allow only .csv extension see
; http://download.oracle.com/javase/tutorial/uiswing/components/filechooser.html#filters

(defn manual-select-csv-to-open []
  (manual-select-csv (fn [^JFileChooser fc]
                       (.showOpenDialog fc nil))
                     "Select a CSV input file."))

(defn manual-select-csv-to-save []
  (manual-select-csv (fn [^JFileChooser fc]
                       (.showSaveDialog fc nil))
                     (str "Create a new file to save the results. "
                          "Must have a .csv extension.")))

(defn manual-select-string [title instructions string-list]
  (JOptionPane/showInputDialog
   nil instructions title JOptionPane/PLAIN_MESSAGE nil
   (if (empty? string-list) nil (to-array string-list))
   (first string-list)))

(defn manual-select-location-field [headings for-who]
  (manual-select-string "Select location field"
                        (str "Which column contains location information for " for-who "?")
                        headings))

(defn parse-network-data [text]
  (let [in-matrix (read-csv text)
        empty-row #(or (empty? %) (every? empty? %))
        non-empty-row (complement empty-row)
        [[alter-head & alter-rows] after-alter-rows] (split-with non-empty-row in-matrix)
        [alter-pair-head & alter-pair-rows] (filter non-empty-row after-alter-rows)
        alter-head (filter not-empty alter-head)
        alter-rows (map #(take (count alter-head) %) alter-rows)
        alter-pair-head (filter not-empty alter-pair-head)
        alter-pair-rows (map #(take (count alter-pair-head) %) alter-pair-rows)]
    {:alter-head alter-head
     :alter-rows alter-rows
     :alter-pair-head alter-pair-head
     :alter-pair-rows alter-pair-rows}))

(defn location-for-alter-row [alter-row location-index location-chooser location-clarifier]
  (let [location-name (nth alter-row location-index)]
    (if (empty? location-name) nil
        (let [location-name (if (.matches location-name "[0-9\\-]*")
                              (location-clarifier location-name)
                              location-name)]
          (if (empty? location-name) nil
              (let [locations (location/geosearch location-name)]
                (cond (empty? locations) nil
                      (empty? (rest locations)) (first locations)
                      :else (location-chooser location-name locations))))))))

(defn index-of-item-in-seq [item items]
  (first (positions #(= item %) items)))

(defn locations-for-alter-rows [alter-head alter-rows location-head key-args]
  (let [location-chooser (if (:location-chooser key-args) (:location-chooser key-args) (fn [desc locs] (first locs)))
        location-clarifier (if (:location-clarifier key-args) (:location-clarifier key-args) (fn [loc-code] nil))
        location-index (index-of-item-in-seq location-head alter-head)
        _ (println "Used " location-head " to determine that index was " location-index)]
    (progress-map (str "Analyzing " location-head " column")
                  (fn [row] (location-for-alter-row row location-index location-chooser location-clarifier))
                  alter-rows)))

(defn-memo manual-location-chooser [location-description locations]
  (let [selected (manual-select-string "Choose location"
                                       (str "To which of these locations does "
                                            location-description
                                            " refer?")
                                       (map location/show locations))]
    (first (filter #(= selected (location/show %)) locations))))

(defn manual-location-clarifier [for-who]
  (memoize (fn [location-code]
             (manual-select-string (str "Unable to understand " for-who " location - please clarify")
                                   (str "Unable to understand " for-who " location: "
                                        location-code
                                        ". Please enter a new description of the location and press OK. "
                                        "Or, to skip this location, press CANCEL.")
                                   nil))))

(defn contents-of-manually-selected-column [dialog-title question column-headings table]
  (let [selected-head (manual-select-string dialog-title question column-headings)
        column-index (index-of-item-in-seq selected-head column-headings)]
    (map #(nth % column-index) table)))

; Note: Actual instructions said absolute value of log of sum,
;       but that makes no sense because very disperse and very
;       close networks won't be distinguishable.
(defn dispersion-index [distances]
  (if (empty? distances) -1
      (Math/log10 (max 1.0 (reduce + distances)))))

; My attempt at improving the dispersion index. This metric is
; a better characterization of a network because its value
; is not dominated by one or a few unusually distant alters.
; It is also more stable as the number of alters changes,
; or as the number of alters with no reported location changes.
(defn dispersion-alt [distances]
  (if (empty? distances) -1
      (/ (reduce + (map (fn [distance]
                          (Math/log10 (max 1.0 distance)))
                        distances))
         (count distances))))

(defn count-until-last-nonempty [items]
  (let [count-minus-one (last (positions #(not (empty? %)) items))]
    (if (nil? count-minus-one) 0 (inc count-minus-one))))

(defn main []
  (try
    (let [in-csv-file (manual-select-csv-to-open)]
      (when in-csv-file
        (let [parsed (parse-network-data in-csv-file)
              alter-head (:alter-head parsed)
              alter-rows (:alter-rows parsed)
              ap-head (:alter-pair-head parsed)
              ap-rows (:alter-pair-rows parsed)
              ego-ids (contents-of-manually-selected-column "EgoId field (ego-alter section)"
                                                            "Which field contains the Ego ID number?"
                                                            alter-head alter-rows)
              alter-ids (contents-of-manually-selected-column "AlterId field (ego-alter section)"
                                                              "Which field contains the Alter ID number?"
                                                              alter-head alter-rows)
              ap-ego-ids (contents-of-manually-selected-column "EgoId field (alter-pair section)"
                                                               "Which field contains the Ego ID number?"
                                                               ap-head ap-rows)
              ap-alter1-ids (contents-of-manually-selected-column "AlterId field 1 (alter-pair section)"
                                                                  "Which field contains the first Alter ID number?"
                                                                  ap-head ap-rows)
              ap-alter2-ids (contents-of-manually-selected-column "AlterId field 2 (alter-pair section)"
                                                                  "Which field contains the second Alter ID number?"
                                                                  ap-head ap-rows)
              ego-location-head (manual-select-location-field alter-head "ego")
              alter-location-head (manual-select-location-field alter-head "alter")
              ego-locations (locations-for-alter-rows alter-head alter-rows
                                                      ego-location-head
                                                      {:location-chooser manual-location-chooser
                                                       :location-clarifier (manual-location-clarifier "ego")})
              alter-locations (locations-for-alter-rows alter-head alter-rows
                                                        alter-location-head
                                                        {:location-chooser manual-location-chooser
                                                         :location-clarifier (manual-location-clarifier "alter")})
              ego-alter-distances (progress-map "ego-alter distances"
                                                distance ego-locations alter-locations)
              ego-alter-to-alterloc (zipmap (map vector ego-ids alter-ids)
                                            alter-locations)
              ap-distances (progress-map "alter pair distances"
                                         (fn [egoid alter1id alter2id]
                                           (apply distance
                                                  (map (fn [alterid]
                                                         (ego-alter-to-alterloc [egoid alterid]))
                                                       [alter1id alter2id])))
                                         ap-ego-ids
                                         ap-alter1-ids
                                         ap-alter2-ids)
              ego-to-distances (group-by :ego (map (fn [ego distance] {:ego ego :distance distance})
                                                   (concat ego-ids ap-ego-ids)
                                                   (concat ego-alter-distances ap-distances)))
              distinct-egos (distinct ego-ids)
              ego-to-metrics (zipmap distinct-egos
                                     (progress-map "geo-dispersion indices"
                                                   (fn [ego]
                                                     (let [distances (filter identity (map :distance (ego-to-distances ego)))]
                                                       {:geo-disp (dispersion-index distances) :geo-alt (dispersion-alt distances)}))
                                                   distinct-egos))
              num-alter-cols (apply max (map count-until-last-nonempty (cons alter-head alter-rows)))
              num-ap-cols (apply max (map count-until-last-nonempty (cons ap-head ap-rows)))
              new-alter-head (concat (take num-alter-cols alter-head)
                                     ["EgoLat" "EgoLon" "AlterLat" "AlterLon" "Distance(km)" "GeoDispersion" "GeoDispersionAlt"])
              new-alter-rows (map (fn [alter-row ego ego-loc alter-loc distance]
                                    (concat (take num-alter-cols alter-row)
                                            [(:latitude ego-loc) (:longitude ego-loc) (:latitude alter-loc) (:longitude alter-loc)
                                             distance (:geo-disp (ego-to-metrics ego)) (:geo-alt (ego-to-metrics ego))]))
                                  alter-rows
                                  ego-ids
                                  ego-locations
                                  alter-locations
                                  ego-alter-distances)
              new-ap-head (concat (take num-ap-cols ap-head)
                                  ["Distance"])
              new-ap-rows (map (fn [ap-row distance]
                                 (concat (take num-ap-cols ap-row)
                                         [distance]))
                               ap-rows
                               ap-distances)
              out-csv-file (manual-select-csv-to-save)]
          (when out-csv-file
            (spit out-csv-file
                  (let [all-rows (concat [new-alter-head] new-alter-rows [[] new-ap-head] new-ap-rows)
                        all-rows (map (fn [row] (map str row)) all-rows)] ; Every cell must be a string.
                    (write-csv all-rows)))))))
    (catch Exception e
      (let [sw (java.io.StringWriter.)
            pw (java.io.PrintWriter. sw)]
        (.printStackTrace e pw)
        (spit "geo-dispersion-error-log.txt" (.toString sw))))))

(defn -main [& args]
  (main))
