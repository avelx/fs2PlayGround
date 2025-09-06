module TextFields exposing (..)

import Browser
import Html exposing (Attribute, Html, button, div, input, text)
import Html.Attributes exposing (..)
import Html.Events exposing (onInput)
import Html.Events exposing (onClick)



-- MAIN


main =
  Browser.sandbox { init = init, update = update, view = view }



-- MODEL


type alias Model =
  { content : String,
    contentSecond : String
  }


init : Model
init =
  { content = ""
   , contentSecond = ""}



-- UPDATE
type Msg
  =     ChangeOne String
    |   ChangeTwo String
    |   BtnResetClick


update : Msg -> Model -> Model
update msg model =
  case msg of
    ChangeOne newContent ->
      { model | content = newContent }
    ChangeTwo newContent ->
      { model | contentSecond = newContent }
    BtnResetClick ->
          { model | contentSecond = "", content = "" }

-- VIEW

view : Model -> Html Msg
view model =
  div
    [ style "display" "flex"
          , style "flex-direction" "column"
          , style "justify-content" "center"
          , style "align-items" "center"
          , style "height" "100vh"
          , style "font-family" "sans-serif"
          , style "background" "#f8f8f8"
     ]
    [
        input [ placeholder "Text to reverse", value model.content, onInput ChangeOne ] []
        , div [] [ text  (model.content ++  " " ++ model.contentSecond ) ]
        , div []
        [
            input [ placeholder "Text to reverse2", value model.contentSecond, onInput ChangeTwo ] []
        ],
        button [onClick BtnResetClick ] [ text "Reset" ]

    ]



