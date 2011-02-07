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
        (string? res) res
        (nil? res) res
	:else-is-clob (declob res)))

(defn query [sql-statement & sql-params]
  (with-query-results rs
    (apply vector sql-statement sql-params)
    (process-result rs)))

(defn atomic-query [db sql-statement & sql-params]
  (with-connection db
    (transaction
     (apply query sql-statement sql-params))))

(def migrations [])

(defn apply-migrations [db]
  (with-connection db
    (transaction
     (when (empty? (query "show tables"))
       (create-table "migration" ["name" "varchar(100)"]))
     (let [already-applied (set (query "select name from migration"))]
       (doseq [migration migrations]
         (when-not (already-applied (:name migration))
           ((:update migration))
           (insert-rows "migration" [(:name migration)])))))))

           