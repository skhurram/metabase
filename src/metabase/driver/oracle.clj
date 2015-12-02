(ns metabase.driver.oracle
  (:require [clojure.tools.logging :as log]
            (korma [core :as k]
                   [db :as kdb])
            [metabase.db :as db]
            [metabase.driver :as driver]
            [metabase.driver.generic-sql :as sql]
            [metabase.models.setting :refer [defsetting], :as setting]
            [metabase.util :as u]))

(declare maybe-register-driver!)

(defsetting oracle-jdbc-driver-path
  "Path to Oracle JDBC driver. e.g. `/Users/camsaul/Downloads/ojdbc7.jar`.
   [You can download it here](http://www.oracle.com/technetwork/database/features/jdbc/default-2280470.html)."
  nil
  :setter (fn [value]
            (setting/set* :oracle-jdbc-driver-path value)
            (maybe-register-driver!)))

(defn jdbc-driver-available?
  "Is `oracle.jdbc.OracleDriver` available?"
  ([]
   (jdbc-driver-available? (oracle-jdbc-driver-path)))
  ([jar-path]
   (boolean (try (u/add-jar-to-classpath! jar-path)
                 (Class/forName "oracle.jdbc.OracleDriver")
                 (catch Throwable _)))))


(def ^:private ^:const pattern->type
  [[#"ALERT_TYPE" :UnknownField]
   [#"ANYDATA"    :UnknownField]
   [#"BFILE"      :UnknownField]
   [#"BLOB"       :UnknownField]
   [#"CHAR"       :TextField]
   [#"CLOB"       :TextField]
   [#"DATE"       :DateField]
   [#"DOUBLE"     :FloatField]
   [#"FLOAT"      :FloatFields]
   [#"LONG"       :TextField]
   [#"NUMBER"     :FloatField]
   [#"RAW"        :UnknownField]
   [#"ROWID"      :UnknownField]
   [#"TIMESTAMP"  :DateTimeField]
   [#"URI"        :TextField]
   [#"XML"        :UnknownField]])

(defn- column->base-type [_ column-type]
  (let [column-type (name column-type)]
    (loop [[[pattern base-type] & more] pattern->type]
      (cond
        (re-find pattern column-type) base-type
        (seq more)                    (recur more)))))

(defn- connection-details->spec [_ {:keys [sid], :as details}]
  (update (kdb/oracle details) :subname (u/rpartial str \: sid)))

(defn- date [_ unit field-or-value]
  nil)

(defn- date-interval [_ unit amount]
  nil)

(defn- unix-timestamp->timestamp [_ field-or-value seconds-or-milliseconds]
  nil)


(defrecord OracleDriver []
  clojure.lang.Named
  (getName [_] "Oracle"))

(extend OracleDriver
  driver/IDriver
  (merge (sql/IDriverSQLDefaultsMixin)
         {:date-interval  date-interval
          :details-fields (constantly [{:name         "host"
                                        :display-name "Host"
                                        :default      "localhost"}
                                       {:name         "port"
                                        :display-name "Port"
                                        :type         :integer
                                        :default      1521}
                                       {:name         "sid"
                                        :display-name "Oracle System ID"
                                        :required     true}
                                       {:name         "db"
                                        :display-name "Database name"
                                        :placeholder  "BirdsOfTheWorld"
                                        :required     true}
                                       {:name         "user"
                                        :display-name "Database username"
                                        :placeholder  "What username do you use to login to the database?"
                                        :required     true}
                                       {:name         "password"
                                        :display-name "Database password"
                                        :type         :password
                                        :placeholder  "*******"}])})

  sql/ISQLDriver
  (merge (sql/ISQLDriverDefaultsMixin)
         {:column->base-type         column->base-type
          :connection-details->spec  connection-details->spec
          ;; :current-datetime-fn       (constantly (k/sqlfn* :GETUTCDATE))
          :date                      date
          ;; :excluded-schemas          (constantly #{"sys" "INFORMATION_SCHEMA"})
          ;; :stddev-fn                 (constantly :STDEV)
          ;; :string-length-fn          (constantly :LEN)
          :unix-timestamp->timestamp unix-timestamp->timestamp}))

(defn- maybe-register-driver! []
  (db/setup-db-if-needed)
  (when (jdbc-driver-available?)
    (log/info (format "Found Oracle JDBC driver: %s" (oracle-jdbc-driver-path)))
    (driver/register-driver! :oracle (OracleDriver.))))

(maybe-register-driver!)
