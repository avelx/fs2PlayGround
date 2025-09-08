module TextFields exposing (..)

import Browser
import Html exposing (Attribute, Html, button, div, input, p, text)
import Html.Attributes exposing (..)
import Html.Events exposing (onInput)
import Html.Events exposing (onClick)
import Http
import Json.Decode exposing (Decoder, field, map, map2, string, list)

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
            ,fruits: (List Fruit)
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
                    },
                fruits = []
        }
        ,
        Cmd.none
    )


-- UPDATE
type Msg
  =     ChangeOne String
    |   ChangeTwo String
    |   GetFruit  (Result Http.Error Fruit)
    |   GetFruits  (Result Http.Error (List Fruit))
    |   BtnLoad
    |   BtnGetFruits
    |   Success Fruit
    |   Failure Http.Error


fruitDecoder : Decoder Fruit
fruitDecoder =
  map2 Fruit
    (field "name" string)
    (field "description" string)

fruitsDecoder : Decoder (List Fruit)
fruitsDecoder =
    Json.Decode.list fruitDecoder

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
                    update (Success fruit) model
                  Err err ->
                    update (Failure err) model

      GetFruits result ->
                 case result of
                        Ok fs ->
                             ( {model | fruits = fs }, Cmd.none)
                        Err _ ->
                          (model, Cmd.none)


      BtnLoad ->
          ( {model | input = { contentOne = "Loading", contentTwo = model.input.contentTwo } },
           Http.get
                     { url = "http://localhost:8080/api/topItem"
                     , expect = Http.expectJson GetFruit fruitDecoder
                     })

      Success fruit ->
          ( {model | input = { contentOne = fruit.name ++ " " ++ fruit.description, contentTwo = model.input.contentTwo } }, Cmd.none)

      Failure _ ->
            ( {model | input = { contentOne = "Failure", contentTwo = model.input.contentTwo } }, Cmd.none )

      BtnGetFruits ->
                 ( {model | input = { contentOne = "Loading", contentTwo = model.input.contentTwo } },
                     Http.get
                               { url = "http://localhost:8080/api/search?query=a"
                               , expect = Http.expectJson GetFruits fruitsDecoder
                               })


-- VIEW
viewFruit: Model -> Html Msg
viewFruit model =
     div [] [ text  (model.input.contentOne ++  " " ++ model.input.contentTwo ) ]


viewFruits : Model -> Html msg
viewFruits  model =
        div
            [ class "error-messages"
            , style "position" "fixed"
            , style "top" "0"
            , style "background" "rgb(250, 250, 250)"
            , style "padding" "20px"
            , style "border" "1px solid"
            ]
        <|
            List.map (\f -> p [] [ text f.name ]) model.fruits


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
                    , viewFruit model
                    , viewFruits model
                    , div []
                    [
                        input [ placeholder "Text to reverse2", value model.input.contentTwo, onInput ChangeTwo ] []
                    ]
                    , button [onClick BtnLoad ] [ text "Reset" ]
                    , button [onClick BtnGetFruits ] [ text "AsyncLoad" ]

                ]



-- MAIN

main =
  Browser.element
    { init = init,
    update = update,
    subscriptions = subscriptions,
    view = view }

