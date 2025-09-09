module TextFields exposing (..)

import Browser
import Domain.Fruit exposing (Fruit, fruitDecoder, fruitsDecoder, fruitsToString)
import Html exposing (Attribute, Html, button, div, input, li, text, ul)
import Html.Attributes exposing (..)
import Html.Events exposing (onCheck, onInput)
import Html.Events exposing (onClick)
import Http
import Task
import Time exposing (Posix)

-- Subscriptions
subscriptions : Model -> Sub Msg
subscriptions _ =
    Time.every 500 (\_ -> FetchSuggestions)

-- MODEL

type alias InputFields =
    { contentOne : String
    , contentTwo : String
    }

type alias Model
    = {
              input : InputFields
            , fruits: (List Fruit)
            , selected: (List String)
            , query: String
            , suggestions : List String
            , debounceTime : Maybe Posix
      }


init : () -> (Model, Cmd Msg)
init _ =
    (
        {
                input =
                    { contentOne = ""
                    , contentTwo = ""
                    },
                fruits = [],
                selected = []
                , query = ""
                , suggestions = []
                ,debounceTime = Nothing
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
    |   AddToBasket String Bool
    |   Debounce Posix
    |   SelectSuggestion String
    |   SuggestionsFetched (Result Http.Error (List Fruit))
    |   UpdateQuery String
    |   FetchSuggestions


getSuggestions : String -> Cmd Msg
getSuggestions q =
           Http.get{
                 url = "http://localhost:8080/api/search?query=" ++ q
                ,expect = Http.expectJson SuggestionsFetched fruitsDecoder
            }

debounceCmd : Cmd Msg
debounceCmd =
    Task.perform Debounce Time.now

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
          ( {model | input = {
                contentOne = fruit.name ++ " " ++ fruit.description, contentTwo = model.input.contentTwo },
                fruits = []  },
                Cmd.none)

      Failure _ ->
            ( {model | input = { contentOne = "Failure", contentTwo = model.input.contentTwo } }, Cmd.none )

      BtnGetFruits ->
                 ( {model | input = { contentOne = "Loading", contentTwo = model.input.contentTwo } },
                     Http.get
                               { url = "http://localhost:8080/api/search?query=a"
                               , expect = Http.expectJson GetFruits fruitsDecoder
                               })

      AddToBasket fruitName checked ->
          if checked then
            ( {model | selected = (model.selected ++ [fruitName]) }, Cmd.none )
         else
            ( {model | selected = (List.filter (\x -> x /= fruitName) model.selected ) }, Cmd.none )

      SelectSuggestion suggestion ->
            ( { model | query = suggestion, suggestions = [] }, Cmd.none )

      UpdateQuery q ->
           ( { model | query = q }, debounceCmd )

      FetchSuggestions ->
            if String.length model.query > 1 then
                ( model, getSuggestions model.query )
            else
                ( { model | suggestions = [] }, Cmd.none )

      Debounce now ->
                  ( { model | debounceTime = Just now }, Cmd.none )

      SuggestionsFetched (Ok suggestions) ->
            ( { model | suggestions = fruitsToString(suggestions) }, Cmd.none )

      SuggestionsFetched (Err _) ->
                  ( { model | suggestions = [] }, Cmd.none )





-- VIEW
viewFruit: Model -> Html Msg
viewFruit model =
     div [] [ text  (model.input.contentOne ++  " " ++ model.input.contentTwo ) ]

viewSelectedFruits: Model -> Html Msg
viewSelectedFruits model =
     div []
            <|
                 List.map (
                    \fruitName ->
                    (
                        div [] [ text  fruitName ]
                    )
     ) (model.selected)


viewFruits : Model -> Html Msg
viewFruits  model =
        div
            [ class "error-messages"
            , style "background" "rgb(250, 250, 250)"
            , style "padding" "20px"
            ]
        <|
            List.indexedMap (
                \index-> \fruitName ->
                    (
                        div [] [
                            div []
                            [ input
                                        [
                                           name  ("fruitId" ++ String.fromInt(index))
                                          , type_ "checkbox"
                                          , onCheck (AddToBasket fruitName)
                                        ]
                                        []
                                    , text ( fruitName)
                                    ]
                        ]
                    )
            ) (fruitsToString model.fruits)

lookUpFruits: Model -> Html Msg
lookUpFruits model =
    div [ class "lookup" ]
        [ input
            [ type_ "text"
            , placeholder "Search..."
            , value model.query
            , onInput UpdateQuery
            ]
            []
        , if List.isEmpty model.suggestions then
            text ""
          else
            ul []
                (List.map (\s -> li [ onClick (SelectSuggestion s) ] [ text s ]) model.suggestions)
        ]



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
                    --input [ placeholder "Text to reverse", value model.input.contentOne, onInput ChangeOne ] []
                    --, viewFruit model
                    --, viewFruits model
                    --, viewSelectedFruits model
                    lookUpFruits model
                    --, div []
                    --[
                    --    input [ placeholder "Text to reverse2", value model.input.contentTwo, onInput ChangeTwo ] []
                    --]
                    --, button [onClick BtnLoad ] [ text "Reset" ]
                    --, button [onClick BtnGetFruits ] [ text "AsyncLoad" ]

                ]

-- MAIN
main =
  Browser.element
    { init = init,
    update = update,
    subscriptions = subscriptions,
    view = view }