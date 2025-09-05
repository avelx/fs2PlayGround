module Main exposing (main)

import Browser
import Html exposing (Html, div, input, li, text, ul)
import Html.Attributes exposing (placeholder, style, value)
import Html.Events exposing (onClick, onInput)


-- MODEL

type alias Model =
    { query : String
    , result : String
    , suggestions : List String
    }


initialModel : Model
initialModel =
    { query = ""
    , result = ""
    , suggestions = [] }


-- DATA

data : List ( String, String )
data =
    [ ( "apple", "A sweet fruit" )
    , ( "banana", "A yellow fruit" )
    , ( "orange", "A citrus fruit" )
    , ( "apricot", "A small orange fruit" )
    , ( "avocado", "A creamy green fruit" )
    ]


-- UPDATE

type Msg
    = UpdateQuery String
    | SelectSuggestion String


update : Msg -> Model -> Model
update msg model =
    case msg of
        UpdateQuery newQuery ->
            let
                lowerQuery = String.toLower newQuery

                filtered =
                    if newQuery == "" then
                        []
                    else
                        List.filter (\( key, _ ) -> String.contains lowerQuery (String.toLower key)) data
                            |> List.map Tuple.first

                found =
                    List.filter (\( key, _ ) -> String.toLower key == lowerQuery) data
                        |> List.head

                newResult =
                    case found of
                        Just ( _, description ) ->
                            description

                        Nothing ->
                            if newQuery == "" then
                                ""
                            else
                                ""
            in
            { model
                | query = newQuery
                , result = newResult
                , suggestions = filtered
            }

        SelectSuggestion suggestion ->
            let
                found =
                    List.filter (\( key, _ ) -> String.toLower key == String.toLower suggestion) data
                        |> List.head

                newResult =
                    case found of
                        Just ( _, description ) ->
                            description

                        Nothing ->
                            "Not found"
            in
            { model | query = suggestion, result = newResult, suggestions = [] }


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
        [ div
            [ style "position" "relative"
            , style "width" "300px"
            ]
            [ input
                [ placeholder "Type a fruit..."
                , value model.query
                , onInput UpdateQuery
                , style "width" "100%"
                , style "padding" "10px"
                , style "font-size" "16px"
                , style "border" "1px solid #ccc"
                , style "border-radius" "6px"
                ]
                []
            , if List.isEmpty model.suggestions then
                text ""
              else
                ul
                    [ style "list-style" "none"
                    , style "padding" "0"
                    , style "margin" "0"
                    , style "border" "1px solid #ccc"
                    , style "border-top" "none"
                    , style "background" "white"
                    , style "position" "absolute"
                    , style "width" "100%"
                    , style "max-height" "150px"
                    , style "overflow-y" "auto"
                    , style "z-index" "10"
                    ]
                    (List.map
                        (\s ->
                            li
                                [ onClick (SelectSuggestion s)
                                , style "padding" "8px"
                                , style "cursor" "pointer"
                                , style "border-bottom" "1px solid #eee"
                                ]
                                [ text s ]
                        )
                        model.suggestions
                    )
            ]
        , div
            [ style "margin-top" "15px"
            , style "font-size" "18px"
            ]
            [ text (if model.result /= "" then "Result: " ++ model.result else "") ]
        ]


-- MAIN

main =
    Browser.sandbox { init = initialModel, update = update, view = view }
