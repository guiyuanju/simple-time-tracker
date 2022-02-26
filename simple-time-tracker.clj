#!/usr/bin/env bb

(ns user
  (:require [clojure.tools.cli :as cli]))

(import 'java.time.format.DateTimeFormatter
        'java.time.LocalDateTime)

(defn create-if-not-exists [file]
  (let [f (io/file file)]
    (when (not (.exists f))
      (.createNewFile f))))

;; return:: [String]
(defn csv-data [file]
  (create-if-not-exists file)
  (with-open [reader (io/reader file)]
    (doall (csv/read-csv reader))))

;; record:: [[String]]
(defn overwrite-file [file records]
  (with-open [writer (io/writer file)]
    (csv/write-csv writer records)))

(defn current-timestamp []
  (.getTime (java.util.Date.)))

(defn current-timestamp-string []
  (.toString (current-timestamp)))

(defn to-time-string [timestamp]
  (.toString timestamp))

;; mili second from midnight
(defn ms-since-today []
  (let [date (LocalDateTime/now)
        h (.getHour date)
        m (.getMinute date)
        s (.getSecond date)]
    (* (+ (* h 3600)
          (* m 60)
          s)
       1000)))

(defn prev-midnight-timestamp []
  (- (current-timestamp) (ms-since-today)))

;; raw:: String
(defn decode-tags [raw]
  (if (= raw "")
    nil
    (str/split raw #",\s*")))

(defn is-number [string]
  (and (> (count string) 0)
       (every? #(Character/isDigit %) string)))

;; raw:: String
(defn decode-timestamp [raw]
  (if (is-number raw)
    (Long/parseLong raw)
    nil))

;; raw:: [String]
;; return:: {}?
(defn decode-record [raw]
  (if (< (count raw) 2)
    nil
    (let [tags (decode-tags (first raw))
          start (decode-timestamp (second raw))
          end (and (get raw 2)
                   (decode-timestamp (get raw 2)))]
      (if start
		{:tags tags :start start :end end}
        nil))))

;; return:: [String]
(defn encode-record [record]
  [(or (str/join "," (:tags record)) "")
   (or (str (:start record)) "")
   (or (str (:end record)) "")])

(defn encode-records [records]
  (map encode-record records))

;; return:: [[String]]
(defn get-records [file]
  (into []
        (map decode-record
             (csv-data file))))

(defn is-not-completed [record]
  (not (:end record)))

(defn is-on-session [records]
  (if-let [last-record (last records)]
    (is-not-completed last-record)
    false))

(defn drop-last-session [records]
  (if (>= (count records) 1)
    (subvec records 0 (dec (count records)))
    records))

(defn drop-current-session [records]
  (if (is-on-session records)
    (drop-last-session records)
    records))

(defn complete-current-session [records]
  (if (is-on-session records)
     (conj
      (drop-last-session records)
       (update-in
        (last records)
        [:end]
        (constantly (current-timestamp))))
     records))

;; tags:: [String]
(defn start-new-session [records tags]
  (if (not (is-on-session records))
    (conj
     records
     {:tags tags
      :start (current-timestamp)
      :end nil})
    records))

(defn get-current-session [records]
  (if (is-on-session records)
    (last records)
    nil))

(defn pause-current-session [records]
  (complete-current-session records))

(defn resume-paused-session [records]
  (start-new-session records
                     (:tags (last records))))

(defn time-rep [mili]
  (let [sec (quot mili 1000)
        min (quot sec 60)
        hour (quot min 60)
        min (mod min 60)
        sec (mod sec 60)]
    (if (not= 0 hour)
      (format "%dh %dm %ds" hour min sec)
      (if (not= 0 min)
        (format "%dm %ds" min sec)
        (format "%ds" sec)))))

(defn status [records]
  (if-let [record (get-current-session records)]
    (let [mili-seconds (- (current-timestamp)
                     (:start record))
          tags (if (:tags record)
                 (str " (" (str/join ", " (:tags record)) ")")
                 nil)]
      (str "A tracker is running: "
           (time-rep mili-seconds)
           tags))
    "No tracker running."))

(defn simple-status [records]
  (if-let [record (get-current-session records)]
    1
    0))

(defn records-since-today [records]
  (let [ts (prev-midnight-timestamp)]
    (filter #(> (:start %) ts)
         records)))

(defn summary [records]
  (map
   #(vector
     (first %)
     (time-rep (second %)))
   (apply
    (partial merge-with +)
    (->> records
         (map (fn [r]
                (into {}
                      (map #(vector
                             %
                             (- (if (not (:end r))
                                  (current-timestamp)
                                  (:end r))
                                (:start r)))
                           (if (:tags r)
                             (conj (:tags r) :ALL)
                             [:notag :ALL])))))))))

(defn summary-of-today [records]
  (summary (records-since-today records)))

(defn expand-home [s]
  (if (.startsWith s "~")
    (clojure.string/replace-first s "~" (System/getProperty "user.home"))
    s))

(def cli-options
  [["-f" "--data-file File" "The file to store all data."
    :default "~/.simple-time-tracker-data.csv"
    :parse-fn #(if (re-find #"\s" %) (str "\"" % "\"") %)]
   ["-s" "--status" "The running session."]
   ["-S" "--simple-status" "The running session simple information."]
   ["-n" "--new" "Create a new session."]
   ["-N" "--new-with-tags Tags" "Create a new session with tags, divided by comma, surrounded by double quote. (\"tag1, tag2\")"
    :parse-fn #(str/split % #",\s*")
    :validate [#(not (some str/blank? %)) "Tags shoud not be blank."]]
   ["-d" "--drop" "Drop the current session."]
   ["-c" "--complete" "Complete the current session."]
   ["-p" "--pause" "Pause the current session."]
   ["-r" "--resume" "Resume paused session,"]
   ["-t" "--today" "Get summary of today."]
   ["-h" "--help"]])

(def cli-map (cli/parse-opts *command-line-args* cli-options))

(def options (:options cli-map))

(def file (expand-home (:data-file options)))

(def aut {:running [:drop
                    :complete
                    :pause
                    :today]
          :not-running [:start
                        :resume
                        :today]})

(defn is-valid-action [aut, status, action]
  (some #(= action %)
        (status aut)))

(defn store [file fun action]
  (let [records (get-records file)
        status (if (is-on-session records)
                 :running
                 :not-running)]
    (if (is-valid-action aut status action)
      (do
        (overwrite-file file
                  (encode-records
                   (fun (get-records file))))
        (str action))
      (str "Impossible action: tracker " status))))

(cond
  (:status options) (status (get-records file))
  (:simple-status options) (simple-status (get-records file))
  (:new options) (store file #(start-new-session % nil) :start)
  (:new-with-tags options) (store file #(start-new-session % (:new-with-tags options)) :start)
  (:drop options) (store file drop-current-session :drop)
  (:complete options) (store file complete-current-session :complete)
  (:pause options) (store file pause-current-session :pause)
  (:resume options) (store file resume-paused-session :resume)
  (:today options) (println (str/join "\n" (sort (map #(str/join ": " %) (summary-of-today (get-records file))))))
  (:help options) (println (:summary cli-map)))

