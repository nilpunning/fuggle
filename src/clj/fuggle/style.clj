(ns fuggle.style
  (:require [garden.stylesheet :refer [at-media]]
            [garden.compression :refer [compress-stylesheet]]
            [garden.core :refer [css]]
            [garden.color :as color]
            [garden.selectors :as selectors]
            [garden.units :refer (px px+ px* px- px-div)]))

(def default-font-family
  "-apple-system, BlinkMacSystemFont,
   \"Segoe UI\", \"Roboto\", \"Oxygen\", \"Ubuntu\", \"Cantarell\",
   \"Fira Sans\", \"Droid Sans\", \"Helvetica Neue\",
   sans-serif")

(defn primary-lighten [lightness]
  (-> "black"
      color/from-name
      (color/lighten lightness)
      color/as-hex))

(defn secondary-darken [darkness]
  "A deep blue."
  (-> "#007aff"
      (color/darken darkness)
      color/as-hex))

(defn gradient [top bottom]
  (str "-webkit-linear-gradient(top," top "," bottom ")"))

(def colors
  {::color             (primary-lighten 5)
   ::dem               (primary-lighten 50)
   ::border            (primary-lighten 80)
   ::header-border     (primary-lighten 85)
   ::header-background (primary-lighten 95)
   ::background        (primary-lighten 100)

   ::link-color        (secondary-darken 10)

   ::normal-a          (primary-lighten 99)
   ::normal-b          (primary-lighten 93)

   ::normal-a1         (primary-lighten 94)
   ::normal-b1         (primary-lighten 89)

   ::primary-a         (secondary-darken 0)
   ::primary-b         (secondary-darken 10)

   ::primary-a1        (secondary-darken 5)
   ::primary-b1        (secondary-darken 15)})

(selectors/defpseudoelement -ms-expand)

(def input-submit (selectors/input (selectors/attr= "type" "submit")))
(def input-button (selectors/input (selectors/attr= "type" "button")))
(def input-reset (selectors/input (selectors/attr= "type" "reset")))

(def buttons
  [[:a
    {:color           (::link-color colors)
     :text-decoration "none"}]
   [input-submit
    input-button
    input-reset
    :select
    {:box-sizing         :border-box
     :min-width          0
     :font-size          "20px"
     :font-family        default-font-family
     :-webkit-appearance :none
     :-moz-appearance    :none
     :appearance         :none
     :cursor             :pointer
     :background-color   (::normal-a colors)
     :background         (gradient (::normal-a colors)
                                   (::normal-b colors))
     :border-color       (::border colors)
     :border-style       "solid"
     :border-width       "1px"
     :border-radius      "3px"
     :padding            "2px 10px"}]
   [input-submit
    input-button
    input-reset
    [:&:disabled :&.primary:disabled
     {:background-color (::header-background colors)
      :background       :none
      :border-color     (::header-border colors)
      :color            (::header-border colors)
      :cursor           :auto}]
    [:&.primary
     {:color            (::background colors)
      :background-color (::primary-a colors)
      :background       (gradient (::primary-a colors)
                                  (::primary-b colors))
      :border-color     (::primary-b colors)}]
    [:&:active
     {:background-color (::normal-a1 colors)
      :background       (gradient (::normal-a1 colors)
                                  (::normal-b1 colors))}]
    [:&.primary:active
     {:background-color (::primary-a1 colors)
      :background       (gradient (::primary-a1 colors)
                                  (::primary-b1 colors))}]]
   [:select
    {:background-color (::background colors)
     ::background      "url(/resources/img/down-arrow.png) no-repeat right"
     :padding-right    "30px"}]
   [(selectors/select -ms-expand)
    {:display :none}]])

(def text-input
  [[(selectors/input (selectors/attr= "type" "text"))
    {:box-sizing                  :border-box
     :min-width                   0
     :border-style                "solid"
     :border-width                "1px"
     :border-color                (::border colors)
     :font-size                   "20px"
     :font-family                 default-font-family
     :padding                     "2px"
     ; iOS adds these, remove them.
     :border-radius               0
     :background-clip             :padding-box
     :-webkit-tap-highlight-color :none}]
   [:textarea
    {:box-sizing                  :border-box
     :min-width                   0
     :border-style                "solid"
     :border-width                "1px"
     :border-color                (::border colors)
     :font-family                 default-font-family
     :font-size                   "20px"
     :resize                      "vertical"
     :overflow-x                  "hidden"
     :overflow-y                  "auto"
     :padding                     "2px"
     ; iOS adds these, remove them.
     :border-radius               0
     :background-clip             "padding-box"
     :-webkit-tap-highlight-color "none"}]])

(def base-rules
  [[:html
    {:overflow-y :scroll}]
   [:body
    {:word-wrap                :break-word
     :-ms-text-size-adjust     :none
     :-webkit-text-size-adjust :none
     :text-size-adjust         :none
     :font-family              default-font-family
     :font-size                "20px"
     :min-width                "300px"
     :line-height              "1.6"
     :color                    (::color colors)
     :background-color         (::background colors)}]
   [:pre
    {:font-family default-font-family
     :white-space #{:pre-wrap                               ; CSS 3
                    :-moz-pre-wrap                          ; Mozilla, since 1999 */
                    :-pre-wrap                              ; Opera 4-6
                    :-o-pre-wrap}                           ; Opera 7
     :word-wrap   :break-word}]                             ; Internet Explorer 5.5+
   [:h1 :h2 :h3
    {:line-height 1.2}]
   [:h1 :h2 :h3 :pre :ul :ol
    {:margin-left  0
     :margin-right 0}]
   [:h1
    {:font-size           "24px"
     :font-weight         "normal"
     :border-bottom-color (::color colors)
     :border-bottom-style "solid"
     :border-bottom-width "1px"
     :margin-bottom       0}]
   [:h2
    {:font-size     "20px"
     :font-weight   "bold"
     :margin-bottom 0}]
   [:h3
    {:font-size     "16px"
     :font-weight   "normal"
     :color         (::dem colors)
     :margin-bottom 0}]
   [:a
    {:margin      0
     :font-weight "normal"
     :line-height "28px"
     :font-size   "20px"}]
   buttons
   text-input
   [:ul :ol
    {:margin-top    0
     :margin-bottom 0
     :padding       "0 0 0 36px"}]])

(def layout-rules
  [[:.center {:max-width "900px"
              :margin    :auto}]
   [:.row {:display     #{:flex
                          :-webkit-flex}
           :align-items :center
           :padding     "4px"
           :margin      "0 20px 0 20px"
           :overflow    :hidden}]
   [:.cell {:flex-grow         1
            :-webkit-flex-grow 1}]
   [:.tright {:text-align :right}]])

(def components
  [[:.progress-bar
    {:background-color "transparent"
     :height           "2px"
     :float            :left
     :position         :fixed
     :left             0
     :right            0}]
   [:.progress-bar
    [:.content
     {:background-color (::primary-a colors)
      :height           "2px"}]]
   [:.header
    {:padding             "10px 0"
     :background-color    (::header-background colors)
     :border-bottom-color (::header-border colors)
     :border-bottom-style "solid"
     :border-bottom-width "1px"}]
   [:.dem
    {:color (::dem colors)}]])

(defn adjustable-spaces [width]
  [[:body
    {:margin-top    0
     :margin-right  0
     :margin-bottom width
     :margin-left   0}]

   [:.mright {:margin-right width}]
   [:.mleft {:margin-left width}]

   [:.mhleft {:margin-left (px-div width 2)}]
   [:.mhright {:margin-right (px-div width 2)}]])

(def responsive-parts
  [(adjustable-spaces (px 20))
   (at-media
     {:screen true :max-width "599px"}
     (adjustable-spaces (px 10)))
   (at-media
     {:print true}
     [:.no-print ".no-print *"
      {:display "none !important"}])])

(def css-text
  (compress-stylesheet
    (css
      base-rules
      layout-rules
      components
      responsive-parts)))
