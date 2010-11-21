(ns geo-dispersion.core
  (:require [clj-http.client :as client])
  (:require [clojure.contrib.json :as json])
  (:use [clojure-csv.core :only (parse-csv write-csv)])
  (:use [clojure.contrib.seq :only (positions)])
  (:use [clojure.contrib.def :only (defn-memo)])
  (:import (javax.swing JFileChooser JOptionPane))
  (:gen-class))

; Raw HTTP response from geosearch for address like
; "1600+Pennsylvania+Avenue,+Washington,+DC".
; :body is in JSON format.
(defn-memo raw-geosearch [address]
  (client/get "http://where.yahooapis.com/geocode"
	      {:query-params {"q" address, 
			      "appid" "pItVa84o", 
			      "flags" "J"}}))

(defn geosearch [address]
  (let [raw-result (raw-geosearch address)]
    (if (= 200 (:status raw-result))
      (:Results (:ResultSet (json/read-json (:body raw-result))))
      (throw (Exception. (str "Geosearch failed for address: " address))))))

(defn manual-select-csv [show-dialog-method]
  (let [fc (doto (JFileChooser.)
             (.setCurrentDirectory (java.io.File. ".")))
        fcret (show-dialog-method fc)
        ok JFileChooser/APPROVE_OPTION]
    (when (= ok fcret)
      (.getSelectedFile fc))))

; To allow only .csv extension see
; http://download.oracle.com/javase/tutorial/uiswing/components/filechooser.html#filters

(defn manual-select-csv-to-open []
  (manual-select-csv (fn [^JFileChooser fc]
                       (.showOpenDialog fc nil))))

(defn manual-select-csv-to-save []
  (manual-select-csv (fn [^JFileChooser fc]
                       (.showSaveDialog fc nil))))

(defn manual-select-string [title instructions string-list]
  (JOptionPane/showInputDialog
   nil instructions title JOptionPane/PLAIN_MESSAGE nil
   (to-array string-list) (first string-list)))

(defn manual-select-location-field [headings for-who]
  (manual-select-string "Select location field"
                        (str "Which column contains location information for " for-who "?")
                        headings))

(defn parse-network-data [text]
  (let [in-matrix (parse-csv text)
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

(defn location-for-alter-row [alter-row location-index location-chooser]
  (let [locations (geosearch (nth alter-row location-index))]
    (cond (empty? locations) nil
          (empty? (rest locations)) (first locations)
          :else (location-chooser locations))))

(defn index-of-item-in-seq [item items]
  (first (positions #(= item %) items)))

(defn locations-for-alter-rows
  [alter-head alter-rows location-head
   {:keys [location-chooser] :or {location-chooser (fn [desc locs] (first locs))}}]
  (let [location-index (index-of-item-in-seq location-head alter-head)]
    (map (fn [row] (location-for-alter-row row location-index location-chooser))
         alter-rows)))

(defn show-location [location]
  (apply str (interpose ", "
                        (filter #(> (count %) 0)
                                (map #(% location)
                                     [:line1 :line2 :line3 :line4])))))

(defn-memo manual-location-chooser [location-description locations]
  (let [selected (manual-select-string "Choose location"
                                       (str "To which of these locations does "
                                            location-description
                                            " refer?")
                                       (map show-location locations))]
    (first (filter #(= selected (show-location %)) locations))))

(defn degrees-to-radians
  "radians = degrees * 2pi / 360"
  [degrees]
  (* degrees Math/PI (/ 2.0 360)))

(defn distance
  "Calculate distance(km) between lat1,long1,lat2,long2 (radians) or loc1,loc2 (yahoo api format)"
  ([lat1 long1 lat2 long2]
     (let [R 6371] ; km
       (* R
          (Math/acos (+ (* (Math/sin lat1) (Math/sin lat2))
                        (* (Math/cos lat1) (Math/cos lat2) (Math/cos (- long2 long1))))))))
  ([{lat1 :latitude long1 :longitude} {lat2 :latitude long2 :longitude}] ; Yahoo API format - lat1,lat2,long1,long2 are stringified degrees.
     (apply distance (map #(degrees-to-radians (Double/parseDouble %)) [lat1 long1 lat2 long2]))))

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
  (let [in-csv-file (manual-select-csv-to-open)]
    (when in-csv-file
      (let [parsed (parse-network-data (slurp in-csv-file))
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
                                                    manual-location-chooser)
            alter-locations (locations-for-alter-rows alter-head alter-rows
                                                      alter-location-head
                                                      manual-location-chooser)
            ego-alter-distances (map distance ego-locations alter-locations)
            ego-alter-to-alterloc (zipmap (map vector ego-ids alter-ids)
                                          alter-locations)
            ap-distances (map (fn [egoid alter1id alter2id]
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
                                   (map (fn [ego]
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
                  (write-csv all-rows))))))))

(defn -main [& args]
  (main))
