module Search exposing (main)

import Browser
import Html exposing (Html, button, div, input, li, text, ul)
import Html.Attributes exposing (..)
import Html.Events exposing (onClick, onInput)
import Http
import Json.Decode as Decode
import Task
import Time exposing (Posix)


-- MODEL

type alias Model =
    { query : String
    , suggestions : List String
    , debounceTime : Maybe Posix
    }


init : () -> ( Model, Cmd Msg )
init _ =
    ( { query = "", suggestions = [], debounceTime = Nothing }
    , Cmd.none
    )


-- UPDATE

type Msg
    = UpdateQuery String
    | Debounce Posix
    | FetchSuggestions
    | SuggestionsFetched (Result Http.Error (List String))
    | SelectSuggestion String


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        UpdateQuery q ->
            ( { model | query = q }, debounceCmd )

        Debounce now ->
            ( { model | debounceTime = Just now }, Cmd.none )

        FetchSuggestions ->
            if String.length model.query > 1 then
                ( model, getSuggestions model.query )
            else
                ( { model | suggestions = [] }, Cmd.none )

        SuggestionsFetched (Ok suggestions) ->
            ( { model | suggestions = suggestions }, Cmd.none )

        SuggestionsFetched (Err _) ->
            ( { model | suggestions = [] }, Cmd.none )

        SelectSuggestion suggestion ->
            ( { model | query = suggestion, suggestions = [] }, Cmd.none )


-- COMMANDS

debounceCmd : Cmd Msg
debounceCmd =
    Task.perform Debounce Time.now


-- HTTP REQUEST

getSuggestions : String -> Cmd Msg
getSuggestions query =
    Http.get
        { url = "http://localhost:8080/api/search?query=" ++ query
        , expect = Http.expectJson SuggestionsFetched (Decode.list Decode.string)
        }


-- VIEW

view : Model -> Html Msg
view model =
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


-- SUBSCRIPTIONS

subscriptions : Model -> Sub Msg
subscriptions _ =
    Time.every 500 (\_ -> FetchSuggestions)


-- MAIN

main : Program () Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , view = view
        , subscriptions = subscriptions
        }
