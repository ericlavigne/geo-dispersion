(ns geo-dispersion.db
  (:use clojure.contrib.sql))

(def main {:classname "org.h2.Driver"
	    :subprotocol "h2"
	    :subname "geo"})

(defn declob [clob]
  (with-open [rdr (java.io.BufferedReader. (.getCharacterStream clob))]
    (apply str (line-seq rdr))))

(defn process-result [res]
  (cond (or (number? res) (string? res) (keyword? res)) res
	(sequential? res) (doall (map process-result res))
	(map? res) (let [ks (keys res)
			 vs (map res ks)]
		     (zipmap (map process-result ks)
			     (map process-result vs)))
	:else-is-clob (declob res)))

(defn query [sql-statement & sql-params]
  (with-query-results rs
    (apply vector sql-statement sql-params)
    (process-result rs)))

(defn atomic-query [db sql-statement & sql-params]
  (with-connection db
    (transaction
     (apply query sql-statement sql-params))))
