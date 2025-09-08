module TextFields exposing (..)

import Browser
import Html exposing (Attribute, Html, button, div, input, text)
import Html.Attributes exposing (..)
import Html.Events exposing (onInput)
import Html.Events exposing (onClick)
import Http

-- Subscriptions
subscriptions : Model -> Sub Msg
subscriptions model =
                Sub.none

-- MODEL

type alias InputFields =
    { contentOne : String
    , contentTwo : String
    }

--type ContentLoad
--    = Load
--    | Success
--    | Failure

type alias Model
    = {
            input : InputFields
      }


init : () -> (Model, Cmd Msg)
init _ =
    (
        {
                input =
                    { contentOne = ""
                    , contentTwo = ""
                    }
        }
        ,
        Cmd.none
        --Http.get
        --  { url = "https://elm-lang.org/assets/public-opinion.txt"
        --  , expect = Http.expectString GetBooks
        --  }

    )


-- UPDATE
type Msg
  =     ChangeOne String
    |   ChangeTwo String
    |   GetBooks  (Result Http.Error String)
    |   BtnLoad
    --|   BtnLoadContent


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
  case msg of
      ChangeOne newText ->
        ( {model | input = { contentOne = newText, contentTwo = model.input.contentTwo } }, Cmd.none)
      ChangeTwo newText ->
        ( {model | input = { contentOne = model.input.contentOne, contentTwo = newText } }, Cmd.none)

      GetBooks result ->
           case result of
                  Ok fullText ->
                    --Debug.log "Value:"
                    ( {model | input = { contentOne = fullText, contentTwo = model.input.contentTwo } }, Cmd.none)
                  Err _ ->
                    ( {model | input = { contentOne = "Failure", contentTwo = model.input.contentTwo } }, Cmd.none)

      BtnLoad ->
          ( {model | input = { contentOne = "Loading", contentTwo = model.input.contentTwo } },
           Http.get
                     { url = "https://elm-lang.org/assets/public-opinion.txt"
                     , expect = Http.expectString GetBooks
                     })


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
                    input [ placeholder "Text to reverse", value model.input.contentOne, onInput ChangeOne ] []
                    , div [] [ text  (model.input.contentOne ++  " " ++ model.input.contentTwo ) ]
                    , div []
                    [
                        input [ placeholder "Text to reverse2", value model.input.contentTwo, onInput ChangeTwo ] []
                    ]
                    , button [onClick BtnLoad ] [ text "Reset" ]
                    --,   button [onClick BtnLoadContent ] [ text "AsyncLoad" ]
                ]


-- MAIN

main =
  Browser.element
    { init = init,
    update = update,
    subscriptions = subscriptions,
    view = view }

