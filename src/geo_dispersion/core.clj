(ns geo-dispersion.core
  (:require [clj-http.client :as client])
  (:require [clojure.contrib.json :as json])
  (:use [clojure-csv.core :only (parse-csv write-csv)])
  (:use [clojure.contrib.seq :only (positions)])
  (:use [clojure.contrib.def :only (defn-memo)]))

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
  (let [fc (doto (javax.swing.JFileChooser.)
             (.setCurrentDirectory (java.io.File. ".")))
        fcret (show-dialog-method fc)
        ok javax.swing.JFileChooser/APPROVE_OPTION]
    (when (= ok fcret)
      (.getSelectedFile fc))))

; To allow only .csv extension see
; http://download.oracle.com/javase/tutorial/uiswing/components/filechooser.html#filters

(defn manual-select-csv-to-open []
  (manual-select-csv #(.showOpenDialog % nil)))

(defn manual-select-csv-to-save []
  (manual-select-csv #(.showSaveDialog % nil)))

(defn manual-select-string [title instructions string-list]
  (javax.swing.JOptionPane/showInputDialog
   nil instructions title javax.swing.JOptionPane/PLAIN_MESSAGE nil
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

(defn locations-for-alter-rows
  [alter-head alter-rows location-head
   {:keys [location-chooser] :or {location-chooser (fn [desc locs] (first locs))}}]
  (let [location-index (first (positions #(= location-head %) alter-head))]
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

(defn main []
  (let [in-csv-file (manual-select-csv-to-open)]
    (when in-csv-file
      (let [parsed (parse-network-data (slurp in-csv-file))
            alter-head (:alter-head parsed)
            alter-rows (:alter-rows parsed)
            ego-location-head (manual-select-location-field alter-head "ego")
            alter-location-head (manual-select-location-field alter-head "alter")
            ego-locations (locations-for-alter-rows alter-head alter-rows
                                                    ego-location-head
                                                    manual-location-chooser)
            alter-locations (locations-for-alter-rows alter-head alter-rows
                                                      alter-location-head
                                                      manual-location-chooser)]
        (println (apply str (interpose " | " (map show-location alter-locations))))))))

; See description of work in doc folder.

