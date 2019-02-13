(ns fuggle.photo
  (:import [java.awt AlphaComposite Color]
           [java.awt.image BufferedImage]
           [java.io InputStream ByteArrayInputStream ByteArrayOutputStream]
           [java.util Base64]
           [java.security MessageDigest]
           [javax.imageio ImageIO]
           [org.im4java.core IMOperation ConvertCmd Stream2BufferedImage]
           [org.im4java.process Pipe]))

(defn checksum [bytes]
  (.encodeToString
    (.withoutPadding (Base64/getUrlEncoder))
    (.digest
      (MessageDigest/getInstance "MD5")
      bytes)))

(defn resize-operation [tempfile size]
  (doto (IMOperation.)
    (.addImage (into-array String [(.getAbsolutePath tempfile)]))
    .flatten
    (.resize (int size) (int size) ">")
    (.addImage (into-array String ["jpg:-"]))))

(defn resize [tempfile size]
  (with-open [baos (ByteArrayOutputStream.)]
    (let [convert-cmd (ConvertCmd.)
          pipe (Pipe. nil baos)]
      (.setOutputConsumer convert-cmd pipe)
      (.run convert-cmd (resize-operation tempfile size) (make-array Object 0))
      (.toByteArray baos))))

(defn photo-map [{:keys [tempfile size limit]}]
  (when (and
          (not (nil? tempfile))
          (> size 0)
          (< size limit))
    (let [format-name "jpg"
          bytes (resize tempfile 300)]
      {:photo_name (str "p" (checksum bytes) "." format-name)
       :photo      bytes})))

(comment
  (import [java.io File])
  (photo-map (File. "/Users/dave/Desktop/fries-small.png"))
  )