(ns geo-dispersion.core
  (:require [clj-http.client :as client])
  (:require [clojure.contrib.json :as json])
  (:use [clojure-csv.core :only (parse-csv write-csv)]))

; Raw HTTP response from geosearch for address like
; "1600+Pennsylvania+Avenue,+Washington,+DC".
; :body is in JSON format.
(defn raw-geosearch [address]
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
  (let [fc (javax.swing.JFileChooser.)
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

(defn manual-select-location-field [headings]
  (manual-select-string "Select location field"
                        "Which column contains location information?"
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

(defn main []
  (let [in-csv-file (manual-select-csv-to-open)]
    (when in-csv-file
      (let [parsed (parse-network-data (slurp in-csv-file))
            alter-head (:alter-head parsed)
            location-head (manual-select-location-field alter-head)]
        (println location-head)))))

; See description of work in doc folder.

