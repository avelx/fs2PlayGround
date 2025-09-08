module TextFields exposing (..)

import Browser
import Html exposing (Attribute, Html, button, div, input, text)
import Html.Attributes exposing (..)
import Html.Events exposing (onInput)
import Html.Events exposing (onClick)
import Http
import Json.Decode exposing (Decoder, field, map, map2, string)

-- Subscriptions
subscriptions : Model -> Sub Msg
subscriptions model =
                Sub.none

-- MODEL

type alias InputFields =
    { contentOne : String
    , contentTwo : String
    }

type alias Model
    = {
            input : InputFields
      }

type alias Fruit =
  {   name : String
    , description: String
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
    )


-- UPDATE
type Msg
  =     ChangeOne String
    |   ChangeTwo String
    |   GetFruit  (Result Http.Error Fruit)
    |   BtnLoad
    --|   BtnLoadContent

fruitDecoder : Decoder Fruit
fruitDecoder =
  map2 Fruit
    (field "name" string)
    (field "description" string)

update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
  case msg of
      ChangeOne newText ->
        ( {model | input = { contentOne = newText, contentTwo = model.input.contentTwo } }, Cmd.none)
      ChangeTwo newText ->
        ( {model | input = { contentOne = model.input.contentOne, contentTwo = newText } }, Cmd.none)

      GetFruit result ->
           case result of
                  Ok fruit ->

                    ( {model | input = { contentOne = fruit.name ++ " " ++ fruit.description, contentTwo = model.input.contentTwo } }, Cmd.none)
                  Err _ ->
                    ( {model | input = { contentOne = "Failure", contentTwo = model.input.contentTwo } }, Cmd.none)

      BtnLoad ->
          ( {model | input = { contentOne = "Loading", contentTwo = model.input.contentTwo } },
           Http.get
                     { url = "http://localhost:8080/api/topItem"
                     , expect = Http.expectJson GetFruit fruitDecoder
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

